package cn.beiming.serverstatus;

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
class ServerStatusRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "server-status-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:server_status_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ServerStatusStore store;

    @Autowired
    TestAuthContextProvider auth;

    @Autowired
    TestStatusCollector collector;

    @BeforeEach
    void setUp() throws Exception {
        auth.reset();
        collector.reset();
        store.reset();
        store.seedTestData();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "server_status_flow_request_log",
                    "server_status_flow_sources",
                    "server_status_flow_snapshots",
                    "server_status_flow_lines",
                    "server_status_flow_outages",
                    "server_status_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void sourceCreateRunsThroughBackendAndDatabaseThenReturnsCreatedSource() throws Exception {
        String requestId = "req-source-" + FLOW_ID;
        String instanceName = "Flow Source " + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/server-status/admin/sources",
                bearerHeaders("admin-token", requestId),
                sourceBody(instanceName)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/instanceName").asText()).isEqualTo(instanceName);
        assertThat(json.at("/data/configStatus").asText()).isEqualTo("ENABLED");
        String sourceId = json.at("/data/sourceId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT instance_name FROM server_status_flow_sources WHERE flow_id = ? AND source_id = ? AND action = 'SERVER_STATUS_SOURCE_CREATED'",
                    FLOW_ID, sourceId, instanceName);
            assertSingleValue(connection,
                    "SELECT config_status FROM server_status_flow_sources WHERE flow_id = ? AND source_id = ? AND action = 'SERVER_STATUS_SOURCE_CREATED'",
                    FLOW_ID, sourceId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM server_status_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'SERVER_STATUS_SOURCE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM server_status_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/server-status/admin/sources'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: server-status source create reached backend, wrote source/audit/request rows, and returned 201.");
    }

    @Test
    void sourceRefreshRunsThroughBackendAndDatabaseThenReturnsSnapshot() throws Exception {
        String requestId = "req-refresh-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/server-status/admin/sources/src-survival/refresh",
                bearerHeaders("admin-token", requestId),
                Map.of("reason", "refresh sql evidence", "idempotencyKey", "flow-refresh-key")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/source").asText()).isEqualTo("MANUAL_REFRESH");
        assertThat(json.at("/data/status").asText()).isEqualTo("ONLINE");
        String snapshotId = json.at("/data/snapshotId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT source FROM server_status_flow_snapshots WHERE flow_id = ? AND snapshot_id = ? AND action = 'SERVER_STATUS_SOURCE_REFRESHED'",
                    FLOW_ID, snapshotId, "MANUAL_REFRESH");
            assertSingleValue(connection,
                    "SELECT online_players FROM server_status_flow_snapshots WHERE flow_id = ? AND snapshot_id = ? AND action = 'SERVER_STATUS_SOURCE_REFRESHED'",
                    FLOW_ID, snapshotId, 36);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM server_status_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'SERVER_STATUS_SOURCE_REFRESHED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM server_status_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/server-status/admin/sources/src-survival/refresh'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: server-status source refresh reached backend, wrote snapshot/audit/request rows, and returned 200.");
    }

    @Test
    void lineCreateRunsThroughBackendAndDatabaseThenReturnsCreatedLine() throws Exception {
        String requestId = "req-line-" + FLOW_ID;
        String entryAddress = "flow-line-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6) + ".example.com";

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/server-status/admin/lines",
                bearerHeaders("admin-token", requestId),
                lineBody(entryAddress)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/entryAddress").asText()).isEqualTo(entryAddress);
        assertThat(json.at("/data/configStatus").asText()).isEqualTo("ENABLED");
        String lineId = json.at("/data/lineId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT entry_address FROM server_status_flow_lines WHERE flow_id = ? AND line_id = ? AND action = 'SERVER_STATUS_LINE_CREATED'",
                    FLOW_ID, lineId, entryAddress);
            assertSingleValue(connection,
                    "SELECT config_status FROM server_status_flow_lines WHERE flow_id = ? AND line_id = ? AND action = 'SERVER_STATUS_LINE_CREATED'",
                    FLOW_ID, lineId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM server_status_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'SERVER_STATUS_LINE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM server_status_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/server-status/admin/lines'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: server-status line create reached backend, wrote line/audit/request rows, and returned 201.");
    }

    @Test
    void outageCreateRunsThroughBackendAndDatabaseThenReturnsCreatedOutage() throws Exception {
        String requestId = "req-outage-" + FLOW_ID;
        String title = "Flow Outage " + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/server-status/admin/outages",
                bearerHeaders("admin-token", requestId),
                outageBody(title)
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/title").asText()).isEqualTo(title);
        assertThat(json.at("/data/status").asText()).isEqualTo("OPEN");
        String outageId = json.at("/data/outageId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT title FROM server_status_flow_outages WHERE flow_id = ? AND outage_id = ? AND action = 'SERVER_STATUS_OUTAGE_CREATED'",
                    FLOW_ID, outageId, title);
            assertSingleValue(connection,
                    "SELECT status FROM server_status_flow_outages WHERE flow_id = ? AND outage_id = ? AND action = 'SERVER_STATUS_OUTAGE_CREATED'",
                    FLOW_ID, outageId, "OPEN");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM server_status_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'SERVER_STATUS_OUTAGE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM server_status_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/server-status/admin/outages'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: server-status outage create reached backend, wrote outage/audit/request rows, and returned 201.");
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

    private Map<String, Object> sourceBody(String instanceName) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("instanceName", instanceName);
        body.put("instanceKind", "SURVIVAL");
        body.put("sourceType", "STUB");
        body.put("target", instanceName.toLowerCase().replace(" ", "-") + ".example.com");
        body.put("publicVisible", true);
        body.put("primary", false);
        body.put("timeoutMs", 3000);
        body.put("sortOrder", 30);
        body.put("startedAt", "2026-05-20T00:00:00Z");
        body.put("adminNote", "internal source note");
        body.put("reason", "source sql evidence");
        return body;
    }

    private Map<String, Object> lineBody(String entryAddress) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", "Line " + entryAddress);
        body.put("entryAddress", entryAddress);
        body.put("checkTarget", "https://" + entryAddress + "/health");
        body.put("description", "Public line");
        body.put("publicVisible", true);
        body.put("primary", false);
        body.put("sortOrder", 50);
        body.put("adminNote", "internal line note");
        body.put("reason", "line sql evidence");
        return body;
    }

    private Map<String, Object> outageBody(String title) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("publicMessage", "Maintenance public message");
        body.put("severity", "HIGH");
        body.put("instanceId", "inst-survival");
        body.put("lineId", "line-main");
        body.put("startedAt", "2026-05-22T01:00:00Z");
        body.put("internalReason", "maintenance internal reason");
        body.put("adminNote", "internal outage note");
        body.put("publicVisible", true);
        body.put("reason", "outage sql evidence");
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
                CREATE TABLE IF NOT EXISTS server_status_flow_sources (
                    flow_id VARCHAR(128) NOT NULL,
                    source_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    instance_name VARCHAR(128) NOT NULL,
                    config_status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS server_status_flow_snapshots (
                    flow_id VARCHAR(128) NOT NULL,
                    snapshot_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    source VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    online_players INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS server_status_flow_lines (
                    flow_id VARCHAR(128) NOT NULL,
                    line_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    entry_address VARCHAR(255) NOT NULL,
                    config_status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS server_status_flow_outages (
                    flow_id VARCHAR(128) NOT NULL,
                    outage_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS server_status_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS server_status_flow_request_log (
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
        ServerStatusFlowEvidenceRecorder serverStatusFlowEvidenceRecorder() {
            return new JdbcServerStatusFlowEvidenceRecorder();
        }
    }

    static class JdbcServerStatusFlowEvidenceRecorder implements ServerStatusFlowEvidenceRecorder {
        @Override
        public void recordSourceWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String sourceId = String.valueOf(payload.get("sourceId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO server_status_flow_sources(flow_id, source_id, action, instance_name, config_status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, sourceId, action, payload.get("instanceName"), payload.get("configStatus"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, sourceId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write server-status source database evidence", exception);
            }
        }

        @Override
        public void recordSnapshotWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String snapshotId = String.valueOf(payload.get("snapshotId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO server_status_flow_snapshots(flow_id, snapshot_id, action, source, status, online_players, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, snapshotId, action, payload.get("source"), payload.get("status"), payload.get("onlinePlayers"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, String.valueOf(payload.get("sourceId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write server-status snapshot database evidence", exception);
            }
        }

        @Override
        public void recordLineWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String lineId = String.valueOf(payload.get("lineId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO server_status_flow_lines(flow_id, line_id, action, entry_address, config_status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, lineId, action, payload.get("entryAddress"), payload.get("configStatus"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, lineId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write server-status line database evidence", exception);
            }
        }

        @Override
        public void recordOutageWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String outageId = String.valueOf(payload.get("outageId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO server_status_flow_outages(flow_id, outage_id, action, title, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, outageId, action, payload.get("title"), payload.get("status"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, outageId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write server-status outage database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO server_status_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO server_status_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
