package cn.beiming.exam;

import cn.beiming.admission.AdmissionTrustedActor;
import cn.beiming.admission.persistence.ExamPersistence;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class ExamModule {
    @Bean
    ExamStore examStore(ExamTestControls testControls, ExamPersistence examPersistence) {
        ExamStore store = new ExamStore(testControls, examPersistence);
        store.seed();
        return store;
    }

    @Bean
    TestExamAuthProvider examAuthProvider() {
        return new TestExamAuthProvider();
    }

    @Bean
    ExamTestControls examTestControls(@Value("${exam.test-controls.enabled:false}") boolean enabled) {
        return new ExamTestControls(enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    ExamFlowEvidenceRecorder examFlowEvidenceRecorder() {
        return new NoopExamFlowEvidenceRecorder();
    }
}

@RestController
@RequestMapping("/api/v1/exams")
class ExamController {
    private final ExamStore store;
    private final TestExamAuthProvider auth;
    private final ExamFlowEvidenceRecorder evidenceRecorder;

    ExamController(ExamStore store, TestExamAuthProvider auth, ExamFlowEvidenceRecorder evidenceRecorder) {
        this.store = store;
        this.auth = auth;
        this.evidenceRecorder = evidenceRecorder;
    }

    @PostMapping("/me/sessions")
    ResponseEntity<Map<String, Object>> createSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        MutationResult result = store.createSession(user, bodyOrEmpty(body), request);
        int responseCode = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        evidenceRecorder.recordSessionWrite(request, result.created() ? "EXAM_SESSION_CREATED" : "EXAM_SESSION_REPLAYED", result.value(), responseCode);
        return ResponseEntity.status(responseCode).body(okBody(result.value()));
    }

    @GetMapping("/me/sessions/current")
    Map<String, Object> currentSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        return ok(store.currentSession(user, request));
    }

    @GetMapping("/me/sessions")
    Map<String, Object> mySessions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query,
                                   HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        return ok(store.mySessions(user, query, request));
    }

    @GetMapping("/me/sessions/{sessionId}/paper")
    Map<String, Object> paper(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable String sessionId,
                              HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        return ok(store.paper(user, sessionId, request));
    }

    @PutMapping("/me/sessions/{sessionId}/answers")
    Map<String, Object> saveAnswers(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String sessionId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        Map<String, Object> payload = store.saveAnswers(user, sessionId, bodyOrEmpty(body), request);
        evidenceRecorder.recordAnswerWrite(request, "EXAM_ANSWERS_SAVED", payload, HttpStatus.OK.value());
        return ok(payload);
    }

    @PostMapping("/me/sessions/{sessionId}/submit")
    Map<String, Object> submit(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String sessionId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        Map<String, Object> payload = store.submit(user, sessionId, bodyOrEmpty(body), request);
        evidenceRecorder.recordSessionWrite(request, "EXAM_SESSION_SUBMITTED", payload, HttpStatus.OK.value());
        return ok(payload);
    }

    @PatchMapping("/me/sessions/{sessionId}/supplement")
    Map<String, Object> supplement(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String sessionId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        return ok(store.supplement(user, sessionId, bodyOrEmpty(body), request));
    }

    @GetMapping("/me/sessions/{sessionId}/result")
    Map<String, Object> result(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String sessionId,
                               HttpServletRequest request) {
        ExamUser user = auth.requireUser(authorization, request);
        return ok(store.result(user, sessionId, request));
    }

    @GetMapping("/admin/sessions")
    Map<String, Object> adminSessions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query,
                                      HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminSessions(actor, query, request));
    }

    @GetMapping("/admin/sessions/{sessionId}")
    Map<String, Object> adminSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String sessionId,
                                     HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminSession(actor, sessionId, request));
    }

    @PatchMapping("/admin/sessions/{sessionId}/manual-review")
    Map<String, Object> manualReview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String sessionId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.manualReview(actor, sessionId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/sessions/{sessionId}/result-correction")
    Map<String, Object> resultCorrection(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String sessionId,
                                         @RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.resultCorrection(actor, sessionId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/sessions/{sessionId}/request-supplement")
    Map<String, Object> requestSupplement(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String sessionId,
                                          @RequestBody(required = false) Map<String, Object> body,
                                          HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.requestSupplement(actor, sessionId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/sessions/{sessionId}/cancel")
    Map<String, Object> cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String sessionId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.cancel(actor, sessionId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/sessions/{sessionId}/whitelist-handoff")
    Map<String, Object> whitelistHandoff(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String sessionId,
                                         HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.whitelistHandoff(actor, sessionId, request));
    }

    @GetMapping("/admin/question-bank/questions")
    Map<String, Object> questions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.questions(actor, query, request));
    }

    @GetMapping("/admin/question-bank/questions/{questionId}/versions")
    Map<String, Object> questionVersions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String questionId,
                                         @RequestParam Map<String, String> query,
                                         HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.questionVersions(actor, questionId, query, request));
    }

    @PostMapping("/admin/question-bank/questions")
    ResponseEntity<Map<String, Object>> createQuestion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        Map<String, Object> payload = store.createQuestion(actor, bodyOrEmpty(body), request);
        evidenceRecorder.recordQuestionWrite(request, "EXAM_QUESTION_CREATED", payload, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(payload));
    }

    @PatchMapping("/admin/question-bank/questions/{questionId}")
    Map<String, Object> updateQuestion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String questionId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.updateQuestion(actor, questionId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/question-bank/questions/{questionId}/archive")
    Map<String, Object> archiveQuestion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String questionId,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.archiveQuestion(actor, questionId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/paper-templates")
    Map<String, Object> templates(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.templates(actor, query, request));
    }

    @PostMapping("/admin/paper-templates")
    ResponseEntity<Map<String, Object>> createTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.createTemplate(actor, bodyOrEmpty(body), request)));
    }

    @PatchMapping("/admin/paper-templates/{templateId}")
    Map<String, Object> updateTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String templateId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.updateTemplate(actor, templateId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/paper-templates/{templateId}/publish-preview")
    Map<String, Object> publishPreview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String templateId,
                                       HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.publishPreview(actor, templateId, request));
    }

    @PatchMapping("/admin/paper-templates/{templateId}/publish")
    Map<String, Object> publishTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String templateId,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.publishTemplate(actor, templateId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/paper-templates/{templateId}/archive")
    Map<String, Object> archiveTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String templateId,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        HttpServletRequest request) {
        ExamUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.archiveTemplate(actor, templateId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.auditLogs(query, request));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest request) {
        auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.opsSummary(request));
    }

    static Map<String, Object> ok(Object data) {
        return okBody(data);
    }

    static Map<String, Object> okBody(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", requestId());
        return response;
    }

    static String requestId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object value = attrs == null ? null : attrs.getRequest().getAttribute("requestId");
        return value == null ? "req-" + UUID.randomUUID() : value.toString();
    }

    private static Map<String, Object> bodyOrEmpty(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }
}

class ExamStore {
    private static final String NOW = "2026-05-23T12:00:00Z";
    private static final Set<String> DIRECTIONS = Set.of("REDSTONE", "LATE_GAME", "BUILDING", "GENERAL");
    private static final Set<String> TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_TEXT");
    private static final Set<String> DIFFICULTIES = Set.of("NORMAL", "RECHECK");
    private static final Set<String> SESSION_STATUSES = Set.of("IN_PROGRESS", "PENDING_MANUAL_REVIEW", "NEEDS_SUPPLEMENT", "SUPPLEMENT_SUBMITTED", "AUTO_PASSED", "AUTO_FAILED", "MANUAL_PASSED", "MANUAL_FAILED", "EXPIRED", "CANCELLED");
    private static final Set<String> RESULTS = Set.of("PENDING", "PASSED", "FAILED", "NEEDS_SUPPLEMENT", "EXPIRED", "CANCELLED");
    private final Map<String, ExamSessionRecord> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> currentByUser = new ConcurrentHashMap<>();
    private final Map<String, ExamQuestionRecord> questions = new ConcurrentHashMap<>();
    private final Map<String, List<ExamQuestionRecord>> questionVersions = new ConcurrentHashMap<>();
    private final Map<String, PaperTemplateRecord> templates = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Set<String> whitelistHandoffGenerated = ConcurrentHashMap.newKeySet();
    private final List<Map<String, Object>> audits = new ArrayList<>();
    private final ExamTestControls testControls;
    private final ExamPersistence persistence;
    private int idSeq = 3000;
    private int whitelistHandoffSnapshotsTotal;

    ExamStore(ExamTestControls testControls, ExamPersistence persistence) {
        this.testControls = testControls;
        this.persistence = persistence;
    }

    void seed() {
        addQuestion(question("q-redstone-single", "SINGLE_CHOICE", "REDSTONE", "NORMAL", "Run a test before enabling a shared machine.", options("A", "B"), List.of("A"), null, 10, List.of("redstone"), "ACTIVE"));
        addQuestion(question("q-redstone-multiple", "MULTIPLE_CHOICE", "REDSTONE", "NORMAL", "Pick safe redstone rollout steps.", options("A", "B", "C"), List.of("A", "C"), null, 10, List.of("redstone"), "ACTIVE"));
        addQuestion(question("q-redstone-short", "SHORT_TEXT", "REDSTONE", "NORMAL", "Explain how to keep a redstone build reproducible.", List.of(), List.of(), "Mention timing and rollback.", 30, List.of("redstone"), "ACTIVE"));
        addQuestion(question("q-general-single", "SINGLE_CHOICE", "GENERAL", "NORMAL", "What should a new player read first?", options("A", "B"), List.of("A"), null, 10, List.of("general"), "ACTIVE"));
        addQuestion(question("q-general-true", "TRUE_FALSE", "GENERAL", "NORMAL", "Rules apply before whitelist approval.", options("A", "B"), List.of("A"), null, 10, List.of("general"), "ACTIVE"));
        templates.put("tpl-redstone", template("tpl-redstone", "Redstone normal", "REDSTONE", "NORMAL", "PUBLISHED", 1, 45, 50, 20, List.of(
                rule("SINGLE_CHOICE", 1, 10, List.of("redstone")),
                rule("MULTIPLE_CHOICE", 1, 10, List.of("redstone")),
                rule("SHORT_TEXT", 1, 30, List.of("redstone"))
        )));
        templates.put("tpl-general", template("tpl-general", "General normal", "GENERAL", "NORMAL", "PUBLISHED", 1, 30, 20, 20, List.of(
                rule("SINGLE_CHOICE", 1, 10, List.of("general")),
                rule("TRUE_FALSE", 1, 10, List.of("general"))
        )));
        audits.add(auditRow("audit-seed-1", "admin", "EXAM", "seed", "EXAM_SEEDED", "SUCCESS", "LOW", null, null, null));
        persistence.seed(questions.values().stream().map(question -> questionView(question, true)).toList(), templates.values().stream().map(this::templateView).toList(), audits);
    }

