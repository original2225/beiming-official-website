package cn.beiming.nodedaemon;

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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NodeDaemonServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NodeDaemonProductionHardeningTest {
    private static final String SECRET = "local-node-signing-secret";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/node-daemon/ops/summary")
                .headers(nodeHeaders("GET", "/api/v1/node-daemon/ops/summary", null, "node-http-prod-summary"))
                .header("X-Test-Node-Auth", "invalid")
                .header("X-Test-Fail-Audit", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", "ops-task-prod");
        body.put("taskType", "CONTAINER_RESTART");
        body.put("nodeId", "node-main");
        body.put("targetType", "CONTAINER");
        body.put("targetId", "container-seed-1");
        body.put("paramsSummary", Map.of("mode", "simulated"));
        body.put("riskLevel", "MEDIUM");
        body.put("reason", "生产默认忽略测试头");
        body.put("nodeRequestId", "node-req-prod");
        body.put("expiresAt", "2026-05-25T16:00:00Z");
        body.put("idempotencyKey", "node-req-prod");

        JsonNode task = performJson(post("/api/v1/node-daemon/tasks")
                        .headers(nodeHeaders("POST", "/api/v1/node-daemon/tasks", body, "node-http-prod"))
                        .header("X-Test-Runtime-Mode", "unavailable")
                        .header("X-Test-Fail-Audit", "true"),
                body, 201);
        assertThat(task.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        assertThat(task.toString()).doesNotContain("node-secret-token", "Authorization", "stackTrace", "cloudreveToken");
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

    private org.springframework.http.HttpHeaders nodeHeaders(String method, String path, Map<String, Object> body, String nodeRequestId) {
        String timestamp = Instant.now().toString();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer node-token-valid");
        headers.set("X-Node-Id", "node-main");
        headers.set("X-Node-Request-Id", nodeRequestId);
        headers.set("X-Node-Timestamp", timestamp);
        headers.set("X-Node-Signature", signature(method, path, body, timestamp, nodeRequestId));
        return headers;
    }

    private String signature(String method, String path, Map<String, Object> body, String timestamp, String nodeRequestId) {
        try {
            String canonicalBody = objectMapper.writeValueAsString(canonicalize(body == null ? Map.of() : body));
            String signingText = method + "\n" + path + "\n" + canonicalBody + "\n" + timestamp + "\n" + nodeRequestId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) {
                hex.append("%02x".formatted(value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.List<Object> values = new java.util.ArrayList<>();
            for (Object item : iterable) {
                values.add(canonicalize(item));
            }
            return values;
        }
        return value;
    }
}
