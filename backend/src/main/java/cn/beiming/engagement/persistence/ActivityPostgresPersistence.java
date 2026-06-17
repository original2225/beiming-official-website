package cn.beiming.engagement.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ActivityPostgresPersistence implements ActivityPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ActivityPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
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
            throw new IllegalStateException("failed to parse activity idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action,
                             String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus,
                             String reason, String idempotencyKey, String fingerprint, Map<String, Object> snapshot,
                             Map<String, Object> responseBody, int responseCode) {
        persistSnapshot(snapshot);
        insertAudit(request, actorUserId, actorRole, targetType, targetId, action, riskLevel, beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    public Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("storageMode", "POSTGRESQL_PRIMARY");
        counts.put("activitiesTotal", count("activity_events"));
        counts.put("publishedActivitiesTotal", countWhere("activity_events", "status IN ('PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'RUNNING', 'COMPLETED', 'RESULT_PUBLISHED')"));
        counts.put("openRegistrationsTotal", count("activity_registrations"));
        counts.put("waitlistedRegistrationsTotal", countWhere("activity_registrations", "status = 'WAITLISTED'"));
        counts.put("checkedInRegistrationsTotal", countWhere("activity_registrations", "status = 'CHECKED_IN'"));
        counts.put("resultsPublishedTotal", countWhere("activity_results", "status = 'PUBLISHED'"));
        counts.put("rewardsTotal", count("activity_rewards"));
        counts.put("contributionCandidatesTotal", count("activity_contribution_candidates"));
        counts.put("auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'ACTIVITY%'"));
        counts.put("idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'activity.%'"));
        counts.put("requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/activity%'"));
        return counts;
    }

    private void persistSnapshot(Map<String, Object> snapshot) {
        String type = text(snapshot, "snapshotType");
        if ("EVENT".equals(type)) {
            upsertEvent(snapshot);
        } else if ("REGISTRATION".equals(type)) {
            upsertRegistration(snapshot);
        } else if ("RESULT".equals(type)) {
            upsertResult(snapshot);
        } else if ("REWARD".equals(type)) {
            upsertReward(snapshot);
        } else if ("CANDIDATES".equals(type)) {
            upsertCandidates(snapshot);
        }
    }

    private void upsertEvent(Map<String, Object> event) {
        jdbc.update("""
                INSERT INTO activity_events(id, activity_id, slug, title, summary, description, type, visibility, registration_policy, status, start_at, end_at, registration_open_at, registration_close_at, capacity, waitlist_capacity, confirmed_count, waitlisted_count, checked_in_count, no_show_count, location_text, cover_image_url, tags, created_by, notification_failure, created_at, updated_at, submitted_at, reviewed_at, published_at, offline_at, archived_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (activity_id) DO UPDATE SET
                    slug = EXCLUDED.slug,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    description = EXCLUDED.description,
                    type = EXCLUDED.type,
                    visibility = EXCLUDED.visibility,
                    registration_policy = EXCLUDED.registration_policy,
                    status = EXCLUDED.status,
                    start_at = EXCLUDED.start_at,
                    end_at = EXCLUDED.end_at,
                    registration_open_at = EXCLUDED.registration_open_at,
                    registration_close_at = EXCLUDED.registration_close_at,
                    capacity = EXCLUDED.capacity,
                    waitlist_capacity = EXCLUDED.waitlist_capacity,
                    confirmed_count = EXCLUDED.confirmed_count,
                    waitlisted_count = EXCLUDED.waitlisted_count,
                    checked_in_count = EXCLUDED.checked_in_count,
                    no_show_count = EXCLUDED.no_show_count,
                    location_text = EXCLUDED.location_text,
                    cover_image_url = EXCLUDED.cover_image_url,
                    tags = EXCLUDED.tags,
                    notification_failure = EXCLUDED.notification_failure,
                    updated_at = EXCLUDED.updated_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    published_at = EXCLUDED.published_at,
                    offline_at = EXCLUDED.offline_at,
                    archived_at = EXCLUDED.archived_at,
                    deleted_at = EXCLUDED.deleted_at
                """, UUID.randomUUID(), text(event, "activityId"), text(event, "slug"), text(event, "title"), text(event, "summary"),
                text(event, "description"), text(event, "type"), text(event, "visibility"), text(event, "registrationPolicy"),
                text(event, "status"), ts(text(event, "startAt")), ts(text(event, "endAt")), ts(text(event, "registrationOpenAt")),
                ts(text(event, "registrationCloseAt")), intValue(event.get("capacity"), 1), intValue(event.get("waitlistCapacity"), 0),
                intValue(event.get("confirmedCount"), 0), intValue(event.get("waitlistedCount"), 0), intValue(event.get("checkedInCount"), 0),
                intValue(event.get("noShowCount"), 0), text(event, "locationText"), text(event, "coverImageUrl"), json(event.get("tags")),
                text(event, "createdBy"), json(event.get("notificationFailure")), ts(text(event, "createdAt")), ts(text(event, "updatedAt")),
                ts(text(event, "submittedAt")), ts(text(event, "reviewedAt")), ts(text(event, "publishedAt")), ts(text(event, "offlineAt")),
                ts(text(event, "archivedAt")), ts(text(event, "deletedAt")));
    }

    private void upsertRegistration(Map<String, Object> registration) {
        Map<?, ?> participant = map(registration.get("participant"));
        jdbc.update("""
                INSERT INTO activity_registrations(id, registration_id, activity_id, user_id, member_id, participant_snapshot, status, answers, guest_count, waitlist_rank, notification_failure, created_at, updated_at, checked_in_at, no_show_at, canceled_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                ON CONFLICT (registration_id) DO UPDATE SET
                    participant_snapshot = EXCLUDED.participant_snapshot,
                    status = EXCLUDED.status,
                    answers = EXCLUDED.answers,
                    guest_count = EXCLUDED.guest_count,
                    waitlist_rank = EXCLUDED.waitlist_rank,
                    notification_failure = EXCLUDED.notification_failure,
                    updated_at = EXCLUDED.updated_at,
                    checked_in_at = EXCLUDED.checked_in_at,
                    no_show_at = EXCLUDED.no_show_at,
                    canceled_at = EXCLUDED.canceled_at
                """, UUID.randomUUID(), text(registration, "registrationId"), text(registration, "activityId"),
                Objects.toString(participant.get("userId"), null), Objects.toString(participant.get("memberId"), null),
                json(participant), text(registration, "status"), json(registration.get("answers")),
                intValue(registration.get("guestCount"), 0), intNullable(registration.get("waitlistRank")),
                json(registration.get("notificationFailure")), ts(text(registration, "createdAt")), ts(text(registration, "updatedAt")),
                ts(text(registration, "checkedInAt")), ts(text(registration, "noShowAt")), ts(text(registration, "canceledAt")));
    }

    private void upsertResult(Map<String, Object> result) {
        jdbc.update("""
                INSERT INTO activity_results(id, result_id, activity_id, status, title, summary, details, participant_total, winner_total, published_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (result_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    details = EXCLUDED.details,
                    participant_total = EXCLUDED.participant_total,
                    winner_total = EXCLUDED.winner_total,
                    published_at = EXCLUDED.published_at,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(result, "resultId"), text(result, "activityId"), text(result, "status"),
                text(result, "title"), text(result, "summary"), text(result, "details"),
                intValue(result.get("participantTotal"), 0), intValue(result.get("winnerTotal"), 0),
                ts(text(result, "publishedAt")), ts(text(result, "createdAt")), ts(text(result, "updatedAt")));
    }

    private void upsertReward(Map<String, Object> reward) {
        Map<?, ?> recipient = map(reward.get("recipient"));
        jdbc.update("""
                INSERT INTO activity_rewards(id, reward_id, activity_id, registration_id, user_id, member_id, recipient_snapshot, type, title, description, quantity, score_candidate_delta, status, notification_failure, issued_at, revoked_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (reward_id) DO UPDATE SET
                    recipient_snapshot = EXCLUDED.recipient_snapshot,
                    type = EXCLUDED.type,
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    quantity = EXCLUDED.quantity,
                    score_candidate_delta = EXCLUDED.score_candidate_delta,
                    status = EXCLUDED.status,
                    notification_failure = EXCLUDED.notification_failure,
                    issued_at = EXCLUDED.issued_at,
                    revoked_at = EXCLUDED.revoked_at,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(reward, "rewardId"), text(reward, "activityId"), text(reward, "registrationId"),
                Objects.toString(recipient.get("userId"), null), Objects.toString(recipient.get("memberId"), null),
                json(recipient), text(reward, "type"), text(reward, "title"), text(reward, "description"),
                intValue(reward.get("quantity"), 1), intValue(reward.get("scoreCandidateDelta"), 0), text(reward, "status"),
                json(reward.get("notificationFailure")), ts(text(reward, "issuedAt")), ts(text(reward, "revokedAt")),
                ts(text(reward, "createdAt")), ts(text(reward, "updatedAt")));
    }

    private void upsertCandidates(Map<String, Object> payload) {
        Object items = payload.get("items");
        if (!(items instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> candidate) {
                upsertCandidate(candidate);
            }
        }
    }

    private void upsertCandidate(Map<?, ?> candidate) {
        jdbc.update("""
                INSERT INTO activity_contribution_candidates(id, candidate_id, activity_id, reward_id, member_id, user_id, title, description, score_delta, status, attendance_response_summary, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (candidate_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    attendance_response_summary = EXCLUDED.attendance_response_summary,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), Objects.toString(candidate.get("candidateId"), null), Objects.toString(candidate.get("activityId"), null),
                Objects.toString(candidate.get("rewardId"), null), Objects.toString(candidate.get("memberId"), null),
                Objects.toString(candidate.get("userId"), null), Objects.toString(candidate.get("title"), null),
                Objects.toString(candidate.get("description"), null), intValue(candidate.get("scoreDelta"), 0),
                Objects.toString(candidate.get("status"), "PENDING"), json(candidate.get("attendanceResponseSummary")),
                ts(Objects.toString(candidate.get("createdAt"), null)), ts(Objects.toString(candidate.get("updatedAt"), null)));
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), 'SUCCESS', NULL, now())
                """, UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "USER" : actorRole, json(List.of()), sourceIp(request),
                targetType, targetId, action, riskLevel, reason, json(Map.of("action", action)), json(stateRow(beforeStatus)), json(stateRow(afterStatus)));
    }

    private void insertIdempotency(String actorUserId, String scope, String idempotencyKey, String fingerprint, int responseCode, Map<String, Object> responseBody) {
        if (idempotencyKey == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO app_idempotency_records(id, actor_user_id, scope, idempotency_key, request_fingerprint, response_code, response_body, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), now(), now() + interval '24 hours')
                ON CONFLICT (actor_user_id, scope, idempotency_key) DO NOTHING
                """, UUID.randomUUID(), actorUserId, scope, idempotencyKey, fingerprint, responseCode,
                json(Map.of("code", 0, "message", "success", "data", responseBody)));
    }

    private void insertRequestLog(HttpServletRequest request, String actorUserId, int responseCode) {
        jdbc.update("""
                INSERT INTO app_request_logs(id, request_id, method, path, actor_user_id, source_ip, response_code, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS', NULL, now())
                ON CONFLICT (request_id) DO NOTHING
                """, UUID.randomUUID(), requestId(request), request.getMethod(), request.getRequestURI(), actorUserId, sourceIp(request), responseCode);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long countWhere(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
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

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize activity json", exception);
        }
    }

    private Map<String, Object> stateRow(String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", status);
        return row;
    }

    private OffsetDateTime ts(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? null : OffsetDateTime.parse(value);
    }

    private Integer intNullable(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value.toString());
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

    private Map<?, ?> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }
}
