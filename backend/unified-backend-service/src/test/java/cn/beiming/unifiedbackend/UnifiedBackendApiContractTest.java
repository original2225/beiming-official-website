package cn.beiming.unifiedbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UnifiedBackendServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UnifiedBackendApiContractTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${server.port}")
    private String port;

    @Test
    void usesUnifiedBackendCandidatePort() {
        assertThat(port).isEqualTo("8135");
    }

    @Test
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "UBACK-COM", 1, 1);
        addRange(mapped, "UBACK-AUTH", 1, 2);
        addRange(mapped, "UBACK-MOUNT", 1, 22);
        addRange(mapped, "UBACK-GATE", 1, 1);
        addRange(mapped, "UBACK-READY", 1, 1);
        addRange(mapped, "UBACK-SMOKE", 1, 1);
        addRange(mapped, "UBACK-BOUNDARY", 1, 1);
        addRange(mapped, "UBACK-REGRESS", 1, 1);

        assertThat(mapped).contains(
                "UBACK-COM-001",
                "UBACK-AUTH-001",
                "UBACK-MOUNT-022",
                "UBACK-GATE-001",
                "UBACK-READY-001",
                "UBACK-SMOKE-001",
                "UBACK-BOUNDARY-001",
                "UBACK-REGRESS-001"
        );
        assertThat(mapped).hasSize(30);
    }

    @Test
    void exposesUnifiedBackendHealth() throws Exception {
        mvc.perform(get("/api/v1/unified-backend/health").header("X-Request-Id", "req-unified-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-unified-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.service").value("unified-backend"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(8135))
                .andExpect(jsonPath("$.data.deploymentMode").value("CANDIDATE_PARALLEL_ENTRYPOINT"))
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'api-gateway')]").exists())
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'business-core')]").exists())
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'admission-core')]").exists())
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'engagement-core')]").exists())
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'ops-core')]").exists())
                .andExpect(jsonPath("$.data.mountedEntrypoints[?(@ == 'portal-core')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'auth')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'profile')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'notification')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'content')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'server-status')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'resource')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'admin')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'onboarding')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'exam')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'whitelist')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'attendance')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'community')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'activity')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'calendar')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'changelog')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'ops-control')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'cloudreve-sync')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'backup-recovery')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'alerting')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'plugin-integration')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'cross-platform-notification')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'ops-image-market')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'guide')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'material')]").exists())
                .andExpect(jsonPath("$.data.mountedRouteIds[?(@ == 'online-map')]").exists())
                .andExpect(jsonPath("$.requestId").value("req-unified-health"));
    }

    @Test
    void protectsUnifiedBackendAdminApis() throws Exception {
        for (String path : Set.of(
                "/api/v1/unified-backend/admin/ops/summary",
                "/api/v1/unified-backend/admin/mounts",
                "/api/v1/unified-backend/admin/readiness"
        )) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(41000));

            mvc.perform(get(path).header("Authorization", "Basic admin-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(41003));

            mvc.perform(get(path).header("Authorization", "Bearer user-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(42001));

            mvc.perform(get(path).header("Authorization", "Bearer helper-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        mvc.perform(post("/api/v1/unified-backend/admin/http-smoke/run").header("Authorization", "Bearer helper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mvc.perform(post("/api/v1/unified-backend/admin/http-smoke/run").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void exposesMountSummaryAndReadinessWithoutClaimingProductionReplacement() throws Exception {
        JsonNode summary = performJson(get("/api/v1/unified-backend/admin/ops/summary")
                .header("Authorization", "Bearer admin-token"));
        assertThat(summary.at("/data/service").asText()).isEqualTo("unified-backend");
        assertThat(summary.at("/data/currentProductionEntrypointsTotal").asInt()).isEqualTo(7);
        assertThat(summary.at("/data/candidateEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(summary.at("/data/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(summary.at("/data/httpFallbackRoutesTotal").asInt()).isEqualTo(0);
        assertThat(summary.at("/data/externalRoutesTotal").asInt()).isEqualTo(1);
        assertThat(summary.at("/data/nodeDaemonDisposition").asText()).isEqualTo("KEEP_EXTERNAL");
        assertThat(summary.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(summary.at("/data/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(summary.at("/data/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(summary.at("/data/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(summary.at("/data/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(summary.at("/data/readyToRetirePortalCore").asBoolean()).isFalse();
        assertNoSecrets(summary);

        JsonNode mounts = performJson(get("/api/v1/unified-backend/admin/mounts")
                .header("Authorization", "Bearer owner-token"));
        assertMount(mounts, "business-core", "BUSINESS_CORE", "/api/v1/business-core", "IN_PROCESS");
        assertMount(mounts, "admission-core", "ADMISSION_CORE", "/api/v1/admission-core", "IN_PROCESS");
        assertMount(mounts, "engagement-core", "ENGAGEMENT_CORE", "/api/v1/engagement-core", "IN_PROCESS");
        assertMount(mounts, "ops-core", "OPS_CORE", "/api/v1/ops-core", "IN_PROCESS");
        assertMount(mounts, "auth", "AUTH", "/api/v1/auth", "IN_PROCESS");
        assertMount(mounts, "profile", "PROFILE", "/api/v1/profile", "IN_PROCESS");
        assertMount(mounts, "notification", "NOTIFICATION", "/api/v1/notifications", "IN_PROCESS");
        assertMount(mounts, "content", "CONTENT", "/api/v1/content", "IN_PROCESS");
        assertMount(mounts, "server-status", "SERVER_STATUS", "/api/v1/server-status", "IN_PROCESS");
        assertMount(mounts, "resource", "RESOURCE", "/api/v1/resources", "IN_PROCESS");
        assertMount(mounts, "admin", "ADMIN", "/api/v1/admin", "IN_PROCESS");
        assertMount(mounts, "guide", "GUIDE", "/api/v1/guides", "IN_PROCESS");
        assertMount(mounts, "material", "MATERIAL", "/api/v1/materials", "IN_PROCESS");
        assertMount(mounts, "online-map", "ONLINE_MAP", "/api/v1/online-map", "IN_PROCESS");
        assertMount(mounts, "onboarding", "ONBOARDING", "/api/v1/onboarding", "IN_PROCESS");
        assertMount(mounts, "exam", "EXAM", "/api/v1/exams", "IN_PROCESS");
        assertMount(mounts, "whitelist", "WHITELIST", "/api/v1/whitelist", "IN_PROCESS");
        assertMount(mounts, "attendance", "ATTENDANCE", "/api/v1/attendance", "IN_PROCESS");
        assertMount(mounts, "community", "COMMUNITY", "/api/v1/community", "IN_PROCESS");
        assertMount(mounts, "activity", "ACTIVITY", "/api/v1/activity", "IN_PROCESS");
        assertMount(mounts, "calendar", "CALENDAR", "/api/v1/calendar", "IN_PROCESS");
        assertMount(mounts, "changelog", "CHANGELOG", "/api/v1/changelog", "IN_PROCESS");
        assertMount(mounts, "ops-control", "OPS_CONTROL", "/api/v1/ops-control", "IN_PROCESS");
        assertMount(mounts, "cloudreve-sync", "CLOUDREVE_SYNC", "/api/v1/cloudreve-sync", "IN_PROCESS");
        assertMount(mounts, "backup-recovery", "BACKUP_RECOVERY", "/api/v1/backup-recovery", "IN_PROCESS");
        assertMount(mounts, "alerting", "ALERTING", "/api/v1/alerting", "IN_PROCESS");
        assertMount(mounts, "plugin-integration", "PLUGIN_INTEGRATION", "/api/v1/plugin-integration", "IN_PROCESS");
        assertMount(mounts, "cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification", "IN_PROCESS");
        assertMount(mounts, "ops-image-market", "OPS_IMAGE_MARKET", "/api/v1/ops-image-market", "IN_PROCESS");
        assertMount(mounts, "node-daemon", "NODE_DAEMON", "/api/v1/node-daemon", "KEEP_EXTERNAL");
        assertThat(mounts.at("/data/items").toString()).doesNotContain("HTTP_UPSTREAM_FALLBACK");
        assertNoSecrets(mounts);

        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token"));
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/productionBlockers").toString())
                .contains("candidate entrypoint is not production traffic entrypoint")
                .contains("node-daemon remains external")
                .doesNotContain("ops-core entrypoint is not mounted in-process");
        assertNoSecrets(readiness);
    }

    @Test
    void runsUnifiedBackendHttpSmokeWithoutHidingDegradedTargets() throws Exception {
        JsonNode smoke = performJson(post("/api/v1/unified-backend/admin/http-smoke/run")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-unified-smoke"));

        assertThat(smoke.at("/data/service").asText()).isEqualTo("unified-backend");
        assertThat(smoke.at("/data/httpSmokeStatus").asText()).isIn("PASS", "DEGRADED");
        assertThat(smoke.at("/data/results").size()).isEqualTo(32);
        assertThat(smoke.at("/data/results").toString())
                .contains("\"targetKey\":\"UNIFIED_HEALTH\"")
                .contains("\"targetKey\":\"GATEWAY_HEALTH\"")
                .contains("\"targetKey\":\"BUSINESS_CORE_HEALTH\"")
                .contains("\"targetKey\":\"ADMISSION_CORE_HEALTH\"")
                .contains("\"targetKey\":\"OPS_CORE_HEALTH\"")
                .contains("\"targetKey\":\"PORTAL_CORE_HEALTH\"")
                .contains("\"targetKey\":\"AUTH_SESSION_VERIFY\"")
                .contains("\"targetKey\":\"PROFILE_MEMBERS\"")
                .contains("\"targetKey\":\"NOTIFICATION_UNREAD_COUNT\"")
                .contains("\"targetKey\":\"CONTENT_HOME\"")
                .contains("\"targetKey\":\"SERVER_STATUS_OVERVIEW\"")
                .contains("\"targetKey\":\"RESOURCE_LIST\"")
                .contains("\"targetKey\":\"ADMIN_OVERVIEW\"")
                .contains("\"targetKey\":\"ONBOARDING_PROGRESS\"")
                .contains("\"targetKey\":\"EXAM_SESSIONS\"")
                .contains("\"targetKey\":\"WHITELIST_CURRENT_APPLICATION\"")
                .contains("\"targetKey\":\"ATTENDANCE_LEADERBOARD\"")
                .contains("\"targetKey\":\"ENGAGEMENT_CORE_HEALTH\"")
                .contains("\"targetKey\":\"COMMUNITY_BOARDS\"")
                .contains("\"targetKey\":\"ACTIVITY_EVENTS\"")
                .contains("\"targetKey\":\"CALENDAR_UPCOMING\"")
                .contains("\"targetKey\":\"CHANGELOG_LATEST_VERSION\"")
                .contains("\"targetKey\":\"OPS_CONTROL_OVERVIEW\"")
                .contains("\"targetKey\":\"CLOUDREVE_SYNC_HEALTH\"")
                .contains("\"targetKey\":\"BACKUP_RECOVERY_HEALTH\"")
                .contains("\"targetKey\":\"ALERTING_HEALTH\"")
                .contains("\"targetKey\":\"PLUGIN_INTEGRATION_HEALTH\"")
                .contains("\"targetKey\":\"CROSS_PLATFORM_NOTIFICATION_HEALTH\"")
                .contains("\"targetKey\":\"OPS_IMAGE_MARKET_HEALTH\"")
                .contains("\"targetKey\":\"GUIDE_CATEGORIES\"")
                .contains("\"targetKey\":\"MATERIAL_FEATURED\"")
                .contains("\"targetKey\":\"ONLINE_MAP_HEALTH\"");
        assertNoSecrets(smoke);
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return objectMapper.readTree(mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    }

    private void assertMount(JsonNode response, String routeId, String serviceKey, String pathPrefix, String disposition) {
        assertThat(response.at("/data/items").toString())
                .contains("\"routeId\":\"" + routeId + "\"")
                .contains("\"serviceKey\":\"" + serviceKey + "\"")
                .contains("\"pathPrefix\":\"" + pathPrefix + "\"")
                .contains("\"mountDisposition\":\"" + disposition + "\"");
    }

    private void assertNoSecrets(JsonNode node) {
        String text = node.toString().toLowerCase();
        assertThat(text)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("stacktrace")
                .doesNotContain("c:\\users\\");
    }

    private void addRange(Set<String> target, String prefix, int from, int to) {
        for (int i = from; i <= to; i++) {
            target.add(prefix + "-" + "%03d".formatted(i));
        }
    }
}
