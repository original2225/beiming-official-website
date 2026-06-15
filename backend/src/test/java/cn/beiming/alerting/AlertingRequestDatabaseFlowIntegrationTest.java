package cn.beiming.alerting;

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
        properties = "alerting.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AlertingRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "alerting-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:alerting_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "alerting_flow_request_log",
                    "alerting_flow_rules",
                    "alerting_flow_evaluations",
                    "alerting_flow_alerts",
                    "alerting_flow_silences",
                    "alerting_flow_routes",
                    "alerting_flow_deliveries",
                    "alerting_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createRuleRunsThroughBackendAndDatabaseThenReturnsDraftRule() throws Exception {
        String requestId = "req-alerting-rule-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/rules",
                bearerHeaders("alert-admin-token", requestId),
                ruleBody("rule-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("alert-admin-user");
        String ruleId = json.at("/data/ruleId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_rules WHERE flow_id = ? AND rule_id = ? AND action = 'ALERT_RULE_CREATED'",
                    FLOW_ID, ruleId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM alerting_flow_rules WHERE flow_id = ? AND rule_id = ? AND action = 'ALERT_RULE_CREATED'",
                    FLOW_ID, ruleId, "alert-admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM alerting_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ALERT_RULE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM alerting_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/alerting/rules'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: alerting rule create reached backend, wrote rule/audit/request rows, and returned 201.");
    }

    @Test
    void evaluateRuleRunsThroughBackendAndDatabaseThenReturnsEvaluationAlertAndDelivery() throws Exception {
        String requestId = "req-alerting-evaluate-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/rules/rule-node-offline/evaluate",
                bearerHeaders("alert-admin-token", requestId),
                Map.of("sourceSnapshot", sourceSnapshot("node-flow-a"), "dryRun", false, "reason", "评估 alerting 数据库流规则", "idempotencyKey", "eval-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("MATCHED");
        assertThat(json.at("/data/createdAlertId").asText()).isNotBlank();
        String evaluationId = json.at("/data/evaluationId").asText();
        String alertId = json.at("/data/createdAlertId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_evaluations WHERE flow_id = ? AND evaluation_id = ? AND action = 'ALERT_RULE_EVALUATED'",
                    FLOW_ID, evaluationId, "MATCHED");
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_alerts WHERE flow_id = ? AND alert_id = ? AND action = 'ALERT_RULE_EVALUATED'",
                    FLOW_ID, alertId, "FIRING");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM alerting_flow_deliveries WHERE flow_id = ? AND alert_id = ? AND action = 'ALERT_RULE_EVALUATED'",
                    FLOW_ID, alertId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM alerting_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/alerting/rules/rule-node-offline/evaluate'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: alerting rule evaluation reached backend, wrote evaluation/alert/delivery/audit/request rows, and returned 201.");
    }

    @Test
    void acknowledgeAndCloseAlertRunThroughBackendAndDatabaseThenReturnClosedAlert() throws Exception {
        String alertId = createAlert("ack-close");
        String ackRequestId = "req-alerting-ack-" + FLOW_ID;

        TestHttpResponse acknowledged = exchange(
                HttpMethod.PATCH,
                "/api/v1/alerting/alerts/" + alertId + "/acknowledge",
                bearerHeaders("alert-admin-token", ackRequestId),
                Map.of("reason", "确认 alerting 数据库流告警", "idempotencyKey", "ack-" + randomKey())
        );

        assertThat(acknowledged.statusCode()).isEqualTo(200);
        JsonNode ackJson = objectMapper.readTree(acknowledged.body());
        assertThat(ackJson.at("/data/status").asText()).isEqualTo("ACKNOWLEDGED");

        String closeRequestId = "req-alerting-close-" + FLOW_ID;
        TestHttpResponse closed = exchange(
                HttpMethod.PATCH,
                "/api/v1/alerting/alerts/" + alertId + "/close",
                bearerHeaders("alert-admin-token", closeRequestId),
                Map.of("resolutionSummary", "节点恢复", "confirmText", "CLOSE_ALERT", "reason", "关闭 alerting 数据库流告警", "idempotencyKey", "close-" + randomKey())
        );

        assertThat(closed.statusCode()).isEqualTo(200);
        JsonNode closeJson = objectMapper.readTree(closed.body());
        assertThat(closeJson.at("/data/status").asText()).isEqualTo("CLOSED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_alerts WHERE flow_id = ? AND alert_id = ? AND action = 'ALERT_ACKNOWLEDGED'",
                    FLOW_ID, alertId, "ACKNOWLEDGED");
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_alerts WHERE flow_id = ? AND alert_id = ? AND action = 'ALERT_CLOSED'",
                    FLOW_ID, alertId, "CLOSED");
            assertSingleValue(connection,
                    "SELECT response_code FROM alerting_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, closeRequestId, "/api/v1/alerting/alerts/" + alertId + "/close", 200);
        }
        System.out.println("SQL evidence: alerting acknowledge and close reached backend, wrote alert/audit/request rows, and returned 200.");
    }

    @Test
    void silenceAndRouteRunThroughBackendAndDatabaseThenReturnCancelledSilenceAndSentDelivery() throws Exception {
        String silenceRequestId = "req-alerting-silence-" + FLOW_ID;
        TestHttpResponse silenceResponse = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/silences",
                bearerHeaders("alert-admin-token", silenceRequestId),
                silenceBody("silence-" + randomKey())
        );

        assertThat(silenceResponse.statusCode()).isEqualTo(201);
        JsonNode silenceJson = objectMapper.readTree(silenceResponse.body());
        assertThat(silenceJson.at("/data/status").asText()).isEqualTo("ACTIVE");
        String silenceId = silenceJson.at("/data/silenceId").asText();

        String cancelRequestId = "req-alerting-cancel-silence-" + FLOW_ID;
        TestHttpResponse cancelled = exchange(
                HttpMethod.PATCH,
                "/api/v1/alerting/silences/" + silenceId + "/cancel",
                bearerHeaders("alert-admin-token", cancelRequestId),
                Map.of("reason", "取消 alerting 数据库流静默", "idempotencyKey", "cancel-" + randomKey())
        );

        assertThat(cancelled.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(cancelled.body()).at("/data/status").asText()).isEqualTo("CANCELLED");

        String routeRequestId = "req-alerting-route-" + FLOW_ID;
        TestHttpResponse routeResponse = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/routes",
                bearerHeaders("alert-admin-token", routeRequestId),
                routeBody("route-" + randomKey())
        );

        assertThat(routeResponse.statusCode()).isEqualTo(201);
        JsonNode routeJson = objectMapper.readTree(routeResponse.body());
        String routeId = routeJson.at("/data/routeId").asText();
        assertThat(routeJson.at("/data/status").asText()).isEqualTo("ENABLED");

        String testRouteRequestId = "req-alerting-test-route-" + FLOW_ID;
        TestHttpResponse deliveryResponse = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/routes/" + routeId + "/test",
                bearerHeaders("alert-admin-token", testRouteRequestId),
                Map.of("sampleAlert", sampleAlert(), "reason", "测试 alerting 数据库流路由", "idempotencyKey", "test-" + randomKey())
        );

        assertThat(deliveryResponse.statusCode()).isEqualTo(201);
        JsonNode deliveryJson = objectMapper.readTree(deliveryResponse.body());
        assertThat(deliveryJson.at("/data/status").asText()).isEqualTo("SENT");
        String deliveryId = deliveryJson.at("/data/deliveryId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_silences WHERE flow_id = ? AND silence_id = ? AND action = 'ALERT_SILENCE_CANCELLED'",
                    FLOW_ID, silenceId, "CANCELLED");
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_routes WHERE flow_id = ? AND route_id = ? AND action = 'ALERT_ROUTE_CREATED'",
                    FLOW_ID, routeId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT status FROM alerting_flow_deliveries WHERE flow_id = ? AND delivery_id = ? AND action = 'ALERT_ROUTE_TESTED'",
                    FLOW_ID, deliveryId, "SENT");
            assertSingleValue(connection,
                    "SELECT response_code FROM alerting_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, testRouteRequestId, "/api/v1/alerting/routes/" + routeId + "/test", 201);
        }
        System.out.println("SQL evidence: alerting silence and route writes reached backend, wrote silence/route/delivery/audit/request rows, and returned 201.");
    }

    private String createAlert(String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/alerting/rules/rule-node-offline/evaluate",
                bearerHeaders("alert-admin-token", "req-alerting-setup-" + purpose + "-" + FLOW_ID),
                Map.of("sourceSnapshot", sourceSnapshot("node-" + purpose + "-" + randomKey()), "dryRun", false,
                        "reason", "创建 alerting 数据库流告警", "idempotencyKey", "setup-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/createdAlertId").asText();
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

    private Map<String, Object> ruleBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Alert Rule " + idempotencyKey);
        body.put("sourceService", "OPS_CONTROL");
        body.put("sourceType", "HEALTH");
        body.put("severity", "WARNING");
        body.put("labels", Map.of("service", "ops-control", "scope", "node"));
        body.put("conditionType", "MISSING_HEARTBEAT");
        body.put("conditionSummary", Map.of("metric", "heartbeatAgeSeconds", "operator", ">", "threshold", 300));
        body.put("evaluationWindowSeconds", 300);
        body.put("forDurationSeconds", 60);
        body.put("dedupeKeyTemplate", "{{sourceService}}:{{nodeId}}");
        body.put("routeId", "route-default");
        body.put("runbookUrl", "/admin/ops/nodes");
        body.put("reason", "创建 alerting 数据库流规则");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> sourceSnapshot(String nodeId) {
        return Map.of("sourceRef", nodeId, "nodeId", nodeId, "status", "OFFLINE", "summary", "Node " + nodeId + " heartbeat delayed");
    }

    private Map<String, Object> silenceBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchers", Map.of("sourceService", "OPS_CONTROL", "severity", "WARNING", "labels", Map.of("node", "node-silence")));
        body.put("startsAt", "2020-01-01T00:00:00Z");
        body.put("endsAt", "2030-01-01T00:00:00Z");
        body.put("reason", "创建 alerting 数据库流静默");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Route " + idempotencyKey);
        body.put("matchers", Map.of("severity", "WARNING", "sourceService", "OPS_CONTROL"));
        body.put("groupBy", List.of("sourceService", "groupKey"));
        body.put("groupWaitSeconds", 30);
        body.put("groupIntervalSeconds", 300);
        body.put("repeatIntervalSeconds", 900);
        body.put("notificationTemplateRef", Map.of("templateCode", "ALERT_WARNING", "channel", "IN_APP"));
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "displayName", "Ops", "targetRefSummary", "#ops"));
        body.put("enabled", true);
        body.put("reason", "创建 alerting 数据库流路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> sampleAlert() {
        return Map.of("summary", "Node heartbeat delayed", "severity", "WARNING", "sourceService", "OPS_CONTROL", "labels", Map.of("node", "main"));
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
                CREATE TABLE IF NOT EXISTS alerting_flow_rules (
                    flow_id VARCHAR(128) NOT NULL,
                    rule_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_evaluations (
                    flow_id VARCHAR(128) NOT NULL,
                    evaluation_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    rule_id VARCHAR(128) NOT NULL,
                    alert_id VARCHAR(128),
                    status VARCHAR(32) NOT NULL,
                    evaluated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_alerts (
                    flow_id VARCHAR(128) NOT NULL,
                    alert_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    rule_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    severity VARCHAR(32) NOT NULL,
                    updated_by VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_silences (
                    flow_id VARCHAR(128) NOT NULL,
                    silence_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    cancelled_by VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_routes (
                    flow_id VARCHAR(128) NOT NULL,
                    route_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_deliveries (
                    flow_id VARCHAR(128) NOT NULL,
                    delivery_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    alert_id VARCHAR(128) NOT NULL,
                    route_id VARCHAR(128),
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS alerting_flow_request_log (
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
        AlertingFlowEvidenceRecorder alertingFlowEvidenceRecorder() {
            return new JdbcAlertingFlowEvidenceRecorder();
        }
    }

    static class JdbcAlertingFlowEvidenceRecorder implements AlertingFlowEvidenceRecorder {
        @Override
        public void recordRuleWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO alerting_flow_rules(flow_id, rule_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("ruleId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("ruleId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting rule database evidence", exception);
            }
        }

        @Override
        public void recordEvaluationWrite(HttpServletRequest request, String action, Map<String, Object> evaluation, Map<String, Object> alert, Map<String, Object> delivery, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO alerting_flow_evaluations(flow_id, evaluation_id, action, rule_id, alert_id, status, evaluated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, evaluation.get("evaluationId"), action, evaluation.get("ruleId"), evaluation.get("createdAlertId"), evaluation.get("status"), evaluation.get("evaluatedBy"), Timestamp.from(Instant.now()));
                if (alert != null && !alert.isEmpty()) {
                    insertAlert(connection, flowId, action, alert, evaluation.get("evaluatedBy"));
                }
                if (delivery != null && !delivery.isEmpty()) {
                    insertDelivery(connection, flowId, action, delivery);
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(evaluation.get("evaluationId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting evaluation database evidence", exception);
            }
        }

        @Override
        public void recordAlertWrite(HttpServletRequest request, String action, Map<String, Object> payload, String actorUserId, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insertAlert(connection, flowId, action, payload, actorUserId);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("alertId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting alert database evidence", exception);
            }
        }

        @Override
        public void recordSilenceWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO alerting_flow_silences(flow_id, silence_id, action, status, created_by, cancelled_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("silenceId"), action, payload.get("status"), payload.get("createdBy"), payload.get("cancelledBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("silenceId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting silence database evidence", exception);
            }
        }

        @Override
        public void recordRouteWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO alerting_flow_routes(flow_id, route_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("routeId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("routeId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting route database evidence", exception);
            }
        }

        @Override
        public void recordDeliveryWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insertDelivery(connection, flowId, action, payload);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("deliveryId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write alerting delivery database evidence", exception);
            }
        }

        private static void insertAlert(Connection connection, String flowId, String action, Map<String, Object> payload, Object actorUserId) throws Exception {
            insert(connection,
                    "INSERT INTO alerting_flow_alerts(flow_id, alert_id, action, rule_id, status, severity, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("alertId"), action, payload.get("ruleId"), payload.get("status"), payload.get("severity"), actorUserId, Timestamp.from(Instant.now()));
        }

        private static void insertDelivery(Connection connection, String flowId, String action, Map<String, Object> payload) throws Exception {
            insert(connection,
                    "INSERT INTO alerting_flow_deliveries(flow_id, delivery_id, action, alert_id, route_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("deliveryId"), action, payload.get("alertId"), payload.get("routeId"), payload.get("status"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO alerting_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO alerting_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
