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
    public List<Map<String, Object>> loadAccounts() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT account_id, user_id, member_id, display_name_snapshot, avatar_url_snapshot,
                       member_group_snapshot, member_status_snapshot, minecraft_binding_snapshot::text AS minecraft_binding_snapshot,
                       status, score_balance, initial_score, total_earned, total_deducted,
                       last_positive_activity_at, last_deducted_at, last_ledger_id,
                       whitelist_application_id, whitelist_handoff_id, whitelist_handoff_version,
                       review_direction, attempt_type, notification_status, notification_failure::text AS notification_failure,
                       profile_snapshot_stale, created_at, updated_at, archived_at
                  FROM attendance_accounts
                 ORDER BY account_id
                """);
        List<Map<String, Object>> accounts = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> account = new java.util.LinkedHashMap<>();
            account.put("accountId", row.get("account_id"));
            account.put("userId", row.get("user_id"));
            account.put("memberId", row.get("member_id"));
            account.put("displayNameSnapshot", row.get("display_name_snapshot"));
            account.put("avatarUrlSnapshot", row.get("avatar_url_snapshot"));
            account.put("memberGroupSnapshot", row.get("member_group_snapshot"));
            account.put("memberStatusSnapshot", row.get("member_status_snapshot"));
            Object binding = readJsonOrNull(row.get("minecraft_binding_snapshot"));
            account.put("minecraftBindingSnapshot", binding == null ? Map.of() : binding);
            account.put("status", row.get("status"));
            account.put("scoreBalance", intOf(row.get("score_balance")));
            account.put("initialScore", intOf(row.get("initial_score")));
            account.put("totalEarned", intOf(row.get("total_earned")));
            account.put("totalDeducted", intOf(row.get("total_deducted")));
            account.put("lastPositiveActivityAt", stringOf(row.get("last_positive_activity_at")));
            account.put("lastDeductedAt", stringOf(row.get("last_deducted_at")));
            account.put("lastLedgerId", row.get("last_ledger_id"));
            account.put("whitelistApplicationId", row.get("whitelist_application_id"));
            account.put("whitelistHandoffId", row.get("whitelist_handoff_id"));
            account.put("whitelistHandoffVersion", intOf(row.get("whitelist_handoff_version")));
            account.put("reviewDirection", row.get("review_direction"));
            account.put("attemptType", row.get("attempt_type"));
            account.put("notificationStatus", row.get("notification_status"));
            account.put("notificationFailure", readJsonOrNull(row.get("notification_failure")));
            account.put("profileSnapshotStale", row.get("profile_snapshot_stale"));
            account.put("createdAt", stringOf(row.get("created_at")));
            account.put("updatedAt", stringOf(row.get("updated_at")));
            account.put("archivedAt", stringOf(row.get("archived_at")));
            accounts.add(account);
        }
        return accounts;
    }

    @Override
    public List<Map<String, Object>> loadLedgers() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT ledger_id, account_id, member_id, user_id, type, status, delta, balance_before, balance_after,
                       source_module, source_id, cycle_key, reason, public_reason, operator_user_id, idempotency_key,
                       reversal_of_ledger_id, reversed_by_ledger_id, notification_status,
                       notification_failure::text AS notification_failure, created_at, reversed_at
                  FROM attendance_ledgers
                 ORDER BY ledger_id
                """);
        List<Map<String, Object>> ledgers = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> ledger = new java.util.LinkedHashMap<>();
            ledger.put("ledgerId", row.get("ledger_id"));
            ledger.put("accountId", row.get("account_id"));
            ledger.put("memberId", row.get("member_id"));
            ledger.put("userId", row.get("user_id"));
            ledger.put("type", row.get("type"));
            ledger.put("status", row.get("status"));
            ledger.put("delta", intOf(row.get("delta")));
            ledger.put("balanceBefore", intOf(row.get("balance_before")));
            ledger.put("balanceAfter", intOf(row.get("balance_after")));
            ledger.put("sourceModule", row.get("source_module"));
            ledger.put("sourceId", row.get("source_id"));
            ledger.put("cycleKey", row.get("cycle_key"));
            ledger.put("reason", row.get("reason"));
            ledger.put("publicReason", row.get("public_reason"));
            ledger.put("operatorUserId", row.get("operator_user_id"));
            ledger.put("idempotencyKey", row.get("idempotency_key"));
            ledger.put("reversalOfLedgerId", row.get("reversal_of_ledger_id"));
            ledger.put("reversedByLedgerId", row.get("reversed_by_ledger_id"));
            ledger.put("notificationStatus", row.get("notification_status"));
            ledger.put("notificationFailure", readJsonOrNull(row.get("notification_failure")));
            ledger.put("createdAt", stringOf(row.get("created_at")));
            ledger.put("reversedAt", stringOf(row.get("reversed_at")));
            ledgers.add(ledger);
        }
        return ledgers;
    }

    @Override
    public List<Map<String, Object>> loadContributions() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT contribution_id, account_id, member_id, user_id, type, source_module, source_id,
                       title, description, occurred_at, score_delta, ledger_id, operator_user_id,
                       correction_of_contribution_id, created_at, updated_at
                  FROM attendance_contributions
                 ORDER BY contribution_id
                """);
        List<Map<String, Object>> contributions = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> contribution = new java.util.LinkedHashMap<>();
            contribution.put("contributionId", row.get("contribution_id"));
            contribution.put("accountId", row.get("account_id"));
            contribution.put("memberId", row.get("member_id"));
            contribution.put("userId", row.get("user_id"));
            contribution.put("type", row.get("type"));
            contribution.put("sourceModule", row.get("source_module"));
            contribution.put("sourceId", row.get("source_id"));
            contribution.put("title", row.get("title"));
            contribution.put("description", row.get("description"));
            contribution.put("occurredAt", stringOf(row.get("occurred_at")));
            contribution.put("scoreDelta", intOf(row.get("score_delta")));
            contribution.put("ledgerId", row.get("ledger_id"));
            contribution.put("operatorUserId", row.get("operator_user_id"));
            contribution.put("correctionOfContributionId", row.get("correction_of_contribution_id"));
            contribution.put("createdAt", stringOf(row.get("created_at")));
            contribution.put("updatedAt", stringOf(row.get("updated_at")));
            contributions.add(contribution);
        }
        return contributions;
    }

    @Override
    public List<Map<String, Object>> loadMonthlyRuns() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT run_id, cycle_key, status, dry_run, reason, deduction_score, eligible_accounts,
                       deducted_accounts, skipped_accounts, candidate_created, idempotency_key,
                       started_at, completed_at, failure_reason, created_by, created_at
                  FROM attendance_monthly_runs
                 ORDER BY run_id
                """);
        List<Map<String, Object>> runs = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> run = new java.util.LinkedHashMap<>();
            run.put("runId", row.get("run_id"));
            run.put("cycleKey", row.get("cycle_key"));
            run.put("status", row.get("status"));
            run.put("dryRun", row.get("dry_run"));
            run.put("reason", row.get("reason"));
            run.put("deductionScore", intOf(row.get("deduction_score")));
            run.put("eligibleAccounts", intOf(row.get("eligible_accounts")));
            run.put("deductedAccounts", intOf(row.get("deducted_accounts")));
            run.put("skippedAccounts", intOf(row.get("skipped_accounts")));
            run.put("candidateCreated", intOf(row.get("candidate_created")));
            run.put("idempotencyKey", row.get("idempotency_key"));
            run.put("startedAt", stringOf(row.get("started_at")));
            run.put("completedAt", stringOf(row.get("completed_at")));
            run.put("failureReason", row.get("failure_reason"));
            run.put("createdBy", row.get("created_by"));
            run.put("createdAt", stringOf(row.get("created_at")));
            runs.add(run);
        }
        return runs;
    }

    @Override
    public List<Map<String, Object>> loadRemovalCandidates() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT candidate_id, account_id, member_id, user_id, display_name_snapshot, score_balance,
                       cycle_key, status, reason, public_reason, recommended_action,
                       confirmed_by, confirmed_at, dismissed_by, dismissed_at, dismiss_reason,
                       created_at, updated_at
                  FROM attendance_removal_candidates
                 ORDER BY candidate_id
                """);
        List<Map<String, Object>> candidates = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> candidate = new java.util.LinkedHashMap<>();
            candidate.put("candidateId", row.get("candidate_id"));
            candidate.put("accountId", row.get("account_id"));
            candidate.put("memberId", row.get("member_id"));
            candidate.put("userId", row.get("user_id"));
            candidate.put("displayNameSnapshot", row.get("display_name_snapshot"));
            candidate.put("scoreBalance", intOf(row.get("score_balance")));
            candidate.put("cycleKey", row.get("cycle_key"));
            candidate.put("status", row.get("status"));
            candidate.put("reason", row.get("reason"));
            candidate.put("publicReason", row.get("public_reason"));
            candidate.put("recommendedAction", row.get("recommended_action"));
            candidate.put("confirmedBy", row.get("confirmed_by"));
            candidate.put("confirmedAt", stringOf(row.get("confirmed_at")));
            candidate.put("dismissedBy", row.get("dismissed_by"));
            candidate.put("dismissedAt", stringOf(row.get("dismissed_at")));
            candidate.put("dismissReason", row.get("dismiss_reason"));
            candidate.put("createdAt", stringOf(row.get("created_at")));
            candidate.put("updatedAt", stringOf(row.get("updated_at")));
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    public List<Map<String, Object>> loadLeaderboardSnapshots() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT snapshot_id, cycle_key, entries_payload::text AS entries_payload, rebuilt_by, request_id, rebuilt_at
                  FROM attendance_leaderboard_snapshots
                 ORDER BY rebuilt_at
                """);
        List<Map<String, Object>> snapshots = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("snapshotId", row.get("snapshot_id"));
            snapshot.put("cycleKey", row.get("cycle_key"));
            snapshot.put("entries", readJsonOrNull(row.get("entries_payload")));
            snapshot.put("rebuiltBy", row.get("rebuilt_by"));
            snapshot.put("requestId", row.get("request_id"));
            snapshot.put("rebuiltAt", stringOf(row.get("rebuilt_at")));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /** jsonb 列读回来是字符串，解回对象；空值返回 null（区别于「空集合」语义）。 */
    private Object readJsonOrNull(Object raw) {
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw.toString(), Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private int intOf(Object value) {
        if (value instanceof Number number) return number.intValue();
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    private String stringOf(Object value) {
        return value == null ? null : value.toString();
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
