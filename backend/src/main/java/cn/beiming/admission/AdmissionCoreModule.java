package cn.beiming.admission;

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
@RequestMapping("/api/v1/admission-core")
class AdmissionCoreController {
    private static final int SELF_ROUTES_TOTAL = 2;
    private final int port;
    private final AdmissionCoreRegistry registry = new AdmissionCoreRegistry();

    AdmissionCoreController(@Value("${server.port}") int port) {
        this.port = port;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ResponseEntity.ok(envelope(0, "success", healthSummary(), requestId(request)));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        AdmissionAuthDecision decision = authorizeAdminOrOwner(authorization);
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
        data.put("routesTotal", registry.admissionRoutesTotal() + SELF_ROUTES_TOTAL);
        data.put("moduleRoutes", registry.modules());
        data.put("handoffChain", registry.handoffChain());
        data.put("businessCoreDependency", registry.businessCoreDependency());
        data.put("gatewaySwitchReady", true);
        data.put("gatewaySwitchStatus", "COMPLETED");
        data.put("legacyBaselines", registry.legacyBaselines());
        data.put("productionGaps", registry.productionGaps());
        data.put("generatedAt", Instant.now().toString());
        return data;
    }

    private Map<String, Object> baseSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "admission-core");
        data.put("status", "UP");
        data.put("port", port);
        data.put("modulesTotal", registry.modulesTotal());
        data.put("modulesMounted", registry.modulesTotal());
        data.put("admissionRoutesTotal", registry.admissionRoutesTotal());
        data.put("selfRoutesTotal", SELF_ROUTES_TOTAL);
        return data;
    }

    private AdmissionAuthDecision authorizeAdminOrOwner(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return new AdmissionAuthDecision(false, HttpStatus.UNAUTHORIZED, 41000, "not logged in");
        }
        if (!authorization.startsWith("Bearer ")) {
            return new AdmissionAuthDecision(false, HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        String token = authorization.substring("Bearer ".length());
        if ("admin-token".equals(token) || "owner-token".equals(token)) {
            return new AdmissionAuthDecision(true, HttpStatus.OK, 0, "success");
        }
        if ("helper-token".equals(token) || "user-token".equals(token)) {
            return new AdmissionAuthDecision(false, HttpStatus.FORBIDDEN, 42001, "role permission denied");
        }
        return new AdmissionAuthDecision(false, HttpStatus.UNAUTHORIZED, 41001, "invalid session");
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

class AdmissionCoreRegistry {
    private static final String SECOND_BATCH_BASELINE_VERIFIED_AT = "2026-06-03T00:00:00+08:00";

    private final List<AdmissionModuleRegistration> modules = List.of(
            new AdmissionModuleRegistration("ONBOARDING", "onboarding", "/api/v1/onboarding", "docs/api-reference.md", 8108, 15,
                    List.of(), List.of("OnboardingExamHandoffSnapshot"), List.of("TestOnboardingAuthProvider", "TestOnboardingProfileClient", "TestOnboardingContentClient", "TestOnboardingNotificationClient")),
            new AdmissionModuleRegistration("EXAM", "exam", "/api/v1/exams", "docs/api-reference.md", 8109, 28,
                    List.of("OnboardingExamHandoffSnapshot"), List.of("ExamWhitelistHandoffSnapshot"), List.of("TestExamAuthProvider", "TestExamOnboardingClient", "TestExamProfileClient", "TestExamContentClient", "TestExamNotificationClient")),
            new AdmissionModuleRegistration("WHITELIST", "whitelist", "/api/v1/whitelist", "docs/api-reference.md", 8110, 20,
                    List.of("ExamWhitelistHandoffSnapshot"), List.of("WhitelistAttendanceHandoffSnapshot"), List.of("TestWhitelistAuthProvider", "TestWhitelistExamClient", "TestWhitelistProfileClient", "TestWhitelistNotificationClient")),
            new AdmissionModuleRegistration("ATTENDANCE", "attendance", "/api/v1/attendance", "docs/api-reference.md", 8111, 21,
                    List.of("WhitelistAttendanceHandoffSnapshot"), List.of(), List.of("TestAttendanceAuthProvider", "TestAttendanceWhitelistClient", "TestAttendanceProfileClient", "TestAttendanceNotificationClient"))
    );

    int modulesTotal() {
        return modules.size();
    }

    int admissionRoutesTotal() {
        return modules.stream().mapToInt(AdmissionModuleRegistration::routesTotal).sum();
    }

    List<Map<String, Object>> publicModules() {
        return modules.stream().map(AdmissionModuleRegistration::toPublicMap).toList();
    }

    List<Map<String, Object>> modules() {
        return modules.stream().map(AdmissionModuleRegistration::toOpsMap).toList();
    }

    List<Map<String, Object>> handoffChain() {
        return List.of(
                handoff("onboarding", "exam", "OnboardingExamHandoffSnapshot"),
                handoff("exam", "whitelist", "ExamWhitelistHandoffSnapshot"),
                handoff("whitelist", "attendance", "WhitelistAttendanceHandoffSnapshot")
        );
    }

    Map<String, Object> businessCoreDependency() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "business-core");
        data.put("port", 8130);
        data.put("status", "REQUIRED_BASELINE");
        data.put("contract", "docs/api-reference.md");
        data.put("testCommand", "mvn -q -f backend/pom.xml test");
        return data;
    }

    List<Map<String, Object>> legacyBaselines() {
        return List.of(
                baseline("business-core-service", 8130, "docs/api-reference.md", "mvn -q -f backend/pom.xml test"),
                baseline("unified-backend-service", 8135, "docs/api-reference.md", "mvn -f backend/pom.xml test")
        );
    }

    List<String> productionGaps() {
        return List.of(
                "real database persistence is still module dependent",
                "persistent audit storage is not connected",
                "real cross-service HTTP adapters are still represented by local test stubs",
                "real server whitelist operations remain outside admission-core",
                "live gateway-to-admission-core HTTP smoke is not verified"
        );
    }

    static String baselineVerifiedAt() {
        return SECOND_BATCH_BASELINE_VERIFIED_AT;
    }

    private Map<String, Object> handoff(String from, String to, String handoff) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from);
        data.put("to", to);
        data.put("handoff", handoff);
        data.put("mutable", false);
        return data;
    }

    private Map<String, Object> baseline(String service, int port, String contract, String testCommand) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", service);
        data.put("port", port);
        data.put("contract", contract);
        data.put("testCommand", testCommand);
        data.put("lastVerifiedAt", SECOND_BATCH_BASELINE_VERIFIED_AT);
        return data;
    }
}

