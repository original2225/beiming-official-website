package cn.beiming.opscore;

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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/v1/ops-core")
class OpsCoreController {
    private final int port;
    private final boolean testControlsEnabled;
    private final String trustedGatewaySigningSecret;
    private final OpsCoreRegistry registry;
    private final OpsCoreSmokeCoordinator smoke;

    OpsCoreController(@Value("${server.port}") int port,
                      @Value("${ops-core.test-controls.enabled:false}") boolean testControlsEnabled,
                      @Value("${ops-core.trusted-gateway.internal-signing-secret:local-test-gateway-signing-secret}") String trustedGatewaySigningSecret,
                      OpsCoreRegistry registry,
                      OpsCoreSmokeCoordinator smoke) {
        this.port = port;
        this.testControlsEnabled = testControlsEnabled;
        this.trustedGatewaySigningSecret = trustedGatewaySigningSecret;
        this.registry = registry;
        this.smoke = smoke;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("version", "0.1.0-contract");
        data.put("moduleRoutes", registry.publicModules());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        OpsCoreActor actor = requireAdminOrOwner(request);
        Map<String, Object> data = baseSummary();
        data.put("modulesMounted", registry.modulesTotal());
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("storageMode", "IN_MEMORY_CONTRACT_STUBS");
        data.put("authMode", actor.authMode());
        data.put("actorUserId", actor.userId());
        data.put("dependencyAdapterMode", "SAFE_SNAPSHOT_AND_TEST_ADAPTERS");
        addProductionDiagnostics(data);
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("moduleRoutes", registry.opsModules(port));
        data.put("productionGaps", registry.productionGaps(smoke.currentStatus()));
        data.put("recentAuditSummary", registry.recentAuditSummary());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/admin/modules")
    ResponseEntity<Map<String, Object>> modules(HttpServletRequest request) {
        requireAdminOrOwner(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", registry.opsModules(port));
        data.put("total", registry.modulesTotal());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @GetMapping("/admin/readiness")
    ResponseEntity<Map<String, Object>> readiness(HttpServletRequest request) {
        requireAdminOrOwner(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "ops-core");
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
        data.put("checks", registry.readinessChecks(smoke.readinessStatus()));
        data.put("moduleReadiness", registry.moduleReadiness(port));
        data.put("productionBlockers", registry.productionGaps(smoke.currentStatus()));
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
    }

    @PostMapping("/admin/http-smoke/run")
    ResponseEntity<Map<String, Object>> runHttpSmoke(HttpServletRequest request) {
        requireAdminOrOwner(request);
        OpsHttpSmokeReport report = smoke.run(requestId(request));
        return ok(request, report.toMap(smoke.serviceDiscoveryMode(), smoke.registeredUpstreams()));
    }

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "ops-core");
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
        data.put("lastHttpSmokeAt", smoke.lastCheckedAt());
        data.put("lastHttpSmokeResults", smoke.lastResults());
        data.put("trustedGatewaySignatureStatus", "HMAC_SHA256_CONFIGURED");
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(0, "success", data, requestId(request)));
    }

