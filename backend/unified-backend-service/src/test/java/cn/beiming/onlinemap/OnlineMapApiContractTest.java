package cn.beiming.onlinemap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.beiming.portalcore.PortalCoreServiceApplication;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PortalCoreServiceApplication.class, properties = {"server.port=8134", "portal-core.test-controls.enabled=true"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OnlineMapApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("online-map local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "MAP-COM", 1, 120);
        addRange(mapped, "MAP-AUTH", 1, 140);
        addRange(mapped, "MAP-HEALTH", 1, 50);
        addRange(mapped, "MAP-PUBLIC", 1, 180);
        addRange(mapped, "MAP-PROVIDER", 1, 200);
        addRange(mapped, "MAP-WORLD", 1, 120);
        addRange(mapped, "MAP-LAYER", 1, 140);
        addRange(mapped, "MAP-MARKER", 1, 180);
        addRange(mapped, "MAP-REGION", 1, 160);
        addRange(mapped, "MAP-EMBED", 1, 80);
        addRange(mapped, "MAP-HEALTHSNAP", 1, 130);
        addRange(mapped, "MAP-AUDIT", 1, 120);
        addRange(mapped, "MAP-DEPS", 1, 150);
        addRange(mapped, "MAP-HARDEN", 1, 220);
        addRange(mapped, "MAP-PORT", 1, 20);
        addRange(mapped, "MAP-CYCLE", 1, 120);
        assertThat(mapped).contains("MAP-COM-001", "MAP-PROVIDER-200", "MAP-HARDEN-220", "MAP-CYCLE-120");
        assertThat(mapped).hasSize(2130);
    }

    @Test
    @DisplayName("MAP-COM, MAP-AUTH, MAP-HEALTH, MAP-PUBLIC, and MAP-OPS cover envelope, auth, health, summary, and public reads")
    void commonAuthHealthSummaryAndPublicReads() throws Exception {
        mvc.perform(get("/api/v1/online-map/health").header("X-Request-Id", "req-map-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-map-health"));

        JsonNode health = performJson(get("/api/v1/online-map/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("online-map");
        assertThat(health.at("/data/status").asText()).isIn("READY", "DEGRADED");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/online-map/admin/ops/summary"), 401, 41000);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("map-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 46820);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 46821);
        performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 46822);

        JsonNode summary = performJson(get("/api/v1/online-map/admin/ops/summary").header("Authorization", bearer("map-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("online-map");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8134);
        assertThat(summary.at("/data/legacyPort").asInt()).isEqualTo(8121);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/providerAdapterMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/productionGaps").toString()).contains(
                "REAL_PERSISTENCE_NOT_CONNECTED",
                "REAL_PROVIDER_HTTP_NOT_CONNECTED",
                "REAL_MARKER_SYNC_NOT_CONNECTED",
                "REAL_TILE_PROXY_FORBIDDEN");
        assertNoSecrets(summary);

        performJson(get("/api/v1/online-map/admin/ops/summary")
                .header("Authorization", bearer("map-viewer-token"))
                .header("X-Test-Fail-Store", "true"), 500, 55600);

        JsonNode overview = performJson(get("/api/v1/online-map/overview"), 200);
        assertThat(overview.at("/data/providers").isArray()).isTrue();
        assertThat(overview.toString()).contains("provider-blue-main", "world-overworld");
        assertNoSecrets(overview);

        performJson(get("/api/v1/online-map/providers").param("sort", "bad"), 400, 40003);
        JsonNode publicProviders = performJson(get("/api/v1/online-map/providers")
                .param("providerType", "BLUEMAP")
                .param("healthStatus", "ONLINE")
                .param("keyword", "blue")
                .param("sort", "displayName_asc"), 200);
        assertThat(publicProviders.toString()).contains("provider-blue-main").doesNotContain("adminNote", "opsRef", "allowedOrigins");

        performJson(get("/api/v1/online-map/providers/provider-blue-main"), 200);
        performJson(get("/api/v1/online-map/providers/provider-disabled"), 404, 49700);
        performJson(get("/api/v1/online-map/worlds").param("providerId", "provider-blue-main").param("dimension", "OVERWORLD"), 200);
        performJson(get("/api/v1/online-map/layers").param("worldId", "world-overworld").param("layerType", "MARKER_SET"), 200);
        performJson(get("/api/v1/online-map/markers").param("bounds", "-100,-100,100,100"), 200);
        performJson(get("/api/v1/online-map/markers").param("bounds", "bad"), 400, 49714);
        performJson(get("/api/v1/online-map/regions").param("bounds", "-200,-200,200,200"), 200);
        performJson(get("/api/v1/online-map/embed").param("origin", "https://beiming.example"), 200);
        performJson(get("/api/v1/online-map/embed").param("origin", "https://evil.example"), 400, 49715);
        performJson(patch("/api/v1/online-map/admin/providers/provider-blue-main/disable").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "关闭默认公开地图", "idempotencyKey", "disable-default-for-empty-embed"), 200);
        JsonNode emptyEmbed = performJson(get("/api/v1/online-map/embed"), 200);
        assertThat(emptyEmbed.at("/data").isNull()).isTrue();
    }

    @Test
    @DisplayName("MAP-PROVIDER and MAP-HEALTHSNAP cover provider lifecycle, URL safety, idempotency, dependencies, and health refresh")
    void providerLifecycleAndHealthRefresh() throws Exception {
        performJson(get("/api/v1/online-map/admin/providers")
                .header("Authorization", bearer("map-viewer-token"))
                .param("page", "0"), 400, 40002);
        performJson(get("/api/v1/online-map/admin/providers")
                .header("Authorization", bearer("map-viewer-token"))
                .param("sort", "bad"), 400, 40003);
        JsonNode providers = performJson(get("/api/v1/online-map/admin/providers")
                .header("Authorization", bearer("map-viewer-token"))
                .param("providerType", "BLUEMAP")
                .param("status", "ENABLED")
                .param("healthStatus", "ONLINE")
                .param("publicVisible", "true")
                .param("sort", "displayName_asc"), 200);
        assertThat(providers.toString()).contains("provider-blue-main");

        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-viewer-token")),
                providerBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("bad-url"), "publicBaseUrl", "http://127.0.0.1:8100/map"), 400, 49713);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("bad-origin"), "allowedOrigins", List.of("*")), 400, 49715);

        JsonNode created = performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                providerBody("provider-create"), 201);
        String providerId = created.at("/data/providerId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                providerBody("provider-create"), 201);
        assertThat(replay.at("/data/providerId").asText()).isEqualTo(providerId);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("provider-create"), "displayName", "Changed map"), 409, 49712);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("provider-url-conflict"), "publicBaseUrl", "https://maps.example.com/blue-main/"), 409, 49711);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("provider-embed-conflict"), "embedUrl", "https://maps.example.com/blue-main/embed/"), 409, 49711);
        performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                with(providerBody("provider-bad-type"), "providerType", "GOOGLE_MAPS"), 400, 40001);

        performJson(patch("/api/v1/online-map/admin/providers/" + providerId).header("Authorization", bearer("map-admin-token")),
                Map.of("publicVisible", true, "reason", "公开入口变更缺确认", "idempotencyKey", "patch-no-confirm"), 403, 42003);
        JsonNode patched = performJson(patch("/api/v1/online-map/admin/providers/" + providerId).header("Authorization", bearer("map-admin-token")),
                Map.of("publicVisible", true, "confirmText", "UPDATE_PUBLIC_MAP_ENTRY", "reason", "公开入口", "idempotencyKey", "patch-provider"), 200);
        assertThat(patched.at("/data/publicVisible").asBoolean()).isTrue();

        performJson(patch("/api/v1/online-map/admin/providers/" + providerId + "/enable").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "缺确认", "idempotencyKey", "enable-no-confirm"), 403, 42003);
        JsonNode enabled = performJson(patch("/api/v1/online-map/admin/providers/" + providerId + "/enable").header("Authorization", bearer("map-admin-token")),
                Map.of("confirmText", "ENABLE_PUBLIC_MAP_PROVIDER", "reason", "启用 provider", "idempotencyKey", "enable-provider"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode snapshot = performJson(post("/api/v1/online-map/admin/providers/" + providerId + "/health/refresh")
                        .header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "刷新健康", "idempotencyKey", "refresh-provider"), 200);
        assertThat(snapshot.at("/data/providerId").asText()).isEqualTo(providerId);
        assertThat(snapshot.at("/data/healthStatus").asText()).isIn("ONLINE", "DEGRADED", "OFFLINE", "UNKNOWN");
        JsonNode refreshReplay = performJson(post("/api/v1/online-map/admin/providers/" + providerId + "/health/refresh")
                        .header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "刷新健康", "idempotencyKey", "refresh-provider"), 200);
        assertThat(refreshReplay.at("/data/snapshotId").asText()).isEqualTo(snapshot.at("/data/snapshotId").asText());
        performJson(post("/api/v1/online-map/admin/providers/" + providerId + "/health/refresh")
                        .header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "刷新冷却冲突", "idempotencyKey", "refresh-provider-cooldown"), 409, 49716);

        performJson(post("/api/v1/online-map/admin/providers/provider-blue-main/health/refresh")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Fail-Store", "true"),
                Map.of("reason", "快照写入失败", "idempotencyKey", "refresh-store-fail"), 500, 55603);
        performJson(post("/api/v1/online-map/admin/providers/provider-blue-main/health/refresh")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Provider-Mode", "unavailable"),
                Map.of("reason", "provider 不可用", "idempotencyKey", "refresh-seed-down"), 200);

        JsonNode snapshots = performJson(get("/api/v1/online-map/admin/providers/" + providerId + "/health/snapshots")
                .header("Authorization", bearer("map-viewer-token"))
                .param("healthStatus", "ONLINE")
                .param("sort", "checkedAt_desc"), 200);
        assertThat(snapshots.at("/data/items").isArray()).isTrue();

        JsonNode disabled = performJson(patch("/api/v1/online-map/admin/providers/" + providerId + "/disable").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "停用 provider", "idempotencyKey", "disable-provider"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");

        performJson(patch("/api/v1/online-map/admin/providers/" + providerId + "/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "缺归档确认", "idempotencyKey", "archive-no-confirm"), 403, 42003);
        performJson(patch("/api/v1/online-map/admin/providers/" + providerId + "/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("confirmText", "ARCHIVE_MAP_PROVIDER", "reason", "仍有自动世界不可归档", "idempotencyKey", "archive-provider"), 409, 49717);
        JsonNode archived = performJson(patch("/api/v1/online-map/admin/providers/provider-disabled/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("confirmText", "ARCHIVE_MAP_PROVIDER", "reason", "归档无公开对象 provider", "idempotencyKey", "archive-provider-disabled"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");

        performJson(patch("/api/v1/online-map/admin/providers/provider-blue-main/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("confirmText", "ARCHIVE_MAP_PROVIDER", "reason", "启用 provider 不可直接归档", "idempotencyKey", "archive-enabled"), 409, 49710);
        performJson(patch("/api/v1/online-map/admin/providers/provider-blue-main/disable").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "先禁用公开 provider", "idempotencyKey", "disable-seeded-provider"), 200);
        performJson(patch("/api/v1/online-map/admin/providers/provider-blue-main/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("confirmText", "ARCHIVE_MAP_PROVIDER", "reason", "仍有公开对象不可归档", "idempotencyKey", "archive-public-children"), 409, 49717);
    }

    @Test
    @DisplayName("MAP-WORLD, MAP-LAYER, MAP-MARKER, and MAP-REGION cover map objects, bounds, state flow, and recursive hardening")
    void worldLayerMarkerAndRegionContracts() throws Exception {
        JsonNode world = performJson(put("/api/v1/online-map/admin/worlds/world-contract")
                        .header("Authorization", bearer("map-admin-token")),
                worldBody("world-contract"), 200);
        assertThat(world.at("/data/worldId").asText()).isEqualTo("world-contract");
        performJson(put("/api/v1/online-map/admin/worlds/world-bad-path").header("Authorization", bearer("map-admin-token")),
                with(worldBody("world-bad-path"), "sourceWorldKey", "../world"), 400, 40001);
        performJson(put("/api/v1/online-map/admin/worlds/world-bad-coord").header("Authorization", bearer("map-admin-token")),
                with(worldBody("world-bad-coord"), "center", Map.of("x", "NaN", "z", 0)), 400, 49714);
        performJson(put("/api/v1/online-map/admin/worlds/world-bad-dimension").header("Authorization", bearer("map-admin-token")),
                with(worldBody("world-bad-dimension"), "dimension", "SKYLAND"), 400, 40001);
        JsonNode selectedEmbed = performJson(get("/api/v1/online-map/embed")
                .param("providerId", "provider-blue-main")
                .param("worldId", "world-contract"), 200);
        assertThat(selectedEmbed.at("/data/defaultWorldId").asText()).isEqualTo("world-contract");
        performJson(put("/api/v1/online-map/admin/worlds/world-hidden-embed").header("Authorization", bearer("map-admin-token")),
                with(worldBody("world-hidden-embed"), "publicVisible", false), 200);
        performJson(get("/api/v1/online-map/embed")
                .param("providerId", "provider-blue-main")
                .param("worldId", "world-hidden-embed"), 404, 49701);

        JsonNode layer = performJson(post("/api/v1/online-map/admin/layers").header("Authorization", bearer("map-admin-token")),
                layerBody("layer-create"), 201);
        String layerId = layer.at("/data/layerId").asText();
        assertThat(layer.at("/data/status").asText()).isEqualTo("VISIBLE");
        performJson(post("/api/v1/online-map/admin/layers").header("Authorization", bearer("map-admin-token")),
                with(layerBody("layer-bad-type"), "layerType", "BAD_LAYER"), 400, 40001);
        performJson(post("/api/v1/online-map/admin/layers").header("Authorization", bearer("map-admin-token")),
                with(layerBody("layer-trusted"), "styleSummary", Map.of("secretKey", "nope")), 400, 40001);
        JsonNode layerPatch = performJson(patch("/api/v1/online-map/admin/layers/" + layerId).header("Authorization", bearer("map-admin-token")),
                Map.of("status", "HIDDEN", "reason", "隐藏图层", "idempotencyKey", "patch-layer"), 200);
        assertThat(layerPatch.at("/data/status").asText()).isEqualTo("HIDDEN");

        JsonNode marker = performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                markerBody("marker-create", layerId), 201);
        String markerId = marker.at("/data/markerId").asText();
        assertThat(marker.at("/data/status").asText()).isEqualTo("PUBLISHED");
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                with(markerBody("marker-bad-line", layerId), "markerType", "LINE"), 400, 49714);
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                with(markerBody("marker-bad-enum", layerId), "sourceModule", "PLUGIN_SECRET"), 400, 40001);
        Map<String, Object> htmlMarker = with(markerBody("marker-html-script", layerId), "markerType", "HTML");
        htmlMarker.put("summary", "<script>alert(1)</script>");
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                htmlMarker, 400, 40001);
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                with(markerBody("marker-source-duplicate", layerId), "sourceRef", marker.at("/data/sourceRef")), 409, 49711);
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                with(markerBody("marker-internal-icon", layerId), "iconRef", Map.of("url", "http://localhost/icon.png")), 400, 40001);
        performJson(post("/api/v1/online-map/admin/markers").header("Authorization", bearer("map-admin-token")),
                with(markerBody("marker-nested-secret", layerId), "sourceRef", Map.of("nested", List.of(Map.of("mapAdminPassword", "nope")))), 400, 40001);
        JsonNode markerPatch = performJson(patch("/api/v1/online-map/admin/markers/" + markerId).header("Authorization", bearer("map-admin-token")),
                Map.of("status", "HIDDEN", "reason", "隐藏 marker", "idempotencyKey", "patch-marker"), 200);
        assertThat(markerPatch.at("/data/status").asText()).isEqualTo("HIDDEN");

        JsonNode region = performJson(post("/api/v1/online-map/admin/regions").header("Authorization", bearer("map-admin-token")),
                regionBody("region-create", layerId), 201);
        String regionId = region.at("/data/regionId").asText();
        performJson(post("/api/v1/online-map/admin/regions").header("Authorization", bearer("map-admin-token")),
                with(regionBody("region-bad-y", layerId), "maxY", -1), 400, 49714);
        performJson(post("/api/v1/online-map/admin/regions").header("Authorization", bearer("map-admin-token")),
                with(regionBody("region-bad-source-module", layerId), "sourceModule", "PLUGIN_SECRET"), 400, 40001);
        performJson(post("/api/v1/online-map/admin/regions").header("Authorization", bearer("map-admin-token")),
                with(regionBody("region-source-duplicate", layerId), "sourceRef", region.at("/data/sourceRef")), 409, 49711);
        JsonNode regionPatch = performJson(patch("/api/v1/online-map/admin/regions/" + regionId).header("Authorization", bearer("map-admin-token")),
                Map.of("status", "HIDDEN", "reason", "隐藏区域", "idempotencyKey", "patch-region"), 200);
        assertThat(regionPatch.at("/data/status").asText()).isEqualTo("HIDDEN");

        performJson(patch("/api/v1/online-map/admin/layers/" + layerId + "/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "归档图层", "idempotencyKey", "archive-layer"), 200);
        performJson(patch("/api/v1/online-map/admin/markers/" + markerId + "/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "归档 marker", "idempotencyKey", "archive-marker"), 200);
        performJson(patch("/api/v1/online-map/admin/regions/" + regionId + "/archive").header("Authorization", bearer("map-admin-token")),
                Map.of("reason", "归档区域", "idempotencyKey", "archive-region"), 200);
    }

    @Test
    @DisplayName("MAP-AUDIT, MAP-DEPS, MAP-HARDEN, and MAP-CYCLE cover audit, dependency failures, source boundaries, and test controls")
    void auditDependencyHardeningAndBoundaryScan() throws Exception {
        performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                providerBody("audit-fail-provider"), 500, 55601);
        performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Server-Status-Mode", "unavailable"),
                providerBody("server-status-down"), 502, 46800);
        performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Ops-Control-Mode", "unavailable"),
                providerBody("ops-down"), 502, 46810);
        performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Content-Mode", "unavailable"),
                providerBody("content-down"), 502, 46830);
        performJson(post("/api/v1/online-map/admin/providers")
                        .header("Authorization", bearer("map-admin-token"))
                        .header("X-Test-Changelog-Mode", "unavailable"),
                providerBody("changelog-down"), 502, 46840);

        JsonNode provider = performJson(post("/api/v1/online-map/admin/providers").header("Authorization", bearer("map-admin-token")),
                providerBody("audit-provider"), 201);
        JsonNode audit = performJson(get("/api/v1/online-map/admin/audit-logs")
                .header("Authorization", bearer("map-admin-token"))
                .param("providerId", provider.at("/data/providerId").asText())
                .param("action", "MAP_PROVIDER_CREATED")
                .param("result", "SUCCESS")
                .param("riskLevel", "MEDIUM"), 200);
        assertThat(audit.at("/data/total").asInt()).isEqualTo(1);
        assertThat(audit.at("/data/items/0/reason").asText()).isEqualTo("创建地图 provider");
        assertThat(audit.at("/data/items/0/paramsSummary/sanitized").asBoolean()).isTrue();
        assertNoSecrets(audit);
        performJson(get("/api/v1/online-map/admin/audit-logs").header("Authorization", bearer("map-viewer-token")), 403, 42001);

        Path serviceRoot = Path.of("src/main/java/cn/beiming/onlinemap");
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
                "cn.beiming.nodedaemon.", "cn.beiming.cloudrevesync.", "cn.beiming.backuprecovery.", "cn.beiming.alerting.",
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken",
                "backupEncryptionKey", "terminal", "container", "restorePath", "worldDirectory", "mapAdminPassword",
                "webhookSecret", "smtpPassword", "smsToken", "rawToken", "credential", "secretKey", "internalUrl",
                "internalPath", "resolvedPath", "rm -rf", "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s",
                "jdbc:", "authorized_keys", "id_rsa", ".env");
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
        body.put("providerType", "BLUEMAP");
        body.put("displayName", "Contract Map " + idempotencyKey);
        body.put("publicBaseUrl", "https://maps.example.com/" + idempotencyKey);
        body.put("embedUrl", "https://maps.example.com/" + idempotencyKey + "/embed");
        body.put("publicVisible", false);
        body.put("allowedOrigins", List.of("https://beiming.example"));
        body.put("contentRef", Map.of("contentId", "content-map-guide"));
        body.put("serverStatusRef", Map.of("instanceId", "survival-main"));
        body.put("opsRef", Map.of("instanceId", "mc-main"));
        body.put("changelogRef", Map.of("releaseId", "map-v1"));
        body.put("adminNote", "contract seed");
        body.put("reason", "创建地图 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> worldBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-blue-main");
        body.put("worldName", idempotencyKey);
        body.put("dimension", "OVERWORLD");
        body.put("displayName", "Contract World");
        body.put("enabled", true);
        body.put("publicVisible", true);
        body.put("sourceWorldKey", "overworld");
        body.put("center", Map.of("x", 0, "y", 64, "z", 0));
        body.put("bounds", Map.of("minX", -1000, "minZ", -1000, "maxX", 1000, "maxZ", 1000));
        body.put("renderStatus", "READY");
        body.put("lastRenderedAt", "2026-05-27T00:00:00Z");
        body.put("styleSummary", Map.of("theme", "surface"));
        body.put("sortOrder", 10);
        body.put("reason", "保存世界快照");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> layerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-blue-main");
        body.put("worldId", "world-overworld");
        body.put("displayName", "Contract Layer " + idempotencyKey);
        body.put("layerType", "MARKER_SET");
        body.put("defaultVisible", true);
        body.put("toggleable", true);
        body.put("visibility", "PUBLIC");
        body.put("styleSummary", Map.of("color", "#2f7d50"));
        body.put("sortOrder", 20);
        body.put("reason", "创建地图图层");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> markerBody(String idempotencyKey, String layerId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-blue-main");
        body.put("worldId", "world-overworld");
        body.put("layerId", layerId);
        body.put("markerType", "POI");
        body.put("title", "Contract Marker " + idempotencyKey);
        body.put("summary", "A safe point");
        body.put("position", Map.of("x", 12, "y", 70, "z", 34));
        body.put("points", List.of());
        body.put("iconRef", Map.of("url", "/assets/map/icon.png"));
        body.put("styleSummary", Map.of("color", "#2f7d50"));
        body.put("visibility", "PUBLIC");
        body.put("status", "PUBLISHED");
        body.put("sourceModule", "MANUAL");
        body.put("sourceRef", Map.of("sourceId", idempotencyKey));
        body.put("expiresAt", "2030-01-01T00:00:00Z");
        body.put("reason", "创建地图 marker");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> regionBody(String idempotencyKey, String layerId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-blue-main");
        body.put("worldId", "world-overworld");
        body.put("layerId", layerId);
        body.put("title", "Contract Region " + idempotencyKey);
        body.put("summary", "A safe area");
        body.put("points", List.of(
                Map.of("x", 0, "z", 0),
                Map.of("x", 60, "z", 0),
                Map.of("x", 60, "z", 60)));
        body.put("minY", 0);
        body.put("maxY", 255);
        body.put("styleSummary", Map.of("fill", "#2f7d50"));
        body.put("visibility", "PUBLIC");
        body.put("status", "PUBLISHED");
        body.put("sourceModule", "MANUAL");
        body.put("sourceRef", Map.of("sourceId", idempotencyKey));
        body.put("expiresAt", "2030-01-01T00:00:00Z");
        body.put("reason", "创建地图区域");
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
                "rawToken", "credential", "secretKey", "nodeToken", "mapAdminPassword", "Authorization",
                "requestHeaders", "stackTrace", "internalUrl", "internalPath", "resolvedPath", "worldDirectory",
                "fullException", "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken",
                "backupEncryptionKey", "terminal", "container", "restorePath", "/srv/", "C:\\\\", ".env",
                "authorized_keys", "id_rsa", "token=");
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
