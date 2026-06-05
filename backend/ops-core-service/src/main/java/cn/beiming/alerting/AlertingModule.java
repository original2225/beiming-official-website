package cn.beiming.alerting;

import cn.beiming.crossplatformnotification.AlertingExternalDeliveryAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/alerting")
class AlertingController {
    private static final String VERSION = "0.1.0-contract";
    private final AlertingStore store;
    private final AlertingAuth auth;
    private final AlertingProperties properties;

    AlertingController(AlertingStore store, AlertingAuth auth, AlertingProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, Map.of("service", "alerting", "status", "READY", "version", VERSION));
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new AlertingException(HttpStatus.INTERNAL_SERVER_ERROR, 55500, "alerting internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/sources")
    ResponseEntity<Map<String, Object>> sources(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "lastEventAt_desc", "lastSnapshotAt_desc", "displayName_asc");
        Boolean enabled = query.get("enabled") == null ? null : Boolean.parseBoolean(query.get("enabled"));
        List<Map<String, Object>> items = store.sources.values().stream()
                .filter(source -> matches(source.displayName, query.get("keyword")) || matches(source.sourceId, query.get("keyword")))
                .filter(source -> query.get("sourceService") == null || source.sourceService.equals(query.get("sourceService")))
                .filter(source -> query.get("sourceType") == null || source.sourceType.equals(query.get("sourceType")))
                .filter(source -> query.get("healthStatus") == null || source.healthStatus.equals(query.get("healthStatus")))
                .filter(source -> enabled == null || source.enabled == enabled)
                .sorted(sourceComparator(query.get("sort")))
                .map(AlertSource::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/sources/{sourceId}")
    ResponseEntity<Map<String, Object>> source(HttpServletRequest request, @PathVariable String sourceId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        AlertSource source = store.source(sourceId);
        Map<String, Object> view = source.view();
        view.put("recentSnapshot", Map.of("sourceId", sourceId, "status", source.healthStatus, "sanitized", true));
        return ok(request, view);
    }

    @GetMapping("/rules")
    ResponseEntity<Map<String, Object>> rules(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "severity_desc");
        List<Map<String, Object>> items = store.rules.values().stream()
                .filter(rule -> matches(rule.displayName, query.get("keyword")) || matches(rule.ruleId, query.get("keyword")))
                .filter(rule -> query.get("sourceService") == null || rule.sourceService.equals(query.get("sourceService")))
                .filter(rule -> query.get("sourceType") == null || rule.sourceType.equals(query.get("sourceType")))
                .filter(rule -> query.get("severity") == null || rule.severity.equals(query.get("severity")))
                .filter(rule -> query.get("status") == null || rule.status.equals(query.get("status")))
                .filter(rule -> query.get("routeId") == null || Objects.equals(rule.routeId, query.get("routeId")))
                .filter(rule -> labelMatch(rule.labels, query))
                .sorted(ruleComparator(query.get("sort")))
                .map(AlertRule::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/rules/{ruleId}")
    ResponseEntity<Map<String, Object>> rule(HttpServletRequest request, @PathVariable String ruleId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        AlertRule rule = store.rule(ruleId);
        Map<String, Object> view = rule.view();
        view.put("recentEvaluation", store.evaluations.values().stream()
                .filter(evaluation -> evaluation.ruleId.equals(ruleId))
                .findFirst()
                .map(AlertEvaluation::view)
                .orElse(null));
        view.put("recentAlert", store.alerts.values().stream()
                .filter(alert -> alert.ruleId.equals(ruleId))
                .findFirst()
                .map(AlertInstance::view)
                .orElse(null));
        view.put("routeSummary", rule.routeId == null ? null : Optional.ofNullable(store.routes.get(rule.routeId)).map(AlertRoute::summary).orElse(null));
        return ok(request, view);
    }

    @PostMapping("/rules")
    ResponseEntity<Map<String, Object>> createRule(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateRuleBody(body, true);
        return idempotent(request, actor, "rule:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                String routeId = text(body.get("routeId"));
                if (!routeId.isBlank() && !store.routes.containsKey(routeId)) {
                    throw new AlertingException(HttpStatus.NOT_FOUND, 49904, "alert route not found");
                }
                String displayName = text(body.get("displayName"));
                String sourceService = text(body.get("sourceService"));
                boolean duplicate = store.rules.values().stream()
                        .anyMatch(rule -> rule.sourceService.equals(sourceService) && rule.displayName.equalsIgnoreCase(displayName));
                if (duplicate) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "alert rule conflict");
                }
                String ruleId = "rule-" + store.nextId();
                AlertRule rule = AlertRule.from(ruleId, body, "DRAFT", actor.userId);
                store.rules.put(ruleId, rule);
                store.audit("ALERT_RULE_CREATED", "RULE", ruleId, actor, request, body, "MEDIUM", "SUCCESS", null, null, rule.status);
                return created(request, rule.view());
            }
        });
    }

    @PatchMapping("/rules/{ruleId}")
    ResponseEntity<Map<String, Object>> patchRule(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateRuleBody(body, false);
        return idempotent(request, actor, "rule:patch:" + ruleId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertRule rule = store.rule(ruleId);
                if ("ARCHIVED".equals(rule.status)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "alert rule state conflict");
                }
                String routeId = text(body.get("routeId"));
                if (body.containsKey("routeId") && !routeId.isBlank() && !store.routes.containsKey(routeId)) {
                    throw new AlertingException(HttpStatus.NOT_FOUND, 49904, "alert route not found");
                }
                String before = rule.status;
                rule.patch(body, actor.userId);
                store.audit("ALERT_RULE_UPDATED", "RULE", ruleId, actor, request, body, "MEDIUM", "SUCCESS", null, before, rule.status);
                return ok(request, rule.view());
            }
        });
    }

    @PatchMapping("/rules/{ruleId}/enable")
    ResponseEntity<Map<String, Object>> enableRule(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        return ruleStatus(request, ruleId, body, "ENABLED", "ALERT_RULE_ENABLED");
    }

    @PatchMapping("/rules/{ruleId}/disable")
    ResponseEntity<Map<String, Object>> disableRule(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        return ruleStatus(request, ruleId, body, "DISABLED", "ALERT_RULE_DISABLED");
    }

    private ResponseEntity<Map<String, Object>> ruleStatus(HttpServletRequest request, String ruleId, Map<String, Object> body, String status, String action) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "rule:" + status + ":" + ruleId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertRule rule = store.rule(ruleId);
                if ("ARCHIVED".equals(rule.status)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "alert rule state conflict");
                }
                String before = rule.status;
                rule.status = status;
                rule.updatedBy = actor.userId;
                rule.updatedAt = now();
                store.audit(action, "RULE", ruleId, actor, request, body, "MEDIUM", "SUCCESS", null, before, rule.status);
                return ok(request, rule.view());
            }
        });
    }

    @PostMapping("/rules/{ruleId}/evaluate")
    ResponseEntity<Map<String, Object>> evaluate(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_READ");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (properties.enabled()) {
            String sourceMode = request.getHeader("X-Test-Source-Mode");
            if ("unavailable".equals(sourceMode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46910, "alert source unavailable");
            if ("timeout".equals(sourceMode)) throw new AlertingException(HttpStatus.GATEWAY_TIMEOUT, 46911, "alert source timeout");
            if ("bad-schema".equals(sourceMode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46912, "alert source bad schema");
        }
        return idempotent(request, actor, "rule:evaluate:" + ruleId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertRule rule = store.rule(ruleId);
                if (!"ENABLED".equals(rule.status)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "alert rule state conflict");
                }
                Map<String, Object> sourceSnapshot = objectMap(body.get("sourceSnapshot"));
                if (sourceSnapshot.isEmpty()) {
                    throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "source snapshot is required");
                }
                boolean dryRun = bool(body.get("dryRun"));
                String sourceRef = textOr(sourceSnapshot.get("nodeId"), textOr(sourceSnapshot.get("sourceRef"), "source"));
                String groupKey = rule.sourceService + ":" + sourceRef;
                String fingerprint = store.dedupeFingerprint(rule, sourceSnapshot, sourceRef, groupKey);
                AlertInstance existing = store.alerts.values().stream()
                        .filter(alert -> alert.fingerprint.equals(fingerprint) && !"CLOSED".equals(alert.status))
                        .findFirst()
                        .orElse(null);
                boolean suppressed = store.activeSilenceMatches(rule, sourceSnapshot);
                String alertId = existing == null ? null : existing.alertId;
                boolean dedupeHit = existing != null;
                if (!dryRun) {
                    if (existing == null) {
                        alertId = "alert-" + store.nextId();
                        AlertInstance alert = new AlertInstance(alertId, rule, sourceSnapshot, fingerprint, sourceRef, suppressed);
                        store.alerts.put(alertId, alert);
                        if (suppressed) {
                            AlertDelivery delivery = AlertDelivery.suppressed("delivery-" + store.nextId(), alertId, rule.routeId);
                            store.deliveries.put(delivery.deliveryId, delivery);
                            alert.notificationSummary = delivery.summary();
                            alert.suppressionSummary = Map.of("suppressed", true, "reason", "MATCHED_SILENCE");
                        } else {
                            store.deliverIfRouteMatches(request, actor, rule, alert);
                        }
                    } else {
                        existing.lastFiredAt = now();
                        if (suppressed) {
                            existing.status = "SUPPRESSED";
                            existing.suppressionSummary = Map.of("suppressed", true, "reason", "MATCHED_SILENCE");
                            existing.notificationSummary = Map.of("status", "SUPPRESSED");
                        } else if ("SUPPRESSED".equals(existing.status)) {
                            existing.status = "FIRING";
                            existing.suppressionSummary = Map.of("suppressed", false, "reason", "SILENCE_NOT_MATCHED");
                            store.deliverIfRouteMatches(request, actor, rule, existing);
                        } else {
                            store.deliverIfRouteMatches(request, actor, rule, existing);
                        }
                    }
                }
                String evaluationId = "evaluation-" + store.nextId();
                AlertEvaluation evaluation = new AlertEvaluation(evaluationId, ruleId, "MATCHED", sourceRef, alertId, dedupeHit, suppressed, actor.userId);
                store.evaluations.put(evaluationId, evaluation);
                store.audit("ALERT_RULE_EVALUATED", "RULE", ruleId, actor, request, body, "MEDIUM", "SUCCESS", null, null, evaluation.status);
                return created(request, evaluation.view());
            }
        });
    }

    @GetMapping("/alerts")
    ResponseEntity<Map<String, Object>> alerts(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "lastFiredAt_desc", "firstFiredAt_desc", "severity_desc", "status_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.alerts.values().stream()
                .filter(alert -> query.get("ruleId") == null || alert.ruleId.equals(query.get("ruleId")))
                .filter(alert -> query.get("sourceService") == null || alert.sourceService.equals(query.get("sourceService")))
                .filter(alert -> query.get("severity") == null || alert.severity.equals(query.get("severity")))
                .filter(alert -> query.get("status") == null || alert.status.equals(query.get("status")))
                .filter(alert -> query.get("groupKey") == null || alert.groupKey.equals(query.get("groupKey")))
                .filter(alert -> matches(alert.summary, query.get("keyword")) || matches(alert.alertId, query.get("keyword")))
                .filter(alert -> labelMatch(alert.labels, query))
                .filter(alert -> inRange(alert.firstFiredAt, query))
                .sorted(alertComparator(query.get("sort")))
                .map(AlertInstance::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/alerts/{alertId}")
    ResponseEntity<Map<String, Object>> alert(HttpServletRequest request, @PathVariable String alertId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        return ok(request, store.alert(alertId).view());
    }

    @PatchMapping("/alerts/{alertId}/acknowledge")
    ResponseEntity<Map<String, Object>> acknowledge(HttpServletRequest request, @PathVariable String alertId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "alert:ack:" + alertId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertInstance alert = store.alert(alertId);
                if ("CLOSED".equals(alert.status)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "alert state conflict");
                }
                String before = alert.status;
                if (!"ACKNOWLEDGED".equals(alert.status)) {
                    alert.status = "ACKNOWLEDGED";
                    alert.acknowledgedBy = actor.userId;
                    alert.acknowledgedAt = now();
                }
                store.audit("ALERT_ACKNOWLEDGED", "ALERT", alertId, actor, request, body, "MEDIUM", "SUCCESS", null, before, alert.status);
                return ok(request, alert.view());
            }
        });
    }

    @PatchMapping("/alerts/{alertId}/close")
    ResponseEntity<Map<String, Object>> close(HttpServletRequest request, @PathVariable String alertId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (!"CLOSE_ALERT".equals(text(body.get("confirmText")))) {
            throw new AlertingException(HttpStatus.FORBIDDEN, 42003, "alert close not confirmed");
        }
        return idempotent(request, actor, "alert:close:" + alertId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertInstance alert = store.alert(alertId);
                if ("BLOCKER".equals(alert.severity) && !"OWNER".equals(actor.role) && !actor.permissions.contains("HIGH_RISK_APPROVE")) {
                    throw new AlertingException(HttpStatus.FORBIDDEN, 42002, "capability denied");
                }
                String before = alert.status;
                if (!"CLOSED".equals(alert.status)) {
                    alert.status = "CLOSED";
                    alert.closedBy = actor.userId;
                    alert.closedAt = now();
                    alert.resolutionSummary = text(body.get("resolutionSummary"));
                }
                store.audit("ALERT_CLOSED", "ALERT", alertId, actor, request, body, "HIGH", "SUCCESS", null, before, alert.status);
                return ok(request, alert.view());
            }
        });
    }

    @GetMapping("/silences")
    ResponseEntity<Map<String, Object>> silences(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "startsAt_asc", "endsAt_asc");
        validateTimeRange(query);
        store.expireSilences();
        List<Map<String, Object>> items = store.silences.values().stream()
                .filter(silence -> query.get("status") == null || silence.status.equals(query.get("status")))
                .filter(silence -> matcherField(silence, "sourceService", query.get("sourceService")))
                .filter(silence -> matcherField(silence, "severity", query.get("severity")))
                .filter(silence -> labelMatch(objectMap(silence.matchers.get("labels")), query))
                .filter(silence -> inRange(silence.createdAt, query))
                .sorted(silenceComparator(query.get("sort")))
                .map(AlertSilence::view)
                .toList();
        return ok(request, page(items, query));
    }

    @PostMapping("/silences")
    ResponseEntity<Map<String, Object>> createSilence(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateSilence(body);
        return idempotent(request, actor, "silence:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                String silenceId = "silence-" + store.nextId();
                AlertSilence silence = AlertSilence.from(silenceId, body, actor.userId);
                silence.refreshStatus();
                store.silences.put(silenceId, silence);
                store.audit("ALERT_SILENCE_CREATED", "SILENCE", silenceId, actor, request, body, "MEDIUM", "SUCCESS", null, null, silence.status);
                return created(request, silence.view());
            }
        });
    }

    @PatchMapping("/silences/{silenceId}/cancel")
    ResponseEntity<Map<String, Object>> cancelSilence(HttpServletRequest request, @PathVariable String silenceId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "silence:cancel:" + silenceId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertSilence silence = store.silence(silenceId);
                silence.refreshStatus();
                if (!"ACTIVE".equals(silence.status) && !"CANCELLED".equals(silence.status)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49910, "silence state conflict");
                }
                String before = silence.status;
                if ("ACTIVE".equals(silence.status)) {
                    silence.status = "CANCELLED";
                    silence.cancelledBy = actor.userId;
                    silence.cancelledAt = now();
                }
                store.audit("ALERT_SILENCE_CANCELLED", "SILENCE", silenceId, actor, request, body, "MEDIUM", "SUCCESS", null, before, silence.status);
                return ok(request, silence.view());
            }
        });
    }

    @GetMapping("/routes")
    ResponseEntity<Map<String, Object>> routes(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "displayName_asc");
        List<Map<String, Object>> items = store.routes.values().stream()
                .filter(route -> matches(route.displayName, query.get("keyword")) || matches(route.routeId, query.get("keyword")))
                .filter(route -> query.get("status") == null || route.status.equals(query.get("status")))
                .filter(route -> matcherField(route.matchers, "severity", query.get("severity")))
                .filter(route -> matcherField(route.matchers, "sourceService", query.get("sourceService")))
                .sorted(routeComparator(query.get("sort")))
                .map(AlertRoute::view)
                .toList();
        return ok(request, page(items, query));
    }

    @PostMapping("/routes")
    ResponseEntity<Map<String, Object>> createRoute(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateRoute(body);
        return idempotent(request, actor, "route:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                String routeId = "route-" + store.nextId();
                AlertRoute route = AlertRoute.from(routeId, body, actor.userId);
                store.routes.put(routeId, route);
                store.audit("ALERT_ROUTE_CREATED", "ROUTE", routeId, actor, request, body, "HIGH", "SUCCESS", null, null, route.status);
                return created(request, route.view());
            }
        });
    }

    @PatchMapping("/routes/{routeId}")
    ResponseEntity<Map<String, Object>> patchRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateRoutePatch(body);
        return idempotent(request, actor, "route:patch:" + routeId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertRoute route = store.route(routeId);
                String before = route.status;
                route.patch(body, actor.userId);
                store.audit("ALERT_ROUTE_UPDATED", "ROUTE", routeId, actor, request, body, "HIGH", "SUCCESS", null, before, route.status);
                return ok(request, route.view());
            }
        });
    }

    @PostMapping("/routes/{routeId}/test")
    ResponseEntity<Map<String, Object>> testRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Notification-Mode");
            if ("unavailable".equals(mode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46900, "notification unavailable");
            if ("timeout".equals(mode)) throw new AlertingException(HttpStatus.GATEWAY_TIMEOUT, 46901, "notification timeout");
            if ("bad-schema".equals(mode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46902, "notification bad schema");
        }
        return idempotent(request, actor, "route:test:" + routeId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                AlertRoute route = store.route(routeId);
                String deliveryId = "delivery-" + store.nextId();
                AlertDelivery delivery = store.createExternalDelivery(request, actor, route,
                        textOr(objectMap(body.get("sampleAlert")).get("alertId"), "sample-alert"), objectMap(body.get("sampleAlert")),
                        "alert-route-test:" + route.routeId + ":" + textOr(body.get("idempotencyKey"), store.fingerprint(body)), deliveryId,
                        textOr(body.get("reason"), "alert route test"));
                store.deliveries.put(deliveryId, delivery);
                store.audit("ALERT_ROUTE_TESTED", "ROUTE", routeId, actor, request, body, "HIGH", "SUCCESS", null, null, "SENT");
                return created(request, delivery.view());
            }
        });
    }

    @GetMapping("/deliveries")
    ResponseEntity<Map<String, Object>> deliveries(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "lastAttemptAt_desc", "status_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.deliveries.values().stream()
                .filter(delivery -> query.get("alertId") == null || delivery.alertId.equals(query.get("alertId")))
                .filter(delivery -> query.get("routeId") == null || Objects.equals(delivery.routeId, query.get("routeId")))
                .filter(delivery -> query.get("status") == null || delivery.status.equals(query.get("status")))
                .filter(delivery -> inRange(delivery.createdAt, query))
                .sorted(deliveryComparator(query.get("sort")))
                .map(AlertDelivery::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request, @RequestParam Map<String, String> query) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.audits.stream()
                .filter(audit -> query.get("actorUserId") == null || audit.actorUserId.equals(query.get("actorUserId")))
                .filter(audit -> query.get("ruleId") == null || Objects.equals(audit.ruleId, query.get("ruleId")))
                .filter(audit -> query.get("alertId") == null || Objects.equals(audit.alertId, query.get("alertId")))
                .filter(audit -> query.get("silenceId") == null || Objects.equals(audit.silenceId, query.get("silenceId")))
                .filter(audit -> query.get("routeId") == null || Objects.equals(audit.routeId, query.get("routeId")))
                .filter(audit -> query.get("action") == null || audit.action.equals(query.get("action")))
                .filter(audit -> query.get("result") == null || audit.result.equals(query.get("result")))
                .filter(audit -> query.get("riskLevel") == null || audit.riskLevel.equals(query.get("riskLevel")))
                .filter(audit -> inRange(audit.createdAt, query))
                .sorted(auditComparator(query.get("sort")))
                .map(AlertAudit::view)
                .toList();
        return ok(request, page(items, query));
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(request, data));
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, data));
    }

    private Map<String, Object> envelope(HttpServletRequest request, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body,
                                                          Supplier<ResponseEntity<Map<String, Object>>> action) {
        String key = text(body == null ? null : body.get("idempotencyKey"));
        if (key.isBlank()) {
            return action.get();
        }
        String idempotencyScope = actor.userId + ":" + scope + ":" + key;
        String fingerprint = store.fingerprint(body);
        synchronized (store) {
            IdempotencyRecord existing = store.idempotency.get(idempotencyScope);
            if (existing != null) {
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new AlertingException(HttpStatus.CONFLICT, 49912, "idempotency fingerprint conflict");
                }
                return ResponseEntity.status(existing.status()).body(envelope(request, existing.data()));
            }
            ResponseEntity<Map<String, Object>> response = action.get();
            Object responseData = response.getBody() == null ? null : response.getBody().get("data");
            store.idempotency.put(idempotencyScope, new IdempotencyRecord(fingerprint, HttpStatus.valueOf(response.getStatusCode().value()), responseData));
            return response;
        }
    }

    private static void validateRuleBody(Map<String, Object> body, boolean create) {
        if (create || body.containsKey("displayName")) validateText(body, "displayName");
        if (create || body.containsKey("sourceService")) validateText(body, "sourceService");
        if (create || body.containsKey("sourceType")) validateText(body, "sourceType");
        if (create || body.containsKey("severity")) validateText(body, "severity");
        if (create || body.containsKey("conditionType")) validateText(body, "conditionType");
        validateText(body, "reason");
        if ((create || body.containsKey("conditionSummary")) && objectMap(body.get("conditionSummary")).isEmpty()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49911, "alert rule condition invalid");
        }
        if (objectMap(body.get("conditionSummary")).containsKey("bad")) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49911, "alert rule condition invalid");
        }
        int window = bodyIntValue(body, "evaluationWindowSeconds", 300, 49911, "alert rule condition invalid");
        int duration = bodyIntValue(body, "forDurationSeconds", 0, 49911, "alert rule condition invalid");
        if ((create || body.containsKey("evaluationWindowSeconds")) && (window < 60 || window > 86400)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49911, "alert rule condition invalid");
        }
        if ((create || body.containsKey("forDurationSeconds")) && (duration < 0 || duration > 86400)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49911, "alert rule condition invalid");
        }
    }

    private static void validateSilence(Map<String, Object> body) {
        validateText(body, "reason");
        Map<String, Object> matchers = objectMap(body.get("matchers"));
        boolean hasKnownMatcher = matchers.containsKey("sourceService")
                || matchers.containsKey("severity")
                || matchers.containsKey("groupKey")
                || !objectMap(matchers.get("labels")).isEmpty();
        if (!hasKnownMatcher) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49914, "silence matchers invalid");
        }
        String startsAt = text(body.get("startsAt"));
        String endsAt = text(body.get("endsAt"));
        if (startsAt.isBlank() || endsAt.isBlank()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49913, "silence time range invalid");
        }
        try {
            if (Instant.parse(endsAt).compareTo(Instant.parse(startsAt)) <= 0) {
                throw new AlertingException(HttpStatus.BAD_REQUEST, 49913, "silence time range invalid");
            }
        } catch (AlertingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 49913, "silence time range invalid");
        }
    }

    private static void validateRoute(Map<String, Object> body) {
        validateText(body, "displayName");
        validateText(body, "reason");
        if (objectMap(body.get("matchers")).isEmpty() || stringList(body.get("groupBy")).isEmpty()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
        validateRouteIntervals(body, true);
    }

    private static void validateRoutePatch(Map<String, Object> body) {
        validateRouteIntervals(body, false);
        if (body.containsKey("displayName")) validateText(body, "displayName");
        if (body.containsKey("matchers") && objectMap(body.get("matchers")).isEmpty()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
        if (body.containsKey("groupBy") && stringList(body.get("groupBy")).isEmpty()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
    }

    private static void validateRouteIntervals(Map<String, Object> body, boolean create) {
        int groupWait = bodyIntValue(body, "groupWaitSeconds", 0, 40001, "alert route invalid");
        int groupInterval = bodyIntValue(body, "groupIntervalSeconds", 60, 40001, "alert route invalid");
        int repeatInterval = bodyIntValue(body, "repeatIntervalSeconds", 300, 40001, "alert route invalid");
        if ((create || body.containsKey("groupWaitSeconds")) && (groupWait < 0 || groupWait > 3600)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
        if ((create || body.containsKey("groupIntervalSeconds")) && (groupInterval < 60 || groupInterval > 86400)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
        if ((create || body.containsKey("repeatIntervalSeconds")) && (repeatInterval < 300 || repeatInterval > 604800)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "alert route invalid");
        }
    }

    private static void validateReason(Map<String, Object> body) {
        validateText(body, "reason");
    }

    private static void validateText(Map<String, Object> body, String field) {
        if (text(body == null ? null : body.get(field)).isBlank()) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "invalid " + field);
        }
    }

    private static void rejectTrusted(Map<String, Object> body) {
        if (body == null) return;
        if (containsTrustedField(body)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "trusted field is not allowed");
        }
    }

    private static boolean containsTrustedField(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lower = key.toLowerCase();
                boolean trusted = List.of("actorUserId", "actorRole", "actorPermissions", "beforeState", "afterState",
                                "auditResult", "createdBy", "updatedBy", "acknowledgedBy", "closedBy", "suppressedBy",
                                "deliveryStatus", "raw" + "Token", "secret" + "Key", "node" + "Token", "notification" + "Token",
                                "webhook" + "Secret", "smtp" + "Password", "sms" + "Token", "internal" + "Path", "resolved" + "Path")
                        .contains(key)
                        || lower.contains("password")
                        || lower.contains("secret")
                        || lower.contains("token")
                        || lower.contains("cred" + "ential")
                        || lower.contains("authorization")
                        || lower.contains("internalpath")
                        || lower.contains("resolvedpath");
                if (trusted || containsTrustedField(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(AlertingController::containsTrustedField);
        }
        return false;
    }

    private static void validatePage(Map<String, String> query) {
        int page = queryIntValue(query, "page", 1, 40002, "invalid page");
        int pageSize = queryIntValue(query, "pageSize", 20, 40002, "invalid page");
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    private static void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) return;
        if (List.of(allowed).stream().noneMatch(sort::equals)) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    private static void validateTimeRange(Map<String, String> query) {
        if (query.get("from") == null || query.get("to") == null) return;
        try {
            if (Instant.parse(query.get("from")).compareTo(Instant.parse(query.get("to"))) > 0) {
                throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
            }
        } catch (AlertingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        }
    }

    private static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intValue(query.get("page"), 1);
        int pageSize = intValue(query.get("pageSize"), 20);
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private static boolean matches(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private static boolean labelMatch(Map<String, Object> labels, Map<String, String> query) {
        String key = query.get("labelKey");
        if (key == null || key.isBlank()) return true;
        Object value = labels.get(key);
        String expected = query.get("labelValue");
        return value != null && (expected == null || Objects.equals(String.valueOf(value), expected));
    }

    private static boolean matcherField(AlertSilence silence, String field, String expected) {
        return matcherField(silence.matchers, field, expected);
    }

    private static boolean matcherField(Map<String, Object> matchers, String field, String expected) {
        if (expected == null) return true;
        return Objects.equals(String.valueOf(matchers.get(field)), expected);
    }

    private static boolean inRange(String value, Map<String, String> query) {
        Instant current = Instant.parse(value);
        Instant from = query.get("from") == null ? Instant.MIN : Instant.parse(query.get("from"));
        Instant to = query.get("to") == null ? Instant.MAX : Instant.parse(query.get("to"));
        return !current.isBefore(from) && !current.isAfter(to);
    }

    private static Comparator<AlertSource> sourceComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(source -> source.displayName);
        if ("lastEventAt_desc".equals(sort)) return Comparator.comparing((AlertSource source) -> source.lastEventAt).reversed();
        return Comparator.comparing((AlertSource source) -> source.lastSnapshotAt).reversed();
    }

    private static Comparator<AlertRule> ruleComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(rule -> rule.displayName);
        if ("severity_desc".equals(sort)) return Comparator.comparing((AlertRule rule) -> rule.severity).reversed();
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((AlertRule rule) -> rule.createdAt).reversed();
        return Comparator.comparing((AlertRule rule) -> rule.updatedAt).reversed();
    }

    private static Comparator<AlertInstance> alertComparator(String sort) {
        if ("firstFiredAt_desc".equals(sort)) return Comparator.comparing((AlertInstance alert) -> alert.firstFiredAt).reversed();
        if ("severity_desc".equals(sort)) return Comparator.comparing((AlertInstance alert) -> alert.severity).reversed();
        if ("status_asc".equals(sort)) return Comparator.comparing(alert -> alert.status);
        return Comparator.comparing((AlertInstance alert) -> alert.lastFiredAt).reversed();
    }

    private static Comparator<AlertSilence> silenceComparator(String sort) {
        if ("startsAt_asc".equals(sort)) return Comparator.comparing(silence -> silence.startsAt);
        if ("endsAt_asc".equals(sort)) return Comparator.comparing(silence -> silence.endsAt);
        return Comparator.comparing((AlertSilence silence) -> silence.createdAt).reversed();
    }

    private static Comparator<AlertRoute> routeComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(route -> route.displayName);
        return Comparator.comparing((AlertRoute route) -> route.updatedAt).reversed();
    }

    private static Comparator<AlertDelivery> deliveryComparator(String sort) {
        if ("lastAttemptAt_desc".equals(sort)) return Comparator.comparing((AlertDelivery delivery) -> delivery.lastAttemptAt).reversed();
        if ("status_asc".equals(sort)) return Comparator.comparing(delivery -> delivery.status);
        return Comparator.comparing((AlertDelivery delivery) -> delivery.createdAt).reversed();
    }

    private static Comparator<AlertAudit> auditComparator(String sort) {
        if ("createdAt_asc".equals(sort)) return Comparator.comparing(audit -> audit.createdAt);
        if ("riskLevel_desc".equals(sort)) return Comparator.comparing((AlertAudit audit) -> audit.riskLevel).reversed();
        return Comparator.comparing((AlertAudit audit) -> audit.createdAt).reversed();
    }

    private static String now() {
        return Instant.now().toString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int bodyIntValue(Map<String, Object> body, String field, int fallback, int code, String message) {
        if (body == null || !body.containsKey(field)) return fallback;
        return strictIntValue(body.get(field), code, message);
    }

    private static int queryIntValue(Map<String, String> query, String field, int fallback, int code, String message) {
        String value = query.get(field);
        if (value == null) return fallback;
        return strictIntValue(value, code, message);
    }

    private static int strictIntValue(Object value, int code, String message) {
        String raw = text(value).trim();
        if (!raw.matches("-?\\d+")) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, code, message);
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new AlertingException(HttpStatus.BAD_REQUEST, code, message);
        }
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(text(value));
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static Map<String, Object> objectMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        }
        return result;
    }
}

