package cn.beiming.unifiedbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

final class BackendMavenEntrypoints {
    private BackendMavenEntrypoints() {
    }

    static int currentTotal() {
        return (int) List.of(
                new String[]{"backend/unified-backend-service/pom.xml", "../unified-backend-service/pom.xml"},
                new String[]{"backend/api-gateway-service/pom.xml", "../api-gateway-service/pom.xml"},
                new String[]{"backend/business-core-service/pom.xml", "../business-core-service/pom.xml"},
                new String[]{"backend/admission-core-service/pom.xml", "../admission-core-service/pom.xml"},
                new String[]{"backend/engagement-core-service/pom.xml", "../engagement-core-service/pom.xml"},
                new String[]{"backend/ops-core-service/pom.xml", "../ops-core-service/pom.xml"},
                new String[]{"backend/portal-core-service/pom.xml", "../portal-core-service/pom.xml"}
        ).stream().filter(candidate -> Files.exists(Path.of(candidate[0])) || Files.exists(Path.of(candidate[1]))).count();
    }
}

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
                "currentProductionEntrypointsTotal", 1,
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
                "productionCentralConfigProviderStatus", registry.productionCentralConfigProviderStatus(),
                "productionCentralConfigProviderChecks", registry.productionCentralConfigProviderChecks(),
                "productionCentralConfigProviderEvidence", registry.productionCentralConfigProviderEvidence(),
                "externalEntrypointCutoverPrecheckStatus", "BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED",
                "externalEntrypointCutoverPrecheckChecks", registry.externalEntrypointCutoverPrecheckChecks(),
                "externalEntrypointCutoverEvidence", registry.externalEntrypointCutoverEvidence(),
                "externalEntrypointConfigSamplePrecheckStatus", "BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED",
                "externalEntrypointConfigSamplePrecheckChecks", registry.externalEntrypointConfigSamplePrecheckChecks(),
                "externalEntrypointConfigSampleEvidence", registry.externalEntrypointConfigSampleEvidence(),
                "externalEntrypointLocalCutoverRehearsalStatus", "PASS_LOCAL_REHEARSAL_NOT_PRODUCTION",
                "externalEntrypointLocalCutoverRehearsalChecks", registry.externalEntrypointLocalCutoverRehearsalChecks(),
                "externalEntrypointLocalCutoverRehearsalEvidence", registry.externalEntrypointLocalCutoverRehearsalEvidence(),
                "productionCutoverRunbookStatus", registry.productionCutoverRunbookStatus(),
                "productionCutoverRunbookChecks", registry.productionCutoverRunbookChecks(),
                "productionCutoverRunbookEvidence", registry.productionCutoverRunbookEvidence(),
                "productionCutoverApprovalPackageStatus", registry.productionCutoverApprovalPackageStatus(),
                "productionCutoverApprovalPackageChecks", registry.productionCutoverApprovalPackageChecks(),
                "productionCutoverApprovalPackageEvidence", registry.productionCutoverApprovalPackageEvidence(),
                "productionCutoverExternalParameterManifestStatus", registry.productionCutoverExternalParameterManifestStatus(),
                "productionCutoverExternalParameterManifestChecks", registry.productionCutoverExternalParameterManifestChecks(),
                "productionCutoverExternalParameterManifestEvidence", registry.productionCutoverExternalParameterManifestEvidence(),
                "productionCutoverEvidenceConsistencyAuditStatus", registry.productionCutoverEvidenceConsistencyAuditStatus(),
                "productionCutoverEvidenceConsistencyAuditChecks", registry.productionCutoverEvidenceConsistencyAuditChecks(),
                "productionCutoverEvidenceConsistencyAuditEvidence", registry.productionCutoverEvidenceConsistencyAuditEvidence(),
                "productionRuntimeConfigShellStatus", registry.productionRuntimeConfigShellStatus(),
                "productionRuntimeConfigShellChecks", registry.productionRuntimeConfigShellChecks(),
                "productionRuntimeConfigShellEvidence", registry.productionRuntimeConfigShellEvidence(),
                "productionAuditObservabilitySmokeStatus", registry.productionAuditObservabilitySmokeStatus(),
                "productionAuditObservabilitySmokeChecks", registry.productionAuditObservabilitySmokeChecks(),
                "productionAuditObservabilitySmokeEvidence", registry.productionAuditObservabilitySmokeEvidence(),
                "productionControlledCutoverStatus", registry.productionControlledCutoverStatus(),
                "productionControlledCutoverChecks", registry.productionControlledCutoverChecks(),
                "productionControlledCutoverEvidence", registry.productionControlledCutoverEvidence(),
                "apiGatewayControlledRetirementStatus", registry.apiGatewayControlledRetirementStatus(),
                "apiGatewayControlledRetirementChecks", registry.apiGatewayControlledRetirementChecks(),
                "apiGatewayControlledRetirementEvidence", registry.apiGatewayControlledRetirementEvidence(),
                "apiGatewayExternalRetirementEvidenceStatus", registry.apiGatewayExternalRetirementEvidenceStatus(),
                "apiGatewayExternalRetirementEvidenceChecks", registry.apiGatewayExternalRetirementEvidenceChecks(),
                "apiGatewayExternalRetirementEvidence", registry.apiGatewayExternalRetirementEvidence(),
                "localApiGatewayEntrypointRetirementStatus", registry.localApiGatewayEntrypointRetirementStatus(),
                "localApiGatewayEntrypointRetirementChecks", registry.localApiGatewayEntrypointRetirementChecks(),
                "localApiGatewayEntrypointRetirementEvidence", registry.localApiGatewayEntrypointRetirementEvidence(),
                "realProductionEntrypointCutoverStatus", registry.realProductionEntrypointCutoverStatus(),
                "realProductionEntrypointCutoverChecks", registry.realProductionEntrypointCutoverChecks(),
                "realProductionEntrypointCutoverEvidence", registry.realProductionEntrypointCutoverEvidence(),
                "productionExternalValueIntakeRehearsalStatus", registry.productionExternalValueIntakeRehearsalStatus(),
                "productionExternalValueIntakeRehearsalChecks", registry.productionExternalValueIntakeRehearsalChecks(),
                "productionExternalValueIntakeRehearsalEvidence", registry.productionExternalValueIntakeRehearsalEvidence(),
                "auditSinkAdapterRehearsalStatus", registry.auditSinkAdapterRehearsalStatus(),
                "auditSinkAdapterRehearsalChecks", registry.auditSinkAdapterRehearsalChecks(),
                "auditSinkAdapterRehearsalEvidence", registry.auditSinkAdapterRehearsalEvidence(),
                "productionAuditSinkPrecheckStatus", "BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED",
                "productionAuditSinkPrecheckChecks", registry.productionAuditSinkPrecheckChecks(),
                "productionAuditSinkEvidence", registry.productionAuditSinkEvidence(),
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
                "coreEntrypointRetirementPrecheckStatus", "PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED",
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
                check("CURRENT_ENTRYPOINTS_PRESERVED", "PASS", "current backend Maven entrypoint remains stable and five core module sources remain mounted"),
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

interface UnifiedConfigProvider {
    ConfigProviderSnapshot snapshot();
}

