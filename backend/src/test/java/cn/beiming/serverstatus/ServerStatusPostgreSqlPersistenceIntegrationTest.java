package cn.beiming.serverstatus;

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
class ServerStatusPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "server-status-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_server_status_flow")
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
    ServerStatusStore store;

    @Autowired
    TestAuthContextProvider auth;

    @Autowired
    TestStatusCollector collector;

    @BeforeEach
    void setUp() {
        auth.reset();
        collector.reset();
        store.reset();
        store.seedTestData();
    }

    @Test
    void sourceCreatePersistsSourceAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String sourceName = unique("Pg Source");
        String idempotencyKey = "idem-" + suffix();
        String requestId = requestId("source-create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId, sourceBody(sourceName, idempotencyKey), 201);
        String sourceId = response.at("/data/sourceId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT instance_name FROM server_status_sources WHERE source_id = ?", sourceId, sourceName);
            assertSingleValue(connection, "SELECT config_status FROM server_status_sources WHERE source_id = ?", sourceId, "ENABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'SERVER_STATUS_SOURCE_CREATED' AND target_id = ?", requestId, sourceId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'server-status.source.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/server-status/admin/sources'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source create wrote server_status_sources/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void sourceCreateIdempotencyReplaysPersistedResultInPostgreSql() throws Exception {
        String sourceName = unique("Pg Replay Source");
        String idempotencyKey = "idem-" + suffix();
        Map<String, Object> body = sourceBody(sourceName, idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("source-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("source-replay-second"), body, 201);

        assertThat(second.at("/data/sourceId").asText()).isEqualTo(first.at("/data/sourceId").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM server_status_sources WHERE instance_name = ?", sourceName, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'server-status.source.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source create idempotency replay kept one source row and one idempotency row.");
    }

    @Test
    void sourceCreateIdempotencyRejectsSameKeyWithDifferentFingerprint() throws Exception {
        String sourceName = unique("Pg Conflict Source");
        String idempotencyKey = "idem-" + suffix();
        exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("source-conflict-first"), sourceBody(sourceName, idempotencyKey), 201);

        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("source-conflict-second"), sourceBody(sourceName + " Changed", idempotencyKey), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'server-status.source.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM server_status_sources WHERE instance_name = ?", sourceName + " Changed", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source create idempotency conflict preserved original rows and returned 409.");
    }

    @Test
    void sourceRefreshPersistsSnapshotAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String idempotencyKey = "idem-refresh-" + suffix();
        String requestId = requestId("source-refresh");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources/src-survival/refresh", "admin-token", requestId, Map.of(
                "reason", "refresh postgres evidence",
                "idempotencyKey", idempotencyKey
        ), 200);
        String snapshotId = response.at("/data/snapshotId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT source FROM server_status_snapshots WHERE snapshot_id = ?", snapshotId, "MANUAL_REFRESH");
            assertSingleValue(connection, "SELECT online_players FROM server_status_snapshots WHERE snapshot_id = ?", snapshotId, 36);
            assertSingleValue(connection, "SELECT COUNT(*) FROM server_status_refresh_records WHERE source_id = 'src-survival' AND snapshot_id = ?", snapshotId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'SERVER_STATUS_SOURCE_REFRESHED' AND target_id = 'src-survival'", requestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'server-status.source.refresh' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/server-status/admin/sources/src-survival/refresh'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source refresh wrote snapshot/refresh/audit/idempotency/request rows and returned 200.");
    }

    @Test
    void sourceRefreshIdempotencyReplaysPersistedResultAndRejectsChangedFingerprintInPostgreSql() throws Exception {
        String idempotencyKey = "idem-refresh-" + suffix();
        Map<String, Object> body = Map.of("reason", "refresh replay", "idempotencyKey", idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources/src-survival/refresh", "admin-token", requestId("refresh-replay-first"), body, 200);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources/src-survival/refresh", "admin-token", requestId("refresh-replay-second"), body, 200);
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources/src-survival/refresh", "admin-token", requestId("refresh-conflict"), Map.of("reason", "refresh changed", "idempotencyKey", idempotencyKey), 409);

        assertThat(second.at("/data/snapshotId").asText()).isEqualTo(first.at("/data/snapshotId").asText());
        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM server_status_refresh_records WHERE source_id = 'src-survival' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'server-status.source.refresh' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source refresh idempotency replay/conflict used persisted rows.");
    }

    @Test
    void sourcePatchDisableAndEnablePersistSourceAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("source-lifecycle-create"), sourceBody(unique("Pg Lifecycle Source"), "idem-" + suffix()), 201);
        String sourceId = created.at("/data/sourceId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/sources/" + sourceId, "admin-token", requestId("source-patch"), Map.of(
                "instanceName", "PostgreSQL Patched Source",
                "sortOrder", 7,
                "reason", "patch source"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/sources/" + sourceId + "/disable", "admin-token", requestId("source-disable"), Map.of("reason", "disable source"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/sources/" + sourceId + "/enable", "admin-token", requestId("source-enable"), Map.of("reason", "enable source"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT instance_name FROM server_status_sources WHERE source_id = ?", sourceId, "PostgreSQL Patched Source");
            assertSingleValue(connection, "SELECT config_status FROM server_status_sources WHERE source_id = ?", sourceId, "ENABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('SERVER_STATUS_SOURCE_UPDATED', 'SERVER_STATUS_SOURCE_DISABLED', 'SERVER_STATUS_SOURCE_ENABLED')", sourceId, 3L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE path LIKE '/api/v1/server-status/admin/sources/%' AND response_code = 200", 3L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status source patch/disable/enable updated source row and wrote audit/request rows.");
    }

    @Test
    void lineCreatePatchDisableAndEnablePersistLineAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String entryAddress = "pg-line-" + suffix() + ".example.com";
        String idempotencyKey = "idem-line-" + suffix();
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/server-status/admin/lines", "admin-token", requestId("line-create"), lineBody(entryAddress, idempotencyKey), 201);
        String lineId = created.at("/data/lineId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/lines/" + lineId, "admin-token", requestId("line-patch"), Map.of(
                "description", "PostgreSQL patched line",
                "publicVisible", false,
                "reason", "patch line"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/lines/" + lineId + "/disable", "admin-token", requestId("line-disable"), Map.of("reason", "disable line"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/lines/" + lineId + "/enable", "admin-token", requestId("line-enable"), Map.of("reason", "enable line"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT description FROM server_status_lines WHERE line_id = ?", lineId, "PostgreSQL patched line");
            assertSingleValue(connection, "SELECT config_status FROM server_status_lines WHERE line_id = ?", lineId, "ENABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('SERVER_STATUS_LINE_CREATED', 'SERVER_STATUS_LINE_UPDATED', 'SERVER_STATUS_LINE_DISABLED', 'SERVER_STATUS_LINE_ENABLED')", lineId, 4L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE scope = 'server-status.line.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE path LIKE '/api/v1/server-status/admin/lines%' AND response_code IN (200, 201)", 4L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status line create/patch/disable/enable wrote line/audit/idempotency/request rows.");
    }

    @Test
    void outageCreatePatchAcknowledgeResolveAndArchivePersistOutageAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String title = unique("Pg Outage");
        String idempotencyKey = "idem-outage-" + suffix();
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/server-status/admin/outages", "admin-token", requestId("outage-create"), outageBody(title, idempotencyKey), 201);
        String outageId = created.at("/data/outageId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/outages/" + outageId, "admin-token", requestId("outage-patch"), Map.of(
                "publicMessage", "PostgreSQL patched outage",
                "internalReason", "PostgreSQL internal",
                "reason", "patch outage"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/outages/" + outageId + "/acknowledge", "admin-token", requestId("outage-ack"), Map.of("reason", "ack outage"), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/outages/" + outageId + "/resolve", "admin-token", requestId("outage-resolve"), Map.of(
                "resolvedAt", "2026-05-22T02:00:00Z",
                "publicMessage", "Recovered",
                "reason", "resolve outage"
        ), 200);
        exchange(HttpMethod.PATCH, "/api/v1/server-status/admin/outages/" + outageId + "/archive", "admin-token", requestId("outage-archive"), Map.of("reason", "archive outage"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM server_status_outages WHERE outage_id = ?", outageId, "ARCHIVED");
            assertSingleValue(connection, "SELECT public_message FROM server_status_outages WHERE outage_id = ?", outageId, "Recovered");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action IN ('SERVER_STATUS_OUTAGE_CREATED', 'SERVER_STATUS_OUTAGE_UPDATED', 'SERVER_STATUS_OUTAGE_ACKNOWLEDGED', 'SERVER_STATUS_OUTAGE_RESOLVED', 'SERVER_STATUS_OUTAGE_ARCHIVED')", outageId, 5L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE scope = 'server-status.outage.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE path LIKE '/api/v1/server-status/admin/outages%' AND response_code IN (200, 201)", 5L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status outage lifecycle wrote outage/audit/idempotency/request rows.");
    }

    @Test
    void auditQueryAndOpsSummaryExposePersistedServerStatusEvidence() throws Exception {
        String sourceName = unique("Pg Audit Source");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/server-status/admin/sources", "admin-token", requestId("audit-source-create"), sourceBody(sourceName, "idem-" + suffix()), 201);
        String sourceId = created.at("/data/sourceId").asText();

        JsonNode audits = exchangeGet("/api/v1/server-status/admin/audit-logs?targetType=SOURCE&targetId=" + sourceId, "admin-token", requestId("audit-query"), 200);
        JsonNode ops = exchangeGet("/api/v1/server-status/admin/ops/summary", "admin-token", requestId("ops-summary"), 200);

        assertThat(audits.at("/data/items/0/action").asText()).isEqualTo("SERVER_STATUS_SOURCE_CREATED");
        assertThat(ops.at("/data/service").asText()).isEqualTo("server-status");
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ?", sourceId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM server_status_sources", 6L);
        }
        System.out.println("SQL evidence: PostgreSQL server-status audit query and ops summary observed persisted source/audit state.");
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

    private Map<String, Object> sourceBody(String instanceName, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("instanceName", instanceName);
        body.put("instanceKind", "SURVIVAL");
        body.put("sourceType", "STUB");
        body.put("target", slug(instanceName) + ".example.com");
        body.put("publicVisible", true);
        body.put("primary", false);
        body.put("timeoutMs", 3000);
        body.put("sortOrder", 30);
        body.put("startedAt", "2026-05-20T00:00:00Z");
        body.put("adminNote", "internal source note");
        body.put("reason", "source postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> lineBody(String entryAddress, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", "Line " + entryAddress);
        body.put("entryAddress", entryAddress);
        body.put("checkTarget", "https://" + entryAddress + "/health");
        body.put("description", "Public line");
        body.put("publicVisible", true);
        body.put("primary", false);
        body.put("sortOrder", 50);
        body.put("adminNote", "internal line note");
        body.put("reason", "line postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> outageBody(String title, String idempotencyKey) {
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
        body.put("reason", "outage postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private String requestId(String name) {
        return "req-" + FLOW_ID + "-" + name;
    }

    private String unique(String prefix) {
        return prefix + " " + suffix();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String slug(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
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
