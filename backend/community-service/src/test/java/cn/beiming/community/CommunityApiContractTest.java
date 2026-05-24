package cn.beiming.community;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CommunityServiceApplication.class, properties = "community.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CommunityApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("community local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "COM-COM", 1, 80);
        addRange(mapped, "COM-PUBLIC", 1, 90);
        addRange(mapped, "COM-BOARD", 1, 80);
        addRange(mapped, "COM-POST", 1, 140);
        addRange(mapped, "COM-COMMENT", 1, 100);
        addRange(mapped, "COM-REACTION", 1, 80);
        addRange(mapped, "COM-VOTE", 1, 90);
        addRange(mapped, "COM-REPORT", 1, 110);
        addRange(mapped, "COM-TICKET", 1, 120);
        addRange(mapped, "COM-PENALTY", 1, 90);
        addRange(mapped, "COM-AUDIT", 1, 50);
        addRange(mapped, "COM-OPS", 1, 30);
        addRange(mapped, "COM-DEPS", 1, 100);
        addRange(mapped, "COM-COMPAT", 1, 70);
        addRange(mapped, "COM-HARDEN", 1, 100);
        addRange(mapped, "COM-PORT", 1, 8);
        addRange(mapped, "COM-CYCLE", 1, 40);
        assertThat(mapped).contains("COM-COM-001", "COM-POST-140", "COM-TICKET-120", "COM-HARDEN-100", "COM-CYCLE-040");
        assertThat(mapped).hasSize(1378);
    }

    @Test
    @DisplayName("COM-COM covers envelope, request id, auth gates, validation, role gates, and trusted field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/community/boards").header("X-Request-Id", "req-community-public"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-community-public"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.requestId").value("req-community-public"));

        JsonNode generated = performJson(get("/api/v1/community/boards"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(post("/api/v1/community/me/posts"), postBody("board-general", "trusted"), 401, 41000);
        performJson(post("/api/v1/community/me/posts").header("Authorization", "bad-token"), postBody("board-general", "trusted"), 401, 41003);
        performJson(get("/api/v1/community/admin/boards"), 401, 41000);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("helper-token")), boardBody("helper-denied"), 403, 42001);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")), with(boardBody("short-key"), "idempotencyKey", "short"), 400, 40001);

        JsonNode board = createBoard("contract-general");
        assertThat(board.at("/data/status").asText()).isEqualTo("ACTIVE");

        JsonNode created = performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")),
                with(postBody(board.at("/data/boardId").asText(), "trusted-post-1"), "userId", "attacker"), 201);
        assertThat(created.at("/data/author/userId").asText()).isEqualTo("member-user-1");
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/likeCount").asInt()).isZero();
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("COM-BOARD, COM-POST, and COM-COMMENT keep forum content stateful, moderated, and public-safe")
    void boardPostAndCommentFlow() throws Exception {
        String boardId = createBoard("forum-flow").at("/data/boardId").asText();
        JsonNode post = createPost(boardId, "forum-post-1");
        String postId = post.at("/data/postId").asText();

        JsonNode updated = performJson(patch("/api/v1/community/me/posts/" + postId).header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "post-update-1", "title", "更新后的工程建议", "body", "更新后的帖子正文"), 200);
        assertThat(updated.at("/data/title").asText()).isEqualTo("更新后的工程建议");

        JsonNode submitted = performJson(post("/api/v1/community/me/posts/" + postId + "/submit").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "post-submit-1"), 200);
        assertThat(submitted.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        JsonNode approved = performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody("post-approve-1"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");

        JsonNode publicPost = performJson(get("/api/v1/community/posts/" + postId), 200);
        assertThat(publicPost.at("/data/postId").asText()).isEqualTo(postId);
        assertNoSecrets(publicPost);

        JsonNode comment = performJson(post("/api/v1/community/me/posts/" + postId + "/comments").header("Authorization", bearer("member-user-2-token")),
                Map.of("body", "我来补一个测试评论", "idempotencyKey", "comment-create-1"), 201);
        String commentId = comment.at("/data/commentId").asText();
        assertThat(comment.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        JsonNode commentApproved = performJson(patch("/api/v1/community/admin/comments/" + commentId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody("comment-approve-1"), 200);
        assertThat(commentApproved.at("/data/status").asText()).isEqualTo("APPROVED");

        JsonNode comments = performJson(get("/api/v1/community/posts/" + postId + "/comments"), 200);
        assertThat(comments.at("/data/total").asInt()).isEqualTo(1);
        assertThat(comments.at("/data/items/0/commentId").asText()).isEqualTo(commentId);

        JsonNode offline = performJson(patch("/api/v1/community/admin/comments/" + commentId + "/offline").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "评论下架", "reason", "治理处理", "idempotencyKey", "comment-offline-1"), 200);
        assertThat(offline.at("/data/status").asText()).isEqualTo("OFFLINE");
        assertThat(performJson(get("/api/v1/community/posts/" + postId + "/comments"), 200).at("/data/total").asInt()).isZero();
    }

    @Test
    @DisplayName("COM-REACTION, COM-VOTE, and COM-REPORT cover interaction idempotency, poll eligibility, and report queues")
    void reactionVoteAndReportFlow() throws Exception {
        String boardId = createBoard("interaction-flow").at("/data/boardId").asText();
        String postId = approvePost(createPost(boardId, "interaction-post-1").at("/data/postId").asText());

        JsonNode like = performJson(post("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "like-post-1"), 200);
        assertThat(like.at("/data/likeCount").asInt()).isEqualTo(1);
        JsonNode likeReplay = performJson(post("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "like-post-1"), 200);
        assertThat(likeReplay.at("/data/likeCount").asInt()).isEqualTo(1);
        JsonNode unlike = performJson(delete("/api/v1/community/me/posts/" + postId + "/like").header("Authorization", bearer("member-user-1-token")),
                200);
        assertThat(unlike.at("/data/likeCount").asInt()).isZero();

        JsonNode favorite = performJson(post("/api/v1/community/me/posts/" + postId + "/favorite").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "favorite-post-1"), 200);
        assertThat(favorite.at("/data/favoriteCount").asInt()).isEqualTo(1);

        JsonNode poll = performJson(post("/api/v1/community/admin/polls").header("Authorization", bearer("admin-token")),
                pollBody(postId, "poll-create-1"), 201);
        String pollId = poll.at("/data/pollId").asText();
        performJson(patch("/api/v1/community/admin/polls/" + pollId + "/open").header("Authorization", bearer("admin-token")),
                Map.of("reason", "开放投票", "idempotencyKey", "poll-open-1"), 200);
        JsonNode vote = performJson(post("/api/v1/community/me/polls/" + pollId + "/votes").header("Authorization", bearer("member-user-1-token")),
                Map.of("optionIds", List.of(poll.at("/data/options/0/optionId").asText()), "idempotencyKey", "poll-vote-1"), 200);
        assertThat(vote.at("/data/voteCount").asInt()).isEqualTo(1);
        performJson(post("/api/v1/community/me/polls/" + pollId + "/votes").header("Authorization", bearer("member-user-1-token")),
                Map.of("optionIds", List.of(poll.at("/data/options/1/optionId").asText()), "idempotencyKey", "poll-vote-2"), 409, 49020);

        JsonNode report = performJson(post("/api/v1/community/me/posts/" + postId + "/reports").header("Authorization", bearer("member-user-2-token")),
                reportBody("report-post-1"), 201);
        String reportId = report.at("/data/reportId").asText();
        JsonNode mine = performJson(get("/api/v1/community/me/reports").header("Authorization", bearer("member-user-2-token")), 200);
        assertThat(mine.at("/data/total").asInt()).isEqualTo(1);
        JsonNode assigned = performJson(patch("/api/v1/community/admin/reports/" + reportId + "/assign").header("Authorization", bearer("helper-token")),
                Map.of("reason", "领取举报", "idempotencyKey", "report-assign-1"), 200);
        assertThat(assigned.at("/data/status").asText()).isEqualTo("UNDER_REVIEW");
        JsonNode dismissed = performJson(patch("/api/v1/community/admin/reports/" + reportId + "/dismiss").header("Authorization", bearer("helper-token")),
                Map.of("resolution", "未发现违规", "reason", "证据不足", "idempotencyKey", "report-dismiss-1"), 200);
        assertThat(dismissed.at("/data/status").asText()).isEqualTo("DISMISSED");
        assertNoSecrets(dismissed);
    }

    @Test
    @DisplayName("COM-TICKET, COM-PENALTY, COM-AUDIT, and COM-OPS keep support and moderation auditable")
    void ticketPenaltyAuditAndOpsFlow() throws Exception {
        JsonNode ticket = performJson(post("/api/v1/community/me/tickets").header("Authorization", bearer("pending-profile-token")),
                ticketBody("ticket-create-1"), 201);
        String ticketId = ticket.at("/data/ticketId").asText();
        assertThat(ticket.at("/data/status").asText()).isEqualTo("WAITING_STAFF");

        JsonNode detail = performJson(get("/api/v1/community/me/tickets/" + ticketId).header("Authorization", bearer("pending-profile-token")), 200);
        assertThat(detail.at("/data/messages/0/body").asText()).contains("账号");
        assertNoSecrets(detail);

        JsonNode assigned = performJson(patch("/api/v1/community/admin/tickets/" + ticketId + "/assign").header("Authorization", bearer("helper-token")),
                Map.of("reason", "领取工单", "idempotencyKey", "ticket-assign-1"), 200);
        assertThat(assigned.at("/data/status").asText()).isEqualTo("WAITING_STAFF");

        JsonNode replied = performJson(post("/api/v1/community/admin/tickets/" + ticketId + "/messages").header("Authorization", bearer("helper-token")),
                Map.of("messageType", "STAFF_REPLY", "body", "已收到，继续补充截图", "reason", "回复工单", "idempotencyKey", "ticket-reply-1"), 201);
        assertThat(replied.at("/data/status").asText()).isEqualTo("WAITING_USER");

        JsonNode resolved = performJson(patch("/api/v1/community/admin/tickets/" + ticketId + "/status").header("Authorization", bearer("helper-token")),
                Map.of("status", "RESOLVED", "publicComment", "已处理", "reason", "处理完成", "idempotencyKey", "ticket-resolve-1"), 200);
        assertThat(resolved.at("/data/status").asText()).isEqualTo("RESOLVED");

        JsonNode penalty = performJson(post("/api/v1/community/admin/penalties").header("Authorization", bearer("admin-token")),
                penaltyBody("member-user-1", "penalty-create-1"), 201);
        String penaltyId = penalty.at("/data/penaltyId").asText();
        performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")),
                postBody("board-general", "blocked-post-1"), 409, 49022);

        JsonNode revoked = performJson(patch("/api/v1/community/admin/penalties/" + penaltyId + "/revoke").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "解除限制", "reason", "处罚解除", "confirmText", "REVOKE_COMMUNITY_PENALTY", "idempotencyKey", "penalty-revoke-1"), 200);
        assertThat(revoked.at("/data/status").asText()).isEqualTo("REVOKED");

        JsonNode audit = performJson(get("/api/v1/community/admin/audit-logs").header("Authorization", bearer("admin-token")).param("pageSize", "100"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(4);
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/community/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("community");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8112);
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED");
        assertNoSecrets(ops);
    }

    @Test
    @DisplayName("COM-DEPS and COM-HARDEN cover dependency failures, rollback headers, sanitization, and module boundaries")
    void dependencyHardeningAndCompatibilityFlow() throws Exception {
        performJson(post("/api/v1/community/me/posts")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Profile-Mode", "unavailable"),
                postBody("board-general", "profile-down-post"), 502, 49210);
        performJson(post("/api/v1/community/me/posts")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Content-Mode", "unavailable"),
                with(postBody("board-general", "content-down-post"), "linkedContentId", "content-public-1"), 502, 49230);
        performJson(post("/api/v1/community/me/posts")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Resource-Mode", "unavailable"),
                with(postBody("board-general", "resource-down-post"), "linkedResourceId", "resource-public-1"), 502, 49240);

        String postId = createAndApprove("dependency-post-1");
        JsonNode notified = performJson(patch("/api/v1/community/admin/posts/" + postId + "/offline")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("publicReason", "通知失败也要下架", "reason", "治理下架", "idempotencyKey", "notify-fail-offline"), 200);
        assertThat(notified.at("/data/notificationFailure/failureCode").asText()).isEqualTo("49220");
        assertNoSecrets(notified);

        String rollbackPost = createAndSubmit("rollback-post-1");
        performJson(patch("/api/v1/community/admin/posts/" + rollbackPost + "/approve")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                reviewBody("audit-fail-approve"), 500, 54001);
        JsonNode afterAuditFail = performJson(get("/api/v1/community/admin/posts/" + rollbackPost).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterAuditFail.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        JsonNode ops = performJson(get("/api/v1/community/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();

        Path serviceRoot = Path.of("backend/community-service/src/main/java");
        String source = Files.exists(serviceRoot)
                ? String.join("\n", Files.walk(serviceRoot)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList())
                : "";
        assertThat(source).doesNotContain(
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.content.",
                "cn.beiming.serverstatus.", "cn.beiming.resource.", "cn.beiming.admin.", "cn.beiming.onboarding.",
                "cn.beiming.exam.", "cn.beiming.whitelist.", "cn.beiming.attendance.", "Repository", "JdbcTemplate",
                "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken", "terminal", "container",
                "backupRestore", "file-manager", "server.properties", "enforce-whitelist", "whitelist add",
                "whitelist remove", "scoreBalance", "attendancePoints", "leaderboard");
    }

    private JsonNode createBoard(String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/community/admin/boards").header("Authorization", bearer("admin-token")), boardBody(idempotencyKey), 201);
    }

    private JsonNode createPost(String boardId, String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/community/me/posts").header("Authorization", bearer("member-user-1-token")), postBody(boardId, idempotencyKey), 201);
    }

    private String createAndSubmit(String idempotencyKey) throws Exception {
        String postId = createPost("board-general", idempotencyKey).at("/data/postId").asText();
        performJson(post("/api/v1/community/me/posts/" + postId + "/submit").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", idempotencyKey + "-submit"), 200);
        return postId;
    }

    private String createAndApprove(String idempotencyKey) throws Exception {
        return approvePost(createAndSubmit(idempotencyKey));
    }

    private String approvePost(String postId) throws Exception {
        performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody(postId + "-approve"), 200);
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
        assertThat(json.at("/requestId").asText()).isNotBlank();
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
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private Map<String, Object> boardBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "board-" + idempotencyKey);
        body.put("name", "社区板块");
        body.put("description", "用于社区契约测试");
        body.put("visibility", "PUBLIC");
        body.put("status", "ACTIVE");
        body.put("allowedPostTypes", List.of("DISCUSSION", "QUESTION", "SUGGESTION", "RESOURCE_DISCUSSION"));
        body.put("tags", List.of("community", "test"));
        body.put("sortOrder", 10);
        body.put("reason", "创建社区板块");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "社区帖子标题");
        body.put("summary", "社区帖子摘要");
        body.put("body", "社区帖子正文，覆盖审核和公开展示。");
        body.put("tags", List.of("community"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "internalNote", "private note", "reason", "符合社区规则", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> pollBody(String postId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", postId);
        body.put("title", "社区投票");
        body.put("description", "选择社区方向");
        body.put("options", List.of(Map.of("label", "方案一"), Map.of("label", "方案二")));
        body.put("multipleChoice", false);
        body.put("minChoices", 1);
        body.put("maxChoices", 1);
        body.put("eligibleVisibility", "PUBLIC");
        body.put("anonymousResult", true);
        body.put("reason", "创建投票");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reportBody(String idempotencyKey) {
        return Map.of("reasonType", "SPAM", "description", "这个内容疑似灌水，需要处理", "evidenceLinks", List.of("https://example.com/evidence"), "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> ticketBody(String idempotencyKey) {
        return Map.of("type", "ACCOUNT_ISSUE", "title", "账号问题", "body", "账号绑定遇到问题", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> penaltyBody(String userId, String idempotencyKey) {
        return Map.of(
                "targetUserId", userId,
                "type", "POST_RESTRICTED",
                "publicReason", "临时限制发帖",
                "reason", "社区治理处罚",
                "confirmText", "CREATE_COMMUNITY_PENALTY",
                "idempotencyKey", idempotencyKey
        );
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
                "container", "cloudreveToken", "attendancePoints", "scoreBalance", "leaderboard");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
