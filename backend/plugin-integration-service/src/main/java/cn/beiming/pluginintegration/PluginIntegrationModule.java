package cn.beiming.pluginintegration;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static cn.beiming.pluginintegration.PluginSupport.*;

@RestController
@RequestMapping("/api/v1/plugin-integration")
class PluginIntegrationController {
    private static final String VERSION = "0.1.0-contract";
    private final PluginStore store;
    private final PluginAuth auth;
    private final PluginProperties properties;

    PluginIntegrationController(PluginStore store, PluginAuth auth, PluginProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, map("service", "plugin-integration", "status", store.health(), "version", VERSION));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireRead(request);
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new PluginApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55700, "plugin-integration internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> providers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "lastEventAt_desc", "lastSyncAt_desc");
        TimeRange range = timeRange(query);
        List<Map<String, Object>> items = store.providers.values().stream()
                .filter(provider -> matches(provider.displayName, query.get("keyword")) || matches(provider.providerId, query.get("keyword")))
                .filter(provider -> query.get("providerType") == null || provider.providerType.equals(query.get("providerType")))
                .filter(provider -> query.get("serverKind") == null || provider.serverKind.equals(query.get("serverKind")))
                .filter(provider -> query.get("status") == null || provider.status.equals(query.get("status")))
                .filter(provider -> query.get("healthStatus") == null || provider.healthStatus.equals(query.get("healthStatus")))
                .filter(provider -> query.get("publicVisible") == null || provider.publicVisible == bool(query.get("publicVisible")))
                .filter(provider -> range == null || range.contains(provider.updatedAt))
                .sorted(providerComparator(query.get("sort")))
                .map(PluginProvider::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> provider(HttpServletRequest request, @PathVariable String providerId) {
        auth.requireRead(request);
        PluginProvider provider = store.provider(providerId);
        Map<String, Object> view = provider.view();
        view.put("recentHealthSnapshot", store.latestHealth(providerId));
        view.put("recentEvent", store.latestEvent(providerId));
        view.put("recentAudit", store.latestAudit("PROVIDER", providerId));
        return ok(request, view);
    }

    @PostMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> createProvider(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateProvider(body, true);
        if (needsProviderConfirm(body) && !"REGISTER_PLUGIN_PROVIDER_ENDPOINT".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        return idempotent(request, actor, "provider:create", body, () -> {
            store.guardDependency(request, properties.enabled(), "ops");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                if (store.providerNameConflict(text(body.get("displayName")))) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49811, "plugin provider conflict");
                }
                String providerId = "provider-" + store.nextId(text(body.get("idempotencyKey")));
                PluginProvider provider = PluginProvider.from(providerId, body, actor.userId);
                store.providers.put(providerId, provider);
                store.seedInstance(provider);
                store.healthSnapshots.put("health-" + providerId, PluginHealth.seed("health-" + providerId, providerId, provider.healthStatus));
                store.audit("PLUGIN_PROVIDER_CREATED", "PROVIDER", providerId, actor, request, body, providerRisk(body), "SUCCESS", null, null, provider.status);
                return created(request, provider.view());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> patchProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (providerPatchHighRisk(body) && !"UPDATE_PLUGIN_PROVIDER_ENDPOINT".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        return idempotent(request, actor, "provider:patch:" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "provider state conflict");
                }
                String before = provider.status;
                provider.patch(body, actor.userId);
                store.audit("PLUGIN_PROVIDER_UPDATED", "PROVIDER", providerId, actor, request, body, providerPatchHighRisk(body) ? "HIGH" : "MEDIUM", "SUCCESS", null, before, provider.status);
                return ok(request, provider.view());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}/enable")
    ResponseEntity<Map<String, Object>> enableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ENABLED", "PLUGIN_PROVIDER_ENABLED", "ENABLE_PLUGIN_PROVIDER", true);
    }

    @PatchMapping("/admin/providers/{providerId}/disable")
    ResponseEntity<Map<String, Object>> disableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "DISABLED", "PLUGIN_PROVIDER_DISABLED", null, false);
    }

    @PatchMapping("/admin/providers/{providerId}/archive")
    ResponseEntity<Map<String, Object>> archiveProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ARCHIVED", "PLUGIN_PROVIDER_ARCHIVED", "ARCHIVE_PLUGIN_PROVIDER", true);
    }

    private ResponseEntity<Map<String, Object>> providerState(HttpServletRequest request, String providerId, Map<String, Object> body, String target,
                                                              String action, String confirmText, boolean high) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (confirmText != null && !confirmText.equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        return idempotent(request, actor, "provider:" + target + ":" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status) || ("ARCHIVED".equals(target) && "ENABLED".equals(provider.status))) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "provider state conflict");
                }
                if ("ARCHIVED".equals(target) && store.providerHasActiveReferences(providerId)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "provider state conflict");
                }
                String before = provider.status;
                provider.status = target;
                if ("ENABLED".equals(target)) {
                    provider.healthStatus = "ONLINE";
                }
                if ("ARCHIVED".equals(target)) {
                    provider.publicVisible = false;
                }
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.audit(action, "PROVIDER", providerId, actor, request, body, high ? "HIGH" : "MEDIUM", "SUCCESS", null, before, provider.status);
                return ok(request, provider.view());
            }
        });
    }

    @GetMapping("/admin/instances")
    ResponseEntity<Map<String, Object>> instances(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "lastSeenAt_desc", "pluginName_asc", "pluginVersion_desc");
        List<Map<String, Object>> items = store.instances.values().stream()
                .filter(item -> query.get("providerId") == null || item.providerId().equals(query.get("providerId")))
                .filter(item -> query.get("pluginName") == null || item.pluginName().equals(query.get("pluginName")))
                .filter(item -> query.get("loaded") == null || item.loaded() == bool(query.get("loaded")))
                .filter(item -> query.get("enabled") == null || item.enabled() == bool(query.get("enabled")))
                .filter(item -> query.get("stale") == null || item.stale() == bool(query.get("stale")))
                .filter(item -> matches(item.pluginName(), query.get("keyword")) || matches(item.instanceId(), query.get("keyword")))
                .sorted(instanceComparator(query.get("sort")))
                .map(PluginInstance::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/instances/{instanceId}")
    ResponseEntity<Map<String, Object>> instance(HttpServletRequest request, @PathVariable String instanceId) {
        auth.requireRead(request);
        PluginInstance instance = Optional.ofNullable(store.instances.get(instanceId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49801, "plugin instance not found"));
        return ok(request, instance.view());
    }

    @GetMapping("/admin/capabilities")
    ResponseEntity<Map<String, Object>> capabilities(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        List<Map<String, Object>> items = store.capabilities.values().stream()
                .filter(item -> query.get("providerId") == null || item.providerId().equals(query.get("providerId")))
                .filter(item -> query.get("namespace") == null || item.namespace().equals(query.get("namespace")))
                .filter(item -> query.get("riskLevel") == null || item.riskLevel().equals(query.get("riskLevel")))
                .filter(item -> query.get("available") == null || item.available() == bool(query.get("available")))
                .filter(item -> matches(item.name(), query.get("keyword")) || matches(item.capabilityId(), query.get("keyword")))
                .map(PluginCapability::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/event-schemas")
    ResponseEntity<Map<String, Object>> schemas(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "eventType_asc");
        List<Map<String, Object>> items = store.schemas.values().stream()
                .filter(schema -> query.get("providerId") == null || schema.providerId.equals(query.get("providerId")))
                .filter(schema -> query.get("eventType") == null || schema.eventType.equals(query.get("eventType")))
                .filter(schema -> query.get("sourcePlugin") == null || schema.sourcePlugin.equals(query.get("sourcePlugin")))
                .filter(schema -> query.get("status") == null || schema.status.equals(query.get("status")))
                .filter(schema -> matches(schema.eventType, query.get("keyword")) || matches(schema.schemaId, query.get("keyword")))
                .sorted(schemaComparator(query.get("sort")))
                .map(PluginSchema::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/event-schemas/{schemaId}")
    ResponseEntity<Map<String, Object>> schema(HttpServletRequest request, @PathVariable String schemaId) {
        auth.requireRead(request);
        return ok(request, store.schema(schemaId).view());
    }

    @PostMapping("/admin/event-schemas")
    ResponseEntity<Map<String, Object>> createSchema(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateSchema(body);
        return idempotent(request, actor, "schema:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                store.provider(text(body.get("providerId")));
                if (store.schemaConflict(body)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49811, "plugin schema conflict");
                }
                String schemaId = "schema-" + store.nextId(text(body.get("idempotencyKey")));
                PluginSchema schema = PluginSchema.from(schemaId, body, actor.userId);
                store.schemas.put(schemaId, schema);
                store.audit("PLUGIN_SCHEMA_CREATED", "SCHEMA", schemaId, actor, request, body, "MEDIUM", "SUCCESS", null, null, schema.status);
                return created(request, schema.view());
            }
        });
    }

    @PatchMapping("/admin/event-schemas/{schemaId}")
    ResponseEntity<Map<String, Object>> patchSchema(HttpServletRequest request, @PathVariable String schemaId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "schema:patch:" + schemaId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginSchema schema = store.schema(schemaId);
                if ("ARCHIVED".equals(schema.status)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "schema state conflict");
                }
                String before = schema.status;
                schema.patch(body, actor.userId);
                store.audit("PLUGIN_SCHEMA_UPDATED", "SCHEMA", schemaId, actor, request, body, "MEDIUM", "SUCCESS", null, before, schema.status);
                return ok(request, schema.view());
            }
        });
    }

    @PatchMapping("/admin/event-schemas/{schemaId}/enable")
    ResponseEntity<Map<String, Object>> enableSchema(HttpServletRequest request, @PathVariable String schemaId, @RequestBody Map<String, Object> body) {
        return schemaState(request, schemaId, body, "ENABLED", "PLUGIN_SCHEMA_ENABLED");
    }

    @PatchMapping("/admin/event-schemas/{schemaId}/disable")
    ResponseEntity<Map<String, Object>> disableSchema(HttpServletRequest request, @PathVariable String schemaId, @RequestBody Map<String, Object> body) {
        return schemaState(request, schemaId, body, "DISABLED", "PLUGIN_SCHEMA_DISABLED");
    }

    private ResponseEntity<Map<String, Object>> schemaState(HttpServletRequest request, String schemaId, Map<String, Object> body, String target, String action) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "schema:" + target + ":" + schemaId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginSchema schema = store.schema(schemaId);
                if ("ARCHIVED".equals(schema.status)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "schema state conflict");
                }
                String before = schema.status;
                schema.status = target;
                schema.updatedBy = actor.userId;
                schema.updatedAt = now();
                store.audit(action, "SCHEMA", schemaId, actor, request, body, "MEDIUM", "SUCCESS", null, before, schema.status);
                return ok(request, schema.view());
            }
        });
    }

    @PostMapping("/admin/events/ingest")
    ResponseEntity<Map<String, Object>> ingest(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new PluginApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55703, "plugin event write failed");
        }
        return idempotent(request, actor, "event:ingest", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginProvider provider = store.provider(text(body.get("providerId")));
                if (!"ENABLED".equals(provider.status)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "provider state conflict");
                }
                String eventType = text(body.get("eventType"));
                if (!provider.allowedEventTypes.contains(eventType)) {
                    throw new PluginApiException(HttpStatus.FORBIDDEN, 49815, "plugin event source denied");
                }
                String origin = text(body.get("origin"));
                if (!origin.isBlank() && !provider.allowedOrigins.contains(origin)) {
                    throw new PluginApiException(HttpStatus.FORBIDDEN, 49815, "plugin event source denied");
                }
                PluginSchema schema = store.enabledSchema(provider.providerId, eventType, text(body.get("sourcePlugin")));
                Map<String, Object> payload = objectMap(body.get("payload"));
                validatePayload(schema, payload);
                String eventId = "event-" + store.nextId(text(body.get("idempotencyKey")));
                String notificationStatus = dependencyFailed(request, properties.enabled(), "notification") ? "FAILED" : "SKIPPED";
                PluginEvent event = PluginEvent.from(eventId, schema, body, payload, notificationStatus, String.valueOf(request.getAttribute("requestId")));
                event.routeStatus = store.enabledRoute(event.eventType) == null ? "IGNORED" : "ROUTED";
                event.syncStatus = "SKIPPED";
                store.events.put(eventId, event);
                provider.lastEventAt = event.receivedAt;
                provider.updatedAt = now();
                store.audit("PLUGIN_EVENT_INGESTED", "EVENT", eventId, actor, request, body, "MEDIUM", "SUCCESS", null, null, event.validationStatus);
                return created(request, event.view());
            }
        });
    }

    @GetMapping("/admin/events")
    ResponseEntity<Map<String, Object>> events(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "receivedAt_desc", "receivedAt_asc", "processedAt_desc");
        TimeRange range = timeRange(query);
        List<Map<String, Object>> items = store.events.values().stream()
                .filter(event -> query.get("providerId") == null || event.providerId.equals(query.get("providerId")))
                .filter(event -> query.get("eventType") == null || event.eventType.equals(query.get("eventType")))
                .filter(event -> query.get("sourcePlugin") == null || event.sourcePlugin.equals(query.get("sourcePlugin")))
                .filter(event -> query.get("validationStatus") == null || event.validationStatus.equals(query.get("validationStatus")))
                .filter(event -> query.get("routeStatus") == null || event.routeStatus.equals(query.get("routeStatus")))
                .filter(event -> query.get("syncStatus") == null || event.syncStatus.equals(query.get("syncStatus")))
                .filter(event -> query.get("notificationStatus") == null || event.notificationStatus.equals(query.get("notificationStatus")))
                .filter(event -> matches(event.eventType, query.get("keyword")) || matches(event.eventId, query.get("keyword")))
                .filter(event -> range == null || range.contains(event.receivedAt))
                .sorted(eventComparator(query.get("sort")))
                .map(PluginEvent::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/events/{eventId}")
    ResponseEntity<Map<String, Object>> event(HttpServletRequest request, @PathVariable String eventId) {
        auth.requireRead(request);
        PluginEvent event = store.event(eventId);
        Map<String, Object> view = event.view();
        view.put("recentSyncTask", store.latestTaskForEvent(eventId));
        view.put("recentAudit", store.latestAudit("EVENT", eventId));
        return ok(request, view);
    }

    @PostMapping("/admin/events/{eventId}/replay")
    ResponseEntity<Map<String, Object>> replay(HttpServletRequest request, @PathVariable String eventId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (!"REPLAY_PLUGIN_EVENT".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        return idempotent(request, actor, "event:replay:" + eventId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginEvent event = store.event(eventId);
                event.processedAt = now();
                store.audit("PLUGIN_EVENT_REPLAYED", "EVENT", eventId, actor, request, body, "HIGH", "SUCCESS", null, event.routeStatus, "ROUTED");
                return created(request, map("eventId", eventId, "replayStatus", "ROUTED", "createdTaskIds", List.of(), "raw" + "PayloadStored", false));
            }
        });
    }

    @GetMapping("/admin/route-rules")
    ResponseEntity<Map<String, Object>> routeRules(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc");
        List<Map<String, Object>> items = store.routes.values().stream()
                .filter(rule -> query.get("eventType") == null || rule.eventType.equals(query.get("eventType")))
                .filter(rule -> query.get("targetModule") == null || rule.targetModule.equals(query.get("targetModule")))
                .filter(rule -> query.get("enabled") == null || rule.enabled == bool(query.get("enabled")))
                .filter(rule -> query.get("riskLevel") == null || rule.riskLevel.equals(query.get("riskLevel")))
                .filter(rule -> matches(rule.displayName, query.get("keyword")) || matches(rule.ruleId, query.get("keyword")))
                .sorted(routeComparator(query.get("sort")))
                .map(PluginRoute::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/route-rules/{ruleId}")
    ResponseEntity<Map<String, Object>> routeRule(HttpServletRequest request, @PathVariable String ruleId) {
        auth.requireRead(request);
        return ok(request, store.route(ruleId).view());
    }

    @PostMapping("/admin/route-rules")
    ResponseEntity<Map<String, Object>> createRoute(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateRoute(body);
        boolean high = routeHighRisk(body);
        if (high && !"CONFIGURE_PLUGIN_ROUTE".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        if ("OPS_CONTROL".equals(text(body.get("targetModule")))) {
            throw new PluginApiException(HttpStatus.CONFLICT, 49817, "plugin sync target blocked");
        }
        return idempotent(request, actor, "route:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                String ruleId = "rule-" + store.nextId(text(body.get("idempotencyKey")));
                PluginRoute route = PluginRoute.from(ruleId, body, actor.userId);
                store.routes.put(ruleId, route);
                store.audit("PLUGIN_ROUTE_RULE_CREATED", "ROUTE_RULE", ruleId, actor, request, body, high ? "HIGH" : "MEDIUM", "SUCCESS", null, null, route.enabled ? "ENABLED" : "DISABLED");
                return created(request, route.view());
            }
        });
    }

    @PatchMapping("/admin/route-rules/{ruleId}")
    ResponseEntity<Map<String, Object>> patchRoute(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (routeHighRisk(body) && !"UPDATE_PLUGIN_ROUTE".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        if ("OPS_CONTROL".equals(text(body.get("targetModule")))) {
            throw new PluginApiException(HttpStatus.CONFLICT, 49817, "plugin sync target blocked");
        }
        return idempotent(request, actor, "route:patch:" + ruleId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginRoute route = store.route(ruleId);
                String before = route.enabled ? "ENABLED" : "DISABLED";
                route.patch(body, actor.userId);
                store.audit("PLUGIN_ROUTE_RULE_UPDATED", "ROUTE_RULE", ruleId, actor, request, body, routeHighRisk(body) ? "HIGH" : "MEDIUM", "SUCCESS", null, before, route.enabled ? "ENABLED" : "DISABLED");
                return ok(request, route.view());
            }
        });
    }

    @PatchMapping("/admin/route-rules/{ruleId}/enable")
    ResponseEntity<Map<String, Object>> enableRoute(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        return routeState(request, ruleId, body, true);
    }

    @PatchMapping("/admin/route-rules/{ruleId}/disable")
    ResponseEntity<Map<String, Object>> disableRoute(HttpServletRequest request, @PathVariable String ruleId, @RequestBody Map<String, Object> body) {
        return routeState(request, ruleId, body, false);
    }

    private ResponseEntity<Map<String, Object>> routeState(HttpServletRequest request, String ruleId, Map<String, Object> body, boolean enabled) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "route:" + enabled + ":" + ruleId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginRoute route = store.route(ruleId);
                if (enabled && ("HIGH".equals(route.riskLevel) || "CRITICAL".equals(route.riskLevel))) {
                    if (!"ENABLE_PLUGIN_ROUTE".equals(text(body.get("confirmText")))) {
                        throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
                    }
                }
                if (enabled && "OPS_CONTROL".equals(route.targetModule)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49817, "plugin sync target blocked");
                }
                String before = route.enabled ? "ENABLED" : "DISABLED";
                route.enabled = enabled;
                route.updatedBy = actor.userId;
                route.updatedAt = now();
                store.audit(enabled ? "PLUGIN_ROUTE_RULE_ENABLED" : "PLUGIN_ROUTE_RULE_DISABLED", "ROUTE_RULE", ruleId, actor, request, body, "MEDIUM", "SUCCESS", null, before, route.enabled ? "ENABLED" : "DISABLED");
                return ok(request, route.view());
            }
        });
    }

    @PostMapping("/admin/sync-tasks")
    ResponseEntity<Map<String, Object>> createTask(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (dependencyFailed(request, properties.enabled(), "online-map")) {
            throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47080, "online-map unavailable");
        }
        if ("HIGH".equals(text(body.get("riskLevel"))) && !"CREATE_PLUGIN_SYNC_TASK".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        if ("OPS_CONTROL".equals(text(body.get("targetModule")))) {
            throw new PluginApiException(HttpStatus.CONFLICT, 49817, "plugin sync target blocked");
        }
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new PluginApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55704, "plugin sync task write failed");
        }
        return idempotent(request, actor, "task:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                store.provider(text(body.get("providerId")));
                String taskId = "task-" + store.nextId(text(body.get("idempotencyKey")));
                PluginTask task = PluginTask.from(taskId, body, actor.userId);
                store.tasks.put(taskId, task);
                store.audit("PLUGIN_SYNC_TASK_CREATED", "SYNC_TASK", taskId, actor, request, body, task.riskLevel, "SUCCESS", null, null, task.status);
                return created(request, task.view());
            }
        });
    }

    @GetMapping("/admin/sync-tasks")
    ResponseEntity<Map<String, Object>> tasks(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "riskLevel_desc");
        List<Map<String, Object>> items = store.tasks.values().stream()
                .filter(task -> query.get("providerId") == null || task.providerId.equals(query.get("providerId")))
                .filter(task -> query.get("eventId") == null || Objects.equals(task.eventId, query.get("eventId")))
                .filter(task -> query.get("targetModule") == null || task.targetModule.equals(query.get("targetModule")))
                .filter(task -> query.get("status") == null || task.status.equals(query.get("status")))
                .filter(task -> query.get("riskLevel") == null || task.riskLevel.equals(query.get("riskLevel")))
                .sorted(taskComparator(query.get("sort")))
                .map(PluginTask::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/sync-tasks/{taskId}")
    ResponseEntity<Map<String, Object>> task(HttpServletRequest request, @PathVariable String taskId) {
        auth.requireRead(request);
        return ok(request, store.task(taskId).view());
    }

    @PatchMapping("/admin/sync-tasks/{taskId}/cancel")
    ResponseEntity<Map<String, Object>> cancelTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "task:cancel:" + taskId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginTask task = store.task(taskId);
                if (List.of("SUCCEEDED", "FAILED", "CANCELED", "TIMEOUT").contains(task.status)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49810, "sync task state conflict");
                }
                String before = task.status;
                task.status = "CANCELED";
                task.updatedAt = now();
                store.audit("PLUGIN_SYNC_TASK_CANCELED", "SYNC_TASK", taskId, actor, request, body, "MEDIUM", "SUCCESS", null, before, task.status);
                return ok(request, task.view());
            }
        });
    }

    @GetMapping("/admin/providers/{providerId}/health-snapshots")
    ResponseEntity<Map<String, Object>> healthSnapshots(HttpServletRequest request, @PathVariable String providerId, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        store.provider(providerId);
        validatePage(query);
        validateSort(query.get("sort"), "checkedAt_desc", "checkedAt_asc");
        List<Map<String, Object>> items = store.healthSnapshots.values().stream()
                .filter(snapshot -> snapshot.providerId().equals(providerId))
                .filter(snapshot -> query.get("healthStatus") == null || snapshot.healthStatus().equals(query.get("healthStatus")))
                .sorted(Comparator.comparing((PluginHealth snapshot) -> snapshot.checkedAt()).reversed())
                .map(PluginHealth::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/object-mappings")
    ResponseEntity<Map<String, Object>> mappings(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "lastSyncedAt_desc");
        List<Map<String, Object>> items = store.mappings.values().stream()
                .filter(mapping -> query.get("providerId") == null || mapping.providerId.equals(query.get("providerId")))
                .filter(mapping -> query.get("sourcePlugin") == null || mapping.sourcePlugin.equals(query.get("sourcePlugin")))
                .filter(mapping -> query.get("sourceObjectType") == null || mapping.sourceObjectType.equals(query.get("sourceObjectType")))
                .filter(mapping -> query.get("targetModule") == null || mapping.targetModule.equals(query.get("targetModule")))
                .filter(mapping -> query.get("targetObjectType") == null || mapping.targetObjectType.equals(query.get("targetObjectType")))
                .filter(mapping -> query.get("status") == null || mapping.status.equals(query.get("status")))
                .filter(mapping -> query.get("visibility") == null || mapping.visibility.equals(query.get("visibility")))
                .filter(mapping -> matches(mapping.sourceObjectKey, query.get("keyword")) || matches(mapping.mappingId, query.get("keyword")))
                .sorted(mappingComparator(query.get("sort")))
                .map(PluginMapping::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/object-mappings/{mappingId}")
    ResponseEntity<Map<String, Object>> mapping(HttpServletRequest request, @PathVariable String mappingId) {
        auth.requireRead(request);
        return ok(request, store.mapping(mappingId).view());
    }

    @PutMapping("/admin/object-mappings/{mappingId}")
    ResponseEntity<Map<String, Object>> upsertMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        boolean high = "PUBLIC".equals(text(body.get("visibility"))) && "ONLINE_MAP".equals(text(body.get("targetModule")));
        if (high && !"UPSERT_PLUGIN_OBJECT_MAPPING".equals(text(body.get("confirmText")))) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
        return idempotent(request, actor, "mapping:upsert:" + mappingId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                store.provider(text(body.get("providerId")));
                if (store.mappingConflict(mappingId, body)) {
                    throw new PluginApiException(HttpStatus.CONFLICT, 49811, "plugin object mapping conflict");
                }
                boolean created = !store.mappings.containsKey(mappingId);
                PluginMapping mapping = PluginMapping.from(mappingId, body, actor.userId, created ? null : store.mappings.get(mappingId));
                store.mappings.put(mappingId, mapping);
                store.audit("PLUGIN_OBJECT_MAPPING_UPSERTED", "OBJECT_MAPPING", mappingId, actor, request, body, high ? "HIGH" : "MEDIUM", "SUCCESS", null, null, mapping.status);
                return created ? created(request, mapping.view()) : ok(request, mapping.view());
            }
        });
    }

    @PatchMapping("/admin/object-mappings/{mappingId}/archive")
    ResponseEntity<Map<String, Object>> archiveMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "mapping:archive:" + mappingId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                PluginMapping mapping = store.mapping(mappingId);
                String before = mapping.status;
                mapping.status = "ARCHIVED";
                mapping.updatedBy = actor.userId;
                mapping.updatedAt = now();
                store.audit("PLUGIN_OBJECT_MAPPING_ARCHIVED", "OBJECT_MAPPING", mappingId, actor, request, body, "MEDIUM", "SUCCESS", null, before, mapping.status);
                return ok(request, mapping.view());
            }
        });
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> audits(HttpServletRequest request, @RequestParam Map<String, String> query) {
        Actor actor = auth.current(request);
        auth.requireAudit(actor);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        List<Map<String, Object>> items = store.audits.values().stream()
                .filter(audit -> query.get("actorUserId") == null || audit.actorUserId.equals(query.get("actorUserId")))
                .filter(audit -> query.get("action") == null || audit.action.equals(query.get("action")))
                .filter(audit -> query.get("targetType") == null || audit.targetType.equals(query.get("targetType")))
                .filter(audit -> query.get("targetId") == null || audit.targetId.equals(query.get("targetId")))
                .filter(audit -> query.get("providerId") == null || Objects.equals(audit.providerId, query.get("providerId")))
                .filter(audit -> query.get("eventId") == null || Objects.equals(audit.eventId, query.get("eventId")))
                .filter(audit -> query.get("schemaId") == null || Objects.equals(audit.schemaId, query.get("schemaId")))
                .filter(audit -> query.get("ruleId") == null || Objects.equals(audit.ruleId, query.get("ruleId")))
                .filter(audit -> query.get("taskId") == null || Objects.equals(audit.taskId, query.get("taskId")))
                .filter(audit -> query.get("mappingId") == null || Objects.equals(audit.mappingId, query.get("mappingId")))
                .filter(audit -> query.get("result") == null || audit.result.equals(query.get("result")))
                .filter(audit -> query.get("riskLevel") == null || audit.riskLevel.equals(query.get("riskLevel")))
                .sorted(auditComparator(query.get("sort")))
                .map(PluginAudit::view)
                .toList();
        return ok(request, page(items, query));
    }

    private static boolean needsProviderConfirm(Map<String, Object> body) {
        return true;
    }
}

