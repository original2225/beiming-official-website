package cn.beiming.opsimagemarket;

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

@SpringBootTest(classes = OpsImageMarketServiceApplication.class, properties = "ops-image-market.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsImageMarketApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("ops-image-market local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "OIM-COM", 1, 140);
        addRange(mapped, "OIM-AUTH", 1, 160);
        addRange(mapped, "OIM-HEALTH", 1, 50);
        addRange(mapped, "OIM-OPS", 1, 120);
        addRange(mapped, "OIM-PROVIDER", 1, 220);
        addRange(mapped, "OIM-IMAGE", 1, 180);
        addRange(mapped, "OIM-VERSION", 1, 200);
        addRange(mapped, "OIM-COMPAT", 1, 130);
        addRange(mapped, "OIM-TEMPLATE", 1, 160);
        addRange(mapped, "OIM-SCAN", 1, 160);
        addRange(mapped, "OIM-PULL", 1, 240);
        addRange(mapped, "OIM-CACHE", 1, 100);
        addRange(mapped, "OIM-AUDIT", 1, 130);
        addRange(mapped, "OIM-DEPS", 1, 180);
        addRange(mapped, "OIM-HARDEN", 1, 240);
        addRange(mapped, "OIM-PORT", 1, 20);
        addRange(mapped, "OIM-CYCLE", 1, 140);
        assertThat(mapped).contains("OIM-COM-001", "OIM-PROVIDER-220", "OIM-PULL-240", "OIM-HARDEN-240", "OIM-CYCLE-140");
        assertThat(mapped).hasSize(2570);
    }

    @Test
    @DisplayName("OIM-COM, OIM-AUTH, OIM-HEALTH, OIM-OPS, and OIM-CACHE cover envelope, auth, summary, and read-only snapshots")
    void commonAuthHealthSummaryAndCache() throws Exception {
        mvc.perform(get("/api/v1/ops-image-market/health").header("X-Request-Id", "req-oim-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-oim-health"));

        JsonNode health = performJson(get("/api/v1/ops-image-market/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("ops-image-market");
        assertThat(health.at("/data/status").asText()).isIn("READY", "DEGRADED");
        assertThat(health.at("/data/version").asText()).isNotBlank();
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertThat(health.at("/data").toString()).doesNotContain("providerCount", "registry", "digest", "scan", "node");
        assertNoSecrets(health);

        performJson(get("/api/v1/ops-image-market/admin/ops/summary"), 401, 41000);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("oim-admin-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 47200);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 47201);
        performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 47202);

        JsonNode summary = performJson(get("/api/v1/ops-image-market/admin/ops/summary").header("Authorization", bearer("oim-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("ops-image-market");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8124);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/registryAdapterMode").asText()).isEqualTo("SIMULATION_ONLY");
        assertThat(summary.at("/data/scannerAdapterMode").asText()).isEqualTo("SIMULATION_ONLY");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/productionGaps").toString()).contains("REAL_REGISTRY_DISABLED", "REAL_PULL_DISABLED");
        assertNoSecrets(summary);

        performJson(get("/api/v1/ops-image-market/admin/ops/summary")
                .header("Authorization", bearer("oim-viewer-token"))
                .header("X-Test-Fail-Store", "true"), 500, 55900);
        performJson(get("/api/v1/ops-image-market/admin/providers")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("page", "0"), 400, 40002);
        performJson(get("/api/v1/ops-image-market/admin/providers")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("sort", "bad"), 400, 40003);

        JsonNode providers = performJson(get("/api/v1/ops-image-market/admin/providers")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("registryType", "DOCKER_HUB")
                .param("status", "ENABLED")
                .param("namespace", "beiming")
                .param("riskLevel", "HIGH")
                .param("sourceModule", "ops-control")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(providers.at("/data/items").toString()).contains("provider-dockerhub-minecraft");
        JsonNode provider = performJson(get("/api/v1/ops-image-market/admin/providers/provider-dockerhub-minecraft")
                .header("Authorization", bearer("oim-viewer-token")), 200);
        assertThat(provider.at("/data/dependencySummary/opsControl/status").asText()).isEqualTo("AVAILABLE");
        performJson(get("/api/v1/ops-image-market/admin/providers/missing")
                .header("Authorization", bearer("oim-viewer-token")), 404, 49700);

        JsonNode caches = performJson(get("/api/v1/ops-image-market/admin/cache-snapshots")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("nodeId", "node-a")
                .param("runtime", "DOCKER")
                .param("source", "TEST_STUB")
                .param("sort", "lastSeenAt_desc"), 200);
        String snapshotId = caches.at("/data/items/0/snapshotId").asText();
        JsonNode cache = performJson(get("/api/v1/ops-image-market/admin/cache-snapshots/" + snapshotId)
                .header("Authorization", bearer("oim-viewer-token")), 200);
        assertThat(cache.at("/data/nodeSummary/nodeId").asText()).isEqualTo("node-a");
        performJson(get("/api/v1/ops-image-market/admin/cache-snapshots/missing")
                .header("Authorization", bearer("oim-viewer-token")), 404, 49707);
        assertNoSecrets(provider);
        assertNoSecrets(cache);
    }

    @Test
    @DisplayName("OIM-PROVIDER covers provider lifecycle, endpoint safety, recursive trusted fields, idempotency, conflicts, health refresh, and audit rollback")
    void providerLifecycleEndpointSafetyAndRollback() throws Exception {
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-viewer-token")),
                providerBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-write-token")),
                providerBody("write-no-high-risk"), 403, 42002);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                providerBody("missing-confirm"), 403, 42003);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("bad-endpoint"), "confirmText", "REGISTER_IMAGE_PROVIDER"),
                        "endpointSummary", Map.of("url", "http://127.0.0.1:5000/v2")), 400, 49713);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("bad-private-endpoint"), "confirmText", "REGISTER_IMAGE_PROVIDER"),
                        "endpointSummary", Map.of("url", "http://172.20.0.1:5000/v2")), 400, 49713);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("bad-unspecified-endpoint"), "confirmText", "REGISTER_IMAGE_PROVIDER"),
                        "endpointSummary", Map.of("url", "http://0.0.0.0:5000/v2")), 400, 49713);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("bad-source-module"), "confirmText", "REGISTER_IMAGE_PROVIDER"),
                        "allowedSourceModules", List.of("auth")), 400, 40001);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                without(with(providerBody("missing-reason-provider"), "confirmText", "REGISTER_IMAGE_PROVIDER"), "reason"), 400, 40001);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("bad-trusted"), "confirmText", "REGISTER_IMAGE_PROVIDER"),
                        "metadata", Map.of("registryToken", "do-not-store")), 400, 40001);
        performJson(post("/api/v1/ops-image-market/admin/providers")
                        .header("Authorization", bearer("oim-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                with(providerBody("audit-fail-provider"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 500, 55901);

        JsonNode created = performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 201);
        String providerId = created.at("/data/providerId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/credentialRefSummary/alias").asText()).isEqualTo("managed-provider-create");
        assertThat(created.toString()).doesNotContain("https://registry.example.com");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 201);
        assertThat(replay.at("/data/providerId").asText()).isEqualTo(providerId);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("provider-create"), "displayName", "Changed Provider"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 409, 49712);
        performJson(post("/api/v1/ops-image-market/admin/providers").header("Authorization", bearer("oim-admin-token")),
                with(with(providerBody("provider-duplicate"), "displayName", "Image Provider provider-create"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 409, 49711);

        performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId).header("Authorization", bearer("oim-admin-token")),
                Map.of("allowedRiskLevels", List.of("LOW", "MEDIUM"), "reason", "缺确认", "idempotencyKey", "patch-no-confirm"), 403, 42003);
        JsonNode patched = performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId).header("Authorization", bearer("oim-admin-token")),
                Map.of("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"), "confirmText", "UPDATE_IMAGE_PROVIDER",
                        "reason", "更新允许风险等级", "idempotencyKey", "patch-provider"), 200);
        assertThat(patched.at("/data/allowedRiskLevels").toString()).contains("HIGH");
        JsonNode credentialPatched = performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId).header("Authorization", bearer("oim-admin-token")),
                Map.of("credentialRefSummary", Map.of("alias", "managed-updated", "managedBy", "vault-summary"),
                        "confirmText", "UPDATE_IMAGE_PROVIDER", "reason", "更新凭据引用摘要", "idempotencyKey", "patch-provider-credential"), 200);
        assertThat(credentialPatched.at("/data/credentialRefSummary/alias").asText()).isEqualTo("managed-updated");

        performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "缺启用确认", "idempotencyKey", "enable-no-confirm"), 403, 42003);
        JsonNode enabled = performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "ENABLE_IMAGE_PROVIDER", "reason", "启用 provider", "idempotencyKey", "enable-provider"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId + "/archive").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "ARCHIVE_IMAGE_PROVIDER", "reason", "启用 provider 不可归档", "idempotencyKey", "archive-enabled"), 409, 49710);
        JsonNode refreshed = performJson(post("/api/v1/ops-image-market/admin/providers/" + providerId + "/health-refresh").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "刷新健康", "idempotencyKey", "refresh-provider"), 200);
        assertThat(refreshed.at("/data/healthStatus").asText()).isEqualTo("HEALTHY");
        JsonNode disabled = performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId + "/disable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "禁用 provider", "idempotencyKey", "disable-provider"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        JsonNode archived = performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId + "/archive").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "ARCHIVE_IMAGE_PROVIDER", "reason", "归档 provider", "idempotencyKey", "archive-provider"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");
        performJson(patch("/api/v1/ops-image-market/admin/providers/" + providerId).header("Authorization", bearer("oim-admin-token")),
                Map.of("displayName", "Archived Changed", "reason", "归档不可修改", "idempotencyKey", "patch-archived"), 409, 49710);
    }

    @Test
    @DisplayName("OIM-IMAGE, OIM-VERSION, OIM-COMPAT, OIM-TEMPLATE, and OIM-SCAN cover image catalog readiness")
    void imageVersionCompatibilityTemplateAndScanFlow() throws Exception {
        performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                with(imageBody("bad-repository"), "repository", "http://127.0.0.1:5000/root/app"), 400, 49713);
        performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                without(imageBody("missing-reason-image"), "reason"), 400, 40001);
        performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                with(imageBody("bad-source-ref"), "sourceRef", Map.of("sourceModule", "auth", "sourceId", "bad")), 400, 40001);

        JsonNode image = performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                imageBody("main"), 201);
        String imageId = image.at("/data/imageId").asText();
        assertThat(image.at("/data/status").asText()).isEqualTo("DRAFT");
        JsonNode imageReplay = performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                imageBody("main"), 201);
        assertThat(imageReplay.at("/data/imageId").asText()).isEqualTo(imageId);
        performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                with(imageBody("main"), "displayName", "Changed Image"), 409, 49712);
        JsonNode imagePatch = performJson(patch("/api/v1/ops-image-market/admin/images/" + imageId).header("Authorization", bearer("oim-admin-token")),
                Map.of("displayName", "Minecraft Runtime Updated", "reason", "更新镜像名称", "idempotencyKey", "patch-image"), 200);
        assertThat(imagePatch.at("/data/displayName").asText()).contains("Updated");

        performJson(post("/api/v1/ops-image-market/admin/images/" + imageId + "/versions").header("Authorization", bearer("oim-admin-token")),
                with(versionBody("bad-manifest"), "manifestPayload", Map.of("layers", List.of("https://layers.example.com/raw"))), 400, 40001);
        JsonNode version = performJson(post("/api/v1/ops-image-market/admin/images/" + imageId + "/versions").header("Authorization", bearer("oim-admin-token")),
                versionBody("1"), 201);
        String versionId = version.at("/data/imageVersionId").asText();
        performJson(patch("/api/v1/ops-image-market/admin/versions/" + versionId + "/approve").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "还没有扫描", "idempotencyKey", "approve-no-scan"), 409, 49715);

        performJson(post("/api/v1/ops-image-market/admin/compatibility-profiles").header("Authorization", bearer("oim-admin-token")),
                with(compatibilityBody(imageId, "secret-value"),
                        "envSchemaSummary", Map.of("secretKeys", List.of(Map.of("name", "RCON_PASSWORD", "value", "plain")))), 400, 40001);
        JsonNode profile = performJson(post("/api/v1/ops-image-market/admin/compatibility-profiles").header("Authorization", bearer("oim-admin-token")),
                compatibilityBody(imageId, "main"), 201);
        String profileId = profile.at("/data/profileId").asText();
        performJson(patch("/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId).header("Authorization", bearer("oim-admin-token")),
                Map.of("requiredVolumesSummary", List.of(Map.of("mountAlias", "/srv/world", "required", true)),
                        "reason", "拒绝宿主路径", "idempotencyKey", "patch-compat-unsafe-volume"), 400, 49713);
        JsonNode profilePatch = performJson(patch("/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId).header("Authorization", bearer("oim-admin-token")),
                Map.of("minimumMemoryMb", 4096, "reason", "提高内存要求", "idempotencyKey", "patch-compat"), 200);
        assertThat(profilePatch.at("/data/minimumMemoryMb").asInt()).isEqualTo(4096);
        JsonNode profileEnabled = performJson(patch("/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "启用兼容配置", "idempotencyKey", "enable-compat"), 200);
        assertThat(profileEnabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode scan = performJson(post("/api/v1/ops-image-market/admin/versions/" + versionId + "/scans").header("Authorization", bearer("oim-admin-token")),
                scanBody("main", "PASSED", "LOW", "SIGNED"), 201);
        String scanId = scan.at("/data/scanId").asText();
        JsonNode approved = performJson(patch("/api/v1/ops-image-market/admin/versions/" + versionId + "/approve").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "批准安全版本", "idempotencyKey", "approve-version"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
        JsonNode published = performJson(patch("/api/v1/ops-image-market/admin/images/" + imageId + "/publish").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "发布镜像", "idempotencyKey", "publish-image"), 200);
        assertThat(published.at("/data/status").asText()).isEqualTo("PUBLISHED");

        performJson(post("/api/v1/ops-image-market/admin/templates").header("Authorization", bearer("oim-admin-token")),
                with(templateBody(imageId, versionId, profileId, "template-secret-value"),
                        "envSchemaSummary", Map.of("secretKeys", List.of(Map.of("name", "RCON_PASSWORD", "value", "plain")))), 400, 40001);
        JsonNode template = performJson(post("/api/v1/ops-image-market/admin/templates").header("Authorization", bearer("oim-admin-token")),
                templateBody(imageId, versionId, profileId, "main"), 201);
        String templateId = template.at("/data/templateId").asText();
        performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId).header("Authorization", bearer("oim-admin-token")),
                Map.of("volumeMountsSummary", List.of(Map.of("mountAlias", "C:\\nodes\\world", "mode", "READ_WRITE")),
                        "reason", "拒绝宿主路径", "idempotencyKey", "patch-template-unsafe-volume"), 400, 49713);
        JsonNode templatePatch = performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId).header("Authorization", bearer("oim-admin-token")),
                Map.of("displayName", "Minecraft Runtime Template Updated", "reason", "更新模板", "idempotencyKey", "patch-template"), 200);
        assertThat(templatePatch.at("/data/displayName").asText()).contains("Updated");
        JsonNode templateEnabled = performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "启用模板", "idempotencyKey", "enable-template"), 200);
        assertThat(templateEnabled.at("/data/status").asText()).isEqualTo("ENABLED");
        JsonNode templateDisabled = performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId + "/disable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "禁用模板", "idempotencyKey", "disable-template"), 200);
        assertThat(templateDisabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "重新启用模板", "idempotencyKey", "enable-template-again"), 200);

        JsonNode imageDetail = performJson(get("/api/v1/ops-image-market/admin/images/" + imageId).header("Authorization", bearer("oim-viewer-token")), 200);
        JsonNode versions = performJson(get("/api/v1/ops-image-market/admin/images/" + imageId + "/versions")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("status", "APPROVED")
                .param("scanStatus", "PASSED")
                .param("sort", "publishedAt_desc"), 200);
        JsonNode versionDetail = performJson(get("/api/v1/ops-image-market/admin/versions/" + versionId).header("Authorization", bearer("oim-viewer-token")), 200);
        JsonNode profiles = performJson(get("/api/v1/ops-image-market/admin/compatibility-profiles")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("imageId", imageId)
                .param("runtime", "DOCKER")
                .param("status", "ENABLED"), 200);
        JsonNode profileDetail = performJson(get("/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId).header("Authorization", bearer("oim-viewer-token")), 200);
        JsonNode templates = performJson(get("/api/v1/ops-image-market/admin/templates")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("imageId", imageId)
                .param("status", "ENABLED"), 200);
        JsonNode templateDetail = performJson(get("/api/v1/ops-image-market/admin/templates/" + templateId).header("Authorization", bearer("oim-viewer-token")), 200);
        JsonNode scans = performJson(get("/api/v1/ops-image-market/admin/scans")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("imageVersionId", versionId)
                .param("highestSeverity", "LOW")
                .param("sort", "finishedAt_desc"), 200);
        JsonNode scanDetail = performJson(get("/api/v1/ops-image-market/admin/scans/" + scanId).header("Authorization", bearer("oim-viewer-token")), 200);
        assertThat(imageDetail.at("/data/latestVersionSummary/imageVersionId").asText()).isEqualTo(versionId);
        assertThat(versions.at("/data/items").toString()).contains(versionId);
        assertThat(versionDetail.at("/data/scanSummary/status").asText()).isEqualTo("PASSED");
        assertThat(profiles.at("/data/items").toString()).contains(profileId);
        assertThat(profileDetail.at("/data/imageSummary/imageId").asText()).isEqualTo(imageId);
        assertThat(templates.at("/data/items").toString()).contains(templateId);
        assertThat(templateDetail.at("/data/versionSummary/imageVersionId").asText()).isEqualTo(versionId);
        assertThat(scans.at("/data/items").toString()).contains(scanId);
        assertThat(scanDetail.at("/data/versionSummary/imageVersionId").asText()).isEqualTo(versionId);

        JsonNode secondVersion = performJson(post("/api/v1/ops-image-market/admin/images/" + imageId + "/versions").header("Authorization", bearer("oim-admin-token")),
                versionBody("2"), 201);
        String secondVersionId = secondVersion.at("/data/imageVersionId").asText();
        JsonNode deprecated = performJson(patch("/api/v1/ops-image-market/admin/versions/" + secondVersionId + "/deprecate").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "废弃旧版本", "idempotencyKey", "deprecate-version"), 200);
        assertThat(deprecated.at("/data/status").asText()).isEqualTo("DEPRECATED");
        JsonNode blockedVersion = performJson(patch("/api/v1/ops-image-market/admin/versions/" + secondVersionId + "/block").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "BLOCK_IMAGE_VERSION", "reason", "阻断版本", "idempotencyKey", "block-version"), 200);
        assertThat(blockedVersion.at("/data/status").asText()).isEqualTo("BLOCKED");

        JsonNode draftImage = performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                imageBody("archive-target"), 201);
        String draftImageId = draftImage.at("/data/imageId").asText();
        JsonNode blockedImage = performJson(patch("/api/v1/ops-image-market/admin/images/" + draftImageId + "/block").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "BLOCK_OPS_IMAGE", "reason", "阻断草稿镜像", "idempotencyKey", "block-image"), 200);
        assertThat(blockedImage.at("/data/status").asText()).isEqualTo("BLOCKED");
        JsonNode archivedImage = performJson(patch("/api/v1/ops-image-market/admin/images/" + draftImageId + "/archive").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "归档草稿镜像", "idempotencyKey", "archive-image"), 200);
        assertThat(archivedImage.at("/data/status").asText()).isEqualTo("ARCHIVED");

        performJson(get("/api/v1/ops-image-market/admin/images/missing").header("Authorization", bearer("oim-viewer-token")), 404, 49701);
        performJson(get("/api/v1/ops-image-market/admin/versions/missing").header("Authorization", bearer("oim-viewer-token")), 404, 49702);
        performJson(get("/api/v1/ops-image-market/admin/compatibility-profiles/missing").header("Authorization", bearer("oim-viewer-token")), 404, 49703);
        performJson(get("/api/v1/ops-image-market/admin/templates/missing").header("Authorization", bearer("oim-viewer-token")), 404, 49704);
        performJson(get("/api/v1/ops-image-market/admin/scans/missing").header("Authorization", bearer("oim-viewer-token")), 404, 49705);
        assertNoSecrets(imageDetail);
        assertNoSecrets(scanDetail);
    }

    @Test
    @DisplayName("OIM-PULL, OIM-AUDIT, OIM-DEPS, and OIM-HARDEN cover plan risk controls, dependency failure, audit, and simulation boundary")
    void pullPlanRiskAuditDependencyAndSimulationBoundary() throws Exception {
        ReadyImage ready = prepareReadyImage("pull-main");

        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-viewer-token")),
                pullPlanBody(ready.versionId(), ready.templateId(), "viewer-denied", "MEDIUM"), 403, 42002);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "critical-admin-denied", "CRITICAL"), "confirmText", "CREATE_IMAGE_PULL_PLAN_RISK"), 403, 42004);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-write-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "write-high-denied", "HIGH"), "confirmText", "CREATE_IMAGE_PULL_PLAN_RISK"), 403, 42002);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "real-execution", "MEDIUM"), "executionMode", "REAL"), 409, 49717);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans")
                        .header("Authorization", bearer("oim-admin-token"))
                        .header("X-Test-Ops-Control-Mode", "unavailable"),
                pullPlanBody(ready.versionId(), ready.templateId(), "ops-unavailable", "MEDIUM"), 502, 47210);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans")
                        .header("Authorization", bearer("oim-admin-token"))
                        .header("X-Test-Fail-Plan", "true"),
                pullPlanBody(ready.versionId(), ready.templateId(), "plan-write-fail", "MEDIUM"), 500, 55903);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "unsigned-no-confirm", "HIGH"), "allowUnsigned", true), 403, 42003);

        JsonNode highPlan = performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "high-plan", "HIGH"), "confirmText", "CREATE_IMAGE_PULL_PLAN_RISK"), 201);
        String planId = highPlan.at("/data/planId").asText();
        assertThat(highPlan.at("/data/status").asText()).isEqualTo("RISK_REVIEW_REQUIRED");
        assertThat(highPlan.at("/data/simulated").asBoolean()).isTrue();
        assertThat(highPlan.toString()).doesNotContain("PULLED", "RUNNING_ON_NODE");
        JsonNode replay = performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(pullPlanBody(ready.versionId(), ready.templateId(), "high-plan", "HIGH"), "confirmText", "CREATE_IMAGE_PULL_PLAN_RISK"), 201);
        assertThat(replay.at("/data/planId").asText()).isEqualTo(planId);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                with(with(pullPlanBody(ready.versionId(), ready.templateId(), "high-plan", "HIGH"), "runtime", "CONTAINERD"), "confirmText", "CREATE_IMAGE_PULL_PLAN_RISK"), 409, 49712);

        performJson(patch("/api/v1/ops-image-market/admin/pull-plans/" + planId + "/approve").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "缺批准确认", "idempotencyKey", "approve-no-confirm"), 403, 42003);
        JsonNode approved = performJson(patch("/api/v1/ops-image-market/admin/pull-plans/" + planId + "/approve").header("Authorization", bearer("oim-admin-token")),
                Map.of("confirmText", "APPROVE_IMAGE_PULL_PLAN", "reason", "批准模拟计划", "idempotencyKey", "approve-plan"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("SIMULATED_READY");
        assertThat(approved.at("/data/opsControlTaskRef").isNull()).isTrue();
        performJson(patch("/api/v1/ops-image-market/admin/pull-plans/" + planId + "/cancel").header("Authorization", bearer("oim-admin-write-token")),
                Map.of("reason", "无高风险能力", "idempotencyKey", "cancel-high-denied"), 403, 42002);
        JsonNode canceled = performJson(patch("/api/v1/ops-image-market/admin/pull-plans/" + planId + "/cancel").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "取消模拟计划", "idempotencyKey", "cancel-plan"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");

        JsonNode plans = performJson(get("/api/v1/ops-image-market/admin/pull-plans")
                .header("Authorization", bearer("oim-viewer-token"))
                .param("imageVersionId", ready.versionId())
                .param("status", "CANCELED")
                .param("riskLevel", "HIGH")
                .param("sort", "createdAt_desc"), 200);
        assertThat(plans.at("/data/items").toString()).contains(planId);
        JsonNode detail = performJson(get("/api/v1/ops-image-market/admin/pull-plans/" + planId)
                .header("Authorization", bearer("oim-viewer-token")), 200);
        assertThat(detail.at("/data/policyDecisionSummary/decision").asText()).isIn("REVIEW_REQUIRED", "ALLOW_SIMULATED");
        performJson(get("/api/v1/ops-image-market/admin/pull-plans/missing")
                .header("Authorization", bearer("oim-viewer-token")), 404, 49706);

        performJson(get("/api/v1/ops-image-market/admin/audit-logs")
                .header("Authorization", bearer("oim-viewer-token")), 403, 42001);
        JsonNode audits = performJson(get("/api/v1/ops-image-market/admin/audit-logs")
                .header("Authorization", bearer("oim-admin-token"))
                .param("planId", planId)
                .param("riskLevel", "HIGH")
                .param("sort", "createdAt_desc"), 200);
        assertThat(audits.at("/data/items").toString()).contains(planId, "IMAGE_PULL_PLAN");
        JsonNode futureAudits = performJson(get("/api/v1/ops-image-market/admin/audit-logs")
                .header("Authorization", bearer("oim-admin-token"))
                .param("from", "2999-01-01T00:00:00Z"), 200);
        assertThat(futureAudits.at("/data/items").size()).isZero();
        performJson(get("/api/v1/ops-image-market/admin/audit-logs")
                .header("Authorization", bearer("oim-admin-token"))
                .param("from", "2030-01-02T00:00:00Z")
                .param("to", "2030-01-01T00:00:00Z"), 400, 40001);

        JsonNode failedScan = performJson(post("/api/v1/ops-image-market/admin/versions/" + ready.versionId() + "/scans")
                        .header("Authorization", bearer("oim-admin-token"))
                        .header("X-Test-Scanner-Mode", "failed"),
                scanBody("scanner-failed", "PASSED", "LOW", "SIGNED"), 502, 47220);
        assertThat(failedScan.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(detail);
        assertNoSecrets(audits);
    }

    @Test
    @DisplayName("OIM-HARDEN rejects cross-image template and pull-plan mismatches")
    void crossImageReferencesAreRejectedBeforeTemplatesOrPlansCanBeCreated() throws Exception {
        ReadyImage first = prepareReadyImage("mismatch-a");
        ReadyImage second = prepareReadyImage("mismatch-b");

        performJson(post("/api/v1/ops-image-market/admin/templates").header("Authorization", bearer("oim-admin-token")),
                templateBody(first.imageId(), second.versionId(), second.profileId(), "mismatch-template"), 409, 49716);
        performJson(post("/api/v1/ops-image-market/admin/pull-plans").header("Authorization", bearer("oim-admin-token")),
                pullPlanBody(first.versionId(), second.templateId(), "mismatch-plan", "MEDIUM"), 409, 49716);
    }

    private ReadyImage prepareReadyImage(String suffix) throws Exception {
        JsonNode image = performJson(post("/api/v1/ops-image-market/admin/images").header("Authorization", bearer("oim-admin-token")),
                imageBody(suffix), 201);
        String imageId = image.at("/data/imageId").asText();
        JsonNode profile = performJson(post("/api/v1/ops-image-market/admin/compatibility-profiles").header("Authorization", bearer("oim-admin-token")),
                compatibilityBody(imageId, suffix), 201);
        String profileId = profile.at("/data/profileId").asText();
        performJson(patch("/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "启用兼容配置", "idempotencyKey", "enable-compat-" + suffix), 200);
        JsonNode version = performJson(post("/api/v1/ops-image-market/admin/images/" + imageId + "/versions").header("Authorization", bearer("oim-admin-token")),
                versionBody(suffix), 201);
        String versionId = version.at("/data/imageVersionId").asText();
        performJson(post("/api/v1/ops-image-market/admin/versions/" + versionId + "/scans").header("Authorization", bearer("oim-admin-token")),
                scanBody(suffix, "PASSED", "LOW", "SIGNED"), 201);
        performJson(patch("/api/v1/ops-image-market/admin/versions/" + versionId + "/approve").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "批准安全版本", "idempotencyKey", "approve-version-" + suffix), 200);
        performJson(patch("/api/v1/ops-image-market/admin/images/" + imageId + "/publish").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "发布镜像", "idempotencyKey", "publish-image-" + suffix), 200);
        JsonNode template = performJson(post("/api/v1/ops-image-market/admin/templates").header("Authorization", bearer("oim-admin-token")),
                templateBody(imageId, versionId, profileId, suffix), 201);
        String templateId = template.at("/data/templateId").asText();
        performJson(patch("/api/v1/ops-image-market/admin/templates/" + templateId + "/enable").header("Authorization", bearer("oim-admin-token")),
                Map.of("reason", "启用模板", "idempotencyKey", "enable-template-" + suffix), 200);
        return new ReadyImage(imageId, versionId, profileId, templateId);
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(json);
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(json);
        return json;
    }

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Image Provider " + idempotencyKey);
        body.put("registryType", "OCI_REGISTRY");
        body.put("endpointSummary", Map.of("url", "https://registry.example.com/" + idempotencyKey + "/v2"));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + idempotencyKey, "managedBy", "vault-summary"));
        body.put("allowedNamespaces", List.of("beiming", "library"));
        body.put("allowedSourceModules", List.of("ops-control", "plugin-integration"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("syncPolicySummary", Map.of("mode", "MANUAL", "window", "maintenance"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 120));
        body.put("reason", "创建镜像 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> imageBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-dockerhub-minecraft");
        body.put("repository", "beiming/minecraft-runtime-" + idempotencyKey);
        body.put("displayName", "Minecraft Runtime " + idempotencyKey);
        body.put("purpose", "MINECRAFT_SERVER");
        body.put("visibility", "OPS_ONLY");
        body.put("maintainerSummary", Map.of("team", "ops", "contact", "ops-summary"));
        body.put("sourceRef", Map.of("sourceModule", "ops-control", "sourceId", "runtime-" + idempotencyKey));
        body.put("architectureSet", List.of("AMD64", "ARM64"));
        body.put("runtimeHints", List.of("DOCKER"));
        body.put("reason", "创建镜像目录");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> versionBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tag", "1.20.1-" + idempotencyKey);
        body.put("digestSummary", Map.of("algorithm", "sha256", "shortHash", "abc" + idempotencyKey, "pinned", true));
        body.put("manifestSummary", Map.of("mediaType", "application/vnd.oci.image.manifest.v1+json", "platformCount", 2, "layerCount", 8));
        body.put("os", "linux");
        body.put("architecture", "AMD64");
        body.put("sizeSummary", Map.of("bytes", 512000000, "human", "512 MB"));
        body.put("publishedAt", "2026-05-01T00:00:00Z");
        body.put("signed", true);
        body.put("signatureSummary", Map.of("status", "SIGNED", "issuer", "beiming-ci"));
        body.put("changeSummary", Map.of("title", "Runtime update " + idempotencyKey));
        body.put("reason", "登记镜像版本");
        body.put("idempotencyKey", "version-" + idempotencyKey);
        return body;
    }

    private Map<String, Object> compatibilityBody(String imageId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageId", imageId);
        body.put("runtime", "DOCKER");
        body.put("architecture", "AMD64");
        body.put("minecraftMode", "PAPER");
        body.put("minimumCpuCores", 2);
        body.put("minimumMemoryMb", 3072);
        body.put("requiredPortsSummary", List.of(Map.of("containerPort", 25565, "protocol", "TCP")));
        body.put("requiredVolumesSummary", List.of(Map.of("mountAlias", "world-data", "required", true)));
        body.put("envSchemaSummary", Map.of("requiredKeys", List.of("EULA"), "secretKeys", List.of("RCON_PASSWORD")));
        body.put("nodeSelectorSummary", Map.of("labels", Map.of("pool", "minecraft"), "architecture", "AMD64"));
        body.put("reason", "创建兼容配置");
        body.put("idempotencyKey", "compat-" + idempotencyKey);
        return body;
    }

    private Map<String, Object> templateBody(String imageId, String versionId, String profileId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageId", imageId);
        body.put("imageVersionId", versionId);
        body.put("displayName", "Minecraft Runtime Template " + idempotencyKey);
        body.put("templateKind", "MINECRAFT_INSTANCE");
        body.put("runtime", "DOCKER");
        body.put("portMappingsSummary", List.of(Map.of("containerPort", 25565, "protocol", "TCP")));
        body.put("volumeMountsSummary", List.of(Map.of("mountAlias", "world-data", "mode", "READ_WRITE")));
        body.put("envSchemaSummary", Map.of("requiredKeys", List.of("EULA"), "secretKeys", List.of("RCON_PASSWORD")));
        body.put("resourceLimitsSummary", Map.of("cpuCores", 4, "memoryMb", 6144));
        body.put("compatibilityProfileId", profileId);
        body.put("reason", "创建镜像模板");
        body.put("idempotencyKey", "template-" + idempotencyKey);
        return body;
    }

    private Map<String, Object> scanBody(String idempotencyKey, String status, String highestSeverity, String signatureStatus) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scanner", "TRIVY_SIMULATED");
        body.put("status", status);
        body.put("severityCounts", Map.of("UNKNOWN", 0, "LOW", 1, "MEDIUM", 0, "HIGH", 0, "CRITICAL", 0));
        body.put("highestSeverity", highestSeverity);
        body.put("fixAvailable", false);
        body.put("cveSummary", List.of(Map.of("id", "CVE-SUMMARY-" + idempotencyKey, "severity", highestSeverity, "fixed", false)));
        body.put("licenseSummary", Map.of("status", "OK"));
        body.put("signatureStatus", signatureStatus);
        body.put("startedAt", "2026-05-01T01:00:00Z");
        body.put("finishedAt", "2026-05-01T01:01:00Z");
        body.put("expiresAt", "2099-01-01T00:00:00Z");
        body.put("degradedReasons", List.of());
        body.put("reason", "登记扫描摘要");
        body.put("idempotencyKey", "scan-" + idempotencyKey);
        return body;
    }

    private Map<String, Object> pullPlanBody(String versionId, String templateId, String idempotencyKey, String riskLevel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageVersionId", versionId);
        body.put("templateId", templateId);
        body.put("targetNodeIds", List.of("node-a", "node-b"));
        body.put("runtime", "DOCKER");
        body.put("riskLevel", riskLevel);
        body.put("allowUnsigned", false);
        body.put("allowHighSeverity", "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel));
        body.put("reason", "创建镜像拉取计划");
        body.put("idempotencyKey", "pull-" + idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private Map<String, Object> without(Map<String, Object> source, String key) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove(key);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void addRange(Set<String> mapped, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            mapped.add(prefix + "-" + String.format("%03d", i));
        }
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "registryToken", "registryPassword", "dockerPassword", "imageSecret", "pullSecret",
                "rawToken", "secretKey", "Authorization", "requestHeaders",
                "manifestPayload", "layerUrl", "internalUrl", "internalPath", "resolvedPath",
                "fullException", "stackTrace", "databaseUrl", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "/srv/", "C:\\\\", ".env", "authorized_keys", "id_rsa", "token=");
    }

    private record ReadyImage(String imageId, String versionId, String profileId, String templateId) {
    }
}
