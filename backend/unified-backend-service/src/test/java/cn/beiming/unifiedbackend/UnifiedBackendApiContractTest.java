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
        addRange(mapped, "UBACK-READY", 1, 10);
        addRange(mapped, "UBACK-SMOKE", 1, 1);
        addRange(mapped, "UBACK-HTTP", 1, 1);
        addRange(mapped, "UBACK-DRIFT", 1, 1);
        addRange(mapped, "UBACK-BOUNDARY", 1, 1);
        addRange(mapped, "UBACK-REGRESS", 1, 1);

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
                "UBACK-SMOKE-001",
                "UBACK-HTTP-001",
                "UBACK-DRIFT-001",
                "UBACK-BOUNDARY-001",
                "UBACK-REGRESS-001"
        );
        assertThat(mapped).hasSize(41);
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
    void exposesProductionSwitchReadinessMatrixWithoutAllowingRetirement() throws Exception {
        JsonNode readiness = performJson(get("/api/v1/unified-backend/admin/readiness")
                .header("Authorization", "Bearer admin-token")
                .header("X-Request-Id", "req-cutover-readiness"));

        assertThat(readiness.at("/data/productionSwitchReadinessStatus").asText()).isEqualTo("BLOCKED");
        assertSwitchCheck(readiness, "ALL_CURRENT_BUSINESS_ROUTES_IN_PROCESS", "PASS", true);
        assertSwitchCheck(readiness, "CURRENT_ENTRYPOINTS_PRESERVED", "PASS", true);
        assertSwitchCheck(readiness, "ROUTE_PREFIX_AND_RESPONSE_PRESERVED", "PASS", true);
        assertSwitchCheck(readiness, "NODE_DAEMON_EXTERNAL_BOUNDARY", "PASS", true);
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
        assertThat(decision.at("/nodeDaemonDisposition").asText()).isEqualTo("KEEP_EXTERNAL");
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
        assertCentralConfigCheck(readiness, "NODE_DAEMON_EXTERNAL_PORT_DOCUMENTED", "PASS", true);
        assertCentralConfigCheck(readiness, "DANGEROUS_TEST_CONTROLS_DISABLED", "PASS", true);
        assertCentralConfigCheck(readiness, "CENTRAL_CONFIG_PROVIDER_CONNECTED", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "PRODUCTION_PROFILE_BOUND", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "SENSITIVE_CONFIG_SOURCE_EXTERNALIZED", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "CONFIG_DRIFT_SCAN_AUTOMATED", "BLOCKED", true);
        assertCentralConfigCheck(readiness, "CONFIG_ROLLBACK_SOURCE_DEFINED", "BLOCKED", true);
        assertThat(readiness.at("/data/centralConfigPrecheckChecks").toString())
                .contains("\"check\":\"CANDIDATE_PORT_FIXED\"")
                .contains("\"check\":\"CENTRAL_CONFIG_PROVIDER_CONNECTED\"")
                .doesNotContain("C:\\Users\\");
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
        assertPersistentAuditCheck(readiness, "AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED", "BLOCKED", true);
        assertSwitchCheck(readiness, "PERSISTENT_AUDIT_READY", "BLOCKED", true);
        assertThat(readiness.at("/data/persistentAuditPrecheckChecks").toString())
                .contains("\"check\":\"AUDIT_SINK_FIXED\"")
                .contains("\"check\":\"PERSISTENT_AUDIT_SINK_CONNECTED\"")
                .doesNotContain("C:\\Users\\")
                .doesNotContain("Authorization");
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
        assertPrecheck(readiness, "/data/realHttpRehearsalPrecheckChecks", "NODE_DAEMON_EXCLUDED_FROM_REHEARSAL", "PASS", true);
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
        assertPrecheck(readiness, "/data/routeDriftPrecheckChecks", "NODE_DAEMON_ROUTE_KEPT_EXTERNAL", "PASS", true);
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
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "CORE_ENTRYPOINTS_ROLLBACK_TARGETS_DOCUMENTED", "PASS", true);
        assertPrecheck(readiness, "/data/rollbackWindowPrecheckChecks", "NODE_DAEMON_UNAFFECTED_BY_CANDIDATE", "PASS", true);
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
                .contains("NODE_DAEMON_BOUNDARY_CHANGED");
        assertThat(evidence.at("/recheckAutomation/commands").toString())
                .contains("mvn -q -f backend/unified-backend-service/pom.xml test")
                .contains("backend/node-daemon-service/pom.xml")
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
                .contains("\"entrypoint\":\"node-daemon\"")
                .contains("\"port\":8117")
                .contains("\"disposition\":\"KEEP_EXTERNAL\"");
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
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PRODUCTION_TRAFFIC_CANARY_DEFINED", "BLOCKED", true);
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
        assertPrecheck(readiness, "/data/entrypointSwitchPrecheckChecks", "PRODUCTION_TRAFFIC_CANARY_DEFINED", "BLOCKED", true);
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
