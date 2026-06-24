package cn.beiming.admission.persistence;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public class NoopOnboardingPersistence implements OnboardingPersistence {
    @Override
    public void seed(List<Map<String, Object>> applications, List<Map<String, Object>> audits) {
    }

    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistApplicationWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String action, String riskLevel, String beforeStatus, String afterStatus, String reason, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistConfirmationWrite(HttpServletRequest request, String actorUserId, String actorRole, String confirmationType, String action, String idempotencyKey, String fingerprint, Map<String, Object> application, Map<String, Object> confirmation, Map<String, Object> responseBody, int responseCode) {
    }

    @Override
    public void persistHandoff(HttpServletRequest request, String actorUserId, Map<String, Object> application, String targetModule, int handoffVersion, Map<String, Object> snapshot) {
    }

    @Override
    public Map<String, Object> counts() {
        return Map.of("storageMode", "IN_MEMORY");
    }
}
