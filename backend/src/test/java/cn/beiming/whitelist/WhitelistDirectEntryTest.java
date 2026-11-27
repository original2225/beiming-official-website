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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 管理员「系统录入」白名单（花蝉 2026-11-27 定）。
 *
 * <p>规则：白名单不走邀请码，管理员在官网后台直接把玩家录进白名单。
 * 这条路不需要考核、不需要 {@code examSessionId}、不消耗邀请码。
 *
 * <p>锁住三条不能错的边界：
 * <ul>
 *   <li>录入即 {@code APPROVED}，不能还挂在待审队列里；</li>
 *   <li>档案必须真的建好（{@code profileActivation=ACTIVATED}），
 *       不许出现「状态通过了但档案是空的」；</li>
 *   <li>游戏昵称必须填——白名单对不上人就没有意义；服型要落进记录。</li>
 * </ul>
 */
@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = {
        "server.port=8143",
        "whitelist.test-controls.enabled=true",
        "whitelist.auth.stub-tokens-enabled=true",
        "whitelist.stub-handoffs-enabled=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WhitelistDirectEntryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("WL-DIRECT-001 管理员录入即 APPROVED，档案建好，服型与群昵称都落库")
    void directEntryCreatesApprovedWhitelist() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("minecraftName", "DirectSteve");
        body.put("minecraftPlatform", "OFFLINE");
        body.put("serverType", "SURVIVAL");
        body.put("qqGroupNickname", "北冥-直录");
        body.put("displayName", "直录玩家");
        body.put("userId", "direct-target-user");
        body.put("reason", "manager vouches for this player");
        body.put("idempotencyKey", "direct-entry-1");

        JsonNode created = performJson(post("/api/v1/whitelist/admin/applications/direct")
                .header("Authorization", bearer("admin-token")), body, 201);

        assertThat(created.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(created.at("/data/result").asText()).isEqualTo("APPROVED");
        assertThat(created.at("/data/profileActivation/status").asText()).isEqualTo("ACTIVATED");
        assertThat(created.at("/data/serverType").asText()).isEqualTo("SURVIVAL");
        assertThat(created.at("/data/qqGroupNickname").asText()).isEqualTo("北冥-直录");
        // 没有考核这一环，如实留空，不编造
        assertThat(created.at("/data/examSessionId").asText()).isEqualTo("direct-entry");
        assertThat(created.at("/data/examPassedAt").isNull()).isTrue();
    }

    @Test
    @DisplayName("WL-DIRECT-002 不填游戏昵称直接 400，白名单对不上人等于没录")
    void minecraftNameRequired() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("serverType", "MODPACK");
        body.put("reason", "missing name");
        body.put("idempotencyKey", "direct-entry-noname");

        MvcResult result = mvc.perform(post("/api/v1/whitelist/admin/applications/direct")
                        .header("Authorization", bearer("admin-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("WL-DIRECT-003 普通玩家无权录入，必须 ADMIN/OWNER")
    void playerCannotDirectEntry() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("minecraftName", "SneakySteve");
        body.put("reason", "try it");
        body.put("idempotencyKey", "direct-entry-forbidden");

        MvcResult result = mvc.perform(post("/api/v1/whitelist/admin/applications/direct")
                        .header("Authorization", bearer("user-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    @DisplayName("WL-DIRECT-004 玩家还没注册官网时录入：白名单照给，但档案如实标记待关联，不谎报 ACTIVATED")
    void placeholderUserGetsApprovedButProfilePendingBinding() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("minecraftName", "NotRegisteredYet");
        body.put("minecraftPlatform", "OFFLINE");
        body.put("serverType", "MODPACK");
        body.put("reason", "player has no website account yet");
        body.put("idempotencyKey", "direct-entry-placeholder");
        // 刻意不传 userId：纯游戏玩家，只在 MC 里存在

        JsonNode created = performJson(post("/api/v1/whitelist/admin/applications/direct")
                .header("Authorization", bearer("admin-token")), body, 201);

        // 白名单该给就给——管理员有权录一个没注册官网的人
        assertThat(created.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(created.at("/data/result").asText()).isEqualTo("APPROVED");
        // 但没有 auth 用户就等于没有档案可建，不能谎报 ACTIVATED
        assertThat(created.at("/data/profileActivation/status").asText())
                .isEqualTo("PENDING_USER_BINDING");
        assertThat(created.at("/data/profileActivation/memberId").isNull()).isTrue();
        // 占位 userId 形如 direct-<序号>，便于日后人工关联真实账号
        assertThat(created.at("/data/userId").asText()).startsWith("direct-");
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
