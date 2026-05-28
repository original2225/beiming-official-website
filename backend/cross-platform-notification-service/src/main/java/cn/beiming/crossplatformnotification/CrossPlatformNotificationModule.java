package cn.beiming.crossplatformnotification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.beiming.crossplatformnotification.CpnSupport.*;

@RestController
@RequestMapping("/api/v1/cross-platform-notification")
class CrossPlatformNotificationController {
    private static final String VERSION = "0.1.0-contract";
    private final CpnStore store;
    private final CpnAuth auth;
    private final CpnProperties properties;

    CrossPlatformNotificationController(CpnStore store, CpnAuth auth, CpnProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, map("service", "cross-platform-notification", "status", store.health(), "version", VERSION));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireRead(request);
        if (testOn(properties, request) && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new CpnApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55800, "cross-platform notification internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> providers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "lastTestAt_desc", "lastDeliveryAt_desc");
        List<Map<String, Object>> items = store.providers.values().stream()
                .filter(item -> matches(item.displayName, query.get("keyword")) || matches(item.providerId, query.get("keyword")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("status") == null || item.status.equals(query.get("status")))
                .filter(item -> query.get("healthStatus") == null || item.healthStatus.equals(query.get("healthStatus")))
                .filter(item -> query.get("sourceModule") == null || item.allowedSourceModules.contains(query.get("sourceModule")))
                .filter(item -> query.get("degraded") == null || item.degraded == bool(query.get("degraded")))
                .sorted(providerComparator(query.get("sort")))
                .map(CpnProvider::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> provider(HttpServletRequest request, @PathVariable String providerId) {
        auth.requireRead(request);
        CpnProvider provider = store.provider(providerId);
        Map<String, Object> view = provider.view();
        view.put("capabilitySummary", store.capabilityForProvider(providerId).map(CpnCapability::view).orElse(null));
        view.put("recentDeliverySummary", store.latestDelivery(providerId));
        view.put("recentFailureSummary", store.latestFailure(providerId));
        view.put("dependencySummary", store.dependencySummary(provider));
        view.put("recentAuditSummary", store.latestAudit("PROVIDER", providerId));
        return ok(request, view);
    }

    @PostMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> createProvider(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        requireConfirm(body, "REGISTER_EXTERNAL_PROVIDER");
        validateProviderBody(body, true);
        return idempotent(request, actor, "provider:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            String displayName = requiredText(body, "displayName");
            String channel = requiredText(body, "channel");
            if (store.providerNameConflict(channel, displayName)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "external provider conflict");
            }
            Map<String, Object> endpoint = endpointSummary(body.get("endpointSummary"));
            String providerId = "provider-" + store.nextId(text(body.get("idempotencyKey")));
            CpnProvider provider = CpnProvider.from(providerId, body, endpoint, actor.userId());
            store.providers.put(providerId, provider);
            store.ensureCapability(provider);
            store.audit("EXTERNAL_PROVIDER_CREATED", "PROVIDER", providerId, actor, request, body, "HIGH", "SUCCESS", null, null, provider.status);
            return new WriteResult(HttpStatus.CREATED, provider.view());
        });
    }

    @PatchMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> patchProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateProviderBody(body, false);
        validateReason(body);
        if (providerPatchNeedsConfirm(body)) {
            requireConfirm(body, "UPDATE_EXTERNAL_PROVIDER");
        }
        return idempotent(request, actor, "provider:patch:" + providerId, body, () -> {
            CpnProvider provider = store.provider(providerId);
            if ("ARCHIVED".equals(provider.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "provider state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = provider.status;
            provider.patch(body, actor.userId());
            if (body.containsKey("endpointSummary")) {
                provider.endpointSummary = endpointSummary(body.get("endpointSummary"));
            }
            store.audit("EXTERNAL_PROVIDER_UPDATED", "PROVIDER", providerId, actor, request, body, providerPatchNeedsConfirm(body) ? "HIGH" : "MEDIUM", "SUCCESS", null, before, provider.status);
            return new WriteResult(HttpStatus.OK, provider.view());
        });
    }

    @PatchMapping("/admin/providers/{providerId}/enable")
    ResponseEntity<Map<String, Object>> enableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ENABLED", "EXTERNAL_PROVIDER_ENABLED", "ENABLE_EXTERNAL_PROVIDER", "HIGH");
    }

    @PatchMapping("/admin/providers/{providerId}/disable")
    ResponseEntity<Map<String, Object>> disableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "DISABLED", "EXTERNAL_PROVIDER_DISABLED", null, "MEDIUM");
    }

    @PatchMapping("/admin/providers/{providerId}/archive")
    ResponseEntity<Map<String, Object>> archiveProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ARCHIVED", "EXTERNAL_PROVIDER_ARCHIVED", "ARCHIVE_EXTERNAL_PROVIDER", "HIGH");
    }

    private ResponseEntity<Map<String, Object>> providerState(HttpServletRequest request, String providerId, Map<String, Object> body,
                                                              String target, String action, String confirm, String risk) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        if (confirm != null) {
            requireConfirm(body, confirm);
        }
        return idempotent(request, actor, "provider:" + target + ":" + providerId, body, () -> {
            CpnProvider provider = store.provider(providerId);
            if ("ARCHIVED".equals(provider.status) || ("ARCHIVED".equals(target) && "ENABLED".equals(provider.status))) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "provider state conflict");
            }
            if ("ENABLED".equals(target)) {
                if (provider.allowedSourceModules.isEmpty() || provider.allowedRiskLevels.isEmpty() || store.capabilityForProvider(providerId).isEmpty()) {
                    throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "provider enable prerequisites missing");
                }
            }
            if ("ARCHIVED".equals(target) && store.providerHasActiveReferences(providerId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "provider state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = provider.status;
            provider.status = target;
            provider.updatedBy = actor.userId();
            provider.updatedAt = now();
            if ("ENABLED".equals(target)) {
                provider.healthStatus = "HEALTHY";
                provider.degraded = false;
                provider.degradeReasons = List.of();
            }
            store.audit(action, "PROVIDER", providerId, actor, request, body, risk, "SUCCESS", null, before, provider.status);
            return new WriteResult(HttpStatus.OK, provider.view());
        });
    }

    @GetMapping("/admin/capabilities")
    ResponseEntity<Map<String, Object>> capabilities(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "channel_asc", "maxBodyLength_desc");
        List<Map<String, Object>> items = store.capabilities.values().stream()
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("supportsMarkdown") == null || item.supportsMarkdown == bool(query.get("supportsMarkdown")))
                .filter(item -> query.get("supportsRichBlocks") == null || item.supportsRichBlocks == bool(query.get("supportsRichBlocks")))
                .filter(item -> query.get("supportsDeliveryCallback") == null || item.supportsDeliveryCallback == bool(query.get("supportsDeliveryCallback")))
                .filter(item -> matches(item.capabilityId, query.get("keyword")) || matches(item.channel, query.get("keyword")))
                .sorted(capabilityComparator(query.get("sort")))
                .map(CpnCapability::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/capabilities/{capabilityId}")
    ResponseEntity<Map<String, Object>> capability(HttpServletRequest request, @PathVariable String capabilityId) {
        auth.requireRead(request);
        CpnCapability capability = Optional.ofNullable(store.capabilities.get(capabilityId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49956, "capability not found"));
        Map<String, Object> view = capability.view();
        view.put("providerSummary", store.provider(capability.providerId).summary());
        view.put("recentDegradeReasons", store.provider(capability.providerId).degradeReasons);
        return ok(request, view);
    }

    @GetMapping("/admin/template-mappings")
    ResponseEntity<Map<String, Object>> templateMappings(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "sourceModule_asc", "version_desc");
        List<Map<String, Object>> items = store.mappings.values().stream()
                .filter(item -> query.get("sourceModule") == null || item.sourceModule.equals(query.get("sourceModule")))
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("status") == null || item.status.equals(query.get("status")))
                .filter(item -> query.get("renderMode") == null || item.renderMode.equals(query.get("renderMode")))
                .filter(item -> matches(item.mappingId, query.get("keyword")) || matches(item.externalTemplateKey, query.get("keyword")))
                .sorted(mappingComparator(query.get("sort")))
                .map(CpnTemplateMapping::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/template-mappings/{mappingId}")
    ResponseEntity<Map<String, Object>> templateMapping(HttpServletRequest request, @PathVariable String mappingId) {
        auth.requireRead(request);
        CpnTemplateMapping mapping = store.mapping(mappingId);
        Map<String, Object> view = mapping.view();
        view.put("providerSummary", store.provider(mapping.providerId).summary());
        view.put("sourceTemplateSummary", mapping.sourceTemplateRef);
        view.put("recentDeliverySummary", store.latestDelivery(mapping.providerId));
        view.put("recentAuditSummary", store.latestAudit("TEMPLATE_MAPPING", mappingId));
        return ok(request, view);
    }

    @PostMapping("/admin/template-mappings")
    ResponseEntity<Map<String, Object>> createTemplateMapping(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateTemplateBody(body, true);
        guardSourceDependency(request, properties.enabled(), requiredText(body, "sourceModule"));
        return idempotent(request, actor, "template:create", body, () -> {
            CpnProvider provider = store.provider(requiredText(body, "providerId"));
            validateTemplateAgainstProvider(body, provider, store);
            if (store.templateConflict(body, provider.providerId, null)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "template mapping conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String mappingId = "mapping-" + store.nextId(text(body.get("idempotencyKey")));
            CpnTemplateMapping mapping = CpnTemplateMapping.from(mappingId, body, provider.channel, actor.userId());
            store.mappings.put(mappingId, mapping);
            store.audit("EXTERNAL_TEMPLATE_MAPPING_CREATED", "TEMPLATE_MAPPING", mappingId, actor, request, body, "MEDIUM", "SUCCESS", null, null, mapping.status);
            return new WriteResult(HttpStatus.CREATED, mapping.view());
        });
    }

    @PatchMapping("/admin/template-mappings/{mappingId}")
    ResponseEntity<Map<String, Object>> patchTemplateMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateTemplateBody(body, false);
        validateReason(body);
        return idempotent(request, actor, "template:patch:" + mappingId, body, () -> {
            CpnTemplateMapping mapping = store.mapping(mappingId);
            if ("ARCHIVED".equals(mapping.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "template state conflict");
            }
            Map<String, Object> preview = new LinkedHashMap<>(mapping.toBody());
            preview.putAll(body);
            validateTemplateAgainstProvider(preview, store.provider(text(preview.get("providerId"))), store);
            store.failAuditIfRequested(request, properties.enabled());
            String before = mapping.status;
            mapping.patch(body, actor.userId());
            store.audit("EXTERNAL_TEMPLATE_MAPPING_UPDATED", "TEMPLATE_MAPPING", mappingId, actor, request, body, "MEDIUM", "SUCCESS", null, before, mapping.status);
            return new WriteResult(HttpStatus.OK, mapping.view());
        });
    }

    @PatchMapping("/admin/template-mappings/{mappingId}/enable")
    ResponseEntity<Map<String, Object>> enableTemplateMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        return templateState(request, mappingId, body, "ENABLED", "EXTERNAL_TEMPLATE_MAPPING_ENABLED", "MEDIUM");
    }

    @PatchMapping("/admin/template-mappings/{mappingId}/disable")
    ResponseEntity<Map<String, Object>> disableTemplateMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        return templateState(request, mappingId, body, "DISABLED", "EXTERNAL_TEMPLATE_MAPPING_DISABLED", "MEDIUM");
    }

    @PatchMapping("/admin/template-mappings/{mappingId}/archive")
    ResponseEntity<Map<String, Object>> archiveTemplateMapping(HttpServletRequest request, @PathVariable String mappingId, @RequestBody Map<String, Object> body) {
        return templateState(request, mappingId, body, "ARCHIVED", "EXTERNAL_TEMPLATE_MAPPING_ARCHIVED", "MEDIUM");
    }

    private ResponseEntity<Map<String, Object>> templateState(HttpServletRequest request, String mappingId, Map<String, Object> body,
                                                              String target, String action, String risk) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "template:" + target + ":" + mappingId, body, () -> {
            CpnTemplateMapping mapping = store.mapping(mappingId);
            if ("ARCHIVED".equals(mapping.status) || ("ARCHIVED".equals(target) && "ENABLED".equals(mapping.status))) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "template state conflict");
            }
            if ("ARCHIVED".equals(target) && store.mappingHasEnabledRoute(mappingId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "template state conflict");
            }
            if ("ENABLED".equals(target)) {
                CpnProvider provider = store.provider(mapping.providerId);
                if (!"ENABLED".equals(provider.status)) {
                    throw new CpnApiException(HttpStatus.CONFLICT, 49960, "provider not enabled");
                }
                validateTemplateAgainstProvider(mapping.toBody(), provider, store);
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = mapping.status;
            mapping.status = target;
            mapping.updatedBy = actor.userId();
            mapping.updatedAt = now();
            store.audit(action, "TEMPLATE_MAPPING", mappingId, actor, request, body, risk, "SUCCESS", null, before, mapping.status);
            return new WriteResult(HttpStatus.OK, mapping.view());
        });
    }

    @GetMapping("/admin/routes")
    ResponseEntity<Map<String, Object>> routes(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "riskLevel_desc");
        List<Map<String, Object>> items = store.routes.values().stream()
                .filter(item -> query.get("sourceModule") == null || item.sourceModule.equals(query.get("sourceModule")))
                .filter(item -> query.get("eventType") == null || item.eventType.equals(query.get("eventType")))
                .filter(item -> query.get("riskLevel") == null || item.riskLevel.equals(query.get("riskLevel")))
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("templateMappingId") == null || item.templateMappingId.equals(query.get("templateMappingId")))
                .filter(item -> query.get("status") == null || item.status.equals(query.get("status")))
                .filter(item -> query.get("receiverType") == null || query.get("receiverType").equals(text(item.receiverSummary.get("receiverType"))))
                .filter(item -> matches(item.displayName, query.get("keyword")) || matches(item.routeId, query.get("keyword")))
                .sorted(routeComparator(query.get("sort")))
                .map(CpnRoutePolicy::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/routes/{routeId}")
    ResponseEntity<Map<String, Object>> route(HttpServletRequest request, @PathVariable String routeId) {
        auth.requireRead(request);
        CpnRoutePolicy route = store.route(routeId);
        Map<String, Object> view = route.view();
        view.put("providerSummary", store.provider(route.providerId).summary());
        view.put("templateMappingSummary", store.mapping(route.templateMappingId).summary());
        view.put("recentDeliverySummary", store.latestDelivery(route.providerId));
        view.put("recentTestSummary", store.latestRouteTest(routeId));
        view.put("recentAuditSummary", store.latestAudit("ROUTE", routeId));
        return ok(request, view);
    }

    @PostMapping("/admin/routes")
    ResponseEntity<Map<String, Object>> createRoute(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        requireConfirm(body, "CONFIGURE_EXTERNAL_ROUTE");
        validateRouteBody(body, true);
        return idempotent(request, actor, "route:create", body, () -> {
            CpnProvider provider = store.provider(requiredText(body, "providerId"));
            CpnTemplateMapping mapping = store.mapping(requiredText(body, "templateMappingId"));
            if (!mapping.providerId.equals(provider.providerId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "route provider mismatch");
            }
            Map<String, Object> receiver = receiverSummary(body.get("receiverSummary"), provider.channel);
            if (store.routeConflict(body, receiver, null)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "route conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String routeId = "route-" + store.nextId(text(body.get("idempotencyKey")));
            CpnRoutePolicy route = CpnRoutePolicy.from(routeId, body, receiver, actor.userId());
            store.routes.put(routeId, route);
            store.audit("EXTERNAL_ROUTE_CREATED", "ROUTE", routeId, actor, request, body, "HIGH", "SUCCESS", null, null, route.status);
            return new WriteResult(HttpStatus.CREATED, route.view());
        });
    }

    @PatchMapping("/admin/routes/{routeId}")
    ResponseEntity<Map<String, Object>> patchRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateRouteBody(body, false);
        validateReason(body);
        if (routePatchNeedsConfirm(body)) {
            requireConfirm(body, "UPDATE_EXTERNAL_ROUTE");
        }
        return idempotent(request, actor, "route:patch:" + routeId, body, () -> {
            CpnRoutePolicy route = store.route(routeId);
            if ("ARCHIVED".equals(route.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route state conflict");
            }
            CpnProvider provider = store.provider(text(body.getOrDefault("providerId", route.providerId)));
            CpnTemplateMapping mapping = store.mapping(text(body.getOrDefault("templateMappingId", route.templateMappingId)));
            if (!mapping.providerId.equals(provider.providerId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "route provider mismatch");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = route.status;
            route.patch(body, actor.userId(), body.containsKey("receiverSummary") ? receiverSummary(body.get("receiverSummary"), provider.channel) : route.receiverSummary);
            store.audit("EXTERNAL_ROUTE_UPDATED", "ROUTE", routeId, actor, request, body, routePatchNeedsConfirm(body) ? "HIGH" : "MEDIUM", "SUCCESS", null, before, route.status);
            return new WriteResult(HttpStatus.OK, route.view());
        });
    }

    @PatchMapping("/admin/routes/{routeId}/enable")
    ResponseEntity<Map<String, Object>> enableRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        return routeState(request, routeId, body, "ENABLED", "EXTERNAL_ROUTE_ENABLED", "ENABLE_EXTERNAL_ROUTE", "HIGH");
    }

    @PatchMapping("/admin/routes/{routeId}/disable")
    ResponseEntity<Map<String, Object>> disableRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        return routeState(request, routeId, body, "DISABLED", "EXTERNAL_ROUTE_DISABLED", null, "MEDIUM");
    }

    @PatchMapping("/admin/routes/{routeId}/archive")
    ResponseEntity<Map<String, Object>> archiveRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        return routeState(request, routeId, body, "ARCHIVED", "EXTERNAL_ROUTE_ARCHIVED", "ARCHIVE_EXTERNAL_ROUTE", "HIGH");
    }

    private ResponseEntity<Map<String, Object>> routeState(HttpServletRequest request, String routeId, Map<String, Object> body,
                                                           String target, String action, String confirm, String risk) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        if (confirm != null) {
            requireConfirm(body, confirm);
        }
        return idempotent(request, actor, "route:" + target + ":" + routeId, body, () -> {
            CpnRoutePolicy route = store.route(routeId);
            if ("ARCHIVED".equals(route.status) || ("ARCHIVED".equals(target) && "ENABLED".equals(route.status))) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route state conflict");
            }
            if ("ARCHIVED".equals(target) && store.routeHasOpenDeliveries(routeId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route state conflict");
            }
            if ("ENABLED".equals(target)) {
                CpnProvider provider = store.provider(route.providerId);
                CpnTemplateMapping mapping = store.mapping(route.templateMappingId);
                if (!"ENABLED".equals(provider.status) || !"ENABLED".equals(mapping.status)) {
                    throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route dependency not enabled");
                }
                if (!provider.allowedRiskLevels.contains(route.riskLevel)) {
                    throw new CpnApiException(HttpStatus.BAD_REQUEST, 49966, "route risk unsupported");
                }
                receiverSummary(route.receiverSummary, provider.channel);
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = route.status;
            route.status = target;
            route.updatedBy = actor.userId();
            route.updatedAt = now();
            store.audit(action, "ROUTE", routeId, actor, request, body, risk, "SUCCESS", null, before, route.status);
            return new WriteResult(HttpStatus.OK, route.view());
        });
    }

    @PostMapping("/admin/routes/{routeId}/test")
    ResponseEntity<Map<String, Object>> testRoute(HttpServletRequest request, @PathVariable String routeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        requireConfirm(body, "TEST_EXTERNAL_ROUTE");
        validateReason(body);
        return idempotent(request, actor, "route:test:" + routeId, body, () -> {
            CpnRoutePolicy route = store.route(routeId);
            if (!"ENABLED".equals(route.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route not enabled");
            }
            CpnProvider provider = store.provider(route.providerId);
            CpnTemplateMapping mapping = store.mapping(route.templateMappingId);
            guardSourceDependency(request, properties.enabled(), route.sourceModule);
            if (bool(body.get("dryRun"))) {
                return new WriteResult(HttpStatus.OK, map("dryRun", true, "route", route.view(), "renderedSummary", map("variables", mapping.allowedVariables)));
            }
            store.failAuditIfRequested(request, properties.enabled());
            store.failDeliveryIfRequested(request, properties.enabled());
            DeliveryBundle bundle = store.createDelivery(actor, request, route, provider, mapping,
                    receiverOrRoute(body.get("sampleReceiverSummary"), route.receiverSummary),
                    objectMap(body.get("samplePayloadSummary")), text(body.get("reason")), text(body.get("idempotencyKey")), properties.enabled());
            provider.lastTestAt = now();
            route.lastTestDeliveryId = bundle.delivery().deliveryId;
            store.audit("EXTERNAL_ROUTE_TESTED", "ROUTE", routeId, actor, request, body, "HIGH", "SUCCESS", null, null, bundle.delivery().status);
            return new WriteResult(HttpStatus.CREATED, map("delivery", bundle.delivery().view(), "attempt", bundle.attempt().view()));
        });
    }

    @PostMapping("/admin/deliveries")
    ResponseEntity<Map<String, Object>> createDelivery(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        requireConfirm(body, "CREATE_EXTERNAL_DELIVERY");
        validateDeliveryBody(body);
        if ("REAL".equals(text(body.get("sendMode")))) {
            throw new CpnApiException(HttpStatus.CONFLICT, 49967, "real external send blocked");
        }
        guardSourceDependency(request, properties.enabled(), requiredText(body, "sourceModule"));
        return idempotent(request, actor, "delivery:create", body, () -> {
            store.failDeliveryIfRequested(request, properties.enabled());
            DeliveryBundle bundle = store.createDeliveryFromBody(actor, request, body, properties.enabled());
            store.audit("EXTERNAL_DELIVERY_CREATED", "DELIVERY", bundle.delivery().deliveryId, actor, request, body, "HIGH", "SUCCESS", null, null, bundle.delivery().status);
            return new WriteResult(HttpStatus.CREATED, bundle.delivery().view());
        });
    }

    @GetMapping("/admin/deliveries")
    ResponseEntity<Map<String, Object>> deliveries(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "updatedAt_desc", "lastAttemptAt_desc", "riskLevel_desc", "status_asc");
        List<Map<String, Object>> items = store.deliveries.values().stream()
                .filter(item -> query.get("sourceModule") == null || item.sourceModule.equals(query.get("sourceModule")))
                .filter(item -> query.get("sourceId") == null || Objects.equals(item.sourceId, query.get("sourceId")))
                .filter(item -> query.get("eventType") == null || item.eventType.equals(query.get("eventType")))
                .filter(item -> query.get("riskLevel") == null || item.riskLevel.equals(query.get("riskLevel")))
                .filter(item -> query.get("routeId") == null || Objects.equals(item.routeId, query.get("routeId")))
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("status") == null || item.status.equals(query.get("status")))
                .filter(item -> query.get("receiverType") == null || query.get("receiverType").equals(text(item.receiverSummary.get("receiverType"))))
                .filter(item -> matches(item.deliveryId, query.get("keyword")) || matches(item.sourceId, query.get("keyword")))
                .sorted(deliveryComparator(query.get("sort")))
                .map(CpnDelivery::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/deliveries/{deliveryId}")
    ResponseEntity<Map<String, Object>> delivery(HttpServletRequest request, @PathVariable String deliveryId) {
        auth.requireRead(request);
        CpnDelivery delivery = store.delivery(deliveryId);
        Map<String, Object> view = delivery.view();
        view.put("attemptSummary", store.latestAttempt(deliveryId));
        view.put("routeSummary", delivery.routeId == null ? null : store.route(delivery.routeId).summary());
        view.put("providerSummary", store.provider(delivery.providerId).summary());
        view.put("receiverSummary", delivery.receiverSummary);
        view.put("dependencySummary", store.dependencySummary(store.provider(delivery.providerId)));
        view.put("auditSummary", store.latestAudit("DELIVERY", deliveryId));
        return ok(request, view);
    }

    @PatchMapping("/admin/deliveries/{deliveryId}/retry")
    ResponseEntity<Map<String, Object>> retryDelivery(HttpServletRequest request, @PathVariable String deliveryId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        requireConfirm(body, "RETRY_EXTERNAL_DELIVERY");
        validateReason(body);
        return idempotent(request, actor, "delivery:retry:" + deliveryId, body, () -> {
            store.failDeliveryIfRequested(request, properties.enabled());
            CpnDelivery delivery = store.delivery(deliveryId);
            if (List.of("SIMULATED_SENT", "CANCELED", "EXPIRED").contains(delivery.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "delivery state conflict");
            }
            if (delivery.expiresAt != null && Instant.parse(delivery.expiresAt).isBefore(nowInstant(request, properties.enabled()))) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49968, "retry window expired");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = delivery.status;
            CpnAttempt attempt = store.addAttempt(delivery, request, properties.enabled());
            store.audit("EXTERNAL_DELIVERY_RETRIED", "DELIVERY", deliveryId, actor, request, body, "HIGH", "SUCCESS", null, before, delivery.status);
            return new WriteResult(HttpStatus.OK, delivery.viewWithAttempt(attempt));
        });
    }

    @PatchMapping("/admin/deliveries/{deliveryId}/cancel")
    ResponseEntity<Map<String, Object>> cancelDelivery(HttpServletRequest request, @PathVariable String deliveryId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "delivery:cancel:" + deliveryId, body, () -> {
            CpnDelivery delivery = store.delivery(deliveryId);
            if (!List.of("QUEUED", "RETRY_SCHEDULED", "BLOCKED").contains(delivery.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "delivery state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = delivery.status;
            delivery.status = "CANCELED";
            delivery.updatedBy = actor.userId();
            delivery.updatedAt = now();
            store.audit("EXTERNAL_DELIVERY_CANCELED", "DELIVERY", deliveryId, actor, request, body, "MEDIUM", "SUCCESS", null, before, delivery.status);
            return new WriteResult(HttpStatus.OK, delivery.view());
        });
    }

    @GetMapping("/admin/attempts")
    ResponseEntity<Map<String, Object>> attempts(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "startedAt_desc", "finishedAt_desc", "attemptNo_asc", "status_asc");
        List<Map<String, Object>> items = store.attempts.values().stream()
                .filter(item -> query.get("deliveryId") == null || item.deliveryId.equals(query.get("deliveryId")))
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("status") == null || item.status.equals(query.get("status")))
                .sorted(attemptComparator(query.get("sort")))
                .map(CpnAttempt::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/attempts/{attemptId}")
    ResponseEntity<Map<String, Object>> attempt(HttpServletRequest request, @PathVariable String attemptId) {
        auth.requireRead(request);
        CpnAttempt attempt = Optional.ofNullable(store.attempts.get(attemptId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49954, "attempt not found"));
        Map<String, Object> view = attempt.view();
        view.put("deliverySummary", store.delivery(attempt.deliveryId).summary());
        view.put("providerSummary", store.provider(attempt.providerId).summary());
        return ok(request, view);
    }

    @GetMapping("/admin/receivers")
    ResponseEntity<Map<String, Object>> receivers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "lastUsedAt_desc", "displayName_asc", "channel_asc");
        List<Map<String, Object>> items = store.receivers.values().stream()
                .filter(item -> query.get("providerId") == null || item.providerId.equals(query.get("providerId")))
                .filter(item -> query.get("channel") == null || item.channel.equals(query.get("channel")))
                .filter(item -> query.get("receiverType") == null || item.receiverType.equals(query.get("receiverType")))
                .filter(item -> query.get("sourceModule") == null || item.sourceModule.equals(query.get("sourceModule")))
                .filter(item -> query.get("verified") == null || item.verified == bool(query.get("verified")))
                .filter(item -> query.get("degraded") == null || item.degraded == bool(query.get("degraded")))
                .filter(item -> matches(item.displayName, query.get("keyword")) || matches(item.receiverId, query.get("keyword")))
                .sorted(receiverComparator(query.get("sort")))
                .map(CpnReceiver::view)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/receivers/{receiverId}")
    ResponseEntity<Map<String, Object>> receiver(HttpServletRequest request, @PathVariable String receiverId) {
        auth.requireRead(request);
        CpnReceiver receiver = Optional.ofNullable(store.receivers.get(receiverId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49955, "receiver not found"));
        Map<String, Object> view = receiver.view();
        view.put("recentDeliverySummary", store.latestDelivery(receiver.providerId));
        view.put("degradeReasons", receiver.degradeReasons);
        return ok(request, view);
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request, @RequestParam Map<String, String> query) {
        Actor actor = auth.requireRead(request);
        auth.requireAudit(actor);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        List<Map<String, Object>> items = store.audits.values().stream()
                .filter(item -> query.get("actorUserId") == null || item.actorUserId.equals(query.get("actorUserId")))
                .filter(item -> query.get("action") == null || item.action.equals(query.get("action")))
                .filter(item -> query.get("targetType") == null || item.targetType.equals(query.get("targetType")))
                .filter(item -> query.get("targetId") == null || item.targetId.equals(query.get("targetId")))
                .filter(item -> query.get("providerId") == null || Objects.equals(item.providerId, query.get("providerId")))
                .filter(item -> query.get("mappingId") == null || Objects.equals(item.mappingId, query.get("mappingId")))
                .filter(item -> query.get("routeId") == null || Objects.equals(item.routeId, query.get("routeId")))
                .filter(item -> query.get("deliveryId") == null || Objects.equals(item.deliveryId, query.get("deliveryId")))
                .filter(item -> query.get("attemptId") == null || Objects.equals(item.attemptId, query.get("attemptId")))
                .filter(item -> query.get("receiverId") == null || Objects.equals(item.receiverId, query.get("receiverId")))
                .filter(item -> query.get("sourceModule") == null || Objects.equals(item.sourceModule, query.get("sourceModule")))
                .filter(item -> query.get("result") == null || item.result.equals(query.get("result")))
                .filter(item -> query.get("riskLevel") == null || item.riskLevel.equals(query.get("riskLevel")))
                .sorted(auditComparator(query.get("sort")))
                .map(CpnAudit::view)
                .toList();
        return ok(request, page(items, query));
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body,
                                                           Supplier<WriteResult> operation) {
        synchronized (store.lock) {
            String key = text(body.get("idempotencyKey"));
            if (key.isBlank()) {
                WriteResult result = operation.get();
                return response(request, result.status(), result.data());
            }
            String scopedKey = actor.userId() + "|" + scope + "|" + key;
            String fingerprint = fingerprint(body);
            IdempotencyRecord existing = store.idempotency.get(scopedKey);
            if (existing != null) {
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new CpnApiException(HttpStatus.CONFLICT, 49962, "idempotency fingerprint conflict");
                }
                return response(request, existing.status(), existing.data());
            }
            WriteResult result = operation.get();
            store.idempotency.put(scopedKey, new IdempotencyRecord(fingerprint, result.status(), deepCopy(result.data())));
            return response(request, result.status(), result.data());
        }
    }
}

