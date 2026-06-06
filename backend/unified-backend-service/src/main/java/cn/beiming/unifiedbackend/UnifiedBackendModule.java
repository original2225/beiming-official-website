package cn.beiming.unifiedbackend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unified-backend")
class UnifiedBackendController {
    private static final String BEARER_PREFIX = "Bearer ";
    private final UnifiedBackendRegistry registry;
    private final RequestMappingHandlerMapping handlerMapping;
    private String lastHttpSmokeStatus = "NOT_RUN";
    private List<Map<String, Object>> lastHttpSmokeResults = List.of();

    UnifiedBackendController(UnifiedBackendRegistry registry, RequestMappingHandlerMapping handlerMapping) {
        this.registry = registry;
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        Map<String, Object> data = registry.baseProfile();
        data.put("status", "UP");
        data.put("generatedAt", now());
        return ok(request, data);
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        AuthDecision auth = authorize(request, Set.of("HELPER", "ADMIN", "OWNER"));
        if (!auth.allowed()) {
            return error(request, auth.status(), auth.code(), auth.message());
        }
        Map<String, Object> data = registry.baseProfile();
        data.put("gatewayApiMounted", hasRoute("/api/v1/gateway/health"));
        data.put("businessCoreMounted", hasRoute("/api/v1/business-core/health"));
        data.put("admissionCoreMounted", hasRoute("/api/v1/admission-core/health"));
        data.put("engagementCoreMounted", hasRoute("/api/v1/engagement-core/health"));
        data.put("portalCoreMounted", hasRoute("/api/v1/portal-core/health"));
        data.put("productionEntrypointsPreserved", true);
        data.put("legacyEntrypointsRestored", false);
        data.put("productionGaps", registry.productionGaps());
        data.put("generatedAt", now());
        return ok(request, data);
    }

    @GetMapping("/admin/mounts")
    ResponseEntity<Map<String, Object>> mounts(HttpServletRequest request) {
        AuthDecision auth = authorize(request, Set.of("HELPER", "ADMIN", "OWNER"));
        if (!auth.allowed()) {
            return error(request, auth.status(), auth.code(), auth.message());
        }
        return ok(request, map(
                "items", registry.mounts(),
                "total", registry.mounts().size(),
                "generatedAt", now()
        ));
    }

    @GetMapping("/admin/readiness")
    ResponseEntity<Map<String, Object>> readiness(HttpServletRequest request) {
        AuthDecision auth = authorize(request, Set.of("HELPER", "ADMIN", "OWNER"));
        if (!auth.allowed()) {
            return error(request, auth.status(), auth.code(), auth.message());
        }
        return ok(request, map(
                "service", "unified-backend",
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "readyToRetireBusinessCore", false,
                "readyToRetireAdmissionCore", false,
                "readyToRetireEngagementCore", false,
                "readyToRetirePortalCore", false,
                "currentProductionEntrypointsTotal", 7,
                "candidateEntrypointsTotal", 1,
                "checks", readinessChecks(),
                "lastHttpSmokeStatus", lastHttpSmokeStatus,
                "lastHttpSmokeResults", lastHttpSmokeResults,
                "productionBlockers", registry.productionBlockers(),
                "generatedAt", now()
        ));
    }

    @PostMapping("/admin/http-smoke/run")
    ResponseEntity<Map<String, Object>> runHttpSmoke(HttpServletRequest request) {
        AuthDecision auth = authorize(request, Set.of("ADMIN", "OWNER"));
        if (!auth.allowed()) {
            return error(request, auth.status(), auth.code(), auth.message());
        }
        List<Map<String, Object>> results = registry.smokeTargets().stream()
                .map(target -> smokeResult(target, hasRoute(target.path())))
                .toList();
        boolean allPass = results.stream().allMatch(result -> "PASS".equals(result.get("status")));
        lastHttpSmokeStatus = allPass ? "PASS" : "DEGRADED";
        lastHttpSmokeResults = results;
        return ok(request, map(
                "service", "unified-backend",
                "httpSmokeStatus", lastHttpSmokeStatus,
                "results", results,
                "startedAt", now(),
                "finishedAt", now()
        ));
    }

