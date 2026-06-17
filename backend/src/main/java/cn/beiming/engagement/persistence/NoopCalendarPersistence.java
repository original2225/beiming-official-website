package cn.beiming.engagement.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class NoopCalendarPersistence implements CalendarPersistence {
    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action,
                             String targetType, String targetId, String riskLevel, String beforeStatus, String afterStatus,
                             String reason, String idempotencyKey, String fingerprint, Map<String, Object> snapshot,
                             Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of("storageMode", "IN_MEMORY");
    }
}
