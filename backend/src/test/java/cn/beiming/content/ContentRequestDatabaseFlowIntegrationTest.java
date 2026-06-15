package cn.beiming.content;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "beiming.business-core.test-control-headers.enabled=true"
)
class ContentRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "content-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:content_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ContentStore store;

    @Autowired
    TestAuthContextProvider auth;

    @Autowired
    TestProfileSnapshotProvider profile;

    @Autowired
    TestNotificationClient notification;

    @BeforeEach
    void setUp() throws Exception {
        auth.reset();
        profile.reset();
        notification.reset();
        store.reset();
        store.seedTestData(profile);
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "content_flow_request_log",
                    "content_flow_items",
                    "content_flow_categories",
                    "content_flow_seo",
                    "content_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void itemCreateRunsThroughBackendAndDatabaseThenReturnsDraftItem() throws Exception {
        String requestId = "req-item-" + FLOW_ID;
        String slug = "flow-item-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/content/admin/items",
                bearerHeaders("admin-token", requestId),
                contentBody(slug)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/slug").asText()).isEqualTo(slug);
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        String contentId = json.at("/data/contentId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT slug FROM content_flow_items WHERE flow_id = ? AND content_id = ? AND action = 'CONTENT_ITEM_CREATED'",
                    FLOW_ID, contentId, slug);
            assertSingleValue(connection,
                    "SELECT status FROM content_flow_items WHERE flow_id = ? AND content_id = ? AND action = 'CONTENT_ITEM_CREATED'",
                    FLOW_ID, contentId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM content_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CONTENT_ITEM_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM content_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/content/admin/items'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: content item create reached backend, wrote item/audit/request rows, and returned 201.");
    }

    @Test
    void itemPublishRunsThroughBackendAndDatabaseThenReturnsPublishedItem() throws Exception {
        String requestId = "req-publish-" + FLOW_ID;
        String contentId = store.contentIdBySlug("offline-only");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/content/admin/items/" + contentId + "/publish",
                bearerHeaders("admin-token", requestId),
                Map.of("reason", "publish sql evidence")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/contentId").asText()).isEqualTo(contentId);
        assertThat(json.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(json.at("/data/publishedAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM content_flow_items WHERE flow_id = ? AND content_id = ? AND action = 'CONTENT_ITEM_PUBLISHED'",
                    FLOW_ID, contentId, "APPROVED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM content_flow_items WHERE flow_id = ? AND content_id = ? AND action = 'CONTENT_ITEM_PUBLISHED' AND published_at IS NOT NULL",
                    FLOW_ID, contentId, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM content_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CONTENT_ITEM_PUBLISHED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM content_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/content/admin/items/" + contentId + "/publish", 200);
        }
        System.out.println("SQL evidence: content item publish reached backend, wrote item/audit/request rows, and returned 200.");
    }

    @Test
    void categoryCreateRunsThroughBackendAndDatabaseThenReturnsCreatedCategory() throws Exception {
        String requestId = "req-category-" + FLOW_ID;
        String slug = "flow-category-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/content/admin/categories",
                bearerHeaders("admin-token", requestId),
                Map.of(
                        "name", "Flow Category",
                        "slug", slug,
                        "description", "SQL flow category",
                        "sortOrder", 18,
                        "reason", "category sql evidence"
                )
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/slug").asText()).isEqualTo(slug);
        String categoryId = json.at("/data/categoryId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT slug FROM content_flow_categories WHERE flow_id = ? AND category_id = ? AND action = 'CONTENT_CATEGORY_CREATED'",
                    FLOW_ID, categoryId, slug);
            assertSingleValue(connection,
                    "SELECT archived FROM content_flow_categories WHERE flow_id = ? AND category_id = ? AND action = 'CONTENT_CATEGORY_CREATED'",
                    FLOW_ID, categoryId, false);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM content_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CONTENT_CATEGORY_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM content_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/content/admin/categories'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: content category create reached backend, wrote category/audit/request rows, and returned 201.");
    }

    @Test
    void seoUpsertRunsThroughBackendAndDatabaseThenReturnsSavedSeo() throws Exception {
        String requestId = "req-seo-" + FLOW_ID;
        String route = "/flow-seo-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.PUT,
                "/api/v1/content/admin/seo/by-route",
                bearerHeaders("admin-token", requestId),
                Map.of(
                        "route", route,
                        "title", "SEO " + route,
                        "description", "SEO flow description",
                        "keywords", List.of("beiming", "flow"),
                        "coverUrl", "/seo-flow.png",
                        "robots", "INDEX_FOLLOW",
                        "canonicalUrl", "https://example.com" + route,
                        "reason", "seo sql evidence"
                )
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/route").asText()).isEqualTo(route);
        String seoId = json.at("/data/seoId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT route FROM content_flow_seo WHERE flow_id = ? AND seo_id = ? AND action = 'CONTENT_SEO_CREATED'",
                    FLOW_ID, seoId, route);
            assertSingleValue(connection,
                    "SELECT enabled FROM content_flow_seo WHERE flow_id = ? AND seo_id = ? AND action = 'CONTENT_SEO_CREATED'",
                    FLOW_ID, seoId, true);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM content_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CONTENT_SEO_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM content_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/content/admin/seo/by-route'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: content seo upsert reached backend, wrote seo/audit/request rows, and returned 200.");
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private Map<String, Object> contentBody(String slug) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("type", "ARTICLE");
        body.put("slug", slug);
        body.put("title", "Title " + slug);
        body.put("summary", "Summary " + slug);
        body.put("body", "Body <script>alert(1)</script>");
        body.put("coverUrl", "/covers/" + slug + ".png");
        body.put("categoryId", store.categoryId("news"));
        body.put("tagIds", List.of(store.tagId("guide")));
        body.put("visibility", "PUBLIC");
        body.put("authorUserId", "user");
        body.put("adminNote", "admin note");
        body.put("reason", "item sql evidence");
        return body;
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object third, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            statement.setObject(3, third);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS content_flow_items (
                    flow_id VARCHAR(128) NOT NULL,
                    content_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    published_at VARCHAR(64),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS content_flow_categories (
                    flow_id VARCHAR(128) NOT NULL,
                    category_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    archived BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS content_flow_seo (
                    flow_id VARCHAR(128) NOT NULL,
                    seo_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    route VARCHAR(256) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS content_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS content_flow_request_log (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    path VARCHAR(256) NOT NULL,
                    response_code INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    private static void deleteFlowRows(Statement statement, String table) {
        try {
            statement.executeUpdate("DELETE FROM " + table + " WHERE flow_id = '" + FLOW_ID + "'");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        ContentFlowEvidenceRecorder contentFlowEvidenceRecorder() {
            return new JdbcContentFlowEvidenceRecorder();
        }
    }

    static class JdbcContentFlowEvidenceRecorder implements ContentFlowEvidenceRecorder {
        @Override
        public void recordItemWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String contentId = String.valueOf(payload.get("contentId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO content_flow_items(flow_id, content_id, action, slug, status, published_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, contentId, action, payload.get("slug"), payload.get("status"), payload.get("publishedAt"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, contentId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write content item database evidence", exception);
            }
        }

        @Override
        public void recordCategoryWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String categoryId = String.valueOf(payload.get("categoryId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO content_flow_categories(flow_id, category_id, action, slug, archived, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, categoryId, action, payload.get("slug"), payload.get("archived"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, categoryId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write content category database evidence", exception);
            }
        }

        @Override
        public void recordSeoWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String seoId = String.valueOf(payload.get("seoId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO content_flow_seo(flow_id, seo_id, action, route, enabled, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, seoId, action, payload.get("route"), true, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, seoId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write content seo database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO content_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO content_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, requestId, path, responseCode, Timestamp.from(Instant.now()));
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
