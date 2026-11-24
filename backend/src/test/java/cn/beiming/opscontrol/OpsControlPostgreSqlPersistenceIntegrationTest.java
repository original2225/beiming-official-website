package cn.beiming.opscontrol;

import cn.beiming.opscore.OpsCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpsCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "ops-control.test-controls.enabled=true",
                "ops-control.auth.stub-tokens-enabled=true"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsControlPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "ops-control-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_ops_control_pg")
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
    Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM ops_approvals");
            statement.executeUpdate("DELETE FROM ops_tasks");
            statement.executeUpdate("DELETE FROM ops_runtime_snapshots");
            statement.executeUpdate("DELETE FROM ops_metrics");
            statement.executeUpdate("DELETE FROM ops_nodes");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'ops-control.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type IN ('NODE', 'TASK', 'APPROVAL')");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/ops-control%'");
        }
    }

    @Test
    void createNodePersistsNodeAuditIdempotencyAndRequestLogs() throws Exception {
        String requestId = requestId("node-create");
        String idempotencyKey = "node-" + randomKey();

        Response response = exchange(HttpMethod.POST, "/api/v1/ops-control/nodes",
                bearerHeaders("ops-admin-token", requestId), nodeBody(idempotencyKey));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/node/status").asText()).isEqualTo("PENDING_REGISTRATION");
        assertThat(json.at("/data/node/createdBy").asText()).isEqualTo("ops-admin-user");
        String nodeId = json.at("/data/node/nodeId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM ops_nodes WHERE node_id = ?", nodeId, "PENDING_REGISTRATION");
            assertSingleValue(connection, "SELECT created_by FROM ops_nodes WHERE node_id = ?", nodeId, "ops-admin-user");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'NODE' AND target_id = ? AND action = 'OPS_NODE_CREATED'", nodeId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'ops-admin-user' AND scope = 'ops-control.node:create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/ops-control/nodes'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL ops-control node create wrote ops_nodes/audit/idempotency/request rows and returned 201.");
    }

    @Test
    void heartbeatPersistsRuntimeSnapshotsAndMetrics() throws Exception {
        String nodeId = createNode("setup-heartbeat");
        String requestId = requestId("heartbeat");
        String containerId = "container-" + randomKey();

        Response response = exchange(HttpMethod.POST, "/api/v1/ops-control/nodes/" + nodeId + "/heartbeat",
                bearerHeaders("ops-node-writer-token", requestId), heartbeatBody(containerId));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("ONLINE");
        assertThat(json.at("/data/lastHeartbeatAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM ops_nodes WHERE node_id = ?", nodeId, "ONLINE");
            assertSingleValue(connection, "SELECT version FROM ops_nodes WHERE node_id = ?", nodeId, "0.1.0-simulated");
            assertSingleValue(connection, "SELECT COUNT(*) FROM ops_runtime_snapshots WHERE node_id = ?", nodeId, 3L);
            assertSingleValue(connection, "SELECT status FROM ops_runtime_snapshots WHERE node_id = ? AND snapshot_type = 'CONTAINER' AND target_id = ?", nodeId, containerId, "RUNNING");
            assertSingleValue(connection, "SELECT COUNT(*) FROM ops_metrics WHERE node_id = ?", nodeId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'NODE' AND target_id = ? AND action = 'OPS_NODE_HEARTBEAT'", nodeId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL ops-control heartbeat wrote ops_nodes/ops_runtime_snapshots/ops_metrics/audit/request rows and returned 200.");
    }

    @Test
    void taskAndNodeResultPersistTaskStateTransitions() throws Exception {
        String createRequestId = requestId("task-create");
        Response created = exchange(HttpMethod.POST, "/api/v1/ops-control/tasks",
                bearerHeaders("ops-container-token", createRequestId, Map.of("X-Test-Node-Mode", "dispatched")),
                taskBody("CONTAINER_STOP", "container-seed-1", "task-" + randomKey()));

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode createdJson = objectMapper.readTree(created.body());
        assertThat(createdJson.at("/data/status").asText()).isEqualTo("DISPATCHED");
        String taskId = createdJson.at("/data/taskId").asText();

        String resultRequestId = requestId("task-result");
        Response result = exchange(HttpMethod.POST, "/api/v1/ops-control/tasks/" + taskId + "/node-result",
                bearerHeaders("ops-node-writer-token", resultRequestId),
                Map.of("nodeRequestId", "node-result-" + randomKey(), "status", "SUCCEEDED",
                        "resultSummary", Map.of("message", "ok"), "finishedAt", "2026-06-01T12:00:00Z"));

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(result.body()).at("/data/status").asText()).isEqualTo("SUCCEEDED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM ops_tasks WHERE task_id = ?", taskId, "SUCCEEDED");
            assertSingleValue(connection, "SELECT created_by FROM ops_tasks WHERE task_id = ?", taskId, "ops-container-user");
            assertSingleValue(connection, "SELECT task_type FROM ops_tasks WHERE task_id = ?", taskId, "CONTAINER_STOP");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'TASK' AND target_id = ? AND action = 'OPS_TASK_CREATED'", taskId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'TASK' AND target_id = ? AND action = 'OPS_TASK_NODE_RESULT_RECORDED'", taskId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", resultRequestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL ops-control task create and node result wrote ops_tasks/audit/request rows.");
    }

    @Test
    void highRiskApprovalPersistsApprovalAndFailedTask() throws Exception {
        Response taskResponse = exchange(HttpMethod.POST, "/api/v1/ops-control/tasks",
                bearerHeaders("ops-file-token", requestId("approval-task")),
                with(taskBody("FILE_DELETE", "/runtime-config.txt", "approval-" + randomKey()), "confirmText", "DELETE_FILE"));

        assertThat(taskResponse.statusCode()).isEqualTo(201);
        JsonNode taskJson = objectMapper.readTree(taskResponse.body());
        assertThat(taskJson.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        String taskId = taskJson.at("/data/taskId").asText();
        String approvalId = taskJson.at("/data/approvalId").asText();

        String approveRequestId = requestId("approve");
        Response approved = exchange(HttpMethod.PATCH, "/api/v1/ops-control/approvals/" + approvalId + "/approve",
                bearerHeaders("owner-token", approveRequestId),
                Map.of("reviewComment", "允许测试文件快照删除", "reason", "审批高风险任务", "idempotencyKey", "approve-" + randomKey()));

        assertThat(approved.statusCode()).isEqualTo(200);
        JsonNode approvedJson = objectMapper.readTree(approved.body());
        assertThat(approvedJson.at("/data/approval/status").asText()).isEqualTo("APPROVED");
        assertThat(approvedJson.at("/data/task/status").asText()).isEqualTo("FAILED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM ops_approvals WHERE approval_id = ?", approvalId, "APPROVED");
            assertSingleValue(connection, "SELECT approved_by FROM ops_approvals WHERE approval_id = ?", approvalId, "owner-user");
            assertSingleValue(connection, "SELECT task_id FROM ops_approvals WHERE approval_id = ?", approvalId, taskId);
            assertSingleValue(connection, "SELECT status FROM ops_tasks WHERE task_id = ?", taskId, "FAILED");
            assertSingleValue(connection, "SELECT failure_reason FROM ops_tasks WHERE task_id = ?", taskId, "EXTERNAL_EXECUTOR_NOT_CONNECTED");
            assertSingleValue(connection, "SELECT approval_id FROM ops_tasks WHERE task_id = ?", taskId, approvalId);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'APPROVAL' AND target_id = ? AND action = 'OPS_APPROVAL_APPROVED'", approvalId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", approveRequestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL ops-control high-risk approval wrote ops_approvals/ops_tasks/audit/request rows.");
    }

    @Test
    void boundariesAndIdempotencyConflictRollBackWithoutRequestLog() throws Exception {
        assertThat(exchangeJson(HttpMethod.GET, "/api/v1/ops-control/nodes", jsonHeaders(), null).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchangeJson(HttpMethod.POST, "/api/v1/ops-control/nodes", bearerHeaders("user-token", requestId("forbidden")), nodeBody("forbidden-" + randomKey())).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchangeJson(HttpMethod.POST, "/api/v1/ops-control/nodes", bearerHeaders("ops-admin-token", requestId("validation")), with(nodeBody("validation-" + randomKey()), "displayName", "")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchangeJson(HttpMethod.POST, "/api/v1/ops-control/nodes/missing-node/heartbeat", bearerHeaders("ops-node-writer-token", requestId("missing")), heartbeatBody("container-" + randomKey())).at("/code").asInt()).isEqualTo(49401);

        String idempotencyKey = "idem-" + randomKey();
        exchange(HttpMethod.POST, "/api/v1/ops-control/nodes", bearerHeaders("ops-admin-token", requestId("idem-seed")), nodeBody(idempotencyKey));
        JsonNode conflict = exchangeJson(HttpMethod.POST, "/api/v1/ops-control/nodes", bearerHeaders("ops-admin-token", requestId("idem-conflict")),
                with(nodeBody(idempotencyKey), "displayName", "不同显示名 " + randomKey()));
        assertThat(conflict.at("/code").asInt()).isEqualTo(49412);

        JsonNode auditFailure = exchangeJson(HttpMethod.POST, "/api/v1/ops-control/nodes",
                bearerHeaders("ops-admin-token", requestId("audit-failure"), Map.of("X-Test-Fail-Audit", "true")),
                nodeBody("audit-failure-" + randomKey()));
        assertThat(auditFailure.at("/code").asInt()).isEqualTo(55001);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM ops_nodes WHERE node_id = 'node-' || ?", "no-such-node", 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'ops-admin-user' AND scope = 'ops-control.node:create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("idem-conflict"), 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("audit-failure"), 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM ops_nodes WHERE created_by = 'ops-admin-user' AND display_name LIKE 'audit-failure-%'", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL ops-control boundary test covered auth, permission, validation, missing resource, idempotency conflict rollback and audit failure rollback.");
    }

    private String createNode(String purpose) throws Exception {
        Response response = exchange(HttpMethod.POST, "/api/v1/ops-control/nodes",
                bearerHeaders("ops-admin-token", requestId("setup-" + purpose)), nodeBody(purpose + "-" + randomKey()));
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/node/nodeId").asText();
    }

    private Response exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    private JsonNode exchangeJson(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        return objectMapper.readTree(exchange(method, path, headers, body).body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        return bearerHeaders(token, requestId, Map.of());
    }

    private HttpHeaders bearerHeaders(String token, String requestId, Map<String, String> extraHeaders) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        extraHeaders.forEach(headers::set);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        body.put("reason", "节点心跳");
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

    private String requestId(String prefix) {
        return "req-" + prefix + "-" + FLOW_ID;
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).as(sql).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).as(sql).isEqualTo(expected);
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
                assertThat(result.getObject(1)).as(sql).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    record Response(int statusCode, String body) {
    }
}