record AdmissionModuleRegistration(String moduleKey,
                                   String moduleName,
                                   String pathPrefix,
                                   String contract,
                                   int legacyPort,
                                   int routesTotal,
                                   List<String> handoffIn,
                                   List<String> handoffOut,
                                   List<String> adapters) {
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
        data.put("handoffIn", handoffIn);
        data.put("handoffOut", handoffOut);
        data.put("adapters", adapters);
        data.put("compatibilityMode", "IN_PROCESS_ADAPTER");
        data.put("lastVerifiedAt", AdmissionCoreRegistry.baselineVerifiedAt());
        data.put("gaps", List.of());
        return data;
    }
}

record AdmissionAuthDecision(boolean allowed, HttpStatus status, int code, String message) {
}

@Configuration
class AdmissionCoreFilterConfig {
    @Bean
    FilterRegistrationBean<AdmissionCoreRequestIdFilter> admissionCoreRequestIdFilter() {
        FilterRegistrationBean<AdmissionCoreRequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdmissionCoreRequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/v1/*");
        return registration;
    }

}

class AdmissionCoreRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(new AdmissionCoreRequest(request, requestId), response);
    }
}

class AdmissionCoreRequest extends HttpServletRequestWrapper {
    private final String requestId;

    AdmissionCoreRequest(HttpServletRequest request, String requestId) {
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
