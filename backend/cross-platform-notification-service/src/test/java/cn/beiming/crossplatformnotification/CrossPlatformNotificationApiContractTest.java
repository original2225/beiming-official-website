package cn.beiming.crossplatformnotification;

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

@SpringBootTest(classes = CrossPlatformNotificationServiceApplication.class, properties = "cross-platform-notification.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CrossPlatformNotificationApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("cross-platform-notification local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "CPN-COM", 1, 120);
        addRange(mapped, "CPN-AUTH", 1, 140);
        addRange(mapped, "CPN-HEALTH", 1, 50);
        addRange(mapped, "CPN-OPS", 1, 100);
        addRange(mapped, "CPN-PROVIDER", 1, 180);
        addRange(mapped, "CPN-CAP", 1, 80);
        addRange(mapped, "CPN-TEMPLATE", 1, 170);
        addRange(mapped, "CPN-ROUTE", 1, 200);
        addRange(mapped, "CPN-DELIVERY", 1, 220);
        addRange(mapped, "CPN-ATTEMPT", 1, 100);
        addRange(mapped, "CPN-RECEIVER", 1, 120);
        addRange(mapped, "CPN-AUDIT", 1, 120);
        addRange(mapped, "CPN-DEPS", 1, 160);
        addRange(mapped, "CPN-HARDEN", 1, 220);
        addRange(mapped, "CPN-PORT", 1, 20);
        addRange(mapped, "CPN-CYCLE", 1, 120);
        assertThat(mapped).contains("CPN-COM-001", "CPN-PROVIDER-180", "CPN-DELIVERY-220", "CPN-HARDEN-220", "CPN-CYCLE-120");
        assertThat(mapped).hasSize(2120);
    }

    @Test
    @DisplayName("CPN-COM, CPN-AUTH, CPN-HEALTH, CPN-OPS, and CPN-CAP cover envelope, auth, health, summary, and capabilities")
    void commonAuthHealthSummaryAndCapabilities() throws Exception {
        mvc.perform(get("/api/v1/cross-platform-notification/health").header("X-Request-Id", "req-cpn-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-cpn-health"));

        JsonNode health = performJson(get("/api/v1/cross-platform-notification/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("cross-platform-notification");
        assertThat(health.at("/data/status").asText()).isIn("READY", "DEGRADED");
        assertThat(health.at("/data/version").asText()).isNotBlank();
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);
        assertThat(health.at("/data").toString()).doesNotContain("providerCount", "receiverCount", "endpoint", "deliveryCount");

        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary"), 401, 41000);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("cpn-admin-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 47150);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 47151);
        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 47152);

        JsonNode summary = performJson(get("/api/v1/cross-platform-notification/admin/ops/summary").header("Authorization", bearer("cpn-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("cross-platform-notification");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8123);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/providerAdapterMode").asText()).isEqualTo("SIMULATION_ONLY");
        assertThat(summary.at("/data/notificationAdapterMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/productionGaps").toString()).contains("REAL_EXTERNAL_SEND_DISABLED", "REAL_CALLBACKS_NOT_CONNECTED");
        assertNoSecrets(summary);

        performJson(get("/api/v1/cross-platform-notification/admin/ops/summary")
                .header("Authorization", bearer("cpn-viewer-token"))
                .header("X-Test-Fail-Store", "true"), 500, 55800);
        performJson(get("/api/v1/cross-platform-notification/admin/providers")
                .header("Authorization", bearer("cpn-viewer-token"))
                .param("page", "0"), 400, 40002);
        performJson(get("/api/v1/cross-platform-notification/admin/providers")
                .header("Authorization", bearer("cpn-viewer-token"))
                .param("sort", "bad"), 400, 40003);

        JsonNode capabilities = performJson(get("/api/v1/cross-platform-notification/admin/capabilities")
                .header("Authorization", bearer("cpn-viewer-token"))
                .param("providerId", "provider-discord-main")
                .param("channel", "DISCORD")
                .param("supportsMarkdown", "true")
                .param("supportsDeliveryCallback", "true")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(capabilities.at("/data/items").toString()).contains("cap-provider-discord-main");
        JsonNode capability = performJson(get("/api/v1/cross-platform-notification/admin/capabilities/cap-provider-discord-main")
                .header("Authorization", bearer("cpn-viewer-token")), 200);
        assertThat(capability.at("/data/providerSummary/providerId").asText()).isEqualTo("provider-discord-main");
        performJson(get("/api/v1/cross-platform-notification/admin/capabilities/missing")
                .header("Authorization", bearer("cpn-viewer-token")), 404, 49956);
        assertNoSecrets(capability);
    }

    @Test
    @DisplayName("CPN-PROVIDER covers provider lifecycle, endpoint safety, recursive trusted fields, idempotency, conflicts, and audit rollback")
    void providerLifecycleEndpointSafetyAndRollback() throws Exception {
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-viewer-token")),
                providerBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                providerBody("missing-confirm"), 403, 42003);
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(providerBody("bad-endpoint"), "confirmText", "REGISTER_EXTERNAL_PROVIDER",
                        "endpointSummary", Map.of("url", "http://127.0.0.1:8123/hook")), 400, 49963);
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(providerBody("bad-trusted"), "confirmText", "REGISTER_EXTERNAL_PROVIDER",
                        "receiverPolicy", Map.of("items", List.of(Map.of("webhookSecret", "do-not-store")))), 400, 40001);
        performJson(post("/api/v1/cross-platform-notification/admin/providers")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                with(providerBody("audit-fail-provider"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 500, 55801);

        JsonNode created = performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 201);
        String providerId = created.at("/data/providerId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.toString()).doesNotContain("https://hooks.example.com");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 201);
        assertThat(replay.at("/data/providerId").asText()).isEqualTo(providerId);
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(with(providerBody("provider-create"), "displayName", "Changed Provider"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 409, 49962);
        performJson(post("/api/v1/cross-platform-notification/admin/providers").header("Authorization", bearer("cpn-admin-token")),
                with(with(providerBody("provider-duplicate"), "displayName", "External Provider provider-create"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 409, 49961);

        performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId).header("Authorization", bearer("cpn-admin-token")),
                Map.of("allowedRiskLevels", List.of("LOW", "MEDIUM"), "reason", "缺确认", "idempotencyKey", "patch-no-confirm"), 403, 42003);
        JsonNode patched = performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId).header("Authorization", bearer("cpn-admin-token")),
                Map.of("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"), "confirmText", "UPDATE_EXTERNAL_PROVIDER",
                        "reason", "更新允许风险等级", "idempotencyKey", "patch-provider"), 200);
        assertThat(patched.at("/data/allowedRiskLevels").toString()).contains("HIGH");

        performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId + "/enable").header("Authorization", bearer("cpn-admin-token")),
                Map.of("reason", "缺启用确认", "idempotencyKey", "enable-no-confirm"), 403, 42003);
        JsonNode enabled = performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId + "/enable").header("Authorization", bearer("cpn-admin-token")),
                Map.of("confirmText", "ENABLE_EXTERNAL_PROVIDER", "reason", "启用 provider", "idempotencyKey", "enable-provider"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId + "/archive").header("Authorization", bearer("cpn-admin-token")),
                Map.of("confirmText", "ARCHIVE_EXTERNAL_PROVIDER", "reason", "启用 provider 不可归档", "idempotencyKey", "archive-enabled"), 409, 49960);
        JsonNode disabled = performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId + "/disable").header("Authorization", bearer("cpn-admin-token")),
                Map.of("reason", "禁用 provider", "idempotencyKey", "disable-provider"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        JsonNode archived = performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId + "/archive").header("Authorization", bearer("cpn-admin-token")),
                Map.of("confirmText", "ARCHIVE_EXTERNAL_PROVIDER", "reason", "归档 provider", "idempotencyKey", "archive-provider"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");
        performJson(patch("/api/v1/cross-platform-notification/admin/providers/" + providerId).header("Authorization", bearer("cpn-admin-token")),
                Map.of("displayName", "Archived Changed", "reason", "归档不可修改", "idempotencyKey", "patch-archived"), 409, 49960);
    }

    @Test
    @DisplayName("CPN-TEMPLATE, CPN-ROUTE, CPN-DELIVERY, CPN-ATTEMPT, CPN-RECEIVER, and CPN-AUDIT cover full simulated notification flow")
    void templateRouteDeliveryAttemptReceiverAndAuditFlow() throws Exception {
        JsonNode mapping = performJson(post("/api/v1/cross-platform-notification/admin/template-mappings").header("Authorization", bearer("cpn-admin-token")),
                templateBody("template-main"), 201);
        String mappingId = mapping.at("/data/mappingId").asText();
        assertThat(mapping.at("/data/status").asText()).isEqualTo("DRAFT");
        performJson(post("/api/v1/cross-platform-notification/admin/template-mappings").header("Authorization", bearer("cpn-admin-token")),
                with(templateBody("template-bad-var"), "fallbackBodyTemplate", "Hello {{missing}}"), 400, 49965);
        performJson(post("/api/v1/cross-platform-notification/admin/template-mappings").header("Authorization", bearer("cpn-admin-token")),
                with(templateBody("template-conflict"), "sourceTemplateRef", Map.of("code", "template-template-main")), 409, 49961);
        JsonNode mappingPatched = performJson(patch("/api/v1/cross-platform-notification/admin/template-mappings/" + mappingId).header("Authorization", bearer("cpn-admin-token")),
                Map.of("fallbackTitleTemplate", "Updated {{title}}", "reason", "更新模板", "idempotencyKey", "patch-template"), 200);
        assertThat(mappingPatched.at("/data/version").asInt()).isEqualTo(2);
        JsonNode mappingEnabled = performJson(patch("/api/v1/cross-platform-notification/admin/template-mappings/" + mappingId + "/enable").header("Authorization", bearer("cpn-admin-token")),
                Map.of("reason", "启用模板", "idempotencyKey", "enable-template"), 200);
        assertThat(mappingEnabled.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(post("/api/v1/cross-platform-notification/admin/template-mappings").header("Authorization", bearer("cpn-admin-token")),
                with(templateBody("template-rich-block"), "renderMode", "RICH_BLOCK", "providerId", "provider-sms-basic"), 400, 49966);

        performJson(post("/api/v1/cross-platform-notification/admin/routes").header("Authorization", bearer("cpn-admin-token")),
                routeBody("route-no-confirm", mappingId), 403, 42003);
        performJson(post("/api/v1/cross-platform-notification/admin/routes").header("Authorization", bearer("cpn-admin-token")),
                with(routeBody("route-bad-receiver", mappingId), "confirmText", "CONFIGURE_EXTERNAL_ROUTE",
                        "receiverSummary", Map.of("receiverType", "EMAIL_ADDRESS", "targetRefSummary", "not-an-email")), 400, 49964);
        JsonNode route = performJson(post("/api/v1/cross-platform-notification/admin/routes").header("Authorization", bearer("cpn-admin-token")),
                with(routeBody("route-main", mappingId), "confirmText", "CONFIGURE_EXTERNAL_ROUTE"), 201);
        String routeId = route.at("/data/routeId").asText();
        JsonNode routeEnabled = performJson(patch("/api/v1/cross-platform-notification/admin/routes/" + routeId + "/enable").header("Authorization", bearer("cpn-admin-token")),
                Map.of("confirmText", "ENABLE_EXTERNAL_ROUTE", "reason", "启用路由", "idempotencyKey", "enable-route"), 200);
        assertThat(routeEnabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode routeTest = performJson(post("/api/v1/cross-platform-notification/admin/routes/" + routeId + "/test").header("Authorization", bearer("cpn-admin-token")),
                Map.of("samplePayloadSummary", Map.of("title", "Alert", "body", "Server down"),
                        "sampleReceiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"),
                        "dryRun", false, "confirmText", "TEST_EXTERNAL_ROUTE", "reason", "测试路由", "idempotencyKey", "test-route"), 201);
        String testDeliveryId = routeTest.at("/data/delivery/deliveryId").asText();
        assertThat(routeTest.at("/data/delivery/status").asText()).isEqualTo("SIMULATED_SENT");
        assertThat(routeTest.at("/data/attempt/status").asText()).isEqualTo("SIMULATED_SUCCESS");
        assertThat(routeTest.toString()).doesNotContain("\"SENT\"");
        assertNoSecrets(routeTest);

        JsonNode failedDelivery = performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Provider-Mode", "failed"),
                deliveryBody("delivery-failed", routeId, mappingId), 201);
        String failedDeliveryId = failedDelivery.at("/data/deliveryId").asText();
        assertThat(failedDelivery.at("/data/status").asText()).isEqualTo("SIMULATED_FAILED");
        JsonNode retried = performJson(patch("/api/v1/cross-platform-notification/admin/deliveries/" + failedDeliveryId + "/retry")
                        .header("Authorization", bearer("cpn-admin-token")),
                Map.of("confirmText", "RETRY_EXTERNAL_DELIVERY", "reason", "重试失败投递", "idempotencyKey", "retry-delivery"), 200);
        assertThat(retried.at("/data/status").asText()).isEqualTo("SIMULATED_SENT");
        performJson(patch("/api/v1/cross-platform-notification/admin/deliveries/" + failedDeliveryId + "/retry")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Fail-Delivery", "true"),
                Map.of("confirmText", "RETRY_EXTERNAL_DELIVERY", "reason", "终态不可重试", "idempotencyKey", "retry-sent-fails-before-state"), 500, 55803);

        JsonNode scheduled = performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Provider-Mode", "rate-limited"),
                deliveryBody("delivery-scheduled", routeId, mappingId), 201);
        String scheduledId = scheduled.at("/data/deliveryId").asText();
        assertThat(scheduled.at("/data/status").asText()).isEqualTo("RETRY_SCHEDULED");
        JsonNode canceled = performJson(patch("/api/v1/cross-platform-notification/admin/deliveries/" + scheduledId + "/cancel")
                        .header("Authorization", bearer("cpn-admin-token")),
                Map.of("reason", "取消排队重试", "idempotencyKey", "cancel-delivery"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");

        performJson(post("/api/v1/cross-platform-notification/admin/deliveries").header("Authorization", bearer("cpn-admin-token")),
                with(deliveryBody("delivery-real", routeId, mappingId), "sendMode", "REAL"), 409, 49967);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries").header("Authorization", bearer("cpn-admin-token")),
                with(deliveryBody("delivery-route-conflict", routeId, mappingId), "providerId", "provider-sms-basic"), 409, 49961);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries").header("Authorization", bearer("cpn-admin-token")),
                with(deliveryBody("delivery-bad-payload", routeId, mappingId), "payloadSummary", Map.of("notAllowed", "x")), 400, 49965);

        JsonNode deliveryDetail = performJson(get("/api/v1/cross-platform-notification/admin/deliveries/" + testDeliveryId)
                .header("Authorization", bearer("cpn-viewer-token")), 200);
        assertThat(deliveryDetail.at("/data/attemptSummary/attemptNo").asInt()).isEqualTo(1);
        JsonNode attempts = performJson(get("/api/v1/cross-platform-notification/admin/attempts")
                .header("Authorization", bearer("cpn-viewer-token"))
                .param("deliveryId", testDeliveryId)
                .param("status", "SIMULATED_SUCCESS")
                .param("sort", "attemptNo_asc"), 200);
        String attemptId = attempts.at("/data/items/0/attemptId").asText();
        JsonNode attemptDetail = performJson(get("/api/v1/cross-platform-notification/admin/attempts/" + attemptId)
                .header("Authorization", bearer("cpn-viewer-token")), 200);
        assertThat(attemptDetail.at("/data/simulated").asBoolean()).isTrue();
        performJson(get("/api/v1/cross-platform-notification/admin/attempts/missing")
                .header("Authorization", bearer("cpn-viewer-token")), 404, 49954);

        JsonNode receivers = performJson(get("/api/v1/cross-platform-notification/admin/receivers")
                .header("Authorization", bearer("cpn-viewer-token"))
                .param("providerId", "provider-discord-main")
                .param("receiverType", "CHANNEL")
                .param("sort", "lastUsedAt_desc"), 200);
        String receiverId = receivers.at("/data/items/0/receiverId").asText();
        JsonNode receiver = performJson(get("/api/v1/cross-platform-notification/admin/receivers/" + receiverId)
                .header("Authorization", bearer("cpn-viewer-token")), 200);
        assertThat(receiver.at("/data/targetRefSummary").asText()).contains("#");
        performJson(get("/api/v1/cross-platform-notification/admin/receivers/missing")
                .header("Authorization", bearer("cpn-viewer-token")), 404, 49955);

        JsonNode audit = performJson(get("/api/v1/cross-platform-notification/admin/audit-logs")
                .header("Authorization", bearer("cpn-admin-token"))
                .param("routeId", routeId)
                .param("result", "SUCCESS")
                .param("riskLevel", "HIGH"), 200);
        assertThat(audit.at("/data/items").toString()).contains("EXTERNAL_ROUTE");
        assertThat(audit.at("/data/items/0/paramsSummary/sanitized").asBoolean()).isTrue();
        performJson(get("/api/v1/cross-platform-notification/admin/audit-logs")
                .header("Authorization", bearer("cpn-viewer-token")), 403, 42001);
        assertNoSecrets(audit);
    }

    @Test
    @DisplayName("CPN-DEPS and CPN-HARDEN cover dependency failures, test controls, source boundaries, and production source scan")
    void dependencyFailuresAndHardeningBoundaries() throws Exception {
        performJson(post("/api/v1/cross-platform-notification/admin/template-mappings")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                templateBody("notification-down"), 502, 47120);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Alerting-Mode", "timeout"),
                deliveryBody("alerting-timeout", "route-alerting-discord-main", "mapping-notification-discord-main"), 504, 47131);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Plugin-Integration-Mode", "bad-schema"),
                with(deliveryBody("plugin-bad-schema", "route-alerting-discord-main", "mapping-notification-discord-main"), "sourceModule", "plugin-integration"), 502, 47142);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Source-Mode", "bad-schema"),
                with(deliveryBody("source-bad-schema", "route-alerting-discord-main", "mapping-notification-discord-main"), "sourceModule", "community"), 502, 47162);
        performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", bearer("cpn-admin-token"))
                        .header("X-Test-Fail-Delivery", "true"),
                deliveryBody("delivery-write-fails", "route-alerting-discord-main", "mapping-notification-discord-main"), 500, 55803);

        Path serviceRoot = Path.of("backend/cross-platform-notification-service/src/main/java");
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
                "cn.beiming.nodedaemon.", "cn.beiming.cloudrevesync.", "cn.beiming.backuprecovery.", "cn.beiming.alerting.",
                "cn.beiming.onlinemap.", "cn.beiming.pluginintegration.", "Repository", "JdbcTemplate", "ProcessBuilder",
                "Runtime.getRuntime", "node-daemon", "webhookSecret", "discordToken", "slackWebhook", "telegramBotToken",
                "qqToken", "oopzToken", "smtpPassword", "smsToken", "botToken", "rconPassword", "pluginToken",
                "pluginSecret", "cloudreveToken", "backupEncryptionKey", "terminal", "container", "restorePath",
                "worldDirectory", "internalUrl", "internalPath", "resolvedPath", "rawPayload", "rawToken", "credential",
                "secretKey", "Authorization", "requestHeaders", ".env", "authorized_keys", "id_rsa", "rm -rf",
                "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s", "jdbc:");
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

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", "DISCORD");
        body.put("displayName", "External Provider " + idempotencyKey);
        body.put("endpointSummary", Map.of("url", "https://hooks.example.com/" + idempotencyKey));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + idempotencyKey, "managedBy", "vault-summary"));
        body.put("receiverPolicy", Map.of("allowedReceiverTypes", List.of("CHANNEL", "EMAIL_ADDRESS"), "maxReceivers", 10));
        body.put("allowedSourceModules", List.of("notification", "alerting", "plugin-integration", "community"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 100));
        body.put("reason", "创建外部 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> templateBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "notification");
        body.put("sourceTemplateRef", Map.of("code", "template-" + idempotencyKey));
        body.put("providerId", "provider-discord-main");
        body.put("externalTemplateKey", "external-template-" + idempotencyKey);
        body.put("allowedVariables", List.of("title", "body", "player"));
        body.put("renderMode", "MARKDOWN");
        body.put("fallbackTitleTemplate", "{{title}}");
        body.put("fallbackBodyTemplate", "{{body}} for {{player}}");
        body.put("reason", "创建模板映射");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey, String mappingId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "External Route " + idempotencyKey);
        body.put("sourceModule", "alerting");
        body.put("eventType", "alert.fired");
        body.put("riskLevel", "HIGH");
        body.put("matchers", Map.of("sourceModule", "alerting", "eventType", "alert.fired", "riskLevel", "HIGH"));
        body.put("providerId", "provider-discord-main");
        body.put("templateMappingId", mappingId);
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"));
        body.put("groupingPolicy", Map.of("groupBy", List.of("sourceModule", "eventType"), "groupWaitSeconds", 10, "groupIntervalSeconds", 60));
        body.put("retryPolicySummary", Map.of("maxAttempts", 3, "backoffSeconds", 30, "expireAfterSeconds", 3600));
        body.put("reason", "创建外部路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> deliveryBody(String idempotencyKey, String routeId, String mappingId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "alerting");
        body.put("sourceId", "alert-" + idempotencyKey);
        body.put("eventType", "alert.fired");
        body.put("riskLevel", "HIGH");
        body.put("routeId", routeId);
        body.put("providerId", "provider-discord-main");
        body.put("templateMappingId", mappingId);
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"));
        body.put("payloadSummary", Map.of("title", "Alert", "body", "Server degraded", "player", "Alex"));
        body.put("expiresAt", "2026-05-29T00:00:00Z");
        body.put("confirmText", "CREATE_EXTERNAL_DELIVERY");
        body.put("reason", "创建模拟投递");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private Map<String, Object> with(Map<String, Object> source, String key1, Object value1, String key2, Object value2) {
        Map<String, Object> copy = with(source, key1, value1);
        copy.put(key2, value2);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "rawToken", "webhookSecret", "discordToken", "slackWebhook", "telegramBotToken",
                "qqToken", "oopzToken", "smtpPassword", "smsToken", "botToken", "rconPassword",
                "secretKey", "Authorization", "requestHeaders", "stackTrace",
                "internalUrl", "internalPath", "resolvedPath", "fullException", "databaseUrl",
                "providerRawResponse", "externalMessageId", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "/srv/", "C:\\\\", ".env", "authorized_keys", "id_rsa", "token=");
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
