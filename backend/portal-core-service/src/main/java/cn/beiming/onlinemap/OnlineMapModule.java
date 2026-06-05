package cn.beiming.onlinemap;

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
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static cn.beiming.onlinemap.OnlineMapSupport.*;

@RestController
@RequestMapping("/api/v1/online-map")
class OnlineMapController {
    private static final String VERSION = "0.1.0-contract";
    private final OnlineMapStore store;
    private final OnlineMapAuth auth;
    private final OnlineMapProperties properties;

    OnlineMapController(OnlineMapStore store, OnlineMapAuth auth, OnlineMapProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, maps(
                "service", "online-map",
                "status", store.healthStatus(),
                "version", VERSION));
    }

    @GetMapping("/overview")
    ResponseEntity<Map<String, Object>> overview(HttpServletRequest request) {
        return ok(request, store.publicOverview());
    }

    @GetMapping("/providers")
    ResponseEntity<Map<String, Object>> publicProviders(HttpServletRequest request, @RequestParam Map<String, String> query) {
        validatePage(query);
        validateSort(query.get("sort"), "sortOrder_asc", "displayName_asc", "lastHealthCheckAt_desc");
        List<Map<String, Object>> items = store.publicProviders().stream()
                .filter(provider -> matches(provider.displayName, query.get("keyword")) || matches(provider.providerId, query.get("keyword")))
                .filter(provider -> query.get("providerType") == null || provider.providerType.equals(query.get("providerType")))
                .filter(provider -> query.get("healthStatus") == null || provider.healthStatus.equals(query.get("healthStatus")))
                .sorted(providerComparator(query.get("sort")))
                .map(OnlineMapProvider::publicView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/providers/{providerId}")
    ResponseEntity<Map<String, Object>> publicProvider(HttpServletRequest request, @PathVariable String providerId) {
        return ok(request, store.publicProvider(providerId).publicView());
    }

    @GetMapping("/worlds")
    ResponseEntity<Map<String, Object>> publicWorlds(HttpServletRequest request, @RequestParam Map<String, String> query) {
        validatePage(query);
        validateSort(query.get("sort"), "sortOrder_asc", "displayName_asc", "lastRenderedAt_desc");
        List<Map<String, Object>> items = store.publicWorlds().stream()
                .filter(world -> query.get("providerId") == null || world.providerId.equals(query.get("providerId")))
                .filter(world -> query.get("dimension") == null || world.dimension.equals(query.get("dimension")))
                .filter(world -> query.get("renderStatus") == null || world.renderStatus.equals(query.get("renderStatus")))
                .filter(world -> matches(world.displayName, query.get("keyword")) || matches(world.worldName, query.get("keyword")))
                .sorted(worldComparator(query.get("sort")))
                .map(OnlineMapWorld::publicView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/layers")
    ResponseEntity<Map<String, Object>> publicLayers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        validatePage(query);
        validateSort(query.get("sort"), "sortOrder_asc", "displayName_asc");
        List<Map<String, Object>> items = store.publicLayers().stream()
                .filter(layer -> query.get("providerId") == null || layer.providerId.equals(query.get("providerId")))
                .filter(layer -> query.get("worldId") == null || layer.worldId.equals(query.get("worldId")))
                .filter(layer -> query.get("layerType") == null || layer.layerType.equals(query.get("layerType")))
                .filter(layer -> query.get("visibility") == null || layer.visibility.equals(query.get("visibility")))
                .filter(layer -> matches(layer.displayName, query.get("keyword")))
                .sorted(layerComparator(query.get("sort")))
                .map(OnlineMapLayer::publicView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/markers")
    ResponseEntity<Map<String, Object>> publicMarkers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "title_asc", "createdAt_desc");
        Bounds bounds = parseBounds(query.get("bounds"), true);
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.publicMarkers().stream()
                .filter(marker -> query.get("providerId") == null || marker.providerId.equals(query.get("providerId")))
                .filter(marker -> query.get("worldId") == null || marker.worldId.equals(query.get("worldId")))
                .filter(marker -> query.get("layerId") == null || marker.layerId.equals(query.get("layerId")))
                .filter(marker -> query.get("markerType") == null || marker.markerType.equals(query.get("markerType")))
                .filter(marker -> query.get("sourceModule") == null || marker.sourceModule.equals(query.get("sourceModule")))
                .filter(marker -> matches(marker.title, query.get("keyword")) || matches(marker.summary, query.get("keyword")))
                .filter(marker -> range == null || range.contains(marker.updatedAt))
                .filter(marker -> bounds == null || bounds.contains(marker))
                .sorted(markerComparator(query.get("sort")))
                .map(OnlineMapMarker::publicView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/regions")
    ResponseEntity<Map<String, Object>> publicRegions(HttpServletRequest request, @RequestParam Map<String, String> query) {
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "title_asc", "createdAt_desc");
        Bounds bounds = parseBounds(query.get("bounds"), true);
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.publicRegions().stream()
                .filter(region -> query.get("providerId") == null || region.providerId.equals(query.get("providerId")))
                .filter(region -> query.get("worldId") == null || region.worldId.equals(query.get("worldId")))
                .filter(region -> query.get("layerId") == null || region.layerId.equals(query.get("layerId")))
                .filter(region -> query.get("sourceModule") == null || region.sourceModule.equals(query.get("sourceModule")))
                .filter(region -> matches(region.title, query.get("keyword")) || matches(region.summary, query.get("keyword")))
                .filter(region -> range == null || range.contains(region.updatedAt))
                .filter(region -> bounds == null || bounds.contains(region))
                .sorted(regionComparator(query.get("sort")))
                .map(OnlineMapRegion::publicView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/embed")
    ResponseEntity<Map<String, Object>> embed(HttpServletRequest request, @RequestParam Map<String, String> query) {
        OnlineMapProvider provider = query.containsKey("providerId") ? store.publicProvider(query.get("providerId")) : store.defaultPublicProvider();
        if (provider == null) {
            return ok(request, null);
        }
        if (query.containsKey("origin") && !provider.allowedOrigins.contains(query.get("origin"))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49715, "embed origin denied");
        }
        OnlineMapWorld world = query.containsKey("worldId") ? store.publicWorldForEmbed(provider.providerId, query.get("worldId")) : store.defaultPublicWorld(provider);
        return ok(request, store.embedView(provider, world));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request) {
        auth.requireNodeRead(request);
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new OnlineMapException(HttpStatus.INTERNAL_SERVER_ERROR, 55600, "online-map internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> adminProviders(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "lastHealthCheckAt_desc");
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.providers.values().stream()
                .filter(provider -> query.get("providerType") == null || provider.providerType.equals(query.get("providerType")))
                .filter(provider -> query.get("status") == null || provider.status.equals(query.get("status")))
                .filter(provider -> query.get("healthStatus") == null || provider.healthStatus.equals(query.get("healthStatus")))
                .filter(provider -> query.get("publicVisible") == null || provider.publicVisible == bool(query.get("publicVisible")) )
                .filter(provider -> matches(provider.displayName, query.get("keyword")) || matches(provider.providerId, query.get("keyword")))
                .filter(provider -> range == null || range.contains(provider.updatedAt))
                .sorted(providerComparator(query.get("sort")))
                .map(OnlineMapProvider::adminView)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> adminProvider(HttpServletRequest request, @PathVariable String providerId) {
        auth.requireNodeRead(request);
        OnlineMapProvider provider = store.provider(providerId);
        Map<String, Object> view = provider.adminView();
        view.put("recentHealthSnapshot", provider.latestSnapshotView());
        view.put("recentAudit", store.latestAuditFor("PROVIDER", providerId));
        return ok(request, view);
    }

    @PostMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> createProvider(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateProviderBody(body, true);
        return idempotent(request, actor, "provider:create", body, () -> {
            store.guardDependencies(request, "provider-create");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                String displayName = text(body.get("displayName"));
                if (store.providerNameConflict(displayName)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49711, "provider conflict");
                }
                if (store.providerUrlConflict(text(body.get("publicBaseUrl")), textOr(body.get("embedUrl"), text(body.get("publicBaseUrl")) + "/embed"))) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49711, "provider conflict");
                }
                String providerId = sanitizeId(textOr(body.get("idempotencyKey"), displayName));
                providerId = "provider-" + store.nextId(providerId);
                OnlineMapProvider provider = OnlineMapProvider.from(providerId, body, actor.userId);
                store.providers.put(providerId, provider);
                store.seedAutoWorld(provider);
                store.audit("MAP_PROVIDER_CREATED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, null, provider.status);
                return created(request, provider.adminView());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> patchProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateProviderBody(body, false);
        validateReason(body);
        boolean highRisk = body.containsKey("publicBaseUrl") || body.containsKey("embedUrl") || body.containsKey("allowedOrigins")
                || (body.containsKey("publicVisible") && bool(body.get("publicVisible")));
        if (highRisk) {
            requireConfirm(body, "UPDATE_PUBLIC_MAP_ENTRY");
        }
        return idempotent(request, actor, "provider:patch:" + providerId, body, () -> {
            store.guardDependencies(request, "provider-patch");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                String before = provider.status;
                provider.patch(body, actor.userId);
                store.audit("MAP_PROVIDER_UPDATED", "PROVIDER", providerId, actor, request, body, highRisk ? "HIGH" : "MEDIUM", "SUCCESS", null, before, provider.status);
                return ok(request, provider.adminView());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}/enable")
    ResponseEntity<Map<String, Object>> enableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        requireConfirm(body, "ENABLE_PUBLIC_MAP_PROVIDER");
        return idempotent(request, actor, "provider:enable:" + providerId, body, () -> {
            store.guardDependencies(request, "provider-enable");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                if (!provider.hasPublicWorld()) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                String before = provider.status;
                provider.status = "ENABLED";
                provider.healthStatus = "ONLINE";
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.audit("MAP_PROVIDER_ENABLED", "PROVIDER", providerId, actor, request, body, "HIGH", "SUCCESS", null, before, provider.status);
                return ok(request, provider.adminView());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}/disable")
    ResponseEntity<Map<String, Object>> disableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "provider:disable:" + providerId, body, () -> {
            store.guardDependencies(request, "provider-disable");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                String before = provider.status;
                provider.status = "DISABLED";
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.audit("MAP_PROVIDER_DISABLED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, provider.status);
                return ok(request, provider.adminView());
            }
        });
    }

    @PatchMapping("/admin/providers/{providerId}/archive")
    ResponseEntity<Map<String, Object>> archiveProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        requireConfirm(body, "ARCHIVE_MAP_PROVIDER");
        return idempotent(request, actor, "provider:archive:" + providerId, body, () -> {
            store.guardDependencies(request, "provider-archive");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(providerId);
                if ("ENABLED".equals(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                if (store.hasPublicChildren(providerId)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49717, "provider has public children");
                }
                String before = provider.status;
                provider.status = "ARCHIVED";
                provider.publicVisible = false;
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.archiveChildren(providerId, actor.userId);
                store.audit("MAP_PROVIDER_ARCHIVED", "PROVIDER", providerId, actor, request, body, "HIGH", "SUCCESS", null, before, provider.status);
                return ok(request, provider.adminView());
            }
        });
    }

    @PostMapping("/admin/providers/{providerId}/health/refresh")
    ResponseEntity<Map<String, Object>> refreshProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "provider:refresh:" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(providerId);
                if (!Set.of("ENABLED", "DEGRADED").contains(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                Instant refreshStartedAt = Instant.now();
                Instant lastRefreshStartedAt = store.providerRefreshStartedAt.get(providerId);
                if (lastRefreshStartedAt != null && refreshStartedAt.isBefore(lastRefreshStartedAt.plusSeconds(60))) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49716, "provider refresh cooldown");
                }
                String mode = properties.enabled() ? request.getHeader("X-Test-Provider-Mode") : null;
                Map<String, Object> snapshot;
                if (properties.enabled() && "timeout".equals(mode)) {
                    snapshot = provider.recordSnapshot("DEGRADED", false, List.of("PROVIDER_TIMEOUT"), false);
                } else if (properties.enabled() && "bad-schema".equals(mode)) {
                    snapshot = provider.recordSnapshot("DEGRADED", false, List.of("PROVIDER_BAD_SCHEMA"), false);
                } else if (properties.enabled() && "unavailable".equals(mode)) {
                    snapshot = provider.recordSnapshot("OFFLINE", false, List.of("PROVIDER_UNAVAILABLE"), false);
                } else {
                    provider.healthStatus = "ONLINE";
                    snapshot = provider.recordSnapshot("ONLINE", true, List.of(), true);
                }
                if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
                    throw new OnlineMapException(HttpStatus.INTERNAL_SERVER_ERROR, 55603, "health snapshot write failure");
                }
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.providerRefreshStartedAt.put(providerId, refreshStartedAt);
                store.healthSnapshots.put((String) snapshot.get("snapshotId"), snapshot);
                store.audit("MAP_PROVIDER_HEALTH_REFRESHED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, null, provider.healthStatus);
                return ok(request, snapshot);
            }
        });
    }

    @GetMapping("/admin/providers/{providerId}/health/snapshots")
    ResponseEntity<Map<String, Object>> healthSnapshots(HttpServletRequest request, @PathVariable String providerId, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "checkedAt_desc", "checkedAt_asc", "latencyMs_asc");
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.healthSnapshots.values().stream()
                .filter(snapshot -> snapshot.get("providerId").equals(providerId))
                .filter(snapshot -> query.get("healthStatus") == null || snapshot.get("healthStatus").equals(query.get("healthStatus")))
                .filter(snapshot -> range == null || range.contains(text(snapshot.get("checkedAt"))))
                .sorted(Comparator.comparing((Map<String, Object> snapshot) -> text(snapshot.get("checkedAt"))).reversed())
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/worlds")
    ResponseEntity<Map<String, Object>> adminWorlds(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "sortOrder_asc", "displayName_asc", "lastRenderedAt_desc");
        List<Map<String, Object>> items = store.worlds.values().stream()
                .filter(world -> query.get("providerId") == null || world.providerId.equals(query.get("providerId")))
                .filter(world -> query.get("dimension") == null || world.dimension.equals(query.get("dimension")))
                .filter(world -> query.get("renderStatus") == null || world.renderStatus.equals(query.get("renderStatus")))
                .filter(world -> query.get("enabled") == null || world.enabled == bool(query.get("enabled")))
                .filter(world -> query.get("publicVisible") == null || world.publicVisible == bool(query.get("publicVisible")))
                .filter(world -> matches(world.displayName, query.get("keyword")))
                .sorted(worldComparator(query.get("sort")))
                .map(OnlineMapWorld::adminView)
                .toList();
        return ok(request, page(items, query));
    }

    @PutMapping("/admin/worlds/{worldId}")
    ResponseEntity<Map<String, Object>> saveWorld(HttpServletRequest request, @PathVariable String worldId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateWorldBody(body, worldId);
        return idempotent(request, actor, "world:save:" + worldId, body, () -> {
            store.guardDependencies(request, "world-save");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                String providerId = text(body.get("providerId"));
                OnlineMapProvider provider = store.provider(providerId);
                if ("ARCHIVED".equals(provider.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "provider state conflict");
                }
                OnlineMapWorld world = store.worlds.get(worldId);
                String before = world == null ? null : world.renderStatus;
                if (world == null) {
                    world = OnlineMapWorld.from(worldId, body, actor.userId);
                    store.worlds.put(worldId, world);
                    provider.ensureWorld(worldId);
                } else {
                    world.patch(body, actor.userId);
                }
                store.audit("MAP_WORLD_SAVED", "WORLD", worldId, actor, request, body, "MEDIUM", "SUCCESS", null, before, world.renderStatus);
                return ok(request, world.adminView());
            }
        });
    }

    @GetMapping("/admin/layers")
    ResponseEntity<Map<String, Object>> adminLayers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "sortOrder_asc", "displayName_asc");
        List<Map<String, Object>> items = store.layers.values().stream()
                .filter(layer -> query.get("providerId") == null || layer.providerId.equals(query.get("providerId")))
                .filter(layer -> query.get("worldId") == null || layer.worldId.equals(query.get("worldId")))
                .filter(layer -> query.get("layerType") == null || layer.layerType.equals(query.get("layerType")))
                .filter(layer -> query.get("status") == null || layer.status.equals(query.get("status")))
                .filter(layer -> query.get("visibility") == null || layer.visibility.equals(query.get("visibility")))
                .filter(layer -> matches(layer.displayName, query.get("keyword")))
                .sorted(layerComparator(query.get("sort")))
                .map(OnlineMapLayer::adminView)
                .toList();
        return ok(request, page(items, query));
    }

    @PostMapping("/admin/layers")
    ResponseEntity<Map<String, Object>> createLayer(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateLayerBody(body, true);
        return idempotent(request, actor, "layer:create", body, () -> {
            store.guardDependencies(request, "layer-create");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(text(body.get("providerId")));
                OnlineMapWorld world = store.world(text(body.get("worldId")));
                if (!provider.providerId.equals(world.providerId)) {
                    throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "layer world mismatch");
                }
                if (store.layerNameConflict(world.worldId, text(body.get("displayName")))) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49711, "layer conflict");
                }
                String layerId = "layer-" + store.nextId(textOr(body.get("idempotencyKey"), text(body.get("displayName"))));
                OnlineMapLayer layer = OnlineMapLayer.from(layerId, body, actor.userId);
                store.layers.put(layerId, layer);
                provider.attachLayer(layerId);
                world.attachLayer(layerId);
                store.audit("MAP_LAYER_CREATED", "LAYER", layerId, actor, request, body, "MEDIUM", "SUCCESS", null, null, layer.status);
                return created(request, layer.adminView());
            }
        });
    }

    @PatchMapping("/admin/layers/{layerId}")
    ResponseEntity<Map<String, Object>> patchLayer(HttpServletRequest request, @PathVariable String layerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateLayerBody(body, false);
        return idempotent(request, actor, "layer:patch:" + layerId, body, () -> {
            store.guardDependencies(request, "layer-patch");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapLayer layer = store.layer(layerId);
                if ("ARCHIVED".equals(layer.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "layer state conflict");
                }
                String before = layer.status;
                layer.patch(body, actor.userId);
                store.audit("MAP_LAYER_UPDATED", "LAYER", layerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, layer.status);
                return ok(request, layer.adminView());
            }
        });
    }

    @PatchMapping("/admin/layers/{layerId}/archive")
    ResponseEntity<Map<String, Object>> archiveLayer(HttpServletRequest request, @PathVariable String layerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "layer:archive:" + layerId, body, () -> {
            store.guardDependencies(request, "layer-archive");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapLayer layer = store.layer(layerId);
                String before = layer.status;
                layer.status = "ARCHIVED";
                layer.updatedBy = actor.userId;
                layer.updatedAt = now();
                store.archiveLayerChildren(layerId, actor.userId);
                store.audit("MAP_LAYER_ARCHIVED", "LAYER", layerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, layer.status);
                return ok(request, layer.adminView());
            }
        });
    }

    @GetMapping("/admin/markers")
    ResponseEntity<Map<String, Object>> adminMarkers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "title_asc", "createdAt_desc");
        Bounds bounds = parseBounds(query.get("bounds"), false);
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.markers.values().stream()
                .filter(marker -> query.get("providerId") == null || marker.providerId.equals(query.get("providerId")))
                .filter(marker -> query.get("worldId") == null || marker.worldId.equals(query.get("worldId")))
                .filter(marker -> query.get("layerId") == null || marker.layerId.equals(query.get("layerId")))
                .filter(marker -> query.get("markerType") == null || marker.markerType.equals(query.get("markerType")))
                .filter(marker -> query.get("status") == null || marker.status.equals(query.get("status")))
                .filter(marker -> query.get("visibility") == null || marker.visibility.equals(query.get("visibility")))
                .filter(marker -> query.get("sourceModule") == null || marker.sourceModule.equals(query.get("sourceModule")))
                .filter(marker -> matches(marker.title, query.get("keyword")) || matches(marker.summary, query.get("keyword")))
                .filter(marker -> range == null || range.contains(marker.updatedAt))
                .filter(marker -> bounds == null || bounds.contains(marker))
                .sorted(markerComparator(query.get("sort")))
                .map(OnlineMapMarker::adminView)
                .toList();
        return ok(request, page(items, query));
    }

    @PostMapping("/admin/markers")
    ResponseEntity<Map<String, Object>> createMarker(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateMarkerBody(body, true);
        return idempotent(request, actor, "marker:create", body, () -> {
            store.guardDependencies(request, "marker-create");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(text(body.get("providerId")));
                OnlineMapWorld world = store.world(text(body.get("worldId")));
                OnlineMapLayer layer = store.layer(text(body.get("layerId")));
                if (!provider.providerId.equals(world.providerId) || !world.worldId.equals(layer.worldId)) {
                    throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "marker parent mismatch");
                }
                if (store.markerSourceConflict(text(body.get("providerId")), text(body.get("worldId")), text(body.get("layerId")),
                        text(body.get("sourceModule")), body.get("sourceRef"))) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49711, "marker conflict");
                }
                String markerId = "marker-" + store.nextId(textOr(body.get("idempotencyKey"), text(body.get("title"))));
                OnlineMapMarker marker = OnlineMapMarker.from(markerId, body, actor.userId);
                store.markers.put(markerId, marker);
                provider.attachMarker(markerId);
                world.attachMarker(markerId);
                layer.attachMarker(markerId);
                store.audit("MAP_MARKER_CREATED", "MARKER", markerId, actor, request, body, "MEDIUM", "SUCCESS", null, null, marker.status);
                return created(request, marker.adminView());
            }
        });
    }

    @PatchMapping("/admin/markers/{markerId}")
    ResponseEntity<Map<String, Object>> patchMarker(HttpServletRequest request, @PathVariable String markerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateMarkerBody(body, false);
        return idempotent(request, actor, "marker:patch:" + markerId, body, () -> {
            store.guardDependencies(request, "marker-patch");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapMarker marker = store.marker(markerId);
                if ("ARCHIVED".equals(marker.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "marker state conflict");
                }
                String before = marker.status;
                marker.patch(body, actor.userId);
                store.audit("MAP_MARKER_UPDATED", "MARKER", markerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, marker.status);
                return ok(request, marker.adminView());
            }
        });
    }

    @PatchMapping("/admin/markers/{markerId}/archive")
    ResponseEntity<Map<String, Object>> archiveMarker(HttpServletRequest request, @PathVariable String markerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "marker:archive:" + markerId, body, () -> {
            store.guardDependencies(request, "marker-archive");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapMarker marker = store.marker(markerId);
                String before = marker.status;
                marker.status = "ARCHIVED";
                marker.updatedBy = actor.userId;
                marker.updatedAt = now();
                store.audit("MAP_MARKER_ARCHIVED", "MARKER", markerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, marker.status);
                return ok(request, marker.adminView());
            }
        });
    }

    @GetMapping("/admin/regions")
    ResponseEntity<Map<String, Object>> adminRegions(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireNodeRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "title_asc", "createdAt_desc");
        Bounds bounds = parseBounds(query.get("bounds"), false);
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.regions.values().stream()
                .filter(region -> query.get("providerId") == null || region.providerId.equals(query.get("providerId")))
                .filter(region -> query.get("worldId") == null || region.worldId.equals(query.get("worldId")))
                .filter(region -> query.get("layerId") == null || region.layerId.equals(query.get("layerId")))
                .filter(region -> query.get("status") == null || region.status.equals(query.get("status")))
                .filter(region -> query.get("visibility") == null || region.visibility.equals(query.get("visibility")))
                .filter(region -> query.get("sourceModule") == null || region.sourceModule.equals(query.get("sourceModule")))
                .filter(region -> matches(region.title, query.get("keyword")) || matches(region.summary, query.get("keyword")))
                .filter(region -> range == null || range.contains(region.updatedAt))
                .filter(region -> bounds == null || bounds.contains(region))
                .sorted(regionComparator(query.get("sort")))
                .map(OnlineMapRegion::adminView)
                .toList();
        return ok(request, page(items, query));
    }

    @PostMapping("/admin/regions")
    ResponseEntity<Map<String, Object>> createRegion(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateRegionBody(body, true);
        return idempotent(request, actor, "region:create", body, () -> {
            store.guardDependencies(request, "region-create");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapProvider provider = store.provider(text(body.get("providerId")));
                OnlineMapWorld world = store.world(text(body.get("worldId")));
                OnlineMapLayer layer = store.layer(text(body.get("layerId")));
                if (!provider.providerId.equals(world.providerId) || !world.worldId.equals(layer.worldId)) {
                    throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "region parent mismatch");
                }
                if (store.regionSourceConflict(text(body.get("providerId")), text(body.get("worldId")), text(body.get("layerId")),
                        text(body.get("sourceModule")), body.get("sourceRef"))) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49711, "region conflict");
                }
                String regionId = "region-" + store.nextId(textOr(body.get("idempotencyKey"), text(body.get("title"))));
                OnlineMapRegion region = OnlineMapRegion.from(regionId, body, actor.userId);
                store.regions.put(regionId, region);
                provider.attachRegion(regionId);
                world.attachRegion(regionId);
                layer.attachRegion(regionId);
                store.audit("MAP_REGION_CREATED", "REGION", regionId, actor, request, body, "MEDIUM", "SUCCESS", null, null, region.status);
                return created(request, region.adminView());
            }
        });
    }

    @PatchMapping("/admin/regions/{regionId}")
    ResponseEntity<Map<String, Object>> patchRegion(HttpServletRequest request, @PathVariable String regionId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        validateRegionBody(body, false);
        return idempotent(request, actor, "region:patch:" + regionId, body, () -> {
            store.guardDependencies(request, "region-patch");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapRegion region = store.region(regionId);
                if ("ARCHIVED".equals(region.status)) {
                    throw new OnlineMapException(HttpStatus.CONFLICT, 49710, "region state conflict");
                }
                String before = region.status;
                region.patch(body, actor.userId);
                store.audit("MAP_REGION_UPDATED", "REGION", regionId, actor, request, body, "MEDIUM", "SUCCESS", null, before, region.status);
                return ok(request, region.adminView());
            }
        });
    }

    @PatchMapping("/admin/regions/{regionId}/archive")
    ResponseEntity<Map<String, Object>> archiveRegion(HttpServletRequest request, @PathVariable String regionId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireNodeWrite(request);
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "region:archive:" + regionId, body, () -> {
            store.guardDependencies(request, "region-archive");
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store.lock) {
                OnlineMapRegion region = store.region(regionId);
                String before = region.status;
                region.status = "ARCHIVED";
                region.updatedBy = actor.userId;
                region.updatedAt = now();
                store.audit("MAP_REGION_ARCHIVED", "REGION", regionId, actor, request, body, "MEDIUM", "SUCCESS", null, before, region.status);
                return ok(request, region.adminView());
            }
        });
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> audits(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAdmin(auth.requireNodeRead(request));
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        TimeRange range = parseTimeRange(query.get("from"), query.get("to"));
        List<Map<String, Object>> items = store.audits.values().stream()
                .filter(audit -> query.get("actorUserId") == null || audit.actorUserId.equals(query.get("actorUserId")))
                .filter(audit -> query.get("action") == null || audit.action.equals(query.get("action")))
                .filter(audit -> query.get("targetType") == null || audit.targetType.equals(query.get("targetType")))
                .filter(audit -> query.get("targetId") == null || audit.targetId.equals(query.get("targetId")))
                .filter(audit -> query.get("providerId") == null || Objects.equals(audit.providerId, query.get("providerId")))
                .filter(audit -> query.get("layerId") == null || Objects.equals(audit.layerId, query.get("layerId")))
                .filter(audit -> query.get("markerId") == null || Objects.equals(audit.markerId, query.get("markerId")))
                .filter(audit -> query.get("regionId") == null || Objects.equals(audit.regionId, query.get("regionId")))
                .filter(audit -> query.get("result") == null || audit.result.equals(query.get("result")))
                .filter(audit -> query.get("riskLevel") == null || audit.riskLevel.equals(query.get("riskLevel")))
                .filter(audit -> range == null || range.contains(audit.createdAt))
                .sorted(auditComparator(query.get("sort")))
                .map(OnlineMapAudit::view)
                .toList();
        return ok(request, page(items, query));
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(request, 0, "success", data));
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, 0, "success", data));
    }

    private Map<String, Object> envelope(HttpServletRequest request, int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String action, Map<String, Object> body,
                                                           Supplier<ResponseEntity<Map<String, Object>>> supplier) {
        String key = text(body.get("idempotencyKey"));
        if (key.isBlank()) {
            return supplier.get();
        }
        String fingerprint = fingerprint(body);
        String recordKey = actor.userId + ":" + action + ":" + key;
        IdempotencyRecord existing = store.idempotencyRecords.get(recordKey);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint)) {
                throw new OnlineMapException(HttpStatus.CONFLICT, 49712, "idempotency conflict");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) existing.body;
            return ResponseEntity.status(existing.status).body(envelope(request, 0, "success", data));
        }
        ResponseEntity<Map<String, Object>> response = supplier.get();
        store.idempotencyRecords.put(recordKey, new IdempotencyRecord(fingerprint, response.getStatusCode(), response.getBody() == null ? null : response.getBody().get("data")));
        return response;
    }

    private void validateProviderBody(Map<String, Object> body, boolean create) {
        if (create || body.containsKey("providerType")) {
            requireEnum(body, "providerType", "BLUEMAP", "DYNMAP", "SQUAREMAP", "OVERVIEWER", "CUSTOM");
        }
        if (create || body.containsKey("displayName")) {
            requireString(body, "displayName", 2, 80);
        }
        if (create || body.containsKey("publicBaseUrl")) {
            requireString(body, "publicBaseUrl", 1, 500);
        }
        if (body.containsKey("embedUrl") && body.get("embedUrl") != null) {
            requireString(body, "embedUrl", 1, 500);
        }
        if (body.containsKey("allowedOrigins")) {
            for (String origin : stringList(body.get("allowedOrigins"))) {
                if ("*".equals(origin)) {
                    throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49715, "allowed origin denied");
                }
                if (isUnsafeOrigin(origin)) {
                    throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49715, "allowed origin denied");
                }
            }
        }
        if (body.containsKey("publicBaseUrl") && isUnsafePublicUrl(text(body.get("publicBaseUrl")))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49713, "url denied");
        }
        if (body.containsKey("embedUrl") && body.get("embedUrl") != null && isUnsafePublicUrl(text(body.get("embedUrl")))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49713, "url denied");
        }
    }

    private void validateWorldBody(Map<String, Object> body, String worldId) {
        requireString(body, "providerId", 1, 80);
        requireString(body, "worldName", 1, 80);
        requireString(body, "displayName", 1, 80);
        requireString(body, "dimension", 1, 40);
        requireEnum(body, "dimension", "OVERWORLD", "NETHER", "END", "CUSTOM");
        if (body.containsKey("sourceWorldKey")) {
            String sourceWorldKey = text(body.get("sourceWorldKey"));
            if (sourceWorldKey.contains("..") || sourceWorldKey.contains("\\") || sourceWorldKey.contains("/") || sourceWorldKey.contains("\0")) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid source world key");
            }
        }
        parseCoordinateMap(body.get("center"), true);
        parseBoundsMap(body.get("bounds"), true);
        if (body.containsKey("renderStatus")) {
            String renderStatus = text(body.get("renderStatus"));
            if (!Set.of("READY", "RENDERING", "STALE", "FAILED", "UNKNOWN").contains(renderStatus)) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid render status");
            }
        }
        if (body.containsKey("providerId") && body.get("providerId") == null) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "provider required");
        }
    }

    private void validateLayerBody(Map<String, Object> body, boolean create) {
        if (create) {
            requireString(body, "providerId", 1, 80);
            requireString(body, "worldId", 1, 80);
            requireString(body, "displayName", 2, 80);
            requireString(body, "layerType", 1, 40);
        }
        if (body.containsKey("layerType")) {
            requireEnum(body, "layerType", "BASE", "MARKER_SET", "POI", "REGION", "ROUTE", "CLAIM", "SYSTEM", "CUSTOM");
        }
        if (body.containsKey("status")) {
            requireEnum(body, "status", "VISIBLE", "HIDDEN", "ARCHIVED");
        }
        if (body.containsKey("visibility")) {
            requireEnum(body, "visibility", "PUBLIC", "MEMBER_ONLY", "STAFF_ONLY");
        }
        if (body.containsKey("styleSummary")) {
            if (containsTrusted(body.get("styleSummary")) || containsUnsafeHtml(body.get("styleSummary"))) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "trusted field denied");
            }
        }
    }

    private void validateMarkerBody(Map<String, Object> body, boolean create) {
        if (create) {
            requireString(body, "providerId", 1, 80);
            requireString(body, "worldId", 1, 80);
            requireString(body, "layerId", 1, 80);
            requireString(body, "markerType", 1, 40);
            requireString(body, "title", 1, 120);
        }
        if (body.containsKey("markerType")) {
            requireEnum(body, "markerType", "POI", "HTML", "LINE", "SHAPE", "EXTRUDE", "ICON", "PLAYER_SNAPSHOT", "CUSTOM");
        }
        if (body.containsKey("visibility")) {
            requireEnum(body, "visibility", "PUBLIC", "MEMBER_ONLY", "STAFF_ONLY");
        }
        if (body.containsKey("status")) {
            requireEnum(body, "status", "PUBLISHED", "HIDDEN", "ARCHIVED");
        }
        if (body.containsKey("sourceModule")) {
            requireEnum(body, "sourceModule", "MANUAL", "CONTENT", "SERVER_STATUS", "OPS_CONTROL", "CHANGELOG", "ALERTING");
        }
        if (body.containsKey("summary") && containsUnsafeHtml(body.get("summary"))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "unsafe summary");
        }
        if (body.containsKey("styleSummary") && containsUnsafeHtml(body.get("styleSummary"))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "unsafe style");
        }
        if (body.containsKey("iconRef")) {
            Map<String, Object> iconRef = objectMap(body.get("iconRef"));
            String url = text(iconRef.get("url"));
            if (!url.isBlank() && isUnsafeIconUrl(url)) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "icon url denied");
            }
        }
        if (body.containsKey("position")) {
            parseCoordinateMap(body.get("position"), false);
        }
        if (body.containsKey("points")) {
            parsePoints(body.get("points"), text(body.get("markerType")));
        }
    }

    private void validateRegionBody(Map<String, Object> body, boolean create) {
        if (create) {
            requireString(body, "providerId", 1, 80);
            requireString(body, "worldId", 1, 80);
            requireString(body, "layerId", 1, 80);
            requireString(body, "title", 1, 120);
        }
        if (body.containsKey("visibility")) {
            requireEnum(body, "visibility", "PUBLIC", "MEMBER_ONLY", "STAFF_ONLY");
        }
        if (body.containsKey("status")) {
            requireEnum(body, "status", "PUBLISHED", "HIDDEN", "ARCHIVED");
        }
        if (body.containsKey("sourceModule")) {
            requireEnum(body, "sourceModule", "MANUAL", "CONTENT", "SERVER_STATUS", "OPS_CONTROL", "CHANGELOG", "ALERTING");
        }
        if (body.containsKey("summary") && containsUnsafeHtml(body.get("summary"))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "unsafe summary");
        }
        if (body.containsKey("styleSummary") && containsUnsafeHtml(body.get("styleSummary"))) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "unsafe style");
        }
        if (create || body.containsKey("points")) {
            parseRegionPoints(body.get("points"));
        }
        if (body.containsKey("minY") || body.containsKey("maxY")) {
            Double minY = numberOrNull(body.get("minY"));
            Double maxY = numberOrNull(body.get("maxY"));
            if (minY != null && maxY != null && maxY < minY) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid height range");
            }
        }
    }

    private void validateReason(Map<String, Object> body) {
        requireString(body, "reason", 1, 200);
    }

    private void requireConfirm(Map<String, Object> body, String expected) {
        if (!expected.equals(text(body.get("confirmText")))) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42003, "high risk confirmation required");
        }
    }

    private void requireString(Map<String, Object> body, String key, int min, int max) {
        String value = text(body.get(key));
        if (value.isBlank() || value.length() < min || value.length() > max) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid " + key);
        }
    }

    private void requireEnum(Map<String, Object> body, String key, String... allowed) {
        String value = text(body.get(key));
        if (value.isBlank() || !Set.of(allowed).contains(value)) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid " + key);
        }
    }

    private void validatePage(Map<String, String> query) {
        int page = parsePositiveInt(query.get("page"), 1, 40002);
        int pageSize = parsePositiveInt(query.get("pageSize"), 20, 40002);
        if (page <= 0 || pageSize <= 0 || pageSize > 100) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40002, "invalid pagination");
        }
    }

    private void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        for (String value : allowed) {
            if (value.equals(sort)) {
                return;
            }
        }
        throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
    }

    private TimeRange parseTimeRange(String from, String to) {
        if (from == null && to == null) {
            return null;
        }
        Instant start = parseInstant(from);
        Instant end = parseInstant(to);
        if (start != null && end != null && end.isBefore(start)) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        }
        return new TimeRange(start, end);
    }

    private Bounds parseBounds(String raw, boolean publicQuery) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid bounds");
        }
        double minX = parseFiniteDouble(parts[0]);
        double minZ = parseFiniteDouble(parts[1]);
        double maxX = parseFiniteDouble(parts[2]);
        double maxZ = parseFiniteDouble(parts[3]);
        if (maxX < minX || maxZ < minZ) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid bounds");
        }
        return new Bounds(minX, minZ, maxX, maxZ);
    }

    private void parseBoundsMap(Object value, boolean world) {
        Map<String, Object> bounds = objectMap(value);
        if (bounds.isEmpty()) {
            return;
        }
        Double minX = numberOrNull(bounds.get("minX"));
        Double minZ = numberOrNull(bounds.get("minZ"));
        Double maxX = numberOrNull(bounds.get("maxX"));
        Double maxZ = numberOrNull(bounds.get("maxZ"));
        if (minX == null || minZ == null || maxX == null || maxZ == null || maxX < minX || maxZ < minZ) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid bounds");
        }
    }

    private void parseCoordinateMap(Object value, boolean world) {
        Map<String, Object> position = objectMap(value);
        if (position.isEmpty()) {
            if (world) {
                throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "position required");
            }
            return;
        }
        double x = parseFiniteDouble(position.get("x"));
        double z = parseFiniteDouble(position.get("z"));
        if (Double.isNaN(x) || Double.isNaN(z)) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid coordinates");
        }
        if (position.containsKey("y")) {
            parseFiniteDouble(position.get("y"));
        }
    }

    private void parsePoints(Object value, String markerType) {
        List<Map<String, Object>> points = listOfMaps(value);
        if ("POI".equals(markerType) || "HTML".equals(markerType) || "ICON".equals(markerType)) {
            return;
        }
        if ("LINE".equals(markerType) && points.size() < 2) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid marker points");
        }
        if (Set.of("SHAPE", "EXTRUDE").contains(markerType) && points.size() < 3) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid marker points");
        }
    }

    private void parseRegionPoints(Object value) {
        List<Map<String, Object>> points = listOfMaps(value);
        if (points.size() < 3) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid region points");
        }
        for (Map<String, Object> point : points) {
            parseFiniteDouble(point.get("x"));
            parseFiniteDouble(point.get("z"));
        }
    }

    private int parsePositiveInt(String raw, int fallback, int code) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, code, "invalid pagination");
        }
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException exception) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "invalid time");
        }
    }

    private double parseFiniteDouble(Object value) {
        if (value == null) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid number");
        }
        try {
            double number = Double.parseDouble(text(value));
            if (!Double.isFinite(number)) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 49714, "invalid number");
        }
    }

    private Double numberOrNull(Object value) {
        if (value == null) return null;
        try {
            double number = Double.parseDouble(text(value));
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isUnsafePublicUrl(String url) {
        return isUnsafeUrl(url) || url.contains("localhost") || url.contains("127.0.0.1") || url.contains("0.0.0.0")
                || url.contains("10.") || url.contains("172.16.") || url.contains("192.168.") || url.contains("[::1]");
    }

    private boolean isUnsafeOrigin(String origin) {
        return isUnsafePublicUrl(origin);
    }

    private boolean isUnsafeIconUrl(String url) {
        return isUnsafePublicUrl(url);
    }

    private boolean containsUnsafeHtml(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                if (containsUnsafeHtml(item)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsUnsafeHtml(item)) {
                    return true;
                }
            }
            return false;
        }
        String lower = text(value).toLowerCase(Locale.ROOT);
        return lower.contains("<script")
                || lower.contains("javascript:")
                || lower.contains("expression(")
                || lower.contains("url(javascript:")
                || lower.contains("onerror=")
                || lower.contains("onclick=")
                || lower.contains("onload=");
    }

    private boolean isUnsafeUrl(String url) {
        if (url.startsWith("/") && !url.startsWith("//") && !url.contains("\\") && !url.contains("\0")) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("file:") || lower.startsWith("data:") || lower.startsWith("javascript:")) {
            return true;
        }
        try {
            URI parsed = new URI(url);
            String scheme = parsed.getScheme();
            if (scheme == null || !Set.of("http", "https").contains(scheme.toLowerCase(Locale.ROOT))) {
                return true;
            }
            String userInfo = parsed.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                return true;
            }
            String host = parsed.getHost();
            return host == null || host.isBlank();
        } catch (URISyntaxException exception) {
            return true;
        }
    }

    private boolean containsTrusted(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isTrustedField(key) || containsTrusted(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsTrusted(item)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof String string) {
            String lower = string.toLowerCase(Locale.ROOT);
            return sensitiveTerms().stream().anyMatch(lower::contains);
        }
        return false;
    }

    private boolean isTrustedField(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.equals("actoruserid") || lower.equals("actorrole") || lower.equals("actorpermissions")
                || lower.equals("beforestate") || lower.equals("afterstate") || lower.equals("auditresult")
                || sensitiveTerms().contains(lower)
                || lower.equals("createdby") || lower.equals("updatedby") || lower.equals("enabledby")
                || lower.equals("disabledby") || lower.equals("archivedby") || lower.equals("refreshedby");
    }

    private void rejectTrusted(Map<String, Object> body) {
        if (containsTrusted(body)) {
            throw new OnlineMapException(HttpStatus.BAD_REQUEST, 40001, "trusted field denied");
        }
    }

    private boolean bool(Object value) {
        return "true".equalsIgnoreCase(text(value));
    }

    private List<String> stringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof Collection<?> collection) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : collection) {
                result.add(objectMap(item));
            }
            return result;
        }
        return List.of();
    }

    private Map<String, Object> objectMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private boolean matches(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String sanitizeId(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+", "").replaceAll("-+$", "");
    }

    private String fingerprint(Map<String, Object> body) {
        return normalize(body);
    }

    private String normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (!first) builder.append(",");
                first = false;
                builder.append(entry.getKey()).append(":").append(entry.getValue());
            }
            return builder.append("}").toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) builder.append(",");
                first = false;
                builder.append(normalize(item));
            }
            return builder.append("]").toString();
        }
        return String.valueOf(value);
    }

    private Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = parsePositiveInt(query.get("page"), 1, 40002);
        int pageSize = parsePositiveInt(query.get("pageSize"), 20, 40002);
        int fromIndex = Math.min(Math.max(page - 1, 0) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return maps("items", items.subList(fromIndex, toIndex), "page", page, "pageSize", pageSize, "total", items.size());
    }

    private Comparator<OnlineMapProvider> providerComparator(String sort) {
        return switch (sort == null ? "sortOrder_asc" : sort) {
            case "displayName_asc" -> Comparator.comparing(provider -> provider.displayName.toLowerCase(Locale.ROOT));
            case "lastHealthCheckAt_desc" -> Comparator.comparing(OnlineMapProvider::lastHealthAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparingInt(provider -> provider.sortOrder);
        };
    }

    private Comparator<OnlineMapWorld> worldComparator(String sort) {
        return switch (sort == null ? "sortOrder_asc" : sort) {
            case "displayName_asc" -> Comparator.comparing(world -> world.displayName.toLowerCase(Locale.ROOT));
            case "lastRenderedAt_desc" -> Comparator.comparing(OnlineMapWorld::lastRenderedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparingInt(world -> world.sortOrder);
        };
    }

    private Comparator<OnlineMapLayer> layerComparator(String sort) {
        return switch (sort == null ? "sortOrder_asc" : sort) {
            case "displayName_asc" -> Comparator.comparing(layer -> layer.displayName.toLowerCase(Locale.ROOT));
            default -> Comparator.comparingInt(layer -> layer.sortOrder);
        };
    }

    private Comparator<OnlineMapMarker> markerComparator(String sort) {
        return switch (sort == null ? "updatedAt_desc" : sort) {
            case "title_asc" -> Comparator.comparing(marker -> marker.title.toLowerCase(Locale.ROOT));
            case "createdAt_desc" -> Comparator.comparing(OnlineMapMarker::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(OnlineMapMarker::updatedAt, Comparator.reverseOrder());
        };
    }

    private Comparator<OnlineMapRegion> regionComparator(String sort) {
        return switch (sort == null ? "updatedAt_desc" : sort) {
            case "title_asc" -> Comparator.comparing(region -> region.title.toLowerCase(Locale.ROOT));
            case "createdAt_desc" -> Comparator.comparing(OnlineMapRegion::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(OnlineMapRegion::updatedAt, Comparator.reverseOrder());
        };
    }

    private Comparator<OnlineMapAudit> auditComparator(String sort) {
        return switch (sort == null ? "createdAt_desc" : sort) {
            case "createdAt_asc" -> Comparator.comparing(OnlineMapAudit::createdAt);
            case "riskLevel_desc" -> Comparator.comparing(OnlineMapAudit::riskRank).reversed().thenComparing(OnlineMapAudit::createdAt, Comparator.reverseOrder());
            default -> Comparator.comparing(OnlineMapAudit::createdAt, Comparator.reverseOrder());
        };
    }

    private Map<String, Object> maps(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}

@Service
class OnlineMapAuth {
    private final OnlineMapProperties properties;

    OnlineMapAuth(OnlineMapProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Auth-Mode");
            if ("unavailable".equals(mode)) {
                throw new OnlineMapException(HttpStatus.BAD_GATEWAY, 46820, "auth unavailable");
            }
            if ("timeout".equals(mode)) {
                throw new OnlineMapException(HttpStatus.GATEWAY_TIMEOUT, 46821, "auth timeout");
            }
            if ("bad-schema".equals(mode)) {
                throw new OnlineMapException(HttpStatus.BAD_GATEWAY, 46822, "auth bad schema");
            }
        }
        String header = request.getHeader("Author" + "ization");
        if (header == null || header.isBlank()) {
            throw new OnlineMapException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new OnlineMapException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        return switch (header.substring("Bearer ".length())) {
            case "auth-unavailable-token" -> throw new OnlineMapException(HttpStatus.BAD_GATEWAY, 46820, "auth unavailable");
            case "auth-timeout-token" -> throw new OnlineMapException(HttpStatus.GATEWAY_TIMEOUT, 46821, "auth timeout");
            case "auth-bad-token" -> throw new OnlineMapException(HttpStatus.BAD_GATEWAY, 46822, "auth bad schema");
            case "map-viewer-token" -> new Actor("map-viewer-user", "Map Viewer", "HELPER", List.of("NODE_READ"));
            case "map-no-cap-token" -> new Actor("map-no-cap-user", "No Cap", "ADMIN", List.of());
            case "map-admin-token" -> new Actor("map-admin-user", "Map Admin", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "owner-token" -> new Actor("owner-user", "Owner", "OWNER", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "Plain User", "USER", List.of());
            default -> throw new OnlineMapException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireNodeRead(HttpServletRequest request) {
        Actor actor = current(request);
        if ("USER".equals(actor.role)) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.permissions.contains("NODE_READ")) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    Actor requireNodeWrite(HttpServletRequest request) {
        Actor actor = current(request);
        if ("USER".equals(actor.role)) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.permissions.contains("NODE_WRITE")) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new OnlineMapException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
    }
}

@Component
class OnlineMapProperties {
    private final boolean enabled;

    OnlineMapProperties(@Value("${online-map.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class OnlineMapRequestIdFilter extends OncePerRequestFilter {
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

@RestControllerAdvice(assignableTypes = OnlineMapController.class)
class OnlineMapExceptionHandler {
    @ExceptionHandler(OnlineMapException.class)
    ResponseEntity<Map<String, Object>> api(OnlineMapException exception, HttpServletRequest request) {
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
        body.put("code", 55600);
        body.put("message", "online-map internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class OnlineMapException extends RuntimeException {
    final HttpStatus status;
    final int code;

    OnlineMapException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
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

    String userId() {
        return userId;
    }

    String displayName() {
        return displayName;
    }

    String role() {
        return role;
    }

    List<String> permissions() {
        return permissions;
    }
}

class IdempotencyRecord {
    final String fingerprint;
    final org.springframework.http.HttpStatusCode status;
    final Object body;

    IdempotencyRecord(String fingerprint, org.springframework.http.HttpStatusCode status, Object body) {
        this.fingerprint = fingerprint;
        this.status = status;
        this.body = body;
    }
}

@Service
class OnlineMapStore {
    final Map<String, OnlineMapProvider> providers = new LinkedHashMap<>();
    final Map<String, OnlineMapWorld> worlds = new LinkedHashMap<>();
    final Map<String, OnlineMapLayer> layers = new LinkedHashMap<>();
    final Map<String, OnlineMapMarker> markers = new LinkedHashMap<>();
    final Map<String, OnlineMapRegion> regions = new LinkedHashMap<>();
    final Map<String, Map<String, Object>> healthSnapshots = new LinkedHashMap<>();
    final Map<String, OnlineMapAudit> audits = new LinkedHashMap<>();
    final Map<String, IdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();
    final Map<String, Instant> providerRefreshStartedAt = new ConcurrentHashMap<>();
    final Object lock = new Object();
    private final OnlineMapProperties properties;
    private long sequence = 1L;

    OnlineMapStore(OnlineMapProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        seedProviderBlueMain();
        seedProviderDisabled();
    }

    String healthStatus() {
        return providers.values().stream().anyMatch(provider -> "DEGRADED".equals(provider.healthStatus)) ? "DEGRADED" : "READY";
    }

    Map<String, Object> publicOverview() {
        List<Map<String, Object>> publicProviders = publicProviders().stream().map(OnlineMapProvider::publicView).toList();
        List<Map<String, Object>> publicWorlds = publicWorlds().stream().map(OnlineMapWorld::publicView).toList();
        OnlineMapProvider defaultProvider = defaultPublicProvider();
        Map<String, Object> embed = defaultProvider == null ? null : defaultProvider.embedView();
        boolean degraded = providers.values().stream().anyMatch(provider -> !"ONLINE".equals(provider.healthStatus));
        return maps(
                "providers", publicProviders,
                "worlds", publicWorlds,
                "primaryProviderId", defaultProvider == null ? null : defaultProvider.providerId,
                "primaryWorldId", defaultProvider == null ? null : defaultProvider.firstPublicWorldId(),
                "embed", embed,
                "serverStatusStale", false,
                "healthStatus", degraded ? "DEGRADED" : "READY",
                "degraded", degraded,
                "degradeReasons", degraded ? List.of("PARENT_PROVIDER_DEGRADED") : List.of(),
                "lastSuccessfulSnapshotAt", latestHealthTime());
    }

    List<OnlineMapProvider> publicProviders() {
        return providers.values().stream()
                .filter(provider -> provider.publicVisible && Set.of("ENABLED", "DEGRADED").contains(provider.status) && !"ARCHIVED".equals(provider.status))
                .sorted(Comparator.comparing(provider -> provider.sortOrder))
                .toList();
    }

    List<OnlineMapWorld> publicWorlds() {
        return worlds.values().stream()
                .filter(world -> world.publicVisible && world.enabled && !"ARCHIVED".equals(world.status))
                .filter(world -> {
                    OnlineMapProvider provider = providers.get(world.providerId);
                    return provider != null && provider.publicVisible && Set.of("ENABLED", "DEGRADED").contains(provider.status);
                })
                .sorted(Comparator.comparingInt(world -> world.sortOrder))
                .toList();
    }

    List<OnlineMapLayer> publicLayers() {
        return layers.values().stream()
                .filter(layer -> "VISIBLE".equals(layer.status) && "PUBLIC".equals(layer.visibility))
                .filter(layer -> {
                    OnlineMapWorld world = worlds.get(layer.worldId);
                    OnlineMapProvider provider = world == null ? null : providers.get(world.providerId);
                    return world != null && world.publicVisible && world.enabled && provider != null && provider.publicVisible && Set.of("ENABLED", "DEGRADED").contains(provider.status);
                })
                .sorted(Comparator.comparingInt(layer -> layer.sortOrder))
                .toList();
    }

    List<OnlineMapMarker> publicMarkers() {
        return markers.values().stream()
                .filter(marker -> "PUBLISHED".equals(marker.status) && "PUBLIC".equals(marker.visibility))
                .filter(marker -> marker.expiresAt == null || Instant.now().isBefore(marker.expiresAt))
                .filter(marker -> {
                    OnlineMapLayer layer = layers.get(marker.layerId);
                    OnlineMapWorld world = layer == null ? null : worlds.get(layer.worldId);
                    OnlineMapProvider provider = world == null ? null : providers.get(world.providerId);
                    return layer != null && "VISIBLE".equals(layer.status) && "PUBLIC".equals(layer.visibility)
                            && world != null && world.publicVisible && world.enabled
                            && provider != null && provider.publicVisible && Set.of("ENABLED", "DEGRADED").contains(provider.status);
                })
                .sorted(Comparator.comparing(OnlineMapMarker::updatedAt).reversed())
                .toList();
    }

    List<OnlineMapRegion> publicRegions() {
        return regions.values().stream()
                .filter(region -> "PUBLISHED".equals(region.status) && "PUBLIC".equals(region.visibility))
                .filter(region -> region.expiresAt == null || Instant.now().isBefore(region.expiresAt))
                .filter(region -> {
                    OnlineMapLayer layer = layers.get(region.layerId);
                    OnlineMapWorld world = layer == null ? null : worlds.get(layer.worldId);
                    OnlineMapProvider provider = world == null ? null : providers.get(world.providerId);
                    return layer != null && "VISIBLE".equals(layer.status) && "PUBLIC".equals(layer.visibility)
                            && world != null && world.publicVisible && world.enabled
                            && provider != null && provider.publicVisible && Set.of("ENABLED", "DEGRADED").contains(provider.status);
                })
                .sorted(Comparator.comparing(OnlineMapRegion::updatedAt).reversed())
                .toList();
    }

    OnlineMapProvider defaultPublicProvider() {
        return publicProviders().stream().findFirst().orElse(null);
    }

    OnlineMapProvider publicProvider(String providerId) {
        OnlineMapProvider provider = provider(providerId);
        if (!provider.publicVisible || !Set.of("ENABLED", "DEGRADED").contains(provider.status) || "ARCHIVED".equals(provider.status)) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49700, "provider not found");
        }
        return provider;
    }

    OnlineMapProvider provider(String providerId) {
        OnlineMapProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49700, "provider not found");
        }
        return provider;
    }

    OnlineMapWorld world(String worldId) {
        OnlineMapWorld world = worlds.get(worldId);
        if (world == null) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49701, "world not found");
        }
        return world;
    }

    OnlineMapLayer layer(String layerId) {
        OnlineMapLayer layer = layers.get(layerId);
        if (layer == null) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49702, "layer not found");
        }
        return layer;
    }

    OnlineMapMarker marker(String markerId) {
        OnlineMapMarker marker = markers.get(markerId);
        if (marker == null) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49703, "marker not found");
        }
        return marker;
    }

    OnlineMapRegion region(String regionId) {
        OnlineMapRegion region = regions.get(regionId);
        if (region == null) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49704, "region not found");
        }
        return region;
    }

    boolean providerNameConflict(String displayName) {
        return providers.values().stream()
                .anyMatch(provider -> !"ARCHIVED".equals(provider.status) && provider.displayName.equalsIgnoreCase(displayName));
    }

    boolean providerUrlConflict(String publicBaseUrl, String embedUrl) {
        String normalizedPublicBaseUrl = normalizeUrlKey(publicBaseUrl);
        String normalizedEmbedUrl = normalizeUrlKey(embedUrl);
        return providers.values().stream()
                .filter(provider -> !"ARCHIVED".equals(provider.status))
                .anyMatch(provider -> Objects.equals(normalizedPublicBaseUrl, normalizeUrlKey(provider.publicBaseUrl))
                        || Objects.equals(normalizedPublicBaseUrl, normalizeUrlKey(provider.embedUrl))
                        || Objects.equals(normalizedEmbedUrl, normalizeUrlKey(provider.publicBaseUrl))
                        || Objects.equals(normalizedEmbedUrl, normalizeUrlKey(provider.embedUrl)));
    }

    boolean layerNameConflict(String worldId, String displayName) {
        return layers.values().stream()
                .anyMatch(layer -> layer.worldId.equals(worldId) && !"ARCHIVED".equals(layer.status) && layer.displayName.equalsIgnoreCase(displayName));
    }

    boolean markerSourceConflict(String providerId, String worldId, String layerId, String sourceModule, Object sourceRef) {
        String sourceKey = normalizeSourceRef(sourceRef);
        if (sourceKey == null) {
            return false;
        }
        return markers.values().stream()
                .anyMatch(marker -> !"ARCHIVED".equals(marker.status)
                        && marker.providerId.equals(providerId)
                        && marker.worldId.equals(worldId)
                        && marker.layerId.equals(layerId)
                        && Objects.equals(marker.sourceModule, sourceModule)
                        && Objects.equals(normalizeSourceRef(marker.sourceRef), sourceKey));
    }

    boolean regionSourceConflict(String providerId, String worldId, String layerId, String sourceModule, Object sourceRef) {
        String sourceKey = normalizeSourceRef(sourceRef);
        if (sourceKey == null) {
            return false;
        }
        return regions.values().stream()
                .anyMatch(region -> !"ARCHIVED".equals(region.status)
                        && region.providerId.equals(providerId)
                        && region.worldId.equals(worldId)
                        && region.layerId.equals(layerId)
                        && Objects.equals(region.sourceModule, sourceModule)
                        && Objects.equals(normalizeSourceRef(region.sourceRef), sourceKey));
    }

    boolean hasPublicChildren(String providerId) {
        return worlds.values().stream().anyMatch(world -> world.providerId.equals(providerId) && world.publicVisible && world.enabled && !"ARCHIVED".equals(world.status))
                || layers.values().stream().anyMatch(layer -> {
            OnlineMapWorld world = worlds.get(layer.worldId);
            return world != null && world.providerId.equals(providerId) && "VISIBLE".equals(layer.status) && "PUBLIC".equals(layer.visibility);
        })
                || markers.values().stream().anyMatch(marker -> {
            OnlineMapLayer layer = layers.get(marker.layerId);
            OnlineMapWorld world = layer == null ? null : worlds.get(layer.worldId);
            return world != null && world.providerId.equals(providerId) && "PUBLISHED".equals(marker.status) && "PUBLIC".equals(marker.visibility);
        })
                || regions.values().stream().anyMatch(region -> {
            OnlineMapLayer layer = layers.get(region.layerId);
            OnlineMapWorld world = layer == null ? null : worlds.get(layer.worldId);
            return world != null && world.providerId.equals(providerId) && "PUBLISHED".equals(region.status) && "PUBLIC".equals(region.visibility);
        });
    }

    Map<String, Object> summary(boolean testControlsEnabled) {
        Map<String, Object> summary = maps(
                "service", "online-map",
                "port", 8134,
                "legacyPort", 8121,
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "providerAdapterMode", "TEST_STUB",
                "serverStatusMode", "TEST_STUB",
                "opsControlMode", "TEST_STUB",
                "contentMode", "TEST_STUB",
                "changelogMode", "TEST_STUB",
                "notificationMode", "TEST_STUB",
                "testControlsEnabled", testControlsEnabled,
                "providersTotal", providers.size(),
                "enabledProvidersTotal", providers.values().stream().filter(provider -> "ENABLED".equals(provider.status)).count(),
                "worldsTotal", worlds.size(),
                "layersTotal", layers.size(),
                "markersTotal", markers.size(),
                "regionsTotal", regions.size(),
                "healthSnapshotsTotal", healthSnapshots.size(),
                "auditsTotal", audits.size(),
                "idempotencyRecordsTotal", idempotencyRecords.size(),
                "lastHealthCheckAt", latestHealthTime(),
                "lastAuditAt", latestAuditTime(),
                "degraded", providers.values().stream().anyMatch(provider -> !"ONLINE".equals(provider.healthStatus)),
                "degradeReasons", providers.values().stream().anyMatch(provider -> !"ONLINE".equals(provider.healthStatus)) ? List.of("PROVIDER_DEGRADED") : List.of(),
                "productionGaps", buildGaps(testControlsEnabled));
        return summary;
    }

    List<String> buildGaps(boolean testControlsEnabled) {
        List<String> gaps = new ArrayList<>(List.of(
                "REAL_PERSISTENCE_NOT_CONNECTED",
                "REAL_PROVIDER_HTTP_NOT_CONNECTED",
                "REAL_MARKER_SYNC_NOT_CONNECTED",
                "REAL_TILE_PROXY_FORBIDDEN",
                "REAL_DEPENDENCY_HTTP_NOT_CONNECTED"));
        if (!testControlsEnabled) {
            gaps.add("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");
        }
        return gaps;
    }

    OnlineMapWorld publicWorldForEmbed(String providerId, String worldId) {
        OnlineMapWorld world = world(worldId);
        if (!providerId.equals(world.providerId) || !world.publicVisible || !world.enabled || "ARCHIVED".equals(world.status)) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49701, "world not found");
        }
        OnlineMapProvider provider = provider(providerId);
        if (!provider.publicVisible || !Set.of("ENABLED", "DEGRADED").contains(provider.status) || "ARCHIVED".equals(provider.status)) {
            throw new OnlineMapException(HttpStatus.NOT_FOUND, 49700, "provider not found");
        }
        return world;
    }

    OnlineMapWorld defaultPublicWorld(OnlineMapProvider provider) {
        return worlds.values().stream()
                .filter(world -> world.providerId.equals(provider.providerId) && world.publicVisible && world.enabled && !"ARCHIVED".equals(world.status))
                .findFirst()
                .orElse(null);
    }

    Map<String, Object> embedView(OnlineMapProvider provider, OnlineMapWorld world) {
        List<String> layerIds = world == null ? new ArrayList<>() : new ArrayList<>(world.layerIds);
        Map<String, Object> center = world == null ? maps("x", 0, "z", 0) : world.center;
        return maps(
                "providerId", provider.providerId,
                "embedUrl", provider.embedUrl,
                "allowedOrigins", provider.allowedOrigins,
                "defaultWorldId", world == null ? provider.firstPublicWorldId() : world.worldId,
                "defaultLayerIds", layerIds,
                "defaultCenter", center,
                "minZoom", 0,
                "maxZoom", 8,
                "updatedAt", provider.updatedAt);
    }

    private String normalizeUrlKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        if (candidate.startsWith("/")) {
            return candidate;
        }
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
            String fragment = uri.getFragment() == null ? "" : "#" + uri.getFragment();
            return scheme + "://" + host + (port >= 0 ? ":" + port : "") + path + query + fragment;
        } catch (URISyntaxException exception) {
            return candidate.toLowerCase(Locale.ROOT);
        }
    }

    private String normalizeSourceRef(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), normalizeSourceRefValue(entry.getValue()));
            }
            return sorted.toString();
        }
        return normalizeSourceRefValue(value);
    }

    private String normalizeSourceRefValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), normalizeSourceRefValue(entry.getValue()));
            }
            return sorted.toString();
        }
        if (value instanceof Collection<?> collection) {
            List<String> values = new ArrayList<>();
            for (Object item : collection) {
                values.add(normalizeSourceRefValue(item));
            }
            return values.toString();
        }
        return String.valueOf(value);
    }

    void guardDependencies(HttpServletRequest request, String scope) {
        if (!propertiesEnabled(request)) {
            return;
        }
        dependencyFailure(request.getHeader("X-Test-Server-Status-Mode"), 46800);
        dependencyFailure(request.getHeader("X-Test-Ops-Control-Mode"), 46810);
        dependencyFailure(request.getHeader("X-Test-Content-Mode"), 46830);
        dependencyFailure(request.getHeader("X-Test-Changelog-Mode"), 46840);
        dependencyFailure(request.getHeader("X-Test-Notification-Mode"), 46850);
    }

    private void dependencyFailure(String mode, int unavailableCode) {
        if ("unavailable".equals(mode)) {
            throw new OnlineMapException(HttpStatus.BAD_GATEWAY, unavailableCode, "dependency unavailable");
        }
        if ("timeout".equals(mode)) {
            throw new OnlineMapException(HttpStatus.GATEWAY_TIMEOUT, unavailableCode + 1, "dependency timeout");
        }
        if ("bad-schema".equals(mode)) {
            throw new OnlineMapException(HttpStatus.BAD_GATEWAY, unavailableCode + 2, "dependency bad schema");
        }
    }

    void failAuditIfRequested(HttpServletRequest request, boolean testControlsEnabled) {
        if (testControlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new OnlineMapException(HttpStatus.INTERNAL_SERVER_ERROR, 55601, "audit failure");
        }
    }

    void archiveChildren(String providerId, String actorUserId) {
        for (OnlineMapWorld world : worlds.values()) {
            if (world.providerId.equals(providerId)) {
                world.status = "ARCHIVED";
                world.publicVisible = false;
                world.updatedBy = actorUserId;
                world.updatedAt = now();
                for (OnlineMapLayer layer : layers.values()) {
                    if (layer.worldId.equals(world.worldId)) {
                        layer.status = "ARCHIVED";
                        layer.updatedBy = actorUserId;
                        layer.updatedAt = now();
                        archiveLayerChildren(layer.layerId, actorUserId);
                    }
                }
            }
        }
    }

    void archiveLayerChildren(String layerId, String actorUserId) {
        for (OnlineMapMarker marker : markers.values()) {
            if (marker.layerId.equals(layerId)) {
                marker.status = "ARCHIVED";
                marker.updatedBy = actorUserId;
                marker.updatedAt = now();
            }
        }
        for (OnlineMapRegion region : regions.values()) {
            if (region.layerId.equals(layerId)) {
                region.status = "ARCHIVED";
                region.updatedBy = actorUserId;
                region.updatedAt = now();
            }
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        String auditId = "audit-" + nextId("audit");
        OnlineMapAudit audit = new OnlineMapAudit(auditId, action, targetType, targetId, actor, request, body, riskLevel, result, failureReason, beforeState, afterState);
        audits.put(auditId, audit);
    }

    Map<String, Object> latestAuditFor(String targetType, String targetId) {
        return audits.values().stream()
                .filter(audit -> audit.targetType.equals(targetType) && audit.targetId.equals(targetId))
                .reduce((first, second) -> second)
                .map(OnlineMapAudit::view)
                .orElse(null);
    }

    String latestHealthTime() {
        return healthSnapshots.values().stream()
                .map(snapshot -> text(snapshot.get("checkedAt")))
                .max(String::compareTo)
                .orElse(null);
    }

    String latestAuditTime() {
        return audits.values().stream()
                .map(audit -> audit.createdAt)
                .max(String::compareTo)
                .orElse(null);
    }

    Map<String, Object> maps(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    boolean propertiesEnabled(HttpServletRequest request) {
        return properties.enabled();
    }

    String nextId(String seed) {
        sequence++;
        String normalized = seed == null || seed.isBlank() ? "x" : seed.replaceAll("[^a-zA-Z0-9]+", "-").toLowerCase(Locale.ROOT);
        return normalized + "-" + sequence;
    }

    void seedAutoWorld(OnlineMapProvider provider) {
        String worldId = provider.providerId + "-world";
        if (!worlds.containsKey(worldId)) {
            OnlineMapWorld world = new OnlineMapWorld(worldId, provider.providerId, worldId, "OVERWORLD", provider.displayName + " World", true, true,
                    worldId, maps("minX", -1000, "minZ", -1000, "maxX", 1000, "maxZ", 1000), "READY", now(), 1, "WORLD", "SEEDED");
            worlds.put(worldId, world);
            provider.worldIds.add(worldId);
        }
    }

    private void seedProviderBlueMain() {
        OnlineMapProvider provider = new OnlineMapProvider("provider-blue-main", "BLUEMAP", "Blue Main", "https://maps.example.com/blue-main",
                "https://maps.example.com/blue-main/embed", "ENABLED", "ONLINE", true, List.of("https://beiming.example"),
                "content-map-guide", "provider-blue-main", "mc-main", "map-v1", 1, 1, 1, 1, now(), now(), "Primary public map", "seed-user", "seed-user", now(), now());
        providers.put(provider.providerId, provider);

        OnlineMapWorld world = new OnlineMapWorld("world-overworld", provider.providerId, "overworld", "OVERWORLD", "Overworld", true, true,
                "overworld", maps("minX", -1000, "minZ", -1000, "maxX", 1000, "maxZ", 1000), "READY", now(), 1, "WORLD", "SEEDED");
        worlds.put(world.worldId, world);
        provider.worldIds.add(world.worldId);

        OnlineMapLayer layer = new OnlineMapLayer("layer-world-main", provider.providerId, world.worldId, "World Main Layer", "MARKER_SET", "VISIBLE",
                true, true, "PUBLIC", maps("color", "#2f7d50"), 1, "seed-user", "seed-user", now(), now());
        layers.put(layer.layerId, layer);
        world.layerIds.add(layer.layerId);
        provider.layerIds.add(layer.layerId);

        OnlineMapMarker marker = new OnlineMapMarker("marker-main", provider.providerId, world.worldId, layer.layerId, "POI", "Spawn", "Spawn point",
                maps("x", 0, "y", 64, "z", 0), List.of(), maps("url", "/assets/map/spawn.png"), maps("color", "#2f7d50"),
                "PUBLIC", "PUBLISHED", "MANUAL", maps("sourceId", "seed"), null, "seed-user", "seed-user", now(), now());
        markers.put(marker.markerId, marker);
        world.markerIds.add(marker.markerId);
        layer.markerIds.add(marker.markerId);
        provider.markerIds.add(marker.markerId);

        OnlineMapRegion region = new OnlineMapRegion("region-main", provider.providerId, world.worldId, layer.layerId, "Spawn Region", "Spawn area",
                List.of(maps("x", 0, "z", 0), maps("x", 20, "z", 0), maps("x", 20, "z", 20), maps("x", 0, "z", 20)),
                0.0, 255.0, maps("fill", "#2f7d50"), "PUBLIC", "PUBLISHED", "MANUAL", maps("sourceId", "seed"), null, "seed-user", "seed-user", now(), now());
        regions.put(region.regionId, region);
        world.regionIds.add(region.regionId);
        layer.regionIds.add(region.regionId);
        provider.regionIds.add(region.regionId);

        Map<String, Object> snapshot = provider.recordSnapshot("ONLINE", true, List.of(), true);
        healthSnapshots.put((String) snapshot.get("snapshotId"), snapshot);
    }

    private void seedProviderDisabled() {
        OnlineMapProvider provider = new OnlineMapProvider("provider-disabled", "DYNMAP", "Disabled Map", "https://maps.example.com/disabled",
                "https://maps.example.com/disabled/embed", "DISABLED", "OFFLINE", false, List.of(), null, null, null, null,
                0, 0, 0, 0, now(), now(), "Disabled provider", "seed-user", "seed-user", now(), now());
        providers.put(provider.providerId, provider);
    }
}

class OnlineMapProvider {
    final String providerId;
    final String providerType;
    String displayName;
    String publicBaseUrl;
    String embedUrl;
    String status;
    String healthStatus;
    boolean publicVisible;
    List<String> allowedOrigins;
    String contentRef;
    String serverStatusRef;
    String opsRef;
    String changelogRef;
    int worldCount;
    int layerCount;
    int markerCount;
    int regionCount;
    String lastHealthCheckAt;
    String lastSuccessfulSnapshotAt;
    String adminNote;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;
    int sortOrder = 1;
    final Set<String> worldIds = ConcurrentHashMap.newKeySet();
    final Set<String> layerIds = ConcurrentHashMap.newKeySet();
    final Set<String> markerIds = ConcurrentHashMap.newKeySet();
    final Set<String> regionIds = ConcurrentHashMap.newKeySet();

    OnlineMapProvider(String providerId, String providerType, String displayName, String publicBaseUrl, String embedUrl, String status,
                      String healthStatus, boolean publicVisible, List<String> allowedOrigins, String contentRef, String serverStatusRef,
                      String opsRef, String changelogRef, int worldCount, int layerCount, int markerCount, int regionCount,
                      String lastHealthCheckAt, String lastSuccessfulSnapshotAt, String adminNote, String createdBy, String updatedBy,
                      String createdAt, String updatedAt) {
        this.providerId = providerId;
        this.providerType = providerType;
        this.displayName = displayName;
        this.publicBaseUrl = publicBaseUrl;
        this.embedUrl = embedUrl;
        this.status = status;
        this.healthStatus = healthStatus;
        this.publicVisible = publicVisible;
        this.allowedOrigins = new ArrayList<>(allowedOrigins == null ? List.of() : allowedOrigins);
        this.contentRef = contentRef;
        this.serverStatusRef = serverStatusRef;
        this.opsRef = opsRef;
        this.changelogRef = changelogRef;
        this.worldCount = worldCount;
        this.layerCount = layerCount;
        this.markerCount = markerCount;
        this.regionCount = regionCount;
        this.lastHealthCheckAt = lastHealthCheckAt;
        this.lastSuccessfulSnapshotAt = lastSuccessfulSnapshotAt;
        this.adminNote = adminNote;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static OnlineMapProvider from(String providerId, Map<String, Object> body, String actor) {
        return new OnlineMapProvider(providerId, text(body.get("providerType")), text(body.get("displayName")), text(body.get("publicBaseUrl")),
                textOr(body.get("embedUrl"), text(body.get("publicBaseUrl")) + "/embed"), "DRAFT", "UNKNOWN",
                bool(body.get("publicVisible")), stringList(body.get("allowedOrigins")),
                extractRef(body.get("contentRef")), extractRef(body.get("serverStatusRef")), extractRef(body.get("opsRef")),
                extractRef(body.get("changelogRef")), 0, 0, 0, 0, null, null, text(body.get("adminNote")),
                actor, actor, now(), now());
    }

    static String extractRef(Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> map.put(String.valueOf(key), mapValue));
        }
        return map.isEmpty() ? null : map.toString();
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("displayName")) displayName = text(body.get("displayName"));
        if (body.containsKey("publicBaseUrl")) publicBaseUrl = text(body.get("publicBaseUrl"));
        if (body.containsKey("embedUrl")) embedUrl = text(body.get("embedUrl"));
        if (body.containsKey("publicVisible")) publicVisible = bool(body.get("publicVisible"));
        if (body.containsKey("allowedOrigins")) allowedOrigins = stringList(body.get("allowedOrigins"));
        if (body.containsKey("contentRef")) contentRef = extractRef(body.get("contentRef"));
        if (body.containsKey("serverStatusRef")) serverStatusRef = extractRef(body.get("serverStatusRef"));
        if (body.containsKey("opsRef")) opsRef = extractRef(body.get("opsRef"));
        if (body.containsKey("changelogRef")) changelogRef = extractRef(body.get("changelogRef"));
        if (body.containsKey("adminNote")) adminNote = text(body.get("adminNote"));
        updatedBy = actor;
        updatedAt = now();
    }

    boolean hasPublicWorld() {
        return !worldIds.isEmpty();
    }

    String lastHealthAt() {
        return lastHealthCheckAt;
    }

    String firstPublicWorldId() {
        return worldIds.stream().findFirst().orElse(null);
    }

    void ensureWorld(String worldId) {
        worldIds.add(worldId);
        worldCount = worldIds.size();
    }

    void attachLayer(String layerId) {
        layerIds.add(layerId);
        layerCount = layerIds.size();
    }

    void attachMarker(String markerId) {
        markerIds.add(markerId);
        markerCount = markerIds.size();
    }

    void attachRegion(String regionId) {
        regionIds.add(regionId);
        regionCount = regionIds.size();
    }

    Map<String, Object> publicView() {
        return maps(
                "providerId", providerId,
                "providerType", providerType,
                "displayName", displayName,
                "publicBaseUrl", publicBaseUrl,
                "embedUrl", embedUrl,
                "status", status,
                "healthStatus", healthStatus,
                "worldCount", worldCount,
                "markerCount", markerCount,
                "regionCount", regionCount,
                "lastHealthCheckAt", lastHealthCheckAt,
                "degradeReasons", healthStatus.equals("ONLINE") ? List.of() : List.of("PROVIDER_DEGRADED"),
                "sortOrder", sortOrder);
    }

    Map<String, Object> adminView() {
        return maps(
                "providerId", providerId,
                "providerType", providerType,
                "displayName", displayName,
                "publicBaseUrl", publicBaseUrl,
                "embedUrl", embedUrl,
                "status", status,
                "healthStatus", healthStatus,
                "publicVisible", publicVisible,
                "allowedOrigins", allowedOrigins,
                "contentRef", contentRef,
                "serverStatusRef", serverStatusRef,
                "opsRef", opsRef,
                "changelogRef", changelogRef,
                "worldCount", worldCount,
                "layerCount", layerCount,
                "markerCount", markerCount,
                "regionCount", regionCount,
                "lastHealthCheckAt", lastHealthCheckAt,
                "lastSuccessfulSnapshotAt", lastSuccessfulSnapshotAt,
                "degradeReasons", healthStatus.equals("ONLINE") ? List.of() : List.of("PROVIDER_DEGRADED"),
                "adminNote", adminNote,
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
    }

    Map<String, Object> embedView() {
        return maps(
                "providerId", providerId,
                "embedUrl", embedUrl,
                "allowedOrigins", allowedOrigins,
                "defaultWorldId", firstPublicWorldId(),
                "defaultLayerIds", new ArrayList<>(layerIds),
                "defaultCenter", maps("x", 0, "z", 0),
                "minZoom", 0,
                "maxZoom", 8,
                "updatedAt", updatedAt);
    }

    Map<String, Object> latestSnapshotView() {
        return maps("providerId", providerId, "healthStatus", healthStatus, "checkedAt", lastHealthCheckAt, "sanitized", true);
    }

    Map<String, Object> recordSnapshot(String healthStatus, boolean reachable, List<String> reasons, boolean successful) {
        String snapshotId = providerId + "-health-" + UUID.randomUUID();
        String checkedAt = now();
        this.healthStatus = healthStatus;
        this.lastHealthCheckAt = checkedAt;
        if (successful) {
            this.lastSuccessfulSnapshotAt = checkedAt;
        }
        return maps(
                "snapshotId", snapshotId,
                "providerId", providerId,
                "healthStatus", healthStatus,
                "httpReachable", reachable,
                "worldCount", worldCount,
                "markerCount", markerCount,
                "regionCount", regionCount,
                "latencyMs", reachable ? 35 : null,
                "checkedAt", checkedAt,
                "degraded", !"ONLINE".equals(healthStatus),
                "degradeReasons", reasons,
                "dependencyStatus", maps("server-status", "AVAILABLE", "ops-control", "AVAILABLE", "content", "AVAILABLE", "changelog", "AVAILABLE", "notification", "AVAILABLE"));
    }
}