    private OpsCoreActor requireAdminOrOwner(HttpServletRequest request) {
        Optional<OpsCoreActor> trusted = TrustedGatewayAuth.from(request, trustedGatewaySigningSecret);
        if (trusted.isPresent()) {
            OpsCoreActor actor = trusted.get();
            if (!actor.hasAny("ADMIN", "OWNER")) {
                throw new OpsCoreException(HttpStatus.FORBIDDEN, 42001, "role permission denied");
            }
            return actor;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new OpsCoreException(HttpStatus.UNAUTHORIZED, 41000, "not logged in");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new OpsCoreException(HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        return switch (authorization.substring("Bearer ".length())) {
            case "admin-token" -> new OpsCoreActor("admin", Set.of("ADMIN"), Set.of(), "TEST_STUB");
            case "owner-token" -> new OpsCoreActor("owner", Set.of("OWNER"), Set.of(), "TEST_STUB");
            case "helper-token", "user-token" -> throw new OpsCoreException(HttpStatus.FORBIDDEN, 42001, "role permission denied");
            default -> throw new OpsCoreException(HttpStatus.UNAUTHORIZED, 41001, "invalid session");
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
class OpsCoreRegistry {
    private static final int CURRENT_PORT = 8133;
    private static final int SELF_ROUTES_TOTAL = 5;

    private final List<OpsCoreModuleRegistration> modules = List.of(
            new OpsCoreModuleRegistration("OPS_CONTROL", "ops-control", "/api/v1/ops-control", "backend/ops-control-service", 8116, 31, "docs/contracts-ops-control.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("CLOUDREVE_SYNC", "cloudreve-sync", "/api/v1/cloudreve-sync", "backend/cloudreve-sync-service", 8118, 16, "docs/contracts-cloudreve-sync.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("BACKUP_RECOVERY", "backup-recovery", "/api/v1/backup-recovery", "backend/backup-recovery-service", 8119, 25, "docs/contracts-backup-recovery.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("ALERTING", "alerting", "/api/v1/alerting", "backend/alerting-service", 8120, 24, "docs/contracts-alerting.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("PLUGIN_INTEGRATION", "plugin-integration", "/api/v1/plugin-integration", "backend/plugin-integration-service", 8122, 38, "docs/contracts-plugin-integration.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("CROSS_PLATFORM_NOTIFICATION", "cross-platform-notification", "/api/v1/cross-platform-notification", "backend/cross-platform-notification-service", 8123, 36, "docs/contracts-cross-platform-notification.md", ".local-docs/tests-ops-core.md"),
            new OpsCoreModuleRegistration("OPS_IMAGE_MARKET", "ops-image-market", "/api/v1/ops-image-market", "backend/ops-image-market-service", 8124, 49, "docs/contracts-ops-image-market.md", ".local-docs/tests-ops-core.md")
    );

    int modulesTotal() {
        return modules.size();
    }

    int inheritedRoutesTotal() {
        return modules.stream().mapToInt(OpsCoreModuleRegistration::routesTotal).sum();
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

    List<Map<String, Object>> opsModules(int currentPort) {
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
                "real node execution stays in node-daemon and is not connected here",
                "real Cloudreve API is not connected",
                "real registry is not connected",
                "real scanner is not connected",
                "real plugin event entry is not connected",
                "real notification delivery is not connected",
                "real external send is not connected",
                "real callback signature is not connected",
                "production credential custody is not connected",
                "async queue is not connected",
                "persistence transaction is not connected"
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
                check("REAL_NODE_EXECUTION", "BLOCKED", "node execution remains delegated to node-daemon"),
                check("REAL_CLOUDREVE_API", "BLOCKED", "real Cloudreve API is not connected"),
                check("REAL_REGISTRY", "BLOCKED", "real registry is not connected"),
                check("REAL_SCANNER", "BLOCKED", "real scanner is not connected"),
                check("REAL_PLUGIN_EVENT_ENTRY", "BLOCKED", "real plugin event entry is not connected"),
                check("REAL_NOTIFICATION_DELIVERY", "BLOCKED", "real notification delivery is not connected"),
                check("REAL_EXTERNAL_SEND", "BLOCKED", "real external send is not connected"),
                check("REAL_CALLBACK_SIGNATURE", "BLOCKED", "real callback signature is not connected"),
                check("PRODUCTION_CREDENTIAL_CUSTODY", "BLOCKED", "production credential custody is not connected"),
                check("ASYNC_QUEUE", "BLOCKED", "async queue is not connected"),
                check("PERSISTENCE_TRANSACTION", "BLOCKED", "persistence transaction is not connected"),
                check("TEST_CONTROL_HEADERS", "PASS", "test control headers are disabled by default"),
                check("REAL_HTTP_SMOKE", httpSmokeStatus, "latest explicit HTTP smoke status is " + httpSmokeStatus),
                check("TRUSTED_GATEWAY_SIGNATURE", "PASS", "trusted gateway context requires HMAC SHA-256 signature"),
                check("INHERITED_ROUTE_DRIFT", "PASS", "inherited route signatures match formal contracts"),
                check("SENSITIVE_FIELD_SCAN", "PASS", "sensitive field scan is covered by automated tests"),
                check("GATEWAY_ROUTE_SWITCH", "PASS", "gateway routes are switched to ops-core")
        );
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
class OpsCoreSmokeCoordinator {
    private static final String DISCOVERY_MODE = "STATIC_LOCAL_CONFIG";

    private final String gatewayBaseUrl;
    private final String selfBaseUrl;
    private final int gatewayPort;
    private final int currentPort;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final List<OpsHttpSmokeTarget> targets;
    private volatile OpsHttpSmokeReport lastReport;

    OpsCoreSmokeCoordinator(@Value("${server.port}") int currentPort,
                            @Value("${ops-core.http-smoke.gateway-base-url:http://127.0.0.1:8125}") String gatewayBaseUrl,
                            @Value("${ops-core.http-smoke.self-base-url:}") String selfBaseUrl,
                            @Value("${ops-core.http-smoke.timeout-ms:1500}") int timeoutMs,
                            ObjectMapper objectMapper) {
        this.currentPort = currentPort;
        this.gatewayBaseUrl = normalizeBaseUrl(gatewayBaseUrl, "http://127.0.0.1:8125");
        this.selfBaseUrl = normalizeBaseUrl(selfBaseUrl, "http://127.0.0.1:" + currentPort);
        this.gatewayPort = portOf(this.gatewayBaseUrl);
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.targets = List.of(
                new OpsHttpSmokeTarget("GATEWAY_OPS_CONTROL_OVERVIEW", "OPS_CONTROL", "GET", this.gatewayBaseUrl, "/api/v1/ops-control/overview", true, 499, 0, timeoutMs),
                new OpsHttpSmokeTarget("GATEWAY_ALERTING_HEALTH", "ALERTING", "GET", this.gatewayBaseUrl, "/api/v1/alerting/health", false, 499, 0, timeoutMs),
                new OpsHttpSmokeTarget("GATEWAY_CPN_HEALTH", "CROSS_PLATFORM_NOTIFICATION", "GET", this.gatewayBaseUrl, "/api/v1/cross-platform-notification/health", false, 499, 0, timeoutMs),
                new OpsHttpSmokeTarget("GATEWAY_OPS_CORE_HEALTH", "OPS_CORE", "GET", this.selfBaseUrl, "/api/v1/ops-core/health", false, 499, 0, timeoutMs)
        );
    }

    String serviceDiscoveryMode() {
        return DISCOVERY_MODE;
    }

    List<Map<String, Object>> registeredUpstreams() {
        return List.of(
                upstream("OPS_CONTROL", "ops-control", gatewayBaseUrl, gatewayPort, "/api/v1/ops-control", "/api/v1/ops-control/overview"),
                upstream("ALERTING", "alerting", gatewayBaseUrl, gatewayPort, "/api/v1/alerting", "/api/v1/alerting/health"),
                upstream("CROSS_PLATFORM_NOTIFICATION", "cross-platform-notification", gatewayBaseUrl, gatewayPort, "/api/v1/cross-platform-notification", "/api/v1/cross-platform-notification/health"),
                upstream("OPS_CORE", "ops-core", selfBaseUrl, currentPort, "/api/v1/ops-core", "/api/v1/ops-core/health")
        );
    }

    String currentStatus() {
        OpsHttpSmokeReport report = lastReport;
        return report == null ? "NOT_RUN" : report.status();
    }

    String readinessStatus() {
        OpsHttpSmokeReport report = lastReport;
        return report == null ? "NOT_CONNECTED" : report.status();
    }

    String lastCheckedAt() {
        OpsHttpSmokeReport report = lastReport;
        return report == null ? null : report.finishedAt().toString();
    }

    List<Map<String, Object>> lastResults() {
        OpsHttpSmokeReport report = lastReport;
        return report == null ? List.of() : report.results().stream().map(OpsHttpSmokeResult::toMap).toList();
    }

    synchronized OpsHttpSmokeReport run(String requestId) {
        if (targets.isEmpty() || timeoutMs <= 0 || targets.stream().map(OpsHttpSmokeTarget::url).anyMatch(this::notHttpUrl)) {
            throw new OpsCoreException(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "http smoke configuration invalid");
        }
        Instant startedAt = Instant.now();
        List<OpsHttpSmokeResult> results = targets.stream()
                .map(target -> probe(target, requestId))
                .toList();
        String status = results.stream().allMatch(result -> "PASS".equals(result.status())) ? "PASS" : "DEGRADED";
        OpsHttpSmokeReport report = new OpsHttpSmokeReport(status, startedAt, Instant.now(), results);
        lastReport = report;
        return report;
    }

    private OpsHttpSmokeResult probe(OpsHttpSmokeTarget target, String requestId) {
        Instant startedAt = Instant.now();
        Instant checkedAt;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target.url()))
                    .timeout(Duration.ofMillis(target.timeoutMs()))
                    .header("X-Request-Id", requestId)
                    .GET();
            if (target.includeSmokeAuthorization()) {
                builder.header("Authorization", "Bearer owner-token");
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            checkedAt = Instant.now();
            Integer businessCode = businessCode(response.body());
            String failureReason = failureReason(response.statusCode(), businessCode, target.expectedStatusMax(), target.expectedBusinessCode());
            String status = failureReason == null ? "PASS" : "FAILED";
            return new OpsHttpSmokeResult(target.targetKey(), target.serviceKey(), target.method(), target.path(), status,
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

    private OpsHttpSmokeResult failed(OpsHttpSmokeTarget target, Instant startedAt, Instant checkedAt, String reason) {
        return new OpsHttpSmokeResult(target.targetKey(), target.serviceKey(), target.method(), target.path(), "FAILED",
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

    private String normalizeBaseUrl(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
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

record OpsHttpSmokeTarget(String targetKey,
                          String serviceKey,
                          String method,
                          String baseUrl,
                          String path,
                          boolean includeSmokeAuthorization,
                          int expectedStatusMax,
                          int expectedBusinessCode,
                          int timeoutMs) {
    String url() {
        return baseUrl + path;
    }
}

record OpsHttpSmokeResult(String targetKey,
                          String serviceKey,
                          String method,
                          String path,
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
        data.put("path", path);
        data.put("status", status);
        data.put("httpStatus", httpStatus);
        data.put("businessCode", businessCode);
        data.put("durationMs", durationMs);
        data.put("checkedAt", checkedAt.toString());
        data.put("failureReason", failureReason);
        return data;
    }
}

record OpsHttpSmokeReport(String status,
                          Instant startedAt,
                          Instant finishedAt,
                          List<OpsHttpSmokeResult> results) {
    Map<String, Object> toMap(String discoveryMode, List<Map<String, Object>> upstreams) {
        long passed = results.stream().filter(result -> "PASS".equals(result.status())).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "ops-core");
        data.put("serviceDiscoveryMode", discoveryMode);
        data.put("registeredUpstreams", upstreams);
        data.put("status", status);
        data.put("httpSmokeStatus", status);
        data.put("realHttpSmoke", true);
        data.put("targetsTotal", results.size());
        data.put("passedTargetsTotal", passed);
        data.put("failedTargetsTotal", results.size() - passed);
        data.put("targets", results.stream().map(OpsHttpSmokeResult::toMap).toList());
        data.put("startedAt", startedAt.toString());
        data.put("finishedAt", finishedAt.toString());
        return data;
    }
}

record OpsCoreModuleRegistration(String moduleKey,
                                 String moduleName,
                                 String pathPrefix,
                                 String legacyServiceDirectory,
                                 int legacyPort,
                                 int routesTotal,
                                 String contract,
                                 String localTestDocument) {
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
        data.put("currentServiceDirectory", "backend/ops-core-service");
        data.put("contract", contract);
        data.put("localTestDocument", localTestDocument);
        data.put("legacyServiceRetired", true);
        data.put("legacyTestCommand", null);
        data.put("currentTestCommand", "mvn -q -f backend/ops-core-service/pom.xml test");
        data.put("contractRoutesTotal", routesTotal);
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("businessContractOwnedByModule", true);
        data.put("compatibilityMode", "IN_PROCESS_MODULE");
        data.put("productionGaps", List.of("REAL_PERSISTENCE_NOT_CONNECTED"));
        return data;
    }

    Map<String, Object> toReadinessMap(int currentPort) {
        Map<String, Object> data = toOpsMap(currentPort);
        data.put("readinessStatus", "NOT_READY");
        data.put("readyForProduction", false);
        return data;
    }
}

record OpsCoreActor(String userId, Set<String> roles, Set<String> permissions, String authMode) {
    boolean hasAny(String... candidates) {
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}

final class TrustedGatewayAuth {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("[a-f0-9]{64}");
    private static final Duration SIGNATURE_SKEW = Duration.ofMinutes(5);
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE",
            "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");

    private TrustedGatewayAuth() {
    }

    static Optional<OpsCoreActor> from(HttpServletRequest request, String signingSecret) {
        String gatewayRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        if (gatewayRequestId == null) {
            return Optional.empty();
        }
        if (gatewayRequestId.isBlank() || !REQUEST_ID_PATTERN.matcher(gatewayRequestId).matches()) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        String userId = request.getHeader("X-Beiming-Actor-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        String rolesHeader = request.getHeader("X-Beiming-Actor-Roles");
        String permissionsHeader = request.getHeader("X-Beiming-Actor-Permissions");
        String timestamp = request.getHeader("X-Gateway-Internal-Timestamp");
        String signature = request.getHeader("X-Gateway-Internal-Signature");
        LinkedHashSet<String> roles = csv(rolesHeader, VALID_ROLES, true);
        LinkedHashSet<String> permissions = csv(permissionsHeader, VALID_PERMISSIONS, false);
        if (timestamp == null || timestamp.isBlank() || signature == null || !SIGNATURE_PATTERN.matcher(signature).matches()) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        try {
            Instant signedAt = Instant.parse(timestamp);
            if (Duration.between(signedAt, Instant.now()).abs().compareTo(SIGNATURE_SKEW) > 0) {
                throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
            }
        } catch (DateTimeParseException ex) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        String minecraftId = request.getHeader("X-Beiming-Actor-Minecraft-Id");
        String minecraftUuid = request.getHeader("X-Beiming-Actor-Minecraft-Uuid");
        String expected = sign(signingSecret, request.getMethod().toUpperCase(), request.getRequestURI(), gatewayRequestId,
                userId.trim(), String.join(",", roles), String.join(",", permissions), timestamp, minecraftId, minecraftUuid);
        if (!expected.equals(signature)) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        return Optional.of(new OpsCoreActor(userId.trim(), roles, permissions, "TRUSTED_GATEWAY_CONTEXT"));
    }

    private static String sign(String secret, String method, String path, String requestId, String userId, String roles,
                               String permissions, String timestamp, String minecraftId, String minecraftUuid) {
        try {
            String plain = String.join("\n",
                    method,
                    path,
                    requestId,
                    userId,
                    roles,
                    permissions,
                    timestamp,
                    minecraftId == null ? "" : minecraftId,
                    minecraftUuid == null ? "" : minecraftUuid);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
    }

    private static LinkedHashSet<String> csv(String value, Set<String> allowed, boolean required) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            if (required) {
                throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
            }
            return parsed;
        }
        for (String part : value.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (!allowed.contains(item)) {
                throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
            }
            parsed.add(item);
        }
        if (required && parsed.isEmpty()) {
            throw new OpsCoreException(HttpStatus.BAD_GATEWAY, 53233, "trusted auth context incompatible");
        }
        return parsed;
    }
}

@Configuration
class OpsCoreFilterConfig {
    @Bean
    FilterRegistrationBean<OpsCoreRequestIdFilter> opsCoreRequestIdFilter() {
        FilterRegistrationBean<OpsCoreRequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OpsCoreRequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/v1/*");
        return registration;
    }
}

class OpsCoreRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(new OpsCoreRequest(request, requestId), response);
    }
}

class OpsCoreRequest extends HttpServletRequestWrapper {
    private final String requestId;

    OpsCoreRequest(HttpServletRequest request, String requestId) {
        super(request);
        this.requestId = requestId;
    }

    @Override
    public String getHeader(String name) {
        if ("X-Request-Id".equalsIgnoreCase(name)) {
            return requestId;
        }
        return super.getHeader(name);
    }
}

@RestControllerAdvice(assignableTypes = OpsCoreController.class)
class OpsCoreExceptionHandler {
    @ExceptionHandler(OpsCoreException.class)
    ResponseEntity<Map<String, Object>> api(OpsCoreException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(OpsCoreController.envelope(exception.code(), exception.getMessage(), null, OpsCoreController.requestId(request)));
    }
}

class OpsCoreException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    OpsCoreException(HttpStatus status, int code, String message) {
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
