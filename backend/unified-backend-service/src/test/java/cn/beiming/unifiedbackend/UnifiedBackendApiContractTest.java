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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
        addRange(mapped, "UBACK-READY", 1, 18);
        addRange(mapped, "UBACK-SMOKE", 1, 1);
        addRange(mapped, "UBACK-HTTP", 1, 1);
        addRange(mapped, "UBACK-DRIFT", 1, 1);
        addRange(mapped, "UBACK-BOUNDARY", 1, 1);
        addRange(mapped, "UBACK-REGRESS", 1, 1);
        addRange(mapped, "UBACK-CUTOVER", 1, 2);
        addRange(mapped, "UBACK-PROD-CUTOVER", 1, 10);
        addRange(mapped, "UBACK-CORE-RETIRE", 1, 10);
        addRange(mapped, "UBACK-HARDEN", 1, 12);
        addRange(mapped, "UBACK-PROD-CONFIG", 1, 12);
        addRange(mapped, "UBACK-EXT-CUTOVER", 1, 12);
        addRange(mapped, "UBACK-PROD-AUDIT", 1, 12);
        addRange(mapped, "UBACK-SAMPLE", 1, 12);
        addRange(mapped, "UBACK-LOCAL-CUTOVER", 1, 12);
        addRange(mapped, "UBACK-CONFIG-PROVIDER", 1, 12);
        addRange(mapped, "UBACK-AUDIT-ADAPTER", 1, 12);
        addRange(mapped, "UBACK-CUTOVER-RUNBOOK", 1, 12);
        addRange(mapped, "UBACK-CUTOVER-APPROVAL", 1, 12);
        addRange(mapped, "UBACK-EXTERNAL-PARAMS", 1, 12);
        addRange(mapped, "UBACK-CUTOVER-CONSISTENCY", 1, 12);
        addRange(mapped, "UBACK-EXTERNAL-VALUE-INTAKE", 1, 12);
        addRange(mapped, "UBACK-RUNTIME-CONFIG-SHELL", 1, 12);
        addRange(mapped, "UBACK-AUDIT-OBS-SMOKE", 1, 14);
        addRange(mapped, "UBACK-CONTROLLED-CUTOVER", 1, 18);
        addRange(mapped, "UBACK-API-GATEWAY-RETIREMENT", 1, 20);
        addRange(mapped, "UBACK-API-GATEWAY-EXTERNAL-RETIREMENT", 1, 24);
        addRange(mapped, "UBACK-REAL-PRODUCTION-CUTOVER", 1, 24);
        addRange(mapped, "UBACK-LOCAL-API-GATEWAY-RETIREMENT", 1, 30);

        assertThat(mapped).contains(
                "UBACK-COM-001",
                "UBACK-AUTH-001",
                "UBACK-MOUNT-022",
                "UBACK-GATE-001",
                "UBACK-READY-001",
                "UBACK-READY-002",
                "UBACK-READY-003",
                "UBACK-READY-004",
                "UBACK-READY-005",
                "UBACK-READY-006",
                "UBACK-READY-007",
                "UBACK-READY-008",
                "UBACK-READY-009",
                "UBACK-READY-010",
                "UBACK-READY-011",
                "UBACK-READY-012",
                "UBACK-READY-013",
                "UBACK-READY-014",
                "UBACK-READY-015",
                "UBACK-READY-016",
                "UBACK-READY-017",
                "UBACK-READY-018",
                "UBACK-SMOKE-001",
                "UBACK-HTTP-001",
                "UBACK-DRIFT-001",
                "UBACK-BOUNDARY-001",
                "UBACK-REGRESS-001",
                "UBACK-CUTOVER-001",
                "UBACK-CUTOVER-002",
                "UBACK-PROD-CUTOVER-001",
                "UBACK-PROD-CUTOVER-010",
                "UBACK-CORE-RETIRE-001",
                "UBACK-CORE-RETIRE-010",
                "UBACK-HARDEN-001",
                "UBACK-HARDEN-012",
                "UBACK-PROD-CONFIG-001",
                "UBACK-PROD-CONFIG-012",
                "UBACK-EXT-CUTOVER-001",
                "UBACK-EXT-CUTOVER-012",
                "UBACK-PROD-AUDIT-001",
                "UBACK-PROD-AUDIT-012",
                "UBACK-SAMPLE-001",
                "UBACK-SAMPLE-012",
                "UBACK-LOCAL-CUTOVER-001",
                "UBACK-LOCAL-CUTOVER-012",
                "UBACK-CONFIG-PROVIDER-001",
                "UBACK-CONFIG-PROVIDER-012",
                "UBACK-AUDIT-ADAPTER-001",
                "UBACK-AUDIT-ADAPTER-012",
                "UBACK-CUTOVER-RUNBOOK-001",
                "UBACK-CUTOVER-RUNBOOK-012",
                "UBACK-CUTOVER-APPROVAL-001",
                "UBACK-CUTOVER-APPROVAL-012",
                "UBACK-EXTERNAL-PARAMS-001",
                "UBACK-EXTERNAL-PARAMS-012",
                "UBACK-CUTOVER-CONSISTENCY-001",
                "UBACK-CUTOVER-CONSISTENCY-012",
                "UBACK-EXTERNAL-VALUE-INTAKE-001",
                "UBACK-EXTERNAL-VALUE-INTAKE-012",
                "UBACK-RUNTIME-CONFIG-SHELL-001",
                "UBACK-RUNTIME-CONFIG-SHELL-012",
                "UBACK-AUDIT-OBS-SMOKE-001",
                "UBACK-AUDIT-OBS-SMOKE-014",
                "UBACK-CONTROLLED-CUTOVER-001",
                "UBACK-CONTROLLED-CUTOVER-018",
                "UBACK-API-GATEWAY-RETIREMENT-001",
                "UBACK-API-GATEWAY-RETIREMENT-020",
                "UBACK-API-GATEWAY-EXTERNAL-RETIREMENT-001",
                "UBACK-API-GATEWAY-EXTERNAL-RETIREMENT-024",
                "UBACK-REAL-PRODUCTION-CUTOVER-001",
                "UBACK-REAL-PRODUCTION-CUTOVER-024",
                "UBACK-LOCAL-API-GATEWAY-RETIREMENT-001",
                "UBACK-LOCAL-API-GATEWAY-RETIREMENT-030"
        );
        assertThat(mapped).hasSize(369);
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
        assertThat(summary.at("/data/currentProductionEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(summary.at("/data/candidateEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(summary.at("/data/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(summary.at("/data/httpFallbackRoutesTotal").asInt()).isEqualTo(0);
        assertThat(summary.at("/data/externalRoutesTotal").asInt()).isZero();
        assertThat(summary.at("/data/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(summary.at("/data/externalNodeExecutorConnected").asBoolean()).isFalse();
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
                .contains("external node executor is out of repository and not connected")
                .doesNotContain("ops-core entrypoint is not mounted in-process");
        assertNoSecrets(readiness);
    }

    @Test
    void excludesNodeDaemonAfterOfficialBackendExtraction() throws Exception {
        JsonNode mounts = performJson(get("/api/v1/unified-backend/admin/mounts")
                .header("Authorization", "Bearer owner-token"));
        String mountsText = mounts.toString();
        assertThat(mountsText)
                .doesNotContain("node-daemon")
                .doesNotContain("NODE_DAEMON")
                .doesNotContain("/api/v1/node-daemon")
                .doesNotContain("KEEP_EXTERNAL");
        assertThat(mounts.at("/data/items").size()).isEqualTo(31);
        assertNoSecrets(mounts);

        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));
        String readinessText = readiness.toString()
                .replace("NODE_DAEMON_OUT_OF_REPOSITORY", "")
                .replace("\"nodeDaemonOutOfRepository\":true", "");
        assertThat(readiness.at("/data/currentProductionEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(readiness.at("/data/candidateEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readinessText)
                .contains("external node executor is out of repository and not connected")
                .doesNotContain("node-daemon remains external")
                .doesNotContain("nodeDaemonDisposition")
                .doesNotContain("KEEP_EXTERNAL")
                .doesNotContain("/api/v1/node-daemon")
                .doesNotContain("NODE_DAEMON");
        assertNoSecrets(readiness);

        JsonNode summary = performJson(get("/api/v1/unified-backend/admin/ops/summary")
                .header("Authorization", "Bearer owner-token"));
        assertThat(summary.at("/data/currentProductionEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(summary.at("/data/externalRoutesTotal").asInt()).isZero();
        assertThat(summary.toString())
                .doesNotContain("node-daemon")
                .doesNotContain("NODE_DAEMON")
                .doesNotContain("KEEP_EXTERNAL");
        assertNoSecrets(summary);
    }

    @Test
    void doesNotRestoreNodeDaemonRepositoryEntrypointOrContract() {
        assertThat(List.of(
                Path.of("../node-daemon-service/pom.xml"),
                Path.of("../node-daemon-service/src/main/resources/application.yml"),
                Path.of("../node-daemon-service/src/main/java/cn/beiming/nodedaemon/NodeDaemonServiceApplication.java"),
                Path.of("../node-daemon-service/src/test/java/cn/beiming/nodedaemon/NodeDaemonApiContractTest.java"),
                Path.of("../node-daemon-service/src/test/java/cn/beiming/nodedaemon/NodeDaemonPortConfigTest.java"),
                Path.of("../node-daemon-service/src/test/java/cn/beiming/nodedaemon/NodeDaemonProductionAuthTest.java"),
                Path.of("../node-daemon-service/src/test/java/cn/beiming/nodedaemon/NodeDaemonProductionHardeningTest.java"),
                Path.of("../../docs/contracts-node-daemon.md")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isFalse());
    }

    @Test
    void exposesSingleServiceCutoverEvidenceAfterNodeDaemonCleanup() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-single-service-cutover"));

        assertThat(readiness.at("/data/singleServiceCutoverPrecheckStatus").asText()).isEqualTo("PASS_READY_FOR_EXTERNAL_CUTOVER");
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "UNIFIED_BACKEND_TARGET_ENTRYPOINT_READY", "PASS", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "NODE_EXECUTOR_REPOSITORY_RESIDUALS_REMOVED", "PASS", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "API_REFERENCE_SYNCHRONIZED", "PASS", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "OLD_ENTRYPOINTS_IN_RETIREMENT_QUEUE", "PASS", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "EXTERNAL_TRAFFIC_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/singleServiceCutoverPrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/singleServiceCutoverEvidence");
        assertThat(evidence.at("/targetBackendApplicationEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/officialBackendEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(evidence.at("/httpFallbackRoutesTotal").asInt()).isZero();
        assertThat(evidence.at("/externalRoutesTotal").asInt()).isZero();
        assertThat(evidence.at("/nodeExecutorRepositoryResidualsRemoved").asBoolean()).isTrue();
        assertThat(evidence.at("/apiReferenceSynchronized").asBoolean()).isTrue();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/oldEntrypointRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("admission-core:8131")
                .contains("engagement-core:8132")
                .contains("ops-core:8133")
                .contains("portal-core:8134");
        assertThat(evidence.at("/retirementQueue").toString())
                .contains("api-gateway")
                .contains("business-core")
                .contains("portal-core");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        String readinessWithoutControlledCutoverNodeExecutorFlag = readiness.toString()
                .replace("\"nodeDaemonOutOfRepository\":true", "")
                .replace("NODE_DAEMON_OUT_OF_REPOSITORY", "");
        assertThat(readinessWithoutControlledCutoverNodeExecutorFlag)
                .doesNotContain("node-daemon")
                .doesNotContain("NODE_DAEMON")
                .doesNotContain("nodeDaemon")
                .doesNotContain("KEEP_EXTERNAL")
                .doesNotContain("8117")
                .doesNotContain("/api/v1/node-daemon")
                .doesNotContain("trafficSwitchApplied\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionSwitchReadinessMatrixWithoutAllowingRetirement() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-cutover-readiness"));

        assertThat(readiness.at("/data/productionSwitchReadinessStatus").asText()).isEqualTo("BLOCKED");
        assertSwitchCheck(readiness, "ALL_CURRENT_BUSINESS_ROUTES_IN_PROCESS", "PASS", true);
        assertSwitchCheck(readiness, "CURRENT_ENTRYPOINTS_PRESERVED", "PASS", true);
        assertSwitchCheck(readiness, "ROUTE_PREFIX_AND_RESPONSE_PRESERVED", "PASS", true);
        assertSwitchCheck(readiness, "EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", true);
        assertSwitchCheck(readiness, "LEGACY_ENTRYPOINTS_NOT_RESTORED", "PASS", true);
        assertSwitchCheck(readiness, "CENTRAL_CONFIG_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "PERSISTENT_AUDIT_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "REAL_HTTP_SMOKE_REHEARSAL_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "ROLLBACK_WINDOW_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode decision = readiness.at("/data/replacementDecision");
        assertThat(decision.at("/canReplaceGateway").asBoolean()).isFalse();
        assertThat(decision.at("/canRetireIndependentCoreEntrypoints").asBoolean()).isFalse();
        assertThat(decision.at("/canRetireApiGateway").asBoolean()).isFalse();
        assertThat(decision.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(decision.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(decision.at("/candidateCoverageStatus").asText()).isEqualTo("PASS");
        assertThat(decision.at("/reason").asText())
                .contains("production cutover prerequisites are still blocked")
                .doesNotContain("node-daemon can be merged");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesCentralConfigPrecheckWithoutAllowingProductionSwitch() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-central-config-precheck"));

        assertThat(readiness.at("/data/centralConfigPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertCentralConfigCheck(readiness, "CANDIDATE_PORT_FIXED", "PASS", true);
        assertCentralConfigCheck(readiness, "CURRENT_ENTRYPOINT_PORTS_DOCUMENTED", "PASS", true);
        assertCentralConfigCheck(readiness, "IN_PROCESS_ROUTE_REGISTRY_FIXED", "PASS", true);
        assertCentralConfigCheck(readiness, "EXTERNAL_NODE_EXECUTOR_CONFIG_BOUNDARY", "PASS", true);
        assertCentralConfigCheck(readiness, "DANGEROUS_TEST_CONTROLS_DISABLED", "PASS", true);
        assertCentralConfigCheck(readiness, "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "PRODUCTION_PROFILE_BOUND", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", true);
        assertCentralConfigCheck(readiness, "CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertThat(readiness.at("/data/centralConfigPrecheckChecks").toString())
                .contains("\"check\":\"CANDIDATE_PORT_FIXED\"")
                .contains("\"check\":\"CENTRAL_CONFIG_PROVIDER_CONNECTED\"")
                .doesNotContain("C:\\Users\\");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesCentralConfigGovernanceEvidenceWithoutConnectingProductionConfig() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-central-config-governance-evidence"));

        assertThat(readiness.at("/data/centralConfigGovernancePrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CONFIG_OWNERSHIP_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "ENTRYPOINT_PORTS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CANDIDATE_CONFIG_SURFACE_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "SENSITIVE_VALUE_REDACTION_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "EXTERNAL_NODE_EXECUTOR_CONFIG_BOUNDARY", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CONFIG_GOVERNANCE_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "PRODUCTION_PROFILE_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/centralConfigGovernancePrecheckChecks", "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/centralConfigGovernanceEvidence");
        assertThat(evidence.at("/governanceMode").asText()).isEqualTo("DOCUMENTED_NOT_CONNECTED");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypointPorts").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("admission-core:8131")
                .contains("engagement-core:8132")
                .contains("ops-core:8133")
                .contains("portal-core:8134")
                                .contains("unified-backend:8135");
        assertThat(evidence.at("/configProviderStatus").asText()).isEqualTo("BLOCKED");
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/configDriftScanAutomated").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackSourceDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("GOVERNANCE_EVIDENCE_RECORDED_NOT_CONNECTED");
        assertThat(readiness.at("/data/centralConfigPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("productionProfileBound\":true")
                .doesNotContain("sensitiveValuesExternalized\":true")
                .doesNotContain("sensitiveValuesExposed\":true")
                .doesNotContain("environmentVariablesRead\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("frontendEntrypointSwitched\":true")
                .doesNotContain("externalProxySwitched\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesPersistentAuditPrecheckWithoutClaimingPersistentAuditReady() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-persistent-audit-precheck"));

        assertThat(readiness.at("/data/persistentAuditPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPersistentAuditCheck(readiness, "AUDIT_SINK_FIXED", "PASS", true);
        assertPersistentAuditCheck(readiness, "AUDIT_REQUEST_ID_PRESERVED", "PASS", true);
        assertPersistentAuditCheck(readiness, "AUDIT_EVENT_SCHEMA_FIXED", "PASS", true);
        assertPersistentAuditCheck(readiness, "AUDIT_RETENTION_WINDOW_DOCUMENTED", "PASS", true);
        assertPersistentAuditCheck(readiness, "AUDIT_BACKUP_EXPORT_PATH_DOCUMENTED", "PASS", true);
        assertPersistentAuditCheck(readiness, "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);
        assertPersistentAuditCheck(readiness, "AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", true);
        assertPersistentAuditCheck(readiness, "AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", true);
        assertPersistentAuditCheck(readiness, "AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", true);
        assertPersistentAuditCheck(readiness, "AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertSwitchCheck(readiness, "PERSISTENT_AUDIT_READY", "BLOCKED", true);
        assertThat(readiness.at("/data/persistentAuditPrecheckChecks").toString())
                .contains("\"check\":\"AUDIT_SINK_FIXED\"")
                .contains("\"check\":\"PERSISTENT_AUDIT_SINK_CONNECTED\"")
                .doesNotContain("C:\\Users\\")
                .doesNotContain("Authorization");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesPersistentAuditGovernanceEvidenceWithoutConnectingAuditSink() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-persistent-audit-governance-evidence"));

        assertThat(readiness.at("/data/persistentAuditGovernancePrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_OWNERSHIP_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_EVENT_SCHEMA_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_REQUEST_ID_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_RETENTION_WINDOW_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_EXPORT_PATH_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_REPLAY_SCOPE_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_REDACTION_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "EXTERNAL_NODE_EXECUTOR_AUDIT_BOUNDARY", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "PERSISTENT_AUDIT_GOVERNANCE_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/persistentAuditGovernancePrecheckChecks", "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/persistentAuditGovernanceEvidence");
        assertThat(evidence.at("/governanceMode").asText()).isEqualTo("DOCUMENTED_NOT_CONNECTED");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/auditSinkStatus").asText()).isEqualTo("BLOCKED");
        assertThat(evidence.at("/auditWritePathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditReplayPathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditRetentionJobConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditConfigRollbackSourceDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/requestIdPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/eventSchemaDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/retentionWindowDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/exportPathDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/replayScopeDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/redactionEnforced").asBoolean()).isTrue();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("GOVERNANCE_EVIDENCE_RECORDED_NOT_CONNECTED");
        assertThat(readiness.at("/data/persistentAuditPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("auditWritePathConnected\":true")
                .doesNotContain("auditReplayPathConnected\":true")
                .doesNotContain("auditRetentionJobConnected\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("frontendEntrypointSwitched\":true")
                .doesNotContain("externalProxySwitched\":true")
                .doesNotContain("node-daemon can be merged");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesRealHttpRehearsalPrecheckWithoutAllowingProductionSwitch() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-real-http-rehearsal-precheck"));

        assertThat(readiness.at("/data/realHttpRehearsalPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "CANDIDATE_HTTP_PORT_FIXED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "REAL_HTTP_TARGETS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "AUTH_FAILURE_PATH_INCLUDED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "EXTERNAL_NODE_EXECUTOR_EXCLUDED_FROM_REHEARSAL", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "SMOKE_RESULT_REDACTION_FIXED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "CANDIDATE_PROCESS_STARTED_FOR_REHEARSAL", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "ALL_REAL_HTTP_TARGETS_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "REHEARSAL_RESULT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "REHEARSAL_RUNBOOK_DEFINED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "REHEARSAL_ROLLBACK_RECHECKED", "BLOCKED", true);
        assertSwitchCheck(readiness, "REAL_HTTP_SMOKE_REHEARSAL_READY", "BLOCKED", true);
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertNoSecrets(readiness);
    }

    @Test
    void exposesRouteDriftPrecheckWithoutChangingRoutes() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-route-drift-precheck"));

        assertThat(readiness.at("/data/routeDriftPrecheckStatus").asText()).isEqualTo("PASS");
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "CURRENT_GATEWAY_ROUTES_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "UNIFIED_MOUNT_ROUTES_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "ROUTE_PREFIX_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "EXTERNAL_NODE_EXECUTOR_ROUTE_ABSENT", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "NO_HTTP_UPSTREAM_FALLBACK_IN_CANDIDATE", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "REAL_GATEWAY_TO_UNIFIED_DIFF_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "AUTH_BEHAVIOR_DIFF_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "ERROR_CODE_DIFF_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "SENSITIVE_FIELD_DIFF_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "DRIFT_SCAN_RESULT_RECORDED", "PASS", true);
        assertThat(readiness.at("/data/routeDriftPrecheckChecks").toString()).doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesRollbackWindowPrecheckBeforeRetiringEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-rollback-window-precheck"));

        assertThat(readiness.at("/data/rollbackWindowPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "CURRENT_ENTRYPOINTS_STILL_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "CURRENT_ENTRYPOINT_TESTS_STILL_REQUIRED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "API_GATEWAY_ROLLBACK_TARGET_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "CORE_MODULE_SOURCES_ROLLBACK_BOUNDARY_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "EXTERNAL_NODE_EXECUTOR_UNAFFECTED_BY_CANDIDATE", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_WINDOW_DURATION_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_TRIGGER_CRITERIA_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_RECHECK_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVAL_READY", "BLOCKED", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_RECORDING_COMPLETED", "PASS", true);
        assertSwitchCheck(readiness, "ROLLBACK_WINDOW_READY", "BLOCKED", true);
        assertThat(readiness.at("/data/replacementDecision/canRetireIndependentCoreEntrypoints").asBoolean()).isFalse();
        assertNoSecrets(readiness);
    }

    @Test
    void exposesRollbackWindowEvidenceWithoutApprovingRetirement() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-rollback-window-evidence"));

        assertThat(readiness.at("/data/rollbackWindowPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_WINDOW_DURATION_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_TRIGGER_CRITERIA_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_RECHECK_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "ROLLBACK_RECORDING_COMPLETED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVAL_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "ROLLBACK_WINDOW_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/rollbackWindowEvidence");
        assertThat(evidence.at("/windowDuration/status").asText()).isEqualTo("DEFINED");
        assertThat(evidence.at("/windowDuration/minimumHours").asInt()).isEqualTo(24);
        assertThat(evidence.at("/triggerCriteria/items").toString())
                .contains("REAL_HTTP_REHEARSAL_FAILURE")
                .contains("ROUTE_DRIFT_DETECTED")
                .contains("AUTH_ERROR_CODE_DRIFT")
                .contains("CURRENT_ENTRYPOINT_REGRESSION_FAILURE")
                .contains("BOUNDARY_SCAN_MATCH")
                .contains("EXTERNAL_NODE_EXECUTOR_BOUNDARY_CHANGED");
        assertThat(evidence.at("/recheckAutomation/commands").toString())
                .contains("mvn -q -f backend/unified-backend-service/pom.xml test")
                                .contains("git diff --check")
                .contains("rg -n");
        assertThat(evidence.at("/rollbackTargets").toString())
                .contains("\"entrypoint\":\"api-gateway\"")
                .contains("\"port\":8125")
                .contains("\"entrypoint\":\"business-core\"")
                .contains("\"port\":8130")
                .contains("\"entrypoint\":\"admission-core\"")
                .contains("\"port\":8131")
                .contains("\"entrypoint\":\"engagement-core\"")
                .contains("\"port\":8132")
                .contains("\"entrypoint\":\"ops-core\"")
                .contains("\"port\":8133")
                .contains("\"entrypoint\":\"portal-core\"")
                .contains("\"port\":8134")
                .contains("\"entrypoint\":\"unified-backend\"")
                .contains("\"port\":8135")
                .doesNotContain("node-daemon")
                .doesNotContain("8117")
                .doesNotContain("KEEP_EXTERNAL");
        assertThat(evidence.at("/recordingStatus").asText()).isEqualTo("COMPLETED");
        assertThat(evidence.at("/retirementApprovalStatus").asText()).isEqualTo("BLOCKED");
        assertThat(readiness.at("/data/replacementDecision/canRetireIndependentCoreEntrypoints").asBoolean()).isFalse();
        assertNoSecrets(readiness);
    }

    @Test
    void exposesEntrypointSwitchPrecheckWithoutChangingFrontend() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-entrypoint-switch-precheck"));

        assertThat(readiness.at("/data/entrypointSwitchPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "CANDIDATE_BASE_URL_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "FRONTEND_NOT_MODIFIED_IN_THIS_ROUND", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PROXY_SWITCH_SCOPE_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "SWITCH_REQUIRES_ROLLBACK_WINDOW", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PRODUCTION_TRAFFIC_CANARY_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "ENTRYPOINT_SWITCH_TESTS_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "SWITCH_AUDIT_RECORDING_READY", "PASS", true);
        assertSwitchCheck(readiness, "FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", true);
        assertThat(readiness.at("/data/entrypointSwitchPrecheckChecks").toString()).doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesEntrypointSwitchEvidenceWithoutSwitchingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-entrypoint-switch-evidence"));

        assertThat(readiness.at("/data/entrypointSwitchPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "ENTRYPOINT_SWITCH_TESTS_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "SWITCH_AUDIT_RECORDING_READY", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PRODUCTION_TRAFFIC_CANARY_DEFINED", "PASS", true);
        assertSwitchCheck(readiness, "FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/entrypointSwitchEvidence");
        assertThat(evidence.at("/candidateBaseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentGatewayBaseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/switchMode").asText()).isEqualTo("ENTRYPOINT_TARGET_ONLY");
        assertThat(evidence.at("/forbiddenPathPrefix").asText()).isEqualTo("/api/v1/unified-backend/<module>");
        assertThat(evidence.at("/rollbackTarget").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/rehearsalStatus").asText()).isEqualTo("PASS");
        assertThat(evidence.at("/auditRecordingStatus").asText()).isEqualTo("READY_FOR_REHEARSAL");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("FRONTEND_ENTRYPOINT_SWITCH_READY\":\"PASS")
                .doesNotContain("PRODUCTION_TRAFFIC_ENTRYPOINT_READY\":\"PASS");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionTrafficCanaryPlanWithoutSendingProductionTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-canary-plan"));

        assertThat(readiness.at("/data/entrypointSwitchPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PRODUCTION_TRAFFIC_CANARY_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertSwitchCheck(readiness, "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionTrafficCanaryEvidence");
        assertThat(evidence.at("/strategy").asText()).isEqualTo("CANARY_WITH_PAUSE_AND_ROLLBACK");
        assertThat(evidence.at("/plannedWeights").toString()).isEqualTo("[0,5,25,50,100]");
        assertThat(evidence.at("/initialWeightPercent").asInt()).isEqualTo(0);
        assertThat(evidence.at("/currentProductionTrafficPercent").asInt()).isEqualTo(0);
        assertThat(evidence.at("/candidateProductionTrafficPercent").asInt()).isEqualTo(0);
        assertThat(evidence.at("/manualPromotionRequired").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackTarget").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/rollbackWindowMinimumHours").asInt()).isEqualTo(24);
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("PLAN_DEFINED_NOT_APPLIED");
        assertThat(evidence.at("/gates").toString())
                .contains("REAL_HTTP_REHEARSAL_PASSED")
                .contains("ROUTE_DRIFT_SCAN_PASSED")
                .contains("ROLLBACK_WINDOW_EVIDENCE_COMPLETED")
                .contains("CURRENT_ENTRYPOINT_REGRESSION_PASSED")
                .contains("BOUNDARY_SCAN_CLEAR")
                .contains("FRONTEND_ENTRYPOINT_SWITCH_READY")
                .contains("EXTERNAL_PROXY_SWITCH_READY");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("PRODUCTION_TRAFFIC_ENTRYPOINT_READY\":\"PASS")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesBackendSingleServiceEvidenceWithoutSwitchingFrontend() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-backend-single-service-evidence"));

        assertThat(readiness.at("/data/backendSingleServicePrecheckStatus").asText()).isEqualTo("PASS");
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "UNIFIED_BACKEND_COVERS_BACKEND_ENTRYPOINT_APIS", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "PATH_AUTH_ENVELOPE_AND_ERROR_CODES_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "SENSITIVE_FIELD_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "ROLLBACK_WINDOW_EVIDENCE_COMPLETED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "CURRENT_ENTRYPOINTS_PRESERVED_AS_ROLLBACK", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "CURRENT_ENTRYPOINT_REGRESSION_REQUIRED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "BACKEND_SINGLE_SERVICE_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/backendSingleServicePrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/backendSingleServiceEvidence");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentGatewayRollbackTarget").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/currentCoreRollbackTargets").toString())
                .contains("business-core:8130")
                .contains("admission-core:8131")
                .contains("engagement-core:8132")
                .contains("ops-core:8133")
                .contains("portal-core:8134");
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(evidence.at("/httpFallbackRoutesTotal").asInt()).isEqualTo(0);
        assertThat(evidence.at("/currentProductionEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/oldEntrypointRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/backendSingleServiceCandidateReady").asBoolean()).isTrue();
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("FRONTEND_ENTRYPOINT_NOT_SWITCHED")
                .contains("EXTERNAL_PROXY_NOT_SWITCHED")
                .contains("PRODUCTION_TRAFFIC_NOT_SWITCHED")
                .contains("OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED")
                .contains("CENTRAL_CONFIG_NOT_CONNECTED")
                .contains("PERSISTENT_AUDIT_NOT_CONNECTED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertSwitchCheck(readiness, "FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);
        assertThat(readiness.toString())
                .doesNotContain("frontendEntrypointSwitched\":true")
                .doesNotContain("externalProxySwitched\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("oldEntrypointRetirementApproved\":true")
                .doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesFinalBackendSingleServiceEvidenceWithoutSwitchingFrontendOrRetiringEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-final-backend-single-service-evidence"));

        assertThat(readiness.at("/data/finalBackendSingleServicePrecheckStatus").asText()).isEqualTo("PASS");
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "BACKEND_APPLICATION_ENTRYPOINT_COVERAGE", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "ALL_OFFICIAL_BACKEND_ROUTES_IN_PROCESS", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "LEGACY_ENTRYPOINT_REGRESSION_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "PRODUCTION_SOURCE_BOUNDARY_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "FINAL_BACKEND_SINGLE_SERVICE_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "TRAFFIC_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/finalBackendSingleServicePrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/finalBackendSingleServiceEvidence");
        assertThat(evidence.at("/targetBackendApplicationEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/externalNodeExecutorProject").asText()).isEqualTo("separate-project");
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/legacyRollbackEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("admission-core:8131")
                .contains("engagement-core:8132")
                .contains("ops-core:8133")
                .contains("portal-core:8134")
                .doesNotContain("node-daemon:8117");
        assertThat(evidence.at("/backendApplicationEntrypointsRequiredForFutureRuntime").toString())
                .contains("unified-backend:8135")
                .doesNotContain("api-gateway:8125")
                .doesNotContain("business-core:8130")
                .doesNotContain("node-daemon:8117");
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(evidence.at("/httpFallbackRoutesTotal").asInt()).isEqualTo(0);
        assertThat(evidence.at("/currentProductionEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/oldEntrypointRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/singleBackendApplicationReadyForCutoverRehearsal").asBoolean()).isTrue();
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("FRONTEND_ENTRYPOINT_NOT_SWITCHED")
                .contains("EXTERNAL_PROXY_NOT_SWITCHED")
                .contains("PRODUCTION_TRAFFIC_NOT_SWITCHED")
                .contains("OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED")
                .contains("CENTRAL_CONFIG_NOT_CONNECTED")
                .contains("PERSISTENT_AUDIT_NOT_CONNECTED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("frontendEntrypointSwitched\":true")
                .doesNotContain("externalProxySwitched\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("oldEntrypointRetirementApproved\":true")
                .doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesEntrypointCutoverAdapterEvidenceWithoutApplyingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-entrypoint-cutover-adapter-evidence"));

        assertThat(readiness.at("/data/entrypointCutoverAdapterPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "FRONTEND_API_BASE_URL_CONTRACT_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "CANDIDATE_BASE_URL_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "ROLLBACK_TARGET_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "NO_FRONTEND_SOURCE_TO_MODIFY_IN_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "NO_PROXY_CONFIG_TO_MODIFY_IN_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "CUTOVER_REQUIRES_EXTERNAL_FRONTEND_OR_PROXY_CHANGE", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "CUTOVER_ADAPTER_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverAdapterPrecheckChecks", "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/entrypointCutoverAdapterEvidence");
        assertThat(evidence.at("/currentGatewayBaseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/candidateBaseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/switchMode").asText()).isEqualTo("ENTRYPOINT_TARGET_ONLY");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/forbiddenPathPrefix").asText()).isEqualTo("/api/v1/unified-backend/<module>");
        assertThat(evidence.at("/frontendSourcePresent").asBoolean()).isFalse();
        assertThat(evidence.at("/proxyConfigPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/repositoryCutoverConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/requiredFrontendEnvVar").asText()).isEqualTo("VITE_API_BASE_URL");
        assertThat(evidence.at("/recommendedNextValue").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/rollbackTarget").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("ADAPTER_EVIDENCE_RECORDED_NOT_APPLIED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertSwitchCheck(readiness, "FRONTEND_ENTRYPOINT_SWITCH_READY", "BLOCKED", true);
        assertSwitchCheck(readiness, "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);
        assertThat(readiness.toString())
                .doesNotContain("repositoryCutoverConfigApplied\":true")
                .doesNotContain("frontendEntrypointSwitched\":true")
                .doesNotContain("externalProxySwitched\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("/api/v1/unified-backend/auth");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesEntrypointCutoverExecutionEvidenceWithoutChangingBusinessPathsOrRetiringEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-entrypoint-cutover-execution-evidence"));

        assertThat(readiness.at("/data/entrypointCutoverExecutionPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "BUSINESS_PATHS_REMAIN_UNCHANGED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "FORBIDDEN_UNIFIED_BUSINESS_PREFIX_ABSENT", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "LEGACY_ROLLBACK_ENTRYPOINTS_PROTECTED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "ROLLBACK_RECHECK_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "FRONTEND_OR_PROXY_CONFIG_PRESENT", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "FRONTEND_OR_PROXY_CONFIG_UPDATED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "TARGET_ENTRYPOINT_SET_TO_UNIFIED_BACKEND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "PRODUCTION_TRAFFIC_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "OLD_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/entrypointCutoverExecutionPrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/entrypointCutoverExecutionEvidence");
        assertThat(evidence.at("/currentGatewayBaseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/candidateBaseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/effectiveApiBaseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/switchMode").asText()).isEqualTo("ENTRYPOINT_TARGET_ONLY");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/forbiddenPathPrefix").asText()).isEqualTo("/api/v1/unified-backend/<module>");
        assertThat(evidence.at("/frontendConfigPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/proxyConfigPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/repositoryCutoverConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/externalCutoverRequired").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackTarget").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/oldEntrypointRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("FRONTEND_OR_PROXY_CONFIG_ABSENT")
                .contains("TARGET_ENTRYPOINT_NOT_SET_TO_UNIFIED_BACKEND")
                .contains("PRODUCTION_TRAFFIC_NOT_SWITCHED")
                .contains("OLD_ENTRYPOINT_RETIREMENT_NOT_APPROVED")
                .contains("CENTRAL_CONFIG_NOT_CONNECTED")
                .contains("PERSISTENT_AUDIT_NOT_CONNECTED");
        assertThat(evidence.at("/status").asText()).isEqualTo("CUTOVER_EXECUTION_BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/content")
                .doesNotContain("/api/v1/unified-backend/ops-control")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("oldEntrypointRetirementApproved\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesOldEntrypointRetirementApprovalEvidenceWithoutRetiringEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-old-entrypoint-retirement-evidence"));

        assertThat(readiness.at("/data/oldEntrypointRetirementPrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "RETIREMENT_SCOPE_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "SEQUENTIAL_ENTRYPOINT_RETIREMENT_REQUIRED", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "BULK_RETIREMENT_FORBIDDEN", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "CURRENT_ENTRYPOINT_REGRESSION_REQUIRED", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "ROLLBACK_TARGETS_STILL_PROTECTED", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "EXTERNAL_NODE_EXECUTOR_OUT_OF_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "RETIREMENT_APPROVAL_EVIDENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "API_GATEWAY_RETIREMENT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "CORE_ENTRYPOINT_RETIREMENT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/oldEntrypointRetirementPrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/oldEntrypointRetirementEvidence");
        assertThat(evidence.at("/retirementMode").asText()).isEqualTo("SEQUENTIAL_APPROVAL_ONLY");
        assertThat(evidence.at("/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/directoryDeletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/mavenRegressionRequired").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/externalNodeExecutorConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/retirementApprovalStatus").asText()).isEqualTo("BLOCKED");
        assertThat(evidence.at("/approvedEntrypoints").size()).isEqualTo(0);
        assertThat(evidence.at("/currentProductionEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("admission-core:8131")
                .contains("engagement-core:8132")
                .contains("ops-core:8133")
                .contains("portal-core:8134")
                .doesNotContain("node-daemon:8117");
        assertThat(evidence.at("/protectedRollbackEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("portal-core:8134")
                .doesNotContain("node-daemon:8117");
        assertThat(evidence.at("/blockedEntrypoints").toString())
                .contains("API_GATEWAY_RETIREMENT_APPROVAL_BLOCKED")
                .contains("CORE_ENTRYPOINT_RETIREMENT_APPROVAL_BLOCKED")
                .contains("PRODUCTION_TRAFFIC_ENTRYPOINT_BLOCKED");
        assertThat(evidence.at("/nextEligibleEntrypoint").asText()).isEqualTo("NONE_UNTIL_TRAFFIC_SWITCH");
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("RETIREMENT_APPROVAL_NOT_GRANTED");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canRetireApiGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canRetireIndependentCoreEntrypoints").asBoolean()).isFalse();
        assertThat(evidence.toString())
                .doesNotContain("retirementApprovalStatus\":\"APPROVED")
                .doesNotContain("bulkRetirementAllowed\":true")
                .doesNotContain("directoryDeletionAllowed\":true")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("node-daemon can be merged");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionEntrypointCutoverGateWithoutFakingExternalSwitch() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-entrypoint-cutover"));

        assertThat(readiness.at("/data/productionEntrypointCutoverPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG");
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "UNIFIED_BACKEND_READY", "PASS", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "API_GATEWAY_ROLLBACK_TARGET_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "EXTERNAL_ENTRYPOINT_CONFIG_PRESENT", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "TRAFFIC_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionEntrypointCutoverPrecheckChecks", "API_GATEWAY_RETIREMENT_APPROVED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionEntrypointCutoverEvidence");
        assertThat(evidence.at("/targetEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/externalEntrypointConfigPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackTarget").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/apiGatewayRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG");
        assertThat(readiness.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("apiGatewayRetirementApproved\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void doesNotAllowApiGatewayDeletionBeforeCutoverApproval() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-retirement-blocked"));

        assertThat(readiness.at("/data/apiGatewayRetirementPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_TRAFFIC_NOT_SWITCHED");
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "API_GATEWAY_ROLLBACK_ROLE_PROTECTED", "PASS", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "API_GATEWAY_SELF_APIS_MOUNTED_IN_UNIFIED", "PASS", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "PRODUCTION_ENTRYPOINT_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "ROLLBACK_WINDOW_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayRetirementPrecheckChecks", "DELETE_LIST_CONFIRMED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/apiGatewayRetirementEvidence");
        assertThat(evidence.at("/retirementApprovalStatus").asText()).isEqualTo("BLOCKED");
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/protectedEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/nextAction").asText()).isEqualTo("WAIT_FOR_UNIFIED_ENTRYPOINT_TRAFFIC_SWITCH");
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_TRAFFIC_NOT_SWITCHED");
        assertThat(evidence.toString())
                .contains("api-gateway:8125")
                .doesNotContain("deletionAllowed\":true")
                .doesNotContain("retirementApprovalStatus\":\"APPROVED");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesCoreEntrypointRetirementReadinessWithoutDeletingRollbackEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-core-retirement-readiness"));

        assertThat(readiness.at("/data/coreEntrypointRetirementPrecheckStatus").asText())
                .isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");
        assertThat(readiness.at("/data/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetirePortalCore").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "UNIFIED_BACKEND_IN_PROCESS_COVERAGE", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "CORE_SELF_APIS_MOUNTED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "INDEPENDENT_CORE_REGRESSION_REPLACED_BY_UNIFIED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "API_GATEWAY_RETIREMENT_COMPLETED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "EXTERNAL_ENTRYPOINT_TRAFFIC_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "ROLLBACK_WINDOW_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "USER_CORE_RETIREMENT_APPROVAL_GRANTED", "PASS", true);
        assertPrecheck(readiness, "/data/coreEntrypointRetirementPrecheckChecks", "CORE_DELETE_LIST_CONFIRMED", "PASS", true);

        JsonNode evidence = readiness.at("/data/coreEntrypointRetirementEvidence");
        assertThat(evidence.at("/retirementApprovalStatus").asText()).isEqualTo("APPROVED_LOCAL_MAVEN_ENTRYPOINT_ONLY");
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isTrue();
        assertThat(evidence.at("/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayRetired").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
                assertThat(evidence.at("/status").asText()).isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");

        String matrix = evidence.at("/coreEntrypointMatrix").toString();
        assertThat(matrix)
                .contains("business-core", "admission-core", "engagement-core", "ops-core", "portal-core")
                .contains("\"inProcessMountedInUnified\":true")
                .contains("\"selfApisMountedInUnified\":true")
                .contains("\"independentRegressionRequired\":false")
                .contains("\"unifiedRegressionRequired\":true")
                .contains("\"retirementStatus\":\"RETIRED_LOCAL_MAVEN_ENTRYPOINT\"")
                .contains("\"moduleSourcePreserved\":true")
                .contains("\"blockedBy\":\"NONE\"");
        assertThat(readiness.toString())
                .contains("business-core:8130")
                .contains("portal-core:8134")
                .doesNotContain("retirementStatus\":\"APPROVED")
                .doesNotContain("trafficSwitchApplied\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionHardeningPrerequisitesWithoutFakingCutover() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-hardening-precheck"));

        assertThat(readiness.at("/data/productionHardeningPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "UNIFIED_BACKEND_CANDIDATE_READY", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "LOCAL_CORE_ENTRYPOINTS_RETIRED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "CENTRAL_CONFIG_CONTRACT_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "AUDIT_TRAIL_CONTRACT_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "CUTOVER_RUNBOOK_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "ROLLBACK_RECHECK_COMMANDS_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "SMOKE_EVIDENCE_FORMAT_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "EXTERNAL_ENTRYPOINT_CONFIG_PRESENT", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "PRODUCTION_TRAFFIC_SWITCH_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "ROLLBACK_WINDOW_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionHardeningEvidence");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/rollbackEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("portal-core:8134");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWritePathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/externalEntrypointConfigPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/coreRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/smokeEvidenceRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/runbookRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES");
        assertThat(evidence.toString())
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("deletionAllowed\":true")
                .doesNotContain("apiGatewayRetirementApproved\":true")
                .doesNotContain("coreRetirementApproved\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void productionHardeningEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-hardening-redaction"));

        String text = readiness.at("/data/productionHardeningEvidence").toString()
                + readiness.at("/data/productionHardeningPrecheckChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionCentralConfigPrerequisitesWithoutConnectingProvider() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-central-config-precheck"));

        assertThat(readiness.at("/data/productionCentralConfigPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED");
        assertThat(readiness.at("/data/centralConfigGovernancePrecheckStatus").asText()).isEqualTo("BLOCKED");
        assertThat(readiness.at("/data/productionHardeningPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CONFIG_OWNERSHIP_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "ENTRYPOINT_PORTS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CANDIDATE_CONFIG_SURFACE_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CONFIG_DRIFT_SCAN_AUTOMATED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CONFIG_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "SENSITIVE_VALUE_REDACTION_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "ROLLBACK_ENTRYPOINTS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CURRENT_GATEWAY_ENTRYPOINT_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "PRODUCTION_PROFILE_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "EXTERNAL_PROXY_SWITCH_IMPLEMENTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigPrecheckChecks", "PRODUCTION_TRAFFIC_ENTRYPOINT_READY", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionCentralConfigEvidence");
        assertThat(evidence.at("/readinessMode").asText()).isEqualTo("PRODUCTION_PREREQUISITES_RECORDED_NOT_CONNECTED");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/rollbackEntrypoints").toString())
                .contains("api-gateway:8125")
                .contains("business-core:8130")
                .contains("portal-core:8134");
        assertThat(evidence.at("/configDomains").toString())
                .contains("entrypoint")
                .contains("security")
                .contains("audit")
                .contains("rollback");
        assertThat(evidence.at("/configProviderStatus").asText()).isEqualTo("BLOCKED");
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/configDriftScanAutomated").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackSourceDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/frontendEntrypointSwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/externalProxySwitched").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficEntrypointReady").asBoolean()).isFalse();
        assertThat(evidence.at("/currentEntrypointPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED");
        assertThat(readiness.toString())
                .doesNotContain("productionProfileBound\":true")
                .doesNotContain("sensitiveConfigExternalized\":true")
                .doesNotContain("environmentVariablesRead\":true")
                .doesNotContain("trafficSwitchApplied\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCentralConfigEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-central-config-redaction"));

        String text = readiness.at("/data/productionCentralConfigEvidence").toString()
                + readiness.at("/data/productionCentralConfigPrecheckChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesProductionCentralConfigProviderBridgeWithoutConnectingProductionProvider() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-central-config-provider"));

        assertThat(readiness.at("/data/productionCentralConfigProviderStatus").asText())
                .isEqualTo("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "LOCAL_CONFIG_PROVIDER_ABSTRACTION_CREATED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "LOCAL_CONFIG_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "LOCAL_CONFIG_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "CONFIG_DOMAINS_DECLARED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "ENTRYPOINT_CONFIG_MATCHES_CUTOVER_SAMPLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "ROLLBACK_CONFIG_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "BUSINESS_PATH_POLICY_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "CONFIG_REDACTION_RULES_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "CONFIG_DRIFT_SCAN_REUSES_PROVIDER_SNAPSHOT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "PRODUCTION_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "PRODUCTION_PROFILE_NOT_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCentralConfigProviderChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionCentralConfigProviderEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_FILE_CONFIG_PROVIDER_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/providerType").asText()).isEqualTo("LOCAL_FILE_SAMPLE");
        assertThat(evidence.at("/providerConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/sampleConfigPath").asText())
                .isEqualTo("docs/unified-backend-central-config-provider-sample.json");
        assertThat(evidence.at("/sampleConfigPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleConfigParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/configDomainsTotal").asInt()).isEqualTo(8);
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("PRODUCTION_PROFILE_BOUND")
                .contains("SENSITIVE_CONFIG_SOURCE_EXTERNALIZED")
                .contains("EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCentralConfigProviderEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-central-config-provider-redaction"));

        assertThat(readiness.at("/data/productionCentralConfigProviderStatus").asText())
                .isEqualTo("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionCentralConfigProviderEvidence").toString()
                + readiness.at("/data/productionCentralConfigProviderChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCentralConfigProviderSampleFileIsParseableAndSafe() throws Exception {
        Path samplePath = Path.of("../../docs/unified-backend-central-config-provider-sample.json");
        assertThat(Files.exists(samplePath)).as(samplePath.toString()).isTrue();
        JsonNode sample = objectMapper.readTree(Files.readString(samplePath));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-central-config-provider");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION");
        assertThat(sample.at("/providerType").asText()).isEqualTo("LOCAL_FILE_SAMPLE");
        assertThat(sample.at("/providerConnected").asBoolean()).isFalse();
        assertThat(sample.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(sample.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(sample.at("/applyProductionTraffic").asBoolean()).isFalse();
        assertThat(sample.at("/entrypoints/current/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/entrypoints/candidate/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(sample.at("/entrypoints/rollback/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/routePolicy/preserveApiV1BusinessPaths").asBoolean()).isTrue();
        assertThat(sample.at("/routePolicy/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/configDomains").size()).isEqualTo(8);
        assertThat(sample.at("/configDomains").toString())
                .contains("entrypoint")
                .contains("route-registry")
                .contains("security-headers")
                .contains("cors")
                .contains("audit-sink")
                .contains("central-config")
                .contains("rollback")
                .contains("retirement-gates");
        assertThat(sample.at("/redactionRules/forbiddenKeys").toString())
                .contains("token")
                .contains("secret")
                .contains("password")
                .contains("Authorization")
                .contains("X-Gateway-Internal-Signature")
                .contains("C:\\\\Users\\\\");
        assertThat(sample.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/profile");
        assertThat(sample.at("/entrypoints").toString().toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("jdbc:");
    }

    @Test
    void exposesExternalEntrypointCutoverAdapterWithoutSwitchingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-cutover-adapter"));

        assertThat(readiness.at("/data/externalEntrypointCutoverPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED");
        assertThat(readiness.at("/data/productionCentralConfigPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED");
        assertThat(readiness.at("/data/productionHardeningPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "UNIFIED_BACKEND_TARGET_READY", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "REAL_HTTP_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "ROLLBACK_TARGET_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "SMOKE_EVIDENCE_FORMAT_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "EXTERNAL_ENTRYPOINT_CONFIG_PROVIDED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "EXTERNAL_ENTRYPOINT_TARGETS_UNIFIED_BACKEND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "CONTROLLED_CUTOVER_WINDOW_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "API_GATEWAY_TRAFFIC_ZERO_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "ROLLBACK_WINDOW_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "PERSISTENT_AUDIT_SINK_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/externalEntrypointCutoverPrecheckChecks", "USER_RETIREMENT_APPROVAL_GRANTED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/externalEntrypointCutoverEvidence");
        assertThat(evidence.at("/readinessMode").asText()).isEqualTo("EXTERNAL_CUTOVER_ADAPTER_RECORDED_NOT_SWITCHED");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/effectiveEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/externalEntrypointConfigProvided").asBoolean()).isFalse();
        assertThat(evidence.at("/externalEntrypointTargetsUnifiedBackend").asBoolean()).isFalse();
        assertThat(evidence.at("/repositoryCutoverConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/controlledCutoverWindowApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObservedOnUnified").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/coreRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED");
        assertThat(evidence.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("trafficSwitchApplied\":true")
                .doesNotContain("deletionAllowed\":true")
                .doesNotContain("externalEntrypointConfigProvided\":true")
                .doesNotContain("externalEntrypointTargetsUnifiedBackend\":true");
        assertNoSecrets(readiness);
    }

    @Test
    void externalEntrypointCutoverEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-cutover-redaction"));

        assertThat(readiness.at("/data/externalEntrypointCutoverPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED");
        String text = readiness.at("/data/externalEntrypointCutoverEvidence").toString()
                + readiness.at("/data/externalEntrypointCutoverPrecheckChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesAuditSinkAdapterRehearsalWithoutConnectingProductionSink() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-audit-sink-adapter-rehearsal"));

        assertThat(readiness.at("/data/auditSinkAdapterRehearsalStatus").asText())
                .isEqualTo("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "LOCAL_AUDIT_SINK_ADAPTER_CREATED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_SAMPLE_JSONL_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_SAMPLE_JSONL_PARSEABLE", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_EVENT_SCHEMA_DECLARED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_EVENT_REQUIRED_FIELDS_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_REQUEST_ID_PROPAGATED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_ACTOR_TARGET_ACTION_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_WRITE_SMOKE_REHEARSED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_REPLAY_REHEARSED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_EXPORT_SUMMARY_REHEARSED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_RETENTION_POLICY_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "AUDIT_REDACTION_RULES_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "PRODUCTION_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "PRODUCTION_AUDIT_TRAFFIC_NOT_OBSERVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/auditSinkAdapterRehearsalChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);

        JsonNode evidence = readiness.at("/data/auditSinkAdapterRehearsalEvidence");
        assertThat(evidence.at("/readinessMode").asText()).isEqualTo("LOCAL_AUDIT_SINK_ADAPTER_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sinkType").asText()).isEqualTo("LOCAL_FILE_JSONL_SAMPLE");
        assertThat(evidence.at("/sinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/sampleEventPath").asText()).isEqualTo("docs/unified-backend-audit-sink-sample.jsonl");
        assertThat(evidence.at("/sampleSchemaPath").asText()).isEqualTo("docs/unified-backend-audit-sink-sample-schema.json");
        assertThat(evidence.at("/sampleEventsPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleEventsParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleEventsTotal").asInt()).isEqualTo(4);
        assertThat(evidence.at("/writeSmokeRehearsed").asBoolean()).isTrue();
        assertThat(evidence.at("/replayRehearsed").asBoolean()).isTrue();
        assertThat(evidence.at("/exportSummaryRehearsed").asBoolean()).isTrue();
        assertThat(evidence.at("/retentionPolicyRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/auditEventSchemaVersion").asText()).isEqualTo("1.0");
        assertThat(evidence.at("/requiredFieldsTotal").asInt()).isGreaterThanOrEqualTo(17);
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/productionAuditTrafficObserved").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONFIGURED")
                .contains("REAL_AUDIT_WRITE_PATH_CONNECTED")
                .contains("REAL_AUDIT_REPLAY_PATH_CONNECTED")
                .contains("REAL_AUDIT_EXPORT_PATH_CONNECTED")
                .contains("REAL_AUDIT_RETENTION_JOB_CONNECTED")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertNoSecrets(readiness);
    }

    @Test
    void auditSinkAdapterEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-audit-sink-adapter-redaction"));

        assertThat(readiness.at("/data/auditSinkAdapterRehearsalStatus").asText())
                .isEqualTo("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/auditSinkAdapterRehearsalEvidence").toString()
                + readiness.at("/data/auditSinkAdapterRehearsalChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void auditSinkSampleFilesAreParseableAndSafe() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("../../docs/unified-backend-audit-sink-sample.jsonl"));
        JsonNode schema = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-audit-sink-sample-schema.json")));

        assertThat(lines).hasSize(4);
        assertThat(schema.at("/schemaVersion").asText()).isEqualTo("1.0");
        assertThat(schema.at("/sinkType").asText()).isEqualTo("LOCAL_FILE_JSONL_SAMPLE");
        assertThat(schema.at("/sinkConnected").asBoolean()).isFalse();
        assertThat(schema.at("/writeMode").asText()).isEqualTo("APPEND_ONLY_REHEARSAL");
        assertThat(schema.at("/replayMode").asText()).isEqualTo("READ_ONLY_REHEARSAL");
        assertThat(schema.at("/exportMode").asText()).isEqualTo("SUMMARY_ONLY_REHEARSAL");
        assertThat(schema.at("/retentionMode").asText()).isEqualTo("POLICY_RECORDED_NOT_EXECUTED");

        for (String line : lines) {
            JsonNode event = objectMapper.readTree(line);
            assertThat(event.at("/schemaVersion").asText()).isEqualTo("1.0");
            assertThat(event.hasNonNull("eventId")).isTrue();
            assertThat(event.hasNonNull("occurredAt")).isTrue();
            assertThat(event.hasNonNull("requestId")).isTrue();
            assertThat(event.hasNonNull("actor")).isTrue();
            assertThat(event.hasNonNull("target")).isTrue();
            assertThat(event.hasNonNull("action")).isTrue();
            assertThat(event.hasNonNull("riskLevel")).isTrue();
            assertThat(event.hasNonNull("result")).isTrue();
            assertThat(event.hasNonNull("redactionApplied")).isTrue();
            assertThat(event.at("/productionTraffic").asBoolean()).isFalse();
            assertThat(event.at("/rehearsalOnly").asBoolean()).isTrue();
            assertThat(event.at("/sensitiveValuesExposed").asBoolean()).isFalse();
            assertThat(event.at("/actor/actorId").asText()).startsWith("local-");
            assertThat(event.at("/target/targetEntrypoint").asText()).isEqualTo("unified-backend:8135");
        }

        String text = String.join("\n", lines) + schema;
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
    }

    @Test
    void exposesProductionCutoverRunbookWithoutSwitchingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-runbook"));

        assertThat(readiness.at("/data/productionCutoverRunbookStatus").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "RUNBOOK_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "RUNBOOK_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "UNIFIED_BACKEND_CANDIDATE_READY", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "ROUTE_DRIFT_SCAN_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "LOCAL_ENTRYPOINT_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "LOCAL_CONFIG_PROVIDER_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "LOCAL_AUDIT_SINK_REHEARSAL_PASSED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "SMOKE_TARGETS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "ROLLBACK_COMMANDS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "CANARY_PLAN_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "OBSERVATION_FIELDS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "RETIREMENT_ORDER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "NO_SENSITIVE_VALUES_IN_RUNBOOK", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "EXTERNAL_ENTRYPOINT_CONFIG_NOT_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "ROLLBACK_WINDOW_NOT_STARTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "USER_RETIREMENT_APPROVAL_NOT_GRANTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverRunbookChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionCutoverRunbookEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_PRODUCTION_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleRunbookPath").asText())
                .isEqualTo("docs/unified-backend-production-cutover-runbook-sample.json");
        assertThat(evidence.at("/sampleRunbookPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleRunbookParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleRunbookApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/smokeTargetsTotal").asInt()).isEqualTo(32);
        assertThat(evidence.at("/mavenEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/rollbackCommandsRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/canaryPlanRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/observationFieldsRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/localConfigProviderRehearsalPassed").asBoolean()).isTrue();
        assertThat(evidence.at("/localAuditSinkRehearsalPassed").asBoolean()).isTrue();
        assertThat(evidence.at("/externalEntrypointConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObservedOnUnified").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowStarted").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/coreRetirementApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverRunbookEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-runbook-redaction"));

        assertThat(readiness.at("/data/productionCutoverRunbookStatus").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionCutoverRunbookEvidence").toString()
                + readiness.at("/data/productionCutoverRunbookChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverRunbookSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-runbook-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-cutover-runbook");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_RUNBOOK_REHEARSAL_NOT_APPLIED");
        assertThat(sample.at("/sampleApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/requiresUserApprovalBeforeApply").asBoolean()).isTrue();
        assertThat(sample.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(sample.at("/rollbackEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/requiredLocalEvidence").toString())
                .contains("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION")
                .contains("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION")
                .contains("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION");
        assertThat(sample.at("/verificationCommands").size()).isEqualTo(1);
        assertThat(sample.at("/canaryPlan/currentProductionTrafficPercent").asInt()).isZero();
        assertThat(sample.at("/canaryPlan/candidateProductionTrafficPercent").asInt()).isZero();
        assertThat(sample.at("/canaryPlan/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(sample.at("/rollbackPlan/rollbackCommands").size()).isEqualTo(1);
        assertThat(sample.at("/retirementPlan/deletionAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/retirementPlan/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/securityPolicy/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(sample.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/profile");
        assertThat(sample.toString().toLowerCase())
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("jdbc:")
                .doesNotContain("bucket")
                .doesNotContain("topic")
                .doesNotContain("c:\\users\\")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe");
    }

    @Test
    void exposesProductionCutoverApprovalPackageWithoutApprovingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-approval-package"));

        assertThat(readiness.at("/data/productionCutoverApprovalPackageStatus").asText())
                .isEqualTo("PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "APPROVAL_PACKAGE_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "APPROVAL_PACKAGE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "EXISTING_LOCAL_EVIDENCE_REFERENCED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "EXTERNAL_PARAMETER_CHECKLIST_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "APPROVAL_ROLES_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "GO_NO_GO_MATRIX_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "OBSERVATION_CHECKLIST_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "ROLLBACK_AUTHORITY_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "RETIREMENT_GATE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "NO_SENSITIVE_VALUES_IN_APPROVAL_PACKAGE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "REAL_EXTERNAL_ENTRYPOINT_VALUES_NOT_PROVIDED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "PRODUCTION_TRAFFIC_NOT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "ROLLBACK_OPERATOR_NOT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "RETIREMENT_APPROVER_NOT_GRANTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverApprovalPackageChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionCutoverApprovalPackageEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_PRODUCTION_CUTOVER_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleApprovalPackagePath").asText())
                .isEqualTo("docs/unified-backend-production-cutover-approval-package-sample.json");
        assertThat(evidence.at("/sampleApprovalPackagePresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleApprovalPackageParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/approvalPackageApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresUserApprovalBeforeApply").asBoolean()).isTrue();
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/existingEvidenceReferencedTotal").asInt()).isGreaterThanOrEqualTo(7);
        assertThat(evidence.at("/externalParametersTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/approvalRolesTotal").asInt()).isGreaterThanOrEqualTo(7);
        assertThat(evidence.at("/goNoGoItemsTotal").asInt()).isGreaterThanOrEqualTo(15);
        assertThat(evidence.at("/observationFieldsTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/verificationCommandsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/externalEntrypointValuesProvided").asBoolean()).isFalse();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWriteSmokePassed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackOperatorApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/retirementApproverGranted").asBoolean()).isFalse();
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPROVED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverApprovalPackageEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-approval-package-redaction"));

        assertThat(readiness.at("/data/productionCutoverApprovalPackageStatus").asText())
                .isEqualTo("PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionCutoverApprovalPackageEvidence").toString()
                + readiness.at("/data/productionCutoverApprovalPackageChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverApprovalPackageSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-approval-package-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-cutover-approval-package");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_APPLIED");
        assertThat(sample.at("/approvalPackageApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/requiresUserApprovalBeforeApply").asBoolean()).isTrue();
        assertThat(sample.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(sample.at("/rollbackEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/evidenceInputs").toString())
                .contains("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION")
                .contains("PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION")
                .contains("PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION")
                .contains("PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION");
        assertThat(sample.at("/externalParameterChecklist").size()).isGreaterThanOrEqualTo(10);
        assertThat(sample.at("/approvalMatrix").size()).isGreaterThanOrEqualTo(7);
        assertThat(sample.at("/goNoGoMatrix").toString())
                .contains("UNIFIED_BACKEND_CANDIDATE_READY")
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("BLOCKED");
        assertThat(sample.at("/observationChecklist").size()).isGreaterThanOrEqualTo(10);
        assertThat(sample.at("/verificationCommands").size()).isEqualTo(1);
        assertThat(sample.at("/rollbackAuthority/rollbackOperatorApproved").asBoolean()).isFalse();
        assertThat(sample.at("/rollbackAuthority/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(sample.at("/retirementGate/deletionAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/retirementGate/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(sample.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/profile");
        assertThat(sample.toString().toLowerCase())
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("jdbc:")
                .doesNotContain("bucket")
                .doesNotContain("topic")
                .doesNotContain("c:\\users\\");
    }

    @Test
    void exposesProductionCutoverExternalParameterManifestWithoutApplyingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-external-parameter-manifest"));

        assertThat(readiness.at("/data/productionCutoverExternalParameterManifestStatus").asText())
                .isEqualTo("PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "EXTERNAL_PARAMETER_MANIFEST_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "EXTERNAL_PARAMETER_MANIFEST_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "PARAMETER_GROUPS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "FRONTEND_ENTRYPOINT_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "PROXY_UPSTREAM_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "DEPLOYMENT_ENTRYPOINT_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "CENTRAL_CONFIG_PROVIDER_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "PRODUCTION_PROFILE_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "SENSITIVE_CONFIG_EXTERNALIZATION_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "AUDIT_SINK_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "OBSERVABILITY_PARAMETER_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "APPROVAL_REFERENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "ROLLBACK_AUTHORITY_REFERENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "RETIREMENT_APPROVAL_REFERENCE_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "NO_REAL_VALUES_IN_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "REAL_EXTERNAL_ENTRYPOINT_VALUES_NOT_PROVIDED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverExternalParameterManifestChecks", "RETIREMENT_NOT_APPROVED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionCutoverExternalParameterManifestEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleManifestPath").asText())
                .isEqualTo("docs/unified-backend-production-cutover-external-parameters-sample.json");
        assertThat(evidence.at("/sampleManifestPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleManifestParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/manifestApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/parameterGroupsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/parametersTotal").asInt()).isGreaterThanOrEqualTo(20);
        assertThat(evidence.at("/requiredExternalParametersTotal").asInt()).isGreaterThanOrEqualTo(20);
        assertThat(evidence.at("/realValuesProvidedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/redactedParametersTotal").asInt()).isGreaterThanOrEqualTo(20);
        assertThat(evidence.at("/approvalPackageReferenced").asBoolean()).isTrue();
        assertThat(evidence.at("/approvalPackageApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWriteSmokePassed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObservedOnUnified").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/retirementApproverGranted").asBoolean()).isFalse();
        assertThat(evidence.at("/deletionAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_VALUES_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverExternalParameterManifestEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-external-parameter-manifest-redaction"));

        assertThat(readiness.at("/data/productionCutoverExternalParameterManifestStatus").asText())
                .isEqualTo("PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionCutoverExternalParameterManifestEvidence").toString()
                + readiness.at("/data/productionCutoverExternalParameterManifestChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase().replace("requiresexternalsecretstore", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverExternalParameterManifestSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-external-parameters-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-cutover-external-parameters");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_APPLIED");
        assertThat(sample.at("/manifestApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/currentEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/parameterGroups").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/approvalPackageReference/path").asText())
                .isEqualTo("docs/unified-backend-production-cutover-approval-package-sample.json");
        assertThat(sample.at("/approvalPackageReference/approvalPackageApplied").asBoolean()).isFalse();
        assertThat(sample.at("/approvalPackageReference/productionTrafficApproved").asBoolean()).isFalse();
        assertThat(sample.at("/approvalPackageReference/rollbackOperatorApproved").asBoolean()).isFalse();
        assertThat(sample.at("/approvalPackageReference/retirementApproverGranted").asBoolean()).isFalse();
        assertThat(sample.at("/goNoGoImpact").toString())
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_APPLIED")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("BLOCKED");
        assertThat(sample.at("/validationRules/allRequiredParametersDeclared").asBoolean()).isTrue();
        assertThat(sample.at("/validationRules/allRealValuesExternalized").asBoolean()).isTrue();
        assertThat(sample.at("/validationRules/noProductionTrafficValue").asBoolean()).isTrue();
        assertThat(sample.at("/validationRules/noRuntimeCommandValue").asBoolean()).isTrue();
        assertThat(sample.at("/validationRules/noCredentialValue").asBoolean()).isTrue();
        assertThat(sample.at("/validationRules/noLocalAbsolutePath").asBoolean()).isTrue();
        assertThat(sample.at("/redactionPolicy/forbiddenValues").toString().toLowerCase())
                .contains("authorization")
                .contains("cookie")
                .contains("token")
                .contains("secret")
                .contains("password")
                .contains("jdbc:")
                .contains("kubectl")
                .contains("docker")
                .contains("powershell")
                .contains("cmd.exe");
        assertThat(sample.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/profile")
                .doesNotContain("http://")
                .doesNotContain("https://");
        String parameterText = sample.at("/parameterGroups").toString().toLowerCase()
                + sample.at("/approvalPackageReference").toString().toLowerCase()
                + sample.at("/goNoGoImpact").toString().toLowerCase()
                + sample.at("/verificationCommands").toString().toLowerCase();
        assertThat(parameterText.replace("requiresexternalsecretstore", ""))
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env");
    }

    @Test
    void exposesProductionCutoverEvidenceConsistencyAuditWithoutApplyingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-evidence-consistency-audit"));

        assertThat(readiness.at("/data/productionCutoverEvidenceConsistencyAuditStatus").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "CUTOVER_EVIDENCE_SAMPLES_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "CUTOVER_EVIDENCE_SAMPLES_PARSEABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "CANDIDATE_ENTRYPOINT_CONSISTENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "CURRENT_ENTRYPOINT_CONSISTENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "ROLLBACK_ENTRYPOINT_CONSISTENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "MAVEN_REGRESSION_COMMANDS_CONSISTENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "EXTERNAL_PARAMETER_KEYS_REFERENCED_BY_APPROVAL_PACKAGE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "RUNBOOK_REFERENCES_EXTERNAL_PARAMETER_MANIFEST", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "CENTRAL_CONFIG_KEYS_COVERED_BY_MANIFEST", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "AUDIT_SINK_KEYS_COVERED_BY_MANIFEST", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "OBSERVABILITY_KEYS_COVERED_BY_RUNBOOK_AND_MANIFEST", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "RETIREMENT_AND_ROLLBACK_GATES_CONSISTENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "NO_REAL_VALUES_IN_CUTOVER_EVIDENCE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "REAL_EXTERNAL_VALUES_NOT_IMPORTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionCutoverEvidenceConsistencyAuditChecks", "RETIREMENT_NOT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_RECORDED", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionCutoverEvidenceConsistencyAuditEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION");
        assertThat(evidence.at("/auditedSamplePaths").size()).isEqualTo(7);
        assertThat(evidence.at("/auditedSamplePaths").toString())
                .contains("docs/deployment-entrypoint-cutover-sample.json")
                .contains("docs/unified-backend-central-config-provider-sample.json")
                .contains("docs/unified-backend-audit-sink-sample.jsonl")
                .contains("docs/unified-backend-audit-sink-sample-schema.json")
                .contains("docs/unified-backend-production-cutover-runbook-sample.json")
                .contains("docs/unified-backend-production-cutover-approval-package-sample.json")
                .contains("docs/unified-backend-production-cutover-external-parameters-sample.json");
        assertThat(evidence.at("/samplesPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/samplesParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/externalParameterKeysTotal").asInt()).isGreaterThanOrEqualTo(30);
        assertThat(evidence.at("/approvalPackageExternalParametersTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/runbookVerificationCommandsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/manifestVerificationCommandsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/missingApprovalParameterKeys")).isEmpty();
        assertThat(evidence.at("/missingManifestParameterKeys")).isEmpty();
        assertThat(evidence.at("/inconsistentEntrypointRefs")).isEmpty();
        assertThat(evidence.at("/inconsistentVerificationCommands")).isEmpty();
        assertThat(evidence.at("/inconsistentBlockers")).isEmpty();
        assertThat(evidence.at("/realValuesProvidedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_EXTERNAL_ENTRYPOINT_CONFIG_VALUES_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverEvidenceConsistencyAuditDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-cutover-evidence-consistency-redaction"));

        assertThat(readiness.at("/data/productionCutoverEvidenceConsistencyAuditStatus").asText())
                .isEqualTo("PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION");
        String text = readiness.at("/data/productionCutoverEvidenceConsistencyAuditEvidence").toString()
                + readiness.at("/data/productionCutoverEvidenceConsistencyAuditChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionCutoverEvidenceSamplesRemainParseableAndConsistent() throws Exception {
        JsonNode entrypoint = objectMapper.readTree(Files.readString(Path.of("../../docs/deployment-entrypoint-cutover-sample.json")));
        JsonNode config = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-central-config-provider-sample.json")));
        JsonNode auditSchema = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-audit-sink-sample-schema.json")));
        JsonNode runbook = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-runbook-sample.json")));
        JsonNode approval = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-approval-package-sample.json")));
        JsonNode manifest = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-cutover-external-parameters-sample.json")));
        List<String> auditEventLines = Files.readAllLines(Path.of("../../docs/unified-backend-audit-sink-sample.jsonl"));
        for (String line : auditEventLines) {
            if (!line.isBlank()) {
                assertThat(objectMapper.readTree(line).path("sensitiveValuesExposed").asBoolean()).isFalse();
            }
        }

        assertThat(entrypoint.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(config.at("/entrypoints/candidate/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(runbook.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(approval.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(entrypoint.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(config.at("/entrypoints/current/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(runbook.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(approval.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");

        assertThat(runbook.at("/verificationCommands").toString())
                .isEqualTo(manifest.at("/verificationCommands").toString())
                .isEqualTo(approval.at("/verificationCommands").toString());
        assertThat(runbook.at("/verificationCommands").size()).isEqualTo(1);
        assertThat(manifest.at("/verificationCommands").size()).isEqualTo(1);
        assertThat(approval.at("/verificationCommands").size()).isEqualTo(1);

        String manifestKeys = manifest.at("/parameterGroups").toString();
        assertThat(manifestKeys)
                .contains("frontendApiBaseUrl")
                .contains("reverseProxyUpstream")
                .contains("deploymentEntrypointTarget")
                .contains("centralConfigProviderRef")
                .contains("persistentAuditSinkRef")
                .contains("auditWriteSmokeRef")
                .contains("httpSmokeObservationRef")
                .contains("retirementApproverRef");
        assertThat(approval.at("/externalParameterChecklist").toString())
                .contains("frontendApiBaseUrlConfigLocation")
                .contains("reverseProxyUpstreamConfigLocation")
                .contains("deploymentEntrypointConfigLocation")
                .contains("centralConfigProviderType")
                .contains("persistentAuditSinkType")
                .contains("productionObservationDashboardLocation")
                .contains("retirementApproverRef");
        assertThat(runbook.toString())
                .contains("docs/unified-backend-production-cutover-external-parameters-sample.json");

        assertThat(config.at("/configDomains").toString())
                .contains("central-config")
                .contains("audit-sink")
                .contains("rollback")
                .contains("retirement-gates");
        assertThat(auditSchema.at("/requiredFields").toString())
                .contains("requestId")
                .contains("entrypoint")
                .contains("actor")
                .contains("target")
                .contains("action");
        assertThat(runbook.at("/observationPlan/fields").toString())
                .contains("httpSmokePassRate")
                .contains("auditWriteSuccessCount")
                .contains("apiGatewayTrafficCount")
                .contains("unifiedBackendTrafficCount");
        assertThat(manifestKeys)
                .contains("httpSmokeObservationRef")
                .contains("auditWriteSuccessCountRef")
                .contains("trafficCounterRef")
                .contains("rollbackTriggerCountRef");

        assertThat(approval.at("/retirementGate/deletionAllowed").asBoolean()).isFalse();
        assertThat(approval.at("/retirementGate/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(runbook.at("/retirementPlan/deletionAllowed").asBoolean()).isFalse();
        assertThat(runbook.at("/retirementPlan/bulkRetirementAllowed").asBoolean()).isFalse();
        assertThat(manifest.at("/goNoGoImpact").toString())
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED")
                .contains("BLOCKED");
    }

    @Test
    void exposesProductionExternalValueIntakeRehearsalWithoutImportingValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-external-value-intake"));

        assertThat(readiness.at("/data/productionExternalValueIntakeRehearsalStatus").asText())
                .isEqualTo("PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "EXTERNAL_VALUE_INTAKE_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "EXTERNAL_VALUE_INTAKE_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "VALUE_GROUPS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "INTAKE_CHANNELS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "INJECTION_TARGETS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "VALIDATION_REFS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "ROLLBACK_REFS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "NO_REAL_VALUES_IN_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "NO_SENSITIVE_VALUES_IN_INTAKE_SAMPLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "EXTERNAL_VALUE_INTAKE_REHEARSAL_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "REAL_EXTERNAL_VALUES_NOT_PROVIDED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "REAL_ENTRYPOINT_NOT_APPLIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionExternalValueIntakeRehearsalChecks", "RETIREMENT_NOT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "EXTERNAL_VALUE_INTAKE_REHEARSAL_RECORDED", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionExternalValueIntakeRehearsalEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleIntakePath").asText())
                .isEqualTo("docs/unified-backend-production-external-value-intake-sample.json");
        assertThat(evidence.at("/sampleIntakePresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleIntakeParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/intakeApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(evidence.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(evidence.at("/currentEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/valueGroupsTotal").asInt()).isGreaterThanOrEqualTo(7);
        assertThat(evidence.at("/intakeChannelsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/valueItemsTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/injectionTargetsTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/validationRefsTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/rollbackRefsTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/realValuesProvidedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/redactedValuesTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWriteSmokePassed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObservedOnUnified").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/retirementApproverGranted").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_EXTERNAL_VALUES_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION");
        assertNoSecrets(readiness);
    }

    @Test
    void productionExternalValueIntakeRehearsalDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-external-value-intake-redaction"));

        assertThat(readiness.at("/data/productionExternalValueIntakeRehearsalStatus").asText())
                .isEqualTo("PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionExternalValueIntakeRehearsalEvidence").toString()
                + readiness.at("/data/productionExternalValueIntakeRehearsalChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase()
                .replace("requiresexternalsecretstore", "")
                .replace("sensitiveconfigexternalized", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("privatekey");
        assertNoSecrets(readiness);
    }

    @Test
    void productionExternalValueIntakeSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-external-value-intake-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-external-value-intake");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_APPLIED");
        assertThat(sample.at("/intakeApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/currentEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/intakeChannels").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/requiredValueGroups").size()).isGreaterThanOrEqualTo(7);
        assertThat(sample.at("/requiredValueGroups").toString())
                .contains("external-entrypoint")
                .contains("central-config")
                .contains("audit-sink")
                .contains("observability")
                .contains("approval")
                .contains("rollback")
                .contains("retirement");

        int valueItems = 0;
        for (JsonNode group : sample.at("/requiredValueGroups")) {
            assertThat(group.path("group").asText()).isNotBlank();
            for (JsonNode item : group.path("values")) {
                valueItems++;
                assertThat(item.path("key").asText()).isNotBlank();
                assertThat(item.path("group").asText()).isEqualTo(group.path("group").asText());
                assertThat(item.path("sourceChannelKey").asText()).isNotBlank();
                String valueRef = item.path("valueRef").asText();
                assertThat(valueRef.startsWith("EXTERNAL_REF_REQUIRED:")
                        || valueRef.startsWith("LOCAL_SAMPLE_REF:")
                        || valueRef.startsWith("APPROVAL_REF_REQUIRED:")).isTrue();
                assertThat(item.path("injectionTarget").asText()).isNotBlank();
                assertThat(item.path("validationRef").asText()).isNotBlank();
                assertThat(item.path("rollbackRef").asText()).isNotBlank();
                assertThat(item.path("realValueProvidedInRepository").asBoolean()).isFalse();
                assertThat(item.path("redacted").asBoolean()).isTrue();
            }
        }
        assertThat(valueItems).isGreaterThanOrEqualTo(14);

        assertThat(sample.at("/goNoGoImpact").toString())
                .contains("REAL_EXTERNAL_VALUES_APPLIED_TO_RUNTIME")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("BLOCKED");
        assertThat(sample.at("/redactionPolicy/forbiddenValues").toString().toLowerCase())
                .contains("authorization")
                .contains("cookie")
                .contains("token")
                .contains("secret")
                .contains("password")
                .contains("jdbc:")
                .contains("kubectl")
                .contains("docker")
                .contains("powershell")
                .contains("cmd.exe");

        String safeValueText = sample.at("/requiredValueGroups").toString().toLowerCase()
                + sample.at("/intakeChannels").toString().toLowerCase()
                + sample.at("/validationPlan").toString().toLowerCase()
                + sample.at("/rollbackPlan").toString().toLowerCase()
                + sample.at("/approvalPlan").toString().toLowerCase()
                + sample.at("/goNoGoImpact").toString().toLowerCase()
                + sample.at("/verificationCommands").toString().toLowerCase();
        assertThat(safeValueText
                .replace("requiresexternalsecretstore", "")
                .replace("sensitiveconfigexternalizationref", ""))
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env");
    }

    @Test
    void exposesProductionRuntimeConfigShellWithoutBindingRealRuntime() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-runtime-config-shell"));

        assertThat(readiness.at("/data/productionRuntimeConfigShellStatus").asText())
                .isEqualTo("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "PRODUCTION_RUNTIME_SHELL_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "PRODUCTION_RUNTIME_SHELL_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "RUNTIME_PROFILE_SLOT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "CENTRAL_CONFIG_PROVIDER_SLOT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "SENSITIVE_CONFIG_EXTERNALIZATION_SLOT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "DEPLOYMENT_ENTRYPOINT_SLOT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "ROLLBACK_CONFIG_SLOT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "EXTERNAL_VALUE_INTAKE_REHEARSAL_REFERENCED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "NO_REAL_VALUES_IN_REPOSITORY", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "NO_SENSITIVE_VALUES_IN_RUNTIME_SHELL_SAMPLE", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_PRODUCTION_PROFILE_NOT_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_CENTRAL_CONFIG_PROVIDER_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_SENSITIVE_CONFIG_NOT_EXTERNALIZED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_DEPLOYMENT_ENTRYPOINT_NOT_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_ROLLBACK_CONFIG_NOT_BOUND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "REAL_AUDIT_SINK_NOT_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "PRODUCTION_TRAFFIC_NOT_SWITCHED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionRuntimeConfigShellChecks", "RETIREMENT_NOT_APPROVED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_RECORDED", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionRuntimeConfigShellEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleRuntimeShellPath").asText())
                .isEqualTo("docs/unified-backend-production-runtime-shell-sample.json");
        assertThat(evidence.at("/sampleRuntimeShellPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleRuntimeShellParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/runtimeShellApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresExternalConfigProvider").asBoolean()).isTrue();
        assertThat(evidence.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(evidence.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(evidence.at("/currentEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/runtimeProfilesTotal").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(evidence.at("/configProviderBindingsTotal").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(evidence.at("/sensitiveConfigBindingsTotal").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(evidence.at("/deploymentEntrypointBindingsTotal").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(evidence.at("/rollbackConfigBindingsTotal").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(evidence.at("/validationCommandsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/productionProfileBound").asBoolean()).isFalse();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveConfigExternalized").asBoolean()).isFalse();
        assertThat(evidence.at("/deploymentEntrypointBound").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackConfigBound").asBoolean()).isFalse();
        assertThat(evidence.at("/persistentAuditSinkConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWriteSmokePassed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObservedOnUnified").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/retirementApproverGranted").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesProvidedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_PRODUCTION_PROFILE_BOUND_OUTSIDE_REPOSITORY")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_SENSITIVE_CONFIG_SOURCE_EXTERNALIZED")
                .contains("REAL_DEPLOYMENT_ENTRYPOINT_BOUND")
                .contains("REAL_ROLLBACK_CONFIG_BOUND")
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        assertNoSecrets(readiness);
    }

    @Test
    void productionRuntimeConfigShellDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-runtime-config-shell-redaction"));

        assertThat(readiness.at("/data/productionRuntimeConfigShellStatus").asText())
                .isEqualTo("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/productionRuntimeConfigShellEvidence").toString()
                + readiness.at("/data/productionRuntimeConfigShellChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase()
                .replace("requiresexternalconfigprovider", "")
                .replace("requiresexternalsecretstore", "")
                .replace("sensitiveconfigexternalized", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("privatekey");
        assertNoSecrets(readiness);
    }

    @Test
    void productionRuntimeConfigShellSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-runtime-shell-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-runtime-shell");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_APPLIED");
        assertThat(sample.at("/runtimeShellApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/requiresExternalConfigProvider").asBoolean()).isTrue();
        assertThat(sample.at("/requiresExternalSecretStore").asBoolean()).isTrue();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/currentEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/runtimeProfiles").size()).isGreaterThanOrEqualTo(3);
        assertThat(sample.at("/runtimeProfiles").toString())
                .contains("production")
                .contains("rollback")
                .contains("local-rehearsal")
                .contains("EXTERNAL_REF_REQUIRED:PRODUCTION_PROFILE")
                .contains("EXTERNAL_REF_REQUIRED:ROLLBACK_PROFILE")
                .contains("LOCAL_SAMPLE_REF:LOCAL_RUNTIME_CONFIG_SHELL_REHEARSAL");
        assertThat(sample.at("/configProviderBindings").size()).isGreaterThanOrEqualTo(5);
        assertThat(sample.at("/sensitiveConfigBindings").size()).isGreaterThanOrEqualTo(5);
        assertThat(sample.at("/deploymentEntrypointBindings").size()).isGreaterThanOrEqualTo(5);
        assertThat(sample.at("/rollbackConfigBindings").size()).isGreaterThanOrEqualTo(5);
        assertThat(sample.at("/validationPlan/externalValueIntakeSampleRef").asText())
                .isEqualTo("docs/unified-backend-production-external-value-intake-sample.json");
        assertThat(sample.at("/validationPlan/externalValueIntakeStatusRequired").asText())
                .isEqualTo("PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION");

        for (String arrayName : List.of("configProviderBindings", "sensitiveConfigBindings", "deploymentEntrypointBindings", "rollbackConfigBindings")) {
            for (JsonNode item : sample.at("/" + arrayName)) {
                assertThat(item.path("key").asText()).isNotBlank();
                assertThat(item.path("validationRef").asText()).isNotBlank();
                assertThat(item.path("rollbackRef").asText()).isNotBlank();
                assertThat(item.path("realValueProvidedInRepository").asBoolean()).isFalse();
                assertThat(item.path("redacted").asBoolean()).isTrue();
            }
        }
        for (JsonNode item : sample.at("/sensitiveConfigBindings")) {
            assertThat(item.path("secretStoreRef").asText()).startsWith("EXTERNAL_REF_REQUIRED:");
            assertThat(item.path("externalValueRequired").asBoolean()).isTrue();
        }

        assertThat(sample.at("/goNoGoImpact").toString())
                .contains("REAL_PRODUCTION_PROFILE_BOUND_OUTSIDE_REPOSITORY")
                .contains("REAL_CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("REAL_SENSITIVE_CONFIG_SOURCE_EXTERNALIZED")
                .contains("REAL_DEPLOYMENT_ENTRYPOINT_BOUND")
                .contains("REAL_ROLLBACK_CONFIG_BOUND")
                .contains("BLOCKED");
        assertThat(sample.at("/redactionPolicy/forbiddenValues").toString().toLowerCase())
                .contains("authorization")
                .contains("cookie")
                .contains("token")
                .contains("secret")
                .contains("password")
                .contains("jdbc:")
                .contains("kubectl")
                .contains("docker")
                .contains("powershell")
                .contains("cmd.exe");

        String safeValueText = sample.at("/runtimeProfiles").toString().toLowerCase()
                + sample.at("/configProviderBindings").toString().toLowerCase()
                + sample.at("/sensitiveConfigBindings").toString().toLowerCase()
                + sample.at("/deploymentEntrypointBindings").toString().toLowerCase()
                + sample.at("/rollbackConfigBindings").toString().toLowerCase()
                + sample.at("/validationPlan").toString().toLowerCase()
                + sample.at("/goNoGoImpact").toString().toLowerCase()
                + sample.at("/verificationCommands").toString().toLowerCase();
        assertThat(safeValueText
                .replace("requiresexternalsecretstore", "")
                .replace("sensitiveconfigbindings", "")
                .replace("secretstoreref", ""))
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env");
    }

    @Test
    void exposesProductionAuditObservabilitySmokeWithoutConnectingProductionPlatforms() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-audit-observability-smoke"));

        assertThat(readiness.at("/data/productionAuditObservabilitySmokeStatus").asText())
                .isEqualTo("PASS_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        for (String check : List.of(
                "AUDIT_OBSERVABILITY_SMOKE_SAMPLE_PRESENT",
                "AUDIT_OBSERVABILITY_SMOKE_SAMPLE_JSON_PARSABLE",
                "RUNTIME_CONFIG_SHELL_REHEARSAL_REFERENCED",
                "AUDIT_SINK_BINDING_RECORDED",
                "AUDIT_EVENT_SCHEMA_RECORDED",
                "REQUEST_ID_PROPAGATION_RECORDED",
                "AUDIT_WRITE_SMOKE_REFERENCE_RECORDED",
                "AUDIT_REPLAY_EXPORT_RETENTION_RECORDED",
                "HTTP_SMOKE_OBSERVATION_RECORDED",
                "ERROR_RATE_OBSERVATION_RECORDED",
                "LATENCY_OBSERVATION_RECORDED",
                "BUSINESS_CODE_OBSERVATION_RECORDED",
                "TRACE_CORRELATION_RECORDED",
                "DASHBOARD_AND_ALERT_REFERENCES_RECORDED",
                "NO_SENSITIVE_VALUES_IN_AUDIT_OBSERVABILITY_SAMPLE",
                "READY_FLAGS_REMAIN_FALSE",
                "PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_RECORDED")) {
            assertPrecheck(readiness, "/data/productionAuditObservabilitySmokeChecks", check, "PASS", true);
        }
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_RECORDED", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionAuditObservabilitySmokeEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleAuditObservabilitySmokePath").asText())
                .isEqualTo("docs/unified-backend-production-audit-observability-smoke-sample.json");
        assertThat(evidence.at("/sampleAuditObservabilitySmokePresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleAuditObservabilitySmokeParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/runtimeConfigShellSampleRef").asText())
                .isEqualTo("docs/unified-backend-production-runtime-shell-sample.json");
        assertThat(evidence.at("/runtimeConfigShellStatusRequired").asText())
                .isEqualTo("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/auditSinkBindingRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/auditEventSchemaRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/requestIdPropagationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/auditWriteSmokeReferenceRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/auditReplayExportRetentionRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/httpSmokeObservationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/errorRateObservationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/latencyObservationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/businessCodeObservationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/traceCorrelationRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/dashboardReferencesRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/alertReferencesRecorded").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleAuditSmokeTargetsTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/sampleObservabilitySignalsTotal").asInt()).isGreaterThanOrEqualTo(12);
        assertThat(evidence.at("/sampleDashboardRefsTotal").asInt()).isGreaterThanOrEqualTo(8);
        assertThat(evidence.at("/sampleAlertRefsTotal").asInt()).isGreaterThanOrEqualTo(8);
        assertThat(evidence.at("/sampleRollbackRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/realValuesProvidedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText())
                .isEqualTo("PASS_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION");
        assertNoSecrets(readiness);
    }

    @Test
    void productionAuditObservabilitySmokeKeepsRealProductionBlockersBlocked() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-audit-observability-smoke-blockers"));

        for (String check : List.of(
                "REAL_PERSISTENT_AUDIT_SINK_NOT_CONNECTED",
                "REAL_AUDIT_WRITE_PATH_NOT_CONNECTED",
                "REAL_AUDIT_WRITE_SMOKE_NOT_PASSED",
                "REAL_AUDIT_REPLAY_EXPORT_RETENTION_NOT_CONNECTED",
                "REAL_OBSERVABILITY_PLATFORM_NOT_CONNECTED",
                "REAL_DASHBOARD_NOT_CONNECTED",
                "REAL_ALERTING_NOT_CONNECTED",
                "REAL_TRACE_PIPELINE_NOT_CONNECTED",
                "PRODUCTION_TRAFFIC_NOT_SWITCHED",
                "PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED",
                "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN",
                "ROLLBACK_WINDOW_NOT_COMPLETED",
                "RETIREMENT_NOT_APPROVED")) {
            assertPrecheck(readiness, "/data/productionAuditObservabilitySmokeChecks", check, "BLOCKED", true);
        }

        JsonNode evidence = readiness.at("/data/productionAuditObservabilitySmokeEvidence");
        for (String field : List.of(
                "persistentAuditSinkConnected",
                "auditWritePathConnected",
                "auditWriteSmokePassed",
                "auditReplayPathConnected",
                "auditExportPathConnected",
                "auditRetentionJobConnected",
                "observabilityPlatformConnected",
                "dashboardConnected",
                "alertingConnected",
                "tracePipelineConnected",
                "environmentVariablesRead",
                "productionTrafficObservedOnUnified",
                "apiGatewayTrafficZeroProven",
                "rollbackWindowCompleted",
                "retirementApproverGranted",
                "realValuesProvidedInRepository",
                "sensitiveValuesExposed",
                "readyForProduction",
                "readyToReplaceGateway")) {
            assertThat(evidence.at("/" + field).asBoolean()).as(field).isFalse();
        }
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("REAL_PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("REAL_AUDIT_WRITE_PATH_CONNECTED")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("REAL_OBSERVABILITY_PLATFORM_CONNECTED")
                .contains("REAL_DASHBOARD_CONNECTED")
                .contains("REAL_ALERTING_CONNECTED")
                .contains("REAL_TRACE_PIPELINE_CONNECTED")
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
    }

    @Test
    void productionAuditObservabilitySmokeDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-audit-observability-smoke-redaction"));

        String text = readiness.at("/data/productionAuditObservabilitySmokeEvidence").toString()
                + readiness.at("/data/productionAuditObservabilitySmokeChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase()
                .replace("auditobservabilitysmoke", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void productionAuditObservabilitySmokeSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-audit-observability-smoke-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-audit-observability-smoke");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_CONNECTED");
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/runtimeConfigShellSampleRef").asText())
                .isEqualTo("docs/unified-backend-production-runtime-shell-sample.json");
        assertThat(sample.at("/runtimeConfigShellStatusRequired").asText())
                .isEqualTo("PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION");
        assertThat(sample.at("/auditSinkBindings").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/auditEventSchema/fields").size()).isGreaterThanOrEqualTo(21);
        assertThat(sample.at("/auditSmokeTargets").size()).isGreaterThanOrEqualTo(10);
        assertThat(sample.at("/observabilitySignals").size()).isGreaterThanOrEqualTo(12);
        assertThat(sample.at("/dashboardRefs").size()).isGreaterThanOrEqualTo(8);
        assertThat(sample.at("/alertRefs").size()).isGreaterThanOrEqualTo(8);
        assertThat(sample.at("/rollbackRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/auditSmokeTargets").toString())
                .contains("/api/v1/auth/login")
                .contains("/api/v1/profile/me")
                .contains("/api/v1/resources")
                .contains("/api/v1/whitelist")
                .contains("/api/v1/exams")
                .contains("/api/v1/attendance")
                .contains("/api/v1/activity")
                .contains("/api/v1/ops-control/overview")
                .contains("/api/v1/unified-backend/admin/readiness");
        assertThat(sample.at("/observabilitySignals").toString())
                .contains("HTTP_SMOKE_STATUS")
                .contains("ERROR_RATE")
                .contains("P95_LATENCY")
                .contains("P99_LATENCY")
                .contains("BUSINESS_CODE_DISTRIBUTION")
                .contains("TRACE_CORRELATION")
                .contains("ROLLBACK_TAG");
        for (String arrayName : List.of("auditSinkBindings", "dashboardRefs", "alertRefs", "rollbackRefs")) {
            for (JsonNode item : sample.at("/" + arrayName)) {
                assertThat(item.toString())
                        .contains("EXTERNAL_REF_REQUIRED:")
                        .doesNotContain("http://")
                        .doesNotContain("https://");
            }
        }

        String safeValueText = sample.toString().toLowerCase()
                .replace(sample.at("/redactionPolicy").toString().toLowerCase(), "")
                .replace("tokenredacted", "")
                .replace("redactionrequired", "");
        assertThat(safeValueText)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("x-gateway-internal-signature")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env");
    }

    @Test
    void exposesControlledCutoverReceiptGateWithoutFakingProductionTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-controlled-cutover-receipt-gate"));

        assertThat(readiness.at("/data/productionControlledCutoverStatus").asText())
                .isEqualTo("BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        for (String check : List.of(
                "CONTROLLED_CUTOVER_RECEIPT_SAMPLE_PRESENT",
                "CONTROLLED_CUTOVER_RECEIPT_SAMPLE_JSON_PARSABLE",
                "RUNTIME_CONFIG_SHELL_REHEARSAL_REFERENCED",
                "AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_REFERENCED",
                "EXTERNAL_VALUE_INTAKE_REHEARSAL_REFERENCED",
                "APPROVAL_REFS_RECORDED",
                "TRAFFIC_PLAN_RECORDED",
                "SMOKE_REFS_RECORDED",
                "AUDIT_REFS_RECORDED",
                "OBSERVABILITY_REFS_RECORDED",
                "ROLLBACK_WINDOW_REFS_RECORDED",
                "OLD_ENTRYPOINT_PROTECTION_RECORDED",
                "NO_REAL_VALUES_IN_REPOSITORY",
                "NO_SENSITIVE_VALUES_IN_CONTROLLED_CUTOVER_RECEIPT",
                "READY_FLAGS_REMAIN_FALSE",
                "CONTROLLED_CUTOVER_RECEIPT_GATE_RECORDED")) {
            assertPrecheck(readiness, "/data/productionControlledCutoverChecks", check, "PASS", true);
        }
        for (String check : List.of(
                "REAL_CUTOVER_RECEIPT_NOT_PROVIDED",
                "REAL_ENTRYPOINT_NOT_APPLIED",
                "PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED",
                "REAL_AUDIT_WRITE_SMOKE_NOT_PASSED",
                "REAL_DASHBOARD_NOT_VERIFIED",
                "REAL_ALERTING_NOT_VERIFIED",
                "REAL_TRACE_PIPELINE_NOT_VERIFIED",
                "ROLLBACK_WINDOW_NOT_COMPLETED",
                "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN",
                "RETIREMENT_NOT_APPROVED",
                "OLD_ENTRYPOINTS_NOT_IN_RETIREMENT_ROUND")) {
            assertPrecheck(readiness, "/data/productionControlledCutoverChecks", check, "BLOCKED", true);
        }
        assertPrecheck(readiness, "/data/productionHardeningPrecheckChecks", "CONTROLLED_CUTOVER_RECEIPT_GATE_RECORDED", "PASS", true);

        JsonNode evidence = readiness.at("/data/productionControlledCutoverEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_CONTROLLED_CUTOVER_RECEIPT_GATE_NOT_PRODUCTION");
        assertThat(evidence.at("/receiptPath").asText())
                .isEqualTo("docs/unified-backend-production-controlled-cutover-receipt-sample.json");
        assertThat(evidence.at("/receiptPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/receiptParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/receiptApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(evidence.at("/previousEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/approvalRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/runtimePrerequisiteRefsTotal").asInt()).isGreaterThanOrEqualTo(7);
        assertThat(evidence.at("/trafficStagesTotal").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(evidence.at("/smokeRefsTotal").asInt()).isGreaterThanOrEqualTo(14);
        assertThat(evidence.at("/auditRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/observabilityRefsTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/rollbackWindowRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/apiGatewayTrafficRefsTotal").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(evidence.at("/cutoverExecutionRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/finalTrafficWeightPercent").asInt()).isZero();
        for (String field : List.of(
                "productionTrafficObservedOnUnified",
                "apiGatewayTrafficZeroProven",
                "rollbackWindowCompleted",
                "persistentAuditSinkConnected",
                "auditWriteSmokePassed",
                "observabilityPlatformConnected",
                "dashboardConnected",
                "alertingConnected",
                "tracePipelineConnected",
                "environmentVariablesRead",
                "sensitiveValuesExposed",
                "readyForProduction",
                "readyToReplaceGateway",
                "readyToRetireOldEntrypoints")) {
            assertThat(evidence.at("/" + field).asBoolean()).as(field).isFalse();
        }
        assertThat(evidence.at("/oldEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/nodeDaemonOutOfRepository").asBoolean()).isTrue();
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("REAL_CUTOVER_RECEIPT_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED");
        assertNoSecrets(readiness);
    }

    @Test
    void controlledCutoverReceiptSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-controlled-cutover-receipt-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-production-controlled-cutover-receipt");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_CONTROLLED_CUTOVER_RECEIPT_SHAPE_NOT_APPLIED");
        assertThat(sample.at("/receiptApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/previousEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/approvalRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/runtimePrerequisiteRefs").toString())
                .contains("docs/unified-backend-production-external-value-intake-sample.json")
                .contains("docs/unified-backend-production-runtime-shell-sample.json")
                .contains("docs/unified-backend-production-audit-observability-smoke-sample.json");
        assertThat(sample.at("/trafficPlan/stages").size()).isGreaterThanOrEqualTo(5);
        assertThat(sample.at("/trafficPlan/stages").toString())
                .contains("\"weightPercent\":0")
                .contains("\"weightPercent\":5")
                .contains("\"weightPercent\":25")
                .contains("\"weightPercent\":50")
                .contains("\"weightPercent\":100");
        assertThat(sample.at("/smokeRefs").size()).isGreaterThanOrEqualTo(14);
        assertThat(sample.at("/auditRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/observabilityRefs").size()).isGreaterThanOrEqualTo(10);
        assertThat(sample.at("/rollbackWindowRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/apiGatewayTrafficRefs").size()).isGreaterThanOrEqualTo(2);
        assertThat(sample.at("/cutoverExecutionRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/oldEntrypointProtection/apiGatewayServicePreserved").asBoolean()).isTrue();
        assertThat(sample.at("/oldEntrypointProtection/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(sample.at("/oldEntrypointProtection/noDeletionInThisRound").asBoolean()).isTrue();
        assertThat(sample.at("/goNoGoImpact/appliedReceiptDoesNotApproveRetirement").asBoolean()).isTrue();

        String safeValueText = sample.toString().toLowerCase()
                .replace(sample.at("/redactionPolicy").toString().toLowerCase(), "");
        assertThat(safeValueText)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("x-gateway-internal-signature")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env")
                .doesNotContain("http://")
                .doesNotContain("https://");
    }

    @Test
    void controlledCutoverReceiptDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-controlled-cutover-redaction"));

        String text = readiness.at("/data/productionControlledCutoverEvidence").toString()
                + readiness.at("/data/productionControlledCutoverChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa")
                .doesNotContain("http://")
                .doesNotContain("https://");
        assertThat(text.toLowerCase()
                .replace("productioncontrolledcutover", "")
                .replace("controlledcutover", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void controlledCutoverKeepsOldEntrypointsProtected() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-controlled-cutover-old-entrypoints"));

        assertThat(readiness.at("/data/productionControlledCutoverEvidence/oldEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(readiness.at("/data/productionControlledCutoverEvidence/readyToRetireOldEntrypoints").asBoolean()).isFalse();
        assertThat(readiness.at("/data/apiGatewayRetirementPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_TRAFFIC_NOT_SWITCHED");
        assertThat(readiness.at("/data/coreEntrypointRetirementPrecheckStatus").asText())
                .isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");
        assertThat(readiness.at("/data/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(List.of(
                Path.of("../unified-backend-service/pom.xml"),
                Path.of("../business-core-service/src/main/java/cn/beiming/core/BusinessCoreModule.java"),
                Path.of("../admission-core-service/src/main/java/cn/beiming/admission/AdmissionCoreModule.java"),
                Path.of("../engagement-core-service/src/main/java/cn/beiming/engagement/EngagementCoreModule.java"),
                Path.of("../ops-core-service/src/main/java/cn/beiming/opscore/OpsCoreModule.java"),
                Path.of("../portal-core-service/src/main/java/cn/beiming/portalcore/PortalCoreModule.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isTrue());
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
        assertThat(Files.exists(Path.of("../node-daemon-service"))).isFalse();
    }

    @Test
    void controlledCutoverCanRepresentAppliedExternalReceiptWithoutApprovingRetirement() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-production-controlled-cutover-receipt-sample.json")));
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-controlled-cutover-applied-receipt-shape"));

        assertThat(sample.at("/goNoGoImpact/appliedReceiptAllowedStatuses").toString())
                .contains("PASS_CONTROLLED_CUTOVER_APPLIED_ROLLBACK_WINDOW_OPEN")
                .contains("PASS_CONTROLLED_CUTOVER_AND_ROLLBACK_WINDOW_COMPLETED");
        assertThat(sample.at("/goNoGoImpact/appliedReceiptDoesNotApproveRetirement").asBoolean()).isTrue();
        assertThat(sample.at("/oldEntrypointProtection/retirementRoundRequired").asText())
                .isEqualTo("FOURTY_THIRD_ROUND_OR_LATER");
        assertThat(readiness.at("/data/productionControlledCutoverEvidence/readyToRetireOldEntrypoints").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canRetireApiGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/replacementDecision/canRetireIndependentCoreEntrypoints").asBoolean()).isFalse();
    }

    @Test
    void exposesApiGatewayControlledRetirementGateWithoutDeletingGateway() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-controlled-retirement"));

        assertThat(readiness.at("/data/apiGatewayControlledRetirementStatus").asText())
                .isEqualTo("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/apiGatewayRetirementPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_TRAFFIC_NOT_SWITCHED");

        for (String check : List.of(
                "API_GATEWAY_RETIREMENT_RECEIPT_SAMPLE_PRESENT",
                "API_GATEWAY_RETIREMENT_RECEIPT_SAMPLE_JSON_PARSABLE",
                "CONTROLLED_CUTOVER_RECEIPT_REFERENCED",
                "GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED",
                "BUSINESS_PATHS_UNCHANGED",
                "CORE_MODULE_SOURCES_PRESERVED",
                "NODE_DAEMON_OUT_OF_REPOSITORY",
                "DELETE_LIST_RECORDED",
                "NO_REAL_VALUES_IN_API_GATEWAY_RETIREMENT_RECEIPT",
                "NO_SENSITIVE_VALUES_IN_API_GATEWAY_RETIREMENT_RECEIPT",
                "READY_FLAGS_REMAIN_FALSE")) {
            assertPrecheck(readiness, "/data/apiGatewayControlledRetirementChecks", check, "PASS", true);
        }
        for (String check : List.of(
                "RETIREMENT_RECEIPT_NOT_PROVIDED",
                "PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED",
                "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN",
                "ROLLBACK_WINDOW_NOT_COMPLETED",
                "REAL_AUDIT_WRITE_SMOKE_NOT_PASSED",
                "DASHBOARD_ALERT_TRACE_NOT_VERIFIED",
                "RETIREMENT_APPROVAL_NOT_GRANTED",
                "DELETE_LIST_NOT_APPROVED",
                "GATEWAY_SELF_API_PARITY_NOT_PROVEN_WITH_REAL_RECEIPT",
                "UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED",
                "CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED",
                "ROLLBACK_PLAN_NOT_REVALIDATED",
                "BULK_DELETE_FORBIDDEN")) {
            assertPrecheck(readiness, "/data/apiGatewayControlledRetirementChecks", check, "BLOCKED", true);
        }

        JsonNode evidence = readiness.at("/data/apiGatewayControlledRetirementEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_API_GATEWAY_RETIREMENT_RECEIPT_GATE_NOT_PRODUCTION");
        assertThat(evidence.at("/receiptPath").asText())
                .isEqualTo("docs/unified-backend-api-gateway-retirement-receipt-sample.json");
        assertThat(evidence.at("/receiptPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/receiptParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/retirementApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/deleteListApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(evidence.at("/retiredEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/rollbackEntrypointRefs").toString())
                .contains("LOCAL_SAMPLE_REF:API_GATEWAY_8125")
                .contains("LOCAL_SAMPLE_REF:BUSINESS_CORE_8130")
                .contains("LOCAL_SAMPLE_REF:PORTAL_CORE_8134");
        assertThat(evidence.at("/controlledCutoverRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/approvalRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/trafficZeroRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/observabilityRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/auditRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/rollbackWindowRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/gatewaySelfApiParityRefsTotal").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(evidence.at("/deleteListItemsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/deletedFilesTotal").asInt()).isZero();
        assertPrecheck(readiness, "/data/apiGatewayControlledRetirementChecks", "UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", "PASS", true);
        assertThat(evidence.at("/remainingGatewayPackageRefs").asInt()).isEqualTo(1);
        assertThat(evidence.at("/unifiedBuildHelperStillReferencesApiGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayServiceDirectoryStillPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        for (String field : List.of(
                "readyToRetireBusinessCore",
                "readyToRetireAdmissionCore",
                "readyToRetireEngagementCore",
                "readyToRetireOpsCore",
                "readyToRetirePortalCore",
                "bulkDeleteAllowed",
                "environmentVariablesRead",
                "sensitiveValuesExposed")) {
            assertThat(evidence.at("/" + field).asBoolean()).as(field).isFalse();
        }
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("REAL_API_GATEWAY_RETIREMENT_RECEIPT_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("DELETE_LIST_APPROVED_BY_USER")
                .contains("GATEWAY_SELF_API_PARITY_NOT_PROVEN_WITH_REAL_RECEIPT")
                .contains("UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED")
                .contains("CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED")
                .contains("ROLLBACK_PLAN_NOT_REVALIDATED")
                .contains("BULK_DELETE_FORBIDDEN");
        assertThat(evidence.at("/remainingBlockers").toString())
                .doesNotContain("GATEWAY_SELF_API_PARITY_PROVEN_WITH_REAL_RECEIPT")
                .doesNotContain("UNIFIED_BACKEND_FULL_REGRESSION_RECORDED")
                .doesNotContain("CORE_ENTRYPOINT_REGRESSION_RECORDED")
                .doesNotContain("ROLLBACK_PLAN_REVALIDATED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED");
        assertNoSecrets(readiness);
    }

    @Test
    void apiGatewayRetirementReceiptSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/unified-backend-api-gateway-retirement-receipt-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-api-gateway-retirement-receipt");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_API_GATEWAY_RETIREMENT_RECEIPT_SHAPE_NOT_APPLIED");
        assertThat(sample.at("/retirementApplied").asBoolean()).isFalse();
        assertThat(sample.at("/deleteListApproved").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/retiredEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/controlledCutoverRefs").toString())
                .contains("docs/unified-backend-production-controlled-cutover-receipt-sample.json");
        assertThat(sample.at("/approvalRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/trafficZeroRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/gatewaySelfApiParityRefs").size()).isGreaterThanOrEqualTo(10);
        assertThat(sample.at("/observabilityRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/auditRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/rollbackWindowRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/deleteList").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/deleteList").toString())
                .contains("backend/api-gateway-service/pom.xml")
                .contains("backend/api-gateway-service/src/main/java/cn/beiming/apigateway/GatewayModule.java");
        assertThat(sample.at("/migrationPlan/unifiedBuildHelperStillReferencesApiGateway").asBoolean()).isFalse();
        assertThat(sample.at("/migrationPlan/unifiedBackendSelfHostsGatewaySource").asBoolean()).isTrue();
        assertThat(sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(sample.at("/coreProtection/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetirePortalCore").asBoolean()).isFalse();

        String safeValueText = sample.toString().toLowerCase()
                .replace(sample.at("/redactionPolicy").toString().toLowerCase(), "");
        assertThat(safeValueText)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("x-gateway-internal-signature")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env")
                .doesNotContain("http://")
                .doesNotContain("https://");
    }

    @Test
    void apiGatewayRetirementDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-retirement-redaction"));

        String text = readiness.at("/data/apiGatewayControlledRetirementEvidence").toString()
                + readiness.at("/data/apiGatewayControlledRetirementChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa")
                .doesNotContain("http://")
                .doesNotContain("https://");
        assertThat(text.toLowerCase()
                .replace("apigatewaycontrolledretirement", "")
                .replace("controlledretirement", "")
                .replace("sensitivevaluesexposed", "")
                .replace("retirement", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void apiGatewayRetirementKeepsCoreEntrypointsProtected() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-retirement-core-protection"));

        JsonNode evidence = readiness.at("/data/apiGatewayControlledRetirementEvidence");
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/bulkDeleteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/coreEntrypointRetirementPrecheckStatus").asText())
                .isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");
        assertThat(List.of(
                Path.of("../business-core-service/src/main/java/cn/beiming/core/BusinessCoreModule.java"),
                Path.of("../admission-core-service/src/main/java/cn/beiming/admission/AdmissionCoreModule.java"),
                Path.of("../engagement-core-service/src/main/java/cn/beiming/engagement/EngagementCoreModule.java"),
                Path.of("../ops-core-service/src/main/java/cn/beiming/opscore/OpsCoreModule.java"),
                Path.of("../portal-core-service/src/main/java/cn/beiming/portalcore/PortalCoreModule.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isTrue());
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
        assertThat(Files.exists(Path.of("../node-daemon-service"))).isFalse();
    }

    @Test
    void unifiedBackendPreservesGatewaySelfApisBeforeApiGatewayRetirement() throws Exception {
        assertThat(performJson(get("/api/v1/gateway/health")
                .header("X-Request-Id", "req-unified-gateway-health")).at("/data/service").asText())
                .isEqualTo("api-gateway");
        assertThat(performJson(get("/api/v1/gateway/admin/ops/summary")
                .header("Authorization", "Bearer owner-token")).at("/data/service").asText())
                .isEqualTo("api-gateway");
        JsonNode topology = performJson(get("/api/v1/gateway/admin/runtime-topology")
                .header("Authorization", "Bearer owner-token"));
        assertThat(topology.at("/data/deploymentMode").asText())
                .isEqualTo("LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED");
        assertThat(topology.at("/data/currentEntrypoints").toString())
                .contains("unified-backend")
                .contains("UNIFIED_GATEWAY_ENTRYPOINT")
                .doesNotContain("backend/api-gateway-service");
        assertThat(performJson(get("/api/v1/gateway/admin/routes?pageSize=100")
                .header("Authorization", "Bearer owner-token")).at("/data/items").size())
                .isGreaterThanOrEqualTo(25);
        assertThat(performJson(get("/api/v1/gateway/admin/upstreams?pageSize=100")
                .header("Authorization", "Bearer owner-token")).at("/data/items").size())
                .isGreaterThanOrEqualTo(25);
        assertThat(performJson(get("/api/v1/gateway/admin/request-logs")
                .header("Authorization", "Bearer owner-token")).at("/data/items").isArray()).isTrue();
    }

    @Test
    void unifiedBackendSelfHostsGatewaySourceBeforeApiGatewayRetirement() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-retirement-build-helper"));
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom).doesNotContain("../api-gateway-service/src/main/java");
        assertThat(readiness.at("/data/apiGatewayControlledRetirementStatus").asText())
                .isEqualTo("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED");
        assertPrecheck(readiness, "/data/apiGatewayControlledRetirementChecks", "UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", "PASS", true);
        assertThat(readiness.at("/data/apiGatewayControlledRetirementEvidence/unifiedBuildHelperStillReferencesApiGateway").asBoolean()).isFalse();
        assertThat(readiness.at("/data/apiGatewayControlledRetirementEvidence/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(readiness.at("/data/apiGatewayControlledRetirementEvidence/apiGatewayServiceDirectoryStillPresent").asBoolean()).isTrue();
        assertThat(readiness.at("/data/apiGatewayControlledRetirementEvidence/deletedFilesTotal").asInt()).isZero();
    }

    @Test
    void apiGatewayRetirementGateIsDocumentedAcrossOperationalHandbooks() throws Exception {
        Map<String, String> docs = Map.of(
                "contracts-overview", Files.readString(Path.of("../../docs/contracts-overview.md")),
                "api-reference", Files.readString(Path.of("../../docs/api-reference.md")),
                "frontend-api-handbook", Files.readString(Path.of("../../docs/frontend-api-handbook.md")),
                "frontend-development-guide", Files.readString(Path.of("../../docs/frontend-development-guide.md")),
                "system-design", Files.readString(Path.of("../../docs/system-design.md")),
                "development-governance", Files.readString(Path.of("../../docs/development-governance.md"))
        );

        docs.forEach((name, text) -> assertThat(text)
                .as(name)
                .contains("apiGatewayControlledRetirementStatus")
                .contains("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED")
                .contains("docs/unified-backend-api-gateway-retirement-receipt-sample.json")
                .contains("api-gateway-service")
                .contains("unified-backend-service:8135")
                .contains("readyToRetireBusinessCore=false")
                .contains("readyToRetireAdmissionCore=false")
                .contains("readyToRetireEngagementCore=false")
                .contains("readyToRetireOpsCore=false")
                .contains("readyToRetirePortalCore=false"));

        assertThat(docs.get("frontend-api-handbook"))
                .contains("VITE_API_BASE_URL")
                .contains("/api/v1/**")
                .contains("http://127.0.0.1:8135")
                .contains("http://127.0.0.1:8125");
        assertThat(docs.get("frontend-development-guide"))
                .contains("VITE_API_BASE_URL")
                .contains("/api/v1/auth/login")
                .contains("apiGatewayControlledRetirementStatus");
        assertThat(docs.get("development-governance"))
                .contains("不得批量删除")
                .contains("没有真实外部退役收据");
    }

    @Test
    void exposesApiGatewayExternalRetirementEvidenceGateWithoutDeletingGateway() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-external-retirement-evidence"));

        assertThat(readiness.at("/data/apiGatewayExternalRetirementEvidenceStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED");
        assertThat(readiness.at("/data/apiGatewayControlledRetirementStatus").asText())
                .isEqualTo("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED");
        assertThat(readiness.at("/data/productionControlledCutoverStatus").asText())
                .isEqualTo("BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        for (String check : List.of(
                "EXTERNAL_RETIREMENT_EVIDENCE_SAMPLE_PRESENT",
                "EXTERNAL_RETIREMENT_EVIDENCE_SAMPLE_JSON_PARSABLE",
                "CONTROLLED_CUTOVER_RECEIPT_REFERENCED",
                "API_GATEWAY_RETIREMENT_RECEIPT_REFERENCED",
                "UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE",
                "GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED",
                "BUSINESS_PATHS_UNCHANGED",
                "CORE_MODULE_SOURCES_PRESERVED",
                "NODE_DAEMON_OUT_OF_REPOSITORY",
                "NO_REAL_VALUES_IN_EXTERNAL_RETIREMENT_EVIDENCE",
                "NO_SENSITIVE_VALUES_IN_EXTERNAL_RETIREMENT_EVIDENCE",
                "BULK_DELETE_FORBIDDEN",
                "READY_FLAGS_REMAIN_FALSE")) {
            assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", check, "PASS", true);
        }
        for (String check : List.of(
                "REAL_EXTERNAL_RETIREMENT_EVIDENCE_NOT_PROVIDED",
                "REAL_ENTRYPOINT_NOT_APPLIED_TO_UNIFIED_BACKEND",
                "PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED",
                "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN",
                "REAL_AUDIT_WRITE_SMOKE_NOT_PASSED",
                "DASHBOARD_NOT_VERIFIED",
                "ALERTING_NOT_VERIFIED",
                "TRACE_PIPELINE_NOT_VERIFIED",
                "ROLLBACK_WINDOW_NOT_COMPLETED",
                "RETIREMENT_APPROVAL_NOT_GRANTED",
                "DELETE_LIST_NOT_APPROVED",
                "UNIFIED_BACKEND_FULL_REGRESSION_NOT_RECORDED",
                "API_GATEWAY_REGRESSION_NOT_RECORDED",
                "CORE_ENTRYPOINT_REGRESSION_NOT_RECORDED",
                "ROLLBACK_PLAN_NOT_REVALIDATED")) {
            assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", check, "BLOCKED", true);
        }

        JsonNode evidence = readiness.at("/data/apiGatewayExternalRetirementEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_API_GATEWAY_EXTERNAL_RETIREMENT_EVIDENCE_GATE_NOT_PRODUCTION");
        assertThat(evidence.at("/evidencePath").asText())
                .isEqualTo("docs/unified-backend-api-gateway-external-retirement-evidence-sample.json");
        assertThat(evidence.at("/evidencePresent").asBoolean()).isTrue();
        assertThat(evidence.at("/evidenceParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/externalEvidenceApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/deleteListApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(evidence.at("/retiredEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(evidence.at("/rollbackEntrypointRefs").toString())
                .contains("LOCAL_SAMPLE_REF:API_GATEWAY_8125")
                .contains("LOCAL_SAMPLE_REF:BUSINESS_CORE_8130")
                .contains("LOCAL_SAMPLE_REF:PORTAL_CORE_8134");
        assertThat(evidence.at("/controlledCutoverRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/apiGatewayRetirementRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/approvalRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/trafficObservationRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/trafficZeroRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/auditRefsTotal").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(evidence.at("/observabilityRefsTotal").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(evidence.at("/rollbackWindowRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/deleteListItemsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/deletedFilesTotal").asInt()).isZero();
        assertThat(evidence.at("/unifiedBuildHelperStillReferencesApiGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayServiceDirectoryStillPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        for (String field : List.of(
                "readyToRetireBusinessCore",
                "readyToRetireAdmissionCore",
                "readyToRetireEngagementCore",
                "readyToRetireOpsCore",
                "readyToRetirePortalCore",
                "bulkDeleteAllowed",
                "environmentVariablesRead",
                "sensitiveValuesExposed")) {
            assertThat(evidence.at("/" + field).asBoolean()).as(field).isFalse();
        }
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("REAL_EXTERNAL_RETIREMENT_EVIDENCE_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("REAL_ENTRYPOINT_APPLIED_TO_UNIFIED_BACKEND")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("REAL_DASHBOARD_VERIFIED")
                .contains("REAL_ALERTING_VERIFIED")
                .contains("REAL_TRACE_PIPELINE_VERIFIED")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("RETIREMENT_APPROVAL_GRANTED")
                .contains("DELETE_LIST_APPROVED_BY_USER");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED");
        assertNoSecrets(readiness);
    }

    @Test
    void apiGatewayExternalRetirementEvidenceSampleFileIsParseableAndSafe() throws Exception {
        Path samplePath = Path.of("../../docs/unified-backend-api-gateway-external-retirement-evidence-sample.json");
        assertThat(Files.exists(samplePath)).isTrue();
        JsonNode sample = objectMapper.readTree(Files.readString(samplePath));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-api-gateway-external-retirement-evidence");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_API_GATEWAY_EXTERNAL_RETIREMENT_EVIDENCE_SHAPE_NOT_APPLIED");
        assertThat(sample.at("/externalEvidenceApplied").asBoolean()).isFalse();
        assertThat(sample.at("/deleteListApproved").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:UNIFIED_BACKEND_8135");
        assertThat(sample.at("/retiredEntrypointRef").asText()).isEqualTo("LOCAL_SAMPLE_REF:API_GATEWAY_8125");
        assertThat(sample.at("/rollbackEntrypointRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/controlledCutoverRefs").toString())
                .contains("docs/unified-backend-production-controlled-cutover-receipt-sample.json");
        assertThat(sample.at("/apiGatewayRetirementReceiptRefs").toString())
                .contains("docs/unified-backend-api-gateway-retirement-receipt-sample.json");
        assertThat(sample.at("/approvalRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/trafficObservationRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/trafficZeroRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/auditWriteSmokeRefs").size()).isGreaterThanOrEqualTo(3);
        assertThat(sample.at("/observabilityRefs").size()).isGreaterThanOrEqualTo(3);
        assertThat(sample.at("/rollbackWindowRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/deleteApproval/approved").asBoolean()).isFalse();
        assertThat(sample.at("/deleteApproval/bulkDeleteAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/deleteList").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/deleteList").toString())
                .contains("backend/api-gateway-service/pom.xml")
                .contains("backend/api-gateway-service/src/main/java/cn/beiming/apigateway/GatewayModule.java");
        assertThat(sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(sample.at("/coreProtection/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetirePortalCore").asBoolean()).isFalse();

        String safeValueText = sample.toString().toLowerCase()
                .replace(sample.at("/redactionPolicy").toString().toLowerCase(), "");
        assertThat(safeValueText)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("x-gateway-internal-signature")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env")
                .doesNotContain("http://")
                .doesNotContain("https://");
    }

    @Test
    void apiGatewayExternalRetirementEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-external-retirement-redaction"));

        String text = readiness.at("/data/apiGatewayExternalRetirementEvidence").toString()
                + readiness.at("/data/apiGatewayExternalRetirementEvidenceChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa")
                .doesNotContain("http://")
                .doesNotContain("https://");
        assertThat(text.toLowerCase()
                .replace("apigatewayexternalretirementevidence", "")
                .replace("externalretirementevidence", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void apiGatewayExternalRetirementEvidenceBlocksDeletionWithoutUserApproval() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-external-retirement-delete-approval"));

        JsonNode evidence = readiness.at("/data/apiGatewayExternalRetirementEvidence");
        assertThat(evidence.at("/externalEvidenceApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/deleteListApproved").asBoolean()).isFalse();
        assertThat(evidence.at("/deletedFilesTotal").asInt()).isZero();
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayServiceDirectoryStillPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/bulkDeleteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("DELETE_LIST_APPROVED_BY_USER")
                .contains("RETIREMENT_APPROVAL_GRANTED");
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
    }

    @Test
    void apiGatewayExternalRetirementEvidenceKeepsCoreEntrypointsProtected() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-external-retirement-core-protection"));

        JsonNode evidence = readiness.at("/data/apiGatewayExternalRetirementEvidence");
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/coreEntrypointRetirementPrecheckStatus").asText())
                .isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");
        assertThat(List.of(
                Path.of("../business-core-service/src/main/java/cn/beiming/core/BusinessCoreModule.java"),
                Path.of("../admission-core-service/src/main/java/cn/beiming/admission/AdmissionCoreModule.java"),
                Path.of("../engagement-core-service/src/main/java/cn/beiming/engagement/EngagementCoreModule.java"),
                Path.of("../ops-core-service/src/main/java/cn/beiming/opscore/OpsCoreModule.java"),
                Path.of("../portal-core-service/src/main/java/cn/beiming/portalcore/PortalCoreModule.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isTrue());
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
        assertThat(Files.exists(Path.of("../node-daemon-service"))).isFalse();
    }

    @Test
    void apiGatewayExternalRetirementEvidenceRequiresRealTrafficZeroAndRollbackWindow() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-api-gateway-external-retirement-real-blockers"));

        assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", "REAL_ENTRYPOINT_NOT_APPLIED_TO_UNIFIED_BACKEND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", "PRODUCTION_TRAFFIC_NOT_OBSERVED_ON_UNIFIED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", "API_GATEWAY_TRAFFIC_ZERO_NOT_PROVEN", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", "REAL_AUDIT_WRITE_SMOKE_NOT_PASSED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/apiGatewayExternalRetirementEvidenceChecks", "ROLLBACK_WINDOW_NOT_COMPLETED", "BLOCKED", true);
        assertThat(readiness.at("/data/apiGatewayExternalRetirementEvidence/remainingBlockers").toString())
                .contains("REAL_ENTRYPOINT_APPLIED_TO_UNIFIED_BACKEND")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("ROLLBACK_WINDOW_COMPLETED");
    }

    @Test
    void exposesRealProductionEntrypointCutoverEvidenceGateWithoutRetiringApiGateway() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-real-production-entrypoint-cutover-evidence"));

        assertThat(readiness.at("/data/realProductionEntrypointCutoverStatus").asText())
                .isEqualTo("BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED");
        assertThat(readiness.at("/data/apiGatewayExternalRetirementEvidenceStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED");
        assertThat(readiness.at("/data/apiGatewayControlledRetirementStatus").asText())
                .isEqualTo("BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        for (String check : List.of(
                "REAL_PRODUCTION_CUTOVER_EVIDENCE_SAMPLE_PRESENT",
                "REAL_PRODUCTION_CUTOVER_EVIDENCE_SAMPLE_JSON_PARSABLE",
                "CANDIDATE_ENTRYPOINT_TARGETS_UNIFIED_BACKEND_8135",
                "PREVIOUS_ENTRYPOINT_TARGETS_API_GATEWAY_8125",
                "UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE",
                "GATEWAY_SELF_APIS_PRESERVED_IN_UNIFIED",
                "CORE_MODULE_SOURCES_PRESERVED",
                "NODE_DAEMON_OUT_OF_REPOSITORY",
                "SINGLE_MAVEN_ENTRYPOINT_PRESENT",
                "NO_REAL_VALUES_IN_CUTOVER_EVIDENCE",
                "NO_SENSITIVE_VALUES_IN_CUTOVER_EVIDENCE",
                "OLD_API_GATEWAY_RETIREMENT_STILL_FORBIDDEN",
                "READY_FLAGS_REMAIN_FALSE")) {
            assertPrecheck(readiness, "/data/realProductionEntrypointCutoverChecks", check, "PASS", true);
        }
        for (String check : List.of(
                "REAL_PRODUCTION_CUTOVER_EVIDENCE_NOT_PROVIDED",
                "PRODUCTION_TRAFFIC_ALLOWED_NOT_DECLARED",
                "CUTOVER_WINDOW_REF_NOT_PROVIDED",
                "PRODUCTION_TRAFFIC_OBSERVATION_NOT_PROVIDED",
                "OLD_GATEWAY_TRAFFIC_ZERO_NOT_PROVIDED",
                "REAL_AUDIT_WRITE_SMOKE_NOT_PROVIDED",
                "DASHBOARD_REF_NOT_PROVIDED",
                "ALERT_REF_NOT_PROVIDED",
                "TRACE_REF_NOT_PROVIDED",
                "ROLLBACK_REF_NOT_PROVIDED",
                "APPROVAL_REF_NOT_PROVIDED")) {
            assertPrecheck(readiness, "/data/realProductionEntrypointCutoverChecks", check, "BLOCKED", true);
        }

        JsonNode evidence = readiness.at("/data/realProductionEntrypointCutoverEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_GATE_NOT_PRODUCTION");
        assertThat(evidence.at("/evidencePath").asText())
                .isEqualTo("docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json");
        assertThat(evidence.at("/evidencePresent").asBoolean()).isTrue();
        assertThat(evidence.at("/evidenceParsed").asBoolean()).isTrue();
        assertThat(evidence.at("/realProductionCutoverEvidenceApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/oldApiGatewayRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(evidence.at("/candidateEntrypointRef").asText())
                .isEqualTo("EXTERNAL_REF_REQUIRED:unified-backend-service-8135");
        assertThat(evidence.at("/previousEntrypointRef").asText())
                .isEqualTo("EXTERNAL_REF_REQUIRED:api-gateway-service-8125");
        assertThat(evidence.at("/trafficObservationRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/oldGatewayTrafficZeroRefsTotal").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(evidence.at("/auditWriteSmokeRefsTotal").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(evidence.at("/dashboardRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/alertRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/traceRefsTotal").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(evidence.at("/rollbackRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/approvalRefsTotal").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(evidence.at("/unifiedBuildHelperStillReferencesApiGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/mavenEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/deleteListPermitGenerated").asBoolean()).isFalse();
        for (String field : List.of(
                "readyToRetireBusinessCore",
                "readyToRetireAdmissionCore",
                "readyToRetireEngagementCore",
                "readyToRetireOpsCore",
                "readyToRetirePortalCore",
                "environmentVariablesRead",
                "sensitiveValuesExposed")) {
            assertThat(evidence.at("/" + field).asBoolean()).as(field).isFalse();
        }
        assertThat(evidence.at("/remainingBlockers").toString())
                .contains("REAL_PRODUCTION_CUTOVER_EVIDENCE_PROVIDED_OUTSIDE_REPOSITORY")
                .contains("PRODUCTION_TRAFFIC_OBSERVED_ON_UNIFIED")
                .contains("OLD_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("REAL_AUDIT_WRITE_SMOKE_PASSED")
                .contains("REAL_DASHBOARD_VERIFIED")
                .contains("REAL_ALERTING_VERIFIED")
                .contains("REAL_TRACE_PIPELINE_VERIFIED")
                .contains("ROLLBACK_PLAN_PROVIDED")
                .contains("PRODUCTION_ENTRYPOINT_OWNER_APPROVAL_GRANTED");
        assertThat(evidence.at("/status").asText())
                .isEqualTo("BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED");
        assertNoSecrets(readiness);
    }

    @Test
    void realProductionEntrypointCutoverEvidenceSampleFileIsParseableAndSafe() throws Exception {
        Path samplePath = Path.of("../../docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json");
        assertThat(Files.exists(samplePath)).isTrue();
        JsonNode sample = objectMapper.readTree(Files.readString(samplePath));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-real-production-entrypoint-cutover-evidence");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_SHAPE_NOT_APPLIED");
        assertThat(sample.at("/realProductionCutoverEvidenceApplied").asBoolean()).isFalse();
        assertThat(sample.at("/productionTrafficAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/oldApiGatewayRetirementAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/realValuesAllowedInRepository").asBoolean()).isFalse();
        assertThat(sample.at("/candidateEntrypointRef").asText()).isEqualTo("EXTERNAL_REF_REQUIRED:unified-backend-service-8135");
        assertThat(sample.at("/previousEntrypointRef").asText()).isEqualTo("EXTERNAL_REF_REQUIRED:api-gateway-service-8125");
        assertThat(sample.at("/trafficObservationRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/oldGatewayTrafficZeroRefs").size()).isGreaterThanOrEqualTo(6);
        assertThat(sample.at("/auditWriteSmokeRefs").size()).isGreaterThanOrEqualTo(3);
        assertThat(sample.at("/observabilityRefs/dashboardRefs").size()).isGreaterThanOrEqualTo(1);
        assertThat(sample.at("/observabilityRefs/alertRefs").size()).isGreaterThanOrEqualTo(1);
        assertThat(sample.at("/observabilityRefs/traceRefs").size()).isGreaterThanOrEqualTo(1);
        assertThat(sample.at("/rollbackRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/approvalRefs").size()).isGreaterThanOrEqualTo(4);
        assertThat(sample.at("/mavenEntrypoints").size()).isEqualTo(1);
        assertThat(sample.at("/coreProtection/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(sample.at("/goNoGoImpact/deleteListPermitGenerated").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(sample.at("/coreProtection/readyToRetirePortalCore").asBoolean()).isFalse();

        String safeValueText = sample.toString().toLowerCase()
                .replace(sample.at("/redactionPolicy").toString().toLowerCase(), "")
                .replace(sample.at("/verificationCommands").toString().toLowerCase(), "")
                .replace(sample.at("/notes").toString().toLowerCase(), "");
        assertThat(safeValueText)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("x-gateway-internal-signature")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("id_rsa")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("akia")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("powershell")
                .doesNotContain("cmd.exe")
                .doesNotContain("ssh ")
                .doesNotContain("scp ")
                .doesNotContain("c:\\users\\")
                .doesNotContain(".env")
                .doesNotContain("http://")
                .doesNotContain("https://");
    }

    @Test
    void realProductionEntrypointCutoverEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-real-production-entrypoint-cutover-redaction"));

        String text = readiness.at("/data/realProductionEntrypointCutoverEvidence").toString()
                + readiness.at("/data/realProductionEntrypointCutoverChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("mongodb://")
                .doesNotContain("redis://")
                .doesNotContain("AKIA")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa")
                .doesNotContain("http://")
                .doesNotContain("https://");
        assertThat(text.toLowerCase()
                .replace("realproductionentrypointcutoverevidence", "")
                .replace("realproductioncutoverevidence", "")
                .replace("sensitivevaluesexposed", ""))
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("passwd")
                .doesNotContain("pwd")
                .doesNotContain("privatekey")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void realProductionEntrypointCutoverEvidenceKeepsApiGatewayAndCoresProtected() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-real-production-entrypoint-cutover-core-protection"));

        JsonNode evidence = readiness.at("/data/realProductionEntrypointCutoverEvidence");
        assertThat(evidence.at("/oldApiGatewayRetirementAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/deleteListPermitGenerated").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(readiness.at("/data/apiGatewayRetirementPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_TRAFFIC_NOT_SWITCHED");
        assertThat(readiness.at("/data/coreEntrypointRetirementPrecheckStatus").asText())
                .isEqualTo("PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED");
        assertThat(List.of(
                Path.of("../business-core-service/src/main/java/cn/beiming/core/BusinessCoreModule.java"),
                Path.of("../admission-core-service/src/main/java/cn/beiming/admission/AdmissionCoreModule.java"),
                Path.of("../engagement-core-service/src/main/java/cn/beiming/engagement/EngagementCoreModule.java"),
                Path.of("../ops-core-service/src/main/java/cn/beiming/opscore/OpsCoreModule.java"),
                Path.of("../portal-core-service/src/main/java/cn/beiming/portalcore/PortalCoreModule.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isTrue());
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
        assertThat(Files.exists(Path.of("../node-daemon-service"))).isFalse();
    }

    @Test
    void realProductionEntrypointCutoverGateIsDocumentedAcrossOperationalHandbooks() throws Exception {
        Map<String, String> docs = Map.of(
                "contracts-overview", Files.readString(Path.of("../../docs/contracts-overview.md")),
                "api-reference", Files.readString(Path.of("../../docs/api-reference.md")),
                "frontend-api-handbook", Files.readString(Path.of("../../docs/frontend-api-handbook.md")),
                "frontend-development-guide", Files.readString(Path.of("../../docs/frontend-development-guide.md")),
                "system-design", Files.readString(Path.of("../../docs/system-design.md")),
                "development-governance", Files.readString(Path.of("../../docs/development-governance.md"))
        );

        docs.forEach((name, text) -> assertThat(text)
                .as(name)
                .contains("realProductionEntrypointCutoverStatus")
                .contains("BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED")
                .contains("docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json")
                .contains("oldApiGatewayRetirementAllowed")
                .contains("api-gateway-service")
                .contains("unified-backend-service:8135")
                .contains("readyToRetireBusinessCore=false")
                .contains("readyToRetireAdmissionCore=false")
                .contains("readyToRetireEngagementCore=false")
                .contains("readyToRetireOpsCore=false")
                .contains("readyToRetirePortalCore=false"));

        assertThat(docs.get("frontend-api-handbook"))
                .contains("VITE_API_BASE_URL")
                .contains("/api/v1/**")
                .contains("http://127.0.0.1:8135")
                .contains("http://127.0.0.1:8125");
        assertThat(docs.get("frontend-development-guide"))
                .contains("VITE_API_BASE_URL")
                .contains("/api/v1/auth/login")
                .contains("realProductionEntrypointCutoverStatus");
        assertThat(docs.get("development-governance"))
                .contains("本轮五个 core 独立 Maven 入口也已退役")
                .contains("不得删除目录")
                .contains("不得删除模块源码");
    }

    @Test
    void apiGatewayExternalRetirementEvidenceGateIsDocumentedAcrossOperationalHandbooks() throws Exception {
        Map<String, String> docs = Map.of(
                "contracts-overview", Files.readString(Path.of("../../docs/contracts-overview.md")),
                "api-reference", Files.readString(Path.of("../../docs/api-reference.md")),
                "frontend-api-handbook", Files.readString(Path.of("../../docs/frontend-api-handbook.md")),
                "frontend-development-guide", Files.readString(Path.of("../../docs/frontend-development-guide.md")),
                "system-design", Files.readString(Path.of("../../docs/system-design.md")),
                "development-governance", Files.readString(Path.of("../../docs/development-governance.md"))
        );

        docs.forEach((name, text) -> assertThat(text)
                .as(name)
                .contains("apiGatewayExternalRetirementEvidenceStatus")
                .contains("BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED")
                .contains("docs/unified-backend-api-gateway-external-retirement-evidence-sample.json")
                .contains("api-gateway-service")
                .contains("unified-backend-service:8135")
                .contains("readyToRetireBusinessCore=false")
                .contains("readyToRetireAdmissionCore=false")
                .contains("readyToRetireEngagementCore=false")
                .contains("readyToRetireOpsCore=false")
                .contains("readyToRetirePortalCore=false"));
    }

    @Test
    void localApiGatewayRetirementPrecheckDocumentsRetiredEntrypointAfterDelete() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-local-api-gateway-retirement-precheck"));

        assertThat(readiness.at("/data/localApiGatewayEntrypointRetirementStatus").asText())
                .isEqualTo("PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED");
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "SINGLE_MAVEN_ENTRYPOINT_AFTER_LOCAL_RETIREMENT", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "API_GATEWAY_POM_PRESENT_BEFORE_DELETE", "INFO", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "UNIFIED_BACKEND_SELF_HOSTS_GATEWAY_SOURCE", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "UNIFIED_BUILD_HELPER_DOES_NOT_REFERENCE_API_GATEWAY", "PASS", true);

        JsonNode evidence = readiness.at("/data/localApiGatewayEntrypointRetirementEvidence");
        assertThat(evidence.at("/mode").asText()).isEqualTo("LOCAL_DEVELOPMENT_API_GATEWAY_ENTRYPOINT_RETIREMENT");
        assertThat(evidence.at("/preDeleteMavenEntrypointsTotal").asInt()).isEqualTo(7);
        assertThat(evidence.at("/postDeleteExpectedMavenEntrypointsTotal").asInt()).isEqualTo(1);
        assertThat(evidence.at("/apiGatewayPomStillPresent").asBoolean()).isFalse();
        assertThat(evidence.at("/localRetirementApplied").asBoolean()).isTrue();
        assertThat(evidence.at("/remainingBlockers")).isEmpty();
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementRejectsUnsafeDeleteList() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));

        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "DELETE_LIST_ONLY_EXPLICIT_FILES", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "DELETE_LIST_REJECTS_DIRECTORIES", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "DELETE_LIST_REJECTS_WILDCARDS", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "DELETE_LIST_REJECTS_BULK_DELETE_COMMANDS", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "DELETE_LIST_EXCLUDES_CORE_ENTRYPOINTS", "PASS", true);

        JsonNode evidence = readiness.at("/data/localApiGatewayEntrypointRetirementEvidence");
        assertThat(evidence.at("/deleteListItemsTotal").asInt()).isEqualTo(8);
        assertThat(evidence.at("/unsafeDeleteListItemsTotal").asInt()).isZero();
        assertThat(evidence.at("/bulkDeleteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/deleteList").toString())
                .contains("backend/api-gateway-service/pom.xml")
                .doesNotContain("backend/business-core-service")
                .doesNotContain("*")
                .doesNotContain("Remove-Item -Recurse")
                .doesNotContain("rmdir /s")
                .doesNotContain("rm -rf");
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementKeepsCoreEntrypointsProtected() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));

        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "FIVE_CORE_MODULE_SOURCES_PRESERVED", "PASS", true);
        JsonNode evidence = readiness.at("/data/localApiGatewayEntrypointRetirementEvidence");
        assertThat(evidence.at("/coreEntrypointsPreserved").asBoolean()).isTrue();
        assertThat(evidence.at("/readyToRetireBusinessCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireAdmissionCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireEngagementCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetireOpsCore").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToRetirePortalCore").asBoolean()).isFalse();
        assertThat(evidence.at("/nextRetirementEntrypoint").asText()).isEqualTo("business-core-service");
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementPreservesUnifiedGatewaySelfApis() throws Exception {
        mvc.perform(get("/api/v1/gateway/health").header("X-Request-Id", "req-local-gateway-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("api-gateway"));

        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "UNIFIED_GATEWAY_SELF_APIS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "UNIFIED_MOUNTS_25_BUSINESS_ROUTES_IN_PROCESS", "PASS", true);
        assertThat(readiness.at("/data/localApiGatewayEntrypointRetirementEvidence/unifiedGatewaySelfApisPreserved").asBoolean()).isTrue();
        assertThat(readiness.at("/data/localApiGatewayEntrypointRetirementEvidence/inProcessRoutesTotal").asInt()).isEqualTo(25);
        assertThat(readiness.at("/data/localApiGatewayEntrypointRetirementEvidence/httpFallbackRoutesTotal").asInt()).isZero();
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementDocumentsSingleEntrypointPostDeleteState() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));

        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "POST_RETIREMENT_SINGLE_MAVEN_ENTRYPOINT_EXPECTED", "PASS", true);
        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "POST_DELETE_API_GATEWAY_POM_ABSENT_EXPECTED", "PASS", true);
        JsonNode evidence = readiness.at("/data/localApiGatewayEntrypointRetirementEvidence");
        assertThat(evidence.at("/postDeleteExpectedEntrypoints").toString())
                .contains("unified-backend-service")
                .doesNotContain("business-core-service", "admission-core-service",
                        "engagement-core-service", "ops-core-service", "portal-core-service")
                .doesNotContain("api-gateway-service");
        assertThat(evidence.at("/productionCutoverRequired").asBoolean()).isFalse();
        assertThat(evidence.at("/productionRetirementClaimed").asBoolean()).isFalse();
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementDoesNotRequireProductionProxyEvidence() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token"));

        assertPrecheck(readiness, "/data/localApiGatewayEntrypointRetirementChecks", "PRODUCTION_PROXY_EVIDENCE_NOT_REQUIRED_FOR_LOCAL_RETIREMENT", "PASS", true);
        JsonNode evidence = readiness.at("/data/localApiGatewayEntrypointRetirementEvidence");
        assertThat(evidence.at("/requiresNginx").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresCloudflare").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresRealDomain").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresProductionTrafficEvidence").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertNoSecrets(readiness);
    }

    @Test
    void localApiGatewayRetirementGateIsDocumentedAcrossOperationalHandbooks() throws Exception {
        String contracts = Files.readString(Path.of("../../docs/contracts-unified-backend.md"));
        String apiGatewayContract = Files.readString(Path.of("../../docs/contracts-api-gateway.md"));
        String overview = Files.readString(Path.of("../../docs/contracts-overview.md"));
        String systemDesign = Files.readString(Path.of("../../docs/system-design.md"));
        String governance = Files.readString(Path.of("../../docs/development-governance.md"));
        String frontendHandbook = Files.readString(Path.of("../../docs/frontend-api-handbook.md"));
        String frontendGuide = Files.readString(Path.of("../../docs/frontend-development-guide.md"));

        assertThat(contracts)
                .contains("localApiGatewayEntrypointRetirementStatus")
                .contains("BLOCKED_BY_LOCAL_API_GATEWAY_ENTRYPOINT_STILL_PRESENT")
                .contains("PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED");
        assertThat(apiGatewayContract)
                .contains("历史网关行为契约")
                .contains("unified-backend-service:8135");
        assertThat(overview + systemDesign + governance + frontendHandbook + frontendGuide)
                .contains("唯一后端 Maven 启动入口")
                .contains("business-core-service");
    }

    @Test
    void exposesProductionAuditSinkPreflightWithoutConnectingSink() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-audit-sink-preflight"));

        assertThat(readiness.at("/data/productionAuditSinkPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED");
        assertThat(readiness.at("/data/productionCentralConfigPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED");
        assertThat(readiness.at("/data/externalEntrypointCutoverPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_EVENT_SCHEMA_FIXED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_REQUEST_ID_PROPAGATED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_ACTOR_FIELDS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_TARGET_FIELDS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_RESULT_FIELDS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_REDACTION_RULES_ENFORCED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_FAILURE_DEGRADATION_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_REPLAY_SCOPE_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_RETENTION_POLICY_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_ROLLBACK_SOURCE_DEFINED", "PASS", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "PERSISTENT_AUDIT_SINK_CONFIGURED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_WRITE_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_WRITE_SMOKE_PASSED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_REPLAY_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_RETENTION_JOB_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "AUDIT_EXPORT_PATH_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "EXTERNAL_ENTRYPOINT_TARGETS_UNIFIED_BACKEND", "BLOCKED", true);
        assertPrecheck(readiness, "/data/productionAuditSinkPrecheckChecks", "PRODUCTION_AUDIT_TRAFFIC_OBSERVED", "BLOCKED", true);

        JsonNode evidence = readiness.at("/data/productionAuditSinkEvidence");
        assertThat(evidence.at("/readinessMode").asText()).isEqualTo("PRODUCTION_AUDIT_SINK_CONTRACT_RECORDED_NOT_CONNECTED");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("unified-backend:8135");
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("api-gateway:8125");
        assertThat(evidence.at("/auditSinkConfigured").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWritePathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditWriteSmokePassed").asBoolean()).isFalse();
        assertThat(evidence.at("/auditReplayPathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditRetentionJobConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditExportPathConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/auditEventSchemaFixed").asBoolean()).isTrue();
        assertThat(evidence.at("/requestIdPropagated").asBoolean()).isTrue();
        assertThat(evidence.at("/actorFieldsDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/targetFieldsDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/resultFieldsDocumented").asBoolean()).isTrue();
        assertThat(evidence.at("/failureDegradationDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/redactionRulesEnforced").asBoolean()).isTrue();
        assertThat(evidence.at("/replayScopeDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/retentionPolicyDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/rollbackSourceDefined").asBoolean()).isTrue();
        assertThat(evidence.at("/centralConfigProviderConnected").asBoolean()).isFalse();
        assertThat(evidence.at("/externalEntrypointTargetsUnifiedBackend").asBoolean()).isFalse();
        assertThat(evidence.at("/productionAuditTrafficObserved").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/environmentVariablesRead").asBoolean()).isFalse();
        assertThat(evidence.at("/trafficSwitchApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED");
        assertNoSecrets(readiness);
    }

    @Test
    void productionAuditSinkEvidenceDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-production-audit-sink-redaction"));

        assertThat(readiness.at("/data/productionAuditSinkPrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED");
        String text = readiness.at("/data/productionAuditSinkEvidence").toString()
                + readiness.at("/data/productionAuditSinkPrecheckChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("://")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void exposesExternalEntrypointConfigSampleWithoutApplyingTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-cutover-sample"));

        assertThat(readiness.at("/data/externalEntrypointConfigSamplePrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "CUTOVER_SAMPLE_PRESENT", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "CUTOVER_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "CANDIDATE_ENTRYPOINT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "CURRENT_ENTRYPOINT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "ROLLBACK_ENTRYPOINT_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "SMOKE_TARGETS_RECORDED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "NO_SENSITIVE_VALUES_IN_SAMPLE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "PRODUCTION_SWITCH_DEFAULT_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointConfigSamplePrecheckChecks", "API_GATEWAY_ROLLBACK_PROTECTED", "PASS", true);

        JsonNode evidence = readiness.at("/data/externalEntrypointConfigSampleEvidence");
        assertThat(evidence.at("/sampleConfigPath").asText()).isEqualTo("docs/deployment-entrypoint-cutover-sample.json");
        assertThat(evidence.at("/sampleConfigPresent").asBoolean()).isTrue();
        assertThat(evidence.at("/sampleConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/applyProductionTraffic").asBoolean()).isFalse();
        assertThat(evidence.at("/requiresUserApprovalBeforeApply").asBoolean()).isTrue();
        assertThat(evidence.at("/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/smokeTargetsTotal").asInt()).isEqualTo(32);
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED");
        assertNoSecrets(readiness);
    }

    @Test
    void externalEntrypointConfigSampleDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-cutover-sample-redaction"));

        assertThat(readiness.at("/data/externalEntrypointConfigSamplePrecheckStatus").asText())
                .isEqualTo("BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED");
        String text = readiness.at("/data/externalEntrypointConfigSampleEvidence").toString()
                + readiness.at("/data/externalEntrypointConfigSamplePrecheckChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void externalEntrypointCutoverSampleFileIsParseableAndSafe() throws Exception {
        JsonNode sample = objectMapper.readTree(Files.readString(Path.of("../../docs/deployment-entrypoint-cutover-sample.json")));

        assertThat(sample.at("/sampleName").asText()).isEqualTo("beiming-unified-backend-external-entrypoint-cutover");
        assertThat(sample.at("/mode").asText()).isEqualTo("LOCAL_REHEARSAL_SAMPLE_NOT_APPLIED");
        assertThat(sample.at("/applyProductionTraffic").asBoolean()).isFalse();
        assertThat(sample.at("/requiresUserApprovalBeforeApply").asBoolean()).isTrue();
        assertThat(sample.at("/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/currentEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/candidateEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(sample.at("/rollbackEntrypoint/baseUrl").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(sample.at("/routePolicy/preserveApiV1BusinessPaths").asBoolean()).isTrue();
        assertThat(sample.at("/routePolicy/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(sample.at("/smokeTargets").size()).isEqualTo(32);
        assertThat(sample.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("/api/v1/unified-backend/profile");
        assertThat(sample.toString().toLowerCase())
                .doesNotContain("authorization")
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("jdbc:")
                .doesNotContain("bucket")
                .doesNotContain("topic")
                .doesNotContain("c:\\users\\");
    }

    @Test
    void exposesExternalEntrypointLocalCutoverRehearsalWithoutSwitchingProductionTraffic() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-local-cutover-rehearsal"));

        assertThat(readiness.at("/data/externalEntrypointLocalCutoverRehearsalStatus").asText())
                .isEqualTo("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION");
        assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "CUTOVER_SAMPLE_LOADED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "CUTOVER_SAMPLE_JSON_PARSABLE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "CANDIDATE_ENTRYPOINT_MATCHES_UNIFIED_BACKEND", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "CURRENT_ENTRYPOINT_MATCHES_API_GATEWAY", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "ROLLBACK_ENTRYPOINT_MATCHES_API_GATEWAY", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "SMOKE_TARGETS_COMPLETE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "BUSINESS_PATHS_PRESERVED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "PRODUCTION_TRAFFIC_SWITCH_REMAINS_FALSE", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "ROLLBACK_TARGET_PROTECTED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "LOCAL_REHEARSAL_EXECUTED", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "NO_SENSITIVE_VALUES_IN_REHEARSAL", "PASS", true);
        assertPrecheck(readiness, "/data/externalEntrypointLocalCutoverRehearsalChecks", "READY_FLAGS_REMAIN_FALSE", "PASS", true);

        JsonNode evidence = readiness.at("/data/externalEntrypointLocalCutoverRehearsalEvidence");
        assertThat(evidence.at("/readinessMode").asText())
                .isEqualTo("LOCAL_EXTERNAL_ENTRYPOINT_CUTOVER_REHEARSAL_EXECUTED_NOT_PRODUCTION");
        assertThat(evidence.at("/sampleConfigPath").asText()).isEqualTo("docs/deployment-entrypoint-cutover-sample.json");
        assertThat(evidence.at("/sampleConfigApplied").asBoolean()).isFalse();
        assertThat(evidence.at("/localRehearsalExecuted").asBoolean()).isTrue();
        assertThat(evidence.at("/applyProductionTraffic").asBoolean()).isFalse();
        assertThat(evidence.at("/currentEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/candidateEntrypoint").asText()).isEqualTo("http://127.0.0.1:8135");
        assertThat(evidence.at("/rollbackEntrypoint").asText()).isEqualTo("http://127.0.0.1:8125");
        assertThat(evidence.at("/smokeTargetsTotal").asInt()).isEqualTo(32);
        assertThat(evidence.at("/businessPathsRemainUnchanged").asBoolean()).isTrue();
        assertThat(evidence.at("/businessPathRewriteAllowed").asBoolean()).isFalse();
        assertThat(evidence.at("/sensitiveValuesExposed").asBoolean()).isFalse();
        assertThat(evidence.at("/productionTrafficObserved").asBoolean()).isFalse();
        assertThat(evidence.at("/apiGatewayTrafficZeroProven").asBoolean()).isFalse();
        assertThat(evidence.at("/rollbackWindowCompleted").asBoolean()).isFalse();
        assertThat(evidence.at("/readyForProduction").asBoolean()).isFalse();
        assertThat(evidence.at("/readyToReplaceGateway").asBoolean()).isFalse();
        assertThat(evidence.at("/status").asText()).isEqualTo("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION");
        assertThat(evidence.at("/remainingProductionBlockers").toString())
                .contains("PRODUCTION_TRAFFIC_SWITCH_APPLIED")
                .contains("EXTERNAL_PROXY_CONFIG_APPLIED")
                .contains("FRONTEND_ENTRYPOINT_SWITCH_APPLIED")
                .contains("API_GATEWAY_TRAFFIC_ZERO_PROVEN")
                .contains("ROLLBACK_WINDOW_COMPLETED")
                .contains("CENTRAL_CONFIG_PROVIDER_CONNECTED")
                .contains("PERSISTENT_AUDIT_SINK_CONNECTED")
                .contains("USER_RETIREMENT_APPROVAL_GRANTED");
        assertNoSecrets(readiness);
    }

    @Test
    void externalEntrypointLocalCutoverRehearsalDoesNotLeakSensitiveRuntimeValues() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-external-entrypoint-local-cutover-rehearsal-redaction"));

        assertThat(readiness.at("/data/externalEntrypointLocalCutoverRehearsalStatus").asText())
                .isEqualTo("PASS_LOCAL_REHEARSAL_NOT_PRODUCTION");
        String text = readiness.at("/data/externalEntrypointLocalCutoverRehearsalEvidence").toString()
                + readiness.at("/data/externalEntrypointLocalCutoverRehearsalChecks");
        assertThat(text)
                .doesNotContain("Authorization")
                .doesNotContain("X-Gateway-Internal-Signature")
                .doesNotContain("C:\\Users\\")
                .doesNotContain(".env")
                .doesNotContain("jdbc:")
                .doesNotContain("cmd.exe")
                .doesNotContain("powershell")
                .doesNotContain("kubectl")
                .doesNotContain("docker")
                .doesNotContain("id_rsa");
        assertThat(text.toLowerCase())
                .doesNotContain("token")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("password")
                .doesNotContain("dsn")
                .doesNotContain("bucket")
                .doesNotContain("topic");
        assertNoSecrets(readiness);
    }

    @Test
    void retiresIndependentCoreMavenEntrypointsWhileKeepingModuleSourcesMounted() {
        assertThat(List.of(
                Path.of("../business-core-service/pom.xml"),
                Path.of("../admission-core-service/pom.xml"),
                Path.of("../engagement-core-service/pom.xml"),
                Path.of("../ops-core-service/pom.xml"),
                Path.of("../portal-core-service/pom.xml")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isFalse());
        assertThat(List.of(
                Path.of("../unified-backend-service/pom.xml"),
                Path.of("../business-core-service/src/main/java/cn/beiming/core/BusinessCoreModule.java"),
                Path.of("../admission-core-service/src/main/java/cn/beiming/admission/AdmissionCoreModule.java"),
                Path.of("../engagement-core-service/src/main/java/cn/beiming/engagement/EngagementCoreModule.java"),
                Path.of("../ops-core-service/src/main/java/cn/beiming/opscore/OpsCoreModule.java"),
                Path.of("../portal-core-service/src/main/java/cn/beiming/portalcore/PortalCoreModule.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).as(path.toString()).isTrue());
        assertThat(Files.exists(Path.of("../api-gateway-service/pom.xml"))).isFalse();
    }

    @Test
    void unifiedBackendReadinessAndDocsDoNotReferenceRetiredIndependentMavenEntrypoints() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-retired-entrypoint-residue-scan"));
        JsonNode gatewayTopology = performJson(get("/api/v1/gateway/admin/runtime-topology")
                .header("Authorization", "Bearer owner-token")
                .header("X-Request-Id", "req-retired-entrypoint-gateway-topology"));

        String joinedEvidence = readiness.toString()
                + gatewayTopology.toString()
                + Files.readString(Path.of("../../docs/contracts-unified-backend.md"))
                + Files.readString(Path.of("../../docs/unified-backend-production-audit-observability-smoke-sample.json"));

        assertThat(joinedEvidence)
                .doesNotContain("current six rollback entrypoints")
                .doesNotContain("current six rollback backend Maven entrypoints")
                .doesNotContain("api-gateway and five core entrypoints remain protected")
                .doesNotContain("api-gateway and five core rollback entrypoints")
                .doesNotContain("five core entrypoints remain protected")
                .doesNotContain("mvn -q -f backend/api-gateway-service/pom.xml test")
                .doesNotContain("mvn -q -f backend/business-core-service/pom.xml test")
                .doesNotContain("mvn -q -f backend/admission-core-service/pom.xml test")
                .doesNotContain("mvn -q -f backend/engagement-core-service/pom.xml test")
                .doesNotContain("mvn -q -f backend/ops-core-service/pom.xml test")
                .doesNotContain("mvn -q -f backend/portal-core-service/pom.xml test")
                .contains("mvn -q -f backend/unified-backend-service/pom.xml test")
                .contains("five core module sources remain mounted");
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

    private void assertSwitchCheck(JsonNode response, String check, String status, boolean requiredForReplacement) {
        String checks = response.at("/data/productionSwitchChecks").toString();
        assertThat(checks)
                .contains("\"check\":\"" + check + "\"")
                .contains("\"status\":\"" + status + "\"")
                .contains("\"requiredForReplacement\":" + requiredForReplacement);
    }

    private void assertCentralConfigCheck(JsonNode response, String check, String status, boolean requiredForReplacement) {
        String checks = response.at("/data/centralConfigPrecheckChecks").toString();
        assertThat(checks)
                .contains("\"check\":\"" + check + "\"")
                .contains("\"status\":\"" + status + "\"")
                .contains("\"requiredForReplacement\":" + requiredForReplacement);
    }

    private void assertPersistentAuditCheck(JsonNode response, String check, String status, boolean requiredForReplacement) {
        String checks = response.at("/data/persistentAuditPrecheckChecks").toString();
        assertThat(checks)
                .contains("\"check\":\"" + check + "\"")
                .contains("\"status\":\"" + status + "\"")
                .contains("\"requiredForReplacement\":" + requiredForReplacement);
    }

    private void assertPrecheck(JsonNode response, String path, String check, String status, boolean requiredForReplacement) {
        String checks = response.at(path).toString();
        assertThat(checks)
                .contains("\"check\":\"" + check + "\"")
                .contains("\"status\":\"" + status + "\"")
                .contains("\"requiredForReplacement\":" + requiredForReplacement);
    }

    private void assertNoSecrets(JsonNode node) {
        String text = node.toString().toLowerCase()
                .replace("requiresexternalsecretstore", "");
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
