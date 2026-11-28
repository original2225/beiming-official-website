package cn.beiming.attendance;

import cn.beiming.admission.AdmissionTrustedActor;
import cn.beiming.admission.persistence.AttendancePersistence;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
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
import cn.beiming.core.AuthSessionResolver;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class AttendanceModule {
    @Bean
    AttendanceStore attendanceStore(AttendanceTestControls testControls, AttendancePersistence persistence) {
        AttendanceStore store = new AttendanceStore(testControls, persistence);
        store.hydrate();
        return store;
    }

    /**
     * 鉴权提供者。
     *
     * <p>硬编码 token（{@code admin-token} 等）只在 {@code attendance.auth.stub-tokens-enabled=true}
     * 时可用，生产默认 false —— 否则真实登录拿到的 {@code ses_*} token 永远命中不到，
     * 整个考勤模块对真实用户不可用（返回 41001，看起来像"没登录"）。
     *
     * <p>开关关闭时走 {@link AuthSessionResolver} 解析真实会话，与其余模块一致。
     */
    @Bean
    TestAttendanceAuthProvider attendanceAuthProvider(
            ObjectProvider<AuthSessionResolver> sessionResolver,
            @Value("${attendance.auth.stub-tokens-enabled:false}") boolean stubTokensEnabled) {
        return new TestAttendanceAuthProvider(sessionResolver, stubTokensEnabled);
    }

    @Bean
    AttendanceTestControls attendanceTestControls(@Value("${attendance.test-controls.enabled:false}") boolean enabled) {
        return new AttendanceTestControls(enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    AttendanceFlowEvidenceRecorder attendanceFlowEvidenceRecorder() {
        return new NoopAttendanceFlowEvidenceRecorder();
    }
}

@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController {
    private final AttendanceStore store;
    private final TestAttendanceAuthProvider auth;
    private final AttendanceFlowEvidenceRecorder evidenceRecorder;

    AttendanceController(AttendanceStore store, TestAttendanceAuthProvider auth, AttendanceFlowEvidenceRecorder evidenceRecorder) {
        this.store = store;
        this.auth = auth;
        this.evidenceRecorder = evidenceRecorder;
    }

    @GetMapping("/leaderboard")
    Map<String, Object> leaderboard(@RequestParam Map<String, String> query, HttpServletRequest request) {
        return ok(store.leaderboard(query, request));
    }

    @GetMapping("/me/account")
    Map<String, Object> myAccount(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization, request);
        return ok(store.myAccount(user, request));
    }

    @GetMapping("/me/ledger")
    Map<String, Object> myLedger(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query,
                                 HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization, request);
        return ok(store.myLedger(user, query, request));
    }

    @GetMapping("/me/contributions")
    Map<String, Object> myContributions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam Map<String, String> query,
                                        HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization, request);
        return ok(store.myContributions(user, query, request));
    }

    @GetMapping("/me/ranking")
    Map<String, Object> myRanking(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request) {
        AttendanceUser user = auth.requireUser(authorization, request);
        return ok(store.myRanking(user, request));
    }

    @GetMapping("/admin/accounts")
    Map<String, Object> adminAccounts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query,
                                      HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminAccounts(actor, query, request));
    }

    @GetMapping("/admin/accounts/{accountId}")
    Map<String, Object> adminAccount(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String accountId,
                                     HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminAccount(actor, accountId, request));
    }

    @PostMapping("/admin/initializations")
    ResponseEntity<Map<String, Object>> initialize(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        MutationResult result = store.initialize(actor, bodyOrEmpty(body), request);
        int responseCode = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        if (result.created()) {
            evidenceRecorder.recordInitializationWrite(request, "ATTENDANCE_INITIALIZED", result.value(), responseCode);
        }
        return ResponseEntity.status(responseCode).body(okBody(result.value()));
    }

    @PostMapping("/admin/accounts/{accountId}/adjustments")
    Map<String, Object> adjust(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String accountId,
                               @RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        Map<String, Object> payload = store.adjust(actor, accountId, bodyOrEmpty(body), request);
        evidenceRecorder.recordAdjustmentWrite(request, "ATTENDANCE_SCORE_ADJUSTED", payload, HttpStatus.OK.value());
        return ok(payload);
    }

    @PostMapping("/admin/ledger/{ledgerId}/reverse")
    Map<String, Object> reverse(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String ledgerId,
                                @RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.reverse(actor, ledgerId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/contributions")
    ResponseEntity<Map<String, Object>> createContribution(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @RequestBody(required = false) Map<String, Object> body,
                                                           HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        Map<String, Object> payload = store.createContribution(actor, bodyOrEmpty(body), request);
        evidenceRecorder.recordContributionWrite(request, "ATTENDANCE_CONTRIBUTION_CREATED", payload, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(payload));
    }

    @PatchMapping("/admin/contributions/{contributionId}")
    Map<String, Object> correctContribution(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable String contributionId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.correctContribution(actor, contributionId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/monthly-runs/preview")
    Map<String, Object> previewMonthly(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.monthlyPreview(actor, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/monthly-runs")
    ResponseEntity<Map<String, Object>> runMonthly(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        MutationResult result = store.runMonthly(actor, bodyOrEmpty(body), request);
        int responseCode = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        if (result.created()) {
            evidenceRecorder.recordMonthlyRunWrite(request, "ATTENDANCE_MONTHLY_RUN_EXECUTED", result.value(), responseCode);
        }
        return ResponseEntity.status(responseCode).body(okBody(result.value()));
    }

    @GetMapping("/admin/monthly-runs/{runId}")
    Map<String, Object> monthlyRun(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String runId,
                                   HttpServletRequest request) {
        auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.monthlyRun(runId, request));
    }

    @GetMapping("/admin/removal-candidates")
    Map<String, Object> removalCandidates(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestParam Map<String, String> query,
                                          HttpServletRequest request) {
        auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
        return ok(store.removalCandidates(query, request));
    }

    @PatchMapping("/admin/removal-candidates/{candidateId}/confirm")
    Map<String, Object> confirmCandidate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String candidateId,
                                         @RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.confirmCandidate(actor, candidateId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/removal-candidates/{candidateId}/dismiss")
    Map<String, Object> dismissCandidate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String candidateId,
                                         @RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.dismissCandidate(actor, candidateId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/leaderboard/rebuild")
    Map<String, Object> rebuildLeaderboard(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody(required = false) Map<String, Object> body,
                                           HttpServletRequest request) {
        AttendanceUser actor = auth.requireAny(authorization, request, "ADMIN", "OWNER");
        return ok(store.rebuildLeaderboard(actor, bodyOrEmpty(body), request));
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
        auth.requireAny(authorization, request, "HELPER", "ADMIN", "OWNER");
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
    private final AttendancePersistence persistence;
    private int idSeq = 1000;

    AttendanceStore(AttendanceTestControls testControls, AttendancePersistence persistence) {
        this.testControls = testControls;
        this.persistence = persistence;
    }

    /**
     * 启动时从正式库把六张表读回内存，覆盖初始状态。
     *
     * <p>此前内存 Map 就是唯一存储：接口 200、库里有行，但进程重启后第一批玩家的考勤账户、
     * 流水、贡献、月度结算全部消失，无异常无告警（假持久化，与 exam / whitelist 同型）。
     *
     * <p>覆盖而非合并：库里那份是运维的真实状态，包含「已被删除/归档」这类决定，合并会让
     * 已删除的记录复活。读回为空（内存模式、或空库首次启动）就什么都不做，保持初始状态。
     *
     * <p>回读后必须重建派生索引并推进 {@code idSeq}——否则重启后新建的 {@code att-acc-1001}
     * 会撞上库里已有的同号主键（这是 whitelist 那轮踩过的坑）。
     */
    synchronized void hydrate() {
        List<Map<String, Object>> storedAccounts = persistence.loadAccounts();
        if (storedAccounts != null && !storedAccounts.isEmpty()) {
            Map<String, AttendanceAccountRecord> loadedAccounts = new LinkedHashMap<>();
            for (Map<String, Object> row : storedAccounts) {
                AttendanceAccountRecord account = accountFromRow(row);
                if (account != null) loadedAccounts.put(account.accountId, account);
            }
            if (!loadedAccounts.isEmpty()) {
                accounts.clear();
                accounts.putAll(loadedAccounts);
                accountByUser.clear();
                accountByMember.clear();
                initByApplication.clear();
                for (AttendanceAccountRecord account : loadedAccounts.values()) {
                    if (account.userId != null) accountByUser.put(account.userId, account.accountId);
                    if (account.memberId != null) accountByMember.put(account.memberId, account.accountId);
                    if (account.whitelistApplicationId != null) initByApplication.put(account.whitelistApplicationId, account.accountId);
                }
            }
        }

        List<Map<String, Object>> storedLedgers = persistence.loadLedgers();
        if (storedLedgers != null && !storedLedgers.isEmpty()) {
            Map<String, AttendanceLedgerRecord> loaded = new LinkedHashMap<>();
            for (Map<String, Object> row : storedLedgers) {
                AttendanceLedgerRecord ledger = ledgerFromRow(row);
                if (ledger != null) loaded.put(ledger.ledgerId, ledger);
            }
            if (!loaded.isEmpty()) {
                ledgers.clear();
                ledgers.putAll(loaded);
            }
        }

        List<Map<String, Object>> storedContributions = persistence.loadContributions();
        if (storedContributions != null && !storedContributions.isEmpty()) {
            Map<String, ContributionRecord> loaded = new LinkedHashMap<>();
            sourceContribution.clear();
            for (Map<String, Object> row : storedContributions) {
                ContributionRecord contribution = contributionFromRow(row);
                if (contribution == null) continue;
                loaded.put(contribution.contributionId, contribution);
                if (contribution.accountId != null && contribution.sourceModule != null && contribution.sourceId != null) {
                    sourceContribution.put(contribution.accountId + ":" + contribution.sourceModule + ":" + contribution.sourceId, contribution.contributionId);
                }
            }
            if (!loaded.isEmpty()) {
                contributions.clear();
                contributions.putAll(loaded);
            }
        }

        List<Map<String, Object>> storedRuns = persistence.loadMonthlyRuns();
        if (storedRuns != null && !storedRuns.isEmpty()) {
            Map<String, MonthlyRunRecord> loaded = new LinkedHashMap<>();
            for (Map<String, Object> row : storedRuns) {
                MonthlyRunRecord run = monthlyRunFromRow(row);
                if (run != null) loaded.put(run.runId, run);
            }
            if (!loaded.isEmpty()) {
                monthlyRuns.clear();
                monthlyRuns.putAll(loaded);
            }
        }

        List<Map<String, Object>> storedCandidates = persistence.loadRemovalCandidates();
        if (storedCandidates != null && !storedCandidates.isEmpty()) {
            Map<String, RemovalCandidateRecord> loaded = new LinkedHashMap<>();
            for (Map<String, Object> row : storedCandidates) {
                RemovalCandidateRecord candidate = candidateFromRow(row);
                if (candidate != null) loaded.put(candidate.candidateId, candidate);
            }
            if (!loaded.isEmpty()) {
                candidates.clear();
                candidates.putAll(loaded);
            }
        }

        // 榜单快照只留档排障，不参与实时榜单计算，读回不覆盖内存（内存不存快照），仅用于推进序号。
        advanceIdSeq();
    }

    /**
     * 把所有以 {@code att-xxx-<n>} 形式生成的标识里最大的 n 抬到 {@code idSeq} 之上，
     * 保证重启后新建记录不会撞主键。库里历史 ID 可能来自更早的序号空间，也可能非本格式，
     * 解析不出来的忽略即可。
     */
    private void advanceIdSeq() {
        int max = idSeq;
        for (String id : accounts.keySet()) max = Math.max(max, numericSuffix(id));
        for (String id : ledgers.keySet()) max = Math.max(max, numericSuffix(id));
        for (String id : contributions.keySet()) max = Math.max(max, numericSuffix(id));
        for (String id : monthlyRuns.keySet()) max = Math.max(max, numericSuffix(id));
        for (String id : candidates.keySet()) max = Math.max(max, numericSuffix(id));
        idSeq = max;
    }

    private int numericSuffix(String id) {
        if (id == null) return Integer.MIN_VALUE;
        int dash = id.lastIndexOf('-');
        if (dash < 0 || dash == id.length() - 1) return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException exception) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * 清空内存态回到初始（仅同包可见，供测试建立干净基线）。
     *
     * <p>有了 {@link #hydrate()} 之后，「删库」不再等于「内存也空」——启动时已把库读进内存。
     * 测试若只用 SQL 删库、不重置内存，就会在幽灵数据上继续跑。生产路径不会调用它。
     */
    synchronized void reset() {
        accounts.clear();
        ledgers.clear();
        contributions.clear();
        candidates.clear();
        monthlyRuns.clear();
        accountByUser.clear();
        accountByMember.clear();
        initByApplication.clear();
        sourceContribution.clear();
        idempotency.clear();
        audits.clear();
        idSeq = 1000;
    }

    /**
     * 回读验收用的内存快照访问器（仅同包可见）。
     *
     * <p>{@code hydrate()} 是从库回读进内存的唯一入口，验证它只能看内存态本身——
     * 走 HTTP 读取会被「新起实例的内存响应」冒充成持久化，正是假持久化能一直绿的原因。
     */
    Map<String, AttendanceAccountRecord> accountsSnapshot() {
        return accounts;
    }

    Map<String, AttendanceLedgerRecord> ledgersSnapshot() {
        return ledgers;
    }

    Map<String, ContributionRecord> contributionsSnapshot() {
        return contributions;
    }

    Map<String, MonthlyRunRecord> monthlyRunsSnapshot() {
        return monthlyRuns;
    }

    Map<String, RemovalCandidateRecord> candidatesSnapshot() {
        return candidates;
    }

    String accountIdByUser(String userId) {
        return accountByUser.get(userId);
    }

    String accountIdByMember(String memberId) {
        return accountByMember.get(memberId);
    }

    String accountIdByApplication(String applicationId) {
        return initByApplication.get(applicationId);
    }

    int currentIdSeq() {
        return idSeq;
    }

    private AttendanceAccountRecord accountFromRow(Map<String, Object> row) {
        Object id = row.get("accountId");
        if (id == null) return null;
        AttendanceAccountRecord account = new AttendanceAccountRecord();
        account.accountId = String.valueOf(id);
        account.userId = textOf(row.get("userId"));
        account.memberId = textOf(row.get("memberId"));
        account.displayNameSnapshot = textOf(row.get("displayNameSnapshot"));
        account.avatarUrlSnapshot = textOf(row.get("avatarUrlSnapshot"));
        account.memberGroupSnapshot = textOf(row.get("memberGroupSnapshot"));
        account.memberStatusSnapshot = textOf(row.get("memberStatusSnapshot"));
        Object binding = row.get("minecraftBindingSnapshot");
        account.minecraftBindingSnapshot = binding instanceof Map<?, ?> map ? castMap(map) : Map.of();
        account.status = textOf(row.get("status"));
        account.scoreBalance = intOf(row.get("scoreBalance"));
        account.initialScore = intOf(row.get("initialScore"));
        account.totalEarned = intOf(row.get("totalEarned"));
        account.totalDeducted = intOf(row.get("totalDeducted"));
        account.lastPositiveActivityAt = textOf(row.get("lastPositiveActivityAt"));
        account.lastDeductedAt = textOf(row.get("lastDeductedAt"));
        account.lastLedgerId = textOf(row.get("lastLedgerId"));
        account.whitelistApplicationId = textOf(row.get("whitelistApplicationId"));
        account.whitelistHandoffId = textOf(row.get("whitelistHandoffId"));
        account.whitelistHandoffVersion = intOf(row.get("whitelistHandoffVersion"));
        account.reviewDirection = textOf(row.get("reviewDirection"));
        account.attemptType = textOf(row.get("attemptType"));
        account.notificationStatus = textOf(row.get("notificationStatus"));
        Object failure = row.get("notificationFailure");
        account.notificationFailure = failure instanceof Map<?, ?> map ? castMap(map) : null;
        Object stale = row.get("profileSnapshotStale");
        account.profileSnapshotStale = stale instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(stale));
        account.createdAt = textOf(row.get("createdAt"));
        account.updatedAt = textOf(row.get("updatedAt"));
        account.archivedAt = textOf(row.get("archivedAt"));
        return account;
    }

    private AttendanceLedgerRecord ledgerFromRow(Map<String, Object> row) {
        Object id = row.get("ledgerId");
        if (id == null) return null;
        AttendanceLedgerRecord ledger = new AttendanceLedgerRecord();
        ledger.ledgerId = String.valueOf(id);
        ledger.accountId = textOf(row.get("accountId"));
        ledger.memberId = textOf(row.get("memberId"));
        ledger.userId = textOf(row.get("userId"));
        ledger.type = textOf(row.get("type"));
        ledger.status = textOf(row.get("status"));
        ledger.delta = intOf(row.get("delta"));
        ledger.balanceBefore = intOf(row.get("balanceBefore"));
        ledger.balanceAfter = intOf(row.get("balanceAfter"));
        ledger.sourceModule = textOf(row.get("sourceModule"));
        ledger.sourceId = textOf(row.get("sourceId"));
        ledger.cycleKey = textOf(row.get("cycleKey"));
        ledger.reason = textOf(row.get("reason"));
        ledger.publicReason = textOf(row.get("publicReason"));
        ledger.operatorUserId = textOf(row.get("operatorUserId"));
        ledger.idempotencyKey = textOf(row.get("idempotencyKey"));
        ledger.reversalOfLedgerId = textOf(row.get("reversalOfLedgerId"));
        ledger.reversedByLedgerId = textOf(row.get("reversedByLedgerId"));
        ledger.notificationStatus = textOf(row.get("notificationStatus"));
        Object failure = row.get("notificationFailure");
        ledger.notificationFailure = failure instanceof Map<?, ?> map ? castMap(map) : null;
        ledger.createdAt = textOf(row.get("createdAt"));
        ledger.reversedAt = textOf(row.get("reversedAt"));
        return ledger;
    }

    private ContributionRecord contributionFromRow(Map<String, Object> row) {
        Object id = row.get("contributionId");
        if (id == null) return null;
        ContributionRecord contribution = new ContributionRecord();
        contribution.contributionId = String.valueOf(id);
        contribution.accountId = textOf(row.get("accountId"));
        contribution.memberId = textOf(row.get("memberId"));
        contribution.userId = textOf(row.get("userId"));
        contribution.type = textOf(row.get("type"));
        contribution.sourceModule = textOf(row.get("sourceModule"));
        contribution.sourceId = textOf(row.get("sourceId"));
        contribution.title = textOf(row.get("title"));
        contribution.description = textOf(row.get("description"));
        contribution.occurredAt = textOf(row.get("occurredAt"));
        contribution.scoreDelta = intOf(row.get("scoreDelta"));
        contribution.ledgerId = textOf(row.get("ledgerId"));
        contribution.operatorUserId = textOf(row.get("operatorUserId"));
        contribution.correctionOfContributionId = textOf(row.get("correctionOfContributionId"));
        contribution.createdAt = textOf(row.get("createdAt"));
        contribution.updatedAt = textOf(row.get("updatedAt"));
        return contribution;
    }

    private MonthlyRunRecord monthlyRunFromRow(Map<String, Object> row) {
        Object id = row.get("runId");
        if (id == null) return null;
        MonthlyRunRecord run = new MonthlyRunRecord();
        run.runId = String.valueOf(id);
        run.cycleKey = textOf(row.get("cycleKey"));
        run.status = textOf(row.get("status"));
        Object dryRun = row.get("dryRun");
        run.dryRun = dryRun instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(dryRun));
        run.reason = textOf(row.get("reason"));
        run.deductionScore = intOf(row.get("deductionScore"));
        run.eligibleAccounts = intOf(row.get("eligibleAccounts"));
        run.deductedAccounts = intOf(row.get("deductedAccounts"));
        run.skippedAccounts = intOf(row.get("skippedAccounts"));
        run.candidateCreated = intOf(row.get("candidateCreated"));
        run.idempotencyKey = textOf(row.get("idempotencyKey"));
        run.startedAt = textOf(row.get("startedAt"));
        run.completedAt = textOf(row.get("completedAt"));
        run.failureReason = textOf(row.get("failureReason"));
        run.createdBy = textOf(row.get("createdBy"));
        run.createdAt = textOf(row.get("createdAt"));
        return run;
    }

    private RemovalCandidateRecord candidateFromRow(Map<String, Object> row) {
        Object id = row.get("candidateId");
        if (id == null) return null;
        RemovalCandidateRecord candidate = new RemovalCandidateRecord();
        candidate.candidateId = String.valueOf(id);
        candidate.accountId = textOf(row.get("accountId"));
        candidate.memberId = textOf(row.get("memberId"));
        candidate.userId = textOf(row.get("userId"));
        candidate.displayNameSnapshot = textOf(row.get("displayNameSnapshot"));
        candidate.scoreBalance = intOf(row.get("scoreBalance"));
        candidate.cycleKey = textOf(row.get("cycleKey"));
        candidate.status = textOf(row.get("status"));
        candidate.reason = textOf(row.get("reason"));
        candidate.publicReason = textOf(row.get("publicReason"));
        candidate.recommendedAction = textOf(row.get("recommendedAction"));
        candidate.confirmedBy = textOf(row.get("confirmedBy"));
        candidate.confirmedAt = textOf(row.get("confirmedAt"));
        candidate.dismissedBy = textOf(row.get("dismissedBy"));
        candidate.dismissedAt = textOf(row.get("dismissedAt"));
        candidate.dismissReason = textOf(row.get("dismissReason"));
        candidate.createdAt = textOf(row.get("createdAt"));
        candidate.updatedAt = textOf(row.get("updatedAt"));
        return candidate;
    }

    private static String textOf(Object value) {
        return value == null ? null : value.toString();
    }

    private static int intOf(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
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
        persistence.persistInitialization(request, actor.userId(), actorRole(actor), idempotencyKey(body), canonical(body), accountView(account, true, request), ledgerView(ledger, true), handoffView(handoff), value, 201);
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
        persistence.persistAccountLedgerWrite(request, actor.userId(), actorRole(actor), "attendance.adjustment.create", "ATTENDANCE_SCORE_ADJUSTED", "ATTENDANCE_ACCOUNT", account.accountId, "MEDIUM", beforeStatus, account.status, ledger.reason, idempotencyKey(body), canonical(body), accountView(account, true, request), ledgerView(ledger, true), candidate == null ? null : candidateView(candidate, true), value, 200);
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
        persistence.persistAccountLedgerWrite(request, actor.userId(), actorRole(actor), "attendance.ledger.reverse", "ATTENDANCE_LEDGER_REVERSED", "ATTENDANCE_LEDGER", ledgerId, "MEDIUM", "POSTED", "REVERSED", reversal.reason, idempotencyKey(body), canonical(body), accountView(account, true, request), ledgerView(reversal, true), null, value, 200);
        persistence.persistAccountLedgerWrite(request, actor.userId(), actorRole(actor), null, "ATTENDANCE_LEDGER_REVERSED_ORIGINAL", "ATTENDANCE_LEDGER", ledgerId, "LOW", "POSTED", "REVERSED", reversal.reason, null, canonical(body), accountView(account, true, request), ledgerView(original, true), null, value, 200);
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
        persistence.persistContributionWrite(request, actor.userId(), actorRole(actor), "attendance.contribution.create", "ATTENDANCE_CONTRIBUTION_CREATED", validateRequiredString(body, "reason", 1, 500), idempotencyKey(body), canonical(body), accountView(account, true, request), contributionView(contribution, true), ledger == null ? null : ledgerView(ledger, true), value, 201);
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
        persistence.persistContributionCorrection(request, actor.userId(), actorRole(actor), idempotencyKey(body), canonical(body), value, value, 200);
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
        List<Map<String, Object>> persistedAccounts = new ArrayList<>();
        List<Map<String, Object>> persistedLedgers = new ArrayList<>();
        List<Map<String, Object>> persistedCandidates = new ArrayList<>();
        for (MonthlyItem item : items) {
            if (!item.deduct()) continue;
            AttendanceAccountRecord account = requireAccount(item.accountId());
            int before = account.scoreBalance;
            int after = Math.max(0, before - deduction);
            AttendanceLedgerRecord ledger = ledger(account, "MONTHLY_DEDUCTION", -deduction, before, after, "attendance", run.runId, cycleKey, run.reason, "月度考勤扣分", actor.userId(), idempotencyKey(body), null, request);
            ledgers.put(ledger.ledgerId, ledger);
            applyBalance(account, after, -deduction, ledger.ledgerId);
            RemovalCandidateRecord candidate = ensureCandidateIfNeeded(account, cycleKey, actor, "月度扣分后进入复核", "积分归零");
            persistedAccounts.add(accountView(account, true, request));
            persistedLedgers.add(ledgerView(ledger, true));
            if (candidate != null) persistedCandidates.add(candidateView(candidate, true));
        }
        monthlyRuns.put(run.runId, run);
        audit(actor, "ATTENDANCE_MONTHLY_RUN", run.runId, "ATTENDANCE_MONTHLY_RUN_EXECUTED", "HIGH", "PENDING", run.status, run.reason);
        Map<String, Object> value = monthlyRunView(run, items);
        persistence.persistMonthlyRun(request, actor.userId(), actorRole(actor), idempotencyKey(body), canonical(body), value, persistedAccounts, persistedLedgers, persistedCandidates, value, 201);
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
        persistence.persistCandidateWrite(request, actor.userId(), actorRole(actor), "attendance.candidate.confirm", "ATTENDANCE_REMOVAL_CANDIDATE_CONFIRMED", "HIGH", "OPEN", "CONFIRMED", validateRequiredString(body, "reason", 1, 500), idempotencyKey(body), canonical(body), accountView(account, true, request), candidateView(candidate, true), value, 200);
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
        persistence.persistCandidateWrite(request, actor.userId(), actorRole(actor), "attendance.candidate.dismiss", "ATTENDANCE_REMOVAL_CANDIDATE_DISMISSED", "MEDIUM", "OPEN", "DISMISSED", candidate.dismissReason, idempotencyKey(body), canonical(body), accountView(requireAccount(candidate.accountId), true, request), candidateView(candidate, true), value, 200);
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
        persistence.persistLeaderboardRebuild(request, actor.userId(), actorRole(actor), idempotencyKey(body), canonical(body), value, value, 200);
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
        return linkedMap("service", "attendance", "port", 8131, "legacyPort", 8111, "storageMode", "IN_MEMORY", "authMode", "TEST_STUB", "whitelistMode", "TEST_STUB", "profileMode", "TEST_STUB", "notificationMode", "TEST_STUB", "testControlsEnabled", testControls.enabled(), "accountsTotal", accounts.size(), "activeAccountsTotal", active, "removalCandidatesOpenTotal", openCandidates, "monthlyRunsTotal", monthlyRuns.size(), "ledgerEntriesTotal", ledgers.size(), "contributionsTotal", contributions.size(), "auditsTotal", audits.size(), "idempotencyRecordsTotal", idempotency.size(), "lastMonthlyRunAt", monthlyRuns.isEmpty() ? null : NOW, "lastAuditAt", audits.isEmpty() ? null : NOW, "productionGaps", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB", "P0_WHITELIST_STUB", "P0_PROFILE_STUB", "P0_NOTIFICATION_STUB", testControls.enabled() ? "TEST_CONTROLS_ENABLED_FOR_LOCAL_TEST" : "TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "REAL_ACTIVITY_EVENTS_NOT_CONNECTED", "REAL_ONLINE_TIME_NOT_CONNECTED", "WHITELIST_REMOVAL_NOT_CONNECTED"));
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

    private String actorRole(AttendanceUser actor) {
        return actor.roles().stream().findFirst().orElse("USER");
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
        if (existing == null) {
            try {
                Map<String, Object> postgresReplay = persistence.replay(actorId, persistenceScope(operation), key, canonical(body));
                if (postgresReplay != null) return new IdempotencyRecord(canonical(body), postgresReplay);
            } catch (IllegalStateException exception) {
                throw new AttendanceException(409, 45017, "idempotency conflict");
            }
        }
        return existing;
    }

    private String persistenceScope(String operation) {
        if ("initialize".equals(operation)) return "attendance.initialization.create";
        if (operation.startsWith("adjust:")) return "attendance.adjustment.create";
        if (operation.startsWith("reverse:")) return "attendance.ledger.reverse";
        if ("contribution:create".equals(operation)) return "attendance.contribution.create";
        if (operation.startsWith("contribution:correct:")) return "attendance.contribution.correct";
        if ("monthly".equals(operation)) return "attendance.monthly-run.execute";
        if (operation.startsWith("candidate:confirm:")) return "attendance.candidate.confirm";
        if (operation.startsWith("candidate:dismiss:")) return "attendance.candidate.dismiss";
        if ("leaderboard".equals(operation)) return "attendance.leaderboard.rebuild";
        return "attendance." + operation;
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
    // 可选注入：只加载 attendance 的测试/裁剪上下文里没有 auth 模块，用 ObjectProvider 避免启动即失败。
    private final org.springframework.beans.factory.ObjectProvider<AuthSessionResolver> sessionResolver;
    private final boolean stubTokensEnabled;

    TestAttendanceAuthProvider(org.springframework.beans.factory.ObjectProvider<AuthSessionResolver> sessionResolver,
                               boolean stubTokensEnabled) {
        this.sessionResolver = sessionResolver;
        this.stubTokensEnabled = stubTokensEnabled;
    }

    AttendanceUser requireUser(String authorization, HttpServletRequest request) {
        if (AdmissionTrustedActor.hasGatewayContext(request)) {
            return trustedActor(request);
        }
        return requireUser(authorization);
    }

    AttendanceUser requireUser(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new AttendanceException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new AttendanceException(401, 41003, "bad token format");

        // 真实会话优先：登录拿到的 ses_* token 由核心 auth 模块解析出真实用户与角色。
        // 不先走这一步，整个模块对真实用户不可用（桩只认硬编码字符串）。
        AuthSessionResolver resolver = sessionResolver.getIfAvailable();
        AuthSessionResolver.ResolvedSession resolved = resolver == null ? null : resolver.resolve(authorization);
        if (resolved != null) {
            // ResolvedSession 只给最高优先级的一个角色（OWNER/ADMIN/HELPER/USER）。
            Set<String> roles = new LinkedHashSet<>();
            if (resolved.role() != null && !resolved.role().isBlank()) {
                roles.add(resolved.role());
            }
            return new AttendanceUser(resolved.userId(), resolved.username(), roles, "ACTIVE");
        }

        if (!stubTokensEnabled) {
            // 开关关闭时明确报无效会话，与核心 auth 的错误码语义一致。
            throw new AttendanceException(401, 41001, "invalid session");
        }

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

    AttendanceUser requireAny(String authorization, HttpServletRequest request, String... roles) {
        AttendanceUser user = requireUser(authorization, request);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new AttendanceException(403, 42001, "role permission denied");
        return user;
    }

    AttendanceUser requireAny(String authorization, String... roles) {
        AttendanceUser user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new AttendanceException(403, 42001, "role permission denied");
        return user;
    }

    private AttendanceUser trustedActor(HttpServletRequest request) {
        try {
            AdmissionTrustedActor.Actor actor = AdmissionTrustedActor.parse(request);
            return new AttendanceUser(actor.userId(), actor.displayName(), actor.roles(), "ACTIVE");
        } catch (IllegalArgumentException exception) {
            throw new AttendanceException(502, 48002, "auth incompatible");
        }
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

interface AttendanceFlowEvidenceRecorder {
    void recordInitializationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordAdjustmentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordContributionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordMonthlyRunWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);
}

class NoopAttendanceFlowEvidenceRecorder implements AttendanceFlowEvidenceRecorder {
    @Override
    public void recordInitializationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordAdjustmentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordContributionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordMonthlyRunWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }
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