    synchronized MutationResult createSession(ExamUser user, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        String applicationId = validateRequiredString(body, "applicationId", 1, 120);
        IdempotencyRecord replay = replay(user.userId(), "CREATE_SESSION", body);
        if (replay != null) return new MutationResult(false, replay.value());
        Handoff handoff = handoff(user, applicationId, request);
        if ("ACTIVE".equals(user.profileStatus()) || "INACTIVE".equals(user.profileStatus())) {
            throw new ExamException(409, 43911, "member profile exists");
        }
        if (testControls.enabled() && "CONTENT_UNAVAILABLE".equals(user.profileStatus())) {
            throw new ExamException(502, 46930, "content unavailable");
        }
        String currentId = currentByUser.get(user.userId());
        if (currentId != null) {
            ExamSessionRecord current = sessions.get(currentId);
            if (current != null && applicationId.equals(current.applicationId) && !ended(current.status)) {
                Map<String, Object> view = sessionView(current, false);
                remember(user.userId(), "CREATE_SESSION", body, view);
                return new MutationResult(false, view);
            }
        }
        String difficulty = "REMOVED".equals(user.profileStatus()) ? "RECHECK" : "NORMAL";
        String attemptType = "REMOVED".equals(user.profileStatus()) ? "RECHECK" : "FIRST_TIME";
        PaperTemplateRecord template = latestTemplate(handoff.reviewDirection(), difficulty);
        if (template == null) throw new ExamException(409, 43914, "no published template");
        if (requiresContent(user, template, request)) throw new ExamException(502, 46930, "content unavailable");
        List<ExamQuestionRecord> selected = selectQuestions(template);
        if (selected.isEmpty()) throw new ExamException(409, 43915, "question bank not enough");
        failBeforeWrite(request);
        ExamSessionRecord session = new ExamSessionRecord();
        session.sessionId = "exam-" + (++idSeq);
        session.applicationId = applicationId;
        session.onboardingHandoffVersion = 1;
        session.userId = user.userId();
        session.displayName = user.displayName();
        session.minecraftBinding = user.minecraftBinding();
        session.reviewDirection = handoff.reviewDirection();
        session.attemptType = attemptType;
        session.difficulty = difficulty;
        session.status = "IN_PROGRESS";
        session.result = "PENDING";
        session.templateId = template.templateId;
        session.templateVersion = template.version;
        session.paperId = "paper-" + session.sessionId;
        session.questions = selected;
        session.timeLimitMinutes = template.timeLimitMinutes;
        session.passScore = template.passScore;
        session.objectivePassScore = template.objectivePassScore;
        session.expiresAt = "2026-05-30T12:00:00Z";
        session.startedAt = NOW;
        session.createdAt = NOW;
        session.updatedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        sessions.put(session.sessionId, session);
        currentByUser.put(user.userId(), session.sessionId);
        audit(user, "EXAM_SESSION", session.sessionId, "EXAM_SESSION_CREATED", "LOW", null, session.status, null);
        Map<String, Object> view = sessionView(session, false);
        remember(user.userId(), "CREATE_SESSION", body, view);
        persistence.persistSessionWrite(request, user.userId(), actorRole(user), "exam.create-session", "EXAM_SESSION_CREATED", "LOW", null, session.status, null, idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 201);
        return new MutationResult(true, view);
    }

    synchronized Object currentSession(ExamUser user, HttpServletRequest request) {
        String currentId = currentByUser.get(user.userId());
        if (currentId == null) return null;
        ExamSessionRecord session = sessions.get(currentId);
        if (session == null || ended(session.status)) return null;
        return sessionView(session, false);
    }