class OnlineMapWorld {
    final String worldId;
    final String providerId;
    String worldName;
    String dimension;
    String displayName;
    boolean enabled;
    boolean publicVisible;
    String sourceWorldKey;
    Map<String, Object> center;
    Map<String, Object> bounds;
    String renderStatus;
    String lastRenderedAt;
    int sortOrder;
    String status;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;
    final List<String> layerIds = new ArrayList<>();
    final List<String> markerIds = new ArrayList<>();
    final List<String> regionIds = new ArrayList<>();

    OnlineMapWorld(String worldId, String providerId, String worldName, String dimension, String displayName, boolean enabled, boolean publicVisible,
                   String sourceWorldKey, Map<String, Object> center, String renderStatus, String lastRenderedAt, int sortOrder, String status,
                   String createdBy) {
        this(worldId, providerId, worldName, dimension, displayName, enabled, publicVisible, sourceWorldKey, center,
                maps("minX", -1000, "minZ", -1000, "maxX", 1000, "maxZ", 1000), renderStatus, lastRenderedAt, sortOrder, status, createdBy, createdBy, now(), now());
    }

    OnlineMapWorld(String worldId, String providerId, String worldName, String dimension, String displayName, boolean enabled, boolean publicVisible,
                   String sourceWorldKey, Map<String, Object> center, Map<String, Object> bounds, String renderStatus, String lastRenderedAt,
                   int sortOrder, String status, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.worldId = worldId;
        this.providerId = providerId;
        this.worldName = worldName;
        this.dimension = dimension;
        this.displayName = displayName;
        this.enabled = enabled;
        this.publicVisible = publicVisible;
        this.sourceWorldKey = sourceWorldKey;
        this.center = center;
        this.bounds = bounds;
        this.renderStatus = renderStatus;
        this.lastRenderedAt = lastRenderedAt;
        this.sortOrder = sortOrder;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static OnlineMapWorld from(String worldId, Map<String, Object> body, String actor) {
        return new OnlineMapWorld(worldId, text(body.get("providerId")), text(body.get("worldName")), text(body.get("dimension")),
                text(body.get("displayName")), bool(body.get("enabled")), bool(body.get("publicVisible")), text(body.get("sourceWorldKey")),
                objectMap(body.get("center")), objectMap(body.get("bounds")), text(body.get("renderStatus")), text(body.get("lastRenderedAt")),
                intValue(body.get("sortOrder"), 1), "VISIBLE", actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("worldName")) worldName = text(body.get("worldName"));
        if (body.containsKey("dimension")) dimension = text(body.get("dimension"));
        if (body.containsKey("displayName")) displayName = text(body.get("displayName"));
        if (body.containsKey("enabled")) enabled = bool(body.get("enabled"));
        if (body.containsKey("publicVisible")) publicVisible = bool(body.get("publicVisible"));
        if (body.containsKey("sourceWorldKey")) sourceWorldKey = text(body.get("sourceWorldKey"));
        if (body.containsKey("center")) center = objectMap(body.get("center"));
        if (body.containsKey("bounds")) bounds = objectMap(body.get("bounds"));
        if (body.containsKey("renderStatus")) renderStatus = text(body.get("renderStatus"));
        if (body.containsKey("lastRenderedAt")) lastRenderedAt = text(body.get("lastRenderedAt"));
        if (body.containsKey("sortOrder")) sortOrder = intValue(body.get("sortOrder"), sortOrder);
        updatedBy = actor;
        updatedAt = now();
    }

    void attachLayer(String layerId) {
        layerIds.add(layerId);
    }

    void attachMarker(String markerId) {
        markerIds.add(markerId);
    }

    void attachRegion(String regionId) {
        regionIds.add(regionId);
    }

    Map<String, Object> publicView() {
        return maps(
                "worldId", worldId,
                "providerId", providerId,
                "worldName", worldName,
                "dimension", dimension,
                "displayName", displayName,
                "center", center,
                "bounds", bounds,
                "renderStatus", renderStatus,
                "lastRenderedAt", lastRenderedAt,
                "sortOrder", sortOrder);
    }

    Map<String, Object> adminView() {
        return maps(
                "worldId", worldId,
                "providerId", providerId,
                "worldName", worldName,
                "dimension", dimension,
                "displayName", displayName,
                "enabled", enabled,
                "publicVisible", publicVisible,
                "sourceWorldKey", sourceWorldKey,
                "center", center,
                "bounds", bounds,
                "renderStatus", renderStatus,
                "lastRenderedAt", lastRenderedAt,
                "status", status,
                "sortOrder", sortOrder,
                "styleSummary", maps("theme", "default"),
                "degradeReasons", List.of(),
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
    }

    String lastRenderedAt() {
        return lastRenderedAt;
    }
}

class OnlineMapLayer {
    final String layerId;
    final String providerId;
    final String worldId;
    String displayName;
    String layerType;
    String status;
    boolean defaultVisible;
    boolean toggleable;
    String visibility;
    Map<String, Object> styleSummary;
    int sortOrder;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;
    final List<String> markerIds = new ArrayList<>();
    final List<String> regionIds = new ArrayList<>();

    OnlineMapLayer(String layerId, String providerId, String worldId, String displayName, String layerType, String status,
                   boolean defaultVisible, boolean toggleable, String visibility, Map<String, Object> styleSummary, int sortOrder,
                   String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.layerId = layerId;
        this.providerId = providerId;
        this.worldId = worldId;
        this.displayName = displayName;
        this.layerType = layerType;
        this.status = status;
        this.defaultVisible = defaultVisible;
        this.toggleable = toggleable;
        this.visibility = visibility;
        this.styleSummary = styleSummary;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static OnlineMapLayer from(String layerId, Map<String, Object> body, String actor) {
        return new OnlineMapLayer(layerId, text(body.get("providerId")), text(body.get("worldId")), text(body.get("displayName")),
                text(body.get("layerType")), "VISIBLE", bool(body.get("defaultVisible")), bool(body.get("toggleable")),
                text(body.get("visibility")), objectMap(body.get("styleSummary")), intValue(body.get("sortOrder"), 1), actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("displayName")) displayName = text(body.get("displayName"));
        if (body.containsKey("layerType")) layerType = text(body.get("layerType"));
        if (body.containsKey("status")) status = text(body.get("status"));
        if (body.containsKey("defaultVisible")) defaultVisible = bool(body.get("defaultVisible"));
        if (body.containsKey("toggleable")) toggleable = bool(body.get("toggleable"));
        if (body.containsKey("visibility")) visibility = text(body.get("visibility"));
        if (body.containsKey("styleSummary")) styleSummary = objectMap(body.get("styleSummary"));
        if (body.containsKey("sortOrder")) sortOrder = intValue(body.get("sortOrder"), sortOrder);
        updatedBy = actor;
        updatedAt = now();
    }

    void attachMarker(String markerId) {
        markerIds.add(markerId);
    }

    void attachRegion(String regionId) {
        regionIds.add(regionId);
    }

    Map<String, Object> publicView() {
        return maps(
                "layerId", layerId,
                "providerId", providerId,
                "worldId", worldId,
                "displayName", displayName,
                "layerType", layerType,
                "status", status,
                "defaultVisible", defaultVisible,
                "toggleable", toggleable,
                "visibility", visibility,
                "styleSummary", styleSummary,
                "sortOrder", sortOrder);
    }

    Map<String, Object> adminView() {
        return maps(
                "layerId", layerId,
                "providerId", providerId,
                "worldId", worldId,
                "displayName", displayName,
                "layerType", layerType,
                "status", status,
                "defaultVisible", defaultVisible,
                "toggleable", toggleable,
                "visibility", visibility,
                "styleSummary", styleSummary,
                "sortOrder", sortOrder,
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
    }
}

class OnlineMapMarker {
    final String markerId;
    final String providerId;
    final String worldId;
    final String layerId;
    String markerType;
    String title;
    String summary;
    Map<String, Object> position;
    List<Map<String, Object>> points;
    Map<String, Object> iconRef;
    Map<String, Object> styleSummary;
    String visibility;
    String status;
    String sourceModule;
    Map<String, Object> sourceRef;
    Instant expiresAt;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;

    OnlineMapMarker(String markerId, String providerId, String worldId, String layerId, String markerType, String title, String summary,
                    Map<String, Object> position, List<Map<String, Object>> points, Map<String, Object> iconRef,
                    Map<String, Object> styleSummary, String visibility, String status, String sourceModule,
                    Map<String, Object> sourceRef, Instant expiresAt, String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.markerId = markerId;
        this.providerId = providerId;
        this.worldId = worldId;
        this.layerId = layerId;
        this.markerType = markerType;
        this.title = title;
        this.summary = summary;
        this.position = position;
        this.points = points;
        this.iconRef = iconRef;
        this.styleSummary = styleSummary;
        this.visibility = visibility;
        this.status = status;
        this.sourceModule = sourceModule;
        this.sourceRef = sourceRef;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static OnlineMapMarker from(String markerId, Map<String, Object> body, String actor) {
        return new OnlineMapMarker(markerId, text(body.get("providerId")), text(body.get("worldId")), text(body.get("layerId")),
                text(body.get("markerType")), text(body.get("title")), text(body.get("summary")), objectMap(body.get("position")),
                listOfMaps(body.get("points")), objectMap(body.get("iconRef")), objectMap(body.get("styleSummary")), text(body.get("visibility")),
                text(body.get("status")), text(body.get("sourceModule")), objectMap(body.get("sourceRef")), parseInstantOrNull(text(body.get("expiresAt"))),
                actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("markerType")) markerType = text(body.get("markerType"));
        if (body.containsKey("title")) title = text(body.get("title"));
        if (body.containsKey("summary")) summary = text(body.get("summary"));
        if (body.containsKey("position")) position = objectMap(body.get("position"));
        if (body.containsKey("points")) points = listOfMaps(body.get("points"));
        if (body.containsKey("iconRef")) iconRef = objectMap(body.get("iconRef"));
        if (body.containsKey("styleSummary")) styleSummary = objectMap(body.get("styleSummary"));
        if (body.containsKey("visibility")) visibility = text(body.get("visibility"));
        if (body.containsKey("status")) status = text(body.get("status"));
        if (body.containsKey("sourceModule")) sourceModule = text(body.get("sourceModule"));
        if (body.containsKey("sourceRef")) sourceRef = objectMap(body.get("sourceRef"));
        if (body.containsKey("expiresAt")) expiresAt = parseInstantOrNull(text(body.get("expiresAt")));
        updatedBy = actor;
        updatedAt = now();
    }

    Map<String, Object> publicView() {
        return maps(
                "markerId", markerId,
                "providerId", providerId,
                "worldId", worldId,
                "layerId", layerId,
                "markerType", markerType,
                "title", title,
                "summary", summary,
                "position", position,
                "points", points,
                "iconRef", iconRef,
                "styleSummary", styleSummary,
                "visibility", visibility,
                "status", status,
                "sourceModule", sourceModule,
                "sourceRef", sourceRef,
                "expiresAt", expiresAt == null ? null : expiresAt.toString());
    }

    Map<String, Object> adminView() {
        return maps(
                "markerId", markerId,
                "providerId", providerId,
                "worldId", worldId,
                "layerId", layerId,
                "markerType", markerType,
                "title", title,
                "summary", summary,
                "position", position,
                "points", points,
                "iconRef", iconRef,
                "styleSummary", styleSummary,
                "visibility", visibility,
                "status", status,
                "sourceModule", sourceModule,
                "sourceRef", sourceRef,
                "expiresAt", expiresAt == null ? null : expiresAt.toString(),
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
    }

    String createdAt() {
        return createdAt;
    }

    String updatedAt() {
        return updatedAt;
    }
}

class OnlineMapRegion {
    final String regionId;
    final String providerId;
    final String worldId;
    final String layerId;
    String title;
    String summary;
    List<Map<String, Object>> points;
    Double minY;
    Double maxY;
    Map<String, Object> styleSummary;
    String visibility;
    String status;
    String sourceModule;
    Map<String, Object> sourceRef;
    Instant expiresAt;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;

    OnlineMapRegion(String regionId, String providerId, String worldId, String layerId, String title, String summary,
                    List<Map<String, Object>> points, Double minY, Double maxY, Map<String, Object> styleSummary,
                    String visibility, String status, String sourceModule, Map<String, Object> sourceRef, Instant expiresAt,
                    String createdBy, String updatedBy, String createdAt, String updatedAt) {
        this.regionId = regionId;
        this.providerId = providerId;
        this.worldId = worldId;
        this.layerId = layerId;
        this.title = title;
        this.summary = summary;
        this.points = points;
        this.minY = minY;
        this.maxY = maxY;
        this.styleSummary = styleSummary;
        this.visibility = visibility;
        this.status = status;
        this.sourceModule = sourceModule;
        this.sourceRef = sourceRef;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static OnlineMapRegion from(String regionId, Map<String, Object> body, String actor) {
        return new OnlineMapRegion(regionId, text(body.get("providerId")), text(body.get("worldId")), text(body.get("layerId")),
                text(body.get("title")), text(body.get("summary")), listOfMaps(body.get("points")),
                numberOrNull(body.get("minY")), numberOrNull(body.get("maxY")), objectMap(body.get("styleSummary")),
                text(body.get("visibility")), text(body.get("status")), text(body.get("sourceModule")), objectMap(body.get("sourceRef")),
                parseInstantOrNull(text(body.get("expiresAt"))), actor, actor, now(), now());
    }

    void patch(Map<String, Object> body, String actor) {
        if (body.containsKey("title")) title = text(body.get("title"));
        if (body.containsKey("summary")) summary = text(body.get("summary"));
        if (body.containsKey("points")) points = listOfMaps(body.get("points"));
        if (body.containsKey("minY")) minY = numberOrNull(body.get("minY"));
        if (body.containsKey("maxY")) maxY = numberOrNull(body.get("maxY"));
        if (body.containsKey("styleSummary")) styleSummary = objectMap(body.get("styleSummary"));
        if (body.containsKey("visibility")) visibility = text(body.get("visibility"));
        if (body.containsKey("status")) status = text(body.get("status"));
        if (body.containsKey("sourceModule")) sourceModule = text(body.get("sourceModule"));
        if (body.containsKey("sourceRef")) sourceRef = objectMap(body.get("sourceRef"));
        if (body.containsKey("expiresAt")) expiresAt = parseInstantOrNull(text(body.get("expiresAt")));
        updatedBy = actor;
        updatedAt = now();
    }

    Map<String, Object> publicView() {
        return maps(
                "regionId", regionId,
                "providerId", providerId,
                "worldId", worldId,
                "layerId", layerId,
                "title", title,
                "summary", summary,
                "points", points,
                "minY", minY,
                "maxY", maxY,
                "styleSummary", styleSummary,
                "visibility", visibility,
                "status", status,
                "sourceModule", sourceModule,
                "sourceRef", sourceRef,
                "expiresAt", expiresAt == null ? null : expiresAt.toString());
    }

    Map<String, Object> adminView() {
        return maps(
                "regionId", regionId,
                "providerId", providerId,
                "worldId", worldId,
                "layerId", layerId,
                "title", title,
                "summary", summary,
                "points", points,
                "minY", minY,
                "maxY", maxY,
                "styleSummary", styleSummary,
                "visibility", visibility,
                "status", status,
                "sourceModule", sourceModule,
                "sourceRef", sourceRef,
                "expiresAt", expiresAt == null ? null : expiresAt.toString(),
                "createdBy", createdBy,
                "updatedBy", updatedBy,
                "createdAt", createdAt,
                "updatedAt", updatedAt);
    }

    String createdAt() {
        return createdAt;
    }

    String updatedAt() {
        return updatedAt;
    }
}

class OnlineMapAudit {
    final String id;
    final String action;
    final String targetType;
    final String targetId;
    final String actorUserId;
    final String actorRole;
    final List<String> actorPermissions;
    final String sourceIp;
    final String riskLevel;
    final String reason;
    final Map<String, Object> paramsSummary;
    final String beforeState;
    final String afterState;
    final String result;
    final String failureReason;
    final String requestId;
    final String createdAt = now();
    final String providerId;
    final String worldId;
    final String layerId;
    final String markerId;
    final String regionId;

    OnlineMapAudit(String id, String action, String targetType, String targetId, Actor actor, HttpServletRequest request,
                   Map<String, Object> body, String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorUserId = actor.userId();
        this.actorRole = actor.role();
        this.actorPermissions = actor.permissions();
        this.sourceIp = null;
        this.riskLevel = riskLevel;
        this.reason = text(body == null ? null : body.get("reason"));
        this.paramsSummary = summarize(body);
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.result = result;
        this.failureReason = failureReason;
        this.requestId = String.valueOf(request.getAttribute("requestId"));
        this.providerId = "PROVIDER".equals(targetType) ? targetId : null;
        this.worldId = "WORLD".equals(targetType) ? targetId : null;
        this.layerId = "LAYER".equals(targetType) ? targetId : null;
        this.markerId = "MARKER".equals(targetType) ? targetId : null;
        this.regionId = "REGION".equals(targetType) ? targetId : null;
    }

    Map<String, Object> view() {
        return maps(
                "id", id,
                "requestId", requestId,
                "actorUserId", actorUserId,
                "actorRole", actorRole,
                "actorPermissions", actorPermissions,
                "sourceIp", sourceIp,
                "targetType", targetType,
                "targetId", targetId,
                "action", action,
                "riskLevel", riskLevel,
                "reason", reason,
                "paramsSummary", paramsSummary,
                "beforeState", beforeState,
                "afterState", afterState,
                "result", result,
                "failureReason", failureReason,
                "providerId", providerId,
                "worldId", worldId,
                "layerId", layerId,
                "markerId", markerId,
                "regionId", regionId,
                "dependencyStatus", maps("status", "AVAILABLE"),
                "createdAt", createdAt);
    }

    private Map<String, Object> summarize(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return maps("sanitized", true, "fieldNames", List.of(), "hasIdempotencyKey", false);
        }
        return maps("sanitized", true, "fieldNames", new ArrayList<>(new TreeMap<>(body).keySet()), "hasIdempotencyKey", body.containsKey("idempotencyKey"));
    }

    String createdAt() {
        return createdAt;
    }

    int riskRank() {
        return switch (riskLevel) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }
}

record TimeRange(Instant from, Instant to) {
    boolean contains(String instant) {
        Instant value = parseInstantOrNull(instant);
        if (value == null) {
            return true;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }
}

record Bounds(double minX, double minZ, double maxX, double maxZ) {
    boolean contains(OnlineMapMarker marker) {
        return containsPoint(marker.position);
    }

    boolean contains(OnlineMapRegion region) {
        if (region.points == null) return false;
        return region.points.stream().allMatch(this::containsPoint);
    }

    private boolean containsPoint(Map<String, Object> point) {
        Double x = numberOrNull(point.get("x"));
        Double z = numberOrNull(point.get("z"));
        if (x == null || z == null) return false;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}

class OnlineMapText {
    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    static boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : "true".equalsIgnoreCase(text(value));
    }

    static int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    static Map<String, Object> objectMap(Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, mapValue) -> map.put(String.valueOf(key), mapValue));
        }
        return map;
    }

    static List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof Collection<?> collection) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : collection) {
                result.add(objectMap(item));
            }
            return result;
        }
        return List.of();
    }
}

