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
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(2));
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
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'auth-service')]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'api-gateway-service')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[0].legacyPort").value(8101))
                .andExpect(jsonPath("$.data.moduleRoutes[0].contract").value("docs/contracts-auth.md"))
                .andExpect(jsonPath("$.data.moduleRoutes[0].status").value("READY"))
                .andExpect(jsonPath("$.data.moduleRoutes[0].gaps").isEmpty())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'gateway route switch is not complete')]").doesNotExist())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'full inherited business-core contract suite is not complete')]").doesNotExist());
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

        assertThat(apiRouteMappings).isEqualTo(176);
        assertThat(apiRoutes).contains(
                "/api/v1/auth/me",
                "/api/v1/profile/members",
                "/api/v1/notifications/me/unread-count",
                "/api/v1/content/home",
                "/api/v1/server-status/overview",
                "/api/v1/resources",
                "/api/v1/admin/overview",
                "/api/v1/business-core/health",
                "/api/v1/business-core/admin/ops/summary"
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
