package cn.beiming.opsimagemarket;

import cn.beiming.opscore.OpsCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpsCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ops-image-market.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsImageMarketRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "oim-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:ops_image_market_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            for (String table : List.of(
                    "oim_flow_providers",
                    "oim_flow_images",
                    "oim_flow_versions",
                    "oim_flow_profiles",
                    "oim_flow_templates",
                    "oim_flow_scans",
                    "oim_flow_plans",
                    "oim_flow_audits",
                    "oim_flow_request_log"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void providerLifecycleRunsThroughBackendAndDatabaseThenReturnsArchivedProvider() throws Exception {
        String providerId = "provider-" + randomKey();

        JsonNode created = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/providers",
                bearerHeaders("oim-admin-token", "req-oim-provider-create-" + FLOW_ID),
                providerBody(providerId, "create"),
                201
        );
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        providerId = created.at("/data/providerId").asText();

        JsonNode patched = requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/providers/" + providerId,
                bearerHeaders("oim-admin-token", "req-oim-provider-patch-" + FLOW_ID),
                providerPatchBody("provider-patch-" + randomKey()),
                200
        );
        assertThat(patched.at("/data/displayName").asText()).isEqualTo("OIM Provider patched");

        JsonNode enabled = requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/providers/" + providerId + "/enable",
                bearerHeaders("oim-admin-token", "req-oim-provider-enable-" + FLOW_ID),
                Map.of("confirmText", "ENABLE_IMAGE_PROVIDER", "reason", "启用 provider", "idempotencyKey", "provider-enable-" + randomKey()),
                200
        );
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode refreshed = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/providers/" + providerId + "/health-refresh",
                bearerHeaders("oim-admin-token", "req-oim-provider-refresh-" + FLOW_ID),
                Map.of("reason", "刷新 provider 健康", "idempotencyKey", "provider-refresh-" + randomKey()),
                200
        );
        assertThat(refreshed.at("/data/healthStatus").asText()).isEqualTo("HEALTHY");

        JsonNode disabled = requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/providers/" + providerId + "/disable",
                bearerHeaders("oim-admin-token", "req-oim-provider-disable-" + FLOW_ID),
                Map.of("reason", "禁用 provider", "idempotencyKey", "provider-disable-" + randomKey()),
                200
        );
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");

        JsonNode archived = requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/providers/" + providerId + "/archive",
                bearerHeaders("oim-admin-token", "req-oim-provider-archive-" + FLOW_ID),
                Map.of("confirmText", "ARCHIVE_IMAGE_PROVIDER", "reason", "归档 provider", "idempotencyKey", "provider-archive-" + randomKey()),
                200
        );
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT display_name FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_UPDATED'",
                    FLOW_ID, providerId, "OIM Provider patched");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_ENABLED'",
                    FLOW_ID, providerId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT health_status FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_HEALTH_REFRESHED'",
                    FLOW_ID, providerId, "HEALTHY");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_DISABLED'",
                    FLOW_ID, providerId, "DISABLED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'IMAGE_PROVIDER_ARCHIVED'",
                    FLOW_ID, providerId, "ARCHIVED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM oim_flow_audits WHERE flow_id = ?",
                    FLOW_ID, 6L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM oim_flow_request_log WHERE flow_id = ?",
                    FLOW_ID, 6L);
        }
        System.out.println("SQL evidence: ops-image-market provider lifecycle reached backend, wrote provider/audit/request rows, and returned 200/201.");
    }

    @Test
    void imageVersionProfileTemplateScanAndPlanRunThroughBackendAndDatabaseThenReturnSimulatedPlan() throws Exception {
        String imageId = createImage("provider-dockerhub-minecraft", "image-" + randomKey());
        String profileId = createProfile(imageId, "profile-" + randomKey());
        String versionId = createVersion(imageId, "version-" + randomKey());

        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/compatibility-profiles/" + profileId + "/enable",
                bearerHeaders("oim-admin-token", "req-oim-profile-enable-" + FLOW_ID),
                Map.of("reason", "启用兼容配置", "idempotencyKey", "profile-enable-" + randomKey()),
                200
        );

        requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/versions/" + versionId + "/scans",
                bearerHeaders("oim-admin-token", "req-oim-scan-create-" + FLOW_ID),
                scanBody(versionId, "scan-" + randomKey()),
                201
        );
        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/versions/" + versionId + "/approve",
                bearerHeaders("oim-admin-token", "req-oim-version-approve-" + FLOW_ID),
                Map.of("reason", "批准版本", "idempotencyKey", "version-approve-" + randomKey()),
                200
        );
        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/images/" + imageId + "/publish",
                bearerHeaders("oim-admin-token", "req-oim-image-publish-" + FLOW_ID),
                Map.of("reason", "发布镜像", "idempotencyKey", "image-publish-" + randomKey()),
                200
        );
        String templateId = createTemplate(imageId, versionId, profileId, "template-" + randomKey());
        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/templates/" + templateId + "/enable",
                bearerHeaders("oim-admin-token", "req-oim-template-enable-" + FLOW_ID),
                Map.of("reason", "启用模板", "idempotencyKey", "template-enable-" + randomKey()),
                200
        );

        JsonNode plan = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/pull-plans",
                bearerHeaders("oim-admin-token", "req-oim-plan-create-" + FLOW_ID),
                pullPlanBody(versionId, templateId, "plan-" + randomKey()),
                201
        );
        assertThat(plan.at("/data/status").asText()).isEqualTo("RISK_REVIEW_REQUIRED");
        String planId = plan.at("/data/planId").asText();

        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/pull-plans/" + planId + "/approve",
                bearerHeaders("oim-admin-token", "req-oim-plan-approve-" + FLOW_ID),
                Map.of("confirmText", "APPROVE_IMAGE_PULL_PLAN", "reason", "审批拉取计划", "idempotencyKey", "plan-approve-" + randomKey()),
                200
        );
        requestJson(
                HttpMethod.PATCH,
                "/api/v1/ops-image-market/admin/pull-plans/" + planId + "/cancel",
                bearerHeaders("oim-admin-token", "req-oim-plan-cancel-" + FLOW_ID),
                Map.of("reason", "取消拉取计划", "idempotencyKey", "plan-cancel-" + randomKey()),
                200
        );

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_images WHERE flow_id = ? AND image_id = ? AND action = 'OPS_IMAGE_CREATED'",
                    FLOW_ID, imageId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_profiles WHERE flow_id = ? AND profile_id = ? AND action = 'IMAGE_COMPAT_PROFILE_CREATED'",
                    FLOW_ID, profileId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_profiles WHERE flow_id = ? AND profile_id = ? AND action = 'IMAGE_COMPAT_PROFILE_ENABLED'",
                    FLOW_ID, profileId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_versions WHERE flow_id = ? AND version_id = ? AND action = 'IMAGE_VERSION_CREATED'",
                    FLOW_ID, versionId, "DISCOVERED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_versions WHERE flow_id = ? AND version_id = ? AND action = 'IMAGE_VERSION_APPROVED'",
                    FLOW_ID, versionId, "APPROVED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_scans WHERE flow_id = ? AND image_version_id = ? AND action = 'IMAGE_SCAN_SUMMARY_CREATED'",
                    FLOW_ID, versionId, "PASSED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_templates WHERE flow_id = ? AND template_id = ? AND action = 'IMAGE_TEMPLATE_CREATED'",
                    FLOW_ID, templateId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_templates WHERE flow_id = ? AND template_id = ? AND action = 'IMAGE_TEMPLATE_ENABLED'",
                    FLOW_ID, templateId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_plans WHERE flow_id = ? AND plan_id = ? AND action = 'IMAGE_PULL_PLAN_CREATED'",
                    FLOW_ID, planId, "RISK_REVIEW_REQUIRED");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_plans WHERE flow_id = ? AND plan_id = ? AND action = 'IMAGE_PULL_PLAN_APPROVED'",
                    FLOW_ID, planId, "SIMULATED_READY");
            assertSingleValue(connection,
                    "SELECT status FROM oim_flow_plans WHERE flow_id = ? AND plan_id = ? AND action = 'IMAGE_PULL_PLAN_CANCELED'",
                    FLOW_ID, planId, "CANCELED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM oim_flow_audits WHERE flow_id = ?",
                    FLOW_ID, 12L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM oim_flow_request_log WHERE flow_id = ?",
                    FLOW_ID, 12L);
        }
        System.out.println("SQL evidence: ops-image-market image/version/profile/template/scan/plan flow reached backend, wrote database rows, and returned 200/201.");
    }

    private JsonNode requestJson(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body, int status) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(status);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private Map<String, Object> providerBody(String providerId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "OIM Provider " + providerId);
        body.put("registryType", "OCI_REGISTRY");
        body.put("endpointSummary", Map.of("url", "https://registry.example.com/" + providerId + "/v2"));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + providerId, "managedBy", "vault-summary"));
        body.put("allowedNamespaces", List.of("beiming", "library"));
        body.put("allowedSourceModules", List.of("ops-control", "plugin-integration"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("syncPolicySummary", Map.of("mode", "MANUAL", "window", "maintenance"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 120));
        body.put("confirmText", "REGISTER_IMAGE_PROVIDER");
        body.put("reason", "创建 OIM provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> providerPatchBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "OIM Provider patched");
        body.put("reason", "更新 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String createImage(String providerId, String idempotencyKey) throws Exception {
        JsonNode image = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/images",
                bearerHeaders("oim-admin-token", "req-oim-image-create-" + FLOW_ID),
                imageBody(providerId, idempotencyKey),
                201
        );
        return image.at("/data/imageId").asText();
    }

    private String createProfile(String imageId, String idempotencyKey) throws Exception {
        JsonNode profile = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/compatibility-profiles",
                bearerHeaders("oim-admin-token", "req-oim-profile-create-" + FLOW_ID),
                profileBody(imageId, idempotencyKey),
                201
        );
        return profile.at("/data/profileId").asText();
    }

    private String createVersion(String imageId, String idempotencyKey) throws Exception {
        JsonNode version = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/images/" + imageId + "/versions",
                bearerHeaders("oim-admin-token", "req-oim-version-create-" + FLOW_ID),
                versionBody(idempotencyKey),
                201
        );
        return version.at("/data/imageVersionId").asText();
    }

    private String createTemplate(String imageId, String versionId, String profileId, String idempotencyKey) throws Exception {
        JsonNode template = requestJson(
                HttpMethod.POST,
                "/api/v1/ops-image-market/admin/templates",
                bearerHeaders("oim-admin-token", "req-oim-template-create-" + FLOW_ID),
                templateBody(imageId, versionId, profileId, idempotencyKey),
                201
        );
        return template.at("/data/templateId").asText();
    }

    private Map<String, Object> imageBody(String providerId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", providerId);
        body.put("repository", "beiming/minecraft-runtime-" + idempotencyKey);
        body.put("displayName", "OIM Image " + idempotencyKey);
        body.put("purpose", "MINECRAFT_SERVER");
        body.put("visibility", "OPS_ONLY");
        body.put("maintainerSummary", Map.of("team", "ops", "contact", "ops-summary"));
        body.put("sourceRef", Map.of("sourceModule", "ops-control", "sourceId", "runtime-" + idempotencyKey));
        body.put("architectureSet", List.of("AMD64", "ARM64"));
        body.put("runtimeHints", List.of("DOCKER"));
        body.put("reason", "创建 OIM 镜像");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> profileBody(String imageId, String idempotencyKey) {
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
        body.put("reason", "创建 OIM 兼容配置");
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
        body.put("reason", "登记 OIM 版本");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> scanBody(String versionId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scanner", "TRIVY_SIMULATED");
        body.put("status", "PASSED");
        body.put("severityCounts", Map.of("UNKNOWN", 0, "LOW", 1, "MEDIUM", 0, "HIGH", 0, "CRITICAL", 0));
        body.put("highestSeverity", "LOW");
        body.put("fixAvailable", false);
        body.put("cveSummary", List.of(Map.of("id", "CVE-SUMMARY-" + idempotencyKey, "severity", "LOW", "fixed", false)));
        body.put("licenseSummary", Map.of("status", "OK"));
        body.put("signatureStatus", "SIGNED");
        body.put("startedAt", "2026-05-01T01:00:00Z");
        body.put("finishedAt", "2026-05-01T01:01:00Z");
        body.put("expiresAt", "2099-01-01T00:00:00Z");
        body.put("degradedReasons", List.of());
        body.put("reason", "登记 OIM 扫描");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> templateBody(String imageId, String versionId, String profileId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageId", imageId);
        body.put("imageVersionId", versionId);
        body.put("displayName", "OIM Template " + idempotencyKey);
        body.put("templateKind", "MINECRAFT_INSTANCE");
        body.put("runtime", "DOCKER");
        body.put("portMappingsSummary", List.of(Map.of("containerPort", 25565, "protocol", "TCP")));
        body.put("volumeMountsSummary", List.of(Map.of("mountAlias", "world-data", "mode", "READ_WRITE")));
        body.put("envSchemaSummary", Map.of("requiredKeys", List.of("EULA"), "secretKeys", List.of("RCON_PASSWORD")));
        body.put("resourceLimitsSummary", Map.of("cpuCores", 4, "memoryMb", 6144));
        body.put("compatibilityProfileId", profileId);
        body.put("reason", "创建 OIM 模板");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> pullPlanBody(String versionId, String templateId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageVersionId", versionId);
        body.put("templateId", templateId);
        body.put("targetNodeIds", List.of("node-a", "node-b"));
        body.put("runtime", "DOCKER");
        body.put("riskLevel", "HIGH");
        body.put("allowUnsigned", false);
        body.put("allowHighSeverity", true);
        body.put("confirmText", "CREATE_IMAGE_PULL_PLAN_RISK");
        body.put("reason", "创建 OIM 拉取计划");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_providers (
                    flow_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    display_name VARCHAR(256) NOT NULL,
                    health_status VARCHAR(32),
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_images (
                    flow_id VARCHAR(128) NOT NULL,
                    image_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    display_name VARCHAR(256) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_versions (
                    flow_id VARCHAR(128) NOT NULL,
                    version_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    tag VARCHAR(128) NOT NULL,
                    image_id VARCHAR(128) NOT NULL,
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_profiles (
                    flow_id VARCHAR(128) NOT NULL,
                    profile_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    image_id VARCHAR(128) NOT NULL,
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_templates (
                    flow_id VARCHAR(128) NOT NULL,
                    template_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    image_id VARCHAR(128) NOT NULL,
                    image_version_id VARCHAR(128) NOT NULL,
                    updated_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_scans (
                    flow_id VARCHAR(128) NOT NULL,
                    scan_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    image_version_id VARCHAR(128) NOT NULL,
                    highest_severity VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_plans (
                    flow_id VARCHAR(128) NOT NULL,
                    plan_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    risk_level VARCHAR(32) NOT NULL,
                    approval_status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_type VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS oim_flow_request_log (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    path VARCHAR(256) NOT NULL,
                    response_code INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    private static void deleteFlowRows(Statement statement, String table) throws Exception {
        statement.executeUpdate("DELETE FROM " + table + " WHERE flow_id = '" + FLOW_ID + "'");
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object third, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            statement.setObject(3, third);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        OpsImageMarketFlowEvidenceRecorder opsImageMarketFlowEvidenceRecorder() {
            return new JdbcOpsImageMarketFlowEvidenceRecorder();
        }
    }

    static class JdbcOpsImageMarketFlowEvidenceRecorder implements OpsImageMarketFlowEvidenceRecorder {
        @Override
        public void recordProviderWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_providers(flow_id, provider_id, action, status, display_name, health_status, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("providerId"),
                        action,
                        payload.get("status"),
                        payload.get("displayName"),
                        payload.get("healthStatus"),
                        firstNonBlank(payload.get("updatedBy"), payload.get("createdBy"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "PROVIDER",
                        payload.get("providerId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordImageWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_images(flow_id, image_id, action, status, display_name, provider_id, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("imageId"),
                        action,
                        payload.get("status"),
                        payload.get("displayName"),
                        payload.get("providerId"),
                        firstNonBlank(payload.get("updatedBy"), payload.get("createdBy"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "IMAGE",
                        payload.get("imageId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordVersionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_versions(flow_id, version_id, action, status, tag, image_id, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("imageVersionId"),
                        action,
                        payload.get("status"),
                        payload.get("tag"),
                        payload.get("imageId"),
                        firstNonBlank(payload.get("updatedBy"), payload.get("createdBy"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "VERSION",
                        payload.get("imageVersionId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordProfileWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_profiles(flow_id, profile_id, action, status, image_id, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("profileId"),
                        action,
                        payload.get("status"),
                        payload.get("imageId"),
                        firstNonBlank(payload.get("updatedBy"), payload.get("createdBy"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "PROFILE",
                        payload.get("profileId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordTemplateWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_templates(flow_id, template_id, action, status, image_id, image_version_id, updated_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("templateId"),
                        action,
                        payload.get("status"),
                        payload.get("imageId"),
                        payload.get("imageVersionId"),
                        firstNonBlank(payload.get("updatedBy"), payload.get("createdBy"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "TEMPLATE",
                        payload.get("templateId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordScanWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_scans(flow_id, scan_id, action, status, image_version_id, highest_severity, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("scanId"),
                        action,
                        payload.get("status"),
                        payload.get("imageVersionId"),
                        payload.get("highestSeverity"),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "SCAN",
                        payload.get("scanId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        @Override
        public void recordPlanWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO oim_flow_plans(flow_id, plan_id, action, status, risk_level, approval_status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        payload.get("planId"),
                        action,
                        payload.get("status"),
                        payload.get("riskLevel"),
                        payload.get("approvalStatus"),
                        firstNonBlank(payload.get("createdBy"), request.getHeader("X-Request-Id"), request.getHeader("X-Request-Id")),
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_audits(flow_id, request_id, action, target_type, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        action,
                        "IMAGE_PULL_PLAN",
                        payload.get("planId"),
                        "SUCCESS",
                        Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO oim_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId,
                        request.getHeader("X-Request-Id"),
                        request.getRequestURI(),
                        responseCode,
                        Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write ops-image-market database evidence", exception);
            }
        }

        private static Object firstNonBlank(Object first, Object second, Object third) {
            if (first != null && !String.valueOf(first).isBlank()) {
                return first;
            }
            if (second != null && !String.valueOf(second).isBlank()) {
                return second;
            }
            return third;
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }
}
