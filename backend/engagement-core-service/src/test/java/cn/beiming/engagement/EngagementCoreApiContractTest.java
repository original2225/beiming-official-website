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
                .andExpect(jsonPath("$.data.routeContractRoutesVerifiedTotal").value(149))
                .andExpect(jsonPath("$.data.routeContractCoverageStatus").value("ROUTE_CONTRACT_VERIFIED"))
                .andExpect(jsonPath("$.data.behaviorContractCoverageStatus").value("PARTIAL_BEHAVIOR_CONTRACT_TESTS"))
                .andExpect(jsonPath("$.data.gatewaySwitchReady").value(true))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'gateway route switch is not complete')]").doesNotExist())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'complete inherited contract tests are not all mounted in engagement-core')]").doesNotExist())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'complete inherited behavior contract tests are not all mounted in engagement-core')]").exists())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'real auth and gateway trusted context adapters are not connected')]").exists())
                .andExpect(jsonPath("$.data.businessCoreDependency.service").value("business-core"))
                .andExpect(jsonPath("$.data.businessCoreDependency.port").value(8130))
                .andExpect(jsonPath("$.data.admissionCoreDependency.service").value("admission-core"))
                .andExpect(jsonPath("$.data.admissionCoreDependency.port").value(8131))
                .andExpect(jsonPath("$.data.admissionCoreDependency.status").value("STABLE_BASELINE"))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'COMMUNITY' && @.port == 8132 && @.legacyPort == 8112 && @.routeContractRoutesVerifiedTotal == 64 && @.routeContractCoverageStatus == 'ROUTE_CONTRACT_VERIFIED' && @.behaviorContractCoverageStatus == 'PARTIAL_BEHAVIOR_CONTRACT_TESTS')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ACTIVITY' && @.port == 8132 && @.legacyPort == 8113 && @.routeContractRoutesVerifiedTotal == 41 && @.routeContractCoverageStatus == 'ROUTE_CONTRACT_VERIFIED' && @.behaviorContractCoverageStatus == 'PARTIAL_BEHAVIOR_CONTRACT_TESTS')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CALENDAR' && @.port == 8132 && @.legacyPort == 8114 && @.routeContractRoutesVerifiedTotal == 21 && @.routeContractCoverageStatus == 'ROUTE_CONTRACT_VERIFIED' && @.behaviorContractCoverageStatus == 'PARTIAL_BEHAVIOR_CONTRACT_TESTS')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'CHANGELOG' && @.port == 8132 && @.legacyPort == 8115 && @.routeContractRoutesVerifiedTotal == 23 && @.routeContractCoverageStatus == 'ROUTE_CONTRACT_VERIFIED' && @.behaviorContractCoverageStatus == 'PARTIAL_BEHAVIOR_CONTRACT_TESTS')]").exists())
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
    void exposesMergedModuleOpsSummariesWithCurrentPortAndLegacyPort() throws Exception {
        mockMvc.perform(get("/api/v1/community/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("community"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.legacyPort").value(8112));

        mockMvc.perform(get("/api/v1/activity/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("activity"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.legacyPort").value(8113));

        mockMvc.perform(get("/api/v1/calendar/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("calendar"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.legacyPort").value(8114));

        mockMvc.perform(get("/api/v1/changelog/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("changelog"))
                .andExpect(jsonPath("$.data.port").value(8132))
                .andExpect(jsonPath("$.data.legacyPort").value(8115));
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
    void registersEveryInheritedThirdBatchRouteSignature() {
        Set<String> actualRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()
                        .filter(pattern -> pattern.startsWith("/api/v1/community")
                                || pattern.startsWith("/api/v1/activity")
                                || pattern.startsWith("/api/v1/calendar")
                                || pattern.startsWith("/api/v1/changelog"))
                        .flatMap(pattern -> mapping.getMethodsCondition().getMethods().stream()
                                .map(method -> method.name() + " " + pattern)))
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(actualRoutes).containsExactlyInAnyOrderElementsOf(inheritedThirdBatchRouteSignatures());
        assertThat(actualRoutes).hasSize(149);
        assertThat(countByPrefix(actualRoutes, "/api/v1/community")).isEqualTo(64);
        assertThat(countByPrefix(actualRoutes, "/api/v1/activity")).isEqualTo(41);
        assertThat(countByPrefix(actualRoutes, "/api/v1/calendar")).isEqualTo(21);
        assertThat(countByPrefix(actualRoutes, "/api/v1/changelog")).isEqualTo(23);
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

    private long countByPrefix(Set<String> routes, String prefix) {
        return routes.stream().filter(route -> route.contains(" " + prefix)).count();
    }

    private Set<String> inheritedThirdBatchRouteSignatures() {
        return Set.of(
                "DELETE /api/v1/community/me/comments/{commentId}/like",
                "DELETE /api/v1/community/me/posts/{postId}/favorite",
                "DELETE /api/v1/community/me/posts/{postId}/like",
                "GET /api/v1/activity/admin/audit-logs",
                "GET /api/v1/activity/admin/events",
                "GET /api/v1/activity/admin/events/{activityId}",
                "GET /api/v1/activity/admin/events/{activityId}/registrations",
                "GET /api/v1/activity/admin/ops/summary",
                "GET /api/v1/activity/calendar-summary",
                "GET /api/v1/activity/events",
                "GET /api/v1/activity/events/{activityIdOrSlug}",
                "GET /api/v1/activity/events/{activityId}/result",
                "GET /api/v1/activity/me/events/{activityId}/check-in",
                "GET /api/v1/activity/me/registrations",
                "GET /api/v1/activity/me/registrations/{registrationId}",
                "GET /api/v1/activity/me/rewards",
                "GET /api/v1/calendar/admin/audit-logs",
                "GET /api/v1/calendar/admin/events",
                "GET /api/v1/calendar/admin/events/{eventId}",
                "GET /api/v1/calendar/admin/ops/summary",
                "GET /api/v1/calendar/events",
                "GET /api/v1/calendar/events/{eventId}",
                "GET /api/v1/calendar/me/watchlist",
                "GET /api/v1/calendar/month",
                "GET /api/v1/calendar/upcoming",
                "GET /api/v1/changelog/admin/audit-logs",
                "GET /api/v1/changelog/admin/ops/summary",
                "GET /api/v1/changelog/admin/releases",
                "GET /api/v1/changelog/admin/releases/{releaseId}",
                "GET /api/v1/changelog/changes",
                "GET /api/v1/changelog/me/bookmarks",
                "GET /api/v1/changelog/releases",
                "GET /api/v1/changelog/releases/{releaseIdOrSlug}",
                "GET /api/v1/changelog/tags",
                "GET /api/v1/changelog/versions/latest",
                "GET /api/v1/community/admin/audit-logs",
                "GET /api/v1/community/admin/boards",
                "GET /api/v1/community/admin/comments",
                "GET /api/v1/community/admin/ops/summary",
                "GET /api/v1/community/admin/posts",
                "GET /api/v1/community/admin/posts/{postId}",
                "GET /api/v1/community/admin/reports",
                "GET /api/v1/community/admin/reports/{reportId}",
                "GET /api/v1/community/admin/tickets",
                "GET /api/v1/community/admin/tickets/{ticketId}",
                "GET /api/v1/community/boards",
                "GET /api/v1/community/boards/{boardId}",
                "GET /api/v1/community/me/reports",
                "GET /api/v1/community/me/tickets",
                "GET /api/v1/community/me/tickets/{ticketId}",
                "GET /api/v1/community/polls/{pollId}",
                "GET /api/v1/community/posts",
                "GET /api/v1/community/posts/{postId}",
                "GET /api/v1/community/posts/{postId}/comments",
                "GET /api/v1/community/search",
                "PATCH /api/v1/activity/admin/events/{activityId}",
                "PATCH /api/v1/activity/admin/events/{activityId}/approve",
                "PATCH /api/v1/activity/admin/events/{activityId}/archive",
                "PATCH /api/v1/activity/admin/events/{activityId}/close-registration",
                "PATCH /api/v1/activity/admin/events/{activityId}/complete",
                "PATCH /api/v1/activity/admin/events/{activityId}/delete",
                "PATCH /api/v1/activity/admin/events/{activityId}/offline",
                "PATCH /api/v1/activity/admin/events/{activityId}/open-registration",
                "PATCH /api/v1/activity/admin/events/{activityId}/publish",
                "PATCH /api/v1/activity/admin/events/{activityId}/reject",
                "PATCH /api/v1/activity/admin/events/{activityId}/request-changes",
                "PATCH /api/v1/activity/admin/events/{activityId}/result/publish",
                "PATCH /api/v1/activity/admin/events/{activityId}/start",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/cancel",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/check-in",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/confirm",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/no-show",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/promote",
                "PATCH /api/v1/activity/admin/registrations/{registrationId}/reject",
                "PATCH /api/v1/activity/admin/rewards/{rewardId}/issue",
                "PATCH /api/v1/activity/admin/rewards/{rewardId}/revoke",
                "PATCH /api/v1/calendar/admin/events/{eventId}",
                "PATCH /api/v1/calendar/admin/events/{eventId}/approve",
                "PATCH /api/v1/calendar/admin/events/{eventId}/archive",
                "PATCH /api/v1/calendar/admin/events/{eventId}/delete",
                "PATCH /api/v1/calendar/admin/events/{eventId}/offline",
                "PATCH /api/v1/calendar/admin/events/{eventId}/publish",
                "PATCH /api/v1/calendar/admin/events/{eventId}/reject",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/approve",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/archive",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/delete",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/offline",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/publish",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/reject",
                "PATCH /api/v1/changelog/admin/releases/{releaseId}/request-changes",
                "PATCH /api/v1/community/admin/boards/{boardId}",
                "PATCH /api/v1/community/admin/boards/{boardId}/archive",
                "PATCH /api/v1/community/admin/comments/{commentId}/approve",
                "PATCH /api/v1/community/admin/comments/{commentId}/offline",
                "PATCH /api/v1/community/admin/comments/{commentId}/reject",
                "PATCH /api/v1/community/admin/penalties/{penaltyId}",
                "PATCH /api/v1/community/admin/penalties/{penaltyId}/revoke",
                "PATCH /api/v1/community/admin/polls/{pollId}",
                "PATCH /api/v1/community/admin/polls/{pollId}/close",
                "PATCH /api/v1/community/admin/polls/{pollId}/open",
                "PATCH /api/v1/community/admin/posts/{postId}/approve",
                "PATCH /api/v1/community/admin/posts/{postId}/archive",
                "PATCH /api/v1/community/admin/posts/{postId}/delete",
                "PATCH /api/v1/community/admin/posts/{postId}/offline",
                "PATCH /api/v1/community/admin/posts/{postId}/reject",
                "PATCH /api/v1/community/admin/posts/{postId}/request-changes",
                "PATCH /api/v1/community/admin/reports/{reportId}/assign",
                "PATCH /api/v1/community/admin/reports/{reportId}/dismiss",
                "PATCH /api/v1/community/admin/reports/{reportId}/resolve",
                "PATCH /api/v1/community/admin/tickets/{ticketId}/assign",
                "PATCH /api/v1/community/admin/tickets/{ticketId}/status",
                "PATCH /api/v1/community/me/comments/{commentId}",
                "PATCH /api/v1/community/me/comments/{commentId}/archive",
                "PATCH /api/v1/community/me/posts/{postId}",
                "PATCH /api/v1/community/me/posts/{postId}/withdraw",
                "PATCH /api/v1/community/me/tickets/{ticketId}",
                "POST /api/v1/activity/admin/events",
                "POST /api/v1/activity/admin/events/{activityId}/contribution-candidates",
                "POST /api/v1/activity/admin/events/{activityId}/rewards",
                "POST /api/v1/activity/admin/events/{activityId}/submit",
                "POST /api/v1/activity/me/events/{activityId}/registrations",
                "POST /api/v1/activity/me/registrations/{registrationId}/cancel",
                "POST /api/v1/calendar/admin/events",
                "POST /api/v1/calendar/admin/events/{eventId}/submit",
                "POST /api/v1/calendar/admin/sync/activity",
                "POST /api/v1/calendar/me/events/{eventId}/unwatch",
                "POST /api/v1/calendar/me/events/{eventId}/watch",
                "POST /api/v1/changelog/admin/releases",
                "POST /api/v1/changelog/admin/releases/{releaseId}/calendar-sync",
                "POST /api/v1/changelog/admin/releases/{releaseId}/submit",
                "POST /api/v1/changelog/me/releases/{releaseId}/bookmark",
                "POST /api/v1/changelog/me/releases/{releaseId}/unbookmark",
                "POST /api/v1/community/admin/boards",
                "POST /api/v1/community/admin/penalties",
                "POST /api/v1/community/admin/polls",
                "POST /api/v1/community/admin/tickets/{ticketId}/messages",
                "POST /api/v1/community/me/comments/{commentId}/like",
                "POST /api/v1/community/me/comments/{commentId}/reports",
                "POST /api/v1/community/me/polls/{pollId}/votes",
                "POST /api/v1/community/me/posts",
                "POST /api/v1/community/me/posts/{postId}/comments",
                "POST /api/v1/community/me/posts/{postId}/favorite",
                "POST /api/v1/community/me/posts/{postId}/like",
                "POST /api/v1/community/me/posts/{postId}/reports",
                "POST /api/v1/community/me/posts/{postId}/submit",
                "POST /api/v1/community/me/tickets",
                "POST /api/v1/community/me/tickets/{ticketId}/close",
                "PUT /api/v1/activity/admin/events/{activityId}/result"
        );
    }
}
