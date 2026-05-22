package cn.beiming.serverstatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServerStatusApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ServerStatusStore store;

    @Autowired
    TestAuthContextProvider auth;

    @Autowired
    TestStatusCollector collector;

    @BeforeEach
    void setUp() {
        auth.reset();
        collector.reset();
        store.reset();
        store.seedTestData();
    }

    @Test
    @DisplayName("server-status local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "SS-COM", 1, 21);
        addRange(mapped, "SS-COM", 22, 24);
        addRange(mapped, "SS-PUB-OVERVIEW", 1, 12);
        addRange(mapped, "SS-PUB-INSTANCE", 1, 16);
        addRange(mapped, "SS-PUB-LINE", 1, 10);
        addRange(mapped, "SS-PUB-SNAPSHOT", 1, 14);
        addRange(mapped, "SS-PUB-OUTAGE", 1, 10);
        addRange(mapped, "SS-SOURCE", 1, 40);
        addRange(mapped, "SS-REFRESH", 1, 14);
        addRange(mapped, "SS-REFRESH", 15, 15);
        addRange(mapped, "SS-LINE-ADMIN", 1, 37);
        addRange(mapped, "SS-OUTAGE-ADMIN", 1, 28);
        addRange(mapped, "SS-OUTAGE-STATE", 1, 21);
        addRange(mapped, "SS-AUDIT", 1, 8);
        addRange(mapped, "SS-AUDIT", 9, 9);
        addRange(mapped, "SS-OPS", 1, 7);
        addRange(mapped, "SS-COMPAT", 1, 12);
        assertThat(mapped).contains("SS-COM-001", "SS-PUB-OVERVIEW-012", "SS-SOURCE-040", "SS-COMPAT-012");
        assertThat(mapped).hasSize(255);
    }

    @Test
    @DisplayName("SS-COM common envelope, request id, auth, role, paging, sorting, field isolation, and audit rollback")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/server-status/overview").header("X-Request-Id", "req-status-overview"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-status-overview"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.overallStatus").value("ONLINE"));

        performJson(get("/api/v1/server-status/history/snapshots").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/server-status/history/snapshots").param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/server-status/instances").param("sort", "bad_sort"), 400, 40003);
        performJson(get("/api/v1/server-status/admin/sources"), 401, 41000);
        performJson(get("/api/v1/server-status/admin/sources").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/server-status/admin/sources").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/server-status/admin/sources").header("Authorization", bearer("helper-token")), validSourceBody("helper-fail"), 403, 42001);
        performJson(post("/api/v1/server-status/admin/sources").header("Authorization", bearer("admin-token")), Map.of("instanceName", "x"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), validSourceBody("common-ok"), 201);
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin");

        store.failNextAudit();
        performJson(post("/api/v1/server-status/admin/lines").header("Authorization", bearer("owner-token")), validLineBody("audit-fail.example.com"), 500, 51501);
        assertThat(store.lineIdByEntryAddress("audit-fail.example.com")).isNull();

        JsonNode publicOverview = performJson(get("/api/v1/server-status/overview"), 200);
        assertThat(publicOverview.toString()).doesNotContain("target", "checkTarget", "adminNote", "internalReason", "idempotencyKey", "node", "terminal");
        assertThat(store.usesPreviousServiceImplementation()).isFalse();
    }

    @Test
    @DisplayName("SS-PUB public overview, instances, lines, snapshots, outages, degradation, and field isolation")
    void publicReadContract() throws Exception {
        JsonNode overview = performJson(get("/api/v1/server-status/overview"), 200);
        assertThat(overview.at("/data/overallStatus").asText()).isEqualTo("ONLINE");
        assertThat(overview.at("/data/primaryInstance/instanceId").asText()).isEqualTo("inst-survival");
        assertThat(overview.at("/data/primaryLine/lineId").asText()).isEqualTo("line-main");
        assertThat(overview.at("/data/peakOnlinePlayers").asInt()).isGreaterThanOrEqualTo(32);
        assertThat(overview.at("/data/uptimeSeconds").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(valuesAt(overview, "/data/instances", "instanceId")).doesNotContain("inst-disabled", "inst-hidden", "inst-archived");
        assertThat(valuesAt(overview, "/data/lines", "lineId")).doesNotContain("line-disabled", "line-hidden");

        JsonNode survival = performJson(get("/api/v1/server-status/instances")
                .param("kind", "SURVIVAL")
                .param("status", "ONLINE")
                .param("sort", "onlinePlayers_desc"), 200);
        assertThat(valuesAt(survival, "/data/items", "instanceId")).contains("inst-survival").doesNotContain("inst-creative");
        performJson(get("/api/v1/server-status/instances").param("kind", "BAD"), 400, 40001);
        performJson(get("/api/v1/server-status/instances/missing"), 404, 43500);
        performJson(get("/api/v1/server-status/instances/inst-hidden"), 404, 43500);
        assertThat(performJson(get("/api/v1/server-status/instances/inst-survival"), 200).toString()).doesNotContain("target", "adminNote");

        JsonNode lines = performJson(get("/api/v1/server-status/lines")
                .param("status", "AVAILABLE")
                .param("sort", "latencyMs_asc"), 200);
        assertThat(valuesAt(lines, "/data/items", "lineId")).contains("line-main").doesNotContain("line-disabled", "line-hidden");
        performJson(get("/api/v1/server-status/lines").param("status", "BAD"), 400, 40001);
        assertThat(lines.toString()).doesNotContain("checkTarget", "adminNote");

        JsonNode snapshots = performJson(get("/api/v1/server-status/history/snapshots")
                .param("instanceId", "inst-survival")
                .param("lineId", "line-main")
                .param("status", "ONLINE")
                .param("from", "2026-05-20T00:00:00Z")
                .param("to", "2026-05-23T00:00:00Z")
                .param("sort", "onlinePlayers_desc"), 200);
        assertThat(valuesAt(snapshots, "/data/items", "instanceId")).containsOnly("inst-survival");
        assertThat(snapshots.toString()).doesNotContain("target", "checkTarget");
        performJson(get("/api/v1/server-status/history/snapshots").param("from", "2026-05-23T00:00:00Z").param("to", "2026-05-20T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/server-status/history/snapshots").param("instanceId", "missing"), 404, 43500);

        JsonNode outages = performJson(get("/api/v1/server-status/outages")
                .param("status", "OPEN")
                .param("severity", "HIGH"), 200);
        assertThat(valuesAt(outages, "/data/items", "status")).containsOnly("OPEN");
        assertThat(outages.toString()).doesNotContain("internalReason", "adminNote", "createdBy");
        performJson(get("/api/v1/server-status/outages").param("status", "ARCHIVED"), 400, 40001);

        store.clearSnapshots();
        JsonNode unknown = performJson(get("/api/v1/server-status/overview"), 200);
        assertThat(unknown.at("/data/overallStatus").asText()).isEqualTo("UNKNOWN");
        assertThat(unknown.at("/data/degraded").asBoolean()).isTrue();
        assertThat(values(unknown.at("/data/degradeReasons"))).contains("NO_RECENT_SNAPSHOT");

        store.seedTestData();
        collector.failNextPublicCheckUnavailable();
        JsonNode stale = performJson(get("/api/v1/server-status/overview"), 200);
        assertThat(stale.at("/data/degraded").asBoolean()).isTrue();
        assertThat(values(stale.at("/data/degradeReasons"))).contains("COLLECTOR_UNAVAILABLE");
    }

    @Test
    @DisplayName("SS-SOURCE and SS-REFRESH cover source lifecycle, validation, idempotency, conflicts, collector failures, and audit")
    void sourceAndRefreshContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("helper-token"))
                .param("keyword", "Survival")
                .param("sourceType", "STUB")
                .param("configStatus", "ENABLED")
                .param("instanceKind", "SURVIVAL")
                .param("publicVisible", "true")
                .param("sort", "displayName_asc"), 200);
        assertThat(valuesAt(list, "/data/items", "sourceId")).contains("src-survival");

        JsonNode created = performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), validSourceBody("source-idem-1"), 201);
        assertThat(created.at("/data/configStatus").asText()).isEqualTo("ENABLED");
        assertThat(store.auditActions()).contains("SERVER_STATUS_SOURCE_CREATED");
        JsonNode retry = performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), validSourceBody("source-idem-1"), 201);
        assertThat(retry.at("/data/sourceId").asText()).isEqualTo(created.at("/data/sourceId").asText());
        performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), validSourceBody("source-idem-changed", "source-idem-1"), 409, 43002);
        performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), validSourceBody("duplicate-target", null, "mc.example.com"), 409, 43511);
        performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), mapOf("instanceName", "x", "instanceKind", "BAD", "sourceType", "BAD", "target", "", "timeoutMs", 99, "reason", "bad"), 400, 40001);

        String sourceId = created.at("/data/sourceId").asText();
        JsonNode patched = performJson(patch("/api/v1/server-status/admin/sources/" + sourceId)
                .header("Authorization", bearer("admin-token")), mapOf("instanceName", "Patched Survival", "sortOrder", 3, "publicVisible", false, "updatedBy", "browser", "reason", "patch"), 200);
        assertThat(patched.at("/data/updatedBy").asText()).isEqualTo("admin");
        performJson(patch("/api/v1/server-status/admin/sources/missing")
                .header("Authorization", bearer("admin-token")), mapOf("instanceName", "Missing", "reason", "missing"), 404, 43502);
        performJson(patch("/api/v1/server-status/admin/sources/src-archived")
                .header("Authorization", bearer("admin-token")), mapOf("target", "new-target", "reason", "archived"), 409, 43510);

        performJson(patch("/api/v1/server-status/admin/sources/" + sourceId + "/disable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "disable"), 200);
        long disableAudits = countAuditAction("SERVER_STATUS_SOURCE_DISABLED");
        performJson(patch("/api/v1/server-status/admin/sources/" + sourceId + "/disable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "disable again"), 200);
        assertThat(countAuditAction("SERVER_STATUS_SOURCE_DISABLED")).isEqualTo(disableAudits);
        performJson(post("/api/v1/server-status/admin/sources/" + sourceId + "/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "refresh disabled"), 409, 43510);
        performJson(patch("/api/v1/server-status/admin/sources/" + sourceId + "/enable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "enable"), 200);

        JsonNode refreshed = performJson(post("/api/v1/server-status/admin/sources/" + sourceId + "/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "refresh", "idempotencyKey", "refresh-idem-1"), 200);
        assertThat(refreshed.at("/data/source").asText()).isEqualTo("MANUAL_REFRESH");
        assertThat(store.auditActions()).contains("SERVER_STATUS_SOURCE_REFRESHED");
        JsonNode refreshRetry = performJson(post("/api/v1/server-status/admin/sources/" + sourceId + "/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "refresh", "idempotencyKey", "refresh-idem-1"), 200);
        assertThat(refreshRetry.at("/data/snapshotId").asText()).isEqualTo(refreshed.at("/data/snapshotId").asText());
        performJson(post("/api/v1/server-status/admin/sources/" + sourceId + "/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "changed", "idempotencyKey", "refresh-idem-1"), 409, 43002);

        collector.failNextUnavailable();
        performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "collector unavailable"), 502, 46510);
        collector.failNextTimeout();
        performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "collector timeout"), 504, 46511);
        store.failNextSnapshotWrite();
        performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "snapshot fail"), 500, 51502);
    }

    @Test
    @DisplayName("SS-LINE-ADMIN covers line lifecycle, public visibility, validation, conflicts, idempotency, and audit")
    void lineAdminContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("helper-token"))
                .param("keyword", "Main")
                .param("configStatus", "ENABLED")
                .param("currentStatus", "AVAILABLE")
                .param("publicVisible", "true")
                .param("sort", "name_asc"), 200);
        assertThat(valuesAt(list, "/data/items", "lineId")).contains("line-main");

        JsonNode line = performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), validLineBody("new-line.example.com", "line-idem-1"), 201);
        assertThat(line.at("/data/configStatus").asText()).isEqualTo("ENABLED");
        assertThat(performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), validLineBody("new-line.example.com", "line-idem-1"), 201).at("/data/lineId").asText()).isEqualTo(line.at("/data/lineId").asText());
        performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), validLineBody("changed-line.example.com", "line-idem-1"), 409, 43002);
        performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), validLineBody("play.beiming.example"), 409, 43511);
        performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), mapOf("name", "x", "entryAddress", "", "checkTarget", "", "reason", "bad"), 400, 40001);

        String lineId = line.at("/data/lineId").asText();
        JsonNode patched = performJson(patch("/api/v1/server-status/admin/lines/" + lineId)
                .header("Authorization", bearer("admin-token")), mapOf("description", "Updated line", "publicVisible", false, "updatedBy", "browser", "reason", "patch"), 200);
        assertThat(patched.at("/data/updatedBy").asText()).isEqualTo("admin");
        performJson(patch("/api/v1/server-status/admin/lines/missing")
                .header("Authorization", bearer("admin-token")), mapOf("name", "Missing", "reason", "missing"), 404, 43501);
        performJson(patch("/api/v1/server-status/admin/lines/line-archived")
                .header("Authorization", bearer("admin-token")), mapOf("checkTarget", "new-target", "reason", "archived"), 409, 43510);

        performJson(patch("/api/v1/server-status/admin/lines/" + lineId + "/disable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "disable"), 200);
        long disableAudits = countAuditAction("SERVER_STATUS_LINE_DISABLED");
        performJson(patch("/api/v1/server-status/admin/lines/" + lineId + "/disable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);
        assertThat(countAuditAction("SERVER_STATUS_LINE_DISABLED")).isEqualTo(disableAudits);
        assertThat(valuesAt(performJson(get("/api/v1/server-status/lines"), 200), "/data/items", "lineId")).doesNotContain(lineId);
        performJson(patch("/api/v1/server-status/admin/lines/" + lineId + "/enable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "enable"), 200);
    }

    @Test
    @DisplayName("SS-OUTAGE covers outage admin CRUD, public isolation, and state transitions")
    void outageContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("helper-token"))
                .param("status", "OPEN")
                .param("severity", "HIGH")
                .param("instanceId", "inst-survival")
                .param("lineId", "line-main")
                .param("keyword", "maintenance")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(valuesAt(list, "/data/items", "outageId")).contains("outage-open");
        assertThat(list.toString()).contains("internalReason", "adminNote");

        JsonNode created = performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), validOutageBody("outage-idem-1"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("OPEN");
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin");
        assertThat(performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), validOutageBody("outage-idem-1"), 201).at("/data/outageId").asText()).isEqualTo(created.at("/data/outageId").asText());
        performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), validOutageBody("changed-outage", "outage-idem-1"), 409, 43002);
        performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), mapOf("title", "x", "publicMessage", "", "severity", "BAD", "startedAt", "2027-01-01T00:00:00Z", "reason", "bad"), 400, 40001);
        performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), validOutageBody("bad-instance", null, "missing", "line-main"), 404, 43500);
        performJson(post("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), validOutageBody("bad-line", null, "inst-survival", "missing"), 404, 43501);

        String outageId = created.at("/data/outageId").asText();
        performJson(patch("/api/v1/server-status/admin/outages/" + outageId)
                .header("Authorization", bearer("admin-token")), mapOf("publicMessage", "Updated public message", "internalReason", "Updated internal", "reason", "patch"), 200);
        performJson(patch("/api/v1/server-status/admin/outages/missing")
                .header("Authorization", bearer("admin-token")), mapOf("publicMessage", "Missing", "reason", "missing"), 404, 43504);
        performJson(patch("/api/v1/server-status/admin/outages/outage-archived")
                .header("Authorization", bearer("admin-token")), mapOf("publicMessage", "Nope", "reason", "archived"), 409, 43510);

        performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/acknowledge")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "ack"), 200);
        long ackAudits = countAuditAction("SERVER_STATUS_OUTAGE_ACKNOWLEDGED");
        performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/acknowledge")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "ack again"), 200);
        assertThat(countAuditAction("SERVER_STATUS_OUTAGE_ACKNOWLEDGED")).isEqualTo(ackAudits);

        JsonNode resolved = performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/resolve")
                .header("Authorization", bearer("admin-token")), mapOf("resolvedAt", "2026-05-22T02:00:00Z", "publicMessage", "Recovered", "reason", "resolve"), 200);
        assertThat(resolved.at("/data/status").asText()).isEqualTo("RESOLVED");
        long resolveAudits = countAuditAction("SERVER_STATUS_OUTAGE_RESOLVED");
        performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/resolve")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "resolve again"), 200);
        assertThat(countAuditAction("SERVER_STATUS_OUTAGE_RESOLVED")).isEqualTo(resolveAudits);
        performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/archive")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "archive"), 200);
        long archiveAudits = countAuditAction("SERVER_STATUS_OUTAGE_ARCHIVED");
        performJson(patch("/api/v1/server-status/admin/outages/" + outageId + "/archive")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "archive again"), 200);
        assertThat(countAuditAction("SERVER_STATUS_OUTAGE_ARCHIVED")).isEqualTo(archiveAudits);
        assertThat(valuesAt(performJson(get("/api/v1/server-status/outages"), 200), "/data/items", "outageId")).doesNotContain(outageId);

        performJson(patch("/api/v1/server-status/admin/outages/outage-resolved/acknowledge")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43510);
        performJson(patch("/api/v1/server-status/admin/outages/outage-open/archive")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43510);
        performJson(patch("/api/v1/server-status/admin/outages/outage-open/resolve")
                .header("Authorization", bearer("admin-token")), mapOf("resolvedAt", "2026-05-19T00:00:00Z", "reason", "bad"), 400, 40001);
    }

    @Test
    @DisplayName("SS-AUDIT/OPS/COMPAT cover audit filters, ops summary, auth dependency failures, and service boundaries")
    void auditOpsAndCompatibilityContract() throws Exception {
        performJson(get("/api/v1/server-status/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        JsonNode audits = performJson(get("/api/v1/server-status/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("targetType", "SOURCE")
                .param("targetId", "src-survival")
                .param("actorUserId", "seed")
                .param("from", "2026-05-20T00:00:00Z")
                .param("to", "2026-05-23T00:00:00Z"), 200);
        assertThat(valuesAt(audits, "/data/items", "targetType")).containsOnly("SOURCE");
        assertThat(audits.toString()).doesNotContain("DELETE_AUDIT");
        JsonNode audit = audits.at("/data/items/0");
        assertThat(audit.has("actorPermissions")).isTrue();
        assertThat(audit.has("sourceIp")).isTrue();
        assertThat(audit.has("paramsSummary")).isTrue();
        assertThat(audit.has("beforeState")).isTrue();
        assertThat(audit.has("afterState")).isTrue();
        assertThat(audit.has("failureReason")).isTrue();

        JsonNode ops = performJson(get("/api/v1/server-status/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("server-status");
        assertThat(values(ops.at("/data/warnings"))).contains("P0_IN_MEMORY_STORAGE", "P0_TEST_COLLECTOR");
        assertThat(ops.toString()).doesNotContain("token", "checkTarget", "adminNote", "internalReason");
        performJson(get("/api/v1/server-status/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);

        auth.failNextCurrentUnavailable();
        performJson(get("/api/v1/server-status/admin/sources").header("Authorization", bearer("admin-token")), 502, 46500);
        auth.failNextCurrentTimeout();
        performJson(get("/api/v1/server-status/admin/sources").header("Authorization", bearer("admin-token")), 504, 46501);
        auth.failNextCurrentIncompatible();
        performJson(get("/api/v1/server-status/admin/sources").header("Authorization", bearer("admin-token")), 502, 46502);

        assertThat(store.previousServiceFilesChanged()).isFalse();
        assertThat(store.exposesResourceRoutes()).isFalse();
        assertThat(store.exposesOpsControlRoutes()).isFalse();
    }

    @Test
    @DisplayName("SS-COM-019/SS-SOURCE-026/SS-LINE-ADMIN-024/SS-OUTAGE-ADMIN-027 audit failures roll back mutating updates")
    void mutatingUpdatesRollBackWhenAuditFails() throws Exception {
        store.failNextAudit();
        performJson(patch("/api/v1/server-status/admin/sources/src-survival")
                .header("Authorization", bearer("admin-token")), mapOf("instanceName", "Mutated Survival", "publicVisible", false, "reason", "audit fail"), 500, 51501);
        JsonNode source = findItem(performJson(get("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), 200), "sourceId", "src-survival");
        assertThat(source.path("instanceName").asText()).isEqualTo("Survival");
        assertThat(source.path("publicVisible").asBoolean()).isTrue();

        store.seedTestData();
        store.failNextAudit();
        performJson(patch("/api/v1/server-status/admin/sources/src-survival/disable")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "audit fail"), 500, 51501);
        assertThat(valuesAt(performJson(get("/api/v1/server-status/instances"), 200), "/data/items", "instanceId")).contains("inst-survival");

        store.seedTestData();
        store.failNextAudit();
        performJson(patch("/api/v1/server-status/admin/lines/line-main")
                .header("Authorization", bearer("admin-token")), mapOf("description", "Mutated line", "publicVisible", false, "reason", "audit fail"), 500, 51501);
        JsonNode line = findItem(performJson(get("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), 200), "lineId", "line-main");
        assertThat(line.path("description").asText()).isEqualTo("Main public line");
        assertThat(line.path("publicVisible").asBoolean()).isTrue();

        store.seedTestData();
        store.failNextAudit();
        performJson(patch("/api/v1/server-status/admin/outages/outage-open")
                .header("Authorization", bearer("admin-token")), mapOf("publicMessage", "Mutated outage", "reason", "audit fail"), 500, 51501);
        JsonNode outage = findItem(performJson(get("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), 200), "outageId", "outage-open");
        assertThat(outage.path("publicMessage").asText()).isEqualTo("Maintenance public message");

        store.seedTestData();
        store.failNextAudit();
        performJson(patch("/api/v1/server-status/admin/outages/outage-open/resolve")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "audit fail"), 500, 51501);
        JsonNode unresolved = findItem(performJson(get("/api/v1/server-status/admin/outages")
                .header("Authorization", bearer("admin-token")), 200), "outageId", "outage-open");
        assertThat(unresolved.path("status").asText()).isEqualTo("OPEN");
        assertThat(unresolved.path("resolvedAt").isNull()).isTrue();
    }

    @Test
    @DisplayName("SS-COM-022/023/024 and SS-REFRESH-009/015 harden validation, idempotency, and refresh guards")
    void productionHardeningContract() throws Exception {
        performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), mapOf(
                "instanceName", "Bad Bool",
                "instanceKind", "SURVIVAL",
                "sourceType", "STUB",
                "target", "bad-bool.example.com",
                "publicVisible", "not-bool",
                "reason", "bad bool"
        ), 400, 40001);
        performJson(post("/api/v1/server-status/admin/sources")
                .header("Authorization", bearer("admin-token")), mapOf(
                "instanceName", "Bad Time",
                "instanceKind", "SURVIVAL",
                "sourceType", "STUB",
                "target", "bad-time.example.com",
                "startedAt", "not-an-instant",
                "reason", "bad time"
        ), 400, 40001);

        Map<String, Object> firstLineBody = validLineBody("stable-idem.example.com", "stable-line-key");
        JsonNode firstLine = performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), firstLineBody, 201);
        Map<String, Object> sameLineBodyDifferentOrder = reversedCopy(firstLineBody);
        JsonNode secondLine = performJson(post("/api/v1/server-status/admin/lines")
                .header("Authorization", bearer("admin-token")), sameLineBodyDifferentOrder, 201);
        assertThat(secondLine.at("/data/lineId").asText()).isEqualTo(firstLine.at("/data/lineId").asText());

        JsonNode firstRefresh = performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "first refresh"), 200);
        performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "too soon"), 409, 43512);

        JsonNode idempotentRefresh = performJson(post("/api/v1/server-status/admin/sources/src-creative/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "refresh idem", "idempotencyKey", "refresh-stable-key"), 200);
        JsonNode idempotentRetry = performJson(post("/api/v1/server-status/admin/sources/src-creative/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("idempotencyKey", "refresh-stable-key", "reason", "refresh idem"), 200);
        assertThat(idempotentRetry.at("/data/snapshotId").asText()).isEqualTo(idempotentRefresh.at("/data/snapshotId").asText());
        assertThat(firstRefresh.at("/data/snapshotId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("SS-REFRESH-008 rejects concurrent refresh for the same source")
    void concurrentRefreshForSameSourceReturnsConflict() throws Exception {
        collector.pauseNextCollect();
        CompletableFuture<JsonNode> firstRefresh = CompletableFuture.supplyAsync(() -> {
            try {
                return performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                        .header("Authorization", bearer("admin-token")), mapOf("reason", "slow refresh"), 200);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });

        collector.awaitPausedCollect();
        performJson(post("/api/v1/server-status/admin/sources/src-survival/refresh")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "parallel refresh"), 409, 43512);
        collector.releasePausedCollect();
        assertThat(firstRefresh.get(5, TimeUnit.SECONDS).at("/data/snapshotId").asText()).isNotBlank();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, expectedStatus);
        assertThat(result.path("code").asInt()).isEqualTo(expectedCode);
        return result;
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, body, expectedStatus);
        assertThat(result.path("code").asInt()).isEqualTo(expectedCode);
        return result;
    }

    private Map<String, Object> validSourceBody(String idempotencyKey) {
        return validSourceBody("Instance " + idempotencyKey, idempotencyKey, "status-" + idempotencyKey + ".example.com");
    }

    private Map<String, Object> validSourceBody(String instanceName, String idempotencyKey) {
        return validSourceBody(instanceName, idempotencyKey, "status-" + instanceName + ".example.com");
    }

    private Map<String, Object> validSourceBody(String instanceName, String idempotencyKey, String target) {
        Map<String, Object> body = mapOf(
                "instanceName", instanceName,
                "instanceKind", "SURVIVAL",
                "sourceType", "STUB",
                "target", target,
                "publicVisible", true,
                "primary", false,
                "timeoutMs", 3000,
                "sortOrder", 30,
                "startedAt", "2026-05-20T00:00:00Z",
                "adminNote", "internal source note",
                "createdBy", "browser",
                "reason", "test source"
        );
        if (idempotencyKey != null) body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> validLineBody(String entryAddress) {
        return validLineBody(entryAddress, null);
    }

    private Map<String, Object> validLineBody(String entryAddress, String idempotencyKey) {
        Map<String, Object> body = mapOf(
                "name", "Line " + entryAddress,
                "entryAddress", entryAddress,
                "checkTarget", "https://" + entryAddress + "/health",
                "description", "Public line",
                "publicVisible", true,
                "primary", false,
                "sortOrder", 50,
                "adminNote", "internal line note",
                "createdBy", "browser",
                "reason", "test line"
        );
        if (idempotencyKey != null) body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> validOutageBody(String idempotencyKey) {
        return validOutageBody("Outage " + idempotencyKey, idempotencyKey);
    }

    private Map<String, Object> validOutageBody(String title, String idempotencyKey) {
        return validOutageBody(title, idempotencyKey, "inst-survival", "line-main");
    }

    private Map<String, Object> validOutageBody(String title, String idempotencyKey, String instanceId, String lineId) {
        Map<String, Object> body = mapOf(
                "title", title,
                "publicMessage", "Maintenance public message",
                "severity", "HIGH",
                "instanceId", instanceId,
                "lineId", lineId,
                "startedAt", "2026-05-22T01:00:00Z",
                "internalReason", "maintenance internal reason",
                "adminNote", "internal outage note",
                "publicVisible", true,
                "createdBy", "browser",
                "reason", "test outage"
        );
        if (idempotencyKey != null) body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private Map<String, Object> reversedCopy(Map<String, Object> source) {
        Map<String, Object> reversed = new LinkedHashMap<>();
        List<Map.Entry<String, Object>> entries = new java.util.ArrayList<>(source.entrySet());
        java.util.Collections.reverse(entries);
        for (Map.Entry<String, Object> entry : entries) {
            reversed.put(entry.getKey(), entry.getValue());
        }
        return reversed;
    }

    private List<String> values(JsonNode arrayNode) {
        return java.util.stream.StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private List<String> valuesAt(JsonNode root, String arrayPointer, String fieldName) {
        return java.util.stream.StreamSupport.stream(root.at(arrayPointer).spliterator(), false)
                .map(item -> item.path(fieldName).asText())
                .toList();
    }

    private JsonNode findItem(JsonNode root, String fieldName, String expected) {
        return java.util.stream.StreamSupport.stream(root.at("/data/items").spliterator(), false)
                .filter(item -> expected.equals(item.path(fieldName).asText()))
                .findFirst()
                .orElseThrow();
    }

    private long countAuditAction(String action) {
        return store.auditActions().stream().filter(action::equals).count();
    }

    private void addRange(Set<String> target, String prefix, int from, int to) {
        IntStream.rangeClosed(from, to).mapToObj(value -> prefix + "-" + String.format("%03d", value)).forEach(target::add);
    }
}
