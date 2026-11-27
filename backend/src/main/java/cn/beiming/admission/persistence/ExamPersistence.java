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

    /**
     * 启动时从正式库读回题目。
     *
     * <p>此前只有 {@link #seed} 往库里写、没有任何读回路径：进程重启后内存里只剩硬编码的
     * 种子题，管理员通过接口新建的题全部消失——接口 200、库里有行，但考生永远考不到。
     * 这是「假持久化」的典型：写是真的，读是缺的。
     *
     * <p>返回空表示没有持久化后端（内存模式），此时沿用 seed 的硬编码数据。
     */
    List<Map<String, Object>> loadQuestions();

    /**
     * 启动时从正式库读回试卷模板。理由同 {@link #loadQuestions()}——
     * 模板丢了会让 {@code latestTemplate} 撞 43914 no published template。
     */
    List<Map<String, Object>> loadTemplates();

    Map<String, Object> counts();
}
