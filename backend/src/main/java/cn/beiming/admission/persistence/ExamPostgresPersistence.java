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

public class ExamPostgresPersistence implements ExamPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ExamPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void seed(List<Map<String, Object>> questions, List<Map<String, Object>> templates, List<Map<String, Object>> audits) {
        jdbc.update("DELETE FROM exam_handoff_snapshots");
        jdbc.update("DELETE FROM exam_reviews");
        jdbc.update("DELETE FROM exam_answer_sheets");
        jdbc.update("DELETE FROM exam_sessions");
        jdbc.update("DELETE FROM exam_question_versions");
        jdbc.update("DELETE FROM exam_questions");
        jdbc.update("DELETE FROM exam_paper_templates");
        jdbc.update("DELETE FROM app_idempotency_records WHERE scope LIKE 'exam.%'");
        jdbc.update("DELETE FROM app_audit_logs WHERE target_type LIKE 'EXAM%'");
        jdbc.update("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/exams%'");
        for (Map<String, Object> question : questions) {
            upsertQuestion(question);
            upsertQuestionVersion(question);
        }
        for (Map<String, Object> template : templates) {
            upsertTemplate(template);
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
            throw new IllegalStateException("failed to parse exam idempotency response", exception);
        }
    }

    @Override
    @Transactional
    public void persistSessionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode) {
        upsertSession(session);
        insertAudit(request, actorUserId, actorRole, "EXAM_SESSION", text(session, "sessionId"), action, riskLevel, beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistAnswerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String idempotencyKey, String fingerprint, Map<String, Object> answerSheet, Map<String, Object> responseBody, int responseCode) {
        insertAnswerSheet(request, actorUserId, answerSheet);
        insertAudit(request, actorUserId, actorRole, "EXAM_SESSION", text(answerSheet, "sessionId"), action, "LOW", text(answerSheet, "status"), text(answerSheet, "status"), null);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistReviewWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode) {
        upsertSession(session);
        insertReview(request, actorUserId, session, responseBody);
        insertAudit(request, actorUserId, actorRole, "EXAM_SESSION", text(session, "sessionId"), action, "MEDIUM", beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistQuestionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> question, Map<String, Object> responseBody, int responseCode) {
        upsertQuestion(question);
        upsertQuestionVersion(question);
        insertAudit(request, actorUserId, actorRole, "EXAM_QUESTION", text(question, "questionId"), action, "MEDIUM", beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistTemplateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> template, Map<String, Object> responseBody, int responseCode) {
        upsertTemplate(template);
        insertAudit(request, actorUserId, actorRole, "EXAM_TEMPLATE", text(template, "templateId"), action, "MEDIUM", beforeStatus, afterStatus, reason);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, responseBody);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    @Transactional
    public void persistHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
        jdbc.update("""
                INSERT INTO exam_handoff_snapshots(id, handoff_id, session_id, target_module, handoff_version, snapshot_payload, generated_by, request_id, generated_at)
                VALUES (?, ?, ?, 'WHITELIST', ?, CAST(? AS jsonb), ?, ?, now())
                """, UUID.randomUUID(), "exam-handoff-" + UUID.randomUUID(), text(snapshot, "sessionId"), intValue(snapshot.get("handoffVersion"), 1), json(snapshot), actorUserId, requestId(request));
        insertRequestLog(request, actorUserId, 200);
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of(
                "storageMode", "POSTGRESQL_PRIMARY",
                "sessionsTotal", count("exam_sessions"),
                "questionsTotal", count("exam_questions"),
                "publishedTemplatesTotal", countWhere("exam_paper_templates", "status = 'PUBLISHED'"),
                "whitelistHandoffSnapshotsTotal", count("exam_handoff_snapshots"),
                "auditsTotal", countWhere("app_audit_logs", "target_type LIKE 'EXAM%'"),
                "idempotencyRecordsTotal", countWhere("app_idempotency_records", "scope LIKE 'exam.%'"),
                "requestLogsTotal", countWhere("app_request_logs", "path LIKE '/api/v1/exams%'")
        );
    }

    private void upsertSession(Map<String, Object> session) {
        jdbc.update("""
                INSERT INTO exam_sessions(id, session_id, application_id, onboarding_handoff_version, user_id, display_name_snapshot, minecraft_binding_snapshot, review_direction, attempt_type, difficulty, status, result, template_id, template_version, paper_id, paper_snapshot, answer_snapshot, score_summary, manual_review, supplement_request, notification_status, started_at, last_saved_at, submitted_at, reviewed_at, expires_at, passed_at, cancelled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    result = EXCLUDED.result,
                    answer_snapshot = EXCLUDED.answer_snapshot,
                    score_summary = EXCLUDED.score_summary,
                    manual_review = EXCLUDED.manual_review,
                    supplement_request = EXCLUDED.supplement_request,
                    notification_status = EXCLUDED.notification_status,
                    last_saved_at = EXCLUDED.last_saved_at,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    passed_at = EXCLUDED.passed_at,
                    cancelled_at = EXCLUDED.cancelled_at,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), text(session, "sessionId"), text(session, "applicationId"), intValue(session.get("handoffVersion"), 1),
                text(session, "userId"), text(session, "displayNameSnapshot"), json(session.get("minecraftBindingSnapshot")), text(session, "reviewDirection"),
                text(session, "attemptType"), text(session, "difficulty"), text(session, "status"), text(session, "result"), text(session, "templateId"),
                intValue(session.get("templateVersion"), 1), text(session, "paperId"), json(paperSnapshot(session)), json(List.of()), json(session.get("scoreSummary")),
                json(session.get("manualReview")), json(session.get("supplementRequest")), text(session, "notificationStatus"), ts(text(session, "startedAt")),
                ts(text(session, "lastSavedAt")), ts(text(session, "submittedAt")), ts(text(session, "reviewedAt")), ts(text(session, "expiresAt")),
                ts(text(session, "passedAt")), ts(text(session, "cancelledAt")), ts(text(session, "createdAt")), ts(text(session, "updatedAt")));
    }

    private Map<String, Object> paperSnapshot(Map<String, Object> session) {
        return Map.of(
                "paperId", text(session, "paperId"),
                "templateId", text(session, "templateId"),
                "templateVersion", intValue(session.get("templateVersion"), 1),
                "questions", session.getOrDefault("questions", List.of())
        );
    }

    private void insertAnswerSheet(HttpServletRequest request, String actorUserId, Map<String, Object> answerSheet) {
        jdbc.update("""
                INSERT INTO exam_answer_sheets(id, answer_sheet_id, session_id, actor_user_id, draft, answers_payload, request_id, saved_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
                """, UUID.randomUUID(), "answer-" + UUID.randomUUID(), text(answerSheet, "sessionId"), actorUserId, Boolean.TRUE.equals(answerSheet.get("draft")), json(answerSheet.get("answers")), requestId(request));
    }

    private void insertReview(HttpServletRequest request, String actorUserId, Map<String, Object> session, Map<String, Object> responseBody) {
        Map<String, Object> review = mapValue(session.get("manualReview"));
        if (review.isEmpty()) {
            return;
        }
        jdbc.update("""
                INSERT INTO exam_reviews(id, review_id, session_id, reviewer_user_id, result, review_payload, request_id, reviewed_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (review_id) DO NOTHING
                """, UUID.randomUUID(), textOrDefault(review, "reviewId", "review-" + UUID.randomUUID()), text(session, "sessionId"), actorUserId, text(review, "result"), json(responseBody), requestId(request), ts(text(review, "reviewedAt")));
    }

    private void upsertQuestion(Map<String, Object> question) {
        jdbc.update("""
                INSERT INTO exam_questions(id, question_id, version, type, review_direction, difficulty, stem, options, correct_option_ids, reference_answer, score, tags, status, created_at, updated_at, archived_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (question_id) DO UPDATE SET
                    version = EXCLUDED.version,
                    stem = EXCLUDED.stem,
                    options = EXCLUDED.options,
                    correct_option_ids = EXCLUDED.correct_option_ids,
                    reference_answer = EXCLUDED.reference_answer,
                    score = EXCLUDED.score,
                    tags = EXCLUDED.tags,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at
                """, UUID.randomUUID(), text(question, "questionId"), intValue(question.get("version"), 1), text(question, "type"), text(question, "reviewDirection"),
                text(question, "difficulty"), text(question, "stem"), json(question.get("options")), json(question.get("correctOptionIds")), text(question, "referenceAnswer"),
                intValue(question.get("score"), 0), json(question.get("tags")), text(question, "status"), ts(text(question, "createdAt")), ts(text(question, "updatedAt")), ts(text(question, "archivedAt")));
    }

    private void upsertQuestionVersion(Map<String, Object> question) {
        jdbc.update("""
                INSERT INTO exam_question_versions(id, question_id, version, question_snapshot, created_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                ON CONFLICT (question_id, version) DO UPDATE SET question_snapshot = EXCLUDED.question_snapshot
                """, UUID.randomUUID(), text(question, "questionId"), intValue(question.get("version"), 1), json(question), ts(text(question, "updatedAt")));
    }

    private void upsertTemplate(Map<String, Object> template) {
        jdbc.update("""
                INSERT INTO exam_paper_templates(id, template_id, version, name, review_direction, difficulty, status, time_limit_minutes, pass_score, objective_pass_score, question_rules, content_rule_version, retake_cooldown_hours, created_at, updated_at, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                ON CONFLICT (template_id) DO UPDATE SET
                    version = EXCLUDED.version,
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    time_limit_minutes = EXCLUDED.time_limit_minutes,
                    pass_score = EXCLUDED.pass_score,
                    objective_pass_score = EXCLUDED.objective_pass_score,
                    question_rules = EXCLUDED.question_rules,
                    content_rule_version = EXCLUDED.content_rule_version,
                    retake_cooldown_hours = EXCLUDED.retake_cooldown_hours,
                    updated_at = EXCLUDED.updated_at,
                    published_at = EXCLUDED.published_at
                """, UUID.randomUUID(), text(template, "templateId"), intValue(template.get("version"), 1), text(template, "name"), text(template, "reviewDirection"),
                text(template, "difficulty"), text(template, "status"), intValue(template.get("timeLimitMinutes"), 0), intValue(template.get("passScore"), 0),
                intValue(template.get("objectivePassScore"), 0), json(template.get("questionRules")), text(template, "contentRuleVersion"), intValue(template.get("retakeCooldownHours"), 0),
                ts(text(template, "createdAt")), ts(text(template, "updatedAt")), ts(text(template, "publishedAt")));
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), 'SUCCESS', NULL, now())
                """, UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "USER" : actorRole, json(List.of()), sourceIp(request), targetType, targetId, action, riskLevel, reason,
                json(Map.of("action", action)), json(stateRow(beforeStatus)), json(stateRow(afterStatus)));
    }

    private void insertSeedAudit(Map<String, Object> audit) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), NULL, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)
                """, UUID.randomUUID(), text(audit, "requestId"), text(audit, "actorUserId"), textOrDefault(audit, "actorRole", "ADMIN"), json(List.of()),
                text(audit, "targetType"), text(audit, "targetId"), text(audit, "action"), text(audit, "riskLevel"), text(audit, "reason"),
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
            throw new IllegalStateException("failed to serialize exam json", exception);
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

    private String textOrDefault(Map<String, Object> source, String key, String fallback) {
        String value = text(source, key);
        return value == null ? fallback : value;
    }
}
