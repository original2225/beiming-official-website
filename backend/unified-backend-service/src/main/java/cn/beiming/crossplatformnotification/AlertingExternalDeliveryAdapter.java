package cn.beiming.crossplatformnotification;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class AlertingExternalDeliveryAdapter {
    private final CpnStore store;
    private final CpnProperties properties;

    AlertingExternalDeliveryAdapter(CpnStore store, CpnProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    public Result createSimulatedDelivery(HttpServletRequest request, String actorUserId, String actorDisplayName, Map<String, Object> body) {
        try {
            Actor actor = new Actor(actorUserId, actorDisplayName, Set.of("ADMIN"), Set.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            synchronized (store.lock) {
                CpnSupport.rejectTrusted(body);
                CpnSupport.validateDeliveryBody(body);
                if (!"alerting".equals(CpnSupport.text(body.get("sourceModule"))) || !"alert.firing".equals(CpnSupport.text(body.get("eventType")))) {
                    throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid alerting delivery request");
                }
                String key = CpnSupport.text(body.get("idempotencyKey"));
                String scopedKey = actor.userId() + "|alerting:delivery|" + key;
                String fingerprint = CpnSupport.fingerprint(body);
                if (!key.isBlank()) {
                    IdempotencyRecord existing = store.idempotency.get(scopedKey);
                    if (existing != null) {
                        if (!existing.fingerprint().equals(fingerprint)) {
                            throw new CpnApiException(HttpStatus.CONFLICT, 49962, "idempotency fingerprint conflict");
                        }
                        return fromData(existing.data());
                    }
                }
                store.failDeliveryIfRequested(request, properties.enabled());
                store.failAuditIfRequested(request, properties.enabled());
                DeliveryBundle bundle = store.createDeliveryFromBody(actor, request, body, properties.enabled());
                store.audit("EXTERNAL_DELIVERY_CREATED", "DELIVERY", bundle.delivery().deliveryId, actor, request, body, "HIGH", "SUCCESS", null, null, bundle.delivery().status);
                Map<String, Object> data = CpnSupport.map("delivery", bundle.delivery().view(), "attempt", bundle.attempt().view());
                if (!key.isBlank()) {
                    store.idempotency.put(scopedKey, new IdempotencyRecord(fingerprint, HttpStatus.CREATED, CpnSupport.deepCopy(data)));
                }
                return fromData(data);
            }
        } catch (CpnApiException ex) {
            return new Result(false, ex.status.value(), ex.code, ex.getMessage(), null, null);
        } catch (RuntimeException ex) {
            return new Result(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), 55800, "cross platform notification adapter failed", null, null);
        }
    }

    @SuppressWarnings("unchecked")
    private Result fromData(Map<String, Object> data) {
        return new Result(true, HttpStatus.CREATED.value(), 0, "success",
                (Map<String, Object>) data.get("delivery"),
                (Map<String, Object>) data.get("attempt"));
    }

    public record Result(boolean success,
                         int statusCode,
                         int code,
                         String message,
                         Map<String, Object> delivery,
                         Map<String, Object> attempt) {
    }
}
