package cn.beiming.opscontrol;

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
        properties = "ops-control.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsControlRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "ops-control-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:ops_control_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "ops_flow_request_log",
                    "ops_flow_nodes",
                    "ops_flow_runtime_snapshots",
                    "ops_flow_tasks",
                    "ops_flow_approvals",
                    "ops_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createNodeRunsThroughBackendAndDatabaseThenReturnsPendingRegistration() throws Exception {
        String requestId = "req-ops-node-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/nodes",
                bearerHeaders("ops-admin-token", requestId),
                nodeBody("node-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/node/status").asText()).isEqualTo("PENDING_REGISTRATION");
        assertThat(json.at("/data/node/createdBy").asText()).isEqualTo("ops-admin-user");
        String nodeId = json.at("/data/node/nodeId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_nodes WHERE flow_id = ? AND node_id = ? AND action = 'OPS_NODE_CREATED'",
                    FLOW_ID, nodeId, "PENDING_REGISTRATION");
            assertSingleValue(connection,
                    "SELECT created_by FROM ops_flow_nodes WHERE flow_id = ? AND node_id = ? AND action = 'OPS_NODE_CREATED'",
                    FLOW_ID, nodeId, "ops-admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM ops_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'OPS_NODE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM ops_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/ops-control/nodes'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: ops-control node create reached backend, wrote node/audit/request rows, and returned 201.");
    }

    @Test
    void heartbeatRunsThroughBackendAndDatabaseThenReturnsOnlineNodeAndRuntimeSnapshots() throws Exception {
        String nodeId = createNode("setup-heartbeat");
        String requestId = "req-ops-heartbeat-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/nodes/" + nodeId + "/heartbeat",
                bearerHeaders("ops-node-writer-token", requestId),
                heartbeatBody("container-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("ONLINE");
        assertThat(json.at("/data/lastHeartbeatAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_nodes WHERE flow_id = ? AND node_id = ? AND action = 'OPS_NODE_HEARTBEAT'",
                    FLOW_ID, nodeId, "ONLINE");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM ops_flow_runtime_snapshots WHERE flow_id = ? AND node_id = ? AND action = 'OPS_NODE_HEARTBEAT'",
                    FLOW_ID, nodeId, 3L);
            assertSingleValue(connection,
                    "SELECT response_code FROM ops_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/ops-control/nodes/" + nodeId + "/heartbeat", 200);
        }
        System.out.println("SQL evidence: ops-control heartbeat reached backend, wrote node/runtime/request rows, and returned 200.");
    }

    @Test
    void taskAndNodeResultRunThroughBackendAndDatabaseThenReturnSucceededTask() throws Exception {
        String createRequestId = "req-ops-task-" + FLOW_ID;

        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/tasks",
                bearerHeaders("ops-container-token", createRequestId, Map.of("X-Test-Node-Mode", "dispatched")),
                taskBody("CONTAINER_STOP", "container-seed-1", "task-" + randomKey())
        );

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode createdJson = objectMapper.readTree(created.body());
        assertThat(createdJson.at("/data/status").asText()).isEqualTo("DISPATCHED");
        String taskId = createdJson.at("/data/taskId").asText();

        String resultRequestId = "req-ops-result-" + FLOW_ID;
        TestHttpResponse result = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/tasks/" + taskId + "/node-result",
                bearerHeaders("ops-node-writer-token", resultRequestId),
                Map.of("nodeRequestId", "node-result-" + randomKey(), "status", "SUCCEEDED", "resultSummary", Map.of("message", "ok"), "finishedAt", "2026-06-01T12:00:00Z")
        );

        assertThat(result.statusCode()).isEqualTo(200);
        JsonNode resultJson = objectMapper.readTree(result.body());
        assertThat(resultJson.at("/data/status").asText()).isEqualTo("SUCCEEDED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_tasks WHERE flow_id = ? AND task_id = ? AND action = 'OPS_TASK_CREATED'",
                    FLOW_ID, taskId, "DISPATCHED");
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_tasks WHERE flow_id = ? AND task_id = ? AND action = 'OPS_TASK_NODE_RESULT_RECORDED'",
                    FLOW_ID, taskId, "SUCCEEDED");
            assertSingleValue(connection,
                    "SELECT response_code FROM ops_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, resultRequestId, "/api/v1/ops-control/tasks/" + taskId + "/node-result", 200);
        }
        System.out.println("SQL evidence: ops-control task and node result reached backend, wrote task/request rows, and returned 200.");
    }

    @Test
    void highRiskApprovalRunsThroughBackendAndDatabaseThenReturnsFailedApprovedTask() throws Exception {
        String taskRequestId = "req-ops-approval-task-" + FLOW_ID;
        TestHttpResponse taskResponse = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/tasks",
                bearerHeaders("ops-file-token", taskRequestId),
                with(taskBody("FILE_DELETE", "/runtime-config.txt", "approval-" + randomKey()), "confirmText", "DELETE_FILE")
        );

        assertThat(taskResponse.statusCode()).isEqualTo(201);
        JsonNode taskJson = objectMapper.readTree(taskResponse.body());
        assertThat(taskJson.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        String taskId = taskJson.at("/data/taskId").asText();
        String approvalId = taskJson.at("/data/approvalId").asText();

        String approveRequestId = "req-ops-approve-" + FLOW_ID;
        TestHttpResponse approved = exchange(
                HttpMethod.PATCH,
                "/api/v1/ops-control/approvals/" + approvalId + "/approve",
                bearerHeaders("owner-token", approveRequestId),
                Map.of("reviewComment", "允许测试文件快照删除", "reason", "审批高风险任务", "idempotencyKey", "approve-" + randomKey())
        );

        assertThat(approved.statusCode()).isEqualTo(200);
        JsonNode approvedJson = objectMapper.readTree(approved.body());
        assertThat(approvedJson.at("/data/approval/status").asText()).isEqualTo("APPROVED");
        assertThat(approvedJson.at("/data/task/status").asText()).isEqualTo("FAILED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_approvals WHERE flow_id = ? AND approval_id = ? AND action = 'OPS_APPROVAL_CREATED'",
                    FLOW_ID, approvalId, "PENDING");
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_approvals WHERE flow_id = ? AND approval_id = ? AND action = 'OPS_APPROVAL_APPROVED'",
                    FLOW_ID, approvalId, "APPROVED");
            assertSingleValue(connection,
                    "SELECT status FROM ops_flow_tasks WHERE flow_id = ? AND task_id = ? AND action = 'OPS_APPROVAL_APPROVED'",
                    FLOW_ID, taskId, "FAILED");
            assertSingleValue(connection,
                    "SELECT response_code FROM ops_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, approveRequestId, "/api/v1/ops-control/approvals/" + approvalId + "/approve", 200);
        }
        System.out.println("SQL evidence: ops-control high-risk approval reached backend, wrote approval/task/request rows, and returned 200.");
    }

    private String createNode(String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/ops-control/nodes",
                bearerHeaders("ops-admin-token", "req-setup-node-" + purpose + "-" + FLOW_ID),
                nodeBody(purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/node/nodeId").asText();
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
        return bearerHeaders(token, requestId, Map.of());
    }

    private HttpHeaders bearerHeaders(String token, String requestId, Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        extraHeaders.forEach(headers::set);
        return headers;
    }

    private Map<String, Object> nodeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "数据库流节点 " + idempotencyKey);
        body.put("endpointSummary", "https://node.example.internal:9443");
        body.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "FILE_MANAGE"));
        body.put("labels", Map.of("env", "database-flow"));
        body.put("reason", "注册 ops-control 数据库流节点");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> heartbeatBody(String containerId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ONLINE");
        body.put("version", "0.1.0-simulated");
        body.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "FILE_MANAGE"));
        body.put("metrics", Map.of("cpuUsagePercent", 32.5, "memoryUsagePercent", 48.0, "diskUsagePercent", 61.0));
        body.put("nodeRequestId", "node-heartbeat-" + randomKey());
        body.put("containers", List.of(Map.of("containerId", containerId, "name", "database-flow-container", "image", "masked/image:latest", "status", "RUNNING")));
        body.put("vms", List.of(Map.of("vmId", "vm-" + randomKey(), "name", "database-flow-vm", "platform", "SIMULATED", "status", "STOPPED")));
        body.put("minecraftInstances", List.of(Map.of("instanceId", "mc-" + randomKey(), "publicInstanceId", "database-flow", "name", "数据库流实例", "version", "1.20.4", "status", "RUNNING")));
        return body;
    }

    private Map<String, Object> taskBody(String taskType, String targetId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskType", taskType);
        body.put("nodeId", "node-main");
        body.put("targetType", targetId.startsWith("/") ? "FILE" : targetId.startsWith("container") ? "CONTAINER" : "NODE");
        body.put("targetId", targetId);
        body.put("params", Map.of("mode", "simulated"));
        body.put("reason", "创建 ops-control 数据库流任务");
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
                CREATE TABLE IF NOT EXISTS ops_flow_nodes (
                    flow_id VARCHAR(128) NOT NULL,
                    node_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ops_flow_runtime_snapshots (
                    flow_id VARCHAR(128) NOT NULL,
                    node_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    snapshot_type VARCHAR(64) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ops_flow_tasks (
                    flow_id VARCHAR(128) NOT NULL,
                    task_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    task_type VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    risk_level VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ops_flow_approvals (
                    flow_id VARCHAR(128) NOT NULL,
                    approval_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    task_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    risk_level VARCHAR(32) NOT NULL,
                    requested_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ops_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ops_flow_request_log (
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
        OpsControlFlowEvidenceRecorder opsControlFlowEvidenceRecorder() {
            return new JdbcOpsControlFlowEvidenceRecorder();
        }
    }

    static class JdbcOpsControlFlowEvidenceRecorder implements OpsControlFlowEvidenceRecorder {
        @Override
        public void recordNodeWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insertNode(connection, flowId, action, payload);
                insertRuntimeSnapshots(connection, flowId, action, payload);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("nodeId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-control node database evidence", exception);
            }
        }

        @Override
        public void recordTaskWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insertTask(connection, flowId, action, payload);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("taskId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-control task database evidence", exception);
            }
        }

        @Override
        public void recordApprovalWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> approval = map(payload.get("approval"));
            Map<?, ?> task = map(payload.get("task"));
            try (Connection connection = openConnection()) {
                insertApproval(connection, flowId, action, approval);
                insertTask(connection, flowId, action, task);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(approval.get("approvalId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-control approval database evidence", exception);
            }
        }

        private static void insertNode(Connection connection, String flowId, String action, Map<?, ?> payload) throws Exception {
            insert(connection,
                    "INSERT INTO ops_flow_nodes(flow_id, node_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("nodeId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
        }

        private static void insertRuntimeSnapshots(Connection connection, String flowId, String action, Map<?, ?> payload) throws Exception {
            Object runtimeValue = payload.get("runtimeEvidence");
            if (!(runtimeValue instanceof Map<?, ?> runtime)) {
                return;
            }
            insertSnapshotList(connection, flowId, action, "CONTAINER", runtime.get("containers"));
            insertSnapshotList(connection, flowId, action, "VM", runtime.get("vms"));
            insertSnapshotList(connection, flowId, action, "MINECRAFT", runtime.get("minecraftInstances"));
            insertSnapshotList(connection, flowId, action, "FILE", runtime.get("files"));
        }

        private static void insertSnapshotList(Connection connection, String flowId, String action, String type, Object value) throws Exception {
            if (!(value instanceof List<?> items)) {
                return;
            }
            for (Object itemValue : items) {
                if (itemValue instanceof Map<?, ?> item) {
                    Object targetValue = item.containsKey(idKey(type)) ? item.get(idKey(type)) : item.get("path");
                    Object statusValue = item.get("status");
                    String targetId = String.valueOf(targetValue == null ? "unknown" : targetValue);
                    String status = String.valueOf(statusValue == null ? "SNAPSHOT" : statusValue);
                    insert(connection,
                            "INSERT INTO ops_flow_runtime_snapshots(flow_id, node_id, action, snapshot_type, target_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            flowId, item.get("nodeId"), action, type, targetId, status, Timestamp.from(Instant.now()));
                }
            }
        }

        private static String idKey(String type) {
            return switch (type) {
                case "CONTAINER" -> "containerId";
                case "VM" -> "vmId";
                case "MINECRAFT" -> "instanceId";
                default -> "path";
            };
        }

        private static void insertTask(Connection connection, String flowId, String action, Map<?, ?> payload) throws Exception {
            insert(connection,
                    "INSERT INTO ops_flow_tasks(flow_id, task_id, action, task_type, status, risk_level, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("taskId"), action, payload.get("taskType"), payload.get("status"), payload.get("riskLevel"), payload.get("createdBy"), Timestamp.from(Instant.now()));
        }

        private static void insertApproval(Connection connection, String flowId, String action, Map<?, ?> approval) throws Exception {
            insert(connection,
                    "INSERT INTO ops_flow_approvals(flow_id, approval_id, action, task_id, status, risk_level, requested_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, approval.get("approvalId"), action, approval.get("taskId"), approval.get("status"), approval.get("riskLevel"), approval.get("requestedBy"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO ops_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO ops_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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

        private static Map<?, ?> map(Object value) {
            if (value instanceof Map<?, ?> map) {
                return map;
            }
            throw new IllegalArgumentException("expected map payload");
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
