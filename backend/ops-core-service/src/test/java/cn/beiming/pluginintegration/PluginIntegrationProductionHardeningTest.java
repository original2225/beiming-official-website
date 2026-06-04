package cn.beiming.pluginintegration;

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

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PluginIntegrationProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/plugin-integration/admin/ops/summary")
                .header("Authorization", "Bearer plugin-viewer-token")
                .header("X-Test-Auth-Mode", "unavailable")
                .header("X-Test-Fail-Store", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        JsonNode provider = performJson(post("/api/v1/plugin-integration/admin/providers")
                        .header("Authorization", "Bearer plugin-admin-token")
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Ops-Control-Mode", "unavailable"),
                providerBody("prod-control-ignored"), 201);
        assertThat(provider.at("/data/providerId").asText()).isNotBlank();
        assertNoSecrets(provider);
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
        body.put("providerType", "PAPER");
        body.put("displayName", "Prod Plugin Provider " + idempotencyKey);
        body.put("pluginName", "BeimingBridge");
        body.put("pluginVersion", "1.0.0");
        body.put("serverKind", "SERVER");
        body.put("instanceRef", Map.of("instanceId", "mc-main"));
        body.put("nodeRef", Map.of("nodeId", "node-main"));
        body.put("publicVisible", false);
        body.put("eventEndpointSummary", "/plugin-events/" + idempotencyKey);
        body.put("allowedEventTypes", List.of("beiming.player_join"));
        body.put("allowedOrigins", List.of("https://plugins.example.com"));
        body.put("confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT");
        body.put("reason", "生产默认忽略测试控制头");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "rawToken", "pluginToken", "pluginSecret", "webhookSecret", "discordToken",
                "credential", "secretKey", "nodeToken", "Authorization", "requestHeaders", "stackTrace",
                "internalUrl", "internalPath", "resolvedPath", "worldDirectory", "serverPassword",
                "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "/srv/", "C:\\\\", ".env",
                "authorized_keys", "id_rsa", "token=");
    }
}
