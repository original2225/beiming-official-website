package cn.beiming.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BusinessCoreServiceApplication.class)
@AutoConfigureMockMvc
class BusinessCoreApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port}")
    private String port;

    @Test
    void usesBusinessCoreContractPort() {
        assertThat(port).isEqualTo("8130");
    }

    @Test
    void exposesBusinessCoreHealthSummary() throws Exception {
        mockMvc.perform(get("/api/v1/business-core/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("business-core"))
                .andExpect(jsonPath("$.data.port").value(8130))
                .andExpect(jsonPath("$.data.modulesTotal").value(7))
                .andExpect(jsonPath("$.data.businessRoutesTotal").value(174))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(3));
    }

    @Test
    void exposesBusinessCoreAdminOpsSummaryWithAdminOnlyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/business-core/admin/ops/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/business-core/admin/ops/summary")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/business-core/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("business-core"))
                .andExpect(jsonPath("$.data.gatewaySwitchReady").value(true))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'auth-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'api-gateway-service')]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@ == 'auth-service')]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@ == 'admin-service')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[0].legacyPort").value(8101))
                .andExpect(jsonPath("$.data.moduleRoutes[0].contract").value("docs/contracts-auth.md"))
                .andExpect(jsonPath("$.data.moduleRoutes[0].status").value("READY"))
                .andExpect(jsonPath("$.data.moduleRoutes[0].gaps").isEmpty())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'gateway route switch is not complete')]").doesNotExist())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'full inherited business-core contract suite is not complete')]").doesNotExist());
    }

    @Test
    void exposesProductionReadinessWithoutPretendingProductionIsReady() throws Exception {
        mockMvc.perform(get("/api/v1/business-core/admin/production-readiness"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/business-core/admin/production-readiness")
                        .header("Authorization", "Basic admin-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mockMvc.perform(get("/api/v1/business-core/admin/production-readiness")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/business-core/admin/production-readiness")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/business-core/admin/production-readiness")
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("business-core"));

        MvcResult result = mockMvc.perform(get("/api/v1/business-core/admin/production-readiness")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("business-core"))
                .andExpect(jsonPath("$.data.port").value(8130))
                .andExpect(jsonPath("$.data.productionReady").value(false))
                .andExpect(jsonPath("$.data.readinessStatus").value("NOT_READY"))
                .andExpect(jsonPath("$.data.routeSummary.businessRoutesTotal").value(174))
                .andExpect(jsonPath("$.data.routeSummary.selfRoutesTotal").value(3))
                .andExpect(jsonPath("$.data.routeSummary.routesTotal").value(177))
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'LIVE_GATEWAY_HTTP_SMOKE_NOT_VERIFIED')]").exists())
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'PERSISTENT_DATABASE_NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'PERSISTENT_AUDIT_NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'PRODUCTION_AUTH_CONTEXT_NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'LEGACY_SOURCE_DRIFT_GUARD_REQUIRED')]").doesNotExist())
                .andExpect(jsonPath("$.data.blockingGaps[?(@.gapKey == 'TEST_CONTROL_HEADERS_REQUIRE_PRODUCTION_GUARD')]").doesNotExist())
                .andExpect(jsonPath("$.data.gapsTotal").value(4))
                .andExpect(jsonPath("$.data.criticalGapsTotal").value(0))
                .andExpect(jsonPath("$.data.highGapsTotal").value(4))
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'LIVE_GATEWAY_HTTP_SMOKE' && @.status == 'NOT_VERIFIED' && @.requiredBeforeProduction == true)]").exists())
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'PERSISTENT_DATABASE' && @.status == 'NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'PERSISTENT_AUDIT' && @.status == 'NOT_CONNECTED')]").exists())
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'PRODUCTION_AUTH_CONTEXT' && @.status != 'PASS')]").exists())
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'GATEWAY_INTERNAL_SIGNATURE' && @.status != 'PASS')]").exists())
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'SOURCE_DRIFT_GUARD' && @.status == 'PASS' && @.requiredBeforeProduction == false)]").exists())
                .andExpect(jsonPath("$.data.testScope.mockMvcContractTests.status").value("PASS"))
                .andExpect(jsonPath("$.data.testScope.legacyBaselineTests.status").value("RETIRED"))
                .andExpect(jsonPath("$.data.testScope.liveHttpSmokeTests.status").value("NOT_VERIFIED"))
                .andExpect(jsonPath("$.data.integrationChecks[?(@.checkKey == 'TEST_CONTROL_GUARD' && @.status == 'PASS')]").exists())
                .andExpect(jsonPath("$.data.testControls.productionGuardRequired").value(true))
                .andExpect(jsonPath("$.data.testControls.productionGuardStatus").value("ENFORCED_OUTSIDE_TEST_MODE"))
                .andExpect(jsonPath("$.data.testControls.knownControlHeaders[?(@ == 'X-Test-Fail-Audit')]").exists())
                .andExpect(jsonPath("$.data.testControls.knownControlHeaders[?(@ == 'X-Test-Notification-Mode')]").exists())
                .andExpect(jsonPath("$.data.sourceDrift.risk").value("LEGACY_SOURCE_RETIRED"))
                .andExpect(jsonPath("$.data.sourceDrift.guardRequired").value(false))
                .andExpect(jsonPath("$.data.nextDevelopmentOrder[0]").value("LIVE_GATEWAY_HTTP_SMOKE"))
                .andExpect(jsonPath("$.data.nextDevelopmentOrder[?(@ == 'PERSISTENCE_AND_AUDIT')]").exists())
                .andExpect(jsonPath("$.data.nextDevelopmentOrder[?(@ == 'SOURCE_DRIFT_GUARD')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselinesKept").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("jdbc:")
                .doesNotContain("Exception")
                .doesNotContain("sharePassword")
                .doesNotContain("nodeSecret");
    }

    @Test
    void mountsRepresentativeFirstBatchRoutesWithoutPathRewrite() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));
        mockMvc.perform(get("/api/v1/profile/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/notifications/me/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));
        mockMvc.perform(get("/api/v1/content/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/server-status/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));
    }

    @Test
    void generatesOneRequestIdAcrossMergedFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBodyRequestId = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.requestId");

        assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo(responseBodyRequestId);
    }

    @Test
    void registersExactlyFirstBatchAndSelfApiRoutes() {
        long apiRouteMappings = handlerMapping.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/v1/")))
                .count();
        Set<String> apiRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(apiRouteMappings).isEqualTo(177);
        assertThat(apiRoutes).contains(
                "/api/v1/auth/me",
                "/api/v1/profile/members",
                "/api/v1/notifications/me/unread-count",
                "/api/v1/content/home",
                "/api/v1/server-status/overview",
                "/api/v1/resources",
                "/api/v1/admin/overview",
                "/api/v1/business-core/health",
                "/api/v1/business-core/admin/ops/summary",
                "/api/v1/business-core/admin/production-readiness"
        );
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = BusinessCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(auth|profile|notification|content|serverstatus|resource|admin)\\..*ServiceApplication");
        });
    }

}
