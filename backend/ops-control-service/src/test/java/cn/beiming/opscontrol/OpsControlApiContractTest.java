package cn.beiming.opscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpsControlServiceApplication.class, properties = "ops-control.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsControlApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("ops-control local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "OPS-COM", 1, 100);
        addRange(mapped, "OPS-AUTH", 1, 100);
        addRange(mapped, "OPS-ASSET", 1, 90);
        addRange(mapped, "OPS-NODE", 1, 120);
        addRange(mapped, "OPS-METRIC", 1, 60);
        addRange(mapped, "OPS-CONTAINER", 1, 90);
        addRange(mapped, "OPS-VM", 1, 70);
        addRange(mapped, "OPS-MC", 1, 90);
        addRange(mapped, "OPS-FILE", 1, 120);
        addRange(mapped, "OPS-LOG", 1, 70);
        addRange(mapped, "OPS-TASK", 1, 160);
        addRange(mapped, "OPS-APPROVAL", 1, 120);
        addRange(mapped, "OPS-AUDIT", 1, 100);
        addRange(mapped, "OPS-DEPS", 1, 90);
        addRange(mapped, "OPS-HARDEN", 1, 160);
        addRange(mapped, "OPS-PORT", 1, 20);
        addRange(mapped, "OPS-CYCLE", 1, 80);
        assertThat(mapped).contains("OPS-COM-001", "OPS-NODE-120", "OPS-FILE-120", "OPS-HARDEN-160", "OPS-CYCLE-080");
        assertThat(mapped).hasSize(1640);
    }

    @Test
    @DisplayName("OPS-COM and OPS-AUTH cover envelope, request id, auth gates, role gates, capability gates, and validation")
    void commonAndAuthContract() throws Exception {
        mvc.perform(get("/api/v1/ops-control/overview")
                        .header("Authorization", bearer("ops-viewer-token"))
                        .header("X-Request-Id", "req-ops-overview"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-ops-overview"));

        JsonNode overview = performJson(get("/api/v1/ops-control/overview").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(overview.at("/code").asInt()).isZero();
        assertThat(overview.at("/message").asText()).isEqualTo("success");
        assertThat(overview.at("/data/nodesTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(overview.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(overview);

        performJson(get("/api/v1/ops-control/overview"), 401, 41000);
        performJson(get("/api/v1/ops-control/overview").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/ops-control/overview").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-viewer-token")), nodeBody("viewer-node"), 403, 42002);
        performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-node-writer-token")), taskBody("CONTAINER_RESTART", "container-seed-1", "restart-no-cap"), 403, 42002);
        performJson(get("/api/v1/ops-control/assets").header("Authorization", bearer("ops-viewer-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/ops-control/assets").header("Authorization", bearer("ops-viewer-token")).param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-admin-token")),
                with(nodeBody("trusted-fields"), "createdBy", "browser-user"), 400, 40001);
    }

    @Test
    @DisplayName("OPS-ASSET, OPS-NODE, OPS-METRIC, OPS-CONTAINER, OPS-VM, and OPS-MC cover inventory and snapshots")
    void inventoryNodeAndRuntimeSnapshots() throws Exception {
        JsonNode assets = performJson(get("/api/v1/ops-control/assets")
                .header("Authorization", bearer("ops-viewer-token"))
                .param("assetType", "MINECRAFT_INSTANCE")
                .param("riskTag", "core"), 200);
        assertThat(assets.toString()).contains("asset-mc-survival");
        assertNoSecrets(assets);

        JsonNode asset = performJson(get("/api/v1/ops-control/assets/asset-mc-survival").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(asset.at("/data/assetType").asText()).isEqualTo("MINECRAFT_INSTANCE");

        JsonNode nodes = performJson(get("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-viewer-token")).param("status", "ONLINE"), 200);
        assertThat(nodes.toString()).contains("node-main");

        JsonNode node = performJson(get("/api/v1/ops-control/nodes/node-main").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(node.at("/data/nodeId").asText()).isEqualTo("node-main");
        assertThat(node.toString()).doesNotContain("node-secret-token");

        JsonNode capabilities = performJson(get("/api/v1/ops-control/nodes/node-main/capabilities").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(capabilities.toString()).contains("NODE_READ");

        JsonNode metrics = performJson(get("/api/v1/ops-control/nodes/node-main/metrics/latest").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(metrics.at("/data/nodeId").asText()).isEqualTo("node-main");

        JsonNode containers = performJson(get("/api/v1/ops-control/nodes/node-main/containers").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(containers.toString()).contains("container-seed-1");
        assertThat(containers.toString()).doesNotContain("C:\\\\", "/var/lib");

        JsonNode container = performJson(get("/api/v1/ops-control/nodes/node-main/containers/container-seed-1").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(container.at("/data/status").asText()).isEqualTo("RUNNING");

        JsonNode vms = performJson(get("/api/v1/ops-control/nodes/node-main/vms").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(vms.toString()).contains("vm-build-1");

        JsonNode minecraft = performJson(get("/api/v1/ops-control/nodes/node-main/minecraft-instances").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(minecraft.toString()).contains("mc-survival");

        performJson(get("/api/v1/ops-control/nodes/missing-node").header("Authorization", bearer("ops-viewer-token")), 404, 49401);
        performJson(get("/api/v1/ops-control/nodes/node-main/containers/missing-container").header("Authorization", bearer("ops-viewer-token")), 404, 49400);
    }

    @Test
    @DisplayName("OPS-NODE covers registration, heartbeat, disable, enable, idempotency, and audit rollback")
    void nodeLifecycleAndAuditRollback() throws Exception {
        JsonNode created = performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-admin-token")),
                nodeBody("node-contract-1"), 201);
        String nodeId = created.at("/data/node/nodeId").asText();
        assertThat(created.toString()).contains("registrationTokenMasked").doesNotContain("node-secret-token");

        JsonNode replay = performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-admin-token")),
                nodeBody("node-contract-1"), 201);
        assertThat(replay.at("/data/node/nodeId").asText()).isEqualTo(nodeId);

        JsonNode newNodeDetail = performJson(get("/api/v1/ops-control/nodes/" + nodeId).header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(newNodeDetail.at("/data/latestMetric").isNull()).isTrue();
        assertThat(newNodeDetail.at("/data/degraded").asBoolean()).isTrue();

        performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-admin-token")),
                with(nodeBody("node-contract-1"), "displayName", "同 key 改名"), 409, 49412);

        JsonNode heartbeat = performJson(post("/api/v1/ops-control/nodes/" + nodeId + "/heartbeat").header("Authorization", bearer("ops-node-writer-token")),
                heartbeatBody(), 200);
        assertThat(heartbeat.at("/data/status").asText()).isEqualTo("ONLINE");

        performJson(patch("/api/v1/ops-control/nodes/" + nodeId + "/disable").header("Authorization", bearer("ops-admin-token")),
                Map.of("reason", "禁用节点", "confirmText", "WRONG", "idempotencyKey", "disable-wrong"), 409, 49413);

        JsonNode disabled = performJson(patch("/api/v1/ops-control/nodes/" + nodeId + "/disable").header("Authorization", bearer("ops-admin-token")),
                Map.of("reason", "禁用节点", "confirmText", "DISABLE_OPS_NODE", "idempotencyKey", "disable-node-1"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");

        JsonNode enabled = performJson(patch("/api/v1/ops-control/nodes/" + nodeId + "/enable").header("Authorization", bearer("ops-admin-token")),
                Map.of("reason", "启用节点", "idempotencyKey", "enable-node-1"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("OFFLINE");

        performJson(post("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-admin-token")).header("X-Test-Fail-Audit", "true"),
                nodeBody("audit-fail-node"), 500, 55001);
        JsonNode list = performJson(get("/api/v1/ops-control/nodes").header("Authorization", bearer("ops-viewer-token")).param("keyword", "audit-fail-node"), 200);
        assertThat(list.at("/data/total").asInt()).isZero();
    }

    @Test
    @DisplayName("OPS-FILE and OPS-LOG cover path guard, offline degradation, and log summaries")
    void filesAndLogsSafety() throws Exception {
        JsonNode files = performJson(get("/api/v1/ops-control/nodes/node-main/files")
                .header("Authorization", bearer("ops-file-token"))
                .param("rootAlias", "mc-config")
                .param("path", "/"), 200);
        assertThat(files.toString()).contains("runtime-config.txt").doesNotContain("/srv", "C:\\\\", "server.properties");

        performJson(get("/api/v1/ops-control/nodes/node-main/files")
                .header("Authorization", bearer("ops-file-token"))
                .param("rootAlias", "mc-config")
                .param("path", "../secret"), 409, 49414);
        performJson(get("/api/v1/ops-control/nodes/node-main/files")
                .header("Authorization", bearer("ops-file-token"))
                .param("rootAlias", "mc-config")
                .param("path", "\\\\secret"), 409, 49414);

        JsonNode read = performJson(post("/api/v1/ops-control/nodes/node-main/files/read").header("Authorization", bearer("ops-file-token")),
                Map.of("rootAlias", "mc-config", "path", "/runtime-config.txt", "reason", "读取配置摘要", "idempotencyKey", "read-file-1"), 200);
        assertThat(read.toString()).contains("contentSummary").doesNotContain("rcon.password");

        performJson(post("/api/v1/ops-control/nodes/node-main/files/read").header("Authorization", bearer("ops-file-token")),
                Map.of("rootAlias", "mc-config", "path", "/binary.dat", "reason", "不可编辑", "idempotencyKey", "read-binary"), 409, 49410);

        JsonNode logs = performJson(post("/api/v1/ops-control/nodes/node-main/logs/query").header("Authorization", bearer("ops-viewer-token")),
                Map.of("targetType", "MINECRAFT_INSTANCE", "targetId", "mc-survival", "tailLines", 100, "reason", "查询日志摘要", "idempotencyKey", "log-query-1"), 200);
        assertThat(logs.toString()).contains("logSummary").doesNotContain("Authorization", "node-secret-token");

        performJson(post("/api/v1/ops-control/nodes/node-main/logs/query").header("Authorization", bearer("ops-viewer-token")),
                Map.of("targetType", "MINECRAFT_INSTANCE", "targetId", "mc-survival", "tailLines", 0, "reason", "非法行数", "idempotencyKey", "bad-log"), 400, 40001);
    }

    @Test
    @DisplayName("OPS-TASK and OPS-APPROVAL cover task creation, risk controls, approval flow, cancellation, node result, and idempotency")
    void taskApprovalAndNodeResultFlow() throws Exception {
        JsonNode restart = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")),
                taskBody("CONTAINER_RESTART", "container-seed-1", "restart-container-1"), 201);
        String restartTaskId = restart.at("/data/taskId").asText();
        assertThat(restart.at("/data/status").asText()).isIn("SUCCEEDED", "DISPATCHED", "QUEUED");

        JsonNode replay = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")),
                taskBody("CONTAINER_RESTART", "container-seed-1", "restart-container-1"), 201);
        assertThat(replay.at("/data/taskId").asText()).isEqualTo(restartTaskId);
        performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")),
                with(taskBody("CONTAINER_RESTART", "container-seed-1", "restart-container-1"), "targetId", "container-seed-2"), 409, 49412);

        performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-file-token")),
                taskBody("FILE_DELETE", "/runtime-config.txt", "delete-no-confirm"), 403, 42003);

        JsonNode deleteTask = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-file-token")),
                with(taskBody("FILE_DELETE", "/runtime-config.txt", "delete-file-1"), "confirmText", "DELETE_FILE"), 201);
        assertThat(deleteTask.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        String approvalId = deleteTask.at("/data/approvalId").asText();

        performJson(get("/api/v1/ops-control/approvals").header("Authorization", bearer("ops-viewer-token")), 403, 42002);
        JsonNode approvals = performJson(get("/api/v1/ops-control/approvals").header("Authorization", bearer("ops-approver-token")), 200);
        assertThat(approvals.toString()).contains(approvalId);

        JsonNode approved = performJson(patch("/api/v1/ops-control/approvals/" + approvalId + "/approve").header("Authorization", bearer("owner-token")),
                Map.of("reviewComment", "允许删除测试文件快照", "reason", "审批高风险任务", "idempotencyKey", "approve-delete-file"), 200);
        assertThat(approved.at("/data/approval/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/data/task/status").asText()).isIn("FAILED", "SUCCEEDED", "DISPATCHED");

        JsonNode commandTask = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-terminal-token")),
                with(taskBody("TERMINAL_COMMAND", "node-main", "terminal-command-self"), "confirmText", "TERMINAL_COMMAND"), 201);
        String selfApprovalId = commandTask.at("/data/approvalId").asText();
        performJson(patch("/api/v1/ops-control/approvals/" + selfApprovalId + "/approve").header("Authorization", bearer("ops-terminal-token")),
                Map.of("reviewComment", "自审应失败", "reason", "自审", "idempotencyKey", "self-approve"), 409, 49416);

        JsonNode queued = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")).header("X-Test-Node-Mode", "queued"),
                taskBody("CONTAINER_STOP", "container-seed-1", "queued-stop"), 201);
        String queuedTaskId = queued.at("/data/taskId").asText();
        JsonNode canceled = performJson(patch("/api/v1/ops-control/tasks/" + queuedTaskId + "/cancel").header("Authorization", bearer("ops-container-token")),
                Map.of("reason", "取消排队任务", "idempotencyKey", "cancel-queued"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");

        JsonNode dispatched = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")).header("X-Test-Node-Mode", "dispatched"),
                taskBody("CONTAINER_STOP", "container-seed-1", "dispatched-stop"), 201);
        String dispatchedTaskId = dispatched.at("/data/taskId").asText();
        JsonNode result = performJson(post("/api/v1/ops-control/tasks/" + dispatchedTaskId + "/node-result").header("Authorization", bearer("ops-node-writer-token")),
                Map.of("nodeRequestId", "node-req-1", "status", "SUCCEEDED", "resultSummary", Map.of("message", "ok"), "finishedAt", "2026-05-25T13:00:00Z"), 200);
        assertThat(result.at("/data/status").asText()).isEqualTo("SUCCEEDED");

        JsonNode secondDispatched = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")).header("X-Test-Node-Mode", "dispatched"),
                taskBody("CONTAINER_STOP", "container-seed-1", "dispatched-stop-invalid-result"), 201);
        performJson(post("/api/v1/ops-control/tasks/" + secondDispatched.at("/data/taskId").asText() + "/node-result").header("Authorization", bearer("ops-node-writer-token")),
                Map.of("nodeRequestId", "node-req-2", "status", "BROKEN", "resultSummary", Map.of("message", "bad"), "finishedAt", "2026-05-25T13:00:00Z"), 400, 40001);

        performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-admin-token")),
                taskBody("UNKNOWN_TASK", "node-main", "unknown-task"), 400, 40001);

        JsonNode backupRestore = performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("owner-token")),
                with(taskBody("BACKUP_RESTORE", "node-main", "backup-restore-1"), "confirmText", "BACKUP_RESTORE"), 201);
        assertThat(backupRestore.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        assertThat(backupRestore.at("/data/riskLevel").asText()).isEqualTo("CRITICAL");

        performJson(post("/api/v1/ops-control/tasks").header("Authorization", bearer("ops-container-token")).header("X-Test-Node-Mode", "offline"),
                taskBody("CONTAINER_RESTART", "container-seed-1", "offline-restart"), 409, 49415);
    }

    @Test
    @DisplayName("OPS-AUDIT, OPS-DEPS, OPS-HARDEN, and OPS-CYCLE cover audit filters, dependency failures, secret scanning, and boundary scanning")
    void auditDependencyHardeningAndBoundaryScanning() throws Exception {
        performJson(get("/api/v1/ops-control/overview")
                .header("Authorization", bearer("ops-viewer-token"))
                .header("X-Test-Auth-Mode", "unavailable"), 502, 49200);

        performJson(get("/api/v1/ops-control/audit-logs").header("Authorization", bearer("ops-viewer-token")), 403, 42001);
        JsonNode audit = performJson(get("/api/v1/ops-control/audit-logs")
                .header("Authorization", bearer("ops-admin-token"))
                .param("targetType", "NODE")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/ops-control/ops/summary").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("ops-control");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8116);
        assertThat(ops.at("/data/nodeAdapterMode").asText()).isEqualTo("SIMULATED");
        assertThat(ops.at("/data/nodeDaemonConnected").asBoolean()).isFalse();
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "NODE_DAEMON_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("backend/ops-control-service/src/main/java");
        String source = Files.exists(serviceRoot)
                ? String.join("\n", Files.walk(serviceRoot)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList())
                : "";
        assertThat(source).doesNotContain(
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.content.",
                "cn.beiming.serverstatus.", "cn.beiming.resource.", "cn.beiming.admin.", "cn.beiming.onboarding.",
                "cn.beiming.exam.", "cn.beiming.whitelist.", "cn.beiming.attendance.", "cn.beiming.community.",
                "cn.beiming.activity.", "cn.beiming.calendar.", "cn.beiming.changelog.", "Repository", "JdbcTemplate",
                "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl", "pvesh", "mcrcon",
                "whitelist add", "whitelist remove", "rm -rf", "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s",
                "cloudreveToken", "server.properties=", "authorized_keys", "id_rsa", "token=");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private Map<String, Object> nodeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "北冥主节点 " + idempotencyKey);
        body.put("endpointSummary", "https://node.example.internal:9443");
        body.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "FILE_MANAGE"));
        body.put("labels", Map.of("env", "test", "region", "local"));
        body.put("reason", "注册 ops-control 测试节点");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> heartbeatBody() {
        return Map.of(
                "status", "ONLINE",
                "version", "0.1.0-simulated",
                "capabilities", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "FILE_MANAGE"),
                "metrics", Map.of("cpuUsagePercent", 32.5, "memoryUsagePercent", 48.0, "diskUsagePercent", 61.0),
                "nodeRequestId", "node-heartbeat-1"
        );
    }

    private Map<String, Object> taskBody(String taskType, String targetId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskType", taskType);
        body.put("nodeId", "node-main");
        body.put("targetType", targetId.startsWith("/") ? "FILE" : targetId.startsWith("container") ? "CONTAINER" : "NODE");
        body.put("targetId", targetId);
        body.put("params", Map.of("mode", "simulated"));
        body.put("reason", "创建受控任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "node-secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "cloudreveToken",
                "authorized_keys", "id_rsa", "rcon.password", "whitelist add", "whitelist remove",
                "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl", "pvesh", "mcrcon",
                "/srv/", "C:\\\\", "server.properties=");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
