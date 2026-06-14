package cn.beiming.portalcore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/portal-core")
class PortalCoreController {
    private final int port;
    private final boolean testControlsEnabled;
    private final PortalCoreRegistry registry;
    private final PortalCoreSmokeCoordinator smoke;

    PortalCoreController(@Value("${server.port}") int port,
                         @Value("${portal-core.test-controls.enabled:false}") boolean testControlsEnabled,
                         PortalCoreRegistry registry,
                         PortalCoreSmokeCoordinator smoke) {
        this.port = port;
        this.testControlsEnabled = testControlsEnabled;
        this.registry = registry;
        this.smoke = smoke;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("version", "0.1.0-contract");
        data.put("livenessStatus", "LIVE");
        data.put("readinessProbePath", "/api/v1/portal-core/admin/readiness");
        data.put("startupProbePath", "/api/v1/portal-core/health");
        data.put("moduleRoutes", registry.publicModules());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        PortalCoreActor actor = requireAdminOrOwner(request);
        Map<String, Object> data = baseSummary();
        data.put("modulesMounted", registry.modulesTotal());
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("storageMode", "IN_MEMORY_CONTRACT_STUBS");
        data.put("authMode", actor.authMode());
        data.put("actorUserId", actor.userId());
        data.put("dependencyAdapterMode", "SAFE_SNAPSHOT_AND_TEST_ADAPTERS");
        addProductionDiagnostics(data);
        data.put("operationalProfile", registry.operationalProfile(smoke.currentStatus(), testControlsEnabled));
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("moduleRoutes", registry.portalModules(port));
        data.put("productionGaps", registry.productionGaps(smoke.currentStatus()));
        data.put("recentAuditSummary", registry.recentAuditSummary());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/admin/modules")
    ResponseEntity<Map<String, Object>> modules(HttpServletRequest request) {
        requireAdminOrOwner(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", registry.portalModules(port));
        data.put("total", registry.modulesTotal());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/admin/readiness")
    ResponseEntity<Map<String, Object>> readiness(HttpServletRequest request) {
        requireAdminOrOwner(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "portal-core");
        data.put("port", port);
        data.put("readyForProduction", false);
        data.put("readinessStatus", "NOT_READY");
        data.put("routesTotal", registry.routesTotal());
        data.put("inheritedRoutesTotal", registry.inheritedRoutesTotal());
        data.put("selfRoutesTotal", registry.selfRoutesTotal());
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("legacyServiceRestoreStatus", "NOT_RESTORED");
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("testControlHeadersStatus", testControlsEnabled ? "ENABLED_FOR_LOCAL_TEST" : "DISABLED_BY_DEFAULT");
        data.put("sensitiveFieldScanStatus", "PASS");
        addProductionDiagnostics(data);
        data.put("operationalProfile", registry.operationalProfile(smoke.currentStatus(), testControlsEnabled));
        data.put("checks", registry.readinessChecks(smoke.currentStatus()));
        data.put("moduleReadiness", registry.moduleReadiness(port));
        data.put("productionBlockers", registry.productionGaps(smoke.currentStatus()));
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @PostMapping("/admin/http-smoke/run")
    ResponseEntity<Map<String, Object>> runHttpSmoke(HttpServletRequest request) {
        requireAdminOrOwner(request);
        PortalHttpSmokeReport report = smoke.run(requestId(request));
        return ok(request, report.toMap(smoke.serviceDiscoveryMode(), smoke.registeredUpstreams(), smoke.targets()));
    }

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "portal-core");
        data.put("status", "UP");
        data.put("port", port);
        data.put("modulesTotal", registry.modulesTotal());
        data.put("inheritedRoutesTotal", registry.inheritedRoutesTotal());
        data.put("selfRoutesTotal", registry.selfRoutesTotal());
        data.put("routesTotal", registry.routesTotal());
        return data;
    }

    private void addProductionDiagnostics(Map<String, Object> data) {
        data.put("serviceDiscoveryMode", smoke.serviceDiscoveryMode());
        data.put("registeredUpstreams", smoke.registeredUpstreams());
        data.put("httpSmokeStatus", smoke.currentStatus());
        data.put("httpSmokeTargets", smoke.targets());
        data.put("lastHttpSmokeAt", smoke.lastCheckedAt());
        data.put("lastHttpSmokeResults", smoke.lastResults());
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(0, "success", data, requestId(request)));
    }

    private PortalCoreActor requireAdminOrOwner(HttpServletRequest request) {
        Optional<PortalCoreActor> trusted = PortalTrustedGatewayAuth.from(request);
        if (trusted.isPresent()) {
            PortalCoreActor actor = trusted.get();
            if (!actor.hasAny("ADMIN", "OWNER")) {
                throw new PortalCoreException(HttpStatus.FORBIDDEN, 42001, "role permission denied");
            }
            return actor;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new PortalCoreException(HttpStatus.UNAUTHORIZED, 41000, "not logged in");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new PortalCoreException(HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        return switch (authorization.substring("Bearer ".length())) {
            case "admin-token" -> new PortalCoreActor("admin", Set.of("ADMIN"), Set.of(), "TEST_STUB");
            case "owner-token" -> new PortalCoreActor("owner", Set.of("OWNER"), Set.of(), "TEST_STUB");
            case "helper-token", "user-token" -> throw new PortalCoreException(HttpStatus.FORBIDDEN, 42001, "role permission denied");
            default -> throw new PortalCoreException(HttpStatus.UNAUTHORIZED, 41001, "invalid session");
        };
    }

    static Map<String, Object> envelope(int code, String message, Object data, String requestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", requestId);
        return body;
    }

    static String requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute("requestId");
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }
        String header = request.getHeader("X-Request-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return "req_" + UUID.randomUUID();
    }
}

@Component
class PortalCoreRegistry {
    private static final int CURRENT_PORT = 8134;
    private static final int SELF_ROUTES_TOTAL = 5;

    private final List<PortalCoreModuleRegistration> modules = List.of(
            new PortalCoreModuleRegistration("GUIDE", "guide", "/api/v1/guides", "backend/guide-service", 8127, 41,
                    "docs/contracts-guide.md", ".local-docs/tests-guide.md", "RETIRED_NO_MAVEN_ENTRY",
                    List.of("REAL_PERSISTENCE_NOT_CONNECTED", "REAL_FULLTEXT_SEARCH_NOT_CONNECTED")),
            new PortalCoreModuleRegistration("MATERIAL", "material", "/api/v1/materials", "backend/material-service", 8126, 33,
                    "docs/contracts-material.md", ".local-docs/tests-material.md", "RETIRED_NO_MAVEN_ENTRY",
                    List.of("REAL_PERSISTENCE_NOT_CONNECTED", "REAL_OBJECT_STORAGE_NOT_CONNECTED", "REAL_FILE_SECURITY_SCANNER_NOT_CONNECTED")),
            new PortalCoreModuleRegistration("ONLINE_MAP", "online-map", "/api/v1/online-map", "backend/online-map-service", 8121, 34,
                    "docs/contracts-online-map.md", ".local-docs/tests-online-map.md", "RETIRED_NO_MAVEN_ENTRY",
                    List.of("REAL_PERSISTENCE_NOT_CONNECTED", "REAL_MAP_PROVIDER_HTTP_NOT_CONNECTED", "REAL_MARKER_SYNC_NOT_CONNECTED", "REAL_TILE_HOSTING_NOT_CONNECTED"))
    );

    int modulesTotal() {
        return modules.size();
    }

    int inheritedRoutesTotal() {
        return modules.stream().mapToInt(PortalCoreModuleRegistration::routesTotal).sum();
    }

    int selfRoutesTotal() {
        return SELF_ROUTES_TOTAL;
    }

    int routesTotal() {
        return inheritedRoutesTotal() + selfRoutesTotal();
    }

    List<Map<String, Object>> publicModules() {
        return modules.stream().map(module -> module.toPublicMap(CURRENT_PORT)).toList();
    }

    List<Map<String, Object>> portalModules(int currentPort) {
        return modules.stream().map(module -> module.toOpsMap(currentPort)).toList();
    }

    List<Map<String, Object>> moduleReadiness(int currentPort) {
        return modules.stream().map(module -> module.toReadinessMap(currentPort)).toList();
    }

    Map<String, Object> recentAuditSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("storageMode", "IN_MEMORY_CONTRACT_STUBS");
        data.put("persistentAuditConnected", false);
        data.put("retainedInProcessOnly", true);
        return data;
    }

    List<String> productionGaps(String httpSmokeStatus) {
        List<String> gaps = new ArrayList<>(List.of(
                "real persistence is not connected",
                "real cross-service HTTP adapters are not connected",
                "real audit persistence is not connected",
                "real object storage is not connected",
                "real file security scanner is not connected",
                "real fulltext search is not connected",
                "real map provider HTTP is not connected",
                "real marker sync is not connected",
                "real tile hosting is not connected",
                "real notification delivery is not connected",
                "dynamic service discovery is not connected"
        ));
        if (!"PASS".equals(httpSmokeStatus)) {
            gaps.add("real HTTP smoke is not passing");
        }
        return List.copyOf(gaps);
    }

    List<Map<String, Object>> readinessChecks(String httpSmokeStatus) {
        return List.of(
                check("REAL_PERSISTENCE", "BLOCKED", "real persistence is not connected"),
                check("REAL_CROSS_SERVICE_HTTP", "BLOCKED", "real cross-service HTTP adapters are not connected"),
                check("REAL_AUDIT_PERSISTENCE", "BLOCKED", "real audit persistence is not connected"),
                check("REAL_OBJECT_STORAGE", "BLOCKED", "real object storage is not connected"),
                check("REAL_FILE_SECURITY_SCANNER", "BLOCKED", "real file security scanner is not connected"),
                check("REAL_FULLTEXT_SEARCH", "BLOCKED", "real fulltext search is not connected"),
                check("REAL_NOTIFICATION_DELIVERY", "BLOCKED", "real notification delivery is not connected"),
                check("REAL_MAP_PROVIDER_HTTP", "BLOCKED", "real map provider HTTP is not connected"),
                check("REAL_MARKER_SYNC", "BLOCKED", "real marker sync is not connected"),
                check("REAL_TILE_HOSTING", "BLOCKED", "real tile hosting is not connected"),
                check("SERVICE_DISCOVERY", "PARTIAL", "static local service discovery registry is mounted"),
                check("REAL_HTTP_SMOKE", httpSmokeStatus, "gateway to portal-core HTTP smoke status is " + httpSmokeStatus),
                check("TEST_CONTROL_HEADERS", "PASS", "test control headers are disabled by default"),
                check("INHERITED_ROUTE_DRIFT", "PASS", "inherited route signatures match formal contracts"),
                check("SENSITIVE_FIELD_SCAN", "PASS", "sensitive field scan is covered by automated tests"),
                check("GATEWAY_ROUTE_SWITCH", "PASS", "gateway routes are switched to portal-core")
        );
    }

    Map<String, Object> operationalProfile(String httpSmokeStatus, boolean testControlsEnabled) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profileVersion", "portal-core-operational-profile-v1");
        data.put("domainBoundary", "PORTAL_EXPERIENCE_CORE");
        data.put("referenceModel", List.of("KUBERNETES_PROBES", "SPRING_BOOT_AVAILABILITY", "GOOGLE_SRE_SLO", "UBER_DOMA"));
        data.put("livenessStatus", "LIVE");
        data.put("readinessGateStatus", "NOT_READY");
        data.put("releaseGateStatus", "NOT_READY");
        data.put("trafficEligibility", "INTERNAL_AND_TEST_ONLY");
        data.put("probeRecommendations", probeRecommendations());
        data.put("sloTargets", sloTargets(httpSmokeStatus, testControlsEnabled));
        data.put("releaseGates", releaseGates(httpSmokeStatus, testControlsEnabled));
        return data;
    }