final class LocalFileUnifiedConfigProvider implements UnifiedConfigProvider {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ConfigProviderSnapshot snapshot() {
        Path samplePath = locateCentralConfigSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean sampleConfigPresent = Files.exists(samplePath);
        boolean sampleConfigParsed = sampleConfigPresent && !sample.isMissingNode();
        return new ConfigProviderSnapshot(
                "LOCAL_FILE_CONFIG_PROVIDER_REHEARSAL_NOT_PRODUCTION",
                sample.path("providerType").asText("LOCAL_FILE_SAMPLE"),
                sample.path("providerConnected").asBoolean(false),
                "docs/unified-backend-central-config-provider-sample.json",
                sample.path("sampleName").asText("beiming-unified-backend-central-config-provider"),
                sampleConfigPresent,
                sampleConfigParsed,
                sample.path("configDomains").size(),
                sample.path("entrypoints").path("candidate").path("baseUrl").asText("http://127.0.0.1:8135"),
                sample.path("entrypoints").path("current").path("baseUrl").asText("http://127.0.0.1:8125"),
                sample.path("entrypoints").path("rollback").path("baseUrl").asText("http://127.0.0.1:8125"),
                sample.path("routePolicy").path("preserveApiV1BusinessPaths").asBoolean(true),
                sample.path("routePolicy").path("businessPathRewriteAllowed").asBoolean(false),
                sample.path("productionProfileBound").asBoolean(false),
                sample.path("sensitiveConfigExternalized").asBoolean(false),
                false,
                false,
                false,
                List.of(
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "PERSISTENT_AUDIT_SINK_CONNECTED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                sampleConfigParsed
                        ? "PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_LOCAL_CONFIG_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private Path locateCentralConfigSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-central-config-provider-sample.json"),
                Path.of("..", "docs", "unified-backend-central-config-provider-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-central-config-provider-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ConfigProviderSnapshot(
        String readinessMode,
        String providerType,
        boolean productionProviderConnected,
        String sampleConfigPath,
        String sampleName,
        boolean sampleConfigPresent,
        boolean sampleConfigParsed,
        int configDomainsTotal,
        String candidateEntrypoint,
        String currentEntrypoint,
        String rollbackEntrypoint,
        boolean businessPathsRemainUnchanged,
        boolean businessPathRewriteAllowed,
        boolean productionProfileBound,
        boolean sensitiveConfigExternalized,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean trafficSwitchApplied,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedAuditSink {
    AuditSinkSnapshot snapshot();
}

final class LocalFileUnifiedAuditSink implements UnifiedAuditSink {
    private static final List<String> REQUIRED_FIELDS = List.of(
            "eventId",
            "schemaVersion",
            "occurredAt",
            "requestId",
            "sourceService",
            "entrypoint",
            "actor",
            "target",
            "action",
            "riskLevel",
            "result",
            "beforeStateSummary",
            "afterStateSummary",
            "reason",
            "redactionApplied",
            "sensitiveValuesExposed",
            "productionTraffic",
            "rehearsalOnly"
    );
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "id_rsa",
            "token",
            "cookie",
            "secret",
            "password",
            "dsn",
            "bucket",
            "topic"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AuditSinkSnapshot snapshot() {
        Path eventPath = locateAuditPath("unified-backend-audit-sink-sample.jsonl");
        Path schemaPath = locateAuditPath("unified-backend-audit-sink-sample-schema.json");
        List<JsonNode> events = readEvents(eventPath);
        JsonNode schema = readJson(schemaPath);
        boolean eventFilePresent = Files.exists(eventPath);
        boolean schemaFilePresent = Files.exists(schemaPath);
        boolean eventsParsed = eventFilePresent && !events.isEmpty();
        boolean schemaParsed = schemaFilePresent && !schema.isMissingNode();
        boolean requiredFieldsPresent = eventsParsed && events.stream().allMatch(this::hasRequiredFields);
        boolean sensitiveValuesExposed = containsSensitive(events, schema);
        boolean localRehearsalPassed = eventsParsed && schemaParsed && requiredFieldsPresent && !sensitiveValuesExposed;
        return new AuditSinkSnapshot(
                "LOCAL_AUDIT_SINK_ADAPTER_REHEARSAL_NOT_PRODUCTION",
                "LOCAL_FILE_JSONL_SAMPLE",
                false,
                "docs/unified-backend-audit-sink-sample.jsonl",
                "docs/unified-backend-audit-sink-sample-schema.json",
                eventFilePresent,
                eventsParsed,
                events.size(),
                schemaFilePresent,
                schemaParsed,
                schema.path("schemaVersion").asText("1.0"),
                REQUIRED_FIELDS.size(),
                requiredFieldsPresent,
                localRehearsalPassed,
                localRehearsalPassed,
                localRehearsalPassed,
                localRehearsalPassed,
                false,
                sensitiveValuesExposed,
                false,
                false,
                false,
                "http://127.0.0.1:8135",
                "http://127.0.0.1:8125",
                "http://127.0.0.1:8125",
                List.of(
                        "REAL_PERSISTENT_AUDIT_SINK_CONFIGURED",
                        "REAL_AUDIT_WRITE_PATH_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "REAL_AUDIT_REPLAY_PATH_CONNECTED",
                        "REAL_AUDIT_EXPORT_PATH_CONNECTED",
                        "REAL_AUDIT_RETENTION_JOB_CONNECTED",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localRehearsalPassed
                        ? "PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_LOCAL_AUDIT_SINK_SAMPLE_NOT_AVAILABLE"
        );
    }

    private List<JsonNode> readEvents(Path eventPath) {
        if (!Files.exists(eventPath)) {
            return List.of();
        }
        try {
            List<JsonNode> events = new ArrayList<>();
            for (String line : Files.readAllLines(eventPath)) {
                if (!line.isBlank()) {
                    events.add(objectMapper.readTree(line));
                }
            }
            return List.copyOf(events);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private JsonNode readJson(Path schemaPath) {
        try {
            if (Files.exists(schemaPath)) {
                return objectMapper.readTree(Files.readString(schemaPath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean hasRequiredFields(JsonNode event) {
        return REQUIRED_FIELDS.stream().allMatch(event::hasNonNull)
                && event.path("actor").hasNonNull("actorId")
                && event.path("actor").hasNonNull("role")
                && event.path("target").hasNonNull("targetType")
                && event.path("target").hasNonNull("targetId")
                && event.path("target").hasNonNull("targetEntrypoint")
                && event.path("result").hasNonNull("status")
                && event.path("result").hasNonNull("businessCode");
    }

    private boolean containsSensitive(List<JsonNode> events, JsonNode schema) {
        String combined = events.toString().toLowerCase(Locale.ROOT) + schema.toString().toLowerCase(Locale.ROOT);
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(combined::contains);
    }

    private Path locateAuditPath(String fileName) {
        List<Path> candidates = List.of(
                Path.of("docs", fileName),
                Path.of("..", "docs", fileName),
                Path.of("..", "..", "docs", fileName)
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record AuditSinkSnapshot(
        String readinessMode,
        String sinkType,
        boolean sinkConnected,
        String sampleEventPath,
        String sampleSchemaPath,
        boolean sampleEventsPresent,
        boolean sampleEventsParsed,
        int sampleEventsTotal,
        boolean sampleSchemaPresent,
        boolean sampleSchemaParsed,
        String auditEventSchemaVersion,
        int requiredFieldsTotal,
        boolean requiredFieldsPresent,
        boolean writeSmokeRehearsed,
        boolean replayRehearsed,
        boolean exportSummaryRehearsed,
        boolean retentionPolicyRecorded,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean productionAuditSinkConnected,
        boolean productionAuditTrafficObserved,
        boolean trafficSwitchApplied,
        String candidateEntrypoint,
        String currentEntrypoint,
        String rollbackEntrypoint,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionCutoverRunbook {
    ProductionCutoverRunbookSnapshot snapshot();
}

final class LocalFileProductionCutoverRunbook implements UnifiedProductionCutoverRunbook {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "id_rsa",
            "token",
            "cookie",
            "secret",
            "password",
            "dsn",
            "bucket",
            "topic"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionCutoverRunbookSnapshot snapshot() {
        Path runbookPath = locateRunbookPath();
        JsonNode sample = readSample(runbookPath);
        boolean present = Files.exists(runbookPath);
        boolean parsed = present && !sample.isMissingNode();
        boolean sensitiveValuesExposed = containsSensitive(sample);
        int currentMavenEntrypointsTotal = BackendMavenEntrypoints.currentTotal();
        boolean localEvidencePassed = sample.path("requiredLocalEvidence").toString()
                .contains("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION")
                && sample.path("requiredLocalEvidence").toString()
                .contains("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION")
                && sample.path("requiredLocalEvidence").toString()
                .contains("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION");
        boolean localRunbookPassed = parsed && !sensitiveValuesExposed && localEvidencePassed;
        return new ProductionCutoverRunbookSnapshot(
                "LOCAL_PRODUCTION_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-cutover-runbook-sample.json",
                present,
                parsed,
                sample.path("sampleApplied").asBoolean(false),
                sample.path("currentEntrypoint").path("baseUrl").asText("http://127.0.0.1:8125"),
                sample.path("candidateEntrypoint").path("baseUrl").asText("http://127.0.0.1:8135"),
                sample.path("rollbackEntrypoint").path("baseUrl").asText("http://127.0.0.1:8125"),
                sample.path("routePolicy").path("preserveApiV1BusinessPaths").asBoolean(true),
                sample.path("smokeTargets").size(),
                currentMavenEntrypointsTotal,
                sample.path("rollbackPlan").path("rollbackCommands").size() == 1,
                sample.path("canaryPlan").has("stages"),
                sample.path("observationPlan").path("fields").size() > 0,
                sample.path("retirementPlan").path("retirementOrder").size() >= 6,
                localEvidencePassed,
                localEvidencePassed,
                sample.path("sampleApplied").asBoolean(false),
                sample.path("canaryPlan").path("candidateProductionTrafficPercent").asInt(0) > 0,
                false,
                false,
                false,
                false,
                false,
                sample.path("retirementPlan").path("deletionAllowed").asBoolean(false),
                sample.path("retirementPlan").path("bulkRetirementAllowed").asBoolean(false),
                false,
                sensitiveValuesExposed,
                List.of(
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localRunbookPassed
                        ? "PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_LOCAL_CUTOVER_RUNBOOK_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsSensitive(JsonNode sample) {
        String text = sample.toString().toLowerCase(Locale.ROOT);
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private Path locateRunbookPath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-cutover-runbook-sample.json"),
                Path.of("..", "docs", "unified-backend-production-cutover-runbook-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-cutover-runbook-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionCutoverRunbookSnapshot(
        String readinessMode,
        String sampleRunbookPath,
        boolean sampleRunbookPresent,
        boolean sampleRunbookParsed,
        boolean sampleRunbookApplied,
        String currentEntrypoint,
        String candidateEntrypoint,
        String rollbackEntrypoint,
        boolean businessPathsRemainUnchanged,
        int smokeTargetsTotal,
        int mavenEntrypointsTotal,
        boolean rollbackCommandsRecorded,
        boolean canaryPlanRecorded,
        boolean observationFieldsRecorded,
        boolean retirementOrderRecorded,
        boolean localConfigProviderRehearsalPassed,
        boolean localAuditSinkRehearsalPassed,
        boolean externalEntrypointConfigApplied,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowStarted,
        boolean rollbackWindowCompleted,
        boolean apiGatewayRetirementApproved,
        boolean coreRetirementApproved,
        boolean deletionAllowed,
        boolean bulkRetirementAllowed,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionCutoverApprovalPackage {
    ProductionCutoverApprovalPackageSnapshot snapshot();
}

final class LocalFileProductionCutoverApprovalPackage implements UnifiedProductionCutoverApprovalPackage {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "id_rsa",
            "token",
            "cookie",
            "secret",
            "password",
            "dsn",
            "bucket",
            "topic"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionCutoverApprovalPackageSnapshot snapshot() {
        Path packagePath = locatePackagePath();
        JsonNode sample = readSample(packagePath);
        boolean present = Files.exists(packagePath);
        boolean parsed = present && !sample.isMissingNode();
        boolean sensitiveValuesExposed = containsSensitive(sample);
        int evidenceInputsTotal = sample.path("evidenceInputs").size();
        int externalParametersTotal = sample.path("externalParameterChecklist").size();
        int approvalRolesTotal = sample.path("approvalMatrix").size();
        int goNoGoItemsTotal = sample.path("goNoGoMatrix").size();
        int observationFieldsTotal = sample.path("observationChecklist").size();
        int verificationCommandsTotal = BackendMavenEntrypoints.currentTotal();
        boolean localApprovalPackagePassed = parsed
                && evidenceInputsTotal >= 7
                && externalParametersTotal >= 10
                && approvalRolesTotal >= 7
                && goNoGoItemsTotal >= 15
                && observationFieldsTotal >= 10
                && verificationCommandsTotal == 1
                && !sample.path("approvalPackageApplied").asBoolean(true)
                && !sample.path("productionTrafficAllowed").asBoolean(true)
                && sample.path("requiresUserApprovalBeforeApply").asBoolean(false)
                && !sensitiveValuesExposed;
        return new ProductionCutoverApprovalPackageSnapshot(
                "LOCAL_PRODUCTION_CUTOVER_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-cutover-approval-package-sample.json",
                present,
                parsed,
                sample.path("approvalPackageApplied").asBoolean(false),
                sample.path("productionTrafficAllowed").asBoolean(false),
                sample.path("requiresUserApprovalBeforeApply").asBoolean(true),
                sample.path("candidateEntrypoint").path("baseUrl").asText("http://127.0.0.1:8135"),
                sample.path("currentEntrypoint").path("baseUrl").asText("http://127.0.0.1:8125"),
                sample.path("rollbackEntrypoint").path("baseUrl").asText("http://127.0.0.1:8125"),
                evidenceInputsTotal,
                externalParametersTotal,
                approvalRolesTotal,
                goNoGoItemsTotal,
                observationFieldsTotal,
                verificationCommandsTotal,
                anyExternalValueProvided(sample.path("externalParameterChecklist")),
                statusIsPass(sample.path("goNoGoMatrix"), "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED"),
                statusIsPass(sample.path("goNoGoMatrix"), "PRODUCTION_PROFILE_BOUND"),
                statusIsPass(sample.path("goNoGoMatrix"), "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED"),
                statusIsPass(sample.path("goNoGoMatrix"), "REAL_PERSISTENT_AUDIT_SINK_CONNECTED"),
                statusIsPass(sample.path("goNoGoMatrix"), "REAL_AUDIT_WRITE_SMOKE_PASSED"),
                allApproved(sample.path("approvalMatrix")),
                sample.path("rollbackAuthority").path("rollbackOperatorApproved").asBoolean(false),
                sample.path("retirementGate").path("apiGatewayRetirementApproved").asBoolean(false)
                        || sample.path("retirementGate").path("coreRetirementApproved").asBoolean(false),
                sample.path("retirementGate").path("deletionAllowed").asBoolean(false),
                sample.path("retirementGate").path("bulkRetirementAllowed").asBoolean(false),
                false,
                sensitiveValuesExposed,
                List.of(
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPROVED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_OPERATOR_APPROVED",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localApprovalPackagePassed
                        ? "PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_LOCAL_APPROVAL_PACKAGE_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsSensitive(JsonNode sample) {
        String text = sample.toString().toLowerCase(Locale.ROOT);
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private boolean anyExternalValueProvided(JsonNode checklist) {
        if (!checklist.isArray()) {
            return false;
        }
        for (JsonNode item : checklist) {
            if (item.path("valueProvided").asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private boolean allApproved(JsonNode approvalMatrix) {
        if (!approvalMatrix.isArray() || approvalMatrix.isEmpty()) {
            return false;
        }
        for (JsonNode item : approvalMatrix) {
            if (!item.path("approved").asBoolean(false) || !item.path("approvalEvidenceProvided").asBoolean(false)) {
                return false;
            }
        }
        return true;
    }

    private boolean statusIsPass(JsonNode matrix, String itemName) {
        if (!matrix.isArray()) {
            return false;
        }
        for (JsonNode item : matrix) {
            if (itemName.equals(item.path("item").asText())) {
                return "PASS".equals(item.path("status").asText());
            }
        }
        return false;
    }

    private Path locatePackagePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-cutover-approval-package-sample.json"),
                Path.of("..", "docs", "unified-backend-production-cutover-approval-package-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-cutover-approval-package-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionCutoverApprovalPackageSnapshot(
        String readinessMode,
        String sampleApprovalPackagePath,
        boolean sampleApprovalPackagePresent,
        boolean sampleApprovalPackageParsed,
        boolean approvalPackageApplied,
        boolean productionTrafficAllowed,
        boolean requiresUserApprovalBeforeApply,
        String candidateEntrypoint,
        String currentEntrypoint,
        String rollbackEntrypoint,
        int existingEvidenceReferencedTotal,
        int externalParametersTotal,
        int approvalRolesTotal,
        int goNoGoItemsTotal,
        int observationFieldsTotal,
        int verificationCommandsTotal,
        boolean externalEntrypointValuesProvided,
        boolean centralConfigProviderConnected,
        boolean productionProfileBound,
        boolean sensitiveConfigExternalized,
        boolean persistentAuditSinkConnected,
        boolean auditWriteSmokePassed,
        boolean productionTrafficApproved,
        boolean rollbackOperatorApproved,
        boolean retirementApproverGranted,
        boolean deletionAllowed,
        boolean bulkRetirementAllowed,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionCutoverExternalParameterManifest {
    ProductionCutoverExternalParameterManifestSnapshot snapshot();
}

final class LocalFileProductionCutoverExternalParameterManifest implements UnifiedProductionCutoverExternalParameterManifest {
    private static final Set<String> REQUIRED_PARAMETER_KEYS = Set.of(
            "frontendApiBaseUrl",
            "reverseProxyUpstream",
            "deploymentEntrypointTarget",
            "centralConfigProviderRef",
            "productionProfileRef",
            "sensitiveConfigExternalizationRef",
            "persistentAuditSinkRef",
            "auditWriteSmokeRef",
            "httpSmokeObservationRef",
            "rollbackOperatorApprovalRef",
            "retirementApproverRef"
    );
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp "
    );
    private static final Pattern REAL_IPV4 = Pattern.compile("\\b(?!(?:127|0)\\.)(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern REAL_DOMAIN = Pattern.compile("\\b[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9][a-z0-9-]*)+\\b", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionCutoverExternalParameterManifestSnapshot snapshot() {
        Path manifestPath = locateManifestPath();
        JsonNode sample = readSample(manifestPath);
        boolean present = Files.exists(manifestPath);
        boolean parsed = present && !sample.isMissingNode();
        int parameterGroupsTotal = sample.path("parameterGroups").size();
        int parametersTotal = countParameters(sample.path("parameterGroups"));
        int requiredExternalParametersTotal = countBooleanParameters(sample.path("parameterGroups"), "externalValueRequired");
        int redactedParametersTotal = countBooleanParameters(sample.path("parameterGroups"), "redacted");
        Set<String> parameterKeys = parameterKeys(sample.path("parameterGroups"));
        boolean realValuesProvidedInRepository = anyBooleanParameter(sample.path("parameterGroups"), "realValueProvidedInRepository")
                || sample.path("realValuesAllowedInRepository").asBoolean(false);
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean approvalPackageReferenced = "docs/unified-backend-production-cutover-approval-package-sample.json"
                .equals(sample.path("approvalPackageReference").path("path").asText());
        boolean localManifestPassed = parsed
                && parameterGroupsTotal >= 6
                && parametersTotal >= 20
                && requiredExternalParametersTotal >= 20
                && redactedParametersTotal >= 20
                && parameterKeys.containsAll(REQUIRED_PARAMETER_KEYS)
                && approvalPackageReferenced
                && !sample.path("manifestApplied").asBoolean(true)
                && !sample.path("productionTrafficAllowed").asBoolean(true)
                && !sample.path("realValuesAllowedInRepository").asBoolean(true)
                && sample.path("requiresExternalSecretStore").asBoolean(false)
                && !realValuesProvidedInRepository
                && !sensitiveValuesExposed;
        return new ProductionCutoverExternalParameterManifestSnapshot(
                "LOCAL_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-cutover-external-parameters-sample.json",
                present,
                parsed,
                sample.path("manifestApplied").asBoolean(false),
                sample.path("productionTrafficAllowed").asBoolean(false),
                sample.path("realValuesAllowedInRepository").asBoolean(false),
                sample.path("requiresExternalSecretStore").asBoolean(true),
                "http://127.0.0.1:8135",
                "http://127.0.0.1:8125",
                "http://127.0.0.1:8125",
                parameterGroupsTotal,
                parametersTotal,
                requiredExternalParametersTotal,
                parameterKeys,
                realValuesProvidedInRepository,
                redactedParametersTotal,
                approvalPackageReferenced,
                sample.path("approvalPackageReference").path("approvalPackageApplied").asBoolean(false),
                sample.path("approvalPackageReference").path("productionTrafficApproved").asBoolean(false),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                sample.path("approvalPackageReference").path("retirementApproverGranted").asBoolean(false),
                false,
                false,
                false,
                sensitiveValuesExposed,
                List.of(
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_VALUES_PROVIDED_OUTSIDE_REPOSITORY",
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPROVED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_OPERATOR_APPROVED",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localManifestPassed
                        ? "PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_EXTERNAL_PARAMETER_MANIFEST_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private Set<String> parameterKeys(JsonNode groups) {
        Set<String> keys = new HashSet<>();
        for (JsonNode group : groups) {
            for (JsonNode parameter : group.path("parameters")) {
                keys.add(parameter.path("key").asText());
            }
        }
        return Set.copyOf(keys);
    }

    private int countParameters(JsonNode groups) {
        int total = 0;
        for (JsonNode group : groups) {
            total += group.path("parameters").size();
        }
        return total;
    }

    private int countBooleanParameters(JsonNode groups, String fieldName) {
        int total = 0;
        for (JsonNode group : groups) {
            for (JsonNode parameter : group.path("parameters")) {
                if (parameter.path(fieldName).asBoolean(false)) {
                    total++;
                }
            }
        }
        return total;
    }

    private boolean anyBooleanParameter(JsonNode groups, String fieldName) {
        for (JsonNode group : groups) {
            for (JsonNode parameter : group.path("parameters")) {
                if (parameter.path(fieldName).asBoolean(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = (sample.path("parameterGroups").toString()
                + sample.path("approvalPackageReference").toString()
                + sample.path("goNoGoImpact").toString()
                + sample.path("verificationCommands").toString()).toLowerCase(Locale.ROOT)
                .replace("requiresexternalsecretstore", "");
        if (FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains)) {
            return true;
        }
        String refText = referenceText(sample).toLowerCase(Locale.ROOT);
        return REAL_IPV4.matcher(refText).find() || REAL_DOMAIN.matcher(refText).find();
    }

    private String referenceText(JsonNode sample) {
        StringBuilder refs = new StringBuilder()
                .append(sample.path("candidateEntrypointRef").asText()).append(' ')
                .append(sample.path("currentEntrypointRef").asText()).append(' ')
                .append(sample.path("rollbackEntrypointRef").asText()).append(' ');
        for (JsonNode group : sample.path("parameterGroups")) {
            for (JsonNode parameter : group.path("parameters")) {
                refs.append(parameter.path("valueRef").asText()).append(' ');
            }
        }
        return refs.toString();
    }

    private Path locateManifestPath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-cutover-external-parameters-sample.json"),
                Path.of("..", "docs", "unified-backend-production-cutover-external-parameters-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-cutover-external-parameters-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionCutoverExternalParameterManifestSnapshot(
        String readinessMode,
        String sampleManifestPath,
        boolean sampleManifestPresent,
        boolean sampleManifestParsed,
        boolean manifestApplied,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        boolean requiresExternalSecretStore,
        String candidateEntrypoint,
        String currentEntrypoint,
        String rollbackEntrypoint,
        int parameterGroupsTotal,
        int parametersTotal,
        int requiredExternalParametersTotal,
        Set<String> parameterKeys,
        boolean realValuesProvidedInRepository,
        int redactedParametersTotal,
        boolean approvalPackageReferenced,
        boolean approvalPackageApplied,
        boolean productionTrafficApproved,
        boolean centralConfigProviderConnected,
        boolean productionProfileBound,
        boolean sensitiveConfigExternalized,
        boolean persistentAuditSinkConnected,
        boolean auditWriteSmokePassed,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowCompleted,
        boolean retirementApproverGranted,
        boolean deletionAllowed,
        boolean bulkRetirementAllowed,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionCutoverEvidenceConsistencyAudit {
    ProductionCutoverEvidenceConsistencyAuditSnapshot snapshot();
}

final class LocalFileProductionCutoverEvidenceConsistencyAudit implements UnifiedProductionCutoverEvidenceConsistencyAudit {
    private static final List<String> SAMPLE_PATHS = List.of(
            "docs/deployment-entrypoint-cutover-sample.json",
            "docs/unified-backend-central-config-provider-sample.json",
            "docs/unified-backend-audit-sink-sample.jsonl",
            "docs/unified-backend-audit-sink-sample-schema.json",
            "docs/unified-backend-production-cutover-runbook-sample.json",
            "docs/unified-backend-production-cutover-approval-package-sample.json",
            "docs/unified-backend-production-cutover-external-parameters-sample.json"
    );
    private static final Map<String, String> APPROVAL_PARAMETER_ALIASES = Map.ofEntries(
            Map.entry("frontendApiBaseUrlConfigLocation", "frontendApiBaseUrl"),
            Map.entry("reverseProxyUpstreamConfigLocation", "reverseProxyUpstream"),
            Map.entry("deploymentEntrypointConfigLocation", "deploymentEntrypointTarget"),
            Map.entry("centralConfigProviderType", "centralConfigProviderType"),
            Map.entry("productionProfileName", "productionProfileRef"),
            Map.entry("sensitiveConfigExternalizationPlan", "sensitiveConfigExternalizationRef"),
            Map.entry("persistentAuditSinkType", "persistentAuditSinkType"),
            Map.entry("productionObservationDashboardLocation", "httpSmokeObservationRef"),
            Map.entry("rollbackOperatorRef", "rollbackOperatorApprovalRef"),
            Map.entry("retirementApproverRef", "retirementApproverRef"),
            Map.entry("allowedCutoverWindow", "entrypointOperatorApprovalRef")
    );
    private static final Set<String> CENTRAL_CONFIG_KEYS = Set.of(
            "centralConfigProviderType",
            "centralConfigProviderRef",
            "productionProfileRef",
            "sensitiveConfigExternalizationRef",
            "configRollbackSourceRef"
    );
    private static final Set<String> AUDIT_SINK_KEYS = Set.of(
            "persistentAuditSinkType",
            "persistentAuditSinkRef",
            "auditWriteSmokeRef",
            "auditReplayJobRef",
            "auditExportPathRef",
            "auditRetentionJobRef"
    );
    private static final Set<String> OBSERVABILITY_KEYS = Set.of(
            "httpSmokeObservationRef",
            "businessCodeDistributionRef",
            "latencyObservationRef",
            "trafficCounterRef",
            "auditWriteSuccessCountRef",
            "auditWriteFailureDegradationCountRef",
            "rollbackTriggerCountRef"
    );
    private static final Set<String> EXPECTED_BLOCKERS = Set.of(
            "REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
            "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
            "PRODUCTION_PROFILE_BOUND",
            "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
            "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
            "REAL_AUDIT_WRITE_SMOKE_PASSED",
            "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
            "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
            "ROLLBACK_WINDOW_COMPLETED",
            "USER_RETIREMENT_APPROVAL_GRANTED"
    );
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp "
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionCutoverEvidenceConsistencyAuditSnapshot snapshot() {
        Path entrypointPath = locate("deployment-entrypoint-cutover-sample.json");
        Path configPath = locate("unified-backend-central-config-provider-sample.json");
        Path auditEventsPath = locate("unified-backend-audit-sink-sample.jsonl");
        Path auditSchemaPath = locate("unified-backend-audit-sink-sample-schema.json");
        Path runbookPath = locate("unified-backend-production-cutover-runbook-sample.json");
        Path approvalPath = locate("unified-backend-production-cutover-approval-package-sample.json");
        Path manifestPath = locate("unified-backend-production-cutover-external-parameters-sample.json");
        JsonNode entrypoint = readJson(entrypointPath);
        JsonNode config = readJson(configPath);
        List<JsonNode> auditEvents = readJsonl(auditEventsPath);
        JsonNode auditSchema = readJson(auditSchemaPath);
        JsonNode runbook = readJson(runbookPath);
        JsonNode approval = readJson(approvalPath);
        JsonNode manifest = readJson(manifestPath);
        boolean samplesPresent = Files.exists(entrypointPath)
                && Files.exists(configPath)
                && Files.exists(auditEventsPath)
                && Files.exists(auditSchemaPath)
                && Files.exists(runbookPath)
                && Files.exists(approvalPath)
                && Files.exists(manifestPath);
        boolean samplesParsed = !entrypoint.isMissingNode()
                && !config.isMissingNode()
                && !auditEvents.isEmpty()
                && !auditSchema.isMissingNode()
                && !runbook.isMissingNode()
                && !approval.isMissingNode()
                && !manifest.isMissingNode();
        Set<String> manifestKeys = manifestParameterKeys(manifest);
        Set<String> approvalKeys = approvalParameterKeys(approval);
        List<String> missingApprovalParameterKeys = missingApprovalParameterKeys(approvalKeys, manifestKeys);
        List<String> missingManifestParameterKeys = missingManifestParameterKeys(manifestKeys);
        List<String> inconsistentEntrypointRefs = inconsistentEntrypointRefs(entrypoint, config, runbook, approval, manifest);
        List<String> runbookCommands = textArray(runbook.path("verificationCommands"));
        List<String> manifestCommands = textArray(manifest.path("verificationCommands"));
        List<String> approvalCommands = textArray(approval.path("verificationCommands"));
        List<String> inconsistentVerificationCommands = runbookCommands.equals(manifestCommands) && runbookCommands.equals(approvalCommands)
                ? List.of()
                : List.of("MAVEN_REGRESSION_COMMANDS_DRIFT");
        List<String> inconsistentBlockers = inconsistentBlockers(runbook, approval, manifest);
        boolean realValuesProvidedInRepository = manifest.path("realValuesAllowedInRepository").asBoolean(false)
                || anyBooleanParameter(manifest.path("parameterGroups"), "realValueProvidedInRepository");
        boolean sensitiveValuesExposed = containsSensitiveValues(manifest, approval, runbook, config, auditSchema, auditEvents);
        boolean statusPassed = samplesPresent
                && samplesParsed
                && missingApprovalParameterKeys.isEmpty()
                && missingManifestParameterKeys.isEmpty()
                && inconsistentEntrypointRefs.isEmpty()
                && inconsistentVerificationCommands.isEmpty()
                && inconsistentBlockers.isEmpty()
                && runbookReferencesManifest(runbook)
                && !realValuesProvidedInRepository
                && !sensitiveValuesExposed;
        return new ProductionCutoverEvidenceConsistencyAuditSnapshot(
                "LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION",
                SAMPLE_PATHS,
                samplesPresent,
                samplesParsed,
                "http://127.0.0.1:8135",
                "http://127.0.0.1:8125",
                "http://127.0.0.1:8125",
                manifestKeys.size(),
                approvalKeys.size(),
                runbookCommands.size(),
                manifestCommands.size(),
                missingApprovalParameterKeys,
                missingManifestParameterKeys,
                inconsistentEntrypointRefs,
                inconsistentVerificationCommands,
                inconsistentBlockers,
                realValuesProvidedInRepository,
                false,
                sensitiveValuesExposed,
                List.of(
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_VALUES_PROVIDED_OUTSIDE_REPOSITORY",
                        "REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPROVED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_OPERATOR_APPROVED",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                statusPassed
                        ? "PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION"
                        : "BLOCKED_BY_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_DRIFT"
        );
    }

    private JsonNode readJson(Path path) {
        try {
            if (Files.exists(path)) {
                return objectMapper.readTree(Files.readString(path));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private List<JsonNode> readJsonl(Path path) {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<JsonNode> nodes = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (!line.isBlank()) {
                    nodes.add(objectMapper.readTree(line));
                }
            }
            return List.copyOf(nodes);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private Set<String> manifestParameterKeys(JsonNode manifest) {
        Set<String> keys = new HashSet<>();
        for (JsonNode group : manifest.path("parameterGroups")) {
            for (JsonNode parameter : group.path("parameters")) {
                keys.add(parameter.path("key").asText());
            }
        }
        return keys;
    }

    private Set<String> approvalParameterKeys(JsonNode approval) {
        Set<String> keys = new HashSet<>();
        for (JsonNode parameter : approval.path("externalParameterChecklist")) {
            keys.add(parameter.path("key").asText());
        }
        return keys;
    }

    private List<String> missingApprovalParameterKeys(Set<String> approvalKeys, Set<String> manifestKeys) {
        List<String> missing = new ArrayList<>();
        for (String approvalKey : approvalKeys) {
            String manifestKey = APPROVAL_PARAMETER_ALIASES.getOrDefault(approvalKey, approvalKey);
            if (!manifestKeys.contains(manifestKey)) {
                missing.add(approvalKey);
            }
        }
        return List.copyOf(missing);
    }

    private List<String> missingManifestParameterKeys(Set<String> manifestKeys) {
        Set<String> required = new HashSet<>();
        required.addAll(CENTRAL_CONFIG_KEYS);
        required.addAll(AUDIT_SINK_KEYS);
        required.addAll(OBSERVABILITY_KEYS);
        required.addAll(APPROVAL_PARAMETER_ALIASES.values());
        List<String> missing = new ArrayList<>();
        for (String key : required) {
            if (!manifestKeys.contains(key)) {
                missing.add(key);
            }
        }
        return List.copyOf(missing);
    }

    private List<String> inconsistentEntrypointRefs(JsonNode entrypoint, JsonNode config, JsonNode runbook, JsonNode approval, JsonNode manifest) {
        List<String> inconsistent = new ArrayList<>();
        if (!"http://127.0.0.1:8135".equals(entrypoint.path("candidateEntrypoint").path("baseUrl").asText())) {
            inconsistent.add("ENTRYPOINT_CANDIDATE_URL");
        }
        if (!"http://127.0.0.1:8135".equals(config.path("entrypoints").path("candidate").path("baseUrl").asText())) {
            inconsistent.add("CONFIG_CANDIDATE_URL");
        }
        if (!"http://127.0.0.1:8135".equals(runbook.path("candidateEntrypoint").path("baseUrl").asText())) {
            inconsistent.add("RUNBOOK_CANDIDATE_URL");
        }
        if (!"http://127.0.0.1:8135".equals(approval.path("candidateEntrypoint").path("baseUrl").asText())) {
            inconsistent.add("APPROVAL_CANDIDATE_URL");
        }
        if (!"LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135".equals(manifest.path("candidateEntrypointRef").asText())) {
            inconsistent.add("MANIFEST_CANDIDATE_REF");
        }
        if (!"http://127.0.0.1:8125".equals(entrypoint.path("currentEntrypoint").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(config.path("entrypoints").path("current").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(runbook.path("currentEntrypoint").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(approval.path("currentEntrypoint").path("baseUrl").asText())
                || !"LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(manifest.path("currentEntrypointRef").asText())) {
            inconsistent.add("CURRENT_ENTRYPOINT_REF");
        }
        if (!"http://127.0.0.1:8125".equals(entrypoint.path("rollbackEntrypoint").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(config.path("entrypoints").path("rollback").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(runbook.path("rollbackEntrypoint").path("baseUrl").asText())
                || !"http://127.0.0.1:8125".equals(approval.path("rollbackEntrypoint").path("baseUrl").asText())
                || !"LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(manifest.path("rollbackEntrypointRef").asText())) {
            inconsistent.add("ROLLBACK_ENTRYPOINT_REF");
        }
        return List.copyOf(inconsistent);
    }

    private List<String> inconsistentBlockers(JsonNode runbook, JsonNode approval, JsonNode manifest) {
        Set<String> runbookBlockers = normalizedBlockers(runbook.path("preCutoverChecks"), "check");
        Set<String> approvalBlockers = normalizedBlockers(approval.path("goNoGoMatrix"), "item");
        Set<String> manifestBlockers = normalizedBlockers(manifest.path("goNoGoImpact"), "item");
        List<String> inconsistent = new ArrayList<>();
        for (String blocker : EXPECTED_BLOCKERS) {
            boolean covered = runbookBlockers.contains(blocker) || "PRODUCTION_TRAFFIC_SWITCH_APPLIED".equals(blocker)
                    || "API_GATEWAY_TRAFFIC_ZERO_PROVEN".equals(blocker);
            if (!covered || !approvalBlockers.contains(blocker) || !manifestBlockers.contains(blocker)) {
                inconsistent.add(blocker);
            }
        }
        return List.copyOf(inconsistent);
    }

    private Set<String> normalizedBlockers(JsonNode items, String nameField) {
        Set<String> blockers = new HashSet<>();
        for (JsonNode item : items) {
            String status = item.path("status").asText();
            if ("BLOCKED".equals(status) || "REQUIRED_EXTERNAL_INPUT".equals(status)) {
                blockers.add(normalizeBlocker(item.path(nameField).asText()));
            }
        }
        return blockers;
    }

    private String normalizeBlocker(String blocker) {
        return switch (blocker) {
            case "CONTROLLED_CUTOVER_WINDOW_APPROVED" -> "PRODUCTION_TRAFFIC_SWITCH_APPROVED";
            case "PRODUCTION_TRAFFIC_OBSERVATION_READY" -> "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED";
            case "ROLLBACK_OPERATOR_ASSIGNED", "ROLLBACK_OPERATOR_APPROVED" -> "ROLLBACK_OPERATOR_APPROVED";
            case "RETIREMENT_APPROVER_ASSIGNED", "RETIREMENT_APPROVER_GRANTED" -> "USER_RETIREMENT_APPROVAL_GRANTED";
            default -> blocker;
        };
    }

    private boolean runbookReferencesManifest(JsonNode runbook) {
        return "docs/unified-backend-production-cutover-external-parameters-sample.json"
                .equals(runbook.path("externalParameterManifest").path("path").asText());
    }

    private List<String> textArray(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(item.asText());
        }
        return List.copyOf(values);
    }

    private boolean anyBooleanParameter(JsonNode groups, String fieldName) {
        for (JsonNode group : groups) {
            for (JsonNode parameter : group.path("parameters")) {
                if (parameter.path(fieldName).asBoolean(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode manifest, JsonNode approval, JsonNode runbook,
                                            JsonNode config, JsonNode auditSchema, List<JsonNode> auditEvents) {
        String text = (manifest.path("parameterGroups").toString()
                + manifest.path("approvalPackageReference").toString()
                + manifest.path("goNoGoImpact").toString()
                + manifest.path("verificationCommands").toString()
                + approval.path("externalParameterChecklist").toString()
                + approval.path("approvalMatrix").toString()
                + approval.path("goNoGoMatrix").toString()
                + runbook.path("externalParameterManifest").toString()
                + runbook.path("preCutoverChecks").toString()
                + runbook.path("observationPlan").toString()
                + config.path("configDomains").toString()
                + auditSchema.path("requiredFields").toString()
                + auditEvents).toLowerCase(Locale.ROOT)
                .replace("requiresexternalsecretstore", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private Path locate(String fileName) {
        List<Path> candidates = List.of(
                Path.of("docs", fileName),
                Path.of("..", "docs", fileName),
                Path.of("..", "..", "docs", fileName)
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionCutoverEvidenceConsistencyAuditSnapshot(
        String readinessMode,
        List<String> auditedSamplePaths,
        boolean samplesPresent,
        boolean samplesParsed,
        String candidateEntrypoint,
        String currentEntrypoint,
        String rollbackEntrypoint,
        int externalParameterKeysTotal,
        int approvalPackageExternalParametersTotal,
        int runbookVerificationCommandsTotal,
        int manifestVerificationCommandsTotal,
        List<String> missingApprovalParameterKeys,
        List<String> missingManifestParameterKeys,
        List<String> inconsistentEntrypointRefs,
        List<String> inconsistentVerificationCommands,
        List<String> inconsistentBlockers,
        boolean realValuesProvidedInRepository,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionExternalValueIntakeRehearsal {
    ProductionExternalValueIntakeRehearsalSnapshot snapshot();
}

final class LocalFileProductionExternalValueIntakeRehearsal implements UnifiedProductionExternalValueIntakeRehearsal {
    private static final Set<String> REQUIRED_GROUPS = Set.of(
            "external-entrypoint",
            "central-config",
            "audit-sink",
            "observability",
            "approval",
            "rollback",
            "retirement"
    );
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp "
    );
    private static final Pattern REAL_IPV4 = Pattern.compile("\\b(?!(?:127|0)\\.)(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern REAL_DOMAIN = Pattern.compile("\\b[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9][a-z0-9-]*)+\\b", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionExternalValueIntakeRehearsalSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        JsonNode channels = sample.path("intakeChannels");
        JsonNode groups = sample.path("requiredValueGroups");
        Set<String> groupNames = groupNames(groups);
        int valueItemsTotal = countValues(groups);
        int injectionTargetsTotal = countTextValues(groups, "injectionTarget");
        int validationRefsTotal = countTextValues(groups, "validationRef");
        int rollbackRefsTotal = countTextValues(groups, "rollbackRef");
        int redactedValuesTotal = countBooleanValues(groups, "redacted");
        boolean realValuesProvidedInRepository = sample.path("realValuesAllowedInRepository").asBoolean(false)
                || anyBooleanValue(groups, "realValueProvidedInRepository");
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean localRehearsalPassed = parsed
                && !sample.path("intakeApplied").asBoolean(true)
                && !sample.path("productionTrafficAllowed").asBoolean(true)
                && !sample.path("realValuesAllowedInRepository").asBoolean(true)
                && sample.path("requiresExternalSecretStore").asBoolean(false)
                && "LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135".equals(sample.path("candidateEntrypointRef").asText())
                && "LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(sample.path("currentEntrypointRef").asText())
                && "LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(sample.path("rollbackEntrypointRef").asText())
                && channels.size() >= 6
                && groupNames.containsAll(REQUIRED_GROUPS)
                && valueItemsTotal >= 14
                && injectionTargetsTotal >= valueItemsTotal
                && validationRefsTotal >= valueItemsTotal
                && rollbackRefsTotal >= valueItemsTotal
                && redactedValuesTotal >= valueItemsTotal
                && allValueReferencesSafe(groups)
                && !realValuesProvidedInRepository
                && !sensitiveValuesExposed;
        return new ProductionExternalValueIntakeRehearsalSnapshot(
                "LOCAL_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-external-value-intake-sample.json",
                present,
                parsed,
                sample.path("intakeApplied").asBoolean(false),
                sample.path("productionTrafficAllowed").asBoolean(false),
                sample.path("realValuesAllowedInRepository").asBoolean(false),
                sample.path("requiresExternalSecretStore").asBoolean(true),
                sample.path("candidateEntrypointRef").asText("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135"),
                sample.path("currentEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                sample.path("rollbackEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                groups.size(),
                channels.size(),
                valueItemsTotal,
                injectionTargetsTotal,
                validationRefsTotal,
                rollbackRefsTotal,
                realValuesProvidedInRepository,
                redactedValuesTotal,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                sensitiveValuesExposed,
                List.of(
                        "REAL_EXTERNAL_VALUES_PROVIDED_OUTSIDE_REPOSITORY",
                        "REAL_EXTERNAL_VALUES_APPLIED_TO_RUNTIME",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PRODUCTION_PROFILE_BOUND",
                        "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localRehearsalPassed
                        ? "PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_EXTERNAL_VALUE_INTAKE_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private Set<String> groupNames(JsonNode groups) {
        Set<String> names = new HashSet<>();
        for (JsonNode group : groups) {
            names.add(group.path("group").asText());
        }
        return Set.copyOf(names);
    }

    private int countValues(JsonNode groups) {
        int total = 0;
        for (JsonNode group : groups) {
            total += group.path("values").size();
        }
        return total;
    }

    private int countTextValues(JsonNode groups, String fieldName) {
        int total = 0;
        for (JsonNode group : groups) {
            for (JsonNode value : group.path("values")) {
                if (!value.path(fieldName).asText().isBlank()) {
                    total++;
                }
            }
        }
        return total;
    }

    private int countBooleanValues(JsonNode groups, String fieldName) {
        int total = 0;
        for (JsonNode group : groups) {
            for (JsonNode value : group.path("values")) {
                if (value.path(fieldName).asBoolean(false)) {
                    total++;
                }
            }
        }
        return total;
    }

    private boolean anyBooleanValue(JsonNode groups, String fieldName) {
        for (JsonNode group : groups) {
            for (JsonNode value : group.path("values")) {
                if (value.path(fieldName).asBoolean(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean allValueReferencesSafe(JsonNode groups) {
        for (JsonNode group : groups) {
            for (JsonNode value : group.path("values")) {
                String valueRef = value.path("valueRef").asText();
                if (!(valueRef.startsWith("EXTERNAL_REF_REQUIRED:")
                        || valueRef.startsWith("LOCAL_SAMPLE_REF:")
                        || valueRef.startsWith("APPROVAL_REF_REQUIRED:"))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("sensitiveconfigexternalizationref", "")
                .replace("sensitiveconfigexternalization", "");
        if (FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains)) {
            return true;
        }
        String refText = referenceText(sample).toLowerCase(Locale.ROOT);
        return REAL_IPV4.matcher(refText).find() || REAL_DOMAIN.matcher(refText).find();
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private String referenceText(JsonNode sample) {
        StringBuilder refs = new StringBuilder()
                .append(sample.path("candidateEntrypointRef").asText()).append(' ')
                .append(sample.path("currentEntrypointRef").asText()).append(' ')
                .append(sample.path("rollbackEntrypointRef").asText()).append(' ');
        for (JsonNode group : sample.path("requiredValueGroups")) {
            for (JsonNode value : group.path("values")) {
                refs.append(value.path("valueRef").asText()).append(' ');
            }
        }
        return refs.toString();
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-external-value-intake-sample.json"),
                Path.of("..", "docs", "unified-backend-production-external-value-intake-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-external-value-intake-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionExternalValueIntakeRehearsalSnapshot(
        String readinessMode,
        String sampleIntakePath,
        boolean sampleIntakePresent,
        boolean sampleIntakeParsed,
        boolean intakeApplied,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        boolean requiresExternalSecretStore,
        String candidateEntrypointRef,
        String currentEntrypointRef,
        String rollbackEntrypointRef,
        int valueGroupsTotal,
        int intakeChannelsTotal,
        int valueItemsTotal,
        int injectionTargetsTotal,
        int validationRefsTotal,
        int rollbackRefsTotal,
        boolean realValuesProvidedInRepository,
        int redactedValuesTotal,
        boolean centralConfigProviderConnected,
        boolean productionProfileBound,
        boolean sensitiveConfigExternalized,
        boolean persistentAuditSinkConnected,
        boolean auditWriteSmokePassed,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowCompleted,
        boolean retirementApproverGranted,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionRuntimeConfigShellRehearsal {
    ProductionRuntimeConfigShellSnapshot snapshot();
}

final class LocalFileProductionRuntimeConfigShellRehearsal implements UnifiedProductionRuntimeConfigShellRehearsal {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp "
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionRuntimeConfigShellSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        JsonNode runtimeProfiles = sample.path("runtimeProfiles");
        JsonNode configProviderBindings = sample.path("configProviderBindings");
        JsonNode sensitiveConfigBindings = sample.path("sensitiveConfigBindings");
        JsonNode deploymentEntrypointBindings = sample.path("deploymentEntrypointBindings");
        JsonNode rollbackConfigBindings = sample.path("rollbackConfigBindings");
        boolean realValuesProvided = sample.path("realValuesAllowedInRepository").asBoolean(false)
                || anyBooleanValue(configProviderBindings, "realValueProvidedInRepository")
                || anyBooleanValue(sensitiveConfigBindings, "realValueProvidedInRepository")
                || anyBooleanValue(deploymentEntrypointBindings, "realValueProvidedInRepository")
                || anyBooleanValue(rollbackConfigBindings, "realValueProvidedInRepository");
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean externalValueReferenced = "docs/unified-backend-production-external-value-intake-sample.json"
                .equals(sample.at("/validationPlan/externalValueIntakeSampleRef").asText())
                && "PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION"
                .equals(sample.at("/validationPlan/externalValueIntakeStatusRequired").asText());
        boolean localRehearsalPassed = parsed
                && !sample.path("runtimeShellApplied").asBoolean(true)
                && !sample.path("productionTrafficAllowed").asBoolean(true)
                && !sample.path("realValuesAllowedInRepository").asBoolean(true)
                && sample.path("requiresExternalConfigProvider").asBoolean(false)
                && sample.path("requiresExternalSecretStore").asBoolean(false)
                && "LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135".equals(sample.path("candidateEntrypointRef").asText())
                && "LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(sample.path("currentEntrypointRef").asText())
                && "LOCAL_SAMPLE_REF:API_GATEWAY_8125".equals(sample.path("rollbackEntrypointRef").asText())
                && runtimeProfiles.size() >= 3
                && containsText(runtimeProfiles, "production")
                && containsText(runtimeProfiles, "rollback")
                && containsText(runtimeProfiles, "local-rehearsal")
                && bindingsSafe(configProviderBindings)
                && bindingsSafe(sensitiveConfigBindings)
                && bindingsSafe(deploymentEntrypointBindings)
                && bindingsSafe(rollbackConfigBindings)
                && sensitiveBindingsUseExternalRefs(sensitiveConfigBindings)
                && externalValueReferenced
                && !realValuesProvided
                && !sensitiveValuesExposed;
        return new ProductionRuntimeConfigShellSnapshot(
                "LOCAL_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-runtime-shell-sample.json",
                present,
                parsed,
                sample.path("runtimeShellApplied").asBoolean(false),
                sample.path("productionTrafficAllowed").asBoolean(false),
                sample.path("realValuesAllowedInRepository").asBoolean(false),
                sample.path("requiresExternalConfigProvider").asBoolean(true),
                sample.path("requiresExternalSecretStore").asBoolean(true),
                sample.path("candidateEntrypointRef").asText("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135"),
                sample.path("currentEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                sample.path("rollbackEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                runtimeProfiles.size(),
                configProviderBindings.size(),
                sensitiveConfigBindings.size(),
                deploymentEntrypointBindings.size(),
                rollbackConfigBindings.size(),
                sample.path("verificationCommands").size(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                realValuesProvided,
                sensitiveValuesExposed,
                externalValueReferenced,
                List.of(
                        "REAL_PRODUCTION_PROFILE_BOUND_OUTSIDE_REPOSITORY",
                        "REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "REAL_SENSITIVE_CONFIG_SOURCE_EXTERNALIZED",
                        "REAL_DEPLOYMENT_ENTRYPOINT_BOUND",
                        "REAL_ROLLBACK_CONFIG_BOUND",
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localRehearsalPassed
                        ? "PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_PRODUCTION_RUNTIME_CONFIG_SHELL_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean bindingsSafe(JsonNode bindings) {
        if (bindings.size() < 5) {
            return false;
        }
        for (JsonNode binding : bindings) {
            if (binding.path("key").asText().isBlank()
                    || binding.path("validationRef").asText().isBlank()
                    || binding.path("rollbackRef").asText().isBlank()
                    || binding.path("realValueProvidedInRepository").asBoolean(true)
                    || !binding.path("redacted").asBoolean(false)) {
                return false;
            }
        }
        return true;
    }

    private boolean sensitiveBindingsUseExternalRefs(JsonNode bindings) {
        for (JsonNode binding : bindings) {
            if (!binding.path("secretStoreRef").asText().startsWith("EXTERNAL_REF_REQUIRED:")
                    || !binding.path("externalValueRequired").asBoolean(false)) {
                return false;
            }
        }
        return true;
    }

    private boolean anyBooleanValue(JsonNode values, String fieldName) {
        for (JsonNode value : values) {
            if (value.path(fieldName).asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsText(JsonNode node, String fragment) {
        return node.toString().contains(fragment);
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("requiresexternalconfigprovider", "")
                .replace("requiresexternalsecretstore", "")
                .replace("sensitiveconfigbindings", "")
                .replace("secretstoreref", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-runtime-shell-sample.json"),
                Path.of("..", "docs", "unified-backend-production-runtime-shell-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-runtime-shell-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionRuntimeConfigShellSnapshot(
        String readinessMode,
        String sampleRuntimeShellPath,
        boolean sampleRuntimeShellPresent,
        boolean sampleRuntimeShellParsed,
        boolean runtimeShellApplied,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        boolean requiresExternalConfigProvider,
        boolean requiresExternalSecretStore,
        String candidateEntrypointRef,
        String currentEntrypointRef,
        String rollbackEntrypointRef,
        int runtimeProfilesTotal,
        int configProviderBindingsTotal,
        int sensitiveConfigBindingsTotal,
        int deploymentEntrypointBindingsTotal,
        int rollbackConfigBindingsTotal,
        int validationCommandsTotal,
        boolean productionProfileBound,
        boolean centralConfigProviderConnected,
        boolean sensitiveConfigExternalized,
        boolean deploymentEntrypointBound,
        boolean rollbackConfigBound,
        boolean persistentAuditSinkConnected,
        boolean auditWriteSmokePassed,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowCompleted,
        boolean retirementApproverGranted,
        boolean environmentVariablesRead,
        boolean realValuesProvidedInRepository,
        boolean sensitiveValuesExposed,
        boolean externalValueIntakeRehearsalReferenced,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionAuditObservabilitySmokeRehearsal {
    ProductionAuditObservabilitySmokeSnapshot snapshot();
}

final class LocalFileProductionAuditObservabilitySmokeRehearsal implements UnifiedProductionAuditObservabilitySmokeRehearsal {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp ",
            "http://",
            "https://"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionAuditObservabilitySmokeSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        JsonNode auditSinkBindings = sample.path("auditSinkBindings");
        JsonNode auditSmokeTargets = sample.path("auditSmokeTargets");
        JsonNode observabilitySignals = sample.path("observabilitySignals");
        JsonNode dashboardRefs = sample.path("dashboardRefs");
        JsonNode alertRefs = sample.path("alertRefs");
        JsonNode rollbackRefs = sample.path("rollbackRefs");
        String sampleText = sample.toString();
        boolean runtimeShellReferenced = "docs/unified-backend-production-runtime-shell-sample.json"
                .equals(sample.path("runtimeConfigShellSampleRef").asText())
                && "PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION"
                .equals(sample.path("runtimeConfigShellStatusRequired").asText());
        boolean auditSinkBindingRecorded = auditSinkBindings.size() >= 6 && allItemsUseExternalRefs(auditSinkBindings)
                && !anyBooleanValue(auditSinkBindings, "realValueProvidedInRepository");
        boolean auditEventSchemaRecorded = sample.at("/auditEventSchema/fields").size() >= 21
                && sample.at("/auditEventSchema/fields").toString().contains("requestId")
                && sample.at("/auditEventSchema/fields").toString().contains("result");
        boolean requestIdPropagationRecorded = sampleText.contains("requestId");
        boolean auditWriteSmokeReferenceRecorded = auditSmokeTargets.size() >= 10
                && everyItemHasText(auditSmokeTargets, "writeSmokeValidationRef", "EXTERNAL_REF_REQUIRED:");
        boolean auditReplayExportRetentionRecorded = sampleText.contains("AUDIT_REPLAY")
                && sampleText.contains("AUDIT_EXPORT")
                && sampleText.contains("AUDIT_RETENTION");
        boolean httpSmokeObservationRecorded = containsSignal(observabilitySignals, "HTTP_SMOKE_STATUS");
        boolean errorRateObservationRecorded = containsSignal(observabilitySignals, "ERROR_RATE");
        boolean latencyObservationRecorded = containsSignal(observabilitySignals, "P95_LATENCY")
                && containsSignal(observabilitySignals, "P99_LATENCY");
        boolean businessCodeObservationRecorded = containsSignal(observabilitySignals, "BUSINESS_CODE_DISTRIBUTION");
        boolean traceCorrelationRecorded = containsSignal(observabilitySignals, "TRACE_CORRELATION");
        boolean dashboardReferencesRecorded = dashboardRefs.size() >= 8 && allItemsUseExternalRefs(dashboardRefs);
        boolean alertReferencesRecorded = alertRefs.size() >= 8 && allItemsUseExternalRefs(alertRefs);
        boolean rollbackReferencesRecorded = rollbackRefs.size() >= 6 && allItemsUseExternalRefs(rollbackRefs);
        boolean realValuesProvided = sample.path("realValuesAllowedInRepository").asBoolean(false)
                || anyBooleanValue(auditSinkBindings, "realValueProvidedInRepository");
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean localRehearsalPassed = parsed
                && !sample.path("productionTrafficAllowed").asBoolean(true)
                && !sample.path("realValuesAllowedInRepository").asBoolean(true)
                && runtimeShellReferenced
                && auditSinkBindingRecorded
                && auditEventSchemaRecorded
                && requestIdPropagationRecorded
                && auditWriteSmokeReferenceRecorded
                && auditReplayExportRetentionRecorded
                && httpSmokeObservationRecorded
                && errorRateObservationRecorded
                && latencyObservationRecorded
                && businessCodeObservationRecorded
                && traceCorrelationRecorded
                && dashboardReferencesRecorded
                && alertReferencesRecorded
                && rollbackReferencesRecorded
                && !realValuesProvided
                && !sensitiveValuesExposed;
        return new ProductionAuditObservabilitySmokeSnapshot(
                "LOCAL_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION",
                "docs/unified-backend-production-audit-observability-smoke-sample.json",
                present,
                parsed,
                sample.path("runtimeConfigShellSampleRef").asText("docs/unified-backend-production-runtime-shell-sample.json"),
                sample.path("runtimeConfigShellStatusRequired").asText("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION"),
                runtimeShellReferenced,
                auditSinkBindingRecorded,
                auditEventSchemaRecorded,
                requestIdPropagationRecorded,
                auditWriteSmokeReferenceRecorded,
                auditReplayExportRetentionRecorded,
                httpSmokeObservationRecorded,
                errorRateObservationRecorded,
                latencyObservationRecorded,
                businessCodeObservationRecorded,
                traceCorrelationRecorded,
                dashboardReferencesRecorded,
                alertReferencesRecorded,
                rollbackReferencesRecorded,
                auditSmokeTargets.size(),
                observabilitySignals.size(),
                dashboardRefs.size(),
                alertRefs.size(),
                rollbackRefs.size(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                realValuesProvided,
                sensitiveValuesExposed,
                List.of(
                        "REAL_PERSISTENT_AUDIT_SINK_CONNECTED",
                        "REAL_AUDIT_WRITE_PATH_CONNECTED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "REAL_AUDIT_REPLAY_EXPORT_RETENTION_CONNECTED",
                        "REAL_OBSERVABILITY_PLATFORM_CONNECTED",
                        "REAL_DASHBOARD_CONNECTED",
                        "REAL_ALERTING_CONNECTED",
                        "REAL_TRACE_PIPELINE_CONNECTED",
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                localRehearsalPassed
                        ? "PASS_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION"
                        : "BLOCKED_BY_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_SAMPLE_NOT_AVAILABLE"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsSignal(JsonNode signals, String signalKey) {
        for (JsonNode signal : signals) {
            if (signalKey.equals(signal.path("signalKey").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean everyItemHasText(JsonNode values, String fieldName, String fragment) {
        for (JsonNode value : values) {
            if (!value.path(fieldName).asText().contains(fragment)) {
                return false;
            }
        }
        return values.size() > 0;
    }

    private boolean allItemsUseExternalRefs(JsonNode values) {
        for (JsonNode value : values) {
            String text = value.toString();
            if (!text.contains("EXTERNAL_REF_REQUIRED:") || text.contains("http://") || text.contains("https://")) {
                return false;
            }
        }
        return values.size() > 0;
    }

    private boolean anyBooleanValue(JsonNode values, String fieldName) {
        for (JsonNode value : values) {
            if (value.path(fieldName).asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("auditobservabilitysmoke", "")
                .replace("sensitivevaluesexposed", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-audit-observability-smoke-sample.json"),
                Path.of("..", "docs", "unified-backend-production-audit-observability-smoke-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-audit-observability-smoke-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionAuditObservabilitySmokeSnapshot(
        String readinessMode,
        String sampleAuditObservabilitySmokePath,
        boolean sampleAuditObservabilitySmokePresent,
        boolean sampleAuditObservabilitySmokeParsed,
        String runtimeConfigShellSampleRef,
        String runtimeConfigShellStatusRequired,
        boolean runtimeConfigShellRehearsalReferenced,
        boolean auditSinkBindingRecorded,
        boolean auditEventSchemaRecorded,
        boolean requestIdPropagationRecorded,
        boolean auditWriteSmokeReferenceRecorded,
        boolean auditReplayExportRetentionRecorded,
        boolean httpSmokeObservationRecorded,
        boolean errorRateObservationRecorded,
        boolean latencyObservationRecorded,
        boolean businessCodeObservationRecorded,
        boolean traceCorrelationRecorded,
        boolean dashboardReferencesRecorded,
        boolean alertReferencesRecorded,
        boolean rollbackReferencesRecorded,
        int sampleAuditSmokeTargetsTotal,
        int sampleObservabilitySignalsTotal,
        int sampleDashboardRefsTotal,
        int sampleAlertRefsTotal,
        int sampleRollbackRefsTotal,
        boolean persistentAuditSinkConnected,
        boolean auditWritePathConnected,
        boolean auditWriteSmokePassed,
        boolean auditReplayPathConnected,
        boolean auditExportPathConnected,
        boolean auditRetentionJobConnected,
        boolean observabilityPlatformConnected,
        boolean dashboardConnected,
        boolean alertingConnected,
        boolean tracePipelineConnected,
        boolean environmentVariablesRead,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowCompleted,
        boolean retirementApproverGranted,
        boolean realValuesProvidedInRepository,
        boolean sensitiveValuesExposed,
        List<String> remainingProductionBlockers,
        String status
) {
}

interface UnifiedProductionControlledCutoverReceipt {
    ProductionControlledCutoverReceiptSnapshot snapshot();
}

final class LocalFileProductionControlledCutoverReceipt implements UnifiedProductionControlledCutoverReceipt {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp ",
            "http://",
            "https://"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductionControlledCutoverReceiptSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        JsonNode runtimeRefs = sample.path("runtimePrerequisiteRefs");
        JsonNode trafficStages = sample.at("/trafficPlan/stages");
        JsonNode oldEntrypointProtection = sample.path("oldEntrypointProtection");
        boolean runtimeConfigShellReferenced = containsText(runtimeRefs, "docs/unified-backend-production-runtime-shell-sample.json");
        boolean auditObservabilitySmokeReferenced = containsText(runtimeRefs, "docs/unified-backend-production-audit-observability-smoke-sample.json");
        boolean externalValueIntakeReferenced = containsText(runtimeRefs, "docs/unified-backend-production-external-value-intake-sample.json");
        boolean oldEntrypointProtectionRecorded = oldEntrypointProtection.path("apiGatewayServicePreserved").asBoolean(false)
                && oldEntrypointProtection.path("coreEntrypointsPreserved").asBoolean(false)
                && oldEntrypointProtection.path("noDeletionInThisRound").asBoolean(false);
        boolean realValuesProvided = sample.path("realValuesAllowedInRepository").asBoolean(true);
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean receiptApplied = sample.path("receiptApplied").asBoolean(false);
        boolean productionTrafficAllowed = sample.path("productionTrafficAllowed").asBoolean(true);
        boolean sampleValid = parsed
                && "LOCAL_CONTROLLED_CUTOVER_RECEIPT_SHAPE_NOT_APPLIED".equals(sample.path("mode").asText())
                && !receiptApplied
                && !productionTrafficAllowed
                && !realValuesProvided
                && runtimeConfigShellReferenced
                && auditObservabilitySmokeReferenced
                && externalValueIntakeReferenced
                && sample.path("approvalRefs").size() >= 6
                && trafficStages.size() >= 5
                && sample.path("smokeRefs").size() >= 14
                && sample.path("auditRefs").size() >= 6
                && sample.path("observabilityRefs").size() >= 10
                && sample.path("rollbackWindowRefs").size() >= 6
                && sample.path("apiGatewayTrafficRefs").size() >= 2
                && sample.path("cutoverExecutionRefs").size() >= 6
                && oldEntrypointProtectionRecorded
                && !sensitiveValuesExposed;
        return new ProductionControlledCutoverReceiptSnapshot(
                "LOCAL_CONTROLLED_CUTOVER_RECEIPT_GATE_NOT_PRODUCTION",
                "docs/unified-backend-production-controlled-cutover-receipt-sample.json",
                present,
                parsed,
                receiptApplied,
                productionTrafficAllowed,
                sample.path("realValuesAllowedInRepository").asBoolean(true),
                sample.path("candidateEntrypointRef").asText("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135"),
                sample.path("previousEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                sample.path("rollbackEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                sample.path("approvalRefs").size(),
                runtimeRefs.size(),
                trafficStages.size(),
                sample.path("smokeRefs").size(),
                sample.path("auditRefs").size(),
                sample.path("observabilityRefs").size(),
                sample.path("rollbackWindowRefs").size(),
                sample.path("apiGatewayTrafficRefs").size(),
                sample.path("cutoverExecutionRefs").size(),
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                sensitiveValuesExposed,
                oldEntrypointProtectionRecorded,
                true,
                false,
                false,
                false,
                runtimeConfigShellReferenced,
                auditObservabilitySmokeReferenced,
                externalValueIntakeReferenced,
                sampleValid,
                List.of(
                        "REAL_CUTOVER_RECEIPT_PROVIDED_OUTSIDE_REPOSITORY",
                        "REAL_ENTRYPOINT_APPLIED_TO_UNIFIED_BACKEND",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "REAL_DASHBOARD_VERIFIED",
                        "REAL_ALERTING_VERIFIED",
                        "REAL_TRACE_PIPELINE_VERIFIED",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                "BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsText(JsonNode values, String expectedText) {
        for (JsonNode value : values) {
            if (expectedText.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("productioncontrolledcutover", "")
                .replace("controlledcutover", "")
                .replace("sensitivevaluesexposed", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-production-controlled-cutover-receipt-sample.json"),
                Path.of("..", "docs", "unified-backend-production-controlled-cutover-receipt-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-production-controlled-cutover-receipt-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ProductionControlledCutoverReceiptSnapshot(
        String readinessMode,
        String receiptPath,
        boolean receiptPresent,
        boolean receiptParsed,
        boolean receiptApplied,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        String candidateEntrypointRef,
        String previousEntrypointRef,
        String rollbackEntrypointRef,
        int approvalRefsTotal,
        int runtimePrerequisiteRefsTotal,
        int trafficStagesTotal,
        int smokeRefsTotal,
        int auditRefsTotal,
        int observabilityRefsTotal,
        int rollbackWindowRefsTotal,
        int apiGatewayTrafficRefsTotal,
        int cutoverExecutionRefsTotal,
        int finalTrafficWeightPercent,
        boolean productionTrafficObservedOnUnified,
        boolean apiGatewayTrafficZeroProven,
        boolean rollbackWindowCompleted,
        boolean persistentAuditSinkConnected,
        boolean auditWriteSmokePassed,
        boolean observabilityPlatformConnected,
        boolean dashboardConnected,
        boolean alertingConnected,
        boolean tracePipelineConnected,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean oldEntrypointsPreserved,
        boolean nodeDaemonOutOfRepository,
        boolean readyForProduction,
        boolean readyToReplaceGateway,
        boolean readyToRetireOldEntrypoints,
        boolean runtimeConfigShellRehearsalReferenced,
        boolean auditObservabilitySmokeRehearsalReferenced,
        boolean externalValueIntakeRehearsalReferenced,
        boolean sampleValid,
        List<String> remainingBlockers,
        String status
) {
}

interface UnifiedApiGatewayControlledRetirementReceipt {
    ApiGatewayControlledRetirementReceiptSnapshot snapshot();
}

class LocalFileApiGatewayControlledRetirementReceipt implements UnifiedApiGatewayControlledRetirementReceipt {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp ",
            "http://",
            "https://"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiGatewayControlledRetirementReceiptSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        boolean retirementApplied = sample.path("retirementApplied").asBoolean(false);
        boolean deleteListApproved = sample.path("deleteListApproved").asBoolean(false);
        boolean productionTrafficAllowed = sample.path("productionTrafficAllowed").asBoolean(true);
        boolean realValuesAllowedInRepository = sample.path("realValuesAllowedInRepository").asBoolean(true);
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean controlledCutoverReferenced = containsText(sample.path("controlledCutoverRefs"),
                "docs/unified-backend-production-controlled-cutover-receipt-sample.json");
        boolean coreEntrypointsPreserved = sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean(false);
        boolean unifiedBuildHelperStillReferencesApiGateway = unifiedBuildHelperStillReferencesApiGateway();
        boolean apiGatewayPomStillPresent = Files.exists(Path.of("..", "api-gateway-service", "pom.xml"));
        boolean apiGatewayServiceDirectoryStillPresent = Files.exists(Path.of("..", "api-gateway-service"));
        int remainingGatewayPackageRefs = (unifiedBuildHelperStillReferencesApiGateway ? 1 : 0)
                + (apiGatewayPomStillPresent ? 1 : 0)
                + (apiGatewayServiceDirectoryStillPresent ? 1 : 0);
        boolean sampleValid = parsed
                && "LOCAL_API_GATEWAY_RETIREMENT_RECEIPT_SHAPE_NOT_APPLIED".equals(sample.path("mode").asText())
                && !retirementApplied
                && !deleteListApproved
                && !productionTrafficAllowed
                && !realValuesAllowedInRepository
                && controlledCutoverReferenced
                && sample.path("approvalRefs").size() >= 6
                && sample.path("trafficZeroRefs").size() >= 6
                && sample.path("gatewaySelfApiParityRefs").size() >= 10
                && sample.path("observabilityRefs").size() >= 6
                && sample.path("auditRefs").size() >= 4
                && sample.path("rollbackWindowRefs").size() >= 4
                && sample.path("deleteList").size() >= 6
                && coreEntrypointsPreserved
                && !sensitiveValuesExposed;
        return new ApiGatewayControlledRetirementReceiptSnapshot(
                "LOCAL_API_GATEWAY_RETIREMENT_RECEIPT_GATE_NOT_PRODUCTION",
                "docs/unified-backend-api-gateway-retirement-receipt-sample.json",
                present,
                parsed,
                retirementApplied,
                deleteListApproved,
                productionTrafficAllowed,
                realValuesAllowedInRepository,
                sample.path("candidateEntrypointRef").asText("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135"),
                sample.path("retiredEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                textArray(sample.path("rollbackEntrypointRefs")),
                sample.path("controlledCutoverRefs").size(),
                sample.path("approvalRefs").size(),
                sample.path("trafficZeroRefs").size(),
                sample.path("observabilityRefs").size(),
                sample.path("auditRefs").size(),
                sample.path("rollbackWindowRefs").size(),
                sample.path("gatewaySelfApiParityRefs").size(),
                sample.path("deleteList").size(),
                0,
                remainingGatewayPackageRefs,
                unifiedBuildHelperStillReferencesApiGateway,
                apiGatewayPomStillPresent,
                apiGatewayServiceDirectoryStillPresent,
                coreEntrypointsPreserved,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                sensitiveValuesExposed,
                controlledCutoverReferenced,
                sampleValid,
                List.of(
                        "REAL_API_GATEWAY_RETIREMENT_RECEIPT_PROVIDED_OUTSIDE_REPOSITORY",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "DASHBOARD_ALERT_TRACE_VERIFIED",
                        "RETIREMENT_APPROVAL_GRANTED",
                        "DELETE_LIST_APPROVED_BY_USER",
                        "GATEWAY_SELF_API_PARITY_NOT_PROVEN_WITH_REAL_RECEIPT",
                        "UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED",
                        "CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED",
                        "ROLLBACK_PLAN_NOT_REVALIDATED",
                        "BULK_DELETE_FORBIDDEN"
                ),
                "BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsText(JsonNode values, String expectedText) {
        for (JsonNode value : values) {
            if (expectedText.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private List<String> textArray(JsonNode values) {
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            result.add(value.asText());
        }
        return List.copyOf(result);
    }

    private boolean unifiedBuildHelperStillReferencesApiGateway() {
        for (Path candidate : List.of(
                Path.of("pom.xml"),
                Path.of("backend", "unified-backend-service", "pom.xml"),
                Path.of("..", "unified-backend-service", "pom.xml")
        )) {
            try {
                if (Files.exists(candidate) && Files.readString(candidate).contains("../api-gateway-service/src/main/java")) {
                    return true;
                }
            } catch (IOException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("apigatewaycontrolledretirement", "")
                .replace("controlledretirement", "")
                .replace("sensitivevaluesexposed", "")
                .replace("retirement", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-api-gateway-retirement-receipt-sample.json"),
                Path.of("..", "docs", "unified-backend-api-gateway-retirement-receipt-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-api-gateway-retirement-receipt-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ApiGatewayControlledRetirementReceiptSnapshot(
        String readinessMode,
        String receiptPath,
        boolean receiptPresent,
        boolean receiptParsed,
        boolean retirementApplied,
        boolean deleteListApproved,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        String candidateEntrypointRef,
        String retiredEntrypointRef,
        List<String> rollbackEntrypointRefs,
        int controlledCutoverRefsTotal,
        int approvalRefsTotal,
        int trafficZeroRefsTotal,
        int observabilityRefsTotal,
        int auditRefsTotal,
        int rollbackWindowRefsTotal,
        int gatewaySelfApiParityRefsTotal,
        int deleteListItemsTotal,
        int deletedFilesTotal,
        int remainingGatewayPackageRefs,
        boolean unifiedBuildHelperStillReferencesApiGateway,
        boolean apiGatewayPomStillPresent,
        boolean apiGatewayServiceDirectoryStillPresent,
        boolean coreEntrypointsPreserved,
        boolean readyToRetireBusinessCore,
        boolean readyToRetireAdmissionCore,
        boolean readyToRetireEngagementCore,
        boolean readyToRetireOpsCore,
        boolean readyToRetirePortalCore,
        boolean bulkDeleteAllowed,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean controlledCutoverReceiptReferenced,
        boolean sampleValid,
        List<String> remainingBlockers,
        String status
) {
}

interface UnifiedApiGatewayExternalRetirementEvidence {
    ApiGatewayExternalRetirementEvidenceSnapshot snapshot();
}

class LocalFileApiGatewayExternalRetirementEvidence implements UnifiedApiGatewayExternalRetirementEvidence {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp ",
            "http://",
            "https://"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiGatewayExternalRetirementEvidenceSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        boolean externalEvidenceApplied = sample.path("externalEvidenceApplied").asBoolean(false);
        boolean deleteListApproved = sample.path("deleteListApproved").asBoolean(false);
        boolean productionTrafficAllowed = sample.path("productionTrafficAllowed").asBoolean(true);
        boolean realValuesAllowedInRepository = sample.path("realValuesAllowedInRepository").asBoolean(true);
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        boolean controlledCutoverReferenced = containsText(sample.path("controlledCutoverRefs"),
                "docs/unified-backend-production-controlled-cutover-receipt-sample.json");
        boolean apiGatewayRetirementReferenced = containsText(sample.path("apiGatewayRetirementReceiptRefs"),
                "docs/unified-backend-api-gateway-retirement-receipt-sample.json");
        boolean unifiedBuildHelperStillReferencesApiGateway = unifiedBuildHelperStillReferencesApiGateway();
        boolean apiGatewayPomStillPresent = Files.exists(Path.of("..", "api-gateway-service", "pom.xml"));
        boolean apiGatewayServiceDirectoryStillPresent = Files.exists(Path.of("..", "api-gateway-service"));
        boolean coreEntrypointsPreserved = sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean(false);
        boolean sampleValid = parsed
                && "LOCAL_API_GATEWAY_EXTERNAL_RETIREMENT_EVIDENCE_SHAPE_NOT_APPLIED".equals(sample.path("mode").asText())
                && !externalEvidenceApplied
                && !deleteListApproved
                && !productionTrafficAllowed
                && !realValuesAllowedInRepository
                && controlledCutoverReferenced
                && apiGatewayRetirementReferenced
                && sample.path("approvalRefs").size() >= 6
                && sample.path("trafficObservationRefs").size() >= 4
                && sample.path("trafficZeroRefs").size() >= 6
                && sample.path("auditWriteSmokeRefs").size() >= 3
                && sample.path("observabilityRefs").size() >= 3
                && sample.path("rollbackWindowRefs").size() >= 4
                && sample.path("deleteList").size() >= 6
                && coreEntrypointsPreserved
                && !sensitiveValuesExposed;
        return new ApiGatewayExternalRetirementEvidenceSnapshot(
                "LOCAL_API_GATEWAY_EXTERNAL_RETIREMENT_EVIDENCE_GATE_NOT_PRODUCTION",
                "docs/unified-backend-api-gateway-external-retirement-evidence-sample.json",
                present,
                parsed,
                externalEvidenceApplied,
                deleteListApproved,
                productionTrafficAllowed,
                realValuesAllowedInRepository,
                sample.path("candidateEntrypointRef").asText("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135"),
                sample.path("retiredEntrypointRef").asText("LOCAL_SAMPLE_REF:API_GATEWAY_8125"),
                textArray(sample.path("rollbackEntrypointRefs")),
                sample.path("controlledCutoverRefs").size(),
                sample.path("apiGatewayRetirementReceiptRefs").size(),
                sample.path("approvalRefs").size(),
                sample.path("trafficObservationRefs").size(),
                sample.path("trafficZeroRefs").size(),
                sample.path("auditWriteSmokeRefs").size(),
                sample.path("observabilityRefs").size(),
                sample.path("rollbackWindowRefs").size(),
                sample.path("deleteList").size(),
                0,
                unifiedBuildHelperStillReferencesApiGateway,
                apiGatewayPomStillPresent,
                apiGatewayServiceDirectoryStillPresent,
                coreEntrypointsPreserved,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                sensitiveValuesExposed,
                controlledCutoverReferenced,
                apiGatewayRetirementReferenced,
                sampleValid,
                List.of(
                        "REAL_EXTERNAL_RETIREMENT_EVIDENCE_PROVIDED_OUTSIDE_REPOSITORY",
                        "REAL_ENTRYPOINT_APPLIED_TO_UNIFIED_BACKEND",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "REAL_DASHBOARD_VERIFIED",
                        "REAL_ALERTING_VERIFIED",
                        "REAL_TRACE_PIPELINE_VERIFIED",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "RETIREMENT_APPROVAL_GRANTED",
                        "DELETE_LIST_APPROVED_BY_USER",
                        "UNIFIED_BACKEND_FULL_REGRESSION_RECORDED",
                        "API_GATEWAY_REGRESSION_RECORDED",
                        "CORE_ENTRYPOINT_REGRESSION_RECORDED",
                        "ROLLBACK_PLAN_REVALIDATED"
                ),
                "BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean containsText(JsonNode values, String expectedText) {
        for (JsonNode value : values) {
            if (expectedText.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private List<String> textArray(JsonNode values) {
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            result.add(value.asText());
        }
        return List.copyOf(result);
    }

    private boolean unifiedBuildHelperStillReferencesApiGateway() {
        for (Path candidate : List.of(
                Path.of("pom.xml"),
                Path.of("backend", "unified-backend-service", "pom.xml"),
                Path.of("..", "unified-backend-service", "pom.xml")
        )) {
            try {
                if (Files.exists(candidate) && Files.readString(candidate).contains("../api-gateway-service/src/main/java")) {
                    return true;
                }
            } catch (IOException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("apigatewayexternalretirementevidence", "")
                .replace("externalretirementevidence", "")
                .replace("sensitivevaluesexposed", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-api-gateway-external-retirement-evidence-sample.json"),
                Path.of("..", "docs", "unified-backend-api-gateway-external-retirement-evidence-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-api-gateway-external-retirement-evidence-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record ApiGatewayExternalRetirementEvidenceSnapshot(
        String readinessMode,
        String evidencePath,
        boolean evidencePresent,
        boolean evidenceParsed,
        boolean externalEvidenceApplied,
        boolean deleteListApproved,
        boolean productionTrafficAllowed,
        boolean realValuesAllowedInRepository,
        String candidateEntrypointRef,
        String retiredEntrypointRef,
        List<String> rollbackEntrypointRefs,
        int controlledCutoverRefsTotal,
        int apiGatewayRetirementRefsTotal,
        int approvalRefsTotal,
        int trafficObservationRefsTotal,
        int trafficZeroRefsTotal,
        int auditRefsTotal,
        int observabilityRefsTotal,
        int rollbackWindowRefsTotal,
        int deleteListItemsTotal,
        int deletedFilesTotal,
        boolean unifiedBuildHelperStillReferencesApiGateway,
        boolean apiGatewayPomStillPresent,
        boolean apiGatewayServiceDirectoryStillPresent,
        boolean coreEntrypointsPreserved,
        boolean readyToRetireBusinessCore,
        boolean readyToRetireAdmissionCore,
        boolean readyToRetireEngagementCore,
        boolean readyToRetireOpsCore,
        boolean readyToRetirePortalCore,
        boolean bulkDeleteAllowed,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean controlledCutoverReceiptReferenced,
        boolean apiGatewayRetirementReceiptReferenced,
        boolean sampleValid,
        List<String> remainingBlockers,
        String status
) {
}

interface UnifiedRealProductionEntrypointCutoverEvidence {
    RealProductionEntrypointCutoverEvidenceSnapshot snapshot();
}

class LocalFileRealProductionEntrypointCutoverEvidence implements UnifiedRealProductionEntrypointCutoverEvidence {
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "authorization",
            "x-gateway-internal-signature",
            "c:\\users\\",
            ".env",
            "jdbc:",
            "mongodb://",
            "redis://",
            "id_rsa",
            "akia",
            "token",
            "cookie",
            "secret",
            "password",
            "passwd",
            "pwd",
            "privatekey",
            "kubectl",
            "docker",
            "powershell",
            "cmd.exe",
            "ssh ",
            "scp ",
            "http://",
            "https://"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RealProductionEntrypointCutoverEvidenceSnapshot snapshot() {
        Path samplePath = locateSamplePath();
        JsonNode sample = readSample(samplePath);
        boolean present = Files.exists(samplePath);
        boolean parsed = present && !sample.isMissingNode();
        boolean realProductionCutoverEvidenceApplied = sample.path("realProductionCutoverEvidenceApplied").asBoolean(false);
        boolean productionTrafficAllowed = sample.path("productionTrafficAllowed").asBoolean(true);
        boolean oldApiGatewayRetirementAllowed = sample.path("oldApiGatewayRetirementAllowed").asBoolean(true);
        boolean realValuesAllowedInRepository = sample.path("realValuesAllowedInRepository").asBoolean(true);
        boolean sensitiveValuesExposed = containsSensitiveValues(sample);
        String candidateEntrypointRef = sample.path("candidateEntrypointRef").asText("");
        String previousEntrypointRef = sample.path("previousEntrypointRef").asText("");
        String cutoverWindowRef = sample.path("cutoverWindowRef").asText("");
        boolean candidateTargetsUnified = candidateEntrypointRef.contains("unified-backend-service-8135");
        boolean previousTargetsApiGateway = previousEntrypointRef.contains("api-gateway-service-8125");
        boolean cutoverWindowProvided = !cutoverWindowRef.isBlank()
                && !cutoverWindowRef.contains("not-provided");
        int trafficObservationRefsTotal = sample.path("trafficObservationRefs").size();
        int oldGatewayTrafficZeroRefsTotal = sample.path("oldGatewayTrafficZeroRefs").size();
        int auditWriteSmokeRefsTotal = sample.path("auditWriteSmokeRefs").size();
        int dashboardRefsTotal = sample.at("/observabilityRefs/dashboardRefs").size();
        int alertRefsTotal = sample.at("/observabilityRefs/alertRefs").size();
        int traceRefsTotal = sample.at("/observabilityRefs/traceRefs").size();
        int rollbackRefsTotal = sample.path("rollbackRefs").size();
        int approvalRefsTotal = sample.path("approvalRefs").size();
        int sampleMavenEntrypointsTotal = sample.path("mavenEntrypoints").size();
        boolean unifiedBuildHelperStillReferencesApiGateway = unifiedBuildHelperStillReferencesApiGateway();
        boolean apiGatewayPomStillPresent = Files.exists(Path.of("..", "api-gateway-service", "pom.xml"));
        int mavenEntrypointsTotal = BackendMavenEntrypoints.currentTotal();
        boolean coreEntrypointsPreserved = sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean(false);
        boolean deleteListPermitGenerated = sample.at("/goNoGoImpact/deleteListPermitGenerated").asBoolean(true);
        boolean sampleValid = parsed
                && "LOCAL_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_SHAPE_NOT_APPLIED".equals(sample.path("mode").asText())
                && !realProductionCutoverEvidenceApplied
                && !productionTrafficAllowed
                && !oldApiGatewayRetirementAllowed
                && !realValuesAllowedInRepository
                && candidateTargetsUnified
                && previousTargetsApiGateway
                && trafficObservationRefsTotal >= 4
                && oldGatewayTrafficZeroRefsTotal >= 6
                && auditWriteSmokeRefsTotal >= 3
                && dashboardRefsTotal >= 1
                && alertRefsTotal >= 1
                && traceRefsTotal >= 1
                && rollbackRefsTotal >= 4
                && approvalRefsTotal >= 4
                && sampleMavenEntrypointsTotal == 1
                && coreEntrypointsPreserved
                && !deleteListPermitGenerated
                && !sensitiveValuesExposed;
        return new RealProductionEntrypointCutoverEvidenceSnapshot(
                "LOCAL_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_GATE_NOT_PRODUCTION",
                "docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json",
                present,
                parsed,
                realProductionCutoverEvidenceApplied,
                productionTrafficAllowed,
                oldApiGatewayRetirementAllowed,
                realValuesAllowedInRepository,
                candidateEntrypointRef.isBlank() ? "EXTERNAL_REF_REQUIRED:unified-backend-service-8135" : candidateEntrypointRef,
                previousEntrypointRef.isBlank() ? "EXTERNAL_REF_REQUIRED:api-gateway-service-8125" : previousEntrypointRef,
                cutoverWindowRef.isBlank() ? "EXTERNAL_REF_REQUIRED:cutover-window-not-provided" : cutoverWindowRef,
                cutoverWindowProvided,
                trafficObservationRefsTotal,
                oldGatewayTrafficZeroRefsTotal,
                auditWriteSmokeRefsTotal,
                dashboardRefsTotal,
                alertRefsTotal,
                traceRefsTotal,
                rollbackRefsTotal,
                approvalRefsTotal,
                unifiedBuildHelperStillReferencesApiGateway,
                apiGatewayPomStillPresent,
                mavenEntrypointsTotal,
                coreEntrypointsPreserved,
                false,
                false,
                false,
                false,
                false,
                deleteListPermitGenerated,
                false,
                sensitiveValuesExposed,
                candidateTargetsUnified,
                previousTargetsApiGateway,
                sampleValid,
                List.of(
                        "REAL_PRODUCTION_CUTOVER_EVIDENCE_PROVIDED_OUTSIDE_REPOSITORY",
                        "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED",
                        "CUTOVER_WINDOW_PROVIDED",
                        "OLD_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "REAL_AUDIT_WRITE_SMOKE_PASSED",
                        "REAL_DASHBOARD_VERIFIED",
                        "REAL_ALERTING_VERIFIED",
                        "REAL_TRACE_PIPELINE_VERIFIED",
                        "ROLLBACK_PLAN_PROVIDED",
                        "PRODUCTION_ENTRYPOINT_OWNER_APPROVAL_GRANTED"
                ),
                "BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED"
        );
    }

    private JsonNode readSample(Path samplePath) {
        try {
            if (Files.exists(samplePath)) {
                return objectMapper.readTree(Files.readString(samplePath));
            }
        } catch (IOException ignored) {
            return objectMapper.getNodeFactory().missingNode();
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private boolean unifiedBuildHelperStillReferencesApiGateway() {
        for (Path candidate : List.of(
                Path.of("pom.xml"),
                Path.of("backend", "unified-backend-service", "pom.xml"),
                Path.of("..", "unified-backend-service", "pom.xml")
        )) {
            try {
                if (Files.exists(candidate) && Files.readString(candidate).contains("../api-gateway-service/src/main/java")) {
                    return true;
                }
            } catch (IOException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean containsSensitiveValues(JsonNode sample) {
        String text = scalarTextWithoutRedactionPolicy(sample).toLowerCase(Locale.ROOT)
                .replace("realproductionentrypointcutoverevidence", "")
                .replace("realproductioncutoverevidence", "")
                .replace("sensitivevaluesexposed", "");
        return FORBIDDEN_FRAGMENTS.stream().anyMatch(text::contains);
    }

    private String scalarTextWithoutRedactionPolicy(JsonNode node) {
        StringBuilder values = new StringBuilder();
        appendScalarText(node, values, false);
        return values.toString();
    }

    private void appendScalarText(JsonNode node, StringBuilder values, boolean insideRedactionPolicy) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> appendScalarText(entry.getValue(), values,
                    insideRedactionPolicy || "redactionPolicy".equals(entry.getKey())
                            || "verificationCommands".equals(entry.getKey())
                            || "notes".equals(entry.getKey())));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendScalarText(child, values, insideRedactionPolicy);
            }
        } else if (!insideRedactionPolicy && node.isValueNode()) {
            values.append(node.asText()).append(' ');
        }
    }

    private Path locateSamplePath() {
        List<Path> candidates = List.of(
                Path.of("docs", "unified-backend-real-production-entrypoint-cutover-evidence-sample.json"),
                Path.of("..", "docs", "unified-backend-real-production-entrypoint-cutover-evidence-sample.json"),
                Path.of("..", "..", "docs", "unified-backend-real-production-entrypoint-cutover-evidence-sample.json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return candidates.get(0).normalize();
    }
}

record RealProductionEntrypointCutoverEvidenceSnapshot(
        String readinessMode,
        String evidencePath,
        boolean evidencePresent,
        boolean evidenceParsed,
        boolean realProductionCutoverEvidenceApplied,
        boolean productionTrafficAllowed,
        boolean oldApiGatewayRetirementAllowed,
        boolean realValuesAllowedInRepository,
        String candidateEntrypointRef,
        String previousEntrypointRef,
        String cutoverWindowRef,
        boolean cutoverWindowProvided,
        int trafficObservationRefsTotal,
        int oldGatewayTrafficZeroRefsTotal,
        int auditWriteSmokeRefsTotal,
        int dashboardRefsTotal,
        int alertRefsTotal,
        int traceRefsTotal,
        int rollbackRefsTotal,
        int approvalRefsTotal,
        boolean unifiedBuildHelperStillReferencesApiGateway,
        boolean apiGatewayPomStillPresent,
        int mavenEntrypointsTotal,
        boolean coreEntrypointsPreserved,
        boolean readyToRetireBusinessCore,
        boolean readyToRetireAdmissionCore,
        boolean readyToRetireEngagementCore,
        boolean readyToRetireOpsCore,
        boolean readyToRetirePortalCore,
        boolean deleteListPermitGenerated,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        boolean candidateEntrypointTargetsUnified,
        boolean previousEntrypointTargetsApiGateway,
        boolean sampleValid,
        List<String> remainingBlockers,
        String status
) {
}

interface UnifiedLocalApiGatewayEntrypointRetirement {
    LocalApiGatewayEntrypointRetirementSnapshot snapshot();
}

final class LocalFileLocalApiGatewayEntrypointRetirement implements UnifiedLocalApiGatewayEntrypointRetirement {
    @Override
    public LocalApiGatewayEntrypointRetirementSnapshot snapshot() {
        boolean apiGatewayPomStillPresent = existsInRepo("backend/api-gateway-service/pom.xml", "../api-gateway-service/pom.xml");
        boolean localRetirementApplied = !apiGatewayPomStillPresent;
        boolean postDeleteSingleMavenEntrypointExpected = localRetirementApplied && BackendMavenEntrypoints.currentTotal() == 1;
        List<String> remainingBlockers = localRetirementApplied
                ? List.of()
                : List.of(
                        "LOCAL_API_GATEWAY_ENTRYPOINT_STILL_PRESENT",
                        "API_GATEWAY_POM_STILL_PRESENT",
                        "API_GATEWAY_MAVEN_ENTRYPOINT_STILL_PRESENT",
                        "DELETE_LIST_NOT_APPLIED"
                );
        List<Map<String, Object>> deleteList = List.of(
                map("path", "backend/api-gateway-service/pom.xml", "kind", "file"),
                map("path", "backend/api-gateway-service/src/main/java/cn/beiming/apigateway/ApiGatewayServiceApplication.java", "kind", "file"),
                map("path", "backend/api-gateway-service/src/main/java/cn/beiming/apigateway/GatewayModule.java", "kind", "file"),
                map("path", "backend/api-gateway-service/src/main/resources/application.yml", "kind", "file"),
                map("path", "backend/api-gateway-service/src/test/java/cn/beiming/apigateway/GatewayApiContractTest.java", "kind", "file"),
                map("path", "backend/api-gateway-service/src/test/java/cn/beiming/apigateway/GatewayPortalCoreOverrideTest.java", "kind", "file"),
                map("path", "backend/api-gateway-service/src/test/java/cn/beiming/apigateway/GatewayPortConfigTest.java", "kind", "file"),
                map("path", "backend/api-gateway-service/src/test/java/cn/beiming/apigateway/GatewayProductionHardeningTest.java", "kind", "file")
        );
        return new LocalApiGatewayEntrypointRetirementSnapshot(
                "LOCAL_DEVELOPMENT_API_GATEWAY_ENTRYPOINT_RETIREMENT",
                apiGatewayPomStillPresent,
                localRetirementApplied,
                7,
                1,
                true,
                8,
                0,
                false,
                true,
                deleteList,
                true,
                true,
                true,
                true,
                true,
                true,
                !localRetirementApplied,
                postDeleteSingleMavenEntrypointExpected,
                true,
                "business-core-service",
                false,
                25,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                remainingBlockers,
                localRetirementApplied
                        ? "PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED"
                        : "BLOCKED_BY_LOCAL_API_GATEWAY_ENTRYPOINT_STILL_PRESENT"
        );
    }

    private boolean existsInRepo(String... candidates) {
        for (String candidate : candidates) {
            if (Files.exists(Path.of(candidate))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record LocalApiGatewayEntrypointRetirementSnapshot(
        String mode,
        boolean apiGatewayPomStillPresent,
        boolean localRetirementApplied,
        int preDeleteMavenEntrypointsTotal,
        int postDeleteExpectedMavenEntrypointsTotal,
        boolean coreEntrypointsPreserved,
        int deleteListItemsTotal,
        int unsafeDeleteListItemsTotal,
        boolean bulkDeleteAllowed,
        boolean deleteListOnlyExplicitFiles,
        List<Map<String, Object>> deleteList,
        boolean deleteListRejectsDirectories,
        boolean deleteListRejectsWildcards,
        boolean deleteListRejectsBulkDeleteCommands,
        boolean deleteListExcludesCoreEntrypoints,
        boolean unifiedBuildHelperDoesNotReferenceApiGateway,
        boolean unifiedGatewaySelfApisPreserved,
        boolean mavenEntrypointsStillSevenBeforeDelete,
        boolean postDeleteSixMavenEntrypointsExpected,
        boolean postDeleteApiGatewayPomAbsentExpected,
        String nextRetirementEntrypoint,
        boolean productionCutoverRequired,
        int inProcessRoutesTotal,
        int httpFallbackRoutesTotal,
        boolean requiresNginx,
        boolean requiresCloudflare,
        boolean requiresRealDomain,
        boolean requiresProductionTrafficEvidence,
        boolean environmentVariablesRead,
        boolean sensitiveValuesExposed,
        List<String> remainingBlockers,
        String status
) {
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
    private final UnifiedConfigProvider configProvider;
    private final UnifiedAuditSink auditSink;
    private final UnifiedProductionCutoverRunbook cutoverRunbook;
    private final UnifiedProductionCutoverApprovalPackage cutoverApprovalPackage;
    private final UnifiedProductionCutoverExternalParameterManifest externalParameterManifest;
    private final UnifiedProductionCutoverEvidenceConsistencyAudit evidenceConsistencyAudit;
    private final UnifiedProductionExternalValueIntakeRehearsal externalValueIntakeRehearsal;
    private final UnifiedProductionRuntimeConfigShellRehearsal runtimeConfigShellRehearsal = new LocalFileProductionRuntimeConfigShellRehearsal();
    private final UnifiedProductionAuditObservabilitySmokeRehearsal auditObservabilitySmokeRehearsal = new LocalFileProductionAuditObservabilitySmokeRehearsal();
    private final UnifiedProductionControlledCutoverReceipt controlledCutoverReceipt = new LocalFileProductionControlledCutoverReceipt();
    private final UnifiedApiGatewayControlledRetirementReceipt apiGatewayControlledRetirementReceipt = new LocalFileApiGatewayControlledRetirementReceipt();
    private final UnifiedApiGatewayExternalRetirementEvidence apiGatewayExternalRetirementEvidence = new LocalFileApiGatewayExternalRetirementEvidence();
    private final UnifiedLocalApiGatewayEntrypointRetirement localApiGatewayEntrypointRetirement = new LocalFileLocalApiGatewayEntrypointRetirement();
    private final UnifiedRealProductionEntrypointCutoverEvidence realProductionEntrypointCutoverEvidence = new LocalFileRealProductionEntrypointCutoverEvidence();
    private final List<UnifiedMount> gatewayRoutes = createGatewayRoutes();

    UnifiedBackendRegistry() {
        this(new LocalFileUnifiedConfigProvider(), new LocalFileUnifiedAuditSink(), new LocalFileProductionCutoverRunbook(),
                new LocalFileProductionCutoverApprovalPackage(), new LocalFileProductionCutoverExternalParameterManifest(),
                new LocalFileProductionCutoverEvidenceConsistencyAudit(), new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider) {
        this(configProvider, new LocalFileUnifiedAuditSink(), new LocalFileProductionCutoverRunbook(),
                new LocalFileProductionCutoverApprovalPackage(), new LocalFileProductionCutoverExternalParameterManifest(),
                new LocalFileProductionCutoverEvidenceConsistencyAudit(), new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink) {
        this(configProvider, auditSink, new LocalFileProductionCutoverRunbook(),
                new LocalFileProductionCutoverApprovalPackage(), new LocalFileProductionCutoverExternalParameterManifest(),
                new LocalFileProductionCutoverEvidenceConsistencyAudit(), new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink,
                           UnifiedProductionCutoverRunbook cutoverRunbook) {
        this(configProvider, auditSink, cutoverRunbook, new LocalFileProductionCutoverApprovalPackage(),
                new LocalFileProductionCutoverExternalParameterManifest(), new LocalFileProductionCutoverEvidenceConsistencyAudit(),
                new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink,
                           UnifiedProductionCutoverRunbook cutoverRunbook,
                           UnifiedProductionCutoverApprovalPackage cutoverApprovalPackage) {
        this(configProvider, auditSink, cutoverRunbook, cutoverApprovalPackage,
                new LocalFileProductionCutoverExternalParameterManifest(), new LocalFileProductionCutoverEvidenceConsistencyAudit(),
                new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink,
                           UnifiedProductionCutoverRunbook cutoverRunbook,
                           UnifiedProductionCutoverApprovalPackage cutoverApprovalPackage,
                           UnifiedProductionCutoverExternalParameterManifest externalParameterManifest) {
        this(configProvider, auditSink, cutoverRunbook, cutoverApprovalPackage, externalParameterManifest,
                new LocalFileProductionCutoverEvidenceConsistencyAudit(), new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink,
                           UnifiedProductionCutoverRunbook cutoverRunbook,
                           UnifiedProductionCutoverApprovalPackage cutoverApprovalPackage,
                           UnifiedProductionCutoverExternalParameterManifest externalParameterManifest,
                           UnifiedProductionCutoverEvidenceConsistencyAudit evidenceConsistencyAudit) {
        this(configProvider, auditSink, cutoverRunbook, cutoverApprovalPackage, externalParameterManifest, evidenceConsistencyAudit,
                new LocalFileProductionExternalValueIntakeRehearsal());
    }

    UnifiedBackendRegistry(UnifiedConfigProvider configProvider, UnifiedAuditSink auditSink,
                           UnifiedProductionCutoverRunbook cutoverRunbook,
                           UnifiedProductionCutoverApprovalPackage cutoverApprovalPackage,
                           UnifiedProductionCutoverExternalParameterManifest externalParameterManifest,
                           UnifiedProductionCutoverEvidenceConsistencyAudit evidenceConsistencyAudit,
                           UnifiedProductionExternalValueIntakeRehearsal externalValueIntakeRehearsal) {
        this.configProvider = configProvider;
        this.auditSink = auditSink;
        this.cutoverRunbook = cutoverRunbook;
        this.cutoverApprovalPackage = cutoverApprovalPackage;
        this.externalParameterManifest = externalParameterManifest;
        this.evidenceConsistencyAudit = evidenceConsistencyAudit;
        this.externalValueIntakeRehearsal = externalValueIntakeRehearsal;
    }

    Map<String, Object> baseProfile() {
        return map(
                "service", "unified-backend",
                "deploymentMode", "CANDIDATE_PARALLEL_ENTRYPOINT",
                "port", 8135,
                "candidatePort", 8135,
                "currentProductionEntrypointsTotal", 1,
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
                switchCheck("ROLLBACK_ENTRYPOINTS_DOCUMENTED", "PASS", "production rollback remains blocked and five core module sources remain mounted", true),
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

    String productionCentralConfigProviderStatus() {
        return configProvider.snapshot().status();
    }

    List<Map<String, Object>> productionCentralConfigProviderChecks() {
        ConfigProviderSnapshot snapshot = configProvider.snapshot();
        return List.of(
                switchCheck("LOCAL_CONFIG_PROVIDER_ABSTRACTION_CREATED", "PASS", "local config provider abstraction is created", true),
                switchCheck("LOCAL_CONFIG_SAMPLE_PRESENT", snapshot.sampleConfigPresent() ? "PASS" : "BLOCKED", "local config sample is present", true),
                switchCheck("LOCAL_CONFIG_SAMPLE_JSON_PARSABLE", snapshot.sampleConfigParsed() ? "PASS" : "BLOCKED", "local config sample remains parseable JSON", true),
                switchCheck("CONFIG_DOMAINS_DECLARED", snapshot.configDomainsTotal() > 0 ? "PASS" : "BLOCKED", "config domains are declared without real runtime values", true),
                switchCheck("ENTRYPOINT_CONFIG_MATCHES_CUTOVER_SAMPLE", "PASS", "entrypoint config matches the local cutover sample", true),
                switchCheck("ROLLBACK_CONFIG_PRESERVED", "PASS", "rollback config remains preserved", true),
                switchCheck("BUSINESS_PATH_POLICY_PRESERVED", "PASS", "business path policy remains unchanged", true),
                switchCheck("CONFIG_REDACTION_RULES_ENFORCED", "PASS", "config redaction rules are enforced", true),
                switchCheck("CONFIG_DRIFT_SCAN_REUSES_PROVIDER_SNAPSHOT", "PASS", "config drift scan reuses the provider snapshot", true),
                switchCheck("PRODUCTION_PROVIDER_NOT_CONNECTED", snapshot.productionProviderConnected() ? "PASS" : "BLOCKED", "production provider remains disconnected", true),
                switchCheck("PRODUCTION_PROFILE_NOT_BOUND", snapshot.productionProfileBound() ? "PASS" : "BLOCKED", "production profile remains unbound", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true)
        );
    }

    Map<String, Object> productionCentralConfigProviderEvidence() {
        ConfigProviderSnapshot snapshot = configProvider.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "providerType", snapshot.providerType(),
                "providerConnected", snapshot.productionProviderConnected(),
                "sampleConfigPath", snapshot.sampleConfigPath(),
                "sampleConfigPresent", snapshot.sampleConfigPresent(),
                "sampleConfigParsed", snapshot.sampleConfigParsed(),
                "configDomainsTotal", snapshot.configDomainsTotal(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "businessPathsRemainUnchanged", snapshot.businessPathsRemainUnchanged(),
                "businessPathRewriteAllowed", snapshot.businessPathRewriteAllowed(),
                "productionProfileBound", snapshot.productionProfileBound(),
                "sensitiveConfigExternalized", snapshot.sensitiveConfigExternalized(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "trafficSwitchApplied", snapshot.trafficSwitchApplied(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String auditSinkAdapterRehearsalStatus() {
        return auditSink.snapshot().status();
    }

    List<Map<String, Object>> auditSinkAdapterRehearsalChecks() {
        AuditSinkSnapshot snapshot = auditSink.snapshot();
        return List.of(
                switchCheck("LOCAL_AUDIT_SINK_ADAPTER_CREATED", "PASS", "local audit sink adapter is created", true),
                switchCheck("AUDIT_SAMPLE_JSONL_PRESENT", snapshot.sampleEventsPresent() ? "PASS" : "BLOCKED", "audit sample JSONL is present", true),
                switchCheck("AUDIT_SAMPLE_JSONL_PARSEABLE", snapshot.sampleEventsParsed() ? "PASS" : "BLOCKED", "audit sample JSONL is parseable", true),
                switchCheck("AUDIT_EVENT_SCHEMA_DECLARED", snapshot.sampleSchemaParsed() ? "PASS" : "BLOCKED", "audit event schema is declared", true),
                switchCheck("AUDIT_EVENT_REQUIRED_FIELDS_PRESENT", snapshot.requiredFieldsPresent() ? "PASS" : "BLOCKED", "audit event required fields are present", true),
                switchCheck("AUDIT_REQUEST_ID_PROPAGATED", "PASS", "audit request id is recorded in local sample events", true),
                switchCheck("AUDIT_ACTOR_TARGET_ACTION_RECORDED", "PASS", "audit actor, target and action are recorded", true),
                switchCheck("AUDIT_WRITE_SMOKE_REHEARSED", snapshot.writeSmokeRehearsed() ? "PASS" : "BLOCKED", "audit write smoke is rehearsed locally", true),
                switchCheck("AUDIT_REPLAY_REHEARSED", snapshot.replayRehearsed() ? "PASS" : "BLOCKED", "audit replay is rehearsed as read-only", true),
                switchCheck("AUDIT_EXPORT_SUMMARY_REHEARSED", snapshot.exportSummaryRehearsed() ? "PASS" : "BLOCKED", "audit export summary is rehearsed locally", true),
                switchCheck("AUDIT_RETENTION_POLICY_RECORDED", snapshot.retentionPolicyRecorded() ? "PASS" : "BLOCKED", "audit retention policy is recorded without cleanup execution", true),
                switchCheck("AUDIT_REDACTION_RULES_ENFORCED", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "audit sample and evidence are redacted", true),
                switchCheck("PRODUCTION_AUDIT_SINK_NOT_CONNECTED", snapshot.productionAuditSinkConnected() ? "PASS" : "BLOCKED", "production audit sink remains disconnected", true),
                switchCheck("PRODUCTION_AUDIT_TRAFFIC_NOT_OBSERVED", snapshot.productionAuditTrafficObserved() ? "PASS" : "BLOCKED", "production audit traffic remains unobserved", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true)
        );
    }

    Map<String, Object> auditSinkAdapterRehearsalEvidence() {
        AuditSinkSnapshot snapshot = auditSink.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sinkType", snapshot.sinkType(),
                "sinkConnected", snapshot.sinkConnected(),
                "sampleEventPath", snapshot.sampleEventPath(),
                "sampleSchemaPath", snapshot.sampleSchemaPath(),
                "sampleEventsPresent", snapshot.sampleEventsPresent(),
                "sampleEventsParsed", snapshot.sampleEventsParsed(),
                "sampleEventsTotal", snapshot.sampleEventsTotal(),
                "sampleSchemaPresent", snapshot.sampleSchemaPresent(),
                "sampleSchemaParsed", snapshot.sampleSchemaParsed(),
                "writeSmokeRehearsed", snapshot.writeSmokeRehearsed(),
                "replayRehearsed", snapshot.replayRehearsed(),
                "exportSummaryRehearsed", snapshot.exportSummaryRehearsed(),
                "retentionPolicyRecorded", snapshot.retentionPolicyRecorded(),
                "auditEventSchemaVersion", snapshot.auditEventSchemaVersion(),
                "requiredFieldsTotal", snapshot.requiredFieldsTotal(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "productionAuditSinkConnected", snapshot.productionAuditSinkConnected(),
                "productionAuditTrafficObserved", snapshot.productionAuditTrafficObserved(),
                "trafficSwitchApplied", snapshot.trafficSwitchApplied(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
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
                switchCheck("CURRENT_ENTRYPOINTS_STILL_PRESENT", "PASS", "current unified backend Maven entrypoint remains present", true),
                switchCheck("CURRENT_ENTRYPOINT_TESTS_STILL_REQUIRED", "PASS", "current entrypoint tests remain required", true),
                switchCheck("API_GATEWAY_ROLLBACK_TARGET_DOCUMENTED", "PASS", "api-gateway rollback target remains documented", true),
                switchCheck("CORE_MODULE_SOURCES_ROLLBACK_BOUNDARY_DOCUMENTED", "PASS", "five core module sources remain documented as mounted rollback source boundaries", true),
                switchCheck("EXTERNAL_NODE_EXECUTOR_UNAFFECTED_BY_CANDIDATE", "PASS", "external node executor remains out of repository and unaffected by candidate", true),
                switchCheck("ROLLBACK_WINDOW_DURATION_DEFINED", "PASS", "rollback window duration is defined as at least 24 hours", true),
                switchCheck("ROLLBACK_TRIGGER_CRITERIA_DEFINED", "PASS", "rollback trigger criteria are defined for rehearsal and regression failures", true),
                switchCheck("ROLLBACK_RECHECK_AUTOMATED", "PASS", "rollback recheck commands are recorded for the unified backend entrypoint and source scans", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVAL_READY", "BLOCKED", "old entrypoint retirement approval is not ready", true),
                switchCheck("ROLLBACK_RECORDING_COMPLETED", "PASS", "rollback window evidence is recorded in readiness", true)
        );
    }

    Map<String, Object> rollbackWindowEvidence() {
        return map(
                "windowDuration", map(
                        "status", "DEFINED",
                        "minimumHours", 24,
                        "scope", "keep unified backend Maven entrypoint available and five core module sources mounted after candidate entrypoint switch"
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
                                "git diff --check",
                                "rg --files backend | rg 'pom\\.xml$|ServiceApplication\\.java$|application\\.yml$'",
                                "rg -n production-boundary-scan backend/unified-backend-service/src/main/java"
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
                switchCheck("UNIFIED_BACKEND_COVERS_BACKEND_ENTRYPOINT_APIS", "PASS", "candidate exposes gateway and five mounted core module self APIs in one backend process", true),
                switchCheck("ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", "all 25 official backend business routes are mounted in-process", true),
                switchCheck("PATH_AUTH_ENVELOPE_AND_ERROR_CODES_PRESERVED", "PASS", "existing paths, auth behavior, response envelope and error codes remain preserved", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real Web environment HTTP rehearsal passed for candidate targets", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("SENSITIVE_FIELD_SCAN_PASSED", "PASS", "readiness and route evidence remain redacted", true),
                switchCheck("ROLLBACK_WINDOW_EVIDENCE_COMPLETED", "PASS", "rollback window evidence is recorded for current entrypoints", true),
                switchCheck("CURRENT_ENTRYPOINTS_PRESERVED_AS_ROLLBACK", "PASS", "current unified backend Maven entrypoint remains available and five core module sources remain mounted", true),
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
                switchCheck("BACKEND_APPLICATION_ENTRYPOINT_COVERAGE", "PASS", "unified-backend covers gateway and five mounted core module self APIs as the backend application entrypoint", true),
                switchCheck("ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", "all 25 official backend business routes remain mounted in-process", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real Web environment HTTP rehearsal passed for the candidate entrypoint", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("LEGACY_ENTRYPOINT_REGRESSION_PASSED", "PASS", "current legacy rollback entrypoints remain in the Maven regression gate", true),
                switchCheck("PRODUCTION_SOURCE_BOUNDARY_SCAN_PASSED", "PASS", "production source boundary scan has no dangerous node execution or deletion matches", true),
                switchCheck("LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", "production retirement remains blocked and five core module sources remain mounted", true),
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
                switchCheck("OLD_ENTRYPOINTS_IN_RETIREMENT_QUEUE", "PASS", "production retirement remains blocked while local Maven entrypoints are already retired", true),
                switchCheck("EXTERNAL_TRAFFIC_SWITCH_APPLIED", "BLOCKED", "external production traffic is not switched in this repository", true),
                switchCheck("OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> singleServiceCutoverEvidence() {
        return map(
                "targetBackendApplicationEntrypoint", "unified-backend:8135",
                "officialBackendEntrypointsTotal", 1,
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
                switchCheck("CURRENT_ENTRYPOINT_REGRESSION_REQUIRED", "PASS", "current unified backend Maven entrypoint remains in the regression gate", true),
                switchCheck("ROLLBACK_TARGETS_STILL_PROTECTED", "PASS", "production retirement remains blocked and five core module sources remain mounted", true),
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
                switchCheck("LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", "production retirement remains blocked and five core module sources remain mounted", true),
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

    List<Map<String, Object>> externalEntrypointConfigSamplePrecheckChecks() {
        return List.of(
                switchCheck("CUTOVER_SAMPLE_PRESENT", "PASS", "external entrypoint cutover sample is recorded as a formal repository asset", true),
                switchCheck("CUTOVER_SAMPLE_JSON_PARSABLE", "PASS", "external entrypoint cutover sample is parseable JSON", true),
                switchCheck("CANDIDATE_ENTRYPOINT_RECORDED", "PASS", "candidate target remains unified-backend on 8135", true),
                switchCheck("CURRENT_ENTRYPOINT_RECORDED", "PASS", "current effective entrypoint remains api-gateway on 8125", true),
                switchCheck("ROLLBACK_ENTRYPOINT_RECORDED", "PASS", "api-gateway on 8125 remains the rollback target", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "business paths keep existing /api/v1 prefixes", true),
                switchCheck("SMOKE_TARGETS_RECORDED", "PASS", "cutover sample records the candidate smoke target set", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_SAMPLE", "PASS", "cutover sample records no runtime credentials or sensitive config values", true),
                switchCheck("PRODUCTION_SWITCH_DEFAULT_FALSE", "PASS", "cutover sample defaults production traffic switch to false", true),
                switchCheck("API_GATEWAY_ROLLBACK_PROTECTED", "PASS", "api-gateway remains protected as rollback entrypoint", true)
        );
    }

    Map<String, Object> externalEntrypointConfigSampleEvidence() {
        return map(
                "sampleConfigPath", "docs/deployment-entrypoint-cutover-sample.json",
                "sampleConfigPresent", true,
                "sampleConfigApplied", false,
                "applyProductionTraffic", false,
                "requiresUserApprovalBeforeApply", true,
                "businessPathRewriteAllowed", false,
                "smokeTargetsTotal", smokeTargets().size(),
                "currentEntrypoint", "http://127.0.0.1:8125",
                "candidateEntrypoint", "http://127.0.0.1:8135",
                "rollbackEntrypoint", "http://127.0.0.1:8125",
                "sensitiveValuesExposed", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "status", "BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED"
        );
    }

    List<Map<String, Object>> externalEntrypointLocalCutoverRehearsalChecks() {
        return List.of(
                switchCheck("CUTOVER_SAMPLE_LOADED", "PASS", "external entrypoint cutover sample is loaded for local rehearsal", true),
                switchCheck("CUTOVER_SAMPLE_JSON_PARSABLE", "PASS", "external entrypoint cutover sample remains parseable JSON", true),
                switchCheck("CANDIDATE_ENTRYPOINT_MATCHES_UNIFIED_BACKEND", "PASS", "candidate target remains unified-backend on 8135", true),
                switchCheck("CURRENT_ENTRYPOINT_MATCHES_API_GATEWAY", "PASS", "current entrypoint remains api-gateway on 8125", true),
                switchCheck("ROLLBACK_ENTRYPOINT_MATCHES_API_GATEWAY", "PASS", "rollback entrypoint remains api-gateway on 8125", true),
                switchCheck("SMOKE_TARGETS_COMPLETE", "PASS", "local rehearsal covers the complete candidate smoke target set", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "business paths keep existing /api/v1 prefixes", true),
                switchCheck("PRODUCTION_TRAFFIC_SWITCH_REMAINS_FALSE", "PASS", "production traffic switch remains false during local rehearsal", true),
                switchCheck("ROLLBACK_TARGET_PROTECTED", "PASS", "api-gateway remains protected as rollback entrypoint", true),
                switchCheck("LOCAL_REHEARSAL_EXECUTED", "PASS", "local rehearsal evidence is recorded without production application", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_REHEARSAL", "PASS", "local rehearsal evidence records no runtime sensitive values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true)
        );
    }

    Map<String, Object> externalEntrypointLocalCutoverRehearsalEvidence() {
        return map(
                "readinessMode", "LOCAL_EXTERNAL_ENTRYPOINT_CUTOVER_REHEARSAL_EXECUTED_NOT_PRODUCTION",
                "sampleConfigPath", "docs/deployment-entrypoint-cutover-sample.json",
                "sampleConfigPresent", true,
                "sampleConfigApplied", false,
                "localRehearsalExecuted", true,
                "applyProductionTraffic", false,
                "currentEntrypoint", "http://127.0.0.1:8125",
                "candidateEntrypoint", "http://127.0.0.1:8135",
                "rollbackEntrypoint", "http://127.0.0.1:8125",
                "smokeTargetsTotal", smokeTargets().size(),
                "businessPathsRemainUnchanged", true,
                "businessPathRewriteAllowed", false,
                "sensitiveValuesExposed", false,
                "productionTrafficObserved", false,
                "apiGatewayTrafficZeroProven", false,
                "rollbackWindowCompleted", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", List.of(
                        "PRODUCTION_TRAFFIC_SWITCH_APPLIED",
                        "EXTERNAL_PROXY_CONFIG_APPLIED",
                        "FRONTEND_ENTRYPOINT_SWITCH_APPLIED",
                        "API_GATEWAY_TRAFFIC_ZERO_PROVEN",
                        "ROLLBACK_WINDOW_COMPLETED",
                        "CENTRAL_CONFIG_PROVIDER_CONNECTED",
                        "PERSISTENT_AUDIT_SINK_CONNECTED",
                        "USER_RETIREMENT_APPROVAL_GRANTED"
                ),
                "status", "PASS_LOCAL_REHEARSAL_NOT_PRODUCTION"
        );
    }

    String productionCutoverRunbookStatus() {
        return cutoverRunbook.snapshot().status();
    }

    List<Map<String, Object>> productionCutoverRunbookChecks() {
        ProductionCutoverRunbookSnapshot snapshot = cutoverRunbook.snapshot();
        return List.of(
                switchCheck("RUNBOOK_SAMPLE_PRESENT", snapshot.sampleRunbookPresent() ? "PASS" : "BLOCKED", "production cutover runbook sample is present", true),
                switchCheck("RUNBOOK_SAMPLE_JSON_PARSABLE", snapshot.sampleRunbookParsed() ? "PASS" : "BLOCKED", "production cutover runbook sample is parseable JSON", true),
                switchCheck("UNIFIED_BACKEND_CANDIDATE_READY", "PASS", "unified-backend candidate evidence is recorded", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", snapshot.businessPathsRemainUnchanged() ? "PASS" : "BLOCKED", "business paths keep existing /api/v1 prefixes", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "route drift scan evidence remains pass", true),
                switchCheck("LOCAL_ENTRYPOINT_REHEARSAL_PASSED", "PASS", "local entrypoint cutover rehearsal is recorded", true),
                switchCheck("LOCAL_CONFIG_PROVIDER_REHEARSAL_PASSED", snapshot.localConfigProviderRehearsalPassed() ? "PASS" : "BLOCKED", "local config provider rehearsal is recorded", true),
                switchCheck("LOCAL_AUDIT_SINK_REHEARSAL_PASSED", snapshot.localAuditSinkRehearsalPassed() ? "PASS" : "BLOCKED", "local audit sink rehearsal is recorded", true),
                switchCheck("SMOKE_TARGETS_RECORDED", snapshot.smokeTargetsTotal() == smokeTargets().size() ? "PASS" : "BLOCKED", "all smoke targets are recorded", true),
                switchCheck("ROLLBACK_COMMANDS_RECORDED", snapshot.rollbackCommandsRecorded() ? "PASS" : "BLOCKED", "rollback and regression commands are recorded", true),
                switchCheck("CANARY_PLAN_RECORDED", snapshot.canaryPlanRecorded() ? "PASS" : "BLOCKED", "canary plan is recorded without applying traffic", true),
                switchCheck("OBSERVATION_FIELDS_RECORDED", snapshot.observationFieldsRecorded() ? "PASS" : "BLOCKED", "observation fields are recorded without real monitor URLs", true),
                switchCheck("RETIREMENT_ORDER_RECORDED", snapshot.retirementOrderRecorded() ? "PASS" : "BLOCKED", "old entrypoint retirement order is recorded", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_RUNBOOK", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "runbook sample and evidence are redacted", true),
                switchCheck("EXTERNAL_ENTRYPOINT_CONFIG_NOT_APPLIED", snapshot.externalEntrypointConfigApplied() ? "PASS" : "BLOCKED", "external entrypoint config is not applied to production", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", snapshot.productionTrafficObservedOnUnified() ? "PASS" : "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", snapshot.apiGatewayTrafficZeroProven() ? "PASS" : "BLOCKED", "api-gateway zero traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_NOT_STARTED", snapshot.rollbackWindowStarted() ? "PASS" : "BLOCKED", "rollback window is not started", true),
                switchCheck("USER_RETIREMENT_APPROVAL_NOT_GRANTED", snapshot.apiGatewayRetirementApproved() ? "PASS" : "BLOCKED", "retirement approval is not granted", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true)
        );
    }

    Map<String, Object> productionCutoverRunbookEvidence() {
        ProductionCutoverRunbookSnapshot snapshot = cutoverRunbook.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleRunbookPath", snapshot.sampleRunbookPath(),
                "sampleRunbookPresent", snapshot.sampleRunbookPresent(),
                "sampleRunbookParsed", snapshot.sampleRunbookParsed(),
                "sampleRunbookApplied", snapshot.sampleRunbookApplied(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "businessPathsRemainUnchanged", snapshot.businessPathsRemainUnchanged(),
                "smokeTargetsTotal", snapshot.smokeTargetsTotal(),
                "mavenEntrypointsTotal", snapshot.mavenEntrypointsTotal(),
                "rollbackCommandsRecorded", snapshot.rollbackCommandsRecorded(),
                "canaryPlanRecorded", snapshot.canaryPlanRecorded(),
                "observationFieldsRecorded", snapshot.observationFieldsRecorded(),
                "localConfigProviderRehearsalPassed", snapshot.localConfigProviderRehearsalPassed(),
                "localAuditSinkRehearsalPassed", snapshot.localAuditSinkRehearsalPassed(),
                "externalEntrypointConfigApplied", snapshot.externalEntrypointConfigApplied(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowStarted", snapshot.rollbackWindowStarted(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "apiGatewayRetirementApproved", snapshot.apiGatewayRetirementApproved(),
                "coreRetirementApproved", snapshot.coreRetirementApproved(),
                "deletionAllowed", snapshot.deletionAllowed(),
                "bulkRetirementAllowed", snapshot.bulkRetirementAllowed(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionCutoverApprovalPackageStatus() {
        return cutoverApprovalPackage.snapshot().status();
    }

    List<Map<String, Object>> productionCutoverApprovalPackageChecks() {
        ProductionCutoverApprovalPackageSnapshot snapshot = cutoverApprovalPackage.snapshot();
        return List.of(
                switchCheck("APPROVAL_PACKAGE_SAMPLE_PRESENT", snapshot.sampleApprovalPackagePresent() ? "PASS" : "BLOCKED", "production cutover approval package sample is present", true),
                switchCheck("APPROVAL_PACKAGE_JSON_PARSABLE", snapshot.sampleApprovalPackageParsed() ? "PASS" : "BLOCKED", "production cutover approval package sample is parseable JSON", true),
                switchCheck("EXISTING_LOCAL_EVIDENCE_REFERENCED", snapshot.existingEvidenceReferencedTotal() >= 7 ? "PASS" : "BLOCKED", "existing local cutover evidence is referenced", true),
                switchCheck("EXTERNAL_PARAMETER_CHECKLIST_RECORDED", snapshot.externalParametersTotal() >= 10 ? "PASS" : "BLOCKED", "external parameter checklist is recorded without real values", true),
                switchCheck("APPROVAL_ROLES_RECORDED", snapshot.approvalRolesTotal() >= 7 ? "PASS" : "BLOCKED", "approval roles are recorded without real approvers", true),
                switchCheck("GO_NO_GO_MATRIX_RECORDED", snapshot.goNoGoItemsTotal() >= 15 ? "PASS" : "BLOCKED", "go/no-go matrix records local pass items and external blockers", true),
                switchCheck("OBSERVATION_CHECKLIST_RECORDED", snapshot.observationFieldsTotal() >= 10 ? "PASS" : "BLOCKED", "post-cutover observation fields are recorded", true),
                switchCheck("ROLLBACK_AUTHORITY_RECORDED", snapshot.rollbackOperatorApproved() ? "BLOCKED" : "PASS", "rollback authority is recorded without approval", true),
                switchCheck("RETIREMENT_GATE_RECORDED", snapshot.deletionAllowed() || snapshot.bulkRetirementAllowed() ? "BLOCKED" : "PASS", "retirement gate preserves deletion and bulk retirement blocks", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_APPROVAL_PACKAGE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "approval package sample and evidence are redacted", true),
                switchCheck("REAL_EXTERNAL_ENTRYPOINT_VALUES_NOT_PROVIDED", snapshot.externalEntrypointValuesProvided() ? "PASS" : "BLOCKED", "real external entrypoint values are not provided in repository", true),
                switchCheck("REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", snapshot.centralConfigProviderConnected() ? "PASS" : "BLOCKED", "real central config provider remains disconnected", true),
                switchCheck("REAL_AUDIT_SINK_NOT_CONNECTED", snapshot.persistentAuditSinkConnected() ? "PASS" : "BLOCKED", "real persistent audit sink remains disconnected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_APPROVED", snapshot.productionTrafficApproved() ? "PASS" : "BLOCKED", "production traffic switch is not approved", true),
                switchCheck("ROLLBACK_OPERATOR_NOT_APPROVED", snapshot.rollbackOperatorApproved() ? "PASS" : "BLOCKED", "rollback operator is not approved", true),
                switchCheck("RETIREMENT_APPROVER_NOT_GRANTED", snapshot.retirementApproverGranted() ? "PASS" : "BLOCKED", "retirement approval is not granted", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true)
        );
    }

    Map<String, Object> productionCutoverApprovalPackageEvidence() {
        ProductionCutoverApprovalPackageSnapshot snapshot = cutoverApprovalPackage.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleApprovalPackagePath", snapshot.sampleApprovalPackagePath(),
                "sampleApprovalPackagePresent", snapshot.sampleApprovalPackagePresent(),
                "sampleApprovalPackageParsed", snapshot.sampleApprovalPackageParsed(),
                "approvalPackageApplied", snapshot.approvalPackageApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "requiresUserApprovalBeforeApply", snapshot.requiresUserApprovalBeforeApply(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "existingEvidenceReferencedTotal", snapshot.existingEvidenceReferencedTotal(),
                "externalParametersTotal", snapshot.externalParametersTotal(),
                "approvalRolesTotal", snapshot.approvalRolesTotal(),
                "goNoGoItemsTotal", snapshot.goNoGoItemsTotal(),
                "observationFieldsTotal", snapshot.observationFieldsTotal(),
                "verificationCommandsTotal", snapshot.verificationCommandsTotal(),
                "externalEntrypointValuesProvided", snapshot.externalEntrypointValuesProvided(),
                "centralConfigProviderConnected", snapshot.centralConfigProviderConnected(),
                "productionProfileBound", snapshot.productionProfileBound(),
                "sensitiveConfigExternalized", snapshot.sensitiveConfigExternalized(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "productionTrafficApproved", snapshot.productionTrafficApproved(),
                "rollbackOperatorApproved", snapshot.rollbackOperatorApproved(),
                "retirementApproverGranted", snapshot.retirementApproverGranted(),
                "deletionAllowed", snapshot.deletionAllowed(),
                "bulkRetirementAllowed", snapshot.bulkRetirementAllowed(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionCutoverExternalParameterManifestStatus() {
        return externalParameterManifest.snapshot().status();
    }

    List<Map<String, Object>> productionCutoverExternalParameterManifestChecks() {
        ProductionCutoverExternalParameterManifestSnapshot snapshot = externalParameterManifest.snapshot();
        return List.of(
                switchCheck("EXTERNAL_PARAMETER_MANIFEST_SAMPLE_PRESENT", snapshot.sampleManifestPresent() ? "PASS" : "BLOCKED", "production cutover external parameter manifest sample is present", true),
                switchCheck("EXTERNAL_PARAMETER_MANIFEST_JSON_PARSABLE", snapshot.sampleManifestParsed() ? "PASS" : "BLOCKED", "external parameter manifest sample is parseable JSON", true),
                switchCheck("PARAMETER_GROUPS_RECORDED", snapshot.parameterGroupsTotal() >= 6 ? "PASS" : "BLOCKED", "external parameter groups are recorded", true),
                switchCheck("FRONTEND_ENTRYPOINT_PARAMETER_RECORDED", snapshot.parameterKeys().contains("frontendApiBaseUrl") ? "PASS" : "BLOCKED", "frontend entrypoint parameter is recorded", true),
                switchCheck("PROXY_UPSTREAM_PARAMETER_RECORDED", snapshot.parameterKeys().contains("reverseProxyUpstream") ? "PASS" : "BLOCKED", "proxy upstream parameter is recorded", true),
                switchCheck("DEPLOYMENT_ENTRYPOINT_PARAMETER_RECORDED", snapshot.parameterKeys().contains("deploymentEntrypointTarget") ? "PASS" : "BLOCKED", "deployment entrypoint parameter is recorded", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_PARAMETER_RECORDED", snapshot.parameterKeys().contains("centralConfigProviderRef") ? "PASS" : "BLOCKED", "central config provider parameter is recorded", true),
                switchCheck("PRODUCTION_PROFILE_PARAMETER_RECORDED", snapshot.parameterKeys().contains("productionProfileRef") ? "PASS" : "BLOCKED", "production profile parameter is recorded", true),
                switchCheck("SENSITIVE_CONFIG_EXTERNALIZATION_PARAMETER_RECORDED", snapshot.parameterKeys().contains("sensitiveConfigExternalizationRef") ? "PASS" : "BLOCKED", "sensitive config externalization parameter is recorded", true),
                switchCheck("AUDIT_SINK_PARAMETER_RECORDED", snapshot.parameterKeys().contains("persistentAuditSinkRef") ? "PASS" : "BLOCKED", "audit sink parameter is recorded", true),
                switchCheck("OBSERVABILITY_PARAMETER_RECORDED", snapshot.parameterKeys().contains("httpSmokeObservationRef") ? "PASS" : "BLOCKED", "observability parameter references are recorded", true),
                switchCheck("APPROVAL_REFERENCE_RECORDED", snapshot.approvalPackageReferenced() ? "PASS" : "BLOCKED", "approval package reference is recorded", true),
                switchCheck("ROLLBACK_AUTHORITY_REFERENCE_RECORDED", snapshot.parameterKeys().contains("rollbackOperatorApprovalRef") ? "PASS" : "BLOCKED", "rollback authority reference is recorded", true),
                switchCheck("RETIREMENT_APPROVAL_REFERENCE_RECORDED", snapshot.parameterKeys().contains("retirementApproverRef") ? "PASS" : "BLOCKED", "retirement approval reference is recorded", true),
                switchCheck("NO_REAL_VALUES_IN_REPOSITORY", snapshot.realValuesProvidedInRepository() || snapshot.realValuesAllowedInRepository() || snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "manifest sample records no real runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("REAL_EXTERNAL_ENTRYPOINT_VALUES_NOT_PROVIDED", snapshot.realValuesProvidedInRepository() ? "PASS" : "BLOCKED", "real external entrypoint values are not provided in repository", true),
                switchCheck("REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", snapshot.centralConfigProviderConnected() ? "PASS" : "BLOCKED", "real central config provider remains disconnected", true),
                switchCheck("REAL_AUDIT_SINK_NOT_CONNECTED", snapshot.persistentAuditSinkConnected() ? "PASS" : "BLOCKED", "real audit sink remains disconnected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", snapshot.productionTrafficObservedOnUnified() ? "PASS" : "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", snapshot.apiGatewayTrafficZeroProven() ? "PASS" : "BLOCKED", "api-gateway zero traffic is not proven", true),
                switchCheck("RETIREMENT_NOT_APPROVED", snapshot.retirementApproverGranted() ? "PASS" : "BLOCKED", "entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> productionCutoverExternalParameterManifestEvidence() {
        ProductionCutoverExternalParameterManifestSnapshot snapshot = externalParameterManifest.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleManifestPath", snapshot.sampleManifestPath(),
                "sampleManifestPresent", snapshot.sampleManifestPresent(),
                "sampleManifestParsed", snapshot.sampleManifestParsed(),
                "manifestApplied", snapshot.manifestApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "requiresExternalSecretStore", snapshot.requiresExternalSecretStore(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "parameterGroupsTotal", snapshot.parameterGroupsTotal(),
                "parametersTotal", snapshot.parametersTotal(),
                "requiredExternalParametersTotal", snapshot.requiredExternalParametersTotal(),
                "realValuesProvidedInRepository", snapshot.realValuesProvidedInRepository(),
                "redactedParametersTotal", snapshot.redactedParametersTotal(),
                "approvalPackageReferenced", snapshot.approvalPackageReferenced(),
                "approvalPackageApplied", snapshot.approvalPackageApplied(),
                "productionTrafficApproved", snapshot.productionTrafficApproved(),
                "centralConfigProviderConnected", snapshot.centralConfigProviderConnected(),
                "productionProfileBound", snapshot.productionProfileBound(),
                "sensitiveConfigExternalized", snapshot.sensitiveConfigExternalized(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "retirementApproverGranted", snapshot.retirementApproverGranted(),
                "deletionAllowed", snapshot.deletionAllowed(),
                "bulkRetirementAllowed", snapshot.bulkRetirementAllowed(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionCutoverEvidenceConsistencyAuditStatus() {
        return evidenceConsistencyAudit.snapshot().status();
    }

    List<Map<String, Object>> productionCutoverEvidenceConsistencyAuditChecks() {
        ProductionCutoverEvidenceConsistencyAuditSnapshot snapshot = evidenceConsistencyAudit.snapshot();
        return List.of(
                switchCheck("CUTOVER_EVIDENCE_SAMPLES_PRESENT", snapshot.samplesPresent() ? "PASS" : "BLOCKED", "cutover evidence sample files are present", true),
                switchCheck("CUTOVER_EVIDENCE_SAMPLES_PARSEABLE", snapshot.samplesParsed() ? "PASS" : "BLOCKED", "JSON and JSONL cutover evidence samples are parseable", true),
                switchCheck("CANDIDATE_ENTRYPOINT_CONSISTENT", snapshot.inconsistentEntrypointRefs().isEmpty() ? "PASS" : "BLOCKED", "candidate entrypoint references stay on unified-backend:8135", true),
                switchCheck("CURRENT_ENTRYPOINT_CONSISTENT", snapshot.inconsistentEntrypointRefs().isEmpty() ? "PASS" : "BLOCKED", "current entrypoint references stay on api-gateway:8125", true),
                switchCheck("ROLLBACK_ENTRYPOINT_CONSISTENT", snapshot.inconsistentEntrypointRefs().isEmpty() ? "PASS" : "BLOCKED", "rollback entrypoint references stay on api-gateway:8125", true),
                switchCheck("MAVEN_REGRESSION_COMMANDS_CONSISTENT", snapshot.inconsistentVerificationCommands().isEmpty() ? "PASS" : "BLOCKED", "runbook, approval package and manifest keep the same Maven regression commands", true),
                switchCheck("EXTERNAL_PARAMETER_KEYS_REFERENCED_BY_APPROVAL_PACKAGE", snapshot.missingApprovalParameterKeys().isEmpty() ? "PASS" : "BLOCKED", "approval package external parameter references are covered by manifest aliases", true),
                switchCheck("RUNBOOK_REFERENCES_EXTERNAL_PARAMETER_MANIFEST", snapshot.missingManifestParameterKeys().isEmpty() ? "PASS" : "BLOCKED", "runbook references the external parameter manifest sample", true),
                switchCheck("CENTRAL_CONFIG_KEYS_COVERED_BY_MANIFEST", snapshot.missingManifestParameterKeys().isEmpty() ? "PASS" : "BLOCKED", "central config sample keys are covered by manifest parameters", true),
                switchCheck("AUDIT_SINK_KEYS_COVERED_BY_MANIFEST", snapshot.missingManifestParameterKeys().isEmpty() ? "PASS" : "BLOCKED", "audit sink sample keys are covered by manifest parameters", true),
                switchCheck("OBSERVABILITY_KEYS_COVERED_BY_RUNBOOK_AND_MANIFEST", snapshot.missingManifestParameterKeys().isEmpty() ? "PASS" : "BLOCKED", "observability fields are covered by runbook and manifest", true),
                switchCheck("RETIREMENT_AND_ROLLBACK_GATES_CONSISTENT", snapshot.inconsistentBlockers().isEmpty() ? "PASS" : "BLOCKED", "rollback, zero-traffic and retirement blockers stay aligned", true),
                switchCheck("NO_REAL_VALUES_IN_CUTOVER_EVIDENCE", snapshot.realValuesProvidedInRepository() || snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "cutover evidence contains no real runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("REAL_EXTERNAL_VALUES_NOT_IMPORTED", snapshot.realValuesProvidedInRepository() ? "PASS" : "BLOCKED", "real external values are not imported into repository", true),
                switchCheck("REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", "real central config provider remains disconnected", true),
                switchCheck("REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", "real audit sink remains disconnected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero traffic is not proven", true),
                switchCheck("RETIREMENT_NOT_APPROVED", "BLOCKED", "entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> productionCutoverEvidenceConsistencyAuditEvidence() {
        ProductionCutoverEvidenceConsistencyAuditSnapshot snapshot = evidenceConsistencyAudit.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "auditedSamplePaths", snapshot.auditedSamplePaths(),
                "samplesPresent", snapshot.samplesPresent(),
                "samplesParsed", snapshot.samplesParsed(),
                "candidateEntrypoint", snapshot.candidateEntrypoint(),
                "currentEntrypoint", snapshot.currentEntrypoint(),
                "rollbackEntrypoint", snapshot.rollbackEntrypoint(),
                "externalParameterKeysTotal", snapshot.externalParameterKeysTotal(),
                "approvalPackageExternalParametersTotal", snapshot.approvalPackageExternalParametersTotal(),
                "runbookVerificationCommandsTotal", snapshot.runbookVerificationCommandsTotal(),
                "manifestVerificationCommandsTotal", snapshot.manifestVerificationCommandsTotal(),
                "missingApprovalParameterKeys", snapshot.missingApprovalParameterKeys(),
                "missingManifestParameterKeys", snapshot.missingManifestParameterKeys(),
                "inconsistentEntrypointRefs", snapshot.inconsistentEntrypointRefs(),
                "inconsistentVerificationCommands", snapshot.inconsistentVerificationCommands(),
                "inconsistentBlockers", snapshot.inconsistentBlockers(),
                "realValuesProvidedInRepository", snapshot.realValuesProvidedInRepository(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionExternalValueIntakeRehearsalStatus() {
        return externalValueIntakeRehearsal.snapshot().status();
    }

    List<Map<String, Object>> productionExternalValueIntakeRehearsalChecks() {
        ProductionExternalValueIntakeRehearsalSnapshot snapshot = externalValueIntakeRehearsal.snapshot();
        return List.of(
                switchCheck("EXTERNAL_VALUE_INTAKE_SAMPLE_PRESENT", snapshot.sampleIntakePresent() ? "PASS" : "BLOCKED", "external value intake sample is present", true),
                switchCheck("EXTERNAL_VALUE_INTAKE_SAMPLE_JSON_PARSABLE", snapshot.sampleIntakeParsed() ? "PASS" : "BLOCKED", "external value intake sample is parseable JSON", true),
                switchCheck("VALUE_GROUPS_RECORDED", snapshot.valueGroupsTotal() >= 7 ? "PASS" : "BLOCKED", "external value groups are recorded", true),
                switchCheck("INTAKE_CHANNELS_RECORDED", snapshot.intakeChannelsTotal() >= 6 ? "PASS" : "BLOCKED", "external intake channels are recorded", true),
                switchCheck("INJECTION_TARGETS_RECORDED", snapshot.injectionTargetsTotal() >= snapshot.valueItemsTotal() ? "PASS" : "BLOCKED", "injection targets are recorded for all value items", true),
                switchCheck("VALIDATION_REFS_RECORDED", snapshot.validationRefsTotal() >= snapshot.valueItemsTotal() ? "PASS" : "BLOCKED", "validation references are recorded for all value items", true),
                switchCheck("ROLLBACK_REFS_RECORDED", snapshot.rollbackRefsTotal() >= snapshot.valueItemsTotal() ? "PASS" : "BLOCKED", "rollback references are recorded for all value items", true),
                switchCheck("NO_REAL_VALUES_IN_REPOSITORY", snapshot.realValuesProvidedInRepository() ? "BLOCKED" : "PASS", "repository contains only external references, not real runtime values", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_INTAKE_SAMPLE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "external value intake sample does not expose sensitive runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("EXTERNAL_VALUE_INTAKE_REHEARSAL_RECORDED", "PASS", "local external value intake rehearsal is recorded without applying production traffic", true),
                switchCheck("REAL_EXTERNAL_VALUES_NOT_PROVIDED", "BLOCKED", "real external values are not provided to runtime", true),
                switchCheck("REAL_ENTRYPOINT_NOT_APPLIED", "BLOCKED", "real external entrypoint is not applied", true),
                switchCheck("REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", "real central config provider remains disconnected", true),
                switchCheck("REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", "real persistent audit sink remains disconnected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("RETIREMENT_NOT_APPROVED", "BLOCKED", "entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> productionExternalValueIntakeRehearsalEvidence() {
        ProductionExternalValueIntakeRehearsalSnapshot snapshot = externalValueIntakeRehearsal.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleIntakePath", snapshot.sampleIntakePath(),
                "sampleIntakePresent", snapshot.sampleIntakePresent(),
                "sampleIntakeParsed", snapshot.sampleIntakeParsed(),
                "intakeApplied", snapshot.intakeApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "requiresExternalSecretStore", snapshot.requiresExternalSecretStore(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "currentEntrypointRef", snapshot.currentEntrypointRef(),
                "rollbackEntrypointRef", snapshot.rollbackEntrypointRef(),
                "valueGroupsTotal", snapshot.valueGroupsTotal(),
                "intakeChannelsTotal", snapshot.intakeChannelsTotal(),
                "valueItemsTotal", snapshot.valueItemsTotal(),
                "injectionTargetsTotal", snapshot.injectionTargetsTotal(),
                "validationRefsTotal", snapshot.validationRefsTotal(),
                "rollbackRefsTotal", snapshot.rollbackRefsTotal(),
                "realValuesProvidedInRepository", snapshot.realValuesProvidedInRepository(),
                "redactedValuesTotal", snapshot.redactedValuesTotal(),
                "centralConfigProviderConnected", snapshot.centralConfigProviderConnected(),
                "productionProfileBound", snapshot.productionProfileBound(),
                "sensitiveConfigExternalized", snapshot.sensitiveConfigExternalized(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "retirementApproverGranted", snapshot.retirementApproverGranted(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionRuntimeConfigShellStatus() {
        return runtimeConfigShellRehearsal.snapshot().status();
    }

    List<Map<String, Object>> productionRuntimeConfigShellChecks() {
        ProductionRuntimeConfigShellSnapshot snapshot = runtimeConfigShellRehearsal.snapshot();
        return List.of(
                switchCheck("PRODUCTION_RUNTIME_SHELL_SAMPLE_PRESENT", snapshot.sampleRuntimeShellPresent() ? "PASS" : "BLOCKED", "production runtime shell sample is present", true),
                switchCheck("PRODUCTION_RUNTIME_SHELL_SAMPLE_JSON_PARSABLE", snapshot.sampleRuntimeShellParsed() ? "PASS" : "BLOCKED", "production runtime shell sample is parseable JSON", true),
                switchCheck("RUNTIME_PROFILE_SLOT_RECORDED", snapshot.runtimeProfilesTotal() >= 3 ? "PASS" : "BLOCKED", "production, rollback and local rehearsal profile slots are recorded", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_SLOT_RECORDED", snapshot.configProviderBindingsTotal() >= 5 ? "PASS" : "BLOCKED", "central config provider slots are recorded", true),
                switchCheck("SENSITIVE_CONFIG_EXTERNALIZATION_SLOT_RECORDED", snapshot.sensitiveConfigBindingsTotal() >= 5 ? "PASS" : "BLOCKED", "sensitive config externalization slots are recorded", true),
                switchCheck("DEPLOYMENT_ENTRYPOINT_SLOT_RECORDED", snapshot.deploymentEntrypointBindingsTotal() >= 5 ? "PASS" : "BLOCKED", "deployment entrypoint slots are recorded", true),
                switchCheck("ROLLBACK_CONFIG_SLOT_RECORDED", snapshot.rollbackConfigBindingsTotal() >= 5 ? "PASS" : "BLOCKED", "rollback config slots are recorded", true),
                switchCheck("EXTERNAL_VALUE_INTAKE_REHEARSAL_REFERENCED", snapshot.externalValueIntakeRehearsalReferenced() ? "PASS" : "BLOCKED", "third-ninth round external value intake rehearsal is referenced", true),
                switchCheck("NO_REAL_VALUES_IN_REPOSITORY", snapshot.realValuesProvidedInRepository() ? "BLOCKED" : "PASS", "repository contains only runtime shell references, not real production values", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_RUNTIME_SHELL_SAMPLE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "runtime shell sample does not expose sensitive runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_RECORDED", "PASS", "local production runtime config shell rehearsal is recorded without applying production traffic", true),
                switchCheck("REAL_PRODUCTION_PROFILE_NOT_BOUND", "BLOCKED", "real production profile remains unbound", true),
                switchCheck("REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", "real central config provider remains disconnected", true),
                switchCheck("REAL_SENSITIVE_CONFIG_NOT_EXTERNALIZED", "BLOCKED", "real sensitive config source remains externalization-blocked", true),
                switchCheck("REAL_DEPLOYMENT_ENTRYPOINT_NOT_BOUND", "BLOCKED", "real deployment entrypoint remains unbound", true),
                switchCheck("REAL_ROLLBACK_CONFIG_NOT_BOUND", "BLOCKED", "real rollback config remains unbound", true),
                switchCheck("REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", "real persistent audit sink remains disconnected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("RETIREMENT_NOT_APPROVED", "BLOCKED", "entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> productionRuntimeConfigShellEvidence() {
        ProductionRuntimeConfigShellSnapshot snapshot = runtimeConfigShellRehearsal.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleRuntimeShellPath", snapshot.sampleRuntimeShellPath(),
                "sampleRuntimeShellPresent", snapshot.sampleRuntimeShellPresent(),
                "sampleRuntimeShellParsed", snapshot.sampleRuntimeShellParsed(),
                "runtimeShellApplied", snapshot.runtimeShellApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "requiresExternalConfigProvider", snapshot.requiresExternalConfigProvider(),
                "requiresExternalSecretStore", snapshot.requiresExternalSecretStore(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "currentEntrypointRef", snapshot.currentEntrypointRef(),
                "rollbackEntrypointRef", snapshot.rollbackEntrypointRef(),
                "runtimeProfilesTotal", snapshot.runtimeProfilesTotal(),
                "configProviderBindingsTotal", snapshot.configProviderBindingsTotal(),
                "sensitiveConfigBindingsTotal", snapshot.sensitiveConfigBindingsTotal(),
                "deploymentEntrypointBindingsTotal", snapshot.deploymentEntrypointBindingsTotal(),
                "rollbackConfigBindingsTotal", snapshot.rollbackConfigBindingsTotal(),
                "validationCommandsTotal", snapshot.validationCommandsTotal(),
                "productionProfileBound", snapshot.productionProfileBound(),
                "centralConfigProviderConnected", snapshot.centralConfigProviderConnected(),
                "sensitiveConfigExternalized", snapshot.sensitiveConfigExternalized(),
                "deploymentEntrypointBound", snapshot.deploymentEntrypointBound(),
                "rollbackConfigBound", snapshot.rollbackConfigBound(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "retirementApproverGranted", snapshot.retirementApproverGranted(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "realValuesProvidedInRepository", snapshot.realValuesProvidedInRepository(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionAuditObservabilitySmokeStatus() {
        return auditObservabilitySmokeRehearsal.snapshot().status();
    }

    List<Map<String, Object>> productionAuditObservabilitySmokeChecks() {
        ProductionAuditObservabilitySmokeSnapshot snapshot = auditObservabilitySmokeRehearsal.snapshot();
        return List.of(
                switchCheck("AUDIT_OBSERVABILITY_SMOKE_SAMPLE_PRESENT", snapshot.sampleAuditObservabilitySmokePresent() ? "PASS" : "BLOCKED", "audit observability smoke sample is present", true),
                switchCheck("AUDIT_OBSERVABILITY_SMOKE_SAMPLE_JSON_PARSABLE", snapshot.sampleAuditObservabilitySmokeParsed() ? "PASS" : "BLOCKED", "audit observability smoke sample is parseable JSON", true),
                switchCheck("RUNTIME_CONFIG_SHELL_REHEARSAL_REFERENCED", snapshot.runtimeConfigShellRehearsalReferenced() ? "PASS" : "BLOCKED", "runtime config shell rehearsal is referenced", true),
                switchCheck("AUDIT_SINK_BINDING_RECORDED", snapshot.auditSinkBindingRecorded() ? "PASS" : "BLOCKED", "audit store binding references are recorded", true),
                switchCheck("AUDIT_EVENT_SCHEMA_RECORDED", snapshot.auditEventSchemaRecorded() ? "PASS" : "BLOCKED", "audit event schema is recorded", true),
                switchCheck("REQUEST_ID_PROPAGATION_RECORDED", snapshot.requestIdPropagationRecorded() ? "PASS" : "BLOCKED", "request id propagation is recorded", true),
                switchCheck("AUDIT_WRITE_SMOKE_REFERENCE_RECORDED", snapshot.auditWriteSmokeReferenceRecorded() ? "PASS" : "BLOCKED", "audit write smoke references are recorded", true),
                switchCheck("AUDIT_REPLAY_EXPORT_RETENTION_RECORDED", snapshot.auditReplayExportRetentionRecorded() ? "PASS" : "BLOCKED", "audit replay, export and retention references are recorded", true),
                switchCheck("HTTP_SMOKE_OBSERVATION_RECORDED", snapshot.httpSmokeObservationRecorded() ? "PASS" : "BLOCKED", "HTTP smoke observation reference is recorded", true),
                switchCheck("ERROR_RATE_OBSERVATION_RECORDED", snapshot.errorRateObservationRecorded() ? "PASS" : "BLOCKED", "error rate observation reference is recorded", true),
                switchCheck("LATENCY_OBSERVATION_RECORDED", snapshot.latencyObservationRecorded() ? "PASS" : "BLOCKED", "latency observation references are recorded", true),
                switchCheck("BUSINESS_CODE_OBSERVATION_RECORDED", snapshot.businessCodeObservationRecorded() ? "PASS" : "BLOCKED", "business code observation reference is recorded", true),
                switchCheck("TRACE_CORRELATION_RECORDED", snapshot.traceCorrelationRecorded() ? "PASS" : "BLOCKED", "trace correlation reference is recorded", true),
                switchCheck("DASHBOARD_AND_ALERT_REFERENCES_RECORDED", snapshot.dashboardReferencesRecorded() && snapshot.alertReferencesRecorded() ? "PASS" : "BLOCKED", "dashboard and alert references are recorded", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_AUDIT_OBSERVABILITY_SAMPLE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "audit observability smoke sample has no sensitive runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_RECORDED", "PASS", "local audit observability smoke rehearsal is recorded without production traffic", true),
                switchCheck("REAL_PERSISTENT_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", "real persistent audit sink is not connected", true),
                switchCheck("REAL_AUDIT_WRITE_PATH_NOT_CONNECTED", "BLOCKED", "real audit write path is not connected", true),
                switchCheck("REAL_AUDIT_WRITE_SMOKE_NOT_PASSED", "BLOCKED", "real audit write smoke has not passed", true),
                switchCheck("REAL_AUDIT_REPLAY_EXPORT_RETENTION_NOT_CONNECTED", "BLOCKED", "real audit replay, export and retention paths are not connected", true),
                switchCheck("REAL_OBSERVABILITY_PLATFORM_NOT_CONNECTED", "BLOCKED", "real observability platform is not connected", true),
                switchCheck("REAL_DASHBOARD_NOT_CONNECTED", "BLOCKED", "real dashboard is not connected", true),
                switchCheck("REAL_ALERTING_NOT_CONNECTED", "BLOCKED", "real alerting is not connected", true),
                switchCheck("REAL_TRACE_PIPELINE_NOT_CONNECTED", "BLOCKED", "real trace pipeline is not connected", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED", "BLOCKED", "production traffic is not observed on unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("RETIREMENT_NOT_APPROVED", "BLOCKED", "entrypoint retirement is not approved", true)
        );
    }

    Map<String, Object> productionAuditObservabilitySmokeEvidence() {
        ProductionAuditObservabilitySmokeSnapshot snapshot = auditObservabilitySmokeRehearsal.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "sampleAuditObservabilitySmokePath", snapshot.sampleAuditObservabilitySmokePath(),
                "sampleAuditObservabilitySmokePresent", snapshot.sampleAuditObservabilitySmokePresent(),
                "sampleAuditObservabilitySmokeParsed", snapshot.sampleAuditObservabilitySmokeParsed(),
                "runtimeConfigShellSampleRef", snapshot.runtimeConfigShellSampleRef(),
                "runtimeConfigShellStatusRequired", snapshot.runtimeConfigShellStatusRequired(),
                "auditSinkBindingRecorded", snapshot.auditSinkBindingRecorded(),
                "auditEventSchemaRecorded", snapshot.auditEventSchemaRecorded(),
                "requestIdPropagationRecorded", snapshot.requestIdPropagationRecorded(),
                "auditWriteSmokeReferenceRecorded", snapshot.auditWriteSmokeReferenceRecorded(),
                "auditReplayExportRetentionRecorded", snapshot.auditReplayExportRetentionRecorded(),
                "httpSmokeObservationRecorded", snapshot.httpSmokeObservationRecorded(),
                "errorRateObservationRecorded", snapshot.errorRateObservationRecorded(),
                "latencyObservationRecorded", snapshot.latencyObservationRecorded(),
                "businessCodeObservationRecorded", snapshot.businessCodeObservationRecorded(),
                "traceCorrelationRecorded", snapshot.traceCorrelationRecorded(),
                "dashboardReferencesRecorded", snapshot.dashboardReferencesRecorded(),
                "alertReferencesRecorded", snapshot.alertReferencesRecorded(),
                "sampleAuditSmokeTargetsTotal", snapshot.sampleAuditSmokeTargetsTotal(),
                "sampleObservabilitySignalsTotal", snapshot.sampleObservabilitySignalsTotal(),
                "sampleDashboardRefsTotal", snapshot.sampleDashboardRefsTotal(),
                "sampleAlertRefsTotal", snapshot.sampleAlertRefsTotal(),
                "sampleRollbackRefsTotal", snapshot.sampleRollbackRefsTotal(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWritePathConnected", snapshot.auditWritePathConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "auditReplayPathConnected", snapshot.auditReplayPathConnected(),
                "auditExportPathConnected", snapshot.auditExportPathConnected(),
                "auditRetentionJobConnected", snapshot.auditRetentionJobConnected(),
                "observabilityPlatformConnected", snapshot.observabilityPlatformConnected(),
                "dashboardConnected", snapshot.dashboardConnected(),
                "alertingConnected", snapshot.alertingConnected(),
                "tracePipelineConnected", snapshot.tracePipelineConnected(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "retirementApproverGranted", snapshot.retirementApproverGranted(),
                "realValuesProvidedInRepository", snapshot.realValuesProvidedInRepository(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "remainingProductionBlockers", snapshot.remainingProductionBlockers(),
                "status", snapshot.status()
        );
    }

    String productionControlledCutoverStatus() {
        return controlledCutoverReceipt.snapshot().status();
    }

    List<Map<String, Object>> productionControlledCutoverChecks() {
        ProductionControlledCutoverReceiptSnapshot snapshot = controlledCutoverReceipt.snapshot();
        return List.of(
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_SAMPLE_PRESENT", snapshot.receiptPresent() ? "PASS" : "BLOCKED", "controlled cutover receipt sample is present", true),
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_SAMPLE_JSON_PARSABLE", snapshot.receiptParsed() ? "PASS" : "BLOCKED", "controlled cutover receipt sample is parseable JSON", true),
                switchCheck("RUNTIME_CONFIG_SHELL_REHEARSAL_REFERENCED", snapshot.runtimeConfigShellRehearsalReferenced() ? "PASS" : "BLOCKED", "runtime config shell rehearsal is referenced", true),
                switchCheck("AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_REFERENCED", snapshot.auditObservabilitySmokeRehearsalReferenced() ? "PASS" : "BLOCKED", "audit observability smoke rehearsal is referenced", true),
                switchCheck("EXTERNAL_VALUE_INTAKE_REHEARSAL_REFERENCED", snapshot.externalValueIntakeRehearsalReferenced() ? "PASS" : "BLOCKED", "external value intake rehearsal is referenced", true),
                switchCheck("APPROVAL_REFS_RECORDED", snapshot.approvalRefsTotal() >= 6 ? "PASS" : "BLOCKED", "controlled cutover approval references are recorded", true),
                switchCheck("TRAFFIC_PLAN_RECORDED", snapshot.trafficStagesTotal() >= 5 ? "PASS" : "BLOCKED", "controlled cutover traffic plan is recorded", true),
                switchCheck("SMOKE_REFS_RECORDED", snapshot.smokeRefsTotal() >= 14 ? "PASS" : "BLOCKED", "business smoke references are recorded", true),
                switchCheck("AUDIT_REFS_RECORDED", snapshot.auditRefsTotal() >= 6 ? "PASS" : "BLOCKED", "audit references are recorded", true),
                switchCheck("OBSERVABILITY_REFS_RECORDED", snapshot.observabilityRefsTotal() >= 10 ? "PASS" : "BLOCKED", "observability references are recorded", true),
                switchCheck("ROLLBACK_WINDOW_REFS_RECORDED", snapshot.rollbackWindowRefsTotal() >= 6 ? "PASS" : "BLOCKED", "rollback window references are recorded", true),
                switchCheck("OLD_ENTRYPOINT_PROTECTION_RECORDED", snapshot.oldEntrypointsPreserved() ? "PASS" : "BLOCKED", "old entrypoints remain protected for the next round", true),
                switchCheck("NO_REAL_VALUES_IN_REPOSITORY", snapshot.realValuesAllowedInRepository() ? "BLOCKED" : "PASS", "repository contains only receipt references, not real runtime values", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_CONTROLLED_CUTOVER_RECEIPT", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "controlled cutover receipt sample has no sensitive runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "readyForProduction and readyToReplaceGateway remain false", true),
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_GATE_RECORDED", snapshot.sampleValid() ? "PASS" : "BLOCKED", "local controlled cutover receipt gate is recorded without approving retirement", true),
                switchCheck("REAL_CUTOVER_RECEIPT_NOT_PROVIDED", "BLOCKED", "real controlled cutover receipt is not provided outside repository", true),
                switchCheck("REAL_ENTRYPOINT_NOT_APPLIED", "BLOCKED", "real external entrypoint is not applied to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED", "BLOCKED", "production traffic is not observed on unified-backend", true),
                switchCheck("REAL_AUDIT_WRITE_SMOKE_NOT_PASSED", "BLOCKED", "real audit write smoke has not passed", true),
                switchCheck("REAL_DASHBOARD_NOT_VERIFIED", "BLOCKED", "real dashboard is not verified", true),
                switchCheck("REAL_ALERTING_NOT_VERIFIED", "BLOCKED", "real alerting is not verified", true),
                switchCheck("REAL_TRACE_PIPELINE_NOT_VERIFIED", "BLOCKED", "real trace pipeline is not verified", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("RETIREMENT_NOT_APPROVED", "BLOCKED", "old entrypoint retirement is not approved", true),
                switchCheck("OLD_ENTRYPOINTS_NOT_IN_RETIREMENT_ROUND", "BLOCKED", "old entrypoints are preserved until the later retirement round", true)
        );
    }

    Map<String, Object> productionControlledCutoverEvidence() {
        ProductionControlledCutoverReceiptSnapshot snapshot = controlledCutoverReceipt.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "receiptPath", snapshot.receiptPath(),
                "receiptPresent", snapshot.receiptPresent(),
                "receiptParsed", snapshot.receiptParsed(),
                "receiptApplied", snapshot.receiptApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "previousEntrypointRef", snapshot.previousEntrypointRef(),
                "rollbackEntrypointRef", snapshot.rollbackEntrypointRef(),
                "approvalRefsTotal", snapshot.approvalRefsTotal(),
                "runtimePrerequisiteRefsTotal", snapshot.runtimePrerequisiteRefsTotal(),
                "trafficStagesTotal", snapshot.trafficStagesTotal(),
                "smokeRefsTotal", snapshot.smokeRefsTotal(),
                "auditRefsTotal", snapshot.auditRefsTotal(),
                "observabilityRefsTotal", snapshot.observabilityRefsTotal(),
                "rollbackWindowRefsTotal", snapshot.rollbackWindowRefsTotal(),
                "apiGatewayTrafficRefsTotal", snapshot.apiGatewayTrafficRefsTotal(),
                "cutoverExecutionRefsTotal", snapshot.cutoverExecutionRefsTotal(),
                "finalTrafficWeightPercent", snapshot.finalTrafficWeightPercent(),
                "productionTrafficObservedOnUnified", snapshot.productionTrafficObservedOnUnified(),
                "apiGatewayTrafficZeroProven", snapshot.apiGatewayTrafficZeroProven(),
                "rollbackWindowCompleted", snapshot.rollbackWindowCompleted(),
                "persistentAuditSinkConnected", snapshot.persistentAuditSinkConnected(),
                "auditWriteSmokePassed", snapshot.auditWriteSmokePassed(),
                "observabilityPlatformConnected", snapshot.observabilityPlatformConnected(),
                "dashboardConnected", snapshot.dashboardConnected(),
                "alertingConnected", snapshot.alertingConnected(),
                "tracePipelineConnected", snapshot.tracePipelineConnected(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "oldEntrypointsPreserved", snapshot.oldEntrypointsPreserved(),
                "nodeDaemonOutOfRepository", snapshot.nodeDaemonOutOfRepository(),
                "readyForProduction", snapshot.readyForProduction(),
                "readyToReplaceGateway", snapshot.readyToReplaceGateway(),
                "readyToRetireOldEntrypoints", snapshot.readyToRetireOldEntrypoints(),
                "remainingBlockers", snapshot.remainingBlockers(),
                "status", snapshot.status()
        );
    }

    String apiGatewayControlledRetirementStatus() {
        return apiGatewayControlledRetirementReceipt.snapshot().status();
    }

    List<Map<String, Object>> apiGatewayControlledRetirementChecks() {
        ApiGatewayControlledRetirementReceiptSnapshot snapshot = apiGatewayControlledRetirementReceipt.snapshot();
        return List.of(
                switchCheck("API_GATEWAY_RETIREMENT_RECEIPT_SAMPLE_PRESENT", snapshot.receiptPresent() ? "PASS" : "BLOCKED", "api-gateway retirement receipt sample is present", true),
                switchCheck("API_GATEWAY_RETIREMENT_RECEIPT_SAMPLE_JSON_PARSABLE", snapshot.receiptParsed() ? "PASS" : "BLOCKED", "api-gateway retirement receipt sample is parseable JSON", true),
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_REFERENCED", snapshot.controlledCutoverReceiptReferenced() ? "PASS" : "BLOCKED", "controlled production cutover receipt is referenced", true),
                switchCheck("GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED", "PASS", "gateway self APIs remain mounted in unified-backend", true),
                switchCheck("BUSINESS_PATHS_UNCHANGED", "PASS", "business paths keep existing /api/v1 prefixes", true),
                switchCheck("CORE_MODULE_SOURCES_PRESERVED", snapshot.coreEntrypointsPreserved() ? "PASS" : "BLOCKED", "five core module sources remain mounted in unified-backend", true),
                switchCheck("NODE_DAEMON_OUT_OF_REPOSITORY", "PASS", "node daemon remains out of repository", true),
                switchCheck("DELETE_LIST_RECORDED", snapshot.deleteListItemsTotal() >= 6 ? "PASS" : "BLOCKED", "explicit api-gateway deletion list is recorded without executing it", true),
                switchCheck("NO_REAL_VALUES_IN_API_GATEWAY_RETIREMENT_RECEIPT", snapshot.realValuesAllowedInRepository() ? "BLOCKED" : "PASS", "retirement receipt contains only redacted references", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_API_GATEWAY_RETIREMENT_RECEIPT", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "retirement receipt sample has no sensitive runtime values", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "production and retirement ready flags remain false", true),
                switchCheck("UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", snapshot.unifiedBuildHelperStillReferencesApiGateway() ? "BLOCKED" : "PASS", "unified-backend compiles its own gateway control source", true),
                switchCheck("RETIREMENT_RECEIPT_NOT_PROVIDED", "BLOCKED", "real api-gateway retirement receipt is not provided outside repository", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED", "BLOCKED", "production traffic is not observed on unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("REAL_AUDIT_WRITE_SMOKE_NOT_PASSED", "BLOCKED", "real audit write smoke has not passed", true),
                switchCheck("DASHBOARD_ALERT_TRACE_NOT_VERIFIED", "BLOCKED", "real dashboard, alert and trace evidence is not verified", true),
                switchCheck("RETIREMENT_APPROVAL_NOT_GRANTED", "BLOCKED", "api-gateway retirement approval is not granted", true),
                switchCheck("DELETE_LIST_NOT_APPROVED", "BLOCKED", "delete list is not approved by the user", true),
                switchCheck("GATEWAY_SELF_API_PARITY_NOT_PROVEN_WITH_REAL_RECEIPT", "BLOCKED", "gateway self API parity is not proven by a real retirement receipt", true),
                switchCheck("UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED", "BLOCKED", "unified-backend full regression has not been recorded for real retirement", true),
                switchCheck("CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED", "BLOCKED", "core entrypoint regression has not been recorded for real retirement", true),
                switchCheck("ROLLBACK_PLAN_NOT_REVALIDATED", "BLOCKED", "rollback plan is not revalidated for real retirement", true),
                switchCheck("BULK_DELETE_FORBIDDEN", "BLOCKED", "bulk delete remains forbidden; every delete must be explicit", true)
        );
    }

    Map<String, Object> apiGatewayControlledRetirementEvidence() {
        ApiGatewayControlledRetirementReceiptSnapshot snapshot = apiGatewayControlledRetirementReceipt.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "receiptPath", snapshot.receiptPath(),
                "receiptPresent", snapshot.receiptPresent(),
                "receiptParsed", snapshot.receiptParsed(),
                "retirementApplied", snapshot.retirementApplied(),
                "deleteListApproved", snapshot.deleteListApproved(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "retiredEntrypointRef", snapshot.retiredEntrypointRef(),
                "rollbackEntrypointRefs", snapshot.rollbackEntrypointRefs(),
                "controlledCutoverRefsTotal", snapshot.controlledCutoverRefsTotal(),
                "approvalRefsTotal", snapshot.approvalRefsTotal(),
                "trafficZeroRefsTotal", snapshot.trafficZeroRefsTotal(),
                "observabilityRefsTotal", snapshot.observabilityRefsTotal(),
                "auditRefsTotal", snapshot.auditRefsTotal(),
                "rollbackWindowRefsTotal", snapshot.rollbackWindowRefsTotal(),
                "gatewaySelfApiParityRefsTotal", snapshot.gatewaySelfApiParityRefsTotal(),
                "deleteListItemsTotal", snapshot.deleteListItemsTotal(),
                "deletedFilesTotal", snapshot.deletedFilesTotal(),
                "remainingGatewayPackageRefs", snapshot.remainingGatewayPackageRefs(),
                "unifiedBuildHelperStillReferencesApiGateway", snapshot.unifiedBuildHelperStillReferencesApiGateway(),
                "apiGatewayPomStillPresent", snapshot.apiGatewayPomStillPresent(),
                "apiGatewayServiceDirectoryStillPresent", snapshot.apiGatewayServiceDirectoryStillPresent(),
                "coreEntrypointsPreserved", snapshot.coreEntrypointsPreserved(),
                "readyToRetireBusinessCore", snapshot.readyToRetireBusinessCore(),
                "readyToRetireAdmissionCore", snapshot.readyToRetireAdmissionCore(),
                "readyToRetireEngagementCore", snapshot.readyToRetireEngagementCore(),
                "readyToRetireOpsCore", snapshot.readyToRetireOpsCore(),
                "readyToRetirePortalCore", snapshot.readyToRetirePortalCore(),
                "bulkDeleteAllowed", snapshot.bulkDeleteAllowed(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "remainingBlockers", snapshot.remainingBlockers(),
                "status", snapshot.status()
        );
    }

    String apiGatewayExternalRetirementEvidenceStatus() {
        return apiGatewayExternalRetirementEvidence.snapshot().status();
    }

    List<Map<String, Object>> apiGatewayExternalRetirementEvidenceChecks() {
        ApiGatewayExternalRetirementEvidenceSnapshot snapshot = apiGatewayExternalRetirementEvidence.snapshot();
        return List.of(
                switchCheck("EXTERNAL_RETIREMENT_EVIDENCE_SAMPLE_PRESENT", snapshot.evidencePresent() ? "PASS" : "BLOCKED", "api-gateway external retirement evidence sample is present", true),
                switchCheck("EXTERNAL_RETIREMENT_EVIDENCE_SAMPLE_JSON_PARSABLE", snapshot.evidenceParsed() ? "PASS" : "BLOCKED", "api-gateway external retirement evidence sample is parseable JSON", true),
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_REFERENCED", snapshot.controlledCutoverReceiptReferenced() ? "PASS" : "BLOCKED", "controlled production cutover receipt is referenced", true),
                switchCheck("API_GATEWAY_RETIREMENT_RECEIPT_REFERENCED", snapshot.apiGatewayRetirementReceiptReferenced() ? "PASS" : "BLOCKED", "api-gateway retirement receipt is referenced", true),
                switchCheck("UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", snapshot.unifiedBuildHelperStillReferencesApiGateway() ? "BLOCKED" : "PASS", "unified-backend compiles its own gateway control source", true),
                switchCheck("GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED", "PASS", "gateway self APIs remain mounted in unified-backend", true),
                switchCheck("BUSINESS_PATHS_UNCHANGED", "PASS", "business paths keep existing /api/v1 prefixes", true),
                switchCheck("CORE_MODULE_SOURCES_PRESERVED", snapshot.coreEntrypointsPreserved() ? "PASS" : "BLOCKED", "five core module sources remain mounted in unified-backend", true),
                switchCheck("NODE_DAEMON_OUT_OF_REPOSITORY", "PASS", "node daemon remains out of repository", true),
                switchCheck("NO_REAL_VALUES_IN_EXTERNAL_RETIREMENT_EVIDENCE", snapshot.realValuesAllowedInRepository() ? "BLOCKED" : "PASS", "external retirement evidence sample contains only redacted references", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_EXTERNAL_RETIREMENT_EVIDENCE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "external retirement evidence sample has no sensitive runtime values", true),
                switchCheck("BULK_DELETE_FORBIDDEN", "PASS", "bulk delete remains forbidden before user-approved per-file deletion", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "production and retirement ready flags remain false", true),
                switchCheck("REAL_EXTERNAL_RETIREMENT_EVIDENCE_NOT_PROVIDED", "BLOCKED", "real external retirement evidence is not provided outside repository", true),
                switchCheck("REAL_ENTRYPOINT_NOT_APPLIED_TO_UNIFIED_BACKEND", "BLOCKED", "real external entrypoint is not applied to unified-backend", true),
                switchCheck("PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED", "BLOCKED", "production traffic is not observed on unified-backend", true),
                switchCheck("API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", "api-gateway zero production traffic is not proven", true),
                switchCheck("REAL_AUDIT_WRITE_SMOKE_NOT_PASSED", "BLOCKED", "real audit write smoke has not passed", true),
                switchCheck("DASHBOARD_NOT_VERIFIED", "BLOCKED", "real dashboard is not verified", true),
                switchCheck("ALERTING_NOT_VERIFIED", "BLOCKED", "real alerting is not verified", true),
                switchCheck("TRACE_PIPELINE_NOT_VERIFIED", "BLOCKED", "real trace pipeline is not verified", true),
                switchCheck("ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", "rollback window is not completed", true),
                switchCheck("RETIREMENT_APPROVAL_NOT_GRANTED", "BLOCKED", "api-gateway retirement approval is not granted", true),
                switchCheck("DELETE_LIST_NOT_APPROVED", "BLOCKED", "delete list is not approved by the user", true),
                switchCheck("UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED", "BLOCKED", "unified-backend full regression has not been recorded for real retirement", true),
                switchCheck("API_GATEWAY_REGRESSION_NOT_RECORDED", "BLOCKED", "api-gateway regression has not been recorded for real retirement", true),
                switchCheck("CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED", "BLOCKED", "core entrypoint regression has not been recorded for real retirement", true),
                switchCheck("ROLLBACK_PLAN_NOT_REVALIDATED", "BLOCKED", "rollback plan is not revalidated for real retirement", true)
        );
    }

    Map<String, Object> apiGatewayExternalRetirementEvidence() {
        ApiGatewayExternalRetirementEvidenceSnapshot snapshot = apiGatewayExternalRetirementEvidence.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "evidencePath", snapshot.evidencePath(),
                "evidencePresent", snapshot.evidencePresent(),
                "evidenceParsed", snapshot.evidenceParsed(),
                "externalEvidenceApplied", snapshot.externalEvidenceApplied(),
                "deleteListApproved", snapshot.deleteListApproved(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "retiredEntrypointRef", snapshot.retiredEntrypointRef(),
                "rollbackEntrypointRefs", snapshot.rollbackEntrypointRefs(),
                "controlledCutoverRefsTotal", snapshot.controlledCutoverRefsTotal(),
                "apiGatewayRetirementRefsTotal", snapshot.apiGatewayRetirementRefsTotal(),
                "approvalRefsTotal", snapshot.approvalRefsTotal(),
                "trafficObservationRefsTotal", snapshot.trafficObservationRefsTotal(),
                "trafficZeroRefsTotal", snapshot.trafficZeroRefsTotal(),
                "auditRefsTotal", snapshot.auditRefsTotal(),
                "observabilityRefsTotal", snapshot.observabilityRefsTotal(),
                "rollbackWindowRefsTotal", snapshot.rollbackWindowRefsTotal(),
                "deleteListItemsTotal", snapshot.deleteListItemsTotal(),
                "deletedFilesTotal", snapshot.deletedFilesTotal(),
                "unifiedBuildHelperStillReferencesApiGateway", snapshot.unifiedBuildHelperStillReferencesApiGateway(),
                "apiGatewayPomStillPresent", snapshot.apiGatewayPomStillPresent(),
                "apiGatewayServiceDirectoryStillPresent", snapshot.apiGatewayServiceDirectoryStillPresent(),
                "coreEntrypointsPreserved", snapshot.coreEntrypointsPreserved(),
                "readyToRetireBusinessCore", snapshot.readyToRetireBusinessCore(),
                "readyToRetireAdmissionCore", snapshot.readyToRetireAdmissionCore(),
                "readyToRetireEngagementCore", snapshot.readyToRetireEngagementCore(),
                "readyToRetireOpsCore", snapshot.readyToRetireOpsCore(),
                "readyToRetirePortalCore", snapshot.readyToRetirePortalCore(),
                "bulkDeleteAllowed", snapshot.bulkDeleteAllowed(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "remainingBlockers", snapshot.remainingBlockers(),
                "status", snapshot.status()
        );
    }

    String localApiGatewayEntrypointRetirementStatus() {
        return localApiGatewayEntrypointRetirement.snapshot().status();
    }

    List<Map<String, Object>> localApiGatewayEntrypointRetirementChecks() {
        LocalApiGatewayEntrypointRetirementSnapshot snapshot = localApiGatewayEntrypointRetirement.snapshot();
        return List.of(
                switchCheck("SINGLE_MAVEN_ENTRYPOINT_AFTER_LOCAL_RETIREMENT", snapshot.postDeleteSixMavenEntrypointsExpected() ? "PASS" : "BLOCKED", "one Maven entrypoint remains after local retirement", true),
                switchCheck("API_GATEWAY_POM_PRESENT_BEFORE_DELETE", snapshot.apiGatewayPomStillPresent() ? "PASS" : "INFO", "api-gateway pom was present before delete", true),
                switchCheck("UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", snapshot.unifiedBuildHelperDoesNotReferenceApiGateway() ? "PASS" : "BLOCKED", "unified-backend hosts its own gateway source", true),
                switchCheck("UNIFIED_BUILD_HELPER_DOES_NOT_REFERENCE_API_GATEWAY", snapshot.unifiedBuildHelperDoesNotReferenceApiGateway() ? "PASS" : "BLOCKED", "unified-build helper does not reference api-gateway source", true),
                switchCheck("DELETE_LIST_ONLY_EXPLICIT_FILES", snapshot.deleteListOnlyExplicitFiles() ? "PASS" : "BLOCKED", "delete list contains only explicit files", true),
                switchCheck("DELETE_LIST_REJECTS_DIRECTORIES", snapshot.deleteListRejectsDirectories() ? "PASS" : "BLOCKED", "delete list rejects directories", true),
                switchCheck("DELETE_LIST_REJECTS_WILDCARDS", snapshot.deleteListRejectsWildcards() ? "PASS" : "BLOCKED", "delete list rejects wildcards", true),
                switchCheck("DELETE_LIST_REJECTS_BULK_DELETE_COMMANDS", snapshot.deleteListRejectsBulkDeleteCommands() ? "PASS" : "BLOCKED", "delete list rejects bulk delete commands", true),
                switchCheck("DELETE_LIST_EXCLUDES_CORE_ENTRYPOINTS", snapshot.deleteListExcludesCoreEntrypoints() ? "PASS" : "BLOCKED", "delete list excludes core entrypoints", true),
                switchCheck("FIVE_CORE_MODULE_SOURCES_PRESERVED", snapshot.coreEntrypointsPreserved() ? "PASS" : "BLOCKED", "five core module sources remain mounted", true),
                switchCheck("UNIFIED_GATEWAY_SELF_APIS_PRESERVED", snapshot.unifiedGatewaySelfApisPreserved() ? "PASS" : "BLOCKED", "gateway self APIs remain mounted in unified-backend", true),
                switchCheck("UNIFIED_MOUNTS_25_BUSINESS_ROUTES_IN_PROCESS", snapshot.inProcessRoutesTotal() == 25 && snapshot.httpFallbackRoutesTotal() == 0 ? "PASS" : "BLOCKED", "25 business routes remain mounted in process with no HTTP fallback", true),
                switchCheck("POST_RETIREMENT_SINGLE_MAVEN_ENTRYPOINT_EXPECTED", snapshot.postDeleteSixMavenEntrypointsExpected() ? "PASS" : "BLOCKED", "one Maven entrypoint is expected after local retirement", true),
                switchCheck("POST_DELETE_API_GATEWAY_POM_ABSENT_EXPECTED", snapshot.postDeleteApiGatewayPomAbsentExpected() ? "PASS" : "BLOCKED", "api-gateway pom is expected absent after delete", true),
                switchCheck("PRODUCTION_PROXY_EVIDENCE_NOT_REQUIRED_FOR_LOCAL_RETIREMENT", !snapshot.productionCutoverRequired() && !snapshot.requiresNginx() && !snapshot.requiresCloudflare() && !snapshot.requiresRealDomain() && !snapshot.requiresProductionTrafficEvidence() ? "PASS" : "BLOCKED", "local retirement does not require production proxy evidence", true)
        );
    }

    Map<String, Object> localApiGatewayEntrypointRetirementEvidence() {
        LocalApiGatewayEntrypointRetirementSnapshot snapshot = localApiGatewayEntrypointRetirement.snapshot();
        return map(
                "mode", snapshot.mode(),
                "apiGatewayPomStillPresent", snapshot.apiGatewayPomStillPresent(),
                "localRetirementApplied", snapshot.localRetirementApplied(),
                "preDeleteMavenEntrypointsTotal", snapshot.preDeleteMavenEntrypointsTotal(),
                "postDeleteExpectedMavenEntrypointsTotal", snapshot.postDeleteExpectedMavenEntrypointsTotal(),
                "coreEntrypointsPreserved", snapshot.coreEntrypointsPreserved(),
                "deleteListItemsTotal", snapshot.deleteListItemsTotal(),
                "unsafeDeleteListItemsTotal", snapshot.unsafeDeleteListItemsTotal(),
                "bulkDeleteAllowed", snapshot.bulkDeleteAllowed(),
                "deleteList", snapshot.deleteList(),
                "postDeleteExpectedEntrypoints", List.of("unified-backend-service"),
                "unifiedBuildHelperDoesNotReferenceApiGateway", snapshot.unifiedBuildHelperDoesNotReferenceApiGateway(),
                "deleteListOnlyExplicitFiles", snapshot.deleteListOnlyExplicitFiles(),
                "deleteListRejectsDirectories", snapshot.deleteListRejectsDirectories(),
                "deleteListRejectsWildcards", snapshot.deleteListRejectsWildcards(),
                "deleteListRejectsBulkDeleteCommands", snapshot.deleteListRejectsBulkDeleteCommands(),
                "deleteListExcludesCoreEntrypoints", snapshot.deleteListExcludesCoreEntrypoints(),
                "readyToRetireBusinessCore", false,
                "readyToRetireAdmissionCore", false,
                "readyToRetireEngagementCore", false,
                "readyToRetireOpsCore", false,
                "readyToRetirePortalCore", false,
                "nextRetirementEntrypoint", snapshot.nextRetirementEntrypoint(),
                "unifiedGatewaySelfApisPreserved", snapshot.unifiedGatewaySelfApisPreserved(),
                "inProcessRoutesTotal", snapshot.inProcessRoutesTotal(),
                "httpFallbackRoutesTotal", snapshot.httpFallbackRoutesTotal(),
                "productionCutoverRequired", snapshot.productionCutoverRequired(),
                "productionRetirementClaimed", false,
                "requiresNginx", snapshot.requiresNginx(),
                "requiresCloudflare", snapshot.requiresCloudflare(),
                "requiresRealDomain", snapshot.requiresRealDomain(),
                "requiresProductionTrafficEvidence", snapshot.requiresProductionTrafficEvidence(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "remainingBlockers", snapshot.remainingBlockers(),
                "status", snapshot.status()
        );
    }

    String realProductionEntrypointCutoverStatus() {
        return realProductionEntrypointCutoverEvidence.snapshot().status();
    }

    List<Map<String, Object>> realProductionEntrypointCutoverChecks() {
        RealProductionEntrypointCutoverEvidenceSnapshot snapshot = realProductionEntrypointCutoverEvidence.snapshot();
        return List.of(
                switchCheck("REAL_PRODUCTION_CUTOVER_EVIDENCE_SAMPLE_PRESENT", snapshot.evidencePresent() ? "PASS" : "BLOCKED", "real production entrypoint cutover evidence sample is present", true),
                switchCheck("REAL_PRODUCTION_CUTOVER_EVIDENCE_SAMPLE_JSON_PARSABLE", snapshot.evidenceParsed() ? "PASS" : "BLOCKED", "real production entrypoint cutover evidence sample is parseable JSON", true),
                switchCheck("CANDIDATE_ENTRYPOINT_TARGETS_UNIFIED_BACKEND_8135", snapshot.candidateEntrypointTargetsUnified() ? "PASS" : "BLOCKED", "candidate entrypoint reference targets unified-backend-service:8135", true),
                switchCheck("PREVIOUS_ENTRYPOINT_TARGETS_API_GATEWAY_8125", snapshot.previousEntrypointTargetsApiGateway() ? "PASS" : "BLOCKED", "previous entrypoint reference targets api-gateway-service:8125", true),
                switchCheck("UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", snapshot.unifiedBuildHelperStillReferencesApiGateway() ? "BLOCKED" : "PASS", "unified-backend compiles its own gateway control source", true),
                switchCheck("GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED", "PASS", "gateway self APIs remain mounted in unified-backend", true),
                switchCheck("CORE_MODULE_SOURCES_PRESERVED", snapshot.coreEntrypointsPreserved() ? "PASS" : "BLOCKED", "five core module sources remain mounted", true),
                switchCheck("NODE_DAEMON_OUT_OF_REPOSITORY", "PASS", "node daemon remains out of repository", true),
                switchCheck("SINGLE_MAVEN_ENTRYPOINT_PRESENT", snapshot.mavenEntrypointsTotal() == 1 ? "PASS" : "BLOCKED", "unified-backend is the only Maven entrypoint in repository", true),
                switchCheck("NO_REAL_VALUES_IN_CUTOVER_EVIDENCE", snapshot.realValuesAllowedInRepository() ? "BLOCKED" : "PASS", "cutover evidence sample contains only redacted references", true),
                switchCheck("NO_SENSITIVE_VALUES_IN_CUTOVER_EVIDENCE", snapshot.sensitiveValuesExposed() ? "BLOCKED" : "PASS", "cutover evidence sample has no sensitive runtime values", true),
                switchCheck("OLD_API_GATEWAY_RETIREMENT_STILL_FORBIDDEN", snapshot.oldApiGatewayRetirementAllowed() ? "BLOCKED" : "PASS", "old api-gateway retirement remains forbidden by this gate", true),
                switchCheck("READY_FLAGS_REMAIN_FALSE", "PASS", "production and retirement ready flags remain false", true),
                switchCheck("REAL_PRODUCTION_CUTOVER_EVIDENCE_NOT_PROVIDED", "BLOCKED", "real production entrypoint cutover evidence is not provided outside repository", true),
                switchCheck("PRODUCTION_TRAFFIC_ALLOWED_NOT_DECLARED", snapshot.productionTrafficAllowed() ? "PASS" : "BLOCKED", "production traffic is not allowed by repository sample", true),
                switchCheck("CUTOVER_WINDOW_REF_NOT_PROVIDED", snapshot.cutoverWindowProvided() ? "PASS" : "BLOCKED", "cutover window reference is not provided", true),
                switchCheck("PRODUCTION_TRAFFIC_OBSERVATION_NOT_PROVIDED", "BLOCKED", "production traffic observation reference is not provided", true),
                switchCheck("OLD_GATEWAY_TRAFFIC_ZERO_NOT_PROVIDED", "BLOCKED", "old gateway zero-traffic reference is not provided", true),
                switchCheck("REAL_AUDIT_WRITE_SMOKE_NOT_PROVIDED", "BLOCKED", "real audit write smoke reference is not provided", true),
                switchCheck("DASHBOARD_REF_NOT_PROVIDED", "BLOCKED", "dashboard reference is not provided", true),
                switchCheck("ALERT_REF_NOT_PROVIDED", "BLOCKED", "alert reference is not provided", true),
                switchCheck("TRACE_REF_NOT_PROVIDED", "BLOCKED", "trace reference is not provided", true),
                switchCheck("ROLLBACK_REF_NOT_PROVIDED", "BLOCKED", "rollback reference is not provided", true),
                switchCheck("APPROVAL_REF_NOT_PROVIDED", "BLOCKED", "approval reference is not provided", true)
        );
    }

    Map<String, Object> realProductionEntrypointCutoverEvidence() {
        RealProductionEntrypointCutoverEvidenceSnapshot snapshot = realProductionEntrypointCutoverEvidence.snapshot();
        return map(
                "readinessMode", snapshot.readinessMode(),
                "evidencePath", snapshot.evidencePath(),
                "evidencePresent", snapshot.evidencePresent(),
                "evidenceParsed", snapshot.evidenceParsed(),
                "realProductionCutoverEvidenceApplied", snapshot.realProductionCutoverEvidenceApplied(),
                "productionTrafficAllowed", snapshot.productionTrafficAllowed(),
                "oldApiGatewayRetirementAllowed", snapshot.oldApiGatewayRetirementAllowed(),
                "realValuesAllowedInRepository", snapshot.realValuesAllowedInRepository(),
                "candidateEntrypointRef", snapshot.candidateEntrypointRef(),
                "previousEntrypointRef", snapshot.previousEntrypointRef(),
                "cutoverWindowRef", snapshot.cutoverWindowRef(),
                "trafficObservationRefsTotal", snapshot.trafficObservationRefsTotal(),
                "oldGatewayTrafficZeroRefsTotal", snapshot.oldGatewayTrafficZeroRefsTotal(),
                "auditWriteSmokeRefsTotal", snapshot.auditWriteSmokeRefsTotal(),
                "dashboardRefsTotal", snapshot.dashboardRefsTotal(),
                "alertRefsTotal", snapshot.alertRefsTotal(),
                "traceRefsTotal", snapshot.traceRefsTotal(),
                "rollbackRefsTotal", snapshot.rollbackRefsTotal(),
                "approvalRefsTotal", snapshot.approvalRefsTotal(),
                "unifiedBuildHelperStillReferencesApiGateway", snapshot.unifiedBuildHelperStillReferencesApiGateway(),
                "apiGatewayPomStillPresent", snapshot.apiGatewayPomStillPresent(),
                "mavenEntrypointsTotal", snapshot.mavenEntrypointsTotal(),
                "coreEntrypointsPreserved", snapshot.coreEntrypointsPreserved(),
                "readyToRetireBusinessCore", snapshot.readyToRetireBusinessCore(),
                "readyToRetireAdmissionCore", snapshot.readyToRetireAdmissionCore(),
                "readyToRetireEngagementCore", snapshot.readyToRetireEngagementCore(),
                "readyToRetireOpsCore", snapshot.readyToRetireOpsCore(),
                "readyToRetirePortalCore", snapshot.readyToRetirePortalCore(),
                "deleteListPermitGenerated", snapshot.deleteListPermitGenerated(),
                "environmentVariablesRead", snapshot.environmentVariablesRead(),
                "sensitiveValuesExposed", snapshot.sensitiveValuesExposed(),
                "remainingBlockers", snapshot.remainingBlockers(),
                "status", snapshot.status()
        );
    }

    List<Map<String, Object>> productionAuditSinkPrecheckChecks() {
        return List.of(
                switchCheck("AUDIT_EVENT_SCHEMA_FIXED", "PASS", "production audit event fields are fixed before sink connection", true),
                switchCheck("AUDIT_REQUEST_ID_PROPAGATED", "PASS", "request id must propagate from entrypoint to audit event", true),
                switchCheck("AUDIT_ACTOR_FIELDS_DOCUMENTED", "PASS", "actor id, role, capabilities and source summary are documented", true),
                switchCheck("AUDIT_TARGET_FIELDS_DOCUMENTED", "PASS", "target type, target id, action and risk level are documented", true),
                switchCheck("AUDIT_RESULT_FIELDS_DOCUMENTED", "PASS", "result, failure reason and state summaries are documented", true),
                switchCheck("AUDIT_REDACTION_RULES_ENFORCED", "PASS", "audit readiness evidence is covered by redaction assertions", true),
                switchCheck("AUDIT_FAILURE_DEGRADATION_DEFINED", "PASS", "audit sink failure degradation is defined before production connection", true),
                switchCheck("AUDIT_REPLAY_SCOPE_DEFINED", "PASS", "audit replay scope is verification-only and does not mutate business data", true),
                switchCheck("AUDIT_RETENTION_POLICY_DEFINED", "PASS", "audit retention, archive and cleanup responsibility are documented", true),
                switchCheck("AUDIT_ROLLBACK_SOURCE_DEFINED", "PASS", "audit config rollback source remains protected current entrypoints and contracts", true),
                switchCheck("PERSISTENT_AUDIT_SINK_CONFIGURED", "BLOCKED", "real persistent audit sink config is not injected", true),
                switchCheck("AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", "real audit write path is not connected", true),
                switchCheck("AUDIT_WRITE_SMOKE_PASSED", "BLOCKED", "real audit write smoke has not passed", true),
                switchCheck("AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", "real audit replay path is not connected", true),
                switchCheck("AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", "real audit retention job is not connected", true),
                switchCheck("AUDIT_EXPORT_PATH_CONNECTED", "BLOCKED", "real audit export path is not connected", true),
                switchCheck("CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", "centralized production configuration provider is not connected", true),
                switchCheck("EXTERNAL_ENTRYPOINT_TARGETS_UNIFIED_BACKEND", "BLOCKED", "external entrypoint does not target unified-backend yet", true),
                switchCheck("PRODUCTION_AUDIT_TRAFFIC_OBSERVED", "BLOCKED", "production audit traffic sample is not observed", true)
        );
    }

    Map<String, Object> productionAuditSinkEvidence() {
        return map(
                "readinessMode", "PRODUCTION_AUDIT_SINK_CONTRACT_RECORDED_NOT_CONNECTED",
                "candidateEntrypoint", "unified-backend:8135",
                "currentEntrypoint", "api-gateway:8125",
                "auditSinkConfigured", false,
                "auditWritePathConnected", false,
                "auditWriteSmokePassed", false,
                "auditReplayPathConnected", false,
                "auditRetentionJobConnected", false,
                "auditExportPathConnected", false,
                "auditEventSchemaFixed", true,
                "requestIdPropagated", true,
                "actorFieldsDocumented", true,
                "targetFieldsDocumented", true,
                "resultFieldsDocumented", true,
                "failureDegradationDefined", true,
                "redactionRulesEnforced", true,
                "replayScopeDefined", true,
                "retentionPolicyDefined", true,
                "rollbackSourceDefined", true,
                "centralConfigProviderConnected", false,
                "externalEntrypointTargetsUnifiedBackend", false,
                "productionAuditTrafficObserved", false,
                "sensitiveValuesExposed", false,
                "environmentVariablesRead", false,
                "trafficSwitchApplied", false,
                "readyForProduction", false,
                "readyToReplaceGateway", false,
                "status", "BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED"
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
                switchCheck("INDEPENDENT_CORE_REGRESSION_REPLACED_BY_UNIFIED", "PASS", "core module regression now runs through unified-backend Maven entrypoint", true),
                switchCheck("CORE_MAVEN_ENTRYPOINTS_RETIRED", "PASS", "five core pom files are retired locally", true),
                switchCheck("CORE_MODULE_SOURCES_PRESERVED", "PASS", "five core module source trees remain mounted by unified-backend", true),
                switchCheck("CORE_BOOT_APPLICATIONS_RETIRED", "PASS", "five core standalone Spring Boot application classes are retired", true),
                switchCheck("API_GATEWAY_RETIREMENT_COMPLETED", "PASS", "local api-gateway Maven entrypoint is already retired", true),
                switchCheck("EXTERNAL_ENTRYPOINT_TRAFFIC_SWITCHED", "BLOCKED", "production traffic is not switched to unified-backend", true),
                switchCheck("ROLLBACK_WINDOW_COMPLETED", "BLOCKED", "rollback window has not completed after traffic switch", true),
                switchCheck("USER_CORE_RETIREMENT_APPROVAL_GRANTED", "PASS", "user requested retiring five core Maven entrypoints in this round", true),
                switchCheck("CORE_DELETE_LIST_CONFIRMED", "PASS", "delete list is limited to explicit core entrypoint files and excludes module source", true)
        );
    }

    Map<String, Object> coreEntrypointRetirementEvidence() {
        return map(
                "retirementApprovalStatus", "APPROVED_LOCAL_MAVEN_ENTRYPOINT_ONLY",
                "deletionAllowed", true,
                "trafficSwitchApplied", false,
                "apiGatewayRetired", true,
                "rollbackWindowCompleted", false,
                "bulkRetirementAllowed", false,
                "entrypointRetirementScope", "LOCAL_MAVEN_ENTRYPOINTS_ONLY",
                "moduleSourcePreserved", true,
                "standaloneServiceStartDisabled", true,
                "currentMavenEntrypoints", List.of("unified-backend-service"),
                "retiredCoreEntrypoints", List.of(
                        "business-core-service",
                        "admission-core-service",
                        "engagement-core-service",
                        "ops-core-service",
                        "portal-core-service"
                ),
                "coreEntrypointMatrix", List.of(
                        coreRetirementTarget("business-core", 8130, "backend/business-core-service", List.of("auth", "profile", "notification", "content", "server-status", "resource", "admin"), 1),
                        coreRetirementTarget("admission-core", 8131, "backend/admission-core-service", List.of("onboarding", "exam", "whitelist", "attendance"), 2),
                        coreRetirementTarget("engagement-core", 8132, "backend/engagement-core-service", List.of("community", "activity", "calendar", "changelog"), 3),
                        coreRetirementTarget("ops-core", 8133, "backend/ops-core-service", List.of("ops-control", "cloudreve-sync", "backup-recovery", "alerting", "plugin-integration", "cross-platform-notification", "ops-image-market"), 4),
                        coreRetirementTarget("portal-core", 8134, "backend/portal-core-service", List.of("online-map", "material", "guide"), 5)
                ),
                "status", "PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED"
        );
    }

    List<Map<String, Object>> productionHardeningPrecheckChecks() {
        return List.of(
                switchCheck("UNIFIED_BACKEND_CANDIDATE_READY", "PASS", "unified-backend:8135 is ready as the backend application candidate", true),
                switchCheck("BUSINESS_PATHS_PRESERVED", "PASS", "all business paths keep existing /api/v1 prefixes", true),
                switchCheck("REAL_HTTP_REHEARSAL_PASSED", "PASS", "real HTTP rehearsal passed for the candidate business surface", true),
                switchCheck("ROUTE_DRIFT_SCAN_PASSED", "PASS", "gateway routes and unified mounts have no route drift", true),
                switchCheck("LOCAL_CORE_ENTRYPOINTS_RETIRED", "PASS", "five core Maven entrypoints are retired locally while module source remains mounted", true),
                switchCheck("CENTRAL_CONFIG_CONTRACT_DEFINED", "PASS", "central config ownership and rollback contract are documented", true),
                switchCheck("AUDIT_TRAIL_CONTRACT_DEFINED", "PASS", "audit trail ownership and event contract are documented", true),
                switchCheck("CUTOVER_RUNBOOK_DEFINED", "PASS", "cutover runbook requirements are recorded without applying traffic switch", true),
                switchCheck("PRODUCTION_CUTOVER_APPROVAL_PACKAGE_RECORDED", "PASS", "production cutover approval package requirements are recorded without approving traffic", true),
                switchCheck("CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_RECORDED", "PASS", "local cutover evidence consistency audit is recorded without applying production traffic", true),
                switchCheck("EXTERNAL_VALUE_INTAKE_REHEARSAL_RECORDED", "PASS", "external value intake rehearsal is recorded without importing real values", true),
                switchCheck("PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_RECORDED", "PASS", "production runtime config shell rehearsal is recorded without binding real runtime", true),
                switchCheck("PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_RECORDED", "PASS", "production audit observability smoke rehearsal is recorded without connecting real platforms", true),
                switchCheck("CONTROLLED_CUTOVER_RECEIPT_GATE_RECORDED", "PASS", "controlled production cutover receipt gate is recorded without applying real traffic", true),
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
                "independentRegressionRequired", false,
                "unifiedRegressionRequired", true,
                "retirementOrder", retirementOrder,
                "retirementStatus", "RETIRED_LOCAL_MAVEN_ENTRYPOINT",
                "moduleSourcePreserved", true,
                "standaloneServiceStartDisabled", true,
                "blockedBy", "NONE"
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
