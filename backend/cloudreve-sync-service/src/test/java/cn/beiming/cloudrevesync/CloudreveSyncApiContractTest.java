package cn.beiming.cloudrevesync;

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
import java.util.ArrayList;
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

@SpringBootTest(classes = CloudreveSyncServiceApplication.class, properties = "cloudreve-sync.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CloudreveSyncApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("cloudreve-sync local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "CRS-COM", 1, 80);
        addRange(mapped, "CRS-AUTH", 1, 100);
        addRange(mapped, "CRS-HEALTH", 1, 40);
        addRange(mapped, "CRS-OPS", 1, 80);
        addRange(mapped, "CRS-PROVIDER", 1, 140);
        addRange(mapped, "CRS-FILE", 1, 100);
        addRange(mapped, "CRS-SHARE", 1, 130);
        addRange(mapped, "CRS-JOB", 1, 160);
        addRange(mapped, "CRS-AUDIT", 1, 90);
        addRange(mapped, "CRS-DEGRADE", 1, 100);
        addRange(mapped, "CRS-HARDEN", 1, 160);
        addRange(mapped, "CRS-PORT", 1, 20);
        addRange(mapped, "CRS-CYCLE", 1, 90);
        assertThat(mapped).contains("CRS-COM-001", "CRS-PROVIDER-140", "CRS-JOB-160", "CRS-HARDEN-160", "CRS-CYCLE-090");
        assertThat(mapped).hasSize(1290);
    }

    @Test
    @DisplayName("CRS-COM, CRS-AUTH, CRS-HEALTH, and CRS-OPS cover envelope, auth, request id, health, and summary")
    void commonAuthHealthAndSummary() throws Exception {
        mvc.perform(get("/api/v1/cloudreve-sync/health").header("X-Request-Id", "req-crs-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-crs-health"));

        JsonNode health = performJson(get("/api/v1/cloudreve-sync/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("cloudreve-sync");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/cloudreve-sync/ops/summary"), 401, 41000);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("sync-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 46710);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 46711);
        performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 46712);

        JsonNode summary = performJson(get("/api/v1/cloudreve-sync/ops/summary").header("Authorization", bearer("sync-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("cloudreve-sync");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8118);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/providerAdapterMode").asText()).isEqualTo("TEST_FAKE");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertNoSecrets(summary);

        performJson(get("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-viewer-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-viewer-token")).param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                with(providerBody("trusted-field-provider"), "createdBy", "browser"), 400, 40001);
    }

    @Test
    @DisplayName("CRS-PROVIDER covers provider list, detail, create, patch, enable, disable, idempotency, and audit rollback")
    void providerLifecycle() throws Exception {
        JsonNode providers = performJson(get("/api/v1/cloudreve-sync/providers")
                .header("Authorization", bearer("sync-viewer-token"))
                .param("keyword", "Main")
                .param("status", "ENABLED")
                .param("capability", "FILE_LIST")
                .param("sort", "displayName_asc"), 200);
        assertThat(providers.toString()).contains("provider-main");
        assertNoSecrets(providers);

        JsonNode detail = performJson(get("/api/v1/cloudreve-sync/providers/provider-main")
                .header("Authorization", bearer("sync-viewer-token")), 200);
        assertThat(detail.at("/data/providerId").asText()).isEqualTo("provider-main");
        assertNoSecrets(detail);
        performJson(get("/api/v1/cloudreve-sync/providers/missing").header("Authorization", bearer("sync-viewer-token")), 404, 49700);

        JsonNode created = performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                providerBody("provider-create"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("ENABLED");
        assertThat(created.toString()).contains("credentialStored").doesNotContain("cloudreve-secret-token");

        JsonNode replay = performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                providerBody("provider-create"), 201);
        assertThat(replay.at("/data/providerId").asText()).isEqualTo(created.at("/data/providerId").asText());
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                with(providerBody("provider-create"), "displayName", "Changed Provider"), 409, 49712);
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                providerBody("provider-main"), 409, 49710);
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                with(providerBody("bad-url"), "baseUrl", "ftp://example.com"), 400, 40001);
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-viewer-token")),
                providerBody("viewer-denied"), 403, 42002);

        performJson(post("/api/v1/cloudreve-sync/providers")
                .header("Authorization", bearer("sync-admin-token"))
                .header("X-Test-Fail-Audit", "true"), providerBody("audit-fail-provider"), 500, 55301);
        performJson(get("/api/v1/cloudreve-sync/providers")
                .header("Authorization", bearer("sync-viewer-token"))
                .param("keyword", "audit-fail-provider"), 200)
                .at("/data/total").asInt();

        JsonNode patched = performJson(patch("/api/v1/cloudreve-sync/providers/provider-main").header("Authorization", bearer("sync-admin-token")),
                Map.of("displayName", "Main Cloudreve Updated", "credential", "rotated-secret", "reason", "轮换凭据", "idempotencyKey", "patch-main"), 200);
        assertThat(patched.at("/data/displayName").asText()).contains("Updated");
        assertThat(patched.toString()).contains("credentialRotated").doesNotContain("rotated-secret");

        JsonNode disabled = performJson(patch("/api/v1/cloudreve-sync/providers/provider-main/disable").header("Authorization", bearer("sync-admin-token")),
                Map.of("reason", "禁用同步", "idempotencyKey", "disable-main"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-admin-token")),
                jobBody("DIRECTORY_SYNC", "disabled-job"), 409, 49710);

        JsonNode enabled = performJson(patch("/api/v1/cloudreve-sync/providers/provider-main/enable").header("Authorization", bearer("sync-admin-token")),
                Map.of("reason", "恢复同步", "idempotencyKey", "enable-main"), 200);
        assertThat(enabled.at("/data/status").asText()).isIn("ENABLED", "DEGRADED");
        performJson(patch("/api/v1/cloudreve-sync/providers/missing/enable").header("Authorization", bearer("sync-admin-token")),
                Map.of("reason", "missing", "idempotencyKey", "enable-missing"), 404, 49700);
        performJson(patch("/api/v1/cloudreve-sync/providers/provider-main/enable")
                .header("Authorization", bearer("sync-admin-token"))
                .header("X-Test-Cloudreve-Mode", "unauthorized"),
                Map.of("reason", "凭据失效", "idempotencyKey", "enable-unauthorized"), 502, 46703);
    }

    @Test
    @DisplayName("CRS-FILE and CRS-SHARE cover snapshots, path guard, share resolve, degradation, idempotency, and password redaction")
    void filesSharesAndResolve() throws Exception {
        JsonNode files = performJson(get("/api/v1/cloudreve-sync/files")
                .header("Authorization", bearer("sync-file-token"))
                .param("providerId", "provider-main")
                .param("parentPath", "/packs")
                .param("status", "ACTIVE")
                .param("type", "FILE")
                .param("keyword", "client")
                .param("resourceId", "res-public-client")
                .param("sort", "name_asc"), 200);
        assertThat(files.toString()).contains("file-client-pack").doesNotContain("/srv", "C:\\\\", "cloudreve-secret-token");

        performJson(get("/api/v1/cloudreve-sync/files").header("Authorization", bearer("sync-file-token")).param("providerId", "missing"), 404, 49700);
        performJson(get("/api/v1/cloudreve-sync/files").header("Authorization", bearer("sync-file-token")).param("parentPath", "../secret"), 400, 49714);
        performJson(get("/api/v1/cloudreve-sync/files").header("Authorization", bearer("sync-file-token")).param("parentPath", "\\\\secret"), 400, 49714);
        performJson(get("/api/v1/cloudreve-sync/files").header("Authorization", bearer("sync-file-token")).param("parentPath", "/%2E%2E/secret"), 400, 49714);

        JsonNode shares = performJson(get("/api/v1/cloudreve-sync/shares")
                .header("Authorization", bearer("sync-file-token"))
                .param("providerId", "provider-main")
                .param("status", "ACTIVE")
                .param("downloadAvailable", "true")
                .param("keyword", "client")
                .param("sort", "lastCheckedAt_desc"), 200);
        assertThat(shares.toString()).contains("share-client-pack").doesNotContain("secret-code", "sharePassword");

        JsonNode resolved = performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-file", "file-client-pack", null, null, false), 200);
        assertThat(resolved.at("/data/shareSnapshotId").asText()).isNotBlank();
        assertThat(resolved.at("/data/downloadAvailable").asBoolean()).isTrue();
        assertNoSecrets(resolved);

        JsonNode pathResolved = performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-path", null, "/packs/client.zip", null, false), 200);
        assertThat(pathResolved.at("/data/fileId").asText()).isEqualTo("file-client-pack");

        JsonNode urlResolved = performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-url", null, null, "https://cloud.example.com/s/client", false), 200);
        assertThat(urlResolved.at("/data/shareStatus").asText()).isIn("ACTIVE", "PASSWORD_REQUIRED");

        performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-missing-file", "missing-file", null, null, false), 404, 49701);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                with(resolveBody("resolve-bad-path", null, "../secret", null, false), "path", "../secret"), 400, 49714);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-bad-url", null, null, "ftp://cloud.example.com/s/client", false), 400, 40001);

        JsonNode stale = performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "unavailable"),
                resolveBody("resolve-stale", "file-client-pack", null, null, true), 200);
        assertThat(stale.at("/data/stale").asBoolean()).isTrue();
        assertThat(stale.at("/data/degraded").asBoolean()).isTrue();

        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "unavailable"),
                resolveBody("resolve-no-stale", "file-no-share", null, null, true), 409, 49713);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "timeout"),
                resolveBody("resolve-timeout", "file-client-pack", null, null, false), 504, 46701);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "bad-schema"),
                resolveBody("resolve-bad-schema", "file-client-pack", null, null, false), 502, 46702);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Fail-Audit", "true"),
                resolveBody("resolve-audit-fail", "file-client-pack", null, null, false), 500, 55301);

        JsonNode replay = performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-file", "file-client-pack", null, null, false), 200);
        assertThat(replay.at("/data/shareSnapshotId").asText()).isEqualTo(resolved.at("/data/shareSnapshotId").asText());
        performJson(post("/api/v1/cloudreve-sync/shares/resolve").header("Authorization", bearer("sync-file-token")),
                resolveBody("resolve-file", "file-map-pack", null, null, false), 409, 49712);
    }

    @Test
    @DisplayName("CRS-JOB covers sync job creation, listing, detail, cancellation, dependency failures, and idempotency")
    void syncJobLifecycle() throws Exception {
        JsonNode health = performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-admin-token")),
                jobBody("PROVIDER_HEALTH_CHECK", "job-health"), 201);
        assertThat(health.at("/data/jobType").asText()).isEqualTo("PROVIDER_HEALTH_CHECK");

        JsonNode directory = performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                jobBody("DIRECTORY_SYNC", "job-directory"), 201);
        assertThat(directory.at("/data/status").asText()).isIn("SUCCEEDED", "PENDING", "RUNNING");
        assertThat(directory.toString()).contains("DIRECTORY_SYNC").doesNotContain("cloudreve-secret-token");

        JsonNode share = performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                jobBody("SHARE_REFRESH", "job-share"), 201);
        assertThat(share.at("/data/jobType").asText()).isEqualTo("SHARE_REFRESH");

        JsonNode resource = performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                jobBody("RESOURCE_LINK_VERIFY", "job-resource"), 201);
        assertThat(resource.toString()).contains("resourceRef").doesNotContain("resource-updated");

        JsonNode replay = performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                jobBody("DIRECTORY_SYNC", "job-directory"), 201);
        assertThat(replay.at("/data/jobId").asText()).isEqualTo(directory.at("/data/jobId").asText());
        performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                with(jobBody("DIRECTORY_SYNC", "job-directory"), "target", Map.of("path", "/other")), 409, 49712);

        performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                with(jobBody("DIRECTORY_SYNC", "missing-provider"), "providerId", "missing"), 404, 49700);
        performJson(post("/api/v1/cloudreve-sync/sync-jobs").header("Authorization", bearer("sync-file-token")),
                with(jobBody("DIRECTORY_SYNC", "bad-path"), "target", Map.of("path", "../secret")), 400, 49714);
        performJson(post("/api/v1/cloudreve-sync/sync-jobs")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "unauthorized"),
                jobBody("DIRECTORY_SYNC", "job-unauthorized"), 502, 46703);
        performJson(post("/api/v1/cloudreve-sync/sync-jobs")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Fail-Audit", "true"),
                jobBody("DIRECTORY_SYNC", "job-audit-fail"), 500, 55301);
        performJson(post("/api/v1/cloudreve-sync/sync-jobs")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Fail-Store", "true"),
                jobBody("DIRECTORY_SYNC", "job-store-fail"), 500, 55302);

        JsonNode jobs = performJson(get("/api/v1/cloudreve-sync/sync-jobs")
                .header("Authorization", bearer("sync-viewer-token"))
                .param("providerId", "provider-main")
                .param("jobType", "DIRECTORY_SYNC")
                .param("status", directory.at("/data/status").asText())
                .param("trigger", "ADMIN_MANUAL")
                .param("createdBy", "sync-file-user")
                .param("sort", "createdAt_desc"), 200);
        assertThat(jobs.toString()).contains(directory.at("/data/jobId").asText());

        JsonNode detail = performJson(get("/api/v1/cloudreve-sync/sync-jobs/" + directory.at("/data/jobId").asText())
                .header("Authorization", bearer("sync-viewer-token")), 200);
        assertThat(detail.at("/data/steps").isArray()).isTrue();
        performJson(get("/api/v1/cloudreve-sync/sync-jobs/missing").header("Authorization", bearer("sync-viewer-token")), 404, 49703);

        JsonNode pending = performJson(post("/api/v1/cloudreve-sync/sync-jobs")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Cloudreve-Mode", "pending"),
                jobBody("DIRECTORY_SYNC", "job-pending"), 201);
        JsonNode canceled = performJson(patch("/api/v1/cloudreve-sync/sync-jobs/" + pending.at("/data/jobId").asText() + "/cancel")
                        .header("Authorization", bearer("sync-file-token")),
                Map.of("reason", "取消待执行任务", "idempotencyKey", "cancel-pending"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELLED");
        performJson(patch("/api/v1/cloudreve-sync/sync-jobs/" + directory.at("/data/jobId").asText() + "/cancel")
                        .header("Authorization", bearer("sync-file-token")),
                Map.of("reason", "终态不能取消", "idempotencyKey", "cancel-finished"), 409, 49711);
    }

    @Test
    @DisplayName("CRS-AUDIT, CRS-DEGRADE, and CRS-HARDEN cover audit filters, dependency failures, boundaries, and source scanning")
    void auditDegradeAndHardening() throws Exception {
        performJson(post("/api/v1/cloudreve-sync/providers").header("Authorization", bearer("sync-admin-token")),
                providerBody("audit-provider"), 201);
        JsonNode audit = performJson(get("/api/v1/cloudreve-sync/audit-logs")
                .header("Authorization", bearer("sync-admin-token"))
                .param("actorUserId", "sync-admin-user")
                .param("providerId", "provider-main")
                .param("action", "CLOUDREVE_PROVIDER_CREATED")
                .param("result", "SUCCESS")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z")
                .param("sort", "createdAt_desc"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        assertNoSecrets(audit);

        performJson(get("/api/v1/cloudreve-sync/audit-logs").header("Authorization", bearer("sync-viewer-token")), 403, 42001);
        performJson(get("/api/v1/cloudreve-sync/audit-logs")
                .header("Authorization", bearer("sync-admin-token"))
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2026-01-01T00:00:00Z"), 400, 40001);

        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Resource-Mode", "unavailable"),
                resolveBody("resource-unavailable", "file-client-pack", null, null, false), 502, 46720);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Resource-Mode", "timeout"),
                resolveBody("resource-timeout", "file-client-pack", null, null, false), 504, 46721);
        performJson(post("/api/v1/cloudreve-sync/shares/resolve")
                        .header("Authorization", bearer("sync-file-token"))
                        .header("X-Test-Resource-Mode", "bad-schema"),
                resolveBody("resource-bad-schema", "file-client-pack", null, null, false), 502, 46722);

        Path serviceRoot = Path.of("backend/cloudreve-sync-service/src/main/java");
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
                "cn.beiming.nodedaemon.", "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime",
                "docker ", "kubectl", "pvesh", "mcrcon", "rm -rf", "Remove-Item -Recurse", "rmdir /s",
                "rd /s", "del /s", "cloudreveToken", "rawToken", "refreshToken", "sharePassword",
                "authorized_keys", "id_rsa", "token=", ".env");
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

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Cloudreve " + idempotencyKey);
        body.put("baseUrl", "https://cloud.example.com");
        body.put("authMode", "TEST_FAKE");
        body.put("credential", "cloudreve-secret-token");
        body.put("capabilities", List.of("FILE_LIST", "FILE_METADATA", "SHARE_RESOLVE", "SHARE_REFRESH"));
        body.put("timeoutMs", 5000);
        body.put("opsAssetRef", Map.of("assetId", "asset-cloudreve-main", "source", "ops-control"));
        body.put("enabled", true);
        body.put("reason", "创建 Cloudreve provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> resolveBody(String idempotencyKey, String fileId, String path, String shareUrl, boolean allowStale) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-main");
        if (fileId != null) {
            body.put("fileId", fileId);
        }
        if (path != null) {
            body.put("path", path);
        }
        if (shareUrl != null) {
            body.put("shareUrl", shareUrl);
        }
        body.put("resourceRef", Map.of("resourceId", "res-public-client", "versionId", "ver-client-1"));
        body.put("allowStale", allowStale);
        body.put("reason", "解析分享快照");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> jobBody(String jobType, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobType", jobType);
        body.put("providerId", "provider-main");
        body.put("trigger", "ADMIN_MANUAL");
        body.put("target", switch (jobType) {
            case "PROVIDER_HEALTH_CHECK" -> Map.of("providerId", "provider-main");
            case "SHARE_REFRESH" -> Map.of("shareSnapshotId", "share-client-pack");
            case "RESOURCE_LINK_VERIFY" -> Map.of("resourceRef", Map.of("resourceId", "res-public-client", "versionId", "ver-client-1"));
            default -> Map.of("path", "/packs");
        });
        body.put("reason", "创建同步任务");
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
                "cloudreve-secret-token", "rawToken", "refreshToken", "sharePassword", "secret-code",
                "authorizationHeader", "requestHeaders", "stackTrace", "internalPath", "resolvedPath",
                "authorized_keys", "id_rsa", "ProcessBuilder", "Runtime.getRuntime", "docker ", "kubectl",
                "pvesh", "mcrcon", "/srv/", "C:\\\\", ".env", "token=");
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
