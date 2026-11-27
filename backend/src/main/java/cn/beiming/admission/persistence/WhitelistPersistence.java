package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface WhitelistPersistence {
    Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint);

    void persistAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason);

    void persistApplicationWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> responseBody, int responseCode);

    Map<String, Object> counts();

    /**
     * 启动时从正式库读回全部申请单。
     *
     * <p>此前只有 {@link #persistApplicationWrite} 往库里写、没有任何读回路径：
     * 进程重启后内存里只剩空表，所有人的白名单申请、审核状态、发过的卡全部消失，
     * 而库里明明有行——运维看到的是「审批通过了，重启后待审列表空了」。
     *
     * <p>返回空表示没有持久化后端（内存模式），沿用原有空初始状态。
     */
    List<Map<String, Object>> loadApplications();
}