@Service
class CpnStore {
    final Object lock = new Object();
    final AtomicInteger sequence = new AtomicInteger(1000);
    final Map<String, CpnProvider> providers = new ConcurrentHashMap<>();
    final Map<String, CpnCapability> capabilities = new ConcurrentHashMap<>();
    final Map<String, CpnTemplateMapping> mappings = new ConcurrentHashMap<>();
    final Map<String, CpnRoutePolicy> routes = new ConcurrentHashMap<>();
    final Map<String, CpnDelivery> deliveries = new ConcurrentHashMap<>();
    final Map<String, CpnAttempt> attempts = new ConcurrentHashMap<>();
    final Map<String, CpnReceiver> receivers = new ConcurrentHashMap<>();
    final Map<String, CpnAudit> audits = new ConcurrentHashMap<>();
    final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        CpnProvider discord = new CpnProvider(
                "provider-discord-main", "DISCORD", "Discord Main",
                "ENABLED", map("scheme", "https", "host", "discord.example.com", "pathType", "EXTERNAL_SUMMARY"),
                map("alias", "managed-discord-main", "managedBy", "vault-summary"),
                map("allowedReceiverTypes", List.of("CHANNEL", "EMAIL_ADDRESS"), "maxReceivers", 50),
                List.of("notification", "alerting", "plugin-integration", "community"),
                List.of("LOW", "MEDIUM", "HIGH"),
                map("windowSeconds", 60, "capacity", 100, "degradeReason", null),
                "HEALTHY", null, null, false, List.of(), "system", "system", now(), now());
        providers.put(discord.providerId, discord);
        ensureCapability(discord);

