package cn.beiming.engagement;

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
@RequestMapping("/api/v1/engagement-core")
class EngagementCoreController {
    private static final int SELF_ROUTES_TOTAL = 2;

    private final int port;
    private final EngagementCoreRegistry registry = new EngagementCoreRegistry();

    EngagementCoreController(@Value("${server.port}") int port) {
        this.port = port;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ResponseEntity.ok(envelope(0, "success", healthSummary(), requestId(request)));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        EngagementAuthDecision decision = authorizeAdminOrOwner(authorization);
        if (!decision.allowed()) {
            return ResponseEntity.status(decision.status()).body(envelope(decision.code(), decision.message(), null, requestId(request)));
        }
        return ResponseEntity.ok(envelope(0, "success", opsSummary(), requestId(request)));
    }

    private Map<String, Object> healthSummary() {
        Map<String, Object> data = baseSummary();
        data.put("moduleRoutes", registry.publicModules());
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> opsSummary() {
        Map<String, Object> data = baseSummary();
        data.put("routesTotal", registry.engagementRoutesTotal() + SELF_ROUTES_TOTAL);
        data.put("moduleRoutes", registry.modules());
        data.put("adapterChain", registry.adapterChain());
        data.put("businessCoreDependency", registry.businessCoreDependency());
        data.put("admissionCoreDependency", registry.admissionCoreDependency());
        data.put("gatewaySwitchReady", true);
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("legacyBaselines", registry.legacyBaselines());
        data.put("retiredLegacyServices", registry.retiredLegacyServices());
        data.put("productionGaps", registry.productionGaps());
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "engagement-core");
        data.put("status", "UP");
        data.put("port", port);
        data.put("modulesTotal", registry.modulesTotal());
        data.put("modulesMounted", registry.modulesTotal());
        data.put("engagementRoutesTotal", registry.engagementRoutesTotal());
        data.put("selfRoutesTotal", SELF_ROUTES_TOTAL);
        return data;
    }

