package cn.beiming.attendance;

import cn.beiming.admission.AdmissionCoreServiceApplication;
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

@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = "server.port=8131")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AttendanceProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode initialized = performJson(post("/api/v1/attendance/admin/initializations")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Whitelist-Mode", "unavailable")
                        .header("X-Test-Profile-Mode", "unavailable")
                        .header("X-Test-Notification-Mode", "unavailable"),
                initBody("wl-app-1", "prod-init-1"), 201);
        String accountId = initialized.at("/data/account/accountId").asText();
        assertThat(initialized.at("/data/account/notificationFailure").isNull()).isTrue();

        JsonNode staleAttempt = performJson(get("/api/v1/attendance/leaderboard").header("X-Test-Profile-Mode", "unavailable"), 200);
        assertThat(staleAttempt.at("/data/items/0/profileSnapshotStale").asBoolean()).isFalse();

        JsonNode adjusted = performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Fail-Store", "true")
                        .header("X-Test-Fail-Ledger", "true"),
                adjustBody(10, "prod-adjust-1"), 200);
        assertThat(adjusted.at("/data/account/scoreBalance").asInt()).isEqualTo(110);

        JsonNode ops = performJson(get("/api/v1/attendance/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(ops.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");
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

    private Map<String, Object> initBody(String applicationId, String idempotencyKey) {
        return Map.of("applicationId", applicationId, "idempotencyKey", idempotencyKey, "reason", "符合白名单初始化");
    }

    private Map<String, Object> adjustBody(int delta, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("delta", delta);
        body.put("publicReason", "生产硬化加分");
        body.put("reason", "确认测试控制头关闭");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
