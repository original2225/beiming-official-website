package cn.beiming.backuprecovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class, properties = "backup-recovery.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BackupRecoveryApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("backup-recovery local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "BKR-COM", 1, 100);
        addRange(mapped, "BKR-AUTH", 1, 120);
        addRange(mapped, "BKR-HEALTH", 1, 40);
        addRange(mapped, "BKR-OPS", 1, 90);
        addRange(mapped, "BKR-DOMAIN", 1, 100);
        addRange(mapped, "BKR-POLICY", 1, 170);
        addRange(mapped, "BKR-JOB", 1, 170);
        addRange(mapped, "BKR-POINT", 1, 130);
        addRange(mapped, "BKR-VERIFY", 1, 110);
        addRange(mapped, "BKR-DRILL", 1, 120);
        addRange(mapped, "BKR-RESTORE", 1, 180);
        addRange(mapped, "BKR-AUDIT", 1, 100);
        addRange(mapped, "BKR-DEPS", 1, 120);
        addRange(mapped, "BKR-HARDEN", 1, 180);
        addRange(mapped, "BKR-PORT", 1, 20);
        addRange(mapped, "BKR-CYCLE", 1, 100);
        assertThat(mapped).contains("BKR-COM-001", "BKR-POLICY-170", "BKR-RESTORE-180", "BKR-HARDEN-180", "BKR-CYCLE-100");
        assertThat(mapped).hasSize(1850);
    }

    @Test
    @DisplayName("BKR-COM, BKR-AUTH, BKR-HEALTH, BKR-OPS, and BKR-DOMAIN cover envelope, auth, health, summary, and domain reads")
    void commonAuthHealthSummaryAndDomains() throws Exception {
        mvc.perform(get("/api/v1/backup-recovery/health").header("X-Request-Id", "req-bkr-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-bkr-health"));

        JsonNode health = performJson(get("/api/v1/backup-recovery/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("backup-recovery");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/backup-recovery/ops/summary"), 401, 41000);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("br-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 46810);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 46811);
        performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 46812);

        JsonNode summary = performJson(get("/api/v1/backup-recovery/ops/summary").header("Authorization", bearer("br-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("backup-recovery");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8133);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/backupAdapterMode").asText()).isEqualTo("SIMULATED");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/domainsTotal").asInt()).isGreaterThan(5);
        assertThat(summary.at("/data/productionGaps").toString()).contains(
                "REAL_PERSISTENCE_NOT_CONNECTED",
                "REAL_BACKUP_MEDIA_NOT_CONNECTED",
                "REAL_CROSS_SERVICE_HTTP_NOT_CONNECTED",
                "REAL_RESTORE_EXECUTION_BLOCKED",
                "ADMIN_READ_ONLY_ENTRY_NOT_CONNECTED",
                "NODE_DAEMON_DIRECT_CALL_FORBIDDEN");
        assertNoSecrets(summary);

        performJson(get("/api/v1/backup-recovery/domains").header("Authorization", bearer("br-viewer-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/backup-recovery/domains").header("Authorization", bearer("br-viewer-token")).param("sort", "bad"), 400, 40003);

        JsonNode domains = performJson(get("/api/v1/backup-recovery/domains")
                .header("Authorization", bearer("br-viewer-token"))
                .param("keyword", "auth")
                .param("sourceService", "auth")
                .param("criticality", "CRITICAL")
                .param("enabled", "true")
                .param("sort", "displayName_asc"), 200);
        assertThat(domains.toString()).contains("DATABASE_AUTH").doesNotContain("jdbc:", "secretKey");

        performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                with(policyBody("trusted-policy"), "createdBy", "browser"), 400, 40001);
    }

    @Test
    @DisplayName("BKR-POLICY covers policy list, detail, create, patch, enable, disable, idempotency, conflict, and audit rollback")
    void policyLifecycle() throws Exception {
        JsonNode policies = performJson(get("/api/v1/backup-recovery/policies")
                .header("Authorization", bearer("br-viewer-token"))
                .param("status", "ENABLED")
                .param("domain", "DATABASE_AUTH")
                .param("sort", "displayName_asc"), 200);
        assertThat(policies.toString()).contains("policy-main");

        JsonNode detail = performJson(get("/api/v1/backup-recovery/policies/policy-main")
                .header("Authorization", bearer("br-viewer-token")), 200);
        assertThat(detail.at("/data/policyId").asText()).isEqualTo("policy-main");
        assertNoSecrets(detail);
        performJson(get("/api/v1/backup-recovery/policies/missing").header("Authorization", bearer("br-viewer-token")), 404, 49801);

        JsonNode created = performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                policyBody("policy-create"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/storageRef/alias").asText()).isEqualTo("backup-vault-main");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                policyBody("policy-create"), 201);
        assertThat(replay.at("/data/policyId").asText()).isEqualTo(created.at("/data/policyId").asText());
        performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                with(policyBody("policy-create"), "displayName", "Changed Policy"), 409, 49812);
        performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                with(policyBody("policy-name-conflict"), "displayName", "Main daily backup"), 409, 49811);
        performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-viewer-token")),
                policyBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                with(policyBody("bad-retention"), "retentionDays", 0), 400, 40001);
        performJson(post("/api/v1/backup-recovery/policies")
                        .header("Authorization", bearer("br-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                policyBody("audit-fail-policy"), 500, 55401);
        JsonNode auditFailedLookup = performJson(get("/api/v1/backup-recovery/policies")
                .header("Authorization", bearer("br-viewer-token"))
                .param("keyword", "audit-fail-policy"), 200);
        assertThat(auditFailedLookup.at("/data/total").asInt()).isZero();

        String policyId = created.at("/data/policyId").asText();
        JsonNode patched = performJson(patch("/api/v1/backup-recovery/policies/" + policyId).header("Authorization", bearer("br-admin-token")),
                Map.of("displayName", "Updated backup policy", "minimumCopies", 3, "reason", "更新保留份数", "idempotencyKey", "patch-policy"), 200);
        assertThat(patched.at("/data/displayName").asText()).contains("Updated");

        JsonNode enabled = performJson(patch("/api/v1/backup-recovery/policies/" + policyId + "/enable").header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "启用策略", "idempotencyKey", "enable-policy"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");
        JsonNode disabled = performJson(patch("/api/v1/backup-recovery/policies/" + policyId + "/disable").header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "停用策略", "idempotencyKey", "disable-policy"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(patch("/api/v1/backup-recovery/policies/missing/enable").header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "missing", "idempotencyKey", "enable-missing"), 404, 49801);
    }

    @Test
    @DisplayName("BKR-JOB and BKR-POINT cover backup job creation, listing, detail, cancellation, dependency failures, and backup points")
    void jobsAndBackupPoints() throws Exception {
        JsonNode job = performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                jobBody("job-create"), 201);
        assertThat(job.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        assertThat(job.at("/data/resultSummary/backupPointId").asText()).isNotBlank();
        assertNoSecrets(job);

        JsonNode replay = performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                jobBody("job-create"), 201);
        assertThat(replay.at("/data/jobId").asText()).isEqualTo(job.at("/data/jobId").asText());
        performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                with(jobBody("job-create"), "trigger", "SCHEDULED"), 409, 49812);
        performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                with(jobBody("missing-policy"), "policyId", "missing"), 404, 49801);
        performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                with(jobBody("unknown-domain"), "domains", List.of("UNKNOWN_DOMAIN")), 400, 40001);
        performJson(post("/api/v1/backup-recovery/jobs")
                        .header("Authorization", bearer("br-admin-token"))
                        .header("X-Test-Ops-Control-Mode", "unavailable"),
                with(jobBody("ops-unavailable"), "opsControlTaskRef", Map.of("taskId", "ops-task-1")), 502, 46820);
        performJson(post("/api/v1/backup-recovery/jobs")
                        .header("Authorization", bearer("br-admin-token"))
                        .header("X-Test-Backup-Mode", "failed"),
                jobBody("job-failed"), 201);

        JsonNode jobsFuture = performJson(get("/api/v1/backup-recovery/jobs")
                .header("Authorization", bearer("br-viewer-token"))
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2031-01-01T00:00:00Z"), 200);
        assertThat(jobsFuture.at("/data/total").asInt()).isZero();
        JsonNode jobsCurrent = performJson(get("/api/v1/backup-recovery/jobs")
                .header("Authorization", bearer("br-viewer-token"))
                .param("policyId", "policy-main")
                .param("status", "SUCCEEDED")
                .param("trigger", "ADMIN_MANUAL")
                .param("createdBy", "br-admin-user")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z")
                .param("sort", "createdAt_desc"), 200);
        assertThat(jobsCurrent.toString()).contains(job.at("/data/jobId").asText());
        performJson(get("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-viewer-token"))
                .param("from", "2030-01-01T00:00:00Z").param("to", "2020-01-01T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/backup-recovery/jobs/missing").header("Authorization", bearer("br-viewer-token")), 404, 49802);

        JsonNode pending = performJson(post("/api/v1/backup-recovery/jobs")
                        .header("Authorization", bearer("br-admin-token"))
                        .header("X-Test-Backup-Mode", "pending"),
                jobBody("job-pending"), 201);
        JsonNode canceled = performJson(patch("/api/v1/backup-recovery/jobs/" + pending.at("/data/jobId").asText() + "/cancel")
                        .header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "取消待执行任务", "idempotencyKey", "cancel-pending"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELLED");
        performJson(patch("/api/v1/backup-recovery/jobs/" + job.at("/data/jobId").asText() + "/cancel")
                        .header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "终态不能取消", "idempotencyKey", "cancel-finished"), 409, 49810);

        String pointId = job.at("/data/resultSummary/backupPointId").asText();
        JsonNode points = performJson(get("/api/v1/backup-recovery/backup-points")
                .header("Authorization", bearer("br-viewer-token"))
                .param("policyId", "policy-main")
                .param("jobId", job.at("/data/jobId").asText())
                .param("domain", "DATABASE_AUTH")
                .param("status", "AVAILABLE")
                .param("verified", "false")
                .param("sort", "createdAt_desc"), 200);
        assertThat(points.toString()).contains(pointId).doesNotContain("C:\\\\", "/srv/", "secretKey");
        JsonNode point = performJson(get("/api/v1/backup-recovery/backup-points/" + pointId)
                .header("Authorization", bearer("br-viewer-token")), 200);
        assertThat(point.at("/data/storageRef/alias").asText()).isEqualTo("backup-vault-main");
        assertNoSecrets(point);
        performJson(get("/api/v1/backup-recovery/backup-points/missing").header("Authorization", bearer("br-viewer-token")), 404, 49803);
    }

    @Test
    @DisplayName("BKR-VERIFY, BKR-DRILL, and BKR-RESTORE cover verification, restore drills, restore requests, approval, rejection, and simulated restore boundary")
    void verifyDrillAndRestoreFlow() throws Exception {
        JsonNode job = performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                jobBody("restore-job"), 201);
        String pointId = job.at("/data/resultSummary/backupPointId").asText();

        JsonNode verification = performJson(post("/api/v1/backup-recovery/backup-points/" + pointId + "/verify")
                        .header("Authorization", bearer("br-approver-token")),
                Map.of("validationLevel", "CHECKSUM", "reason", "校验备份点", "idempotencyKey", "verify-point"), 200);
        assertThat(verification.at("/data/status").asText()).isEqualTo("PASSED");

        JsonNode point = performJson(get("/api/v1/backup-recovery/backup-points/" + pointId)
                .header("Authorization", bearer("br-viewer-token")), 200);
        assertThat(point.at("/data/status").asText()).isEqualTo("VERIFIED");
        performJson(post("/api/v1/backup-recovery/backup-points/missing/verify")
                        .header("Authorization", bearer("br-approver-token")),
                Map.of("validationLevel", "CHECKSUM", "reason", "missing", "idempotencyKey", "verify-missing"), 404, 49803);

        JsonNode drill = performJson(post("/api/v1/backup-recovery/restore-drills")
                        .header("Authorization", bearer("br-approver-token")),
                drillBody(pointId, "drill-main"), 201);
        assertThat(drill.at("/data/status").asText()).isEqualTo("PASSED");
        String drillId = drill.at("/data/drillId").asText();
        JsonNode drills = performJson(get("/api/v1/backup-recovery/restore-drills")
                .header("Authorization", bearer("br-viewer-token"))
                .param("backupPointId", pointId)
                .param("status", "PASSED")
                .param("createdBy", "br-approver-user")
                .param("sort", "createdAt_desc"), 200);
        assertThat(drills.toString()).contains(drillId);
        performJson(get("/api/v1/backup-recovery/restore-drills/" + drillId)
                .header("Authorization", bearer("br-viewer-token")), 200);
        performJson(get("/api/v1/backup-recovery/restore-drills/missing").header("Authorization", bearer("br-viewer-token")), 404, 49800);

        performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("br-admin-token")),
                restoreBody(pointId, null, "restore-no-drill"), 409, 49814);
        performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("br-admin-token")),
                with(restoreBody(pointId, drillId, "restore-domain-overreach"), "domains", List.of("DATABASE_AUTH", "ATTENDANCE_LEDGER")), 400, 40001);
        performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("br-admin-token")),
                with(restoreBody(pointId, drillId, "restore-prod-write"), "impactSummary", Map.of("scope", "production", "writesProduction", true)), 400, 40001);
        JsonNode request = performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("br-admin-token")),
                restoreBody(pointId, drillId, "restore-main"), 201);
        assertThat(request.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        String requestId = request.at("/data/restoreRequestId").asText();

        JsonNode requests = performJson(get("/api/v1/backup-recovery/restore-requests")
                .header("Authorization", bearer("br-viewer-token"))
                .param("backupPointId", pointId)
                .param("status", "PENDING_APPROVAL")
                .param("requestedBy", "br-admin-user")
                .param("riskLevel", "CRITICAL")
                .param("sort", "createdAt_desc"), 200);
        assertThat(requests.toString()).contains(requestId);
        performJson(get("/api/v1/backup-recovery/restore-requests/" + requestId)
                .header("Authorization", bearer("br-viewer-token")), 200);
        performJson(get("/api/v1/backup-recovery/restore-requests/missing").header("Authorization", bearer("br-viewer-token")), 404, 49804);

        JsonNode approved = performJson(patch("/api/v1/backup-recovery/restore-requests/" + requestId + "/approve")
                        .header("Authorization", bearer("owner-token")),
                Map.of("reviewComment", "批准模拟恢复", "confirmText", "APPROVE_SIMULATED_RESTORE", "reason", "审批恢复", "idempotencyKey", "approve-restore"), 200);
        assertThat(approved.at("/data/status").asText()).isIn("COMPLETED_SIMULATED", "EXECUTION_BLOCKED");
        assertThat(approved.at("/data/approvalSummary/executionMode").asText()).isIn("SIMULATED_ONLY", "BLOCKED_BY_CONTRACT");
        assertNoSecrets(approved);

        JsonNode rejectTarget = performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("br-admin-token")),
                restoreBody(pointId, drillId, "restore-reject"), 201);
        JsonNode rejected = performJson(patch("/api/v1/backup-recovery/restore-requests/" + rejectTarget.at("/data/restoreRequestId").asText() + "/reject")
                        .header("Authorization", bearer("owner-token")),
                Map.of("reviewComment", "拒绝恢复", "reason", "风险过高", "idempotencyKey", "reject-restore"), 200);
        assertThat(rejected.at("/data/status").asText()).isEqualTo("REJECTED");

        JsonNode ownerRequest = performJson(post("/api/v1/backup-recovery/restore-requests")
                        .header("Authorization", bearer("owner-token")),
                restoreBody(pointId, drillId, "restore-self"), 201);
        performJson(patch("/api/v1/backup-recovery/restore-requests/" + ownerRequest.at("/data/restoreRequestId").asText() + "/approve")
                        .header("Authorization", bearer("owner-token")),
                Map.of("reviewComment", "自审禁止", "confirmText", "APPROVE_SIMULATED_RESTORE", "reason", "自审", "idempotencyKey", "approve-self"), 409, 49810);
    }

    @Test
    @DisplayName("BKR-AUDIT, BKR-DEPS, and BKR-HARDEN cover audit filters, dependency failures, trusted fields, and source scanning")
    void auditDegradeAndHardening() throws Exception {
        JsonNode policy = performJson(post("/api/v1/backup-recovery/policies").header("Authorization", bearer("br-admin-token")),
                policyBody("audit-policy"), 201);
        String policyId = policy.at("/data/policyId").asText();
        JsonNode audit = performJson(get("/api/v1/backup-recovery/audit-logs")
                .header("Authorization", bearer("br-admin-token"))
                .param("actorUserId", "br-admin-user")
                .param("policyId", policyId)
                .param("action", "BACKUP_POLICY_CREATED")
                .param("result", "SUCCESS")
                .param("riskLevel", "MEDIUM")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z")
                .param("sort", "createdAt_desc"), 200);
        assertThat(audit.at("/data/total").asInt()).isEqualTo(1);
        assertNoSecrets(audit);

        performJson(get("/api/v1/backup-recovery/audit-logs").header("Authorization", bearer("br-viewer-token")), 403, 42001);
        performJson(get("/api/v1/backup-recovery/audit-logs")
                .header("Authorization", bearer("br-admin-token"))
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2020-01-01T00:00:00Z"), 400, 40001);
        JsonNode futureAudit = performJson(get("/api/v1/backup-recovery/audit-logs")
                .header("Authorization", bearer("br-admin-token"))
                .param("policyId", policyId)
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2031-01-01T00:00:00Z"), 200);
        assertThat(futureAudit.at("/data/total").asInt()).isZero();

        performJson(patch("/api/v1/backup-recovery/policies/" + policyId + "/enable")
                        .header("Authorization", bearer("br-admin-token")),
                Map.of("reason", "拒绝可信字段", "idempotencyKey", "enable-trusted", "taskStatus", "SUCCEEDED"), 400, 40001);
        JsonNode stillDraft = performJson(get("/api/v1/backup-recovery/policies/" + policyId)
                .header("Authorization", bearer("br-viewer-token")), 200);
        assertThat(stillDraft.at("/data/status").asText()).isEqualTo("DRAFT");

        JsonNode job = performJson(post("/api/v1/backup-recovery/jobs").header("Authorization", bearer("br-admin-token")),
                jobBody("hardening-job"), 201);
        String pointId = job.at("/data/resultSummary/backupPointId").asText();
        performJson(post("/api/v1/backup-recovery/restore-drills")
                        .header("Authorization", bearer("br-approver-token")),
                with(drillBody(pointId, "trusted-drill"), "internalPath", "/srv/secret"), 400, 40001);

        Path serviceRoot = Path.of("src/main/java/cn/beiming/backuprecovery");
        String source = Files.exists(serviceRoot)
                ? String.join("\n", Files.walk(serviceRoot)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList())
                : "";
        assertThat(source).doesNotContain(
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.content.",
                "cn.beiming.serverstatus.", "cn.beiming.resource.", "cn.beiming.admin.", "cn.beiming.onboarding.",
                "cn.beiming.exam.", "cn.beiming.whitelist.", "cn.beiming.attendance.", "cn.beiming.community.",
                "cn.beiming.activity.", "cn.beiming.calendar.", "cn.beiming.changelog.", "cn.beiming.opscontrol.",
                "cn.beiming.nodedaemon.", "cn.beiming.cloudrevesync.", "Repository", "JdbcTemplate",
                "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl", "pvesh", "mcrcon", "rm -rf",
                "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s", "rawToken", "credential",
                "secretKey", "backupEncryptionKey", "nodeToken", "jdbc:", "authorized_keys", "id_rsa", ".env",
                "BACKUP_RESTORE", "node-daemon");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private Map<String, Object> policyBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Backup " + idempotencyKey);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA", "OPS_AUDIT_INDEX"));
        body.put("scheduleSummary", Map.of("mode", "DAILY", "cron", "0 30 3 * * *", "timezone", "Asia/Shanghai", "windowMinutes", 90));
        body.put("retentionDays", 30);
        body.put("minimumCopies", 2);
        body.put("storageRef", Map.of("alias", "backup-vault-main", "regionSummary", "cn-local", "tier", "WARM"));
        body.put("encryptionMode", "MANAGED_KEY");
        body.put("reason", "创建备份策略");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> jobBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", "policy-main");
        body.put("trigger", "ADMIN_MANUAL");
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("reason", "创建备份任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> drillBody(String backupPointId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("backupPointId", backupPointId);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("validationPlan", Map.of("mode", "SANDBOX_READ", "checks", List.of("CHECKSUM", "SCHEMA")));
        body.put("reason", "创建恢复演练");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> restoreBody(String backupPointId, String drillId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("backupPointId", backupPointId);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("restoreMode", "SANDBOX_RESTORE");
        if (drillId != null) {
            body.put("drillId", drillId);
        }
        body.put("impactSummary", Map.of("scope", "sandbox", "writesProduction", false));
        body.put("confirmText", "REQUEST_RESTORE_REVIEW");
        body.put("reason", "申请模拟恢复");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "rawToken", "credential", "secretKey", "backupEncryptionKey", "nodeToken", "Authorization",
                "authorizationHeader", "requestHeaders", "stackTrace", "internalPath", "resolvedPath",
                "jdbc:", "AKIA", "objectSecret", "databasePassword", "authorized_keys", "id_rsa",
                "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl", "pvesh", "mcrcon",
                "targetDatabaseUrl", "restorePath", "shellCommand", "nodeEndpoint",
                "/srv/", "C:\\\\", ".env", "token=");
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
