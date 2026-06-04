package cn.beiming.opscontrol;

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
class OpsControlProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode created = performJson(post("/api/v1/ops-control/nodes")
                        .header("Authorization", bearer("ops-admin-token"))
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Auth-Mode", "unavailable"),
                nodeBody("prod-ops-node-1"), 201);
        assertThat(created.at("/data/node/nodeId").asText()).isNotBlank();

        JsonNode task = performJson(post("/api/v1/ops-control/tasks")
                        .header("Authorization", bearer("ops-container-token"))
                        .header("X-Test-Node-Mode", "offline"),
                taskBody("CONTAINER_RESTART", "container-seed-1", "prod-restart"), 201);
        assertThat(task.at("/data/status").asText()).isIn("SUCCEEDED", "DISPATCHED", "QUEUED");

        JsonNode ops = performJson(get("/api/v1/ops-control/ops/summary").header("Authorization", bearer("ops-viewer-token")), 200);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(ops.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "NODE_DAEMON_NOT_CONNECTED");
        assertThat(ops.toString()).doesNotContain("node-secret-token", "Authorization", "stackTrace", "cloudreveToken");
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

    private Map<String, Object> nodeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "生产硬化节点");
        body.put("endpointSummary", "https://node.example.internal:9443");
        body.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE"));
        body.put("labels", Map.of("env", "prod-test"));
        body.put("reason", "验证默认环境忽略测试控制头");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> taskBody(String taskType, String targetId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskType", taskType);
        body.put("nodeId", "node-main");
        body.put("targetType", "CONTAINER");
        body.put("targetId", targetId);
        body.put("params", Map.of("mode", "simulated"));
        body.put("reason", "生产硬化任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