    synchronized Map<String, Object> mySessions(ExamUser user, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", SESSION_STATUSES);
        String result = enumQuery(query, "result", RESULTS);
        String sort = sort(query, Set.of("createdAt_desc", "createdAt_asc", "submittedAt_desc", "updatedAt_desc"), "createdAt_desc");
        List<Map<String, Object>> rows = sessions.values().stream()
                .filter(session -> user.userId().equals(session.userId))
                .filter(session -> status == null || status.equals(session.status))
                .filter(session -> result == null || result.equals(session.result))
                .map(session -> sessionView(session, false))
                .sorted(sessionComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> paper(ExamUser user, String sessionId, HttpServletRequest request) {
        ExamSessionRecord session = requireOwnedSession(user, sessionId);
        if (!"IN_PROGRESS".equals(session.status) && !"NEEDS_SUPPLEMENT".equals(session.status)) {
            throw new ExamException(409, 43913, "session status conflict");
        }
        return paperView(session, false);
    }

    synchronized Map<String, Object> saveAnswers(ExamUser user, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "SAVE:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireOwnedSession(user, sessionId);
        if (!"IN_PROGRESS".equals(session.status)) {
            throw new ExamException(409, Set.of("AUTO_PASSED", "AUTO_FAILED", "MANUAL_PASSED", "MANUAL_FAILED", "PENDING_MANUAL_REVIEW", "SUPPLEMENT_SUBMITTED").contains(session.status) ? 43918 : 43913, "cannot save answers");
        }
        List<Map<String, Object>> answers = answerList(body);
        validateAnswers(session, answers, false, false);
        failStoreOnly(request);
        session.answers = copyList(answers);
        session.lastSavedAt = NOW;
        session.updatedAt = NOW;
        Map<String, Object> sheet = mapOf("sessionId", session.sessionId, "answers", session.answers, "draft", true, "savedAt", NOW, "submittedAt", null);
        remember(user.userId(), "SAVE:" + sessionId, body, sheet);
        persistence.persistAnswerWrite(request, user.userId(), actorRole(user), "exam.save-answers", "EXAM_ANSWERS_SAVED", idempotencyKey(body), canonical(body), answerPersistenceView(session, sheet), sheet, 200);
        return sheet;
    }

    synchronized Map<String, Object> submit(ExamUser user, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "SUBMIT:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireOwnedSession(user, sessionId);
        if (!"IN_PROGRESS".equals(session.status)) {
            throw new ExamException(409, Set.of("PENDING_MANUAL_REVIEW", "AUTO_PASSED", "AUTO_FAILED", "MANUAL_PASSED", "MANUAL_FAILED").contains(session.status) ? 43918 : 43913, "cannot submit");
        }
        List<Map<String, Object>> answers = answerList(body);
        validateAnswers(session, answers, true, false);
        failBeforeWrite(request);
        session.answers = copyList(answers);
        score(session, answers);
        session.submittedAt = NOW;
        session.updatedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        audit(user, "EXAM_SESSION", session.sessionId, "EXAM_SUBMITTED", "MEDIUM", "IN_PROGRESS", session.status, null);
        Map<String, Object> view = sessionView(session, false);
        remember(user.userId(), "SUBMIT:" + sessionId, body, view);
        persistence.persistSessionWrite(request, user.userId(), actorRole(user), "exam.submit", "EXAM_SUBMITTED", "MEDIUM", "IN_PROGRESS", session.status, null, idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> supplement(ExamUser user, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "SUPPLEMENT:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireOwnedSession(user, sessionId);
        if (!"NEEDS_SUPPLEMENT".equals(session.status)) throw new ExamException(409, 43913, "not supplementable");
        List<Map<String, Object>> answers = answerList(body);
        validateAnswers(session, answers, true, true);
        failBeforeWrite(request);
        session.answers = mergeSupplement(session, answers);
        String before = session.status;
        session.status = "SUPPLEMENT_SUBMITTED";
        session.result = "PENDING";
        session.updatedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        audit(user, "EXAM_SESSION", session.sessionId, "EXAM_SUPPLEMENT_SUBMITTED", "MEDIUM", before, session.status, null);
        Map<String, Object> view = sessionView(session, false);
        remember(user.userId(), "SUPPLEMENT:" + sessionId, body, view);
        persistence.persistSessionWrite(request, user.userId(), actorRole(user), "exam.supplement", "EXAM_SUPPLEMENT_SUBMITTED", "MEDIUM", before, session.status, null, idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> result(ExamUser user, String sessionId, HttpServletRequest request) {
        ExamSessionRecord session = requireOwnedSession(user, sessionId);
        if ("IN_PROGRESS".equals(session.status)) throw new ExamException(409, 43913, "not submitted");
        return sessionView(session, false);
    }

    synchronized Map<String, Object> adminSessions(ExamUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", SESSION_STATUSES);
        String result = enumQuery(query, "result", RESULTS);
        String direction = enumQuery(query, "reviewDirection", DIRECTIONS);
        String attemptType = enumQuery(query, "attemptType", Set.of("FIRST_TIME", "RECHECK"));
        Boolean needsManual = boolQuery(query, "needsManualReview");
        String keyword = lower(query.get("keyword"));
        String sort = sort(query, Set.of("createdAt_desc", "submittedAt_desc", "updatedAt_desc", "status_asc", "score_desc"), "updatedAt_desc");
        List<Map<String, Object>> rows = sessions.values().stream()
                .filter(session -> status == null || status.equals(session.status))
                .filter(session -> result == null || result.equals(session.result))
                .filter(session -> direction == null || direction.equals(session.reviewDirection))
                .filter(session -> attemptType == null || attemptType.equals(session.attemptType))
                .filter(session -> needsManual == null || !needsManual || Set.of("PENDING_MANUAL_REVIEW", "SUPPLEMENT_SUBMITTED").contains(session.status))
                .filter(session -> keyword == null || matchesSession(session, keyword))
                .map(session -> sessionView(session, false))
                .sorted(sessionComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> adminSession(ExamUser actor, String sessionId, HttpServletRequest request) {
        ExamSessionRecord session = requireSession(sessionId);
        Map<String, Object> view = sessionView(session, actor.roles().contains("ADMIN") || actor.roles().contains("OWNER"));
        view.put("paper", paperView(session, true));
        view.put("answerSheet", mapOf("sessionId", session.sessionId, "answers", session.answers, "draft", false, "savedAt", session.lastSavedAt, "submittedAt", session.submittedAt));
        return view;
    }

    synchronized Map<String, Object> manualReview(ExamUser actor, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "REVIEW:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireSession(sessionId);
        if (!Set.of("PENDING_MANUAL_REVIEW", "SUPPLEMENT_SUBMITTED").contains(session.status)) {
            throw new ExamException(409, 43913, "not reviewable");
        }
        String result = validateEnum(body, "result", Set.of("PASSED", "FAILED"));
        String publicComment = validateRequiredString(body, "publicComment", 1, 1000);
        List<Map<String, Object>> scores = objectList(body, "manualScores");
        int manual = validateManualScores(session, scores);
        failBeforeWrite(request);
        String before = session.status;
        session.manualScore = manual;
        session.totalScore = session.objectiveScore + manual;
        boolean passed = "PASSED".equals(result) && session.totalScore >= session.passScore;
        session.status = passed ? "MANUAL_PASSED" : "MANUAL_FAILED";
        session.result = passed ? "PASSED" : "FAILED";
        session.finalPassed = passed;
        session.passedAt = passed ? NOW : null;
        session.reviewedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        session.manualReview = mapOf("reviewId", "review-" + (++idSeq), "reviewerUserId", actor.userId(), "reviewerDisplayNameSnapshot", actor.displayName(), "manualScores", scores, "publicComment", publicComment, "internalNote", string(body.get("internalNote")), "result", result, "reviewedAt", NOW);
        audit(actor, "EXAM_SESSION", session.sessionId, passed ? "EXAM_MANUAL_PASSED" : "EXAM_MANUAL_FAILED", "MEDIUM", before, session.status, string(body.get("reason")));
        Map<String, Object> view = sessionView(session, true);
        remember(actor.userId(), "REVIEW:" + sessionId, body, view);
        persistence.persistReviewWrite(request, actor.userId(), actorRole(actor), "exam.manual-review", passed ? "EXAM_MANUAL_PASSED" : "EXAM_MANUAL_FAILED", before, session.status, string(body.get("reason")), idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> resultCorrection(ExamUser actor, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "CORRECTION:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireSession(sessionId);
        if (!Set.of("AUTO_PASSED", "AUTO_FAILED", "MANUAL_PASSED", "MANUAL_FAILED").contains(session.status)) {
            throw new ExamException(409, 43913, "result not correctable");
        }
        if (whitelistHandoffGenerated.contains(sessionId)) throw new ExamException(409, 43925, "handoff already generated");
        String result = validateEnum(body, "result", Set.of("PASSED", "FAILED"));
        String publicComment = validateRequiredString(body, "publicComment", 1, 1000);
        List<Map<String, Object>> scores = body.get("manualScores") instanceof List<?> ? objectList(body, "manualScores") : List.of();
        int manual = scores.isEmpty() ? (session.manualScore == null ? 0 : session.manualScore) : validateManualScores(session, scores);
        int total = session.objectiveScore + manual;
        boolean passed = "PASSED".equals(result);
        if (passed && total < session.passScore) throw new ExamException(409, 43913, "corrected score below pass line");
        failBeforeWrite(request);
        String beforeStatus = session.status;
        String beforeResult = session.result;
        session.manualScore = session.manualRequired ? manual : null;
        session.totalScore = total;
        session.finalPassed = passed;
        session.status = passed ? (session.manualRequired ? "MANUAL_PASSED" : "AUTO_PASSED") : (session.manualRequired ? "MANUAL_FAILED" : "AUTO_FAILED");
        session.result = passed ? "PASSED" : "FAILED";
        session.passedAt = passed ? NOW : null;
        session.reviewedAt = NOW;
        session.updatedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        session.manualReview = mapOf("reviewId", "review-" + (++idSeq), "reviewerUserId", actor.userId(), "reviewerDisplayNameSnapshot", actor.displayName(), "manualScores", scores, "publicComment", publicComment, "internalNote", string(body.get("internalNote")), "result", result, "reviewedAt", NOW, "correction", true, "correctedFromStatus", beforeStatus, "correctedFromResult", beforeResult);
        audit(actor, "EXAM_SESSION", session.sessionId, "EXAM_RESULT_CORRECTED", "MEDIUM", beforeStatus, session.status, string(body.get("reason")));
        Map<String, Object> view = sessionView(session, true);
        remember(actor.userId(), "CORRECTION:" + sessionId, body, view);
        persistence.persistReviewWrite(request, actor.userId(), actorRole(actor), "exam.result-correction", "EXAM_RESULT_CORRECTED", beforeStatus, session.status, string(body.get("reason")), idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> requestSupplement(ExamUser actor, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "REQ_SUPP:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireSession(sessionId);
        if (!Set.of("PENDING_MANUAL_REVIEW", "SUPPLEMENT_SUBMITTED").contains(session.status)) throw new ExamException(409, 43913, "not supplementable");
        List<String> questionIds = stringList(body, "questionIds");
        if (questionIds.isEmpty()) throw new ExamException(400, 40001, "questionIds required");
        for (String id : questionIds) {
            ExamQuestionRecord question = questionInSession(session, id);
            if (!"SHORT_TEXT".equals(question.type)) throw new ExamException(409, 43916, "only short text can supplement");
        }
        validateRequiredString(body, "publicComment", 1, 1000);
        String dueAt = validateRequiredString(body, "supplementDueAt", 1, 80);
        try {
            if (!Instant.parse(dueAt).isAfter(Instant.parse(NOW)) || Instant.parse(dueAt).isAfter(Instant.parse("2026-06-06T12:00:00Z"))) {
                throw new ExamException(400, 40001, "invalid supplement due");
            }
        } catch (DateTimeParseException ex) {
            throw new ExamException(400, 40001, "invalid supplement due");
        }
        failBeforeWrite(request);
        String before = session.status;
        session.status = "NEEDS_SUPPLEMENT";
        session.result = "NEEDS_SUPPLEMENT";
        session.supplementQuestionIds = questionIds;
        session.supplementRequest = mapOf("questionIds", questionIds, "publicComment", string(body.get("publicComment")), "supplementDueAt", dueAt, "internalNote", string(body.get("internalNote")), "requestedAt", NOW);
        session.notificationStatus = notificationStatus(request);
        session.updatedAt = NOW;
        audit(actor, "EXAM_SESSION", session.sessionId, "EXAM_SUPPLEMENT_REQUESTED", "MEDIUM", before, session.status, string(body.get("reason")));
        Map<String, Object> view = sessionView(session, true);
        remember(actor.userId(), "REQ_SUPP:" + sessionId, body, view);
        persistence.persistSessionWrite(request, actor.userId(), actorRole(actor), "exam.request-supplement", "EXAM_SUPPLEMENT_REQUESTED", "MEDIUM", before, session.status, string(body.get("reason")), idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> cancel(ExamUser actor, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "CANCEL:" + sessionId, body);
        if (replay != null) return replay.value();
        ExamSessionRecord session = requireSession(sessionId);
        if (!Set.of("IN_PROGRESS", "PENDING_MANUAL_REVIEW", "NEEDS_SUPPLEMENT", "SUPPLEMENT_SUBMITTED").contains(session.status)) {
            throw new ExamException(409, 43913, "not cancellable");
        }
        failBeforeWrite(request);
        String before = session.status;
        session.status = "CANCELLED";
        session.result = "CANCELLED";
        session.cancelledAt = NOW;
        session.updatedAt = NOW;
        session.notificationStatus = notificationStatus(request);
        audit(actor, "EXAM_SESSION", session.sessionId, "EXAM_CANCELLED", "MEDIUM", before, session.status, string(body.get("reason")));
        Map<String, Object> view = sessionView(session, true);
        remember(actor.userId(), "CANCEL:" + sessionId, body, view);
        persistence.persistSessionWrite(request, actor.userId(), actorRole(actor), "exam.cancel", "EXAM_CANCELLED", "MEDIUM", before, session.status, string(body.get("reason")), idempotencyKey(body), canonical(body), sessionPersistenceView(session), view, 200);
        return view;
    }

    synchronized Map<String, Object> whitelistHandoff(ExamUser actor, String sessionId, HttpServletRequest request) {
        ExamSessionRecord session = requireSession(sessionId);
        if (!Set.of("AUTO_PASSED", "MANUAL_PASSED").contains(session.status)) throw new ExamException(409, 43924, "not passed");
        whitelistHandoffSnapshotsTotal++;
        whitelistHandoffGenerated.add(sessionId);
        Map<String, Object> snapshot = mapOf("sessionId", session.sessionId, "applicationId", session.applicationId, "handoffVersion", 1, "onboardingHandoffVersion", session.onboardingHandoffVersion, "userId", session.userId, "minecraftBindingSnapshot", session.minecraftBinding, "reviewDirection", session.reviewDirection, "attemptType", session.attemptType, "result", "PASSED", "scoreSummary", scoreSummary(session), "passedAt", session.passedAt == null ? NOW : session.passedAt, "reviewerSnapshot", reviewerSnapshot(session), "generatedAt", NOW);
        persistence.persistHandoff(request, actor.userId(), snapshot);
        return snapshot;
    }

    synchronized Map<String, Object> questions(ExamUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String type = enumQuery(query, "type", TYPES);
        String direction = enumQuery(query, "reviewDirection", DIRECTIONS);
        String difficulty = enumQuery(query, "difficulty", DIFFICULTIES);
        String status = enumQuery(query, "status", Set.of("DRAFT", "ACTIVE", "ARCHIVED"));
        String tag = query.get("tag");
        String keyword = lower(query.get("keyword"));
        String sort = sort(query, Set.of("updatedAt_desc", "createdAt_desc", "score_desc", "type_asc"), "updatedAt_desc");
        List<Map<String, Object>> rows = questions.values().stream()
                .filter(question -> type == null || type.equals(question.type))
                .filter(question -> direction == null || direction.equals(question.reviewDirection))
                .filter(question -> difficulty == null || difficulty.equals(question.difficulty))
                .filter(question -> status == null || status.equals(question.status))
                .filter(question -> tag == null || question.tags.contains(tag))
                .filter(question -> keyword == null || question.stem.toLowerCase().contains(keyword) || question.tags.stream().anyMatch(t -> t.toLowerCase().contains(keyword)))
                .map(question -> questionView(question, true))
                .sorted(questionComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> questionVersions(ExamUser actor, String questionId, Map<String, String> query, HttpServletRequest request) {
        if (!questions.containsKey(questionId)) throw new ExamException(404, 43902, "question not found");
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = sort(query, Set.of("version_desc", "version_asc"), "version_desc");
        List<Map<String, Object>> rows = questionVersions.getOrDefault(questionId, List.of()).stream()
                .sorted((left, right) -> "version_asc".equals(sort) ? Integer.compare(left.version, right.version) : Integer.compare(right.version, left.version))
                .map(question -> questionView(question, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> createQuestion(ExamUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "CREATE_QUESTION", body);
        if (replay != null) return replay.value();
        ExamQuestionRecord question = parseQuestion("q-" + (++idSeq), body, "DRAFT", 1);
        failBeforeWrite(request);
        addQuestion(question);
        audit(actor, "EXAM_QUESTION", question.questionId, "EXAM_QUESTION_CREATED", "MEDIUM", null, question.status, string(body.get("reason")));
        Map<String, Object> view = questionView(question, true);
        remember(actor.userId(), "CREATE_QUESTION", body, view);
        persistence.persistQuestionWrite(request, actor.userId(), actorRole(actor), "exam.create-question", "EXAM_QUESTION_CREATED", null, question.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 201);
        return view;
    }

    synchronized Map<String, Object> updateQuestion(ExamUser actor, String questionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "UPDATE_QUESTION:" + questionId, body);
        if (replay != null) return replay.value();
        ExamQuestionRecord old = requireQuestion(questionId);
        if ("ARCHIVED".equals(old.status)) throw new ExamException(409, 43913, "question archived");
        failBeforeWrite(request);
        ExamQuestionRecord updated = copyQuestion(old);
        if (body.containsKey("stem")) updated.stem = validateRequiredString(body, "stem", 1, 2000);
        if (body.containsKey("score")) updated.score = intValue(body.get("score"), 1, 100, 40001);
        updated.version = old.version + 1;
        updated.updatedAt = NOW;
        questions.put(questionId, updated);
        rememberQuestionVersion(updated);
        audit(actor, "EXAM_QUESTION", questionId, "EXAM_QUESTION_UPDATED", "MEDIUM", old.status, updated.status, string(body.get("reason")));
        Map<String, Object> view = questionView(updated, true);
        remember(actor.userId(), "UPDATE_QUESTION:" + questionId, body, view);
        persistence.persistQuestionWrite(request, actor.userId(), actorRole(actor), "exam.update-question", "EXAM_QUESTION_UPDATED", old.status, updated.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 200);
        return view;
    }

    synchronized Map<String, Object> archiveQuestion(ExamUser actor, String questionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        ExamQuestionRecord question = requireQuestion(questionId);
        IdempotencyRecord replay = replay(actor.userId(), "ARCHIVE_QUESTION:" + questionId, body);
        if (replay != null) return replay.value();
        failBeforeWrite(request);
        String before = question.status;
        question.status = "ARCHIVED";
        question.archivedAt = NOW;
        question.updatedAt = NOW;
        audit(actor, "EXAM_QUESTION", questionId, "EXAM_QUESTION_ARCHIVED", "MEDIUM", before, question.status, string(body.get("reason")));
        Map<String, Object> view = questionView(question, true);
        remember(actor.userId(), "ARCHIVE_QUESTION:" + questionId, body, view);
        persistence.persistQuestionWrite(request, actor.userId(), actorRole(actor), "exam.archive-question", "EXAM_QUESTION_ARCHIVED", before, question.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 200);
        return view;
    }

    synchronized Map<String, Object> templates(ExamUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String direction = enumQuery(query, "reviewDirection", DIRECTIONS);
        String difficulty = enumQuery(query, "difficulty", DIFFICULTIES);
        String status = enumQuery(query, "status", Set.of("DRAFT", "PUBLISHED", "ARCHIVED"));
        String sort = sort(query, Set.of("updatedAt_desc", "publishedAt_desc", "name_asc"), "updatedAt_desc");
        List<Map<String, Object>> rows = templates.values().stream()
                .filter(template -> direction == null || direction.equals(template.reviewDirection))
                .filter(template -> difficulty == null || difficulty.equals(template.difficulty))
                .filter(template -> status == null || status.equals(template.status))
                .map(this::templateView)
                .sorted(templateComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> createTemplate(ExamUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "CREATE_TEMPLATE", body);
        if (replay != null) return replay.value();
        PaperTemplateRecord template = parseTemplate("tpl-" + (++idSeq), body, "DRAFT", 1);
        failBeforeWrite(request);
        templates.put(template.templateId, template);
        audit(actor, "EXAM_TEMPLATE", template.templateId, "EXAM_TEMPLATE_CREATED", "MEDIUM", null, template.status, string(body.get("reason")));
        Map<String, Object> view = templateView(template);
        remember(actor.userId(), "CREATE_TEMPLATE", body, view);
        persistence.persistTemplateWrite(request, actor.userId(), actorRole(actor), "exam.create-template", "EXAM_TEMPLATE_CREATED", null, template.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 201);
        return view;
    }

    synchronized Map<String, Object> updateTemplate(ExamUser actor, String templateId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        PaperTemplateRecord template = requireTemplate(templateId);
        IdempotencyRecord replay = replay(actor.userId(), "UPDATE_TEMPLATE:" + templateId, body);
        if (replay != null) return replay.value();
        if ("ARCHIVED".equals(template.status)) throw new ExamException(409, 43922, "template archived");
        failBeforeWrite(request);
        if (body.containsKey("name")) template.name = validateRequiredString(body, "name", 1, 80);
        if ("PUBLISHED".equals(template.status)) {
            template.version++;
            template.status = "DRAFT";
            template.publishedAt = null;
        }
        template.updatedAt = NOW;
        audit(actor, "EXAM_TEMPLATE", templateId, "EXAM_TEMPLATE_UPDATED", "MEDIUM", null, template.status, string(body.get("reason")));
        Map<String, Object> view = templateView(template);
        remember(actor.userId(), "UPDATE_TEMPLATE:" + templateId, body, view);
        persistence.persistTemplateWrite(request, actor.userId(), actorRole(actor), "exam.update-template", "EXAM_TEMPLATE_UPDATED", null, template.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 200);
        return view;
    }

    synchronized Map<String, Object> publishPreview(ExamUser actor, String templateId, HttpServletRequest request) {
        PaperTemplateRecord template = requireTemplate(templateId);
        if ("ARCHIVED".equals(template.status)) throw new ExamException(409, 43922, "template archived");
        return publishPreviewView(template, request);
    }

    synchronized Map<String, Object> publishTemplate(ExamUser actor, String templateId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        PaperTemplateRecord template = requireTemplate(templateId);
        IdempotencyRecord replay = replay(actor.userId(), "PUBLISH_TEMPLATE:" + templateId, body);
        if (replay != null) return replay.value();
        if ("ARCHIVED".equals(template.status)) throw new ExamException(409, 43922, "template archived");
        if ("CONTENT:UNAVAILABLE".equals(testHeader(request, "X-Test-Dependency-Mode"))) throw new ExamException(502, 46930, "content unavailable");
        if (selectQuestions(template).isEmpty()) throw new ExamException(409, 43915, "question bank not enough");
        failBeforeWrite(request);
        String before = template.status;
        template.status = "PUBLISHED";
        template.publishedAt = NOW;
        template.updatedAt = NOW;
        audit(actor, "EXAM_TEMPLATE", templateId, "EXAM_TEMPLATE_PUBLISHED", "MEDIUM", before, template.status, string(body.get("reason")));
        Map<String, Object> view = templateView(template);
        remember(actor.userId(), "PUBLISH_TEMPLATE:" + templateId, body, view);
        persistence.persistTemplateWrite(request, actor.userId(), actorRole(actor), "exam.publish-template", "EXAM_TEMPLATE_PUBLISHED", before, template.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 200);
        return view;
    }

    synchronized Map<String, Object> archiveTemplate(ExamUser actor, String templateId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        PaperTemplateRecord template = requireTemplate(templateId);
        IdempotencyRecord replay = replay(actor.userId(), "ARCHIVE_TEMPLATE:" + templateId, body);
        if (replay != null) return replay.value();
        failBeforeWrite(request);
        String before = template.status;
        template.status = "ARCHIVED";
        template.updatedAt = NOW;
        audit(actor, "EXAM_TEMPLATE", templateId, "EXAM_TEMPLATE_ARCHIVED", "MEDIUM", before, template.status, string(body.get("reason")));
        Map<String, Object> view = templateView(template);
        remember(actor.userId(), "ARCHIVE_TEMPLATE:" + templateId, body, view);
        persistence.persistTemplateWrite(request, actor.userId(), actorRole(actor), "exam.archive-template", "EXAM_TEMPLATE_ARCHIVED", before, template.status, string(body.get("reason")), idempotencyKey(body), canonical(body), view, view, 200);
        return view;
    }

    synchronized Map<String, Object> auditLogs(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        validateTimeRange(query);
        String result = enumQuery(query, "result", Set.of("SUCCESS", "FAILED"));
        String action = query.get("action");
        String actorUserId = query.get("actorUserId");
        String sessionId = query.get("sessionId");
        String sort = sort(query, Set.of("createdAt_desc", "createdAt_asc"), "createdAt_desc");
        List<Map<String, Object>> rows = audits.stream()
                .filter(row -> result == null || result.equals(row.get("result")))
                .filter(row -> action == null || action.equals(row.get("action")))
                .filter(row -> actorUserId == null || actorUserId.equals(row.get("actorUserId")))
                .filter(row -> sessionId == null || sessionId.equals(row.get("targetId")))
                .sorted((a, b) -> "createdAt_asc".equals(sort) ? Objects.toString(a.get("createdAt")).compareTo(Objects.toString(b.get("createdAt"))) : Objects.toString(b.get("createdAt")).compareTo(Objects.toString(a.get("createdAt"))))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> opsSummary(HttpServletRequest request) {
        if (storeShouldFail(request)) throw new ExamException(500, 51900, "exam internal error");
        long pending = sessions.values().stream().filter(s -> Set.of("PENDING_MANUAL_REVIEW", "SUPPLEMENT_SUBMITTED").contains(s.status)).count();
        long passed = sessions.values().stream().filter(s -> "PASSED".equals(s.result)).count();
        long failed = sessions.values().stream().filter(s -> "FAILED".equals(s.result)).count();
        long published = templates.values().stream().filter(t -> "PUBLISHED".equals(t.status)).count();
        Map<String, Object> counts = persistence.counts();
        String storageMode = Objects.toString(counts.getOrDefault("storageMode", "IN_MEMORY"));
        return mapOf("service", "exam", "port", 8131, "legacyPort", 8109, "storageMode", storageMode, "authMode", "TEST_STUB", "onboardingMode", "TEST_STUB", "profileMode", "TEST_STUB", "contentMode", "TEST_STUB", "notificationMode", "TEST_STUB", "testControlsEnabled", testControls.enabled(), "sessionsTotal", counts.getOrDefault("sessionsTotal", sessions.size()), "pendingManualReviewTotal", (int) pending, "passedTotal", (int) passed, "failedTotal", (int) failed, "questionsTotal", counts.getOrDefault("questionsTotal", questions.size()), "publishedTemplatesTotal", counts.getOrDefault("publishedTemplatesTotal", (int) published), "whitelistHandoffSnapshotsTotal", counts.getOrDefault("whitelistHandoffSnapshotsTotal", whitelistHandoffSnapshotsTotal), "auditsTotal", counts.getOrDefault("auditsTotal", audits.size()), "idempotencyRecordsTotal", counts.getOrDefault("idempotencyRecordsTotal", idempotency.size()), "lastAuditAt", audits.isEmpty() ? null : audits.getLast().get("createdAt"), "productionGaps", "POSTGRESQL_PRIMARY".equals(storageMode) ? List.of("P0_AUTH_STUB", "P0_ONBOARDING_STUB", "P0_PROFILE_STUB", "P0_CONTENT_STUB", "P0_NOTIFICATION_STUB", "WHITELIST_NOT_IMPLEMENTED") : List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB", "P0_ONBOARDING_STUB", "P0_PROFILE_STUB", "P0_CONTENT_STUB", "P0_NOTIFICATION_STUB", "WHITELIST_NOT_IMPLEMENTED"));
    }

    private Handoff handoff(ExamUser user, String applicationId, HttpServletRequest request) {
        if (applicationId.contains("blocked")) throw new ExamException(409, 43910, "onboarding handoff blocked");
        if ("ONBOARDING:UNAVAILABLE".equals(testHeader(request, "X-Test-Dependency-Mode"))) throw new ExamException(502, 46910, "onboarding unavailable");
        if (!applicationId.startsWith("app-")) throw new ExamException(409, 43910, "onboarding not ready");
        String direction = switch (applicationId) {
            case "app-general" -> "GENERAL";
            default -> "REDSTONE";
        };
        return new Handoff(applicationId, user.userId(), direction);
    }

    private boolean requiresContent(ExamUser user, PaperTemplateRecord template, HttpServletRequest request) {
        return (testControls.enabled() && "CONTENT_UNAVAILABLE".equals(user.profileStatus())) || "CONTENT:UNAVAILABLE".equals(testHeader(request, "X-Test-Dependency-Mode"));
    }

    private PaperTemplateRecord latestTemplate(String direction, String difficulty) {
        return templates.values().stream()
                .filter(template -> "PUBLISHED".equals(template.status))
                .filter(template -> direction.equals(template.reviewDirection))
                .filter(template -> difficulty.equals(template.difficulty))
                .max(Comparator.comparing(template -> template.publishedAt == null ? "" : template.publishedAt))
                .orElse(null);
    }

    private List<ExamQuestionRecord> selectQuestions(PaperTemplateRecord template) {
        List<ExamQuestionRecord> selected = new ArrayList<>();
        for (Map<String, Object> rule : template.questionRules) {
            String type = string(rule.get("type"));
            int count = intValue(rule.get("count"), 1, 100, 40001);
            @SuppressWarnings("unchecked")
            List<String> tags = rule.get("tags") instanceof List<?> list ? list.stream().map(Object::toString).toList() : List.of();
            List<ExamQuestionRecord> matches = matchingQuestions(template, type, tags)
                    .limit(count)
                    .toList();
            if (matches.size() < count) return List.of();
            selected.addAll(matches);
        }
        return selected;
    }

    private java.util.stream.Stream<ExamQuestionRecord> matchingQuestions(PaperTemplateRecord template, String type, List<String> tags) {
        return questions.values().stream()
                .filter(q -> "ACTIVE".equals(q.status))
                .filter(q -> template.reviewDirection.equals(q.reviewDirection))
                .filter(q -> template.difficulty.equals(q.difficulty))
                .filter(q -> type.equals(q.type))
                .filter(q -> tags.isEmpty() || q.tags.stream().anyMatch(tags::contains))
                .sorted(Comparator.comparing(q -> q.questionId));
    }

    private void validateAnswers(ExamSessionRecord session, List<Map<String, Object>> answers, boolean requireAll, boolean supplementOnly) {
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> answer : answers) {
            String questionId = validateRequiredString(answer, "questionId", 1, 120);
            if (!seen.add(questionId)) throw new ExamException(409, 43916, "duplicated answer");
            ExamQuestionRecord question = questionInSession(session, questionId);
            if (supplementOnly && !session.supplementQuestionIds.contains(questionId)) throw new ExamException(409, 43916, "question not requested");
            if (supplementOnly && !"SHORT_TEXT".equals(question.type)) throw new ExamException(409, 43916, "cannot supplement objective");
            if ("SHORT_TEXT".equals(question.type)) {
                String text = string(answer.get("textAnswer"));
                if (requireAll && (text == null || text.isBlank())) throw new ExamException(409, 43916, "text answer missing");
                if (text != null && text.length() > 2000) throw new ExamException(400, 40001, "text answer too long");
            } else {
                List<String> selected = stringList(answer, "selectedOptionIds");
                if (selected.isEmpty()) throw new ExamException(409, 43916, "option missing");
                if (("SINGLE_CHOICE".equals(question.type) || "TRUE_FALSE".equals(question.type)) && selected.size() != 1) throw new ExamException(409, 43916, "single answer expected");
                for (String option : selected) {
                    if (question.options.stream().noneMatch(o -> option.equals(o.get("optionId")))) throw new ExamException(409, 43916, "invalid option");
                }
            }
        }
        if (requireAll && !supplementOnly) {
            for (ExamQuestionRecord question : session.questions) {
                if (!seen.contains(question.questionId)) throw new ExamException(409, 43916, "answer missing");
            }
        }
    }

    private void score(ExamSessionRecord session, List<Map<String, Object>> answers) {
        int objective = 0;
        boolean manualRequired = false;
        Map<String, Map<String, Object>> byQuestion = new LinkedHashMap<>();
        for (Map<String, Object> answer : answers) byQuestion.put(string(answer.get("questionId")), answer);
        for (ExamQuestionRecord question : session.questions) {
            Map<String, Object> answer = byQuestion.get(question.questionId);
            if ("SHORT_TEXT".equals(question.type)) {
                manualRequired = true;
                continue;
            }
            List<String> selected = stringList(answer, "selectedOptionIds");
            if (new LinkedHashSet<>(selected).equals(new LinkedHashSet<>(question.correctOptionIds))) {
                objective += question.score;
            }
        }
        session.objectiveScore = objective;
        session.manualRequired = manualRequired;
        session.objectivePassed = objective >= session.objectivePassScore;
        if (!session.objectivePassed) {
            session.status = "AUTO_FAILED";
            session.result = "FAILED";
            session.finalPassed = false;
            session.totalScore = objective;
            return;
        }
        if (manualRequired) {
            session.status = "PENDING_MANUAL_REVIEW";
            session.result = "PENDING";
            session.finalPassed = null;
            session.totalScore = null;
            return;
        }
        session.totalScore = objective;
        boolean passed = objective >= session.passScore;
        session.status = passed ? "AUTO_PASSED" : "AUTO_FAILED";
        session.result = passed ? "PASSED" : "FAILED";
        session.finalPassed = passed;
        session.passedAt = passed ? NOW : null;
    }

    private List<Map<String, Object>> mergeSupplement(ExamSessionRecord session, List<Map<String, Object>> supplement) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> answer : session.answers) merged.put(string(answer.get("questionId")), new LinkedHashMap<>(answer));
        for (Map<String, Object> answer : supplement) merged.put(string(answer.get("questionId")), new LinkedHashMap<>(answer));
        return new ArrayList<>(merged.values());
    }

    private int validateManualScores(ExamSessionRecord session, List<Map<String, Object>> scores) {
        if (scores.isEmpty()) throw new ExamException(400, 40001, "manual scores required");
        int total = 0;
        for (Map<String, Object> item : scores) {
            String questionId = validateRequiredString(item, "questionId", 1, 120);
            ExamQuestionRecord question = questionInSession(session, questionId);
            if (!"SHORT_TEXT".equals(question.type)) throw new ExamException(409, 43916, "manual score only for short text");
            int score = intValue(item.get("score"), 0, question.score, 43923);
            total += score;
        }
        return total;
    }

    private ExamSessionRecord requireOwnedSession(ExamUser user, String sessionId) {
        ExamSessionRecord session = requireSession(sessionId);
        if (!user.userId().equals(session.userId)) throw new ExamException(404, 43900, "session not found");
        return session;
    }

    private ExamSessionRecord requireSession(String sessionId) {
        ExamSessionRecord session = sessions.get(sessionId);
        if (session == null) throw new ExamException(404, 43900, "session not found");
        return session;
    }

    private ExamQuestionRecord requireQuestion(String questionId) {
        ExamQuestionRecord question = questions.get(questionId);
        if (question == null) throw new ExamException(404, 43902, "question not found");
        return question;
    }

    private PaperTemplateRecord requireTemplate(String templateId) {
        PaperTemplateRecord template = templates.get(templateId);
        if (template == null) throw new ExamException(404, 43903, "template not found");
        return template;
    }

    private ExamQuestionRecord questionInSession(ExamSessionRecord session, String questionId) {
        return session.questions.stream().filter(q -> questionId.equals(q.questionId)).findFirst().orElseThrow(() -> new ExamException(409, 43916, "question not in paper"));
    }

    private boolean ended(String status) {
        return Set.of("AUTO_PASSED", "AUTO_FAILED", "MANUAL_PASSED", "MANUAL_FAILED", "EXPIRED", "CANCELLED").contains(status);
    }

    private Map<String, Object> sessionView(ExamSessionRecord session, boolean admin) {
        Map<String, Object> view = mapOf("sessionId", session.sessionId, "applicationId", session.applicationId, "handoffVersion", session.onboardingHandoffVersion, "userId", session.userId, "displayNameSnapshot", session.displayName, "minecraftBindingSnapshot", session.minecraftBinding, "reviewDirection", session.reviewDirection, "attemptType", session.attemptType, "difficulty", session.difficulty, "status", session.status, "result", session.result, "templateId", session.templateId, "templateVersion", session.templateVersion, "paperId", session.paperId, "scoreSummary", session.submittedAt == null ? null : scoreSummary(session), "manualReview", manualReviewView(session, admin), "supplementRequest", supplementView(session, admin), "notificationStatus", session.notificationStatus, "degraded", false, "degradeReasons", List.of(), "startedAt", session.startedAt, "lastSavedAt", session.lastSavedAt, "submittedAt", session.submittedAt, "reviewedAt", session.reviewedAt, "expiresAt", session.expiresAt, "passedAt", session.passedAt, "cancelledAt", session.cancelledAt, "createdAt", session.createdAt, "updatedAt", session.updatedAt);
        return view;
    }

    private Map<String, Object> scoreSummary(ExamSessionRecord session) {
        return mapOf("objectiveScore", session.objectiveScore, "manualScore", session.manualScore, "totalScore", session.totalScore, "objectivePassed", session.objectivePassed, "finalPassed", session.finalPassed, "passScore", session.passScore, "objectivePassScore", session.objectivePassScore, "manualRequired", session.manualRequired, "scoredAt", session.submittedAt == null ? NOW : session.submittedAt);
    }

    private Map<String, Object> manualReviewView(ExamSessionRecord session, boolean admin) {
        if (session.manualReview == null) return null;
        Map<String, Object> view = new LinkedHashMap<>(session.manualReview);
        if (!admin) view.remove("internalNote");
        return view;
    }

    private Map<String, Object> supplementView(ExamSessionRecord session, boolean admin) {
        if (session.supplementRequest == null) return null;
        Map<String, Object> view = new LinkedHashMap<>(session.supplementRequest);
        if (!admin) view.remove("internalNote");
        return view;
    }

    private Map<String, Object> paperView(ExamSessionRecord session, boolean admin) {
        int objective = session.questions.stream().filter(q -> !"SHORT_TEXT".equals(q.type)).mapToInt(q -> q.score).sum();
        int manual = session.questions.stream().filter(q -> "SHORT_TEXT".equals(q.type)).mapToInt(q -> q.score).sum();
        return mapOf("paperId", session.paperId, "sessionId", session.sessionId, "templateId", session.templateId, "templateVersion", session.templateVersion, "reviewDirection", session.reviewDirection, "attemptType", session.attemptType, "timeLimitMinutes", session.timeLimitMinutes, "questions", session.questions.stream().map(q -> questionView(q, admin)).toList(), "totalScore", objective + manual, "objectiveTotalScore", objective, "manualTotalScore", manual, "generatedAt", session.createdAt);
    }

    private Map<String, Object> questionView(ExamQuestionRecord question, boolean admin) {
        Map<String, Object> view = mapOf("questionId", question.questionId, "version", question.version, "type", question.type, "reviewDirection", question.reviewDirection, "difficulty", question.difficulty, "stem", question.stem, "options", question.options, "score", question.score, "tags", question.tags, "status", question.status, "createdAt", question.createdAt, "updatedAt", question.updatedAt, "archivedAt", question.archivedAt);
        if (admin) {
            view.put("correctOptionIds", question.correctOptionIds);
            view.put("referenceAnswer", question.referenceAnswer);
        }
        return view;
    }

    private Map<String, Object> templateView(PaperTemplateRecord template) {
        return mapOf("templateId", template.templateId, "version", template.version, "name", template.name, "reviewDirection", template.reviewDirection, "difficulty", template.difficulty, "status", template.status, "timeLimitMinutes", template.timeLimitMinutes, "passScore", template.passScore, "objectivePassScore", template.objectivePassScore, "questionRules", template.questionRules, "contentRuleVersion", template.contentRuleVersion, "retakeCooldownHours", template.retakeCooldownHours, "createdAt", template.createdAt, "updatedAt", template.updatedAt, "publishedAt", template.publishedAt);
    }

    private Map<String, Object> sessionPersistenceView(ExamSessionRecord session) {
        Map<String, Object> view = sessionView(session, true);
        view.put("questions", session.questions.stream().map(question -> questionView(question, true)).toList());
        return view;
    }

    private Map<String, Object> answerPersistenceView(ExamSessionRecord session, Map<String, Object> sheet) {
        Map<String, Object> view = new LinkedHashMap<>(sheet);
        view.put("status", session.status);
        return view;
    }

    private Map<String, Object> publishPreviewView(PaperTemplateRecord template, HttpServletRequest request) {
        boolean contentUnavailable = template.contentRuleVersion != null && "CONTENT:UNAVAILABLE".equals(testHeader(request, "X-Test-Dependency-Mode"));
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        List<ExamQuestionRecord> sample = new ArrayList<>();
        int totalScore = 0;
        int objectiveScore = 0;
        int manualScore = 0;
        boolean enough = true;
        for (Map<String, Object> rule : template.questionRules) {
            String type = string(rule.get("type"));
            int count = ((Number) rule.get("count")).intValue();
            int scoreEach = ((Number) rule.get("scoreEach")).intValue();
            List<String> tags = rule.get("tags") instanceof List<?> list ? list.stream().map(Object::toString).toList() : List.of();
            List<ExamQuestionRecord> matches = matchingQuestions(template, type, tags).toList();
            boolean ruleEnough = matches.size() >= count;
            if (!ruleEnough) {
                enough = false;
                warnings.add("QUESTION_BANK_NOT_ENOUGH:" + type);
            }
            totalScore += count * scoreEach;
            if ("SHORT_TEXT".equals(type)) manualScore += count * scoreEach;
            else objectiveScore += count * scoreEach;
            sample.addAll(matches.stream().limit(count).toList());
            rules.add(mapOf("type", type, "count", count, "scoreEach", scoreEach, "tags", tags, "matchedQuestionCount", matches.size(), "enough", ruleEnough));
        }
        if (contentUnavailable) warnings.add("CONTENT_RULE_UNAVAILABLE");
        return mapOf("templateId", template.templateId, "templateVersion", template.version, "status", template.status, "readyToPublish", enough && !contentUnavailable, "totalScore", totalScore, "objectiveTotalScore", objectiveScore, "manualTotalScore", manualScore, "contentRuleStatus", contentUnavailable ? "UNAVAILABLE" : "VALID", "rules", rules, "samplePaper", mapOf("questions", sample.stream().map(q -> questionView(q, true)).toList()), "warnings", warnings);
    }

    private Map<String, Object> reviewerSnapshot(ExamSessionRecord session) {
        if (session.manualReview == null) return null;
        return mapOf("userId", session.manualReview.get("reviewerUserId"), "displayName", session.manualReview.get("reviewerDisplayNameSnapshot"));
    }

    private ExamQuestionRecord parseQuestion(String id, Map<String, Object> body, String status, int version) {
        String type = validateEnum(body, "type", TYPES);
        String direction = validateEnum(body, "reviewDirection", DIRECTIONS);
        String difficulty = validateEnum(body, "difficulty", DIFFICULTIES);
        String stem = validateRequiredString(body, "stem", 1, 2000);
        int score = intValue(body.get("score"), 1, 100, 40001);
        List<Map<String, Object>> options = body.get("options") instanceof List<?> list ? list.stream().map(this::map).toList() : List.of();
        List<String> correct = body.get("correctOptionIds") instanceof List<?> list ? list.stream().map(Object::toString).toList() : List.of();
        String reference = string(body.get("referenceAnswer"));
        if ("SHORT_TEXT".equals(type)) {
            if (!options.isEmpty() || reference == null || reference.isBlank()) throw new ExamException(400, 40001, "invalid short text question");
        } else {
            if (options.size() < 2 || correct.isEmpty()) throw new ExamException(400, 40001, "invalid objective question");
            if ("TRUE_FALSE".equals(type) && options.size() != 2) throw new ExamException(400, 40001, "invalid true false question");
        }
        List<String> tags = body.get("tags") instanceof List<?> list ? list.stream().map(Object::toString).limit(10).toList() : List.of();
        return question(id, type, direction, difficulty, stem, options, correct, reference, score, tags, status, version);
    }

    private PaperTemplateRecord parseTemplate(String id, Map<String, Object> body, String status, int version) {
        String name = validateRequiredString(body, "name", 1, 80);
        String direction = validateEnum(body, "reviewDirection", DIRECTIONS);
        String difficulty = validateEnum(body, "difficulty", DIFFICULTIES);
        int timeLimit = intValue(body.get("timeLimitMinutes"), 15, 180, 40001);
        int passScore = intValue(body.get("passScore"), 1, 10000, 40001);
        int objectivePassScore = intValue(body.get("objectivePassScore"), 0, 10000, 40001);
        List<Map<String, Object>> rules = objectList(body, "questionRules");
        if (rules.isEmpty()) throw new ExamException(400, 40001, "question rules required");
        int total = 0;
        for (Map<String, Object> rule : rules) {
            validateEnum(rule, "type", TYPES);
            int count = intValue(rule.get("count"), 1, 100, 40001);
            int scoreEach = intValue(rule.get("scoreEach"), 1, 100, 40001);
            total += count * scoreEach;
        }
        if (passScore > total || objectivePassScore > total) throw new ExamException(400, 40001, "score threshold invalid");
        int cooldown = intValue(body.get("retakeCooldownHours"), 0, 720, 40001);
        PaperTemplateRecord template = template(id, name, direction, difficulty, status, version, timeLimit, passScore, objectivePassScore, rules);
        template.contentRuleVersion = string(body.get("contentRuleVersion"));
        template.retakeCooldownHours = cooldown;
        return template;
    }

    private ExamQuestionRecord question(String id, String type, String direction, String difficulty, String stem, List<Map<String, Object>> options, List<String> correct, String reference, int score, List<String> tags, String status) {
        return question(id, type, direction, difficulty, stem, options, correct, reference, score, tags, status, 1);
    }

    private ExamQuestionRecord question(String id, String type, String direction, String difficulty, String stem, List<Map<String, Object>> options, List<String> correct, String reference, int score, List<String> tags, String status, int version) {
        ExamQuestionRecord question = new ExamQuestionRecord();
        question.questionId = id;
        question.version = version;
        question.type = type;
        question.reviewDirection = direction;
        question.difficulty = difficulty;
        question.stem = stem;
        question.options = copyList(options);
        question.correctOptionIds = new ArrayList<>(correct);
        question.referenceAnswer = reference;
        question.score = score;
        question.tags = new ArrayList<>(tags);
        question.status = status;
        question.createdAt = NOW;
        question.updatedAt = NOW;
        return question;
    }

    private ExamQuestionRecord copyQuestion(ExamQuestionRecord source) {
        ExamQuestionRecord copy = question(source.questionId, source.type, source.reviewDirection, source.difficulty, source.stem, source.options, source.correctOptionIds, source.referenceAnswer, source.score, source.tags, source.status, source.version);
        copy.archivedAt = source.archivedAt;
        return copy;
    }

    private PaperTemplateRecord template(String id, String name, String direction, String difficulty, String status, int version, int timeLimit, int passScore, int objectivePassScore, List<Map<String, Object>> rules) {
        PaperTemplateRecord template = new PaperTemplateRecord();
        template.templateId = id;
        template.name = name;
        template.reviewDirection = direction;
        template.difficulty = difficulty;
        template.status = status;
        template.version = version;
        template.timeLimitMinutes = timeLimit;
        template.passScore = passScore;
        template.objectivePassScore = objectivePassScore;
        template.questionRules = copyList(rules);
        template.contentRuleVersion = "2026-05-22";
        template.retakeCooldownHours = 24;
        template.createdAt = NOW;
        template.updatedAt = NOW;
        template.publishedAt = "PUBLISHED".equals(status) ? NOW : null;
        return template;
    }

    private Map<String, Object> rule(String type, int count, int scoreEach, List<String> tags) {
        return mapOf("type", type, "count", count, "scoreEach", scoreEach, "tags", tags);
    }

    private List<Map<String, Object>> options(String... ids) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (String id : ids) options.add(mapOf("optionId", id, "label", id, "text", "Option " + id));
        return options;
    }

    private void addQuestion(ExamQuestionRecord question) {
        questions.put(question.questionId, question);
        rememberQuestionVersion(question);
    }

    private void rememberQuestionVersion(ExamQuestionRecord question) {
        questionVersions.computeIfAbsent(question.questionId, ignored -> new ArrayList<>()).add(copyQuestion(question));
    }

    private void audit(ExamUser actor, String targetType, String targetId, String action, String risk, String before, String after, String reason) {
        audits.add(auditRow("audit-" + (++idSeq), actor.userId(), targetType, targetId, action, "SUCCESS", risk, before, after, reason));
    }

    private String actorRole(ExamUser actor) {
        return actor.roles().stream().findFirst().orElse("USER");
    }

    private Map<String, Object> auditRow(String id, String actorId, String targetType, String targetId, String action, String result, String risk, String before, String after, String reason) {
        return mapOf("id", id, "requestId", ExamController.requestId(), "actorUserId", actorId, "actorRole", "ADMIN", "actorPermissions", List.of(), "sourceIp", "127.0.0.1", "targetType", targetType, "targetId", targetId, "action", action, "riskLevel", risk, "reason", reason == null ? "contract" : reason, "paramsSummary", "summary", "beforeState", before, "afterState", after, "result", result, "failureReason", null, "createdAt", NOW);
    }

    private void failBeforeWrite(HttpServletRequest request) {
        if (auditShouldFail(request)) throw new ExamException(500, 51901, "exam audit failed");
        if (storeShouldFail(request)) throw new ExamException(500, 51902, "exam state failed");
    }

    private void failStoreOnly(HttpServletRequest request) {
        if (storeShouldFail(request)) throw new ExamException(500, 51902, "exam state failed");
    }

    private boolean auditShouldFail(HttpServletRequest request) {
        return "true".equals(testHeader(request, "X-Test-Fail-Audit"));
    }

    private boolean storeShouldFail(HttpServletRequest request) {
        return "true".equals(testHeader(request, "X-Test-Fail-Store"));
    }

    private String notificationStatus(HttpServletRequest request) {
        String mode = testHeader(request, "X-Test-Notification-Mode");
        return "unavailable".equals(mode) || "timeout".equals(mode) ? "FAILED" : "DELIVERED";
    }

    private String testHeader(HttpServletRequest request, String name) {
        return testControls.enabled() && request != null ? request.getHeader(name) : null;
    }

    private IdempotencyRecord replay(String actorId, String operation, Map<String, Object> body) {
        String key = idempotencyKey(body);
        if (key == null) return null;
        IdempotencyRecord existing = idempotency.get(actorId + ":" + operation + ":" + key);
        if (existing != null && !existing.fingerprint().equals(canonical(body))) throw new ExamException(409, 43919, "idempotency conflict");
        return existing;
    }

    private void remember(String actorId, String operation, Map<String, Object> body, Map<String, Object> value) {
        String key = idempotencyKey(body);
        if (key != null) idempotency.put(actorId + ":" + operation + ":" + key, new IdempotencyRecord(canonical(body), value));
    }

    private String idempotencyKey(Map<String, Object> body) {
        Object value = body.get("idempotencyKey");
        return value == null ? null : value.toString();
    }

    private String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> Objects.toString(entry.getKey())))
                    .forEach(entry -> builder.append(Objects.toString(entry.getKey())).append('=').append(canonical(entry.getValue())).append(';'));
            return builder.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (Object item : list) builder.append(canonical(item)).append(';');
            return builder.append(']').toString();
        }
        return value == null ? "null" : value.getClass().getSimpleName() + ":" + value;
    }

    private Map<String, Object> pageRows(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return mapOf("items", new ArrayList<>(rows.subList(from, to)), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private Comparator<Map<String, Object>> sessionComparator(String sort) {
        return switch (sort) {
            case "createdAt_asc" -> Comparator.comparing(row -> Objects.toString(row.get("createdAt")));
            case "submittedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("submittedAt"))).reversed();
            case "status_asc" -> Comparator.comparing(row -> Objects.toString(row.get("status")));
            case "score_desc" -> Comparator.comparing((Map<String, Object> row) -> scoreForSort(row)).reversed();
            default -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("updatedAt"))).reversed();
        };
    }

    private int scoreForSort(Map<String, Object> row) {
        Object summary = row.get("scoreSummary");
        if (summary instanceof Map<?, ?> map && map.get("totalScore") instanceof Number number) return number.intValue();
        return -1;
    }

    private Comparator<Map<String, Object>> questionComparator(String sort) {
        return switch (sort) {
            case "createdAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("createdAt"))).reversed();
            case "score_desc" -> Comparator.comparing((Map<String, Object> row) -> ((Number) row.get("score")).intValue()).reversed();
            case "type_asc" -> Comparator.comparing(row -> Objects.toString(row.get("type")));
            default -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("updatedAt"))).reversed();
        };
    }

    private Comparator<Map<String, Object>> templateComparator(String sort) {
        return switch (sort) {
            case "publishedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("publishedAt"))).reversed();
            case "name_asc" -> Comparator.comparing(row -> Objects.toString(row.get("name")));
            default -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("updatedAt"))).reversed();
        };
    }

    private boolean matchesSession(ExamSessionRecord session, String keyword) {
        String mc = session.minecraftBinding == null ? "" : Objects.toString(session.minecraftBinding.get("minecraftId"), "");
        return session.sessionId.toLowerCase().contains(keyword) || session.applicationId.toLowerCase().contains(keyword) || session.userId.toLowerCase().contains(keyword) || session.displayName.toLowerCase().contains(keyword) || mc.toLowerCase().contains(keyword);
    }

    private int page(Map<String, String> query) {
        return intRange(query, "page", 1, 1, Integer.MAX_VALUE, 40002);
    }

    private int pageSize(Map<String, String> query) {
        return intRange(query, "pageSize", 20, 1, 100, 40002);
    }

    private int intRange(Map<String, String> query, String key, int fallback, int min, int max, int code) {
        if (!query.containsKey(key)) return fallback;
        try {
            int value = Integer.parseInt(query.get(key));
            if (value < min || value > max) throw new ExamException(400, code, "invalid number");
            return value;
        } catch (NumberFormatException ex) {
            throw new ExamException(400, code, "invalid number");
        }
    }

    private String sort(Map<String, String> query, Set<String> allowed, String fallback) {
        String value = query.getOrDefault("sort", fallback);
        if (!allowed.contains(value)) throw new ExamException(400, 40003, "invalid sort");
        return value;
    }

    private String enumQuery(Map<String, String> query, String key, Set<String> allowed) {
        if (!query.containsKey(key)) return null;
        String value = query.get(key);
        if (!allowed.contains(value)) throw new ExamException(400, 40001, "invalid " + key);
        return value;
    }

    private Boolean boolQuery(Map<String, String> query, String key) {
        if (!query.containsKey(key)) return null;
        if ("true".equals(query.get(key))) return true;
        if ("false".equals(query.get(key))) return false;
        throw new ExamException(400, 40001, "invalid boolean");
    }

    private void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.containsKey("from") ? Instant.parse(query.get("from")) : null;
            Instant to = query.containsKey("to") ? Instant.parse(query.get("to")) : null;
            if (from != null && to != null && from.isAfter(to)) throw new ExamException(400, 40001, "invalid time range");
        } catch (DateTimeParseException ex) {
            throw new ExamException(400, 40001, "invalid time");
        }
    }

    private void validateReason(Map<String, Object> body) {
        validateRequiredString(body, "reason", 1, 200);
    }

    private void validateIdempotencyKey(Map<String, Object> body) {
        if (!body.containsKey("idempotencyKey")) return;
        String value = string(body.get("idempotencyKey"));
        if (value == null || value.length() < 8 || value.length() > 80) throw new ExamException(400, 40001, "invalid idempotency key");
    }

    private String validateEnum(Map<String, Object> body, String field, Set<String> allowed) {
        String value = validateRequiredString(body, field, 1, 120);
        if (!allowed.contains(value)) throw new ExamException(400, 40001, "invalid " + field);
        return value;
    }

    private String validateRequiredString(Map<String, Object> body, String field, int min, int max) {
        String value = string(body.get(field));
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) throw new ExamException(400, 40001, "invalid " + field);
        return value;
    }

    private int intValue(Object value, int min, int max, int code) {
        if (!(value instanceof Number number)) throw new ExamException(code == 40001 ? 400 : 409, code, "invalid number");
        int integer = number.intValue();
        if (integer < min || integer > max) throw new ExamException(code == 40001 ? 400 : 409, code, "invalid number");
        return integer;
    }

    private List<Map<String, Object>> answerList(Map<String, Object> body) {
        return objectList(body, "answers");
    }

    private List<Map<String, Object>> objectList(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof List<?> list)) throw new ExamException(400, 40001, "invalid " + field);
        return list.stream().map(this::map).toList();
    }

    private List<String> stringList(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof List<?> list)) return List.of();
        return list.stream().map(Object::toString).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new ExamException(400, 40001, "invalid object");
    }

