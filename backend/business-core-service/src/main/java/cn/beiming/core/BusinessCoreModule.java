package cn.beiming.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-core")
class BusinessCoreController {
    private static final int SELF_ROUTES_TOTAL = 3;
    private final int port;
    private final BusinessCoreRegistry registry = new BusinessCoreRegistry();

    BusinessCoreController(@Value("${server.port}") int port) {
        this.port = port;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ResponseEntity.ok(envelope(0, "success", healthSummary(request), requestId(request)));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthDecision decision = authorizeAdminOrOwner(authorization);
        if (!decision.allowed()) {
            return ResponseEntity.status(decision.status()).body(envelope(decision.code(), decision.message(), null, requestId(request)));
        }
        return ResponseEntity.ok(envelope(0, "success", opsSummary(request), requestId(request)));
    }

    @GetMapping("/admin/production-readiness")
    ResponseEntity<Map<String, Object>> productionReadiness(HttpServletRequest request,
                                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthDecision decision = authorizeAdminOrOwner(authorization);
        if (!decision.allowed()) {
            return ResponseEntity.status(decision.status()).body(envelope(decision.code(), decision.message(), null, requestId(request)));
        }
        return ResponseEntity.ok(envelope(0, "success", productionReadinessSummary(), requestId(request)));
    }

    private Map<String, Object> healthSummary(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("moduleRoutes", registry.publicModules());
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> opsSummary(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("routesTotal", registry.businessRoutesTotal() + SELF_ROUTES_TOTAL);
        data.put("moduleRoutes", registry.modules());
        data.put("gatewaySwitchReady", true);
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("legacyBaselines", registry.legacyBaselines());
        data.put("productionGaps", List.of(
                "real database persistence is still module dependent"
        ));
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> productionReadinessSummary() {
        List<Map<String, Object>> gaps = productionGaps();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "business-core");
        data.put("port", port);
        data.put("productionReady", false);
        data.put("readinessStatus", "NOT_READY");
        data.put("routeSummary", Map.of(
                "businessRoutesTotal", registry.businessRoutesTotal(),
                "selfRoutesTotal", SELF_ROUTES_TOTAL,
                "routesTotal", registry.businessRoutesTotal() + SELF_ROUTES_TOTAL
        ));
        data.put("blockingGaps", gaps);
        data.put("gapsTotal", gaps.size());
        data.put("criticalGapsTotal", gaps.stream().filter(gap -> "CRITICAL".equals(gap.get("severity"))).count());
        data.put("highGapsTotal", gaps.stream().filter(gap -> "HIGH".equals(gap.get("severity"))).count());
        data.put("integrationChecks", integrationChecks());
        data.put("testScope", testScope());
        data.put("testControls", testControls());
        data.put("sourceDrift", sourceDrift());
        data.put("nextDevelopmentOrder", List.of(
                "LIVE_GATEWAY_HTTP_SMOKE",
                "PRODUCTION_AUTH_CONTEXT",
                "PERSISTENCE_AND_AUDIT",
                "TEST_CONTROL_GUARD",
                "SOURCE_DRIFT_GUARD"
        ));
        data.put("legacyBaselinesKept", true);
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "business-core");
        data.put("status", "UP");
        data.put("port", port);
        data.put("modulesTotal", registry.modulesTotal());
        data.put("modulesMounted", registry.modulesTotal());
        data.put("businessRoutesTotal", registry.businessRoutesTotal());
        data.put("selfRoutesTotal", SELF_ROUTES_TOTAL);
        return data;
    }

