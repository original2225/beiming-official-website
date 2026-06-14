package cn.beiming.portalcore;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PortalCoreServiceApplication.class, properties = "server.port=8134")
@AutoConfigureMockMvc
class PortalCoreApiContractTest {
    private static final int PORTAL_CORE_PORT = 8134;
    private static final int INHERITED_ROUTES_TOTAL = 108;
    private static final int SELF_ROUTES_TOTAL = 5;
    private static final int ROUTES_TOTAL = INHERITED_ROUTES_TOTAL + SELF_ROUTES_TOTAL;
    private static final PortalSmokeTestServer SMOKE_SERVER = PortalSmokeTestServer.start();

    @DynamicPropertySource
    static void portalCoreSmokeProperties(DynamicPropertyRegistry registry) {
        registry.add("portal-core.http-smoke.gateway-base-url", SMOKE_SERVER::baseUrl);
        registry.add("portal-core.http-smoke.timeout-ms", () -> "1000");
    }

    @AfterAll
    static void stopSmokeServer() {
        SMOKE_SERVER.stop();
    }

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
                .andExpect(jsonPath("$.data.modulesTotal").value(3))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.livenessStatus").value("LIVE"))
                .andExpect(jsonPath("$.data.readinessProbePath").value("/api/v1/portal-core/admin/readiness"))
                .andExpect(jsonPath("$.data.startupProbePath").value("/api/v1/portal-core/health"))
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
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void protectsPortalCoreSmokeEndpointWithLocalAndTrustedGatewayAuth() throws Exception {
        String path = "/api/v1/portal-core/admin/http-smoke/run";
        mockMvc.perform(post(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(post(path).header("Authorization", "Basic admin-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mockMvc.perform(post(path).header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(post(path).header("Authorization", "Bearer helper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(post(path).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(path).header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(trusted(post(path), "gateway-owner", "OWNER", "CONTENT_MANAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(trusted(post(path), "gateway-helper", "HELPER", "CONTENT_MANAGE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
    }

    @Test
    void exposesPortalCoreSummaryAndModuleAssembly() throws Exception {
        mockMvc.perform(get("/api/v1/portal-core/ops/summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"))
                .andExpect(jsonPath("$.data.port").value(PORTAL_CORE_PORT))
                .andExpect(jsonPath("$.data.modulesTotal").value(3))
                .andExpect(jsonPath("$.data.modulesMounted").value(3))
                .andExpect(jsonPath("$.data.inheritedRoutesTotal").value(INHERITED_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(SELF_ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.routesTotal").value(ROUTES_TOTAL))
                .andExpect(jsonPath("$.data.testControlsEnabled").value(false))
                .andExpect(jsonPath("$.data.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.dependencyAdapterMode").value("SAFE_SNAPSHOT_AND_TEST_ADAPTERS"))
                .andExpect(jsonPath("$.data.serviceDiscoveryMode").value("STATIC_LOCAL_REGISTRY"))
                .andExpect(jsonPath("$.data.registeredUpstreams[?(@.serviceKey == 'API_GATEWAY' && @.baseUrl == '" + SMOKE_SERVER.baseUrl() + "' && @.discoverySource == 'STATIC_LOCAL_REGISTRY')]").exists())
                .andExpect(jsonPath("$.data.registeredUpstreams[?(@.serviceKey == 'PORTAL_CORE' && @.port == 8134 && @.pathPrefix == '/api/v1/portal-core')]").exists())
                .andExpect(jsonPath("$.data.registeredUpstreams[?(@.serviceKey == 'GUIDE' && @.pathPrefix == '/api/v1/guides')]").exists())
                .andExpect(jsonPath("$.data.registeredUpstreams[?(@.serviceKey == 'MATERIAL' && @.pathPrefix == '/api/v1/materials')]").exists())
                .andExpect(jsonPath("$.data.registeredUpstreams[?(@.serviceKey == 'ONLINE_MAP' && @.pathPrefix == '/api/v1/online-map')]").exists())
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("NOT_RUN"))
                .andExpect(jsonPath("$.data.httpSmokeTargets.length()").value(3))
                .andExpect(jsonPath("$.data.lastHttpSmokeAt").value(nullValue()))
                .andExpect(jsonPath("$.data.lastHttpSmokeResults.length()").value(0))
                .andExpect(jsonPath("$.data.operationalProfile.profileVersion").value("portal-core-operational-profile-v1"))
                .andExpect(jsonPath("$.data.operationalProfile.domainBoundary").value("PORTAL_EXPERIENCE_CORE"))
                .andExpect(jsonPath("$.data.operationalProfile.referenceModel[?(@ == 'KUBERNETES_PROBES')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.referenceModel[?(@ == 'SPRING_BOOT_AVAILABILITY')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.referenceModel[?(@ == 'GOOGLE_SRE_SLO')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.referenceModel[?(@ == 'UBER_DOMA')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.livenessStatus").value("LIVE"))
                .andExpect(jsonPath("$.data.operationalProfile.readinessGateStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.operationalProfile.releaseGateStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.operationalProfile.trafficEligibility").value("INTERNAL_AND_TEST_ONLY"))
                .andExpect(jsonPath("$.data.operationalProfile.probeRecommendations.livenessPath").value("/api/v1/portal-core/health"))
                .andExpect(jsonPath("$.data.operationalProfile.probeRecommendations.readinessPath").value("/api/v1/portal-core/admin/readiness"))
                .andExpect(jsonPath("$.data.operationalProfile.probeRecommendations.startupPath").value("/api/v1/portal-core/health"))
                .andExpect(jsonPath("$.data.operationalProfile.probeRecommendations.externalDependenciesInLiveness").value(false))
                .andExpect(jsonPath("$.data.operationalProfile.sloTargets[?(@.sloKey == 'ROUTE_DRIFT_ZERO' && @.target == '0 drifted inherited routes' && @.currentStatus == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.sloTargets[?(@.sloKey == 'TEST_CONTROLS_DISABLED' && @.currentStatus == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.sloTargets[?(@.sloKey == 'HTTP_SMOKE_ALL_TARGETS' && @.currentStatus == 'NOT_RUN')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.sloTargets[?(@.sloKey == 'PRODUCTION_BLOCKERS_ZERO' && @.currentStatus == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'INHERITED_ROUTE_DRIFT' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'GATEWAY_ROUTE_SWITCH' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'REAL_HTTP_SMOKE' && @.status == 'NOT_RUN')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'REAL_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'DYNAMIC_SERVICE_DISCOVERY' && @.status == 'PARTIAL')]").exists())
                .andExpect(jsonPath("$.data.routeDriftStatus").value("NO_DRIFT"))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'GUIDE' && @.pathPrefix == '/api/v1/guides' && @.legacyPort == 8127 && @.currentPort == 8134 && @.routesTotal == 41)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'MATERIAL' && @.pathPrefix == '/api/v1/materials' && @.legacyPort == 8126 && @.currentPort == 8134 && @.routesTotal == 33)]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ONLINE_MAP' && @.pathPrefix == '/api/v1/online-map' && @.legacyPort == 8121 && @.currentPort == 8134 && @.routesTotal == 34)]").exists())
                .andExpect(jsonPath("$.data.recentAuditSummary.storageMode").value("IN_MEMORY_CONTRACT_STUBS"))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/portal-core/admin/modules").header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'guide' && @.legacyServiceDirectory == 'backend/guide-service' && @.legacyTestCommand == 'RETIRED_NO_MAVEN_ENTRY' && @.currentServiceDirectory == 'backend/portal-core-service' && @.legacyPort == 8127 && @.currentPort == 8134 && @.pathPrefix == '/api/v1/guides' && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'material' && @.legacyServiceDirectory == 'backend/material-service' && @.legacyTestCommand == 'RETIRED_NO_MAVEN_ENTRY' && @.currentServiceDirectory == 'backend/portal-core-service' && @.legacyPort == 8126 && @.currentPort == 8134 && @.pathPrefix == '/api/v1/materials' && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.moduleName == 'online-map' && @.legacyServiceDirectory == 'backend/online-map-service' && @.legacyTestCommand == 'RETIRED_NO_MAVEN_ENTRY' && @.currentServiceDirectory == 'backend/portal-core-service' && @.legacyPort == 8121 && @.currentPort == 8134 && @.pathPrefix == '/api/v1/online-map' && @.routeDriftStatus == 'NO_DRIFT')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/portal-core/guides')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/portal-core/materials')]").doesNotExist())
                .andExpect(jsonPath("$.data.items[?(@.pathPrefix == '/api/v1/portal-core/online-map')]").doesNotExist());
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
                .andExpect(jsonPath("$.data.serviceDiscoveryMode").value("STATIC_LOCAL_REGISTRY"))
                .andExpect(jsonPath("$.data.registeredUpstreams.length()").value(5))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("NOT_RUN"))
                .andExpect(jsonPath("$.data.httpSmokeTargets[?(@.targetKey == 'GATEWAY_GUIDE_CATEGORIES' && @.serviceKey == 'GUIDE' && @.expectedBusinessCode == 0)]").exists())
                .andExpect(jsonPath("$.data.httpSmokeTargets[?(@.targetKey == 'GATEWAY_MATERIAL_FEATURED' && @.serviceKey == 'MATERIAL' && @.expectedBusinessCode == 0)]").exists())
                .andExpect(jsonPath("$.data.httpSmokeTargets[?(@.targetKey == 'GATEWAY_ONLINE_MAP_HEALTH' && @.serviceKey == 'ONLINE_MAP' && @.expectedBusinessCode == 0)]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.profileVersion").value("portal-core-operational-profile-v1"))
                .andExpect(jsonPath("$.data.operationalProfile.domainBoundary").value("PORTAL_EXPERIENCE_CORE"))
                .andExpect(jsonPath("$.data.operationalProfile.livenessStatus").value("LIVE"))
                .andExpect(jsonPath("$.data.operationalProfile.readinessGateStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.operationalProfile.releaseGateStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.operationalProfile.trafficEligibility").value("INTERNAL_AND_TEST_ONLY"))
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'REAL_EXTERNAL_DEPENDENCIES' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.lastHttpSmokeAt").value(nullValue()))
                .andExpect(jsonPath("$.data.lastHttpSmokeResults.length()").value(0))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_CROSS_SERVICE_HTTP' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_AUDIT_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_OBJECT_STORAGE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_FILE_SECURITY_SCANNER' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_FULLTEXT_SEARCH' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_NOTIFICATION_DELIVERY' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_MAP_PROVIDER_HTTP' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_MARKER_SYNC' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_TILE_HOSTING' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'SERVICE_DISCOVERY' && @.status == 'PARTIAL')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_HTTP_SMOKE' && @.status == 'NOT_RUN')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'TEST_CONTROL_HEADERS' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'INHERITED_ROUTE_DRIFT' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'SENSITIVE_FIELD_SCAN' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'GATEWAY_ROUTE_SWITCH' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.moduleReadiness.length()").value(3))
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real persistence is not connected')]").exists())
                .andExpect(jsonPath("$.data.productionBlockers[?(@ == 'real object storage is not connected')]").exists());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void runsConfiguredGatewayHttpSmokeAndPersistsLatestInProcessResult() throws Exception {
        SMOKE_SERVER.prepare(
                SmokeResponse.success(),
                SmokeResponse.success(),
                SmokeResponse.success()
        );

        mockMvc.perform(post("/api/v1/portal-core/admin/http-smoke/run")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Request-Id", "req-portal-smoke-pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"))
                .andExpect(jsonPath("$.data.serviceDiscoveryMode").value("STATIC_LOCAL_REGISTRY"))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("PASS"))
                .andExpect(jsonPath("$.data.targets.length()").value(3))
                .andExpect(jsonPath("$.data.results.length()").value(3))
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_GUIDE_CATEGORIES' && @.status == 'PASS' && @.httpStatus == 200 && @.businessCode == 0)]").exists())
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_MATERIAL_FEATURED' && @.status == 'PASS' && @.httpStatus == 200 && @.businessCode == 0)]").exists())
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_ONLINE_MAP_HEALTH' && @.status == 'PASS' && @.httpStatus == 200 && @.businessCode == 0)]").exists());

        assertThat(SMOKE_SERVER.requestPaths()).containsExactly("/api/v1/guides/categories", "/api/v1/materials/featured", "/api/v1/online-map/health");
        assertThat(SMOKE_SERVER.requestIds()).containsExactly("req-portal-smoke-pass", "req-portal-smoke-pass", "req-portal-smoke-pass");

        mockMvc.perform(get("/api/v1/portal-core/admin/readiness").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("PASS"))
                .andExpect(jsonPath("$.data.operationalProfile.releaseGateStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.operationalProfile.trafficEligibility").value("INTERNAL_AND_TEST_ONLY"))
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'REAL_HTTP_SMOKE' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.operationalProfile.releaseGates[?(@.gateKey == 'REAL_PERSISTENCE' && @.status == 'BLOCKED')]").exists())
                .andExpect(jsonPath("$.data.lastHttpSmokeResults.length()").value(3))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_HTTP_SMOKE' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.readyForProduction").value(false));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void marksHttpSmokeDegradedWithoutFailingTheReadinessEndpoint() throws Exception {
        SMOKE_SERVER.prepare(
                SmokeResponse.success(),
                SmokeResponse.success(),
                new SmokeResponse(500, "{\"code\":51700,\"message\":\"material failure\",\"data\":null}")
        );

        mockMvc.perform(post("/api/v1/portal-core/admin/http-smoke/run")
                        .header("Authorization", "Bearer owner-token")
                        .header("X-Request-Id", "req-portal-smoke-degraded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_GUIDE_CATEGORIES' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_MATERIAL_FEATURED' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_ONLINE_MAP_HEALTH' && @.status == 'FAILED' && @.httpStatus == 500)]").exists())
                .andExpect(jsonPath("$.data.results[?(@.targetKey == 'GATEWAY_ONLINE_MAP_HEALTH')].failureReason").isNotEmpty());

        mockMvc.perform(get("/api/v1/portal-core/admin/readiness").header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readyForProduction").value(false))
                .andExpect(jsonPath("$.data.httpSmokeStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'REAL_HTTP_SMOKE' && @.status == 'DEGRADED')]").exists());
    }

    @Test
    void rejectsNonHttpSmokeBaseUrlAsConfigurationError() {
        PortalCoreSmokeCoordinator smoke = new PortalCoreSmokeCoordinator(8134, "file:///tmp/gateway", 1000, new com.fasterxml.jackson.databind.ObjectMapper());

        assertThatThrownBy(() -> smoke.run("req-invalid-smoke-url"))
                .isInstanceOf(PortalCoreException.class)
                .extracting("code")
                .isEqualTo(50000);
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
                "/api/v1/portal-core/admin/http-smoke/run",
                "/api/v1/guides/home",
                "/api/v1/guides/admin/ops/summary",
                "/api/v1/materials/featured",
                "/api/v1/materials/admin/ops/summary",
                "/api/v1/online-map/health",
                "/api/v1/online-map/admin/ops/summary"
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
        assertThat(countByPrefix(actualRoutes, "/api/v1/online-map")).isEqualTo(34);
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = PortalCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(guide|material|onlinemap)\\..*ServiceApplication");
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
                "material-service",
                "online-map-service"
        )).allSatisfy(serviceDirectory ->
                assertThat(retiredServicePomCandidates(serviceDirectory))
                        .allSatisfy(path -> assertThat(Files.exists(path)).isFalse()));
    }

    @Test
    void productionSourceDoesNotContainForbiddenExecutionBoundaries() throws IOException {
        List<Path> moduleSourceRoots = List.of(
                Path.of("src/main/java/cn/beiming/portalcore"),
                Path.of("src/main/java/cn/beiming/guide"),
                Path.of("src/main/java/cn/beiming/material"),
                Path.of("src/main/java/cn/beiming/onlinemap")
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
        for (String file : List.of("contracts-guide.md", "contracts-material.md", "contracts-online-map.md")) {
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
        return pattern.startsWith("/api/v1/guides")
                || pattern.startsWith("/api/v1/materials")
                || pattern.startsWith("/api/v1/online-map");
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

    private List<Path> retiredServicePomCandidates(String serviceDirectory) {
        return List.of(
                Path.of("backend", serviceDirectory, "pom.xml"),
                Path.of("..", serviceDirectory, "pom.xml")
        );
    }

    private static final class PortalSmokeTestServer {
        private final ServerSocket serverSocket;
        private final Thread thread;
        private final LinkedBlockingQueue<SmokeResponse> responses = new LinkedBlockingQueue<>();
        private final List<String> requestPaths = Collections.synchronizedList(new ArrayList<>());
        private final List<String> requestIds = Collections.synchronizedList(new ArrayList<>());

        private PortalSmokeTestServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.thread = new Thread(this::acceptLoop, "portal-smoke-test-server");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        static PortalSmokeTestServer start() {
            try {
                return new PortalSmokeTestServer(new ServerSocket(0));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        void prepare(SmokeResponse... nextResponses) {
            responses.clear();
            requestPaths.clear();
            requestIds.clear();
            for (SmokeResponse response : nextResponses) {
                responses.add(response);
            }
        }

        List<String> requestPaths() {
            awaitRequests();
            synchronized (requestPaths) {
                return List.copyOf(requestPaths);
            }
        }

        List<String> requestIds() {
            awaitRequests();
            synchronized (requestIds) {
                return List.copyOf(requestIds);
            }
        }

        void stop() {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    handle(socket);
                } catch (IOException ignored) {
                    if (serverSocket.isClosed()) {
                        return;
                    }
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            socket.setSoTimeout(2000);
            InputStream input = socket.getInputStream();
            String request = readHeaders(input);
            requestPaths.add(parsePath(request));
            requestIds.add(parseHeader(request, "X-Request-Id"));
            SmokeResponse response = responses.poll();
            if (response == null) {
                response = SmokeResponse.success();
            }
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            String statusText = response.status() >= 500 ? "Internal Server Error" : "OK";
            String head = "HTTP/1.1 " + response.status() + " " + statusText + "\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + body.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            OutputStream output = socket.getOutputStream();
            output.write(head.getBytes(StandardCharsets.UTF_8));
            output.write(body);
            output.flush();
        }

        private String readHeaders(InputStream input) throws IOException {
            StringBuilder builder = new StringBuilder();
            int previous = -1;
            int current;
            while ((current = input.read()) != -1) {
                builder.append((char) current);
                if (previous == '\r' && current == '\n' && builder.toString().endsWith("\r\n\r\n")) {
                    break;
                }
                previous = current;
            }
            return builder.toString();
        }

        private String parsePath(String request) {
            String firstLine = request.lines().findFirst().orElse("");
            String[] parts = firstLine.split(" ");
            return parts.length >= 2 ? parts[1] : "";
        }

        private String parseHeader(String request, String name) {
            String prefix = name.toLowerCase() + ":";
            return request.lines()
                    .filter(line -> line.toLowerCase().startsWith(prefix))
                    .map(line -> line.substring(line.indexOf(':') + 1).trim())
                    .findFirst()
                    .orElse("");
        }

        private void awaitRequests() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (requestPaths.size() < 3 && System.nanoTime() < deadline) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private record SmokeResponse(int status, String body) {
        static SmokeResponse success() {
            return new SmokeResponse(200, "{\"code\":0,\"message\":\"success\",\"data\":{\"ok\":true}}");
        }
    }
}
