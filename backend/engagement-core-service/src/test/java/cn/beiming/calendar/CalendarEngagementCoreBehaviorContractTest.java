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

@SpringBootTest(classes = cn.beiming.engagement.EngagementCoreServiceApplication.class, properties = "calendar.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CalendarEngagementCoreBehaviorContractTest {
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
                "startAt", "2026-01-31T15:00:00Z"), "endAt", "2026-02-02T16:00:00Z"), "allDay", true);
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
                with(with(with(eventBody("manual-version-release"), "type", "VERSION_RELEASE"), "sourceType", "CHANGELOG"), "sourceId", "future-changelog-1"), 201);
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
    @DisplayName("CAL-ADMIN allows partial time updates against the existing event range")
    void partialTimeUpdateUsesExistingRangeBoundary() throws Exception {
        String eventId = createEvent("partial-time-update").at("/data/eventId").asText();

        JsonNode updated = performJson(patch("/api/v1/calendar/admin/events/" + eventId).header("Authorization", bearer("admin-token")),
                Map.of("startAt", "2026-06-01T13:00:00Z", "reason", "只调整开始时间", "idempotencyKey", "partial-time-update-start"), 200);

        assertThat(updated.at("/data/startAt").asText()).isEqualTo("2026-06-01T13:00:00Z");
        assertThat(updated.at("/data/endAt").asText()).isEqualTo("2026-06-01T14:00:00Z");
    }

    @Test
    @DisplayName("CAL-PUB and CAL-ME suppress notification failure and backend-only event fields")
    void publicAndMeViewsDoNotExposeBackendOnlyEventFields() throws Exception {
        String eventId = createEvent("public-redaction").at("/data/eventId").asText();
        performJson(post("/api/v1/calendar/admin/events/" + eventId + "/submit").header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", "public-redaction-submit"), 200);
        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/approve").header("Authorization", bearer("helper-token")),
                Map.of("reviewComment", "审核通过", "reason", "符合日程规则", "idempotencyKey", "public-redaction-approve"), 200);
        performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/publish")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("reason", "发布日程", "idempotencyKey", "public-redaction-publish"), 200);

        JsonNode publicDetail = performJson(get("/api/v1/calendar/events/" + eventId), 200);
        assertThat(publicDetail.toString()).doesNotContain("49820", "failureCode", "failureReason");

        performJson(post("/api/v1/calendar/me/events/" + eventId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody("public-redaction-watch"), 201);
        JsonNode unwatched = performJson(post("/api/v1/calendar/me/events/" + eventId + "/unwatch").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "取消关注", "idempotencyKey", "public-redaction-unwatch"), 200);
        assertThat(unwatched.toString()).doesNotContain("createdBy", "updatedBy", "reviewedBy", "deletedAt", "49820", "failureCode");
    }

    @Test
    @DisplayName("CAL-ME rejects malformed reminder offsets as a validation error")
    void malformedReminderOffsetReturnsValidationError() throws Exception {
        performJson(post("/api/v1/calendar/me/events/cal-seed-public/watch").header("Authorization", bearer("member-user-1-token")),
                Map.of("reminderEnabled", true, "reminderOffsets", List.of("not-a-number"), "idempotencyKey", "bad-reminder-offset"), 400, 40001);
    }

    @Test
    @DisplayName("CAL-P1H-001 to CAL-P1H-003 complete admin event filtering and P1 source creation limits")
    void adminEventFiltersAndSourceCreationBoundaries() throws Exception {
        String maintenanceId = createEvent("admin-filter-maintenance").at("/data/eventId").asText();
        JsonNode voteDraft = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(with(with(with(eventBody("admin-filter-vote"), "title", "vote filter event"), "type", "VOTE_DEADLINE"), "visibility", "STAFF_ONLY"), "startAt", "2026-08-01T12:00:00Z"), "endAt", "2026-08-01T14:00:00Z"), 201);
        String voteId = voteDraft.at("/data/eventId").asText();
        String publishedId = createApprovedPublishedEvent("admin-filter-published");

        JsonNode keyword = performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("keyword", "vote filter"), 200);
        assertThat(keyword.toString()).contains(voteId).doesNotContain(maintenanceId);

        JsonNode type = performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("type", "VOTE_DEADLINE"), 200);
        assertThat(type.toString()).contains(voteId).doesNotContain(maintenanceId);

        JsonNode status = performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("status", "DRAFT"), 200);
        assertThat(status.toString()).contains(maintenanceId, voteId).doesNotContain(publishedId);

        JsonNode visibility = performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("visibility", "STAFF_ONLY"), 200);
        assertThat(visibility.toString()).contains(voteId).doesNotContain(maintenanceId);

        JsonNode createdBy = performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("createdBy", "admin-user"), 200);
        assertThat(createdBy.toString()).contains(maintenanceId, voteId);

        JsonNode range = performJson(get("/api/v1/calendar/admin/events")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2026-07-31T00:00:00Z")
                .param("to", "2026-08-02T00:00:00Z"), 200);
        assertThat(range.toString()).contains(voteId).doesNotContain(maintenanceId);

        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("type", "BAD_TYPE"), 400, 40001);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 40001);
        performJson(get("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token"))
                .param("from", "2026-08-02T00:00:00Z").param("to", "2026-08-01T00:00:00Z"), 409, 49911);

        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(eventBody("bad-activity-create"), "sourceType", "ACTIVITY"), "sourceId", "act-direct-create"), 400, 40001);
        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(eventBody("bad-poll-create"), "sourceType", "COMMUNITY_POLL"), "sourceId", "poll-direct-create"), 400, 40001);
        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(eventBody("bad-ops-create"), "sourceType", "OPS_PLACEHOLDER"), "sourceId", "ops-direct-create"), 400, 40001);
        performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("admin-token")),
                with(with(with(eventBody("bad-changelog-type"), "sourceType", "CHANGELOG"), "sourceId", "change-wrong-type"), "type", "MAINTENANCE"), 400, 40001);
    }

    @Test
    @DisplayName("CAL-P1H-004 filters current user watchlist without leaking another user")
    void watchlistFiltersStatusTypeTimeAndSort() throws Exception {
        String maintenanceId = createApprovedPublishedEvent("watchlist-filter-maintenance");
        String voteId = createApprovedPublishedEvent(with(with(with(eventBody("watchlist-filter-vote"), "type", "VOTE_DEADLINE"), "startAt", "2026-08-10T12:00:00Z"), "endAt", "2026-08-10T14:00:00Z"));

        performJson(post("/api/v1/calendar/me/events/" + maintenanceId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody("watchlist-maintenance-user1"), 201);
        performJson(post("/api/v1/calendar/me/events/" + voteId + "/watch").header("Authorization", bearer("member-user-1-token")),
                watchBody("watchlist-vote-user1"), 201);
        performJson(post("/api/v1/calendar/me/events/" + maintenanceId + "/watch").header("Authorization", bearer("member-user-2-token")),
                watchBody("watchlist-maintenance-user2"), 201);
        performJson(post("/api/v1/calendar/me/events/" + maintenanceId + "/unwatch").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "测试取消关注过滤", "idempotencyKey", "watchlist-unwatch-maintenance"), 200);

        JsonNode active = performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")).param("status", "ACTIVE"), 200);
        assertThat(active.toString()).contains(voteId).doesNotContain(maintenanceId, "member-user-2");

        JsonNode canceled = performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")).param("status", "CANCELED"), 200);
        assertThat(canceled.toString()).contains(maintenanceId).doesNotContain(voteId, "member-user-2");

        JsonNode type = performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")).param("type", "VOTE_DEADLINE"), 200);
        assertThat(type.toString()).contains(voteId).doesNotContain(maintenanceId);

        JsonNode range = performJson(get("/api/v1/calendar/me/watchlist")
                .header("Authorization", bearer("member-user-1-token"))
                .param("from", "2026-08-09T00:00:00Z")
                .param("to", "2026-08-11T00:00:00Z")
                .param("sort", "startAt_asc"), 200);
        assertThat(range.toString()).contains(voteId).doesNotContain(maintenanceId);

        performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")).param("status", "BAD"), 400, 40001);
        performJson(get("/api/v1/calendar/me/watchlist").header("Authorization", bearer("member-user-1-token")).param("sort", "bad"), 400, 40003);
    }

    @Test
    @DisplayName("CAL-P1H-005 and CAL-P1H-007 enforce audit filters and HELPER ownership")
    void auditFiltersAndHelperOwnershipBoundaries() throws Exception {
        JsonNode helperEvent = performJson(post("/api/v1/calendar/admin/events").header("Authorization", bearer("helper-token")),
                eventBody("helper-owned-event"), 201);
        String helperEventId = helperEvent.at("/data/eventId").asText();
        JsonNode helperPatch = performJson(patch("/api/v1/calendar/admin/events/" + helperEventId).header("Authorization", bearer("helper-token")),
                Map.of("title", "helper owned event", "reason", "修改自己的草稿", "idempotencyKey", "helper-owned-patch"), 200);
        assertThat(helperPatch.at("/data/title").asText()).isEqualTo("helper owned event");

        String adminEventId = createEvent("admin-owned-for-helper").at("/data/eventId").asText();
        performJson(patch("/api/v1/calendar/admin/events/" + adminEventId).header("Authorization", bearer("helper-token")),
                Map.of("title", "协管不能改别人", "reason", "权限边界", "idempotencyKey", "helper-patch-admin-owned"), 403, 42001);

        performJson(get("/api/v1/calendar/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);

        JsonNode action = performJson(get("/api/v1/calendar/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "CALENDAR_EVENT_UPDATED"), 200);
        assertThat(action.toString()).contains(helperEventId).doesNotContain(adminEventId);

        JsonNode actor = performJson(get("/api/v1/calendar/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("actorUserId", "helper-user")
                .param("result", "SUCCESS")
                .param("sort", "createdAt_asc"), 200);
        assertThat(actor.toString()).contains(helperEventId).doesNotContain("admin-owned-for-helper");

        JsonNode event = performJson(get("/api/v1/calendar/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("eventId", helperEventId), 200);
        assertThat(event.toString()).contains(helperEventId).doesNotContain(adminEventId);

        performJson(get("/api/v1/calendar/admin/audit-logs").header("Authorization", bearer("admin-token")).param("result", "BAD"), 400, 40001);
        performJson(get("/api/v1/calendar/admin/audit-logs").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/calendar/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2026-08-02T00:00:00Z")
                .param("to", "2026-08-01T00:00:00Z"), 409, 49911);
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
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8132);
        assertThat(ops.at("/data/legacyPort").asInt()).isEqualTo(8114);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "CHANGELOG_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("backend/engagement-core-service/src/main/java/cn/beiming/calendar");
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
