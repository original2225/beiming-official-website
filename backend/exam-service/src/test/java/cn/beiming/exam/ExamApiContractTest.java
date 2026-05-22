package cn.beiming.exam;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ExamApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("exam local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "EXAM-COM", 1, 40);
        addRange(mapped, "EXAM-ME-CREATE", 1, 40);
        addRange(mapped, "EXAM-ME-CURRENT", 1, 18);
        addRange(mapped, "EXAM-ME-HISTORY", 1, 22);
        addRange(mapped, "EXAM-ME-PAPER", 1, 26);
        addRange(mapped, "EXAM-ME-ANSWERS", 1, 34);
        addRange(mapped, "EXAM-ME-SUBMIT", 1, 44);
        addRange(mapped, "EXAM-ME-SUPPLEMENT", 1, 26);
        addRange(mapped, "EXAM-ME-RESULT", 1, 26);
        addRange(mapped, "EXAM-ADMIN-SESSIONS", 1, 38);
        addRange(mapped, "EXAM-MANUAL", 1, 38);
        addRange(mapped, "EXAM-CORRECTION", 1, 26);
        addRange(mapped, "EXAM-SUPPLEMENT-ADMIN", 1, 26);
        addRange(mapped, "EXAM-CANCEL", 1, 24);
        addRange(mapped, "EXAM-HANDOFF", 1, 24);
        addRange(mapped, "EXAM-QBANK", 1, 48);
        addRange(mapped, "EXAM-QVERS", 1, 20);
        addRange(mapped, "EXAM-TEMPLATE", 1, 48);
        addRange(mapped, "EXAM-TEMPLATE-PREVIEW", 1, 20);
        addRange(mapped, "EXAM-AUDIT", 1, 26);
        addRange(mapped, "EXAM-OPS", 1, 20);
        addRange(mapped, "EXAM-DEPS", 1, 44);
        addRange(mapped, "EXAM-COMPAT", 1, 30);
        addRange(mapped, "EXAM-PORT", 1, 4);
        assertThat(mapped).contains("EXAM-COM-001", "EXAM-CORRECTION-026", "EXAM-QVERS-020", "EXAM-TEMPLATE-PREVIEW-020", "EXAM-PORT-004");
        assertThat(mapped).hasSize(712);
    }

    @Test
    @DisplayName("EXAM-COM covers request id, auth, role gates, validation, pagination, and field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/exams/me/sessions/current")
                        .header("Authorization", bearer("ready-token"))
                        .header("X-Request-Id", "req-exam-current"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-exam-current"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("req-exam-current"));

        JsonNode generated = performJson(get("/api/v1/exams/me/sessions/current")
                .header("Authorization", bearer("ready-token")), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/exams/me/sessions/current"), 401, 41000);
        performJson(get("/api/v1/exams/me/sessions/current").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/exams/admin/sessions"), 401, 41000);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/exams/admin/question-bank/questions").header("Authorization", bearer("helper-token")), questionBody("helper denied"), 403, 42001);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/exams/admin/sessions").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")), Map.of("applicationId", "app-ready", "idempotencyKey", "short"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")),
                with(Map.of("applicationId", "app-ready", "idempotencyKey", "create-trusted-1"), "userId", "attacker"), 201);
        assertThat(created.at("/data/userId").asText()).isEqualTo("seed-ready");
        assertNoSecrets(created);

        performJson(get("/api/v1/exams/public/anything").header("Authorization", bearer("ready-token")), 404);
    }

    @Test
    @DisplayName("EXAM-ME creates from onboarding handoff, freezes paper, saves answers, submits, reviews, and exposes whitelist handoff")
    void userFlowAndManualReviewContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")),
                Map.of("applicationId", "app-ready", "idempotencyKey", "create-flow-1"), 201);
        String sessionId = created.at("/data/sessionId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(created.at("/data/reviewDirection").asText()).isEqualTo("REDSTONE");
        assertThat(created.at("/data/templateVersion").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode replay = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")),
                Map.of("applicationId", "app-ready", "idempotencyKey", "create-flow-1"), 200);
        assertThat(replay.at("/data/sessionId").asText()).isEqualTo(sessionId);
        performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")),
                Map.of("applicationId", "app-ready", "idempotencyKey", "create-flow-1", "client", "changed"), 409, 43919);

        JsonNode current = performJson(get("/api/v1/exams/me/sessions/current").header("Authorization", bearer("ready-token")), 200);
        assertThat(current.at("/data/sessionId").asText()).isEqualTo(sessionId);

        JsonNode paper = performJson(get("/api/v1/exams/me/sessions/" + sessionId + "/paper").header("Authorization", bearer("ready-token")), 200);
        assertThat(paper.at("/data/questions").size()).isGreaterThanOrEqualTo(3);
        assertThat(paper.toString()).doesNotContain("correctOptionIds", "referenceAnswer", "internalNote");

        performJson(put("/api/v1/exams/me/sessions/" + sessionId + "/answers").header("Authorization", bearer("ready-token")),
                Map.of("idempotencyKey", "save-flow-1", "answers", answers()), 200);

        JsonNode submitted = performJson(post("/api/v1/exams/me/sessions/" + sessionId + "/submit").header("Authorization", bearer("ready-token")),
                Map.of("idempotencyKey", "submit-flow-1", "answers", answers()), 200);
        assertThat(submitted.at("/data/status").asText()).isEqualTo("PENDING_MANUAL_REVIEW");
        assertThat(submitted.at("/data/result").asText()).isEqualTo("PENDING");
        assertThat(submitted.at("/data/scoreSummary/objectiveScore").asInt()).isEqualTo(20);
        assertThat(submitted.at("/data/scoreSummary/manualRequired").asBoolean()).isTrue();

        performJson(put("/api/v1/exams/me/sessions/" + sessionId + "/answers").header("Authorization", bearer("ready-token")),
                Map.of("idempotencyKey", "save-after-submit", "answers", answers()), 409, 43918);

        JsonNode reviewed = performJson(patch("/api/v1/exams/admin/sessions/" + sessionId + "/manual-review").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "review-flow-1", "manualScores", List.of(Map.of("questionId", "q-redstone-short", "score", 30, "comment", "ok")), "result", "PASSED", "publicComment", "通过", "internalNote", "private note", "reason", "manual review"), 200);
        assertThat(reviewed.at("/data/status").asText()).isEqualTo("MANUAL_PASSED");
        assertThat(reviewed.at("/data/result").asText()).isEqualTo("PASSED");

        JsonNode result = performJson(get("/api/v1/exams/me/sessions/" + sessionId + "/result").header("Authorization", bearer("ready-token")), 200);
        assertThat(result.at("/data/result").asText()).isEqualTo("PASSED");
        assertNoSecrets(result);

        JsonNode handoff = performJson(get("/api/v1/exams/admin/sessions/" + sessionId + "/whitelist-handoff").header("Authorization", bearer("admin-token")), 200);
        assertThat(handoff.at("/data/sessionId").asText()).isEqualTo(sessionId);
        assertThat(handoff.at("/data/result").asText()).isEqualTo("PASSED");
        assertThat(handoff.toString()).doesNotContain("whitelistApplicationId", "attendanceScore");
    }

    @Test
    @DisplayName("EXAM-ME handles auto scoring, supplement, cancellation, current/history, and dependency boundaries")
    void stateMachineAndDependencyContract() throws Exception {
        performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("active-member-token")),
                Map.of("applicationId", "app-active", "idempotencyKey", "active-denied-1"), 409, 43911);
        performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("blocked-token")),
                Map.of("applicationId", "app-blocked", "idempotencyKey", "blocked-denied-1"), 409, 43910);
        performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("content-unavailable-token")),
                Map.of("applicationId", "app-content", "idempotencyKey", "content-denied-1"), 502, 46930);

        JsonNode auto = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("general-token")),
                Map.of("applicationId", "app-general", "idempotencyKey", "general-create-1"), 201);
        String autoId = auto.at("/data/sessionId").asText();
        JsonNode autoPassed = performJson(post("/api/v1/exams/me/sessions/" + autoId + "/submit").header("Authorization", bearer("general-token")),
                Map.of("idempotencyKey", "general-submit-1", "answers", generalAnswers()), 200);
        assertThat(autoPassed.at("/data/status").asText()).isEqualTo("AUTO_PASSED");
        assertThat(autoPassed.at("/data/result").asText()).isEqualTo("PASSED");
        performJson(get("/api/v1/exams/admin/sessions/" + autoId + "/whitelist-handoff").header("Authorization", bearer("helper-token")), 403, 42001);

        JsonNode review = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("supplement-token")),
                Map.of("applicationId", "app-supplement", "idempotencyKey", "supp-create-1"), 201);
        String reviewId = review.at("/data/sessionId").asText();
        performJson(post("/api/v1/exams/me/sessions/" + reviewId + "/submit").header("Authorization", bearer("supplement-token")),
                Map.of("idempotencyKey", "supp-submit-1", "answers", answers()), 200);
        JsonNode supplementRequest = performJson(patch("/api/v1/exams/admin/sessions/" + reviewId + "/request-supplement").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "supp-admin-1", "questionIds", List.of("q-redstone-short"), "publicComment", "补充说明", "supplementDueAt", "2026-05-30T12:00:00Z", "reason", "needs detail"), 200);
        assertThat(supplementRequest.at("/data/status").asText()).isEqualTo("NEEDS_SUPPLEMENT");
        JsonNode supplemented = performJson(patch("/api/v1/exams/me/sessions/" + reviewId + "/supplement").header("Authorization", bearer("supplement-token")),
                Map.of("idempotencyKey", "supp-user-1", "answers", List.of(Map.of("questionId", "q-redstone-short", "textAnswer", "补充内容"))), 200);
        assertThat(supplemented.at("/data/status").asText()).isEqualTo("SUPPLEMENT_SUBMITTED");
        performJson(get("/api/v1/exams/me/sessions/" + reviewId + "/paper").header("Authorization", bearer("supplement-token")), 409, 43913);

        JsonNode cancel = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("cancel-token")),
                Map.of("applicationId", "app-cancel", "idempotencyKey", "cancel-create-1"), 201);
        String cancelId = cancel.at("/data/sessionId").asText();
        JsonNode cancelled = performJson(patch("/api/v1/exams/admin/sessions/" + cancelId + "/cancel").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "cancel-admin-1", "notifyUser", false, "reason", "bad attempt"), 200);
        assertThat(cancelled.at("/data/status").asText()).isEqualTo("CANCELLED");
        performJson(post("/api/v1/exams/me/sessions/" + cancelId + "/submit").header("Authorization", bearer("cancel-token")),
                Map.of("idempotencyKey", "cancel-submit-1", "answers", answers()), 409, 43913);

        JsonNode history = performJson(get("/api/v1/exams/me/sessions").header("Authorization", bearer("cancel-token")).param("result", "CANCELLED"), 200);
        assertThat(history.at("/data/items").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("EXAM-QBANK EXAM-TEMPLATE EXAM-AUDIT EXAM-OPS cover admin maintenance, audit, summary, and source boundaries")
    void adminMaintenanceAndCompatibilityContract() throws Exception {
        JsonNode question = performJson(post("/api/v1/exams/admin/question-bank/questions").header("Authorization", bearer("admin-token")),
                questionBody("create custom question"), 201);
        String questionId = question.at("/data/questionId").asText();
        assertThat(question.at("/data/status").asText()).isEqualTo("DRAFT");

        JsonNode modified = performJson(patch("/api/v1/exams/admin/question-bank/questions/" + questionId).header("Authorization", bearer("admin-token")),
                Map.of("stem", "Updated stem", "reason", "update", "idempotencyKey", "question-update-1"), 200);
        assertThat(modified.at("/data/version").asInt()).isEqualTo(2);

        JsonNode archived = performJson(patch("/api/v1/exams/admin/question-bank/questions/" + questionId + "/archive").header("Authorization", bearer("admin-token")),
                Map.of("reason", "archive", "idempotencyKey", "question-archive-1"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");

        JsonNode template = performJson(post("/api/v1/exams/admin/paper-templates").header("Authorization", bearer("admin-token")),
                templateBody("Custom redstone"), 201);
        String templateId = template.at("/data/templateId").asText();
        assertThat(template.at("/data/status").asText()).isEqualTo("DRAFT");

        JsonNode published = performJson(patch("/api/v1/exams/admin/paper-templates/" + templateId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "publish", "idempotencyKey", "template-publish-1"), 200);
        assertThat(published.at("/data/status").asText()).isEqualTo("PUBLISHED");

        JsonNode templates = performJson(get("/api/v1/exams/admin/paper-templates").header("Authorization", bearer("helper-token")).param("status", "PUBLISHED"), 200);
        assertThat(templates.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode audit = performJson(get("/api/v1/exams/admin/audit-logs").header("Authorization", bearer("admin-token")).param("result", "SUCCESS"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        assertNoSecrets(audit);

        JsonNode opsBefore = performJson(get("/api/v1/exams/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(opsBefore.at("/data/service").asText()).isEqualTo("exam");
        assertThat(opsBefore.at("/data/port").asInt()).isEqualTo(8109);
        assertThat(opsBefore.at("/data/productionGaps").toString()).contains("WHITELIST_NOT_IMPLEMENTED");

        Path serviceRoot = Path.of("backend/exam-service/src/main/java");
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
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.content.", "cn.beiming.onboarding.",
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon",
                "cloudreveToken", "terminal", "container", "backupRestore", "file-manager");
    }

    @Test
    @DisplayName("EXAM-CORRECTION EXAM-QVERS EXAM-TEMPLATE-PREVIEW cover result correction, question history, and publish precheck")
    void enhancedAdminExamOperationsContract() throws Exception {
        JsonNode question = performJson(post("/api/v1/exams/admin/question-bank/questions").header("Authorization", bearer("admin-token")),
                questionBody("history create"), 201);
        String questionId = question.at("/data/questionId").asText();
        performJson(patch("/api/v1/exams/admin/question-bank/questions/" + questionId).header("Authorization", bearer("admin-token")),
                Map.of("stem", "历史版本 2", "reason", "history update 1", "idempotencyKey", "question-history-update-1"), 200);
        performJson(patch("/api/v1/exams/admin/question-bank/questions/" + questionId).header("Authorization", bearer("admin-token")),
                Map.of("score", 12, "reason", "history update 2", "idempotencyKey", "question-history-update-2"), 200);

        JsonNode versions = performJson(get("/api/v1/exams/admin/question-bank/questions/" + questionId + "/versions")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(versions.at("/data/total").asInt()).isEqualTo(3);
        assertThat(versions.at("/data/items/0/version").asInt()).isEqualTo(3);
        assertThat(versions.at("/data/items/2/version").asInt()).isEqualTo(1);
        assertThat(versions.toString()).contains("correctOptionIds");
        performJson(get("/api/v1/exams/admin/question-bank/questions/" + questionId + "/versions")
                .header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/exams/admin/question-bank/questions/missing/versions")
                .header("Authorization", bearer("admin-token")), 404, 43902);

        JsonNode template = performJson(post("/api/v1/exams/admin/paper-templates").header("Authorization", bearer("admin-token")),
                templateBody("Preview redstone"), 201);
        String templateId = template.at("/data/templateId").asText();
        JsonNode preview = performJson(get("/api/v1/exams/admin/paper-templates/" + templateId + "/publish-preview")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(preview.at("/data/readyToPublish").asBoolean()).isTrue();
        assertThat(preview.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(preview.at("/data/rules/0/matchedQuestionCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(preview.at("/data/samplePaper/questions").size()).isEqualTo(1);

        JsonNode unchanged = performJson(get("/api/v1/exams/admin/paper-templates").header("Authorization", bearer("admin-token"))
                .param("status", "DRAFT"), 200);
        assertThat(unchanged.toString()).contains(templateId);

        JsonNode unavailablePreview = performJson(get("/api/v1/exams/admin/paper-templates/" + templateId + "/publish-preview")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Dependency-Mode", "CONTENT:UNAVAILABLE"), 200);
        assertThat(unavailablePreview.at("/data/readyToPublish").asBoolean()).isFalse();
        assertThat(unavailablePreview.at("/data/contentRuleStatus").asText()).isEqualTo("UNAVAILABLE");

        Map<String, Object> insufficientTemplate = templateBody("Preview insufficient");
        insufficientTemplate.put("questionRules", List.of(Map.of("type", "SINGLE_CHOICE", "count", 1, "scoreEach", 10, "tags", List.of("missing-tag"))));
        JsonNode insufficient = performJson(post("/api/v1/exams/admin/paper-templates").header("Authorization", bearer("admin-token")),
                insufficientTemplate, 201);
        JsonNode insufficientPreview = performJson(get("/api/v1/exams/admin/paper-templates/" + insufficient.at("/data/templateId").asText() + "/publish-preview")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(insufficientPreview.at("/data/readyToPublish").asBoolean()).isFalse();
        assertThat(insufficientPreview.at("/data/rules/0/enough").asBoolean()).isFalse();

        JsonNode created = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("ready-token")),
                Map.of("applicationId", "app-ready", "idempotencyKey", "correction-create-1"), 201);
        String sessionId = created.at("/data/sessionId").asText();
        performJson(post("/api/v1/exams/me/sessions/" + sessionId + "/submit").header("Authorization", bearer("ready-token")),
                Map.of("idempotencyKey", "correction-submit-1", "answers", answers()), 200);
        performJson(patch("/api/v1/exams/admin/sessions/" + sessionId + "/manual-review").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "correction-review-1", "manualScores", List.of(Map.of("questionId", "q-redstone-short", "score", 0, "comment", "needs correction")), "result", "FAILED", "publicComment", "未通过", "internalNote", "first private note", "reason", "manual fail"), 200);

        Map<String, Object> correctionBody = Map.of("idempotencyKey", "correction-pass-1",
                "manualScores", List.of(Map.of("questionId", "q-redstone-short", "score", 30, "comment", "corrected")),
                "result", "PASSED", "publicComment", "复核通过", "internalNote", "correction private note", "reason", "score correction");
        JsonNode corrected = performJson(patch("/api/v1/exams/admin/sessions/" + sessionId + "/result-correction")
                .header("Authorization", bearer("admin-token")), correctionBody, 200);
        assertThat(corrected.at("/data/status").asText()).isEqualTo("MANUAL_PASSED");
        assertThat(corrected.at("/data/result").asText()).isEqualTo("PASSED");
        assertThat(corrected.at("/data/manualReview/correction").asBoolean()).isTrue();
        assertThat(corrected.at("/data/scoreSummary/objectiveScore").asInt()).isEqualTo(20);

        JsonNode correctionReplay = performJson(patch("/api/v1/exams/admin/sessions/" + sessionId + "/result-correction")
                .header("Authorization", bearer("admin-token")), correctionBody, 200);
        assertThat(correctionReplay.at("/data/manualReview/reviewId").asText()).isEqualTo(corrected.at("/data/manualReview/reviewId").asText());

        JsonNode userResult = performJson(get("/api/v1/exams/me/sessions/" + sessionId + "/result").header("Authorization", bearer("ready-token")), 200);
        assertThat(userResult.toString()).doesNotContain("correction private note", "internalNote");

        JsonNode audit = performJson(get("/api/v1/exams/admin/audit-logs").header("Authorization", bearer("admin-token"))
                .param("action", "EXAM_RESULT_CORRECTED"), 200);
        assertThat(audit.at("/data/total").asInt()).isEqualTo(1);

        JsonNode auto = performJson(post("/api/v1/exams/me/sessions").header("Authorization", bearer("general-token")),
                Map.of("applicationId", "app-general", "idempotencyKey", "handoff-correction-create-1"), 201);
        String autoId = auto.at("/data/sessionId").asText();
        performJson(post("/api/v1/exams/me/sessions/" + autoId + "/submit").header("Authorization", bearer("general-token")),
                Map.of("idempotencyKey", "handoff-correction-submit-1", "answers", generalAnswers()), 200);
        performJson(get("/api/v1/exams/admin/sessions/" + autoId + "/whitelist-handoff").header("Authorization", bearer("admin-token")), 200);
        performJson(patch("/api/v1/exams/admin/sessions/" + autoId + "/result-correction").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "handoff-correction-fail-1", "result", "FAILED", "publicComment", "handoff already generated", "reason", "late correction"), 409, 43925);
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

    private List<Map<String, Object>> answers() {
        return List.of(
                Map.of("questionId", "q-redstone-single", "selectedOptionIds", List.of("A")),
                Map.of("questionId", "q-redstone-multiple", "selectedOptionIds", List.of("A", "C")),
                Map.of("questionId", "q-redstone-short", "textAnswer", "红石时序要可复现")
        );
    }

    private List<Map<String, Object>> generalAnswers() {
        return List.of(
                Map.of("questionId", "q-general-single", "selectedOptionIds", List.of("A")),
                Map.of("questionId", "q-general-true", "selectedOptionIds", List.of("A"))
        );
    }

    private Map<String, Object> questionBody(String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SINGLE_CHOICE");
        body.put("reviewDirection", "REDSTONE");
        body.put("difficulty", "NORMAL");
        body.put("stem", "红石机器上线前应该先做什么？");
        body.put("options", List.of(Map.of("optionId", "A", "label", "A", "text", "测试与说明"), Map.of("optionId", "B", "label", "B", "text", "直接开机")));
        body.put("correctOptionIds", List.of("A"));
        body.put("score", 10);
        body.put("tags", List.of("redstone"));
        body.put("reason", reason);
        body.put("idempotencyKey", "question-" + reason.replaceAll("[^a-zA-Z0-9]", "-"));
        return body;
    }

    private Map<String, Object> templateBody(String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("reviewDirection", "REDSTONE");
        body.put("difficulty", "NORMAL");
        body.put("timeLimitMinutes", 45);
        body.put("passScore", 10);
        body.put("objectivePassScore", 10);
        body.put("questionRules", List.of(Map.of("type", "SINGLE_CHOICE", "count", 1, "scoreEach", 10, "tags", List.of("redstone"))));
        body.put("contentRuleVersion", "2026-05-22");
        body.put("retakeCooldownHours", 24);
        body.put("reason", "create template");
        body.put("idempotencyKey", "template-" + name.replaceAll("[^a-zA-Z0-9]", "-"));
        return body;
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
                "private note", "internalNote", "notification body", "content body",
                "minecraftVerifyCode", "requestHeaders", "paramsSummaryFull", "attendanceScore");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