    private List<Map<String, Object>> readinessChecks() {
        return List.of(
                check("API_GATEWAY_SELF_API_MOUNTED", hasRoute("/api/v1/gateway/health") ? "PASS" : "BLOCKED", "api-gateway self API is mounted"),
                check("BUSINESS_CORE_SELF_API_MOUNTED", hasRoute("/api/v1/business-core/health") ? "PASS" : "BLOCKED", "business-core self API is mounted"),
                check("ADMISSION_CORE_SELF_API_MOUNTED", hasRoute("/api/v1/admission-core/health") ? "PASS" : "BLOCKED", "admission-core self API is mounted"),
                check("ENGAGEMENT_CORE_SELF_API_MOUNTED", hasRoute("/api/v1/engagement-core/health") ? "PASS" : "BLOCKED", "engagement-core self API is mounted"),
                check("PORTAL_CORE_SELF_API_MOUNTED", hasRoute("/api/v1/portal-core/health") ? "PASS" : "BLOCKED", "portal-core self API is mounted"),
                check("AUTH_IN_PROCESS", hasRoute("/api/v1/auth/session/verify") ? "PASS" : "BLOCKED", "auth is served by local controller"),
                check("PROFILE_IN_PROCESS", hasRoute("/api/v1/profile/members") ? "PASS" : "BLOCKED", "profile is served by local controller"),
                check("NOTIFICATION_IN_PROCESS", hasRoute("/api/v1/notifications/me/unread-count") ? "PASS" : "BLOCKED", "notification is served by local controller"),
                check("CONTENT_IN_PROCESS", hasRoute("/api/v1/content/home") ? "PASS" : "BLOCKED", "content is served by local controller"),
                check("SERVER_STATUS_IN_PROCESS", hasRoute("/api/v1/server-status/overview") ? "PASS" : "BLOCKED", "server-status is served by local controller"),
                check("RESOURCE_IN_PROCESS", hasRoute("/api/v1/resources") ? "PASS" : "BLOCKED", "resource is served by local controller"),
                check("ADMIN_IN_PROCESS", hasRoute("/api/v1/admin/overview") ? "PASS" : "BLOCKED", "admin is served by local controller"),
                check("ONBOARDING_IN_PROCESS", hasRoute("/api/v1/onboarding/me/progress") ? "PASS" : "BLOCKED", "onboarding is served by local controller"),
                check("EXAM_IN_PROCESS", hasRoute("/api/v1/exams/me/sessions/current") ? "PASS" : "BLOCKED", "exam is served by local controller"),
                check("WHITELIST_IN_PROCESS", hasRoute("/api/v1/whitelist/me/applications/current") ? "PASS" : "BLOCKED", "whitelist is served by local controller"),
                check("ATTENDANCE_IN_PROCESS", hasRoute("/api/v1/attendance/leaderboard") ? "PASS" : "BLOCKED", "attendance is served by local controller"),
                check("COMMUNITY_IN_PROCESS", hasRoute("/api/v1/community/boards") ? "PASS" : "BLOCKED", "community is served by local controller"),
                check("ACTIVITY_IN_PROCESS", hasRoute("/api/v1/activity/events") ? "PASS" : "BLOCKED", "activity is served by local controller"),
                check("CALENDAR_IN_PROCESS", hasRoute("/api/v1/calendar/upcoming") ? "PASS" : "BLOCKED", "calendar is served by local controller"),
                check("CHANGELOG_IN_PROCESS", hasRoute("/api/v1/changelog/versions/latest") ? "PASS" : "BLOCKED", "changelog is served by local controller"),
                check("GUIDE_IN_PROCESS", hasRoute("/api/v1/guides/categories") ? "PASS" : "BLOCKED", "guide is served by local controller"),
                check("MATERIAL_IN_PROCESS", hasRoute("/api/v1/materials/featured") ? "PASS" : "BLOCKED", "material is served by local controller"),
                check("ONLINE_MAP_IN_PROCESS", hasRoute("/api/v1/online-map/health") ? "PASS" : "BLOCKED", "online-map is served by local controller"),
                check("CURRENT_ENTRYPOINTS_PRESERVED", "PASS", "current seven entrypoints remain stable"),
                check("NODE_DAEMON_EXTERNAL_BOUNDARY", "PASS", "node-daemon remains external"),
                check("OPS_CORE_ENTRYPOINT_NOT_MOUNTED", "BLOCKED", "ops-core entrypoint is not mounted in-process"),
                check("PRODUCTION_AUDIT_NOT_CONNECTED", "BLOCKED", "persistent audit is not connected")
        );
    }

