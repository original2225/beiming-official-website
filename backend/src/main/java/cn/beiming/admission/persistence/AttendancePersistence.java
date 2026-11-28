package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface AttendancePersistence {
    Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint);

    void persistInitialization(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> handoff, Map<String, Object> responseBody, int responseCode);

    void persistAccountLedgerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode);

    void persistContributionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> contribution, Map<String, Object> ledger, Map<String, Object> responseBody, int responseCode);

    void persistContributionCorrection(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> contribution, Map<String, Object> responseBody, int responseCode);

    void persistMonthlyRun(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> run, List<Map<String, Object>> accounts, List<Map<String, Object>> ledgers, List<Map<String, Object>> candidates, Map<String, Object> responseBody, int responseCode);

    void persistCandidateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode);

    void persistLeaderboardRebuild(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> snapshot, Map<String, Object> responseBody, int responseCode);

    /**
     * 启动时从正式库读回考勤账户。
     *
     * <p>此前 {@code AttendancePersistence} 只有写、没有任何读回路径：接口 200、库里也有行，
     * 但进程重启后 {@code AttendanceStore} 的内存 Map 全空，第一批玩家初始化出来的考勤直接消失，
     * 无异常、无告警——与 exam、whitelist 是同一个「假持久化」形态。
     *
     * <p>返回空表示没有持久化后端（内存模式）或空库首次启动，此时保留内存状态。
     */
    List<Map<String, Object>> loadAccounts();

    /** 启动时从正式库读回积分流水。理由同 {@link #loadAccounts()}——流水丢了榜单和历史对不上。 */
    List<Map<String, Object>> loadLedgers();

    /** 启动时从正式库读回贡献记录。理由同 {@link #loadAccounts()}。 */
    List<Map<String, Object>> loadContributions();

    /**
     * 启动时从正式库读回月度结算任务。必须在回读里恢复，否则重启后同一 {@code cycleKey}
     * 会被当成「没执行过」，重复扣一次全服的月度分（真实事故级的缺陷）。
     */
    List<Map<String, Object>> loadMonthlyRuns();

    /** 启动时从正式库读回移除候选。理由同 {@link #loadAccounts()}。 */
    List<Map<String, Object>> loadRemovalCandidates();

    /** 启动时从正式库读回榜单重建快照。榜单按实时数据重算，快照仅留档与排障。 */
    List<Map<String, Object>> loadLeaderboardSnapshots();

    Map<String, Object> counts();
}
