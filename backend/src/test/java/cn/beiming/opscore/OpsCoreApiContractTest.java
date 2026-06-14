package cn.beiming.opscore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpsCoreServiceApplication.class, properties = {
        "server.port=8133",
        "ops-core.http-smoke.gateway-base-url=http://127.0.0.1:1",
        "ops-core.http-smoke.self-base-url=http://127.0.0.1:1",
        "ops-core.http-smoke.timeout-ms=100"
})
@AutoConfigureMockMvc
class OpsCoreApiContractTest {
    private static final int OPS_CORE_PORT = 8133;
    private static final int INHERITED_ROUTES_TOTAL = 219;
    private static final int SELF_ROUTES_TOTAL = 5;
    private static final int ROUTES_TOTAL = INHERITED_ROUTES_TOTAL + SELF_ROUTES_TOTAL;
    private static final String INTERNAL_SIGNING_SECRET = "local-test-gateway-signing-secret";

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
                .andExpect(jsonPath("$.data.modulesTotal").value(7))
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

            mockMvc.perform(trusted(get(path), "GET", path, "gateway-owner", "OWNER", "NODE_READ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(trusted(get(path), "GET", path, "gateway-helper", "HELPER", "NODE_READ"))
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
    void rejectsUnsignedOrTamperedTrustedGatewayContext() throws Exception {
        String path = "/api/v1/ops-core/admin/readiness";

        mockMvc.perform(get(path)
                        .header("X-Gateway-Internal-Request-Id", "req-unsigned-gateway")
                        .header("X-Gateway-Internal-Timestamp", "2026-06-05T00:00:00Z")
                        .header("X-Beiming-Actor-User-Id", "gateway-owner")
                        .header("X-Beiming-Actor-Roles", "OWNER"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(53233));

        mockMvc.perform(get(path)
                        .header("X-Gateway-Internal-Request-Id", "req-bad-signature")
                        .header("X-Gateway-Internal-Timestamp", "2026-06-05T00:00:00Z")
                        .header("X-Gateway-Internal-Signature", "bad-signature")
                        .header("X-Beiming-Actor-User-Id", "gateway-owner")
                        .header("X-Beiming-Actor-Roles", "OWNER"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(53233));

        mockMvc.perform(trusted(get(path), "GET", path, "gateway-owner", "OWNER", "NODE_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trustedGatewaySignatureStatus").value("HMAC_SHA256_CONFIGURED"));
    }

    @Test
    void exposesOpsCoreSummaryAndModuleAssembly() throws Exception {
        mockMvc.perform(get("/api/v1/ops-core/ops/summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("ops-core"))
                .andExpect(jsonPath("$.data.port").value(OPS_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(7))
                .andExpect(jsonPath("$.data.modulesMounted").value(7))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false))
                .andExpect(jsonPath("$.data.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.dependencyAdapterMode").value("SAFE_SNAPSHOT_AND_TEST_ADAPTERS"))
                .andExpect(jsonPath("$.data.serviceDiscoveryMode").value("STATIC_LOCAL_CONFIG"))
                .andExpect(jsonPath("$.data.registeredUpstreams.length()").value(4))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("NOT_RUN"))
                .andExpect(jsonPath("$.data.lastHttpSmokeResults").isArray())
                .andExpect(jsonPath("$.data.trustedGatewaySignatureStatus").value("HMAC_SHA256_CONFIGURED"))
                .andExpect(jsonPath("$.data.routeDriftStatus").value("NO_DRIFT"))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'OPS_CONTROL' && @.pathPrefix == '/api/v1/ops-control' && @.legacyPort == 8116 && @.currentPort == 8133 && @.routesTotal == 31)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CLOUDREVE_SYNC' && @.pathPrefix == '/api/v1/cloudreve-sync' && @.legacyPort == 8118 && @.currentPort == 8133 && @.routesTotal == 16)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'BACKUP_RECOVERY' && @.pathPrefix == '/api/v1/backup-recovery' && @.legacyPort == 8119 && @.currentPort == 8133 && @.routesTotal == 25)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ALERTING' && @.pathPrefix == '/api/v1/alerting' && @.legacyPort == 8120 && @.currentPort == 8133 && @.routesTotal == 24)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'PLUGIN_INTEGRATION' && @.pathPrefix == '/api/v1/plugin-integration' && @.legacyPort == 8122 && @.currentPort == 8133 && @.routesTotal == 38)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CROSS_PLATFORM_NOTIFICATION' && @.pathPrefix == '/api/v1/cross-platform-notification' && @.legacyPort == 8123 && @.currentPort == 8133 && @.routesTotal == 36)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'OPS_IMAGE_MARKET' && @.pathPrefix == '/api/v1/ops-image-market' && @.legacyPort == 8124 && @.currentPort == 8133 && @.routesTotal == 49)]").exists())
                .andExpect(jsonPath("$.data.recentAuditSummary.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/ops-core/admin/modules").header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(7))
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'ops-control' && @.legacyServiceDirectory == 'backend/ops-control-service' && @.legacyServiceRetired == true && @.legacyTestCommand == null && @.localTestDocument == '.local-docs/tests-ops-core.md' && @.currentServiceDirectory == 'backend/ops-core-service' && @.currentPort == 8133 && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'cloudreve-sync' && @.legacyPort == 8118 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/cloudreve-sync')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'backup-recovery' && @.legacyPort == 8119 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/backup-recovery')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'alerting' && @.legacyPort == 8120 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/alerting')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'plugin-integration' && @.legacyPort == 8122 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/plugin-integration')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'cross-platform-notification' && @.legacyServiceDirectory == 'backend/cross-platform-notification-service' && @.legacyPort == 8123 && @.currentServiceDirectory == 'backend/ops-core-service' && @.currentPort == 8133 && @.pathPrefix == '/api/v1/cross-platform-notification' && @.routesTotal == 36 && @.contract == 'docs/contracts-cross-platform-notification.md' && @.localTestDocument == '.local-docs/tests-ops-core.md')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'ops-image-market' && @.legacyPort == 8124 && @.currentPort == 8133 && @.pathPrefix == '/api/v1/ops-image-market')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/ops-core/ops-control')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/ops-core/cloudreve-sync')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/ops-core/cross-platform-notification')]").doesNotExist());
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
                .andExpect(jsonPath("$.data.serviceDiscoveryMode").value("STATIC_LOCAL_CONFIG"))
                .andExpect(jsonPath("$.data.registeredUpstreams.length()").value(4))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("NOT_RUN"))
                .andExpect(jsonPath("$.data.lastHttpSmokeResults").isArray())
                .andExpect(jsonPath("$.data.trustedGatewaySignatureStatus").value("HMAC_SHA256_CONFIGURED"))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CROSS_SERVICE_HTTP' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_AUDIT_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'EXTERNAL_EXECUTOR_CONNECTION' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CLOUDREVE_API' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_REGISTRY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_SCANNER' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_PLUGIN_EVENT_ENTRY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_NOTIFICATION_DELIVERY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_EXTERNAL_SEND' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CALLBACK_SIGNATURE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'PRODUCTION_CREDENTIAL_CUSTODY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'ASYNC_QUEUE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'PERSISTENCE_TRANSACTION' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'TEST_CONTROL_HEADERS' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_HTTP_SMOKE' && @.status == 'NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'TRUSTED_GATEWAY_SIGNATURE' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'INHERITED_ROUTE_DRIFT' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'SENSITIVE_FIELD_SCAN' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'GATEWAY_ROUTE_SWITCH' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.moduleReadiness.length()").value(7))
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real persistence is not connected')]").exists())
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'external node executor is out of repository and not connected')]").exists());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void exposesOpsCoreHttpSmokeRunAndUpdatesReadinessSnapshot() throws Exception {
        mockMvc.perform(post("/api/v1/ops-core/admin/http-smoke/run")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Request-Id", "req-ops-http-smoke"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-ops-http-smoke"))
                .andExpect(jsonPath("$.data.realHttpSmoke").value(true))
                .andExpect(jsonPath("$.data.status").value("DEGRADED"))
                .andExpect(jsonPath("$.data.targetsTotal").value(4))
                .andExpect(jsonPath("$.data.passedTargetsTotal").value(0))
                .andExpect(jsonPath("$.data.failedTargetsTotal").value(4))
                .andExpect(jsonPath("$.data.targets[?(@.targetKey == 'GATEWAY_OPS_CONTROL_OVERVIEW' && @.status == 'FAILED')]").exists())
                .andExpect(jsonPath("$.data.targets[?(@.targetKey == 'GATEWAY_ALERTING_HEALTH' && @.status == 'FAILED')]").exists())
                .andExpect(jsonPath("$.data.targets[?(@.targetKey == 'GATEWAY_CPN_HEALTH' && @.status == 'FAILED')]").exists())
                .andExpect(jsonPath("$.data.targets[?(@.targetKey == 'GATEWAY_OPS_CORE_HEALTH' && @.status == 'FAILED')]").exists());
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
                "/api/v1/ops-core/admin/http-smoke/run",
                "/api/v1/ops-control/overview",
                "/api/v1/cloudreve-sync/health",
                "/api/v1/backup-recovery/health",
                "/api/v1/alerting/health",
                "/api/v1/plugin-integration/health",
                "/api/v1/cross-platform-notification/health",
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
        assertThat(countByPrefix(actualRoutes, "/api/v1/cross-platform-notification")).isEqualTo(36);
        assertThat(countByPrefix(actualRoutes, "/api/v1/ops-image-market")).isEqualTo(49);
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = OpsCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(opscontrol|cloudrevesync|backuprecovery|alerting|pluginintegration|crossplatformnotification|opsimagemarket)\\..*ServiceApplication");
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
                pathFromProject("../ops-image-market-service/pom.xml"),
                pathFromProject("../cross-platform-notification-service/pom.xml"),
                pathFromProject("../cross-platform-notification-service/src/main/resources/application.yml"),
                pathFromProject("../cross-platform-notification-service/src/main/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationServiceApplication.java"),
                pathFromProject("../cross-platform-notification-service/src/main/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationModule.java"),
                pathFromProject("../cross-platform-notification-service/src/test/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationApiContractTest.java"),
                pathFromProject("../cross-platform-notification-service/src/test/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationPortConfigTest.java"),
                pathFromProject("../cross-platform-notification-service/src/test/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationProductionHardeningTest.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).isFalse());
    }

    @Test
    void productionSourceDoesNotContainForbiddenExecutionBoundaries() throws IOException {
        List<Path> moduleSourceRoots = List.of(
                Path.of("src/main/java/cn/beiming/opscore"),
                Path.of("src/main/java/cn/beiming/opscontrol"),
                Path.of("src/main/java/cn/beiming/cloudrevesync"),
                Path.of("src/main/java/cn/beiming/backuprecovery"),
                Path.of("src/main/java/cn/beiming/alerting"),
                Path.of("src/main/java/cn/beiming/pluginintegration"),
                Path.of("src/main/java/cn/beiming/crossplatformnotification"),
                Path.of("src/main/java/cn/beiming/opsimagemarket")
        );
        String source = "";
        for (Path sourceRoot : moduleSourceRoots) {
            if (!Files.exists(sourceRoot)) {
                continue;
            }
            try (var paths = Files.walk(sourceRoot)) {
                source += paths.filter(path -> path.toString().endsWith(".java"))
                        .map(path -> {
                            try {
                                return Files.readString(path);
                            } catch (IOException ex) {
                                throw new IllegalStateException(ex);
                            }
                        })
                        .collect(Collectors.joining("\n")) + "\n";
            }
        }
        assertThat(source).doesNotContain(
                "ProcessBuilder", "Runtime.getRuntime", "DockerClient", "containerd", "kubectl", "helm",
                "skopeo", "crane", "oras", "mcrcon", "RconClient", "registryPassword", "node-secret-token",
                "cloudreve-secret-token", "webhookSecret", "smtpPassword", "manifestPayload", "rawPayload",
                "worldDirectory", "authorized_keys", "id_rsa", "jdbc:", "rm -rf", "Remove-Item -Recurse",
                "rmdir /s", "rd /s", "del /s");
    }

    private MockHttpServletRequestBuilder trusted(MockHttpServletRequestBuilder request, String method, String path, String userId, String roles, String permissions) {
        String requestId = "req-gateway-context";
        String timestamp = Instant.now().toString();
        return request
                .header("X-Gateway-Internal-Request-Id", requestId)
                .header("X-Gateway-Internal-Timestamp", timestamp)
                .header("X-Gateway-Internal-Signature", signature(method, path, requestId, userId, roles, permissions, timestamp))
                .header("X-Beiming-Actor-User-Id", userId)
                .header("X-Beiming-Actor-Roles", roles)
                .header("X-Beiming-Actor-Permissions", permissions);
    }

    private String signature(String method, String path, String requestId, String userId, String roles, String permissions, String timestamp) {
        try {
            String plain = String.join("\n", method, path, requestId, userId, roles, permissions, timestamp, "", "");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(INTERNAL_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
                "contracts-cross-platform-notification.md",
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
                || pattern.startsWith("/api/v1/cross-platform-notification")
                || pattern.startsWith("/api/v1/ops-image-market");
    }

    private long countByPrefix(Set<String> routes, String prefix) {
        return routes.stream().filter(route -> route.contains(" " + prefix)).count();
    }

    private Path docsPath(String file) {
        Path fromModule = Path.of("../docs", file);
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        return Path.of("../docs", file);
    }

    private Path pathFromProject(String value) {
        Path path = Path.of(value);
        if (Files.exists(path) || value.startsWith("../")) {
            return path;
        }
        return Path.of("backend/ops-core-service").resolve(value);
    }
}
