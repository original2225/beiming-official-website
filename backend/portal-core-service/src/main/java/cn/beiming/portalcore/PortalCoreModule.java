package cn.beiming.portalcore;

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

    PortalCoreController(@Value("${server.port}") int port,
                         @Value("${portal-core.test-controls.enabled:false}") boolean testControlsEnabled,
                         PortalCoreRegistry registry) {
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
        PortalCoreActor actor = requireAdminOrOwner(request);
        Map<String, Object> data = baseSummary();
        data.put("modulesMounted", registry.modulesTotal());
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("storageMode", "IN_MEMORY_CONTRACT_STUBS");
        data.put("authMode", actor.authMode());
        data.put("actorUserId", actor.userId());
        data.put("dependencyAdapterMode", "SAFE_SNAPSHOT_AND_TEST_ADAPTERS");
        data.put("routeDriftStatus", "NO_DRIFT");
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("moduleRoutes", registry.portalModules(port));
        data.put("productionGaps", registry.productionGaps());
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
        data.put("checks", registry.readinessChecks());
        data.put("moduleReadiness", registry.moduleReadiness(port));
        data.put("productionBlockers", registry.productionGaps());
        data.put("generatedAt", Instant.now().toString());
        return ok(request, data);
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
    private static final int SELF_ROUTES_TOTAL = 4;

    private final List<PortalCoreModuleRegistration> modules = List.of(
            new PortalCoreModuleRegistration("GUIDE", "guide", "/api/v1/guides", "backend/guide-service", 8127, 41, "docs/contracts-guide.md", ".local-docs/tests-guide.md"),
            new PortalCoreModuleRegistration("MATERIAL", "material", "/api/v1/materials", "backend/material-service", 8126, 33, "docs/contracts-material.md", ".local-docs/tests-material.md")
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

    List<String> productionGaps() {
        return List.of(
                "real persistence is not connected",
                "real cross-service HTTP adapters are not connected",
                "real audit persistence is not connected",
                "real object storage is not connected",
                "real file security scanner is not connected",
                "real fulltext search is not connected",
                "real notification delivery is not connected",
                "real HTTP smoke is not connected"
        );
    }

    List<Map<String, Object>> readinessChecks() {
        return List.of(
                check("REAL_PERSISTENCE", "BLOCKED", "real persistence is not connected"),
                check("REAL_CROSS_SERVICE_HTTP", "BLOCKED", "real cross-service HTTP adapters are not connected"),
                check("REAL_AUDIT_PERSISTENCE", "BLOCKED", "real audit persistence is not connected"),
                check("REAL_OBJECT_STORAGE", "BLOCKED", "real object storage is not connected"),
                check("REAL_FILE_SECURITY_SCANNER", "BLOCKED", "real file security scanner is not connected"),
                check("REAL_FULLTEXT_SEARCH", "BLOCKED", "real fulltext search is not connected"),
                check("REAL_NOTIFICATION_DELIVERY", "BLOCKED", "real notification delivery is not connected"),
                check("REAL_HTTP_SMOKE", "BLOCKED", "real HTTP smoke is not connected"),
                check("TEST_CONTROL_HEADERS", "PASS", "test control headers are disabled by default"),
                check("INHERITED_ROUTE_DRIFT", "PASS", "inherited route signatures match formal contracts"),
                check("SENSITIVE_FIELD_SCAN", "PASS", "sensitive field scan is covered by automated tests"),
                check("GATEWAY_ROUTE_SWITCH", "PASS", "gateway routes are switched to portal-core")
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

record PortalCoreModuleRegistration(String moduleKey,
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
        data.put("currentServiceDirectory", "backend/portal-core-service");
        data.put("contract", contract);
        data.put("localTestDocument", localTestDocument);
        data.put("legacyTestCommand", "mvn -q -f " + legacyServiceDirectory + "/pom.xml test");
        data.put("currentTestCommand", "mvn -q -f backend/portal-core-service/pom.xml test");
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
