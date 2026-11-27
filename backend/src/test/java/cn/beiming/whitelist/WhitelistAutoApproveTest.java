package cn.beiming.whitelist;

import cn.beiming.admission.AdmissionCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * B 方案的准入判定（花蝉 2026-11-27 定）：
 * <ol>
 *   <li>邀请码只用于注册；</li>
 *   <li><b>管理员签发的邀请码</b>注册进来的玩家，白名单<b>免人工审核</b>；</li>
 *   <li>考核通过即直接发放白名单，不再走「申请 → 排队等人审」。</li>
 * </ol>
 *
 * <p>本测试锁死两个不能错的边界：
 * <ul>
 *   <li>普通玩家码（PLAYER）注册，即使考过了也必须留 PENDING_REVIEW——否则等于人人免审，
 *       审核这一环就废了；</li>
 *   <li>查不到邀请码来源时不放行（宁可漏判不可误判），避免历史账号被静默放行。</li>
 * </ul>
 *
 * <p>用 AtomicReference 控制 resolver 返回值，是因为 Spring 上下文在一个类里共享，
 * 而三个用例要对同一个注入点给出不同的「来源」结果。
 */
@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = {
        "server.port=8139",
        "whitelist.test-controls.enabled=true",
        "whitelist.auth.stub-tokens-enabled=true",
        "whitelist.stub-handoffs-enabled=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WhitelistAutoApproveTest {

    /** 用例可改写的来源返回值；null = 查不到来源。 */
    static final AtomicReference<cn.beiming.core.InvitationOriginResolver.Origin> ORIGIN = new AtomicReference<>();

    @TestConfiguration
    static class StubOriginConfig {
        @Bean
        cn.beiming.core.InvitationOriginResolver stubInvitationOriginResolver() {
            return userId -> ORIGIN.get();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        ORIGIN.set(null);
    }

    @Test
    @DisplayName("WL-AUTO-001 管理员邀请码 + 考核通过 = 直接 APPROVED，不用人工审核")
    void adminCodeAutoApproves() throws Exception {
        ORIGIN.set(new cn.beiming.core.InvitationOriginResolver.Origin("ADMIN", true, "ADMIN-CO"));

        JsonNode created = performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")), createBody("session-passed", "auto-admin-1", "MODPACK"), 201);

        assertThat(created.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(created.at("/data/result").asText()).isEqualTo("APPROVED");
        // 自动过也要建好档，不能只把状态改成 APPROVED 而 profile 还是空的
        assertThat(created.at("/data/profileActivation/status").asText()).isEqualTo("ACTIVATED");
    }

    @Test
    @DisplayName("WL-AUTO-002 普通玩家邀请码 + 考核通过 = 仍然 PENDING_REVIEW，审核不能绕过")
    void playerCodeStillNeedsReview() throws Exception {
        ORIGIN.set(new cn.beiming.core.InvitationOriginResolver.Origin("PLAYER", false, "PLAYER-C"));

        JsonNode created = performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")), createBody("session-passed", "auto-player-1", "MODPACK"), 201);

        assertThat(created.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(created.at("/data/result").asText()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("WL-AUTO-003 查不到邀请码来源 = 不放行，按普通玩家走审核")
    void unknownOriginNeedsReview() throws Exception {
        ORIGIN.set(null);

        JsonNode created = performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")), createBody("session-passed", "auto-none-1", "MODPACK"), 201);

        assertThat(created.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("WL-AUTO-004 管理员邀请码但考核没过 = 直接拒（考点没过的分数不能不算）")
    void adminCodeButExamFailedIsRejected() throws Exception {
        ORIGIN.set(new cn.beiming.core.InvitationOriginResolver.Origin("ADMIN", true, "ADMIN-CO"));

        // 比「留待审核」更严：考试未通过连申请都建不出来（44010），
        // 管理员码也不能把没考过的人放进来。
        MvcResult result = mvc.perform(post("/api/v1/whitelist/me/applications")
                        .header("Authorization", bearer("user-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("session-failed", "auto-admin-failed-1", "MODPACK"))))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.at("/code").asInt()).isEqualTo(44010);
    }

    private Map<String, Object> createBody(String sessionId, String idempotencyKey, String serverType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("examSessionId", sessionId);
        body.put("idempotencyKey", idempotencyKey);
        body.put("serverType", serverType);
        body.put("materials", List.of(Map.of("type", "TEXT", "title", "说明", "content", "申请说明")));
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        int actual = result.getResponse().getStatus();
        String payload = result.getResponse().getContentAsString();
        if (actual != status) {
            throw new AssertionError("expected status " + status + " but was " + actual + ", body=" + payload);
        }
        return objectMapper.readTree(payload);
    }
}