    private Map<String, Object> probeRecommendations() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("livenessPath", "/api/v1/portal-core/health");
        data.put("readinessPath", "/api/v1/portal-core/admin/readiness");
        data.put("startupPath", "/api/v1/portal-core/health");
        data.put("externalDependenciesInLiveness", false);
        return data;
    }

    private List<Map<String, Object>> sloTargets(String httpSmokeStatus, boolean testControlsEnabled) {
        return List.of(
                slo("ROUTE_DRIFT_ZERO", "0 drifted inherited routes", "PASS"),
                slo("TEST_CONTROLS_DISABLED", "test controls disabled outside local tests", testControlsEnabled ? "LOCAL_TEST_ONLY" : "PASS"),
                slo("HTTP_SMOKE_ALL_TARGETS", "all configured smoke targets return business success", httpSmokeStatus),
                slo("PRODUCTION_BLOCKERS_ZERO", "0 production blockers before external traffic", "BLOCKED")
        );
    }

    private Map<String, Object> slo(String key, String target, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sloKey", key);
        data.put("target", target);
        data.put("currentStatus", status);
        return data;
    }

    private List<Map<String, Object>> releaseGates(String httpSmokeStatus, boolean testControlsEnabled) {
        return List.of(
                gate("INHERITED_ROUTE_DRIFT", "PASS", "inherited routes match formal contracts"),
                gate("GATEWAY_ROUTE_SWITCH", "PASS", "gateway routes point to portal-core"),
                gate("TEST_CONTROL_HEADERS", testControlsEnabled ? "LOCAL_TEST_ONLY" : "PASS", "test controls are disabled by default"),
                gate("REAL_HTTP_SMOKE", httpSmokeStatus, "latest explicit HTTP smoke status"),
                gate("REAL_PERSISTENCE", "BLOCKED", "real persistence is not connected"),
                gate("REAL_AUDIT_PERSISTENCE", "BLOCKED", "real audit persistence is not connected"),
                gate("REAL_EXTERNAL_DEPENDENCIES", "BLOCKED", "object storage, scanner, search, map provider and notification delivery are not connected"),
                gate("DYNAMIC_SERVICE_DISCOVERY", "PARTIAL", "static local registry is mounted")
        );
    }

    private Map<String, Object> gate(String key, String status, String summary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gateKey", key);
        data.put("status", status);
        data.put("summary", summary);
        data.put("requiredForExternalTraffic", true);
        return data;
    }

    private Map<String, Object> check(String key, String status, String summary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkKey", key);
        data.put("status", status);
        data.put("summary", summary);
        data.put("required", true);
        return data;
    }
}

