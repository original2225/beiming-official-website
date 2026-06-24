package cn.beiming.resource;

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
import java.util.LinkedHashMap;
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
class ResourcePostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "resource-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_resource_flow")
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
    ResourceStore store;

    @BeforeEach
    void setUp() {
        store.seed();
    }

    @Test
    void itemCreatePersistsItemAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-item");
        String idempotencyKey = "idem-" + slug;
        String requestId = requestId("item-create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId, resourceBody(slug, idempotencyKey), 201);
        String resourceId = response.at("/data/resourceId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT slug FROM resource_items WHERE resource_id = ?", resourceId, slug);
            assertSingleValue(connection, "SELECT status FROM resource_items WHERE resource_id = ?", resourceId, "DRAFT");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'RESOURCE_CREATED' AND target_id = ?", requestId, resourceId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'resource.item.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/resources/admin/items'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL resource item create wrote resource_items/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void itemCreateIdempotencyReplaysPersistedResultAndRejectsChangedFingerprint() throws Exception {
        String slug = uniqueSlug("pg-replay");
        String idempotencyKey = "idem-" + slug;
        Map<String, Object> body = resourceBody(slug, idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId("item-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId("item-replay-second"), body, 201);
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId("item-replay-conflict"), resourceBody(slug + "-changed", idempotencyKey), 409);

        assertThat(second.at("/data/resourceId").asText()).isEqualTo(first.at("/data/resourceId").asText());
        assertThat(conflict.at("/code").asInt()).isEqualTo(43612);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM resource_items WHERE slug = ?", slug, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM resource_items WHERE slug = ?", slug + "-changed", 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'resource.item.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL resource item create idempotency replay/conflict preserved persisted rows.");
    }

    @Test
    void itemPatchAndStateTransitionsPersistItemAuditAndRequestLogInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-lifecycle");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId("item-lifecycle-create"), resourceBody(slug, "idem-" + slug), 201);
        String resourceId = created.at("/data/resourceId").asText();

        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId, "admin-token", requestId("item-patch"), Map.of(
                "summary", "PostgreSQL patched summary",
                "reason", "patch resource"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/submit-review", "admin-token", requestId("item-submit"), Map.of("reason", "submit resource"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/approve", "admin-token", requestId("item-approve"), Map.of(
                "reviewOpinion", "approved",
                "reason", "approve resource"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/publish", "admin-token", requestId("item-publish-failed"), Map.of("reason", "publish without version"), 409);
        exchange(HttpMethod.POST, "/api/v1/resources/admin/items/" + resourceId + "/versions", "admin-token", requestId("item-version-create"), versionBody("pg-lifecycle-version", "idem-version-" + slug), 201);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/publish", "admin-token", requestId("item-publish"), Map.of("reason", "publish resource"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/offline", "admin-token", requestId("item-offline"), Map.of("reason", "offline resource"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/" + resourceId + "/archive", "admin-token", requestId("item-archive"), Map.of("reason", "archive resource"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM resource_items WHERE resource_id = ?", resourceId, "ARCHIVED");
            assertSingleValue(connection, "SELECT summary FROM resource_items WHERE resource_id = ?", resourceId, "PostgreSQL patched summary");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND request_id LIKE ? AND action IN ('RESOURCE_UPDATED', 'RESOURCE_SUBMIT_REVIEW', 'RESOURCE_APPROVE', 'RESOURCE_VERSION_CREATED', 'RESOURCE_PUBLISH', 'RESOURCE_OFFLINE', 'RESOURCE_ARCHIVE')", resourceId, "req-" + FLOW_ID + "-item-%", 7L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id LIKE ? AND path LIKE '/api/v1/resources/admin/items/%' AND response_code IN (200, 201)", "req-" + FLOW_ID + "-item-%", 7L);
        }
        System.out.println("SQL evidence: PostgreSQL resource patch/state transitions/version create wrote item/version/audit/request rows.");
    }

    @Test
    void versionCreatePatchDisableAndEnablePersistVersionEntryAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String versionName = "pg-version-" + suffix();
        String idempotencyKey = "idem-version-" + suffix();
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/resources/admin/items/res-draft/versions", "admin-token", requestId("version-create"), versionBody(versionName, idempotencyKey), 201);
        String versionId = created.at("/data/versionId").asText();

        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/res-draft/versions/" + versionId, "admin-token", requestId("version-patch"), Map.of(
                "changelog", "PostgreSQL patched changelog",
                "reason", "patch version"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/res-draft/versions/" + versionId + "/disable", "admin-token", requestId("version-disable"), Map.of("reason", "disable version"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/items/res-draft/versions/" + versionId + "/enable", "admin-token", requestId("version-enable"), Map.of("reason", "enable version"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT changelog FROM resource_versions WHERE version_id = ?", versionId, "PostgreSQL patched changelog");
            assertSingleValue(connection, "SELECT status FROM resource_versions WHERE version_id = ?", versionId, "ENABLED");
            assertSingleValue(connection, "SELECT provider FROM resource_download_entries WHERE version_id = ?", versionId, "CLOUDREVE_SHARE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = 'res-draft' AND action IN ('RESOURCE_VERSION_CREATED', 'RESOURCE_VERSION_UPDATED', 'RESOURCE_VERSION_DISABLED', 'RESOURCE_VERSION_ENABLED')", 4L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE scope = 'resource.version.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE path LIKE '/api/v1/resources/admin/items/res-draft/versions%' AND response_code IN (200, 201)", 4L);
        }
        System.out.println("SQL evidence: PostgreSQL resource version create/patch/disable/enable wrote version/download entry/audit/idempotency/request rows.");
    }

    @Test
    void categoryCreatePatchAndArchivePersistCategoryAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String slug = uniqueSlug("pg-category");
        String idempotencyKey = "idem-category-" + slug;
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/resources/admin/categories", "admin-token", requestId("category-create"), categoryBody(slug, idempotencyKey), 201);
        String categoryId = created.at("/data/categoryId").asText();

        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/categories/" + categoryId, "admin-token", requestId("category-patch"), Map.of(
                "description", "PostgreSQL patched category",
                "enabled", false,
                "reason", "patch category"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/resources/admin/categories/" + categoryId + "/archive", "admin-token", requestId("category-archive"), Map.of("reason", "archive category"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT description FROM resource_categories WHERE category_id = ?", categoryId, "PostgreSQL patched category");
            assertSingleValue(connection, "SELECT archived FROM resource_categories WHERE category_id = ?", categoryId, true);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('RESOURCE_CATEGORY_CREATED', 'RESOURCE_CATEGORY_UPDATED', 'RESOURCE_CATEGORY_ARCHIVED')", categoryId, 3L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE scope = 'resource.category.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE path LIKE '/api/v1/resources/admin/categories%' AND response_code IN (200, 201)", 3L);
        }
        System.out.println("SQL evidence: PostgreSQL resource category create/patch/archive wrote category/audit/idempotency/request rows.");
    }

    @Test
    void downloadPersistsDownloadRecordAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String idempotencyKey = "idem-download-" + suffix();
        String requestId = requestId("download-create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/resources/res-public-client/versions/ver-client-1/download", null, requestId, Map.of(
                "clientLabel", "launcher",
                "idempotencyKey", idempotencyKey
        ), 200);
        String ticketId = response.at("/data/ticketId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT provider FROM resource_download_records WHERE ticket_id = ?", ticketId, "CLOUDREVE_SHARE");
            assertSingleValue(connection, "SELECT result FROM resource_download_records WHERE ticket_id = ?", ticketId, "SUCCESS");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'RESOURCE_DOWNLOADED' AND target_id = 'res-public-client'", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'anonymous' AND scope = 'resource.download.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/resources/res-public-client/versions/ver-client-1/download'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL resource download wrote resource_download_records/app_audit_logs/app_idempotency_records/app_request_logs and returned 200.");
    }

    @Test
    void downloadIdempotencyReplaysPersistedTicketAndRejectsChangedFingerprint() throws Exception {
        String idempotencyKey = "idem-download-replay-" + suffix();
        Map<String, Object> body = Map.of("clientLabel", "launcher", "idempotencyKey", idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/resources/res-public-client/versions/ver-client-1/download", null, requestId("download-replay-first"), body, 200);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/resources/res-public-client/versions/ver-client-1/download", null, requestId("download-replay-second"), body, 200);
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/resources/res-public-client/versions/ver-client-1/download", null, requestId("download-replay-conflict"), Map.of(
                "clientLabel", "changed",
                "idempotencyKey", idempotencyKey
        ), 409);

        assertThat(second.at("/data/ticketId").asText()).isEqualTo(first.at("/data/ticketId").asText());
        assertThat(conflict.at("/code").asInt()).isEqualTo(43612);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM resource_download_records WHERE ticket_id = ?", first.at("/data/ticketId").asText(), 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'anonymous' AND scope = 'resource.download.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL resource download idempotency replay/conflict used persisted ticket rows.");
    }

    @Test
    void auditQueryAndOpsSummaryExposePersistedResourceEvidence() throws Exception {
        String slug = uniqueSlug("pg-audit");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/resources/admin/items", "admin-token", requestId("audit-item-create"), resourceBody(slug, "idem-" + slug), 201);
        String resourceId = created.at("/data/resourceId").asText();

        JsonNode audits = exchangeGet("/api/v1/resources/admin/items/" + resourceId + "/audit-logs?action=RESOURCE_CREATED", "admin-token", requestId("audit-query"), 200);
        JsonNode ops = exchangeGet("/api/v1/resources/admin/ops/summary", "admin-token", requestId("ops-summary"), 200);

        assertThat(audits.at("/data/items/0/action").asText()).isEqualTo("RESOURCE_CREATED");
        assertThat(ops.at("/data/service").asText()).isEqualTo("resource");
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("POSTGRESQL_WITH_IN_MEMORY_RESPONSE_MODEL");
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ?", resourceId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM resource_items WHERE resource_id = ?", resourceId, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL resource audit query and ops summary observed persisted item/audit state.");
    }

    private JsonNode exchange(HttpMethod method, String path, String token, String requestId, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.set("X-Request-Id", requestId);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private JsonNode exchangeGet(String path, String token, String requestId, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> resourceBody(String slug, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "CLIENT_PACK");
        body.put("visibility", "PUBLIC");
        body.put("slug", slug);
        body.put("title", "Client Pack " + slug);
        body.put("summary", "PostgreSQL resource summary");
        body.put("description", "PostgreSQL resource description");
        body.put("coverUrl", "/assets/resources/" + slug + ".png");
        body.put("categoryId", "cat-client");
        body.put("tags", List.of("p0", "client"));
        body.put("maintainerMemberId", "member-active");
        body.put("visibleFrom", "2026-05-20T00:00:00Z");
        body.put("visibleUntil", "2026-12-31T00:00:00Z");
        body.put("adminNote", "internal note");
        body.put("reason", "resource postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> versionBody(String versionName, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("versionName", versionName);
        body.put("title", "Version " + versionName);
        body.put("changelog", "PostgreSQL version changelog");
        body.put("minecraftVersions", List.of("1.20.1"));
        body.put("loader", "Fabric");
        body.put("fileSizeBytes", 1024);
        body.put("checksumSha256", "a".repeat(64));
        body.put("downloadEntry", downloadEntry("https://cloud.example.com/s/" + versionName));
        body.put("releasedAt", "2026-05-22T00:00:00Z");
        body.put("reason", "version postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> downloadEntry(String url) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("provider", "CLOUDREVE_SHARE");
        entry.put("displayName", "Cloudreve");
        entry.put("shareUrl", url);
        entry.put("status", "ACTIVE");
        entry.put("expiresAt", "2026-12-31T00:00:00Z");
        entry.put("adminNote", "entry note");
        return entry;
    }

    private Map<String, Object> categoryBody(String slug, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Category " + slug);
        body.put("slug", slug);
        body.put("description", "PostgreSQL category");
        body.put("icon", "folder");
        body.put("sortOrder", 20);
        body.put("enabled", true);
        body.put("reason", "category postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
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

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
        }
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
}