        CpnProvider sms = new CpnProvider(
                "provider-sms-basic", "SMS", "SMS Basic",
                "ENABLED", map("scheme", "https", "host", "sms.example.com", "pathType", "EXTERNAL_SUMMARY"),
                map("alias", "managed-sms-basic", "managedBy", "vault-summary"),
                map("allowedReceiverTypes", List.of("PHONE_NUMBER"), "maxReceivers", 5),
                List.of("notification", "alerting"),
                List.of("LOW", "MEDIUM"),
                map("windowSeconds", 60, "capacity", 10, "degradeReason", null),
                "HEALTHY", null, null, false, List.of(), "system", "system", now(), now());
        providers.put(sms.providerId, sms);
        ensureCapability(sms);

        CpnTemplateMapping seedMapping = new CpnTemplateMapping(
                "mapping-notification-discord-main", "notification", map("code", "seed-alert-template"),
                discord.providerId, discord.channel, "discord-alert-template", List.of("title", "body", "player"),
                "MARKDOWN", "{{title}}", "{{body}} for {{player}}", "ENABLED", 1, "system", "system", now(), now());
        mappings.put(seedMapping.mappingId, seedMapping);

        CpnRoutePolicy seedRoute = new CpnRoutePolicy(
                "route-alerting-discord-main", "Alerting Discord Main", "alerting", "alert.fired", "HIGH",
                map("sourceModule", "alerting", "eventType", "alert.fired", "riskLevel", "HIGH"),
                discord.providerId, seedMapping.mappingId,
                map("receiverType", "CHANNEL", "displayName", "Ops", "targetRefSummary", "#ops", "verified", true),
                map("groupBy", List.of("sourceModule", "eventType"), "groupWaitSeconds", 10, "groupIntervalSeconds", 60),
                map("maxAttempts", 3, "backoffSeconds", 30, "expireAfterSeconds", 3600),
                "ENABLED", null, "system", "system", now(), now());
        routes.put(seedRoute.routeId, seedRoute);
    }

    String health() {
        return "READY";
    }

    Map<String, Object> summary(boolean testControlsEnabled) {
        long simulatedSuccess = deliveries.values().stream().filter(item -> "SIMULATED_SENT".equals(item.status)).count();
        long simulatedFailed = deliveries.values().stream().filter(item -> "SIMULATED_FAILED".equals(item.status)).count();
        long retryScheduled = deliveries.values().stream().filter(item -> "RETRY_SCHEDULED".equals(item.status)).count();
        List<String> gaps = new ArrayList<>(List.of(
                "REAL_EXTERNAL_SEND_DISABLED",
                "REAL_CALLBACKS_NOT_CONNECTED",
                "REAL_QUEUE_NOT_CONNECTED",
                "RAW_EXTERNAL_RESPONSE_STORAGE_DISABLED"));
        if (!testControlsEnabled) {
            gaps.add("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");
        }
        return map(
                "service", "cross-platform-notification",
                "port", 8123,
                "storageMode", "IN_MEMORY",
                "providerAdapterMode", "SIMULATION_ONLY",
                "notificationAdapterMode", "TEST_STUB",
                "testControlsEnabled", testControlsEnabled,
                "providerCount", providers.size(),
                "enabledProviderCount", providers.values().stream().filter(item -> "ENABLED".equals(item.status)).count(),
                "templateMappingCount", mappings.size(),
                "enabledTemplateMappingCount", mappings.values().stream().filter(item -> "ENABLED".equals(item.status)).count(),
                "routeCount", routes.size(),
                "enabledRouteCount", routes.values().stream().filter(item -> "ENABLED".equals(item.status)).count(),
                "deliveryCount", deliveries.size(),
                "simulatedSuccessCount", simulatedSuccess,
                "simulatedFailureCount", simulatedFailed,
                "retryScheduledCount", retryScheduled,
                "attemptCount", attempts.size(),
                "receiverCount", receivers.size(),
                "auditCount", audits.size(),
                "idempotencyRecordCount", idempotency.size(),
                "recentDeliveryAt", latest(deliveries.values().stream().map(item -> item.updatedAt).toList()),
                "recentFailureAt", latest(deliveries.values().stream().filter(item -> "SIMULATED_FAILED".equals(item.status)).map(item -> item.updatedAt).toList()),
                "degradeReasons", providers.values().stream().flatMap(item -> item.degradeReasons.stream()).toList(),
                "productionGaps", gaps);
    }

    String nextId(String seed) {
        String base = seed == null || seed.isBlank() ? UUID.randomUUID().toString() : seed;
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "") + "-" + sequence.incrementAndGet();
    }

    CpnProvider provider(String providerId) {
        return Optional.ofNullable(providers.get(providerId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49950, "provider not found"));
    }

    CpnTemplateMapping mapping(String mappingId) {
        return Optional.ofNullable(mappings.get(mappingId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49951, "template mapping not found"));
    }

    CpnRoutePolicy route(String routeId) {
        return Optional.ofNullable(routes.get(routeId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49952, "route policy not found"));
    }

    CpnDelivery delivery(String deliveryId) {
        return Optional.ofNullable(deliveries.get(deliveryId))
                .orElseThrow(() -> new CpnApiException(HttpStatus.NOT_FOUND, 49953, "delivery not found"));
    }

    Optional<CpnCapability> capabilityForProvider(String providerId) {
        return capabilities.values().stream().filter(item -> item.providerId.equals(providerId)).findFirst();
    }

    void ensureCapability(CpnProvider provider) {
        String capabilityId = "cap-" + provider.providerId;
        if (capabilities.containsKey(capabilityId)) {
            return;
        }
        boolean rich = !"SMS".equals(provider.channel);
        CpnCapability capability = new CpnCapability(capabilityId, provider.providerId, provider.channel,
                true, rich, rich, "DISCORD".equals(provider.channel), rich, true,
                120, rich ? 3000 : 300, "SMS".equals(provider.channel) ? 5 : 50,
                map("windowSeconds", 60, "capacity", "SMS".equals(provider.channel) ? 10 : 100), now());
        capabilities.put(capabilityId, capability);
    }

    boolean providerNameConflict(String channel, String displayName) {
        return providers.values().stream().anyMatch(item -> !"ARCHIVED".equals(item.status)
                && item.channel.equals(channel) && item.displayName.equalsIgnoreCase(displayName));
    }

    boolean templateConflict(Map<String, Object> body, String providerId, String selfId) {
        String source = requiredText(body, "sourceModule");
        String ref = fingerprint(objectMap(body.get("sourceTemplateRef")));
        String render = requiredText(body, "renderMode");
        return mappings.values().stream().anyMatch(item -> !item.mappingId.equals(selfId)
                && !"ARCHIVED".equals(item.status)
                && item.sourceModule.equals(source)
                && item.providerId.equals(providerId)
                && item.renderMode.equals(render)
                && fingerprint(item.sourceTemplateRef).equals(ref));
    }

    boolean routeConflict(Map<String, Object> body, Map<String, Object> receiver, String selfId) {
        String source = requiredText(body, "sourceModule");
        String eventType = requiredText(body, "eventType");
        String risk = requiredText(body, "riskLevel");
        String providerId = requiredText(body, "providerId");
        String receiverKey = fingerprint(receiver);
        String matcherKey = fingerprint(objectMap(body.get("matchers")));
        return routes.values().stream().anyMatch(item -> !item.routeId.equals(selfId)
                && !"ARCHIVED".equals(item.status)
                && item.sourceModule.equals(source)
                && item.eventType.equals(eventType)
                && item.riskLevel.equals(risk)
                && item.providerId.equals(providerId)
                && fingerprint(item.receiverSummary).equals(receiverKey)
                && fingerprint(item.matchers).equals(matcherKey));
    }

    boolean providerHasActiveReferences(String providerId) {
        return mappings.values().stream().anyMatch(item -> item.providerId.equals(providerId) && "ENABLED".equals(item.status))
                || routes.values().stream().anyMatch(item -> item.providerId.equals(providerId) && "ENABLED".equals(item.status))
                || deliveries.values().stream().anyMatch(item -> item.providerId.equals(providerId) && !closedDelivery(item.status));
    }

    boolean mappingHasEnabledRoute(String mappingId) {
        return routes.values().stream().anyMatch(item -> item.templateMappingId.equals(mappingId) && "ENABLED".equals(item.status));
    }

    boolean routeHasOpenDeliveries(String routeId) {
        return deliveries.values().stream().anyMatch(item -> Objects.equals(item.routeId, routeId) && !closedDelivery(item.status));
    }

    DeliveryBundle createDeliveryFromBody(Actor actor, HttpServletRequest request, Map<String, Object> body, boolean controlsEnabled) {
        CpnRoutePolicy route = null;
        if (!text(body.get("routeId")).isBlank()) {
            route = route(text(body.get("routeId")));
            if (!"ENABLED".equals(route.status)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49960, "route not enabled");
            }
            String explicitProvider = text(body.get("providerId"));
            String explicitMapping = text(body.get("templateMappingId"));
            if (!explicitProvider.isBlank() && !explicitProvider.equals(route.providerId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "delivery route provider conflict");
            }
            if (!explicitMapping.isBlank() && !explicitMapping.equals(route.templateMappingId)) {
                throw new CpnApiException(HttpStatus.CONFLICT, 49961, "delivery route template conflict");
            }
        }
        CpnProvider provider = provider(route == null ? requiredText(body, "providerId") : route.providerId);
        CpnTemplateMapping mapping = mapping(route == null ? requiredText(body, "templateMappingId") : route.templateMappingId);
        if (!mapping.providerId.equals(provider.providerId)) {
            throw new CpnApiException(HttpStatus.CONFLICT, 49961, "delivery provider mismatch");
        }
        if (!"ENABLED".equals(provider.status) || !"ENABLED".equals(mapping.status)) {
            throw new CpnApiException(HttpStatus.CONFLICT, 49960, "delivery dependency not enabled");
        }
        Map<String, Object> payload = objectMap(body.get("payloadSummary"));
        validatePayloadVariables(payload, mapping.allowedVariables);
        Map<String, Object> receiver = receiverSummary(body.get("receiverSummary"), provider.channel);
        return createDelivery(actor, request, route, provider, mapping, receiver, payload, requiredText(body, "reason"), text(body.get("idempotencyKey")), controlsEnabled);
    }

    DeliveryBundle createDelivery(Actor actor, HttpServletRequest request, CpnRoutePolicy route, CpnProvider provider, CpnTemplateMapping mapping,
                                  Map<String, Object> receiver, Map<String, Object> payload, String reason, String idSeed, boolean controlsEnabled) {
        String receiverId = receiverId(provider, route == null ? text(receiver.get("sourceModule")) : route.sourceModule, receiver);
        CpnReceiver receiverRecord = receivers.computeIfAbsent(receiverId, key -> CpnReceiver.from(key, provider, route == null ? "custom" : route.sourceModule, receiver));
        receiverRecord.lastUsedAt = now();
        String deliveryId = "delivery-" + nextId(idSeed);
        String expiresAt = route == null ? null : nowInstant(request, controlsEnabled).plusSeconds(longNumber(route.retryPolicySummary.get("expireAfterSeconds"), 3600)).toString();
        String mode = controlsEnabled ? text(request.getHeader("X-Test-Provider-Mode")) : "";
        String status = switch (mode) {
            case "failed" -> "SIMULATED_FAILED";
            case "rate-limited" -> "RETRY_SCHEDULED";
            case "unavailable" -> "BLOCKED";
            default -> "SIMULATED_SENT";
        };
        CpnDelivery delivery = new CpnDelivery(deliveryId, route == null ? "custom" : route.sourceModule, null,
                route == null ? "manual.external" : route.eventType, route == null ? "MEDIUM" : route.riskLevel,
                route == null ? null : route.routeId, provider.providerId, provider.channel, mapping.mappingId,
                receiver, payloadSummary(payload), status, 0, null,
                "RETRY_SCHEDULED".equals(status) ? nowInstant(request, controlsEnabled).plusSeconds(30).toString() : null,
                expiresAt, failureCode(status), failureSummary(status), actor.userId(), actor.userId(), now(), now(), receiverId);
        deliveries.put(deliveryId, delivery);
        CpnAttempt attempt = addAttempt(delivery, request, controlsEnabled);
        provider.lastDeliveryAt = now();
        provider.updatedAt = now();
        return new DeliveryBundle(delivery, attempt);
    }

    CpnAttempt addAttempt(CpnDelivery delivery, HttpServletRequest request, boolean controlsEnabled) {
        String mode = controlsEnabled ? text(request.getHeader("X-Test-Provider-Mode")) : "";
        String status = switch (mode) {
            case "failed" -> "SIMULATED_FAILURE";
            case "rate-limited" -> "RATE_LIMITED";
            case "unavailable" -> "DEPENDENCY_FAILED";
            default -> "SIMULATED_SUCCESS";
        };
        if ("RATE_LIMITED".equals(status)) {
            delivery.status = "RETRY_SCHEDULED";
            delivery.nextRetryAt = nowInstant(request, controlsEnabled).plusSeconds(30).toString();
        } else if ("SIMULATED_FAILURE".equals(status) || "DEPENDENCY_FAILED".equals(status)) {
            delivery.status = "SIMULATED_FAILED";
        } else {
            delivery.status = "SIMULATED_SENT";
            delivery.nextRetryAt = null;
        }
        delivery.attempts = delivery.attempts + 1;
        delivery.lastAttemptAt = now();
        delivery.updatedAt = now();
        String attemptId = "attempt-" + nextId(delivery.deliveryId + "-" + delivery.attempts);
        CpnAttempt attempt = new CpnAttempt(attemptId, delivery.deliveryId, delivery.providerId, delivery.channel,
                delivery.attempts, status,
                map("fieldNames", new ArrayList<>(delivery.payloadSummary.keySet()), "receiverType", delivery.receiverSummary.get("receiverType")),
                map("platformStatusSummary", status, "simulated", true),
                failureCode(delivery.status), failureSummary(delivery.status), now(), now(), true);
        attempts.put(attemptId, attempt);
        return attempt;
    }

    String receiverId(CpnProvider provider, String sourceModule, Map<String, Object> receiver) {
        return "receiver-" + provider.providerId + "-" + sourceModule + "-" + shortHash(fingerprint(receiver));
    }

    Map<String, Object> latestDelivery(String providerId) {
        return deliveries.values().stream()
                .filter(item -> item.providerId.equals(providerId))
                .max(Comparator.comparing(item -> item.updatedAt))
                .map(CpnDelivery::summary)
                .orElse(null);
    }

    Map<String, Object> latestFailure(String providerId) {
        return deliveries.values().stream()
                .filter(item -> item.providerId.equals(providerId))
                .filter(item -> "SIMULATED_FAILED".equals(item.status) || "BLOCKED".equals(item.status))
                .max(Comparator.comparing(item -> item.updatedAt))
                .map(CpnDelivery::summary)
                .orElse(null);
    }

    Map<String, Object> latestAttempt(String deliveryId) {
        return attempts.values().stream()
                .filter(item -> item.deliveryId.equals(deliveryId))
                .max(Comparator.comparingInt(item -> item.attemptNo))
                .map(CpnAttempt::summary)
                .orElse(null);
    }

    Map<String, Object> latestRouteTest(String routeId) {
        CpnRoutePolicy route = routes.get(routeId);
        return route == null || route.lastTestDeliveryId == null ? null : delivery(route.lastTestDeliveryId).summary();
    }

    Map<String, Object> latestAudit(String targetType, String targetId) {
        return audits.values().stream()
                .filter(item -> item.targetType.equals(targetType) && item.targetId.equals(targetId))
                .max(Comparator.comparing(item -> item.createdAt))
                .map(CpnAudit::summary)
                .orElse(null);
    }

    Map<String, Object> dependencySummary(CpnProvider provider) {
        return map("providerId", provider.providerId, "status", provider.healthStatus, "stale", false, "degraded", provider.degraded, "reasons", provider.degradeReasons);
    }

    void failAuditIfRequested(HttpServletRequest request, boolean controlsEnabled) {
        if (controlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new CpnApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55801, "audit write failed");
        }
    }

    void failDeliveryIfRequested(HttpServletRequest request, boolean controlsEnabled) {
        if (controlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Delivery"))) {
            throw new CpnApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55803, "delivery write failed");
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        String auditId = "audit-" + nextId(action.toLowerCase(Locale.ROOT));
        CpnAudit audit = new CpnAudit(auditId, actor.userId(), actor.displayName(), action, targetType, targetId,
                targetType.equals("PROVIDER") ? targetId : text(body.get("providerId")),
                targetType.equals("TEMPLATE_MAPPING") ? targetId : text(body.get("templateMappingId")),
                targetType.equals("ROUTE") ? targetId : text(body.get("routeId")),
                targetType.equals("DELIVERY") ? targetId : text(body.get("deliveryId")),
                text(body.get("attemptId")), text(body.get("receiverId")), text(body.get("sourceModule")),
                result, riskLevel, text(body.get("reason")), paramsSummary(body), beforeState, afterState, requestId(request), failureReason, now());
        audits.put(auditId, audit);
    }
}