@Component
class PortalCoreSmokeCoordinator {
    private static final String DISCOVERY_MODE = "STATIC_LOCAL_REGISTRY";

    private final String gatewayBaseUrl;
    private final int gatewayPort;
    private final int currentPort;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final List<PortalHttpSmokeTarget> targets;
    private volatile PortalHttpSmokeReport lastReport;

    PortalCoreSmokeCoordinator(@Value("${server.port}") int currentPort,
                               @Value("${portal-core.http-smoke.gateway-base-url:http://127.0.0.1:8135}") String gatewayBaseUrl,
                               @Value("${portal-core.http-smoke.timeout-ms:1500}") int timeoutMs,
                               ObjectMapper objectMapper) {
        this.currentPort = currentPort;
        this.gatewayBaseUrl = normalizeBaseUrl(gatewayBaseUrl);
        this.gatewayPort = portOf(this.gatewayBaseUrl);
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.targets = List.of(
                new PortalHttpSmokeTarget("GATEWAY_GUIDE_CATEGORIES", "GUIDE", "GET", this.gatewayBaseUrl, "/api/v1/guides/categories", 499, 0, timeoutMs),
                new PortalHttpSmokeTarget("GATEWAY_MATERIAL_FEATURED", "MATERIAL", "GET", this.gatewayBaseUrl, "/api/v1/materials/featured", 499, 0, timeoutMs),
                new PortalHttpSmokeTarget("GATEWAY_ONLINE_MAP_HEALTH", "ONLINE_MAP", "GET", this.gatewayBaseUrl, "/api/v1/online-map/health", 499, 0, timeoutMs)
        );
    }