    private List<Map<String, Object>> copyList(List<Map<String, Object>> values) {
        return values.stream().map(LinkedHashMap::new).map(map -> (Map<String, Object>) map).toList();
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase();
    }

    static String string(Object value) {
        return value == null ? null : value.toString();
    }

    @SafeVarargs
    static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }
}

class TestExamAuthProvider {
    ExamUser requireUser(String authorization, HttpServletRequest request) {
        if (AdmissionTrustedActor.hasGatewayContext(request)) {
            return trustedActor(request);
        }
        return requireUser(authorization);
    }

    ExamUser requireUser(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new ExamException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new ExamException(401, 41003, "bad token format");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new ExamException(502, 46900, "auth unavailable");
            case "auth-timeout-token" -> throw new ExamException(504, 46901, "auth timeout");
            case "auth-bad-token" -> throw new ExamException(502, 46902, "auth incompatible");
            case "owner-token" -> new ExamUser("owner", "Owner", Set.of("OWNER"), "ACTIVE", minecraft("OwnerMc", "uuid-owner"), null);
            case "admin-token" -> new ExamUser("admin", "Admin", Set.of("ADMIN"), "ACTIVE", minecraft("AdminMc", "uuid-admin"), null);
            case "helper-token" -> new ExamUser("helper", "Helper", Set.of("HELPER"), "ACTIVE", minecraft("HelperMc", "uuid-helper"), null);
            case "user-token" -> new ExamUser("user", "User", Set.of("USER"), "ACTIVE", minecraft("UserSteve", "uuid-user"), null);
            case "ready-token" -> new ExamUser("seed-ready", "Ready Player", Set.of("USER"), "ACTIVE", minecraft("ReadySteve", "uuid-ready"), null);
            case "active-member-token" -> new ExamUser("active-member", "Active Member", Set.of("USER"), "ACTIVE", minecraft("ActiveSteve", "uuid-active"), "ACTIVE");
            case "blocked-token" -> new ExamUser("seed-blocked", "Blocked Player", Set.of("USER"), "ACTIVE", minecraft("BlockedSteve", "uuid-blocked"), null);
            case "content-unavailable-token" -> new ExamUser("content-unavailable", "Content Down", Set.of("USER"), "ACTIVE", minecraft("ContentSteve", "uuid-content"), "CONTENT_UNAVAILABLE");
            case "general-token" -> new ExamUser("general-user", "General User", Set.of("USER"), "ACTIVE", minecraft("GeneralSteve", "uuid-general"), null);
            case "supplement-token" -> new ExamUser("supplement-user", "Supplement User", Set.of("USER"), "ACTIVE", minecraft("SupplementSteve", "uuid-supplement"), null);
            case "cancel-token" -> new ExamUser("cancel-user", "Cancel User", Set.of("USER"), "ACTIVE", minecraft("CancelSteve", "uuid-cancel"), null);
            default -> throw new ExamException(401, 41001, "invalid session");
        };
    }

    ExamUser requireAny(String authorization, HttpServletRequest request, String... roles) {
        ExamUser user = requireUser(authorization, request);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new ExamException(403, 42001, "role permission denied");
        return user;
    }

    ExamUser requireAny(String authorization, String... roles) {
        ExamUser user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new ExamException(403, 42001, "role permission denied");
        return user;
    }

    private ExamUser trustedActor(HttpServletRequest request) {
        try {
            AdmissionTrustedActor.Actor actor = AdmissionTrustedActor.parse(request);
            return new ExamUser(actor.userId(), actor.displayName(), actor.roles(), "ACTIVE", actor.minecraftBinding(), null);
        } catch (IllegalArgumentException exception) {
            throw new ExamException(502, 46902, "auth incompatible");
        }
    }

    private static Map<String, Object> minecraft(String id, String uuid) {
        return ExamStore.mapOf("minecraftId", id, "minecraftUuid", uuid, "verifiedAt", "2026-05-23T12:00:00Z", "source", "MANUAL_VERIFICATION");
    }
}

