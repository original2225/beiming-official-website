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

    Map<String, Object> counts();
}
