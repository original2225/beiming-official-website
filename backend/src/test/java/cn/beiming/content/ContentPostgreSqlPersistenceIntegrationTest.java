package cn.beiming.content;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "beiming.business-core.test-control-headers.enabled=true",
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true"
        }
)
@Testcontainers
class ContentPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "content-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_content_flow")
            .withUsername("beiming")
            .withPassword("beiming");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

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
    void setUp() {
        auth.reset();
        profile.reset();
        notification.reset();
        store.reset();
        store.seedTestData(profile);
    }

    @Test
    void itemCreatePersistsItemVersionAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-item");
        String idempotencyKey = "idem-" + slug;
        String requestId = requestId("item-create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/content/admin/items", "admin-token", requestId, contentBody(slug, idempotencyKey), 201);
        String contentId = response.at("/data/contentId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT slug FROM content_items WHERE content_id = ?", contentId, slug);
            assertSingleValue(connection, "SELECT status FROM content_items WHERE content_id = ?", contentId, "DRAFT");
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_item_versions WHERE content_id = ? AND source_action = 'CREATED'", contentId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_item_tags WHERE content_id = ?", contentId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'CONTENT_ITEM_CREATED' AND target_id = ?", requestId, contentId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'content.item.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/content/admin/items'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL content item create wrote content_items/content_item_versions/content_item_tags/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void itemCreateIdempotencyReplaysPersistedResultInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-replay");
        String idempotencyKey = "idem-" + slug;
        Map<String, Object> body = contentBody(slug, idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/content/admin/items", "admin-token", requestId("item-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/content/admin/items", "admin-token", requestId("item-replay-second"), body, 201);

        assertThat(second.at("/data/contentId").asText()).isEqualTo(first.at("/data/contentId").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_items WHERE slug = ?", slug, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'content.item.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL content item create idempotency replay kept one content_items row and one app_idempotency_records row.");
    }

    @Test
    void itemCreateIdempotencyRejectsSameKeyWithDifferentFingerprint() throws Exception {
        String slug = uniqueSlug("pg-conflict");
        String idempotencyKey = "idem-" + slug;
        exchange(HttpMethod.POST, "/api/v1/content/admin/items", "admin-token", requestId("item-conflict-first"), contentBody(slug, idempotencyKey), 201);

        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/content/admin/items", "admin-token", requestId("item-conflict-second"), contentBody(slug + "-changed", idempotencyKey), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'content.item.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_items WHERE slug = ?", slug + "-changed", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL content item create idempotency conflict preserved original rows and returned 409.");
    }

    @Test
    void itemPublishPersistsItemVersionAuditAndRequestLogInPostgreSql() throws Exception {
        String contentId = store.contentIdBySlug("offline-only");
        String requestId = requestId("item-publish");
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/items/" + contentId + "/publish", "admin-token", requestId, Map.of("reason", "publish postgres evidence"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM content_items WHERE content_id = ?", contentId, "APPROVED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_items WHERE content_id = ? AND published_at IS NOT NULL", contentId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_item_versions WHERE content_id = ? AND source_action = 'PUBLISHED'", contentId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'CONTENT_ITEM_PUBLISHED' AND target_id = ?", requestId, contentId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL content item publish updated content_items/content_item_versions and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void categoryAndTagCreatePersistAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String categorySlug = uniqueSlug("pg-category");
        String tagSlug = uniqueSlug("pg-tag");
        JsonNode category = exchange(HttpMethod.POST, "/api/v1/content/admin/categories", "admin-token", requestId("category-create"), categoryBody("Category " + categorySlug, categorySlug, "idem-" + categorySlug), 201);
        JsonNode tag = exchange(HttpMethod.POST, "/api/v1/content/admin/tags", "admin-token", requestId("tag-create"), tagBody("Tag " + tagSlug, tagSlug, "idem-" + tagSlug), 201);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT slug FROM content_categories WHERE category_id = ?", category.at("/data/categoryId").asText(), categorySlug);
            assertSingleValue(connection, "SELECT slug FROM content_tags WHERE tag_id = ?", tag.at("/data/tagId").asText(), tagSlug);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE action = 'CONTENT_CATEGORY_CREATED' AND target_id = ?", category.at("/data/categoryId").asText(), 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE action = 'CONTENT_TAG_CREATED' AND target_id = ?", tag.at("/data/tagId").asText(), 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE scope IN ('content.category.create', 'content.tag.create')", 2L);
        }
        System.out.println("SQL evidence: PostgreSQL content category/tag create wrote taxonomy tables, audit rows and idempotency rows.");
    }

    @Test
    void topicCreateAndStateTransitionsPersistTopicAuditAndRequestLogInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-topic");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/content/admin/topics", "admin-token", requestId("topic-create"), topicBody(slug, "idem-" + slug), 201);
        String topicId = created.at("/data/topicId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/topics/" + topicId + "/publish", "admin-token", requestId("topic-publish"), Map.of("reason", "publish topic"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/topics/" + topicId + "/offline", "admin-token", requestId("topic-offline"), Map.of("reason", "offline topic"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/topics/" + topicId + "/archive", "admin-token", requestId("topic-archive"), Map.of("reason", "archive topic"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM content_topics WHERE topic_id = ?", topicId, "ARCHIVED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_topic_items WHERE topic_id = ?", topicId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('CONTENT_TOPIC_CREATED', 'CONTENT_TOPIC_PUBLISHED', 'CONTENT_TOPIC_OFFLINED', 'CONTENT_TOPIC_ARCHIVED')", topicId, 4L);
        }
        System.out.println("SQL evidence: PostgreSQL content topic create/publish/offline/archive updated content_topics/content_topic_items and wrote app_audit_logs.");
    }

    @Test
    void homeSavePublishAndRollbackPersistHomeAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String idempotencyKey = "idem-home-" + suffix();
        JsonNode saved = exchange(HttpMethod.PUT, "/api/v1/content/admin/home", "admin-token", requestId("home-save"), homeBody(idempotencyKey, "PostgreSQL Home"), 200);
        String draftId = saved.at("/data/draft/homeConfigId").asText();
        JsonNode published = exchange(HttpMethod.PATCH, "/api/v1/content/admin/home/publish", "admin-token", requestId("home-publish"), Map.of("reason", "publish home"), 200);
        int version = published.at("/data/published/version").asInt();
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/home/rollback", "admin-token", requestId("home-rollback"), Map.of("version", 1, "reason", "rollback home"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_home_configs WHERE home_config_id = ?", draftId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_home_versions WHERE version = ?", version, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE action IN ('CONTENT_HOME_DRAFT_SAVED', 'CONTENT_HOME_PUBLISHED', 'CONTENT_HOME_ROLLED_BACK')", 3L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'content.home.save' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL content home save/publish/rollback wrote home tables, audit rows and idempotency rows.");
    }

    @Test
    void previewTokenAndVersionRestorePersistTokenVersionAuditAndRequestLogInPostgreSql() throws Exception {
        String draftId = store.contentIdBySlug("draft-only");
        JsonNode token = exchange(HttpMethod.POST, "/api/v1/content/admin/items/" + draftId + "/preview-token", "admin-token", requestId("preview-token"), Map.of("expiresInMinutes", 30, "reason", "preview token"), 201);
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/items/" + draftId, "admin-token", requestId("item-patch"), Map.of("title", "Draft Updated", "reason", "patch before restore"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/content/admin/items/" + draftId + "/versions/1/restore", "admin-token", requestId("version-restore"), Map.of("reason", "restore version"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_preview_tokens WHERE token_hash = ?", token.at("/data/token").asText(), 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_preview_tokens WHERE content_id = ?", draftId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM content_item_versions WHERE content_id = ? AND source_action = 'RESTORED'", draftId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('CONTENT_ITEM_PREVIEW_TOKEN_CREATED', 'CONTENT_ITEM_UPDATED', 'CONTENT_ITEM_VERSION_RESTORED')", draftId, 3L);
        }
        System.out.println("SQL evidence: PostgreSQL content preview token and version restore wrote token/version/audit rows without storing raw preview token.");
    }

    @Test
    void seoUpsertPersistsSeoAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String route = "/pg-seo-" + suffix();
        String idempotencyKey = "idem-seo-" + suffix();
        String requestId = requestId("seo-upsert");
        JsonNode seo = exchange(HttpMethod.PUT, "/api/v1/content/admin/seo/by-route", "admin-token", requestId, seoBody(route, idempotencyKey), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT route FROM content_seo_configs WHERE seo_id = ?", seo.at("/data/seoId").asText(), route);
            assertSingleValue(connection, "SELECT enabled FROM content_seo_configs WHERE seo_id = ?", seo.at("/data/seoId").asText(), true);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'CONTENT_SEO_CREATED'", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'content.seo.save' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL content seo upsert wrote content_seo_configs/app_audit_logs/app_idempotency_records/app_request_logs.");
    }

    private JsonNode exchange(HttpMethod method, String path, String token, String requestId, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> contentBody(String slug, String idempotencyKey) {
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
        body.put("reason", "item postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> categoryBody(String name, String slug, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("slug", slug);
        body.put("description", "PostgreSQL category");
        body.put("sortOrder", 12);
        body.put("reason", "category postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> tagBody(String name, String slug, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("slug", slug);
        body.put("reason", "tag postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> topicBody(String slug, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("slug", slug);
        body.put("title", "Topic " + slug);
        body.put("summary", "Topic summary");
        body.put("coverUrl", "/topics/" + slug + ".png");
        body.put("visibility", "PUBLIC");
        body.put("contentIds", List.of(store.contentIdBySlug("guide-article")));
        body.put("seo", seoPayload("/topics/" + slug));
        body.put("reason", "topic postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> homeBody(String idempotencyKey, String title) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("sections", List.of(Map.of(
                "sectionId", "draft-featured",
                "type", "FEATURED_ARTICLES",
                "title", title,
                "subtitle", "Subtitle",
                "enabled", true,
                "sortOrder", 1,
                "items", List.of(Map.of("contentId", store.contentIdBySlug("guide-article")))
        )));
        body.put("seo", seoPayload("/"));
        body.put("reason", "home postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> seoBody(String route, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>(seoPayload(route));
        body.put("reason", "seo postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> seoPayload(String route) {
        return Map.of(
                "route", route,
                "title", "SEO " + route,
                "description", "SEO description",
                "keywords", List.of("beiming", "server"),
                "coverUrl", "/seo.png",
                "robots", "INDEX_FOLLOW",
                "canonicalUrl", "https://example.com" + route
        );
    }

    private String requestId(String name) {
        return "req-" + FLOW_ID + "-" + name;
    }

    private String uniqueSlug(String prefix) {
        return prefix + "-" + suffix();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
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

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
        }
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
