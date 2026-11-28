package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public class NoopAttendancePersistence implements AttendancePersistence {
    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistInitialization(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> handoff, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistAccountLedgerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistContributionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> contribution, Map<String, Object> ledger, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistContributionCorrection(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> contribution, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistMonthlyRun(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> run, List<Map<String, Object>> accounts, List<Map<String, Object>> ledgers, List<Map<String, Object>> candidates, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistCandidateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistLeaderboardRebuild(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> snapshot, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public List<Map<String, Object>> loadAccounts() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> loadLedgers() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> loadContributions() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> loadMonthlyRuns() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> loadRemovalCandidates() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> loadLeaderboardSnapshots() {
        return List.of();
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of("storageMode", "IN_MEMORY");
    }
}