@Service
class AlertingStore {
    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{([^{}]+)}}");
    final Map<String, AlertSource> sources = new ConcurrentHashMap<>();
    final Map<String, AlertRule> rules = new ConcurrentHashMap<>();
    final Map<String, AlertInstance> alerts = new ConcurrentHashMap<>();
    final Map<String, AlertSilence> silences = new ConcurrentHashMap<>();
    final Map<String, AlertRoute> routes = new ConcurrentHashMap<>();
    final Map<String, AlertDelivery> deliveries = new ConcurrentHashMap<>();
    final Map<String, AlertEvaluation> evaluations = new ConcurrentHashMap<>();
    final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    final List<AlertAudit> audits = new ArrayList<>();
    private final ObjectMapper mapper;
    private final AlertingExternalDeliveryAdapter externalDeliveryAdapter;
    private long sequence = 1000;

    AlertingStore(ObjectMapper mapper, AlertingExternalDeliveryAdapter externalDeliveryAdapter) {
        this.mapper = mapper;
        this.externalDeliveryAdapter = externalDeliveryAdapter;
    }

    @PostConstruct
    void seed() {
        sources.put("source-ops-health", new AlertSource("source-ops-health", "OPS_CONTROL", "HEALTH", "Ops control health", true, "AVAILABLE", List.of("heartbeat", "node-status")));
        sources.put("source-backup-health", new AlertSource("source-backup-health", "BACKUP_RECOVERY", "TASK", "Backup recovery task", true, "AVAILABLE", List.of("backup-job", "restore-request")));
        AlertRoute route = new AlertRoute("route-default", "Default ops route", Map.of("severity", "WARNING", "sourceService", "OPS_CONTROL"),
                List.of("sourceService", "groupKey"), 30, 300, 900, Map.of("templateCode", "ALERT_WARNING", "channel", "IN_APP"),
                Map.of("receiverType", "IN_APP", "target", "ops-admins"), "ENABLED", "system");
        routes.put(route.routeId, route);
        AlertRule nodeOffline = new AlertRule("rule-node-offline", "Node offline heartbeat", "OPS_CONTROL", "HEALTH", "WARNING",
                Map.of("service", "ops-control", "scope", "node"), "MISSING_HEARTBEAT",
                Map.of("metric", "heartbeatAgeSeconds", "operator", ">", "threshold", 300), 300, 60,
                "{{sourceService}}:{{nodeId}}", "route-default", "/admin/ops/nodes", "ENABLED", "system");
        rules.put(nodeOffline.ruleId, nodeOffline);
        AlertRule blocker = new AlertRule("rule-backup-blocker", "Production restore blocker", "BACKUP_RECOVERY", "TASK", "BLOCKER",
                Map.of("service", "backup-recovery", "scope", "restore"), "RESTORE_PENDING",
                Map.of("status", "EXECUTION_BLOCKED"), 300, 0, "{{sourceService}}:{{sourceRef}}",
                "route-default", "/admin/backup/recovery", "ENABLED", "system");
        rules.put(blocker.ruleId, blocker);
    }

    synchronized String nextId() {
        sequence += 1;
        return String.valueOf(sequence);
    }

    AlertSource source(String id) {
        return Optional.ofNullable(sources.get(id)).orElseThrow(() -> new AlertingException(HttpStatus.NOT_FOUND, 49900, "alert source not found"));
    }

    AlertRule rule(String id) {
        return Optional.ofNullable(rules.get(id)).orElseThrow(() -> new AlertingException(HttpStatus.NOT_FOUND, 49901, "alert rule not found"));
    }

    AlertInstance alert(String id) {
        return Optional.ofNullable(alerts.get(id)).orElseThrow(() -> new AlertingException(HttpStatus.NOT_FOUND, 49902, "alert not found"));
    }

    AlertSilence silence(String id) {
        return Optional.ofNullable(silences.get(id)).orElseThrow(() -> new AlertingException(HttpStatus.NOT_FOUND, 49903, "silence not found"));
    }

    AlertRoute route(String id) {
        return Optional.ofNullable(routes.get(id)).orElseThrow(() -> new AlertingException(HttpStatus.NOT_FOUND, 49904, "alert route not found"));
    }

    void failAuditIfRequested(HttpServletRequest request, boolean enabled) {
        if (enabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new AlertingException(HttpStatus.INTERNAL_SERVER_ERROR, 55501, "alerting audit write failed");
        }
    }

    boolean activeSilenceMatches(AlertRule rule, Map<String, Object> sourceSnapshot) {
        expireSilences();
        String sourceRef = AlertingText.textOr(sourceSnapshot.get("nodeId"), AlertingText.textOr(sourceSnapshot.get("sourceRef"), ""));
        String groupKey = rule.sourceService + ":" + sourceRef;
        Map<String, Object> alertLabels = new LinkedHashMap<>(rule.labels);
        alertLabels.putIfAbsent("node", sourceRef);
        Instant now = Instant.now();
        return silences.values().stream().anyMatch(silence -> {
            if (!"ACTIVE".equals(silence.status)) return false;
            Instant start = Instant.parse(silence.startsAt);
            Instant end = Instant.parse(silence.endsAt);
            if (now.isBefore(start) || now.isAfter(end)) return false;
            if (silence.matchers.get("sourceService") != null && !Objects.equals(silence.matchers.get("sourceService"), rule.sourceService)) return false;
            if (silence.matchers.get("severity") != null && !Objects.equals(silence.matchers.get("severity"), rule.severity)) return false;
            if (silence.matchers.get("groupKey") != null && !Objects.equals(String.valueOf(silence.matchers.get("groupKey")), groupKey)) return false;
            Map<String, Object> labels = AlertingText.objectMap(silence.matchers.get("labels"));
            return labels.entrySet().stream()
                    .allMatch(entry -> Objects.equals(String.valueOf(entry.getValue()), String.valueOf(alertLabels.get(entry.getKey()))));
        });
    }

    void expireSilences() {
        silences.values().forEach(AlertSilence::refreshStatus);
    }

    String dedupeFingerprint(AlertRule rule, Map<String, Object> sourceSnapshot, String sourceRef, String groupKey) {
        String template = AlertingText.textOr(rule.dedupeKeyTemplate, "{{sourceService}}:{{sourceRef}}");
        Matcher matcher = TEMPLATE.matcher(template);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String value = templateValue(matcher.group(1).trim(), rule, sourceSnapshot, sourceRef, groupKey);
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(fingerprintPart(value)));
        }
        matcher.appendTail(resolved);
        String fingerprint = resolved.toString().replaceAll("\\s+", "_");
        if (fingerprint.isBlank() || fingerprint.replace(":", "").isBlank()) {
            return rule.sourceService + ":" + sourceRef;
        }
        return fingerprint;
    }

    private String templateValue(String key, AlertRule rule, Map<String, Object> sourceSnapshot, String sourceRef, String groupKey) {
        if ("sourceService".equals(key)) return rule.sourceService;
        if ("sourceType".equals(key)) return rule.sourceType;
        if ("severity".equals(key)) return rule.severity;
        if ("sourceRef".equals(key)) return sourceRef;
        if ("nodeId".equals(key)) return AlertingText.textOr(sourceSnapshot.get("nodeId"), sourceRef);
        if ("groupKey".equals(key)) return groupKey;
        if (key.startsWith("labels.")) return AlertingText.text(rule.labels.get(key.substring("labels.".length())));
        if (key.startsWith("snapshot.")) return AlertingText.text(sourceSnapshot.get(key.substring("snapshot.".length())));
        return AlertingText.text(sourceSnapshot.get(key));
    }

    private String fingerprintPart(String value) {
        return AlertingText.text(value).trim().replaceAll("\\s+", "_");
    }

    void deliverIfRouteMatches(HttpServletRequest request, Actor actor, AlertRule rule, AlertInstance alert) {
        if (rule.routeId == null || rule.routeId.isBlank()) {
            alert.notificationSummary = Map.of("status", "PENDING", "reason", "NO_ROUTE");
            return;
        }
        AlertRoute route = routes.get(rule.routeId);
        if (route == null) {
            alert.notificationSummary = Map.of("status", "PENDING", "reason", "ROUTE_NOT_FOUND");
            return;
        }
        if (!"ENABLED".equals(route.status)) {
            alert.notificationSummary = Map.of("status", "PENDING", "reason", "ROUTE_DISABLED", "routeId", route.routeId);
            return;
        }
        if (!routeMatches(route, alert)) {
            alert.notificationSummary = Map.of("status", "PENDING", "reason", "ROUTE_NOT_MATCHED", "routeId", route.routeId);
            return;
        }
        AlertDelivery existing = deliveries.values().stream()
                .filter(delivery -> alert.alertId.equals(delivery.alertId))
                .filter(delivery -> route.routeId.equals(delivery.routeId))
                .filter(delivery -> !"SUPPRESSED".equals(delivery.status))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            alert.notificationSummary = existing.summary();
            return;
        }
        String deliveryId = "delivery-" + nextId();
        AlertDelivery delivery = createExternalDelivery(request, actor, route, alert.alertId,
                Map.of("summary", alert.summary, "severity", alert.severity, "sourceService", alert.sourceService, "labels", alert.labels),
                "alert:" + alert.alertId + ":" + route.routeId + ":" + alert.fingerprint,
                deliveryId, "alerting rule matched");
        deliveries.put(delivery.deliveryId, delivery);
        alert.notificationSummary = delivery.summary();
    }

    AlertDelivery createExternalDelivery(HttpServletRequest request, Actor actor, AlertRoute route, String alertId,
                                         Map<String, Object> alertSummary, String idempotencyKey, String deliveryId, String reason) {
        AlertingExternalDeliveryAdapter.Result result = externalDeliveryAdapter.createSimulatedDelivery(request,
                actor.userId, actor.displayName, cpnDeliveryBody(route, alertId, alertSummary, idempotencyKey, reason));
        return result.success()
                ? AlertDelivery.fromCpn(deliveryId, alertId, route.routeId, result)
                : AlertDelivery.failedExternal(deliveryId, alertId, route.routeId, result);
    }

    private Map<String, Object> cpnDeliveryBody(AlertRoute route, String alertId, Map<String, Object> alertSummary,
                                               String idempotencyKey, String reason) {
        return AlertingMaps.linked(
                "sourceModule", "alerting",
                "sourceId", alertId,
                "eventType", "alert.firing",
                "riskLevel", cpnRiskLevel(AlertingText.text(alertSummary.get("severity"))),
                "providerId", "provider-discord-main",
                "templateMappingId", "mapping-notification-discord-main",
                "receiverSummary", cpnReceiver(route),
                "payloadSummary", cpnPayload(alertSummary),
                "expiresAt", Instant.now().plusSeconds(3600).toString(),
                "confirmText", "CREATE_EXTERNAL_DELIVERY",
                "reason", reason,
                "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> cpnReceiver(AlertRoute route) {
        Map<String, Object> receiver = AlertingText.objectMap(route.receiverSummary);
        String type = AlertingText.text(receiver.get("receiverType"));
        if ("CHANNEL".equals(type)) {
            return AlertingMaps.linked("receiverType", "CHANNEL",
                    "displayName", AlertingText.textOr(receiver.get("displayName"), "Ops"),
                    "targetRefSummary", AlertingText.textOr(receiver.get("targetRefSummary"), "#ops"));
        }
        return AlertingMaps.linked("receiverType", "CHANNEL", "displayName", "Ops", "targetRefSummary", "#ops");
    }

    private Map<String, Object> cpnPayload(Map<String, Object> alertSummary) {
        String title = AlertingText.textOr(alertSummary.get("summary"), "Alert firing");
        String body = AlertingText.textOr(alertSummary.get("sourceService"), "alerting") + " " + AlertingText.textOr(alertSummary.get("severity"), "WARNING");
        return AlertingMaps.linked("title", title, "body", body, "player", "system");
    }

    private String cpnRiskLevel(String severity) {
        return switch (severity) {
            case "INFO" -> "LOW";
            case "CRITICAL" -> "HIGH";
            case "BLOCKER" -> "CRITICAL";
            default -> "MEDIUM";
        };
    }

    private boolean routeMatches(AlertRoute route, AlertInstance alert) {
        if (route.matchers.get("sourceService") != null && !Objects.equals(String.valueOf(route.matchers.get("sourceService")), alert.sourceService)) return false;
        if (route.matchers.get("severity") != null && !Objects.equals(String.valueOf(route.matchers.get("severity")), alert.severity)) return false;
        if (route.matchers.get("groupKey") != null && !Objects.equals(String.valueOf(route.matchers.get("groupKey")), alert.groupKey)) return false;
        Map<String, Object> labels = AlertingText.objectMap(route.matchers.get("labels"));
        return labels.entrySet().stream()
                .allMatch(entry -> Objects.equals(String.valueOf(entry.getValue()), String.valueOf(alert.labels.get(entry.getKey()))));
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        audits.add(0, new AlertAudit("audit-" + nextId(), action, targetType, targetId, actor, request, body, riskLevel, result, failureReason, beforeState, afterState));
    }

    Map<String, Object> summary(boolean testControlsEnabled) {
        long enabledRules = rules.values().stream().filter(rule -> "ENABLED".equals(rule.status)).count();
        long firing = alerts.values().stream().filter(alert -> "FIRING".equals(alert.status)).count();
        long acknowledged = alerts.values().stream().filter(alert -> "ACKNOWLEDGED".equals(alert.status)).count();
        expireSilences();
        long activeSilences = silences.values().stream().filter(silence -> "ACTIVE".equals(silence.status)).count();
        long failedDeliveries = deliveries.values().stream().filter(delivery -> "FAILED".equals(delivery.status)).count();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("service", "alerting");
        view.put("port", 8133);
        view.put("legacyPort", 8120);
        view.put("storageMode", "IN_MEMORY");
        view.put("authMode", "TEST_STUB");
        view.put("sourceAdapterMode", "TEST_STUB");
        view.put("notificationAdapterMode", "TEST_STUB");
        view.put("externalDeliveryAdapterMode", "CPN_SIMULATED_EXTERNAL");
        view.put("testControlsEnabled", testControlsEnabled);
        view.put("sourcesTotal", sources.size());
        view.put("rulesTotal", rules.size());
        view.put("enabledRulesTotal", enabledRules);
        view.put("alertsTotal", alerts.size());
        view.put("firingAlertsTotal", firing);
        view.put("acknowledgedAlertsTotal", acknowledged);
        view.put("silencesTotal", silences.size());
        view.put("activeSilencesTotal", activeSilences);
        view.put("routesTotal", routes.size());
        view.put("deliveriesTotal", deliveries.size());
        view.put("failedDeliveriesTotal", failedDeliveries);
        view.put("auditsTotal", audits.size());
        view.put("idempotencyRecordsTotal", idempotency.size());
        view.put("lastAlertAt", alerts.values().stream().findFirst().map(alert -> alert.lastFiredAt).orElse(null));
        view.put("lastDeliveryFailureAt", deliveries.values().stream().filter(delivery -> "FAILED".equals(delivery.status)).findFirst().map(delivery -> delivery.lastAttemptAt).orElse(null));
        view.put("degraded", false);
        view.put("degradeReasons", List.of());
        view.put("productionGaps", List.of("REAL_PERSISTENCE_NOT_CONNECTED", "REAL_SOURCE_HTTP_NOT_CONNECTED",
                "REAL_NOTIFICATION_DELIVERY_NOT_CONNECTED", "REAL_METRIC_COLLECTION_NOT_CONNECTED",
                "ADMIN_READ_ONLY_ENTRY_NOT_CONNECTED", "NODE_DAEMON_DIRECT_CALL_FORBIDDEN",
                testControlsEnabled ? "TEST_CONTROLS_ENABLED_FOR_LOCAL_TEST" : "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
        return view;
    }

    String fingerprint(Object value) {
        try {
            return mapper.writeValueAsString(canonical(value));
        } catch (JsonProcessingException exception) {
            throw new AlertingException(HttpStatus.INTERNAL_SERVER_ERROR, 55500, "alerting internal error");
        }
    }

    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, mapValue) -> sorted.put(String.valueOf(key), canonical(mapValue)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonical).toList();
        }
        return value;
    }
}