    private List<Map<String, Object>> productionGaps() {
        return List.of(
                readinessGap("LIVE_GATEWAY_HTTP_SMOKE_NOT_VERIFIED", "INTEGRATION", "HIGH", "BUSINESS_CORE",
                        "MOCKMVC_AND_MAVEN_CONTRACTS", "REAL_HTTP_GATEWAY_TO_BUSINESS_CORE",
                        "Run api-gateway-service and business-core-service together, then verify first-batch paths through real HTTP.",
                        "Record request paths, status codes, request ids, degraded responses, and commands in .local-docs/tests-business-core.md."),
                readinessGap("PERSISTENT_DATABASE_NOT_CONNECTED", "PERSISTENCE", "HIGH", "ALL_FIRST_BATCH_MODULES",
                        "IN_MEMORY", "RELATIONAL_DATABASE_WITH_MIGRATIONS",
                        "Add persistent stores, migrations, unique constraints, and transaction boundaries for first-batch module data.",
                        "Run module contract tests against the persistent profile and record migration verification."),
                readinessGap("PERSISTENT_AUDIT_NOT_CONNECTED", "AUDIT", "HIGH", "ALL_FIRST_BATCH_MODULES",
                        "IN_MEMORY_AUDIT_LISTS", "PERSISTENT_APPEND_ONLY_AUDIT",
                        "Persist audit records and keep audit write failure rollback semantics.",
                        "Verify audit records survive restart and failed audit writes leave business state unchanged."),
                readinessGap("PRODUCTION_AUTH_CONTEXT_NOT_CONNECTED", "SECURITY", "HIGH", "BUSINESS_CORE",
                        "LOCAL_FIXED_TOKENS_AND_TRUSTED_HEADERS", "REAL_AUTH_SESSION_AND_TRUSTED_GATEWAY_ONLY",
                        "Replace local fixed-token checks for business-core self endpoints with real auth context.",
                        "Verify forged trusted headers cannot grant access and real gateway context is accepted."),
                readinessGap("TEST_CONTROL_HEADERS_REQUIRE_PRODUCTION_GUARD", "SAFETY", "MEDIUM", "ALL_FIRST_BATCH_MODULES",
                        "MODULE_LOCAL_TEST_CONTROLS", "CENTRAL_PRODUCTION_GUARD",
                        "Add a central guard that disables X-Test-* controls outside local test mode.",
                        "Run boundary tests proving X-Test-* headers are ignored or rejected in production mode."),
                readinessGap("LEGACY_SOURCE_DRIFT_GUARD_REQUIRED", "MAINTENANCE", "MEDIUM", "BUSINESS_CORE",
                        "DUPLICATED_SOURCE_BASELINES", "EXPLICIT_FREEZE_OR_SHARED_SOURCE_POLICY",
                        "Define whether old services are frozen or generated from shared sources, then guard drift in tests.",
                        "Compare business-core module copies with legacy service baselines before every merge.")
        );
    }

    private Map<String, Object> readinessGap(String key, String category, String severity, String ownerModule,
                                             String currentMode, String requiredMode, String nextAction,
                                             String verification) {
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("gapKey", key);
        gap.put("category", category);
        gap.put("severity", severity);
        gap.put("ownerModule", ownerModule);
        gap.put("currentMode", currentMode);
        gap.put("requiredMode", requiredMode);
        gap.put("nextAction", nextAction);
        gap.put("verification", verification);
        return gap;
    }

    private List<Map<String, Object>> integrationChecks() {
        return List.of(
                check("LIVE_GATEWAY_HTTP_SMOKE", "NOT_VERIFIED", "No live api-gateway to business-core HTTP record exists yet.", true),
                check("PERSISTENT_DATABASE", "NOT_CONNECTED", "First-batch modules still expose in-memory storage modes.", true),
                check("PERSISTENT_AUDIT", "NOT_CONNECTED", "Audit records are still module-local in-memory records.", true),
                check("PRODUCTION_AUTH_CONTEXT", "REQUIRED", "Business-core self endpoints still accept local fixed test tokens.", true),
                check("GATEWAY_INTERNAL_SIGNATURE", "REQUIRED", "Gateway internal signature or mTLS is not enabled.", true),
                check("TEST_CONTROL_GUARD", "REQUIRED", "X-Test-* controls need a central production guard.", true),
                check("SOURCE_DRIFT_GUARD", "REQUIRED", "Legacy services and business-core source copies can drift.", true)
        );
    }

