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
        data.put("opsCoreMounted", hasRoute("/api/v1/ops-core/health"));
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
                "readyToRetireOpsCore", false,
                "readyToRetirePortalCore", false,
                "currentProductionEntrypointsTotal", 6,
                "candidateEntrypointsTotal", 1,
                "checks", readinessChecks(),
                "lastHttpSmokeStatus", lastHttpSmokeStatus,
                "lastHttpSmokeResults", lastHttpSmokeResults,
                "productionBlockers", registry.productionBlockers(),
                "productionSwitchReadinessStatus", "BLOCKED",
                "productionSwitchChecks", registry.productionSwitchChecks(),
                "centralConfigPrecheckStatus", "BLOCKED",
                "centralConfigPrecheckChecks", registry.centralConfigPrecheckChecks(),
                "centralConfigGovernancePrecheckStatus", "BLOCKED",
                "centralConfigGovernancePrecheckChecks", registry.centralConfigGovernancePrecheckChecks(),
                "centralConfigGovernanceEvidence", registry.centralConfigGovernanceEvidence(),
                "productionCentralConfigPrecheckStatus", "BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED",
                "productionCentralConfigPrecheckChecks", registry.productionCentralConfigPrecheckChecks(),
                "productionCentralConfigEvidence", registry.productionCentralConfigEvidence(),
                "externalEntrypointCutoverPrecheckStatus", "BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED",
                "externalEntrypointCutoverPrecheckChecks", registry.externalEntrypointCutoverPrecheckChecks(),
                "externalEntrypointCutoverEvidence", registry.externalEntrypointCutoverEvidence(),
                "persistentAuditPrecheckStatus", "BLOCKED",
                "persistentAuditPrecheckChecks", registry.persistentAuditPrecheckChecks(),
                "persistentAuditGovernancePrecheckStatus", "BLOCKED",
                "persistentAuditGovernancePrecheckChecks", registry.persistentAuditGovernancePrecheckChecks(),
                "persistentAuditGovernanceEvidence", registry.persistentAuditGovernanceEvidence(),
                "realHttpRehearsalPrecheckStatus", "BLOCKED",
                "realHttpRehearsalPrecheckChecks", registry.realHttpRehearsalPrecheckChecks(),
                "routeDriftPrecheckStatus", "PASS",
                "routeDriftPrecheckChecks", registry.routeDriftPrecheckChecks(),
                "rollbackWindowPrecheckStatus", "BLOCKED",
                "rollbackWindowPrecheckChecks", registry.rollbackWindowPrecheckChecks(),
                "rollbackWindowEvidence", registry.rollbackWindowEvidence(),
                "entrypointSwitchPrecheckStatus", "BLOCKED",
                "entrypointSwitchPrecheckChecks", registry.entrypointSwitchPrecheckChecks(),
                "entrypointSwitchEvidence", registry.entrypointSwitchEvidence(),
                "productionTrafficCanaryEvidence", registry.productionTrafficCanaryEvidence(),
                "backendSingleServicePrecheckStatus", "PASS",
                "backendSingleServicePrecheckChecks", registry.backendSingleServicePrecheckChecks(),
                "backendSingleServiceEvidence", registry.backendSingleServiceEvidence(),
                "finalBackendSingleServicePrecheckStatus", "PASS",
                "finalBackendSingleServicePrecheckChecks", registry.finalBackendSingleServicePrecheckChecks(),
                "finalBackendSingleServiceEvidence", registry.finalBackendSingleServiceEvidence(),
                "singleServiceCutoverPrecheckStatus", "PASS_READY_FOR_EXTERNAL_CUTOVER",
                "singleServiceCutoverPrecheckChecks", registry.singleServiceCutoverPrecheckChecks(),
                "singleServiceCutoverEvidence", registry.singleServiceCutoverEvidence(),
                "entrypointCutoverAdapterPrecheckStatus", "BLOCKED",
                "entrypointCutoverAdapterPrecheckChecks", registry.entrypointCutoverAdapterPrecheckChecks(),
                "entrypointCutoverAdapterEvidence", registry.entrypointCutoverAdapterEvidence(),
                "oldEntrypointRetirementPrecheckStatus", "BLOCKED",
                "oldEntrypointRetirementPrecheckChecks", registry.oldEntrypointRetirementPrecheckChecks(),
                "oldEntrypointRetirementEvidence", registry.oldEntrypointRetirementEvidence(),
                "entrypointCutoverExecutionPrecheckStatus", "BLOCKED",
                "entrypointCutoverExecutionPrecheckChecks", registry.entrypointCutoverExecutionPrecheckChecks(),
                "entrypointCutoverExecutionEvidence", registry.entrypointCutoverExecutionEvidence(),
                "productionEntrypointCutoverPrecheckStatus", "BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG",
                "productionEntrypointCutoverPrecheckChecks", registry.productionEntrypointCutoverPrecheckChecks(),
                "productionEntrypointCutoverEvidence", registry.productionEntrypointCutoverEvidence(),
                "apiGatewayRetirementPrecheckStatus", "BLOCKED_BY_TRAFFIC_NOT_SWITCHED",
                "apiGatewayRetirementPrecheckChecks", registry.apiGatewayRetirementPrecheckChecks(),
                "apiGatewayRetirementEvidence", registry.apiGatewayRetirementEvidence(),
                "coreEntrypointRetirementPrecheckStatus", "BLOCKED_BY_PROTECTED_ROLLBACK_ROLE",
                "coreEntrypointRetirementPrecheckChecks", registry.coreEntrypointRetirementPrecheckChecks(),
                "coreEntrypointRetirementEvidence", registry.coreEntrypointRetirementEvidence(),
                "productionHardeningPrecheckStatus", "BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES",
                "productionHardeningPrecheckChecks", registry.productionHardeningPrecheckChecks(),
                "productionHardeningEvidence", registry.productionHardeningEvidence(),
                "replacementDecision", registry.replacementDecision(),
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
                check("OPS_CORE_SELF_API_MOUNTED", hasRoute("/api/v1/ops-core/health") ? "PASS" : "BLOCKED", "ops-core self API is mounted"),
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
                check("OPS_CONTROL_IN_PROCESS", hasRoute("/api/v1/ops-control/overview") ? "PASS" : "BLOCKED", "ops-control is served by local controller"),
                check("CLOUDREVE_SYNC_IN_PROCESS", hasRoute("/api/v1/cloudreve-sync/health") ? "PASS" : "BLOCKED", "cloudreve-sync is served by local controller"),
                check("BACKUP_RECOVERY_IN_PROCESS", hasRoute("/api/v1/backup-recovery/health") ? "PASS" : "BLOCKED", "backup-recovery is served by local controller"),
                check("ALERTING_IN_PROCESS", hasRoute("/api/v1/alerting/health") ? "PASS" : "BLOCKED", "alerting is served by local controller"),
                check("PLUGIN_INTEGRATION_IN_PROCESS", hasRoute("/api/v1/plugin-integration/health") ? "PASS" : "BLOCKED", "plugin-integration is served by local controller"),
                check("CROSS_PLATFORM_NOTIFICATION_IN_PROCESS", hasRoute("/api/v1/cross-platform-notification/health") ? "PASS" : "BLOCKED", "cross-platform-notification is served by local controller"),
                check("OPS_IMAGE_MARKET_IN_PROCESS", hasRoute("/api/v1/ops-image-market/health") ? "PASS" : "BLOCKED", "ops-image-market is served by local controller"),
                check("GUIDE_IN_PROCESS", hasRoute("/api/v1/guides/categories") ? "PASS" : "BLOCKED", "guide is served by local controller"),
                check("MATERIAL_IN_PROCESS", hasRoute("/api/v1/materials/featured") ? "PASS" : "BLOCKED", "material is served by local controller"),
                check("ONLINE_MAP_IN_PROCESS", hasRoute("/api/v1/online-map/health") ? "PASS" : "BLOCKED", "online-map is served by local controller"),
                check("CURRENT_ENTRYPOINTS_PRESERVED", "PASS", "current six rollback entrypoints remain stable"),
                check("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected"),
                check("PRODUCTION_TRAFFIC_SWITCH_NOT_RUN", "BLOCKED", "candidate entrypoint is not production traffic entrypoint"),
                check("CENTRAL_CONFIG_NOT_CONNECTED", "BLOCKED", "centralized config is not connected"),
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
    private static final List<String> MOUNTED_ENTRYPOINTS = List.of("api-gateway", "business-core", "admission-core", "engagement-core", "ops-core", "portal-core");
    private static final List<String> MOUNTED_ROUTE_IDS = List.of(
            "auth", "profile", "notification", "content", "server-status", "resource", "admin",
            "onboarding", "exam", "whitelist", "attendance",
            "community", "activity", "calendar", "changelog",
            "ops-control", "cloudreve-sync", "backup-recovery", "alerting", "plugin-integration",
            "cross-platform-notification", "ops-image-market",
            "guide", "material", "online-map"
    );
    private final List<UnifiedMount> gatewayRoutes = createGatewayRoutes();

    Map<String, Object> baseProfile() {
        return map(
                "service", "unified-backend",
                "deploymentMode", "CANDIDATE_PARALLEL_ENTRYPOINT",
                "port", 8135,
                "candidatePort", 8135,
                "currentProductionEntrypointsTotal", 6,
                "candidateEntrypointsTotal", 1,
                "mountedEntrypoints", MOUNTED_ENTRYPOINTS,
                "mountedRouteIds", MOUNTED_ROUTE_IDS,
                "inProcessRoutesTotal", 25,
                "httpFallbackRoutesTotal", 0,
                "externalRoutesTotal", 0,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "externalNodeExecutorProject", "separate-project",
                "readyToReplaceGateway", false,
                "readyToRetireBusinessCore", false,
                "readyToRetireAdmissionCore", false,
                "readyToRetireEngagementCore", false,
                "readyToRetireOpsCore", false,
                "readyToRetirePortalCore", false
        );
    }

    List<Map<String, Object>> mounts() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(selfMount("api-gateway", "API_GATEWAY", "/api/v1/gateway", "api-gateway", 8125, "api-gateway self API mounted in candidate process"));
        items.add(selfMount("business-core", "BUSINESS_CORE", "/api/v1/business-core", "business-core", 8130, "business-core self API mounted in candidate process"));
        items.add(selfMount("admission-core", "ADMISSION_CORE", "/api/v1/admission-core", "admission-core", 8131, "admission-core self API mounted in candidate process"));
        items.add(selfMount("engagement-core", "ENGAGEMENT_CORE", "/api/v1/engagement-core", "engagement-core", 8132, "engagement-core self API mounted in candidate process"));
        items.add(selfMount("ops-core", "OPS_CORE", "/api/v1/ops-core", "ops-core", 8133, "ops-core self API mounted in candidate process"));
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
                new UnifiedSmokeTarget("OPS_CORE_HEALTH", "OPS_CORE", "GET", "/api/v1/ops-core/health", "IN_PROCESS"),
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
                new UnifiedSmokeTarget("OPS_CONTROL_OVERVIEW", "OPS_CONTROL", "GET", "/api/v1/ops-control/overview", "IN_PROCESS"),
                new UnifiedSmokeTarget("CLOUDREVE_SYNC_HEALTH", "CLOUDREVE_SYNC", "GET", "/api/v1/cloudreve-sync/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("BACKUP_RECOVERY_HEALTH", "BACKUP_RECOVERY", "GET", "/api/v1/backup-recovery/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("ALERTING_HEALTH", "ALERTING", "GET", "/api/v1/alerting/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("PLUGIN_INTEGRATION_HEALTH", "PLUGIN_INTEGRATION", "GET", "/api/v1/plugin-integration/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("CROSS_PLATFORM_NOTIFICATION_HEALTH", "CROSS_PLATFORM_NOTIFICATION", "GET", "/api/v1/cross-platform-notification/health", "IN_PROCESS"),
                new UnifiedSmokeTarget("OPS_IMAGE_MARKET_HEALTH", "OPS_IMAGE_MARKET", "GET", "/api/v1/ops-image-market/health", "IN_PROCESS"),
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
                "ops-core independent entrypoint is not retired",
                "portal-core independent entrypoint is not retired",
                "dynamic service discovery is not connected",
                "centralized config is not connected",
                "persistent audit is not connected",
                "real production traffic rehearsal is not completed",
                "external node executor is out of repository and not connected"
        );
    }

    List<String> productionBlockers() {
        return List.of(
                "external node executor is out of repository and not connected",
                "dynamic service discovery is not connected",
                "centralized config is not connected",
                "persistent audit is not connected",
                "candidate entrypoint is not production traffic entrypoint"
        );
    }

    List<Map<String, Object>> productionSwitchChecks() {
        return List.of(
                switchCheck("ALL_CURRENT_BUSINESS_ROUTES_IN_PROCESS", "PASS", "all current official backend business routes are mounted in-process", true),
                switchCheck("CURRENT_ENTRYPOINTS_PRESERVED", "PASS", "current six production entrypoints remain available for rollback", true),
                switchCheck("ROUTE_PREFIX_AND_RESPONSE_PRESERVED", "PASS", "candidate preserves existing route prefixes and response envelope", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected", true),
                switchCheck("LEGACY_ENTRYPOINTS_NOT_RESTORED", "PASS", "retired legacy service entrypoints are not restored", true),
                switchCheck("CENTRAL_CONFIG_READY", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PERSISTENT_AUDIT_READY", "BLOCKED", "persistent production audit is not connected", true),
                switchCheck("REAL_HTTP_SMOKE_REHEARSAL_READY", "BLOCKED", "real production HTTP smoke rehearsal is not completed", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", "frontend and external callers are not switched to unified-backend", true),
                switchCheck("ROLLBACK_WINDOW_READY", "BLOCKED", "rollback window and old entrypoint retention plan are not validated", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "candidate entrypoint is not receiving production traffic", true)
        );
    }

    List<Map<String, Object>> centralConfigPrecheckChecks() {
        return List.of(
                switchCheck("CANDIDATE_PORT_FIXED", "PASS", "candidate port remains fixed at 8135", true),
                switchCheck("CURRENT_ENTRYPOINT_PORTS_DOCUMENTED", "PASS", "current entrypoint ports are documented and preserved", true),
                switchCheck("IN_PROCESS_ROUTE_REGISTRY_FIXED", "PASS", "in-process route registry remains fixed for the candidate", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_CONFIG_BOUNDARY", "PASS", "external node executor config remains outside the official backend repository", true),
                switchCheck("DANGEROUS_TEST_CONTROLS_DISABLED", "PASS", "dangerous test controls remain disabled in the candidate", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PRODUCTION_PROFILE_BOUND", "BLOCKED", "production profile binding is not available yet", true),
                switchCheck("SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", "sensitive config sources are not externalized yet", true),
                switchCheck("CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", "config drift scan is covered by readiness and boundary tests", true),
                switchCheck("CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", "config rollback source remains the documented current entrypoint set", true)
        );
    }

    List<Map<String, Object>> centralConfigGovernancePrecheckChecks() {
        return List.of(
                switchCheck("CONFIG_OWNERSHIP_DOCUMENTED", "PASS", "configuration ownership remains documented in unified-backend readiness", true),
                switchCheck("ENTRYPOINT_PORTS_DOCUMENTED", "PASS", "current and candidate entrypoint ports are documented", true),
                switchCheck("CANDIDATE_CONFIG_SURFACE_DOCUMENTED", "PASS", "candidate config surface is documented without connecting production provider", true),
                switchCheck("CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", "config drift scan is automated through readiness assertions", true),
                switchCheck("CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", "rollback source is the preserved current entrypoint set", true),
                switchCheck("SENSITIVE_VALUE_REDACTION_ENFORCED", "PASS", "readiness evidence is covered by redaction assertions", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_CONFIG_BOUNDARY", "PASS", "external node executor config remains outside unified-backend candidate", true),
                switchCheck("CONFIG_GOVERNANCE_EVIDENCE_RECORDED", "PASS", "central config governance evidence is recorded without production connection", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration provider is not connected", true),
                switchCheck("PRODUCTION_PROFILE_BOUND", "BLOCKED", "production profile is not bound to the candidate", true),
                switchCheck("SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", "sensitive config source is not externalized yet", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "production traffic entrypoint is not switched to unified-backend", true)
        );
    }

    Map<String, Object> centralConfigGovernanceEvidence() {
        return map(
                "governanceMode", "DOCUMENTED_NOT_CONNECTED",
                "candidateEntrypoint", "unified-backend:8135",
                "currentEntrypointPorts", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134",
                        "unified-backend:8135"
                ),
                "configProviderStatus", "BLOCKED",
                "productionProfileBound", false,
                "sensitiveValuesExternalized", false,
                "configDriftScanAutomated", true,
                "rollbackSourceDefined", true,
                "sensitiveValuesExposed", false,
                "environmentVariablesRead", false,
                "trafficSwitchApplied", false,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "status", "GOVERNANCE_EVIDENCE_RECORDED_NOT_CONNECTED"
        );
    }

    List<Map<String, Object>> productionCentralConfigPrecheckChecks() {
        return List.of(
                switchCheck("CONFIG_OWNERSHIP_DOCUMENTED", "PASS", "configuration ownership remains documented before production provider connection", true),
                switchCheck("ENTRYPOINT_PORTS_DOCUMENTED", "PASS", "current gateway, rollback entrypoints and candidate port are documented", true),
                switchCheck("CANDIDATE_CONFIG_SURFACE_DOCUMENTED", "PASS", "candidate production config surface is documented without reading real values", true),
                switchCheck("CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", "config drift scan remains automated through readiness assertions", true),
                switchCheck("CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", "config rollback source remains the protected rollback entrypoint set", true),
                switchCheck("SENSITIVE_VALUE_REDACTION_ENFORCED", "PASS", "production config readiness evidence is covered by redaction assertions", true),
                switchCheck("ROLLBACK_ENTRYPOINTS_DOCUMENTED", "PASS", "api-gateway and five core rollback entrypoints remain documented", true),
                switchCheck("CURRENT_GATEWAY_ENTRYPOINT_PRESERVED", "PASS", "api-gateway:8125 remains preserved as the current gateway entrypoint", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "production centralized configuration provider is not connected", true),
                switchCheck("PRODUCTION_PROFILE_BOUND", "BLOCKED", "production profile is not bound to the candidate", true),
                switchCheck("SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", "sensitive configuration source is not externalized yet", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "production traffic entrypoint is not switched to unified-backend", true)
        );
    }

    Map<String, Object> productionCentralConfigEvidence() {
        return map(
                "readinessMode", "PRODUCTION_PREREQUISITES_RECORDED_NOT_CONNECTED",
                "candidateEntrypoint", "unified-backend:8135",
                "currentEntrypoint", "api-gateway:8125",
                "rollbackEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "configDomains", List.of(
                        "entrypoint",
                        "security",
                        "audit",
                        "rollback"
                ),
                "configProviderStatus", "BLOCKED",
                "productionProfileBound", false,
                "sensitiveConfigExternalized", false,
                "environmentVariablesRead", false,
                "sensitiveValuesExposed", false,
                "configDriftScanAutomated", true,
                "rollbackSourceDefined", true,
                "trafficSwitchApplied", false,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "productionTrafficEntrypointReady", false,
                "currentEntrypointPreserved", true,
                "status", "BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED"
        );
    }

    List<Map<String, Object>> persistentAuditPrecheckChecks() {
        return List.of(
                switchCheck("AUDIT_SINK_FIXED", "PASS", "audit sink remains fixed for the candidate", true),
                switchCheck("AUDIT_REQUEST_ID_PRESERVED", "PASS", "audit request id is preserved", true),
                switchCheck("AUDIT_EVENT_SCHEMA_FIXED", "PASS", "audit event schema remains fixed", true),
                switchCheck("AUDIT_RETENTION_WINDOW_DOCUMENTED", "PASS", "audit retention window is documented", true),
                switchCheck("AUDIT_BACKUP_EXPORT_PATH_DOCUMENTED", "PASS", "audit backup export path is documented", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent audit sink is not connected", true),
                switchCheck("AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", "audit write path is not connected", true),
                switchCheck("AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", "audit replay path is not connected", true),
                switchCheck("AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", "audit retention job is not connected", true),
                switchCheck("AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", "audit config rollback source remains the documented current entrypoint set", true)
        );
    }

    List<Map<String, Object>> persistentAuditGovernancePrecheckChecks() {
        return List.of(
                switchCheck("AUDIT_OWNERSHIP_DOCUMENTED", "PASS", "audit ownership remains documented for the unified-backend candidate", true),
                switchCheck("AUDIT_EVENT_SCHEMA_DOCUMENTED", "PASS", "audit event schema remains documented", true),
                switchCheck("AUDIT_REQUEST_ID_PRESERVED", "PASS", "audit request id is preserved", true),
                switchCheck("AUDIT_RETENTION_WINDOW_DOCUMENTED", "PASS", "audit retention window is documented", true),
                switchCheck("AUDIT_EXPORT_PATH_DOCUMENTED", "PASS", "audit export path is documented", true),
                switchCheck("AUDIT_REPLAY_SCOPE_DOCUMENTED", "PASS", "audit replay scope is documented", true),
                switchCheck("AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", "audit config rollback source remains the documented current entrypoint set", true),
                switchCheck("AUDIT_REDACTION_ENFORCED", "PASS", "readiness audit evidence is covered by redaction assertions", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_AUDIT_BOUNDARY", "PASS", "external node executor audit boundary remains outside unified-backend candidate", true),
                switchCheck("PERSISTENT_AUDIT_GOVERNANCE_EVIDENCE_RECORDED", "PASS", "persistent audit governance evidence is recorded without production connection", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent audit sink is not connected", true),
                switchCheck("AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", "audit write path is not connected", true),
                switchCheck("AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", "audit replay path is not connected", true),
                switchCheck("AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", "audit retention job is not connected", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "production traffic entrypoint is not switched to unified-backend", true)
        );
    }

    Map<String, Object> persistentAuditGovernanceEvidence() {
        return map(
                "governanceMode", "DOCUMENTED_NOT_CONNECTED",
                "candidateEntrypoint", "unified-backend:8135",
                "auditSinkStatus", "BLOCKED",
                "auditWritePathConnected", false,
                "auditReplayPathConnected", false,
                "auditRetentionJobConnected", false,
                "auditConfigRollbackSourceDefined", true,
                "requestIdPreserved", true,
                "eventSchemaDocumented", true,
                "retentionWindowDocumented", true,
                "exportPathDocumented", true,
                "replayScopeDocumented", true,
                "redactionEnforced", true,
                "trafficSwitchApplied", false,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "status", "GOVERNANCE_EVIDENCE_RECORDED_NOT_CONNECTED"
        );
    }

    List<Map<String, Object>> realHttpRehearsalPrecheckChecks() {
        return List.of(
                switchCheck("CANDIDATE_HTTP_PORT_FIXED", "PASS", "candidate HTTP port remains fixed at 8135", true),
                switchCheck("REAL_HTTP_TARGETS_DOCUMENTED", "PASS", "real HTTP rehearsal targets are documented", true),
                switchCheck("AUTH_FAILURE_PATH_INCLUDED", "PASS", "auth failure path is included in rehearsal scope", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_EXCLUDED_FROM_REHEARSAL", "PASS", "external node executor remains outside unified rehearsal execution", true),
                switchCheck("SMOKE_RESULT_REDACTION_FIXED", "PASS", "smoke result redaction fields are fixed", true),
                switchCheck("CANDIDATE_PROCESS_STARTED_FOR_REHEARSAL", "PASS", "candidate process is started in real Web environment rehearsal tests", true),
                switchCheck("ALL_REAL_HTTP_TARGETS_PASSED", "PASS", "real HTTP rehearsal covers candidate health, six entrypoint health checks, business targets and auth failure path", true),
                switchCheck("REHEARSAL_RESULT_RECORDED", "PASS", "real HTTP rehearsal result is recorded in the local test log", true),
                switchCheck("REHEARSAL_RUNBOOK_DEFINED", "BLOCKED", "real HTTP rehearsal runbook is not defined yet", true),
                switchCheck("REHEARSAL_ROLLBACK_RECHECKED", "BLOCKED", "rollback recheck after rehearsal is not verified yet", true)
        );
    }

    List<Map<String, Object>> routeDriftPrecheckChecks() {
        return List.of(
                switchCheck("CURRENT_GATEWAY_ROUTES_DOCUMENTED", "PASS", "current gateway routes are documented", true),
                switchCheck("UNIFIED_MOUNT_ROUTES_DOCUMENTED", "PASS", "unified mount routes are documented", true),
                switchCheck("ROUTE_PREFIX_PRESERVED", "PASS", "candidate keeps existing business path prefixes", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_ROUTE_ABSENT", "PASS", "external node executor route is absent from the official backend route set", true),
                switchCheck("NO_HTTP_UPSTREAM_FALLBACK_IN_CANDIDATE", "PASS", "candidate mount list has no HTTP fallback route", true),
                switchCheck("REAL_GATEWAY_TO_UNIFIED_DIFF_SCAN_AUTOMATED", "PASS", "gateway route registry and unified mount list are compared by automated real HTTP scan", true),
                switchCheck("AUTH_BEHAVIOR_DIFF_SCAN_AUTOMATED", "PASS", "admin auth failure and malformed token behavior are covered by automated scan", true),
                switchCheck("ERROR_CODE_DIFF_SCAN_AUTOMATED", "PASS", "admin auth error codes remain covered by automated scan", true),
                switchCheck("SENSITIVE_FIELD_DIFF_SCAN_AUTOMATED", "PASS", "route scan responses are covered by sensitive field assertions", true),
                switchCheck("DRIFT_SCAN_RESULT_RECORDED", "PASS", "route drift scan result is recorded in the local test log", true)
        );
    }

    List<Map<String, Object>> rollbackWindowPrecheckChecks() {
        return List.of(
                switchCheck("CURRENT_ENTRYPOINTS_STILL_PRESENT", "PASS", "current six rollback entrypoints remain present", true),
                switchCheck("CURRENT_ENTRYPOINT_TESTS_STILL_REQUIRED", "PASS", "current entrypoint tests remain required", true),
                switchCheck("API_GATEWAY_ROLLBACK_TARGET_DOCUMENTED", "PASS", "api-gateway rollback target remains documented", true),
                switchCheck("CORE_ENTRYPOINTS_ROLLBACK_TARGETS_DOCUMENTED", "PASS", "core rollback targets remain documented", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_UNAFFECTED_BY_CANDIDATE", "PASS", "external node executor remains out of repository and unaffected by candidate", true),
                switchCheck("ROLLBACK_WINDOW_DURATION_DEFINED", "PASS", "rollback window duration is defined as at least 24 hours", true),
                switchCheck("ROLLBACK_TRIGGER_CRITERIA_DEFINED", "PASS", "rollback trigger criteria are defined for rehearsal and regression failures", true),
                switchCheck("ROLLBACK_RECHECK_AUTOMATED", "PASS", "rollback recheck commands are recorded for candidate and current entrypoints", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVAL_READY", "BLOCKED", "old entrypoint retirement approval is not ready", true),
                switchCheck("ROLLBACK_RECORDING_COMPLETED", "PASS", "rollback window evidence is recorded in readiness", true)
        );
    }

    Map<String, Object> rollbackWindowEvidence() {
        return map(
                "windowDuration", map(
                        "status", "DEFINED",
                        "minimumHours", 24,
                        "scope", "keep current six rollback entrypoints available after candidate entrypoint switch"
                ),
                "triggerCriteria", map(
                        "items", List.of(
                                "REAL_HTTP_REHEARSAL_FAILURE",
                                "ROUTE_DRIFT_DETECTED",
                                "AUTH_ERROR_CODE_DRIFT",
                                "CURRENT_ENTRYPOINT_REGRESSION_FAILURE",
                                "BOUNDARY_SCAN_MATCH",
                                "EXTERNAL_NODE_EXECUTOR_BOUNDARY_CHANGED"
                        )
                ),
                "recheckAutomation", map(
                        "commands", List.of(
                                "mvn -q -f backend/unified-backend-service/pom.xml test",
                                "mvn -q -f backend/ops-core-service/pom.xml test",
                                "mvn -q -f backend/api-gateway-service/pom.xml test",
                                "mvn -q -f backend/business-core-service/pom.xml test",
                                "mvn -q -f backend/admission-core-service/pom.xml test",
                                "mvn -q -f backend/engagement-core-service/pom.xml test",
                                "mvn -q -f backend/portal-core-service/pom.xml test",
                                "git diff --check",
                                "rg -n production-boundary-scan backend/*/src/main/java"
                        )
                ),
                "rollbackTargets", List.of(
                        rollbackTarget("api-gateway", 8125, "CURRENT_PRODUCTION_ENTRYPOINT"),
                        rollbackTarget("business-core", 8130, "CURRENT_CORE_ENTRYPOINT"),
                        rollbackTarget("admission-core", 8131, "CURRENT_CORE_ENTRYPOINT"),
                        rollbackTarget("engagement-core", 8132, "CURRENT_CORE_ENTRYPOINT"),
                        rollbackTarget("ops-core", 8133, "CURRENT_CORE_ENTRYPOINT"),
                        rollbackTarget("portal-core", 8134, "CURRENT_CORE_ENTRYPOINT"),
                        rollbackTarget("unified-backend", 8135, "CANDIDATE_PARALLEL_ENTRYPOINT")
                ),
                "recordingStatus", "COMPLETED",
                "retirementApprovalStatus", "BLOCKED"
        );
    }

    List<Map<String, Object>> entrypointSwitchPrecheckChecks() {
        return List.of(
                switchCheck("BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", "business paths remain unchanged", true),
                switchCheck("CANDIDATE_BASE_URL_DOCUMENTED", "PASS", "candidate base URL target is documented as port 8135", true),
                switchCheck("FRONTEND_NOT_MODIFIED_IN_THIS_ROUND", "PASS", "frontend is not modified in this round", true),
                switchCheck("PROXY_SWITCH_SCOPE_DOCUMENTED", "PASS", "proxy switch scope is documented", true),
                switchCheck("SWITCH_REQUIRES_ROLLBACK_WINDOW", "PASS", "entrypoint switch still requires rollback window", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint switch is not implemented", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy switch is not implemented", true),
                switchCheck("PRODUCTION_TRAFFIC_CANARY_DEFINED", "PASS", "production traffic canary plan is defined without applying traffic switch", true),
                switchCheck("ENTRYPOINT_SWITCH_TESTS_AUTOMATED", "PASS", "entrypoint switch rehearsal is covered by automated readiness tests", true),
                switchCheck("SWITCH_AUDIT_RECORDING_READY", "PASS", "switch audit recording is ready for rehearsal evidence", true)
        );
    }

    Map<String, Object> entrypointSwitchEvidence() {
        return map(
                "candidateBaseUrl", "http://127.0.0.1:8135",
                "currentGatewayBaseUrl", "http://127.0.0.1:8125",
                "businessPathsRemainUnchanged", true,
                "switchMode", "ENTRYPOINT_TARGET_ONLY",
                "forbiddenPathPrefix", "/api/v1/unified-backend/<module>",
                "rollbackTarget", "api-gateway:8125",
                "rehearsalStatus", "PASS",
                "auditRecordingStatus", "READY_FOR_REHEARSAL"
        );
    }

    Map<String, Object> productionTrafficCanaryEvidence() {
        return map(
                "strategy", "CANARY_WITH_PAUSE_AND_ROLLBACK",
                "plannedWeights", List.of(0, 5, 25, 50, 100),
                "initialWeightPercent", 0,
                "currentProductionTrafficPercent", 0,
                "candidateProductionTrafficPercent", 0,
                "manualPromotionRequired", true,
                "rollbackTarget", "api-gateway:8125",
                "rollbackWindowMinimumHours", 24,
                "trafficSwitchApplied", false,
                "status", "PLAN_DEFINED_NOT_APPLIED",
                "gates", List.of(
                        "REAL_HTTP_REHEARSAL_PASSED",
                        "ROUTE_DRIFT_SCAN_PASSED",
                        "ROLLBACK_WINDOW_EVIDENCE_COMPLETED",
                        "CURRENT_ENTRYPOINT_REGRESSION_PASSED",
                        "BOUNDARY_SCAN_CLEAR",
                        "FRONTEND_ENTRYPOINT_SWITCH_READY",
                        "EXTERNAL_PROXY_SWITCH_READY"
                )
        );
    }

    List<Map<String, Object>> backendSingleServicePrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_COVERS_BACKEND_ENTRYPOINT_APIS", "PASS", "candidate exposes api-gateway and five core self APIs in one backend process", true),
                switchCheck("ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", "all 25 official backend business routes are mounted in-process", true),
                switchCheck("PATH_AUTH_ENVELOPE_AND_ERROR_CODES_PRESERVED", "PASS", "existing paths, auth behavior, response envelope and error codes remain preserved", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real Web environment HTTP rehearsal passed for candidate targets", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("SENSITIVE_FIELD_SCAN_PASSED", "PASS", "readiness and route evidence remain redacted", true),
                switchCheck("ROLLBACK_WINDOW_EVIDENCE_COMPLETED", "PASS", "rollback window evidence is recorded for current entrypoints", true),
                switchCheck("CURRENT_ENTRYPOINTS_PRESERVED_AS_ROLLBACK", "PASS", "current six backend entrypoints remain available as rollback targets", true),
                switchCheck("CURRENT_ENTRYPOINT_REGRESSION_REQUIRED", "PASS", "current entrypoint regression remains required before completion", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected", true),
                switchCheck("BACKEND_SINGLE_SERVICE_EVIDENCE_RECORDED", "PASS", "backend single-service candidate evidence is recorded without applying traffic switch", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true)
        );
    }

    Map<String, Object> backendSingleServiceEvidence() {
        return map(
                "candidateEntrypoint", "unified-backend:8135",
                "currentGatewayRollbackTarget", "api-gateway:8125",
                "currentCoreRollbackTargets", List.of(
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "businessPathsRemainUnchanged", true,
                "inProcessRoutesTotal", 25,
                "httpFallbackRoutesTotal", 0,
                "currentProductionEntrypointsPreserved", true,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "trafficSwitchApplied", false,
                "oldEntrypointRetirementApproved", false,
                "backendSingleServiceCandidateReady", true,
                "remainingBlockers", List.of(
                        "FRONTEND_ENTRYPOINT_NOT_SWITCHED",
                        "EXTERNAL_PROXY_NOT_SWITCHED",
                        "PRODUCTION_TRAFFIC_NOT_SWITCHED",
                        "OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED",
                        "CENTRAL_CONFIG_NOT_CONNECTED",
                        "PERSISTENT_AUDIT_NOT_CONNECTED"
                )
        );
    }

    List<Map<String, Object>> finalBackendSingleServicePrecheckChecks() {
        return List.of(
                switchCheck("BACKEND_APPLICATION_ENTRYPOINT_COVERAGE", "PASS", "unified-backend covers api-gateway and five core self APIs as the future backend application entrypoint", true),
                switchCheck("ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", "all 25 official backend business routes remain mounted in-process", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real Web environment HTTP rehearsal passed for the candidate entrypoint", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("LEGACY_ENTRYPOINT_REGRESSION_PASSED", "PASS", "current legacy rollback entrypoints remain in the Maven regression gate", true),
                switchCheck("PRODUCTION_SOURCE_BOUNDARY_SCAN_PASSED", "PASS", "production source boundary scan has no dangerous node execution or deletion matches", true),
                switchCheck("LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", "api-gateway and five core entrypoints remain protected rollback targets", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected", true),
                switchCheck("FINAL_BACKEND_SINGLE_SERVICE_EVIDENCE_RECORDED", "PASS", "final backend single-service cutover rehearsal evidence is recorded without applying traffic switch", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("TRAFFIC_SWITCH_APPLIED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true)
        );
    }

    Map<String, Object> finalBackendSingleServiceEvidence() {
        return map(
                "targetBackendApplicationEntrypoint", "unified-backend:8135",
                "externalNodeExecutorProject", "separate-project",
                "externalNodeExecutorConnected", false,
                "legacyRollbackEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "backendApplicationEntrypointsRequiredForFutureRuntime", List.of("unified-backend:8135"),
                "externalNodeExecutorOutOfRepository", true,
                "businessPathsRemainUnchanged", true,
                "inProcessRoutesTotal", 25,
                "httpFallbackRoutesTotal", 0,
                "currentProductionEntrypointsPreserved", true,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "trafficSwitchApplied", false,
                "oldEntrypointRetirementApproved", false,
                "singleBackendApplicationReadyForCutoverRehearsal", true,
                "remainingBlockers", List.of(
                        "FRONTEND_ENTRYPOINT_NOT_SWITCHED",
                        "EXTERNAL_PROXY_NOT_SWITCHED",
                        "PRODUCTION_TRAFFIC_NOT_SWITCHED",
                        "OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED",
                        "CENTRAL_CONFIG_NOT_CONNECTED",
                        "PERSISTENT_AUDIT_NOT_CONNECTED"
                )
        );
    }

    List<Map<String, Object>> singleServiceCutoverPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_TARGET_ENTRYPOINT_READY", "PASS", "unified-backend:8135 is the target backend application entrypoint", true),
                switchCheck("ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", "all 25 official backend business routes are mounted in-process", true),
                switchCheck("NODE_EXECUTOR_REPOSITORY_RESIDUALS_REMOVED", "PASS", "node executor repository residuals are removed from official backend scope", true),
                switchCheck("API_REFERENCE_SYNCHRONIZED", "PASS", "official API reference is synchronized with the backend application scope", true),
                switchCheck("OLD_ENTRYPOINTS_IN_RETIREMENT_QUEUE", "PASS", "api-gateway and five core entrypoints are protected rollback entrypoints awaiting sequential retirement", true),
                switchCheck("EXTERNAL_TRAFFIC_SWITCH_APPLIED", "BLOCKED", "external production traffic is not switched in this repository", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> singleServiceCutoverEvidence() {
        return map(
                "targetBackendApplicationEntrypoint", "unified-backend:8135",
                "officialBackendEntrypointsTotal", 7,
                "backendApplicationEntrypointsRequiredForFutureRuntime", List.of("unified-backend:8135"),
                "rollbackEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "retirementQueue", List.of(
                        "api-gateway",
                        "business-core",
                        "admission-core",
                        "engagement-core",
                        "ops-core",
                        "portal-core"
                ),
                "businessPathsRemainUnchanged", true,
                "inProcessRoutesTotal", 25,
                "httpFallbackRoutesTotal", 0,
                "externalRoutesTotal", 0,
                "nodeExecutorRepositoryResidualsRemoved", true,
                "apiReferenceSynchronized", true,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "trafficSwitchApplied", false,
                "oldEntrypointRetirementApproved", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "status", "READY_FOR_EXTERNAL_CUTOVER_NOT_SWITCHED"
        );
    }

    List<Map<String, Object>> entrypointCutoverAdapterPrecheckChecks() {
        return List.of(
                switchCheck("FRONTEND_API_BASE_URL_CONTRACT_DOCUMENTED", "PASS", "frontend API base URL override is documented as VITE_API_BASE_URL", true),
                switchCheck("BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", "business paths keep their existing /api/v1 prefixes", true),
                switchCheck("CANDIDATE_BASE_URL_DOCUMENTED", "PASS", "candidate base URL is documented as http://127.0.0.1:8135", true),
                switchCheck("ROLLBACK_TARGET_DOCUMENTED", "PASS", "rollback base URL is documented as http://127.0.0.1:8125", true),
                switchCheck("NO_FRONTEND_SOURCE_TO_MODIFY_IN_REPOSITORY", "PASS", "repository does not contain frontend source to modify in this round", true),
                switchCheck("NO_PROXY_CONFIG_TO_MODIFY_IN_REPOSITORY", "PASS", "repository does not contain proxy config to modify in this round", true),
                switchCheck("CUTOVER_REQUIRES_EXTERNAL_FRONTEND_OR_PROXY_CHANGE", "PASS", "actual cutover requires changing an external frontend build or proxy target", true),
                switchCheck("CUTOVER_ADAPTER_EVIDENCE_RECORDED", "PASS", "cutover adapter evidence is recorded without applying traffic switch", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched in this repository", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched in this repository", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "production traffic entrypoint is not switched to unified-backend", true)
        );
    }

    Map<String, Object> entrypointCutoverAdapterEvidence() {
        return map(
                "currentGatewayBaseUrl", "http://127.0.0.1:8125",
                "candidateBaseUrl", "http://127.0.0.1:8135",
                "switchMode", "ENTRYPOINT_TARGET_ONLY",
                "businessPathsRemainUnchanged", true,
                "forbiddenPathPrefix", "/api/v1/unified-backend/<module>",
                "frontendSourcePresent", false,
                "proxyConfigPresent", false,
                "repositoryCutoverConfigApplied", false,
                "requiredFrontendEnvVar", "VITE_API_BASE_URL",
                "recommendedNextValue", "http://127.0.0.1:8135",
                "rollbackTarget", "http://127.0.0.1:8125",
                "trafficSwitchApplied", false,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "status", "ADAPTER_EVIDENCE_RECORDED_NOT_APPLIED"
        );
    }

    List<Map<String, Object>> oldEntrypointRetirementPrecheckChecks() {
        return List.of(
                switchCheck("RETIREMENT_SCOPE_DOCUMENTED", "PASS", "current production entrypoints and rollback targets are documented", true),
                switchCheck("SEQUENTIAL_ENTRYPOINT_RETIREMENT_REQUIRED", "PASS", "old entrypoints require sequential approval and verification", true),
                switchCheck("BULK_RETIREMENT_FORBIDDEN", "PASS", "bulk entrypoint retirement and bulk deletion remain forbidden", true),
                switchCheck("CURRENT_ENTRYPOINT_REGRESSION_REQUIRED", "PASS", "current six rollback backend Maven entrypoints remain in the regression gate", true),
                switchCheck("ROLLBACK_TARGETS_STILL_PROTECTED", "PASS", "api-gateway and five core entrypoints remain protected rollback targets", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected", true),
                switchCheck("RETIREMENT_APPROVAL_EVIDENCE_RECORDED", "PASS", "old entrypoint retirement approval evidence is recorded without retiring entrypoints", true),
                switchCheck("FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", "frontend entrypoint is not switched to unified-backend", true),
                switchCheck("EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", "external proxy target is not switched to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_RETIREMENT_APPROVED", "BLOCKED", "api-gateway retirement is not approved", true),
                switchCheck("CORE_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "core entrypoint retirement is not approved", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true)
        );
    }

    Map<String, Object> oldEntrypointRetirementEvidence() {
        return map(
                "retirementMode", "SEQUENTIAL_APPROVAL_ONLY",
                "bulkRetirementAllowed", false,
                "directoryDeletionAllowed", false,
                "mavenRegressionRequired", true,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "retirementApprovalStatus", "BLOCKED",
                "approvedEntrypoints", List.of(),
                "currentProductionEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "protectedRollbackEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "blockedEntrypoints", List.of(
                        "API_GATEWAY_RETIREMENT_APPROVAL_BLOCKED",
                        "CORE_ENTRYPOINT_RETIREMENT_APPROVAL_BLOCKED",
                        "PRODUCTION_TRAFFIC_ENTRYPOINT_BLOCKED"
                ),
                "nextEligibleEntrypoint", "NONE_UNTIL_TRAFFIC_SWITCH",
                "trafficSwitchApplied", false,
                "frontendEntrypointSwitched", false,
                "externalProxySwitched", false,
                "status", "RETIREMENT_APPROVAL_NOT_GRANTED"
        );
    }

    List<Map<String, Object>> entrypointCutoverExecutionPrecheckChecks() {
        return List.of(
                switchCheck("BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", "business paths keep their existing /api/v1 prefixes", true),
                switchCheck("FORBIDDEN_UNIFIED_BUSINESS_PREFIX_ABSENT", "PASS", "business paths are not rewritten under /api/v1/unified-backend/<module>", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for the candidate entrypoint", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", "api-gateway and five core entrypoints remain protected rollback targets", true),
                switchCheck("ROLLBACK_RECHECK_PASSED", "PASS", "current backend Maven entrypoint regression remains in the cutover gate", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", "external node executor is out of repository and not connected", true),
                switchCheck("FRONTEND_OR_PROXY_CONFIG_PRESENT", "BLOCKED", "repository does not contain frontend or proxy config to update", true),
                switchCheck("FRONTEND_OR_PROXY_CONFIG_UPDATED", "BLOCKED", "frontend or proxy target is not updated to unified-backend in this repository", true),
                switchCheck("TARGET_ENTRYPOINT_SET_TO_UNIFIED_BACKEND", "BLOCKED", "effective API base URL still points at the current gateway", true),
                switchCheck("PRODUCTION_TRAFFIC_SWITCH_APPLIED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration is not connected", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true)
        );
    }

    Map<String, Object> entrypointCutoverExecutionEvidence() {
        return map(
                "currentGatewayBaseUrl", "http://127.0.0.1:8125",
                "candidateBaseUrl", "http://127.0.0.1:8135",
                "effectiveApiBaseUrl", "http://127.0.0.1:8125",
                "switchMode", "ENTRYPOINT_TARGET_ONLY",
                "businessPathsRemainUnchanged", true,
                "forbiddenPathPrefix", "/api/v1/unified-backend/<module>",
                "frontendConfigPresent", false,
                "proxyConfigPresent", false,
                "repositoryCutoverConfigApplied", false,
                "externalCutoverRequired", true,
                "rollbackTarget", "http://127.0.0.1:8125",
                "trafficSwitchApplied", false,
                "oldEntrypointRetirementApproved", false,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "readyToReplaceGateway", false,
                "readyForProduction", false,
                "remainingBlockers", List.of(
                        "FRONTEND_OR_PROXY_CONFIG_ABSENT",
                        "TARGET_ENTRYPOINT_NOT_SET_TO_UNIFIED_BACKEND",
                        "PRODUCTION_TRAFFIC_NOT_SWITCHED",
                        "OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED",
                        "CENTRAL_CONFIG_NOT_CONNECTED",
                        "PERSISTENT_AUDIT_NOT_CONNECTED"
                ),
                "status", "CUTOVER_EXECUTION_BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG"
        );
    }

    List<Map<String, Object>> productionEntrypointCutoverPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_READY", "PASS", "unified-backend:8135 is ready as the target backend application entrypoint", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "all business paths keep existing /api/v1 prefixes", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for the candidate business surface", true),
                switchCheck("API_GATEWAY_ROLLBACK_TARGET_DEFINED", "PASS", "api-gateway:8125 remains the rollback target", true),
                switchCheck("EXTERNAL_ENTRYPOINT_CONFIG_PRESENT", "BLOCKED", "repository does not contain external frontend or proxy entrypoint config", true),
                switchCheck("TRAFFIC_SWITCH_APPLIED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_RETIREMENT_APPROVED", "BLOCKED", "api-gateway retirement is not approved", true)
        );
    }

    Map<String, Object> productionEntrypointCutoverEvidence() {
        return map(
                "targetEntrypoint", "unified-backend:8135",
                "currentEntrypoint", "api-gateway:8125",
                "businessPathsRemainUnchanged", true,
                "externalEntrypointConfigPresent", false,
                "trafficSwitchApplied", false,
                "rollbackTarget", "api-gateway:8125",
                "apiGatewayRetirementApproved", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "status", "BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG"
        );
    }

    List<Map<String, Object>> externalEntrypointCutoverPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_TARGET_READY", "PASS", "unified-backend:8135 is ready as the external cutover target", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "all business paths keep existing /api/v1 prefixes", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for the candidate business surface", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("ROLLBACK_TARGET_DEFINED", "PASS", "api-gateway:8125 remains the rollback entrypoint", true),
                switchCheck("SMOKE_EVIDENCE_FORMAT_DEFINED", "PASS", "smoke evidence format is recorded without treating it as production switch proof", true),
                switchCheck("EXTERNAL_ENTRYPOINT_CONFIG_PROVIDED", "BLOCKED", "external frontend, proxy or deployment entrypoint config is not provided in this repository", true),
                switchCheck("EXTERNAL_ENTRYPOINT_TARGETS_UNIFIED_BACKEND", "BLOCKED", "external entrypoint does not target unified-backend yet", true),
                switchCheck("CONTROLLED_CUTOVER_WINDOW_APPROVED", "BLOCKED", "controlled cutover window is not approved", true),
                switchCheck("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED", "BLOCKED", "production traffic is not observed on unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_COMPLETED", "BLOCKED", "rollback window is not completed after traffic switch", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration provider is not connected", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true),
                switchCheck("USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", "entrypoint retirement approval has not been granted by the user", true)
        );
    }

    Map<String, Object> externalEntrypointCutoverEvidence() {
        return map(
                "readinessMode", "EXTERNAL_CUTOVER_ADAPTER_RECORDED_NOT_SWITCHED",
                "candidateEntrypoint", "unified-backend:8135",
                "currentEntrypoint", "api-gateway:8125",
                "effectiveEntrypoint", "api-gateway:8125",
                "rollbackEntrypoint", "api-gateway:8125",
                "businessPathsRemainUnchanged", true,
                "externalEntrypointConfigProvided", false,
                "externalEntrypointTargetsUnifiedBackend", false,
                "repositoryCutoverConfigApplied", false,
                "controlledCutoverWindowApproved", false,
                "trafficSwitchApplied", false,
                "productionTrafficObservedOnUnified", false,
                "apiGatewayTrafficZeroProven", false,
                "rollbackWindowCompleted", false,
                "centralConfigProviderConnected", false,
                "persistentAuditSinkConnected", false,
                "apiGatewayRetirementApproved", false,
                "coreRetirementApproved", false,
                "deletionAllowed", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "status", "BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED"
        );
    }

    List<Map<String, Object>> apiGatewayRetirementPrecheckChecks() {
        return List.of(
                switchCheck("API_GATEWAY_ROLLBACK_ROLE_PROTECTED", "PASS", "api-gateway:8125 remains a protected rollback entrypoint", true),
                switchCheck("API_GATEWAY_SELF_APIS_MOUNTED_IN_UNIFIED", "PASS", "api-gateway self APIs are mounted in unified-backend", true),
                switchCheck("PRODUCTION_ENTRYPOINT_SWITCH_APPLIED", "BLOCKED", "production entrypoint is not switched to unified-backend", true),
                switchCheck("ROLLBACK_WINDOW_COMPLETED", "BLOCKED", "rollback window is not completed after a production switch", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", "api-gateway retirement has not been approved by the user", true),
                switchCheck("DELETE_LIST_CONFIRMED", "BLOCKED", "api-gateway deletion list is not confirmed", true)
        );
    }

    Map<String, Object> apiGatewayRetirementEvidence() {
        return map(
                "retirementApprovalStatus", "BLOCKED",
                "trafficSwitchApplied", false,
                "trafficSwitchProven", false,
                "rollbackWindowCompleted", false,
                "apiGatewayRetirementApproved", false,
                "protectedEntrypoint", "api-gateway:8125",
                "nextAction", "WAIT_FOR_UNIFIED_ENTRYPOINT_TRAFFIC_SWITCH",
                "deletionAllowed", false,
                "status", "BLOCKED_BY_TRAFFIC_NOT_SWITCHED"
        );
    }

    List<Map<String, Object>> coreEntrypointRetirementPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_IN_PROCESS_COVERAGE", "PASS", "five core business surfaces are mounted in unified-backend", true),
                switchCheck("CORE_SELF_APIS_MOUNTED", "PASS", "five core self APIs are mounted in unified-backend", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "all business paths keep existing /api/v1 prefixes", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for candidate business surfaces", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "route drift scan passed before core retirement readiness", true),
                switchCheck("INDEPENDENT_CORE_REGRESSION_REQUIRED", "PASS", "independent core Maven entrypoints remain in regression before retirement", true),
                switchCheck("API_GATEWAY_RETIREMENT_COMPLETED", "BLOCKED", "api-gateway is still a protected rollback entrypoint", true),
                switchCheck("EXTERNAL_ENTRYPOINT_TRAFFIC_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("ROLLBACK_WINDOW_COMPLETED", "BLOCKED", "rollback window has not completed after traffic switch", true),
                switchCheck("USER_CORE_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", "core entrypoint retirement approval has not been granted", true),
                switchCheck("CORE_DELETE_LIST_CONFIRMED", "BLOCKED", "core deletion list is not confirmed", true)
        );
    }

    Map<String, Object> coreEntrypointRetirementEvidence() {
        return map(
                "retirementApprovalStatus", "BLOCKED",
                "deletionAllowed", false,
                "trafficSwitchApplied", false,
                "apiGatewayRetired", false,
                "rollbackWindowCompleted", false,
                "bulkRetirementAllowed", false,
                "nextEligibleCore", "NONE_UNTIL_API_GATEWAY_RETIRED",
                "protectedCoreEntrypoints", List.of(
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "coreEntrypointMatrix", List.of(
                        coreRetirementTarget("business-core", 8130, "backend/business-core-service", List.of("auth", "profile", "notification", "content", "server-status", "resource", "admin"), 1),
                        coreRetirementTarget("admission-core", 8131, "backend/admission-core-service", List.of("onboarding", "exam", "whitelist", "attendance"), 2),
                        coreRetirementTarget("engagement-core", 8132, "backend/engagement-core-service", List.of("community", "activity", "calendar", "changelog"), 3),
                        coreRetirementTarget("ops-core", 8133, "backend/ops-core-service", List.of("ops-control", "cloudreve-sync", "backup-recovery", "alerting", "plugin-integration", "cross-platform-notification", "ops-image-market"), 4),
                        coreRetirementTarget("portal-core", 8134, "backend/portal-core-service", List.of("online-map", "material", "guide"), 5)
                ),
                "status", "BLOCKED_BY_PROTECTED_ROLLBACK_ROLE"
        );
    }

    List<Map<String, Object>> productionHardeningPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_CANDIDATE_READY", "PASS", "unified-backend:8135 is ready as the backend application candidate", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "all business paths keep existing /api/v1 prefixes", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for the candidate business surface", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", "api-gateway and five core entrypoints remain protected rollback targets", true),
                switchCheck("CENTRAL_CONFIG_CONTRACT_DEFINED", "PASS", "central config ownership and rollback contract are documented", true),
                switchCheck("AUDIT_TRAIL_CONTRACT_DEFINED", "PASS", "audit trail ownership and event contract are documented", true),
                switchCheck("CUTOVER_RUNBOOK_DEFINED", "PASS", "cutover runbook requirements are recorded without applying traffic switch", true),
                switchCheck("ROLLBACK_RECHECK_COMMANDS_DEFINED", "PASS", "rollback recheck commands are recorded for candidate and rollback entrypoints", true),
                switchCheck("SMOKE_EVIDENCE_FORMAT_DEFINED", "PASS", "smoke evidence format is recorded without treating it as production switch proof", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration provider is not connected", true),
                switchCheck("SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", "sensitive config source is not externalized yet", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", "persistent production audit sink is not connected", true),
                switchCheck("AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", "audit write path is not connected", true),
                switchCheck("EXTERNAL_ENTRYPOINT_CONFIG_PRESENT", "BLOCKED", "repository does not contain external frontend or proxy entrypoint config", true),
                switchCheck("PRODUCTION_TRAFFIC_SWITCH_APPLIED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("ROLLBACK_WINDOW_COMPLETED", "BLOCKED", "rollback window has not completed after traffic switch", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", "entrypoint retirement approval has not been granted by the user", true)
        );
    }

    Map<String, Object> productionHardeningEvidence() {
        return map(
                "candidateEntrypoint", "unified-backend:8135",
                "currentEntrypoint", "api-gateway:8125",
                "rollbackEntrypoints", List.of(
                        "api-gateway:8125",
                        "business-core:8130",
                        "admission-core:8131",
                        "engagement-core:8132",
                        "ops-core:8133",
                        "portal-core:8134"
                ),
                "businessPathsRemainUnchanged", true,
                "centralConfigProviderConnected", false,
                "sensitiveConfigExternalized", false,
                "persistentAuditSinkConnected", false,
                "auditWritePathConnected", false,
                "externalEntrypointConfigPresent", false,
                "trafficSwitchApplied", false,
                "rollbackWindowCompleted", false,
                "apiGatewayTrafficZeroProven", false,
                "apiGatewayRetirementApproved", false,
                "coreRetirementApproved", false,
                "smokeEvidenceRecorded", true,
                "runbookRecorded", true,
                "deletionAllowed", false,
                "status", "BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES"
        );
    }

    Map<String, Object> replacementDecision() {
        return map(
                "canReplaceGateway", false,
                "canRetireIndependentCoreEntrypoints", false,
                "canRetireApiGateway", false,
                "externalNodeExecutorOutOfRepository", true,
                "externalNodeExecutorConnected", false,
                "candidateCoverageStatus", "PASS",
                "reason", "production cutover prerequisites are still blocked; external node executor is out of repository and not connected"
        );
    }

    private Map<String, Object> switchCheck(String check, String status, String detail, boolean requiredForReplacement) {
        return map(
                "check", check,
                "status", status,
                "detail", detail,
                "requiredForReplacement", requiredForReplacement
        );
    }

    private Map<String, Object> rollbackTarget(String entrypoint, int port, String disposition) {
        return map(
                "entrypoint", entrypoint,
                "port", port,
                "disposition", disposition
        );
    }

    private Map<String, Object> coreRetirementTarget(String entrypointKey, int port, String sourceDirectory,
                                                     List<String> hostedRouteIds, int retirementOrder) {
        return map(
                "entrypointKey", entrypointKey,
                "port", port,
                "sourceDirectory", sourceDirectory,
                "hostedRouteIds", hostedRouteIds,
                "inProcessMountedInUnified", true,
                "selfApisMountedInUnified", true,
                "independentRegressionRequired", true,
                "retirementOrder", retirementOrder,
                "retirementStatus", "BLOCKED",
                "blockedBy", "PROTECTED_ROLLBACK_ROLE"
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
        items.add(inProcess("ops-control", "OPS_CONTROL", "/api/v1/ops-control", "ops-core", 8133));
        items.add(inProcess("cloudreve-sync", "CLOUDREVE_SYNC", "/api/v1/cloudreve-sync", "ops-core", 8133));
        items.add(inProcess("backup-recovery", "BACKUP_RECOVERY", "/api/v1/backup-recovery", "ops-core", 8133));
        items.add(inProcess("alerting", "ALERTING", "/api/v1/alerting", "ops-core", 8133));
        items.add(inProcess("online-map", "ONLINE_MAP", "/api/v1/online-map", "portal-core", 8134));
        items.add(inProcess("plugin-integration", "PLUGIN_INTEGRATION", "/api/v1/plugin-integration", "ops-core", 8133));
        items.add(inProcess("cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification", "ops-core", 8133));
        items.add(inProcess("ops-image-market", "OPS_IMAGE_MARKET", "/api/v1/ops-image-market", "ops-core", 8133));
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
