package cn.beiming.admin;

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
        properties = {
                "beiming.admin.test-mode=true",
                "beiming.business-core.test-control-headers.enabled=true"
        }
)
class AdminRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "admin-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:admin_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "admin_flow_request_log",
                    "admin_flow_settings",
                    "admin_flow_layouts",
                    "admin_flow_idempotency",
                    "admin_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void settingsPatchRunsThroughBackendAndDatabaseThenReturnsUpdatedSetting() throws Exception {
        String requestId = "req-settings-" + FLOW_ID;
        String idempotencyKey = "settings-" + UUID.randomUUID().toString().replace("-", "");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/admin/settings",
                bearerHeaders("admin-token", requestId),
                settingsPatchBody(idempotencyKey, 45, List.of("AUTH", "CONTENT", "RESOURCE", "ADMIN"), List.of())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/idempotency/replayed").asBoolean()).isFalse();
        assertThat(json.at("/data/layout/navigationModuleOrder").toString()).contains("AUTH", "CONTENT", "RESOURCE", "ADMIN");
        assertThat(settingValue(json, "dashboard.refreshSeconds")).isEqualTo(45);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT setting_value FROM admin_flow_settings WHERE flow_id = ? AND setting_key = 'dashboard.refreshSeconds' AND action = 'ADMIN_SETTINGS_UPDATED'",
                    FLOW_ID, "45");
            assertSingleValue(connection,
                    "SELECT hidden_modules_count FROM admin_flow_layouts WHERE flow_id = ? AND idempotency_key = ? AND action = 'ADMIN_SETTINGS_UPDATED'",
                    FLOW_ID, idempotencyKey, 0);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM admin_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ADMIN_SETTINGS_UPDATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM admin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/admin/settings'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: admin settings patch reached backend, wrote setting/layout/audit/request rows, and returned 200.");
    }

    @Test
    void settingsPatchIdempotencyReplayRunsThroughBackendAndDatabaseThenReturnsReplayMarker() throws Exception {
        String requestId = "req-idem-" + FLOW_ID;
        String idempotencyKey = "settings-" + UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> body = settingsPatchBody(idempotencyKey, 60, List.of("AUTH", "ADMIN", "CONTENT"), List.of("RESOURCE"));

        TestHttpResponse first = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", bearerHeaders("admin-token", requestId + "-first"), body);
        assertThat(first.statusCode()).isEqualTo(200);

        TestHttpResponse replay = exchange(HttpMethod.PATCH, "/api/v1/admin/settings", bearerHeaders("admin-token", requestId), body);

        assertThat(replay.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(replay.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/idempotency/replayed").asBoolean()).isTrue();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT replayed FROM admin_flow_idempotency WHERE flow_id = ? AND request_id = ? AND idempotency_key = ?",
                    FLOW_ID, requestId, idempotencyKey, true);
            assertSingleValue(connection,
                    "SELECT hidden_modules_count FROM admin_flow_layouts WHERE flow_id = ? AND idempotency_key = ? AND action = 'ADMIN_SETTINGS_REPLAYED'",
                    FLOW_ID, idempotencyKey, 1);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM admin_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ADMIN_SETTINGS_REPLAYED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM admin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/admin/settings'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: admin settings idempotency replay reached backend, wrote idempotency/layout/audit/request rows, and returned 200.");
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

    private Map<String, Object> settingsPatchBody(String idempotencyKey, int refreshSeconds, List<String> order, List<String> hidden) {
        Map<String, Object> layout = new java.util.LinkedHashMap<>();
        layout.put("navigationModuleOrder", order);
        layout.put("hiddenModules", hidden);
        layout.put("dashboardCards", List.of("todos", "metrics", "health"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "dashboard.refreshSeconds", "value", refreshSeconds)));
        body.put("reason", "admin sql evidence");
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

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS admin_flow_settings (
                    flow_id VARCHAR(128) NOT NULL,
                    setting_key VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    setting_value VARCHAR(128) NOT NULL,
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS admin_flow_layouts (
                    flow_id VARCHAR(128) NOT NULL,
                    idempotency_key VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    navigation_modules_count INT NOT NULL,
                    hidden_modules_count INT NOT NULL,
                    quick_actions_count INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS admin_flow_idempotency (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    idempotency_key VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    replayed BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS admin_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS admin_flow_request_log (
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
        AdminFlowEvidenceRecorder adminFlowEvidenceRecorder() {
            return new JdbcAdminFlowEvidenceRecorder();
        }
    }

    static class JdbcAdminFlowEvidenceRecorder implements AdminFlowEvidenceRecorder {
        @Override
        public void recordSettingsWrite(HttpServletRequest request, String action, AuthUser actor, Map<String, Object> requestBody, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String idempotencyKey = String.valueOf(requestBody.get("idempotencyKey"));
            boolean replayed = payload.get("idempotency") instanceof Map<?, ?> idempotency && Boolean.TRUE.equals(idempotency.get("replayed"));
            try (Connection connection = openConnection()) {
                for (Map<String, Object> setting : changedSettings(payload, requestBody)) {
                    insert(connection,
                            "INSERT INTO admin_flow_settings(flow_id, setting_key, action, setting_value, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                            flowId, setting.get("key"), action, String.valueOf(setting.get("value")), actor.userId(), Timestamp.from(Instant.now()));
                }
                Map<String, Object> layout = layout(payload);
                insert(connection,
                        "INSERT INTO admin_flow_layouts(flow_id, idempotency_key, action, navigation_modules_count, hidden_modules_count, quick_actions_count, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, idempotencyKey, action, size(layout.get("navigationModuleOrder")), size(layout.get("hiddenModules")), size(layout.get("quickActions")), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO admin_flow_idempotency(flow_id, request_id, idempotency_key, action, replayed, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, requestId, idempotencyKey, action, replayed, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, "settings", request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write admin database evidence", exception);
            }
        }

        @SuppressWarnings("unchecked")
        private static List<Map<String, Object>> changedSettings(Map<String, Object> payload, Map<String, Object> requestBody) {
            Object items = requestBody.get("items");
            if (!(items instanceof List<?> requested)) {
                return List.of();
            }
            List<String> changedKeys = requested.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> String.valueOf(((Map<?, ?>) item).get("key")))
                    .toList();
            return ((List<Map<String, Object>>) payload.get("items")).stream()
                    .filter(item -> changedKeys.contains(String.valueOf(item.get("key"))))
                    .toList();
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> layout(Map<String, Object> payload) {
            return (Map<String, Object>) payload.get("layout");
        }

        private static int size(Object value) {
            return value instanceof List<?> list ? list.size() : 0;
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO admin_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO admin_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
