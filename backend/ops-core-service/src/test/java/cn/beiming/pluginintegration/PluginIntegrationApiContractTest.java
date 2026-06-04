package cn.beiming.pluginintegration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class, properties = "plugin-integration.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PluginIntegrationApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("plugin-integration local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "PINT-COM", 1, 120);
        addRange(mapped, "PINT-AUTH", 1, 140);
        addRange(mapped, "PINT-HEALTH", 1, 50);
        addRange(mapped, "PINT-OPS", 1, 120);
        addRange(mapped, "PINT-PROVIDER", 1, 220);
        addRange(mapped, "PINT-INSTANCE", 1, 100);
        addRange(mapped, "PINT-CAP", 1, 80);
        addRange(mapped, "PINT-SCHEMA", 1, 180);
        addRange(mapped, "PINT-EVENT", 1, 240);
        addRange(mapped, "PINT-REPLAY", 1, 100);
        addRange(mapped, "PINT-ROUTE", 1, 180);
        addRange(mapped, "PINT-SYNC", 1, 180);
        addRange(mapped, "PINT-HEALTHSNAP", 1, 100);
        addRange(mapped, "PINT-MAPPING", 1, 180);
        addRange(mapped, "PINT-AUDIT", 1, 140);
        addRange(mapped, "PINT-DEPS", 1, 180);
        addRange(mapped, "PINT-HARDEN", 1, 240);
        addRange(mapped, "PINT-PORT", 1, 20);
        addRange(mapped, "PINT-CYCLE", 1, 140);
        assertThat(mapped).contains("PINT-COM-001", "PINT-PROVIDER-220", "PINT-EVENT-240", "PINT-HARDEN-240", "PINT-CYCLE-140");
        assertThat(mapped).hasSize(2710);
    }

    @Test
    @DisplayName("PINT-COM, PINT-AUTH, PINT-HEALTH, PINT-OPS, PINT-INSTANCE, and PINT-CAP cover envelope, auth, health, summary, instances, and capabilities")
    void commonAuthHealthSummaryInstancesAndCapabilities() throws Exception {
        mvc.perform(get("/api/v1/plugin-integration/health").header("X-Request-Id", "req-plugin-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-plugin-health"));

        JsonNode health = performJson(get("/api/v1/plugin-integration/health"), 200);
        assertThat(health.at("/code").asInt()).isZero();
        assertThat(health.at("/message").asText()).isEqualTo("success");
        assertThat(health.at("/data/service").asText()).isEqualTo("plugin-integration");
        assertThat(health.at("/data/status").asText()).isIn("READY", "DEGRADED");
        assertThat(health.at("/requestId").asText()).isNotBlank();
        assertNoSecrets(health);

        performJson(get("/api/v1/plugin-integration/admin/ops/summary"), 401, 41000);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("plugin-no-cap-token")), 403, 42002);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("auth-unavailable-token")), 502, 47050);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("auth-timeout-token")), 504, 47051);
        performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("auth-bad-token")), 502, 47052);

        JsonNode summary = performJson(get("/api/v1/plugin-integration/admin/ops/summary").header("Authorization", bearer("plugin-viewer-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("plugin-integration");
        assertThat(summary.at("/data/port").asInt()).isEqualTo(8133);
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(summary.at("/data/productionGaps").toString()).contains(
                "REAL_PLUGIN_RUNTIME_NOT_CONNECTED",
                "REAL_NODE_DAEMON_NOT_CONNECTED",
                "REAL_ONLINE_MAP_SYNC_NOT_CONNECTED",
                "RAW_PAYLOAD_STORAGE_DISABLED");
        assertNoSecrets(summary);

        performJson(get("/api/v1/plugin-integration/admin/ops/summary")
                .header("Authorization", bearer("plugin-viewer-token"))
                .header("X-Test-Fail-Store", "true"), 500, 55700);
        performJson(get("/api/v1/plugin-integration/admin/instances")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("providerId", "provider-paper-main")
                .param("loaded", "true")
                .param("enabled", "true")
                .param("stale", "false")
                .param("sort", "lastSeenAt_desc"), 200);
        performJson(get("/api/v1/plugin-integration/admin/instances/missing").header("Authorization", bearer("plugin-viewer-token")), 404, 49801);
        JsonNode capabilities = performJson(get("/api/v1/plugin-integration/admin/capabilities")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("providerId", "provider-paper-main")
                .param("namespace", "beiming")
                .param("riskLevel", "LOW")
                .param("available", "true"), 200);
        assertThat(capabilities.toString()).contains("beiming.event.ingest");
        assertNoSecrets(capabilities);
    }

    @Test
    @DisplayName("PINT-PROVIDER and PINT-HEALTHSNAP cover provider lifecycle, endpoint safety, idempotency, dependencies, and health snapshots")
    void providerLifecycleAndHealthSnapshots() throws Exception {
        performJson(get("/api/v1/plugin-integration/admin/providers")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("page", "0"), 400, 40002);
        performJson(get("/api/v1/plugin-integration/admin/providers")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("sort", "bad"), 400, 40003);
        JsonNode providers = performJson(get("/api/v1/plugin-integration/admin/providers")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("providerType", "PAPER")
                .param("serverKind", "SERVER")
                .param("status", "ENABLED")
                .param("healthStatus", "ONLINE")
                .param("publicVisible", "false")
                .param("sort", "displayName_asc"), 200);
        assertThat(providers.toString()).contains("provider-paper-main");

        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-viewer-token")),
                providerBody("viewer-denied"), 403, 42002);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("bad-origin"), "allowedOrigins", List.of("*")), 400, 49813);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("bad-endpoint"), "eventEndpointSummary", "http://127.0.0.1:8122/plugin"), 400, 49813);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                providerBody("no-confirm"), 403, 42003);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("bad-provider-type"), "providerType", "BAD_PROVIDER"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 400, 40001);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("bad-private-origin"), "allowedOrigins", List.of("http://10.0.0.5/plugin")), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 400, 49813);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("bad-userinfo-origin"), "allowedOrigins", List.of("https://user:pass@plugins.example.com")), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 400, 49813);

        JsonNode created = performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 201);
        String providerId = created.at("/data/providerId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertNoSecrets(created);

        JsonNode replay = performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("provider-create"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 201);
        assertThat(replay.at("/data/providerId").asText()).isEqualTo(providerId);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("provider-create"), "displayName", "Changed Provider"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 409, 49812);
        performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("provider-conflict"), "displayName", "Paper Bridge"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 409, 49811);

        performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId).header("Authorization", bearer("plugin-admin-token")),
                Map.of("publicVisible", true, "reason", "公开 plugin provider 缺确认", "idempotencyKey", "patch-no-confirm"), 403, 42003);
        JsonNode patched = performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId).header("Authorization", bearer("plugin-admin-token")),
                Map.of("publicVisible", true, "confirmText", "UPDATE_PLUGIN_PROVIDER_ENDPOINT", "reason", "公开 provider", "idempotencyKey", "patch-provider"), 200);
        assertThat(patched.at("/data/publicVisible").asBoolean()).isTrue();

        performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "缺启用确认", "idempotencyKey", "enable-no-confirm"), 403, 42003);
        JsonNode enabled = performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "ENABLE_PLUGIN_PROVIDER", "reason", "启用 provider", "idempotencyKey", "enable-provider"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode emptyEventsProvider = performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(with(providerBody("provider-empty-events"), "allowedEventTypes", List.of()), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 201);
        String emptyEventsProviderId = emptyEventsProvider.at("/data/providerId").asText();
        performJson(patch("/api/v1/plugin-integration/admin/providers/" + emptyEventsProviderId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "ENABLE_PLUGIN_PROVIDER", "reason", "缺少允许事件类型", "idempotencyKey", "enable-empty-events"), 400, 40001);

        JsonNode snapshots = performJson(get("/api/v1/plugin-integration/admin/providers/" + providerId + "/health-snapshots")
                .header("Authorization", bearer("plugin-viewer-token"))
                .param("healthStatus", "ONLINE")
                .param("sort", "checkedAt_desc"), 200);
        assertThat(snapshots.at("/data/items").isArray()).isTrue();

        JsonNode disabled = performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId + "/disable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "禁用 provider", "idempotencyKey", "disable-provider"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId + "/archive").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "缺归档确认", "idempotencyKey", "archive-no-confirm"), 403, 42003);
        JsonNode archived = performJson(patch("/api/v1/plugin-integration/admin/providers/" + providerId + "/archive").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "ARCHIVE_PLUGIN_PROVIDER", "reason", "归档 provider", "idempotencyKey", "archive-provider"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");
        performJson(patch("/api/v1/plugin-integration/admin/providers/provider-paper-main/archive").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "ARCHIVE_PLUGIN_PROVIDER", "reason", "启用 provider 不可归档", "idempotencyKey", "archive-enabled"), 409, 49810);
    }

    @Test
    @DisplayName("PINT-SCHEMA, PINT-EVENT, PINT-REPLAY, and PINT-ROUTE cover schemas, event ingest, replay, route rules, validation, and routing")
    void schemaEventReplayAndRouteContracts() throws Exception {
        JsonNode schema = performJson(post("/api/v1/plugin-integration/admin/event-schemas").header("Authorization", bearer("plugin-admin-token")),
                schemaBody("schema-create"), 201);
        String schemaId = schema.at("/data/schemaId").asText();
        assertThat(schema.at("/data/status").asText()).isEqualTo("DRAFT");
        performJson(post("/api/v1/plugin-integration/admin/event-schemas").header("Authorization", bearer("plugin-admin-token")),
                with(schemaBody("schema-bad-sensitive"), "samplePayloadSummary", Map.of("webhookSecret", "nope")), 400, 40001);
        JsonNode schemaEnabled = performJson(patch("/api/v1/plugin-integration/admin/event-schemas/" + schemaId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "启用 schema", "idempotencyKey", "enable-schema"), 200);
        assertThat(schemaEnabled.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(patch("/api/v1/plugin-integration/admin/event-schemas/" + schemaId + "/disable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "禁用 schema", "idempotencyKey", "disable-schema"), 200);
        performJson(patch("/api/v1/plugin-integration/admin/event-schemas/" + schemaId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "重新启用 schema", "idempotencyKey", "enable-schema-2"), 200);

        JsonNode route = performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                routeBody("route-create"), 201);
        String ruleId = route.at("/data/ruleId").asText();
        performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                with(routeBody("route-duplicate"), "targetAction", "UPSERT_MARKER_PREVIEW_route-create"), 409, 49811);
        performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                with(routeBody("route-bad-target"), "targetModule", "BAD_TARGET"), 400, 40001);
        performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                with(routeBody("route-high-no-confirm"), "riskLevel", "HIGH"), 403, 42003);
        JsonNode highRoute = performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                with(with(with(routeBody("route-high-disabled"), "riskLevel", "HIGH"), "enabled", false), "confirmText", "CONFIGURE_PLUGIN_ROUTE"), 201);
        String highRuleId = highRoute.at("/data/ruleId").asText();
        performJson(patch("/api/v1/plugin-integration/admin/route-rules/" + highRuleId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "缺高风险路由启用确认", "idempotencyKey", "enable-high-route-no-confirm"), 403, 42003);
        performJson(patch("/api/v1/plugin-integration/admin/route-rules/" + highRuleId).header("Authorization", bearer("plugin-admin-token")),
                Map.of("targetModule", "OPS_CONTROL", "confirmText", "UPDATE_PLUGIN_ROUTE", "reason", "禁止改到真实运维目标", "idempotencyKey", "patch-route-ops"), 409, 49817);
        performJson(post("/api/v1/plugin-integration/admin/route-rules").header("Authorization", bearer("plugin-admin-token")),
                with(with(routeBody("route-ops-blocked"), "targetModule", "OPS_CONTROL"), "confirmText", "CONFIGURE_PLUGIN_ROUTE"), 409, 49817);
        performJson(patch("/api/v1/plugin-integration/admin/route-rules/" + ruleId + "/disable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "禁用路由", "idempotencyKey", "disable-route"), 200);
        JsonNode routeEnabled = performJson(patch("/api/v1/plugin-integration/admin/route-rules/" + ruleId + "/enable").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "启用路由", "idempotencyKey", "enable-route"), 200);
        assertThat(routeEnabled.at("/data/enabled").asBoolean()).isTrue();

        JsonNode event = performJson(post("/api/v1/plugin-integration/admin/events/ingest").header("Authorization", bearer("plugin-admin-token")),
                eventBody("event-main"), 201);
        String eventId = event.at("/data/eventId").asText();
        assertThat(event.at("/data/rawPayloadStored").asBoolean()).isFalse();
        assertThat(event.at("/data/validationStatus").asText()).isEqualTo("VALIDATED");
        assertThat(event.at("/data/routeStatus").asText()).isIn("ROUTED", "IGNORED");
        assertNoSecrets(event);

        performJson(post("/api/v1/plugin-integration/admin/events/ingest").header("Authorization", bearer("plugin-admin-token")),
                with(eventBody("event-missing-field"), "payload", Map.of("player", "Steve")), 400, 49814);
        performJson(post("/api/v1/plugin-integration/admin/events/ingest").header("Authorization", bearer("plugin-admin-token")),
                with(eventBody("event-trusted-raw"), "payload", Map.of("player", "Steve", "world", "world", "rawPayload", Map.of("hidden", true))), 400, 40001);
        performJson(post("/api/v1/plugin-integration/admin/events/ingest").header("Authorization", bearer("plugin-admin-token")),
                with(eventBody("event-sensitive"), "payload", Map.of("player", "Steve", "world", "world", "webhookSecret", "nope")), 400, 40001);
        performJson(post("/api/v1/plugin-integration/admin/events/ingest").header("Authorization", bearer("plugin-admin-token")),
                with(eventBody("event-bad-type"), "eventType", "beiming.unknown"), 403, 49815);

        performJson(get("/api/v1/plugin-integration/admin/events/" + eventId).header("Authorization", bearer("plugin-viewer-token")), 200);
        JsonNode replay = performJson(post("/api/v1/plugin-integration/admin/events/" + eventId + "/replay").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "REPLAY_PLUGIN_EVENT", "reason", "重放事件", "idempotencyKey", "replay-event"), 201);
        assertThat(replay.at("/data/eventId").asText()).isEqualTo(eventId);
        performJson(post("/api/v1/plugin-integration/admin/events/" + eventId + "/replay").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "缺重放确认", "idempotencyKey", "replay-no-confirm"), 403, 42003);
        performJson(post("/api/v1/plugin-integration/admin/events/" + eventId + "/replay").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "REPLAY_PLUGIN_EVENT", "targetRuleIds", List.of("missing-rule"), "reason", "重放目标规则不存在", "idempotencyKey", "replay-missing-rule"), 404, 49804);
        performJson(post("/api/v1/plugin-integration/admin/events/" + eventId + "/replay").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "REPLAY_PLUGIN_EVENT", "targetRuleIds", List.of(highRuleId), "reason", "重放目标规则未启用", "idempotencyKey", "replay-disabled-rule"), 409, 49810);
    }

    @Test
    @DisplayName("PINT-SYNC, PINT-MAPPING, PINT-AUDIT, PINT-DEPS, and PINT-HARDEN cover sync tasks, mappings, audit, dependency failures, and source boundaries")
    void syncMappingAuditDependencyAndHardening() throws Exception {
        JsonNode task = performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                syncTaskBody("sync-main"), 201);
        String taskId = task.at("/data/taskId").asText();
        assertThat(task.at("/data/status").asText()).isIn("QUEUED", "SIMULATED_BLOCKED");
        JsonNode taskReplay = performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                syncTaskBody("sync-main"), 201);
        assertThat(taskReplay.at("/data/taskId").asText()).isEqualTo(taskId);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                with(syncTaskBody("sync-high-no-confirm"), "riskLevel", "HIGH"), 403, 42003);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                with(with(syncTaskBody("sync-ops-blocked"), "targetModule", "OPS_CONTROL"), "confirmText", "CREATE_PLUGIN_SYNC_TASK"), 409, 49817);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                with(syncTaskBody("sync-missing-event"), "eventId", "missing-event"), 404, 49803);
        JsonNode mismatchProvider = performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("sync-mismatch-provider"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 201);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                with(syncTaskBody("sync-provider-mismatch"), "providerId", mismatchProvider.at("/data/providerId").asText()), 409, 49810);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks").header("Authorization", bearer("plugin-admin-token")),
                with(syncTaskBody("sync-bad-risk"), "riskLevel", "BAD_RISK"), 400, 40001);
        JsonNode canceled = performJson(patch("/api/v1/plugin-integration/admin/sync-tasks/" + taskId + "/cancel").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "取消模拟同步", "idempotencyKey", "cancel-sync"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");
        performJson(patch("/api/v1/plugin-integration/admin/sync-tasks/" + taskId + "/cancel").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "终态不可重复取消", "idempotencyKey", "cancel-sync-2"), 409, 49810);

        performJson(put("/api/v1/plugin-integration/admin/object-mappings/mapping-public").header("Authorization", bearer("plugin-admin-token")),
                mappingBody("mapping-public"), 403, 42003);
        JsonNode mapping = performJson(put("/api/v1/plugin-integration/admin/object-mappings/mapping-public").header("Authorization", bearer("plugin-admin-token")),
                with(mappingBody("mapping-public"), "confirmText", "UPSERT_PLUGIN_OBJECT_MAPPING"), 201);
        assertThat(mapping.at("/data/status").asText()).isEqualTo("ACTIVE");
        JsonNode mappedProvider = performJson(post("/api/v1/plugin-integration/admin/providers").header("Authorization", bearer("plugin-admin-token")),
                with(providerBody("provider-with-active-mapping"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 201);
        String mappedProviderId = mappedProvider.at("/data/providerId").asText();
        performJson(put("/api/v1/plugin-integration/admin/object-mappings/mapping-blocks-provider-archive").header("Authorization", bearer("plugin-admin-token")),
                with(with(mappingBody("mapping-blocks-provider-archive"), "providerId", mappedProviderId), "confirmText", "UPSERT_PLUGIN_OBJECT_MAPPING"), 201);
        performJson(patch("/api/v1/plugin-integration/admin/providers/" + mappedProviderId + "/archive").header("Authorization", bearer("plugin-admin-token")),
                Map.of("confirmText", "ARCHIVE_PLUGIN_PROVIDER", "reason", "仍有活动对象映射", "idempotencyKey", "archive-provider-with-active-mapping"), 409, 49810);
        performJson(put("/api/v1/plugin-integration/admin/object-mappings/mapping-conflict").header("Authorization", bearer("plugin-admin-token")),
                with(with(mappingBody("mapping-conflict"), "sourceObjectKey", "source-mapping-public"), "confirmText", "UPSERT_PLUGIN_OBJECT_MAPPING"), 409, 49811);
        JsonNode archivedMapping = performJson(patch("/api/v1/plugin-integration/admin/object-mappings/mapping-public/archive").header("Authorization", bearer("plugin-admin-token")),
                Map.of("reason", "归档对象映射", "idempotencyKey", "archive-mapping"), 200);
        assertThat(archivedMapping.at("/data/status").asText()).isEqualTo("ARCHIVED");

        performJson(post("/api/v1/plugin-integration/admin/providers")
                        .header("Authorization", bearer("plugin-admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                with(providerBody("audit-fail-provider"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 500, 55701);
        performJson(post("/api/v1/plugin-integration/admin/providers")
                        .header("Authorization", bearer("plugin-admin-token"))
                        .header("X-Test-Ops-Control-Mode", "unavailable"),
                with(providerBody("ops-down"), "confirmText", "REGISTER_PLUGIN_PROVIDER_ENDPOINT"), 502, 47060);
        performJson(post("/api/v1/plugin-integration/admin/sync-tasks")
                        .header("Authorization", bearer("plugin-admin-token"))
                        .header("X-Test-Online-Map-Mode", "unavailable"),
                syncTaskBody("online-map-down"), 502, 47080);
        performJson(post("/api/v1/plugin-integration/admin/events/ingest")
                        .header("Authorization", bearer("plugin-admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                eventBody("notification-down"), 201);

        JsonNode audit = performJson(get("/api/v1/plugin-integration/admin/audit-logs")
                .header("Authorization", bearer("plugin-admin-token"))
                .param("action", "PLUGIN_OBJECT_MAPPING_UPSERTED")
                .param("mappingId", "mapping-public")
                .param("result", "SUCCESS")
                .param("riskLevel", "HIGH"), 200);
        assertThat(audit.at("/data/total").asInt()).isEqualTo(1);
        assertThat(audit.at("/data/items/0/reason").asText()).isEqualTo("创建对象映射");
        assertThat(audit.at("/data/items/0/paramsSummary/sanitized").asBoolean()).isTrue();
        assertNoSecrets(audit);
        performJson(get("/api/v1/plugin-integration/admin/audit-logs").header("Authorization", bearer("plugin-viewer-token")), 403, 42001);

        Path serviceRoot = Path.of("src/main/java/cn/beiming/pluginintegration");
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
                "cn.beiming.onlinemap.", "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "pluginToken", "pluginSecret", "webhookSecret", "discordToken", "cloudreveToken",
                "backupEncryptionKey", "terminal", "container", "restorePath", "worldDirectory", "internalUrl",
                "internalPath", "resolvedPath", "rawPayload", "rawToken", "credential", "secretKey", "Authorization",
                "requestHeaders", "rm -rf", "Remove-Item -Recurse", "rmdir /s", "rd /s", "del /s", "jdbc:",
                "authorized_keys", "id_rsa", ".env");
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
        body.put("providerType", "PAPER");
        body.put("displayName", "Plugin Provider " + idempotencyKey);
        body.put("pluginName", "BeimingBridge");
        body.put("pluginVersion", "1.0.0");
        body.put("serverKind", "SERVER");
        body.put("instanceRef", Map.of("instanceId", "mc-main"));
        body.put("nodeRef", Map.of("nodeId", "node-main"));
        body.put("publicVisible", false);
        body.put("eventEndpointSummary", "/plugin-events/" + idempotencyKey);
        body.put("allowedEventTypes", List.of("beiming.player_join", "beiming.map_marker"));
        body.put("allowedOrigins", List.of("https://plugins.example.com"));
        body.put("adminNote", "contract seed");
        body.put("reason", "创建插件 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> schemaBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventType", "beiming.player_join");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("version", "1.0.0-" + idempotencyKey);
        body.put("requiredFields", List.of("player", "world"));
        body.put("optionalFields", List.of("dimension"));
        body.put("sensitiveFields", List.of("ip", "token", "webhookSecret"));
        body.put("routingHints", Map.of("targetModule", "ONLINE_MAP"));
        body.put("samplePayloadSummary", Map.of("player", "Steve", "world", "overworld"));
        body.put("reason", "创建事件 schema");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventType", "beiming.player_join");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("sourceInstanceId", "instance-paper-main");
        body.put("dedupeKey", idempotencyKey);
        body.put("origin", "https://plugins.example.com");
        body.put("payload", Map.of("player", "Steve", "world", "overworld", "dimension", "OVERWORLD"));
        body.put("occurredAt", "2026-05-28T00:00:00Z");
        body.put("reason", "接收插件事件");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Plugin Route " + idempotencyKey);
        body.put("eventType", "beiming.player_join");
        body.put("matchers", Map.of("providerId", "provider-paper-main", "sourcePlugin", "BeimingBridge"));
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetAction", "UPSERT_MARKER_PREVIEW_" + idempotencyKey);
        body.put("enabled", true);
        body.put("riskLevel", "MEDIUM");
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "maxEvents", 100));
        body.put("reason", "创建事件路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> syncTaskBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("eventId", "event-seed-player-join");
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetAction", "UPSERT_MARKER_PREVIEW");
        body.put("params", Map.of("mappingId", "mapping-seed"));
        body.put("riskLevel", "MEDIUM");
        body.put("reason", "创建同步任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> mappingBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-paper-main");
        body.put("sourcePlugin", "BeimingBridge");
        body.put("sourceObjectType", "PLAYER_MARKER");
        body.put("sourceObjectKey", "source-" + idempotencyKey);
        body.put("targetModule", "ONLINE_MAP");
        body.put("targetObjectType", "MAP_MARKER");
        body.put("targetObjectId", "marker-" + idempotencyKey);
        body.put("status", "ACTIVE");
        body.put("visibility", "PUBLIC");
        body.put("syncHash", "hash-" + idempotencyKey);
        body.put("reason", "创建对象映射");
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
                "rawToken", "pluginToken", "pluginSecret", "webhookSecret", "discordToken",
                "credential", "secretKey", "nodeToken", "Authorization", "requestHeaders", "stackTrace",
                "internalUrl", "internalPath", "resolvedPath", "worldDirectory", "serverPassword",
                "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken", "backupEncryptionKey",
                "terminal", "container", "restorePath", "/srv/", "C:\\\\", ".env", "authorized_keys",
                "id_rsa", "token=");
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
