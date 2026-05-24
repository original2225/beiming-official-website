package cn.beiming.community;

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

@SpringBootTest(classes = CommunityServiceApplication.class, properties = "community.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CommunityContractHardeningGapTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicPostViewCountIsDeduplicatedByServerSideAccessFingerprint() throws Exception {
        String postId = createAndApprove("view-dedupe-post");

        JsonNode first = performJson(get("/api/v1/community/posts/" + postId)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("User-Agent", "contract-hardening"), 200);
        JsonNode second = performJson(get("/api/v1/community/posts/" + postId)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("User-Agent", "contract-hardening"), 200);
        JsonNode third = performJson(get("/api/v1/community/posts/" + postId)
                .header("X-Forwarded-For", "203.0.113.11")
                .header("User-Agent", "contract-hardening"), 200);

        assertThat(first.at("/data/viewCount").asInt()).isEqualTo(1);
        assertThat(second.at("/data/viewCount").asInt()).isEqualTo(1);
        assertThat(third.at("/data/viewCount").asInt()).isEqualTo(2);
    }

    @Test
    void pollTimeWindowAndStaffOnlyEligibilityAreEnforced() throws Exception {
        String postId = createAndApprove("poll-hardening-post");
        performJson(post("/api/v1/community/admin/polls").header("Authorization", bearer("admin-token")),
                with(pollBody(postId, "poll-window-invalid"), "closesAt", "2026-05-24T11:00:00Z"), 400, 40001);

        JsonNode futurePoll = performJson(post("/api/v1/community/admin/polls").header("Authorization", bearer("admin-token")),
                with(with(pollBody(postId, "poll-window-future"), "opensAt", "2026-05-24T13:00:00Z"), "closesAt", "2026-05-24T14:00:00Z"), 201);
        String futurePollId = futurePoll.at("/data/pollId").asText();
        performJson(patch("/api/v1/community/admin/polls/" + futurePollId + "/open").header("Authorization", bearer("admin-token")),
                Map.of("reason", "开放投票", "idempotencyKey", "poll-window-open"), 200);
        performJson(post("/api/v1/community/me/polls/" + futurePollId + "/votes").header("Authorization", bearer("member-user-1-token")),
                Map.of("optionIds", List.of(futurePoll.at("/data/options/0/optionId").asText()), "idempotencyKey", "poll-before-open"), 409, 49020);

        JsonNode staffPoll = performJson(post("/api/v1/community/admin/polls").header("Authorization", bearer("admin-token")),
                with(pollBody(postId, "poll-staff-only"), "eligibleVisibility", "STAFF_ONLY"), 201);
        String staffPollId = staffPoll.at("/data/pollId").asText();
        performJson(patch("/api/v1/community/admin/polls/" + staffPollId + "/open").header("Authorization", bearer("admin-token")),
                Map.of("reason", "开放员工投票", "idempotencyKey", "poll-staff-open"), 200);
        performJson(post("/api/v1/community/me/polls/" + staffPollId + "/votes").header("Authorization", bearer("member-user-1-token")),
                Map.of("optionIds", List.of(staffPoll.at("/data/options/0/optionId").asText()), "idempotencyKey", "poll-staff-member-denied"), 403, 42001);
        performJson(post("/api/v1/community/me/polls/" + staffPollId + "/votes").header("Authorization", bearer("helper-token")),
                Map.of("optionIds", List.of(staffPoll.at("/data/options/0/optionId").asText()), "idempotencyKey", "poll-staff-helper-ok"), 200);
    }

    @Test
    void reportEvidenceLinksAndLinkedPenaltyAreValidatedAndPersisted() throws Exception {
        String postId = createAndApprove("report-hardening-post");
        performJson(post("/api/v1/community/me/posts/" + postId + "/reports").header("Authorization", bearer("member-user-2-token")),
                reportBody("report-bad-evidence", List.of("ftp://example.com/evidence")), 400, 40001);

        JsonNode report = performJson(post("/api/v1/community/me/posts/" + postId + "/reports").header("Authorization", bearer("member-user-2-token")),
                reportBody("report-linked-penalty", List.of("/community/evidence/1")), 201);
        JsonNode penalty = performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("admin-token")),
                penaltyBody("member-user-1", "report-linked-penalty-create"), 201);
        String reportId = report.at("/data/reportId").asText();
        String penaltyId = penalty.at("/data/penaltyId").asText();

        performJson(patch("/api/v1/community/admin/reports/" + reportId + "/resolve").header("Authorization", bearer("helper-token")),
                Map.of("resolution", "已处罚", "linkedPenaltyId", penaltyId, "reason", "举报成立", "idempotencyKey", "report-linked-resolve"), 200);
        JsonNode adminReport = performJson(get("/api/v1/community/admin/reports/" + reportId).header("Authorization", bearer("helper-token")), 200);
        JsonNode myReport = performJson(get("/api/v1/community/me/reports").header("Authorization", bearer("member-user-2-token")), 200);

        assertThat(adminReport.at("/data/linkedPenaltyId").asText()).isEqualTo(penaltyId);
        assertThat(myReport.toString()).doesNotContain("linkedPenaltyId", penaltyId);
    }

    @Test
    void ticketsPersistSafeRelatedObjectAndAttachments() throws Exception {
        performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("member-user-1-token")),
                with(ticketBody("ticket-bad-attachment"), "attachments", List.of(Map.of("attachmentId", "att-bad", "name", "bad", "url", "https://evil.example/a.png"))),
                400, 40001);

        Map<String, Object> body = ticketBody("ticket-safe-attachment");
        body.put("relatedObject", Map.of("type", "POST", "id", "post-1", "title", "关联帖子", "authorizationHeader", "secret-token"));
        body.put("attachments", List.of(Map.of("attachmentId", "att-1", "name", "截图.png", "url", "/uploads/tickets/att-1.png")));
        JsonNode ticket = performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("member-user-1-token")), body, 201);
        String ticketId = ticket.at("/data/ticketId").asText();

        assertThat(ticket.at("/data/relatedObject/title").asText()).isEqualTo("关联帖子");
        assertThat(ticket.at("/data/messages/0/attachments/0/url").asText()).isEqualTo("/uploads/tickets/att-1.png");
        assertNoSecrets(ticket);

        JsonNode replied = performJson(post("/api/v1/community/admin/tickets/" + ticketId + "/messages").header("Authorization", bearer("admin-token")),
                Map.of("messageType", "STAFF_REPLY", "body", "请看处理截图", "attachments", List.of(Map.of("attachmentId", "att-2", "name", "处理.png", "url", "/uploads/tickets/att-2.png")), "reason", "回复工单", "idempotencyKey", "ticket-reply-attachment"),
                201);
        assertThat(replied.at("/data/messages/1/attachments/0/attachmentId").asText()).isEqualTo("att-2");
        assertNoSecrets(replied);
    }

    @Test
    void revokePenaltyRollsBackWhenAuditFails() throws Exception {
        JsonNode penalty = performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("admin-token")),
                penaltyBody("member-user-1", "penalty-audit-fail-create"), 201);
        String penaltyId = penalty.at("/data/penaltyId").asText();

        performJson(patch("/api/v1/community/admin/penalties/" + penaltyId + "/revoke")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                Map.of("publicReason", "解除限制", "reason", "审计失败", "confirmText", "REVOKE_COMMUNITY_PENALTY", "idempotencyKey", "penalty-audit-fail-revoke"),
                500, 54001);
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")),
                postBody("board-general", "penalty-still-active"), 409, 49022);
    }

    private String createAndApprove(String idempotencyKey) throws Exception {
        JsonNode post = performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")),
                postBody("board-general", idempotencyKey), 201);
        String postId = post.at("/data/postId").asText();
        performJson(post("/api/v1/community/me/posts/" + postId + "/submit").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", idempotencyKey + "-submit"), 200);
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody(idempotencyKey + "-approve"), 200);
        return postId;
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
        assertNoSecrets(json);
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder.contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertNoSecrets(json);
        return json;
    }

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "社区帖子标题");
        body.put("summary", "社区帖子摘要");
        body.put("body", "社区帖子正文，覆盖硬化契约。");
        body.put("tags", List.of("community"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "reason", "符合社区规则", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> pollBody(String postId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", postId);
        body.put("title", "社区投票");
        body.put("description", "硬化投票");
        body.put("options", List.of(Map.of("label", "方案一"), Map.of("label", "方案二")));
        body.put("multipleChoice", false);
        body.put("minChoices", 1);
        body.put("maxChoices", 1);
        body.put("eligibleVisibility", "PUBLIC");
        body.put("anonymousResult", true);
        body.put("opensAt", "2026-05-24T11:00:00Z");
        body.put("closesAt", "2026-05-24T13:00:00Z");
        body.put("reason", "创建投票");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reportBody(String idempotencyKey, List<String> evidenceLinks) {
        return Map.of("reasonType", "SPAM", "description", "这个内容疑似灌水，需要处理", "evidenceLinks", evidenceLinks, "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> ticketBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "BUG_REPORT");
        body.put("title", "工单标题");
        body.put("body", "工单正文");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> penaltyBody(String userId, String idempotencyKey) {
        return Map.of("targetUserId", userId, "type", "POST_RESTRICTED", "publicReason", "临时限制发帖", "reason", "社区治理处罚", "confirmText", "CREATE_COMMUNITY_PENALTY", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "notification body",
                "internalNote", "private note", "profile internal", "content internal", "resource internal",
                "server.properties", "whitelist add", "whitelist remove", "node-daemon", "terminal",
                "container", "cloudreveToken", "attendancePoints", "scoreBalance", "leaderboard"
        );
    }
}
