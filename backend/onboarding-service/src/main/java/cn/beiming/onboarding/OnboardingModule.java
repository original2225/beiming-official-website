package cn.beiming.onboarding;

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
class OnboardingModule {
    @Bean
    OnboardingStore onboardingStore() {
        OnboardingStore store = new OnboardingStore();
        store.seed();
        return store;
    }

    @Bean
    TestOnboardingAuthProvider onboardingAuthProvider() {
        return new TestOnboardingAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController {
    private final OnboardingStore store;
    private final TestOnboardingAuthProvider auth;

    OnboardingController(OnboardingStore store, TestOnboardingAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/me/progress")
    Map<String, Object> progress(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.progress(user, request));
    }

    @PostMapping("/me/start")
    ResponseEntity<Map<String, Object>> start(@RequestHeader(value = "Authorization", required = false) String authorization,
                                              @RequestBody(required = false) Map<String, Object> body,
                                              HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        MutationResult result = store.start(user, bodyOrEmpty(body), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(okBody(result.application()));
    }

    @PatchMapping("/me/profile-confirmation")
    Map<String, Object> confirmProfile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.confirmProfile(user, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/rules-confirmation")
    Map<String, Object> confirmRules(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.confirmRules(user, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/direction")
    Map<String, Object> direction(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestBody(required = false) Map<String, Object> body,
                                  HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.direction(user, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/advance")
    Map<String, Object> advance(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.advance(user, bodyOrEmpty(body), request));
    }

    @GetMapping("/me/next-action")
    Map<String, Object> nextAction(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest request) {
        AuthContext user = auth.requireUser(authorization);
        return ok(store.nextAction(user, request));
    }

    @GetMapping("/admin/applications")
    Map<String, Object> applications(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query,
                                     HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.applications(actor, query, request));
    }

    @GetMapping("/admin/applications/{applicationId}")
    Map<String, Object> application(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String applicationId,
                                    HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.application(actor, applicationId, request));
    }

    @GetMapping("/admin/applications/{applicationId}/exam-handoff")
    Map<String, Object> examHandoff(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String applicationId,
                                    HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.examHandoff(actor, applicationId, request));
    }

    @PatchMapping("/admin/applications/{applicationId}/reset")
    Map<String, Object> reset(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable String applicationId,
                              @RequestBody(required = false) Map<String, Object> body,
                              HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.reset(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/block")
    Map<String, Object> block(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable String applicationId,
                              @RequestBody(required = false) Map<String, Object> body,
                              HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.block(actor, applicationId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/applications/{applicationId}/unblock")
    Map<String, Object> unblock(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String applicationId,
                                @RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.unblock(actor, applicationId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        AuthContext actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(actor, query, request));
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

class OnboardingStore {
    private static final String NOW = "2026-05-22T12:00:00Z";
    private static final Set<String> STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "BLOCKED", "READY_FOR_EXAM", "WAITING_EXAM", "READY_FOR_WHITELIST", "WAITING_WHITELIST", "COMPLETED", "CANCELLED");
    private static final Set<String> DIRECTIONS = Set.of("REDSTONE", "LATE_GAME", "BUILDING", "GENERAL");
    private static final Set<String> RESET_STEPS = Set.of("ACCOUNT_READY", "PROFILE_CONFIRMED", "RULES_CONFIRMED", "DIRECTION_SELECTED");
    private final Map<String, ApplicationRecord> applications = new ConcurrentHashMap<>();
    private final Map<String, String> currentByUser = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = new ArrayList<>();
    private int idSeq = 2000;
    private int handoffSnapshotsTotal;

    void seed() {
        ApplicationRecord inProgress = newRecord("app-in-progress", "seed-in-progress", "Seed Player", minecraft("SeedSteve", "uuid-seed"));
        inProgress.profileConfirmation = profileConfirmation(inProgress.displayName, "SeedSteve", "uuid-seed");
        inProgress.ruleConfirmation = ruleConfirmation();
        put(inProgress);

        ApplicationRecord blocked = newRecord("app-blocked", "seed-blocked", "Blocked Player", minecraft("BlockedSteve", "uuid-blocked"));
        blocked.profileConfirmation = profileConfirmation(blocked.displayName, "BlockedSteve", "uuid-blocked");
        blocked.status = "BLOCKED";
        blocked.previousStatus = "IN_PROGRESS";
        blocked.blockedReason = "Manual review";
        blocked.blockedBy = "admin";
        blocked.blockedAt = NOW;
        put(blocked);

        ApplicationRecord ready = newRecord("app-ready", "seed-ready", "Ready Player", minecraft("ReadySteve", "uuid-ready"));
        ready.profileConfirmation = profileConfirmation(ready.displayName, "ReadySteve", "uuid-ready");
        ready.ruleConfirmation = ruleConfirmation();
        ready.reviewDirection = "REDSTONE";
        ready.status = "READY_FOR_EXAM";
        put(ready);

        audits.add(auditRow("audit-seed-1", "admin", "app-in-progress", "ONBOARDING_STARTED", "SUCCESS", "LOW", "IN_PROGRESS", "IN_PROGRESS", null));
    }

    synchronized Map<String, Object> progress(AuthContext user, HttpServletRequest request) {
        ApplicationRecord app = current(user.userId());
        List<String> degradeReasons = new ArrayList<>();
        String profileMode = profileMode(user, request);
        if (Set.of("UNAVAILABLE", "TIMEOUT", "BAD").contains(profileMode)) {
            degradeReasons.add("PROFILE_" + profileMode);
        }
        RuleSummary rule = ruleForRead(user, request, degradeReasons);
        return applicationView(app, user, rule, !degradeReasons.isEmpty(), degradeReasons);
    }

    synchronized MutationResult start(AuthContext user, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "START", body);
        if (replay != null) {
            return new MutationResult(false, replay.value());
        }
        requireProfileWrite(user, request);
        rejectExistingMember(user);
        if (auditShouldFail(request)) {
            throw new OnboardingException(500, 51801, "onboarding audit failed");
        }
        if (storeShouldFail(request)) {
            throw new OnboardingException(500, 51802, "onboarding state failed");
        }

        boolean created = false;
        ApplicationRecord app = current(user.userId());
        if (app == null) {
            app = newRecord("app-" + (++idSeq), user.userId(), user.displayName(), user.minecraftBinding());
            put(app);
            audit(user, app, "ONBOARDING_STARTED", "LOW", null, app.status, null);
            created = true;
        }
        applyAuxNotification(app, request);
        Map<String, Object> view = applicationView(app, user, ruleForRead(user, request, new ArrayList<>()), false, List.of());
        remember(user.userId(), "START", body, view);
        return new MutationResult(created, view);
    }

    synchronized Map<String, Object> confirmProfile(AuthContext user, Map<String, Object> body, HttpServletRequest request) {
        validateTrue(body, "confirmed");
        validateIdempotencyKey(body);
        requireProfileWrite(user, request);
        IdempotencyRecord replay = replay(user.userId(), "PROFILE", body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireCurrent(user);
        requireNotBlocked(app);
        requireCompleteBinding(user);
        rejectExistingMember(user);
        failBeforeWrite(request);
        if (app.profileConfirmation == null) {
            app.profileConfirmation = profileConfirmation(user.displayName(), string(user.minecraftBinding().get("minecraftId")), string(user.minecraftBinding().get("minecraftUuid")));
            app.updatedAt = NOW;
            audit(user, app, "ONBOARDING_PROFILE_CONFIRMED", "LOW", app.status, app.status, null);
        }
        applyAuxNotification(app, request);
        Map<String, Object> view = applicationView(app, user, ruleForRead(user, request, new ArrayList<>()), false, List.of());
        remember(user.userId(), "PROFILE", body, view);
        return view;
    }

    synchronized Map<String, Object> confirmRules(AuthContext user, Map<String, Object> body, HttpServletRequest request) {
        validateTrue(body, "confirmed");
        validateRequiredString(body, "ruleContentId", 1, 120);
        validateRequiredString(body, "ruleVersion", 1, 80);
        validateIdempotencyKey(body);
        RuleSummary rule = requireRuleForWrite(user, request);
        IdempotencyRecord replay = replay(user.userId(), "RULES", body);
        if (replay != null) return replay.value();
        if (!rule.ruleContentId().equals(string(body.get("ruleContentId"))) || !rule.ruleVersion().equals(string(body.get("ruleVersion")))) {
            throw new OnboardingException(409, "rule-current".equals(string(body.get("ruleContentId"))) ? 43814 : 43814, "rule version expired");
        }
        ApplicationRecord app = requireCurrent(user);
        requireNotBlocked(app);
        failBeforeWrite(request);
        app.ruleConfirmation = ruleConfirmation();
        app.updatedAt = NOW;
        audit(user, app, "ONBOARDING_RULES_CONFIRMED", "LOW", app.status, app.status, null);
        applyAuxNotification(app, request);
        Map<String, Object> view = applicationView(app, user, rule, false, List.of());
        remember(user.userId(), "RULES", body, view);
        return view;
    }

    synchronized Map<String, Object> direction(AuthContext user, Map<String, Object> body, HttpServletRequest request) {
        String direction = validateEnum(body, "reviewDirection", DIRECTIONS);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "DIRECTION", body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireCurrent(user);
        requireNotBlocked(app);
        if ("READY_FOR_EXAM".equals(app.status) || "WAITING_EXAM".equals(app.status)) {
            throw new OnboardingException(409, 43815, "direction locked");
        }
        if (app.profileConfirmation == null || app.ruleConfirmation == null) {
            throw new OnboardingException(409, 43811, "step prerequisite missing");
        }
        failBeforeWrite(request);
        app.reviewDirection = direction;
        app.updatedAt = NOW;
        audit(user, app, "ONBOARDING_DIRECTION_SELECTED", "LOW", app.status, app.status, null);
        applyAuxNotification(app, request);
        Map<String, Object> view = applicationView(app, user, ruleForRead(user, request, new ArrayList<>()), false, List.of());
        remember(user.userId(), "DIRECTION", body, view);
        return view;
    }

    synchronized Map<String, Object> advance(AuthContext user, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(user.userId(), "ADVANCE", body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireCurrent(user);
        requireNotBlocked(app);
        requireCompleteBinding(user);
        requireProfileWrite(user, request);
        RuleSummary rule = requireRuleForWrite(user, request);
        if (app.profileConfirmation == null || app.ruleConfirmation == null || app.reviewDirection == null) {
            throw new OnboardingException(409, 43811, "step prerequisite missing");
        }
        if (!rule.ruleVersion().equals(string(app.ruleConfirmation.get("ruleVersion")))) {
            throw new OnboardingException(409, 43814, "rule version expired");
        }
        failBeforeWrite(request);
        String before = app.status;
        app.status = "READY_FOR_EXAM";
        app.updatedAt = NOW;
        audit(user, app, "ONBOARDING_READY_FOR_EXAM", "LOW", before, app.status, null);
        applyAuxNotification(app, request);
        Map<String, Object> view = applicationView(app, user, rule, false, List.of());
        remember(user.userId(), "ADVANCE", body, view);
        return view;
    }

    synchronized Map<String, Object> nextAction(AuthContext user, HttpServletRequest request) {
        Map<String, Object> app = progress(user, request);
        return map(app.get("nextAction"));
    }

    synchronized Map<String, Object> applications(AuthContext actor, Map<String, String> query, HttpServletRequest request) {
        if (storeShouldFail(request)) throw new OnboardingException(500, 51800, "onboarding internal error");
        int page = page(query);
        int pageSize = pageSize(query);
        String status = query.get("status");
        if (status != null && !STATUSES.contains(status)) throw new OnboardingException(400, 40001, "invalid status");
        String direction = query.get("reviewDirection");
        if (direction != null && !DIRECTIONS.contains(direction)) throw new OnboardingException(400, 40001, "invalid direction");
        Boolean blocked = boolValue(query, "blocked");
        String sort = query.getOrDefault("sort", "updatedAt_desc");
        if (!Set.of("updatedAt_desc", "createdAt_desc", "status_asc", "displayName_asc").contains(sort)) {
            throw new OnboardingException(400, 40003, "invalid sort");
        }
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationRecord app : applications.values()) {
            if (status != null && !status.equals(app.status)) continue;
            if (direction != null && !direction.equals(app.reviewDirection)) continue;
            if (blocked != null && blocked != "BLOCKED".equals(app.status)) continue;
            if (!keyword.isBlank() && !matchesKeyword(app, keyword)) continue;
            rows.add(applicationView(app, null, defaultRule(), false, List.of()));
        }
        rows.sort(comparator(sort).thenComparing(row -> row.get("applicationId").toString()));
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> application(AuthContext actor, String applicationId, HttpServletRequest request) {
        ApplicationRecord app = applications.get(applicationId);
        if (app == null) throw new OnboardingException(404, 43800, "application not found");
        return applicationView(app, null, defaultRule(), false, List.of());
    }

    synchronized Map<String, Object> examHandoff(AuthContext actor, String applicationId, HttpServletRequest request) {
        ApplicationRecord app = requireApplication(applicationId);
        if ("BLOCKED".equals(app.status)) throw new OnboardingException(409, 43816, "application blocked");
        if (!"READY_FOR_EXAM".equals(app.status)
                || app.profileConfirmation == null
                || app.ruleConfirmation == null
                || app.reviewDirection == null) {
            throw new OnboardingException(409, 43811, "exam handoff prerequisites missing");
        }
        if (!completeBinding(app.minecraftBinding)) throw new OnboardingException(409, 43813, "Minecraft identity missing");
        requireProfileSnapshotForHandoff(request);
        RuleSummary rule = requireRuleForWrite(actor, request);
        if (!rule.ruleVersion().equals(string(app.ruleConfirmation.get("ruleVersion")))) {
            throw new OnboardingException(409, 43814, "rule version expired");
        }
        handoffSnapshotsTotal++;
        return mapOf(
                "applicationId", app.applicationId,
                "userId", app.userId,
                "displayNameSnapshot", app.displayName,
                "minecraftBindingSnapshot", app.minecraftBinding,
                "profileConfirmation", app.profileConfirmation,
                "ruleConfirmation", app.ruleConfirmation,
                "reviewDirection", app.reviewDirection,
                "status", app.status,
                "readyForExam", true,
                "handoffAllowed", true,
                "targetModule", "EXAM",
                "targetModuleStatus", "NOT_IMPLEMENTED",
                "blocked", false,
                "blockedReason", null,
                "handoffVersion", 1,
                "generatedAt", NOW
        );
    }

    synchronized Map<String, Object> reset(AuthContext actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        String step = body.containsKey("resetToStep") ? validateEnum(body, "resetToStep", RESET_STEPS) : "ACCOUNT_READY";
        IdempotencyRecord replay = replay(actor.userId(), "RESET:" + applicationId, body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireApplication(applicationId);
        requireNotificationIfNeeded(body, request);
        failBeforeWrite(request);
        String before = app.status;
        app.status = "IN_PROGRESS";
        app.previousStatus = null;
        app.blockedReason = null;
        app.blockedBy = null;
        app.blockedAt = null;
        if ("ACCOUNT_READY".equals(step)) {
            app.profileConfirmation = null;
            app.ruleConfirmation = null;
            app.reviewDirection = null;
        } else if ("PROFILE_CONFIRMED".equals(step)) {
            app.ruleConfirmation = null;
            app.reviewDirection = null;
        } else if ("RULES_CONFIRMED".equals(step)) {
            app.reviewDirection = null;
        }
        app.completedAt = null;
        app.updatedAt = NOW;
        audit(actor, app, "ONBOARDING_RESET", "MEDIUM", before, app.status, string(body.get("reason")));
        Map<String, Object> view = applicationView(app, null, defaultRule(), false, List.of());
        remember(actor.userId(), "RESET:" + applicationId, body, view);
        return view;
    }

    synchronized Map<String, Object> block(AuthContext actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateRequiredString(body, "blockReason", 1, 500);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "BLOCK:" + applicationId, body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireApplication(applicationId);
        requireNotificationIfNeeded(body, request);
        failBeforeWrite(request);
        String before = app.status;
        if (!"BLOCKED".equals(app.status)) {
            app.previousStatus = app.status;
            app.status = "BLOCKED";
        }
        app.blockedReason = string(body.get("blockReason"));
        app.blockedBy = actor.userId();
        app.blockedAt = NOW;
        app.updatedAt = NOW;
        audit(actor, app, "ONBOARDING_BLOCKED", "MEDIUM", before, app.status, string(body.get("reason")));
        Map<String, Object> view = applicationView(app, null, defaultRule(), false, List.of());
        remember(actor.userId(), "BLOCK:" + applicationId, body, view);
        return view;
    }

    synchronized Map<String, Object> unblock(AuthContext actor, String applicationId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        validateIdempotencyKey(body);
        IdempotencyRecord replay = replay(actor.userId(), "UNBLOCK:" + applicationId, body);
        if (replay != null) return replay.value();
        ApplicationRecord app = requireApplication(applicationId);
        requireNotificationIfNeeded(body, request);
        failBeforeWrite(request);
        String before = app.status;
        if ("BLOCKED".equals(app.status)) {
            app.status = app.previousStatus == null ? "IN_PROGRESS" : app.previousStatus;
            app.previousStatus = null;
            app.blockedReason = null;
            app.blockedBy = null;
            app.blockedAt = null;
            app.updatedAt = NOW;
            audit(actor, app, "ONBOARDING_UNBLOCKED", "MEDIUM", before, app.status, string(body.get("reason")));
        }
        Map<String, Object> view = applicationView(app, null, defaultRule(), false, List.of());
        remember(actor.userId(), "UNBLOCK:" + applicationId, body, view);
        return view;
    }

    synchronized Map<String, Object> auditLogs(AuthContext actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = query.getOrDefault("sort", "createdAt_desc");
        if (!Set.of("createdAt_desc", "createdAt_asc").contains(sort)) throw new OnboardingException(400, 40003, "invalid sort");
        if (query.containsKey("result") && !Set.of("SUCCESS", "FAILED").contains(query.get("result"))) throw new OnboardingException(400, 40001, "invalid result");
        validateTimeRange(query);
        if (query.containsKey("action") && query.get("action").length() > 80) throw new OnboardingException(400, 40001, "invalid action");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> audit : audits) {
            if (query.containsKey("applicationId") && !query.get("applicationId").equals(audit.get("targetId"))) continue;
            if (query.containsKey("actorUserId") && !query.get("actorUserId").equals(audit.get("actorUserId"))) continue;
            if (query.containsKey("action") && !query.get("action").equals(audit.get("action"))) continue;
            if (query.containsKey("result") && !query.get("result").equals(audit.get("result"))) continue;
            rows.add(new LinkedHashMap<>(audit));
        }
        Comparator<Map<String, Object>> cmp = Comparator.comparing(row -> row.get("createdAt").toString());
        if ("createdAt_desc".equals(sort)) cmp = cmp.reversed();
        rows.sort(cmp.thenComparing(row -> row.get("id").toString()));
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> opsSummary(HttpServletRequest request) {
        if (storeShouldFail(request)) throw new OnboardingException(500, 51800, "onboarding internal error");
        long blocked = applications.values().stream().filter(app -> "BLOCKED".equals(app.status)).count();
        long ready = applications.values().stream().filter(app -> "READY_FOR_EXAM".equals(app.status)).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "onboarding");
        data.put("port", 8108);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", "TEST_STUB");
        data.put("profileMode", "TEST_STUB");
        data.put("contentMode", "TEST_STUB");
        data.put("notificationMode", "TEST_STUB");
        data.put("applicationsTotal", applications.size());
        data.put("blockedTotal", blocked);
        data.put("readyForExamTotal", ready);
        data.put("stateMachineMode", "EXPLICIT_P0");
        data.put("handoffSnapshotsTotal", handoffSnapshotsTotal);
        data.put("auditsTotal", audits.size());
        data.put("idempotencyRecordsTotal", idempotency.size());
        data.put("lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).get("createdAt"));
        data.put("productionGaps", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB", "P0_PROFILE_STUB", "P0_CONTENT_STUB", "P0_NOTIFICATION_STUB", "EXAM_NOT_IMPLEMENTED", "WHITELIST_NOT_IMPLEMENTED"));
        return data;
    }

    private Map<String, Object> applicationView(ApplicationRecord app, AuthContext user, RuleSummary rule, boolean degraded, List<String> degradeReasons) {
        AuthContext source = user;
        if (source == null && app != null) source = AuthContext.forSnapshot(app.userId, app.displayName, app.minecraftBinding);
        if (app == null) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("applicationId", null);
            view.put("userId", source.userId());
            view.put("displayNameSnapshot", source.displayName());
            view.put("authStatusSnapshot", source.status());
            view.put("minecraftBindingSnapshot", source.minecraftBinding());
            view.put("profileSummary", profileSummary(source));
            view.put("status", "NOT_STARTED");
            view.put("currentStep", currentStep(null, source));
            view.put("steps", steps(null, source, rule));
            view.put("reviewDirection", null);
            view.put("ruleConfirmation", null);
            view.put("profileConfirmation", null);
            view.put("nextAction", next(null, source));
            view.put("blockedReason", null);
            view.put("blockedBy", null);
            view.put("blockedAt", null);
            view.put("notificationStatus", null);
            view.put("degraded", degraded);
            view.put("degradeReasons", degradeReasons);
            view.put("createdAt", null);
            view.put("updatedAt", null);
            view.put("completedAt", null);
            view.put("cancelledAt", null);
            return view;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("applicationId", app.applicationId);
        view.put("userId", app.userId);
        view.put("displayNameSnapshot", app.displayName);
        view.put("authStatusSnapshot", source == null ? "ACTIVE" : source.status());
        view.put("minecraftBindingSnapshot", app.minecraftBinding);
        view.put("profileSummary", profileSummary(source));
        view.put("status", app.status);
        view.put("currentStep", currentStep(app, source));
        view.put("steps", steps(app, source, rule));
        view.put("reviewDirection", app.reviewDirection);
        view.put("ruleConfirmation", app.ruleConfirmation);
        view.put("profileConfirmation", app.profileConfirmation);
        view.put("nextAction", next(app, source));
        view.put("blockedReason", app.blockedReason);
        view.put("blockedBy", app.blockedBy);
        view.put("blockedAt", app.blockedAt);
        view.put("notificationStatus", app.notificationStatus);
        view.put("degraded", degraded);
        view.put("degradeReasons", degradeReasons);
        view.put("createdAt", app.createdAt);
        view.put("updatedAt", app.updatedAt);
        view.put("completedAt", app.completedAt);
        view.put("cancelledAt", app.cancelledAt);
        return view;
    }

    private List<Map<String, Object>> steps(ApplicationRecord app, AuthContext user, RuleSummary rule) {
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean bound = completeBinding(user == null ? (app == null ? null : app.minecraftBinding) : user.minecraftBinding());
        boolean profile = app != null && app.profileConfirmation != null;
        boolean rules = app != null && app.ruleConfirmation != null;
        boolean direction = app != null && app.reviewDirection != null;
        boolean ready = app != null && "READY_FOR_EXAM".equals(app.status);
        boolean blocked = app != null && "BLOCKED".equals(app.status);
        rows.add(step("ACCOUNT_READY", "COMPLETED", "Account ready", true, NOW, null, "/onboarding/start", "/api/v1/onboarding/me/start"));
        rows.add(step("MINECRAFT_BOUND", bound ? "COMPLETED" : "BLOCKED", "Minecraft bound", true, bound ? NOW : null, bound ? null : "Minecraft identity required", "/profile/minecraft", null));
        rows.add(step("PROFILE_CONFIRMED", profile ? "COMPLETED" : (bound ? "AVAILABLE" : "LOCKED"), "Confirm profile", true, completed(app == null ? null : app.profileConfirmation), null, "/onboarding/profile", "/api/v1/onboarding/me/profile-confirmation"));
        rows.add(step("RULES_CONFIRMED", rules ? "COMPLETED" : (rule == null ? "BLOCKED" : "AVAILABLE"), "Confirm rules", true, completed(app == null ? null : app.ruleConfirmation), rule == null ? "Rules unavailable" : null, "/rules", "/api/v1/onboarding/me/rules-confirmation"));
        rows.add(step("DIRECTION_SELECTED", direction ? "COMPLETED" : (profile && rules ? "AVAILABLE" : "LOCKED"), "Select direction", true, direction ? NOW : null, null, "/onboarding/direction", "/api/v1/onboarding/me/direction"));
        rows.add(step("EXAM_READY", ready ? "WAITING_MODULE" : (direction ? "AVAILABLE" : "LOCKED"), "Exam ready", true, null, ready ? "Exam module is not implemented" : null, "/exam/start", null));
        rows.add(step("WHITELIST_READY", "LOCKED", "Whitelist ready", true, null, "Whitelist module is not implemented", "/whitelist", null));
        if (blocked) {
            rows.replaceAll(row -> {
                if ("COMPLETED".equals(row.get("status"))) return row;
                Map<String, Object> copy = new LinkedHashMap<>(row);
                copy.put("status", "BLOCKED");
                copy.put("blockReason", app.blockedReason);
                return copy;
            });
        }
        return rows;
    }

    private Map<String, Object> next(ApplicationRecord app, AuthContext user) {
        if (app != null && "BLOCKED".equals(app.status)) return next("BLOCKED", "Application blocked", null, null, "ONBOARDING", "UNAVAILABLE", false, app.blockedReason);
        if (user != null && isMemberProfile(user.profileStatus())) return next("MEMBER_EXISTS", "Member profile exists", null, null, "ONBOARDING", "UNAVAILABLE", false, "member profile exists");
        if (app == null) return next("ACCOUNT_READY", "Start onboarding", "/onboarding/start", "/api/v1/onboarding/me/start", "ONBOARDING", "AVAILABLE", true, null);
        if (!completeBinding(user == null ? app.minecraftBinding : user.minecraftBinding())) return next("MINECRAFT_BOUND", "Bind Minecraft", "/profile/minecraft", null, "ONBOARDING", "UNAVAILABLE", false, "Minecraft identity required");
        if (app.profileConfirmation == null) return next("PROFILE_CONFIRMED", "Confirm profile", "/onboarding/profile", "/api/v1/onboarding/me/profile-confirmation", "ONBOARDING", "AVAILABLE", true, null);
        if (app.ruleConfirmation == null) return next("RULES_CONFIRMED", "Confirm rules", "/rules", "/api/v1/onboarding/me/rules-confirmation", "ONBOARDING", "AVAILABLE", true, null);
        if (app.reviewDirection == null) return next("DIRECTION_SELECTED", "Select direction", "/onboarding/direction", "/api/v1/onboarding/me/direction", "ONBOARDING", "AVAILABLE", true, null);
        return next("EXAM_READY", "Start exam", "/exam/start", null, "EXAM", "NOT_IMPLEMENTED", false, "Exam module is not implemented");
    }

    private String currentStep(ApplicationRecord app, AuthContext user) {
        if (app != null && "BLOCKED".equals(app.status)) return app.profileConfirmation == null ? "PROFILE_CONFIRMED" : "DIRECTION_SELECTED";
        if (app != null && "READY_FOR_EXAM".equals(app.status)) return "EXAM_READY";
        if (!completeBinding(user == null ? (app == null ? null : app.minecraftBinding) : user.minecraftBinding())) return "MINECRAFT_BOUND";
        if (app == null || app.profileConfirmation == null) return "PROFILE_CONFIRMED";
        if (app.ruleConfirmation == null) return "RULES_CONFIRMED";
        if (app.reviewDirection == null) return "DIRECTION_SELECTED";
        return "EXAM_READY";
    }

    private Map<String, Object> profileSummary(AuthContext user) {
        if (user == null || user.profileStatus() == null || user.profileStatus().startsWith("PROFILE_")) return null;
        if (!"ACTIVE".equals(user.profileStatus()) && !"INACTIVE".equals(user.profileStatus()) && !"REMOVED".equals(user.profileStatus()) && !"SUSPENDED".equals(user.profileStatus())) return null;
        return mapOf("memberId", "member-" + user.userId(), "status", user.profileStatus(), "displayName", user.displayName(), "minecraftId", user.minecraftBinding() == null ? null : user.minecraftBinding().get("minecraftId"), "snapshotAt", NOW);
    }

    private RuleSummary ruleForRead(AuthContext user, HttpServletRequest request, List<String> degradeReasons) {
        String mode = dependencyMode(user, request, "CONTENT");
        if ("UNAVAILABLE".equals(mode) || "TIMEOUT".equals(mode) || "BAD".equals(mode)) {
            degradeReasons.add("CONTENT_" + mode);
            return null;
        }
        return defaultRule();
    }

    private RuleSummary requireRuleForWrite(AuthContext user, HttpServletRequest request) {
        String mode = dependencyMode(user, request, "CONTENT");
        if ("UNAVAILABLE".equals(mode)) throw new OnboardingException(502, 46820, "content unavailable");
        if ("TIMEOUT".equals(mode)) throw new OnboardingException(504, 46821, "content timeout");
        if ("BAD".equals(mode)) throw new OnboardingException(502, 46822, "content incompatible");
        return defaultRule();
    }

    private void requireProfileWrite(AuthContext user, HttpServletRequest request) {
        String mode = profileMode(user, request);
        if ("UNAVAILABLE".equals(mode)) throw new OnboardingException(502, 46810, "profile unavailable");
        if ("TIMEOUT".equals(mode)) throw new OnboardingException(504, 46811, "profile timeout");
        if ("BAD".equals(mode)) throw new OnboardingException(502, 46812, "profile incompatible");
    }

    private void requireProfileSnapshotForHandoff(HttpServletRequest request) {
        String mode = dependencyMode(null, request, "PROFILE");
        if ("UNAVAILABLE".equals(mode)) throw new OnboardingException(502, 46810, "profile unavailable");
        if ("TIMEOUT".equals(mode)) throw new OnboardingException(504, 46811, "profile timeout");
        if ("BAD".equals(mode)) throw new OnboardingException(502, 46812, "profile incompatible");
    }

    private String profileMode(AuthContext user, HttpServletRequest request) {
        String mode = dependencyMode(user, request, "PROFILE");
        if (!"OK".equals(mode)) return mode;
        if ("PROFILE_UNAVAILABLE".equals(user.profileStatus())) return "UNAVAILABLE";
        if ("PROFILE_TIMEOUT".equals(user.profileStatus())) return "TIMEOUT";
        if ("PROFILE_BAD".equals(user.profileStatus())) return "BAD";
        return "OK";
    }

    private String dependencyMode(AuthContext user, HttpServletRequest request, String dependency) {
        if (request != null) {
            String header = request.getHeader("X-Test-Dependency-Mode");
            if (header != null) {
                for (String pair : header.split(",")) {
                    String[] parts = pair.split(":");
                    if (parts.length == 2 && dependency.equals(parts[0])) return parts[1];
                }
            }
        }
        if ("CONTENT".equals(dependency) && user != null && user.userId().contains("content-unavailable")) return "UNAVAILABLE";
        return "OK";
    }

    private void requireNotificationIfNeeded(Map<String, Object> body, HttpServletRequest request) {
        boolean notify = bool(body.getOrDefault("notifyUser", true));
        if (!notify) return;
        String mode = request.getHeader("X-Test-Notification-Mode");
        if ("unavailable".equals(mode)) throw new OnboardingException(502, 46830, "notification unavailable");
        if ("timeout".equals(mode)) throw new OnboardingException(504, 46831, "notification timeout");
        if ("bad".equals(mode)) throw new OnboardingException(502, 46832, "notification incompatible");
    }

    private void applyAuxNotification(ApplicationRecord app, HttpServletRequest request) {
        String mode = request.getHeader("X-Test-Notification-Mode");
        if ("unavailable".equals(mode) || "timeout".equals(mode) || "bad".equals(mode)) {
            app.notificationStatus = "FAILED";
        } else {
            app.notificationStatus = "DELIVERED";
        }
    }

    private void failBeforeWrite(HttpServletRequest request) {
        if (auditShouldFail(request)) throw new OnboardingException(500, 51801, "onboarding audit failed");
        if (storeShouldFail(request)) throw new OnboardingException(500, 51802, "onboarding state failed");
    }

    private boolean auditShouldFail(HttpServletRequest request) {
        return "true".equals(request.getHeader("X-Test-Fail-Audit"));
    }

    private boolean storeShouldFail(HttpServletRequest request) {
        return "true".equals(request.getHeader("X-Test-Fail-Store"));
    }

    private void rejectExistingMember(AuthContext user) {
        if (isMemberProfile(user.profileStatus())) {
            throw new OnboardingException(409, 43812, "member profile exists");
        }
    }

    private boolean isMemberProfile(String profileStatus) {
        return "ACTIVE".equals(profileStatus) || "INACTIVE".equals(profileStatus);
    }

    private void requireCompleteBinding(AuthContext user) {
        if (!completeBinding(user.minecraftBinding())) throw new OnboardingException(409, 43813, "Minecraft identity missing");
    }

    private void requireNotBlocked(ApplicationRecord app) {
        if ("BLOCKED".equals(app.status)) throw new OnboardingException(409, 43816, "application blocked");
    }

    private ApplicationRecord requireCurrent(AuthContext user) {
        ApplicationRecord app = current(user.userId());
        if (app == null) throw new OnboardingException(404, 43800, "application not found");
        return app;
    }

    private ApplicationRecord requireApplication(String applicationId) {
        ApplicationRecord app = applications.get(applicationId);
        if (app == null) throw new OnboardingException(404, 43800, "application not found");
        return app;
    }

    private ApplicationRecord current(String userId) {
        String id = currentByUser.get(userId);
        return id == null ? null : applications.get(id);
    }

    private void put(ApplicationRecord app) {
        applications.put(app.applicationId, app);
        currentByUser.put(app.userId, app.applicationId);
    }

    private ApplicationRecord newRecord(String id, String userId, String displayName, Map<String, Object> binding) {
        ApplicationRecord app = new ApplicationRecord();
        app.applicationId = id;
        app.userId = userId;
        app.displayName = displayName;
        app.minecraftBinding = binding;
        app.status = "IN_PROGRESS";
        app.createdAt = NOW;
        app.updatedAt = NOW;
        return app;
    }

    private void audit(AuthContext actor, ApplicationRecord app, String action, String risk, String before, String after, String reason) {
        audits.add(auditRow("audit-" + (++idSeq), actor.userId(), app.applicationId, action, "SUCCESS", risk, before, after, reason));
    }

    private Map<String, Object> auditRow(String id, String actorId, String targetId, String action, String result, String risk, String before, String after, String reason) {
        return mapOf("id", id, "requestId", OnboardingController.requestId(), "actorUserId", actorId, "actorRole", "ADMIN", "targetType", "ONBOARDING_APPLICATION", "targetId", targetId, "action", action, "riskLevel", risk, "reasonSummary", reason == null ? "contract" : reason, "beforeState", before, "afterState", after, "result", result, "failureReason", null, "createdAt", NOW);
    }

    private IdempotencyRecord replay(String actorId, String operation, Map<String, Object> body) {
        String key = idempotencyKey(body);
        if (key == null) return null;
        IdempotencyRecord existing = idempotency.get(actorId + ":" + operation + ":" + key);
        if (existing != null && !existing.fingerprint().equals(canonical(body))) throw new OnboardingException(409, 43817, "idempotency conflict");
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

    private Comparator<Map<String, Object>> comparator(String sort) {
        return switch (sort) {
            case "createdAt_desc" -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("createdAt"))).reversed();
            case "status_asc" -> Comparator.comparing(row -> Objects.toString(row.get("status")));
            case "displayName_asc" -> Comparator.comparing(row -> Objects.toString(row.get("displayNameSnapshot")));
            default -> Comparator.comparing((Map<String, Object> row) -> Objects.toString(row.get("updatedAt"))).reversed();
        };
    }

    private boolean matchesKeyword(ApplicationRecord app, String keyword) {
        String mc = app.minecraftBinding == null ? "" : Objects.toString(app.minecraftBinding.get("minecraftId"), "");
        return app.applicationId.toLowerCase().contains(keyword) || app.userId.toLowerCase().contains(keyword) || app.displayName.toLowerCase().contains(keyword) || mc.toLowerCase().contains(keyword);
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
            if (value < min || value > max) throw new OnboardingException(400, code, "invalid number");
            return value;
        } catch (NumberFormatException ex) {
            throw new OnboardingException(400, code, "invalid number");
        }
    }

    private Boolean boolValue(Map<String, String> query, String key) {
        if (!query.containsKey(key)) return null;
        if ("true".equals(query.get(key))) return true;
        if ("false".equals(query.get(key))) return false;
        throw new OnboardingException(400, 40001, "invalid boolean");
    }

    private void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.containsKey("from") ? Instant.parse(query.get("from")) : null;
            Instant to = query.containsKey("to") ? Instant.parse(query.get("to")) : null;
            if (from != null && to != null && from.isAfter(to)) throw new OnboardingException(400, 40001, "invalid time range");
        } catch (DateTimeParseException ex) {
            throw new OnboardingException(400, 40001, "invalid time");
        }
    }

    private void validateReason(Map<String, Object> body) {
        validateRequiredString(body, "reason", 1, 200);
    }

    private void validateIdempotencyKey(Map<String, Object> body) {
        if (!body.containsKey("idempotencyKey")) return;
        String value = string(body.get("idempotencyKey"));
        if (value == null || value.length() < 8 || value.length() > 80) throw new OnboardingException(400, 40001, "invalid idempotency key");
    }

    private void validateTrue(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof Boolean value) || !value) throw new OnboardingException(400, 40001, "invalid " + field);
    }

    private String validateEnum(Map<String, Object> body, String field, Set<String> allowed) {
        String value = validateRequiredString(body, field, 1, 120);
        if (!allowed.contains(value)) throw new OnboardingException(400, 40001, "invalid " + field);
        return value;
    }

    private String validateRequiredString(Map<String, Object> body, String field, int min, int max) {
        String value = string(body.get(field));
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) throw new OnboardingException(400, 40001, "invalid " + field);
        return value;
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean b) return b;
        if ("true".equals(value) || "false".equals(value)) return Boolean.parseBoolean(value.toString());
        throw new OnboardingException(400, 40001, "invalid boolean");
    }

    private boolean completeBinding(Map<String, Object> binding) {
        return binding != null && string(binding.get("minecraftId")) != null && string(binding.get("minecraftUuid")) != null;
    }

    private String completed(Map<String, Object> confirmation) {
        return confirmation == null ? null : string(confirmation.getOrDefault("confirmedAt", NOW));
    }

    private RuleSummary defaultRule() {
        return new RuleSummary("rule-current", "2026-05-22", "\u5317\u51a5\u670d\u52a1\u5668\u89c4\u5219", "/rules");
    }

    private Map<String, Object> ruleConfirmation() {
        RuleSummary rule = defaultRule();
        return mapOf("confirmed", true, "ruleContentId", rule.ruleContentId(), "ruleVersion", rule.ruleVersion(), "ruleTitle", rule.ruleTitle(), "guideRoute", rule.guideRoute(), "confirmedAt", NOW);
    }

    private Map<String, Object> profileConfirmation(String displayName, String minecraftId, String minecraftUuid) {
        return mapOf("confirmed", true, "displayNameSnapshot", displayName, "minecraftIdSnapshot", minecraftId, "minecraftUuidSnapshot", minecraftUuid, "confirmedAt", NOW);
    }

    private Map<String, Object> step(String key, String status, String title, boolean required, String completedAt, String blockReason, String route, String api) {
        return mapOf("key", key, "status", status, "title", title, "required", required, "completedAt", completedAt, "blockReason", blockReason, "targetRoute", route, "targetApi", api);
    }

    private Map<String, Object> next(String step, String label, String route, String api, String module, String moduleStatus, boolean enabled, String disabledReason) {
        return mapOf("step", step, "label", label, "targetRoute", route, "targetApi", api, "targetModule", module, "targetModuleStatus", moduleStatus, "enabled", enabled, "disabledReason", disabledReason);
    }

    private static Map<String, Object> minecraft(String id, String uuid) {
        return mapOf("minecraftId", id, "minecraftUuid", uuid, "verified", true, "snapshotAt", NOW);
    }

    @SafeVarargs
    static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new OnboardingException(500, 51800, "invalid object");
    }

    static String string(Object value) {
        return value == null ? null : value.toString();
    }
}

class TestOnboardingAuthProvider {
    AuthContext requireUser(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new OnboardingException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new OnboardingException(401, 41003, "bad token format");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new OnboardingException(502, 46800, "auth unavailable");
            case "auth-timeout-token" -> throw new OnboardingException(504, 46801, "auth timeout");
            case "auth-bad-token" -> throw new OnboardingException(502, 46802, "auth incompatible");
            case "owner-token" -> new AuthContext("owner", "Owner", Set.of("OWNER"), "ACTIVE", minecraft("OwnerMc", "uuid-owner"), null);
            case "admin-token" -> new AuthContext("admin", "Admin", Set.of("ADMIN"), "ACTIVE", minecraft("AdminMc", "uuid-admin"), null);
            case "helper-token" -> new AuthContext("helper", "Helper", Set.of("HELPER"), "ACTIVE", minecraft("HelperMc", "uuid-helper"), null);
            case "minecraft-bound-token" -> new AuthContext("user-bound", "Bound User", Set.of("USER"), "ACTIVE", minecraft("Steve", "uuid-steve"), null);
            case "minecraft-unbound-token" -> new AuthContext("user-unbound", "Unbound User", Set.of("USER"), "ACTIVE", null, null);
            case "active-member-token" -> new AuthContext("active-member", "Active Member", Set.of("USER"), "ACTIVE", minecraft("ActiveSteve", "uuid-active"), "ACTIVE");
            case "inactive-member-token" -> new AuthContext("inactive-member", "Inactive Member", Set.of("USER"), "ACTIVE", minecraft("InactiveSteve", "uuid-inactive"), "INACTIVE");
            case "removed-member-token" -> new AuthContext("removed-member", "Removed Member", Set.of("USER"), "ACTIVE", minecraft("RemovedSteve", "uuid-removed"), "REMOVED");
            case "profile-unavailable-token" -> new AuthContext("profile-unavailable", "Profile Down", Set.of("USER"), "ACTIVE", minecraft("ProfileDown", "uuid-profile-down"), "PROFILE_UNAVAILABLE");
            case "profile-timeout-token" -> new AuthContext("profile-timeout", "Profile Timeout", Set.of("USER"), "ACTIVE", minecraft("ProfileTimeout", "uuid-profile-timeout"), "PROFILE_TIMEOUT");
            case "profile-bad-token" -> new AuthContext("profile-bad", "Profile Bad", Set.of("USER"), "ACTIVE", minecraft("ProfileBad", "uuid-profile-bad"), "PROFILE_BAD");
            case "content-unavailable-token" -> new AuthContext("content-unavailable", "Content Down", Set.of("USER"), "ACTIVE", minecraft("ContentDown", "uuid-content-down"), null);
            case "notification-fail-token" -> new AuthContext("notification-fail", "Notify Fail", Set.of("USER"), "ACTIVE", minecraft("NotifySteve", "uuid-notify"), null);
            case "direction-missing-token" -> new AuthContext("direction-missing", "Direction Missing", Set.of("USER"), "ACTIVE", minecraft("DirectionSteve", "uuid-direction"), null);
            case "seed-in-progress-token" -> new AuthContext("seed-in-progress", "Seed Player", Set.of("USER"), "ACTIVE", minecraft("SeedSteve", "uuid-seed"), null);
            case "nested-token" -> new AuthContext("nested-user", "Nested User", Set.of("USER"), "ACTIVE", minecraft("NestedSteve", "uuid-nested"), null);
            case "rollback-token" -> new AuthContext("rollback-user", "Rollback User", Set.of("USER"), "ACTIVE", minecraft("RollbackSteve", "uuid-rollback"), null);
            case "user-token" -> new AuthContext("user", "User", Set.of("USER"), "ACTIVE", minecraft("UserSteve", "uuid-user"), null);
            default -> throw new OnboardingException(401, 41001, "invalid session");
        };
    }

    AuthContext requireAny(String authorization, String... roles) {
        AuthContext user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new OnboardingException(403, 42001, "role permission denied");
        return user;
    }

    private static Map<String, Object> minecraft(String id, String uuid) {
        return OnboardingStore.mapOf("minecraftId", id, "minecraftUuid", uuid, "verified", true, "snapshotAt", "2026-05-22T12:00:00Z");
    }
}

record AuthContext(String userId, String displayName, Set<String> roles, String status, Map<String, Object> minecraftBinding, String profileStatus) {
    static AuthContext forSnapshot(String userId, String displayName, Map<String, Object> binding) {
        return new AuthContext(userId, displayName, Set.of("USER"), "ACTIVE", binding, null);
    }
}

record RuleSummary(String ruleContentId, String ruleVersion, String ruleTitle, String guideRoute) {
}

record IdempotencyRecord(String fingerprint, Map<String, Object> value) {
}

record MutationResult(boolean created, Map<String, Object> application) {
}

class ApplicationRecord {
    String applicationId;
    String userId;
    String displayName;
    Map<String, Object> minecraftBinding;
    String status;
    String previousStatus;
    String reviewDirection;
    Map<String, Object> profileConfirmation;
    Map<String, Object> ruleConfirmation;
    String blockedReason;
    String blockedBy;
    String blockedAt;
    String notificationStatus;
    String createdAt;
    String updatedAt;
    String completedAt;
    String cancelledAt;
}

class OnboardingException extends RuntimeException {
    final int httpStatus;
    final int code;

    OnboardingException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

@RestControllerAdvice
class OnboardingExceptionHandler {
    @ExceptionHandler(OnboardingException.class)
    ResponseEntity<Map<String, Object>> handle(OnboardingException ex) {
        return ResponseEntity.status(ex.httpStatus).body(error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(error(40400, "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(error(51800, "onboarding internal error"));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", OnboardingController.requestId());
        return body;
    }
}

class OnboardingRequestIdFilter extends OncePerRequestFilter {
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
class OnboardingRequestIdFilterConfig {
    @Bean
    OnboardingRequestIdFilter onboardingRequestIdFilter() {
        return new OnboardingRequestIdFilter();
    }
}
