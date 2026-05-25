package cn.beiming.nodedaemon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

@SpringBootTest(classes = NodeDaemonServiceApplication.class, properties = {
        "node-daemon.node-id=node-main",
        "node-daemon.node-token=node-token-valid",
        "node-daemon.node-signing-secret=local-node-signing-secret",
        "node-daemon.allowed-clock-skew-seconds=300"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NodeDaemonProductionAuthTest {
    private static final String SECRET = "local-node-signing-secret";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void productionDefaultAcceptsConfiguredHmacAndRejectsFixedTestSignature() throws Exception {
        JsonNode summary = performJson(get("/api/v1/node-daemon/ops/summary")
                .headers(signedHeaders("GET", "/api/v1/node-daemon/ops/summary", null, "node-auth-summary")), 200);

        assertThat(summary.at("/data/authMode").asText()).isEqualTo("HMAC_CONFIGURED");
        assertThat(summary.at("/data/signatureAlgorithm").asText()).isEqualTo("HmacSHA256");
        assertThat(summary.at("/data/nodeIdBound").asBoolean()).isTrue();
        assertThat(summary.toString()).doesNotContain(SECRET, "node-token-valid", "test-signature");

        performJson(get("/api/v1/node-daemon/ops/summary").headers(fixedSignatureHeaders("node-fixed-signature")), 401, 49601);
    }

    @Test
    void productionSignatureUsesCanonicalJsonBodyAndProtectsNodeRequestReplay() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotencyKey", "handshake-hmac");
        body.put("capabilities", java.util.List.of("NODE_WRITE", "NODE_READ"));
        body.put("daemonVersion", "0.1.0-simulated");
        body.put("registrationNonce", "nonce-hmac");
        body.put("controlPlaneNodeId", "node-main");

        String replayTimestamp = Instant.now().toString();
        JsonNode handshake = performJson(post("/api/v1/node-daemon/registration/handshake")
                .headers(signedHeaders("POST", "/api/v1/node-daemon/registration/handshake", body, "node-auth-handshake", replayTimestamp)), body, 200);
        assertThat(handshake.at("/data/nodeId").asText()).isEqualTo("node-main");

        JsonNode replay = performJson(post("/api/v1/node-daemon/registration/handshake")
                .headers(signedHeaders("POST", "/api/v1/node-daemon/registration/handshake", body, "node-auth-handshake", replayTimestamp)), body, 200);
        assertThat(replay.at("/data/handshakeId").asText()).isEqualTo(handshake.at("/data/handshakeId").asText());

        performJson(get("/api/v1/node-daemon/capabilities")
                .headers(signedHeaders("GET", "/api/v1/node-daemon/capabilities", null, "node-auth-handshake")), 409, 49612);
    }

    @Test
    void productionAuthRejectsBadTimestampAndBadCanonicalSignature() throws Exception {
        performJson(get("/api/v1/node-daemon/ops/summary")
                .headers(signedHeaders("GET", "/api/v1/node-daemon/ops/summary", null, "node-auth-old", "2020-01-01T00:00:00Z")), 401, 49602);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", "签名必须覆盖请求体");
        body.put("dryRun", true);
        body.put("idempotencyKey", "bad-body-signature");

        Map<String, Object> differentBody = new LinkedHashMap<>(body);
        differentBody.put("dryRun", false);
        performJson(post("/api/v1/node-daemon/runtime/heartbeat")
                .headers(signedHeaders("POST", "/api/v1/node-daemon/runtime/heartbeat", differentBody, "node-auth-bad-body")), body, 401, 49601);
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
        return json;
    }

    private HttpHeaders signedHeaders(String method, String path, Map<String, Object> body, String nodeRequestId) {
        return signedHeaders(method, path, body, nodeRequestId, Instant.now().toString());
    }

    private HttpHeaders signedHeaders(String method, String path, Map<String, Object> body, String nodeRequestId, String timestamp) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer node-token-valid");
        headers.set("X-Node-Id", "node-main");
        headers.set("X-Node-Request-Id", nodeRequestId);
        headers.set("X-Node-Timestamp", timestamp);
        headers.set("X-Node-Signature", signature(method, path, body, timestamp, nodeRequestId));
        return headers;
    }

    private HttpHeaders fixedSignatureHeaders(String nodeRequestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer node-token-valid");
        headers.set("X-Node-Id", "node-main");
        headers.set("X-Node-Request-Id", nodeRequestId);
        headers.set("X-Node-Timestamp", Instant.now().toString());
        headers.set("X-Node-Signature", "test-signature");
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

    private Object canonicalize(Object value) throws JsonProcessingException {
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