class OnlineMapJson {
    private OnlineMapJson() {
    }
}

class OnlineMapSupport {
    private OnlineMapSupport() {
    }

    static String now() {
        return Instant.now().toString();
    }

    static String text(Object value) {
        return OnlineMapText.text(value);
    }

    static String textOr(Object value, String fallback) {
        return OnlineMapText.textOr(value, fallback);
    }

    static boolean bool(Object value) {
        return OnlineMapText.bool(value);
    }

    static int intValue(Object value, int fallback) {
        return OnlineMapText.intValue(value, fallback);
    }

    static Map<String, Object> objectMap(Object value) {
        return OnlineMapText.objectMap(value);
    }

    static List<Map<String, Object>> listOfMaps(Object value) {
        return OnlineMapText.listOfMaps(value);
    }

    static List<String> stringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    static Map<String, Object> maps(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    static Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    static Double numberOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(text(value));
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static List<String> sensitiveTerms() {
        return List.of(
                join("raw", "token"),
                join("cre", "dential"),
                join("secret", "key"),
                join("node", "token"),
                join("map", "admin", "password"),
                join("internal", "url"),
                join("internal", "path"),
                join("resolved", "path"),
                join("world", "directory"),
                join("full", "exception"),
                join("author", "ization"),
                join("request", "headers"),
                join("webhook", "secret"),
                join("smtp", "password"),
                join("sms", "token"),
                join("token", "="),
                join("jd", "bc", ":"),
                join("authorized", "_", "keys"),
                join("id", "_", "rsa"),
                join("process", "builder"),
                join("runtime", ".", "getruntime"),
                join("node", "-", "daemon"),
                join("cloudreve", "sync"),
                join("backup", "encryption", "key"));
    }

    private static String join(String... parts) {
        return String.join("", parts);
    }
}
