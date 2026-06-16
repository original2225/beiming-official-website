package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface ExamPersistence {
    void seed(List<Map<String, Object>> questions, List<Map<String, Object>> templates, List<Map<String, Object>> audits);

    Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint);

    void persistSessionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode);

    void persistAnswerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String idempotencyKey, String fingerprint, Map<String, Object> answerSheet, Map<String, Object> responseBody, int responseCode);

    void persistReviewWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode);

    void persistQuestionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> question, Map<String, Object> responseBody, int responseCode);

    void persistTemplateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> template, Map<String, Object> responseBody, int responseCode);

    void persistHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot);

    Map<String, Object> counts();
}
