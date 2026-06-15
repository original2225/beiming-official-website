package cn.beiming.resource;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
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
class ResourceRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "resource-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:resource_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "resource_flow_request_log",
                    "resource_flow_resources",
                    "resource_flow_versions",
                    "resource_flow_categories",
                    "resource_flow_downloads",
                    "resource_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void resourceCreateRunsThroughBackendAndDatabaseThenReturnsDraftResource() throws Exception {
        String requestId = "req-resource-" + FLOW_ID;
        String slug = "flow-resource-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/resources/admin/items",
                bearerHeaders("admin-token", requestId),
                resourceBody(slug)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/slug").asText()).isEqualTo(slug);
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        String resourceId = json.at("/data/resourceId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT slug FROM resource_flow_resources WHERE flow_id = ? AND resource_id = ? AND action = 'RESOURCE_CREATED'",
                    FLOW_ID, resourceId, slug);
            assertSingleValue(connection,
                    "SELECT status FROM resource_flow_resources WHERE flow_id = ? AND resource_id = ? AND action = 'RESOURCE_CREATED'",
                    FLOW_ID, resourceId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM resource_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'RESOURCE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM resource_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/resources/admin/items'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: resource create reached backend, wrote resource/audit/request rows, and returned 201.");
    }

    @Test
    void versionCreateRunsThroughBackendAndDatabaseThenReturnsEnabledVersion() throws Exception {
        String requestId = "req-version-" + FLOW_ID;
        String versionName = "flow-version-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/resources/admin/items/res-draft/versions",
                bearerHeaders("admin-token", requestId),
                versionBody(versionName)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/resourceId").asText()).isEqualTo("res-draft");
        assertThat(json.at("/data/versionName").asText()).isEqualTo(versionName);
        assertThat(json.at("/data/status").asText()).isEqualTo("ENABLED");
        String versionId = json.at("/data/versionId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT version_name FROM resource_flow_versions WHERE flow_id = ? AND version_id = ? AND action = 'RESOURCE_VERSION_CREATED'",
                    FLOW_ID, versionId, versionName);
            assertSingleValue(connection,
                    "SELECT download_entry_id FROM resource_flow_versions WHERE flow_id = ? AND version_id = ? AND action = 'RESOURCE_VERSION_CREATED'",
                    FLOW_ID, versionId, "entry-" + versionId);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM resource_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'RESOURCE_VERSION_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM resource_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/resources/admin/items/res-draft/versions'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: resource version create reached backend, wrote version/audit/request rows, and returned 201.");
    }

    @Test
    void categoryCreateRunsThroughBackendAndDatabaseThenReturnsCreatedCategory() throws Exception {
        String requestId = "req-category-" + FLOW_ID;
        String slug = "flow-category-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/resources/admin/categories",
                bearerHeaders("admin-token", requestId),
                categoryBody(slug)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/slug").asText()).isEqualTo(slug);
        String categoryId = json.at("/data/categoryId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT slug FROM resource_flow_categories WHERE flow_id = ? AND category_id = ? AND action = 'RESOURCE_CATEGORY_CREATED'",
                    FLOW_ID, categoryId, slug);
            assertSingleValue(connection,
                    "SELECT archived FROM resource_flow_categories WHERE flow_id = ? AND category_id = ? AND action = 'RESOURCE_CATEGORY_CREATED'",
                    FLOW_ID, categoryId, false);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM resource_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'RESOURCE_CATEGORY_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM resource_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/resources/admin/categories'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: resource category create reached backend, wrote category/audit/request rows, and returned 201.");
    }

    @Test
    void downloadRunsThroughBackendAndDatabaseThenReturnsTicket() throws Exception {
        String requestId = "req-download-" + FLOW_ID;
        String idempotencyKey = "download-" + UUID.randomUUID();

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/resources/res-public-client/versions/ver-client-1/download",
                publicHeaders(requestId),
                Map.of("clientLabel", "browser", "idempotencyKey", idempotencyKey)
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/resourceId").asText()).isEqualTo("res-public-client");
        assertThat(json.at("/data/versionId").asText()).isEqualTo("ver-client-1");
        assertThat(json.at("/data/provider").asText()).isEqualTo("CLOUDREVE_SHARE");
        assertThat(json.at("/data/degraded").asBoolean()).isFalse();
        String ticketId = json.at("/data/ticketId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT provider FROM resource_flow_downloads WHERE flow_id = ? AND ticket_id = ? AND action = 'RESOURCE_DOWNLOADED'",
                    FLOW_ID, ticketId, "CLOUDREVE_SHARE");
            assertSingleValue(connection,
                    "SELECT result FROM resource_flow_downloads WHERE flow_id = ? AND ticket_id = ? AND action = 'RESOURCE_DOWNLOADED'",
                    FLOW_ID, ticketId, "SUCCESS");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM resource_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'RESOURCE_DOWNLOADED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM resource_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/resources/res-public-client/versions/ver-client-1/download'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: resource download reached backend, wrote download/audit/request rows, and returned 200.");
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = publicHeaders(requestId);
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders publicHeaders(String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private Map<String, Object> resourceBody(String slug) {
        return new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("type", "CLIENT_PACK"),
                Map.entry("visibility", "PUBLIC"),
                Map.entry("slug", slug),
                Map.entry("title", "Client Pack " + slug),
                Map.entry("summary", "P0 client resource"),
                Map.entry("description", "A stable client package for tests."),
                Map.entry("coverUrl", "/assets/resources/" + slug + ".png"),
                Map.entry("categoryId", "cat-client"),
                Map.entry("tags", List.of("p0", "client")),
                Map.entry("maintainerMemberId", "member-active"),
                Map.entry("visibleFrom", "2026-05-20T00:00:00Z"),
                Map.entry("visibleUntil", "2026-12-31T00:00:00Z"),
                Map.entry("adminNote", "internal note"),
                Map.entry("reason", "resource sql evidence")
        ));
    }

    private Map<String, Object> versionBody(String versionName) {
        return new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("versionName", versionName),
                Map.entry("title", "Version " + versionName),
                Map.entry("changelog", "SQL version"),
                Map.entry("minecraftVersions", List.of("1.20.1")),
                Map.entry("loader", "Fabric"),
                Map.entry("fileSizeBytes", 1024),
                Map.entry("checksumSha256", "a".repeat(64)),
                Map.entry("downloadEntry", Map.of(
                        "provider", "CLOUDREVE_SHARE",
                        "displayName", "Cloudreve",
                        "shareUrl", "https://cloud.example.com/s/" + versionName,
                        "status", "ACTIVE",
                        "expiresAt", "2026-12-31T00:00:00Z",
                        "adminNote", "entry note"
                )),
                Map.entry("releasedAt", "2026-05-22T00:00:00Z"),
                Map.entry("reason", "version sql evidence")
        ));
    }

    private Map<String, Object> categoryBody(String slug) {
        return new java.util.LinkedHashMap<>(Map.of(
                "name", "Category " + slug,
                "slug", slug,
                "description", "category",
                "icon", "folder",
                "sortOrder", 20,
                "enabled", true,
                "reason", "category sql evidence"
        ));
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
                CREATE TABLE IF NOT EXISTS resource_flow_resources (
                    flow_id VARCHAR(128) NOT NULL,
                    resource_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    visibility VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS resource_flow_versions (
                    flow_id VARCHAR(128) NOT NULL,
                    resource_id VARCHAR(128) NOT NULL,
                    version_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    version_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    download_entry_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS resource_flow_categories (
                    flow_id VARCHAR(128) NOT NULL,
                    category_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    archived BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS resource_flow_downloads (
                    flow_id VARCHAR(128) NOT NULL,
                    ticket_id VARCHAR(128) NOT NULL,
                    resource_id VARCHAR(128) NOT NULL,
                    version_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider VARCHAR(64) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS resource_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS resource_flow_request_log (
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
        ResourceFlowEvidenceRecorder resourceFlowEvidenceRecorder() {
            return new JdbcResourceFlowEvidenceRecorder();
        }
    }

    static class JdbcResourceFlowEvidenceRecorder implements ResourceFlowEvidenceRecorder {
        @Override
        public void recordResourceWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String resourceId = String.valueOf(payload.get("resourceId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO resource_flow_resources(flow_id, resource_id, action, slug, status, visibility, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, resourceId, action, payload.get("slug"), payload.get("status"), payload.get("visibility"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, resourceId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write resource database evidence", exception);
            }
        }

        @Override
        public void recordVersionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String versionId = String.valueOf(payload.get("versionId"));
            Map<String, Object> downloadEntry = downloadEntry(payload);
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO resource_flow_versions(flow_id, resource_id, version_id, action, version_name, status, download_entry_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("resourceId"), versionId, action, payload.get("versionName"), payload.get("status"), downloadEntry.get("downloadEntryId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, String.valueOf(payload.get("resourceId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write resource version database evidence", exception);
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
                        "INSERT INTO resource_flow_categories(flow_id, category_id, action, slug, enabled, archived, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, categoryId, action, payload.get("slug"), payload.get("enabled"), payload.get("archived"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, categoryId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write resource category database evidence", exception);
            }
        }

        @Override
        public void recordDownloadWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String ticketId = String.valueOf(payload.get("ticketId"));
            String result = Boolean.TRUE.equals(payload.get("degraded")) ? "DEGRADED" : "SUCCESS";
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO resource_flow_downloads(flow_id, ticket_id, resource_id, version_id, action, provider, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, ticketId, payload.get("resourceId"), payload.get("versionId"), action, payload.get("provider"), result, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, String.valueOf(payload.get("resourceId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write resource download database evidence", exception);
            }
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> downloadEntry(Map<String, Object> payload) {
            return (Map<String, Object>) payload.get("downloadEntry");
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO resource_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO resource_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
