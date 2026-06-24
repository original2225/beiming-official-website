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

public class CalendarPostgresPersistence implements CalendarPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public CalendarPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
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
            throw new IllegalStateException("failed to parse calendar idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action,
                             String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus,
                             String reason, String idempotencyKey, String fingerprint, Map<String, Object> snapshot,
                             Map<String, Object> responseBody, int responseCode) {
        persistSnapshot(request, actorUserId, snapshot);
        insertAudit(request, actorUserId, actorRole, targetType, targetId, action, riskLevel, beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    public Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("storageMode", "POSTGRESQL_PRIMARY");
        counts.put("eventsTotal", count("calendar_events"));
        counts.put("publishedEventsTotal", countWhere("calendar_events", "status = 'PUBLISHED' AND visibility = 'PUBLIC'"));
        counts.put("watchesTotal", count("calendar_watches"));
        counts.put("activitySourceEventsTotal", countWhere("calendar_events", "source_type = 'ACTIVITY'"));
        counts.put("manualEventsTotal", countWhere("calendar_events", "source_type = 'MANUAL'"));
        counts.put("auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'CALENDAR%'"));
        counts.put("idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'calendar.%'"));
        counts.put("requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/calendar%'"));
        counts.put("lastActivitySyncAt", jdbc.queryForObject("SELECT MAX(created_at) FROM calendar_activity_sync_runs", OffsetDateTime.class));
        return counts;
    }

    private void persistSnapshot(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
        String type = text(snapshot, "snapshotType");
        if ("EVENT".equals(type)) {
            upsertEvent(snapshot);
        } else if ("WATCH".equals(type)) {
            upsertWatch(map(snapshot.get("watch")));
            Object event = snapshot.get("event");
            if (event instanceof Map<?, ?> eventMap) {
                upsertEvent(eventMap);
            }
        } else if ("ACTIVITY_SYNC".equals(type)) {
            upsertSyncRun(request, actorUserId, snapshot);
        }
    }

    private void upsertEvent(Map<?, ?> event) {
        Map<?, ?> source = map(event.get("source"));
        Map<?, ?> reminder = map(event.get("reminderPolicy"));
        jdbc.update("""
                INSERT INTO calendar_events(id, event_id, source_type, source_id, source_version, title, summary, description, type, status, visibility, start_at, end_at, timezone, all_day, location_text, related_url, labels, priority, watch_count, created_by, updated_by, reviewed_by, reminder_failure, source_snapshot_stale, created_at, updated_at, submitted_at, reviewed_at, published_at, offline_at, archived_at, deleted_at, last_synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO UPDATE SET
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    source_version = EXCLUDED.source_version,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    description = EXCLUDED.description,
                    type = EXCLUDED.type,
                    status = EXCLUDED.status,
                    visibility = EXCLUDED.visibility,
                    start_at = EXCLUDED.start_at,
                    end_at = EXCLUDED.end_at,
                    timezone = EXCLUDED.timezone,
                    all_day = EXCLUDED.all_day,
                    location_text = EXCLUDED.location_text,
                    related_url = EXCLUDED.related_url,
                    labels = EXCLUDED.labels,
                    priority = EXCLUDED.priority,
                    watch_count = EXCLUDED.watch_count,
                    updated_by = EXCLUDED.updated_by,
                    reviewed_by = EXCLUDED.reviewed_by,
                    reminder_failure = EXCLUDED.reminder_failure,
                    source_snapshot_stale = EXCLUDED.source_snapshot_stale,
                    updated_at = EXCLUDED.updated_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    published_at = EXCLUDED.published_at,
                    offline_at = EXCLUDED.offline_at,
                    archived_at = EXCLUDED.archived_at,
                    deleted_at = EXCLUDED.deleted_at,
                    last_synced_at = EXCLUDED.last_synced_at
                """, UUID.randomUUID(), Objects.toString(event.get("eventId"), null), Objects.toString(source.get("sourceType"), "MANUAL"),
                Objects.toString(source.get("sourceId"), null), Objects.toString(source.get("sourceVersion"), null),
                Objects.toString(event.get("title"), null), Objects.toString(event.get("summary"), null), Objects.toString(event.get("description"), ""),
                Objects.toString(event.get("type"), null), Objects.toString(event.get("status"), null), Objects.toString(event.get("visibility"), "PUBLIC"),
                ts(Objects.toString(event.get("startAt"), null)), ts(Objects.toString(event.get("endAt"), null)), Objects.toString(event.get("timezone"), "Asia/Shanghai"),
                boolValue(event.get("allDay")), Objects.toString(event.get("location"), null), Objects.toString(event.get("relatedUrl"), null),
                json(event.get("labels")), intValue(event.get("priority"), 50), intValue(event.get("watchCount"), 0),
                Objects.toString(event.get("createdBy"), Objects.toString(event.get("updatedBy"), "system")),
                Objects.toString(event.get("updatedBy"), Objects.toString(event.get("createdBy"), "system")),
                Objects.toString(event.get("reviewedBy"), null), json(reminder.get("failure")), boolValue(source.get("sourceSnapshotStale")),
                ts(Objects.toString(event.get("createdAt"), null)), ts(Objects.toString(event.get("updatedAt"), null)),
                ts(Objects.toString(event.get("submittedAt"), null)), ts(Objects.toString(event.get("reviewedAt"), null)),
                ts(Objects.toString(event.get("publishedAt"), null)), ts(Objects.toString(event.get("offlineAt"), null)),
                ts(Objects.toString(event.get("archivedAt"), null)), ts(Objects.toString(event.get("deletedAt"), null)),
                ts(Objects.toString(event.get("lastSyncedAt"), null)));
    }

    private void upsertWatch(Map<?, ?> watch) {
        jdbc.update("""
                INSERT INTO calendar_watches(id, watch_id, event_id, user_id, display_name_snapshot, reminder_enabled, reminder_offsets, status, created_at, updated_at, canceled_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (watch_id) DO UPDATE SET
                    display_name_snapshot = EXCLUDED.display_name_snapshot,
                    reminder_enabled = EXCLUDED.reminder_enabled,
                    reminder_offsets = EXCLUDED.reminder_offsets,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    canceled_at = EXCLUDED.canceled_at
                """, UUID.randomUUID(), Objects.toString(watch.get("watchId"), null), Objects.toString(watch.get("eventId"), null),
                Objects.toString(watch.get("userId"), null), Objects.toString(watch.get("displayNameSnapshot"), null),
                boolValue(watch.get("reminderEnabled")), json(watch.get("reminderOffsets")), Objects.toString(watch.get("status"), null),
                ts(Objects.toString(watch.get("createdAt"), null)), ts(Objects.toString(watch.get("updatedAt"), null)),
                ts(Objects.toString(watch.get("canceledAt"), null)));
    }

    private void upsertSyncRun(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
        jdbc.update("""
                INSERT INTO calendar_activity_sync_runs(id, request_id, sync_status, mode, range_from, range_to, created_total, updated_total, skipped_total, failed_total, actor_user_id, activity_mode, items, synced_events, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), now())
                ON CONFLICT (request_id) DO UPDATE SET
                    sync_status = EXCLUDED.sync_status,
                    mode = EXCLUDED.mode,
                    created_total = EXCLUDED.created_total,
                    updated_total = EXCLUDED.updated_total,
                    skipped_total = EXCLUDED.skipped_total,
                    failed_total = EXCLUDED.failed_total,
                    items = EXCLUDED.items,
                    synced_events = EXCLUDED.synced_events
                """, UUID.randomUUID(), requestId(request), text(snapshot, "syncStatus"), Objects.toString(request.getAttribute("calendar.syncMode"), "UPSERT_SNAPSHOT"),
                ts(Objects.toString(request.getAttribute("calendar.syncFrom"), null)), ts(Objects.toString(request.getAttribute("calendar.syncTo"), null)),
                intValue(snapshot.get("createdTotal"), 0), intValue(snapshot.get("updatedTotal"), 0), intValue(snapshot.get("skippedTotal"), 0),
                intValue(snapshot.get("failedTotal"), 0), actorUserId, text(snapshot, "activityMode"), json(snapshot.get("items")), json(snapshot.get("syncedEvents")));

        Object events = snapshot.get("syncedEvents");
        if (events instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> event) {
                    upsertEvent(event);
                }
            }
        }
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
            throw new IllegalStateException("failed to serialize calendar json", exception);
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

    private boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
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
