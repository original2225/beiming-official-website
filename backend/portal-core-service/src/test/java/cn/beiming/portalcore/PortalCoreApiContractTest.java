package cn.beiming.portalcore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PortalCoreServiceApplication.class)
@AutoConfigureMockMvc
class PortalCoreApiContractTest {
    private static final int PORTAL_CORE_PORT = 8134;
    private static final int INHERITED_ROUTES_TOTAL = 74;
    private static final int SELF_ROUTES_TOTAL = 4;
    private static final int ROUTES_TOTAL = INHERITED_ROUTES_TOTAL + SELF_ROUTES_TOTAL;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port}")
    private String port;

    @Test
    void usesPortalCoreContractPort() {
        assertThat(port).isEqualTo(String.valueOf(PORTAL_CORE_PORT));
    }

    @Test
    void exposesPortalCoreHealthSummary() throws Exception {
        mockMvc.perform(get("/api/v1/portal-core/health").header("X-Request-Id", "req-portal-core-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-portal-core-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.service").value("portal-core"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(2))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void protectsPortalCoreAdminEndpointsWithLocalAndTrustedGatewayAuth() throws Exception {
        for (String path : List.of("/api/v1/portal-core/ops/summary", "/api/v1/portal-core/admin/modules", "/api/v1/portal-core/admin/readiness")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(41000));

            mockMvc.perform(get(path).header("Authorization", "Basic admin-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(41003));

            mockMvc.perform(get(path).header("Authorization", "Bearer user-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(42001));

            mockMvc.perform(get(path).header("Authorization", "Bearer helper-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(42001));

            mockMvc.perform(get(path).header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get(path).header("Authorization", "Bearer owner-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(trusted(get(path), "gateway-owner", "OWNER", "CONTENT_MANAGE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(trusted(get(path), "gateway-helper", "HELPER", "CONTENT_MANAGE"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(42001));

            mockMvc.perform(get(path)
                            .header("X-Gateway-Internal-Request-Id", "bad request id")
                            .header("X-Beiming-Actor-User-Id", "gateway-owner")
                            .header("X-Beiming-Actor-Roles", "OWNER"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value(53233));

            mockMvc.perform(get(path)
                            .header("X-Gateway-Internal-Request-Id", "req-missing-user")
                            .header("X-Beiming-Actor-Roles", "OWNER"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value(53233));
        }
    }

    @Test
    void exposesPortalCoreSummaryAndModuleAssembly() throws Exception {
        mockMvc.perform(get("/api/v1/portal-core/ops/summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(2))
                .andExpect(jsonPath("$.data.modulesMounted").value(2))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false))
                .andExpect(jsonPath("$.data.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.dependencyAdapterMode").value("SAFE_SNAPSHOT_AND_TEST_ADAPTERS"))
                .andExpect(jsonPath("$.data.routeDriftStatus").value("NO_DRIFT"))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'GUIDE' && @.pathPrefix == '/api/v1/guides' && @.legacyPort == 8127 && @.currentPort == 8134 && @.routesTotal == 41)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'MATERIAL' && @.pathPrefix == '/api/v1/materials' && @.legacyPort == 8126 && @.currentPort == 8134 && @.routesTotal == 33)]").exists())
                .andExpect(jsonPath("$.data.recentAuditSummary.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/portal-core/admin/modules").header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'guide' && @.legacyServiceDirectory == 'backend/guide-service' && @.legacyTestCommand == 'RETIRED_NO_MAVEN_ENTRY' && @.currentServiceDirectory == 'backend/portal-core-service' && @.legacyPort == 8127 && @.currentPort == 8134 && @.pathPrefix == '/api/v1/guides' && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'material' && @.legacyServiceDirectory == 'backend/material-service' && @.legacyTestCommand == 'RETIRED_NO_MAVEN_ENTRY' && @.currentServiceDirectory == 'backend/portal-core-service' && @.legacyPort == 8126 && @.currentPort == 8134 && @.pathPrefix == '/api/v1/materials' && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/portal-core/guides')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/portal-core/materials')]").doesNotExist());
    }

    @Test
    void exposesProductionReadinessWithoutHidingBlockedProductionCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/portal-core/admin/readiness").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.readyForProduction").value(false))
                .andExpect(jsonPath("$.data.readinessStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routeDriftStatus").value("NO_DRIFT"))
                .andExpect(jsonPath("$.data.legacyServiceRestoreStatus").value("NOT_RESTORED"))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.testControlHeadersStatus").value("DISABLED_BY_DEFAULT"))
                .andExpect(jsonPath("$.data.sensitiveFieldScanStatus").value("PASS"))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CROSS_SERVICE_HTTP' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_AUDIT_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_OBJECT_STORAGE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_FILE_SECURITY_SCANNER' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_FULLTEXT_SEARCH' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_NOTIFICATION_DELIVERY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_HTTP_SMOKE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'TEST_CONTROL_HEADERS' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'INHERITED_ROUTE_DRIFT' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'SENSITIVE_FIELD_SCAN' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'GATEWAY_ROUTE_SWITCH' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.moduleReadiness.length()").value(2))
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real persistence is not connected')]").exists())
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real object storage is not connected')]").exists());
    }

    @Test
    void ignoresInheritedModuleTestControlHeadersByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/guides/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Test-Profile-Mode", "unavailable")
                        .header("X-Test-Fail-Audit", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("guide"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.legacyPort").value(8127))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false));

        mockMvc.perform(get("/api/v1/materials/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Test-Notification-Mode", "unavailable")
                        .header("X-Test-Fail-Store", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("material"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.legacyPort").value(8126))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false));
    }

    @Test
    void registersExactlyPortalCoreAndInheritedApiRoutes() {
        long apiRouteMappings = handlerMapping.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/v1/")))
                .count();
        Set<String> apiRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(apiRouteMappings).isEqualTo(ROUTES_TOTAL);
        assertThat(apiRoutes).contains(
                "/api/v1/portal-core/health",
                "/api/v1/portal-core/ops/summary",
                "/api/v1/portal-core/admin/modules",
                "/api/v1/portal-core/admin/readiness",
                "/api/v1/guides/home",
                "/api/v1/guides/admin/ops/summary",
                "/api/v1/materials/featured",
                "/api/v1/materials/admin/ops/summary"
        );
    }

    @Test
    void registersEveryInheritedPortalRouteSignatureFromFormalContracts() throws IOException {
        Set<String> actualRoutes = inheritedRouteSignatures();
        Set<String> expectedRoutes = inheritedContractRouteSignatures();

        assertThat(expectedRoutes).hasSize(INHERITED_ROUTES_TOTAL);
        assertThat(actualRoutes).containsExactlyInAnyOrderElementsOf(expectedRoutes);
        assertThat(countByPrefix(actualRoutes, "/api/v1/guides")).isEqualTo(41);
        assertThat(countByPrefix(actualRoutes, "/api/v1/materials")).isEqualTo(33);
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = PortalCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(guide|material)\\..*ServiceApplication");
        });
    }

    @Test
    void doesNotRestoreRetiredLegacyServiceEntrypoints() {
        assertThat(List.of(
                "auth-service",
                "profile-service",
                "notification-service",
                "content-service",
                "server-status-service",
                "resource-service",
                "admin-service",
                "onboarding-service",
                "exam-service",
                "whitelist-service",
                "attendance-service",
                "community-service",
                "activity-service",
                "calendar-service",
                "changelog-service",
                "guide-service",
                "material-service"
        )).allSatisfy(serviceDirectory ->
                assertThat(retiredServicePomCandidates(serviceDirectory))
                        .allSatisfy(path -> assertThat(Files.exists(path)).isFalse()));
    }

    @Test
    void productionSourceDoesNotContainForbiddenExecutionBoundaries() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        String source = "";
        if (Files.exists(sourceRoot)) {
            try (var paths = Files.walk(sourceRoot)) {
                source = paths.filter(path -> path.toString().endsWith(".java"))
                        .map(path -> {
                            try {
                                return Files.readString(path);
                            } catch (IOException ex) {
                                throw new IllegalStateException(ex);
                            }
                        })
                        .collect(Collectors.joining("\n"));
            }
        }
        assertThat(source).doesNotContain(
                "ProcessBuilder", "Runtime.getRuntime", "DockerClient", "containerd", "kubectl", "helm",
                "objectStorageKey", "storageSecret", "sharePassword", "cloudreve-secret-token",
                "webhookSecret", "smtpPassword", "botToken", "uploadTicketLedger", "internalPath",
                "worldDirectory", "authorized_keys", "id_rsa", "jdbc:", "rm -rf", "Remove-Item -Recurse",
                "rmdir /s", "rd /s", "del /s");
    }

    private MockHttpServletRequestBuilder trusted(MockHttpServletRequestBuilder request, String userId, String roles, String permissions) {
        return request
                .header("X-Gateway-Internal-Request-Id", "req-gateway-context")
                .header("X-Beiming-Actor-User-Id", userId)
                .header("X-Beiming-Actor-Roles", roles)
                .header("X-Beiming-Actor-Permissions", permissions);
    }

    private Set<String> inheritedRouteSignatures() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()
                        .filter(this::isInheritedPortalPath)
                        .flatMap(pattern -> mapping.getMethodsCondition().getMethods().stream()
                                .map(method -> method.name() + " " + pattern)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> inheritedContractRouteSignatures() throws IOException {
        Set<String> routes = new TreeSet<>();
        for (String file : List.of("contracts-guide.md", "contracts-material.md")) {
            Path path = docsPath(file);
            Pattern row = Pattern.compile("\\|[^|]+\\|\\s*(GET|POST|PUT|PATCH|DELETE)\\s*\\|\\s*`([^`]+)`");
            for (String line : Files.readAllLines(path)) {
                var matcher = row.matcher(line);
                if (matcher.find()) {
                    routes.add(matcher.group(1) + " " + matcher.group(2));
                }
            }
        }
        return routes;
    }

    private boolean isInheritedPortalPath(String pattern) {
        return pattern.startsWith("/api/v1/guides") || pattern.startsWith("/api/v1/materials");
    }

    private long countByPrefix(Set<String> routes, String prefix) {
        return routes.stream().filter(route -> route.contains(" " + prefix)).count();
    }

    private Path docsPath(String file) {
        Path fromModule = Path.of("../../docs", file);
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        return Path.of("docs", file);
    }

    private List<Path> retiredServicePomCandidates(String serviceDirectory) {
        return List.of(
                Path.of("backend", serviceDirectory, "pom.xml"),
                Path.of("..", serviceDirectory, "pom.xml")
        );
    }
}
