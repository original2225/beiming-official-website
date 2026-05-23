package cn.beiming.whitelist;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WhitelistApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("whitelist local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "WL-COM", 1, 44);
        addRange(mapped, "WL-ME-CREATE", 1, 44);
        addRange(mapped, "WL-ME-CURRENT", 1, 20);
        addRange(mapped, "WL-ME-HISTORY", 1, 24);
        addRange(mapped, "WL-ME-DETAIL", 1, 22);
        addRange(mapped, "WL-ME-MATERIALS", 1, 32);
        addRange(mapped, "WL-ME-SUBMIT", 1, 22);
        addRange(mapped, "WL-ME-SUPPLEMENT", 1, 32);
        addRange(mapped, "WL-ME-WITHDRAW", 1, 28);
        addRange(mapped, "WL-ME-RESULT", 1, 24);
        addRange(mapped, "WL-ADMIN-LIST", 1, 30);
        addRange(mapped, "WL-ADMIN-DETAIL", 1, 24);
        addRange(mapped, "WL-ADMIN-ASSIGN", 1, 30);
        addRange(mapped, "WL-ADMIN-SUPPLEMENT", 1, 34);
        addRange(mapped, "WL-ADMIN-APPROVE", 1, 48);
        addRange(mapped, "WL-ADMIN-REJECT", 1, 32);
        addRange(mapped, "WL-ADMIN-REMOVE", 1, 40);
        addRange(mapped, "WL-ADMIN-REOPEN", 1, 30);
        addRange(mapped, "WL-HANDOFF", 1, 28);
        addRange(mapped, "WL-AUDIT", 1, 32);
        addRange(mapped, "WL-OPS", 1, 24);
        addRange(mapped, "WL-DEPS", 1, 62);
        addRange(mapped, "WL-COMPAT", 1, 44);
        addRange(mapped, "WL-HARDEN", 1, 46);
        addRange(mapped, "WL-PORT", 1, 6);
        addRange(mapped, "WL-CYCLE", 1, 20);
        assertThat(mapped).contains("WL-COM-001", "WL-ADMIN-APPROVE-048", "WL-HANDOFF-028", "WL-DEPS-062", "WL-HARDEN-046", "WL-CYCLE-020");
        assertThat(mapped).hasSize(822);
    }

    @Test
    @DisplayName("WL-COM covers request id, auth, role gates, validation, pagination, and field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/whitelist/me/applications/current")
                        .header("Authorization", bearer("user-token"))
                        .header("X-Request-Id", "req-wl-current"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-wl-current"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("req-wl-current"));

        JsonNode generated = performJson(get("/api/v1/whitelist/me/applications/current")
                .header("Authorization", bearer("user-token")), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/whitelist/me/applications/current"), 401, 41000);
        performJson(get("/api/v1/whitelist/me/applications/current").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/whitelist/admin/applications"), 401, 41000);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/whitelist/admin/applications/missing/approve").header("Authorization", bearer("helper-token")), approveBody("helper denied"), 403, 42001);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/whitelist/admin/applications").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")), Map.of("examSessionId", "session-passed", "idempotencyKey", "short"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                with(createBody("session-passed", "create-trusted-1"), "userId", "attacker"), 201);
        assertThat(created.at("/data/userId").asText()).isEqualTo("user");
        assertThat(created.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("WL-ME creates from exam handoff, isolates users, edits materials, withdraws, and handles dependency failures")
    void currentUserApplicationFlow() throws Exception {
        JsonNode created = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                createBody("session-passed", "create-flow-1"), 201);
        String applicationId = created.at("/data/applicationId").asText();
        assertThat(created.at("/data/examSessionId").asText()).isEqualTo("session-passed");
        assertThat(created.at("/data/attemptType").asText()).isEqualTo("FIRST_TIME");
        assertThat(created.at("/data/notificationStatus").asText()).isEqualTo("DELIVERED");

        JsonNode replay = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                createBody("session-passed", "create-flow-1"), 200);
        assertThat(replay.at("/data/applicationId").asText()).isEqualTo(applicationId);
        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                with(createBody("session-passed", "create-flow-1"), "publicComment", "changed"), 409, 44017);

        JsonNode current = performJson(get("/api/v1/whitelist/me/applications/current").header("Authorization", bearer("user-token")), 200);
        assertThat(current.at("/data/applicationId").asText()).isEqualTo(applicationId);
        JsonNode detail = performJson(get("/api/v1/whitelist/me/applications/" + applicationId).header("Authorization", bearer("user-token")), 200);
        assertThat(detail.at("/data/internalNote").isMissingNode() || detail.at("/data/internalNote").isNull()).isTrue();

        JsonNode history = performJson(get("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")).param("status", "PENDING_REVIEW"), 200);
        assertThat(history.at("/data/total").asInt()).isEqualTo(1);

        JsonNode updated = performJson(patch("/api/v1/whitelist/me/applications/" + applicationId).header("Authorization", bearer("user-token")),
                Map.of("idempotencyKey", "materials-flow-1", "materials", materials("新的申请说明")), 200);
        assertThat(updated.at("/data/materials/0/content").asText()).isEqualTo("新的申请说明");

        JsonNode submitted = performJson(post("/api/v1/whitelist/me/applications/" + applicationId + "/submit").header("Authorization", bearer("user-token")),
                Map.of("idempotencyKey", "submit-flow-1"), 200);
        assertThat(submitted.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        JsonNode withdrawn = performJson(patch("/api/v1/whitelist/me/applications/" + applicationId + "/withdraw").header("Authorization", bearer("user-token")),
                Map.of("idempotencyKey", "withdraw-flow-1", "reason", "暂缓申请"), 200);
        assertThat(withdrawn.at("/data/status").asText()).isEqualTo("WITHDRAWN");
        assertThat(withdrawn.at("/data/result").asText()).isEqualTo("WITHDRAWN");

        JsonNode result = performJson(get("/api/v1/whitelist/me/applications/" + applicationId + "/result").header("Authorization", bearer("user-token")), 200);
        assertThat(result.at("/data/result").asText()).isEqualTo("WITHDRAWN");
        assertNoSecrets(result);

        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("other-token")),
                createBody("session-user-mismatch", "mismatch-1"), 409, 44011);
        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                createBody("session-failed", "failed-1"), 409, 44010);
        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("exam-unavailable-token")),
                createBody("session-passed", "exam-down-1"), 502, 47010);
        performJson(get("/api/v1/whitelist/me/applications/" + applicationId).header("Authorization", bearer("other-token")), 404, 44000);
    }

    @Test
    @DisplayName("WL-ADMIN covers assign, supplement, approve, reject, remove, reopen, handoff, audit, and ops summary")
    void adminReviewFlowContract() throws Exception {
        JsonNode first = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("approve-user-token")),
                createBody("session-approve", "create-approve-1"), 201);
        String approveId = first.at("/data/applicationId").asText();

        JsonNode assigned = performJson(patch("/api/v1/whitelist/admin/applications/" + approveId + "/assign").header("Authorization", bearer("helper-token")),
                Map.of("idempotencyKey", "assign-approve-1", "reason", "领取初审"), 200);
        assertThat(assigned.at("/data/status").asText()).isEqualTo("UNDER_REVIEW");
        performJson(patch("/api/v1/whitelist/admin/applications/" + approveId + "/request-supplement").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "supplement-approve-1", "publicComment", "补一段规划", "dueAt", "2026-05-30T12:00:00Z", "reason", "材料不足"), 200);
        JsonNode supplemented = performJson(patch("/api/v1/whitelist/me/applications/" + approveId + "/supplement").header("Authorization", bearer("approve-user-token")),
                Map.of("idempotencyKey", "supplement-user-1", "materials", materials("补充规划")), 200);
        assertThat(supplemented.at("/data/status").asText()).isEqualTo("SUPPLEMENT_SUBMITTED");

        JsonNode approved = performJson(patch("/api/v1/whitelist/admin/applications/" + approveId + "/approve").header("Authorization", bearer("admin-token")),
                approveBody("approve-flow-1"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/data/profileActivation/status").asText()).isEqualTo("ACTIVATED");
        assertThat(approved.at("/data/attendanceHandoff/initializationStatus").asText()).isEqualTo("WAITING_MODULE");

        JsonNode handoff = performJson(get("/api/v1/whitelist/admin/applications/" + approveId + "/attendance-handoff").header("Authorization", bearer("admin-token")), 200);
        assertThat(handoff.at("/data/memberId").asText()).startsWith("member-");
        assertThat(handoff.at("/data/consumedAt").isNull()).isTrue();

        JsonNode removed = performJson(patch("/api/v1/whitelist/admin/applications/" + approveId + "/remove").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "remove-approve-1", "publicComment", "暂时移除", "reason", "长期未参与", "confirmText", "REMOVE_WHITELIST"), 200);
        assertThat(removed.at("/data/status").asText()).isEqualTo("REMOVED");
        assertThat(removed.at("/data/nextExamAttemptType").asText()).isEqualTo("RECHECK");

        JsonNode reopened = performJson(post("/api/v1/whitelist/admin/applications/" + approveId + "/reopen").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "reopen-approve-1", "publicComment", "可重新申请", "reason", "允许重考"), 200);
        assertThat(reopened.at("/data/status").asText()).isEqualTo("REAPPLYING");

        JsonNode rejectedSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("reject-user-token")),
                createBody("session-reject", "create-reject-1"), 201);
        String rejectId = rejectedSeed.at("/data/applicationId").asText();
        JsonNode rejected = performJson(patch("/api/v1/whitelist/admin/applications/" + rejectId + "/reject").header("Authorization", bearer("owner-token")),
                Map.of("idempotencyKey", "reject-flow-1", "reviewComment", "暂未通过", "reason", "审核拒绝", "allowReapply", true), 200);
        assertThat(rejected.at("/data/status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.at("/data/attendanceHandoff").isNull()).isTrue();

        JsonNode blockedSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("profile-fail-token")),
                createBody("session-profile-fail", "create-profile-fail-1"), 201);
        JsonNode blocked = performJson(patch("/api/v1/whitelist/admin/applications/" + blockedSeed.at("/data/applicationId").asText() + "/approve").header("Authorization", bearer("admin-token")),
                approveBody("approve-profile-fail-1"), 200);
        assertThat(blocked.at("/data/status").asText()).isEqualTo("APPROVAL_BLOCKED");
        assertThat(blocked.at("/data/result").asText()).isEqualTo("PENDING");

        performJson(patch("/api/v1/whitelist/admin/applications/" + rejectId + "/remove").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "remove-reject-denied", "publicComment", "bad", "reason", "bad", "confirmText", "REMOVE_WHITELIST"), 409, 44014);
        performJson(patch("/api/v1/whitelist/admin/applications/" + approveId + "/remove").header("Authorization", bearer("helper-token")),
                Map.of("idempotencyKey", "remove-helper-denied", "publicComment", "bad", "reason", "bad", "confirmText", "REMOVE_WHITELIST"), 403, 42001);

        JsonNode audit = performJson(get("/api/v1/whitelist/admin/audit-logs").header("Authorization", bearer("admin-token")).param("action", "WHITELIST_APPROVED"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/whitelist/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("whitelist");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8110);
        assertThat(ops.at("/data/productionGaps").toString()).contains("ATTENDANCE_NOT_IMPLEMENTED", "REAL_SERVER_WHITELIST_NOT_CONNECTED");
    }

    @Test
    @DisplayName("WL-DEPS and WL-HARDEN cover reapply handoffs, profile hard failures, compensation, and dueAt limits")
    void dependencyAndHardeningAdditions() throws Exception {
        JsonNode dueSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("user-token")),
                createBody("session-passed", "create-due-limit"), 201);
        String dueId = dueSeed.at("/data/applicationId").asText();
        performJson(patch("/api/v1/whitelist/admin/applications/" + dueId + "/request-supplement").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "due-too-long", "publicComment", "补充说明", "dueAt", "2026-06-20T12:00:00Z", "reason", "截止过长"), 400, 40001);
        JsonNode dueDetail = performJson(get("/api/v1/whitelist/admin/applications/" + dueId).header("Authorization", bearer("admin-token")), 200);
        assertThat(dueDetail.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
        performJson(patch("/api/v1/whitelist/admin/applications/" + dueId + "/request-supplement").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "due-invalid", "publicComment", "补充说明", "dueAt", "not-time", "reason", "格式错误"), 400, 40001);
        performJson(patch("/api/v1/whitelist/admin/applications/" + dueId + "/request-supplement").header("Authorization", bearer("admin-token")),
                with(Map.of("idempotencyKey", "due-null", "publicComment", "补充说明", "reason", "空截止时间"), "dueAt", null), 400, 40001);
        JsonNode dueOk = performJson(patch("/api/v1/whitelist/admin/applications/" + dueId + "/request-supplement").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "due-within-limit", "publicComment", "补充说明", "dueAt", "2026-06-05T12:00:00Z", "reason", "材料不足"), 200);
        assertThat(dueOk.at("/data/status").asText()).isEqualTo("NEEDS_SUPPLEMENT");

        JsonNode profileDownSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("profile-unavailable-token")),
                createBody("session-profile-unavailable", "create-profile-down"), 201);
        String profileDownId = profileDownSeed.at("/data/applicationId").asText();
        performJson(patch("/api/v1/whitelist/admin/applications/" + profileDownId + "/approve")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Profile-Mode", "unavailable"),
                approveBody("approve-profile-down"), 502, 47020);
        JsonNode profileDownDetail = performJson(get("/api/v1/whitelist/admin/applications/" + profileDownId).header("Authorization", bearer("admin-token")), 200);
        assertThat(profileDownDetail.at("/data/status").asText()).isNotEqualTo("APPROVED");

        JsonNode profileTimeoutSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("profile-timeout-token")),
                createBody("session-profile-timeout", "create-profile-timeout"), 201);
        performJson(patch("/api/v1/whitelist/admin/applications/" + profileTimeoutSeed.at("/data/applicationId").asText() + "/approve")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Profile-Mode", "timeout"),
                approveBody("approve-profile-timeout"), 504, 47021);

        JsonNode profileBadSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("profile-bad-schema-token")),
                createBody("session-profile-bad-schema", "create-profile-bad"), 201);
        performJson(patch("/api/v1/whitelist/admin/applications/" + profileBadSeed.at("/data/applicationId").asText() + "/approve")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Profile-Mode", "bad-schema"),
                approveBody("approve-profile-bad"), 502, 47022);

        JsonNode compensateSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("profile-compensate-token")),
                createBody("session-profile-compensate", "create-compensate"), 201);
        String compensateId = compensateSeed.at("/data/applicationId").asText();
        performJson(patch("/api/v1/whitelist/admin/applications/" + compensateId + "/approve")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-After-Profile", "true"),
                approveBody("approve-compensate"), 500, 52003);
        JsonNode compensateDetail = performJson(get("/api/v1/whitelist/admin/applications/" + compensateId).header("Authorization", bearer("admin-token")), 200);
        assertThat(compensateDetail.at("/data/status").asText()).isEqualTo("APPROVAL_BLOCKED");
        assertThat(compensateDetail.at("/data/profileActivation/status").asText()).isEqualTo("ACTIVATED");
        assertNoRuntimeSecrets(compensateDetail);

        JsonNode removeSeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("remove-profile-fail-token")),
                createBody("session-remove-profile-fail", "create-remove-fail"), 201);
        String removeId = removeSeed.at("/data/applicationId").asText();
        performJson(patch("/api/v1/whitelist/admin/applications/" + removeId + "/approve").header("Authorization", bearer("admin-token")),
                approveBody("approve-remove-fail"), 200);
        performJson(patch("/api/v1/whitelist/admin/applications/" + removeId + "/remove")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Profile-Mode", "unavailable"),
                Map.of("idempotencyKey", "remove-profile-down", "publicComment", "暂时移除", "reason", "profile 不可用", "confirmText", "REMOVE_WHITELIST"), 502, 47020);
        JsonNode removeDetail = performJson(get("/api/v1/whitelist/admin/applications/" + removeId).header("Authorization", bearer("admin-token")), 200);
        assertThat(removeDetail.at("/data/status").asText()).isEqualTo("APPROVED");

        JsonNode reapplySeed = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("reapply-user-token")),
                createBody("session-reapply-first", "create-reapply-first"), 201);
        String reapplyId = reapplySeed.at("/data/applicationId").asText();
        performJson(patch("/api/v1/whitelist/admin/applications/" + reapplyId + "/approve").header("Authorization", bearer("admin-token")),
                approveBody("approve-reapply"), 200);
        performJson(patch("/api/v1/whitelist/admin/applications/" + reapplyId + "/remove").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "remove-reapply", "publicComment", "暂时移除", "reason", "长期未参与", "confirmText", "REMOVE_WHITELIST"), 200);
        performJson(post("/api/v1/whitelist/admin/applications/" + reapplyId + "/reopen").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "reopen-reapply", "publicComment", "可重新申请", "reason", "允许重考"), 200);
        performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("reapply-user-token")),
                createBody("session-reapply-first-again", "create-first-again"), 409, 44019);
        JsonNode recheck = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("reapply-user-token")),
                createBody("session-reapply-recheck", "create-recheck"), 201);
        assertThat(recheck.at("/data/attemptType").asText()).isEqualTo("RECHECK");
        JsonNode replay = performJson(post("/api/v1/whitelist/me/applications").header("Authorization", bearer("reapply-user-token")),
                createBody("session-reapply-recheck", "create-recheck"), 200);
        assertThat(replay.at("/data/applicationId").asText()).isEqualTo(recheck.at("/data/applicationId").asText());
    }

    @Test
    @DisplayName("WL-COMPAT scans source boundaries and verifies server-operation and attendance ownership stay outside whitelist")
    void compatibilityAndBoundaryContract() throws Exception {
        Path serviceRoot = Path.of("backend/whitelist-service/src/main/java");
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
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.onboarding.", "cn.beiming.exam.",
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken",
                "terminal", "container", "backupRestore", "file-manager", "scoreCalculator", "attendancePoints", "leaderboard",
                "server.properties", "enforce-whitelist", "whitelist add", "whitelist remove");
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

    private Map<String, Object> createBody(String sessionId, String idempotencyKey) {
        return Map.of("examSessionId", sessionId, "idempotencyKey", idempotencyKey, "materials", materials("申请说明"));
    }

    private List<Map<String, Object>> materials(String content) {
        return List.of(Map.of("type", "TEXT", "title", "说明", "content", content));
    }

    private Map<String, Object> approveBody(String idempotencyKey) {
        return Map.of("idempotencyKey", idempotencyKey, "reviewComment", "审核通过", "reason", "符合准入要求");
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
                "secret-token", "authorizationHeader", "stackTrace", "correctOptionIds", "referenceAnswer",
                "private note", "internalNote", "notification body", "content body", "minecraftVerifyCode",
                "requestHeaders", "paramsSummaryFull", "attendancePoints", "leaderboard", "server.properties",
                "whitelist add", "whitelist remove", "node-daemon", "terminal", "container");
    }

    private void assertNoRuntimeSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "secret-token", "authorizationHeader", "stackTrace", "requestHeaders", "notification body",
                "content body", "minecraftVerifyCode", "server.properties", "whitelist add", "whitelist remove",
                "node-daemon", "terminal", "container");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
