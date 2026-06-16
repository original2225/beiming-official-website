package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class NoopWhitelistPersistence implements WhitelistPersistence {
    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistAudit(HttpServletRequest request, String actorUserId, String actorRole, String targetType, String targetId, String action, String riskLevel, String beforeStatus, String afterStatus, String reason) {
    }

    @Override
    public void persistApplicationWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of("storageMode", "IN_MEMORY");
    }
}
