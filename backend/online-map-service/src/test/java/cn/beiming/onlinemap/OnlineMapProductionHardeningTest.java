package cn.beiming.onlinemap;

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

@SpringBootTest(classes = OnlineMapServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OnlineMapProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/online-map/admin/ops/summary")
                .header("Authorization", "Bearer map-viewer-token")
                .header("X-Test-Auth-Mode", "unavailable")
                .header("X-Test-Fail-Store", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        JsonNode provider = performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", "Bearer map-admin-token")
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Server-Status-Mode", "unavailable"),
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
        body.put("providerType", "BLUEMAP");
        body.put("displayName", "Prod Contract Map " + idempotencyKey);
        body.put("publicBaseUrl", "https://maps.example.com/" + idempotencyKey);
        body.put("embedUrl", "https://maps.example.com/" + idempotencyKey + "/embed");
        body.put("publicVisible", false);
        body.put("allowedOrigins", List.of("https://beiming.example"));
        body.put("contentRef", Map.of("contentId", "content-map-guide"));
        body.put("serverStatusRef", Map.of("instanceId", "survival-main"));
        body.put("opsRef", Map.of("instanceId", "mc-main"));
        body.put("changelogRef", Map.of("releaseId", "map-v1"));
        body.put("reason", "生产默认忽略测试控制头");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "rawToken", "credential", "secretKey", "nodeToken", "mapAdminPassword", "Authorization",
                "stackTrace", "internalUrl", "internalPath", "resolvedPath", "worldDirectory", "fullException",
                "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "/srv/", "C:\\\\", ".env",
                "authorized_keys", "id_rsa", "token=");
    }
}
