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

public class OnboardingPostgresPersistence implements OnboardingPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OnboardingPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void seed(List<Map<String, Object>> applications, List<Map<String, Object>> audits) {
        jdbc.update("DELETE FROM onboarding_handoff_snapshots");
        jdbc.update("DELETE FROM onboarding_state_events");
        jdbc.update("DELETE FROM onboarding_confirmations");
        jdbc.update("DELETE FROM onboarding_applications");
        jdbc.update("DELETE FROM app_idempotency_records WHERE scope LIKE 'onboarding.%'");
        jdbc.update("DELETE FROM app_audit_logs WHERE target_type = 'ONBOARDING_APPLICATION'");
        jdbc.update("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/onboarding%'");
        for (Map<String, Object> application : applications) {
            upsertApplication(application);
        }
        for (Map<String, Object> audit : audits) {
            insertSeedAudit(audit);
        }
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
            throw new IllegalStateException("failed to parse onboarding idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistApplicationWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> responseBody, int responseCode) {
        upsertApplication(application);
        insertStateEvent(request, actorUserId, text(application, "applicationId"), action, beforeStatus, afterStatus, reason, responseBody);
        insertAudit(request, actorUserId, actorRole, text(application, "applicationId"), action, riskLevel, beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistConfirmationWrite(HttpServletRequest request, String actorUserId, String actorRole, String confirmationType, String action, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> confirmation, Map<String, Object> responseBody, int responseCode) {
        upsertApplication(application);
        insertConfirmation(text(application, "applicationId"), confirmationType, confirmation);
        insertStateEvent(request, actorUserId, text(application, "applicationId"), action, text(application, "status"), text(application, "status"), null, responseBody);
        insertAudit(request, actorUserId, actorRole, text(application, "applicationId"), action, "LOW", text(application, "status"), text(application, "status"), null);
        insertIdempotency(actorUserId, scopeForConfirmation(confirmationType), idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> application, String targetModule, int handoffVersion, Map<String, Object> snapshot) {
        jdbc.update("""
                INSERT INTO onboarding_handoff_snapshots(id, handoff_id, application_id, target_module, handoff_version, snapshot_payload, generated_by, request_id, generated_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, now())
                """, UUID.randomUUID(), "handoff-" + UUID.randomUUID(), text(application, "applicationId"), targetModule, handoffVersion, json(snapshot), actorUserId, requestId(request));
        insertRequestLog(request, actorUserId, 200);
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of(
                "storageMode", "POSTGRESQL_PRIMARY",
                "applicationsTotal", count("onboarding_applications"),
                "confirmationsTotal", count("onboarding_confirmations"),
                "stateEventsTotal", count("onboarding_state_events"),
                "handoffSnapshotsTotal", count("onboarding_handoff_snapshots"),
                "auditsTotal", countWhere("app_audit_logs", "target_type = 'ONBOARDING_APPLICATION'"),
                "idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'onboarding.%'"),
                "requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/onboarding%'")
        );
    }

    private void upsertApplication(Map<String, Object> application) {
        jdbc.update("""
                INSERT INTO onboarding_applications(id, application_id, user_id, display_name_snapshot, auth_status_snapshot, minecraft_binding_snapshot, status, previous_status, review_direction, profile_confirmation, rule_confirmation, blocked_reason, blocked_by, blocked_at, notification_status, created_at, updated_at, completed_at, cancelled_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (application_id) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    display_name_snapshot = EXCLUDED.display_name_snapshot,
                    auth_status_snapshot = EXCLUDED.auth_status_snapshot,
                    minecraft_binding_snapshot = EXCLUDED.minecraft_binding_snapshot,
                    status = EXCLUDED.status,
                    previous_status = EXCLUDED.previous_status,
                    review_direction = EXCLUDED.review_direction,
                    profile_confirmation = EXCLUDED.profile_confirmation,
                    rule_confirmation = EXCLUDED.rule_confirmation,
                    blocked_reason = EXCLUDED.blocked_reason,
                    blocked_by = EXCLUDED.blocked_by,
                    blocked_at = EXCLUDED.blocked_at,
                    notification_status = EXCLUDED.notification_status,
                    updated_at = EXCLUDED.updated_at,
                    completed_at = EXCLUDED.completed_at,
                    cancelled_at = EXCLUDED.cancelled_at
                """, UUID.randomUUID(), text(application, "applicationId"), text(application, "userId"), text(application, "displayNameSnapshot"),
                textOrDefault(application, "authStatusSnapshot", "ACTIVE"), json(application.get("minecraftBindingSnapshot")), text(application, "status"),
                text(application, "previousStatus"), text(application, "reviewDirection"), json(application.get("profileConfirmation")),
                json(application.get("ruleConfirmation")), text(application, "blockedReason"), text(application, "blockedBy"),
                ts(text(application, "blockedAt")), text(application, "notificationStatus"), ts(text(application, "createdAt")),
                ts(text(application, "updatedAt")), ts(text(application, "completedAt")), ts(text(application, "cancelledAt")));
    }

    private void insertConfirmation(String applicationId, String confirmationType, Map<String, Object> confirmation) {
        jdbc.update("""
                INSERT INTO onboarding_confirmations(id, confirmation_id, application_id, confirmation_type, confirmation_payload, confirmed_at, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, now())
                ON CONFLICT (application_id, confirmation_type) DO UPDATE SET
                    confirmation_payload = EXCLUDED.confirmation_payload,
                    confirmed_at = EXCLUDED.confirmed_at
                """, UUID.randomUUID(), "confirm-" + applicationId + "-" + confirmationType, applicationId, confirmationType, json(confirmation), ts(text(confirmation, "confirmedAt")));
    }

    private void insertStateEvent(HttpServletRequest request, String actorUserId, String applicationId, String action, String beforeStatus, String afterStatus, String reason, Map<String, Object> payload) {
        jdbc.update("""
                INSERT INTO onboarding_state_events(id, event_id, application_id, actor_user_id, action, before_status, after_status, reason, event_payload, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
                """, UUID.randomUUID(), "event-" + UUID.randomUUID(), applicationId, actorUserId, action, beforeStatus, afterStatus, reason, json(payload), requestId(request));
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, 'ONBOARDING_APPLICATION', ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), 'SUCCESS', NULL, now())
                """, UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "USER" : actorRole, json(List.of()), sourceIp(request), targetId, action,
                riskLevel, reason, json(Map.of("action", action)), json(stateRow(beforeStatus)), json(stateRow(afterStatus)));
    }

    private void insertSeedAudit(Map<String, Object> audit) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), NULL, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)
                """, UUID.randomUUID(), text(audit, "requestId"), text(audit, "actorUserId"), textOrDefault(audit, "actorRole", "ADMIN"), json(List.of()),
                text(audit, "targetType"), text(audit, "targetId"), text(audit, "action"), text(audit, "riskLevel"), text(audit, "reasonSummary"),
                json(Map.of("seed", true)), json(stateRow(text(audit, "beforeState"))), json(stateRow(text(audit, "afterState"))),
                text(audit, "result"), text(audit, "failureReason"), ts(text(audit, "createdAt")));
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

    private String scopeForConfirmation(String confirmationType) {
        return "PROFILE".equals(confirmationType) ? "onboarding.profile-confirmation" : "onboarding.rules-confirmation";
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
            throw new IllegalStateException("failed to serialize onboarding json", exception);
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

    private String text(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }

    private String textOrDefault(Map<String, Object> source, String key, String fallback) {
        String value = text(source, key);
        return value == null ? fallback : value;
    }
}
