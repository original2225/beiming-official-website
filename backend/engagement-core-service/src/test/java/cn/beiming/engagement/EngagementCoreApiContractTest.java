package cn.beiming.engagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EngagementCoreServiceApplication.class)
@AutoConfigureMockMvc
class EngagementCoreApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port}")
    private String port;

    @Test
    void usesEngagementCoreContractPort() {
        assertThat(port).isEqualTo("8132");
    }

    @Test
    void exposesEngagementCoreHealthSummary() throws Exception {
        mockMvc.perform(get("/api/v1/engagement-core/health").header("X-Request-Id", "req-engagement-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-engagement-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("engagement-core"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.modulesTotal").value(4))
                .andExpect(jsonPath("$.data.modulesMounted").value(4))
                .andExpect(jsonPath("$.data.engagementRoutesTotal").value(149))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(2))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'COMMUNITY' && @.pathPrefix == '/api/v1/community' && @.routesTotal == 64 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ACTIVITY' && @.pathPrefix == '/api/v1/activity' && @.routesTotal == 41 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CALENDAR' && @.pathPrefix == '/api/v1/calendar' && @.routesTotal == 21 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CHANGELOG' && @.pathPrefix == '/api/v1/changelog' && @.routesTotal == 23 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void exposesEngagementCoreAdminOpsSummaryWithAdminOnlyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary")
                        .header("Authorization", "Basic admin-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary")
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("engagement-core"));

        mockMvc.perform(get("/api/v1/engagement-core/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("engagement-core"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.modulesTotal").value(4))
                .andExpect(jsonPath("$.data.modulesMounted").value(4))
                .andExpect(jsonPath("$.data.routesTotal").value(151))
                .andExpect(jsonPath("$.data.engagementRoutesTotal").value(149))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(2))
                .andExpect(jsonPath("$.data.gatewaySwitchReady").value(true))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'gateway route switch is not complete')]").doesNotExist())
                .andExpect(jsonPath("$.data.businessCoreDependency.service").value("business-core"))
                .andExpect(jsonPath("$.data.businessCoreDependency.port").value(8130))
                .andExpect(jsonPath("$.data.admissionCoreDependency.service").value("admission-core"))
                .andExpect(jsonPath("$.data.admissionCoreDependency.port").value(8131))
                .andExpect(jsonPath("$.data.admissionCoreDependency.status").value("STABLE_BASELINE"))
                .andExpect(jsonPath("$.data.adapterChain[?(@.from == 'activity' && @.to == 'community' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.adapterChain[?(@.from == 'calendar' && @.to == 'activity' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.adapterChain[?(@.from == 'changelog' && @.to == 'calendar' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'community-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'activity-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'calendar-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'changelog-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'business-core-service' && @.port == 8130)]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'admission-core-service' && @.port == 8131)]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'api-gateway-service' && @.port == 8125)]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@.service == 'community-service' && @.directory == 'backend/community-service' && @.testCommand == null)]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@.service == 'activity-service' && @.directory == 'backend/activity-service' && @.testCommand == null)]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@.service == 'calendar-service' && @.directory == 'backend/calendar-service' && @.testCommand == null)]").exists())
                .andExpect(jsonPath("$.data.retiredLegacyServices[?(@.service == 'changelog-service' && @.directory == 'backend/changelog-service' && @.testCommand == null)]").exists())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void mountsRepresentativeThirdBatchRoutesWithoutPathRewrite() throws Exception {
        mockMvc.perform(get("/api/v1/community/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/activity/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/calendar/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/changelog/versions/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/community-core")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/activity-log")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/calendarize")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/changelogger")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/engagement")).andExpect(status().isNotFound());
    }

    @Test
    void registersExactlyThirdBatchAndSelfApiRoutes() {
        long apiRouteMappings = handlerMapping.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/v1/")))
                .count();
        Set<String> apiRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(apiRouteMappings).isEqualTo(151);
        assertThat(apiRoutes).contains(
                "/api/v1/engagement-core/health",
                "/api/v1/engagement-core/admin/ops/summary",
                "/api/v1/community/boards",
                "/api/v1/activity/events",
                "/api/v1/calendar/upcoming",
                "/api/v1/changelog/versions/latest"
        );
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = EngagementCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(community|activity|calendar|changelog)\\..*ServiceApplication");
        });
    }

    @Test
    void doesNotRestoreMergedLegacyServiceEntrypoints() {
        assertThat(List.of(
                Path.of("../auth-service/pom.xml"),
                Path.of("../profile-service/pom.xml"),
                Path.of("../notification-service/pom.xml"),
                Path.of("../content-service/pom.xml"),
                Path.of("../server-status-service/pom.xml"),
                Path.of("../resource-service/pom.xml"),
                Path.of("../admin-service/pom.xml"),
                Path.of("../onboarding-service/pom.xml"),
                Path.of("../exam-service/pom.xml"),
                Path.of("../whitelist-service/pom.xml"),
                Path.of("../attendance-service/pom.xml"),
                Path.of("../community-service/pom.xml"),
                Path.of("../community-service/src/main/java/cn/beiming/community/CommunityServiceApplication.java"),
                Path.of("../activity-service/pom.xml"),
                Path.of("../activity-service/src/main/java/cn/beiming/activity/ActivityServiceApplication.java"),
                Path.of("../calendar-service/pom.xml"),
                Path.of("../calendar-service/src/main/java/cn/beiming/calendar/CalendarServiceApplication.java"),
                Path.of("../changelog-service/pom.xml"),
                Path.of("../changelog-service/src/main/java/cn/beiming/changelog/ChangelogServiceApplication.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).isFalse());
    }
}
