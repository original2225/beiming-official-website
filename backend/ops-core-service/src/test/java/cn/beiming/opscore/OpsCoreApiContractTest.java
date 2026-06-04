package cn.beiming.opscore;

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

@SpringBootTest(classes = OpsCoreServiceApplication.class)
@AutoConfigureMockMvc
class OpsCoreApiContractTest {
    private static final int OPS_CORE_PORT = 8133;
    private static final int INHERITED_ROUTES_TOTAL = 183;
    private static final int SELF_ROUTES_TOTAL = 4;
    private static final int ROUTES_TOTAL = INHERITED_ROUTES_TOTAL + SELF_ROUTES_TOTAL;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port}")
    private String port;

    @Test
    void usesOpsCoreContractPort() {
        assertThat(port).isEqualTo(String.valueOf(OPS_CORE_PORT));
    }

    @Test
    void exposesOpsCoreHealthSummary() throws Exception {
        mockMvc.perform(get("/api/v1/ops-core/health").header("X-Request-Id", "req-ops-core-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-ops-core-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.service").value("ops-core"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(OPS_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(6))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void protectsOpsCoreAdminEndpointsWithLocalAndTrustedGatewayAuth() throws Exception {
        for (String path : List.of("/api/v1/ops-core/ops/summary", "/api/v1/ops-core/admin/modules", "/api/v1/ops-core/admin/readiness")) {
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

            mockMvc.perform(trusted(get(path), "gateway-owner", "OWNER", "NODE_READ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(trusted(get(path), "gateway-helper", "HELPER", "NODE_READ"))
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
    void exposesOpsCoreSummaryAndModuleAssembly() throws Exception {
        mockMvc.perform(get("/api/v1/ops-core/ops/summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("ops-core"))
                .andExpect(jsonPath("$.data.port").value(OPS_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(6))
                .andExpect(jsonPath("$.data.modulesMounted").value(6))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false))
                .andExpect(jsonPath("$.data.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.dependencyAdapterMode").value("SAFE_SNAPSHOT_AND_TEST_ADAPTERS"))
                .andExpect(jsonPath("$.data.routeDriftStatus").value("NO_DRIFT"))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'OPS_CONTROL' && @.pathPrefix == '/api/v1/ops-control' && @.legacyPort == 8116 && @.currentPort == 8133 && @.routesTotal == 31)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CLOUDREVE_SYNC' && @.pathPrefix == '/api/v1/cloudreve-sync' && @.legacyPort == 8118 && @.currentPort == 8133 && @.routesTotal == 16)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'BACKUP_RECOVERY' && @.pathPrefix == '/api/v1/backup-recovery' && @.legacyPort == 8119 && @.currentPort == 8133 && @.routesTotal == 25)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ALERTING' && @.pathPrefix == '/api/v1/alerting' && @.legacyPort == 8120 && @.currentPort == 8133 && @.routesTotal == 24)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'PLUGIN_INTEGRATION' && @.pathPrefix == '/api/v1/plugin-integration' && @.legacyPort == 8122 && @.currentPort == 8133 && @.routesTotal == 38)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'OPS_IMAGE_MARKET' && @.pathPrefix == '/api/v1/ops-image-market' && @.legacyPort == 8124 && @.currentPort == 8133 && @.routesTotal == 49)]").exists())
                .andExpect(jsonPath("$.data.recentAuditSummary.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/ops-core/admin/modules").header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(6))
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'ops-control' && @.legacyServiceDirectory == 'backend/ops-control-service' && @.legacyServiceRetired == true && @.legacyTestCommand == null && @.localTestDocument == '.local-docs/tests-ops-core.md' && @.currentServiceDirectory == 'backend/ops-core-service' && @.currentPort == 8133 && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'cloudreve-sync' && @.legacyPort == 8118 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/cloudreve-sync')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'backup-recovery' && @.legacyPort == 8119 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/backup-recovery')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'alerting' && @.legacyPort == 8120 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/alerting')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'plugin-integration' && @.legacyPort == 8122 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/plugin-integration')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'ops-image-market' && @.legacyPort == 8124 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/ops-image-market')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/ops-core/ops-control')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/ops-core/cloudreve-sync')]").doesNotExist());
    }

    @Test
    void exposesProductionReadinessWithoutHidingBlockedProductionCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/ops-core/admin/readiness").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("ops-core"))
                .andExpect(jsonPath("$.data.port").value(OPS_CORE_PORT))
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
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_NODE_EXECUTION' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CLOUDREVE_API' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_REGISTRY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_SCANNER' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_PLUGIN_EVENT_ENTRY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_NOTIFICATION_DELIVERY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'TEST_CONTROL_HEADERS' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'INHERITED_ROUTE_DRIFT' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'SENSITIVE_FIELD_SCAN' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'GATEWAY_ROUTE_SWITCH' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.moduleReadiness.length()").value(6))
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real persistence is not connected')]").exists())
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real node execution stays in node-daemon and is not connected here')]").exists());
    }

    @Test
    void ignoresInheritedModuleTestControlHeadersByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/ops-control/ops/summary")
                        .header("Authorization", "Bearer ops-viewer-token")
                        .header("X-Test-Auth-Mode", "unavailable")
                        .header("X-Test-Fail-Store", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("ops-control"))
                .andExpect(jsonPath("$.data.port").value(OPS_CORE_PORT))
                .andExpect(jsonPath("$.data.legacyPort").value(8116))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false));
    }

    @Test
    void registersExactlyOpsCoreAndInheritedApiRoutes() {
        long apiRouteMappings = handlerMapping.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/v1/")))
                .count();
        Set<String> apiRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(apiRouteMappings).isEqualTo(ROUTES_TOTAL);
        assertThat(apiRoutes).contains(
                "/api/v1/ops-core/health",
                "/api/v1/ops-core/ops/summary",
                "/api/v1/ops-core/admin/modules",
                "/api/v1/ops-core/admin/readiness",
                "/api/v1/ops-control/overview",
                "/api/v1/cloudreve-sync/health",
                "/api/v1/backup-recovery/health",
                "/api/v1/alerting/health",
                "/api/v1/plugin-integration/health",
                "/api/v1/ops-image-market/health"
        );
    }

    @Test
    void registersEveryInheritedOpsRouteSignatureFromFormalContracts() throws IOException {
        Set<String> actualRoutes = inheritedRouteSignatures();
        Set<String> expectedRoutes = inheritedContractRouteSignatures();

        assertThat(expectedRoutes).hasSize(INHERITED_ROUTES_TOTAL);
        assertThat(actualRoutes).containsExactlyInAnyOrderElementsOf(expectedRoutes);
        assertThat(countByPrefix(actualRoutes, "/api/v1/ops-control")).isEqualTo(31);
        assertThat(countByPrefix(actualRoutes, "/api/v1/cloudreve-sync")).isEqualTo(16);
        assertThat(countByPrefix(actualRoutes, "/api/v1/backup-recovery")).isEqualTo(25);
        assertThat(countByPrefix(actualRoutes, "/api/v1/alerting")).isEqualTo(24);
        assertThat(countByPrefix(actualRoutes, "/api/v1/plugin-integration")).isEqualTo(38);
        assertThat(countByPrefix(actualRoutes, "/api/v1/ops-image-market")).isEqualTo(49);
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = OpsCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(opscontrol|cloudrevesync|backuprecovery|alerting|pluginintegration|opsimagemarket)\\..*ServiceApplication");
        });
    }

    @Test
    void doesNotRestoreMergedLegacyServiceEntrypoints() {
        assertThat(List.of(
                pathFromProject("../auth-service/pom.xml"),
                pathFromProject("../profile-service/pom.xml"),
                pathFromProject("../notification-service/pom.xml"),
                pathFromProject("../content-service/pom.xml"),
                pathFromProject("../server-status-service/pom.xml"),
                pathFromProject("../resource-service/pom.xml"),
                pathFromProject("../admin-service/pom.xml"),
                pathFromProject("../onboarding-service/pom.xml"),
                pathFromProject("../exam-service/pom.xml"),
                pathFromProject("../whitelist-service/pom.xml"),
                pathFromProject("../attendance-service/pom.xml"),
                pathFromProject("../community-service/pom.xml"),
                pathFromProject("../activity-service/pom.xml"),
                pathFromProject("../calendar-service/pom.xml"),
                pathFromProject("../changelog-service/pom.xml"),
                pathFromProject("../ops-control-service/pom.xml"),
                pathFromProject("../cloudreve-sync-service/pom.xml"),
                pathFromProject("../backup-recovery-service/pom.xml"),
                pathFromProject("../alerting-service/pom.xml"),
                pathFromProject("../plugin-integration-service/pom.xml"),
                pathFromProject("../ops-image-market-service/pom.xml")
        )).allSatisfy(path -> assertThat(Files.exists(path)).isFalse());
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
                "skopeo", "crane", "oras", "mcrcon", "RconClient", "registryPassword", "node-secret-token",
                "cloudreve-secret-token", "webhookSecret", "smtpPassword", "manifestPayload", "rawPayload",
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
                        .filter(this::isInheritedOpsPath)
                        .flatMap(pattern -> mapping.getMethodsCondition().getMethods().stream()
                                .map(method -> method.name() + " " + pattern)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> inheritedContractRouteSignatures() throws IOException {
        Set<String> routes = new TreeSet<>();
        for (String file : List.of(
                "contracts-ops-control.md",
                "contracts-cloudreve-sync.md",
                "contracts-backup-recovery.md",
                "contracts-alerting.md",
                "contracts-plugin-integration.md",
                "contracts-ops-image-market.md"
        )) {
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

    private boolean isInheritedOpsPath(String pattern) {
        return pattern.startsWith("/api/v1/ops-control")
                || pattern.startsWith("/api/v1/cloudreve-sync")
                || pattern.startsWith("/api/v1/backup-recovery")
                || pattern.startsWith("/api/v1/alerting")
                || pattern.startsWith("/api/v1/plugin-integration")
                || pattern.startsWith("/api/v1/ops-image-market");
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

    private Path pathFromProject(String value) {
        Path path = Path.of(value);
        if (Files.exists(path) || value.startsWith("../")) {
            return path;
        }
        return Path.of("backend/ops-core-service").resolve(value);
    }
}