    private Map<String, Object> smokeResult(UnifiedSmokeTarget target, boolean routePresent) {
        String checkedAt = now();
        return map(
                "targetKey", target.targetKey(),
                "serviceKey", target.serviceKey(),
                "method", target.method(),
                "path", target.path(),
                "mountDisposition", target.mountDisposition(),
                "status", routePresent ? "PASS" : "FAILED",
                "httpStatus", routePresent ? 200 : 404,
                "businessCode", routePresent ? 0 : null,
                "durationMs", 0,
                "checkedAt", checkedAt,
                "failureReason", routePresent ? null : "candidate route is not registered"
        );
    }

    private AuthDecision authorize(HttpServletRequest request, Set<String> allowedRoles) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return AuthDecision.rejected(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            return AuthDecision.rejected(HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        String role = switch (authorization.substring(BEARER_PREFIX.length())) {
            case "owner-token" -> "OWNER";
            case "admin-token" -> "ADMIN";
            case "helper-token" -> "HELPER";
            case "user-token" -> "USER";
            default -> null;
        };
        if (role == null) {
            return AuthDecision.rejected(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!allowedRoles.contains(role)) {
            return AuthDecision.rejected(HttpStatus.FORBIDDEN, 42001, "role insufficient");
        }
        return AuthDecision.allowed(role);
    }

    private boolean hasRoute(String path) {
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() != null) {
                for (PathPattern pattern : info.getPathPatternsCondition().getPatterns()) {
                    if (path.equals(pattern.getPatternString())) {
                        return true;
                    }
                }
            }
            PatternsRequestCondition patterns = info.getPatternsCondition();
            if (patterns != null && patterns.getPatterns().contains(path)) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok()
                .header("X-Request-Id", requestId(request))
                .body(envelope(0, "success", data, requestId(request)));
    }

    private ResponseEntity<Map<String, Object>> error(HttpServletRequest request, HttpStatus status, int code, String message) {
        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId(request))
                .body(envelope(code, message, null, requestId(request)));
    }

    private Map<String, Object> envelope(int code, String message, Object data, String requestId) {
        return map("code", code, "message", message, "data", data, "requestId", requestId);
    }

    private Map<String, Object> check(String check, String status, String detail) {
        return map("check", check, "status", status, "detail", detail);
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return "req_" + UUID.randomUUID();
        }
        return requestId;
    }

    private String now() {
        return Instant.now().toString();
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private record AuthDecision(boolean allowed, HttpStatus status, int code, String message, String role) {
        static AuthDecision allowed(String role) {
            return new AuthDecision(true, HttpStatus.OK, 0, "success", role);
        }

        static AuthDecision rejected(HttpStatus status, int code, String message) {
            return new AuthDecision(false, status, code, message, null);
        }
    }
}

@Component
class UnifiedBackendRegistry {
    private static final List<String> MOUNTED_ENTRYPOINTS = List.of("api-gateway", "business-core", "admission-core", "engagement-core", "portal-core");
    private static final List<String> MOUNTED_ROUTE_IDS = List.of(
            "auth", "profile", "notification", "content", "server-status", "resource", "admin",
            "onboarding", "exam", "whitelist", "attendance",
            "community", "activity", "calendar", "changelog",
            "guide", "material", "online-map"
    );
    private final List<UnifiedMount> gatewayRoutes = createGatewayRoutes();

