package cn.beiming.crossplatformnotification;

import cn.beiming.opscore.OpsCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpsCoreServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CrossPlatformNotificationProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/cross-platform-notification/admin/ops/summary")
                .header("Authorization", "Bearer cpn-viewer-token")
                .header("X-Test-Auth-Mode", "unavailable")
                .header("X-Test-Fail-Store", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        JsonNode provider = performJson(post("/api/v1/cross-platform-notification/admin/providers")
                        .header("Authorization", "Bearer cpn-admin-token")
                        .header("X-Test-Fail-Audit", "true"),
                with(providerBody("prod-control-ignored"), "confirmText", "REGISTER_EXTERNAL_PROVIDER"), 201);
        assertThat(provider.at("/data/providerId").asText()).isNotBlank();

        JsonNode mapping = performJson(post("/api/v1/cross-platform-notification/admin/template-mappings")
                        .header("Authorization", "Bearer cpn-admin-token"),
                templateBody("prod-template"), 201);
        String mappingId = mapping.at("/data/mappingId").asText();
        performJson(patch("/api/v1/cross-platform-notification/admin/template-mappings/" + mappingId + "/enable")
                        .header("Authorization", "Bearer cpn-admin-token"),
                Map.of("reason", "启用模板", "idempotencyKey", "prod-enable-template"), 200);
        JsonNode route = performJson(post("/api/v1/cross-platform-notification/admin/routes")
                        .header("Authorization", "Bearer cpn-admin-token"),
                with(routeBody("prod-route", mappingId), "confirmText", "CONFIGURE_EXTERNAL_ROUTE"), 201);
        String routeId = route.at("/data/routeId").asText();
        performJson(patch("/api/v1/cross-platform-notification/admin/routes/" + routeId + "/enable")
                        .header("Authorization", "Bearer cpn-admin-token"),
                Map.of("confirmText", "ENABLE_EXTERNAL_ROUTE", "reason", "启用路由", "idempotencyKey", "prod-enable-route"), 200);
        JsonNode routeTest = performJson(post("/api/v1/cross-platform-notification/admin/routes/" + routeId + "/test")
                        .header("Authorization", "Bearer cpn-admin-token")
                        .header("X-Test-Provider-Mode", "failed"),
                Map.of("samplePayloadSummary", Map.of("title", "Alert", "body", "Server down", "player", "Alex"),
                        "sampleReceiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"),
                        "dryRun", false, "confirmText", "TEST_EXTERNAL_ROUTE", "reason", "生产默认忽略测试 provider 控制头", "idempotencyKey", "prod-test-route"), 201);
        assertThat(routeTest.at("/data/delivery/status").asText()).isEqualTo("SIMULATED_SENT");
        assertNoSecrets(routeTest);
    }

    @Test
    void alertingAdapterRouteUsesAlertFiringEventAndAlertingTemplateOwnership() throws Exception {
        JsonNode route = performJson(get("/api/v1/cross-platform-notification/admin/routes/route-alerting-discord-main")
                .header("Authorization", "Bearer cpn-viewer-token"), 200);

        assertThat(route.at("/data/sourceModule").asText()).isEqualTo("alerting");
        assertThat(route.at("/data/eventType").asText()).isEqualTo("alert.firing");
        assertThat(route.at("/data/riskLevel").asText()).isEqualTo("HIGH");
        assertThat(route.at("/data/templateMappingSummary/sourceModule").asText()).isEqualTo("alerting");
        assertThat(route.at("/data/providerSummary/providerId").asText()).isEqualTo("provider-discord-main");
        assertNoSecrets(route);
    }

    @Test
    void alertingSourceDeliveryCreatesSimulatedAttemptAndAuditWithSafePayloadSummary() throws Exception {
        JsonNode delivery = performJson(post("/api/v1/cross-platform-notification/admin/deliveries")
                        .header("Authorization", "Bearer cpn-admin-token"),
                alertingDeliveryBody("cpn-alerting-delivery"), 201);

        String deliveryId = delivery.at("/data/deliveryId").asText();
        assertThat(delivery.at("/data/sourceModule").asText()).isEqualTo("alerting");
        assertThat(delivery.at("/data/sourceId").asText()).isEqualTo("alert-node-main");
        assertThat(delivery.at("/data/eventType").asText()).isEqualTo("alert.firing");
        assertThat(delivery.at("/data/status").asText()).isEqualTo("SIMULATED_SENT");
        assertThat(delivery.at("/data/attempts").asInt()).isEqualTo(1);
        assertThat(delivery.at("/data/payloadSummary/fieldNames").toString()).contains("title", "body", "player");
        assertThat(delivery.at("/data/payloadSummary/values/body/hash").asText()).isNotBlank();
        assertThat(delivery.toString()).doesNotContain("Node heartbeat delayed for 600 seconds");

        JsonNode audits = performJson(get("/api/v1/cross-platform-notification/admin/audit-logs")
                .header("Authorization", "Bearer cpn-admin-token")
                .param("sourceModule", "alerting")
                .param("deliveryId", deliveryId), 200);
        assertThat(audits.at("/data/items/0/sourceModule").asText()).isEqualTo("alerting");
        assertThat(audits.at("/data/items/0/routeId").asText()).isEqualTo("route-alerting-discord-main");
        assertThat(audits.at("/data/items/0/riskLevel").asText()).isEqualTo("HIGH");
        assertNoSecrets(audits);
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", "DISCORD");
        body.put("displayName", "Prod External Provider " + idempotencyKey);
        body.put("endpointSummary", Map.of("url", "https://hooks.example.com/" + idempotencyKey));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + idempotencyKey, "managedBy", "vault-summary"));
        body.put("receiverPolicy", Map.of("allowedReceiverTypes", List.of("CHANNEL"), "maxReceivers", 10));
        body.put("allowedSourceModules", List.of("notification", "alerting"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 100));
        body.put("reason", "生产默认忽略测试控制头");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> templateBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "notification");
        body.put("sourceTemplateRef", Map.of("code", "prod-template-" + idempotencyKey));
        body.put("providerId", "provider-discord-main");
        body.put("externalTemplateKey", "prod-external-template-" + idempotencyKey);
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
        body.put("displayName", "Prod External Route " + idempotencyKey);
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

    private Map<String, Object> alertingDeliveryBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "alerting");
        body.put("sourceId", "alert-node-main");
        body.put("eventType", "alert.firing");
        body.put("riskLevel", "HIGH");
        body.put("routeId", "route-alerting-discord-main");
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"));
        body.put("payloadSummary", Map.of(
                "title", "Node heartbeat delayed",
                "body", "Node heartbeat delayed for 600 seconds",
                "player", "system"));
        body.put("expiresAt", "2026-06-05T01:00:00Z");
        body.put("confirmText", "CREATE_EXTERNAL_DELIVERY");
        body.put("reason", "alerting 内部适配模拟外部投递");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
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
}