class AlertSource {
    final String sourceId;
    final String sourceService;
    final String sourceType;
    final String displayName;
    final boolean enabled;
    final String healthStatus;
    final String lastEventAt = Instant.now().toString();
    final String lastSnapshotAt = lastEventAt;
    final List<String> capabilities;
    final Map<String, Object> labels = new LinkedHashMap<>();
    final boolean degraded = false;
    final List<String> degradeReasons = List.of();

    AlertSource(String sourceId, String sourceService, String sourceType, String displayName, boolean enabled, String healthStatus, List<String> capabilities) {
        this.sourceId = sourceId;
        this.sourceService = sourceService;
        this.sourceType = sourceType;
        this.displayName = displayName;
        this.enabled = enabled;
        this.healthStatus = healthStatus;
        this.capabilities = capabilities;
        this.labels.put("service", sourceService.toLowerCase());
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("sourceId", sourceId, "sourceService", sourceService, "sourceType", sourceType,
                "displayName", displayName, "enabled", enabled, "healthStatus", healthStatus, "lastEventAt", lastEventAt,
                "lastSnapshotAt", lastSnapshotAt, "capabilities", capabilities, "labels", labels, "degraded", degraded,
                "degradeReasons", degradeReasons);
    }
}

class AlertRule {
    final String ruleId;
    String displayName;
    String sourceService;
    String sourceType;
    String severity;
    Map<String, Object> labels;
    String conditionType;
    Map<String, Object> conditionSummary;
    int evaluationWindowSeconds;
    int forDurationSeconds;
    String dedupeKeyTemplate;
    String routeId;
    String runbookUrl;
    String status;
    final String createdBy;
    String updatedBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    AlertRule(String ruleId, String displayName, String sourceService, String sourceType, String severity, Map<String, Object> labels,
              String conditionType, Map<String, Object> conditionSummary, int evaluationWindowSeconds, int forDurationSeconds,
              String dedupeKeyTemplate, String routeId, String runbookUrl, String status, String createdBy) {
        this.ruleId = ruleId;
        this.displayName = displayName;
        this.sourceService = sourceService;
        this.sourceType = sourceType;
        this.severity = severity;
        this.labels = new LinkedHashMap<>(labels);
        this.conditionType = conditionType;
        this.conditionSummary = new LinkedHashMap<>(conditionSummary);
        this.evaluationWindowSeconds = evaluationWindowSeconds;
        this.forDurationSeconds = forDurationSeconds;
        this.dedupeKeyTemplate = dedupeKeyTemplate;
        this.routeId = routeId;
        this.runbookUrl = runbookUrl;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    static AlertRule from(String ruleId, Map<String, Object> body, String status, String actor) {
        return new AlertRule(ruleId, AlertingText.text(body.get("displayName")), AlertingText.text(body.get("sourceService")),
                AlertingText.text(body.get("sourceType")), AlertingText.text(body.get("severity")), AlertingText.objectMap(body.get("labels")),
                AlertingText.text(body.get("conditionType")), AlertingText.objectMap(body.get("conditionSummary")),
                AlertingText.intValue(body.get("evaluationWindowSeconds"), 300), AlertingText.intValue(body.get("forDurationSeconds"), 0),
                AlertingText.textOr(body.get("dedupeKeyTemplate"), "{{sourceService}}:{{sourceRef}}"), AlertingText.text(body.get("routeId")),
                AlertingText.text(body.get("runbookUrl")), status, actor);
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("displayName")) displayName = AlertingText.text(body.get("displayName"));
        if (body.containsKey("sourceService")) sourceService = AlertingText.text(body.get("sourceService"));
        if (body.containsKey("sourceType")) sourceType = AlertingText.text(body.get("sourceType"));
        if (body.containsKey("severity")) severity = AlertingText.text(body.get("severity"));
        if (body.containsKey("labels")) labels = AlertingText.objectMap(body.get("labels"));
        if (body.containsKey("conditionType")) conditionType = AlertingText.text(body.get("conditionType"));
        if (body.containsKey("conditionSummary")) conditionSummary = AlertingText.objectMap(body.get("conditionSummary"));
        if (body.containsKey("evaluationWindowSeconds")) evaluationWindowSeconds = AlertingText.intValue(body.get("evaluationWindowSeconds"), evaluationWindowSeconds);
        if (body.containsKey("forDurationSeconds")) forDurationSeconds = AlertingText.intValue(body.get("forDurationSeconds"), forDurationSeconds);
        if (body.containsKey("dedupeKeyTemplate")) dedupeKeyTemplate = AlertingText.text(body.get("dedupeKeyTemplate"));
        if (body.containsKey("routeId")) routeId = AlertingText.text(body.get("routeId"));
        if (body.containsKey("runbookUrl")) runbookUrl = AlertingText.text(body.get("runbookUrl"));
        updatedBy = actor;
        updatedAt = Instant.now().toString();
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("ruleId", ruleId, "displayName", displayName, "sourceService", sourceService,
                "sourceType", sourceType, "severity", severity, "labels", labels, "conditionType", conditionType,
                "conditionSummary", conditionSummary, "evaluationWindowSeconds", evaluationWindowSeconds,
                "forDurationSeconds", forDurationSeconds, "dedupeKeyTemplate", dedupeKeyTemplate, "routeId", routeId,
                "runbookUrl", runbookUrl, "status", status, "createdBy", createdBy, "updatedBy", updatedBy,
                "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

class AlertEvaluation {
    final String evaluationId;
    final String ruleId;
    final String status;
    final String matchedSourceId;
    final String createdAlertId;
    final boolean dedupeHit;
    final boolean suppressed;
    final String dependencyStatus = "AVAILABLE";
    final Map<String, Object> resultSummary;
    final String failureReason = null;
    final String evaluatedBy;
    final String evaluatedAt = Instant.now().toString();

    AlertEvaluation(String evaluationId, String ruleId, String status, String matchedSourceId, String createdAlertId, boolean dedupeHit,
                    boolean suppressed, String evaluatedBy) {
        this.evaluationId = evaluationId;
        this.ruleId = ruleId;
        this.status = status;
        this.matchedSourceId = matchedSourceId;
        this.createdAlertId = createdAlertId;
        this.dedupeHit = dedupeHit;
        this.suppressed = suppressed;
        this.evaluatedBy = evaluatedBy;
        this.resultSummary = Map.of("matched", true, "dedupeHit", dedupeHit, "suppressed", suppressed);
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("evaluationId", evaluationId, "ruleId", ruleId, "status", status,
                "matchedSourceId", matchedSourceId, "createdAlertId", createdAlertId, "dedupeHit", dedupeHit,
                "suppressed", suppressed, "dependencyStatus", dependencyStatus, "resultSummary", resultSummary,
                "failureReason", failureReason, "evaluatedBy", evaluatedBy, "evaluatedAt", evaluatedAt);
    }
}

class AlertInstance {
    final String alertId;
    final String ruleId;
    final String sourceService;
    final Map<String, Object> sourceRef;
    final String severity;
    String status;
    final Map<String, Object> labels;
    final String fingerprint;
    final String groupKey;
    final String firstFiredAt = Instant.now().toString();
    String lastFiredAt = firstFiredAt;
    String acknowledgedBy;
    String acknowledgedAt;
    String closedBy;
    String closedAt;
    final String summary;
    final String runbookUrl;
    Map<String, Object> notificationSummary = Map.of("status", "PENDING");
    Map<String, Object> suppressionSummary = Map.of("suppressed", false);
    String resolutionSummary;

    AlertInstance(String alertId, AlertRule rule, Map<String, Object> snapshot, String fingerprint, String sourceRefValue, boolean suppressed) {
        this.alertId = alertId;
        this.ruleId = rule.ruleId;
        this.sourceService = rule.sourceService;
        this.sourceRef = Map.of("sourceRef", sourceRefValue, "sanitized", true);
        this.severity = rule.severity;
        this.status = suppressed ? "SUPPRESSED" : "FIRING";
        this.labels = new LinkedHashMap<>(rule.labels);
        this.labels.putIfAbsent("node", sourceRefValue);
        this.fingerprint = fingerprint;
        this.groupKey = rule.sourceService + ":" + sourceRefValue;
        this.summary = AlertingText.textOr(snapshot.get("summary"), rule.displayName);
        this.runbookUrl = rule.runbookUrl;
        if (suppressed) {
            this.notificationSummary = Map.of("status", "SUPPRESSED");
            this.suppressionSummary = Map.of("suppressed", true, "reason", "MATCHED_SILENCE");
        }
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("alertId", alertId, "ruleId", ruleId, "sourceService", sourceService,
                "sourceRef", sourceRef, "severity", severity, "status", status, "labels", labels,
                "fingerprint", fingerprint, "groupKey", groupKey, "firstFiredAt", firstFiredAt,
                "lastFiredAt", lastFiredAt, "acknowledgedBy", acknowledgedBy, "acknowledgedAt", acknowledgedAt,
                "closedBy", closedBy, "closedAt", closedAt, "summary", summary, "runbookUrl", runbookUrl,
                "notificationSummary", notificationSummary, "suppressionSummary", suppressionSummary,
                "resolutionSummary", resolutionSummary);
    }
}

class AlertSilence {
    final String silenceId;
    final Map<String, Object> matchers;
    final String startsAt;
    final String endsAt;
    final String reason;
    String status = "ACTIVE";
    final String createdBy;
    String cancelledBy;
    final String createdAt = Instant.now().toString();
    String cancelledAt;

    AlertSilence(String silenceId, Map<String, Object> matchers, String startsAt, String endsAt, String reason, String createdBy) {
        this.silenceId = silenceId;
        this.matchers = new LinkedHashMap<>(matchers);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    static AlertSilence from(String silenceId, Map<String, Object> body, String actor) {
        return new AlertSilence(silenceId, AlertingText.objectMap(body.get("matchers")), AlertingText.text(body.get("startsAt")),
                AlertingText.text(body.get("endsAt")), AlertingText.text(body.get("reason")), actor);
    }

    void refreshStatus() {
        if ("ACTIVE".equals(status) && Instant.now().isAfter(Instant.parse(endsAt))) {
            status = "EXPIRED";
        }
    }

    Map<String, Object> view() {
        refreshStatus();
        return AlertingMaps.linked("silenceId", silenceId, "matchers", matchers, "startsAt", startsAt, "endsAt", endsAt,
                "reason", reason, "status", status, "createdBy", createdBy, "cancelledBy", cancelledBy,
                "createdAt", createdAt, "cancelledAt", cancelledAt);
    }
}

class AlertRoute {
    final String routeId;
    String displayName;
    Map<String, Object> matchers;
    List<String> groupBy;
    int groupWaitSeconds;
    int groupIntervalSeconds;
    int repeatIntervalSeconds;
    Map<String, Object> notificationTemplateRef;
    Map<String, Object> receiverSummary;
    String status;
    final String createdBy;
    String updatedBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    AlertRoute(String routeId, String displayName, Map<String, Object> matchers, List<String> groupBy, int groupWaitSeconds,
               int groupIntervalSeconds, int repeatIntervalSeconds, Map<String, Object> notificationTemplateRef,
               Map<String, Object> receiverSummary, String status, String createdBy) {
        this.routeId = routeId;
        this.displayName = displayName;
        this.matchers = new LinkedHashMap<>(matchers);
        this.groupBy = groupBy;
        this.groupWaitSeconds = groupWaitSeconds;
        this.groupIntervalSeconds = groupIntervalSeconds;
        this.repeatIntervalSeconds = repeatIntervalSeconds;
        this.notificationTemplateRef = new LinkedHashMap<>(notificationTemplateRef);
        this.receiverSummary = new LinkedHashMap<>(receiverSummary);
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    static AlertRoute from(String routeId, Map<String, Object> body, String actor) {
        return new AlertRoute(routeId, AlertingText.text(body.get("displayName")), AlertingText.objectMap(body.get("matchers")),
                AlertingText.stringList(body.get("groupBy")), AlertingText.intValue(body.get("groupWaitSeconds"), 0),
                AlertingText.intValue(body.get("groupIntervalSeconds"), 60), AlertingText.intValue(body.get("repeatIntervalSeconds"), 300),
                AlertingText.objectMap(body.get("notificationTemplateRef")), AlertingText.objectMap(body.get("receiverSummary")),
                AlertingText.bool(body.get("enabled")) ? "ENABLED" : "DISABLED", actor);
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("displayName")) displayName = AlertingText.text(body.get("displayName"));
        if (body.containsKey("matchers")) matchers = AlertingText.objectMap(body.get("matchers"));
        if (body.containsKey("groupBy")) groupBy = AlertingText.stringList(body.get("groupBy"));
        if (body.containsKey("groupWaitSeconds")) groupWaitSeconds = AlertingText.intValue(body.get("groupWaitSeconds"), groupWaitSeconds);
        if (body.containsKey("groupIntervalSeconds")) groupIntervalSeconds = AlertingText.intValue(body.get("groupIntervalSeconds"), groupIntervalSeconds);
        if (body.containsKey("repeatIntervalSeconds")) repeatIntervalSeconds = AlertingText.intValue(body.get("repeatIntervalSeconds"), repeatIntervalSeconds);
        if (body.containsKey("notificationTemplateRef")) notificationTemplateRef = AlertingText.objectMap(body.get("notificationTemplateRef"));
        if (body.containsKey("receiverSummary")) receiverSummary = AlertingText.objectMap(body.get("receiverSummary"));
        if (body.containsKey("enabled")) status = AlertingText.bool(body.get("enabled")) ? "ENABLED" : "DISABLED";
        updatedBy = actor;
        updatedAt = Instant.now().toString();
    }

    Map<String, Object> summary() {
        return Map.of("routeId", routeId, "displayName", displayName, "status", status);
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("routeId", routeId, "displayName", displayName, "matchers", matchers,
                "groupBy", groupBy, "groupWaitSeconds", groupWaitSeconds, "groupIntervalSeconds", groupIntervalSeconds,
                "repeatIntervalSeconds", repeatIntervalSeconds, "notificationTemplateRef", notificationTemplateRef,
                "receiverSummary", receiverSummary, "status", status, "createdBy", createdBy, "updatedBy", updatedBy,
                "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

class AlertDelivery {
    final String deliveryId;
    final String alertId;
    final String routeId;
    final Map<String, Object> notificationRef;
    final String deliveryMode;
    final String externalModule;
    final String externalDeliveryId;
    final String externalDeliveryStatus;
    final String externalAttemptStatus;
    final boolean realExternalSend;
    final String status;
    final int attempts;
    final String lastAttemptAt;
    final String failureCode;
    final String failureSummary;
    final String nextRetryAt;
    final String createdAt = Instant.now().toString();

    AlertDelivery(String deliveryId, String alertId, String routeId, String status, int attempts, String failureCode, String failureSummary) {
        this(deliveryId, alertId, routeId, Map.of("mode", "TEST_STUB", "channel", "IN_APP"), "TEST_STUB",
                null, null, null, null, false, status, attempts, failureCode, failureSummary, null);
    }

    AlertDelivery(String deliveryId, String alertId, String routeId, Map<String, Object> notificationRef, String deliveryMode,
                  String externalModule, String externalDeliveryId, String externalDeliveryStatus, String externalAttemptStatus,
                  boolean realExternalSend, String status, int attempts, String failureCode, String failureSummary, String nextRetryAt) {
        this.deliveryId = deliveryId;
        this.alertId = alertId;
        this.routeId = routeId;
        this.notificationRef = new LinkedHashMap<>(notificationRef);
        this.deliveryMode = deliveryMode;
        this.externalModule = externalModule;
        this.externalDeliveryId = externalDeliveryId;
        this.externalDeliveryStatus = externalDeliveryStatus;
        this.externalAttemptStatus = externalAttemptStatus;
        this.realExternalSend = realExternalSend;
        this.status = status;
        this.attempts = attempts;
        this.lastAttemptAt = createdAt;
        this.failureCode = failureCode;
        this.failureSummary = failureSummary;
        this.nextRetryAt = nextRetryAt;
    }

    static AlertDelivery sent(String deliveryId, String alertId, String routeId) {
        return new AlertDelivery(deliveryId, alertId, routeId, "SENT", 1, null, null);
    }

    static AlertDelivery suppressed(String deliveryId, String alertId, String routeId) {
        return new AlertDelivery(deliveryId, alertId, routeId, "SUPPRESSED", 0, null, "MATCHED_SILENCE");
    }

    static AlertDelivery fromCpn(String deliveryId, String alertId, String routeId, AlertingExternalDeliveryAdapter.Result result) {
        Map<String, Object> externalDelivery = AlertingText.objectMap(result.delivery());
        Map<String, Object> externalAttempt = AlertingText.objectMap(result.attempt());
        String externalStatus = AlertingText.text(externalDelivery.get("status"));
        String attemptStatus = AlertingText.text(externalAttempt.get("status"));
        String status = alertingStatus(externalStatus, attemptStatus);
        Map<String, Object> ref = AlertingMaps.linked(
                "mode", "SIMULATED_EXTERNAL",
                "externalModule", "cross-platform-notification",
                "providerId", externalDelivery.get("providerId"),
                "channel", externalDelivery.get("channel"),
                "templateMappingId", externalDelivery.get("templateMappingId"),
                "receiverSummary", externalDelivery.get("receiverSummary"));
        return new AlertDelivery(deliveryId, alertId, routeId, ref, "SIMULATED_EXTERNAL",
                "cross-platform-notification", AlertingText.text(externalDelivery.get("deliveryId")),
                externalStatus, attemptStatus, false, status,
                AlertingText.intValue(externalDelivery.get("attempts"), 1),
                blankToNull(AlertingText.text(externalDelivery.get("failureCode"))),
                blankToNull(AlertingText.text(externalDelivery.get("failureSummary"))),
                blankToNull(AlertingText.text(externalDelivery.get("nextRetryAt"))));
    }

    static AlertDelivery failedExternal(String deliveryId, String alertId, String routeId, AlertingExternalDeliveryAdapter.Result result) {
        Map<String, Object> ref = AlertingMaps.linked("mode", "SIMULATED_EXTERNAL", "externalModule", "cross-platform-notification");
        return new AlertDelivery(deliveryId, alertId, routeId, ref, "SIMULATED_EXTERNAL",
                "cross-platform-notification", null, "ADAPTER_FAILED", "ADAPTER_FAILED", false,
                "FAILED", 0, "CPN_" + result.code(), result.message(), null);
    }

    Map<String, Object> summary() {
        return AlertingMaps.linked("deliveryId", deliveryId, "status", status, "attempts", attempts,
                "deliveryMode", deliveryMode, "externalModule", externalModule, "externalDeliveryId", externalDeliveryId,
                "externalDeliveryStatus", externalDeliveryStatus, "externalAttemptStatus", externalAttemptStatus,
                "realExternalSend", realExternalSend);
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("deliveryId", deliveryId, "alertId", alertId, "routeId", routeId,
                "notificationRef", notificationRef, "deliveryMode", deliveryMode, "externalModule", externalModule,
                "externalDeliveryId", externalDeliveryId, "externalDeliveryStatus", externalDeliveryStatus,
                "externalAttemptStatus", externalAttemptStatus, "realExternalSend", realExternalSend,
                "status", status, "attempts", attempts,
                "lastAttemptAt", lastAttemptAt, "failureCode", failureCode, "failureSummary", failureSummary,
                "nextRetryAt", nextRetryAt, "createdAt", createdAt);
    }

    private static String alertingStatus(String externalStatus, String attemptStatus) {
        if ("SIMULATED_SENT".equals(externalStatus) && "SIMULATED_SUCCESS".equals(attemptStatus)) {
            return "SENT";
        }
        if ("RETRY_SCHEDULED".equals(externalStatus) || "RATE_LIMITED".equals(attemptStatus)) {
            return "RETRYING";
        }
        return "FAILED";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

class AlertAudit {
    final String id;
    final String action;
    final String targetType;
    final String targetId;
    final String actorUserId;
    final String actorRole;
    final List<String> actorPermissions;
    final String riskLevel;
    final String result;
    final String failureReason;
    final String beforeState;
    final String afterState;
    final String requestId;
    final String createdAt = Instant.now().toString();
    final String ruleId;
    final String alertId;
    final String silenceId;
    final String routeId;
    final String deliveryId;
    final String reason;
    final Map<String, Object> paramsSummary;

    AlertAudit(String id, String action, String targetType, String targetId, Actor actor, HttpServletRequest request,
               Map<String, Object> body, String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorUserId = actor.userId;
        this.actorRole = actor.role;
        this.actorPermissions = actor.permissions;
        this.riskLevel = riskLevel;
        this.result = result;
        this.failureReason = failureReason;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.requestId = String.valueOf(request.getAttribute("requestId"));
        this.ruleId = "RULE".equals(targetType) ? targetId : null;
        this.alertId = "ALERT".equals(targetType) ? targetId : null;
        this.silenceId = "SILENCE".equals(targetType) ? targetId : null;
        this.routeId = "ROUTE".equals(targetType) ? targetId : null;
        this.deliveryId = "DELIVERY".equals(targetType) ? targetId : null;
        this.reason = AlertingText.text(body == null ? null : body.get("reason"));
        this.paramsSummary = summarize(body);
    }

    Map<String, Object> view() {
        return AlertingMaps.linked("id", id, "requestId", requestId, "actorUserId", actorUserId, "actorRole", actorRole,
                "actorPermissions", actorPermissions, "sourceIp", null, "targetType", targetType, "targetId", targetId,
                "action", action, "riskLevel", riskLevel, "reason", reason, "paramsSummary", paramsSummary,
                "beforeState", beforeState, "afterState", afterState, "result", result, "failureReason", failureReason,
                "ruleId", ruleId, "alertId", alertId, "silenceId", silenceId, "routeId", routeId, "deliveryId", deliveryId,
                "dependencyStatus", "AVAILABLE", "createdAt", createdAt);
    }

    private static Map<String, Object> summarize(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return Map.of("sanitized", true, "fieldNames", List.of(), "hasIdempotencyKey", false);
        }
        return AlertingMaps.linked("sanitized", true, "fieldNames", new ArrayList<>(new TreeMap<>(body).keySet()),
                "hasIdempotencyKey", body.containsKey("idempotencyKey"));
    }
}

@Service
class AlertingAuth {
    private final AlertingProperties properties;

    AlertingAuth(AlertingProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Auth-Mode");
            if ("unavailable".equals(mode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46920, "auth unavailable");
            if ("timeout".equals(mode)) throw new AlertingException(HttpStatus.GATEWAY_TIMEOUT, 46921, "auth timeout");
            if ("bad-schema".equals(mode)) throw new AlertingException(HttpStatus.BAD_GATEWAY, 46922, "auth bad schema");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new AlertingException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new AlertingException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        return switch (header.substring("Bearer ".length())) {
            case "auth-unavailable-token" -> throw new AlertingException(HttpStatus.BAD_GATEWAY, 46920, "auth unavailable");
            case "auth-timeout-token" -> throw new AlertingException(HttpStatus.GATEWAY_TIMEOUT, 46921, "auth timeout");
            case "auth-bad-token" -> throw new AlertingException(HttpStatus.BAD_GATEWAY, 46922, "auth bad schema");
            case "alert-viewer-token" -> new Actor("alert-viewer-user", "Alert Viewer", "HELPER", List.of("NODE_READ"));
            case "alert-no-cap-token" -> new Actor("alert-no-cap-user", "No Cap", "ADMIN", List.of());
            case "alert-admin-token" -> new Actor("alert-admin-user", "Alert Admin", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "owner-token" -> new Actor("owner-user", "Owner", "OWNER", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "Plain User", "USER", List.of());
            default -> throw new AlertingException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireAnyCapability(HttpServletRequest request, String... capabilities) {
        return requireAnyCapability(current(request), capabilities);
    }

    Actor requireAnyCapability(Actor actor, String... capabilities) {
        if ("USER".equals(actor.role)) {
            throw new AlertingException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (List.of(capabilities).stream().noneMatch(actor.permissions::contains)) {
            throw new AlertingException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new AlertingException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
    }
}

class Actor {
    final String userId;
    final String displayName;
    final String role;
    final List<String> permissions;

    Actor(String userId, String displayName, String role, List<String> permissions) {
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.permissions = permissions;
    }
}

record IdempotencyRecord(String fingerprint, HttpStatus status, Object data) {
}

@Component
class AlertingProperties {
    private final boolean enabled;

    AlertingProperties(@Value("${alerting.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class AlertingRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank())
                .orElse("req_" + UUID.randomUUID());
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice(assignableTypes = AlertingController.class)
class AlertingExceptionHandler {
    @ExceptionHandler(AlertingException.class)
    ResponseEntity<Map<String, Object>> api(AlertingException exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.code);
        body.put("message", exception.getMessage());
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> fallback(Exception exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 55500);
        body.put("message", "alerting internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class AlertingException extends RuntimeException {
    final HttpStatus status;
    final int code;

    AlertingException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

class AlertingMaps {
    static Map<String, Object> linked(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}

class AlertingText {
    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    static int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    static boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(text(value));
    }

    static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    static Map<String, Object> objectMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        }
        return result;
    }
}
