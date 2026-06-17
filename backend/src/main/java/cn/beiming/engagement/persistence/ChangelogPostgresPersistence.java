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

public class ChangelogPostgresPersistence implements ChangelogPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ChangelogPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
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
            throw new IllegalStateException("failed to parse changelog idempotency response", exception);
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
        counts.put("releasesTotal", count("changelog_releases"));
        counts.put("publishedReleasesTotal", countWhere("changelog_releases", "status = 'PUBLISHED' AND visibility = 'PUBLIC'"));
        counts.put("bookmarksTotal", count("changelog_bookmarks"));
        counts.put("auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'CHANGELOG%'"));
        counts.put("idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'changelog.%'"));
        counts.put("requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/changelog%'"));
        counts.put("lastPublishedAt", jdbc.queryForObject("SELECT MAX(published_at) FROM changelog_releases", OffsetDateTime.class));
        return counts;
    }

    private void persistSnapshot(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
        String type = text(snapshot, "snapshotType");
        if ("RELEASE".equals(type)) {
            upsertRelease(snapshot);
        } else if ("BOOKMARK".equals(type)) {
            upsertBookmark(map(snapshot.get("bookmark")));
            Object release = snapshot.get("release");
            if (release instanceof Map<?, ?> releaseMap) {
                updateReleaseBookmarkCount(releaseMap);
            }
        } else if ("CALENDAR_SYNC".equals(type)) {
            upsertCalendarSync(request, actorUserId, snapshot);
        }
    }

    private void upsertRelease(Map<?, ?> release) {
        Map<?, ?> calendar = map(release.get("relatedCalendarEvent"));
        Map<?, ?> notification = map(release.get("notificationSummary"));
        jdbc.update("""
                INSERT INTO changelog_releases(id, release_id, slug, version_name, title, summary, body, type, status, visibility, impact_level, released_at, effective_at, minecraft_version, plugin_versions, resource_pack_versions, map_version, groups, compatibility_notes, known_issues, rollback_notes, security_public_summary, internal_note, related_resources, related_server_instances, related_content, calendar_sync_status, calendar_event_id, calendar_synced_at, notification_failure, bookmark_count, created_by, updated_by, reviewed_by, review_comment, created_at, updated_at, submitted_at, reviewed_at, published_at, offline_at, archived_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (release_id) DO UPDATE SET
                    slug = EXCLUDED.slug,
                    version_name = EXCLUDED.version_name,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    body = EXCLUDED.body,
                    type = EXCLUDED.type,
                    status = EXCLUDED.status,
                    visibility = EXCLUDED.visibility,
                    impact_level = EXCLUDED.impact_level,
                    released_at = EXCLUDED.released_at,
                    effective_at = EXCLUDED.effective_at,
                    minecraft_version = EXCLUDED.minecraft_version,
                    plugin_versions = EXCLUDED.plugin_versions,
                    resource_pack_versions = EXCLUDED.resource_pack_versions,
                    map_version = EXCLUDED.map_version,
                    groups = EXCLUDED.groups,
                    compatibility_notes = EXCLUDED.compatibility_notes,
                    known_issues = EXCLUDED.known_issues,
                    rollback_notes = EXCLUDED.rollback_notes,
                    security_public_summary = EXCLUDED.security_public_summary,
                    internal_note = EXCLUDED.internal_note,
                    related_resources = EXCLUDED.related_resources,
                    related_server_instances = EXCLUDED.related_server_instances,
                    related_content = EXCLUDED.related_content,
                    calendar_sync_status = EXCLUDED.calendar_sync_status,
                    calendar_event_id = EXCLUDED.calendar_event_id,
                    calendar_synced_at = EXCLUDED.calendar_synced_at,
                    notification_failure = EXCLUDED.notification_failure,
                    bookmark_count = EXCLUDED.bookmark_count,
                    updated_by = EXCLUDED.updated_by,
                    reviewed_by = EXCLUDED.reviewed_by,
                    review_comment = EXCLUDED.review_comment,
                    updated_at = EXCLUDED.updated_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    published_at = EXCLUDED.published_at,
                    offline_at = EXCLUDED.offline_at,
                    archived_at = EXCLUDED.archived_at,
                    deleted_at = EXCLUDED.deleted_at
                """, UUID.randomUUID(), Objects.toString(release.get("releaseId"), null), Objects.toString(release.get("slug"), null),
                Objects.toString(release.get("versionName"), null), Objects.toString(release.get("title"), null),
                Objects.toString(release.get("summary"), null), Objects.toString(release.get("body"), ""),
                Objects.toString(release.get("type"), null), Objects.toString(release.get("status"), null),
                Objects.toString(release.get("visibility"), "PUBLIC"), Objects.toString(release.get("impactLevel"), "MEDIUM"),
                ts(Objects.toString(release.get("releasedAt"), null)), ts(Objects.toString(release.get("effectiveAt"), null)),
                Objects.toString(release.get("minecraftVersion"), null), json(release.get("pluginVersions")),
                json(release.get("resourcePackVersions")), Objects.toString(release.get("mapVersion"), null),
                json(release.get("groups")), Objects.toString(release.get("compatibilityNotes"), null),
                Objects.toString(release.get("knownIssues"), null), Objects.toString(release.get("rollbackNotes"), null),
                Objects.toString(release.get("securityPublicSummary"), null), Objects.toString(release.get("internalNote"), null),
                json(release.get("relatedResources")), json(release.get("relatedServerInstances")), json(release.get("relatedContent")),
                Objects.toString(calendar.get("syncStatus"), "SKIPPED"), Objects.toString(calendar.get("eventId"), null),
                ts(Objects.toString(calendar.get("lastSyncedAt"), null)), json(notification.get("failure")),
                intValue(release.get("bookmarkCount"), 0), Objects.toString(release.get("createdBy"), Objects.toString(release.get("updatedBy"), "system")),
                Objects.toString(release.get("updatedBy"), Objects.toString(release.get("createdBy"), "system")),
                Objects.toString(release.get("reviewedBy"), null), Objects.toString(release.get("reviewComment"), null),
                ts(Objects.toString(release.get("createdAt"), null)), ts(Objects.toString(release.get("updatedAt"), null)),
                ts(Objects.toString(release.get("submittedAt"), null)), ts(Objects.toString(release.get("reviewedAt"), null)),
                ts(Objects.toString(release.get("publishedAt"), null)), ts(Objects.toString(release.get("offlineAt"), null)),
                ts(Objects.toString(release.get("archivedAt"), null)), ts(Objects.toString(release.get("deletedAt"), null)));
    }

    private void updateReleaseBookmarkCount(Map<?, ?> release) {
        jdbc.update("""
                UPDATE changelog_releases
                SET bookmark_count = ?, updated_at = COALESCE(updated_at, now())
                WHERE release_id = ?
                """, intValue(release.get("bookmarkCount"), 0), Objects.toString(release.get("releaseId"), null));
    }

    private void upsertBookmark(Map<?, ?> bookmark) {
        jdbc.update("""
                INSERT INTO changelog_bookmarks(id, bookmark_id, release_id, user_id, display_name_snapshot, status, created_at, updated_at, canceled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (bookmark_id) DO UPDATE SET
                    display_name_snapshot = EXCLUDED.display_name_snapshot,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    canceled_at = EXCLUDED.canceled_at
                """, UUID.randomUUID(), Objects.toString(bookmark.get("bookmarkId"), null), Objects.toString(bookmark.get("releaseId"), null),
                Objects.toString(bookmark.get("userId"), null), Objects.toString(bookmark.get("displayNameSnapshot"), null),
                Objects.toString(bookmark.get("status"), null), ts(Objects.toString(bookmark.get("createdAt"), null)),
                ts(Objects.toString(bookmark.get("updatedAt"), null)), ts(Objects.toString(bookmark.get("canceledAt"), null)));
    }

    private void upsertCalendarSync(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
        Map<?, ?> event = map(snapshot.get("calendarEvent"));
        jdbc.update("""
                INSERT INTO changelog_calendar_syncs(id, request_id, release_id, sync_status, mode, calendar_event_id, items, calendar_event_snapshot, actor_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, now())
                ON CONFLICT (request_id) DO UPDATE SET
                    sync_status = EXCLUDED.sync_status,
                    mode = EXCLUDED.mode,
                    calendar_event_id = EXCLUDED.calendar_event_id,
                    items = EXCLUDED.items,
                    calendar_event_snapshot = EXCLUDED.calendar_event_snapshot
                """, UUID.randomUUID(), requestId(request), text(snapshot, "releaseId"), text(snapshot, "syncStatus"),
                Objects.toString(request.getAttribute("changelog.syncMode"), "DRY_RUN"), Objects.toString(event.get("eventId"), null),
                json(snapshot.get("items")), json(event), actorUserId);
        jdbc.update("""
                UPDATE changelog_releases
                SET calendar_sync_status = ?, calendar_event_id = ?, calendar_synced_at = ?, updated_at = now()
                WHERE release_id = ?
                """, text(snapshot, "syncStatus"), Objects.toString(event.get("eventId"), null),
                ts(Objects.toString(event.get("lastSyncedAt"), null)), text(snapshot, "releaseId"));
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
            throw new IllegalStateException("failed to serialize changelog json", exception);
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
