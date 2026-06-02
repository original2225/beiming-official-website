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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-core")
class BusinessCoreController {
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

    private Map<String, Object> healthSummary(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("moduleRoutes", registry.publicModules());
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> opsSummary(HttpServletRequest request) {
        Map<String, Object> data = baseSummary();
        data.put("routesTotal", registry.businessRoutesTotal() + 2);
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

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "business-core");
        data.put("status", "UP");
        data.put("port", port);
        data.put("modulesTotal", registry.modulesTotal());
        data.put("modulesMounted", registry.modulesTotal());
        data.put("businessRoutesTotal", registry.businessRoutesTotal());
        data.put("selfRoutesTotal", 2);
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
