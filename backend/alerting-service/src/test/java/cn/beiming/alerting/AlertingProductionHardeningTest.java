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

@SpringBootTest(classes = AlertingServiceApplication.class)
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
