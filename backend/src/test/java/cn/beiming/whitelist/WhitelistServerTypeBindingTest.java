package cn.beiming.whitelist;

import cn.beiming.admission.AdmissionCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

/**
 * 服型与申请时绑定的契约（花蝉 2026-11-27 定的流程）。
 *
 * <p>背景：注册阶段已不绑定游戏账号（查得到昵称 ≠ 拥有它），绑定挪到「考核通过后申请邀请码」这一步。
 * 于是绑定的强制程度必须由服型决定：
 * <ul>
 *   <li><b>SURVIVAL 原版生电服</b> —— 进服必须过白名单且有一致游戏身份，云端已按 13+ 条生电服服规
 *       出题，申请时<b>强制</b>填平台与昵称；</li>
 *   <li><b>MODPACK 整合包服</b> —— 只要求遵守基本友好合作守则，申请时<b>建议</b>填，留空放行。</li>
 * </ul>
 *
 * <p>本上下文只装配 whitelist 模块，所以只覆盖「要不要填」这一层；
 * 「填了但平台上查无此人」的拒绝路径（43102）依赖真实 resolver，在集成环境验证。
 */
@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = {
        "server.port=8137",
        "whitelist.test-controls.enabled=true",
        "whitelist.auth.stub-tokens-enabled=true",
        "whitelist.stub-handoffs-enabled=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WhitelistServerTypeBindingTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("WL-SRV-001 生电服必须绑游戏账号：不填平台/昵称直接 400/44013")
    void survivalRequiresMinecraftBinding() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/whitelist/me/applications")
                        .header("Authorization", bearer("user-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody("session-passed", "srv-survival-nobind", "SURVIVAL"))))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.at("/code").asInt()).isEqualTo(44013);
    }

    @Test
    @DisplayName("WL-SRV-002 整合包留空绑定放行，且服型写入申请记录")
    void modpackAllowsEmptyBinding() throws Exception {
        JsonNode created = performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")),
                createBody("session-passed", "srv-modpack-nobind", "MODPACK"), 201);
        String id = created.at("/data/applicationId").asText();
        assertThat(created.at("/data/serverType").asText()).isEqualTo("MODPACK");
        // 留空绑定不报错是关键；若考核快照里本来就带绑定（老账号），沿用它而不是抹掉
        assertThat(created.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        // 管理端读回同一份服型，审核时才知道该按哪套规则核
        JsonNode detail = performJson(get("/api/v1/whitelist/admin/applications/" + id)
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(detail.at("/data/serverType").asText()).isEqualTo("MODPACK");
    }

    @Test
    @DisplayName("WL-SRV-003 生电服填了平台但没填昵称 → 400，不能只填一半")
    void survivalRejectsPlatformWithoutName() throws Exception {
        Map<String, Object> body = createBody("session-passed", "srv-survival-half", "SURVIVAL");
        body.put("minecraftPlatform", "LITTLESKIN");
        MvcResult result = mvc.perform(post("/api/v1/whitelist/me/applications")
                        .header("Authorization", bearer("user-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).at("/code").asInt())
                .isEqualTo(44013);
    }

    @Test
    @DisplayName("WL-SRV-004 服型只认 SURVIVAL/MODPACK，别的值一律 400")
    void unknownServerTypeIsRejected() throws Exception {
        performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")),
                createBody("session-passed", "srv-bogus", "NETHER"), 400, 40001);
    }

    @Test
    @DisplayName("WL-SRV-005 QQ 群昵称会跟着申请存下来，供审核端并排核对")
    void qqGroupNicknameIsPersisted() throws Exception {
        Map<String, Object> body = createBody("session-passed", "srv-qqnick", "MODPACK");
        body.put("qqGroupNickname", "北冥小助手");
        JsonNode created = performJson(post("/api/v1/whitelist/me/applications")
                .header("Authorization", bearer("user-token")), body, 201);
        assertThat(created.at("/data/qqGroupNickname").asText()).isEqualTo("北冥小助手");
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

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON)).andReturn();
        int actual = result.getResponse().getStatus();
        String payload = result.getResponse().getContentAsString();
        if (actual != status) {
            throw new AssertionError("expected status " + status + " but was " + actual + ", body=" + payload);
        }
        return objectMapper.readTree(payload);
    }
}