    private Map<String, Object> check(String key, String status, String evidence, boolean requiredBeforeProduction) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("checkKey", key);
        item.put("status", status);
        item.put("evidence", evidence);
        item.put("requiredBeforeProduction", requiredBeforeProduction);
        return item;
    }

    private Map<String, Object> testScope() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mockMvcContractTests", Map.of("status", "PASS", "evidence", "mvn -f backend/business-core-service/pom.xml test"));
        data.put("legacyBaselineTests", Map.of("status", "PASS", "evidence", "old first-batch service Maven tests remain required"));
        data.put("apiGatewayRouteSwitchTests", Map.of("status", "PASS", "evidence", "api-gateway routes first-batch prefixes to port 8130"));
        data.put("liveHttpSmokeTests", Map.of("status", "NOT_VERIFIED", "evidence", "no live multi-process HTTP smoke record"));
        return data;
    }

    private Map<String, Object> testControls() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productionGuardRequired", true);
        data.put("knownControlHeaders", List.of(
                "X-Test-Fail-Audit",
                "X-Test-Notification-Mode",
                "X-Test-Profile-Mode",
                "X-Test-Auth-Mode",
                "X-Test-Status-Collector"
        ));
        data.put("risk", "TEST_CONTROLS_MUST_NOT_TRIGGER_FAILURES_IN_PRODUCTION");
        return data;
    }

    private Map<String, Object> sourceDrift() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("risk", "DUAL_MAINTENANCE");
        data.put("legacyServices", List.of(
                "auth-service",
                "profile-service",
                "notification-service",
                "content-service",
                "server-status-service",
                "resource-service",
                "admin-service"
        ));
        data.put("guardRequired", true);
        data.put("policyNeeded", "FREEZE_LEGACY_OR_SHARE_SOURCE");
        return data;
    }

    private AuthDecision authorizeAdminOrOwner(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return new AuthDecision(false, HttpStatus.UNAUTHORIZED, 41000, "not logged in");
        }
        if (!authorization.startsWith("Bearer ")) {
            return new AuthDecision(false, HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        String token = authorization.substring("Bearer ".length());
        if ("admin-token".equals(token) || "owner-token".equals(token)) {
            return new AuthDecision(true, HttpStatus.OK, 0, "success");
        }
        if ("helper-token".equals(token) || "user-token".equals(token)) {
            return new AuthDecision(false, HttpStatus.FORBIDDEN, 42001, "role permission denied");
        }
        return new AuthDecision(false, HttpStatus.UNAUTHORIZED, 41001, "invalid session");
    }

    private Map<String, Object> envelope(int code, String message, Object data, String requestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", requestId);
        return body;
    }

    private String requestId(HttpServletRequest request) {
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

class BusinessCoreRegistry {
    private static final String FIRST_BATCH_VERIFIED_AT = "2026-06-02T15:34:38+08:00";

    private final List<ModuleRegistration> modules = List.of(
            new ModuleRegistration("AUTH", "auth", "/api/v1/auth", "docs/contracts-auth.md", 8101, 20,
                    List.of("AuthStore", "AuthLocalWebConfig"), "auth-service"),
            new ModuleRegistration("PROFILE", "profile", "/api/v1/profile", "docs/contracts-profile.md", 8102, 16,
                    List.of("ProfileAuthContextProvider"), "profile-service"),
            new ModuleRegistration("NOTIFICATION", "notification", "/api/v1/notifications", "docs/contracts-notification.md", 8103, 19,
                    List.of("NotificationAuthContextProvider"), "notification-service"),
            new ModuleRegistration("CONTENT", "content", "/api/v1/content", "docs/contracts-content.md", 8104, 55,
                    List.of("TestAuthContextProvider", "TestProfileSnapshotProvider", "TestNotificationClient"), "content-service"),
            new ModuleRegistration("SERVER_STATUS", "server-status", "/api/v1/server-status", "docs/contracts-server-status.md", 8105, 25,
                    List.of("TestAuthContextProvider", "TestStatusCollector"), "server-status-service"),
            new ModuleRegistration("RESOURCE", "resource", "/api/v1/resources", "docs/contracts-resource.md", 8106, 29,
                    List.of("TestResourceAuthProvider"), "resource-service"),
            new ModuleRegistration("ADMIN", "admin", "/api/v1/admin", "docs/contracts-admin.md", 8107, 10,
                    List.of("TestAdminAuthProvider"), "admin-service")
    );

    int modulesTotal() {
        return modules.size();
    }

    int businessRoutesTotal() {
        return modules.stream().mapToInt(ModuleRegistration::routesTotal).sum();
    }

    List<Map<String, Object>> publicModules() {
        return modules.stream().map(ModuleRegistration::toPublicMap).toList();
    }

    List<Map<String, Object>> modules() {
        return modules.stream().map(ModuleRegistration::toOpsMap).toList();
    }

    List<Map<String, Object>> legacyBaselines() {
        List<Map<String, Object>> baselines = new java.util.ArrayList<>(
                modules.stream().map(ModuleRegistration::toLegacyBaseline).toList());
        Map<String, Object> gateway = new LinkedHashMap<>();
        gateway.put("service", "api-gateway-service");
        gateway.put("port", 8125);
        gateway.put("contract", "docs/contracts-api-gateway.md");
        gateway.put("testCommand", "mvn -f backend/api-gateway-service/pom.xml test");
        gateway.put("lastVerifiedAt", FIRST_BATCH_VERIFIED_AT);
        baselines.add(gateway);
        return baselines;
    }

    static String firstBatchVerifiedAt() {
        return FIRST_BATCH_VERIFIED_AT;
    }
}

record ModuleRegistration(String moduleKey,
                          String moduleName,
                          String pathPrefix,
                          String contract,
                          int legacyPort,
                          int routesTotal,
                          List<String> adapters,
                          String legacyService) {
    Map<String, Object> toPublicMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleKey", moduleKey);
        data.put("pathPrefix", pathPrefix);
        data.put("mounted", true);
        data.put("routesTotal", routesTotal);
        data.put("status", "READY");
        return data;
    }

    Map<String, Object> toOpsMap() {
        Map<String, Object> data = toPublicMap();
        data.put("moduleName", moduleName);
        data.put("contract", contract);
        data.put("legacyPort", legacyPort);
        data.put("contractRoutesTotal", routesTotal);
        data.put("adapters", adapters);
        data.put("compatibilityMode", "IN_PROCESS_ADAPTER");
        data.put("lastVerifiedAt", BusinessCoreRegistry.firstBatchVerifiedAt());
        data.put("gaps", List.of());
        return data;
    }

    Map<String, Object> toLegacyBaseline() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", legacyService);
        data.put("port", legacyPort);
        data.put("contract", contract);
        data.put("testCommand", "mvn -f backend/" + legacyService + "/pom.xml test");
        data.put("lastVerifiedAt", BusinessCoreRegistry.firstBatchVerifiedAt());
        return data;
    }
}

record AuthDecision(boolean allowed, HttpStatus status, int code, String message) {
}

@Configuration
class BusinessCoreFilterConfig {
    @Bean
    FilterRegistrationBean<BusinessCoreRequestIdFilter> businessCoreRequestIdFilter() {
        FilterRegistrationBean<BusinessCoreRequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new BusinessCoreRequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/v1/*");
        return registration;
    }
}

class BusinessCoreRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(new BusinessCoreRequest(request, requestId), response);
    }
}

class BusinessCoreRequest extends HttpServletRequestWrapper {
    private final String requestId;

    BusinessCoreRequest(HttpServletRequest request, String requestId) {
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
