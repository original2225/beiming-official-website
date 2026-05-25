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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NodeDaemonServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NodeDaemonProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/node-daemon/ops/summary")
                .headers(nodeHeaders())
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
                        .headers(nodeHeaders())
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

    private org.springframework.http.HttpHeaders nodeHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer node-token-valid");
        headers.set("X-Node-Id", "node-main");
        headers.set("X-Node-Request-Id", "node-http-prod");
        headers.set("X-Node-Timestamp", "2026-05-25T15:00:00Z");
        headers.set("X-Node-Signature", "test-signature");
        return headers;
    }
}
