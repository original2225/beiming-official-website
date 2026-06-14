package cn.beiming.alerting;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class, properties = "server.port=8133")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AlertingProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/alerting/ops/summary")
                .header("Authorization", "Bearer alert-viewer-token")
                .header("X-Test-Auth-Mode", "unavailable")
                .header("X-Test-Fail-Store", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        JsonNode delivery = performJson(post("/api/v1/alerting/routes/route-default/test")
                        .header("Authorization", "Bearer alert-admin-token")
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("sampleAlert", sampleAlert(), "reason", "生产默认忽略测试控制头", "idempotencyKey", "prod-route-test"), 201);
        assertThat(delivery.at("/data/status").asText()).isEqualTo("SENT");
        assertNoSecrets(delivery);
    }

    @Test
    void routeTestUsesCrossPlatformNotificationSimulatedExternalDeliverySummary() throws Exception {
        JsonNode delivery = performJson(post("/api/v1/alerting/routes/route-default/test")
                        .header("Authorization", "Bearer alert-admin-token"),
                Map.of("sampleAlert", sampleAlert(), "reason", "验证 CPN 模拟外部投递", "idempotencyKey", "prod-cpn-route-test"), 201);

        assertThat(delivery.at("/data/status").asText()).isEqualTo("SENT");
        assertThat(delivery.at("/data/deliveryMode").asText()).isEqualTo("SIMULATED_EXTERNAL");
        assertThat(delivery.at("/data/externalModule").asText()).isEqualTo("cross-platform-notification");
        assertThat(delivery.at("/data/externalDeliveryId").asText()).startsWith("delivery-");
        assertThat(delivery.at("/data/externalAttemptStatus").asText()).isEqualTo("SIMULATED_SUCCESS");
        assertThat(delivery.at("/data/realExternalSend").asBoolean()).isFalse();
        assertThat(delivery.at("/data/notificationRef/mode").asText()).isEqualTo("SIMULATED_EXTERNAL");
        assertNoSecrets(delivery);
    }

    @Test
    void ruleEvaluationKeepsAlertFiringAndStoresCpnExternalDeliveryReference() throws Exception {
        JsonNode evaluation = performJson(post("/api/v1/alerting/rules/rule-node-offline/evaluate")
                        .header("Authorization", "Bearer alert-admin-token"),
                Map.of("sourceSnapshot", Map.of("nodeId", "node-main", "heartbeatAgeSeconds", 600),
                        "dryRun", false, "reason", "触发告警并投递 CPN 摘要", "idempotencyKey", "prod-cpn-evaluate"), 201);
        String alertId = evaluation.at("/data/createdAlertId").asText();

        JsonNode alert = performJson(get("/api/v1/alerting/alerts/" + alertId)
                .header("Authorization", "Bearer alert-viewer-token"), 200);
        assertThat(alert.at("/data/status").asText()).isEqualTo("FIRING");
        assertThat(alert.at("/data/notificationSummary/status").asText()).isEqualTo("SENT");
        assertThat(alert.at("/data/notificationSummary/deliveryMode").asText()).isEqualTo("SIMULATED_EXTERNAL");
        assertThat(alert.at("/data/notificationSummary/externalModule").asText()).isEqualTo("cross-platform-notification");
        assertThat(alert.at("/data/notificationSummary/realExternalSend").asBoolean()).isFalse();

        JsonNode deliveries = performJson(get("/api/v1/alerting/deliveries")
                .header("Authorization", "Bearer alert-viewer-token")
                .param("alertId", alertId), 200);
        assertThat(deliveries.at("/data/items/0/deliveryMode").asText()).isEqualTo("SIMULATED_EXTERNAL");
        assertThat(deliveries.at("/data/items/0/externalAttemptStatus").asText()).isEqualTo("SIMULATED_SUCCESS");
        assertNoSecrets(deliveries);
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

    private Map<String, Object> sampleAlert() {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("summary", "Node heartbeat delayed");
        sample.put("severity", "WARNING");
        sample.put("sourceService", "OPS_CONTROL");
        sample.put("labels", Map.of("node", "main"));
        return sample;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "rawToken", "credential", "secretKey", "nodeToken", "notificationToken", "webhookSecret",
                "smtpPassword", "smsToken", "Authorization", "stackTrace", "internalPath", "resolvedPath",
                "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "/srv/", "C:\\\\", ".env",
                "authorized_keys", "id_rsa", "token=");
    }
}