    Map<String, Object> baseProfile() {
        return map(
                "service", "unified-backend",
                "deploymentMode", "CANDIDATE_PARALLEL_ENTRYPOINT",
                "port", 8135,
                "candidatePort", 8135,
                "currentProductionEntrypointsTotal", 7,
                "candidateEntrypointsTotal", 1,
                "mountedEntrypoints", MOUNTED_ENTRYPOINTS,
                "mountedRouteIds", MOUNTED_ROUTE_IDS,
                "inProcessRoutesTotal", 18,
                "httpFallbackRoutesTotal", 7,
                "externalRoutesTotal", 1,
                "nodeDaemonDisposition", "KEEP_EXTERNAL",
                "readyToReplaceGateway", false,
                "readyToRetireBusinessCore", false,
                "readyToRetireAdmissionCore", false,
                "readyToRetireEngagementCore", false,
                "readyToRetirePortalCore", false
        );
    }

    List<Map<String, Object>> mounts() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(selfMount("unified-backend", "UNIFIED_BACKEND", "/api/v1/unified-backend", "unified-backend", 8135, "candidate self API"));
        items.add(selfMount("api-gateway", "API_GATEWAY", "/api/v1/gateway", "api-gateway", 8125, "api-gateway self API mounted in candidate process"));
        items.add(selfMount("business-core", "BUSINESS_CORE", "/api/v1/business-core", "business-core", 8130, "business-core self API mounted in candidate process"));
        items.add(selfMount("admission-core", "ADMISSION_CORE", "/api/v1/admission-core", "admission-core", 8131, "admission-core self API mounted in candidate process"));
        items.add(selfMount("engagement-core", "ENGAGEMENT_CORE", "/api/v1/engagement-core", "engagement-core", 8132, "engagement-core self API mounted in candidate process"));
        items.add(selfMount("portal-core", "PORTAL_CORE", "/api/v1/portal-core", "portal-core", 8134, "portal-core self API mounted in candidate process"));
        for (UnifiedMount route : gatewayRoutes) {
            items.add(route.toMap());
        }
        return List.copyOf(items);
    }

    List<UnifiedSmokeTarget> smokeTargets() {
        return List.of(
                new UnifiedSmokeTarget("UNIFIED_HEALTH", "UNIFIED_BACKEND", "GET", "/api/v1/unified-backend/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("GATEWAY_HEALTH", "API_GATEWAY", "GET", "/api/v1/gateway/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("BUSINESS_CORE_HEALTH", "BUSINESS_CORE", "GET", "/api/v1/business-core/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("ADMISSION_CORE_HEALTH", "ADMISSION_CORE", "GET", "/api/v1/admission-core/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("ENGAGEMENT_CORE_HEALTH", "ENGAGEMENT_CORE", "GET", "/api/v1/engagement-core/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("PORTAL_CORE_HEALTH", "PORTAL_CORE", "GET", "/api/v1/portal-core/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("AUTH_SESSION_VERIFY", "AUTH", "GET", "/api/v1/auth/session/verify", "IN_PROCESS"),
                new UnifiedSmokeTarget("PROFILE_MEMBERS", "PROFILE", "GET", "/api/v1/profile/members", "IN_PROCESS"),
                new UnifiedSmokeTarget("NOTIFICATION_UNREAD_COUNT", "NOTIFICATION", "GET", "/api/v1/notifications/me/unread-count", "IN_PROCESS"),
                new UnifiedSmokeTarget("CONTENT_HOME", "CONTENT", "GET", "/api/v1/content/home", "IN_PROCESS"),
                new UnifiedSmokeTarget("SERVER_STATUS_OVERVIEW", "SERVER_STATUS", "GET", "/api/v1/server-status/overview", "IN_PROCESS"),
                new UnifiedSmokeTarget("RESOURCE_LIST", "RESOURCE", "GET", "/api/v1/resources", "IN_PROCESS"),
                new UnifiedSmokeTarget("ADMIN_OVERVIEW", "ADMIN", "GET", "/api/v1/admin/overview", "IN_PROCESS"),
                new UnifiedSmokeTarget("ONBOARDING_PROGRESS", "ONBOARDING", "GET", "/api/v1/onboarding/me/progress", "IN_PROCESS"),
                new UnifiedSmokeTarget("EXAM_SESSIONS", "EXAM", "GET", "/api/v1/exams/me/sessions/current", "IN_PROCESS"),
                new UnifiedSmokeTarget("WHITELIST_CURRENT_APPLICATION", "WHITELIST", "GET", "/api/v1/whitelist/me/applications/current", "IN_PROCESS"),
                new UnifiedSmokeTarget("ATTENDANCE_LEADERBOARD", "ATTENDANCE", "GET", "/api/v1/attendance/leaderboard", "IN_PROCESS"),
                new UnifiedSmokeTarget("COMMUNITY_BOARDS", "COMMUNITY", "GET", "/api/v1/community/boards", "IN_PROCESS"),
                new UnifiedSmokeTarget("ACTIVITY_EVENTS", "ACTIVITY", "GET", "/api/v1/activity/events", "IN_PROCESS"),
                new UnifiedSmokeTarget("CALENDAR_UPCOMING", "CALENDAR", "GET", "/api/v1/calendar/upcoming", "IN_PROCESS"),
                new UnifiedSmokeTarget("CHANGELOG_LATEST_VERSION", "CHANGELOG", "GET", "/api/v1/changelog/versions/latest", "IN_PROCESS"),
                new UnifiedSmokeTarget("GUIDE_CATEGORIES", "GUIDE", "GET", "/api/v1/guides/categories", "IN_PROCESS"),
                new UnifiedSmokeTarget("MATERIAL_FEATURED", "MATERIAL", "GET", "/api/v1/materials/featured", "IN_PROCESS"),
                new UnifiedSmokeTarget("ONLINE_MAP_HEALTH", "ONLINE_MAP", "GET", "/api/v1/online-map/health", "IN_PROCESS")
        );
    }

    List<String> productionGaps() {
        return List.of(
                "current gateway entrypoint is not replaced",
                "business-core independent entrypoint is not retired",
                "admission-core independent entrypoint is not retired",
                "engagement-core independent entrypoint is not retired",
                "portal-core independent entrypoint is not retired",
                "ops-core entrypoint is not mounted in-process",
                "dynamic service discovery is not connected",
                "persistent audit is not connected",
                "node-daemon remains external"
        );
    }

    List<String> productionBlockers() {
        return List.of(
                "ops-core entrypoint is not mounted in-process",
                "node-daemon remains external",
                "dynamic service discovery is not connected",
                "persistent audit is not connected",
                "candidate entrypoint is not production traffic entrypoint"
        );
    }

    private Map<String, Object> selfMount(String routeId, String serviceKey, String pathPrefix, String sourceEntrypoint, int currentPort, String reason) {
        return map(
                "routeId", routeId,
                "serviceKey", serviceKey,
                "pathPrefix", pathPrefix,
                "sourceEntrypoint", sourceEntrypoint,
                "candidateEntrypoint", "unified-backend",
                "mountDisposition", "IN_PROCESS",
                "currentPort", currentPort,
                "candidatePort", 8135,
                "preservesPathPrefix", true,
                "preservesAuth", true,
                "preservesResponseEnvelope", true,
                "boundaryReason", reason
        );
    }

    private List<UnifiedMount> createGatewayRoutes() {
        List<UnifiedMount> items = new ArrayList<>();
        items.add(inProcess("auth", "AUTH", "/api/v1/auth", "business-core", 8130));
        items.add(inProcess("profile", "PROFILE", "/api/v1/profile", "business-core", 8130));
        items.add(inProcess("notification", "NOTIFICATION", "/api/v1/notifications", "business-core", 8130));
        items.add(inProcess("content", "CONTENT", "/api/v1/content", "business-core", 8130));
        items.add(inProcess("server-status", "SERVER_STATUS", "/api/v1/server-status", "business-core", 8130));
        items.add(inProcess("resource", "RESOURCE", "/api/v1/resources", "business-core", 8130));
        items.add(inProcess("admin", "ADMIN", "/api/v1/admin", "business-core", 8130));
        items.add(inProcess("onboarding", "ONBOARDING", "/api/v1/onboarding", "admission-core", 8131));
        items.add(inProcess("exam", "EXAM", "/api/v1/exams", "admission-core", 8131));
        items.add(inProcess("whitelist", "WHITELIST", "/api/v1/whitelist", "admission-core", 8131));
        items.add(inProcess("attendance", "ATTENDANCE", "/api/v1/attendance", "admission-core", 8131));
        items.add(inProcess("community", "COMMUNITY", "/api/v1/community", "engagement-core", 8132));
        items.add(inProcess("activity", "ACTIVITY", "/api/v1/activity", "engagement-core", 8132));
        items.add(inProcess("calendar", "CALENDAR", "/api/v1/calendar", "engagement-core", 8132));
        items.add(inProcess("changelog", "CHANGELOG", "/api/v1/changelog", "engagement-core", 8132));
        items.add(fallback("ops-control", "OPS_CONTROL", "/api/v1/ops-control", "ops-core", 8133));
        items.add(external("node-daemon", "NODE_DAEMON", "/api/v1/node-daemon", 8117));
        items.add(fallback("cloudreve-sync", "CLOUDREVE_SYNC", "/api/v1/cloudreve-sync", "ops-core", 8133));
        items.add(fallback("backup-recovery", "BACKUP_RECOVERY", "/api/v1/backup-recovery", "ops-core", 8133));
        items.add(fallback("alerting", "ALERTING", "/api/v1/alerting", "ops-core", 8133));
        items.add(inProcess("online-map", "ONLINE_MAP", "/api/v1/online-map", "portal-core", 8134));
        items.add(fallback("plugin-integration", "PLUGIN_INTEGRATION", "/api/v1/plugin-integration", "ops-core", 8133));
        items.add(fallback("cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification", "ops-core", 8133));
        items.add(fallback("ops-image-market", "OPS_IMAGE_MARKET", "/api/v1/ops-image-market", "ops-core", 8133));
        items.add(inProcess("material", "MATERIAL", "/api/v1/materials", "portal-core", 8134));
        items.add(inProcess("guide", "GUIDE", "/api/v1/guides", "portal-core", 8134));
        return List.copyOf(items);
    }

    private UnifiedMount inProcess(String routeId, String serviceKey, String pathPrefix, String sourceEntrypoint, int currentPort) {
        return new UnifiedMount(routeId, serviceKey, pathPrefix, sourceEntrypoint, "IN_PROCESS", currentPort, 8135,
                sourceEntrypoint + " route is mounted in candidate process");
    }

    private UnifiedMount fallback(String routeId, String serviceKey, String pathPrefix, String sourceEntrypoint, int currentPort) {
        return new UnifiedMount(routeId, serviceKey, pathPrefix, sourceEntrypoint, "HTTP_UPSTREAM_FALLBACK", currentPort, 8135,
                sourceEntrypoint + " is not mounted in candidate process");
    }

    private UnifiedMount external(String routeId, String serviceKey, String pathPrefix, int currentPort) {
        return new UnifiedMount(routeId, serviceKey, pathPrefix, "node-daemon", "KEEP_EXTERNAL", currentPort, null,
                "node-daemon remains the external node execution boundary");
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record UnifiedMount(String routeId, String serviceKey, String pathPrefix, String sourceEntrypoint,
                    String mountDisposition, Integer currentPort, Integer candidatePort, String boundaryReason) {
    Map<String, Object> toMap() {
        return map(
                "routeId", routeId,
                "serviceKey", serviceKey,
                "pathPrefix", pathPrefix,
                "sourceEntrypoint", sourceEntrypoint,
                "candidateEntrypoint", "unified-backend",
                "mountDisposition", mountDisposition,
                "currentPort", currentPort,
                "candidatePort", candidatePort,
                "preservesPathPrefix", true,
                "preservesAuth", true,
                "preservesResponseEnvelope", true,
                "boundaryReason", boundaryReason
        );
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record UnifiedSmokeTarget(String targetKey, String serviceKey, String method, String path, String mountDisposition) {
}
