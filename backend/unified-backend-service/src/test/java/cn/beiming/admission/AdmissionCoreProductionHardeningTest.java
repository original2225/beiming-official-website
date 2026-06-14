package cn.beiming.admission;

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

@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = "server.port=8131")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdmissionCoreProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void secondBatchTestControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode onboarding = performJson(get("/api/v1/onboarding/me/progress")
                .header("Authorization", bearer("content-unavailable-token"))
                .header("X-Test-Dependency-Mode", "PROFILE:UNAVAILABLE"), 200);
        assertThat(onboarding.at("/data/degraded").asBoolean()).isFalse();

        JsonNode exam = performJson(post("/api/v1/exams/me/sessions")
                        .header("Authorization", bearer("ready-token"))
                        .header("X-Test-Dependency-Mode", "ONBOARDING:UNAVAILABLE")
                        .header("X-Test-Fail-Store", "true"),
                Map.of("applicationId", "app-ready", "idempotencyKey", "prod-exam-create-1"), 201);
        String sessionId = exam.at("/data/sessionId").asText();
        JsonNode submitted = performJson(post("/api/v1/exams/me/sessions/" + sessionId + "/submit")
                        .header("Authorization", bearer("ready-token"))
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("idempotencyKey", "prod-exam-submit-1", "answers", answers()), 200);
        assertThat(submitted.at("/data/notificationStatus").asText()).isEqualTo("DELIVERED");

        JsonNode whitelist = performJson(post("/api/v1/whitelist/me/applications")
                        .header("Authorization", bearer("user-token"))
                        .header("X-Test-Fail-Store", "true")
                        .header("X-Test-Notification-Mode", "unavailable"),
                createBody("session-passed", "prod-wl-create-1"), 201);
        assertThat(whitelist.at("/data/notificationStatus").asText()).isEqualTo("DELIVERED");
        JsonNode approved = performJson(patch("/api/v1/whitelist/admin/applications/" + whitelist.at("/data/applicationId").asText() + "/approve")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Profile-Mode", "unavailable")
                        .header("X-Test-Fail-After-Profile", "true"),
                approveBody("prod-wl-approve-1"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
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

    private List<Map<String, Object>> answers() {
        return List.of(
                Map.of("questionId", "q-redstone-single", "selectedOptionIds", List.of("A")),
                Map.of("questionId", "q-redstone-multiple", "selectedOptionIds", List.of("A", "C")),
                Map.of("questionId", "q-redstone-short", "textAnswer", "红石时序要可复现")
        );
    }

    private Map<String, Object> createBody(String sessionId, String idempotencyKey) {
        return Map.of("examSessionId", sessionId, "idempotencyKey", idempotencyKey, "materials", List.of(Map.of("type", "TEXT", "title", "说明", "content", "申请说明")));
    }

    private Map<String, Object> approveBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotencyKey", idempotencyKey);
        body.put("reviewComment", "审核通过");
        body.put("reason", "符合准入要求");
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
