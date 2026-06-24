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

public class CommunityPostgresPersistence implements CommunityPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public CommunityPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
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
            throw new IllegalStateException("failed to parse community idempotency response", exception);
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
        counts.put("boardsTotal", count("community_boards"));
        counts.put("postsTotal", count("community_posts"));
        counts.put("pendingReviewPostsTotal", countWhere("community_posts", "status = 'PENDING_REVIEW'"));
        counts.put("commentsTotal", count("community_comments"));
        counts.put("openReportsTotal", countWhere("community_reports", "status = 'OPEN'"));
        counts.put("openTicketsTotal", countWhere("community_tickets", "status IN ('OPEN', 'WAITING_STAFF', 'WAITING_USER')"));
        counts.put("activePenaltiesTotal", countWhere("community_penalties", "status = 'ACTIVE'"));
        counts.put("pollsOpenTotal", countWhere("community_polls", "status = 'OPEN'"));
        counts.put("auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'COMMUNITY%'"));
        counts.put("idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'community.%'"));
        counts.put("requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/community%'"));
        return counts;
    }

    private void persistSnapshot(Map<String, Object> snapshot) {
        String type = text(snapshot, "snapshotType");
        if ("BOARD".equals(type)) {
            upsertBoard(snapshot);
        } else if ("POST".equals(type)) {
            upsertPost(snapshot);
        } else if ("COMMENT".equals(type)) {
            upsertComment(snapshot);
        } else if ("REACTION".equals(type)) {
            upsertReaction(snapshot);
        } else if ("FAVORITE".equals(type)) {
            upsertFavorite(snapshot);
        } else if ("POLL".equals(type)) {
            upsertPoll(snapshot);
        } else if ("POLL_VOTE".equals(type)) {
            upsertPollVote(snapshot);
        } else if ("REPORT".equals(type)) {
            upsertReport(snapshot);
        } else if ("TICKET".equals(type)) {
            upsertTicket(snapshot);
        } else if ("PENALTY".equals(type)) {
            upsertPenalty(snapshot);
        }
    }

    private void upsertBoard(Map<String, Object> board) {
        jdbc.update("""
                INSERT INTO community_boards(id, board_id, slug, name, description, visibility, status, allowed_post_types, tags, sort_order, last_post_at, created_at, updated_at, archived_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?)
                ON CONFLICT (board_id) DO UPDATE SET
                    slug = EXCLUDED.slug,
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    visibility = EXCLUDED.visibility,
                    status = EXCLUDED.status,
                    allowed_post_types = EXCLUDED.allowed_post_types,
                    tags = EXCLUDED.tags,
                    sort_order = EXCLUDED.sort_order,
                    last_post_at = EXCLUDED.last_post_at,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at
                """, UUID.randomUUID(), text(board, "boardId"), text(board, "slug"), text(board, "name"), text(board, "description"),
                text(board, "visibility"), text(board, "status"), json(board.get("allowedPostTypes")), json(board.get("tags")),
                intValue(board.get("sortOrder"), 0), ts(text(board, "lastPostAt")), ts(text(board, "createdAt")),
                ts(text(board, "updatedAt")), ts(text(board, "archivedAt")));
    }

    private void upsertPost(Map<String, Object> post) {
        Map<?, ?> author = map(post.get("author"));
        jdbc.update("""
                INSERT INTO community_posts(id, post_id, board_id, type, title, summary, body, tags, status, author_user_id, author_snapshot, linked_content_snapshot, linked_resource_snapshot, poll_id, like_count, favorite_count, view_count, accepted_comment_id, last_comment_at, submitted_at, reviewed_at, reviewer_user_id, review_comment, notification_status, notification_failure, created_at, updated_at, offline_at, archived_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                ON CONFLICT (post_id) DO UPDATE SET
                    board_id = EXCLUDED.board_id,
                    type = EXCLUDED.type,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    body = EXCLUDED.body,
                    tags = EXCLUDED.tags,
                    status = EXCLUDED.status,
                    author_snapshot = EXCLUDED.author_snapshot,
                    linked_content_snapshot = EXCLUDED.linked_content_snapshot,
                    linked_resource_snapshot = EXCLUDED.linked_resource_snapshot,
                    poll_id = EXCLUDED.poll_id,
                    like_count = EXCLUDED.like_count,
                    favorite_count = EXCLUDED.favorite_count,
                    view_count = EXCLUDED.view_count,
                    accepted_comment_id = EXCLUDED.accepted_comment_id,
                    last_comment_at = EXCLUDED.last_comment_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    review_comment = EXCLUDED.review_comment,
                    notification_status = EXCLUDED.notification_status,
                    notification_failure = EXCLUDED.notification_failure,
                    updated_at = EXCLUDED.updated_at,
                    offline_at = EXCLUDED.offline_at,
                    archived_at = EXCLUDED.archived_at,
                    deleted_at = EXCLUDED.deleted_at
                """, UUID.randomUUID(), text(post, "postId"), text(post, "boardId"), text(post, "type"), text(post, "title"),
                text(post, "summary"), text(post, "body"), json(post.get("tags")), text(post, "status"),
                Objects.toString(author.get("userId"), null), json(author), json(post.get("linkedContentSnapshot")),
                json(post.get("linkedResourceSnapshot")), text(post, "pollId"), intValue(post.get("likeCount"), 0),
                intValue(post.get("favoriteCount"), 0), intValue(post.get("viewCount"), 0), text(post, "acceptedCommentId"),
                ts(text(post, "lastCommentAt")), ts(text(post, "submittedAt")), ts(text(post, "reviewedAt")),
                text(post, "reviewerUserId"), text(post, "reviewComment"), text(post, "notificationStatus"),
                json(post.get("notificationFailure")), ts(text(post, "createdAt")), ts(text(post, "updatedAt")),
                ts(text(post, "offlineAt")), ts(text(post, "archivedAt")), ts(text(post, "deletedAt")));
    }

    private void upsertComment(Map<String, Object> comment) {
        Map<?, ?> author = map(comment.get("author"));
        jdbc.update("""
                INSERT INTO community_comments(id, comment_id, post_id, parent_comment_id, body, status, author_user_id, author_snapshot, like_count, is_accepted_answer, submitted_at, reviewed_at, review_comment, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (comment_id) DO UPDATE SET
                    parent_comment_id = EXCLUDED.parent_comment_id,
                    body = EXCLUDED.body,
                    status = EXCLUDED.status,
                    author_snapshot = EXCLUDED.author_snapshot,
                    like_count = EXCLUDED.like_count,
                    is_accepted_answer = EXCLUDED.is_accepted_answer,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    review_comment = EXCLUDED.review_comment,
                    updated_at = EXCLUDED.updated_at,
                    deleted_at = EXCLUDED.deleted_at
                """, UUID.randomUUID(), text(comment, "commentId"), text(comment, "postId"), text(comment, "parentCommentId"),
                text(comment, "body"), text(comment, "status"), Objects.toString(author.get("userId"), null), json(author),
                intValue(comment.get("likeCount"), 0), Boolean.TRUE.equals(comment.get("isAcceptedAnswer")),
                ts(text(comment, "submittedAt")), ts(text(comment, "reviewedAt")), text(comment, "reviewComment"),
                ts(text(comment, "createdAt")), ts(text(comment, "updatedAt")), ts(text(comment, "deletedAt")));
    }

    private void upsertReaction(Map<String, Object> reaction) {
        jdbc.update("""
                INSERT INTO community_reactions(id, reaction_id, target_type, target_id, actor_user_id, reaction_type, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'LIKE', ?, now(), now())
                ON CONFLICT (target_type, target_id, actor_user_id, reaction_type) DO UPDATE SET
                    active = EXCLUDED.active,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), "reaction-" + text(reaction, "targetType") + "-" + text(reaction, "targetId") + "-" + text(reaction, "actorUserId"),
                text(reaction, "targetType"), text(reaction, "targetId"), text(reaction, "actorUserId"), Boolean.TRUE.equals(reaction.get("active")));
    }

    private void upsertFavorite(Map<String, Object> favorite) {
        jdbc.update("""
                INSERT INTO community_favorites(id, favorite_id, post_id, actor_user_id, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (post_id, actor_user_id) DO UPDATE SET
                    active = EXCLUDED.active,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), "favorite-" + text(favorite, "postId") + "-" + text(favorite, "actorUserId"),
                text(favorite, "postId"), text(favorite, "actorUserId"), Boolean.TRUE.equals(favorite.get("active")));
    }

    private void upsertPoll(Map<String, Object> poll) {
        jdbc.update("""
                INSERT INTO community_polls(id, poll_id, post_id, title, description, status, options_payload, multiple_choice, min_choices, max_choices, eligible_visibility, anonymous_result, vote_count, opens_at, closes_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (poll_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    status = EXCLUDED.status,
                    options_payload = EXCLUDED.options_payload,
                    multiple_choice = EXCLUDED.multiple_choice,
                    min_choices = EXCLUDED.min_choices,
                    max_choices = EXCLUDED.max_choices,
                    eligible_visibility = EXCLUDED.eligible_visibility,
                    anonymous_result = EXCLUDED.anonymous_result,
                    vote_count = EXCLUDED.vote_count,
                    opens_at = EXCLUDED.opens_at,
                    closes_at = EXCLUDED.closes_at,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(poll, "pollId"), text(poll, "postId"), text(poll, "title"), text(poll, "description"),
                text(poll, "status"), json(poll.get("options")), Boolean.TRUE.equals(poll.get("multipleChoice")),
                intValue(poll.get("minChoices"), 1), intValue(poll.get("maxChoices"), 1), text(poll, "eligibleVisibility"),
                Boolean.TRUE.equals(poll.get("anonymousResult")), intValue(poll.get("voteCount"), 0),
                ts(text(poll, "opensAt")), ts(text(poll, "closesAt")), ts(text(poll, "createdAt")), ts(text(poll, "updatedAt")));
    }

    private void upsertPollVote(Map<String, Object> vote) {
        jdbc.update("""
                INSERT INTO community_poll_votes(id, vote_id, poll_id, actor_user_id, option_ids, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), now())
                ON CONFLICT (poll_id, actor_user_id) DO NOTHING
                """, UUID.randomUUID(), "vote-" + text(vote, "pollId") + "-" + text(vote, "actorUserId"),
                text(vote, "pollId"), text(vote, "actorUserId"), json(vote.get("optionIds")));
    }

    private void upsertReport(Map<String, Object> report) {
        Map<?, ?> reporter = map(report.get("reporter"));
        jdbc.update("""
                INSERT INTO community_reports(id, report_id, target_type, target_id, reason_type, description, evidence_links, status, reporter_user_id, reporter_snapshot, assignee_user_id, resolution, linked_penalty_id, notification_status, created_at, updated_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (report_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    assignee_user_id = EXCLUDED.assignee_user_id,
                    resolution = EXCLUDED.resolution,
                    linked_penalty_id = EXCLUDED.linked_penalty_id,
                    notification_status = EXCLUDED.notification_status,
                    updated_at = EXCLUDED.updated_at,
                    resolved_at = EXCLUDED.resolved_at
                """, UUID.randomUUID(), text(report, "reportId"), text(report, "targetType"), text(report, "targetId"),
                text(report, "reasonType"), text(report, "description"), json(report.get("evidenceLinks")), text(report, "status"),
                Objects.toString(reporter.get("userId"), null), json(reporter), text(report, "assigneeUserId"),
                text(report, "resolution"), text(report, "linkedPenaltyId"), text(report, "notificationStatus"),
                ts(text(report, "createdAt")), ts(text(report, "updatedAt")), ts(text(report, "resolvedAt")));
    }

    private void upsertTicket(Map<String, Object> ticket) {
        Map<?, ?> creator = map(ticket.get("creator"));
        jdbc.update("""
                INSERT INTO community_tickets(id, ticket_id, type, title, status, priority, creator_user_id, creator_snapshot, assignee_user_id, related_object, last_reply_at, resolved_at, closed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                ON CONFLICT (ticket_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    priority = EXCLUDED.priority,
                    assignee_user_id = EXCLUDED.assignee_user_id,
                    related_object = EXCLUDED.related_object,
                    last_reply_at = EXCLUDED.last_reply_at,
                    resolved_at = EXCLUDED.resolved_at,
                    closed_at = EXCLUDED.closed_at,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(ticket, "ticketId"), text(ticket, "type"), text(ticket, "title"),
                text(ticket, "status"), text(ticket, "priority"), Objects.toString(creator.get("userId"), null),
                json(creator), text(ticket, "assigneeUserId"), json(ticket.get("relatedObject")),
                ts(text(ticket, "lastReplyAt")), ts(text(ticket, "resolvedAt")), ts(text(ticket, "closedAt")),
                ts(text(ticket, "createdAt")), ts(text(ticket, "updatedAt")));
        Object messages = ticket.get("messages");
        if (messages instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> message) {
                    upsertTicketMessage(message);
                }
            }
        }
    }

    private void upsertTicketMessage(Map<?, ?> message) {
        Object authorValue = message.get("author");
        Map<?, ?> author = authorValue instanceof Map<?, ?> map ? map : Map.of();
        jdbc.update("""
                INSERT INTO community_ticket_messages(id, message_id, ticket_id, message_type, body, author_user_id, author_snapshot, attachments, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
                ON CONFLICT (message_id) DO NOTHING
                """, UUID.randomUUID(), Objects.toString(message.get("messageId"), null), Objects.toString(message.get("ticketId"), null),
                Objects.toString(message.get("messageType"), null), Objects.toString(message.get("body"), null),
                Objects.toString(author.get("userId"), null), json(author), json(message.get("attachments")),
                ts(Objects.toString(message.get("createdAt"), null)));
    }

    private void upsertPenalty(Map<String, Object> penalty) {
        jdbc.update("""
                INSERT INTO community_penalties(id, penalty_id, target_user_id, target_member_id, type, status, reason, public_reason, evidence_report_id, related_post_id, related_comment_id, starts_at, expires_at, created_by, revoked_by, revoked_at, revoke_reason, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (penalty_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    reason = EXCLUDED.reason,
                    public_reason = EXCLUDED.public_reason,
                    expires_at = EXCLUDED.expires_at,
                    revoked_by = EXCLUDED.revoked_by,
                    revoked_at = EXCLUDED.revoked_at,
                    revoke_reason = EXCLUDED.revoke_reason,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(penalty, "penaltyId"), text(penalty, "targetUserId"), text(penalty, "targetMemberId"),
                text(penalty, "type"), text(penalty, "status"), text(penalty, "reason"), text(penalty, "publicReason"),
                text(penalty, "evidenceReportId"), text(penalty, "relatedPostId"), text(penalty, "relatedCommentId"),
                ts(text(penalty, "startsAt")), ts(text(penalty, "expiresAt")), text(penalty, "createdBy"),
                text(penalty, "revokedBy"), ts(text(penalty, "revokedAt")), text(penalty, "revokeReason"),
                ts(text(penalty, "createdAt")), ts(text(penalty, "updatedAt")));
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
            throw new IllegalStateException("failed to serialize community json", exception);
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

    private Map<?, ?> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }
}
