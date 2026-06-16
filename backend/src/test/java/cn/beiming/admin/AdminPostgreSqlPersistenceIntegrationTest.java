package cn.beiming.admin;

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
                "beiming.admin.test-mode=true",
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true"
        }
)
@Testcontainers
class AdminPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "admin-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_admin_flow")
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
    AdminStore store;

    @BeforeEach
    void setUp() {
        store.seed();
    }

    @Test
    void settingsPatchPersistsSettingsLayoutAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String idempotencyKey = "idem-settings-" + suffix();
        String requestId = requestId("settings-success");
        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId,
                settingsPatchBody(idempotencyKey, "postgres settings evidence", 54, List.of("AUTH", "CONTENT", "RESOURCE", "ADMIN"), List.of("OPS_CONTROL")), 200);

        assertThat(response.at("/data/idempotency/replayed").asBoolean()).isFalse();
        assertThat(settingValue(response, "dashboard.refreshSeconds")).isEqualTo(54);
        assertThat(response.at("/data/layout/hiddenModules").toString()).contains("OPS_CONTROL");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT setting_value FROM admin_settings WHERE setting_key = 'dashboard.refreshSeconds'", "54");
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_layouts WHERE layout_key = 'default' AND jsonb_exists(hidden_modules, 'OPS_CONTROL')", 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_audit_indexes WHERE request_id = ? AND action = 'ADMIN_SETTINGS_UPDATED'", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ADMIN_SETTINGS_UPDATED' AND target_id = 'settings'", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'admin.settings.patch' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/admin/settings'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL admin settings patch wrote admin_settings/admin_layouts/admin_audit_indexes/app_audit_logs/app_idempotency_records/app_request_logs and returned 200.");
    }

    @Test
    void settingsPatchIdempotencyReplaysOriginalResponseAndDoesNotDuplicateBusinessRows() throws Exception {
        String idempotencyKey = "idem-replay-" + suffix();
        Map<String, Object> body = settingsPatchBody(idempotencyKey, "postgres replay evidence", 61, List.of("AUTH", "ADMIN", "CONTENT"), List.of("RESOURCE"));

        JsonNode first = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("settings-replay-first"), body, 200);
        JsonNode second = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("settings-replay-second"), body, 200);

        assertThat(second.at("/data/idempotency/replayed").asBoolean()).isTrue();
        assertThat(settingValue(second, "dashboard.refreshSeconds")).isEqualTo(settingValue(first, "dashboard.refreshSeconds"));
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_setting_change_records WHERE idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_layouts WHERE layout_key = 'default' AND jsonb_exists(hidden_modules, 'RESOURCE')", 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'admin.settings.patch' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL admin settings idempotency replay kept one business change row and one idempotency row.");
    }

    @Test
    void settingsPatchIdempotencyConflictReturns43712AndDoesNotPersistChangedBusinessState() throws Exception {
        String idempotencyKey = "idem-conflict-" + suffix();
        exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("settings-conflict-first"),
                settingsPatchBody(idempotencyKey, "first conflict evidence", 48, List.of("AUTH", "ADMIN"), List.of()), 200);

        JsonNode conflict = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("settings-conflict-second"),
                settingsPatchBody(idempotencyKey, "changed conflict evidence", 49, List.of("AUTH", "CONTENT"), List.of("RESOURCE")), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43712);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT setting_value FROM admin_settings WHERE setting_key = 'dashboard.refreshSeconds'", "48");
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_setting_change_records WHERE idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_layouts WHERE layout_key = 'default' AND jsonb_exists(hidden_modules, 'RESOURCE')", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL admin settings idempotency conflict preserved original business rows and returned 409/43712.");
    }

    @Test
    void adminCannotPatchHighImpactSettingButOwnerCanPersistItInPostgreSql() throws Exception {
        String adminKey = "idem-high-admin-" + suffix();
        JsonNode forbidden = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("high-impact-admin"),
                highImpactBody(adminKey, 180), 403);

        assertThat(forbidden.at("/code").asInt()).isEqualTo(42001);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_setting_change_records WHERE idempotency_key = ?", adminKey, 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_settings WHERE setting_key = 'audit.retentionDays' AND setting_value = '180'", 0L);
        }

        String ownerKey = "idem-high-owner-" + suffix();
        exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "owner-token", requestId("high-impact-owner"), highImpactBody(ownerKey, 180), 200);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT setting_value FROM admin_settings WHERE setting_key = 'audit.retentionDays'", "180");
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_setting_change_records WHERE idempotency_key = ?", ownerKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("high-impact-owner"), 1L);
        }
        System.out.println("SQL evidence: PostgreSQL admin high-impact settings reject ADMIN writes and persist OWNER writes.");
    }

    @Test
    void auditSettingsAndOpsReadsExposePostgreSqlAdminEvidence() throws Exception {
        String idempotencyKey = "idem-read-" + suffix();
        String requestId = requestId("read-setup");
        exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId,
                settingsPatchBody(idempotencyKey, "postgres read evidence", 57, List.of("AUTH", "ADMIN", "RESOURCE"), List.of()), 200);

        JsonNode audits = exchangeGet("/api/v1/admin/audit-logs?action=ADMIN_SETTINGS_UPDATED", "admin-token", requestId("audit-read"), 200);
        JsonNode settings = exchangeGet("/api/v1/admin/settings", "admin-token", requestId("settings-read"), 200);
        JsonNode ops = exchangeGet("/api/v1/admin/ops/summary", "admin-token", requestId("ops-read"), 200);

        assertThat(audits.at("/data/items/0/action").asText()).isEqualTo("ADMIN_SETTINGS_UPDATED");
        assertThat(settingValue(settings, "dashboard.refreshSeconds")).isEqualTo(57);
        assertThat(settings.at("/data/layout/navigationModuleOrder").toString()).contains("AUTH", "RESOURCE");
        assertThat(ops.at("/data/service").asText()).isEqualTo("admin");
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("POSTGRESQL_WITH_IN_MEMORY_RESPONSE_MODEL");
        assertThat(ops.at("/data/postgresTablesReady/admin_settings").asBoolean()).isTrue();
        assertThat(ops.at("/data/postgresCounts/settingsTotal").asLong()).isGreaterThanOrEqualTo(3L);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_audit_indexes WHERE request_id = ?", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ?", requestId, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL admin audit/settings/ops reads observed persisted settings and audit index evidence.");
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

    private Map<String, Object> settingsPatchBody(String idempotencyKey, String reason, int refreshSeconds, List<String> order, List<String> hidden) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("navigationModuleOrder", order);
        layout.put("hiddenModules", hidden);
        layout.put("dashboardCards", List.of("todos", "metrics", "health"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "dashboard.refreshSeconds", "value", refreshSeconds)));
        body.put("reason", reason);
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> highImpactBody(String idempotencyKey, int retentionDays) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("hiddenModules", List.of("AUTH"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "audit.retentionDays", "value", retentionDays)));
        body.put("reason", "postgres high impact evidence");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private int settingValue(JsonNode json, String key) {
        for (JsonNode item : json.at("/data/items")) {
            if (key.equals(item.at("/key").asText())) {
                return item.at("/value").asInt();
            }
        }
        throw new AssertionError("missing setting " + key);
    }

    private String requestId(String name) {
        return "req-" + FLOW_ID + "-" + name;
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
}