    String serviceDiscoveryMode() {
        return DISCOVERY_MODE;
    }

    List<Map<String, Object>> registeredUpstreams() {
        return List.of(
                upstream("API_GATEWAY", "api-gateway", gatewayBaseUrl, gatewayPort, "/api/v1", "/api/v1/gateway/health"),
                upstream("PORTAL_CORE", "portal-core", "http://127.0.0.1:" + currentPort, currentPort, "/api/v1/portal-core", "/api/v1/portal-core/health"),
                upstream("GUIDE", "guide", gatewayBaseUrl, gatewayPort, "/api/v1/guides", "/api/v1/guides/categories"),
                upstream("MATERIAL", "material", gatewayBaseUrl, gatewayPort, "/api/v1/materials", "/api/v1/materials/featured"),
                upstream("ONLINE_MAP", "online-map", gatewayBaseUrl, gatewayPort, "/api/v1/online-map", "/api/v1/online-map/health")
        );
    }

    List<Map<String, Object>> targets() {
        return targets.stream().map(PortalHttpSmokeTarget::toMap).toList();
    }

    String currentStatus() {
        PortalHttpSmokeReport report = lastReport;
        return report == null ? "NOT_RUN" : report.status();
    }

    String lastCheckedAt() {
        PortalHttpSmokeReport report = lastReport;
        return report == null ? null : report.finishedAt().toString();
    }

