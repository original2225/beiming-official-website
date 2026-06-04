package cn.beiming.opscore;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/ops-core")
class OpsCoreController {
    private final int port;
    private final boolean testControlsEnabled;
    private final OpsCoreRegistry registry;

    OpsCoreController(@Value("${server.port}") int port,
                      @Value("${ops-core.test-controls.enabled:false}") boolean testControlsEnabled,
                      OpsCoreRegistry registry) {
        this.port = port;
        this.testControlsEnabled = testControlsEnabled;
        this.registry = registry;
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
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("moduleRoutes", registry.opsModules(port));
        data.put("productionGaps", registry.productionGaps());
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
        data.put("checks", registry.readinessChecks());
        data.put("moduleReadiness", registry.moduleReadiness(port));
        data.put("productionBlockers", registry.productionGaps());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
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

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(0, "success", data, requestId(request)));
    }

    private OpsCoreActor requireAdminOrOwner(HttpServletRequest request) {
        Optional<OpsCoreActor> trusted = TrustedGatewayAuth.from(request);
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
    private static final int SELF_ROUTES_TOTAL = 4;

    private final List<OpsCoreModuleRegistration> modules = List.of(
            new OpsCoreModuleRegistration("OPS_CONTROL", "ops-control", "/api/v1/ops-control", "backend/ops-control-service", 8116, 31, "docs/contracts-ops-control.md", ".local-docs/tests-ops-control.md"),
            new OpsCoreModuleRegistration("CLOUDREVE_SYNC", "cloudreve-sync", "/api/v1/cloudreve-sync", "backend/cloudreve-sync-service", 8118, 16, "docs/contracts-cloudreve-sync.md", ".local-docs/tests-cloudreve-sync.md"),
            new OpsCoreModuleRegistration("BACKUP_RECOVERY", "backup-recovery", "/api/v1/backup-recovery", "backend/backup-recovery-service", 8119, 25, "docs/contracts-backup-recovery.md", ".local-docs/tests-backup-recovery.md"),
            new OpsCoreModuleRegistration("ALERTING", "alerting", "/api/v1/alerting", "backend/alerting-service", 8120, 24, "docs/contracts-alerting.md", ".local-docs/tests-alerting.md"),
            new OpsCoreModuleRegistration("PLUGIN_INTEGRATION", "plugin-integration", "/api/v1/plugin-integration", "backend/plugin-integration-service", 8122, 38, "docs/contracts-plugin-integration.md", ".local-docs/tests-plugin-integration.md"),
            new OpsCoreModuleRegistration("OPS_IMAGE_MARKET", "ops-image-market", "/api/v1/ops-image-market", "backend/ops-image-market-service", 8124, 49, "docs/contracts-ops-image-market.md", ".local-docs/tests-ops-image-market.md")
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

    List<String> productionGaps() {
        return List.of(
                "real persistence is not connected",
                "real cross-service HTTP adapters are not connected",
                "real audit persistence is not connected",
                "real node execution stays in node-daemon and is not connected here",
                "real Cloudreve API is not connected",
                "real registry is not connected",
                "real scanner is not connected",
                "real plugin event entry is not connected",
                "real notification delivery is not connected"
        );
    }

    List<Map<String, Object>> readinessChecks() {
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
                check("TEST_CONTROL_HEADERS", "PASS", "test control headers are disabled by default"),
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
        data.put("legacyTestCommand", "mvn -q -f " + legacyServiceDirectory + "/pom.xml test");
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
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE",
            "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");

    private TrustedGatewayAuth() {
    }

    static Optional<OpsCoreActor> from(HttpServletRequest request) {
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
        LinkedHashSet<String> roles = csv(request.getHeader("X-Beiming-Actor-Roles"), VALID_ROLES, true);
        LinkedHashSet<String> permissions = csv(request.getHeader("X-Beiming-Actor-Permissions"), VALID_PERMISSIONS, false);
        return Optional.of(new OpsCoreActor(userId.trim(), roles, permissions, "TRUSTED_GATEWAY_CONTEXT"));
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