    private EngagementAuthDecision authorizeAdminOrOwner(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return new EngagementAuthDecision(false, HttpStatus.UNAUTHORIZED, 41000, "not logged in");
        }
        if (!authorization.startsWith("Bearer ")) {
            return new EngagementAuthDecision(false, HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        String token = authorization.substring("Bearer ".length());
        if ("admin-token".equals(token) || "owner-token".equals(token)) {
            return new EngagementAuthDecision(true, HttpStatus.OK, 0, "success");
        }
        if ("helper-token".equals(token) || "user-token".equals(token)) {
            return new EngagementAuthDecision(false, HttpStatus.FORBIDDEN, 42001, "role permission denied");
        }
        return new EngagementAuthDecision(false, HttpStatus.UNAUTHORIZED, 41001, "invalid session");
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

class EngagementCoreRegistry {
    private static final String THIRD_BATCH_BASELINE_VERIFIED_AT = "2026-06-03T00:00:00+08:00";

    private final List<EngagementModuleRegistration> modules = List.of(
            new EngagementModuleRegistration("COMMUNITY", "community", "/api/v1/community", "docs/contracts-community.md", 8112, 64,
                    List.of(), List.of("CommunityPublicSnapshotAdapter"), List.of("business-core:auth", "business-core:profile", "business-core:notification")),
            new EngagementModuleRegistration("ACTIVITY", "activity", "/api/v1/activity", "docs/contracts-activity.md", 8113, 41,
                    List.of("CommunityPublicSnapshotAdapter"), List.of("ActivityCalendarSummaryAdapter"), List.of("business-core:auth", "business-core:profile", "business-core:notification", "community")),
            new EngagementModuleRegistration("CALENDAR", "calendar", "/api/v1/calendar", "docs/contracts-calendar.md", 8114, 21,
                    List.of("ActivityCalendarSummaryAdapter"), List.of("CalendarReleaseSummaryAdapter"), List.of("business-core:auth", "business-core:notification", "activity")),
            new EngagementModuleRegistration("CHANGELOG", "changelog", "/api/v1/changelog", "docs/contracts-changelog.md", 8115, 23,
                    List.of("CalendarReleaseSummaryAdapter"), List.of(), List.of("business-core:auth", "business-core:resource", "business-core:server-status", "business-core:content", "calendar"))
    );

    int modulesTotal() {
        return modules.size();
    }

    int engagementRoutesTotal() {
        return modules.stream().mapToInt(EngagementModuleRegistration::routesTotal).sum();
    }

    List<Map<String, Object>> publicModules() {
        return modules.stream().map(EngagementModuleRegistration::toPublicMap).toList();
    }

    List<Map<String, Object>> modules() {
        return modules.stream().map(EngagementModuleRegistration::toOpsMap).toList();
    }

    List<Map<String, Object>> adapterChain() {
        return List.of(
                adapter("activity", "community", "CommunityPublicSnapshotAdapter"),
                adapter("calendar", "activity", "ActivityCalendarSummaryAdapter"),
                adapter("changelog", "calendar", "CalendarReleaseSummaryAdapter")
        );
    }

    Map<String, Object> businessCoreDependency() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "business-core");
        data.put("port", 8130);
        data.put("status", "REQUIRED_BASELINE");
        data.put("contract", "docs/contracts-business-core.md");
        data.put("testCommand", "mvn -f backend/business-core-service/pom.xml test");
        return data;
    }

    Map<String, Object> admissionCoreDependency() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "admission-core");
        data.put("port", 8131);
        data.put("status", "STABLE_BASELINE");
        data.put("contract", "docs/contracts-admission-core.md");
        data.put("testCommand", "mvn -f backend/admission-core-service/pom.xml test");
        return data;
    }

    List<Map<String, Object>> legacyBaselines() {
        return List.of(
                baseline("business-core-service", 8130, "docs/contracts-business-core.md", "mvn -f backend/business-core-service/pom.xml test"),
                baseline("admission-core-service", 8131, "docs/contracts-admission-core.md", "mvn -f backend/admission-core-service/pom.xml test"),
                baseline("api-gateway-service", 8125, "docs/contracts-api-gateway.md", "mvn -f backend/api-gateway-service/pom.xml test")
        );
    }

    List<Map<String, Object>> retiredLegacyServices() {
        return List.of(
                retired("community-service", 8112, "backend/community-service", "docs/contracts-community.md"),
                retired("activity-service", 8113, "backend/activity-service", "docs/contracts-activity.md"),
                retired("calendar-service", 8114, "backend/calendar-service", "docs/contracts-calendar.md"),
                retired("changelog-service", 8115, "backend/changelog-service", "docs/contracts-changelog.md")
        );
    }

    List<String> productionGaps() {
        return List.of(
                "real database persistence is still module dependent",
                "real cross-service adapters are still represented by local test stubs",
                "persistent audit storage is not connected",
                "real notification delivery is not connected",
                "live gateway-to-engagement-core HTTP smoke is not verified"
        );
    }

    static String baselineVerifiedAt() {
        return THIRD_BATCH_BASELINE_VERIFIED_AT;
    }

    private Map<String, Object> adapter(String from, String to, String adapter) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from);
        data.put("to", to);
        data.put("adapter", adapter);
        data.put("mutable", false);
        return data;
    }

    private Map<String, Object> baseline(String service, int port, String contract, String testCommand) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", service);
        data.put("port", port);
        data.put("contract", contract);
        data.put("testCommand", testCommand);
        data.put("lastVerifiedAt", THIRD_BATCH_BASELINE_VERIFIED_AT);
        return data;
    }

    private Map<String, Object> retired(String service, int port, String directory, String contract) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", service);
        data.put("port", port);
        data.put("directory", directory);
        data.put("contract", contract);
        data.put("status", "RETIRED");
        data.put("testCommand", null);
        data.put("retiredAt", THIRD_BATCH_BASELINE_VERIFIED_AT);
        return data;
    }
}

record EngagementModuleRegistration(String moduleKey,
                                    String moduleName,
                                    String pathPrefix,
                                    String contract,
                                    int legacyPort,
                                    int routesTotal,
                                    List<String> adapters,
                                    List<String> downstreamAdapters,
                                    List<String> upstreamDependencies) {
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
        data.put("downstreamAdapters", downstreamAdapters);
        data.put("upstreamDependencies", upstreamDependencies);
        data.put("compatibilityMode", "IN_PROCESS_ADAPTER");
        data.put("lastVerifiedAt", EngagementCoreRegistry.baselineVerifiedAt());
        data.put("gaps", List.of());
        return data;
    }
}

record EngagementAuthDecision(boolean allowed, HttpStatus status, int code, String message) {
}

@Configuration
class EngagementCoreFilterConfig {
    @Bean
    FilterRegistrationBean<EngagementCoreRequestIdFilter> engagementCoreRequestIdFilter() {
        FilterRegistrationBean<EngagementCoreRequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new EngagementCoreRequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/v1/*");
        return registration;
    }
}

class EngagementCoreRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(new EngagementCoreRequest(request, requestId), response);
    }
}

class EngagementCoreRequest extends HttpServletRequestWrapper {
    private final String requestId;

    EngagementCoreRequest(HttpServletRequest request, String requestId) {
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