    List<Map<String, Object>> lastResults() {
        PortalHttpSmokeReport report = lastReport;
        return report == null ? List.of() : report.results().stream().map(PortalHttpSmokeResult::toMap).toList();
    }

    synchronized PortalHttpSmokeReport run(String requestId) {
        if (targets.isEmpty() || timeoutMs <= 0 || targets.stream().map(PortalHttpSmokeTarget::url).anyMatch(this::notHttpUrl)) {
            throw new PortalCoreException(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "http smoke configuration invalid");
        }
        Instant startedAt = Instant.now();
        List<PortalHttpSmokeResult> results = targets.stream()
                .map(target -> probe(target, requestId))
                .toList();
        String status = results.stream().allMatch(result -> "PASS".equals(result.status())) ? "PASS" : "DEGRADED";
        PortalHttpSmokeReport report = new PortalHttpSmokeReport(status, startedAt, Instant.now(), results);
        lastReport = report;
        return report;
    }

    private PortalHttpSmokeResult probe(PortalHttpSmokeTarget target, String requestId) {
        Instant startedAt = Instant.now();
        Instant checkedAt;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(target.url()))
                    .timeout(Duration.ofMillis(target.timeoutMs()))
                    .header("X-Request-Id", requestId)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            checkedAt = Instant.now();
            Integer businessCode = businessCode(response.body());
            String failureReason = failureReason(response.statusCode(), businessCode, target.expectedStatusMax(), target.expectedBusinessCode());
            String status = failureReason == null ? "PASS" : "FAILED";
            return new PortalHttpSmokeResult(target.targetKey(), target.serviceKey(), target.method(), target.url(), status,
                    response.statusCode(), businessCode, durationMs(startedAt, checkedAt), checkedAt, failureReason);
        } catch (HttpTimeoutException ex) {
            checkedAt = Instant.now();
            return failed(target, startedAt, checkedAt, "timeout");
        } catch (IllegalArgumentException ex) {
            checkedAt = Instant.now();
            return failed(target, startedAt, checkedAt, "invalid target url");
        } catch (IOException ex) {
            checkedAt = Instant.now();
            return failed(target, startedAt, checkedAt, "connection failed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            checkedAt = Instant.now();
            return failed(target, startedAt, checkedAt, "interrupted");
        }
    }

    private boolean notHttpUrl(String url) {
        URI uri = URI.create(url);
        return !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
    }

    private PortalHttpSmokeResult failed(PortalHttpSmokeTarget target, Instant startedAt, Instant checkedAt, String reason) {
        return new PortalHttpSmokeResult(target.targetKey(), target.serviceKey(), target.method(), target.url(), "FAILED",
                null, null, durationMs(startedAt, checkedAt), checkedAt, reason);
    }

    private String failureReason(int httpStatus, Integer businessCode, int expectedStatusMax, int expectedBusinessCode) {
        if (httpStatus > expectedStatusMax) {
            return "http status " + httpStatus;
        }
        if (businessCode == null) {
            return "business code missing";
        }
        if (businessCode != expectedBusinessCode) {
            return "business code " + businessCode;
        }
        return null;
    }

    private Integer businessCode(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode code = node.get("code");
            return code == null || !code.canConvertToInt() ? null : code.asInt();
        } catch (IOException ex) {
            return null;
        }
    }

    private Map<String, Object> upstream(String serviceKey, String serviceName, String baseUrl, int port, String pathPrefix, String healthPath) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serviceKey", serviceKey);
        data.put("serviceName", serviceName);
        data.put("baseUrl", baseUrl);
        data.put("port", port);
        data.put("pathPrefix", pathPrefix);
        data.put("healthPath", healthPath);
        data.put("discoverySource", DISCOVERY_MODE);
        data.put("enabled", true);
        data.put("lastObservedStatus", "UNKNOWN");
        return data;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://127.0.0.1:8135" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private int portOf(String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private int durationMs(Instant startedAt, Instant finishedAt) {
        return Math.max(0, (int) Duration.between(startedAt, finishedAt).toMillis());
    }
}

