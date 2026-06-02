package cn.beiming.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
abstract class BusinessCoreAdminContractCases {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("admin local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ADM-COM", 1, 30);
        addRange(mapped, "ADM-OVERVIEW", 1, 30);
        addRange(mapped, "ADM-MODULES", 1, 26);
        addRange(mapped, "ADM-MODULE-DETAIL", 1, 18);
        addRange(mapped, "ADM-TODOS", 1, 30);
        addRange(mapped, "ADM-TODO-DETAIL", 1, 18);
        addRange(mapped, "ADM-METRICS", 1, 22);
        addRange(mapped, "ADM-AUDIT", 1, 26);
        addRange(mapped, "ADM-SETTINGS-READ", 1, 18);
        addRange(mapped, "ADM-SETTINGS-WRITE", 1, 34);
        addRange(mapped, "ADM-OPS", 1, 16);
        addRange(mapped, "ADM-COMPAT", 1, 30);
        addRange(mapped, "ADM-MODULE-REFRESH", 1, 34);
        addRange(mapped, "ADM-MATERIAL", 1, 14);
        addRange(mapped, "ADM-GUIDE", 1, 14);
        addRange(mapped, "ADM-P3", 1, 22);
        addRange(mapped, "ADM-GATEWAY", 1, 14);
        addRange(mapped, "ADM-PROD", 1, 20);
        addRange(mapped, "ADM-CYCLE", 1, 18);
        assertThat(mapped).contains("ADM-COM-001", "ADM-OVERVIEW-030", "ADM-SETTINGS-WRITE-034", "ADM-MODULE-REFRESH-034", "ADM-GATEWAY-014", "ADM-PROD-020", "ADM-CYCLE-018");
        assertThat(mapped).hasSize(434);
    }

    @Test
    @DisplayName("ADM-COM common envelope, request id, auth, role, paging, sorting, strict fields, and secret isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Request-Id", "req-admin-overview"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-admin-overview"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.modules").isArray());

        JsonNode generatedRequestId = performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")), 200);
        assertThat(generatedRequestId.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/admin/overview"), 401, 41000);
        performJson(get("/api/v1/admin/overview").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/admin/overview").header("Authorization", bearer("disabled-token")), 502, 46703);
        performJson(get("/api/v1/admin/overview").header("Authorization", bearer("auth-unavailable-token")), 502, 46703);
        performJson(get("/api/v1/admin/overview").header("Authorization", bearer("auth-timeout-token")), 504, 46704);
        performJson(get("/api/v1/admin/todos").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/admin/todos").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/admin/todos").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")).param("includeDisabled", "not-bool"), 400, 40001);
        performJson(get("/api/v1/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 40001);

        JsonNode overview = performJson(get("/api/v1/admin/overview").header("Authorization", bearer("admin-token")), 200);
        assertNoSecrets(overview);
    }

    @Test
    @DisplayName("ADM-OVERVIEW dashboard aggregation, role trimming, module status, degradation, and placeholders")
    void overviewContract() throws Exception {
        JsonNode helper = performJson(get("/api/v1/admin/overview").header("Authorization", bearer("helper-token")), 200);
        assertThat(helper.toString()).contains("CONTENT", "RESOURCE", "SERVER_STATUS", "MATERIAL", "GUIDE").doesNotContain("settings", "audit-logs");
        assertThat(valuesAt(helper, "/data/modules", "moduleKey")).doesNotContain("OPS_CONTROL", "NODE_DAEMON", "ONLINE_MAP", "PLUGIN_INTEGRATION", "OPS_IMAGE_MARKET");

        JsonNode owner = performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("owner-token"))
                .param("includeDisabled", "true")
                .param("moduleLimit", "50")
                .param("todoLimit", "3")
                .param("auditLimit", "2"), 200);
        assertThat(valuesAt(owner, "/data/modules", "moduleKey")).containsAll(expectedImplementedModules()).doesNotContain("API_GATEWAY");
        assertThat(valuesAt(owner, "/data/modules", "moduleKey")).hasSize(26);
        assertThat(owner.at("/data/notImplementedModules").toString()).doesNotContain("ONBOARDING", "OPS_CONTROL", "NODE_DAEMON", "MATERIAL", "GUIDE");
        assertThat(owner.at("/data/platformDependencies").toString()).contains("API_GATEWAY", "/api/v1/gateway/admin", "\"port\":8125");
        assertThat(owner.at("/data/recentAudits").size()).isLessThanOrEqualTo(2);
        assertNoSecrets(owner);

        performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("admin-token"))
                .param("todoLimit", "-1"), 400, 40001);
        performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("admin-token"))
                .param("includeDisabled", "true"), 403, 42001);

        JsonNode degraded = performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Module-Mode", "CONTENT:UNAVAILABLE,RESOURCE:TIMEOUT"), 200);
        assertThat(degraded.at("/data/degradedModules").toString()).contains("CONTENT", "RESOURCE");
        assertThat(degraded.toString()).contains("DEGRADED").doesNotContain("rawInvitationCode", "secret-token", "secret-code");
    }

    @Test
    @DisplayName("ADM-MODULES registry and detail expose stable entries without business write proxies")
    void moduleContract() throws Exception {
        JsonNode modules = performJson(get("/api/v1/admin/modules")
                .header("Authorization", bearer("owner-token"))
                .param("includeNotImplemented", "true")
                .param("sort", "moduleKey_asc"), 200);
        assertThat(valuesAt(modules, "/data/items", "moduleKey")).containsAll(expectedImplementedModules()).doesNotContain("API_GATEWAY");
        assertThat(valuesAt(modules, "/data/items", "moduleKey")).hasSize(26);
        assertThat(modules.toString()).contains("\"targetApiBase\":\"/api/v1/content/admin\"", "\"targetApiBase\":\"/api/v1/resources/admin\"", "\"targetApiBase\":\"/api/v1/materials/admin\"", "\"targetApiBase\":\"/api/v1/guides/admin\"");
        assertThat(modules.toString()).doesNotContain("\"status\":\"NOT_IMPLEMENTED\"", "\"targetApiBase\":null");
        assertThat(modules.toString()).doesNotContain("/api/v1/admin/users", "/api/v1/admin/resources");
        assertNoSecrets(modules);

        JsonNode implementedOnly = performJson(get("/api/v1/admin/modules")
                .header("Authorization", bearer("owner-token"))
                .param("includeNotImplemented", "false"), 200);
        assertThat(valuesAt(implementedOnly, "/data/items", "moduleKey")).containsAll(expectedImplementedModules()).doesNotContain("API_GATEWAY");

        JsonNode adminModules = performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")), 200);
        assertThat(valuesAt(adminModules, "/data/items", "moduleKey")).doesNotContain("OPS_CONTROL", "NODE_DAEMON");
        JsonNode adminWithoutNode = performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-no-node-token")), 200);
        assertThat(valuesAt(adminWithoutNode, "/data/items", "moduleKey")).doesNotContain("ONLINE_MAP", "PLUGIN_INTEGRATION", "OPS_IMAGE_MARKET");
        performJson(get("/api/v1/admin/modules/OPS_CONTROL").header("Authorization", bearer("admin-token")), 403, 42001);
        performJson(get("/api/v1/admin/modules/ONLINE_MAP").header("Authorization", bearer("admin-no-node-token")), 403, 42002);

        JsonNode content = performJson(get("/api/v1/admin/modules/CONTENT").header("Authorization", bearer("helper-token")), 200);
        assertThat(content.at("/data/moduleKey").asText()).isEqualTo("CONTENT");
        assertThat(content.at("/data/capabilities").toString()).contains("READ").doesNotContain("TERMINAL_ACCESS");
        assertThat(content.at("/data/targetApiBase").asText()).isEqualTo("/api/v1/content/admin");

        JsonNode ops = performJson(get("/api/v1/admin/modules/OPS_CONTROL").header("Authorization", bearer("owner-token")), 200);
        assertThat(ops.at("/data/status").asText()).isEqualTo("AVAILABLE");
        assertThat(ops.at("/data/targetApiBase").asText()).isEqualTo("/api/v1/ops-control");
        assertThat(ops.at("/data/requiredPermissions").toString()).contains("NODE_READ");
        assertThat(ops.toString()).doesNotContain("terminal", "container-start", "file-delete", "node-register");

        assertModuleEntry("MATERIAL", "/api/v1/materials/admin", 8126, "/admin/materials");
        assertModuleEntry("GUIDE", "/api/v1/guides/admin", 8127, "/admin/guides");
        assertModuleEntry("CLOUDREVE_SYNC", "/api/v1/cloudreve-sync", 8118, "/admin/cloudreve-sync");
        assertModuleEntry("ONLINE_MAP", "/api/v1/online-map/admin", 8121, "/admin/online-map");
        assertModuleEntry("PLUGIN_INTEGRATION", "/api/v1/plugin-integration/admin", 8122, "/admin/plugin-integration");
        assertModuleEntry("CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification/admin", 8123, "/admin/cross-platform-notification");
        assertModuleEntry("OPS_IMAGE_MARKET", "/api/v1/ops-image-market/admin", 8124, "/admin/ops-image-market");

        performJson(get("/api/v1/admin/modules/BAD").header("Authorization", bearer("admin-token")), 400, 40001);
        performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/admin/modules").header("Authorization", bearer("user-token")), 403, 42001);
    }

    @Test
    @DisplayName("ADM-TODOS aggregate read-only source tasks, filters, paging, degradation, and detail context")
    void todoContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/admin/todos")
                .header("Authorization", bearer("admin-token"))
                .param("page", "1")
                .param("pageSize", "100")
                .param("sort", "severity_desc"), 200);
        assertThat(valuesAt(list, "/data/items", "sourceModule"))
                .contains("AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR", "CHANGELOG", "MATERIAL", "GUIDE")
                .doesNotContain("OPS_CONTROL", "NODE_DAEMON", "API_GATEWAY");
        assertThat(list.toString()).contains("\"readOnly\":true", "CONTENT_REVIEW", "RESOURCE_REVIEW", "MATERIAL_REVIEW", "GUIDE_REVIEW");
        assertNoSecrets(list);

        JsonNode filtered = performJson(get("/api/v1/admin/todos")
                .header("Authorization", bearer("admin-token"))
                .param("sourceModule", "RESOURCE")
                .param("type", "REVIEW")
                .param("keyword", "resource"), 200);
        assertThat(valuesAt(filtered, "/data/items", "sourceModule")).containsOnly("RESOURCE");

        JsonNode secondPage = performJson(get("/api/v1/admin/todos")
                .header("Authorization", bearer("admin-token"))
                .param("page", "2")
                .param("pageSize", "1"), 200);
        assertThat(secondPage.at("/data/page").asInt()).isEqualTo(2);
        assertThat(secondPage.at("/data/items").size()).isEqualTo(1);

        JsonNode detail = performJson(get("/api/v1/admin/todos/todo-content-review-1")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(detail.at("/data/todoId").asText()).isEqualTo("todo-content-review-1");
        assertThat(detail.at("/data/readOnly").asBoolean()).isTrue();
        assertThat(detail.at("/data/targetApi").asText()).startsWith("/api/v1/content/admin");
        assertNoSecrets(detail);

        performJson(get("/api/v1/admin/todos/missing").header("Authorization", bearer("admin-token")), 404, 43701);
        performJson(get("/api/v1/admin/todos").header("Authorization", bearer("admin-token")).param("sourceModule", "BAD"), 400, 40001);
        performJson(get("/api/v1/admin/todos").header("Authorization", bearer("admin-token")).param("severity", "BAD"), 400, 40001);

        JsonNode degraded = performJson(get("/api/v1/admin/todos")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Module-Mode", "CONTENT:UNAVAILABLE"), 200);
        assertThat(degraded.toString()).contains("SOURCE_UNAVAILABLE").doesNotContain("private article body");
    }

    @Test
    @DisplayName("ADM-METRICS summary keeps unknown degraded values distinct from real zero")
    void metricContract() throws Exception {
        JsonNode metrics = performJson(get("/api/v1/admin/metrics/summary").header("Authorization", bearer("owner-token")), 200);
        assertThat(valuesAt(metrics, "/data/items", "sourceModule")).containsAll(expectedImplementedModules()).doesNotContain("API_GATEWAY");
        assertThat(metrics.toString()).contains("content.pendingReview", "resource.pendingReview", "material.pendingReview", "guide.pendingReview", "\"degraded\":false");
        assertNoSecrets(metrics);

        JsonNode helperMetrics = performJson(get("/api/v1/admin/metrics/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(valuesAt(helperMetrics, "/data/items", "sourceModule")).doesNotContain("OPS_CONTROL", "NODE_DAEMON", "ONLINE_MAP", "PLUGIN_INTEGRATION", "OPS_IMAGE_MARKET");

        JsonNode contentMetrics = performJson(get("/api/v1/admin/metrics/summary")
                .header("Authorization", bearer("admin-token"))
                .param("sourceModule", "CONTENT"), 200);
        assertThat(valuesAt(contentMetrics, "/data/items", "sourceModule")).containsOnly("CONTENT");

        JsonNode degraded = performJson(get("/api/v1/admin/metrics/summary")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Module-Mode", "NOTIFICATION:UNAVAILABLE"), 200);
        assertThat(degraded.toString()).contains("NOTIFICATION").contains("\"degraded\":true");

        JsonNode noDegraded = performJson(get("/api/v1/admin/metrics/summary")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Module-Mode", "NOTIFICATION:UNAVAILABLE")
                .param("includeDegraded", "false"), 200);
        assertThat(noDegraded.toString()).doesNotContain("notification.failedDeliveries");

        performJson(get("/api/v1/admin/metrics/summary").header("Authorization", bearer("admin-token")).param("sourceModule", "BAD"), 400, 40001);
        performJson(get("/api/v1/admin/metrics/summary").header("Authorization", bearer("user-token")), 403, 42001);
    }

    @Test
    @DisplayName("ADM-AUDIT index is filtered, paginated, read-only, and sanitized")
    void auditContract() throws Exception {
        JsonNode audit = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("page", "1")
                .param("pageSize", "100")
                .param("sort", "createdAt_desc"), 200);
        assertThat(valuesAt(audit, "/data/items", "sourceModule")).containsAll(expectedAuditSources());
        assertThat(audit.toString()).contains("ADMIN_SETTINGS_UPDATED").doesNotContain("paramsSummary", "secret-token", "secret-code");
        assertNoSecrets(audit);

        JsonNode resourceOnly = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("sourceModule", "RESOURCE")
                .param("actorUserId", "admin")
                .param("result", "SUCCESS"), 200);
        assertThat(valuesAt(resourceOnly, "/data/items", "sourceModule")).containsOnly("RESOURCE");

        JsonNode pageTwo = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("page", "2")
                .param("pageSize", "1"), 200);
        assertThat(pageTwo.at("/data/page").asInt()).isEqualTo(2);
        assertThat(pageTwo.at("/data/items").size()).isEqualTo(1);

        performJson(get("/api/v1/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/admin/audit-logs").header("Authorization", bearer("admin-token")).param("sourceModule", "BAD"), 400, 40001);
        performJson(get("/api/v1/admin/audit-logs").header("Authorization", bearer("admin-token")).param("result", "BAD"), 400, 40001);
        performJson(get("/api/v1/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "2026-05-23T00:00:00Z").param("to", "2026-05-22T00:00:00Z"), 400, 40001);
        JsonNode futureRange = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2027-01-01T00:00:00Z")
                .param("to", "2027-12-31T23:59:59Z"), 200);
        assertThat(futureRange.at("/data/total").asInt()).isZero();

        JsonNode degraded = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Module-Mode", "CONTENT:UNAVAILABLE"), 200);
        assertThat(degraded.toString()).contains("CONTENT").contains("DEGRADED");
    }

    @Test
    @DisplayName("ADM-SETTINGS read and write cover scope, high impact owner gate, idempotency, and rollback")
    void settingsContract() throws Exception {
        JsonNode settings = performJson(get("/api/v1/admin/settings")
                .header("Authorization", bearer("admin-token"))
                .param("scope", "NAVIGATION"), 200);
        assertThat(settings.at("/data/layout/navigationModuleOrder").isArray()).isTrue();
        assertThat(settings.toString()).doesNotContain("content.homepage", "resource.downloadEntry", "server.lines");
        assertNoSecrets(settings);

        performJson(get("/api/v1/admin/settings").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/admin/settings").header("Authorization", bearer("admin-token")).param("includeHighImpact", "true"), 403, 42001);
        performJson(get("/api/v1/admin/settings").header("Authorization", bearer("owner-token")).param("includeHighImpact", "true"), 200);
        performJson(get("/api/v1/admin/settings").header("Authorization", bearer("admin-token")).param("scope", "BAD"), 400, 40001);

        Map<String, Object> body = settingsPatchBody("settings-idem-1", "update navigation", List.of("AUTH", "CONTENT", "RESOURCE", "ADMIN"), List.of("NODE_DAEMON"));
        JsonNode updated = performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), body, 200);
        assertThat(updated.at("/data/layout/navigationModuleOrder").toString()).contains("AUTH", "CONTENT", "RESOURCE");

        JsonNode replay = performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), body, 200);
        assertThat(replay.at("/data/idempotency/replayed").asBoolean()).isTrue();

        Map<String, Object> conflict = settingsPatchBody("settings-idem-1", "different", List.of("AUTH", "ADMIN"), List.of());
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), conflict, 409, 43712);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("helper-token")), body, 403, 42001);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), Map.of("idempotencyKey", "no-reason"), 400, 40001);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), Map.of("reason", "missing idem"), 400, 40001);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), highImpactBody("hide-auth"), 403, 42001);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("owner-token")), highImpactBody("hide-auth"), 200);
        performJson(patch("/api/v1/admin/settings")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Fail-Audit", "true"), settingsPatchBody("audit-fail", "audit fail", List.of("AUTH", "ADMIN"), List.of()), 500, 51701);
        performJson(patch("/api/v1/admin/settings")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Fail-Settings", "true"), settingsPatchBody("settings-fail", "settings fail", List.of("AUTH", "ADMIN"), List.of()), 500, 51702);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")),
                invalidLayoutListBody("bad-navigation-card", "navigationModuleOrder", List.of("AUTH", "todos")), 400, 40001);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")),
                invalidLayoutListBody("bad-hidden-card", "hiddenModules", List.of("RESOURCE", "health")), 400, 40001);

        JsonNode afterFailures = performJson(get("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), 200);
        assertThat(afterFailures.toString()).doesNotContain("audit fail", "settings fail");
        assertNoSecrets(afterFailures);
    }

    @Test
    @DisplayName("ADM-HARDENING hidden modules, quick actions, and nested idempotency are enforced")
    void hardeningContract() throws Exception {
        JsonNode hidden = performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), hideResourceBody("hide-resource-1"), 200);
        assertThat(hidden.at("/data/layout/hiddenModules").toString()).contains("RESOURCE");

        JsonNode adminModules = performJson(get("/api/v1/admin/modules").header("Authorization", bearer("admin-token")), 200);
        assertThat(valuesAt(adminModules, "/data/items", "moduleKey")).doesNotContain("RESOURCE");
        performJson(get("/api/v1/admin/modules/RESOURCE").header("Authorization", bearer("admin-token")), 409, 43713);

        JsonNode ownerModules = performJson(get("/api/v1/admin/modules")
                .header("Authorization", bearer("owner-token"))
                .param("includeDisabled", "true"), 200);
        assertThat(ownerModules.toString()).contains("\"moduleKey\":\"RESOURCE\"", "\"status\":\"DISABLED\"");
        JsonNode ownerResource = performJson(get("/api/v1/admin/modules/RESOURCE").header("Authorization", bearer("owner-token")), 200);
        assertThat(ownerResource.at("/data/status").asText()).isEqualTo("DISABLED");

        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), invalidQuickActionBody("bad-quick-action"), 409, 43713);
        performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")),
                quickActionBody("bad-ops-logs", "ops-logs", "/admin/ops-control/logs/live"), 409, 43713);

        Map<String, Object> first = nestedIdempotencyBody("nested-idem-1", true);
        JsonNode updated = performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), first, 200);
        assertThat(updated.at("/data/idempotency/replayed").asBoolean()).isFalse();

        Map<String, Object> sameDifferentOrder = nestedIdempotencyBody("nested-idem-1", false);
        JsonNode replay = performJson(patch("/api/v1/admin/settings").header("Authorization", bearer("admin-token")), sameDifferentOrder, 200);
        assertThat(replay.at("/data/idempotency/replayed").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("ADM-OPS summary reports runtime modes, counts, production gaps, and no secrets")
    void opsContract() throws Exception {
        JsonNode ops = performJson(get("/api/v1/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("admin");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8107);
        assertThat(ops.at("/data/modulesTotal").asInt()).isEqualTo(26);
        assertThat(ops.at("/data/availableModulesTotal").asInt()).isEqualTo(26);
        assertThat(ops.at("/data/notImplementedModulesTotal").asInt()).isZero();
        assertThat(ops.at("/data/platformDependencies").toString()).contains("API_GATEWAY", "/api/v1/gateway/admin", "\"port\":8125");
        assertThat(ops.toString()).contains("IN_MEMORY", "TEST_STUB", "productionGaps", "moduleHealth");
        assertNoSecrets(ops);

        JsonNode authModule = performJson(get("/api/v1/admin/modules/AUTH").header("Authorization", bearer("owner-token")), 200);
        assertThat(authModule.at("/data/health/port").asInt()).isEqualTo(8101);

        JsonNode degraded = performJson(get("/api/v1/admin/ops/summary")
                .header("Authorization", bearer("owner-token"))
                .header("X-Test-Module-Mode", "RESOURCE:UNAVAILABLE"), 200);
        assertThat(degraded.at("/data/degradedModulesTotal").asInt()).isGreaterThanOrEqualTo(1);

        performJson(get("/api/v1/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
    }

    @Test
    @DisplayName("ADM-COMPAT admin service does not modify prior services or expose business and ops write proxies")
    void compatibilityContract() throws Exception {
        Path serviceRoot = adminServiceSourceRoot();
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
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.",
                "cn.beiming.content.", "cn.beiming.serverstatus.", "cn.beiming.resource.",
                "ProcessBuilder", "Runtime.getRuntime", "deleteFile", "TerminalController",
                "@GetMapping(\"/terminal", "@PostMapping(\"/terminal", "executeTerminal",
                "terminalCommand", "containerStart", "nodeRegister", "backupRestore", "cloudreveToken");

        JsonNode modules = performJson(get("/api/v1/admin/modules").header("Authorization", bearer("owner-token")), 200);
        assertThat(valuesAt(modules, "/data/items", "moduleKey")).doesNotContain("API_GATEWAY");
        assertThat(modules.toString()).doesNotContain("/api/v1/admin/users", "/api/v1/admin/items", "/api/v1/admin/files", "/api/v1/admin/terminal", "/api/v1/admin/containers");
    }

    @Test
    @DisplayName("ADM-MODULE-REFRESH, ADM-MATERIAL, ADM-GUIDE, ADM-P3, and ADM-GATEWAY refresh current ecosystem compatibility")
    void compatibilityRefreshContract() throws Exception {
        JsonNode modules = performJson(get("/api/v1/admin/modules")
                .header("Authorization", bearer("owner-token"))
                .param("sort", "moduleKey_asc"), 200);
        List<String> keys = valuesAt(modules, "/data/items", "moduleKey");
        assertThat(keys).containsAll(expectedImplementedModules()).hasSize(26);
        assertThat(modules.toString()).doesNotContain("NOT_IMPLEMENTED", "API_GATEWAY", "\"targetApiBase\":null", "rawInvitationCode", "cloudrevePassword", "registryToken");

        JsonNode overview = performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("owner-token"))
                .param("moduleLimit", "50"), 200);
        assertThat(valuesAt(overview, "/data/modules", "moduleKey")).containsAll(expectedImplementedModules()).doesNotContain("API_GATEWAY");
        assertThat(overview.at("/data/notImplementedModules").toString()).doesNotContain("AUTH", "ONBOARDING", "OPS_CONTROL", "MATERIAL", "GUIDE");
        assertThat(overview.at("/data/platformDependencies").toString()).contains("API_GATEWAY", "\"routeCount\":26");

        JsonNode gatewayDegraded = performJson(get("/api/v1/admin/overview")
                .header("Authorization", bearer("owner-token"))
                .header("X-Test-Platform-Mode", "API_GATEWAY:UNAVAILABLE"), 200);
        assertThat(gatewayDegraded.at("/data/platformDependencies").toString()).contains("API_GATEWAY", "UNAVAILABLE");
        assertThat(valuesAt(gatewayDegraded, "/data/modules", "moduleKey")).contains("ADMIN", "GUIDE", "MATERIAL");

        JsonNode guideDegraded = performJson(get("/api/v1/admin/modules/GUIDE")
                .header("Authorization", bearer("owner-token"))
                .header("X-Test-Module-Mode", "GUIDE:TIMEOUT"), 200);
        assertThat(guideDegraded.at("/data/status").asText()).isEqualTo("UNAVAILABLE");
        assertThat(guideDegraded.at("/data/health/degraded").asBoolean()).isTrue();

        JsonNode p3Audit = performJson(get("/api/v1/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("sourceModule", "OPS_IMAGE_MARKET"), 200);
        assertThat(valuesAt(p3Audit, "/data/items", "sourceModule")).containsOnly("OPS_IMAGE_MARKET");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, int status) throws Exception {
        MvcResult result = mvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, int status, int code) throws Exception {
        JsonNode json = performJson(request, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(request, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        return json;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }

    private List<String> expectedImplementedModules() {
        return List.of("AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN",
                "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR",
                "CHANGELOG", "OPS_CONTROL", "NODE_DAEMON", "CLOUDREVE_SYNC", "BACKUP_RECOVERY", "ALERTING",
                "ONLINE_MAP", "PLUGIN_INTEGRATION", "CROSS_PLATFORM_NOTIFICATION", "OPS_IMAGE_MARKET",
                "MATERIAL", "GUIDE");
    }

    private List<String> expectedAuditSources() {
        List<String> sources = new ArrayList<>(expectedImplementedModules());
        sources.add("API_GATEWAY");
        return sources;
    }

    private void assertModuleEntry(String moduleKey, String targetApiBase, int port, String frontendRoute) throws Exception {
        JsonNode detail = performJson(get("/api/v1/admin/modules/" + moduleKey).header("Authorization", bearer("owner-token")), 200);
        assertThat(detail.at("/data/moduleKey").asText()).isEqualTo(moduleKey);
        assertThat(detail.at("/data/status").asText()).isEqualTo("AVAILABLE");
        assertThat(detail.at("/data/implemented").asBoolean()).isTrue();
        assertThat(detail.at("/data/targetApiBase").asText()).isEqualTo(targetApiBase);
        assertThat(detail.at("/data/health/port").asInt()).isEqualTo(port);
        assertThat(detail.at("/data/frontendRoute").asText()).isEqualTo(frontendRoute);
        assertNoSecrets(detail);
    }

    private List<String> valuesAt(JsonNode root, String pointer, String field) {
        JsonNode array = root.at(pointer);
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                values.add(item.path(field).asText());
            }
        }
        return values;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "BM-SECRET-RAW", "secret-code", "secret-token", "C:\\\\server\\\\secret",
                "private message", "private template", "private note", "java.lang.Secret",
                "cloudrevePassword", "authorizationHeader", "rawInvitationCode", "stackTrace");
    }

    private Path adminServiceSourceRoot() {
        Path repositoryRootPath = Path.of("backend/business-core-service/src/main/java/cn/beiming/admin");
        if (Files.exists(repositoryRootPath)) {
            return repositoryRootPath;
        }
        return Path.of("src/main/java/cn/beiming/admin");
    }

    private Map<String, Object> settingsPatchBody(String idempotencyKey, String reason, List<String> order, List<String> hidden) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("navigationModuleOrder", order);
        layout.put("hiddenModules", hidden);
        layout.put("dashboardCards", List.of("todos", "metrics", "health"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "dashboard.refreshSeconds", "value", 60)));
        body.put("reason", reason);
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> highImpactBody(String idempotencyKey) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("hiddenModules", List.of("AUTH"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "audit.retentionDays", "value", 180)));
        body.put("reason", "owner high impact change");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> hideResourceBody(String idempotencyKey) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("hiddenModules", List.of("RESOURCE"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("reason", "hide resource from navigation");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> invalidQuickActionBody(String idempotencyKey) {
        return quickActionBody(idempotencyKey, "ops-control", "/admin/ops-control/terminal");
    }

    private Map<String, Object> quickActionBody(String idempotencyKey, String key, String targetRoute) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("quickActions", List.of(Map.of("key", key, "targetRoute", targetRoute)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("reason", "invalid quick action");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> invalidLayoutListBody(String idempotencyKey, String field, List<String> values) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put(field, values);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("reason", "invalid layout list");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> nestedIdempotencyBody(String idempotencyKey, boolean keyFirst) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (keyFirst) {
            action.put("key", "content-review");
            action.put("targetRoute", "/admin/content");
        } else {
            action.put("targetRoute", "/admin/content");
            action.put("key", "content-review");
        }
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("quickActions", List.of(action));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("reason", "nested idempotency order");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }
}
