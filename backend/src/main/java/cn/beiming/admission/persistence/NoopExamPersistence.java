package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public class NoopExamPersistence implements ExamPersistence {
    @Override
    public void seed(List<Map<String, Object>> questions, List<Map<String, Object>> templates, List<Map<String, Object>> audits) {
    }

    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistSessionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistAnswerWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String idempotencyKey, String fingerprint, Map<String, Object> answerSheet, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistReviewWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> session, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistQuestionWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> question, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistTemplateWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> template, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> snapshot) {
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of("storageMode", "IN_MEMORY");
    }
}
