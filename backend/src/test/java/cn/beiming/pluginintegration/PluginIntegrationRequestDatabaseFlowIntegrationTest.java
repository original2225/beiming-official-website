package cn.beiming.pluginintegration;

import cn.beiming.opscore.OpsCoreServiceApplication;
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
import org.springframework.test.annotation.DirtiesContext;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpsCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "plugin-integration.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PluginIntegrationRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "plugin-integration-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:plugin_integration_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
            for (String table : List.of(
                    "plugin_flow_request_log",
                    "plugin_flow_providers",
                    "plugin_flow_schemas",
                    "plugin_flow_events",
                    "plugin_flow_routes",
                    "plugin_flow_tasks",
                    "plugin_flow_mappings",
                    "plugin_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createProviderRunsThroughBackendAndDatabaseThenReturnsDraftProvider() throws Exception {
        String requestId = "req-plugin-provider-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/plugin-integration/admin/providers",
                bearerHeaders("plugin-admin-token", requestId),
                with(providerBody("provider-" + randomKey()), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT")
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("plugin-admin-user");
        String providerId = json.at("/data/providerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'PLUGIN_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM plugin_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'PLUGIN_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "plugin-admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM plugin_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'PLUGIN_PROVIDER_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM plugin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/plugin-integration/admin/providers'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: plugin-integration provider create reached backend, wrote provider/audit/request rows, and returned 201.");
    }

    @Test
    void schemaAndEventRunThroughBackendAndDatabaseThenReturnValidatedEvent() throws Exception {
        String schemaRequestId = "req-plugin-schema-" + FLOW_ID;
        String schemaVersion = "1.0.0-flow-" + randomKey();

        TestHttpResponse schemaResponse = exchange(
                HttpMethod.POST,
                "/api/v1/plugin-integration/admin/event-schemas",
                bearerHeaders("plugin-admin-token", schemaRequestId),
                schemaBody("schema-" + randomKey(), schemaVersion)
        );

        assertThat(schemaResponse.statusCode()).isEqualTo(201);
        JsonNode schemaJson = objectMapper.readTree(schemaResponse.body());
        assertThat(schemaJson.at("/data/status").asText()).isEqualTo("DRAFT");
        String schemaId = schemaJson.at("/data/schemaId").asText();

        String enableRequestId = "req-plugin-schema-enable-" + FLOW_ID;
        TestHttpResponse enabledResponse = exchange(
                HttpMethod.PATCH,
                "/api/v1/plugin-integration/admin/event-schemas/" + schemaId + "/enable",
                bearerHeaders("plugin-admin-token", enableRequestId),
                Map.of("reason", "启用 plugin-integration 数据库流 schema", "idempotencyKey", "enable-" + randomKey())
        );
        assertThat(enabledResponse.statusCode()).isEqualTo(200);

        String eventRequestId = "req-plugin-event-" + FLOW_ID;
        TestHttpResponse eventResponse = exchange(
                HttpMethod.POST,
                "/api/v1/plugin-integration/admin/events/ingest",
                bearerHeaders("plugin-admin-token", eventRequestId),
                eventBody("event-" + randomKey())
        );

        assertThat(eventResponse.statusCode()).isEqualTo(201);
        JsonNode eventJson = objectMapper.readTree(eventResponse.body());
        assertThat(eventJson.at("/data/validationStatus").asText()).isEqualTo("VALIDATED");
        assertThat(eventJson.at("/data/rawPayloadStored").asBoolean()).isFalse();
        String eventId = eventJson.at("/data/eventId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_schemas WHERE flow_id = ? AND schema_id = ? AND action = 'PLUGIN_SCHEMA_CREATED'",
                    FLOW_ID, schemaId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_schemas WHERE flow_id = ? AND schema_id = ? AND action = 'PLUGIN_SCHEMA_ENABLED'",
                    FLOW_ID, schemaId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT validation_status FROM plugin_flow_events WHERE flow_id = ? AND event_id = ? AND action = 'PLUGIN_EVENT_INGESTED'",
                    FLOW_ID, eventId, "VALIDATED");
            assertSingleValue(connection,
                    "SELECT response_code FROM plugin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/plugin-integration/admin/events/ingest'",
                    FLOW_ID, eventRequestId, 201);
        }
        System.out.println("SQL evidence: plugin-integration schema enable and event ingest reached backend, wrote schema/event/audit/request rows, and returned 201.");
    }

    @Test
    void routeAndSyncTaskRunThroughBackendAndDatabaseThenReturnCanceledTask() throws Exception {
        String routeRequestId = "req-plugin-route-" + FLOW_ID;
        TestHttpResponse routeResponse = exchange(
                HttpMethod.POST,
                "/api/v1/plugin-integration/admin/route-rules",
                bearerHeaders("plugin-admin-token", routeRequestId),
                routeBody("route-" + randomKey())
        );

        assertThat(routeResponse.statusCode()).isEqualTo(201);
        JsonNode routeJson = objectMapper.readTree(routeResponse.body());
        assertThat(routeJson.at("/data/enabled").asBoolean()).isTrue();
        String ruleId = routeJson.at("/data/ruleId").asText();

        String taskRequestId = "req-plugin-task-" + FLOW_ID;
        TestHttpResponse taskResponse = exchange(
                HttpMethod.POST,
                "/api/v1/plugin-integration/admin/sync-tasks",
                bearerHeaders("plugin-admin-token", taskRequestId),
                syncTaskBody("task-" + randomKey())
        );

        assertThat(taskResponse.statusCode()).isEqualTo(201);
        JsonNode taskJson = objectMapper.readTree(taskResponse.body());
        assertThat(taskJson.at("/data/status").asText()).isEqualTo("SIMULATED_BLOCKED");
        String taskId = taskJson.at("/data/taskId").asText();

        String cancelRequestId = "req-plugin-task-cancel-" + FLOW_ID;
        TestHttpResponse cancelResponse = exchange(
                HttpMethod.PATCH,
                "/api/v1/plugin-integration/admin/sync-tasks/" + taskId + "/cancel",
                bearerHeaders("plugin-admin-token", cancelRequestId),
                Map.of("reason", "取消 plugin-integration 数据库流同步任务", "idempotencyKey", "cancel-" + randomKey())
        );

        assertThat(cancelResponse.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(cancelResponse.body()).at("/data/status").asText()).isEqualTo("CANCELED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT enabled FROM plugin_flow_routes WHERE flow_id = ? AND rule_id = ? AND action = 'PLUGIN_ROUTE_RULE_CREATED'",
                    FLOW_ID, ruleId, true);
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_tasks WHERE flow_id = ? AND task_id = ? AND action = 'PLUGIN_SYNC_TASK_CREATED'",
                    FLOW_ID, taskId, "SIMULATED_BLOCKED");
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_tasks WHERE flow_id = ? AND task_id = ? AND action = 'PLUGIN_SYNC_TASK_CANCELED'",
                    FLOW_ID, taskId, "CANCELED");
            assertSingleValue(connection,
                    "SELECT response_code FROM plugin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, cancelRequestId, "/api/v1/plugin-integration/admin/sync-tasks/" + taskId + "/cancel", 200);
        }
        System.out.println("SQL evidence: plugin-integration route and sync task reached backend, wrote route/task/audit/request rows, and returned 200.");
    }

    @Test
    void objectMappingRunsThroughBackendAndDatabaseThenReturnsArchivedMapping() throws Exception {
        String mappingId = "mapping-flow-" + randomKey();
        String mappingRequestId = "req-plugin-mapping-" + FLOW_ID;

        TestHttpResponse mappingResponse = exchange(
                HttpMethod.PUT,
                "/api/v1/plugin-integration/admin/object-mappings/" + mappingId,
                bearerHeaders("plugin-admin-token", mappingRequestId),
                with(mappingBody(mappingId), "confirmText", "UPSERT_PLUGIN_OBJECT_MAPPING")
        );

        assertThat(mappingResponse.statusCode()).isEqualTo(201);
        JsonNode mappingJson = objectMapper.readTree(mappingResponse.body());
        assertThat(mappingJson.at("/data/status").asText()).isEqualTo("ACTIVE");

        String archiveRequestId = "req-plugin-mapping-archive-" + FLOW_ID;
        TestHttpResponse archiveResponse = exchange(
                HttpMethod.PATCH,
                "/api/v1/plugin-integration/admin/object-mappings/" + mappingId + "/archive",
                bearerHeaders("plugin-admin-token", archiveRequestId),
                Map.of("reason", "归档 plugin-integration 数据库流对象映射", "idempotencyKey", "archive-" + randomKey())
        );

        assertThat(archiveResponse.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(archiveResponse.body()).at("/data/status").asText()).isEqualTo("ARCHIVED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_mappings WHERE flow_id = ? AND mapping_id = ? AND action = 'PLUGIN_OBJECT_MAPPING_UPSERTED'",
                    FLOW_ID, mappingId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT status FROM plugin_flow_mappings WHERE flow_id = ? AND mapping_id = ? AND action = 'PLUGIN_OBJECT_MAPPING_ARCHIVED'",
                    FLOW_ID, mappingId, "ARCHIVED");
            assertSingleValue(connection,
                    "SELECT response_code FROM plugin_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, archiveRequestId, "/api/v1/plugin-integration/admin/object-mappings/" + mappingId + "/archive", 200);
        }
        System.out.println("SQL evidence: plugin-integration object mapping reached backend, wrote mapping/audit/request rows, and returned 200.");
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
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

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerType", "PAPER");
        body.put("displayName", "Database Flow Plugin Provider " + idempotencyKey);
        body.put("pluginName", "BeimingBridge");
        body.put("pluginVersion", "1.0.0");
        body.put("serverKind", "SERVER");
        body.put("instanceRef", Map.of("instanceId", "mc-main"));
        body.put("nodeRef", Map.of("nodeId", "node-main"));
        body.put("publicVisible", false);
        body.put("eventEndpointSummary", "/plugin-events/" + idempotencyKey);
        body.put("allowedEventTypes", List.of("beiming.player_join", "beiming.map_marker"));
        body.put("allowedOrigins", List.of("https://plugins.example.com"));
        body.put("reason", "创建 plugin-integration 数据库流 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> schemaBody(String idempotencyKey, String version) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventType", "beiming.player_join");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("version", version);
        body.put("requiredFields", List.of("player", "world"));
        body.put("optionalFields", List.of("dimension"));
        body.put("sensitiveFields", List.of("ip", "token", "webhookSecret"));
        body.put("routingHints", Map.of("targetModule", "ONLINE_MAP"));
        body.put("samplePayloadSummary", Map.of("player", "Steve", "world", "overworld"));
        body.put("reason", "创建 plugin-integration 数据库流 schema");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventType", "beiming.player_join");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("sourceInstanceId", "instance-paper-main");
        body.put("dedupeKey", idempotencyKey);
        body.put("origin", "https://plugins.example.com");
        body.put("payload", Map.of("player", "Steve", "world", "overworld", "dimension", "OVERWORLD"));
        body.put("occurredAt", "2026-05-28T00:00:00Z");
        body.put("reason", "接收 plugin-integration 数据库流事件");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Plugin Route " + idempotencyKey);
        body.put("eventType", "beiming.player_join");
        body.put("matchers", Map.of("providerId", "provider-paper-main", "sourcePlugin", "BeimingBridge"));
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetAction", "UPSERT_MARKER_PREVIEW_" + idempotencyKey);
        body.put("enabled", true);
        body.put("riskLevel", "MEDIUM");
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "maxEvents", 100));
        body.put("reason", "创建 plugin-integration 数据库流路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> syncTaskBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventId", "event-seed-player-join");
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetAction", "UPSERT_MARKER_PREVIEW");
        body.put("params", Map.of("mappingId", "mapping-seed"));
        body.put("riskLevel", "MEDIUM");
        body.put("reason", "创建 plugin-integration 数据库流同步任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> mappingBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("sourceObjectType", "PLAYER_MARKER");
        body.put("sourceObjectKey", "source-" + idempotencyKey);
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetObjectType", "MAP_MARKER");
        body.put("targetObjectId", "marker-" + idempotencyKey);
        body.put("status", "ACTIVE");
        body.put("visibility", "PUBLIC");
        body.put("syncHash", "hash-" + idempotencyKey);
        body.put("reason", "创建 plugin-integration 数据库流对象映射");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
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
                CREATE TABLE IF NOT EXISTS plugin_flow_providers (
                    flow_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_schemas (
                    flow_id VARCHAR(128) NOT NULL,
                    schema_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_events (
                    flow_id VARCHAR(128) NOT NULL,
                    event_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    schema_id VARCHAR(128) NOT NULL,
                    validation_status VARCHAR(32) NOT NULL,
                    route_status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_routes (
                    flow_id VARCHAR(128) NOT NULL,
                    rule_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_module VARCHAR(64) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    risk_level VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_tasks (
                    flow_id VARCHAR(128) NOT NULL,
                    task_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    event_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_mappings (
                    flow_id VARCHAR(128) NOT NULL,
                    mapping_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    visibility VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS plugin_flow_request_log (
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

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        PluginIntegrationFlowEvidenceRecorder pluginIntegrationFlowEvidenceRecorder() {
            return new JdbcPluginIntegrationFlowEvidenceRecorder();
        }
    }

    static class JdbcPluginIntegrationFlowEvidenceRecorder implements PluginIntegrationFlowEvidenceRecorder {
        @Override
        public void recordProviderWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_providers(flow_id, provider_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("providerId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("providerId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration provider database evidence", exception);
            }
        }

        @Override
        public void recordSchemaWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_schemas(flow_id, schema_id, action, provider_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("schemaId"), action, payload.get("providerId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("schemaId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration schema database evidence", exception);
            }
        }

        @Override
        public void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_events(flow_id, event_id, action, provider_id, schema_id, validation_status, route_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("eventId"), action, payload.get("providerId"), payload.get("schemaId"),
                        payload.get("validationStatus"), payload.get("routeStatus"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("eventId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration event database evidence", exception);
            }
        }

        @Override
        public void recordRouteWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_routes(flow_id, rule_id, action, target_module, enabled, risk_level, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("ruleId"), action, payload.get("targetModule"), payload.get("enabled"), payload.get("riskLevel"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("ruleId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration route database evidence", exception);
            }
        }

        @Override
        public void recordTaskWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_tasks(flow_id, task_id, action, provider_id, event_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("taskId"), action, payload.get("providerId"), payload.get("eventId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("taskId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration task database evidence", exception);
            }
        }

        @Override
        public void recordMappingWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) return;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO plugin_flow_mappings(flow_id, mapping_id, action, provider_id, status, visibility, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("mappingId"), action, payload.get("providerId"), payload.get("status"), payload.get("visibility"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("mappingId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write plugin-integration mapping database evidence", exception);
            }
        }

        private static String flowId(HttpServletRequest request) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            return flowId == null || flowId.isBlank() ? null : flowId;
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, HttpServletRequest request, String action, String targetId, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO plugin_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, request.getHeader("X-Request-Id"), action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO plugin_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, request.getHeader("X-Request-Id"), request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
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