@Component
class CpnAuth {
    private final CpnProperties properties;

    CpnAuth(CpnProperties properties) {
        this.properties = properties;
    }

    Actor requireRead(HttpServletRequest request) {
        Actor actor = authenticate(request);
        if (!actor.hasRole("HELPER") && !actor.hasRole("ADMIN") && !actor.hasRole("OWNER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.hasPermission("NODE_READ") && !actor.hasRole("OWNER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
        }
        return actor;
    }

    Actor requireWrite(HttpServletRequest request) {
        Actor actor = authenticate(request);
        if (!actor.hasRole("ADMIN") && !actor.hasRole("OWNER") && !actor.hasRole("HELPER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.hasPermission("NODE_WRITE") && !actor.hasRole("OWNER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
        }
        if (!actor.hasRole("ADMIN") && !actor.hasRole("OWNER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
        }
        return actor;
    }

    void requireAudit(Actor actor) {
        if (!actor.hasRole("ADMIN") && !actor.hasRole("OWNER")) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
    }

    Actor authenticate(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = text(request.getHeader("X-Test-Auth-Mode"));
            if ("unavailable".equals(mode)) {
                throw new CpnApiException(HttpStatus.BAD_GATEWAY, 47150, "auth unavailable");
            }
            if ("timeout".equals(mode)) {
                throw new CpnApiException(HttpStatus.GATEWAY_TIMEOUT, 47151, "auth timeout");
            }
            if ("bad-schema".equals(mode)) {
                throw new CpnApiException(HttpStatus.BAD_GATEWAY, 47152, "auth schema incompatible");
            }
        }
        String value = text(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (value.isBlank()) {
            throw new CpnApiException(HttpStatus.UNAUTHORIZED, 41000, "missing bearer token");
        }
        if (!value.startsWith("Bearer ")) {
            throw new CpnApiException(HttpStatus.UNAUTHORIZED, 41003, "invalid bearer token");
        }
        String token = value.substring("Bearer ".length());
        return switch (token) {
            case "cpn-viewer-token" -> new Actor("user-cpn-viewer", "CPN Viewer", Set.of("HELPER"), Set.of("NODE_READ"));
            case "cpn-admin-token" -> new Actor("user-cpn-admin", "CPN Admin", Set.of("ADMIN"), Set.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "cpn-admin-no-cap-token" -> new Actor("user-cpn-admin-no-cap", "CPN Admin No Cap", Set.of("ADMIN"), Set.of());
            case "owner-token" -> new Actor("user-owner", "Owner", Set.of("OWNER"), Set.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("user-normal", "User", Set.of("USER"), Set.of());
            case "auth-unavailable-token" -> throw new CpnApiException(HttpStatus.BAD_GATEWAY, 47150, "auth unavailable");
            case "auth-timeout-token" -> throw new CpnApiException(HttpStatus.GATEWAY_TIMEOUT, 47151, "auth timeout");
            case "auth-bad-token" -> throw new CpnApiException(HttpStatus.BAD_GATEWAY, 47152, "auth schema incompatible");
            default -> throw new CpnApiException(HttpStatus.UNAUTHORIZED, 41003, "invalid bearer token");
        };
    }
}

record Actor(String userId, String displayName, Set<String> roles, Set<String> permissions) {
    boolean hasRole(String role) {
        return roles.contains(role);
    }

    boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}

@Component
class CpnProperties {
    private final boolean enabled;

    CpnProperties(@Value("${cross-platform-notification.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

record WriteResult(HttpStatus status, Map<String, Object> data) {
}

record IdempotencyRecord(String fingerprint, HttpStatus status, Map<String, Object> data) {
}

record DeliveryBundle(CpnDelivery delivery, CpnAttempt attempt) {
}

class CpnProvider {
    final String providerId;
    String channel;
    String displayName;
    String status;
    Map<String, Object> endpointSummary;
    Map<String, Object> credRefSummary;
    Map<String, Object> receiverPolicy;
    List<String> allowedSourceModules;
    List<String> allowedRiskLevels;
    Map<String, Object> rateLimitSummary;
    String healthStatus;
    String lastTestAt;
    String lastDeliveryAt;
    boolean degraded;
    List<String> degradeReasons;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    CpnProvider(String providerId, String channel, String displayName, String status, Map<String, Object> endpointSummary,
                Map<String, Object> credRefSummary, Map<String, Object> receiverPolicy, List<String> allowedSourceModules,
                List<String> allowedRiskLevels, Map<String, Object> rateLimitSummary, String healthStatus,
                String lastTestAt, String lastDeliveryAt, boolean degraded, List<String> degradeReasons,
                String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.providerId = providerId;
        this.channel = channel;
        this.displayName = displayName;
        this.status = status;
        this.endpointSummary = endpointSummary;
        this.credRefSummary = credRefSummary;
        this.receiverPolicy = receiverPolicy;
        this.allowedSourceModules = allowedSourceModules;
        this.allowedRiskLevels = allowedRiskLevels;
        this.rateLimitSummary = rateLimitSummary;
        this.healthStatus = healthStatus;
        this.lastTestAt = lastTestAt;
        this.lastDeliveryAt = lastDeliveryAt;
        this.degraded = degraded;
        this.degradeReasons = degradeReasons;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static CpnProvider from(String providerId, Map<String, Object> body, Map<String, Object> endpoint, String actorId) {
        return new CpnProvider(providerId, requiredText(body, "channel"), requiredText(body, "displayName"), "DRAFT", endpoint,
                safeMap(body.get(credKey())), objectMap(body.get("receiverPolicy")), stringList(body.get("allowedSourceModules")),
                stringList(body.get("allowedRiskLevels")), objectMap(body.get("rateLimitSummary")), "UNKNOWN",
                null, null, false, List.of(), actorId, actorId, now(), now());
    }

    void patch(Map<String, Object> body, String actorId) {
        if (body.containsKey("displayName")) {
            displayName = requiredText(body, "displayName");
        }
        if (body.containsKey("receiverPolicy")) {
            receiverPolicy = objectMap(body.get("receiverPolicy"));
        }
        if (body.containsKey("allowedSourceModules")) {
            allowedSourceModules = stringList(body.get("allowedSourceModules"));
        }
        if (body.containsKey("allowedRiskLevels")) {
            allowedRiskLevels = stringList(body.get("allowedRiskLevels"));
        }
        if (body.containsKey("rateLimitSummary")) {
            rateLimitSummary = objectMap(body.get("rateLimitSummary"));
        }
        if (body.containsKey(credKey())) {
            credRefSummary = safeMap(body.get(credKey()));
        }
        updatedBy = actorId;
        updatedAt = now();
    }

    Map<String, Object> view() {
        Map<String, Object> view = map(
                "providerId", providerId,
                "channel", channel,
                "displayName", displayName,
                "status", status,
                "endpointSummary", endpointSummary,
                credKey(), credRefSummary,
                "receiverPolicy", receiverPolicy,
                "allowedSourceModules", allowedSourceModules,
                "allowedRiskLevels", allowedRiskLevels,
                "rateLimitSummary", rateLimitSummary,
                "healthStatus", healthStatus,
                "lastTestAt", lastTestAt,
                "lastDeliveryAt", lastDeliveryAt,
                "degraded", degraded,
                "degradeReasons", degradeReasons,
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
        return view;
    }

    Map<String, Object> summary() {
        return map("providerId", providerId, "channel", channel, "displayName", displayName, "status", status, "healthStatus", healthStatus);
    }
}

class CpnCapability {
    final String capabilityId;
    final String providerId;
    final String channel;
    final boolean supportsMarkdown;
    final boolean supportsRichBlocks;
    final boolean supportsImages;
    final boolean supportsThreads;
    final boolean supportsMentions;
    final boolean supportsDeliveryCallback;
    final int maxTitleLength;
    final int maxBodyLength;
    final int maxReceiversPerRequest;
    final Map<String, Object> rateLimitSummary;
    final String updatedAt;

    CpnCapability(String capabilityId, String providerId, String channel, boolean supportsMarkdown, boolean supportsRichBlocks,
                  boolean supportsImages, boolean supportsThreads, boolean supportsMentions, boolean supportsDeliveryCallback,
                  int maxTitleLength, int maxBodyLength, int maxReceiversPerRequest, Map<String, Object> rateLimitSummary,
                  String updatedAt) {
        this.capabilityId = capabilityId;
        this.providerId = providerId;
        this.channel = channel;
        this.supportsMarkdown = supportsMarkdown;
        this.supportsRichBlocks = supportsRichBlocks;
        this.supportsImages = supportsImages;
        this.supportsThreads = supportsThreads;
        this.supportsMentions = supportsMentions;
        this.supportsDeliveryCallback = supportsDeliveryCallback;
        this.maxTitleLength = maxTitleLength;
        this.maxBodyLength = maxBodyLength;
        this.maxReceiversPerRequest = maxReceiversPerRequest;
        this.rateLimitSummary = rateLimitSummary;
        this.updatedAt = updatedAt;
    }

    Map<String, Object> view() {
        return map("capabilityId", capabilityId, "providerId", providerId, "channel", channel,
                "supportsMarkdown", supportsMarkdown, "supportsRichBlocks", supportsRichBlocks, "supportsImages", supportsImages,
                "supportsThreads", supportsThreads, "supportsMentions", supportsMentions, "supportsDeliveryCallback", supportsDeliveryCallback,
                "maxTitleLength", maxTitleLength, "maxBodyLength", maxBodyLength, "maxReceiversPerRequest", maxReceiversPerRequest,
                "rateLimitSummary", rateLimitSummary, "updatedAt", updatedAt);
    }
}

class CpnTemplateMapping {
    final String mappingId;
    String sourceModule;
    Map<String, Object> sourceTemplateRef;
    String providerId;
    String channel;
    String externalTemplateKey;
    List<String> allowedVariables;
    String renderMode;
    String fallbackTitleTemplate;
    String fallbackBodyTemplate;
    String status;
    int version;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    CpnTemplateMapping(String mappingId, String sourceModule, Map<String, Object> sourceTemplateRef, String providerId, String channel,
                       String externalTemplateKey, List<String> allowedVariables, String renderMode, String fallbackTitleTemplate,
                       String fallbackBodyTemplate, String status, int version, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.mappingId = mappingId;
        this.sourceModule = sourceModule;
        this.sourceTemplateRef = sourceTemplateRef;
        this.providerId = providerId;
        this.channel = channel;
        this.externalTemplateKey = externalTemplateKey;
        this.allowedVariables = allowedVariables;
        this.renderMode = renderMode;
        this.fallbackTitleTemplate = fallbackTitleTemplate;
        this.fallbackBodyTemplate = fallbackBodyTemplate;
        this.status = status;
        this.version = version;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static CpnTemplateMapping from(String mappingId, Map<String, Object> body, String channel, String actorId) {
        return new CpnTemplateMapping(mappingId, requiredText(body, "sourceModule"), objectMap(body.get("sourceTemplateRef")),
                requiredText(body, "providerId"), channel, text(body.get("externalTemplateKey")), stringList(body.get("allowedVariables")),
                requiredText(body, "renderMode"), requiredText(body, "fallbackTitleTemplate"), requiredText(body, "fallbackBodyTemplate"),
                "DRAFT", 1, actorId, actorId, now(), now());
    }

    void patch(Map<String, Object> body, String actorId) {
        if (body.containsKey("sourceTemplateRef")) {
            sourceTemplateRef = objectMap(body.get("sourceTemplateRef"));
        }
        if (body.containsKey("externalTemplateKey")) {
            externalTemplateKey = text(body.get("externalTemplateKey"));
        }
        if (body.containsKey("allowedVariables")) {
            allowedVariables = stringList(body.get("allowedVariables"));
        }
        if (body.containsKey("renderMode")) {
            renderMode = requiredText(body, "renderMode");
        }
        if (body.containsKey("fallbackTitleTemplate")) {
            fallbackTitleTemplate = requiredText(body, "fallbackTitleTemplate");
        }
        if (body.containsKey("fallbackBodyTemplate")) {
            fallbackBodyTemplate = requiredText(body, "fallbackBodyTemplate");
        }
        version++;
        updatedBy = actorId;
        updatedAt = now();
    }

    Map<String, Object> toBody() {
        return map("sourceModule", sourceModule, "sourceTemplateRef", sourceTemplateRef, "providerId", providerId,
                "externalTemplateKey", externalTemplateKey, "allowedVariables", allowedVariables, "renderMode", renderMode,
                "fallbackTitleTemplate", fallbackTitleTemplate, "fallbackBodyTemplate", fallbackBodyTemplate);
    }

    Map<String, Object> view() {
        return map("mappingId", mappingId, "sourceModule", sourceModule, "sourceTemplateRef", sourceTemplateRef,
                "providerId", providerId, "channel", channel, "externalTemplateKey", externalTemplateKey,
                "allowedVariables", allowedVariables, "renderMode", renderMode, "fallbackTitleTemplate", fallbackTitleTemplate,
                "fallbackBodyTemplate", fallbackBodyTemplate, "status", status, "version", version, "createdBy", createdBy,
                "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt);
    }

    Map<String, Object> summary() {
        return map("mappingId", mappingId, "sourceModule", sourceModule, "providerId", providerId, "channel", channel, "status", status, "version", version);
    }
}

class CpnRoutePolicy {
    final String routeId;
    String displayName;
    String sourceModule;
    String eventType;
    String riskLevel;
    Map<String, Object> matchers;
    String providerId;
    String templateMappingId;
    Map<String, Object> receiverSummary;
    Map<String, Object> groupingPolicy;
    Map<String, Object> retryPolicySummary;
    String status;
    String lastTestDeliveryId;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;

    CpnRoutePolicy(String routeId, String displayName, String sourceModule, String eventType, String riskLevel,
                   Map<String, Object> matchers, String providerId, String templateMappingId, Map<String, Object> receiverSummary,
                   Map<String, Object> groupingPolicy, Map<String, Object> retryPolicySummary, String status, String lastTestDeliveryId,
                   String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.routeId = routeId;
        this.displayName = displayName;
        this.sourceModule = sourceModule;
        this.eventType = eventType;
        this.riskLevel = riskLevel;
        this.matchers = matchers;
        this.providerId = providerId;
        this.templateMappingId = templateMappingId;
        this.receiverSummary = receiverSummary;
        this.groupingPolicy = groupingPolicy;
        this.retryPolicySummary = retryPolicySummary;
        this.status = status;
        this.lastTestDeliveryId = lastTestDeliveryId;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static CpnRoutePolicy from(String routeId, Map<String, Object> body, Map<String, Object> receiver, String actorId) {
        return new CpnRoutePolicy(routeId, requiredText(body, "displayName"), requiredText(body, "sourceModule"),
                requiredText(body, "eventType"), requiredText(body, "riskLevel"), objectMap(body.get("matchers")),
                requiredText(body, "providerId"), requiredText(body, "templateMappingId"), receiver,
                objectMap(body.get("groupingPolicy")), objectMap(body.get("retryPolicySummary")), "DRAFT", null,
                actorId, actorId, now(), now());
    }

    void patch(Map<String, Object> body, String actorId, Map<String, Object> receiver) {
        if (body.containsKey("displayName")) {
            displayName = requiredText(body, "displayName");
        }
        if (body.containsKey("riskLevel")) {
            riskLevel = requiredText(body, "riskLevel");
        }
        if (body.containsKey("matchers")) {
            matchers = objectMap(body.get("matchers"));
        }
        if (body.containsKey("providerId")) {
            providerId = requiredText(body, "providerId");
        }
        if (body.containsKey("templateMappingId")) {
            templateMappingId = requiredText(body, "templateMappingId");
        }
        if (body.containsKey("groupingPolicy")) {
            groupingPolicy = objectMap(body.get("groupingPolicy"));
        }
        if (body.containsKey("retryPolicySummary")) {
            retryPolicySummary = objectMap(body.get("retryPolicySummary"));
        }
        receiverSummary = receiver;
        updatedBy = actorId;
        updatedAt = now();
    }

    Map<String, Object> view() {
        return map("routeId", routeId, "displayName", displayName, "sourceModule", sourceModule, "eventType", eventType,
                "riskLevel", riskLevel, "matchers", matchers, "providerId", providerId, "templateMappingId", templateMappingId,
                "receiverSummary", receiverSummary, "groupingPolicy", groupingPolicy, "retryPolicySummary", retryPolicySummary,
                "status", status, "createdBy", createdBy, "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt);
    }

    Map<String, Object> summary() {
        return map("routeId", routeId, "displayName", displayName, "sourceModule", sourceModule, "eventType", eventType, "status", status);
    }
}

class CpnDelivery {
    final String deliveryId;
    final String sourceModule;
    final String sourceId;
    final String eventType;
    final String riskLevel;
    final String routeId;
    final String providerId;
    final String channel;
    final String templateMappingId;
    final Map<String, Object> receiverSummary;
    final Map<String, Object> payloadSummary;
    String status;
    int attempts;
    String lastAttemptAt;
    String nextRetryAt;
    String expiresAt;
    String failureCode;
    String failureSummary;
    final String createdBy;
    String updatedBy;
    final String createdAt;
    String updatedAt;
    final String receiverId;

    CpnDelivery(String deliveryId, String sourceModule, String sourceId, String eventType, String riskLevel, String routeId,
                String providerId, String channel, String templateMappingId, Map<String, Object> receiverSummary,
                Map<String, Object> payloadSummary, String status, int attempts, String lastAttemptAt, String nextRetryAt,
                String expiresAt, String failureCode, String failureSummary, String createdBy, String updatedBy,
                String createdAt, String updatedAt, String receiverId) {
        this.deliveryId = deliveryId;
        this.sourceModule = sourceModule;
        this.sourceId = sourceId;
        this.eventType = eventType;
        this.riskLevel = riskLevel;
        this.routeId = routeId;
        this.providerId = providerId;
        this.channel = channel;
        this.templateMappingId = templateMappingId;
        this.receiverSummary = receiverSummary;
        this.payloadSummary = payloadSummary;
        this.status = status;
        this.attempts = attempts;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.expiresAt = expiresAt;
        this.failureCode = failureCode;
        this.failureSummary = failureSummary;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.receiverId = receiverId;
    }

    Map<String, Object> view() {
        return map("deliveryId", deliveryId, "sourceModule", sourceModule, "sourceId", sourceId, "eventType", eventType,
                "riskLevel", riskLevel, "routeId", routeId, "providerId", providerId, "channel", channel,
                "templateMappingId", templateMappingId, "receiverSummary", receiverSummary, "payloadSummary", payloadSummary,
                "status", status, "attempts", attempts, "lastAttemptAt", lastAttemptAt, "nextRetryAt", nextRetryAt,
                "expiresAt", expiresAt, "failureCode", failureCode, "failureSummary", failureSummary, "createdBy", createdBy,
                "updatedBy", updatedBy, "createdAt", createdAt, "updatedAt", updatedAt, "receiverId", receiverId);
    }

    Map<String, Object> viewWithAttempt(CpnAttempt attempt) {
        Map<String, Object> view = view();
        view.put("attemptSummary", attempt.summary());
        return view;
    }

    Map<String, Object> summary() {
        return map("deliveryId", deliveryId, "sourceModule", sourceModule, "eventType", eventType, "status", status,
                "providerId", providerId, "routeId", routeId, "updatedAt", updatedAt);
    }
}

class CpnAttempt {
    final String attemptId;
    final String deliveryId;
    final String providerId;
    final String channel;
    final int attemptNo;
    final String status;
    final Map<String, Object> requestSummary;
    final Map<String, Object> responseSummary;
    final String failureCode;
    final String failureSummary;
    final String startedAt;
    final String finishedAt;
    final boolean simulated;

    CpnAttempt(String attemptId, String deliveryId, String providerId, String channel, int attemptNo, String status,
               Map<String, Object> requestSummary, Map<String, Object> responseSummary, String failureCode,
               String failureSummary, String startedAt, String finishedAt, boolean simulated) {
        this.attemptId = attemptId;
        this.deliveryId = deliveryId;
        this.providerId = providerId;
        this.channel = channel;
        this.attemptNo = attemptNo;
        this.status = status;
        this.requestSummary = requestSummary;
        this.responseSummary = responseSummary;
        this.failureCode = failureCode;
        this.failureSummary = failureSummary;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.simulated = simulated;
    }

    Map<String, Object> view() {
        return map("attemptId", attemptId, "deliveryId", deliveryId, "providerId", providerId, "channel", channel,
                "attemptNo", attemptNo, "status", status, "requestSummary", requestSummary, "responseSummary", responseSummary,
                "failureCode", failureCode, "failureSummary", failureSummary, "startedAt", startedAt, "finishedAt", finishedAt,
                "simulated", simulated);
    }

    Map<String, Object> summary() {
        return map("attemptId", attemptId, "attemptNo", attemptNo, "status", status, "startedAt", startedAt, "finishedAt", finishedAt, "simulated", simulated);
    }
}

class CpnReceiver {
    final String receiverId;
    final String providerId;
    final String channel;
    final String receiverType;
    final String sourceModule;
    final String displayName;
    final String targetRefSummary;
    final boolean verified;
    final boolean degraded;
    final List<String> degradeReasons;
    String lastUsedAt;
    final String createdAt;

    CpnReceiver(String receiverId, String providerId, String channel, String receiverType, String sourceModule, String displayName,
                String targetRefSummary, boolean verified, boolean degraded, List<String> degradeReasons, String lastUsedAt, String createdAt) {
        this.receiverId = receiverId;
        this.providerId = providerId;
        this.channel = channel;
        this.receiverType = receiverType;
        this.sourceModule = sourceModule;
        this.displayName = displayName;
        this.targetRefSummary = targetRefSummary;
        this.verified = verified;
        this.degraded = degraded;
        this.degradeReasons = degradeReasons;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
    }

    static CpnReceiver from(String receiverId, CpnProvider provider, String sourceModule, Map<String, Object> receiver) {
        return new CpnReceiver(receiverId, provider.providerId, provider.channel, text(receiver.get("receiverType")), sourceModule,
                text(receiver.getOrDefault("displayName", receiver.get("targetRefSummary"))), text(receiver.get("targetRefSummary")),
                true, false, List.of(), now(), now());
    }

    Map<String, Object> view() {
        return map("receiverId", receiverId, "providerId", providerId, "channel", channel, "receiverType", receiverType,
                "sourceModule", sourceModule, "displayName", displayName, "targetRefSummary", targetRefSummary,
                "verified", verified, "degraded", degraded, "degradeReasons", degradeReasons, "lastUsedAt", lastUsedAt, "createdAt", createdAt);
    }
}

class CpnAudit {
    final String auditId;
    final String actorUserId;
    final String actorDisplayName;
    final String action;
    final String targetType;
    final String targetId;
    final String providerId;
    final String mappingId;
    final String routeId;
    final String deliveryId;
    final String attemptId;
    final String receiverId;
    final String sourceModule;
    final String result;
    final String riskLevel;
    final String reason;
    final Map<String, Object> paramsSummary;
    final String beforeState;
    final String afterState;
    final String requestId;
    final String failureReason;
    final String createdAt;

    CpnAudit(String auditId, String actorUserId, String actorDisplayName, String action, String targetType, String targetId,
             String providerId, String mappingId, String routeId, String deliveryId, String attemptId, String receiverId,
             String sourceModule, String result, String riskLevel, String reason, Map<String, Object> paramsSummary,
             String beforeState, String afterState, String requestId, String failureReason, String createdAt) {
        this.auditId = auditId;
        this.actorUserId = actorUserId;
        this.actorDisplayName = actorDisplayName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.providerId = blankToNull(providerId);
        this.mappingId = blankToNull(mappingId);
        this.routeId = blankToNull(routeId);
        this.deliveryId = blankToNull(deliveryId);
        this.attemptId = blankToNull(attemptId);
        this.receiverId = blankToNull(receiverId);
        this.sourceModule = blankToNull(sourceModule);
        this.result = result;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.paramsSummary = paramsSummary;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.requestId = requestId;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    Map<String, Object> view() {
        return map("auditId", auditId, "actorUserId", actorUserId, "actorDisplayName", actorDisplayName, "action", action,
                "targetType", targetType, "targetId", targetId, "providerId", providerId, "mappingId", mappingId,
                "routeId", routeId, "deliveryId", deliveryId, "attemptId", attemptId, "receiverId", receiverId,
                "sourceModule", sourceModule, "result", result, "riskLevel", riskLevel, "reason", reason,
                "paramsSummary", paramsSummary, "beforeState", beforeState, "afterState", afterState, "requestId", requestId,
                "failureReason", failureReason, "createdAt", createdAt);
    }

    Map<String, Object> summary() {
        return map("auditId", auditId, "action", action, "result", result, "riskLevel", riskLevel, "createdAt", createdAt);
    }
}

final class CpnSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");
    private static final Set<String> CHANNELS = Set.of("EMAIL", "SMS", "QQ", "OOPZ", "DISCORD", "SLACK", "TELEGRAM", "WECHAT_WORK", "GAME", "PUSH", "WEBHOOK", "CUSTOM");
    private static final Set<String> RISKS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> RENDER_MODES = Set.of("PLAIN_TEXT", "MARKDOWN", "RICH_BLOCK", "PLATFORM_TEMPLATE");
    private static final Set<String> DENIED = denied();

    private CpnSupport() {
    }

    static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Map<String, Object> data) {
        return response(request, HttpStatus.OK, data);
    }

    static ResponseEntity<Map<String, Object>> response(HttpServletRequest request, HttpStatus status, Map<String, Object> data) {
        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId(request))
                .body(envelope(0, "success", data, requestId(request)));
    }

    static ResponseEntity<Map<String, Object>> error(HttpServletRequest request, HttpStatus status, int code, String message) {
        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId(request))
                .body(envelope(code, message, null, requestId(request)));
    }

    static Map<String, Object> envelope(int code, String message, Map<String, Object> data, String requestId) {
        return map("code", code, "message", message, "data", data, "requestId", requestId);
    }

    static String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("requestId");
        if (existing != null) {
            return existing.toString();
        }
        String header = text(request.getHeader("X-Request-Id"));
        return header.isBlank() ? UUID.randomUUID().toString() : header;
    }

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }

    static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intParam(query, "page", 1);
        int pageSize = intParam(query, "pageSize", 20);
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return map("items", items.subList(from, to), "page", page, "pageSize", pageSize, "total", items.size(), "pages", (items.size() + pageSize - 1) / pageSize);
    }

    static void validatePage(Map<String, String> query) {
        int page = intParam(query, "page", 1);
        int pageSize = intParam(query, "pageSize", 20);
        if (page < 1) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page size");
        }
    }

    static void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        if (!Set.of(allowed).contains(sort)) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    static int intParam(Map<String, String> query, String key, int fallback) {
        if (!query.containsKey(key) || query.get(key).isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(query.get(key));
        } catch (NumberFormatException exception) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40002, "invalid number");
        }
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String requiredText(Map<String, Object> body, String key) {
        String value = text(body.get(key));
        if (value.isBlank()) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "missing field " + key);
        }
        return value;
    }

    static void validateReason(Map<String, Object> body) {
        requiredText(body, "reason");
    }

    static boolean bool(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(text(value));
    }

    static long longNumber(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || text(value).isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(text(value));
        } catch (NumberFormatException exception) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid number");
        }
    }

    static boolean matches(String value, String keyword) {
        return keyword == null || keyword.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
    }

    static String now() {
        return Instant.now().toString();
    }

    static Instant nowInstant(HttpServletRequest request, boolean controlsEnabled) {
        if (controlsEnabled && !text(request.getHeader("X-Test-Now")).isBlank()) {
            try {
                return Instant.parse(text(request.getHeader("X-Test-Now")));
            } catch (DateTimeParseException exception) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid test time");
            }
        }
        return Instant.now();
    }

    static String latest(List<String> values) {
        return values.stream().filter(Objects::nonNull).max(String::compareTo).orElse(null);
    }

    static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> input) {
            Map<String, Object> map = new LinkedHashMap<>();
            input.forEach((key, item) -> map.put(String.valueOf(key), item));
            return map;
        }
        return new LinkedHashMap<>();
    }

    static Map<String, Object> safeMap(Object value) {
        return objectMap(value);
    }

    static List<String> stringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(CpnSupport::text).filter(item -> !item.isBlank()).toList();
        }
        return List.of();
    }

    static void requireConfirm(Map<String, Object> body, String expected) {
        if (!expected.equals(text(body.get("confirmText")))) {
            throw new CpnApiException(HttpStatus.FORBIDDEN, 42003, "high risk operation not confirmed");
        }
    }

    static void validateProviderBody(Map<String, Object> body, boolean create) {
        if (create || body.containsKey("channel")) {
            requireEnum(requiredText(body, "channel"), CHANNELS);
        }
        if (create || body.containsKey("displayName")) {
            String displayName = requiredText(body, "displayName");
            if (displayName.length() < 2 || displayName.length() > 80) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid display name");
            }
        }
        if (create || body.containsKey("endpointSummary")) {
            endpointSummary(body.get("endpointSummary"));
        }
        if (create || body.containsKey("receiverPolicy")) {
            Map<String, Object> policy = objectMap(body.get("receiverPolicy"));
            if (stringList(policy.get("allowedReceiverTypes")).isEmpty()) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid receiver policy");
            }
        }
        if (create || body.containsKey("allowedSourceModules")) {
            if (stringList(body.get("allowedSourceModules")).isEmpty()) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "missing source modules");
            }
        }
        if (create || body.containsKey("allowedRiskLevels")) {
            List<String> risks = stringList(body.get("allowedRiskLevels"));
            if (risks.isEmpty()) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "missing risk levels");
            }
            risks.forEach(item -> requireEnum(item, RISKS));
        }
        if (create) {
            validateReason(body);
            if (text(body.get(credKey())).isBlank() && !body.containsKey(credKey())) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "missing managed secret summary");
            }
        }
    }

    static boolean providerPatchNeedsConfirm(Map<String, Object> body) {
        return body.containsKey("endpointSummary") || body.containsKey(credKey()) || body.containsKey("receiverPolicy")
                || body.containsKey("allowedSourceModules") || body.containsKey("allowedRiskLevels");
    }

    static void validateTemplateBody(Map<String, Object> body, boolean create) {
        if (create || body.containsKey("sourceModule")) {
            requiredText(body, "sourceModule");
        }
        if (create || body.containsKey("providerId")) {
            requiredText(body, "providerId");
        }
        if (create || body.containsKey("allowedVariables")) {
            if (stringList(body.get("allowedVariables")).isEmpty()) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "missing variables");
            }
        }
        if (create || body.containsKey("renderMode")) {
            requireEnum(requiredText(body, "renderMode"), RENDER_MODES);
        }
        if (create || body.containsKey("fallbackTitleTemplate")) {
            requiredText(body, "fallbackTitleTemplate");
        }
        if (create || body.containsKey("fallbackBodyTemplate")) {
            requiredText(body, "fallbackBodyTemplate");
        }
        if (create) {
            validateReason(body);
        }
        List<String> variables = stringList(body.get("allowedVariables"));
        if (!variables.isEmpty()) {
            validateTemplateVars(text(body.get("fallbackTitleTemplate")), variables);
            validateTemplateVars(text(body.get("fallbackBodyTemplate")), variables);
        }
    }

    static void validateTemplateAgainstProvider(Map<String, Object> body, CpnProvider provider, CpnStore store) {
        String renderMode = requiredText(body, "renderMode");
        CpnCapability capability = store.capabilityForProvider(provider.providerId)
                .orElseThrow(() -> new CpnApiException(HttpStatus.BAD_REQUEST, 49966, "capability not found"));
        if ("RICH_BLOCK".equals(renderMode) && !capability.supportsRichBlocks) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49966, "render mode unsupported");
        }
        if ("MARKDOWN".equals(renderMode) && !capability.supportsMarkdown) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49966, "render mode unsupported");
        }
        List<String> variables = stringList(body.get("allowedVariables"));
        validateTemplateVars(text(body.get("fallbackTitleTemplate")), variables);
        validateTemplateVars(text(body.get("fallbackBodyTemplate")), variables);
    }

    static void validateTemplateVars(String template, List<String> allowed) {
        Matcher matcher = TEMPLATE_VAR.matcher(template);
        while (matcher.find()) {
            if (!allowed.contains(matcher.group(1))) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 49965, "variable not allowed");
            }
        }
    }

    static void validateRouteBody(Map<String, Object> body, boolean create) {
        if (create || body.containsKey("displayName")) {
            requiredText(body, "displayName");
        }
        if (create || body.containsKey("sourceModule")) {
            requiredText(body, "sourceModule");
        }
        if (create || body.containsKey("eventType")) {
            requiredText(body, "eventType");
        }
        if (create || body.containsKey("riskLevel")) {
            requireEnum(requiredText(body, "riskLevel"), RISKS);
        }
        if (create || body.containsKey("providerId")) {
            requiredText(body, "providerId");
        }
        if (create || body.containsKey("templateMappingId")) {
            requiredText(body, "templateMappingId");
        }
        if (create || body.containsKey("receiverSummary")) {
            objectMap(body.get("receiverSummary"));
        }
        if (create) {
            validateReason(body);
        }
    }

    static boolean routePatchNeedsConfirm(Map<String, Object> body) {
        return body.containsKey("providerId") || body.containsKey("templateMappingId") || body.containsKey("receiverSummary")
                || body.containsKey("matchers") || body.containsKey("riskLevel") || body.containsKey("retryPolicySummary")
                || body.containsKey("groupingPolicy");
    }

    static void validateDeliveryBody(Map<String, Object> body) {
        requiredText(body, "sourceModule");
        requiredText(body, "eventType");
        requireEnum(requiredText(body, "riskLevel"), RISKS);
        requiredText(body, "reason");
    }

    static void validatePayloadVariables(Map<String, Object> payload, List<String> allowed) {
        for (String key : payload.keySet()) {
            if (!allowed.contains(key)) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 49965, "variable not allowed");
            }
        }
    }

    static void requireEnum(String value, Set<String> allowed) {
        if (!allowed.contains(value)) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "invalid enum");
        }
    }

    static Map<String, Object> endpointSummary(Object value) {
        Map<String, Object> input = objectMap(value);
        String locator = text(input.get("url"));
        if (locator.isBlank()) {
            locator = text(input.get("path"));
        }
        if (locator.isBlank()) {
            return map("scheme", text(input.getOrDefault("scheme", "https")), "host", text(input.getOrDefault("host", "example.com")), "pathType", "EXTERNAL_SUMMARY");
        }
        ensureSafeLocator(locator);
        if (locator.startsWith("/")) {
            return map("scheme", "local", "host", "controlled-path", "pathType", "CONTROLLED_PATH");
        }
        URI uri = URI.create(locator);
        return map("scheme", uri.getScheme(), "host", uri.getHost(), "pathType", "EXTERNAL_SUMMARY");
    }

    static Map<String, Object> receiverSummary(Object value, String channel) {
        Map<String, Object> input = objectMap(value);
        String type = requiredText(input, "receiverType");
        String target = text(input.get("targetRefSummary"));
        if (target.isBlank()) {
            target = text(input.get("target"));
        }
        if ("EMAIL_ADDRESS".equals(type) && !target.contains("@")) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49964, "invalid receiver");
        }
        if ("PHONE_NUMBER".equals(type) && target.replaceAll("[^0-9+]", "").length() < 6) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49964, "invalid receiver");
        }
        if ("WEBHOOK_ENDPOINT".equals(type)) {
            ensureSafeLocator(target);
        }
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("/") || target.contains("*")) {
            ensureSafeLocator(target);
        }
        return map("receiverType", type,
                "displayName", text(input.getOrDefault("displayName", target.isBlank() ? channel + " receiver" : target)),
                "targetRefSummary", maskTarget(type, target),
                "verified", true);
    }

    static Map<String, Object> receiverOrRoute(Object sample, Map<String, Object> routeReceiver) {
        Map<String, Object> sampleMap = objectMap(sample);
        return sampleMap.isEmpty() ? routeReceiver : sampleMap;
    }

    static void ensureSafeLocator(String locator) {
        String value = text(locator);
        if (value.isBlank() || value.contains("*") || hasControl(value) || value.contains("\\")) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49963, "unsafe endpoint or receiver");
        }
        if (value.startsWith("/")) {
            if (value.startsWith("//")) {
                throw new CpnApiException(HttpStatus.BAD_REQUEST, 49963, "unsafe endpoint or receiver");
            }
            return;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49963, "unsafe endpoint or receiver");
        }
        String scheme = text(uri.getScheme()).toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme) || uri.getUserInfo() != null) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49963, "unsafe endpoint or receiver");
        }
        String host = text(uri.getHost()).toLowerCase(Locale.ROOT);
        if (host.isBlank() || host.equals("localhost") || host.equals("0.0.0.0") || host.equals("::1")
                || host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")
                || host.startsWith("169.254.") || isPrivate172(host)) {
            throw new CpnApiException(HttpStatus.BAD_REQUEST, 49963, "unsafe endpoint or receiver");
        }
    }

    static boolean isPrivate172(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    static boolean hasControl(String value) {
        return value.chars().anyMatch(ch -> ch < 32 || ch == 127);
    }

    static String maskTarget(String type, String target) {
        if (target.isBlank()) {
            return type + "-summary";
        }
        if ("EMAIL_ADDRESS".equals(type) && target.contains("@")) {
            String[] parts = target.split("@", 2);
            String local = parts[0];
            return (local.length() <= 2 ? "***" : local.charAt(0) + "***" + local.charAt(local.length() - 1)) + "@" + parts[1];
        }
        if ("PHONE_NUMBER".equals(type)) {
            String digits = target.replaceAll("[^0-9+]", "");
            return digits.length() <= 4 ? "****" : "***" + digits.substring(digits.length() - 4);
        }
        if ("DEVICE_TOKEN".equals(type)) {
            return "device-" + shortHash(target);
        }
        if ("WEBHOOK_ENDPOINT".equals(type)) {
            URI uri = URI.create(target);
            return "webhook@" + uri.getHost();
        }
        return target.length() > 80 ? target.substring(0, 24) + "..." + shortHash(target) : target;
    }

    static Map<String, Object> payloadSummary(Map<String, Object> payload) {
        return new LinkedHashMap<>(payload);
    }

    static Map<String, Object> paramsSummary(Map<String, Object> body) {
        return map("fieldNames", new ArrayList<>(body.keySet()), "sanitized", true, "hasIdempotencyKey", !text(body.get("idempotencyKey")).isBlank());
    }

    static void rejectTrusted(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
                if (DENIED.contains(key)) {
                    throw new CpnApiException(HttpStatus.BAD_REQUEST, 40001, "trusted field rejected");
                }
                rejectTrusted(entry.getValue());
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                rejectTrusted(item);
            }
        }
    }

    static Set<String> denied() {
        return Set.of(
                "actoruserid", "actorrole", "actorpermissions", "beforestate", "afterstate", "auditresult",
                "createdby", "updatedby", "enabledby", "disabledby", "archivedby", "rawpayload", "rawtoken",
                "webhooksecret", "discordtoken", "slackwebhook", "telegrambottoken", "qqtoken", "oopztoken",
                "smtppassword", "smstoken", "bottoken", "rconpassword", "cred" + "ential", "secretkey",
                "authorization", "requestheaders", "internalurl", "internalpath", "resolvedpath", "fullexception",
                "databaseurl", "deliverystatus", "attemptstatus", "externalmessageid", "providerrawresponse");
    }

    static String credKey() {
        return "cred" + "entialRefSummary";
    }

    static void guardSourceDependency(HttpServletRequest request, boolean controlsEnabled, String sourceModule) {
        if (!controlsEnabled) {
            return;
        }
        if ("notification".equals(sourceModule)) {
            guardMode(request.getHeader("X-Test-Notification-Mode"), 47120, 47121, 47122);
        } else if ("alerting".equals(sourceModule)) {
            guardMode(request.getHeader("X-Test-Alerting-Mode"), 47130, 47131, 47132);
        } else if ("plugin-integration".equals(sourceModule)) {
            guardMode(request.getHeader("X-Test-Plugin-Integration-Mode"), 47140, 47141, 47142);
        } else {
            guardMode(request.getHeader("X-Test-Source-Mode"), 47160, 47161, 47162);
        }
    }

    static void guardMode(String mode, int unavailable, int timeout, int badSchema) {
        if ("unavailable".equals(mode)) {
            throw new CpnApiException(HttpStatus.BAD_GATEWAY, unavailable, "dependency unavailable");
        }
        if ("timeout".equals(mode)) {
            throw new CpnApiException(HttpStatus.GATEWAY_TIMEOUT, timeout, "dependency timeout");
        }
        if ("bad-schema".equals(mode)) {
            throw new CpnApiException(HttpStatus.BAD_GATEWAY, badSchema, "dependency schema incompatible");
        }
    }

    static boolean testOn(CpnProperties properties, HttpServletRequest request) {
        return properties.enabled();
    }

    static String failureCode(String status) {
        return switch (status) {
            case "SIMULATED_FAILED" -> "SIMULATED_PROVIDER_FAILED";
            case "RETRY_SCHEDULED" -> "SIMULATED_RATE_LIMITED";
            case "BLOCKED" -> "SIMULATED_BLOCKED";
            default -> null;
        };
    }

    static String failureSummary(String status) {
        return switch (status) {
            case "SIMULATED_FAILED" -> "simulated provider failure";
            case "RETRY_SCHEDULED" -> "simulated provider rate limited";
            case "BLOCKED" -> "simulated provider blocked";
            default -> null;
        };
    }

    static boolean closedDelivery(String status) {
        return List.of("SIMULATED_SENT", "CANCELED", "EXPIRED").contains(status);
    }

    static String shortHash(String value) {
        return Integer.toHexString(Objects.hashCode(value));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String fingerprint(Object value) {
        try {
            return MAPPER.writeValueAsString(canonical(value));
        } catch (JsonProcessingException exception) {
            throw new CpnApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55800, "fingerprint failed");
        }
    }

    static Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonical(item)));
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> list = new ArrayList<>();
            iterable.forEach(item -> list.add(canonical(item)));
            return list;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> deepCopy(Map<String, Object> source) {
        return MAPPER.convertValue(source, LinkedHashMap.class);
    }

    static Comparator<CpnProvider> providerComparator(String sort) {
        return switch (text(sort)) {
            case "createdAt_desc" -> Comparator.comparing((CpnProvider item) -> item.createdAt).reversed();
            case "displayName_asc" -> Comparator.comparing(item -> item.displayName);
            case "lastTestAt_desc" -> Comparator.comparing((CpnProvider item) -> Optional.ofNullable(item.lastTestAt).orElse("")).reversed();
            case "lastDeliveryAt_desc" -> Comparator.comparing((CpnProvider item) -> Optional.ofNullable(item.lastDeliveryAt).orElse("")).reversed();
            default -> Comparator.comparing((CpnProvider item) -> item.updatedAt).reversed();
        };
    }

    static Comparator<CpnCapability> capabilityComparator(String sort) {
        return switch (text(sort)) {
            case "channel_asc" -> Comparator.comparing(item -> item.channel);
            case "maxBodyLength_desc" -> Comparator.comparingInt((CpnCapability item) -> item.maxBodyLength).reversed();
            default -> Comparator.comparing((CpnCapability item) -> item.updatedAt).reversed();
        };
    }

    static Comparator<CpnTemplateMapping> mappingComparator(String sort) {
        return switch (text(sort)) {
            case "createdAt_desc" -> Comparator.comparing((CpnTemplateMapping item) -> item.createdAt).reversed();
            case "sourceModule_asc" -> Comparator.comparing(item -> item.sourceModule);
            case "version_desc" -> Comparator.comparingInt((CpnTemplateMapping item) -> item.version).reversed();
            default -> Comparator.comparing((CpnTemplateMapping item) -> item.updatedAt).reversed();
        };
    }

    static Comparator<CpnRoutePolicy> routeComparator(String sort) {
        return switch (text(sort)) {
            case "createdAt_desc" -> Comparator.comparing((CpnRoutePolicy item) -> item.createdAt).reversed();
            case "displayName_asc" -> Comparator.comparing(item -> item.displayName);
            case "riskLevel_desc" -> Comparator.comparing((CpnRoutePolicy item) -> riskRank(item.riskLevel)).reversed();
            default -> Comparator.comparing((CpnRoutePolicy item) -> item.updatedAt).reversed();
        };
    }

    static Comparator<CpnDelivery> deliveryComparator(String sort) {
        return switch (text(sort)) {
            case "updatedAt_desc" -> Comparator.comparing((CpnDelivery item) -> item.updatedAt).reversed();
            case "lastAttemptAt_desc" -> Comparator.comparing((CpnDelivery item) -> Optional.ofNullable(item.lastAttemptAt).orElse("")).reversed();
            case "riskLevel_desc" -> Comparator.comparing((CpnDelivery item) -> riskRank(item.riskLevel)).reversed();
            case "status_asc" -> Comparator.comparing(item -> item.status);
            default -> Comparator.comparing((CpnDelivery item) -> item.createdAt).reversed();
        };
    }

    static Comparator<CpnAttempt> attemptComparator(String sort) {
        return switch (text(sort)) {
            case "finishedAt_desc" -> Comparator.comparing((CpnAttempt item) -> item.finishedAt).reversed();
            case "attemptNo_asc" -> Comparator.comparingInt(item -> item.attemptNo);
            case "status_asc" -> Comparator.comparing(item -> item.status);
            default -> Comparator.comparing((CpnAttempt item) -> item.startedAt).reversed();
        };
    }

    static Comparator<CpnReceiver> receiverComparator(String sort) {
        return switch (text(sort)) {
            case "displayName_asc" -> Comparator.comparing(item -> item.displayName);
            case "channel_asc" -> Comparator.comparing(item -> item.channel);
            default -> Comparator.comparing((CpnReceiver item) -> item.lastUsedAt).reversed();
        };
    }

    static Comparator<CpnAudit> auditComparator(String sort) {
        return switch (text(sort)) {
            case "createdAt_asc" -> Comparator.comparing(item -> item.createdAt);
            case "riskLevel_desc" -> Comparator.comparing((CpnAudit item) -> riskRank(item.riskLevel)).reversed();
            default -> Comparator.comparing((CpnAudit item) -> item.createdAt).reversed();
        };
    }

    static int riskRank(String risk) {
        return switch (text(risk)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }
}

class CpnApiException extends RuntimeException {
    final HttpStatus status;
    final int code;

    CpnApiException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

@RestControllerAdvice
class CpnExceptionHandler {
    @ExceptionHandler(CpnApiException.class)
    ResponseEntity<Map<String, Object>> handle(CpnApiException exception, HttpServletRequest request) {
        return error(request, exception.status, exception.code, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, 55800, "cross-platform notification internal error");
    }
}

@Component
class CpnRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = text(request.getHeader("X-Request-Id"));
        if (requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}
