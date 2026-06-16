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

public class WhitelistPostgresPersistence implements WhitelistPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public WhitelistPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
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
            throw new IllegalStateException("failed to parse whitelist idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
        insertAudit(request, actorUserId, actorRole, targetType, targetId, action, riskLevel, beforeStatus, afterStatus, reason);
    }

    @Override
    @Transactional
    public void persistApplicationWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> responseBody, int responseCode) {
        upsertApplication(application);
        insertStateEvent(request, actorUserId, text(application, "applicationId"), action, beforeStatus, afterStatus, reason, responseBody);
        persistActivationAndHandoff(request, actorUserId, application);
        insertAudit(request, actorUserId, actorRole, "WHITELIST_APPLICATION", text(application, "applicationId"), action, "MEDIUM", beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of(
                "storageMode", "POSTGRESQL_PRIMARY",
                "applicationsTotal", count("whitelist_applications"),
                "approvedTotal", countWhere("whitelist_applications", "status = 'APPROVED'"),
                "attendanceHandoffsTotal", count("whitelist_attendance_handoffs"),
                "auditsTotal", countWhere("app_audit_logs", "target_type = 'WHITELIST_APPLICATION'"),
                "idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'whitelist.%'"),
                "requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/whitelist%'")
        );
    }

    private void upsertApplication(Map<String, Object> app) {
        jdbc.update("""
                INSERT INTO whitelist_applications(id, application_id, exam_session_id, onboarding_application_id, exam_handoff_version, onboarding_handoff_version, user_id, display_name_snapshot, minecraft_binding_snapshot, review_direction, attempt_type, status, result, materials_payload, score_summary, exam_passed_at, reviewer_user_id, reviewer_display_name_snapshot, review_comment, internal_note, supplement_request, profile_activation, attendance_handoff, notification_status, notification_failure, removed_at, removed_by, removal_reason, reapply_required, next_exam_attempt_type, created_at, updated_at, submitted_at, reviewed_at, approved_at, rejected_at, withdrawn_at, archived_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (application_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    result = EXCLUDED.result,
                    materials_payload = EXCLUDED.materials_payload,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    reviewer_display_name_snapshot = EXCLUDED.reviewer_display_name_snapshot,
                    review_comment = EXCLUDED.review_comment,
                    internal_note = EXCLUDED.internal_note,
                    supplement_request = EXCLUDED.supplement_request,
                    profile_activation = EXCLUDED.profile_activation,
                    attendance_handoff = EXCLUDED.attendance_handoff,
                    notification_status = EXCLUDED.notification_status,
                    notification_failure = EXCLUDED.notification_failure,
                    removed_at = EXCLUDED.removed_at,
                    removed_by = EXCLUDED.removed_by,
                    removal_reason = EXCLUDED.removal_reason,
                    reapply_required = EXCLUDED.reapply_required,
                    next_exam_attempt_type = EXCLUDED.next_exam_attempt_type,
                    updated_at = EXCLUDED.updated_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    approved_at = EXCLUDED.approved_at,
                    rejected_at = EXCLUDED.rejected_at,
                    withdrawn_at = EXCLUDED.withdrawn_at,
                    archived_at = EXCLUDED.archived_at
                """, UUID.randomUUID(), text(app, "applicationId"), text(app, "examSessionId"), text(app, "onboardingApplicationId"),
                intValue(app.get("examHandoffVersion"), 1), intValue(app.get("onboardingHandoffVersion"), 1), text(app, "userId"), text(app, "displayNameSnapshot"),
                json(app.get("minecraftBindingSnapshot")), text(app, "reviewDirection"), text(app, "attemptType"), text(app, "status"), text(app, "result"),
                json(app.get("materials")), json(app.get("scoreSummary")), ts(text(app, "examPassedAt")), text(app, "reviewerUserId"), text(app, "reviewerDisplayNameSnapshot"),
                text(app, "reviewComment"), text(app, "internalNote"), json(app.get("supplementRequest")), json(app.get("profileActivation")), json(app.get("attendanceHandoff")),
                text(app, "notificationStatus"), json(app.get("notificationFailure")), ts(text(app, "removedAt")), text(app, "removedBy"), text(app, "removalReason"),
                Boolean.TRUE.equals(app.get("reapplyRequired")), text(app, "nextExamAttemptType"), ts(text(app, "createdAt")), ts(text(app, "updatedAt")),
                ts(text(app, "submittedAt")), ts(text(app, "reviewedAt")), ts(text(app, "approvedAt")), ts(text(app, "rejectedAt")), ts(text(app, "withdrawnAt")), ts(text(app, "archivedAt")));
    }

    private void insertStateEvent(HttpServletRequest request, String actorUserId, String applicationId, String action, String beforeStatus, String afterStatus, String reason, Map<String, Object> payload) {
        jdbc.update("""
                INSERT INTO whitelist_state_events(id, event_id, application_id, actor_user_id, action, before_status, after_status, reason, event_payload, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
                """, UUID.randomUUID(), "wl-event-" + UUID.randomUUID(), applicationId, actorUserId, action, beforeStatus, afterStatus, reason, json(payload), requestId(request));
    }

    private void persistActivationAndHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> app) {
        Map<String, Object> activation = mapValue(app.get("profileActivation"));
        if (!activation.isEmpty()) {
            jdbc.update("""
                    INSERT INTO whitelist_profile_activations(id, activation_id, application_id, member_id, status, activation_payload, request_id, created_at)
                    VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
                    ON CONFLICT (activation_id) DO NOTHING
                    """, UUID.randomUUID(), "activation-" + text(app, "applicationId"), text(app, "applicationId"), text(activation, "memberId"), text(activation, "status"), json(activation), requestId(request));
        }
        Map<String, Object> handoff = mapValue(app.get("attendanceHandoff"));
        if (!handoff.isEmpty()) {
            jdbc.update("""
                    INSERT INTO whitelist_attendance_handoffs(id, handoff_id, application_id, member_id, initialization_status, handoff_version, handoff_payload, generated_by, request_id, generated_at, consumed_at)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                    ON CONFLICT (handoff_id) DO UPDATE SET initialization_status = EXCLUDED.initialization_status, consumed_at = EXCLUDED.consumed_at
                    """, UUID.randomUUID(), text(handoff, "handoffId"), text(app, "applicationId"), text(handoff, "memberId"), text(handoff, "initializationStatus"),
                    intValue(handoff.get("handoffVersion"), 1), json(handoff), actorUserId, requestId(request), ts(text(handoff, "generatedAt")), ts(text(handoff, "consumedAt")));
        }
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
            throw new IllegalStateException("failed to serialize whitelist json", exception);
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

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((key, item) -> result.put(Objects.toString(key), item));
            return result;
        }
        return Map.of();
    }

    private String text(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }
}
