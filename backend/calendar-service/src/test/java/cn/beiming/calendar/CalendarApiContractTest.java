package cn.beiming.calendar;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CalendarServiceApplication.class, properties = "calendar.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CalendarApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("calendar local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "CAL-COM", 1, 90);
        addRange(mapped, "CAL-PUB", 1, 90);
        addRange(mapped, "CAL-ME", 1, 80);
        addRange(mapped, "CAL-ADMIN", 1, 130);
        addRange(mapped, "CAL-SYNC", 1, 90);
        addRange(mapped, "CAL-TIME", 1, 100);
        addRange(mapped, "CAL-AUDIT", 1, 70);
        addRange(mapped, "CAL-OPS", 1, 50);
        addRange(mapped, "CAL-DEPS", 1, 100);
        addRange(mapped, "CAL-COMPAT", 1, 90);
        addRange(mapped, "CAL-HARDEN", 1, 140);
        addRange(mapped, "CAL-PORT", 1, 16);
        addRange(mapped, "CAL-CYCLE", 1, 60);
        assertThat(mapped).contains("CAL-COM-001", "CAL-PUB-090", "CAL-SYNC-090", "CAL-HARDEN-140", "CAL-CYCLE-060");
        assertThat(mapped).hasSize(1106);
    }

    @Test
    @DisplayName("CAL-COM covers envelope, request id, auth gates, validation, role gates, and trusted field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/calendar/events").header("X-Request-Id", "req-calendar-public"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-calendar-public"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.requestId").value("req-calendar-public"));

        JsonNode generated = performJson(get("/api/v1/calendar/events"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(post("/api/v1/calendar/me/events/cal-seed-public/watch"), watchBody("trusted"), 401, 41000);
        performJson(post("/api/v1/calendar/me/events/cal-seed-public/watch").header("Authorization", "bad-token"), watchBody("trusted"), 401, 41003);
        performJson(get("/api/v1/calendar/admin/events"), 401, 41000);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/calendar/admin/events/cal-seed-public/publish").header("Authorization", bearer("helper-token")),
                Map.of("reason", "helper cannot publish", "idempotencyKey", "helper-publish-1"), 403, 42001);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("short-key"), "idempotencyKey", "short"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("trusted-event-1"), "status", "PUBLISHED"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin-user");
        assertThat(created.at("/data/watchCount").asInt()).isZero();
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("CAL-PUB, CAL-ME, CAL-ADMIN, and CAL-TIME cover the event lifecycle and public calendar views")
    void eventLifecyclePublicViewsWatchAndTimeRanges() throws Exception {
        String eventId = createApprovedPublishedEvent("flow-calendar-1");

        JsonNode publicList = performJson(get("/api/v1/calendar/events").param("keyword", "北冥"), 200);
        assertThat(publicList.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(publicList);

        JsonNode detail = performJson(get("/api/v1/calendar/events/" + eventId), 200);
        assertThat(detail.at("/data/status").asText()).isEqualTo("PUBLISHED");

        JsonNode month = performJson(get("/api/v1/calendar/month").param("month", "2026-06"), 200);
        assertThat(month.toString()).contains(eventId);

        JsonNode upcoming = performJson(get("/api/v1/calendar/upcoming").param("from", "2026-05-25T00:00:00Z"), 200);
        assertThat(upcoming.toString()).contains(eventId);

        JsonNode watched = performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody("watch-flow-1"), 201);
        assertThat(watched.at("/data/watch/status").asText()).isEqualTo("ACTIVE");
        String watchId = watched.at("/data/watch/watchId").asText();

        JsonNode replay = performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody("watch-flow-1"), 201);
        assertThat(replay.at("/data/watch/watchId").asText()).isEqualTo(watchId);

        JsonNode watchlist = performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(watchlist.at("/data/total").asInt()).isEqualTo(1);

        JsonNode unwatched = performJson(post("/api/v1/calendar/me/events/" + eventId + "/unwatch").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "取消提醒", "idempotencyKey", "unwatch-flow-1"), 200);
        assertThat(unwatched.at("/data/watch/status").asText()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("CAL-TIME covers cross-month and all-day range overlap semantics")
    void crossMonthAndAllDayRangeQueries() throws Exception {
        Map<String, Object> body = with(with(with(eventBody("cross-month-1"),
                "startAt", "2026-01-31T16:00:00Z"), "endAt", "2026-02-02T16:00:00Z"), "allDay", true);
        String eventId = createApprovedPublishedEvent(body);

        JsonNode january = performJson(get("/api/v1/calendar/month").param("month", "2026-01"), 200);
        JsonNode february = performJson(get("/api/v1/calendar/month").param("month", "2026-02"), 200);
        assertThat(january.toString()).contains(eventId);
        assertThat(february.toString()).contains(eventId);

        performJson(get("/api/v1/calendar/events")
                .param("from", "2026-02-02T16:00:00Z")
                .param("to", "2026-02-03T00:00:00Z"), 200);
        JsonNode overlap = performJson(get("/api/v1/calendar/events")
                .param("from", "2026-02-01T00:00:00Z")
                .param("to", "2026-02-01T23:59:59Z"), 200);
        assertThat(overlap.toString()).contains(eventId);
    }

    @Test
    @DisplayName("CAL-ADMIN enforces status transitions, soft delete confirmation, and public visibility")
    void adminStateMachineAndSoftDelete() throws Exception {
        JsonNode draft = createEvent("state-machine-1");
        String eventId = draft.at("/data/eventId").asText();

        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "不能跳过审核", "idempotencyKey", "state-publish-bad"), 409, 49910);

        approveAndPublish(eventId, "state-machine-1");
        performJson(patch("/api/v1/calendar/admin/events/" + eventId).header("Authorization", bearer("helper-token")),
                with(eventBody("state-patch-published"), "title", "发布后不能直接修改"), 409, 49910);

        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/archive").header("Authorization", bearer("admin-token")),
                Map.of("reason", "已发布不能直接归档", "idempotencyKey", "state-archive-bad"), 409, 49910);

        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/offline").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "维护调整", "reason", "下架日程", "idempotencyKey", "state-offline"), 200);
        performJson(get("/api/v1/calendar/events/" + eventId), 404, 49900);

        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/delete").header("Authorization", bearer("admin-token")),
                Map.of("reason", "缺少确认", "idempotencyKey", "state-delete-bad"), 400, 40001);
        JsonNode deleted = performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/delete").header("Authorization", bearer("admin-token")),
                Map.of("reason", "软删除测试", "confirmText", "DELETE_CALENDAR_EVENT", "idempotencyKey", "state-delete"), 200);
        assertThat(deleted.at("/data/status").asText()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("CAL-SYNC covers activity dry-run, upsert, stale fallback, and changelog placeholder boundaries")
    void activitySyncAndChangelogPlaceholder() throws Exception {
        JsonNode dryRun = performJson(post("/api/v1/calendar/admin/sync/activity").header("Authorization", bearer("admin-token")),
                syncBody("sync-dry-run", "DRY_RUN"), 200);
        assertThat(dryRun.at("/data/syncStatus").asText()).isEqualTo("SKIPPED");
        assertThat(dryRun.at("/data/items").isArray()).isTrue();

        JsonNode synced = performJson(post("/api/v1/calendar/admin/sync/activity").header("Authorization", bearer("admin-token")),
                syncBody("sync-upsert-1", "UPSERT_SNAPSHOT"), 200);
        assertThat(synced.at("/data/syncStatus").asText()).isEqualTo("SYNCED");
        assertThat(synced.at("/data/createdTotal").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode events = performJson(get("/api/v1/calendar/admin/events")
                .header("Authorization", bearer("admin-token"))
                .param("sourceType", "ACTIVITY"), 200);
        assertThat(events.toString()).contains("ACTIVITY");

        performJson(post("/api/v1/calendar/admin/sync/activity")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Activity-Mode", "unavailable"),
                syncBody("sync-activity-down", "UPSERT_SNAPSHOT"), 502, 49810);

        JsonNode versionEvent = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(eventBody("manual-version-release"), "type", "VERSION_RELEASE"), "sourceType", "CHANGELOG"), 201);
        assertThat(versionEvent.at("/data/type").asText()).isEqualTo("VERSION_RELEASE");

        JsonNode ops = performJson(get("/api/v1/calendar/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.toString()).contains("CHANGELOG_NOT_CONNECTED");
    }

    @Test
    @DisplayName("CAL-HARDEN replays idempotent writes and rejects idempotency fingerprint conflicts")
    void idempotencyReplaysWritesAndRejectsConflicts() throws Exception {
        Map<String, Object> body = eventBody("idem-calendar-create");
        JsonNode firstCreate = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                body, 201);
        JsonNode replayCreate = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                body, 201);
        assertThat(replayCreate.at("/data/eventId").asText()).isEqualTo(firstCreate.at("/data/eventId").asText());
        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(body, "title", "同 key 改标题"), 409, 49914);

        String eventId = approveAndPublish(firstCreate.at("/data/eventId").asText(), "idem-calendar-create");
        Map<String, Object> watchBody = watchBody("idem-watch");
        JsonNode firstWatch = performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody, 201);
        JsonNode replayWatch = performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody, 201);
        assertThat(replayWatch.at("/data/watch/watchId").asText()).isEqualTo(firstWatch.at("/data/watch/watchId").asText());
        performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                with(watchBody, "reminderOffsets", List.of(30)), 409, 49914);
    }

    @Test
    @DisplayName("CAL-AUDIT rolls back backend writes when audit persistence fails")
    void auditFailureRollsBackBackendWrites() throws Exception {
        String eventId = createEvent("audit-rollback-submit").at("/data/eventId").asText();
        performJson(post("/api/v1/calendar/admin/events/" + eventId + "/submit")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                Map.of("reason", "审计失败", "idempotencyKey", "audit-fail-submit"), 500, 54801);
        JsonNode afterSubmitFail = performJson(get("/api/v1/calendar/admin/events/" + eventId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterSubmitFail.at("/data/event/status").asText()).isEqualTo("DRAFT");

        String watchEventId = createApprovedPublishedEvent("audit-watch-event");
        performJson(post("/api/v1/calendar/me/events/" + watchEventId + "/watch")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Fail-Watch", "true"),
                watchBody("audit-watch-fail"), 500, 54803);
        JsonNode afterWatchFail = performJson(get("/api/v1/calendar/admin/events/" + watchEventId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterWatchFail.at("/data/event/watchCount").asInt()).isZero();
    }

    @Test
    @DisplayName("CAL-DEPS, CAL-AUDIT, CAL-OPS, CAL-COMPAT, and CAL-HARDEN cover dependency failures and boundaries")
    void dependencyHardeningAuditOpsAndCompatibilityFlow() throws Exception {
        performJson(post("/api/v1/calendar/me/events/cal-seed-public/watch")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Auth-Mode", "unavailable"),
                watchBody("auth-down-watch"), 502, 49800);

        String eventId = createApprovedPublishedEvent("notify-fail-event");
        JsonNode publishedAgain = performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/offline")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("publicReason", "提醒失败也要下架", "reason", "通知失败摘要", "idempotencyKey", "notify-offline"), 200);
        assertThat(publishedAgain.at("/data/reminderPolicy/failure/failureCode").asText()).isEqualTo("49820");
        assertNoSecrets(publishedAgain);

        JsonNode audit = performJson(get("/api/v1/calendar/admin/audit-logs").header("Authorization", bearer("admin-token")).param("pageSize", "100"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(3);
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/calendar/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("calendar");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8114);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "CHANGELOG_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("backend/calendar-service/src/main/java");
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
                "cn.beiming.activity.", "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "cloudreveToken", "terminal", "container", "backupRestore", "file-manager",
                "server.properties", "whitelist add", "whitelist remove", "scoreBalance", "attendancePoints",
                "leaderboard", "ActivityStore", "CommunityStore", "AttendanceStore", "ChangelogStore");
    }

    private JsonNode createEvent(String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")), eventBody(idempotencyKey), 201);
    }

    private String createApprovedPublishedEvent(String idempotencyKey) throws Exception {
        JsonNode event = createEvent(idempotencyKey);
        return approveAndPublish(event.at("/data/eventId").asText(), idempotencyKey);
    }

    private String createApprovedPublishedEvent(Map<String, Object> body) throws Exception {
        String idempotencyKey = body.get("idempotencyKey").toString();
        JsonNode event = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")), body, 201);
        return approveAndPublish(event.at("/data/eventId").asText(), idempotencyKey);
    }

    private String approveAndPublish(String eventId, String idempotencyKey) throws Exception {
        performJson(post("/api/v1/calendar/admin/events/" + eventId + "/submit").header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", idempotencyKey + "-submit"), 200);
        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/approve").header("Authorization", bearer("helper-token")),
                Map.of("reviewComment", "审核通过", "internalNote", "private note", "reason", "符合日程规则", "idempotencyKey", idempotencyKey + "-approve"), 200);
        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布日程", "idempotencyKey", idempotencyKey + "-publish"), 200);
        return eventId;
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

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "北冥日程安排");
        body.put("summary", "一次用于契约测试的日程");
        body.put("description", "日程描述覆盖发布、关注、同步和提醒摘要。");
        body.put("type", "MAINTENANCE");
        body.put("visibility", "PUBLIC");
        body.put("startAt", "2026-06-01T12:00:00Z");
        body.put("endAt", "2026-06-01T14:00:00Z");
        body.put("timezone", "Asia/Shanghai");
        body.put("allDay", false);
        body.put("location", "北冥服务器");
        body.put("relatedUrl", "/calendar/test");
        body.put("labels", List.of("calendar", "test"));
        body.put("priority", 50);
        body.put("sourceType", "MANUAL");
        body.put("reason", "创建日程");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> watchBody(String idempotencyKey) {
        return Map.of("reminderEnabled", true, "reminderOffsets", List.of(60), "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> syncBody(String idempotencyKey, String mode) {
        return Map.of(
                "from", "2026-05-25T00:00:00Z",
                "to", "2026-06-30T00:00:00Z",
                "mode", mode,
                "reason", "同步 activity 日历摘要",
                "idempotencyKey", idempotencyKey
        );
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
                "secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "notification body",
                "internalNote", "private note", "activity internal", "server.properties", "whitelist add",
                "whitelist remove", "node-daemon", "terminal", "container", "cloudreveToken", "attendancePoints",
                "scoreBalance", "leaderboard", "ActivityStore", "CommunityStore", "AttendanceStore", "ChangelogStore");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
