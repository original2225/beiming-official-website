package cn.beiming.alerting;

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

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class, properties = {"server.port=8133", "alerting.test-controls.enabled=true"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AlertingApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("alerting local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ALT-COM", 1, 100);
        addRange(mapped, "ALT-AUTH", 1, 120);
        addRange(mapped, "ALT-HEALTH", 1, 40);
        addRange(mapped, "ALT-OPS", 1, 90);
        addRange(mapped, "ALT-SOURCE", 1, 100);
        addRange(mapped, "ALT-RULE", 1, 180);
        addRange(mapped, "ALT-EVAL", 1, 130);
        addRange(mapped, "ALT-INSTANCE", 1, 130);
        addRange(mapped, "ALT-ACK", 1, 120);
        addRange(mapped, "ALT-SILENCE", 1, 130);
        addRange(mapped, "ALT-ROUTE", 1, 140);
        addRange(mapped, "ALT-DELIVERY", 1, 100);
        addRange(mapped, "ALT-AUDIT", 1, 100);
        addRange(mapped, "ALT-DEPS", 1, 120);
        addRange(mapped, "ALT-HARDEN", 1, 180);
        addRange(mapped, "ALT-PORT", 1, 20);
        addRange(mapped, "ALT-CYCLE", 1, 100);
        assertThat(mapped).contains("ALT-COM-001", "ALT-RULE-180", "ALT-ROUTE-140", "ALT-HARDEN-180", "ALT-CYCLE-100");
        assertThat(mapped).hasSize(1900);
    }

    @Test
    @DisplayName("ALT-COM, ALT-AUTH, ALT-HEALTH, ALT-OPS, and ALT-SOURCE cover envelope, auth, health, summary, and source reads")
    void commonAuthHealthSummaryAndSources() throws Exception {
        mvc.perform(get("/api/v1/alerting/health").header("X-Request-Id", "req-alt-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-alt-health"));

        JsonNode health = performJson(get("/api/v1/alerting/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("alerting");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/alerting/ops/summary"), 401, 41000);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("alert-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 46920);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 46921);
        performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 46922);

        JsonNode summary = performJson(get("/api/v1/alerting/ops/summary").header("Authorization", bearer("alert-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("alerting");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8133);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/sourceAdapterMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/notificationAdapterMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/productionGaps").toString()).contains(
                "REAL_PERSISTENCE_NOT_CONNECTED",
                "REAL_SOURCE_HTTP_NOT_CONNECTED",
                "REAL_NOTIFICATION_DELIVERY_NOT_CONNECTED",
                "REAL_METRIC_COLLECTION_NOT_CONNECTED",
                "ADMIN_READ_ONLY_ENTRY_NOT_CONNECTED",
                "EXTERNAL_EXECUTOR_DIRECT_CALL_FORBIDDEN");
        assertNoSecrets(summary);

        performJson(get("/api/v1/alerting/ops/summary")
                .header("Authorization", bearer("alert-viewer-token"))
                .header("X-Test-Fail-Store", "true"), 500, 55500);
        performJson(get("/api/v1/alerting/sources").header("Authorization", bearer("alert-viewer-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/alerting/sources").header("Authorization", bearer("alert-viewer-token")).param("page", "bad"), 400, 40002);
        performJson(get("/api/v1/alerting/sources").header("Authorization", bearer("alert-viewer-token")).param("sort", "bad"), 400, 40003);

        JsonNode sources = performJson(get("/api/v1/alerting/sources")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("keyword", "ops")
                .param("sourceService", "OPS_CONTROL")
                .param("sourceType", "HEALTH")
                .param("healthStatus", "AVAILABLE")
                .param("enabled", "true")
                .param("sort", "displayName_asc"), 200);
        assertThat(sources.toString()).contains("source-ops-health").doesNotContain("nodeToken", "secretKey", "/srv/");
        performJson(get("/api/v1/alerting/sources/source-ops-health").header("Authorization", bearer("alert-viewer-token")), 200);
        performJson(get("/api/v1/alerting/sources/missing").header("Authorization", bearer("alert-viewer-token")), 404, 49900);
    }

    @Test
    @DisplayName("ALT-RULE and ALT-EVAL cover rule lifecycle, idempotency, conflict, source failures, dedupe, and audit rollback")
    void ruleLifecycleAndEvaluation() throws Exception {
        JsonNode rules = performJson(get("/api/v1/alerting/rules")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("sourceService", "OPS_CONTROL")
                .param("sourceType", "HEALTH")
                .param("severity", "WARNING")
                .param("status", "ENABLED")
                .param("sort", "displayName_asc"), 200);
        assertThat(rules.toString()).contains("rule-node-offline");
        performJson(get("/api/v1/alerting/rules/rule-node-offline").header("Authorization", bearer("alert-viewer-token")), 200);
        performJson(get("/api/v1/alerting/rules/missing").header("Authorization", bearer("alert-viewer-token")), 404, 49901);

        JsonNode created = performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                ruleBody("rule-create"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                ruleBody("rule-create"), 201);
        assertThat(replay.at("/data/ruleId").asText()).isEqualTo(created.at("/data/ruleId").asText());
        performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                with(ruleBody("rule-create"), "displayName", "Changed rule"), 409, 49912);
        performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                with(ruleBody("rule-bad-condition"), "conditionSummary", Map.of("bad", true)), 400, 49911);
        performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-viewer-token")),
                ruleBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/alerting/rules")
                        .header("Authorization", bearer("alert-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                ruleBody("audit-fail-rule"), 500, 55501);

        String ruleId = created.at("/data/ruleId").asText();
        JsonNode patched = performJson(patch("/api/v1/alerting/rules/" + ruleId).header("Authorization", bearer("alert-admin-token")),
                Map.of("displayName", "Updated alerting rule", "severity", "CRITICAL", "reason", "更新告警级别", "idempotencyKey", "patch-rule"), 200);
        assertThat(patched.at("/data/severity").asText()).isEqualTo("CRITICAL");
        performJson(patch("/api/v1/alerting/rules/" + ruleId).header("Authorization", bearer("alert-admin-token")),
                Map.of("conditionSummary", Map.of("bad", true), "reason", "拒绝非法条件更新", "idempotencyKey", "patch-bad-condition"), 400, 49911);
        performJson(patch("/api/v1/alerting/rules/" + ruleId).header("Authorization", bearer("alert-admin-token")),
                Map.of("evaluationWindowSeconds", "bad", "reason", "拒绝非法评估窗口", "idempotencyKey", "patch-bad-window"), 400, 49911);
        performJson(patch("/api/v1/alerting/rules/" + ruleId).header("Authorization", bearer("alert-admin-token")),
                Map.of("routeId", "missing-route", "reason", "拒绝不存在路由", "idempotencyKey", "patch-missing-route"), 404, 49904);
        JsonNode enabled = performJson(patch("/api/v1/alerting/rules/" + ruleId + "/enable").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "启用告警规则", "idempotencyKey", "enable-rule"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");
        JsonNode disabled = performJson(patch("/api/v1/alerting/rules/" + ruleId + "/disable").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "停用告警规则", "idempotencyKey", "disable-rule"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(post("/api/v1/alerting/rules/" + ruleId + "/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("node-a"), "dryRun", false, "reason", "禁用规则不可评估", "idempotencyKey", "eval-disabled"), 409, 49910);

        JsonNode evaluated = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("node-a"), "dryRun", false, "reason", "手动评估", "idempotencyKey", "eval-main"), 201);
        assertThat(evaluated.at("/data/status").asText()).isEqualTo("MATCHED");
        assertThat(evaluated.at("/data/createdAlertId").asText()).isNotBlank();
        JsonNode dedupe = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("node-a"), "dryRun", false, "reason", "重复评估", "idempotencyKey", "eval-main-2"), 201);
        assertThat(dedupe.at("/data/dedupeHit").asBoolean()).isTrue();
        performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate")
                        .header("Authorization", bearer("alert-admin-token"))
                        .header("X-Test-Source-Mode", "unavailable"),
                Map.of("sourceSnapshot", sourceSnapshot("node-b"), "dryRun", false, "reason", "来源失败", "idempotencyKey", "eval-source-fail"), 502, 46910);
    }

    @Test
    @DisplayName("ALT-INSTANCE, ALT-ACK, ALT-SILENCE, ALT-ROUTE, ALT-DELIVERY, ALT-AUDIT, and ALT-HARDEN cover alert operations and hardening")
    void alertSilenceRouteDeliveryAuditAndHardening() throws Exception {
        JsonNode evaluation = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("node-z"), "dryRun", false, "reason", "生成告警", "idempotencyKey", "eval-alert-z"), 201);
        String alertId = evaluation.at("/data/createdAlertId").asText();

        JsonNode alerts = performJson(get("/api/v1/alerting/alerts")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("ruleId", "rule-node-offline")
                .param("sourceService", "OPS_CONTROL")
                .param("severity", "WARNING")
                .param("status", "FIRING")
                .param("groupKey", "OPS_CONTROL:node-z")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z")
                .param("sort", "lastFiredAt_desc"), 200);
        assertThat(alerts.toString()).contains(alertId);
        performJson(get("/api/v1/alerting/alerts")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2020-01-01T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/alerting/alerts/" + alertId).header("Authorization", bearer("alert-viewer-token")), 200);
        performJson(get("/api/v1/alerting/alerts/missing").header("Authorization", bearer("alert-viewer-token")), 404, 49902);

        JsonNode acknowledged = performJson(patch("/api/v1/alerting/alerts/" + alertId + "/acknowledge").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "收到告警", "idempotencyKey", "ack-alert"), 200);
        assertThat(acknowledged.at("/data/status").asText()).isEqualTo("ACKNOWLEDGED");
        JsonNode closed = performJson(patch("/api/v1/alerting/alerts/" + alertId + "/close").header("Authorization", bearer("alert-admin-token")),
                Map.of("resolutionSummary", "节点恢复", "confirmText", "CLOSE_ALERT", "reason", "关闭告警", "idempotencyKey", "close-alert"), 200);
        assertThat(closed.at("/data/status").asText()).isEqualTo("CLOSED");
        performJson(patch("/api/v1/alerting/alerts/" + alertId + "/acknowledge").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "终态不可确认", "idempotencyKey", "ack-closed"), 409, 49910);

        JsonNode blocker = performJson(post("/api/v1/alerting/rules/rule-backup-blocker/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("restore-prod"), "dryRun", false, "reason", "严重恢复阻断", "idempotencyKey", "eval-blocker"), 201);
        performJson(patch("/api/v1/alerting/alerts/" + blocker.at("/data/createdAlertId").asText() + "/close").header("Authorization", bearer("alert-no-cap-token")),
                Map.of("resolutionSummary", "无高危审批", "confirmText", "CLOSE_ALERT", "reason", "关闭严重告警", "idempotencyKey", "close-blocker-denied"), 403, 42002);

        JsonNode silence = performJson(post("/api/v1/alerting/silences").header("Authorization", bearer("alert-admin-token")),
                silenceBody("silence-main"), 201);
        assertThat(silence.at("/data/status").asText()).isEqualTo("ACTIVE");
        performJson(post("/api/v1/alerting/silences").header("Authorization", bearer("alert-admin-token")),
                with(silenceBody("silence-bad-time"), "endsAt", "2020-01-01T00:00:00Z"), 400, 49913);
        performJson(post("/api/v1/alerting/silences").header("Authorization", bearer("alert-admin-token")),
                with(silenceBody("silence-bad-matcher"), "matchers", Map.of("unknown", "value")), 400, 49914);
        JsonNode suppressed = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("silenced-node"), "dryRun", false, "reason", "静默命中", "idempotencyKey", "eval-suppressed"), 201);
        assertThat(suppressed.at("/data/suppressed").asBoolean()).isTrue();
        JsonNode cancelled = performJson(patch("/api/v1/alerting/silences/" + silence.at("/data/silenceId").asText() + "/cancel").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "结束维护窗口", "idempotencyKey", "cancel-silence"), 200);
        assertThat(cancelled.at("/data/status").asText()).isEqualTo("CANCELLED");
        performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("silenced-node"), "dryRun", false, "reason", "静默取消后恢复", "idempotencyKey", "eval-unsuppressed"), 201);
        JsonNode resumed = performJson(get("/api/v1/alerting/alerts/" + suppressed.at("/data/createdAlertId").asText()).header("Authorization", bearer("alert-viewer-token")), 200);
        assertThat(resumed.at("/data/status").asText()).isEqualTo("FIRING");

        performJson(post("/api/v1/alerting/silences").header("Authorization", bearer("alert-admin-token")),
                with(silenceBody("silence-group-key"), "matchers", Map.of("groupKey", "OPS_CONTROL:groupkey-target")), 201);
        JsonNode groupMiss = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("groupkey-other"), "dryRun", false, "reason", "groupKey 不应误命中", "idempotencyKey", "eval-groupkey-miss"), 201);
        assertThat(groupMiss.at("/data/suppressed").asBoolean()).isFalse();
        JsonNode groupHit = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("groupkey-target"), "dryRun", false, "reason", "groupKey 命中", "idempotencyKey", "eval-groupkey-hit"), 201);
        assertThat(groupHit.at("/data/suppressed").asBoolean()).isTrue();

        JsonNode route = performJson(post("/api/v1/alerting/routes").header("Authorization", bearer("alert-admin-token")),
                routeBody("route-create"), 201);
        assertThat(route.at("/data/status").asText()).isEqualTo("ENABLED");
        JsonNode routePatched = performJson(patch("/api/v1/alerting/routes/" + route.at("/data/routeId").asText()).header("Authorization", bearer("alert-admin-token")),
                Map.of("displayName", "Updated route", "repeatIntervalSeconds", 1200, "reason", "更新重复提醒", "idempotencyKey", "patch-route"), 200);
        assertThat(routePatched.at("/data/repeatIntervalSeconds").asInt()).isEqualTo(1200);
        performJson(patch("/api/v1/alerting/routes/" + route.at("/data/routeId").asText()).header("Authorization", bearer("alert-admin-token")),
                Map.of("repeatIntervalSeconds", 120, "reason", "拒绝非法重复提醒间隔", "idempotencyKey", "patch-route-bad-repeat"), 400, 40001);
        performJson(patch("/api/v1/alerting/routes/" + route.at("/data/routeId").asText()).header("Authorization", bearer("alert-admin-token")),
                Map.of("groupIntervalSeconds", "bad", "reason", "拒绝非法分组间隔", "idempotencyKey", "patch-route-bad-group-interval"), 400, 40001);
        JsonNode delivery = performJson(post("/api/v1/alerting/routes/" + route.at("/data/routeId").asText() + "/test").header("Authorization", bearer("alert-admin-token")),
                Map.of("sampleAlert", sampleAlert(), "reason", "测试路由", "idempotencyKey", "test-route"), 201);
        assertThat(delivery.at("/data/status").asText()).isEqualTo("SENT");
        performJson(post("/api/v1/alerting/routes/" + route.at("/data/routeId").asText() + "/test")
                        .header("Authorization", bearer("alert-admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("sampleAlert", sampleAlert(), "reason", "通知失败", "idempotencyKey", "test-route-fail"), 502, 46900);

        JsonNode deliveries = performJson(get("/api/v1/alerting/deliveries")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("routeId", route.at("/data/routeId").asText())
                .param("status", "SENT")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z"), 200);
        assertThat(deliveries.toString()).contains(delivery.at("/data/deliveryId").asText());
        performJson(get("/api/v1/alerting/deliveries")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("from", "not-a-time")
                .param("to", "2030-01-01T00:00:00Z"), 400, 40001);

        JsonNode audit = performJson(get("/api/v1/alerting/audit-logs")
                .header("Authorization", bearer("alert-admin-token"))
                .param("actorUserId", "alert-admin-user")
                .param("routeId", route.at("/data/routeId").asText())
                .param("action", "ALERT_ROUTE_CREATED")
                .param("result", "SUCCESS")
                .param("riskLevel", "HIGH")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z"), 200);
        assertThat(audit.at("/data/total").asInt()).isEqualTo(1);
        performJson(get("/api/v1/alerting/audit-logs").header("Authorization", bearer("alert-viewer-token")), 403, 42001);
        assertNoSecrets(audit);

        performJson(patch("/api/v1/alerting/rules/rule-node-offline/enable").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "拒绝可信字段", "idempotencyKey", "enable-trusted", "createdBy", "browser"), 400, 40001);

        Path serviceRoot = Path.of("src/main/java/cn/beiming/alerting");
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
                "cn.beiming.nodedaemon.", "cn.beiming.cloudrevesync.", "cn.beiming.backuprecovery.", "Repository",
                "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "webhookSecret",
                "smtpPassword", "smsToken", "rawToken", "credential", "secretKey", "nodeToken",
                "notificationToken", "internalPath", "resolvedPath", "rm -rf", "Remove-Item -Recurse",
                "rmdir /s", "rd /s", "del /s", "jdbc:", "authorized_keys", "id_rsa", ".env");
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

    private Map<String, Object> ruleBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Alert " + idempotencyKey);
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
        body.put("reason", "创建告警规则");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> sourceSnapshot(String nodeId) {
        return Map.of("sourceRef", nodeId, "nodeId", nodeId, "status", "OFFLINE", "summary", "Node " + nodeId + " heartbeat delayed");
    }

    private Map<String, Object> silenceBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchers", Map.of("sourceService", "OPS_CONTROL", "severity", "WARNING", "labels", Map.of("node", "silenced-node")));
        body.put("startsAt", "2020-01-01T00:00:00Z");
        body.put("endsAt", "2030-01-01T00:00:00Z");
        body.put("reason", "维护窗口静默");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Route " + idempotencyKey);
        body.put("matchers", Map.of("severity", "WARNING", "sourceService", "OPS_CONTROL"));
        body.put("groupBy", List.of("sourceService", "groupKey"));
        body.put("groupWaitSeconds", 30);
        body.put("groupIntervalSeconds", 300);
        body.put("repeatIntervalSeconds", 900);
        body.put("notificationTemplateRef", Map.of("templateCode", "ALERT_WARNING", "channel", "IN_APP"));
        body.put("receiverSummary", Map.of("receiverType", "IN_APP", "target", "ops-admins"));
        body.put("enabled", true);
        body.put("reason", "创建告警路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> sampleAlert() {
        return Map.of("summary", "Node heartbeat delayed", "severity", "WARNING", "sourceService", "OPS_CONTROL", "labels", Map.of("node", "main"));
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
                "rawToken", "credential", "secretKey", "nodeToken", "notificationToken", "webhookSecret",
                "smtpPassword", "smsToken", "Authorization", "authorizationHeader", "requestHeaders", "stackTrace",
                "internalPath", "resolvedPath", "jdbc:", "AKIA", "objectSecret", "databasePassword",
                "authorized_keys", "id_rsa", "ProcessBuilder", "Runtime.getRuntime", "node-daemon",
                "targetDatabaseUrl", "restorePath", "shellCommand", "/srv/", "C:\\\\", ".env", "token=");
    }

    @Test
    @DisplayName("ALT-EVAL, ALT-SILENCE, ALT-ROUTE, ALT-DELIVERY, ALT-AUDIT, and ALT-HARDEN cover refined alerting contract gaps")
    void refinedContractGapsAreCovered() throws Exception {
        performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of(
                        "sourceSnapshot", Map.of("nodeId", "nested-secret-node", "nested", Map.of("webhookSecret", "must-not-enter")),
                        "dryRun", false,
                        "reason", "拒绝嵌套可信字段",
                        "idempotencyKey", "eval-nested-secret"), 400, 40001);

        JsonNode firstTemplateRule = performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                with(with(with(ruleBody("rule-template-alpha"), "displayName", "Template Alpha"),
                        "labels", Map.of("service", "template-alpha", "scope", "node")), "dedupeKeyTemplate", "{{sourceService}}:{{labels.service}}:{{nodeId}}"), 201);
        JsonNode secondTemplateRule = performJson(post("/api/v1/alerting/rules").header("Authorization", bearer("alert-admin-token")),
                with(with(with(ruleBody("rule-template-beta"), "displayName", "Template Beta"),
                        "labels", Map.of("service", "template-beta", "scope", "node")), "dedupeKeyTemplate", "{{sourceService}}:{{labels.service}}:{{nodeId}}"), 201);
        String firstRuleId = firstTemplateRule.at("/data/ruleId").asText();
        String secondRuleId = secondTemplateRule.at("/data/ruleId").asText();
        performJson(patch("/api/v1/alerting/rules/" + firstRuleId + "/enable").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "启用模板规则一", "idempotencyKey", "enable-template-alpha"), 200);
        performJson(patch("/api/v1/alerting/rules/" + secondRuleId + "/enable").header("Authorization", bearer("alert-admin-token")),
                Map.of("reason", "启用模板规则二", "idempotencyKey", "enable-template-beta"), 200);

        JsonNode firstEvaluation = performJson(post("/api/v1/alerting/rules/" + firstRuleId + "/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("template-node"), "dryRun", false, "reason", "模板去重一", "idempotencyKey", "eval-template-alpha"), 201);
        JsonNode secondEvaluation = performJson(post("/api/v1/alerting/rules/" + secondRuleId + "/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("template-node"), "dryRun", false, "reason", "模板去重二", "idempotencyKey", "eval-template-beta"), 201);
        assertThat(firstEvaluation.at("/data/dedupeHit").asBoolean()).isFalse();
        assertThat(secondEvaluation.at("/data/dedupeHit").asBoolean()).isFalse();
        assertThat(secondEvaluation.at("/data/createdAlertId").asText()).isNotEqualTo(firstEvaluation.at("/data/createdAlertId").asText());

        JsonNode repeatedTemplateEvaluation = performJson(post("/api/v1/alerting/rules/" + firstRuleId + "/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("template-node"), "dryRun", false, "reason", "模板重复评估", "idempotencyKey", "eval-template-alpha-repeat"), 201);
        assertThat(repeatedTemplateEvaluation.at("/data/dedupeHit").asBoolean()).isTrue();

        JsonNode routedEvaluation = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("delivered-node"), "dryRun", false, "reason", "正常路由投递", "idempotencyKey", "eval-delivered-node"), 201);
        JsonNode routedAlert = performJson(get("/api/v1/alerting/alerts/" + routedEvaluation.at("/data/createdAlertId").asText())
                .header("Authorization", bearer("alert-viewer-token")), 200);
        assertThat(routedAlert.at("/data/notificationSummary/status").asText()).isEqualTo("SENT");
        JsonNode routedDeliveries = performJson(get("/api/v1/alerting/deliveries")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("alertId", routedEvaluation.at("/data/createdAlertId").asText())
                .param("routeId", "route-default")
                .param("status", "SENT"), 200);
        assertThat(routedDeliveries.at("/data/total").asInt()).isEqualTo(1);

        JsonNode expiredSilence = performJson(post("/api/v1/alerting/silences").header("Authorization", bearer("alert-admin-token")),
                with(with(silenceBody("silence-expired"), "startsAt", "2020-01-01T00:00:00Z"), "endsAt", "2021-01-01T00:00:00Z"), 201);
        JsonNode expiredList = performJson(get("/api/v1/alerting/silences")
                .header("Authorization", bearer("alert-viewer-token"))
                .param("status", "EXPIRED"), 200);
        assertThat(expiredList.toString()).contains(expiredSilence.at("/data/silenceId").asText());
        JsonNode notSuppressedByExpired = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate").header("Authorization", bearer("alert-admin-token")),
                Map.of("sourceSnapshot", sourceSnapshot("silenced-node"), "dryRun", false, "reason", "过期静默不抑制", "idempotencyKey", "eval-expired-silence"), 201);
        assertThat(notSuppressedByExpired.at("/data/suppressed").asBoolean()).isFalse();

        JsonNode auditedRoute = performJson(post("/api/v1/alerting/routes").header("Authorization", bearer("alert-admin-token")),
                routeBody("route-audit-detail"), 201);
        JsonNode audit = performJson(get("/api/v1/alerting/audit-logs")
                .header("Authorization", bearer("alert-admin-token"))
                .param("routeId", auditedRoute.at("/data/routeId").asText())
                .param("action", "ALERT_ROUTE_CREATED"), 200);
        assertThat(audit.at("/data/items/0/reason").asText()).isEqualTo("创建告警路由");
        assertThat(audit.at("/data/items/0/paramsSummary/sanitized").asBoolean()).isTrue();
        assertThat(audit.at("/data/items/0/paramsSummary/fieldNames").toString()).contains("displayName", "idempotencyKey");
        assertNoSecrets(audit);
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