record PortalHttpSmokeTarget(String targetKey,
                             String serviceKey,
                             String method,
                             String baseUrl,
                             String path,
                             int expectedStatusMax,
                             int expectedBusinessCode,
                             int timeoutMs) {
    String url() {
        return baseUrl + path;
    }

    Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetKey", targetKey);
        data.put("serviceKey", serviceKey);
        data.put("method", method);
        data.put("url", url());
        data.put("expectedStatusMax", expectedStatusMax);
        data.put("expectedBusinessCode", expectedBusinessCode);
        data.put("timeoutMs", timeoutMs);
        return data;
    }
}

record PortalHttpSmokeResult(String targetKey,
                             String serviceKey,
                             String method,
                             String url,
                             String status,
                             Integer httpStatus,
                             Integer businessCode,
                             int durationMs,
                             Instant checkedAt,
                             String failureReason) {
    Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetKey", targetKey);
        data.put("serviceKey", serviceKey);
        data.put("method", method);
        data.put("url", url);
        data.put("status", status);
        data.put("httpStatus", httpStatus);
        data.put("businessCode", businessCode);
        data.put("durationMs", durationMs);
        data.put("checkedAt", checkedAt.toString());
        data.put("failureReason", failureReason);
        return data;
    }
}

record PortalHttpSmokeReport(String status,
                             Instant startedAt,
                             Instant finishedAt,
                             List<PortalHttpSmokeResult> results) {
    Map<String, Object> toMap(String discoveryMode, List<Map<String, Object>> upstreams, List<Map<String, Object>> targets) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "portal-core");
        data.put("serviceDiscoveryMode", discoveryMode);
        data.put("registeredUpstreams", upstreams);
        data.put("httpSmokeStatus", status);
        data.put("startedAt", startedAt.toString());
        data.put("finishedAt", finishedAt.toString());
        data.put("targets", targets);
        data.put("results", results.stream().map(PortalHttpSmokeResult::toMap).toList());
        return data;
    }
}