interface ExamFlowEvidenceRecorder {
    void recordSessionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordAnswerWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordQuestionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);
}

class NoopExamFlowEvidenceRecorder implements ExamFlowEvidenceRecorder {
    @Override
    public void recordSessionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordAnswerWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordQuestionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }
}

record ExamUser(String userId, String displayName, Set<String> roles, String status, Map<String, Object> minecraftBinding, String profileStatus) {
}

record Handoff(String applicationId, String userId, String reviewDirection) {
}

record IdempotencyRecord(String fingerprint, Map<String, Object> value) {
}

record MutationResult(boolean created, Map<String, Object> value) {
}

record ExamTestControls(boolean enabled) {
}

class ExamSessionRecord {
    String sessionId;
    String applicationId;
    int onboardingHandoffVersion;
    String userId;
    String displayName;
    Map<String, Object> minecraftBinding;
    String reviewDirection;
    String attemptType;
    String difficulty;
    String status;
    String result;
    String templateId;
    int templateVersion;
    String paperId;
    List<ExamQuestionRecord> questions = List.of();
    List<Map<String, Object>> answers = List.of();
    int timeLimitMinutes;
    int passScore;
    int objectivePassScore;
    int objectiveScore;
    Integer manualScore;
    Integer totalScore;
    boolean objectivePassed;
    Boolean finalPassed;
    boolean manualRequired;
    Map<String, Object> manualReview;
    Map<String, Object> supplementRequest;
    List<String> supplementQuestionIds = List.of();
    String notificationStatus;
    String startedAt;
    String lastSavedAt;
    String submittedAt;
    String reviewedAt;
    String expiresAt;
    String passedAt;
    String cancelledAt;
    String createdAt;
    String updatedAt;
}

