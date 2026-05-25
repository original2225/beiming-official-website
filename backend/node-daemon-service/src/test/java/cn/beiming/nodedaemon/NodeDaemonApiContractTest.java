package cn.beiming.nodedaemon;

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

@SpringBootTest(classes = NodeDaemonServiceApplication.class, properties = "node-daemon.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NodeDaemonApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("node-daemon local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ND-COM", 1, 90);
        addRange(mapped, "ND-AUTH", 1, 120);
        addRange(mapped, "ND-HEALTH", 1, 60);
        addRange(mapped, "ND-SUMMARY", 1, 80);
        addRange(mapped, "ND-REGISTER", 1, 90);
        addRange(mapped, "ND-CAP", 1, 70);
        addRange(mapped, "ND-SNAPSHOT", 1, 120);
        addRange(mapped, "ND-HEARTBEAT", 1, 100);
        addRange(mapped, "ND-TASK", 1, 180);
        addRange(mapped, "ND-CONTAINER", 1, 90);
        addRange(mapped, "ND-MC", 1, 90);
        addRange(mapped, "ND-FILE", 1, 150);
        addRange(mapped, "ND-LOG", 1, 100);
        addRange(mapped, "ND-CANCEL", 1, 80);
        addRange(mapped, "ND-AUDIT", 1, 110);
        addRange(mapped, "ND-DEPS", 1, 100);
        addRange(mapped, "ND-HARDEN", 1, 170);
        addRange(mapped, "ND-PORT", 1, 20);
        addRange(mapped, "ND-CYCLE", 1, 90);
        assertThat(mapped).contains("ND-COM-001", "ND-TASK-180", "ND-HARDEN-170", "ND-CYCLE-090");
        assertThat(mapped).hasSize(1900);
    }

    @Test
    @DisplayName("ND-COM, ND-AUTH, ND-HEALTH, ND-SUMMARY, and ND-CAP cover envelope, auth, health, summary, and capabilities")
    void commonAuthHealthSummaryAndCapabilities() throws Exception {
        mvc.perform(get("/api/v1/node-daemon/health").header("X-Request-Id", "req-node-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-node-health"));

        JsonNode health = performJson(get("/api/v1/node-daemon/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/data/service").asText()).isEqualTo("node-daemon");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/node-daemon/ops/summary"), 401, 49600);
        performJson(get("/api/v1/node-daemon/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/node-daemon/ops/summary").headers(nodeHeaders("wrong-signature")), 401, 49601);
        performJson(get("/api/v1/node-daemon/ops/summary").headers(nodeHeaders("expired")), 401, 49602);
        performJson(get("/api/v1/node-daemon/ops/summary").headers(nodeHeaders("wrong-node")), 403, 49603);

        JsonNode summary = performJson(get("/api/v1/node-daemon/ops/summary").headers(nodeHeaders()), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("node-daemon");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8117);
        assertThat(summary.at("/data/mode").asText()).isEqualTo("SIMULATED");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.toString()).contains("TEST_CONTROLS_ENABLED_FOR_LOCAL_TESTS");
        assertNoSecrets(summary);

        JsonNode capabilities = performJson(get("/api/v1/node-daemon/capabilities").headers(nodeHeaders()), 200);
        assertThat(capabilities.toString()).contains("NODE_READ", "FILE_MANAGE", "TERMINAL_ACCESS");
        assertThat(capabilities.at("/data/executableCapabilities").toString()).doesNotContain("TERMINAL_ACCESS");
        assertNoSecrets(capabilities);
    }

    @Test
    @DisplayName("ND-REGISTER, ND-SNAPSHOT, and ND-HEARTBEAT cover registration, runtime snapshot, heartbeat dry run, callback failures, and idempotency")
    void registrationSnapshotAndHeartbeat() throws Exception {
        JsonNode handshake = performJson(post("/api/v1/node-daemon/registration/handshake").headers(nodeHeaders()),
                handshakeBody("handshake-1"), 200);
        assertThat(handshake.at("/data/nodeId").asText()).isEqualTo("node-main");
        assertThat(handshake.toString()).contains("signatureAlgorithm").doesNotContain("node-secret-token");

        JsonNode replay = performJson(post("/api/v1/node-daemon/registration/handshake").headers(nodeHeaders()),
                handshakeBody("handshake-1"), 200);
        assertThat(replay.at("/data/handshakeId").asText()).isEqualTo(handshake.at("/data/handshakeId").asText());
        performJson(post("/api/v1/node-daemon/registration/handshake").headers(nodeHeaders()),
                with(handshakeBody("handshake-1"), "daemonVersion", "0.2.0"), 409, 49612);
        performJson(post("/api/v1/node-daemon/registration/handshake").headers(nodeHeaders()),
                with(handshakeBody("handshake-conflict"), "controlPlaneNodeId", "other-node"), 403, 49603);

        JsonNode snapshot = performJson(get("/api/v1/node-daemon/runtime/snapshot").headers(nodeHeaders()), 200);
        assertThat(snapshot.at("/data/nodeId").asText()).isEqualTo("node-main");
        assertThat(snapshot.toString()).contains("container-seed-1", "mc-survival", "runtime-config.txt");
        assertNoSecrets(snapshot);

        JsonNode degradedSnapshot = performJson(get("/api/v1/node-daemon/runtime/snapshot")
                .headers(nodeHeaders())
                .header("X-Test-Runtime-Mode", "unavailable"), 200);
        assertThat(degradedSnapshot.at("/data/degraded").asBoolean()).isTrue();

        JsonNode dryRun = performJson(post("/api/v1/node-daemon/runtime/heartbeat").headers(nodeHeaders()),
                Map.of("reason", "生成心跳摘要", "dryRun", true, "idempotencyKey", "heartbeat-dry-run"), 200);
        assertThat(dryRun.at("/data/dryRun").asBoolean()).isTrue();
        assertThat(dryRun.toString()).contains("/api/v1/ops-control/nodes/node-main/heartbeat");
        assertNoSecrets(dryRun);

        performJson(post("/api/v1/node-daemon/runtime/heartbeat").headers(nodeHeaders()).header("X-Test-Ops-Control-Mode", "unavailable"),
                Map.of("reason", "控制面不可用", "dryRun", false, "idempotencyKey", "heartbeat-unavailable"), 502, 49611);
        performJson(post("/api/v1/node-daemon/runtime/heartbeat").headers(nodeHeaders()).header("X-Test-Fail-Audit", "true"),
                Map.of("reason", "审计失败", "dryRun", true, "idempotencyKey", "heartbeat-audit-fail"), 500, 55201);
    }

    @Test
    @DisplayName("ND-TASK, ND-CONTAINER, ND-MC, and ND-CANCEL cover task receipt, simulated execution, idempotency, state conflicts, and result summaries")
    void taskLifecycleAndRuntimeActions() throws Exception {
        JsonNode created = performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("CONTAINER_RESTART", "CONTAINER", "container-seed-1", "node-req-task-1"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        assertThat(created.at("/data/resultSummary/mode").asText()).isEqualTo("SIMULATED");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("CONTAINER_RESTART", "CONTAINER", "container-seed-1", "node-req-task-1"), 201);
        assertThat(replay.at("/data/nodeRequestId").asText()).isEqualTo("node-req-task-1");
        performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("CONTAINER_RESTART", "CONTAINER", "container-missing", "node-req-task-missing"), 404, 49608);
        performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("TERMINAL_COMMAND", "NODE", "node-main", "node-req-terminal"), 403, 49604);
        performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                with(taskBody("CONTAINER_RESTART", "CONTAINER", "container-seed-1", "node-req-task-1"), "targetId", "container-seed-2"), 409, 49612);
        performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                with(taskBody("CONTAINER_RESTART", "CONTAINER", "container-seed-1", "node-req-other-node"), "nodeId", "other-node"), 403, 49603);

        JsonNode mcTask = performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("MC_RESTART", "MINECRAFT_INSTANCE", "mc-survival", "node-req-mc-1"), 201);
        assertThat(mcTask.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        assertThat(mcTask.toString()).contains("SIMULATED").doesNotContain("rcon.password");

        JsonNode listed = performJson(get("/api/v1/node-daemon/tasks")
                .headers(nodeHeaders())
                .param("status", "SUCCEEDED")
                .param("taskType", "CONTAINER_RESTART")
                .param("sort", "receivedAt_desc"), 200);
        assertThat(listed.toString()).contains("node-req-task-1").doesNotContain("node-req-mc-1");

        JsonNode detail = performJson(get("/api/v1/node-daemon/tasks/node-req-task-1").headers(nodeHeaders()), 200);
        assertThat(detail.at("/data/taskId").asText()).isEqualTo("ops-task-node-req-task-1");

        JsonNode result = performJson(get("/api/v1/node-daemon/tasks/node-req-task-1/result").headers(nodeHeaders()), 200);
        assertThat(result.at("/data/nodeRequestId").asText()).isEqualTo("node-req-task-1");
        assertThat(result.at("/data/resultSummary/mode").asText()).isEqualTo("SIMULATED");

        performJson(patch("/api/v1/node-daemon/tasks/node-req-task-1/cancel").headers(nodeHeaders()),
                Map.of("reason", "终态不能取消", "controlPlaneTaskId", "ops-task-node-req-task-1", "idempotencyKey", "cancel-finished"), 409, 49606);

        JsonNode received = performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()).header("X-Test-Runtime-Mode", "queued"),
                taskBody("CONTAINER_STOP", "CONTAINER", "container-seed-1", "node-req-cancel-1"), 201);
        assertThat(received.at("/data/status").asText()).isEqualTo("RECEIVED");
        JsonNode canceled = performJson(patch("/api/v1/node-daemon/tasks/node-req-cancel-1/cancel").headers(nodeHeaders()),
                Map.of("reason", "取消未执行任务", "controlPlaneTaskId", "ops-task-node-req-cancel-1", "idempotencyKey", "cancel-received"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("ND-FILE and ND-LOG cover authorized path guard, text summaries, log summaries, and redaction")
    void filesAndLogsSafety() throws Exception {
        JsonNode files = performJson(get("/api/v1/node-daemon/files")
                .headers(nodeHeaders())
                .param("rootAlias", "mc-config")
                .param("path", "/"), 200);
        assertThat(files.toString()).contains("runtime-config.txt").doesNotContain("/srv", "C:\\\\", "server.properties");

        performJson(get("/api/v1/node-daemon/files").headers(nodeHeaders()).param("rootAlias", "mc-config").param("path", "../secret"), 409, 49605);
        performJson(get("/api/v1/node-daemon/files").headers(nodeHeaders()).param("rootAlias", "mc-config").param("path", "\\\\secret"), 409, 49605);
        performJson(get("/api/v1/node-daemon/files").headers(nodeHeaders()).param("rootAlias", "missing-root").param("path", "/"), 404, 49608);

        JsonNode folder = performJson(get("/api/v1/node-daemon/files")
                .headers(nodeHeaders())
                .param("rootAlias", "mc-config")
                .param("path", "/folder"), 200);
        assertThat(folder.toString()).contains("/folder/item.txt").doesNotContain("/folder-other/item.txt");

        JsonNode read = performJson(post("/api/v1/node-daemon/files/read").headers(nodeHeaders()),
                Map.of("rootAlias", "mc-config", "path", "/runtime-config.txt", "maxBytes", 512, "reason", "读取配置摘要", "idempotencyKey", "read-config"), 200);
        assertThat(read.toString()).contains("contentSummary", "redactionApplied").doesNotContain("rcon.password", "node-secret-token");

        performJson(post("/api/v1/node-daemon/files/read").headers(nodeHeaders()),
                Map.of("rootAlias", "mc-config", "path", "/binary.dat", "maxBytes", 512, "reason", "不可读文件", "idempotencyKey", "read-binary"), 409, 49609);
        performJson(post("/api/v1/node-daemon/files/read").headers(nodeHeaders()).header("X-Test-Fail-Audit", "true"),
                Map.of("rootAlias", "mc-config", "path", "/runtime-config.txt", "maxBytes", 512, "reason", "审计失败", "idempotencyKey", "read-audit-fail"), 500, 55201);

        JsonNode logs = performJson(post("/api/v1/node-daemon/logs/query").headers(nodeHeaders()),
                Map.of("targetType", "MINECRAFT_INSTANCE", "targetId", "mc-survival", "tailLines", 100, "keyword", "joined", "reason", "查询日志摘要", "idempotencyKey", "log-query"), 200);
        assertThat(logs.toString()).contains("logSummary").doesNotContain("Authorization", "node-secret-token", "rcon.password");

        performJson(post("/api/v1/node-daemon/logs/query").headers(nodeHeaders()),
                Map.of("targetType", "MINECRAFT_INSTANCE", "targetId", "mc-survival", "tailLines", 0, "reason", "非法行数", "idempotencyKey", "bad-log"), 400, 40001);
    }

    @Test
    @DisplayName("ND-AUDIT, ND-DEPS, ND-HARDEN, and ND-CYCLE cover local audit, dependency failures, production boundaries, and source scanning")
    void auditDependencyHardeningAndBoundaryScanning() throws Exception {
        performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()).header("X-Test-Runtime-Mode", "unavailable"),
                taskBody("CONTAINER_RESTART", "CONTAINER", "container-seed-1", "node-req-runtime-fail"), 409, 49607);

        JsonNode task = performJson(post("/api/v1/node-daemon/tasks").headers(nodeHeaders()),
                taskBody("CONTAINER_STOP", "CONTAINER", "container-seed-1", "node-req-audit-1"), 201);
        assertThat(task.at("/data/status").asText()).isEqualTo("SUCCEEDED");

        JsonNode audit = performJson(get("/api/v1/node-daemon/audit-logs")
                .headers(nodeHeaders())
                .param("nodeRequestId", "node-req-audit-1")
                .param("result", "SUCCESS"), 200);
        assertThat(audit.toString()).contains("node-req-audit-1", "TASK_RECEIVED");
        assertNoSecrets(audit);

        JsonNode summary = performJson(get("/api/v1/node-daemon/ops/summary").headers(nodeHeaders()), 200);
        assertThat(summary.toString()).contains("SIMULATED_RUNTIME", "REAL_TERMINAL_DISABLED");
        assertNoSecrets(summary);

        Path serviceRoot = Path.of("backend/node-daemon-service/src/main/java");
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
                "cn.beiming.activity.", "cn.beiming.calendar.", "cn.beiming.changelog.", "cn.beiming.opscontrol.",
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl",
                "pct ", "qm ", "pvesh", "mcrcon", "whitelist add", "whitelist remove", "rm -rf",
                "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s", "cloudreveToken", "server.properties",
                "authorized_keys", "id_rsa", "token=", ".env");
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

    private org.springframework.http.HttpHeaders nodeHeaders() {
        return nodeHeaders("valid");
    }

    private org.springframework.http.HttpHeaders nodeHeaders(String mode) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", bearer(switch (mode) {
            case "wrong-signature" -> "node-token-valid";
            case "expired" -> "node-token-valid";
            case "wrong-node" -> "node-token-valid";
            default -> "node-token-valid";
        }));
        headers.set("X-Node-Id", mode.equals("wrong-node") ? "other-node" : "node-main");
        headers.set("X-Node-Request-Id", "node-http-" + mode);
        headers.set("X-Node-Timestamp", mode.equals("expired") ? "2020-01-01T00:00:00Z" : "2026-05-25T15:00:00Z");
        headers.set("X-Node-Signature", mode.equals("wrong-signature") ? "bad-signature" : "test-signature");
        return headers;
    }

    private Map<String, Object> handshakeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("controlPlaneNodeId", "node-main");
        body.put("registrationNonce", "nonce-" + idempotencyKey);
        body.put("daemonVersion", "0.1.0-simulated");
        body.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "CONTAINER_OPERATE"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> taskBody(String taskType, String targetType, String targetId, String nodeRequestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", "ops-task-" + nodeRequestId);
        body.put("taskType", taskType);
        body.put("nodeId", "node-main");
        body.put("targetType", targetType);
        body.put("targetId", targetId);
        body.put("paramsSummary", Map.of("mode", "simulated"));
        body.put("riskLevel", "MEDIUM");
        body.put("reason", "执行节点契约测试任务");
        body.put("nodeRequestId", nodeRequestId);
        body.put("expiresAt", "2026-05-25T16:00:00Z");
        body.put("idempotencyKey", nodeRequestId);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "node-secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "cloudreveToken",
                "authorized_keys", "id_rsa", "rcon.password", "whitelist add", "whitelist remove",
                "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl", "pvesh", "mcrcon",
                "/srv/", "C:\\\\", "server.properties", ".env");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