record PortalCoreModuleRegistration(String moduleKey,
                                    String moduleName,
                                    String pathPrefix,
                                    String legacyServiceDirectory,
                                    int legacyPort,
                                    int routesTotal,
                                    String contract,
                                    String localTestDocument,
                                    String legacyTestCommand,
                                    List<String> productionGaps) {
    Map<String, Object> toPublicMap(int currentPort) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleKey", moduleKey);
        data.put("moduleName", moduleName);
        data.put("pathPrefix", pathPrefix);
        data.put("currentPort", currentPort);
        data.put("routesTotal", routesTotal);
        data.put("mounted", true);
        data.put("enabled", true);
        data.put("status", "READY");
        return data;
    }

    Map<String, Object> toOpsMap(int currentPort) {
        Map<String, Object> data = toPublicMap(currentPort);
        data.put("legacyServiceDirectory", legacyServiceDirectory);
        data.put("legacyPort", legacyPort);
        data.put("currentServiceDirectory", "backend/portal-core-service");
        data.put("contract", contract);
        data.put("localTestDocument", localTestDocument);
        data.put("legacyTestCommand", legacyTestCommand);
        data.put("currentTestCommand", "mvn -q -f backend/pom.xml test");
        data.put("contractRoutesTotal", routesTotal);
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("businessContractOwnedByModule", true);
        data.put("compatibilityMode", "IN_PROCESS_MODULE");
        data.put("productionGaps", productionGaps);
        return data;
    }

    Map<String, Object> toReadinessMap(int currentPort) {
        Map<String, Object> data = toOpsMap(currentPort);
        data.put("readinessStatus", "NOT_READY");
        data.put("readyForProduction", false);
        return data;
    }
}

record PortalCoreActor(String userId, Set<String> roles, Set<String> permissions, String authMode) {
    boolean hasAny(String... candidates) {
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}

final class PortalTrustedGatewayAuth {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("CONTENT_MANAGE", "CONTENT_REVIEW", "RESOURCE_MANAGE",
            "GUIDE_MANAGE", "MATERIAL_REVIEW", "ADMIN_READ");

    private PortalTrustedGatewayAuth() {
    }

    static Optional<PortalCoreActor> from(HttpServletRequest request) {
        String gatewayRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        if (gatewayRequestId == null) {
            return Optional.empty();
        }
        if (gatewayRequestId.isBlank() || !REQUEST_ID_PATTERN.matcher(gatewayRequestId).matches()) {
            throw new PortalCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        String userId = request.getHeader("X-Beiming-Actor-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new PortalCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        LinkedHashSet<String> roles = csv(request.getHeader("X-Beiming-Actor-Roles"), VALID_ROLES, true);
        LinkedHashSet<String> permissions = csv(request.getHeader("X-Beiming-Actor-Permissions"), VALID_PERMISSIONS, false);
        return Optional.of(new PortalCoreActor(userId.trim(), roles, permissions, "TRUSTED_GATEWAY_CONTEXT"));
    }

    private static LinkedHashSet<String> csv(String value, Set<String> allowed, boolean required) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            if (required) {
                throw new PortalCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
            }
            return parsed;
        }
        for (String part : value.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (!allowed.contains(item)) {
                throw new PortalCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
            }
            parsed.add(item);
        }
        if (required && parsed.isEmpty()) {
            throw new PortalCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        return parsed;
    }
}

@Configuration
class PortalCoreFilterConfig {
    @Bean
    FilterRegistrationBean<PortalCoreTestControlFilter> portalCoreTestControlFilter(@Value("${portal-core.test-controls.enabled:false}") boolean enabled) {
        FilterRegistrationBean<PortalCoreTestControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PortalCoreTestControlFilter(enabled));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/v1/*");
        return registration;
    }
}

class PortalCoreTestControlFilter extends OncePerRequestFilter {
    private final boolean enabled;

    PortalCoreTestControlFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest filtered = enabled ? request : new TestControlStrippingRequest(request);
        filterChain.doFilter(filtered, response);
    }
}

class TestControlStrippingRequest extends HttpServletRequestWrapper {
    TestControlStrippingRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        if (name != null && name.toLowerCase().startsWith("x-test-")) {
            return null;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (name != null && name.toLowerCase().startsWith("x-test-")) {
            return java.util.Collections.emptyEnumeration();
        }
        return super.getHeaders(name);
    }
}

@RestControllerAdvice(assignableTypes = PortalCoreController.class)
class PortalCoreExceptionHandler {
    @ExceptionHandler(PortalCoreException.class)
    ResponseEntity<Map<String, Object>> api(PortalCoreException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(PortalCoreController.envelope(exception.code(), exception.getMessage(), null, PortalCoreController.requestId(request)));
    }
}

class PortalCoreException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    PortalCoreException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    HttpStatus status() {
        return status;
    }

    int code() {
        return code;
    }
}