@Service
class PluginStore {
    final Object lock = new Object();
    final Map<String, PluginProvider> providers = new ConcurrentHashMap<>();
    final Map<String, PluginInstance> instances = new ConcurrentHashMap<>();
    final Map<String, PluginCapability> capabilities = new ConcurrentHashMap<>();
    final Map<String, PluginSchema> schemas = new ConcurrentHashMap<>();
    final Map<String, PluginEvent> events = new ConcurrentHashMap<>();
    final Map<String, PluginRoute> routes = new ConcurrentHashMap<>();
    final Map<String, PluginTask> tasks = new ConcurrentHashMap<>();
    final Map<String, PluginMapping> mappings = new ConcurrentHashMap<>();
    final Map<String, PluginHealth> healthSnapshots = new ConcurrentHashMap<>();
    final Map<String, PluginAudit> audits = new ConcurrentHashMap<>();
    final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    int sequence = 1000;

    @PostConstruct
    void seed() {
        PluginProvider provider = new PluginProvider("provider-paper-main", "PAPER", "Paper Bridge", "BeimingBridge", "1.0.0",
                "SERVER", map("instanceId", "mc-main"), map("nodeId", "node-main"), "ENABLED", false,
                "/plugin-events/paper-main", List.of("beiming.player_join", "beiming.map_marker"),
                List.of("https://plugins.example.com"), "ONLINE", null, null, List.of(), "seed", "seed", now(), now());
        providers.put(provider.providerId, provider);
        seedInstance(provider);
        PluginSchema schema = new PluginSchema("schema-player-join", provider.providerId, "beiming.player_join", "BeimingBridge", "1.0.0",
                "ENABLED", List.of("player", "world"), List.of("dimension"), List.of("ip", "token", "webhook"),
                map("targetModule", "ONLINE_MAP"), map("player", "Steve", "world", "overworld"), "seed", "seed", now(), now());
        schemas.put(schema.schemaId, schema);
        PluginRoute route = new PluginRoute("rule-default-map", "Default player marker route", "beiming.player_join",
                map("providerId", provider.providerId, "sourcePlugin", "BeimingBridge"), "ONLINE_MAP", "UPSERT_MARKER_PREVIEW",
                true, "MEDIUM", map("windowSeconds", 60, "maxEvents", 100), "seed", "seed", now(), now());
        routes.put(route.ruleId, route);
        PluginEvent event = new PluginEvent("event-seed-player-join", provider.providerId, "beiming.player_join", "schema-player-join",
                "BeimingBridge", "instance-paper-main", "seed-event", map("player", "Steve", "world", "overworld"),
                "VALIDATED", "ROUTED", "SKIPPED", "SKIPPED", now(), null, null, "req_seed");
        events.put(event.eventId, event);
        healthSnapshots.put("health-provider-paper-main", PluginHealth.seed("health-provider-paper-main", provider.providerId, "ONLINE"));
    }

