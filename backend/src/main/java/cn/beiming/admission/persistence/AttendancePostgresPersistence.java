package cn.beiming.admission.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AttendancePostgresPersistence implements AttendancePersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AttendancePostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        if (idempotencyKey == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT request_fingerprint, response_body::text
                FROM app_idempotency_records
                WHERE actor_user_id = ? AND scope = ? AND idempotency_key = ?
                """, actorUserId, scope, idempotencyKey);
        if (rows.isEmpty()) {
            return null;
        }
        String storedFingerprint = Objects.toString(rows.getFirst().get("request_fingerprint"), null);
        if (!Objects.equals(storedFingerprint, fingerprint)) {
            throw new IllegalStateException("idempotency conflict");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(Objects.toString(rows.getFirst().get("response_body")), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            return data;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to parse attendance idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistInitialization(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> handoff, Map<String, Object> responseBody, int responseCode) {
        upsertAccount(account);
        upsertLedger(ledger);
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_ACCOUNT", text(account, "accountId"), "ATTENDANCE_INITIALIZED", "MEDIUM", null, text(account, "status"), "initialize attendance");
        insertIdempotency(actorUserId, "attendance.initialization.create", idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistAccountLedgerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> ledger, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode) {
        upsertAccount(account);
        if (ledger != null) {
            upsertLedger(ledger);
        }
        if (candidate != null) {
            upsertCandidate(candidate);
        }
        insertAudit(request, actorUserId, actorRole, targetType, targetId, action, riskLevel, beforeStatus, afterStatus, reason);
        if (scope != null) {
            insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
            insertRequestLog(request, actorUserId, responseCode);
        }
    }

    @Override
    @Transactional
    public void persistContributionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> contribution, Map<String, Object> ledger, Map<String, Object> responseBody, int responseCode) {
        upsertAccount(account);
        if (ledger != null) {
            upsertLedger(ledger);
        }
        upsertContribution(contribution);
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_CONTRIBUTION", text(contribution, "contributionId"), action, "MEDIUM", null, text(contribution, "type"), reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistContributionCorrection(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> contribution, Map<String, Object> responseBody, int responseCode) {
        upsertContribution(contribution);
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_CONTRIBUTION", text(contribution, "contributionId"), "ATTENDANCE_CONTRIBUTION_CORRECTED", "MEDIUM", null, text(contribution, "type"), null);
        insertIdempotency(actorUserId, "attendance.contribution.correct", idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistMonthlyRun(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> run, List<Map<String, Object>> accounts, List<Map<String, Object>> ledgers, List<Map<String, Object>> candidates, Map<String, Object> responseBody, int responseCode) {
        for (Map<String, Object> account : accounts) {
            upsertAccount(account);
        }
        for (Map<String, Object> ledger : ledgers) {
            upsertLedger(ledger);
        }
        for (Map<String, Object> candidate : candidates) {
            upsertCandidate(candidate);
        }
        upsertMonthlyRun(run);
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_MONTHLY_RUN", text(run, "runId"), "ATTENDANCE_MONTHLY_RUN_EXECUTED", "HIGH", "PENDING", text(run, "status"), text(run, "reason"));
        insertIdempotency(actorUserId, "attendance.monthly-run.execute", idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistCandidateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> account, Map<String, Object> candidate, Map<String, Object> responseBody, int responseCode) {
        upsertAccount(account);
        upsertCandidate(candidate);
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_REMOVAL_CANDIDATE", text(candidate, "candidateId"), action, riskLevel, beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistLeaderboardRebuild(HttpServletRequest request, String actorUserId, String actorRole, String idempotencyKey, String fingerprint, Map<String, Object> snapshot, Map<String, Object> responseBody, int responseCode) {
        jdbc.update("""
                INSERT INTO attendance_leaderboard_snapshots(id, snapshot_id, cycle_key, entries_payload, rebuilt_by, request_id, rebuilt_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, now())
                """, UUID.randomUUID(), "att-leaderboard-" + UUID.randomUUID(), text(snapshot, "cycleKey"), json(snapshot.get("items")), actorUserId, requestId(request));
        insertAudit(request, actorUserId, actorRole, "ATTENDANCE_LEADERBOARD", "current", "ATTENDANCE_LEADERBOARD_REBUILT", "MEDIUM", null, "REBUILT", text(snapshot, "reason"));
        insertIdempotency(actorUserId, "attendance.leaderboard.rebuild", idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of(
                "storageMode", "POSTGRESQL_PRIMARY",
                "accountsTotal", count("attendance_accounts"),
                "activeAccountsTotal", countWhere("attendance_accounts", "status = 'ACTIVE'"),
                "removalCandidatesOpenTotal", countWhere("attendance_removal_candidates", "status = 'OPEN'"),
                "monthlyRunsTotal", count("attendance_monthly_runs"),
                "ledgerEntriesTotal", count("attendance_ledgers"),
                "contributionsTotal", count("attendance_contributions"),
                "auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'ATTENDANCE%'"),
                "idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'attendance.%'"),
                "requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/attendance%'")
        );
    }

    private void upsertAccount(Map<String, Object> account) {
        jdbc.update("""
                INSERT INTO attendance_accounts(id, account_id, user_id, member_id, display_name_snapshot, avatar_url_snapshot, member_group_snapshot, member_status_snapshot, minecraft_binding_snapshot, status, score_balance, initial_score, total_earned, total_deducted, last_positive_activity_at, last_deducted_at, last_ledger_id, whitelist_application_id, whitelist_handoff_id, whitelist_handoff_version, review_direction, attempt_type, notification_status, notification_failure, profile_snapshot_stale, created_at, updated_at, archived_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (account_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    score_balance = EXCLUDED.score_balance,
                    total_earned = EXCLUDED.total_earned,
                    total_deducted = EXCLUDED.total_deducted,
                    last_positive_activity_at = EXCLUDED.last_positive_activity_at,
                    last_deducted_at = EXCLUDED.last_deducted_at,
                    last_ledger_id = EXCLUDED.last_ledger_id,
                    notification_status = EXCLUDED.notification_status,
                    notification_failure = EXCLUDED.notification_failure,
                    profile_snapshot_stale = EXCLUDED.profile_snapshot_stale,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at
                """, UUID.randomUUID(), text(account, "accountId"), text(account, "userId"), text(account, "memberId"),
                text(account, "displayNameSnapshot"), text(account, "avatarUrlSnapshot"), text(account, "memberGroupSnapshot"),
                text(account, "memberStatusSnapshot"), json(account.get("minecraftBindingSnapshot")), text(account, "status"),
                intValue(account.get("scoreBalance"), 0), intValue(account.get("initialScore"), 0), intValue(account.get("totalEarned"), 0),
                intValue(account.get("totalDeducted"), 0), ts(text(account, "lastPositiveActivityAt")), ts(text(account, "lastDeductedAt")),
                text(account, "lastLedgerId"), text(account, "whitelistApplicationId"), text(account, "whitelistHandoffId"),
                intValue(account.get("whitelistHandoffVersion"), 1), text(account, "reviewDirection"), text(account, "attemptType"),
                text(account, "notificationStatus"), json(account.get("notificationFailure")), Boolean.TRUE.equals(account.get("profileSnapshotStale")),
                ts(text(account, "createdAt")), ts(text(account, "updatedAt")), ts(text(account, "archivedAt")));
    }

    private void upsertLedger(Map<String, Object> ledger) {
        jdbc.update("""
                INSERT INTO attendance_ledgers(id, ledger_id, account_id, member_id, user_id, type, status, delta, balance_before, balance_after, source_module, source_id, cycle_key, reason, public_reason, operator_user_id, idempotency_key, reversal_of_ledger_id, reversed_by_ledger_id, notification_status, notification_failure, created_at, reversed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (ledger_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    reversed_by_ledger_id = EXCLUDED.reversed_by_ledger_id,
                    reversed_at = EXCLUDED.reversed_at
                """, UUID.randomUUID(), text(ledger, "ledgerId"), text(ledger, "accountId"), text(ledger, "memberId"),
                text(ledger, "userId"), text(ledger, "type"), text(ledger, "status"), intValue(ledger.get("delta"), 0),
                intValue(ledger.get("balanceBefore"), 0), intValue(ledger.get("balanceAfter"), 0), text(ledger, "sourceModule"),
                text(ledger, "sourceId"), text(ledger, "cycleKey"), text(ledger, "reason"), text(ledger, "publicReason"),
                text(ledger, "operatorUserId"), text(ledger, "idempotencyKey"), text(ledger, "reversalOfLedgerId"),
                text(ledger, "reversedByLedgerId"), text(ledger, "notificationStatus"), json(ledger.get("notificationFailure")),
                ts(text(ledger, "createdAt")), ts(text(ledger, "reversedAt")));
    }

    private void upsertContribution(Map<String, Object> contribution) {
        jdbc.update("""
                INSERT INTO attendance_contributions(id, contribution_id, account_id, member_id, user_id, type, source_module, source_id, title, description, occurred_at, score_delta, ledger_id, operator_user_id, correction_of_contribution_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (contribution_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    occurred_at = EXCLUDED.occurred_at,
                    score_delta = EXCLUDED.score_delta,
                    ledger_id = EXCLUDED.ledger_id,
                    correction_of_contribution_id = EXCLUDED.correction_of_contribution_id,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(contribution, "contributionId"), text(contribution, "accountId"), text(contribution, "memberId"),
                text(contribution, "userId"), text(contribution, "type"), text(contribution, "sourceModule"), text(contribution, "sourceId"),
                text(contribution, "title"), text(contribution, "description"), ts(text(contribution, "occurredAt")),
                intValue(contribution.get("scoreDelta"), 0), text(contribution, "ledgerId"), text(contribution, "operatorUserId"),
                text(contribution, "correctionOfContributionId"), ts(text(contribution, "createdAt")), ts(text(contribution, "updatedAt")));
    }

    private void upsertMonthlyRun(Map<String, Object> run) {
        jdbc.update("""
                INSERT INTO attendance_monthly_runs(id, run_id, cycle_key, status, dry_run, reason, deduction_score, eligible_accounts, deducted_accounts, skipped_accounts, candidate_created, idempotency_key, started_at, completed_at, failure_reason, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (run_id) DO UPDATE SET status = EXCLUDED.status
                """, UUID.randomUUID(), text(run, "runId"), text(run, "cycleKey"), text(run, "status"), Boolean.TRUE.equals(run.get("dryRun")),
                text(run, "reason"), intValue(run.get("deductionScore"), 0), intValue(run.get("eligibleAccounts"), 0),
                intValue(run.get("deductedAccounts"), 0), intValue(run.get("skippedAccounts"), 0), intValue(run.get("candidateCreated"), 0),
                text(run, "idempotencyKey"), ts(text(run, "startedAt")), ts(text(run, "completedAt")), text(run, "failureReason"),
                text(run, "createdBy"), ts(text(run, "createdAt")));
    }

    private void upsertCandidate(Map<String, Object> candidate) {
        jdbc.update("""
                INSERT INTO attendance_removal_candidates(id, candidate_id, account_id, member_id, user_id, display_name_snapshot, score_balance, cycle_key, status, reason, public_reason, recommended_action, confirmed_by, confirmed_at, dismissed_by, dismissed_at, dismiss_reason, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (candidate_id) DO UPDATE SET
                    score_balance = EXCLUDED.score_balance,
                    status = EXCLUDED.status,
                    reason = EXCLUDED.reason,
                    public_reason = EXCLUDED.public_reason,
                    confirmed_by = EXCLUDED.confirmed_by,
                    confirmed_at = EXCLUDED.confirmed_at,
                    dismissed_by = EXCLUDED.dismissed_by,
                    dismissed_at = EXCLUDED.dismissed_at,
                    dismiss_reason = EXCLUDED.dismiss_reason,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(candidate, "candidateId"), text(candidate, "accountId"), text(candidate, "memberId"),
                text(candidate, "userId"), text(candidate, "displayNameSnapshot"), intValue(candidate.get("scoreBalance"), 0),
                text(candidate, "cycleKey"), text(candidate, "status"), text(candidate, "reason"), text(candidate, "publicReason"),
                text(candidate, "recommendedAction"), text(candidate, "confirmedBy"), ts(text(candidate, "confirmedAt")),
                text(candidate, "dismissedBy"), ts(text(candidate, "dismissedAt")), text(candidate, "dismissReason"),
                ts(text(candidate, "createdAt")), ts(text(candidate, "updatedAt")));
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), 'SUCCESS', NULL, now())
                """, UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "USER" : actorRole, json(List.of()), sourceIp(request), targetType, targetId, action,
                riskLevel, reason, json(Map.of("action", action)), json(stateRow(beforeStatus)), json(stateRow(afterStatus)));
    }

    private void insertIdempotency(String actorUserId, String scope, String idempotencyKey, String fingerprint, int responseCode, Map<String, Object> responseBody) {
        if (idempotencyKey == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO app_idempotency_records(id, actor_user_id, scope, idempotency_key, request_fingerprint, response_code, response_body, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), now(), now() + interval '24 hours')
                ON CONFLICT (actor_user_id, scope, idempotency_key) DO NOTHING
                """, UUID.randomUUID(), actorUserId, scope, idempotencyKey, fingerprint, responseCode, json(Map.of("code", 0, "message", "success", "data", responseBody)));
    }

    private void insertRequestLog(HttpServletRequest request, String actorUserId, int responseCode) {
        jdbc.update("""
                INSERT INTO app_request_logs(id, request_id, method, path, actor_user_id, source_ip, response_code, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS', NULL, now())
                ON CONFLICT (request_id) DO NOTHING
                """, UUID.randomUUID(), requestId(request), request.getMethod(), request.getRequestURI(), actorUserId, sourceIp(request), responseCode);
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? Objects.toString(request.getHeader("X-Request-Id"), "req-" + UUID.randomUUID()) : value.toString();
    }

    private String sourceIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long countWhere(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize attendance json", exception);
        }
    }

    private Map<String, Object> stateRow(String status) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("status", status);
        return row;
    }

    private OffsetDateTime ts(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? null : OffsetDateTime.parse(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }

    private String text(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }
}
