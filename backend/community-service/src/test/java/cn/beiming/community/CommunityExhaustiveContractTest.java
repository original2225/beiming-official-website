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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CommunityServiceApplication.class, properties = "community.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CommunityExhaustiveContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicAndCommonContractEdgesAreEnforced() throws Exception {
        performJson(get("/api/v1/community/search").param("keyword", ""), 400, 40001);
        performJson(get("/api/v1/community/search").param("keyword", "社区").param("scope", "BAD"), 400, 40001);
        performJson(get("/api/v1/community/boards").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/community/boards").param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/community/boards/board-missing"), 404, 49000);
        performJson(get("/api/v1/community/posts/post-missing"), 404, 49001);
        performJson(get("/api/v1/community/posts/post-missing/comments"), 404, 49001);
        performJson(get("/api/v1/community/polls/poll-missing"), 404, 49003);
        assertNoSecrets(performJson(get("/api/v1/community/not-found"), 404));

        String memberOnlyBoard = createBoard(boardBody("member-only-board", "MEMBER_ONLY", "ACTIVE")).at("/data/boardId").asText();
        String staffOnlyBoard = createBoard(boardBody("staff-only-board", "STAFF_ONLY", "ACTIVE")).at("/data/boardId").asText();
        String archivedBoard = createBoard(boardBody("archived-board", "PUBLIC", "ACTIVE")).at("/data/boardId").asText();
        performJson(patch("/api/v1/community/admin/boards/" + archivedBoard + "/archive").header("Authorization", bearer("admin-token")),
                Map.of("reason", "归档板块", "idempotencyKey", "archive-board-1"), 200);

        JsonNode publicBoards = performJson(get("/api/v1/community/boards"), 200);
        assertThat(publicBoards.toString()).doesNotContain(memberOnlyBoard, staffOnlyBoard, archivedBoard);
        performJson(get("/api/v1/community/boards/" + memberOnlyBoard), 404, 49000);
        performJson(get("/api/v1/community/boards/" + archivedBoard), 404, 49000);
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")),
                postBody(archivedBoard, "archived-post-denied"), 409, 49010);
    }

    @Test
    void boardAndPostStateMachinesValidateAllCriticalTransitions() throws Exception {
        Map<String, Object> boardBody = boardBody("idempotent-board", "PUBLIC", "ACTIVE");
        String boardId = createBoard(boardBody).at("/data/boardId").asText();
        JsonNode boardReplay = performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")), boardBody, 200);
        assertThat(boardReplay.at("/data/boardId").asText()).isEqualTo(boardId);
        performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")),
                with(boardBody, "name", "另一个名称"), 409, 49017);
        performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")),
                with(boardBody("invalid-board", "PUBLIC", "ACTIVE"), "slug", "Bad Slug"), 400, 40001);
        performJson(patch("/api/v1/community/admin/boards/" + boardId).header("Authorization", bearer("admin-token")),
                Map.of("name", "改名板块", "reason", "修改板块", "idempotencyKey", "board-update-1"), 200);

        JsonNode created = createPost(boardId, "state-post-1");
        String postId = created.at("/data/postId").asText();
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody("draft-approve-denied"), 409, 49011);
        submitPost(postId, "state-post-submit-1");
        performJson(patch("/api/v1/community/me/posts/" + postId).header("Authorization", bearer("member-user-1-token")),
                Map.of("title", "待审不可改", "idempotencyKey", "pending-update-denied"), 409, 49011);
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/request-changes").header("Authorization", bearer("helper-token")),
                reviewBody("request-changes-1"), 200);
        performJson(patch("/api/v1/community/me/posts/" + postId).header("Authorization", bearer("member-user-1-token")),
                Map.of("body", "按审核意见修改", "idempotencyKey", "needs-changes-update"), 200);
        submitPost(postId, "needs-changes-submit");
        approvePost(postId, "needs-changes-approve");
        performJson(patch("/api/v1/community/me/posts/" + postId + "/withdraw").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "公开后不能撤回", "idempotencyKey", "withdraw-approved-denied"), 409, 49011);
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/delete").header("Authorization", bearer("admin-token")),
                Map.of("reason", "缺少确认", "idempotencyKey", "delete-no-confirm"), 403, 42003);
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/offline").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "下架", "reason", "治理", "idempotencyKey", "post-offline-state"), 200);
        performJson(get("/api/v1/community/posts/" + postId), 404, 49001);
    }

    @Test
    void commentReactionFavoriteAndPollEdgesStayConsistent() throws Exception {
        String postId = createAndApprove("comment-reaction-post");
        String commentId = createAndApproveComment(postId, "root-comment");
        String childId = createAndApproveComment(postId, "child-comment", commentId);
        performJson(post("/api/v1/community/me/posts/" + postId + "/comments").header("Authorization", bearer("member-user-2-token")),
                Map.of("parentCommentId", childId, "body", "三级回复", "idempotencyKey", "comment-depth-denied"), 409, 49023);
        performJson(patch("/api/v1/community/me/comments/" + commentId).header("Authorization", bearer("other-token")),
                Map.of("body", "别人不能改", "idempotencyKey", "comment-other-denied"), 404, 49002);
        performJson(patch("/api/v1/community/me/comments/" + commentId + "/archive").header("Authorization", bearer("member-user-2-token")),
                Map.of("reason", "归档评论", "idempotencyKey", "comment-archive-1"), 200);
        performJson(post("/api/v1/community/me/comments/" + commentId + "/like").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "archived-comment-like"), 404, 49002);

        JsonNode like = performJson(post("/api/v1/community/me/comments/" + childId + "/like").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "comment-like-1"), 200);
        assertThat(like.at("/data/likeCount").asInt()).isEqualTo(1);
        JsonNode unlike = performJson(delete("/api/v1/community/me/comments/" + childId + "/like").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(unlike.at("/data/likeCount").asInt()).isZero();

        JsonNode favorite = performJson(post("/api/v1/community/me/posts/" + postId + "/favorite").header("Authorization", bearer("member-user-2-token")),
                Map.of("idempotencyKey", "favorite-edge-1"), 200);
        assertThat(favorite.at("/data/favoriteCount").asInt()).isEqualTo(1);
        assertThat(performJson(delete("/api/v1/community/me/posts/" + postId + "/favorite").header("Authorization", bearer("member-user-2-token")), 200).at("/data/favoriteCount").asInt()).isZero();

        JsonNode poll = performJson(post("/api/v1/community/admin/polls").header("Authorization", bearer("admin-token")),
                pollBody(postId, "poll-edge-1", true, 1, 2), 201);
        String pollId = poll.at("/data/pollId").asText();
        performJson(get("/api/v1/community/polls/" + pollId), 404, 49003);
        performJson(patch("/api/v1/community/admin/polls/" + pollId + "/open").header("Authorization", bearer("admin-token")),
                Map.of("reason", "开放投票", "idempotencyKey", "poll-open-edge"), 200);
        performJson(patch("/api/v1/community/admin/polls/" + pollId).header("Authorization", bearer("admin-token")),
                Map.of("title", "开放后不能改", "reason", "修改投票", "idempotencyKey", "poll-update-open-denied"), 409, 49013);
        performJson(post("/api/v1/community/me/polls/" + pollId + "/votes").header("Authorization", bearer("member-user-1-token")),
                Map.of("optionIds", List.of(poll.at("/data/options/0/optionId").asText(), poll.at("/data/options/1/optionId").asText()), "idempotencyKey", "poll-multi-vote"), 200);
        performJson(patch("/api/v1/community/admin/polls/" + pollId + "/close").header("Authorization", bearer("admin-token")),
                Map.of("reason", "关闭投票", "idempotencyKey", "poll-close-edge"), 200);
        performJson(post("/api/v1/community/me/polls/" + pollId + "/votes").header("Authorization", bearer("member-user-2-token")),
                Map.of("optionIds", List.of(poll.at("/data/options/0/optionId").asText()), "idempotencyKey", "poll-closed-vote"), 409, 49020);
    }

    @Test
    void reportTicketPenaltyAndAuditEdgesArePermissionedAndAuditable() throws Exception {
        String postId = createAndApprove("governance-post");
        String commentId = createAndApproveComment(postId, "reportable-comment");
        JsonNode report = performJson(post("/api/v1/community/me/comments/" + commentId + "/reports").header("Authorization", bearer("member-user-1-token")),
                reportBody("comment-report-1"), 201);
        String reportId = report.at("/data/reportId").asText();
        assertThat(report.at("/data/reporter").isNull()).isTrue();
        performJson(post("/api/v1/community/me/comments/" + commentId + "/reports").header("Authorization", bearer("member-user-1-token")),
                reportBody("comment-report-2"), 409, 49021);
        performJson(patch("/api/v1/community/admin/reports/" + reportId + "/assign").header("Authorization", bearer("helper-token")),
                Map.of("assigneeUserId", "admin", "reason", "越权分配", "idempotencyKey", "report-assign-other-denied"), 403, 42001);
        performJson(patch("/api/v1/community/admin/reports/" + reportId + "/resolve").header("Authorization", bearer("helper-token")),
                Map.of("resolution", "已处理", "reason", "处理举报", "idempotencyKey", "report-resolve-1"), 200);
        performJson(patch("/api/v1/community/admin/reports/" + reportId + "/dismiss").header("Authorization", bearer("helper-token")),
                Map.of("resolution", "重复处理", "reason", "重复", "idempotencyKey", "report-dismiss-after-resolve"), 409, 49014);

        JsonNode ticket = performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("member-user-1-token")),
                ticketBody("ticket-edge-1", "BUG_REPORT"), 201);
        String ticketId = ticket.at("/data/ticketId").asText();
        performJson(get("/api/v1/community/me/tickets/" + ticketId).header("Authorization", bearer("member-user-2-token")), 404, 49005);
        performJson(post("/api/v1/community/admin/tickets/" + ticketId + "/messages").header("Authorization", bearer("helper-token")),
                Map.of("messageType", "INTERNAL_NOTE", "body", "内部备注", "reason", "内部协作", "idempotencyKey", "helper-internal-note-denied"), 403, 42001);
        performJson(post("/api/v1/community/admin/tickets/" + ticketId + "/messages").header("Authorization", bearer("admin-token")),
                Map.of("messageType", "INTERNAL_NOTE", "body", "内部备注", "reason", "内部协作", "idempotencyKey", "admin-internal-note"), 201);
        assertThat(performJson(get("/api/v1/community/me/tickets/" + ticketId).header("Authorization", bearer("member-user-1-token")), 200).toString()).doesNotContain("INTERNAL_NOTE", "内部备注");
        performJson(patch("/api/v1/community/admin/tickets/" + ticketId + "/status").header("Authorization", bearer("helper-token")),
                Map.of("status", "ARCHIVED", "reason", "非法终态", "idempotencyKey", "ticket-archive-denied"), 409, 49015);
        performJson(post("/api/v1/community/me/tickets/" + ticketId + "/close").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "关闭工单", "idempotencyKey", "ticket-close-edge"), 200);
        performJson(patch("/api/v1/community/me/tickets/" + ticketId).header("Authorization", bearer("member-user-1-token")),
                Map.of("body", "关闭后不能补充", "idempotencyKey", "ticket-append-closed"), 409, 49015);

        performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("helper-token")),
                penaltyBody("member-user-2", "penalty-helper-denied", "MUTE"), 403, 42001);
        performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("admin-token")),
                with(penaltyBody("member-user-2", "penalty-confirm-denied", "MUTE"), "confirmText", "BAD"), 403, 42003);
        JsonNode mute = performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("admin-token")),
                penaltyBody("member-user-2", "penalty-mute-1", "MUTE"), 201);
        performJson(post("/api/v1/community/me/posts/" + postId + "/comments").header("Authorization", bearer("member-user-2-token")),
                Map.of("body", "被禁言不能评论", "idempotencyKey", "muted-comment-denied"), 409, 49022);
        String penaltyId = mute.at("/data/penaltyId").asText();
        performJson(patch("/api/v1/community/admin/penalties/" + penaltyId).header("Authorization", bearer("helper-token")),
                Map.of("publicReason", "helper 不可修正", "reason", "越权", "idempotencyKey", "penalty-helper-update-denied"), 403, 42001);
        performJson(patch("/api/v1/community/admin/penalties/" + penaltyId + "/revoke").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "解除禁言", "reason", "解除", "confirmText", "REVOKE_COMMUNITY_PENALTY", "idempotencyKey", "penalty-revoke-edge"), 200);
        performJson(patch("/api/v1/community/admin/penalties/" + penaltyId).header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "解除后不能修正", "reason", "非法状态", "idempotencyKey", "penalty-update-revoked-denied"), 409, 49016);
        performJson(get("/api/v1/community/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/community/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 40001);
    }

    @Test
    void dependencyFailureRollbackAndConcurrencyEdgesDoNotLeaveHalfState() throws Exception {
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("auth-unavailable-token")),
                postBody("board-general", "auth-down"), 502, 49200);
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("auth-timeout-token")),
                postBody("board-general", "auth-timeout"), 504, 49201);
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("auth-bad-token")),
                postBody("board-general", "auth-bad-schema"), 502, 49202);
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("pending-profile-token")),
                postBody("board-general", "pending-profile-post"), 409, 49022);
        performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("pending-profile-token")),
                ticketBody("pending-ticket-denied", "BUG_REPORT"), 409, 49022);
        performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("pending-profile-token")),
                ticketBody("pending-ticket-ok", "ACCOUNT_ISSUE"), 201);

        performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")).header("X-Test-Fail-Store", "true"),
                boardBody("store-fail-board", "PUBLIC", "ACTIVE"), 500, 54002);
        assertThat(performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")), 200).toString()).doesNotContain("store-fail-board");

        String postId = createAndApprove("reaction-fail-post");
        performJson(post("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")).header("X-Test-Fail-Reaction", "true"),
                Map.of("idempotencyKey", "reaction-fail-key"), 500, 54003);
        assertThat(performJson(get("/api/v1/community/posts/" + postId), 200).at("/data/likeCount").asInt()).isZero();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> performStatus(post("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")), Map.of("idempotencyKey", "concurrent-like-1")),
                    () -> performStatus(post("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")), Map.of("idempotencyKey", "concurrent-like-2"))
            );
            List<Future<Integer>> results = executor.invokeAll(tasks);
            assertThat(results.get(0).get()).isEqualTo(200);
            assertThat(results.get(1).get()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }
        assertThat(performJson(get("/api/v1/community/posts/" + postId), 200).at("/data/likeCount").asInt()).isEqualTo(1);
    }

    private JsonNode createBoard(Map<String, Object> body) throws Exception {
        return performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")), body, 201);
    }

    private JsonNode createPost(String boardId, String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")), postBody(boardId, idempotencyKey), 201);
    }

    private String createAndApprove(String idempotencyKey) throws Exception {
        JsonNode post = createPost("board-general", idempotencyKey);
        String postId = post.at("/data/postId").asText();
        submitPost(postId, idempotencyKey + "-submit");
        approvePost(postId, idempotencyKey + "-approve");
        return postId;
    }

    private String createAndApproveComment(String postId, String idempotencyKey) throws Exception {
        return createAndApproveComment(postId, idempotencyKey, null);
    }

    private String createAndApproveComment(String postId, String idempotencyKey, String parentCommentId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("body", "评论正文 " + idempotencyKey);
        body.put("idempotencyKey", idempotencyKey);
        if (parentCommentId != null) body.put("parentCommentId", parentCommentId);
        JsonNode comment = performJson(post("/api/v1/community/me/posts/" + postId + "/comments").header("Authorization", bearer("member-user-2-token")), body, 201);
        String commentId = comment.at("/data/commentId").asText();
        performJson(patch("/api/v1/community/admin/comments/" + commentId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody(idempotencyKey + "-approve"), 200);
        return commentId;
    }

    private void submitPost(String postId, String idempotencyKey) throws Exception {
        performJson(post("/api/v1/community/me/posts/" + postId + "/submit").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", idempotencyKey), 200);
    }

    private void approvePost(String postId, String idempotencyKey) throws Exception {
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody(idempotencyKey), 200);
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
        assertThat(json.at("/requestId").asText()).isNotBlank();
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
        assertThat(json.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(json);
        return json;
    }

    private int performStatus(MockHttpServletRequestBuilder builder, Map<String, Object> body) throws Exception {
        MvcResult result = mvc.perform(builder.contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private Map<String, Object> boardBody(String idempotencyKey, String visibility, String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "board-" + idempotencyKey);
        body.put("name", "社区板块");
        body.put("description", "用于完备测试");
        body.put("visibility", visibility);
        body.put("status", status);
        body.put("allowedPostTypes", List.of("DISCUSSION", "QUESTION", "SUGGESTION", "RESOURCE_DISCUSSION"));
        body.put("tags", List.of("community", "test"));
        body.put("sortOrder", 10);
        body.put("reason", "创建板块");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "社区帖子标题");
        body.put("summary", "社区帖子摘要");
        body.put("body", "社区帖子正文，覆盖状态机、治理和公开展示。");
        body.put("tags", List.of("community"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核意见", "internalNote", "private note", "reason", "审核原因", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> pollBody(String postId, String idempotencyKey, boolean multipleChoice, int minChoices, int maxChoices) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", postId);
        body.put("title", "社区投票");
        body.put("description", "测试投票");
        body.put("options", List.of(Map.of("label", "方案一"), Map.of("label", "方案二"), Map.of("label", "方案三")));
        body.put("multipleChoice", multipleChoice);
        body.put("minChoices", minChoices);
        body.put("maxChoices", maxChoices);
        body.put("eligibleVisibility", "PUBLIC");
        body.put("anonymousResult", true);
        body.put("reason", "创建投票");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reportBody(String idempotencyKey) {
        return Map.of("reasonType", "SPAM", "description", "这个内容疑似灌水，需要处理", "evidenceLinks", List.of("https://example.com/evidence"), "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> ticketBody(String idempotencyKey, String type) {
        return Map.of("type", type, "title", "工单标题", "body", "工单正文", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> penaltyBody(String userId, String idempotencyKey, String type) {
        return Map.of("targetUserId", userId, "type", type, "publicReason", "社区处罚", "reason", "治理原因", "confirmText", "CREATE_COMMUNITY_PENALTY", "idempotencyKey", idempotencyKey);
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