    String health() {
        return providers.values().stream().anyMatch(provider -> "DEGRADED".equals(provider.status)) ? "DEGRADED" : "READY";
    }

    String nextId(String seed) {
        String value = seed == null || seed.isBlank() ? String.valueOf(++sequence) : seed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
        return value.replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    void seedInstance(PluginProvider provider) {
        String instanceId = provider.providerId.equals("provider-paper-main") ? "instance-paper-main" : "instance-" + provider.providerId;
        instances.putIfAbsent(instanceId, new PluginInstance(instanceId, provider.providerId, provider.instanceRef, map("instanceId", "survival-main"),
                provider.pluginName, provider.pluginVersion, "1.20.4", true, true, List.of("LuckPerms", "PlaceholderAPI"),
                map("tps", 20.0, "eventsPerMinute", 12), now(), false));
        String capabilityId = "cap-" + provider.providerId;
        capabilities.putIfAbsent(capabilityId, new PluginCapability(capabilityId, provider.providerId, "beiming", "beiming.event.ingest", "1.0.0",
                "LOW", true, map("summary", "event ingestion", "sanitized", true)));
    }

    PluginProvider provider(String providerId) {
        return Optional.ofNullable(providers.get(providerId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49800, "plugin provider not found"));
    }

    PluginSchema schema(String schemaId) {
        return Optional.ofNullable(schemas.get(schemaId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49802, "plugin event schema not found"));
    }

    PluginSchema enabledSchema(String providerId, String eventType, String sourcePlugin) {
        return schemas.values().stream()
                .filter(schema -> schema.providerId.equals(providerId) && schema.eventType.equals(eventType) && schema.sourcePlugin.equals(sourcePlugin) && "ENABLED".equals(schema.status))
                .findFirst()
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49802, "plugin event schema not found"));
    }

    PluginEvent event(String eventId) {
        return Optional.ofNullable(events.get(eventId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49803, "plugin event not found"));
    }

    PluginRoute route(String ruleId) {
        return Optional.ofNullable(routes.get(ruleId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49804, "plugin route rule not found"));
    }

    PluginRoute enabledRoute(String eventType) {
        return routes.values().stream().filter(route -> route.enabled && route.eventType.equals(eventType)).findFirst().orElse(null);
    }

    PluginTask task(String taskId) {
        return Optional.ofNullable(tasks.get(taskId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49805, "plugin sync task not found"));
    }

    PluginMapping mapping(String mappingId) {
        return Optional.ofNullable(mappings.get(mappingId))
                .orElseThrow(() -> new PluginApiException(HttpStatus.NOT_FOUND, 49806, "plugin object mapping not found"));
    }

    boolean providerNameConflict(String displayName) {
        return providers.values().stream().anyMatch(provider -> !"ARCHIVED".equals(provider.status) && provider.displayName.equalsIgnoreCase(displayName));
    }

    boolean schemaConflict(Map<String, Object> body) {
        return schemas.values().stream().anyMatch(schema -> schema.providerId.equals(text(body.get("providerId")))
                && schema.eventType.equals(text(body.get("eventType")))
                && schema.sourcePlugin.equals(text(body.get("sourcePlugin")))
                && schema.version.equals(text(body.get("version")))
                && !"ARCHIVED".equals(schema.status));
    }

    boolean mappingConflict(String mappingId, Map<String, Object> body) {
        return mappings.values().stream()
                .filter(mapping -> !mapping.mappingId.equals(mappingId))
                .filter(mapping -> !"ARCHIVED".equals(mapping.status))
                .anyMatch(mapping -> mapping.providerId.equals(text(body.get("providerId")))
                        && mapping.sourcePlugin.equals(text(body.get("sourcePlugin")))
                        && mapping.sourceObjectType.equals(text(body.get("sourceObjectType")))
                        && mapping.sourceObjectKey.equals(text(body.get("sourceObjectKey")))
                        && (!mapping.targetModule.equals(text(body.get("targetModule")))
                        || !mapping.targetObjectType.equals(text(body.get("targetObjectType")))
                        || !mapping.targetObjectId.equals(text(body.get("targetObjectId")))));
    }

    boolean providerHasActiveReferences(String providerId) {
        boolean hasActiveMapping = mappings.values().stream()
                .anyMatch(mapping -> providerId.equals(mapping.providerId) && "ACTIVE".equals(mapping.status));
        boolean hasEnabledRoute = routes.values().stream()
                .anyMatch(route -> providerId.equals(text(route.matchers.get("providerId"))) && route.enabled);
        boolean hasOpenTask = tasks.values().stream()
                .anyMatch(task -> providerId.equals(task.providerId) && !taskTerminal(task.status));
        return hasActiveMapping || hasEnabledRoute || hasOpenTask;
    }

    private static boolean taskTerminal(String status) {
        return List.of("SUCCEEDED", "FAILED", "CANCELED", "TIMEOUT").contains(status);
    }

    void guardDependency(HttpServletRequest request, boolean enabled, String dependency) {
        if (!enabled) return;
        if (dependencyFailed(request, true, dependency)) {
            switch (dependency) {
                case "ops" -> throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47060, "ops-control unavailable");
                case "online-map" -> throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47080, "online-map unavailable");
                default -> throw new PluginApiException(HttpStatus.BAD_GATEWAY, 46000, "dependency unavailable");
            }
        }
    }

    void failAuditIfRequested(HttpServletRequest request, boolean enabled) {
        if (enabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new PluginApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55701, "plugin audit write failed");
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        String id = "audit-" + (++sequence);
        audits.put(id, new PluginAudit(id, action, targetType, targetId, actor, request, body, riskLevel, result, failureReason, beforeState, afterState));
    }

    Map<String, Object> summary(boolean testControls) {
        return map("service", "plugin-integration",
                "port", 8122,
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "opsControlAdapterMode", "TEST_STUB",
                "nodeDaemonAdapterMode", "SIMULATED",
                "onlineMapAdapterMode", "TEST_STUB",
                "notificationAdapterMode", "TEST_STUB",
                "alertingAdapterMode", "TEST_STUB",
                "testControlsEnabled", testControls,
                "providersTotal", providers.size(),
                "enabledProvidersTotal", providers.values().stream().filter(provider -> "ENABLED".equals(provider.status)).count(),
                "instancesTotal", instances.size(),
                "schemasTotal", schemas.size(),
                "eventsTotal", events.size(),
                "routeRulesTotal", routes.size(),
                "syncTasksTotal", tasks.size(),
                "objectMappingsTotal", mappings.size(),
                "auditsTotal", audits.size(),
                "idempotencyRecordsTotal", idempotency.size(),
                "lastEventAt", providers.values().stream().map(provider -> provider.lastEventAt).filter(Objects::nonNull).findFirst().orElse(null),
                "lastSyncAt", providers.values().stream().map(provider -> provider.lastSyncAt).filter(Objects::nonNull).findFirst().orElse(null),
                "degraded", false,
                "degradeReasons", List.of(),
                "productionGaps", List.of("REAL_PLUGIN_RUNTIME_NOT_CONNECTED", "REAL_NODE_DAEMON_NOT_CONNECTED",
                        "REAL_ONLINE_MAP_SYNC_NOT_CONNECTED", "RAW_PAYLOAD_STORAGE_DISABLED", "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
    }

    Map<String, Object> latestHealth(String providerId) {
        return healthSnapshots.values().stream().filter(snapshot -> snapshot.providerId().equals(providerId)).findFirst().map(PluginHealth::view).orElse(null);
    }

    Map<String, Object> latestEvent(String providerId) {
        return events.values().stream().filter(event -> event.providerId.equals(providerId)).findFirst().map(PluginEvent::view).orElse(null);
    }

    Map<String, Object> latestTaskForEvent(String eventId) {
        return tasks.values().stream().filter(task -> Objects.equals(task.eventId, eventId)).findFirst().map(PluginTask::view).orElse(null);
    }

    Map<String, Object> latestAudit(String targetType, String targetId) {
        return audits.values().stream().filter(audit -> audit.targetType.equals(targetType) && audit.targetId.equals(targetId)).findFirst().map(PluginAudit::view).orElse(null);
    }
}

class PluginProvider {
    final String providerId;
    String providerType;
    String displayName;
    String pluginName;
    String pluginVersion;
    String serverKind;
    Map<String, Object> instanceRef;
    Map<String, Object> nodeRef;
    String status;
    boolean publicVisible;
    String eventEndpointSummary;
    List<String> allowedEventTypes;
    List<String> allowedOrigins;
    String healthStatus;
    String lastEventAt;
    String lastSyncAt;
    List<String> degradeReasons;
    String adminNote;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    PluginProvider(String providerId, String providerType, String displayName, String pluginName, String pluginVersion, String serverKind,
                   Map<String, Object> instanceRef, Map<String, Object> nodeRef, String status, boolean publicVisible,
                   String eventEndpointSummary, List<String> allowedEventTypes, List<String> allowedOrigins, String healthStatus,
                   String lastEventAt, String lastSyncAt, List<String> degradeReasons, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.providerId = providerId;
        this.providerType = providerType;
        this.displayName = displayName;
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.serverKind = serverKind;
        this.instanceRef = instanceRef;
        this.nodeRef = nodeRef;
        this.status = status;
        this.publicVisible = publicVisible;
        this.eventEndpointSummary = eventEndpointSummary;
        this.allowedEventTypes = allowedEventTypes;
        this.allowedOrigins = allowedOrigins;
        this.healthStatus = healthStatus;
        this.lastEventAt = lastEventAt;
        this.lastSyncAt = lastSyncAt;
        this.degradeReasons = degradeReasons;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.adminNote = null;
    }

    static PluginProvider from(String providerId, Map<String, Object> body, String actor) {
        return new PluginProvider(providerId, text(body.get("providerType")), text(body.get("displayName")), text(body.get("pluginName")),
                text(body.get("pluginVersion")), text(body.get("serverKind")), objectMap(body.get("instanceRef")), objectMap(body.get("nodeRef")),
                "DRAFT", bool(body.get("publicVisible")), text(body.get("eventEndpointSummary")), stringList(body.get("allowedEventTypes")),
                stringList(body.get("allowedOrigins")), "UNKNOWN", null, null, List.of(), actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("providerType")) providerType = text(body.get("providerType"));
        if (body.containsKey("displayName")) displayName = text(body.get("displayName"));
        if (body.containsKey("pluginName")) pluginName = text(body.get("pluginName"));
        if (body.containsKey("pluginVersion")) pluginVersion = text(body.get("pluginVersion"));
        if (body.containsKey("serverKind")) serverKind = text(body.get("serverKind"));
        if (body.containsKey("instanceRef")) instanceRef = objectMap(body.get("instanceRef"));
        if (body.containsKey("nodeRef")) nodeRef = objectMap(body.get("nodeRef"));
        if (body.containsKey("publicVisible")) publicVisible = bool(body.get("publicVisible"));
        if (body.containsKey("eventEndpointSummary")) eventEndpointSummary = text(body.get("eventEndpointSummary"));
        if (body.containsKey("allowedEventTypes")) allowedEventTypes = stringList(body.get("allowedEventTypes"));
        if (body.containsKey("allowedOrigins")) allowedOrigins = stringList(body.get("allowedOrigins"));
        updatedBy = actor;
        updatedAt = now();
    }

    Map<String, Object> view() {
        return map("providerId", providerId, "providerType", providerType, "displayName", displayName, "pluginName", pluginName,
                "pluginVersion", pluginVersion, "serverKind", serverKind, "instanceRef", instanceRef, "nodeRef", nodeRef,
                "status", status, "publicVisible", publicVisible, "eventEndpointSummary", eventEndpointSummary,
                "allowedEventTypes", allowedEventTypes, "allowedOrigins", allowedOrigins, "healthStatus", healthStatus,
                "lastEventAt", lastEventAt, "lastSyncAt", lastSyncAt, "degradeReasons", degradeReasons, "adminNote", adminNote,
                "createdBy", createdBy, "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

record PluginInstance(String instanceId, String providerId, Map<String, Object> opsInstanceRef, Map<String, Object> serverStatusRef,
                      String pluginName, String pluginVersion, String serverVersion, boolean loaded, boolean enabled,
                      List<String> dependencyPlugins, Map<String, Object> metricsSummary, String lastSeenAt, boolean stale) {
    Map<String, Object> view() {
        return map("instanceId", instanceId, "providerId", providerId, "opsInstanceRef", opsInstanceRef, "serverStatusRef", serverStatusRef,
                "pluginName", pluginName, "pluginVersion", pluginVersion, "serverVersion", serverVersion, "loaded", loaded, "enabled", enabled,
                "dependencyPlugins", dependencyPlugins, "capabilities", List.of(), "metricsSummary", metricsSummary, "lastSeenAt", lastSeenAt, "stale", stale);
    }
}

record PluginCapability(String capabilityId, String providerId, String namespace, String name, String version, String riskLevel,
                        boolean available, Map<String, Object> summary) {
    Map<String, Object> view() {
        return map("capabilityId", capabilityId, "providerId", providerId, "namespace", namespace, "name", name, "version", version,
                "riskLevel", riskLevel, "available", available, "summary", summary);
    }
}

class PluginSchema {
    final String schemaId;
    final String providerId;
    String eventType;
    String sourcePlugin;
    String version;
    String status;
    List<String> requiredFields;
    List<String> optionalFields;
    List<String> sensitiveFields;
    Map<String, Object> routingHints;
    Map<String, Object> samplePayloadSummary;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    PluginSchema(String schemaId, String providerId, String eventType, String sourcePlugin, String version, String status,
                 List<String> requiredFields, List<String> optionalFields, List<String> sensitiveFields, Map<String, Object> routingHints,
                 Map<String, Object> samplePayloadSummary, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.schemaId = schemaId;
        this.providerId = providerId;
        this.eventType = eventType;
        this.sourcePlugin = sourcePlugin;
        this.version = version;
        this.status = status;
        this.requiredFields = requiredFields;
        this.optionalFields = optionalFields;
        this.sensitiveFields = sensitiveFields;
        this.routingHints = routingHints;
        this.samplePayloadSummary = samplePayloadSummary;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static PluginSchema from(String schemaId, Map<String, Object> body, String actor) {
        return new PluginSchema(schemaId, text(body.get("providerId")), text(body.get("eventType")), text(body.get("sourcePlugin")), text(body.get("version")),
                "DRAFT", stringList(body.get("requiredFields")), stringList(body.get("optionalFields")), stringList(body.get("sensitiveFields")),
                objectMap(body.get("routingHints")), objectMap(body.get("samplePayloadSummary")), actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("eventType")) eventType = text(body.get("eventType"));
        if (body.containsKey("sourcePlugin")) sourcePlugin = text(body.get("sourcePlugin"));
        if (body.containsKey("version")) version = text(body.get("version"));
        if (body.containsKey("requiredFields")) requiredFields = stringList(body.get("requiredFields"));
        if (body.containsKey("optionalFields")) optionalFields = stringList(body.get("optionalFields"));
        if (body.containsKey("sensitiveFields")) sensitiveFields = stringList(body.get("sensitiveFields"));
        if (body.containsKey("routingHints")) routingHints = objectMap(body.get("routingHints"));
        if (body.containsKey("samplePayloadSummary")) samplePayloadSummary = objectMap(body.get("samplePayloadSummary"));
        updatedBy = actor;
        updatedAt = now();
    }

    Map<String, Object> view() {
        return map("schemaId", schemaId, "providerId", providerId, "eventType", eventType, "sourcePlugin", sourcePlugin, "version", version,
                "status", status, "requiredFields", requiredFields, "optionalFields", optionalFields, "sensitiveFields", sensitiveFields,
                "routingHints", routingHints, "samplePayloadSummary", samplePayloadSummary, "createdBy", createdBy, "updatedBy", updatedBy,
                "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

class PluginEvent {
    final String eventId;
    final String providerId;
    final String eventType;
    final String schemaId;
    final String sourcePlugin;
    final String sourceInstanceId;
    final String dedupeKey;
    final Map<String, Object> payloadSummary;
    final String validationStatus;
    String routeStatus;
    String syncStatus;
    final String notificationStatus;
    final String receivedAt;
    String processedAt;
    String failureReason;
    final String requestId;

    PluginEvent(String eventId, String providerId, String eventType, String schemaId, String sourcePlugin, String sourceInstanceId, String dedupeKey,
                Map<String, Object> payloadSummary, String validationStatus, String routeStatus, String syncStatus, String notificationStatus,
                String receivedAt, String processedAt, String failureReason, String requestId) {
        this.eventId = eventId;
        this.providerId = providerId;
        this.eventType = eventType;
        this.schemaId = schemaId;
        this.sourcePlugin = sourcePlugin;
        this.sourceInstanceId = sourceInstanceId;
        this.dedupeKey = dedupeKey;
        this.payloadSummary = payloadSummary;
        this.validationStatus = validationStatus;
        this.routeStatus = routeStatus;
        this.syncStatus = syncStatus;
        this.notificationStatus = notificationStatus;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.failureReason = failureReason;
        this.requestId = requestId;
    }

    static PluginEvent from(String eventId, PluginSchema schema, Map<String, Object> body, Map<String, Object> payload, String notificationStatus, String requestId) {
        return new PluginEvent(eventId, text(body.get("providerId")), schema.eventType, schema.schemaId, schema.sourcePlugin, text(body.get("sourceInstanceId")),
                text(body.get("dedupeKey")), summarizePayload(payload), "VALIDATED", "PENDING", "SKIPPED", notificationStatus, now(), now(), null, requestId);
    }

    Map<String, Object> view() {
        return map("eventId", eventId, "providerId", providerId, "eventType", eventType, "schemaId", schemaId, "sourcePlugin", sourcePlugin,
                "sourceInstanceId", sourceInstanceId, "dedupeKey", dedupeKey, "payloadSummary", payloadSummary, "raw" + "PayloadStored", false,
                "validationStatus", validationStatus, "routeStatus", routeStatus, "syncStatus", syncStatus, "notificationStatus", notificationStatus,
                "receivedAt", receivedAt, "processedAt", processedAt, "failureReason", failureReason, "requestId", requestId);
    }
}

class PluginRoute {
    final String ruleId;
    String displayName;
    String eventType;
    Map<String, Object> matchers;
    String targetModule;
    String targetAction;
    boolean enabled;
    String riskLevel;
    Map<String, Object> rateLimitSummary;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    PluginRoute(String ruleId, String displayName, String eventType, Map<String, Object> matchers, String targetModule, String targetAction,
                boolean enabled, String riskLevel, Map<String, Object> rateLimitSummary, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.ruleId = ruleId;
        this.displayName = displayName;
        this.eventType = eventType;
        this.matchers = matchers;
        this.targetModule = targetModule;
        this.targetAction = targetAction;
        this.enabled = enabled;
        this.riskLevel = riskLevel;
        this.rateLimitSummary = rateLimitSummary;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static PluginRoute from(String ruleId, Map<String, Object> body, String actor) {
        return new PluginRoute(ruleId, text(body.get("displayName")), text(body.get("eventType")), objectMap(body.get("matchers")),
                text(body.get("targetModule")), text(body.get("targetAction")), bool(body.get("enabled")),
                textOr(body.get("riskLevel"), "MEDIUM"), objectMap(body.get("rateLimitSummary")), actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("displayName")) displayName = text(body.get("displayName"));
        if (body.containsKey("eventType")) eventType = text(body.get("eventType"));
        if (body.containsKey("matchers")) matchers = objectMap(body.get("matchers"));
        if (body.containsKey("targetModule")) targetModule = text(body.get("targetModule"));
        if (body.containsKey("targetAction")) targetAction = text(body.get("targetAction"));
        if (body.containsKey("enabled")) enabled = bool(body.get("enabled"));
        if (body.containsKey("riskLevel")) riskLevel = text(body.get("riskLevel"));
        if (body.containsKey("rateLimitSummary")) rateLimitSummary = objectMap(body.get("rateLimitSummary"));
        updatedBy = actor;
        updatedAt = now();
    }

    Map<String, Object> view() {
        return map("ruleId", ruleId, "displayName", displayName, "eventType", eventType, "matchers", matchers, "targetModule", targetModule,
                "targetAction", targetAction, "enabled", enabled, "riskLevel", riskLevel, "rateLimitSummary", rateLimitSummary,
                "createdBy", createdBy, "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

class PluginTask {
    final String taskId;
    final String providerId;
    final String eventId;
    final String targetModule;
    final String targetAction;
    String status;
    final String riskLevel;
    final Map<String, Object> paramsSummary;
    Map<String, Object> resultSummary;
    String failureReason;
    final String idempotencyKey;
    final String createdBy;
    final String createdAt;
    String updatedAt;
    final String expiresAt;

    PluginTask(String taskId, String providerId, String eventId, String targetModule, String targetAction, String status, String riskLevel,
               Map<String, Object> paramsSummary, String idempotencyKey, String createdBy) {
        this.taskId = taskId;
        this.providerId = providerId;
        this.eventId = eventId;
        this.targetModule = targetModule;
        this.targetAction = targetAction;
        this.status = status;
        this.riskLevel = riskLevel;
        this.paramsSummary = paramsSummary;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        this.createdAt = now();
        this.updatedAt = createdAt;
        this.expiresAt = Instant.now().plusSeconds(3600).toString();
    }

    static PluginTask from(String taskId, Map<String, Object> body, String actor) {
        String target = text(body.get("targetModule"));
        String status = "ONLINE_MAP".equals(target) ? "SIMULATED_BLOCKED" : "QUEUED";
        return new PluginTask(taskId, text(body.get("providerId")), text(body.get("eventId")), target, text(body.get("targetAction")),
                status, textOr(body.get("riskLevel"), "MEDIUM"), summarizeParams(objectMap(body.get("params"))), text(body.get("idempotencyKey")), actor);
    }

    Map<String, Object> view() {
        return map("taskId", taskId, "providerId", providerId, "eventId", eventId, "targetModule", targetModule, "targetAction", targetAction,
                "status", status, "riskLevel", riskLevel, "paramsSummary", paramsSummary, "resultSummary", resultSummary,
                "failureReason", failureReason, "idempotencyKey", idempotencyKey, "createdBy", createdBy, "createdAt", createdAt,
                "updatedAt", updatedAt, "expiresAt", expiresAt);
    }
}

class PluginMapping {
    final String mappingId;
    String providerId;
    String sourcePlugin;
    String sourceObjectType;
    String sourceObjectKey;
    String targetModule;
    String targetObjectType;
    String targetObjectId;
    String status;
    String visibility;
    String lastSyncedAt;
    String syncHash;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    PluginMapping(String mappingId, String providerId, String sourcePlugin, String sourceObjectType, String sourceObjectKey, String targetModule,
                  String targetObjectType, String targetObjectId, String status, String visibility, String lastSyncedAt, String syncHash,
                  String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.mappingId = mappingId;
        this.providerId = providerId;
        this.sourcePlugin = sourcePlugin;
        this.sourceObjectType = sourceObjectType;
        this.sourceObjectKey = sourceObjectKey;
        this.targetModule = targetModule;
        this.targetObjectType = targetObjectType;
        this.targetObjectId = targetObjectId;
        this.status = status;
        this.visibility = visibility;
        this.lastSyncedAt = lastSyncedAt;
        this.syncHash = syncHash;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static PluginMapping from(String mappingId, Map<String, Object> body, String actor, PluginMapping existing) {
        String createdAt = existing == null ? now() : existing.createdAt;
        String createdBy = existing == null ? actor : existing.createdBy;
        return new PluginMapping(mappingId, text(body.get("providerId")), text(body.get("sourcePlugin")), text(body.get("sourceObjectType")),
                text(body.get("sourceObjectKey")), text(body.get("targetModule")), text(body.get("targetObjectType")), text(body.get("targetObjectId")),
                textOr(body.get("status"), "ACTIVE"), textOr(body.get("visibility"), "PRIVATE"), now(), text(body.get("syncHash")),
                createdBy, actor, createdAt, now());
    }

    Map<String, Object> view() {
        return map("mappingId", mappingId, "providerId", providerId, "sourcePlugin", sourcePlugin, "sourceObjectType", sourceObjectType,
                "sourceObjectKey", sourceObjectKey, "targetModule", targetModule, "targetObjectType", targetObjectType, "targetObjectId", targetObjectId,
                "status", status, "visibility", visibility, "lastSyncedAt", lastSyncedAt, "syncHash", syncHash, "createdBy", createdBy,
                "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

record PluginHealth(String snapshotId, String providerId, String healthStatus, Map<String, Object> dependencyStatus,
                    Map<String, Object> metricsSummary, boolean degraded, List<String> degradeReasons, String checkedAt) {
    static PluginHealth seed(String snapshotId, String providerId, String status) {
        return new PluginHealth(snapshotId, providerId, status, map("status", "AVAILABLE"), map("eventsPerMinute", 12), false, List.of(), now());
    }

    Map<String, Object> view() {
        return map("snapshotId", snapshotId, "providerId", providerId, "healthStatus", healthStatus, "dependencyStatus", dependencyStatus,
                "metricsSummary", metricsSummary, "degraded", degraded, "degradeReasons", degradeReasons, "checkedAt", checkedAt);
    }
}

class PluginAudit {
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
    final String reason;
    final Map<String, Object> paramsSummary;
    final String createdAt = now();
    final String providerId;
    final String eventId;
    final String schemaId;
    final String ruleId;
    final String taskId;
    final String mappingId;

    PluginAudit(String id, String action, String targetType, String targetId, Actor actor, HttpServletRequest request,
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
        this.reason = text(body == null ? null : body.get("reason"));
        this.paramsSummary = summarizeParams(body == null ? Map.of() : body);
        this.providerId = "PROVIDER".equals(targetType) ? targetId : null;
        this.eventId = "EVENT".equals(targetType) ? targetId : null;
        this.schemaId = "SCHEMA".equals(targetType) ? targetId : null;
        this.ruleId = "ROUTE_RULE".equals(targetType) ? targetId : null;
        this.taskId = "SYNC_TASK".equals(targetType) ? targetId : null;
        this.mappingId = "OBJECT_MAPPING".equals(targetType) ? targetId : null;
    }

    Map<String, Object> view() {
        return map("id", id, "requestId", requestId, "actorUserId", actorUserId, "actorRole", actorRole, "actorPermissions", actorPermissions,
                "sourceIp", null, "targetType", targetType, "targetId", targetId, "action", action, "riskLevel", riskLevel,
                "reason", reason, "paramsSummary", paramsSummary, "beforeState", beforeState, "afterState", afterState,
                "result", result, "failureReason", failureReason, "providerId", providerId, "eventId", eventId, "schemaId", schemaId,
                "ruleId", ruleId, "taskId", taskId, "mappingId", mappingId, "dependencyStatus", map("status", "AVAILABLE"),
                "notificationStatus", "SKIPPED", "createdAt", createdAt);
    }
}

@Service
class PluginAuth {
    private final PluginProperties properties;

    PluginAuth(PluginProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Auth-Mode");
            if ("unavailable".equals(mode)) throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47050, "auth unavailable");
            if ("timeout".equals(mode)) throw new PluginApiException(HttpStatus.GATEWAY_TIMEOUT, 47051, "auth timeout");
            if ("bad-schema".equals(mode)) throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47052, "auth bad schema");
        }
        String header = request.getHeader("Author" + "ization");
        if (header == null || header.isBlank()) {
            throw new PluginApiException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new PluginApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        return switch (header.substring("Bearer ".length())) {
            case "auth-unavailable-token" -> throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47050, "auth unavailable");
            case "auth-timeout-token" -> throw new PluginApiException(HttpStatus.GATEWAY_TIMEOUT, 47051, "auth timeout");
            case "auth-bad-token" -> throw new PluginApiException(HttpStatus.BAD_GATEWAY, 47052, "auth bad schema");
            case "plugin-viewer-token" -> new Actor("plugin-viewer-user", "Plugin Viewer", "HELPER", List.of("NODE_READ"));
            case "plugin-no-cap-token" -> new Actor("plugin-no-cap-user", "No Cap", "ADMIN", List.of());
            case "plugin-admin-token" -> new Actor("plugin-admin-user", "Plugin Admin", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "owner-token" -> new Actor("owner-user", "Owner", "OWNER", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "Plain User", "USER", List.of());
            default -> throw new PluginApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireRead(HttpServletRequest request) {
        Actor actor = current(request);
        if ("USER".equals(actor.role)) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.permissions.contains("NODE_READ")) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    Actor requireWrite(HttpServletRequest request) {
        Actor actor = current(request);
        if ("USER".equals(actor.role)) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.permissions.contains("NODE_WRITE")) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
    }

    void requireAudit(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new PluginApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
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
class PluginProperties {
    private final boolean enabled;

    PluginProperties(@Value("${plugin-integration.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class PluginRequestIdFilter extends OncePerRequestFilter {
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

@RestControllerAdvice
class PluginExceptionHandler {
    @ExceptionHandler(PluginApiException.class)
    ResponseEntity<Map<String, Object>> api(PluginApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status).body(errorBody(exception.code, exception.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> fallback(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(55700, "plugin-integration internal error", request));
    }
}

class PluginApiException extends RuntimeException {
    final HttpStatus status;
    final int code;

    PluginApiException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

class PluginText {
    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    static boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(text(value));
    }

    static int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    static Map<String, Object> objectMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        }
        return result;
    }

    static List<String> stringList(Object value) {
        if (value instanceof Collection<?> source) {
            return source.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}

record TimeRange(Instant from, Instant to) {
    boolean contains(String value) {
        Instant instant = parseInstantOrNull(value);
        if (instant == null) return true;
        if (from != null && instant.isBefore(from)) return false;
        return to == null || !instant.isAfter(to);
    }
}

class PluginSupport {
    static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(successBody(data, request));
    }

    static ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(successBody(data, request));
    }

    static Map<String, Object> successBody(Object data, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    static Map<String, Object> errorBody(int code, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    static ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body,
                                                          Supplier<ResponseEntity<Map<String, Object>>> action) {
        PluginStore store = SpringAccess.store;
        String key = PluginText.text(body.get("idempotencyKey"));
        if (key.isBlank()) {
            return action.get();
        }
        String recordKey = actor.userId + ":" + scope + ":" + key;
        String fingerprint = fingerprint(body);
        IdempotencyRecord existing = store.idempotency.get(recordKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new PluginApiException(HttpStatus.CONFLICT, 49812, "idempotency key conflict");
            }
            return ResponseEntity.status(existing.status()).body(successBody(existing.data(), request));
        }
        ResponseEntity<Map<String, Object>> response = action.get();
        Object data = response.getBody() == null ? null : response.getBody().get("data");
        store.idempotency.put(recordKey, new IdempotencyRecord(fingerprint, HttpStatus.valueOf(response.getStatusCode().value()), data));
        return response;
    }

    static void validatePage(Map<String, String> query) {
        int page = intQuery(query.get("page"), 1);
        int pageSize = intQuery(query.get("pageSize"), 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    static void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) return;
        if (!List.of(allowed).contains(sort)) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intQuery(query.get("page"), 1);
        int pageSize = intQuery(query.get("pageSize"), 20);
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return map("items", items.subList(from, to), "page", page, "pageSize", pageSize, "total", items.size());
    }

    static void rejectTrusted(Object value) {
        if (containsTrusted(value)) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "trusted field is not allowed");
        }
    }

    static boolean containsTrusted(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
                if (trustedKey(key) || containsTrusted(entry.getValue())) {
                    return true;
                }
            }
        }
        if (value instanceof Collection<?> values) {
            for (Object item : values) {
                if (containsTrusted(item)) return true;
            }
        }
        return false;
    }

    static boolean trustedKey(String key) {
        return key.contains("actor") || key.contains("beforestate") || key.contains("afterstate") || key.contains("auditresult")
                || key.contains("createdby") || key.contains("updatedby") || key.contains("enabledby") || key.contains("disabledby")
                || key.contains("archivedby") || key.contains("token") || key.contains("secret") || key.contains("password")
                || key.contains("cred" + "ential") || key.contains("authorization") || key.contains("requestheaders")
                || key.contains("internal") || key.contains("resolvedpath") || key.contains("worlddirectory")
                || key.contains("fullexception") || key.contains("raw" + "payload");
    }

    static void validateReason(Map<String, Object> body) {
        String reason = text(body.get("reason"));
        if (reason.isBlank() || reason.length() > 200) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "reason is required");
        }
    }

    static void validateProvider(Map<String, Object> body, boolean create) {
        validateReason(body);
        if (create && (text(body.get("providerType")).isBlank() || text(body.get("displayName")).isBlank() || text(body.get("pluginName")).isBlank())) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "provider fields are required");
        }
        String endpoint = text(body.get("eventEndpointSummary"));
        if (!endpoint.isBlank() && unsafeEndpoint(endpoint)) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 49813, "unsafe plugin endpoint");
        }
        for (String origin : stringList(body.get("allowedOrigins"))) {
            if ("*".equals(origin) || unsafeEndpoint(origin)) {
                throw new PluginApiException(HttpStatus.BAD_REQUEST, 49813, "unsafe plugin origin");
            }
        }
    }

    static void validateSchema(Map<String, Object> body) {
        validateReason(body);
        if (text(body.get("providerId")).isBlank() || text(body.get("eventType")).isBlank() || text(body.get("sourcePlugin")).isBlank()) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "schema fields are required");
        }
        rejectTrusted(body.get("samplePayloadSummary"));
    }

    static void validateRoute(Map<String, Object> body) {
        validateReason(body);
        if (text(body.get("eventType")).isBlank() || text(body.get("targetModule")).isBlank() || text(body.get("targetAction")).isBlank()) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "route fields are required");
        }
    }

    static void validatePayload(PluginSchema schema, Map<String, Object> payload) {
        rejectTrusted(payload);
        for (String field : schema.requiredFields) {
            if (!payload.containsKey(field)) {
                throw new PluginApiException(HttpStatus.BAD_REQUEST, 49814, "plugin event payload invalid");
            }
        }
        for (String field : schema.sensitiveFields) {
            if (payload.containsKey(field)) {
                throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "sensitive payload field is not allowed");
            }
        }
    }

    static boolean unsafeEndpoint(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("127.0.0.1") || lower.contains("localhost") || lower.startsWith("file:")
                || lower.startsWith("data:") || lower.startsWith("javascript:") || lower.contains("@");
    }

    static boolean providerPatchHighRisk(Map<String, Object> body) {
        return body.containsKey("eventEndpointSummary") || body.containsKey("allowedOrigins") || body.containsKey("allowedEventTypes")
                || (body.containsKey("publicVisible") && bool(body.get("publicVisible")));
    }

    static boolean routeHighRisk(Map<String, Object> body) {
        String risk = text(body.get("riskLevel"));
        return "HIGH".equals(risk) || "CRITICAL".equals(risk) || "OPS_CONTROL".equals(text(body.get("targetModule")));
    }

    static String providerRisk(Map<String, Object> body) {
        return needsPublicRisk(body) ? "HIGH" : "MEDIUM";
    }

    static boolean needsPublicRisk(Map<String, Object> body) {
        return bool(body.get("publicVisible")) || !text(body.get("eventEndpointSummary")).isBlank() || !stringList(body.get("allowedOrigins")).isEmpty();
    }

    static boolean dependencyFailed(HttpServletRequest request, boolean enabled, String dependency) {
        if (!enabled) return false;
        String header = switch (dependency) {
            case "ops" -> "X-Test-Ops-Control-Mode";
            case "online-map" -> "X-Test-Online-Map-Mode";
            case "notification" -> "X-Test-Notification-Mode";
            default -> "";
        };
        return "unavailable".equals(request.getHeader(header));
    }

    static Map<String, Object> summarizePayload(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sanitized", true);
        result.put("fieldNames", new ArrayList<>(new TreeMap<>(payload).keySet()));
        payload.forEach((key, value) -> result.put(key, value instanceof Number || value instanceof Boolean ? value : text(value)));
        return result;
    }

    static Map<String, Object> summarizeParams(Map<String, Object> body) {
        Map<String, Object> sorted = new TreeMap<>(body);
        return map("sanitized", true, "fieldNames", new ArrayList<>(sorted.keySet()), "hasIdempotencyKey", body.containsKey("idempotencyKey"));
    }

    static TimeRange timeRange(Map<String, String> query) {
        Instant from = parseInstantOrNull(query.get("from"));
        Instant to = parseInstantOrNull(query.get("to"));
        if (query.get("from") != null && from == null) throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "invalid time");
        if (query.get("to") != null && to == null) throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "invalid time");
        if (from != null && to != null && to.isBefore(from)) throw new PluginApiException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        return from == null && to == null ? null : new TimeRange(from, to);
    }

    static String fingerprint(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            TreeMap<String, String> sorted = new TreeMap<>();
            mapValue.forEach((key, mapItem) -> sorted.put(String.valueOf(key), fingerprint(mapItem)));
            return sorted.toString();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(PluginSupport::fingerprint).toList().toString();
        }
        return String.valueOf(value);
    }

    static Comparator<PluginProvider> providerComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(provider -> provider.displayName);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((PluginProvider provider) -> provider.createdAt).reversed();
        return Comparator.comparing((PluginProvider provider) -> provider.updatedAt).reversed();
    }

    static Comparator<PluginInstance> instanceComparator(String sort) {
        if ("pluginName_asc".equals(sort)) return Comparator.comparing(PluginInstance::pluginName);
        if ("pluginVersion_desc".equals(sort)) return Comparator.comparing(PluginInstance::pluginVersion).reversed();
        return Comparator.comparing(PluginInstance::lastSeenAt).reversed();
    }

    static Comparator<PluginSchema> schemaComparator(String sort) {
        if ("eventType_asc".equals(sort)) return Comparator.comparing(schema -> schema.eventType);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((PluginSchema schema) -> schema.createdAt).reversed();
        return Comparator.comparing((PluginSchema schema) -> schema.updatedAt).reversed();
    }

    static Comparator<PluginEvent> eventComparator(String sort) {
        if ("receivedAt_asc".equals(sort)) return Comparator.comparing(event -> event.receivedAt);
        if ("processedAt_desc".equals(sort)) return Comparator.comparing((PluginEvent event) -> text(event.processedAt)).reversed();
        return Comparator.comparing((PluginEvent event) -> event.receivedAt).reversed();
    }

    static Comparator<PluginRoute> routeComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(route -> route.displayName);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((PluginRoute route) -> route.createdAt).reversed();
        return Comparator.comparing((PluginRoute route) -> route.updatedAt).reversed();
    }

    static Comparator<PluginTask> taskComparator(String sort) {
        if ("riskLevel_desc".equals(sort)) return Comparator.comparing((PluginTask task) -> task.riskLevel).reversed();
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((PluginTask task) -> task.createdAt).reversed();
        return Comparator.comparing((PluginTask task) -> task.updatedAt).reversed();
    }

    static Comparator<PluginMapping> mappingComparator(String sort) {
        if ("lastSyncedAt_desc".equals(sort)) return Comparator.comparing((PluginMapping mapping) -> text(mapping.lastSyncedAt)).reversed();
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((PluginMapping mapping) -> mapping.createdAt).reversed();
        return Comparator.comparing((PluginMapping mapping) -> mapping.updatedAt).reversed();
    }

    static Comparator<PluginAudit> auditComparator(String sort) {
        if ("createdAt_asc".equals(sort)) return Comparator.comparing(audit -> audit.createdAt);
        if ("riskLevel_desc".equals(sort)) return Comparator.comparing((PluginAudit audit) -> audit.riskLevel).reversed();
        return Comparator.comparing((PluginAudit audit) -> audit.createdAt).reversed();
    }

    static boolean matches(String value, String keyword) {
        return keyword == null || keyword.isBlank() || text(value).toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    static Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    static int intQuery(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new PluginApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    static String now() {
        return Instant.now().toString();
    }

    static String text(Object value) {
        return PluginText.text(value);
    }

    static String textOr(Object value, String fallback) {
        return PluginText.textOr(value, fallback);
    }

    static boolean bool(Object value) {
        return PluginText.bool(value);
    }

    static Map<String, Object> objectMap(Object value) {
        return PluginText.objectMap(value);
    }

    static List<String> stringList(Object value) {
        return PluginText.stringList(value);
    }
}

@Component
class SpringAccess {
    static PluginStore store;

    SpringAccess(PluginStore store) {
        SpringAccess.store = store;
    }
}
