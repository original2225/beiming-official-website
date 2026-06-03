package cn.beiming.whitelist;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
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
class WhitelistModule {
    @Bean
    WhitelistStore whitelistStore() {
        return new WhitelistStore();
    }

    @Bean
    TestWhitelistAuthProvider whitelistAuthProvider() {
        return new TestWhitelistAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/whitelist")
class WhitelistController {
    private final WhitelistStore store;
    private final TestWhitelistAuthProvider auth;

    WhitelistController(WhitelistStore store, TestWhitelistAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @PostMapping("/me/applications")
    ResponseEntity<Map<String, Object>> createApplication(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                          @RequestBody(required = false) Map<String, Object> body,
                                                          HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        MutationResult result = store.createApplication(user, bodyOrEmpty(body), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(okBody(result.value()));
    }

    @GetMapping("/me/applications/current")
    Map<String, Object> currentApplication(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.currentApplication(user, request));
    }

    @GetMapping("/me/applications")
    Map<String, Object> myApplications(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestParam Map<String, String> query,
                                       HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.myApplications(user, query, request));
    }

    @GetMapping("/me/applications/{applicationId}")
    Map<String, Object> myApplication(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String applicationId,
                                      HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.myApplication(user, applicationId, request));
    }

    @PatchMapping("/me/applications/{applicationId}")
    Map<String, Object> updateMaterials(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String applicationId,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.updateMaterials(user, applicationId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/applications/{applicationId}/submit")
    Map<String, Object> submit(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String applicationId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.submit(user, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/applications/{applicationId}/supplement")
    Map<String, Object> supplement(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String applicationId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.supplement(user, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/applications/{applicationId}/withdraw")
    Map<String, Object> withdraw(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String applicationId,
                                 @RequestBody(required = false) Map<String, Object> body,
                                 HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.withdraw(user, applicationId, bodyOrEmpty(body), request));
    }

    @GetMapping("/me/applications/{applicationId}/result")
    Map<String, Object> myResult(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String applicationId,
                                 HttpServletRequest request) {
        WhitelistUser user = auth.requireUser(authorization);
        return ok(store.myResult(user, applicationId, request));
    }

    @GetMapping("/admin/applications")
    Map<String, Object> adminApplications(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestParam Map<String, String> query,
                                          HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminApplications(actor, query, request));
    }

    @GetMapping("/admin/applications/{applicationId}")
    Map<String, Object> adminApplication(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String applicationId,
                                         HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminApplication(actor, applicationId, request));
    }

    @PatchMapping("/admin/applications/{applicationId}/assign")
    Map<String, Object> assign(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String applicationId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.assign(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/request-supplement")
    Map<String, Object> requestSupplement(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String applicationId,
                                          @RequestBody(required = false) Map<String, Object> body,
                                          HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.requestSupplement(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/approve")
    Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String applicationId,
                                @RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.approve(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/reject")
    Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String applicationId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.reject(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/remove")
    Map<String, Object> remove(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String applicationId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.remove(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/applications/{applicationId}/reopen")
    Map<String, Object> reopen(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String applicationId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.reopen(actor, applicationId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/applications/{applicationId}/attendance-handoff")
    Map<String, Object> attendanceHandoff(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String applicationId,
                                          HttpServletRequest request) {
        WhitelistUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.attendanceHandoff(actor, applicationId, request));
    }

    @GetMapping("/admin/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(query, request));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest request) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
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

class WhitelistStore {
    private static final String NOW = "2026-05-23T12:00:00Z";
    private static final Set<String> APPLICATION_STATUSES = Set.of("DRAFT", "PENDING_REVIEW", "UNDER_REVIEW", "NEEDS_SUPPLEMENT", "SUPPLEMENT_SUBMITTED", "APPROVAL_BLOCKED", "APPROVED", "REJECTED", "WITHDRAWN", "REMOVED", "REAPPLYING", "ARCHIVED");
    private static final Set<String> RESULTS = Set.of("PENDING", "NEEDS_SUPPLEMENT", "APPROVED", "REJECTED", "WITHDRAWN", "REMOVED");
    private static final Set<String> DIRECTIONS = Set.of("REDSTONE", "LATE_GAME", "BUILDING", "GENERAL");
    private static final Set<String> ATTEMPT_TYPES = Set.of("FIRST_TIME", "RECHECK");
    private final Map<String, WhitelistApplicationRecord> applications = new ConcurrentHashMap<>();
    private final Map<String, String> currentByUser = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Set<String> consumedHandoffs = ConcurrentHashMap.newKeySet();
    private final List<Map<String, Object>> audits = Collections.synchronizedList(new ArrayList<>());
    private int idSeq = 1000;
    private int attendanceHandoffReads;

    synchronized MutationResult createApplication(WhitelistUser user, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord existing = replay(user.userId(), "create", body);
        if (existing != null) return new MutationResult(false, existing.value());
        failBeforeWrite(request);
        String sessionId = validateRequiredString(body, "examSessionId", 1, 80);
        Handoff handoff = handoff(sessionId, user);
        if (!"PASSED".equals(handoff.result())) throw new WhitelistException(409, 44010, "exam result not passed");
        if (!Objects.equals(handoff.userId(), user.userId())) throw new WhitelistException(409, 44011, "handoff user mismatch");
        if (handoff.minecraftBinding() == null || handoff.minecraftBinding().get("minecraftUuid") == null) throw new WhitelistException(502, 47012, "exam handoff incompatible");
        String handoffKey = handoff.sessionId() + ":" + handoff.handoffVersion();
        String currentId = currentByUser.get(user.userId());
        if (currentId != null) {
            WhitelistApplicationRecord current = applications.get(currentId);
            if (current != null && Set.of("REMOVED", "REAPPLYING").contains(current.status) && !"RECHECK".equals(handoff.attemptType())) {
                throw new WhitelistException(409, 44019, "recheck handoff required");
            }
            if (current != null && !Set.of("WITHDRAWN", "REJECTED", "REMOVED", "ARCHIVED", "REAPPLYING").contains(current.status)) {
                Map<String, Object> value = view(current, false);
                remember(user.userId(), "create", body, value);
                return new MutationResult(false, value);
            }
        }
        if (consumedHandoffs.contains(handoffKey)) throw new WhitelistException(409, 44012, "handoff consumed");
        WhitelistApplicationRecord app = new WhitelistApplicationRecord();
        app.applicationId = "wl-" + (++idSeq);
        app.examSessionId = handoff.sessionId();
        app.onboardingApplicationId = handoff.applicationId();
        app.examHandoffVersion = handoff.handoffVersion();
        app.onboardingHandoffVersion = handoff.onboardingHandoffVersion();
        app.userId = handoff.userId();
        app.displayNameSnapshot = user.displayName();
        app.minecraftBindingSnapshot = handoff.minecraftBinding();
        app.reviewDirection = handoff.reviewDirection();
        app.attemptType = handoff.attemptType();
        app.status = "PENDING_REVIEW";
        app.result = "PENDING";
        app.materials = normalizeMaterials(body.get("materials"), false);
        app.scoreSummary = handoff.scoreSummary();
        app.examPassedAt = handoff.passedAt();
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.reapplyRequired = false;
        app.createdAt = NOW;
        app.updatedAt = NOW;
        app.submittedAt = NOW;
        consumedHandoffs.add(handoffKey);
        applications.put(app.applicationId, app);
        currentByUser.put(app.userId, app.applicationId);
        audit(user, app.applicationId, "WHITELIST_APPLICATION_CREATED", "LOW", null, app.status, "create application");
        auditNotificationFailure(user, app);
        Map<String, Object> value = view(app, false);
        remember(user.userId(), "create", body, value);
        return new MutationResult(true, value);
    }

    Map<String, Object> currentApplication(WhitelistUser user, HttpServletRequest request) {
        String id = currentByUser.get(user.userId());
        if (id == null) return null;
        WhitelistApplicationRecord app = applications.get(id);
        if (app == null || "ARCHIVED".equals(app.status)) return null;
        return view(app, false);
    }

    Map<String, Object> myApplications(WhitelistUser user, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", APPLICATION_STATUSES);
        String result = enumQuery(query, "result", RESULTS);
        String sort = sort(query, Set.of("createdAt_desc", "updatedAt_desc", "reviewedAt_desc", "approvedAt_desc"), "createdAt_desc");
        List<Map<String, Object>> rows = applications.values().stream()
                .filter(app -> app.userId.equals(user.userId()))
                .filter(app -> status == null || status.equals(app.status))
                .filter(app -> result == null || result.equals(app.result))
                .map(app -> view(app, false))
                .sorted(applicationComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> myApplication(WhitelistUser user, String applicationId, HttpServletRequest request) {
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        return view(app, false);
    }

    synchronized Map<String, Object> updateMaterials(WhitelistUser user, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        IdempotencyRecord existing = replay(user.userId(), "materials:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("DRAFT", "PENDING_REVIEW", "NEEDS_SUPPLEMENT", "SUPPLEMENT_SUBMITTED").contains(app.status)) throw new WhitelistException(409, 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.materials = normalizeMaterials(body.get("materials"), false);
        app.updatedAt = NOW;
        audit(user, app.applicationId, "WHITELIST_MATERIALS_UPDATED", "LOW", before, app.status, "materials updated");
        Map<String, Object> value = view(app, false);
        remember(user.userId(), "materials:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> submit(WhitelistUser user, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        IdempotencyRecord existing = replay(user.userId(), "submit:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("DRAFT", "PENDING_REVIEW").contains(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "PENDING_REVIEW";
        app.result = "PENDING";
        if (app.submittedAt == null) app.submittedAt = NOW;
        app.updatedAt = NOW;
        audit(user, app.applicationId, "WHITELIST_APPLICATION_SUBMITTED", "LOW", before, app.status, "submit");
        Map<String, Object> value = view(app, false);
        remember(user.userId(), "submit:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> supplement(WhitelistUser user, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        IdempotencyRecord existing = replay(user.userId(), "supplement:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!"NEEDS_SUPPLEMENT".equals(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        List<Map<String, Object>> materials = normalizeMaterials(body.get("materials"), true);
        app.status = "SUPPLEMENT_SUBMITTED";
        app.result = "PENDING";
        app.materials = materials;
        Map<String, Object> requestSummary = linkedMap("requestId", app.supplementRequest == null ? "supp-" + app.applicationId : app.supplementRequest.get("requestId"), "publicComment", app.supplementRequest == null ? "补充材料" : app.supplementRequest.get("publicComment"), "dueAt", app.supplementRequest == null ? null : app.supplementRequest.get("dueAt"), "requestedBy", app.supplementRequest == null ? app.reviewerUserId : app.supplementRequest.get("requestedBy"), "requestedAt", app.supplementRequest == null ? NOW : app.supplementRequest.get("requestedAt"), "submittedAt", NOW, "materials", materials);
        app.supplementRequest = requestSummary;
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.updatedAt = NOW;
        audit(user, app.applicationId, "WHITELIST_SUPPLEMENT_SUBMITTED", "LOW", before, app.status, "supplement");
        auditNotificationFailure(user, app);
        Map<String, Object> value = view(app, false);
        remember(user.userId(), "supplement:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> withdraw(WhitelistUser user, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        IdempotencyRecord existing = replay(user.userId(), "withdraw:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("DRAFT", "PENDING_REVIEW", "NEEDS_SUPPLEMENT", "SUPPLEMENT_SUBMITTED").contains(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "WITHDRAWN";
        app.result = "WITHDRAWN";
        app.withdrawnAt = NOW;
        app.updatedAt = NOW;
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        audit(user, app.applicationId, "WHITELIST_APPLICATION_WITHDRAWN", "MEDIUM", before, app.status, "withdraw");
        auditNotificationFailure(user, app);
        Map<String, Object> value = view(app, false);
        remember(user.userId(), "withdraw:" + applicationId, body, value);
        return value;
    }

    Map<String, Object> myResult(WhitelistUser user, String applicationId, HttpServletRequest request) {
        WhitelistApplicationRecord app = requireOwned(user, applicationId);
        return resultView(app);
    }

    Map<String, Object> adminApplications(WhitelistUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", APPLICATION_STATUSES);
        String result = enumQuery(query, "result", RESULTS);
        String direction = enumQuery(query, "reviewDirection", DIRECTIONS);
        String attemptType = enumQuery(query, "attemptType", ATTEMPT_TYPES);
        String reviewer = query.get("reviewerUserId");
        String keyword = lower(query.get("keyword"));
        String sort = sort(query, Set.of("createdAt_desc", "updatedAt_desc", "submittedAt_desc", "reviewedAt_desc", "approvedAt_desc"), "createdAt_desc");
        List<Map<String, Object>> rows = applications.values().stream()
                .filter(app -> status == null || status.equals(app.status))
                .filter(app -> result == null || result.equals(app.result))
                .filter(app -> direction == null || direction.equals(app.reviewDirection))
                .filter(app -> attemptType == null || attemptType.equals(app.attemptType))
                .filter(app -> reviewer == null || reviewer.equals(app.reviewerUserId))
                .filter(app -> keyword == null || matches(app, keyword))
                .map(app -> view(app, true))
                .sorted(applicationComparator(sort))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminApplication(WhitelistUser actor, String applicationId, HttpServletRequest request) {
        return view(require(applicationId), true);
    }

    synchronized Map<String, Object> assign(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "assign:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("PENDING_REVIEW", "SUPPLEMENT_SUBMITTED", "UNDER_REVIEW").contains(app.status)) throw new WhitelistException(409, 44014, "state conflict");
        String reviewer = string(body.get("reviewerUserId"));
        if (reviewer == null || reviewer.isBlank()) reviewer = actor.userId();
        if (actor.roles().contains("HELPER") && !reviewer.equals(actor.userId())) throw new WhitelistException(403, 42001, "role permission denied");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "UNDER_REVIEW";
        app.reviewerUserId = reviewer;
        app.reviewerDisplayNameSnapshot = reviewer.equals(actor.userId()) ? actor.displayName() : "Reviewer " + reviewer;
        app.updatedAt = NOW;
        audit(actor, app.applicationId, "WHITELIST_REVIEW_ASSIGNED", "LOW", before, app.status, "assign");
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "assign:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> requestSupplement(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        String comment = validateRequiredString(body, "publicComment", 1, 1000);
        if (body.containsKey("dueAt")) validateFutureDueAt(string(body.get("dueAt")));
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "requestSupplement:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("PENDING_REVIEW", "UNDER_REVIEW", "SUPPLEMENT_SUBMITTED").contains(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "NEEDS_SUPPLEMENT";
        app.result = "NEEDS_SUPPLEMENT";
        app.reviewComment = comment;
        app.internalNote = string(body.get("internalNote"));
        app.supplementRequest = linkedMap("requestId", "supp-" + (++idSeq), "publicComment", comment, "dueAt", body.getOrDefault("dueAt", null), "requestedBy", actor.userId(), "requestedAt", NOW, "submittedAt", null, "materials", List.of());
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.reviewedAt = NOW;
        app.updatedAt = NOW;
        audit(actor, app.applicationId, "WHITELIST_SUPPLEMENT_REQUESTED", "MEDIUM", before, app.status, "request supplement");
        auditNotificationFailure(actor, app);
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "requestSupplement:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> approve(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        String comment = validateRequiredString(body, "reviewComment", 1, 1000);
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "approve:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("PENDING_REVIEW", "UNDER_REVIEW", "SUPPLEMENT_SUBMITTED", "APPROVAL_BLOCKED").contains(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        failProfile(request);
        String before = app.status;
        app.reviewerUserId = actor.userId();
        app.reviewerDisplayNameSnapshot = actor.displayName();
        app.reviewComment = comment;
        app.internalNote = string(body.get("internalNote"));
        app.reviewedAt = NOW;
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        if (app.userId.contains("profile-fail")) {
            app.status = "APPROVAL_BLOCKED";
            app.result = "PENDING";
            app.profileActivation = linkedMap("status", "FAILED", "memberId", null, "profileStatus", null, "calledAt", NOW, "failureCode", "44020", "failureReason", "profile activation conflict");
            audit(actor, app.applicationId, "WHITELIST_PROFILE_ACTIVATION_FAILED", "MEDIUM", before, app.status, "profile failed");
        } else if ("true".equals(request.getHeader("X-Test-Fail-After-Profile"))) {
            app.status = "APPROVAL_BLOCKED";
            app.result = "PENDING";
            app.profileActivation = linkedMap("status", "ACTIVATED", "memberId", "member-" + app.userId, "profileStatus", "ACTIVE", "calledAt", NOW, "failureCode", "52003", "failureReason", "whitelist state confirmation failed");
            app.updatedAt = NOW;
            audit(actor, app.applicationId, "WHITELIST_PROFILE_ACTIVATED", "MEDIUM", before, app.status, "profile activated");
            audit(actor, app.applicationId, "WHITELIST_DOWNSTREAM_COMPENSATION_REQUIRED", "HIGH", before, app.status, "state confirmation failed");
            throw new WhitelistException(500, 52003, "whitelist downstream compensation required");
        } else {
            app.status = "APPROVED";
            app.result = "APPROVED";
            app.approvedAt = NOW;
            app.profileActivation = linkedMap("status", "ACTIVATED", "memberId", "member-" + app.userId, "profileStatus", "ACTIVE", "calledAt", NOW, "failureCode", null, "failureReason", null);
            app.attendanceHandoff = handoffView(app);
            audit(actor, app.applicationId, "WHITELIST_APPROVED", "MEDIUM", before, app.status, "approve");
            audit(actor, app.applicationId, "WHITELIST_PROFILE_ACTIVATED", "MEDIUM", before, app.status, "profile activated");
        }
        app.updatedAt = NOW;
        auditNotificationFailure(actor, app);
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "approve:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> reject(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        String comment = validateRequiredString(body, "reviewComment", 1, 1000);
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "reject:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("PENDING_REVIEW", "UNDER_REVIEW", "SUPPLEMENT_SUBMITTED", "APPROVAL_BLOCKED").contains(app.status)) throw new WhitelistException(409, Set.of("APPROVED", "REMOVED").contains(app.status) ? 44018 : 44014, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "REJECTED";
        app.result = "REJECTED";
        app.reviewComment = comment;
        app.internalNote = string(body.get("internalNote"));
        app.reviewerUserId = actor.userId();
        app.reviewerDisplayNameSnapshot = actor.displayName();
        app.reviewedAt = NOW;
        app.rejectedAt = NOW;
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.updatedAt = NOW;
        audit(actor, app.applicationId, "WHITELIST_REJECTED", "MEDIUM", before, app.status, "reject");
        auditNotificationFailure(actor, app);
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "reject:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> remove(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        validateRequiredString(body, "publicComment", 1, 1000);
        if (!"REMOVE_WHITELIST".equals(string(body.get("confirmText")))) throw new WhitelistException(403, 42003, "high risk operation not confirmed");
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "remove:" + applicationId, body);
        if (existing != null) return existing.value();
        if ("REMOVED".equals(app.status)) return view(app, true);
        if (!"APPROVED".equals(app.status)) throw new WhitelistException(409, 44014, "state conflict");
        failBeforeWrite(request);
        failProfile(request);
        String before = app.status;
        app.status = "REMOVED";
        app.result = "REMOVED";
        app.removedAt = NOW;
        app.removedBy = actor.userId();
        app.removalReason = validateRequiredString(body, "reason", 1, 200);
        app.reapplyRequired = true;
        app.nextExamAttemptType = "RECHECK";
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.updatedAt = NOW;
        audit(actor, app.applicationId, "WHITELIST_REMOVED", "HIGH", before, app.status, "remove");
        auditNotificationFailure(actor, app);
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "remove:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> reopen(WhitelistUser actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body);
        validateRequiredString(body, "publicComment", 1, 1000);
        WhitelistApplicationRecord app = require(applicationId);
        IdempotencyRecord existing = replay(actor.userId(), "reopen:" + applicationId, body);
        if (existing != null) return existing.value();
        if (!Set.of("REJECTED", "WITHDRAWN", "REMOVED").contains(app.status)) throw new WhitelistException(409, 44022, "state conflict");
        failBeforeWrite(request);
        String before = app.status;
        app.status = "REAPPLYING";
        app.result = "PENDING";
        app.reapplyRequired = true;
        if (app.nextExamAttemptType == null) app.nextExamAttemptType = "RECHECK";
        app.reviewComment = validateRequiredString(body, "publicComment", 1, 1000);
        app.notificationStatus = notificationStatus(request);
        app.notificationFailure = notificationFailure(request);
        app.updatedAt = NOW;
        audit(actor, app.applicationId, "WHITELIST_REOPENED", "MEDIUM", before, app.status, "reopen");
        auditNotificationFailure(actor, app);
        Map<String, Object> value = view(app, true);
        remember(actor.userId(), "reopen:" + applicationId, body, value);
        return value;
    }

    synchronized Map<String, Object> attendanceHandoff(WhitelistUser actor, String applicationId, HttpServletRequest request) {
        WhitelistApplicationRecord app = require(applicationId);
        if (!"APPROVED".equals(app.status)) throw new WhitelistException(409, 44014, "state conflict");
        if (app.profileActivation == null || !"ACTIVATED".equals(app.profileActivation.get("status"))) throw new WhitelistException(409, 44014, "state conflict");
        if (app.attendanceHandoff == null) throw new WhitelistException(409, 44021, "handoff missing");
        attendanceHandoffReads++;
        audit(actor, app.applicationId, "WHITELIST_ATTENDANCE_HANDOFF_READ", "LOW", app.status, app.status, "read handoff");
        return app.attendanceHandoff;
    }

    Map<String, Object> auditLogs(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String result = enumQuery(query, "result", Set.of("SUCCESS", "FAILED"));
        validateTimeRange(query);
        String applicationId = query.get("applicationId");
        String actor = query.get("actorUserId");
        String action = query.get("action");
        String sort = sort(query, Set.of("createdAt_desc", "createdAt_asc"), "createdAt_desc");
        Comparator<Map<String, Object>> comparator = Comparator.comparing(row -> Objects.toString(row.get("createdAt")));
        if ("createdAt_desc".equals(sort)) comparator = comparator.reversed();
        List<Map<String, Object>> rows = audits.stream()
                .filter(row -> applicationId == null || applicationId.equals(row.get("targetId")))
                .filter(row -> actor == null || actor.equals(row.get("actorUserId")))
                .filter(row -> action == null || action.equals(row.get("action")))
                .filter(row -> result == null || result.equals(row.get("result")))
                .sorted(comparator)
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> opsSummary(HttpServletRequest request) {
        long pending = applications.values().stream().filter(app -> "PENDING_REVIEW".equals(app.status)).count();
        long approved = applications.values().stream().filter(app -> "APPROVED".equals(app.status)).count();
        long rejected = applications.values().stream().filter(app -> "REJECTED".equals(app.status)).count();
        long removed = applications.values().stream().filter(app -> "REMOVED".equals(app.status)).count();
        long blocked = applications.values().stream().filter(app -> "APPROVAL_BLOCKED".equals(app.status)).count();
        long handoffs = applications.values().stream().filter(app -> app.attendanceHandoff != null).count();
        return linkedMap("service", "whitelist", "port", 8110, "storageMode", "IN_MEMORY", "authMode", "TEST_STUB", "examMode", "TEST_STUB", "profileMode", "TEST_STUB", "notificationMode", "TEST_STUB", "applicationsTotal", applications.size(), "pendingReviewTotal", pending, "approvedTotal", approved, "rejectedTotal", rejected, "removedTotal", removed, "approvalBlockedTotal", blocked, "attendanceHandoffsTotal", handoffs, "auditsTotal", audits.size(), "idempotencyRecordsTotal", idempotency.size(), "lastAuditAt", audits.isEmpty() ? null : NOW, "productionGaps", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB", "P0_EXAM_STUB", "P0_PROFILE_STUB", "P0_NOTIFICATION_STUB", "ATTENDANCE_NOT_IMPLEMENTED", "REAL_SERVER_WHITELIST_NOT_CONNECTED"));
    }

    private Handoff handoff(String sessionId, WhitelistUser user) {
        if ("EXAM_UNAVAILABLE".equals(user.mode())) throw new WhitelistException(502, 47010, "exam unavailable");
        return switch (sessionId) {
            case "session-passed" -> new Handoff(sessionId, "onb-user", 1, 1, "user", minecraft("UserSteve", "uuid-user"), "REDSTONE", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-user-mismatch" -> new Handoff(sessionId, "onb-other", 1, 1, "not-" + user.userId(), minecraft("MismatchSteve", "uuid-mismatch"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-failed" -> new Handoff(sessionId, "onb-user", 1, 1, user.userId(), minecraft("UserSteve", "uuid-user"), "REDSTONE", "FIRST_TIME", "FAILED", score(), null);
            case "session-approve" -> new Handoff(sessionId, "onb-approve", 1, 1, "approve-user", minecraft("ApproveSteve", "uuid-approve"), "BUILDING", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-reject" -> new Handoff(sessionId, "onb-reject", 1, 1, "reject-user", minecraft("RejectSteve", "uuid-reject"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-profile-fail" -> new Handoff(sessionId, "onb-profile-fail", 1, 1, "profile-fail-user", minecraft("ProfileFailSteve", "uuid-profile-fail"), "LATE_GAME", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-profile-unavailable" -> new Handoff(sessionId, "onb-profile-unavailable", 1, 1, "profile-unavailable-user", minecraft("ProfileDownSteve", "uuid-profile-down"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-profile-timeout" -> new Handoff(sessionId, "onb-profile-timeout", 1, 1, "profile-timeout-user", minecraft("ProfileTimeoutSteve", "uuid-profile-timeout"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-profile-bad-schema" -> new Handoff(sessionId, "onb-profile-bad-schema", 1, 1, "profile-bad-schema-user", minecraft("ProfileBadSteve", "uuid-profile-bad"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-profile-compensate" -> new Handoff(sessionId, "onb-profile-compensate", 1, 1, "profile-compensate-user", minecraft("ProfileCompensateSteve", "uuid-profile-compensate"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-remove-profile-fail" -> new Handoff(sessionId, "onb-remove-profile-fail", 1, 1, "remove-downstream-user", minecraft("RemoveProfileFailSteve", "uuid-remove-profile-fail"), "GENERAL", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-reapply-first" -> new Handoff(sessionId, "onb-reapply-first", 1, 1, "reapply-user", minecraft("ReapplySteve", "uuid-reapply"), "REDSTONE", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-reapply-first-again" -> new Handoff(sessionId, "onb-reapply-first-again", 1, 1, "reapply-user", minecraft("ReapplySteve", "uuid-reapply"), "REDSTONE", "FIRST_TIME", "PASSED", score(), NOW);
            case "session-reapply-recheck" -> new Handoff(sessionId, "onb-reapply-recheck", 1, 1, "reapply-user", minecraft("ReapplySteve", "uuid-reapply"), "REDSTONE", "RECHECK", "PASSED", score(), NOW);
            default -> throw new WhitelistException(502, 47010, "exam handoff unavailable");
        };
    }

    private WhitelistApplicationRecord requireOwned(WhitelistUser user, String applicationId) {
        WhitelistApplicationRecord app = applications.get(applicationId);
        if (app == null || !app.userId.equals(user.userId())) throw new WhitelistException(404, 44000, "application not found");
        return app;
    }

    private WhitelistApplicationRecord require(String applicationId) {
        WhitelistApplicationRecord app = applications.get(applicationId);
        if (app == null) throw new WhitelistException(404, 44000, "application not found");
        return app;
    }

    private Map<String, Object> view(WhitelistApplicationRecord app, boolean admin) {
        Map<String, Object> row = linkedMap(
                "applicationId", app.applicationId,
                "examSessionId", app.examSessionId,
                "onboardingApplicationId", app.onboardingApplicationId,
                "examHandoffVersion", app.examHandoffVersion,
                "onboardingHandoffVersion", app.onboardingHandoffVersion,
                "userId", app.userId,
                "displayNameSnapshot", app.displayNameSnapshot,
                "minecraftBindingSnapshot", app.minecraftBindingSnapshot,
                "reviewDirection", app.reviewDirection,
                "attemptType", app.attemptType,
                "status", app.status,
                "result", app.result,
                "materials", app.materials,
                "scoreSummary", app.scoreSummary,
                "examPassedAt", app.examPassedAt,
                "reviewerUserId", app.reviewerUserId,
                "reviewerDisplayNameSnapshot", app.reviewerDisplayNameSnapshot,
                "reviewComment", app.reviewComment,
                "supplementRequest", app.supplementRequest,
                "profileActivation", app.profileActivation,
                "notificationStatus", app.notificationStatus,
                "notificationFailure", app.notificationFailure,
                "removedAt", app.removedAt,
                "removedBy", app.removedBy,
                "reapplyRequired", app.reapplyRequired,
                "nextExamAttemptType", app.nextExamAttemptType,
                "createdAt", app.createdAt,
                "updatedAt", app.updatedAt,
                "submittedAt", app.submittedAt,
                "reviewedAt", app.reviewedAt,
                "approvedAt", app.approvedAt,
                "rejectedAt", app.rejectedAt,
                "withdrawnAt", app.withdrawnAt,
                "archivedAt", app.archivedAt);
        if (admin) {
            row.put("internalNote", app.internalNote);
            row.put("removalReason", app.removalReason);
            row.put("attendanceHandoff", app.attendanceHandoff);
        } else {
            if (app.attendanceHandoff != null) row.put("attendanceInitializationStatus", app.attendanceHandoff.get("initializationStatus"));
        }
        return row;
    }

    private Map<String, Object> resultView(WhitelistApplicationRecord app) {
        return linkedMap("applicationId", app.applicationId, "status", app.status, "result", app.result, "reviewComment", app.reviewComment, "profileActivationStatus", app.profileActivation == null ? "PENDING" : app.profileActivation.get("status"), "attendanceInitializationStatus", app.attendanceHandoff == null ? null : app.attendanceHandoff.get("initializationStatus"), "notificationStatus", app.notificationStatus, "notificationFailure", app.notificationFailure, "reviewedAt", app.reviewedAt);
    }

    private Map<String, Object> handoffView(WhitelistApplicationRecord app) {
        return linkedMap("handoffId", "att-" + app.applicationId, "applicationId", app.applicationId, "userId", app.userId, "memberId", "member-" + app.userId, "minecraftBindingSnapshot", app.minecraftBindingSnapshot, "reviewDirection", app.reviewDirection, "attemptType", app.attemptType, "approvedAt", NOW, "scoreSummary", app.scoreSummary, "initializationStatus", "WAITING_MODULE", "handoffVersion", 1, "generatedAt", NOW, "consumedAt", null);
    }

    private List<Map<String, Object>> normalizeMaterials(Object value, boolean requireNotEmpty) {
        if (value == null) {
            if (requireNotEmpty) throw new WhitelistException(400, 40001, "invalid materials");
            return List.of();
        }
        if (!(value instanceof List<?> list)) throw new WhitelistException(400, 40001, "invalid materials");
        if (list.size() > 20 || (requireNotEmpty && list.isEmpty())) throw new WhitelistException(400, 40001, "invalid materials");
        List<Map<String, Object>> materials = new ArrayList<>();
        int index = 0;
        for (Object item : list) {
            Map<String, Object> input = map(item);
            String type = string(input.getOrDefault("type", "TEXT"));
            if (!Set.of("TEXT", "LINK", "IMAGE", "OTHER").contains(type)) throw new WhitelistException(400, 40001, "invalid material type");
            String title = validateRequiredString(input, "title", 2, 80);
            String content = validateRequiredString(input, "content", 1, 2000);
            if ("LINK".equals(type) && !(content.startsWith("http://") || content.startsWith("https://") || content.startsWith("/"))) throw new WhitelistException(400, 40001, "invalid link");
            materials.add(linkedMap("materialId", "mat-" + (++index), "type", type, "title", title, "content", content, "publicVisibleToApplicant", true, "createdAt", NOW, "updatedAt", NOW));
        }
        return materials;
    }

    private void audit(WhitelistUser actor, String applicationId, String action, String risk, String before, String after, String reason) {
        audits.add(linkedMap("id", "audit-" + (++idSeq), "requestId", WhitelistController.requestId(), "actorUserId", actor.userId(), "actorRole", actor.roles().iterator().next(), "actorPermissions", List.of(), "sourceIp", "127.0.0.1", "targetType", "WHITELIST_APPLICATION", "targetId", applicationId, "action", action, "riskLevel", risk, "reason", reason, "paramsSummary", "summary", "beforeState", before, "afterState", after, "result", "SUCCESS", "failureReason", null, "createdAt", NOW));
    }

    private void auditNotificationFailure(WhitelistUser actor, WhitelistApplicationRecord app) {
        if ("FAILED".equals(app.notificationStatus)) {
            String reason = app.notificationFailure == null ? "notification failed" : Objects.toString(app.notificationFailure.get("failureType")) + ":" + Objects.toString(app.notificationFailure.get("failureCode"));
            audit(actor, app.applicationId, "WHITELIST_NOTIFICATION_FAILED", "LOW", app.status, app.status, reason);
        }
    }

    private void failBeforeWrite(HttpServletRequest request) {
        if ("true".equals(request.getHeader("X-Test-Fail-Audit"))) throw new WhitelistException(500, 52001, "whitelist audit failed");
        if ("true".equals(request.getHeader("X-Test-Fail-Store"))) throw new WhitelistException(500, 52002, "whitelist state failed");
    }

    private void failProfile(HttpServletRequest request) {
        switch (Objects.toString(request.getHeader("X-Test-Profile-Mode"), "")) {
            case "unavailable" -> throw new WhitelistException(502, 47020, "profile unavailable");
            case "timeout" -> throw new WhitelistException(504, 47021, "profile timeout");
            case "bad-schema" -> throw new WhitelistException(502, 47022, "profile incompatible");
            default -> {
            }
        }
    }

    private String notificationStatus(HttpServletRequest request) {
        String mode = Objects.toString(request.getHeader("X-Test-Notification-Mode"), "");
        return Set.of("unavailable", "timeout", "bad-schema").contains(mode) ? "FAILED" : "DELIVERED";
    }

    private Map<String, Object> notificationFailure(HttpServletRequest request) {
        return switch (Objects.toString(request.getHeader("X-Test-Notification-Mode"), "")) {
            case "unavailable" -> linkedMap("status", "FAILED", "failureCode", "47030", "failureType", "UNAVAILABLE", "failureReason", "notification unavailable", "failedAt", NOW);
            case "timeout" -> linkedMap("status", "FAILED", "failureCode", "47031", "failureType", "TIMEOUT", "failureReason", "notification timeout", "failedAt", NOW);
            case "bad-schema" -> linkedMap("status", "FAILED", "failureCode", "47032", "failureType", "BAD_SCHEMA", "failureReason", "notification response incompatible", "failedAt", NOW);
            default -> null;
        };
    }

    private IdempotencyRecord replay(String actorId, String operation, Map<String, Object> body) {
        String key = idempotencyKey(body);
        if (key == null) return null;
        IdempotencyRecord existing = idempotency.get(actorId + ":" + operation + ":" + key);
        if (existing != null && !existing.fingerprint().equals(canonical(body))) throw new WhitelistException(409, 44017, "idempotency conflict");
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
        return linkedMap("items", new ArrayList<>(rows.subList(from, to)), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private Comparator<Map<String, Object>> applicationComparator(String sort) {
        return switch (sort) {
            case "updatedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("updatedAt"))).reversed();
            case "reviewedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("reviewedAt"))).reversed();
            case "approvedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("approvedAt"))).reversed();
            default -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("createdAt"))).reversed();
        };
    }

    private boolean matches(WhitelistApplicationRecord app, String keyword) {
        String mc = app.minecraftBindingSnapshot == null ? "" : Objects.toString(app.minecraftBindingSnapshot.get("minecraftId"), "");
        return app.applicationId.toLowerCase().contains(keyword) || app.userId.toLowerCase().contains(keyword) || app.displayNameSnapshot.toLowerCase().contains(keyword) || app.examSessionId.toLowerCase().contains(keyword) || mc.toLowerCase().contains(keyword);
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
            if (value < min || value > max) throw new WhitelistException(400, code, "invalid number");
            return value;
        } catch (NumberFormatException ex) {
            throw new WhitelistException(400, code, "invalid number");
        }
    }

    private String sort(Map<String, String> query, Set<String> allowed, String fallback) {
        String value = query.getOrDefault("sort", fallback);
        if (!allowed.contains(value)) throw new WhitelistException(400, 40003, "invalid sort");
        return value;
    }

    private String enumQuery(Map<String, String> query, String key, Set<String> allowed) {
        if (!query.containsKey(key)) return null;
        String value = query.get(key);
        if (!allowed.contains(value)) throw new WhitelistException(400, 40001, "invalid " + key);
        return value;
    }

    private void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.containsKey("from") ? Instant.parse(query.get("from")) : null;
            Instant to = query.containsKey("to") ? Instant.parse(query.get("to")) : null;
            if (from != null && to != null && from.isAfter(to)) throw new WhitelistException(400, 40001, "invalid time range");
        } catch (DateTimeParseException ex) {
            throw new WhitelistException(400, 40001, "invalid time");
        }
    }

    private void validateFutureDueAt(String dueAt) {
        try {
            if (dueAt == null) throw new WhitelistException(400, 40001, "invalid dueAt");
            Instant value = Instant.parse(dueAt);
            Instant now = Instant.parse(NOW);
            if (!value.isAfter(now) || value.isAfter(now.plus(Duration.ofDays(14)))) throw new WhitelistException(400, 40001, "invalid dueAt");
        } catch (DateTimeParseException ex) {
            throw new WhitelistException(400, 40001, "invalid dueAt");
        }
    }

    private void validateReason(Map<String, Object> body) {
        validateRequiredString(body, "reason", 1, 200);
    }

    private void validateIdempotencyKey(Map<String, Object> body) {
        if (!body.containsKey("idempotencyKey")) return;
        String value = string(body.get("idempotencyKey"));
        if (value == null || value.length() < 8 || value.length() > 80) throw new WhitelistException(400, 40001, "invalid idempotency key");
    }

    private static String validateRequiredString(Map<String, Object> body, String field, int min, int max) {
        String value = string(body.get(field));
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) throw new WhitelistException(400, 40001, "invalid " + field);
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new WhitelistException(400, 40001, "invalid object");
    }

    private static Map<String, Object> minecraft(String id, String uuid) {
        return linkedMap("minecraftId", id, "minecraftUuid", uuid, "verifiedAt", NOW, "source", "MANUAL_VERIFICATION");
    }

    private static Map<String, Object> score() {
        return linkedMap("objectiveScore", 20, "manualScore", 30, "totalScore", 50, "objectivePassed", true, "finalPassed", true, "passScore", 50, "objectivePassScore", 20, "manualRequired", true, "scoredAt", NOW);
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase();
    }

    static String string(Object value) {
        return value == null ? null : value.toString();
    }

    static Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }
}

class TestWhitelistAuthProvider {
    WhitelistUser requireUser(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new WhitelistException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new WhitelistException(401, 41003, "bad token format");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new WhitelistException(502, 47000, "auth unavailable");
            case "auth-timeout-token" -> throw new WhitelistException(504, 47001, "auth timeout");
            case "auth-bad-token" -> throw new WhitelistException(502, 47002, "auth incompatible");
            case "owner-token" -> new WhitelistUser("owner", "Owner", Set.of("OWNER"), "ACTIVE", "NORMAL");
            case "admin-token" -> new WhitelistUser("admin", "Admin", Set.of("ADMIN"), "ACTIVE", "NORMAL");
            case "helper-token" -> new WhitelistUser("helper", "Helper", Set.of("HELPER"), "ACTIVE", "NORMAL");
            case "user-token" -> new WhitelistUser("user", "User", Set.of("USER"), "ACTIVE", "NORMAL");
            case "other-token" -> new WhitelistUser("other", "Other", Set.of("USER"), "ACTIVE", "NORMAL");
            case "exam-unavailable-token" -> new WhitelistUser("user", "User", Set.of("USER"), "ACTIVE", "EXAM_UNAVAILABLE");
            case "approve-user-token" -> new WhitelistUser("approve-user", "Approve User", Set.of("USER"), "ACTIVE", "NORMAL");
            case "reject-user-token" -> new WhitelistUser("reject-user", "Reject User", Set.of("USER"), "ACTIVE", "NORMAL");
            case "profile-fail-token" -> new WhitelistUser("profile-fail-user", "Profile Fail", Set.of("USER"), "ACTIVE", "NORMAL");
            case "profile-unavailable-token" -> new WhitelistUser("profile-unavailable-user", "Profile Unavailable", Set.of("USER"), "ACTIVE", "NORMAL");
            case "profile-timeout-token" -> new WhitelistUser("profile-timeout-user", "Profile Timeout", Set.of("USER"), "ACTIVE", "NORMAL");
            case "profile-bad-schema-token" -> new WhitelistUser("profile-bad-schema-user", "Profile Bad Schema", Set.of("USER"), "ACTIVE", "NORMAL");
            case "profile-compensate-token" -> new WhitelistUser("profile-compensate-user", "Profile Compensate", Set.of("USER"), "ACTIVE", "NORMAL");
            case "remove-profile-fail-token" -> new WhitelistUser("remove-downstream-user", "Remove Profile Fail", Set.of("USER"), "ACTIVE", "NORMAL");
            case "reapply-user-token" -> new WhitelistUser("reapply-user", "Reapply User", Set.of("USER"), "ACTIVE", "NORMAL");
            default -> throw new WhitelistException(401, 41001, "invalid session");
        };
    }

    WhitelistUser requireAny(String authorization, String... roles) {
        WhitelistUser user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new WhitelistException(403, 42001, "role permission denied");
        return user;
    }
}

record WhitelistUser(String userId, String displayName, Set<String> roles, String status, String mode) {
}

record Handoff(String sessionId, String applicationId, int handoffVersion, int onboardingHandoffVersion, String userId,
               Map<String, Object> minecraftBinding, String reviewDirection, String attemptType, String result,
               Map<String, Object> scoreSummary, String passedAt) {
}

record IdempotencyRecord(String fingerprint, Map<String, Object> value) {
}

record MutationResult(boolean created, Map<String, Object> value) {
}

class WhitelistApplicationRecord {
    String applicationId;
    String examSessionId;
    String onboardingApplicationId;
    int examHandoffVersion;
    int onboardingHandoffVersion;
    String userId;
    String displayNameSnapshot;
    Map<String, Object> minecraftBindingSnapshot;
    String reviewDirection;
    String attemptType;
    String status;
    String result;
    List<Map<String, Object>> materials = List.of();
    Map<String, Object> scoreSummary;
    String examPassedAt;
    String reviewerUserId;
    String reviewerDisplayNameSnapshot;
    String reviewComment;
    String internalNote;
    Map<String, Object> supplementRequest;
    Map<String, Object> profileActivation;
    Map<String, Object> attendanceHandoff;
    String notificationStatus;
    Map<String, Object> notificationFailure;
    String removedAt;
    String removedBy;
    String removalReason;
    boolean reapplyRequired;
    String nextExamAttemptType;
    String createdAt;
    String updatedAt;
    String submittedAt;
    String reviewedAt;
    String approvedAt;
    String rejectedAt;
    String withdrawnAt;
    String archivedAt;
}

class WhitelistException extends RuntimeException {
    final int httpStatus;
    final int code;

    WhitelistException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

@RestControllerAdvice(basePackages = "cn.beiming.whitelist")
class WhitelistExceptionHandler {
    @ExceptionHandler(WhitelistException.class)
    ResponseEntity<Map<String, Object>> handle(WhitelistException ex) {
        return ResponseEntity.status(ex.httpStatus).body(error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(error(40400, "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(error(52000, "whitelist internal error"));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", WhitelistController.requestId());
        return body;
    }
}

class WhitelistRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req-" + UUID.randomUUID();
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@Configuration
class WhitelistRequestIdFilterConfig {
    @Bean
    WhitelistRequestIdFilter whitelistRequestIdFilter() {
        return new WhitelistRequestIdFilter();
    }
}
