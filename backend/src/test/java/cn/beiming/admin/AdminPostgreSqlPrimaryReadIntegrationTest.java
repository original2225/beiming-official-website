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
class AdminPostgreSqlPrimaryReadIntegrationTest {
    private static final String FLOW_ID = "admin-primary-read-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_admin_primary_read")
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
    void settingsReadsCurrentPostgreSqlRowsAndSeedDoesNotOverwritePatchValues() throws Exception {
        String idempotencyKey = "primary-settings-" + suffix();
        exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId("settings-patch"),
                settingsPatchBody(idempotencyKey, 66, List.of("ADMIN", "AUTH", "RESOURCE"), List.of("GUIDE")), 200);

        store.seed();

        JsonNode settings = exchangeGet("/api/v1/admin/settings", "admin-token", requestId("settings-read"), 200);

        assertThat(settingValue(settings, "dashboard.refreshSeconds")).isEqualTo(66);
        assertThat(settings.at("/data/layout/navigationModuleOrder").toString()).contains("ADMIN", "AUTH", "RESOURCE");
        assertThat(settings.at("/data/layout/hiddenModules").toString()).contains("GUIDE");
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT setting_value FROM admin_settings WHERE setting_key = 'dashboard.refreshSeconds'", "66");
            assertSingleValue(connection, "SELECT COUNT(*) FROM admin_layouts WHERE layout_key = 'default' AND jsonb_exists(hidden_modules, 'GUIDE')", 1L);
        }
        System.out.println("SQL evidence: PostgreSQL admin settings read used admin_settings/admin_layouts after seed without overwriting patched values.");
    }

    @Test
    void auditModulesTodosMetricsOverviewAndOpsReadFromPostgreSqlIndexes() throws Exception {
        String requestId = requestId("index-patch");
        exchange(HttpMethod.PATCH, "/api/v1/admin/settings", "admin-token", requestId,
                settingsPatchBody("primary-index-" + suffix(), 63, List.of("AUTH", "CONTENT", "ADMIN"), List.of()), 200);
        try (Connection connection = openConnection()) {
            updateSingleValue(connection, "UPDATE admin_module_indexes SET badge_count = 88 WHERE module_key = 'CONTENT'");
            updateSingleValue(connection, "UPDATE admin_todo_indexes SET title = 'PostgreSQL primary todo title' WHERE todo_id = 'todo-content-review-1'");
            updateSingleValue(connection, "UPDATE admin_metric_snapshots SET metric_value = 123 WHERE metric_key = 'content.pendingReview'");
        }

        JsonNode audits = exchangeGet("/api/v1/admin/audit-logs?action=ADMIN_SETTINGS_UPDATED", "admin-token", requestId("audit-read"), 200);
        JsonNode modules = exchangeGet("/api/v1/admin/modules?includeDisabled=true", "owner-token", requestId("modules-read"), 200);
        JsonNode todos = exchangeGet("/api/v1/admin/todos?pageSize=100&sourceModule=CONTENT", "admin-token", requestId("todos-read"), 200);
        JsonNode metrics = exchangeGet("/api/v1/admin/metrics/summary?sourceModule=CONTENT", "admin-token", requestId("metrics-read"), 200);
        JsonNode overview = exchangeGet("/api/v1/admin/overview?moduleLimit=50&todoLimit=20&auditLimit=20", "owner-token", requestId("overview-read"), 200);
        JsonNode ops = exchangeGet("/api/v1/admin/ops/summary", "admin-token", requestId("ops-read"), 200);

        assertThat(audits.at("/data/items/0/action").asText()).isEqualTo("ADMIN_SETTINGS_UPDATED");
        assertThat(moduleField(modules, "CONTENT", "badgeCount")).isEqualTo(88);
        assertThat(todos.toString()).contains("PostgreSQL primary todo title");
        assertThat(metricValue(metrics, "content.pendingReview")).isEqualTo(123);
        assertThat(moduleField(overview, "CONTENT", "badgeCount")).isEqualTo(88);
        assertThat(overview.toString()).contains("PostgreSQL primary todo title", "ADMIN_SETTINGS_UPDATED");
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("POSTGRESQL_PRIMARY");
        assertThat(ops.at("/data/postgresTablesReady/admin_module_indexes").asBoolean()).isTrue();
        assertThat(ops.at("/data/postgresCounts/moduleIndexesTotal").asLong()).isEqualTo(modules.at("/data/items").size());
        assertThat(ops.at("/data/postgresCounts/todoIndexesTotal").asLong()).isEqualTo(sqlCount("admin_todo_indexes"));
        assertThat(ops.at("/data/postgresCounts/metricSnapshotsTotal").asLong()).isEqualTo(sqlCount("admin_metric_snapshots"));
        assertThat(ops.at("/data/postgresCounts/auditIndexesTotal").asLong()).isEqualTo(sqlCount("admin_audit_indexes"));
        System.out.println("SQL evidence: PostgreSQL admin primary reads used module/todo/metric/audit indexes and ops summary returned PostgreSQL counts.");
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

    private Map<String, Object> settingsPatchBody(String idempotencyKey, int refreshSeconds, List<String> order, List<String> hidden) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("navigationModuleOrder", order);
        layout.put("hiddenModules", hidden);
        layout.put("dashboardCards", List.of("todos", "metrics", "health"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "dashboard.refreshSeconds", "value", refreshSeconds)));
        body.put("reason", "postgres primary read evidence");
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

    private int moduleField(JsonNode json, String moduleKey, String field) {
        JsonNode modules = json.at("/data/items").isArray() ? json.at("/data/items") : json.at("/data/modules");
        for (JsonNode item : modules) {
            if (moduleKey.equals(item.at("/moduleKey").asText())) {
                return item.at("/" + field).asInt();
            }
        }
        throw new AssertionError("missing module " + moduleKey);
    }

    private int metricValue(JsonNode json, String metricKey) {
        for (JsonNode item : json.at("/data/items")) {
            if (metricKey.equals(item.at("/metricKey").asText())) {
                return item.at("/value").asInt();
            }
        }
        throw new AssertionError("missing metric " + metricKey);
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

    private static long sqlCount(String table) throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private static void updateSingleValue(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            assertThat(statement.executeUpdate()).isEqualTo(1);
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
}