class ExamQuestionRecord {
    String questionId;
    int version;
    String type;
    String reviewDirection;
    String difficulty;
    String stem;
    List<Map<String, Object>> options = List.of();
    List<String> correctOptionIds = List.of();
    String referenceAnswer;
    int score;
    List<String> tags = List.of();
    String status;
    String createdAt;
    String updatedAt;
    String archivedAt;
}

class PaperTemplateRecord {
    String templateId;
    int version;
    String name;
    String reviewDirection;
    String difficulty;
    String status;
    int timeLimitMinutes;
    int passScore;
    int objectivePassScore;
    List<Map<String, Object>> questionRules = List.of();
    String contentRuleVersion;
    int retakeCooldownHours;
    String createdAt;
    String updatedAt;
    String publishedAt;
}

class ExamException extends RuntimeException {
    final int httpStatus;
    final int code;

    ExamException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

@RestControllerAdvice(basePackages = "cn.beiming.exam")
class ExamExceptionHandler {
    @ExceptionHandler(ExamException.class)
    ResponseEntity<Map<String, Object>> handle(ExamException ex) {
        return ResponseEntity.status(ex.httpStatus).body(error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(error(40400, "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(error(51900, "exam internal error"));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", ExamController.requestId());
        return body;
    }
}

class ExamRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@Configuration
class ExamRequestIdFilterConfig {
    @Bean
    ExamRequestIdFilter examRequestIdFilter() {
        return new ExamRequestIdFilter();
    }
}
