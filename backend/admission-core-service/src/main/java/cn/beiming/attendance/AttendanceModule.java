package cn.beiming.attendance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
class AttendanceModule {
    @Bean
    AttendanceStore attendanceStore(AttendanceTestControls testControls) {
        return new AttendanceStore(testControls);
    }

    @Bean
    TestAttendanceAuthProvider attendanceAuthProvider() {
        return new TestAttendanceAuthProvider();
    }

    @Bean
    AttendanceTestControls attendanceTestControls(@Value("${attendance.test-controls.enabled:false}") boolean enabled) {
        return new AttendanceTestControls(enabled);
    }
}

@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController {
    private final AttendanceStore store;
    private final TestAttendanceAuthProvider auth;

    AttendanceController(AttendanceStore store, TestAttendanceAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/leaderboard")
    Map<String, Object> leaderboard(@RequestParam Map<String, String> query, HttpServletRequest request) {
        return ok(store.leaderboard(query, request));
    }

    @GetMapping("/me/account")
    Map<String, Object> myAccount(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization);
        return ok(store.myAccount(user, request));
    }

    @GetMapping("/me/ledger")
    Map<String, Object> myLedger(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query,
                                 HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization);
        return ok(store.myLedger(user, query, request));
    }

    @GetMapping("/me/contributions")
    Map<String, Object> myContributions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam Map<String, String> query,
                                        HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization);
        return ok(store.myContributions(user, query, request));
    }

    @GetMapping("/me/ranking")
    Map<String, Object> myRanking(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization);
        return ok(store.myRanking(user, request));
    }

    @GetMapping("/admin/accounts")
    Map<String, Object> adminAccounts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query,
                                      HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminAccounts(actor, query, request));
    }

    @GetMapping("/admin/accounts/{accountId}")
    Map<String, Object> adminAccount(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String accountId,
                                     HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminAccount(actor, accountId, request));
    }

    @PostMapping("/admin/initializations")
    ResponseEntity<Map<String, Object>> initialize(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        MutationResult result = store.initialize(actor, bodyOrEmpty(body), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(okBody(result.value()));
    }

    @PostMapping("/admin/accounts/{accountId}/adjustments")
    Map<String, Object> adjust(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String accountId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adjust(actor, accountId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/ledger/{ledgerId}/reverse")
    Map<String, Object> reverse(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String ledgerId,
                                @RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.reverse(actor, ledgerId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/contributions")
    ResponseEntity<Map<String, Object>> createContribution(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @RequestBody(required = false) Map<String, Object> body,
                                                           HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.createContribution(actor, bodyOrEmpty(body), request)));
    }

    @PatchMapping("/admin/contributions/{contributionId}")
    Map<String, Object> correctContribution(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable String contributionId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.correctContribution(actor, contributionId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/monthly-runs/preview")
    Map<String, Object> previewMonthly(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.monthlyPreview(actor, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/monthly-runs")
    ResponseEntity<Map<String, Object>> runMonthly(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        MutationResult result = store.runMonthly(actor, bodyOrEmpty(body), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(okBody(result.value()));
    }

    @GetMapping("/admin/monthly-runs/{runId}")
    Map<String, Object> monthlyRun(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String runId,
                                   HttpServletRequest request) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.monthlyRun(runId, request));
    }

    @GetMapping("/admin/removal-candidates")
    Map<String, Object> removalCandidates(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestParam Map<String, String> query,
                                          HttpServletRequest request) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.removalCandidates(query, request));
    }

    @PatchMapping("/admin/removal-candidates/{candidateId}/confirm")
    Map<String, Object> confirmCandidate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String candidateId,
                                         @RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.confirmCandidate(actor, candidateId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/removal-candidates/{candidateId}/dismiss")
    Map<String, Object> dismissCandidate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String candidateId,
                                         @RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.dismissCandidate(actor, candidateId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/leaderboard/rebuild")
    Map<String, Object> rebuildLeaderboard(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody(required = false) Map<String, Object> body,
                                           HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.rebuildLeaderboard(actor, bodyOrEmpty(body), request));
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
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
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

class AttendanceStore {
    private static final String NOW = "2026-05-23T12:00:00Z";
    private static final Set<String> ACCOUNT_STATUSES = Set.of("PENDING_INITIALIZATION", "ACTIVE", "FROZEN", "REMOVAL_CANDIDATE", "REMOVED", "ARCHIVED");
    private static final Set<String> LEDGER_TYPES = Set.of("INITIAL_GRANT", "ADMIN_ADJUSTMENT", "ACTIVITY_REWARD", "CONTRIBUTION_REWARD", "MONTHLY_DEDUCTION", "REVERSAL");
    private static final Set<String> CONTRIBUTION_TYPES = Set.of("ONLINE_ACTIVE", "PROJECT_BUILD", "EVENT_PARTICIPATION", "WORK_SUBMISSION", "HELPER_SUPPORT", "MANUAL");
    private static final Set<String> CANDIDATE_STATUSES = Set.of("OPEN", "CONFIRMED", "DISMISSED", "EXPIRED");
    private final Map<String, AttendanceAccountRecord> accounts = new ConcurrentHashMap<>();
    private final Map<String, AttendanceLedgerRecord> ledgers = new ConcurrentHashMap<>();
    private final Map<String, ContributionRecord> contributions = new ConcurrentHashMap<>();
    private final Map<String, RemovalCandidateRecord> candidates = new ConcurrentHashMap<>();
    private final Map<String, MonthlyRunRecord> monthlyRuns = new ConcurrentHashMap<>();
    private final Map<String, String> accountByUser = new ConcurrentHashMap<>();
    private final Map<String, String> accountByMember = new ConcurrentHashMap<>();
    private final Map<String, String> initByApplication = new ConcurrentHashMap<>();
    private final Map<String, String> sourceContribution = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = java.util.Collections.synchronizedList(new ArrayList<>());
    private final AttendanceTestControls testControls;
    private int idSeq = 1000;

    AttendanceStore(AttendanceTestControls testControls) {
        this.testControls = testControls;
    }

    synchronized MutationResult initialize(AttendanceUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        IdempotencyRecord existing = replay(actor.userId(), "initialize", body);
        if (existing != null) return new MutationResult(false, existing.value());
        failBeforeWrite(request);
        Handoff handoff = handoff(body, request);
        String existingAccountId = initByApplication.get(handoff.applicationId());
        if (existingAccountId != null) {
            Map<String, Object> value = initializationView(accounts.get(existingAccountId), firstLedger(existingAccountId), handoffView(handoff));
            remember(actor.userId(), "initialize", body, value);
            return new MutationResult(false, value);
        }
        if (accountByMember.containsKey(handoff.memberId())) throw new AttendanceException(409, 45012, "attendance account exists");
        profileCheck(handoff.memberId(), request);
        AttendanceAccountRecord account = new AttendanceAccountRecord();
        account.accountId = "att-acc-" + (++idSeq);
        account.userId = handoff.userId();
        account.memberId = handoff.memberId();
        account.displayNameSnapshot = "Member " + handoff.userId();
        account.avatarUrlSnapshot = null;
        account.memberGroupSnapshot = "default";
        account.memberStatusSnapshot = "ACTIVE";
        account.minecraftBindingSnapshot = handoff.minecraftBinding();
        account.status = "ACTIVE";
        account.scoreBalance = 100;
        account.initialScore = 100;
        account.totalEarned = 100;
        account.totalDeducted = 0;
        account.lastPositiveActivityAt = NOW;
        account.whitelistApplicationId = handoff.applicationId();
        account.whitelistHandoffId = handoff.handoffId();
        account.whitelistHandoffVersion = handoff.handoffVersion();
        account.reviewDirection = handoff.reviewDirection();
        account.attemptType = handoff.attemptType();
        account.notificationStatus = notificationStatus(request);
        account.notificationFailure = notificationFailure(request);
        account.profileSnapshotStale = false;
        account.createdAt = NOW;
        account.updatedAt = NOW;
        AttendanceLedgerRecord ledger = ledger(account, "INITIAL_GRANT", 100, 0, 100, "whitelist", handoff.handoffId(), null, "符合白名单初始化", "初始化考勤积分", actor.userId(), idempotencyKey(body), null, request);
        account.lastLedgerId = ledger.ledgerId;
        accounts.put(account.accountId, account);
        ledgers.put(ledger.ledgerId, ledger);
        accountByUser.put(account.userId, account.accountId);
        accountByMember.put(account.memberId, account.accountId);
        initByApplication.put(handoff.applicationId(), account.accountId);
        audit(actor, "ATTENDANCE_ACCOUNT", account.accountId, "ATTENDANCE_INITIALIZED", "MEDIUM", null, account.status, "initialize attendance");
        auditNotificationFailure(actor, account.accountId, account.notificationFailure);
        Map<String, Object> value = initializationView(account, ledger, handoffView(handoff));
        remember(actor.userId(), "initialize", body, value);
        return new MutationResult(true, value);
    }

    Map<String, Object> myAccount(AttendanceUser user, HttpServletRequest request) {
        String id = accountByUser.get(user.userId());
        return id == null ? null : accountView(accounts.get(id), false, request);
    }

    Map<String, Object> myLedger(AttendanceUser user, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String type = enumQuery(query, "type", LEDGER_TYPES);
        String cycleKey = cycleKey(query, false);
        String sort = sort(query, Set.of("createdAt_desc", "createdAt_asc"), "createdAt_desc");
        String accountId = accountByUser.get(user.userId());
        List<Map<String, Object>> rows = ledgers.values().stream()
                .filter(ledger -> Objects.equals(ledger.accountId, accountId))
                .filter(ledger -> type == null || type.equals(ledger.type))
                .filter(ledger -> cycleKey == null || cycleKey.equals(ledger.cycleKey))
                .sorted(ledgerComparator(sort))
                .map(ledger -> ledgerView(ledger, false))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> myContributions(AttendanceUser user, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String type = enumQuery(query, "type", CONTRIBUTION_TYPES);
        String accountId = accountByUser.get(user.userId());
        List<Map<String, Object>> rows = contributions.values().stream()
                .filter(item -> Objects.equals(item.accountId, accountId))
                .filter(item -> type == null || type.equals(item.type))
                .sorted(Comparator.comparing((ContributionRecord item) -> item.createdAt).reversed().thenComparing(item -> item.contributionId))
                .map(item -> contributionView(item, false))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> myRanking(AttendanceUser user, HttpServletRequest request) {
        String accountId = accountByUser.get(user.userId());
        if (accountId == null) return null;
        List<Map<String, Object>> entries = leaderboardEntries(Map.of(), request);
        for (Map<String, Object> entry : entries) {
            if (accountId.equals(entry.get("accountId"))) {
                return linkedMap("rank", entry.get("rank"), "totalRanked", entries.size(), "entry", entry);
            }
        }
        return null;
    }

    Map<String, Object> leaderboard(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = sort(query, Set.of("score_desc", "earned_desc", "lastActivity_desc"), "score_desc");
        String cycle = cycleKey(query, false);
        String memberGroup = query.get("memberGroup");
        if (memberGroup != null && memberGroup.length() > 80) throw new AttendanceException(400, 40001, "invalid memberGroup");
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("sort", sort);
        if (cycle != null) filters.put("cycleKey", cycle);
        if (memberGroup != null) filters.put("memberGroup", memberGroup);
        return pageRows(leaderboardEntries(filters, request), page, pageSize);
    }

    Map<String, Object> adminAccounts(AttendanceUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", ACCOUNT_STATUSES);
        String sort = sort(query, Set.of("createdAt_desc", "updatedAt_desc", "score_desc", "score_asc", "lastActivity_desc"), "createdAt_desc");
        String keyword = lower(query.get("keyword"));
        if (keyword != null && keyword.length() > 80) throw new AttendanceException(400, 40001, "invalid keyword");
        int minScore = intQuery(query, "minScore", Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 40001);
        int maxScore = intQuery(query, "maxScore", Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 40001);
        if (minScore > maxScore) throw new AttendanceException(400, 40001, "invalid score range");
        String direction = query.get("reviewDirection");
        String attempt = query.get("attemptType");
        if (attempt != null && !Set.of("FIRST_TIME", "RECHECK").contains(attempt)) throw new AttendanceException(400, 40001, "invalid attemptType");
        List<Map<String, Object>> rows = accounts.values().stream()
                .filter(account -> status == null || status.equals(account.status))
                .filter(account -> direction == null || direction.equals(account.reviewDirection))
                .filter(account -> attempt == null || attempt.equals(account.attemptType))
                .filter(account -> account.scoreBalance >= minScore && account.scoreBalance <= maxScore)
                .filter(account -> keyword == null || matches(account, keyword))
                .sorted(accountComparator(sort))
                .map(account -> accountView(account, true, request))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminAccount(AttendanceUser actor, String accountId, HttpServletRequest request) {
        AttendanceAccountRecord account = requireAccount(accountId);
        return linkedMap("account", accountView(account, true, request), "recentLedger", recentLedger(accountId), "recentContributions", recentContributions(accountId), "openCandidate", openCandidateView(accountId), "dependencyStatus", dependencyStatus(request));
    }

    synchronized Map<String, Object> adjust(AttendanceUser actor, String accountId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        String publicReason = validateRequiredString(body, "publicReason", 1, 200);
        IdempotencyRecord existing = replay(actor.userId(), "adjust:" + accountId, body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        AttendanceAccountRecord account = requireAccount(accountId);
        if (!Set.of("ACTIVE", "FROZEN", "REMOVAL_CANDIDATE").contains(account.status)) throw new AttendanceException(409, 45013, "account status conflict");
        int delta = intBody(body, "delta", Integer.MIN_VALUE);
        if (delta == 0 || delta < -1000 || delta > 1000) throw new AttendanceException(400, 40001, "invalid delta");
        String beforeStatus = account.status;
        int before = account.scoreBalance;
        int after = Math.max(0, before + delta);
        failLedger(request);
        AttendanceLedgerRecord ledger = ledger(account, "ADMIN_ADJUSTMENT", delta, before, after, "manual", Objects.toString(body.getOrDefault("sourceId", "manual-" + (++idSeq))), null, validateRequiredString(body, "reason", 1, 500), publicReason, actor.userId(), idempotencyKey(body), null, request);
        ledgers.put(ledger.ledgerId, ledger);
        applyBalance(account, after, delta, ledger.ledgerId);
        RemovalCandidateRecord candidate = ensureCandidateIfNeeded(account, "manual", actor, "积分为 0，进入复核", "积分归零");
        audit(actor, "ATTENDANCE_ACCOUNT", account.accountId, "ATTENDANCE_SCORE_ADJUSTED", "MEDIUM", beforeStatus, account.status, ledger.reason);
        auditNotificationFailure(actor, account.accountId, ledger.notificationFailure);
        Map<String, Object> value = linkedMap("ledger", ledgerView(ledger, true), "account", accountView(account, true, request), "candidate", candidate == null ? null : candidateView(candidate, true));
        remember(actor.userId(), "adjust:" + accountId, body, value);
        return value;
    }

    synchronized Map<String, Object> reverse(AttendanceUser actor, String ledgerId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        String publicReason = validateRequiredString(body, "publicReason", 1, 200);
        IdempotencyRecord existing = replay(actor.userId(), "reverse:" + ledgerId, body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        AttendanceLedgerRecord original = ledgers.get(ledgerId);
        if (original == null) throw new AttendanceException(404, 45001, "ledger not found");
        if (!"POSTED".equals(original.status) || "INITIAL_GRANT".equals(original.type) || "REVERSAL".equals(original.type)) throw new AttendanceException(409, 45015, "ledger cannot reverse");
        AttendanceAccountRecord account = requireAccount(original.accountId);
        if (Set.of("REMOVED", "ARCHIVED").contains(account.status)) throw new AttendanceException(409, 45013, "account status conflict");
        int before = account.scoreBalance;
        int delta = -original.delta;
        int after = Math.max(0, before + delta);
        failLedger(request);
        AttendanceLedgerRecord reversal = ledger(account, "REVERSAL", delta, before, after, "attendance", ledgerId, original.cycleKey, validateRequiredString(body, "reason", 1, 500), publicReason, actor.userId(), idempotencyKey(body), ledgerId, request);
        ledgers.put(reversal.ledgerId, reversal);
        original.status = "REVERSED";
        original.reversedAt = NOW;
        original.reversedByLedgerId = reversal.ledgerId;
        applyBalance(account, after, delta, reversal.ledgerId);
        audit(actor, "ATTENDANCE_LEDGER", ledgerId, "ATTENDANCE_LEDGER_REVERSED", "MEDIUM", "POSTED", "REVERSED", reversal.reason);
        Map<String, Object> value = linkedMap("reversal", ledgerView(reversal, true), "original", ledgerView(original, true), "account", accountView(account, true, request));
        remember(actor.userId(), "reverse:" + ledgerId, body, value);
        return value;
    }

    synchronized Map<String, Object> createContribution(AttendanceUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        IdempotencyRecord existing = replay(actor.userId(), "contribution:create", body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        String accountId = validateRequiredString(body, "accountId", 1, 80);
        AttendanceAccountRecord account = requireAccount(accountId);
        if (Set.of("REMOVED", "ARCHIVED").contains(account.status)) throw new AttendanceException(409, 45013, "account status conflict");
        String type = validateRequiredString(body, "type", 1, 80);
        if (!CONTRIBUTION_TYPES.contains(type)) throw new AttendanceException(400, 40001, "invalid type");
        String sourceModule = Objects.toString(body.getOrDefault("sourceModule", "manual"));
        if (!Set.of("manual", "attendance").contains(sourceModule)) throw new AttendanceException(400, 40001, "invalid source");
        String sourceId = Objects.toString(body.getOrDefault("sourceId", "src-" + (++idSeq)));
        String sourceKey = accountId + ":" + sourceModule + ":" + sourceId;
        if (sourceContribution.containsKey(sourceKey)) throw new AttendanceException(409, 45019, "contribution source conflict");
        String title = validateRequiredString(body, "title", 2, 80);
        String description = optionalString(body, "description", 1000);
        String occurredAt = validateRequiredString(body, "occurredAt", 1, 40);
        parseTime(occurredAt);
        int scoreDelta = intBody(body, "scoreDelta", Integer.MIN_VALUE);
        if (scoreDelta < 0 || scoreDelta > 1000) throw new AttendanceException(400, 40001, "invalid scoreDelta");
        if (scoreDelta > 0) validateRequiredString(body, "publicReason", 1, 200);
        ContributionRecord contribution = new ContributionRecord();
        contribution.contributionId = "att-contrib-" + (++idSeq);
        contribution.accountId = accountId;
        contribution.memberId = account.memberId;
        contribution.userId = account.userId;
        contribution.type = type;
        contribution.sourceModule = sourceModule;
        contribution.sourceId = sourceId;
        contribution.title = title;
        contribution.description = description;
        contribution.occurredAt = occurredAt;
        contribution.scoreDelta = scoreDelta;
        contribution.operatorUserId = actor.userId();
        contribution.createdAt = NOW;
        contribution.updatedAt = NOW;
        AttendanceLedgerRecord ledger = null;
        if (scoreDelta > 0) {
            failLedger(request);
            int before = account.scoreBalance;
            int after = before + scoreDelta;
            ledger = ledger(account, "CONTRIBUTION_REWARD", scoreDelta, before, after, sourceModule, sourceId, null, validateRequiredString(body, "reason", 1, 500), validateRequiredString(body, "publicReason", 1, 200), actor.userId(), idempotencyKey(body), null, request);
            ledgers.put(ledger.ledgerId, ledger);
            applyBalance(account, after, scoreDelta, ledger.ledgerId);
            contribution.ledgerId = ledger.ledgerId;
        } else {
            account.lastPositiveActivityAt = NOW;
            account.updatedAt = NOW;
        }
        contributions.put(contribution.contributionId, contribution);
        sourceContribution.put(sourceKey, contribution.contributionId);
        audit(actor, "ATTENDANCE_CONTRIBUTION", contribution.contributionId, "ATTENDANCE_CONTRIBUTION_CREATED", "MEDIUM", null, contribution.type, validateRequiredString(body, "reason", 1, 500));
        Map<String, Object> value = linkedMap("contribution", contributionView(contribution, true), "ledger", ledger == null ? null : ledgerView(ledger, true), "account", accountView(account, true, request));
        remember(actor.userId(), "contribution:create", body, value);
        return value;
    }

    synchronized Map<String, Object> correctContribution(AttendanceUser actor, String contributionId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        IdempotencyRecord existing = replay(actor.userId(), "contribution:correct:" + contributionId, body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        ContributionRecord contribution = contributions.get(contributionId);
        if (contribution == null) throw new AttendanceException(404, 45002, "contribution not found");
        if (body.containsKey("title")) contribution.title = validateRequiredString(body, "title", 2, 80);
        if (body.containsKey("description")) contribution.description = optionalString(body, "description", 1000);
        if (body.containsKey("occurredAt")) {
            contribution.occurredAt = validateRequiredString(body, "occurredAt", 1, 40);
            parseTime(contribution.occurredAt);
        }
        contribution.updatedAt = NOW;
        contribution.correctionOfContributionId = contributionId;
        audit(actor, "ATTENDANCE_CONTRIBUTION", contributionId, "ATTENDANCE_CONTRIBUTION_CORRECTED", "MEDIUM", null, contribution.type, validateRequiredString(body, "reason", 1, 500));
        Map<String, Object> value = contributionView(contribution, true);
        remember(actor.userId(), "contribution:correct:" + contributionId, body, value);
        return value;
    }

    Map<String, Object> monthlyPreview(AttendanceUser actor, Map<String, Object> body, HttpServletRequest request) {
        String cycleKey = validateCycleBody(body);
        int deduction = deductionScore(body);
        validateReason(body, 500);
        List<MonthlyItem> items = monthlyItems(cycleKey, deduction);
        audit(actor, "ATTENDANCE_MONTHLY_RUN", cycleKey, "ATTENDANCE_MONTHLY_PREVIEWED", "MEDIUM", null, "PENDING", validateRequiredString(body, "reason", 1, 500));
        return monthlyRunView(previewRun(actor, cycleKey, deduction, validateRequiredString(body, "reason", 1, 500), items), items);
    }

    synchronized MutationResult runMonthly(AttendanceUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord existing = replay(actor.userId(), "monthly", body);
        if (existing != null) return new MutationResult(false, existing.value());
        failBeforeWrite(request);
        String cycleKey = validateCycleBody(body);
        int deduction = deductionScore(body);
        validateReason(body, 500);
        if (!"RUN_MONTHLY_DEDUCTION".equals(Objects.toString(body.get("confirmText"), ""))) throw new AttendanceException(403, 42003, "high risk not confirmed");
        if (monthlyRuns.values().stream().anyMatch(run -> cycleKey.equals(run.cycleKey) && !run.dryRun)) throw new AttendanceException(409, 45016, "cycle already executed");
        List<MonthlyItem> items = monthlyItems(cycleKey, deduction);
        MonthlyRunRecord run = previewRun(actor, cycleKey, deduction, validateRequiredString(body, "reason", 1, 500), items);
        run.runId = "att-run-" + (++idSeq);
        run.dryRun = false;
        run.status = "COMPLETED";
        run.startedAt = NOW;
        run.completedAt = NOW;
        run.idempotencyKey = idempotencyKey(body);
        for (MonthlyItem item : items) {
            if (!item.deduct()) continue;
            AttendanceAccountRecord account = requireAccount(item.accountId());
            int before = account.scoreBalance;
            int after = Math.max(0, before - deduction);
            AttendanceLedgerRecord ledger = ledger(account, "MONTHLY_DEDUCTION", -deduction, before, after, "attendance", run.runId, cycleKey, run.reason, "月度考勤扣分", actor.userId(), idempotencyKey(body), null, request);
            ledgers.put(ledger.ledgerId, ledger);
            applyBalance(account, after, -deduction, ledger.ledgerId);
            ensureCandidateIfNeeded(account, cycleKey, actor, "月度扣分后进入复核", "积分归零");
        }
        monthlyRuns.put(run.runId, run);
        audit(actor, "ATTENDANCE_MONTHLY_RUN", run.runId, "ATTENDANCE_MONTHLY_RUN_EXECUTED", "HIGH", "PENDING", run.status, run.reason);
        Map<String, Object> value = monthlyRunView(run, items);
        remember(actor.userId(), "monthly", body, value);
        return new MutationResult(true, value);
    }

    Map<String, Object> monthlyRun(String runId, HttpServletRequest request) {
        MonthlyRunRecord run = monthlyRuns.get(runId);
        if (run == null) throw new AttendanceException(404, 45003, "monthly run not found");
        return monthlyRunView(run, List.of());
    }

    Map<String, Object> removalCandidates(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", CANDIDATE_STATUSES);
        String cycleKey = cycleKey(query, false);
        String sort = sort(query, Set.of("createdAt_desc", "updatedAt_desc", "score_asc"), "createdAt_desc");
        String keyword = lower(query.get("keyword"));
        List<Map<String, Object>> rows = candidates.values().stream()
                .filter(candidate -> status == null || status.equals(candidate.status))
                .filter(candidate -> cycleKey == null || cycleKey.equals(candidate.cycleKey))
                .filter(candidate -> keyword == null || candidate.candidateId.toLowerCase().contains(keyword) || candidate.accountId.toLowerCase().contains(keyword) || candidate.memberId.toLowerCase().contains(keyword) || candidate.displayNameSnapshot.toLowerCase().contains(keyword))
                .sorted(candidateComparator(sort))
                .map(candidate -> candidateView(candidate, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> confirmCandidate(AttendanceUser actor, String candidateId, Map<String, Object> body, HttpServletRequest request) {
        if (!"CONFIRM_REMOVAL_CANDIDATE".equals(Objects.toString(body.get("confirmText"), ""))) throw new AttendanceException(403, 42003, "high risk not confirmed");
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        IdempotencyRecord existing = replay(actor.userId(), "candidate:confirm:" + candidateId, body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        RemovalCandidateRecord candidate = requireCandidate(candidateId);
        if (!"OPEN".equals(candidate.status)) throw new AttendanceException(409, 45018, "candidate status conflict");
        candidate.status = "CONFIRMED";
        candidate.confirmedBy = actor.userId();
        candidate.confirmedAt = NOW;
        candidate.updatedAt = NOW;
        AttendanceAccountRecord account = requireAccount(candidate.accountId);
        account.status = "REMOVAL_CANDIDATE";
        audit(actor, "ATTENDANCE_REMOVAL_CANDIDATE", candidateId, "ATTENDANCE_REMOVAL_CANDIDATE_CONFIRMED", "HIGH", "OPEN", "CONFIRMED", validateRequiredString(body, "reason", 1, 500));
        Map<String, Object> value = linkedMap("candidate", candidateView(candidate, true), "account", accountView(account, true, request));
        remember(actor.userId(), "candidate:confirm:" + candidateId, body, value);
        return value;
    }

    synchronized Map<String, Object> dismissCandidate(AttendanceUser actor, String candidateId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        IdempotencyRecord existing = replay(actor.userId(), "candidate:dismiss:" + candidateId, body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        RemovalCandidateRecord candidate = requireCandidate(candidateId);
        if (!"OPEN".equals(candidate.status)) throw new AttendanceException(409, 45018, "candidate status conflict");
        candidate.status = "DISMISSED";
        candidate.dismissedBy = actor.userId();
        candidate.dismissedAt = NOW;
        candidate.dismissReason = validateRequiredString(body, "reason", 1, 500);
        candidate.updatedAt = NOW;
        audit(actor, "ATTENDANCE_REMOVAL_CANDIDATE", candidateId, "ATTENDANCE_REMOVAL_CANDIDATE_DISMISSED", "MEDIUM", "OPEN", "DISMISSED", candidate.dismissReason);
        Map<String, Object> value = linkedMap("candidate", candidateView(candidate, true), "account", accountView(requireAccount(candidate.accountId), true, request));
        remember(actor.userId(), "candidate:dismiss:" + candidateId, body, value);
        return value;
    }

    Map<String, Object> rebuildLeaderboard(AttendanceUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        IdempotencyRecord existing = replay(actor.userId(), "leaderboard", body);
        if (existing != null) return existing.value();
        failBeforeWrite(request);
        Map<String, String> filters = new LinkedHashMap<>();
        if (body.containsKey("cycleKey")) {
            String cycle = Objects.toString(body.get("cycleKey"), "");
            if (!cycle.matches("\\d{4}-\\d{2}")) throw new AttendanceException(400, 40001, "invalid cycleKey");
            filters.put("cycleKey", cycle);
        }
        List<Map<String, Object>> entries = leaderboardEntries(filters, request);
        Map<String, Object> value = linkedMap("rebuiltAt", NOW, "entriesTotal", entries.size(), "items", entries.subList(0, Math.min(20, entries.size())));
        audit(actor, "ATTENDANCE_LEADERBOARD", "current", "ATTENDANCE_LEADERBOARD_REBUILT", "MEDIUM", null, "REBUILT", validateRequiredString(body, "reason", 1, 500));
        remember(actor.userId(), "leaderboard", body, value);
        return value;
    }

    Map<String, Object> auditLogs(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = sort(query, Set.of("createdAt_desc", "createdAt_asc"), "createdAt_desc");
        if (query.containsKey("result") && !Set.of("SUCCESS", "FAILED").contains(query.get("result"))) throw new AttendanceException(400, 40001, "invalid result");
        validateTimeRange(query);
        String accountId = query.get("accountId");
        String memberId = query.get("memberId");
        String actorUserId = query.get("actorUserId");
        String action = query.get("action");
        String result = query.get("result");
        List<Map<String, Object>> rows = audits.stream()
                .filter(audit -> accountId == null || accountId.equals(audit.get("accountId")) || accountId.equals(audit.get("targetId")))
                .filter(audit -> memberId == null || memberId.equals(audit.get("memberId")))
                .filter(audit -> actorUserId == null || actorUserId.equals(audit.get("actorUserId")))
                .filter(audit -> action == null || action.equals(audit.get("action")))
                .filter(audit -> result == null || result.equals(audit.get("result")))
                .sorted("createdAt_asc".equals(sort) ? Comparator.comparing(row -> row.get("createdAt").toString()) : Comparator.comparing((Map<String, Object> row) -> row.get("createdAt").toString()).reversed())
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> opsSummary(HttpServletRequest request) {
        long active = accounts.values().stream().filter(account -> "ACTIVE".equals(account.status)).count();
        long openCandidates = candidates.values().stream().filter(candidate -> "OPEN".equals(candidate.status)).count();
        return linkedMap("service", "attendance", "port", 8111, "storageMode", "IN_MEMORY", "authMode", "TEST_STUB", "whitelistMode", "TEST_STUB", "profileMode", "TEST_STUB", "notificationMode", "TEST_STUB", "testControlsEnabled", testControls.enabled(), "accountsTotal", accounts.size(), "activeAccountsTotal", active, "removalCandidatesOpenTotal", openCandidates, "monthlyRunsTotal", monthlyRuns.size(), "ledgerEntriesTotal", ledgers.size(), "contributionsTotal", contributions.size(), "auditsTotal", audits.size(), "idempotencyRecordsTotal", idempotency.size(), "lastMonthlyRunAt", monthlyRuns.isEmpty() ? null : NOW, "lastAuditAt", audits.isEmpty() ? null : NOW, "productionGaps", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB", "P0_WHITELIST_STUB", "P0_PROFILE_STUB", "P0_NOTIFICATION_STUB", testControls.enabled() ? "TEST_CONTROLS_ENABLED_FOR_LOCAL_TEST" : "TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "REAL_ACTIVITY_EVENTS_NOT_CONNECTED", "REAL_ONLINE_TIME_NOT_CONNECTED", "WHITELIST_REMOVAL_NOT_CONNECTED"));
    }

    private Handoff handoff(Map<String, Object> body, HttpServletRequest request) {
        switch (testHeader(request, "X-Test-Whitelist-Mode")) {
            case "unavailable" -> throw new AttendanceException(502, 48010, "whitelist handoff unavailable");
            case "timeout" -> throw new AttendanceException(504, 48011, "whitelist handoff timeout");
            case "bad-schema" -> throw new AttendanceException(502, 48012, "whitelist handoff incompatible");
            default -> {
            }
        }
        String applicationId = string(body.get("applicationId"));
        String handoffId = string(body.get("handoffId"));
        if ((applicationId == null || applicationId.isBlank()) && (handoffId == null || handoffId.isBlank())) throw new AttendanceException(400, 40001, "applicationId or handoffId required");
        if (applicationId == null || applicationId.isBlank()) applicationId = handoffId.replace("att-", "");
        if (handoffId == null || handoffId.isBlank()) handoffId = "att-" + applicationId;
        if ("wl-bad-status".equals(applicationId)) throw new AttendanceException(409, 45010, "handoff status conflict");
        if ("wl-consumed".equals(applicationId)) throw new AttendanceException(409, 45011, "handoff consumed");
        if (!Set.of("wl-app-1", "wl-app-2").contains(applicationId)) {
            throw new AttendanceException(502, 48010, "whitelist handoff unavailable");
        }
        String suffix = applicationId.endsWith("2") ? "2" : "1";
        return new Handoff(handoffId, applicationId, "member-user-" + suffix, "member-" + suffix, minecraft("MemberMC" + suffix, "uuid-" + suffix), "GENERAL", "FIRST_TIME", 1, NOW, score());
    }

    private void profileCheck(String memberId, HttpServletRequest request) {
        switch (testHeader(request, "X-Test-Profile-Mode")) {
            case "unavailable" -> throw new AttendanceException(502, 48020, "profile unavailable");
            case "timeout" -> throw new AttendanceException(504, 48021, "profile timeout");
            case "bad-schema" -> throw new AttendanceException(502, 48022, "profile incompatible");
            default -> {
            }
        }
    }

    private List<Map<String, Object>> leaderboardEntries(Map<String, String> filters, HttpServletRequest request) {
        String sort = filters.getOrDefault("sort", "score_desc");
        String group = filters.get("memberGroup");
        boolean stale = "unavailable".equals(testHeader(request, "X-Test-Profile-Mode"));
        Comparator<AttendanceAccountRecord> comparator = switch (sort) {
            case "earned_desc" -> Comparator.comparingInt((AttendanceAccountRecord account) -> account.totalEarned).reversed();
            case "lastActivity_desc" -> Comparator.comparing((AttendanceAccountRecord account) -> Objects.toString(account.lastPositiveActivityAt, "")).reversed();
            default -> Comparator.comparingInt((AttendanceAccountRecord account) -> account.scoreBalance).reversed();
        };
        List<AttendanceAccountRecord> ranked = accounts.values().stream()
                .filter(account -> Set.of("ACTIVE", "REMOVAL_CANDIDATE").contains(account.status))
                .filter(account -> group == null || Objects.equals(group, account.memberGroupSnapshot))
                .sorted(comparator.thenComparing(account -> account.accountId))
                .toList();
        List<Map<String, Object>> entries = new ArrayList<>();
        int rank = 1;
        for (AttendanceAccountRecord account : ranked) {
            entries.add(linkedMap("rank", rank++, "accountId", account.accountId, "memberId", account.memberId, "displayNameSnapshot", account.displayNameSnapshot, "avatarUrlSnapshot", account.avatarUrlSnapshot, "memberGroupSnapshot", account.memberGroupSnapshot, "scoreBalance", account.scoreBalance, "totalEarned", account.totalEarned, "lastPositiveActivityAt", account.lastPositiveActivityAt, "profileSnapshotStale", stale || account.profileSnapshotStale, "generatedAt", NOW));
        }
        return entries;
    }

    private AttendanceLedgerRecord ledger(AttendanceAccountRecord account, String type, int delta, int before, int after, String sourceModule, String sourceId, String cycleKey, String reason, String publicReason, String operatorUserId, String idempotencyKey, String reversalOf, HttpServletRequest request) {
        AttendanceLedgerRecord ledger = new AttendanceLedgerRecord();
        ledger.ledgerId = "att-ledger-" + (++idSeq);
        ledger.accountId = account.accountId;
        ledger.memberId = account.memberId;
        ledger.userId = account.userId;
        ledger.type = type;
        ledger.status = "POSTED";
        ledger.delta = delta;
        ledger.balanceBefore = before;
        ledger.balanceAfter = after;
        ledger.sourceModule = sourceModule;
        ledger.sourceId = sourceId;
        ledger.cycleKey = cycleKey;
        ledger.reason = reason;
        ledger.publicReason = publicReason;
        ledger.operatorUserId = operatorUserId;
        ledger.idempotencyKey = idempotencyKey;
        ledger.reversalOfLedgerId = reversalOf;
        ledger.notificationStatus = notificationStatus(request);
        ledger.notificationFailure = notificationFailure(request);
        ledger.createdAt = NOW;
        return ledger;
    }

    private void applyBalance(AttendanceAccountRecord account, int after, int delta, String ledgerId) {
        account.scoreBalance = after;
        if (delta > 0) {
            account.totalEarned += delta;
            account.lastPositiveActivityAt = NOW;
            if ("REMOVAL_CANDIDATE".equals(account.status) && after > 0) account.status = "ACTIVE";
        } else if (delta < 0) {
            account.totalDeducted += Math.abs(delta);
            account.lastDeductedAt = NOW;
            if (after <= 0) account.status = "REMOVAL_CANDIDATE";
        }
        account.lastLedgerId = ledgerId;
        account.updatedAt = NOW;
    }

    private RemovalCandidateRecord ensureCandidateIfNeeded(AttendanceAccountRecord account, String cycleKey, AttendanceUser actor, String publicReason, String reason) {
        if (account.scoreBalance > 0) return null;
        for (RemovalCandidateRecord candidate : candidates.values()) {
            if (candidate.accountId.equals(account.accountId) && "OPEN".equals(candidate.status)) return candidate;
        }
        RemovalCandidateRecord candidate = new RemovalCandidateRecord();
        candidate.candidateId = "att-candidate-" + (++idSeq);
        candidate.accountId = account.accountId;
        candidate.memberId = account.memberId;
        candidate.userId = account.userId;
        candidate.displayNameSnapshot = account.displayNameSnapshot;
        candidate.scoreBalance = account.scoreBalance;
        candidate.cycleKey = cycleKey;
        candidate.status = "OPEN";
        candidate.reason = reason;
        candidate.publicReason = publicReason;
        candidate.recommendedAction = "WHITELIST_REVIEW_REQUIRED";
        candidate.createdAt = NOW;
        candidate.updatedAt = NOW;
        candidates.put(candidate.candidateId, candidate);
        audit(actor, "ATTENDANCE_REMOVAL_CANDIDATE", candidate.candidateId, "ATTENDANCE_REMOVAL_CANDIDATE_CREATED", "MEDIUM", null, "OPEN", reason);
        return candidate;
    }

    private List<MonthlyItem> monthlyItems(String cycleKey, int deduction) {
        return accounts.values().stream()
                .filter(account -> Set.of("ACTIVE", "REMOVAL_CANDIDATE").contains(account.status))
                .sorted(Comparator.comparing(account -> account.accountId))
                .map(account -> new MonthlyItem(account.accountId, noContributionInCycle(account.accountId, cycleKey), Math.max(0, account.scoreBalance - deduction)))
                .toList();
    }

    private boolean noContributionInCycle(String accountId, String cycleKey) {
        return contributions.values().stream()
                .noneMatch(contribution -> contribution.accountId.equals(accountId) && contribution.occurredAt.startsWith(cycleKey));
    }

    private MonthlyRunRecord previewRun(AttendanceUser actor, String cycleKey, int deduction, String reason, List<MonthlyItem> items) {
        MonthlyRunRecord run = new MonthlyRunRecord();
        run.runId = "att-run-preview-" + (++idSeq);
        run.cycleKey = cycleKey;
        run.status = "PENDING";
        run.dryRun = true;
        run.reason = reason;
        run.deductionScore = deduction;
        run.eligibleAccounts = items.size();
        run.deductedAccounts = (int) items.stream().filter(MonthlyItem::deduct).count();
        run.skippedAccounts = run.eligibleAccounts - run.deductedAccounts;
        run.candidateCreated = (int) items.stream().filter(item -> item.deduct() && item.balanceAfter() <= 0).count();
        run.createdBy = actor.userId();
        run.createdAt = NOW;
        return run;
    }

    private Map<String, Object> monthlyRunView(MonthlyRunRecord run, List<MonthlyItem> items) {
        List<Map<String, Object>> preview = items.stream().map(item -> linkedMap("accountId", item.accountId(), "deduct", item.deduct(), "balanceAfter", item.balanceAfter())).toList();
        return linkedMap("runId", run.runId, "cycleKey", run.cycleKey, "status", run.status, "dryRun", run.dryRun, "reason", run.reason, "deductionScore", run.deductionScore, "eligibleAccounts", run.eligibleAccounts, "deductedAccounts", run.deductedAccounts, "skippedAccounts", run.skippedAccounts, "candidateCreated", run.candidateCreated, "previewItems", preview, "idempotencyKey", run.idempotencyKey, "startedAt", run.startedAt, "completedAt", run.completedAt, "failureReason", run.failureReason, "createdBy", run.createdBy, "createdAt", run.createdAt);
    }

    private List<Map<String, Object>> recentLedger(String accountId) {
        return ledgers.values().stream().filter(ledger -> ledger.accountId.equals(accountId)).sorted(ledgerComparator("createdAt_desc")).limit(5).map(ledger -> ledgerView(ledger, true)).toList();
    }

    private List<Map<String, Object>> recentContributions(String accountId) {
        return contributions.values().stream().filter(contribution -> contribution.accountId.equals(accountId)).sorted(Comparator.comparing((ContributionRecord item) -> item.createdAt).reversed()).limit(5).map(item -> contributionView(item, true)).toList();
    }

    private Map<String, Object> openCandidateView(String accountId) {
        return candidates.values().stream().filter(candidate -> candidate.accountId.equals(accountId) && "OPEN".equals(candidate.status)).findFirst().map(candidate -> candidateView(candidate, true)).orElse(null);
    }

    private Map<String, Object> initializationView(AttendanceAccountRecord account, AttendanceLedgerRecord ledger, Map<String, Object> handoff) {
        return linkedMap("account", accountView(account, true, null), "ledger", ledgerView(ledger, true), "handoff", handoff);
    }

    private Map<String, Object> accountView(AttendanceAccountRecord account, boolean admin, HttpServletRequest request) {
        boolean stale = account.profileSnapshotStale || "unavailable".equals(testHeader(request, "X-Test-Profile-Mode"));
        Map<String, Object> row = linkedMap("accountId", account.accountId, "userId", account.userId, "memberId", account.memberId, "displayNameSnapshot", account.displayNameSnapshot, "avatarUrlSnapshot", account.avatarUrlSnapshot, "memberGroupSnapshot", account.memberGroupSnapshot, "memberStatusSnapshot", account.memberStatusSnapshot, "minecraftBindingSnapshot", account.minecraftBindingSnapshot, "status", account.status, "scoreBalance", account.scoreBalance, "initialScore", account.initialScore, "totalEarned", account.totalEarned, "totalDeducted", account.totalDeducted, "lastPositiveActivityAt", account.lastPositiveActivityAt, "lastDeductedAt", account.lastDeductedAt, "lastLedgerId", account.lastLedgerId, "whitelistApplicationId", account.whitelistApplicationId, "whitelistHandoffId", account.whitelistHandoffId, "whitelistHandoffVersion", account.whitelistHandoffVersion, "reviewDirection", account.reviewDirection, "attemptType", account.attemptType, "notificationStatus", account.notificationStatus, "notificationFailure", admin ? account.notificationFailure : null, "profileSnapshotStale", stale, "createdAt", account.createdAt, "updatedAt", account.updatedAt, "archivedAt", account.archivedAt);
        if (!admin) row.remove("notificationFailure");
        return row;
    }

    private Map<String, Object> ledgerView(AttendanceLedgerRecord ledger, boolean admin) {
        Map<String, Object> row = linkedMap("ledgerId", ledger.ledgerId, "accountId", ledger.accountId, "memberId", ledger.memberId, "userId", ledger.userId, "type", ledger.type, "status", ledger.status, "delta", ledger.delta, "balanceBefore", ledger.balanceBefore, "balanceAfter", ledger.balanceAfter, "sourceModule", ledger.sourceModule, "sourceId", ledger.sourceId, "cycleKey", ledger.cycleKey, "reason", admin ? ledger.reason : null, "publicReason", ledger.publicReason, "operatorUserId", admin ? ledger.operatorUserId : null, "idempotencyKey", admin ? ledger.idempotencyKey : null, "reversalOfLedgerId", ledger.reversalOfLedgerId, "reversedByLedgerId", ledger.reversedByLedgerId, "notificationStatus", ledger.notificationStatus, "notificationFailure", admin ? ledger.notificationFailure : null, "createdAt", ledger.createdAt, "reversedAt", ledger.reversedAt);
        if (!admin) {
            row.remove("reason");
            row.remove("operatorUserId");
            row.remove("idempotencyKey");
            row.remove("notificationFailure");
        }
        return row;
    }

    private Map<String, Object> contributionView(ContributionRecord item, boolean admin) {
        Map<String, Object> row = linkedMap("contributionId", item.contributionId, "accountId", item.accountId, "memberId", item.memberId, "userId", item.userId, "type", item.type, "sourceModule", item.sourceModule, "sourceId", item.sourceId, "title", item.title, "description", item.description, "occurredAt", item.occurredAt, "scoreDelta", item.scoreDelta, "ledgerId", item.ledgerId, "operatorUserId", admin ? item.operatorUserId : null, "correctionOfContributionId", item.correctionOfContributionId, "createdAt", item.createdAt, "updatedAt", item.updatedAt);
        if (!admin) row.remove("operatorUserId");
        return row;
    }

    private Map<String, Object> candidateView(RemovalCandidateRecord candidate, boolean admin) {
        return linkedMap("candidateId", candidate.candidateId, "accountId", candidate.accountId, "memberId", candidate.memberId, "userId", candidate.userId, "displayNameSnapshot", candidate.displayNameSnapshot, "scoreBalance", candidate.scoreBalance, "cycleKey", candidate.cycleKey, "status", candidate.status, "reason", admin ? candidate.reason : null, "publicReason", candidate.publicReason, "recommendedAction", candidate.recommendedAction, "confirmedBy", admin ? candidate.confirmedBy : null, "confirmedAt", candidate.confirmedAt, "dismissedBy", admin ? candidate.dismissedBy : null, "dismissedAt", candidate.dismissedAt, "dismissReason", admin ? candidate.dismissReason : null, "createdAt", candidate.createdAt, "updatedAt", candidate.updatedAt);
    }

    private Map<String, Object> handoffView(Handoff handoff) {
        return linkedMap("handoffId", handoff.handoffId(), "applicationId", handoff.applicationId(), "userId", handoff.userId(), "memberId", handoff.memberId(), "minecraftBindingSnapshot", handoff.minecraftBinding(), "reviewDirection", handoff.reviewDirection(), "attemptType", handoff.attemptType(), "approvedAt", handoff.approvedAt(), "scoreSummary", handoff.scoreSummary(), "initializationStatus", "WAITING_MODULE", "handoffVersion", handoff.handoffVersion(), "generatedAt", NOW, "consumedAt", null);
    }

    private Map<String, Object> dependencyStatus(HttpServletRequest request) {
        return linkedMap("auth", "OK", "profile", "unavailable".equals(testHeader(request, "X-Test-Profile-Mode")) ? "DEGRADED" : "OK", "notification", "OK");
    }

    private void audit(AttendanceUser actor, String targetType, String targetId, String action, String risk, String before, String after, String reason) {
        audits.add(linkedMap("id", "att-audit-" + (++idSeq), "requestId", AttendanceController.requestId(), "actorUserId", actor.userId(), "actorRole", actor.roles().iterator().next(), "actorPermissions", List.of(), "sourceIp", "127.0.0.1", "targetType", targetType, "targetId", targetId, "action", action, "riskLevel", risk, "reason", reason, "paramsSummary", "summary", "beforeState", before, "afterState", after, "result", "SUCCESS", "failureReason", null, "createdAt", NOW));
    }

    private void auditNotificationFailure(AttendanceUser actor, String targetId, Map<String, Object> failure) {
        if (failure != null) {
            audit(actor, "ATTENDANCE_NOTIFICATION", targetId, "ATTENDANCE_NOTIFICATION_FAILED", "LOW", null, "FAILED", Objects.toString(failure.get("failureType")) + ":" + Objects.toString(failure.get("failureCode")));
        }
    }

    private String notificationStatus(HttpServletRequest request) {
        return notificationFailure(request) == null ? "DELIVERED" : "FAILED";
    }

    private Map<String, Object> notificationFailure(HttpServletRequest request) {
        return switch (testHeader(request, "X-Test-Notification-Mode")) {
            case "unavailable" -> linkedMap("status", "FAILED", "failureCode", "48030", "failureType", "UNAVAILABLE", "failureReason", "notification unavailable", "failedAt", NOW);
            case "timeout" -> linkedMap("status", "FAILED", "failureCode", "48031", "failureType", "TIMEOUT", "failureReason", "notification timeout", "failedAt", NOW);
            case "bad-schema" -> linkedMap("status", "FAILED", "failureCode", "48032", "failureType", "BAD_SCHEMA", "failureReason", "notification response incompatible", "failedAt", NOW);
            default -> null;
        };
    }

    private AttendanceAccountRecord requireAccount(String accountId) {
        AttendanceAccountRecord account = accounts.get(accountId);
        if (account == null) throw new AttendanceException(404, 45000, "attendance account not found");
        return account;
    }

    private RemovalCandidateRecord requireCandidate(String candidateId) {
        RemovalCandidateRecord candidate = candidates.get(candidateId);
        if (candidate == null) throw new AttendanceException(404, 45004, "candidate not found");
        return candidate;
    }

    private AttendanceLedgerRecord firstLedger(String accountId) {
        return ledgers.values().stream().filter(ledger -> ledger.accountId.equals(accountId) && "INITIAL_GRANT".equals(ledger.type)).findFirst().orElseThrow();
    }

    private Comparator<AttendanceAccountRecord> accountComparator(String sort) {
        return switch (sort) {
            case "updatedAt_desc" -> Comparator.comparing((AttendanceAccountRecord account) -> account.updatedAt).reversed();
            case "score_desc" -> Comparator.comparingInt((AttendanceAccountRecord account) -> account.scoreBalance).reversed();
            case "score_asc" -> Comparator.comparingInt(account -> account.scoreBalance);
            case "lastActivity_desc" -> Comparator.comparing((AttendanceAccountRecord account) -> Objects.toString(account.lastPositiveActivityAt, "")).reversed();
            default -> Comparator.comparing((AttendanceAccountRecord account) -> account.createdAt).reversed();
        };
    }

    private Comparator<AttendanceLedgerRecord> ledgerComparator(String sort) {
        Comparator<AttendanceLedgerRecord> comparator = Comparator.comparing(ledger -> ledger.createdAt);
        if ("createdAt_desc".equals(sort)) comparator = comparator.reversed();
        return comparator.thenComparing(ledger -> ledger.ledgerId);
    }

    private Comparator<RemovalCandidateRecord> candidateComparator(String sort) {
        return switch (sort) {
            case "updatedAt_desc" -> Comparator.comparing((RemovalCandidateRecord candidate) -> candidate.updatedAt).reversed();
            case "score_asc" -> Comparator.comparingInt(candidate -> candidate.scoreBalance);
            default -> Comparator.comparing((RemovalCandidateRecord candidate) -> candidate.createdAt).reversed();
        };
    }

    private boolean matches(AttendanceAccountRecord account, String keyword) {
        String mc = account.minecraftBindingSnapshot == null ? "" : Objects.toString(account.minecraftBindingSnapshot.get("minecraftId"), "");
        return account.accountId.toLowerCase().contains(keyword) || account.userId.toLowerCase().contains(keyword) || account.memberId.toLowerCase().contains(keyword) || account.displayNameSnapshot.toLowerCase().contains(keyword) || mc.toLowerCase().contains(keyword);
    }

    private Map<String, Object> pageRows(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return linkedMap("items", new ArrayList<>(rows.subList(from, to)), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private int page(Map<String, String> query) {
        return intQuery(query, "page", 1, 1, Integer.MAX_VALUE, 40002);
    }

    private int pageSize(Map<String, String> query) {
        return intQuery(query, "pageSize", 20, 1, 100, 40002);
    }

    private int intQuery(Map<String, String> query, String key, int fallback, int min, int max, int code) {
        if (!query.containsKey(key)) return fallback;
        try {
            int value = Integer.parseInt(query.get(key));
            if (value < min || value > max) throw new AttendanceException(400, code, "invalid number");
            return value;
        } catch (NumberFormatException ex) {
            throw new AttendanceException(400, code, "invalid number");
        }
    }

    private String sort(Map<String, String> query, Set<String> allowed, String fallback) {
        String value = query.getOrDefault("sort", fallback);
        if (!allowed.contains(value)) throw new AttendanceException(400, 40003, "invalid sort");
        return value;
    }

    private String enumQuery(Map<String, String> query, String key, Set<String> allowed) {
        if (!query.containsKey(key)) return null;
        String value = query.get(key);
        if (!allowed.contains(value)) throw new AttendanceException(400, 40001, "invalid " + key);
        return value;
    }

    private String cycleKey(Map<String, String> query, boolean required) {
        String value = query.get("cycleKey");
        if ((value == null || value.isBlank()) && !required) return null;
        if (value == null || !value.matches("\\d{4}-\\d{2}")) throw new AttendanceException(400, 40001, "invalid cycleKey");
        return value;
    }

    private String validateCycleBody(Map<String, Object> body) {
        String value = string(body.get("cycleKey"));
        if (value == null || !value.matches("\\d{4}-\\d{2}")) throw new AttendanceException(400, 40001, "invalid cycleKey");
        return value;
    }

    private int deductionScore(Map<String, Object> body) {
        int value = body.containsKey("deductionScore") ? intBody(body, "deductionScore", 20) : 20;
        if (value < 1 || value > 100) throw new AttendanceException(400, 40001, "invalid deductionScore");
        return value;
    }

    private void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.containsKey("from") ? Instant.parse(query.get("from")) : null;
            Instant to = query.containsKey("to") ? Instant.parse(query.get("to")) : null;
            if (from != null && to != null && from.isAfter(to)) throw new AttendanceException(400, 40001, "invalid time range");
        } catch (DateTimeParseException ex) {
            throw new AttendanceException(400, 40001, "invalid time");
        }
    }

    private void parseTime(String value) {
        try {
            Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new AttendanceException(400, 40001, "invalid time");
        }
    }

    private void validateReason(Map<String, Object> body, int max) {
        validateRequiredString(body, "reason", 1, max);
    }

    private void validateIdempotencyKey(Map<String, Object> body) {
        if (!body.containsKey("idempotencyKey")) return;
        String value = string(body.get("idempotencyKey"));
        if (value == null || value.length() < 8 || value.length() > 80) throw new AttendanceException(400, 40001, "invalid idempotency key");
    }

    private String idempotencyKey(Map<String, Object> body) {
        Object value = body.get("idempotencyKey");
        return value == null ? null : value.toString();
    }

    private IdempotencyRecord replay(String actorId, String operation, Map<String, Object> body) {
        String key = idempotencyKey(body);
        if (key == null) return null;
        IdempotencyRecord existing = idempotency.get(actorId + ":" + operation + ":" + key);
        if (existing != null && !existing.fingerprint().equals(canonical(body))) throw new AttendanceException(409, 45017, "idempotency conflict");
        return existing;
    }

    private void remember(String actorId, String operation, Map<String, Object> body, Map<String, Object> value) {
        String key = idempotencyKey(body);
        if (key != null) idempotency.put(actorId + ":" + operation + ":" + key, new IdempotencyRecord(canonical(body), value));
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

    private void failBeforeWrite(HttpServletRequest request) {
        if ("true".equals(testHeader(request, "X-Test-Fail-Audit"))) throw new AttendanceException(500, 53001, "attendance audit failed");
        if ("true".equals(testHeader(request, "X-Test-Fail-Store"))) throw new AttendanceException(500, 53002, "attendance state failed");
    }

    private void failLedger(HttpServletRequest request) {
        if ("true".equals(testHeader(request, "X-Test-Fail-Ledger"))) throw new AttendanceException(500, 53003, "attendance ledger failed");
    }

    private String testHeader(HttpServletRequest request, String name) {
        return testControls.enabled() && request != null ? Objects.toString(request.getHeader(name), "") : "";
    }

    private static String validateRequiredString(Map<String, Object> body, String field, int min, int max) {
        String value = string(body.get(field));
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) throw new AttendanceException(400, 40001, "invalid " + field);
        return value;
    }

    private static String optionalString(Map<String, Object> body, String field, int max) {
        String value = string(body.get(field));
        if (value == null) return null;
        if (value.length() > max) throw new AttendanceException(400, 40001, "invalid " + field);
        return value;
    }

    private static int intBody(Map<String, Object> body, String field, int fallback) {
        if (!body.containsKey(field)) return fallback;
        Object value = body.get(field);
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException ex) {
            throw new AttendanceException(400, 40001, "invalid " + field);
        }
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase();
    }

    private static Map<String, Object> minecraft(String id, String uuid) {
        return linkedMap("minecraftId", id, "minecraftUuid", uuid, "verifiedAt", NOW, "source", "WHITELIST_HANDOFF");
    }

    private static Map<String, Object> score() {
        return linkedMap("objectiveScore", 20, "manualScore", 30, "totalScore", 50, "finalPassed", true, "scoredAt", NOW);
    }

    static Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }
}

class TestAttendanceAuthProvider {
    AttendanceUser requireUser(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new AttendanceException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new AttendanceException(401, 41003, "bad token format");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new AttendanceException(502, 48000, "auth unavailable");
            case "auth-timeout-token" -> throw new AttendanceException(504, 48001, "auth timeout");
            case "auth-bad-token" -> throw new AttendanceException(502, 48002, "auth incompatible");
            case "owner-token" -> new AttendanceUser("owner", "Owner", Set.of("OWNER"), "ACTIVE");
            case "admin-token" -> new AttendanceUser("admin", "Admin", Set.of("ADMIN"), "ACTIVE");
            case "helper-token" -> new AttendanceUser("helper", "Helper", Set.of("HELPER"), "ACTIVE");
            case "user-token" -> new AttendanceUser("user", "User", Set.of("USER"), "ACTIVE");
            case "member-user-1-token" -> new AttendanceUser("member-user-1", "Member One", Set.of("USER"), "ACTIVE");
            case "member-user-2-token" -> new AttendanceUser("member-user-2", "Member Two", Set.of("USER"), "ACTIVE");
            case "other-token" -> new AttendanceUser("other", "Other", Set.of("USER"), "ACTIVE");
            default -> throw new AttendanceException(401, 41001, "invalid session");
        };
    }

    AttendanceUser requireAny(String authorization, String... roles) {
        AttendanceUser user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new AttendanceException(403, 42001, "role permission denied");
        return user;
    }
}

record AttendanceUser(String userId, String displayName, Set<String> roles, String status) {
}

record Handoff(String handoffId, String applicationId, String userId, String memberId, Map<String, Object> minecraftBinding,
               String reviewDirection, String attemptType, int handoffVersion, String approvedAt, Map<String, Object> scoreSummary) {
}

record MonthlyItem(String accountId, boolean deduct, int balanceAfter) {
}

record IdempotencyRecord(String fingerprint, Map<String, Object> value) {
}

record MutationResult(boolean created, Map<String, Object> value) {
}

record AttendanceTestControls(boolean enabled) {
}

class AttendanceAccountRecord {
    String accountId;
    String userId;
    String memberId;
    String displayNameSnapshot;
    String avatarUrlSnapshot;
    String memberGroupSnapshot;
    String memberStatusSnapshot;
    Map<String, Object> minecraftBindingSnapshot;
    String status;
    int scoreBalance;
    int initialScore;
    int totalEarned;
    int totalDeducted;
    String lastPositiveActivityAt;
    String lastDeductedAt;
    String lastLedgerId;
    String whitelistApplicationId;
    String whitelistHandoffId;
    int whitelistHandoffVersion;
    String reviewDirection;
    String attemptType;
    String notificationStatus;
    Map<String, Object> notificationFailure;
    boolean profileSnapshotStale;
    String createdAt;
    String updatedAt;
    String archivedAt;
}

class AttendanceLedgerRecord {
    String ledgerId;
    String accountId;
    String memberId;
    String userId;
    String type;
    String status;
    int delta;
    int balanceBefore;
    int balanceAfter;
    String sourceModule;
    String sourceId;
    String cycleKey;
    String reason;
    String publicReason;
    String operatorUserId;
    String idempotencyKey;
    String reversalOfLedgerId;
    String reversedByLedgerId;
    String notificationStatus;
    Map<String, Object> notificationFailure;
    String createdAt;
    String reversedAt;
}

class ContributionRecord {
    String contributionId;
    String accountId;
    String memberId;
    String userId;
    String type;
    String sourceModule;
    String sourceId;
    String title;
    String description;
    String occurredAt;
    int scoreDelta;
    String ledgerId;
    String operatorUserId;
    String correctionOfContributionId;
    String createdAt;
    String updatedAt;
}

class MonthlyRunRecord {
    String runId;
    String cycleKey;
    String status;
    boolean dryRun;
    String reason;
    int deductionScore;
    int eligibleAccounts;
    int deductedAccounts;
    int skippedAccounts;
    int candidateCreated;
    String idempotencyKey;
    String startedAt;
    String completedAt;
    String failureReason;
    String createdBy;
    String createdAt;
}

class RemovalCandidateRecord {
    String candidateId;
    String accountId;
    String memberId;
    String userId;
    String displayNameSnapshot;
    int scoreBalance;
    String cycleKey;
    String status;
    String reason;
    String publicReason;
    String recommendedAction;
    String confirmedBy;
    String confirmedAt;
    String dismissedBy;
    String dismissedAt;
    String dismissReason;
    String createdAt;
    String updatedAt;
}

class AttendanceException extends RuntimeException {
    final int httpStatus;
    final int code;

    AttendanceException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

@RestControllerAdvice(basePackages = "cn.beiming.attendance")
class AttendanceExceptionHandler {
    @ExceptionHandler(AttendanceException.class)
    ResponseEntity<Map<String, Object>> handleAttendance(AttendanceException ex) {
        return ResponseEntity.status(ex.httpStatus).body(error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(error(40400, "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(error(53000, "attendance internal error"));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", AttendanceController.requestId());
        return body;
    }
}

@Configuration
class AttendanceRequestIdFilterConfig {
    @Bean
    AttendanceRequestIdFilter attendanceRequestIdFilter() {
        return new AttendanceRequestIdFilter();
    }
}

class AttendanceRequestIdFilter extends OncePerRequestFilter {
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
