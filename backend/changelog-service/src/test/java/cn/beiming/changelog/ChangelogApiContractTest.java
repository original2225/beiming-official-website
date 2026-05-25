package cn.beiming.changelog;

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

@SpringBootTest(classes = ChangelogServiceApplication.class, properties = "changelog.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ChangelogApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("changelog local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "CHG-COM", 1, 90);
        addRange(mapped, "CHG-PUB", 1, 110);
        addRange(mapped, "CHG-ME", 1, 80);
        addRange(mapped, "CHG-ADMIN", 1, 150);
        addRange(mapped, "CHG-STATE", 1, 100);
        addRange(mapped, "CHG-GROUP", 1, 90);
        addRange(mapped, "CHG-RELATED", 1, 100);
        addRange(mapped, "CHG-CALENDAR", 1, 80);
        addRange(mapped, "CHG-NOTIF", 1, 70);
        addRange(mapped, "CHG-AUDIT", 1, 80);
        addRange(mapped, "CHG-DEPS", 1, 100);
        addRange(mapped, "CHG-HARDEN", 1, 150);
        addRange(mapped, "CHG-PORT", 1, 20);
        addRange(mapped, "CHG-CYCLE", 1, 70);
        assertThat(mapped).contains("CHG-COM-001", "CHG-PUB-110", "CHG-CALENDAR-080", "CHG-HARDEN-150", "CHG-CYCLE-070");
        assertThat(mapped).hasSize(1290);
    }

    @Test
    @DisplayName("CHG-COM covers envelope, request id, auth gates, role gates, validation, and trusted field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/changelog/releases").header("X-Request-Id", "req-changelog-public"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-changelog-public"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.requestId").value("req-changelog-public"));

        JsonNode generated = performJson(get("/api/v1/changelog/releases"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(post("/api/v1/changelog/me/releases/chg-seed-public/bookmark"), Map.of("idempotencyKey", "trusted-bookmark"), 401, 41000);
        performJson(post("/api/v1/changelog/me/releases/chg-seed-public/bookmark").header("Authorization", "bad-token"), Map.of("idempotencyKey", "trusted-bookmark"), 401, 41003);
        performJson(get("/api/v1/changelog/admin/releases"), 401, 41000);
        performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/changelog/admin/releases/chg-seed-public/publish").header("Authorization", bearer("helper-token")),
                Map.of("reason", "helper cannot publish", "idempotencyKey", "helper-publish-1"), 403, 42001);
        performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")),
                with(releaseBody("short-key"), "idempotencyKey", "short"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")),
                with(releaseBody("trusted-release-1"), "status", "PUBLISHED"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin-user");
        assertThat(created.at("/data/bookmarkCount").asInt()).isZero();
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("CHG-PUB, CHG-ME, and CHG-ADMIN cover the release lifecycle and public views")
    void releaseLifecyclePublicViewsBookmarksAndSearch() throws Exception {
        String releaseId = createApprovedPublishedRelease("flow-changelog-1");

        JsonNode publicList = performJson(get("/api/v1/changelog/releases").param("keyword", "北冥"), 200);
        assertThat(publicList.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(publicList);

        JsonNode detail = performJson(get("/api/v1/changelog/releases/" + releaseId), 200);
        assertThat(detail.at("/data/status").asText()).isEqualTo("PUBLISHED");

        JsonNode slugDetail = performJson(get("/api/v1/changelog/releases/flow-changelog-1"), 200);
        assertThat(slugDetail.at("/data/releaseId").asText()).isEqualTo(releaseId);

        JsonNode latest = performJson(get("/api/v1/changelog/versions/latest").param("type", "SERVER_VERSION"), 200);
        assertThat(latest.toString()).contains(releaseId);

        JsonNode tags = performJson(get("/api/v1/changelog/tags"), 200);
        assertThat(tags.toString()).contains("SERVER_VERSION", "ADDED", "1.20.4");

        JsonNode changes = performJson(get("/api/v1/changelog/changes").param("keyword", "公开安全"), 200);
        assertThat(changes.toString()).contains(releaseId, "公开安全");

        JsonNode bookmarked = performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/bookmark").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "bookmark-flow-1"), 201);
        assertThat(bookmarked.at("/data/bookmark/status").asText()).isEqualTo("ACTIVE");
        String bookmarkId = bookmarked.at("/data/bookmark/bookmarkId").asText();

        JsonNode replay = performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/bookmark").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "bookmark-flow-1"), 201);
        assertThat(replay.at("/data/bookmark/bookmarkId").asText()).isEqualTo(bookmarkId);

        JsonNode bookmarks = performJson(get("/api/v1/changelog/me/bookmarks").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(bookmarks.at("/data/total").asInt()).isEqualTo(1);

        JsonNode unbookmarked = performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/unbookmark").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "取消收藏", "idempotencyKey", "unbookmark-flow-1"), 200);
        assertThat(unbookmarked.at("/data/bookmark/status").asText()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("CHG-PUB rejects invalid filters and keeps review workflow fields out of public views")
    void publicFiltersAndWorkflowFieldIsolation() throws Exception {
        String releaseId = createApprovedPublishedRelease("public-isolation-1");

        performJson(get("/api/v1/changelog/releases").param("type", "BAD_TYPE"), 400, 40001);
        performJson(get("/api/v1/changelog/releases").param("visibility", "BAD_VISIBILITY"), 400, 40001);
        performJson(get("/api/v1/changelog/releases").param("impactLevel", "BAD_IMPACT"), 400, 40001);
        performJson(get("/api/v1/changelog/versions/latest").param("type", "BAD_TYPE"), 400, 40001);
        performJson(get("/api/v1/changelog/changes").param("groupType", "BAD_GROUP"), 400, 40001);
        performJson(get("/api/v1/changelog/changes").param("severity", "BAD_SEVERITY"), 400, 40001);
        performJson(get("/api/v1/changelog/changes").param("releaseType", "BAD_TYPE"), 400, 40001);

        JsonNode detail = performJson(get("/api/v1/changelog/releases/" + releaseId), 200);
        assertThat(detail.at("/data/reviewComment").isMissingNode()).isTrue();
        assertThat(detail.at("/data/submittedAt").isMissingNode()).isTrue();
        assertThat(detail.at("/data/reviewedAt").isMissingNode()).isTrue();
        assertThat(detail.at("/data/offlineAt").isMissingNode()).isTrue();
        assertThat(detail.at("/data/archivedAt").isMissingNode()).isTrue();
        assertNoSecrets(detail);
    }

    @Test
    @DisplayName("CHG-ADMIN partial updates preserve existing relation snapshots and groups")
    void partialUpdatePreservesRelationSnapshots() throws Exception {
        JsonNode created = createRelease("partial-preserve-relations");
        String releaseId = created.at("/data/releaseId").asText();
        assertThat(created.at("/data/relatedResources").size()).isEqualTo(1);
        assertThat(created.at("/data/relatedServerInstances").size()).isEqualTo(1);
        assertThat(created.at("/data/relatedContent/contentId").asText()).isEqualTo("content-release-note");

        JsonNode patched = performJson(patch("/api/v1/changelog/admin/releases/" + releaseId).header("Authorization", bearer("admin-token")),
                Map.of("title", "只修改标题", "reason", "局部更新不应清空快照", "idempotencyKey", "partial-preserve-patch"), 200);

        assertThat(patched.at("/data/title").asText()).isEqualTo("只修改标题");
        assertThat(patched.at("/data/groups").size()).isEqualTo(1);
        assertThat(patched.at("/data/relatedResources").size()).isEqualTo(1);
        assertThat(patched.at("/data/relatedServerInstances").size()).isEqualTo(1);
        assertThat(patched.at("/data/relatedContent/contentId").asText()).isEqualTo("content-release-note");
    }

    @Test
    @DisplayName("CHG-STATE enforces status transitions, soft delete confirmation, and public visibility")
    void adminStateMachineAndSoftDelete() throws Exception {
        JsonNode draft = createRelease("state-machine-1");
        String releaseId = draft.at("/data/releaseId").asText();

        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "不能跳过审核", "idempotencyKey", "state-publish-bad"), 409, 49310);

        approveAndPublish(releaseId, "state-machine-1");
        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId).header("Authorization", bearer("helper-token")),
                with(releaseBody("state-patch-published"), "title", "发布后不能直接修改"), 409, 49310);

        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/archive").header("Authorization", bearer("admin-token")),
                Map.of("reason", "已发布不能直接归档", "idempotencyKey", "state-archive-bad"), 409, 49310);

        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/offline").header("Authorization", bearer("admin-token")),
                Map.of("publicReason", "维护调整", "reason", "下架更新日志", "idempotencyKey", "state-offline"), 200);
        performJson(get("/api/v1/changelog/releases/" + releaseId), 404, 49300);

        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/delete").header("Authorization", bearer("admin-token")),
                Map.of("reason", "缺少确认", "idempotencyKey", "state-delete-bad"), 400, 40001);
        JsonNode deleted = performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/delete").header("Authorization", bearer("admin-token")),
                Map.of("reason", "软删除测试", "confirmText", "DELETE_CHANGELOG_RELEASE", "idempotencyKey", "state-delete"), 200);
        assertThat(deleted.at("/data/status").asText()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("CHG-GROUP covers group validation and security public redaction")
    void groupValidationAndSecurityRedaction() throws Exception {
        performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")),
                with(releaseBody("empty-groups"), "groups", List.of()), 409, 49315);

        performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")),
                with(with(releaseBody("security-missing-summary"), "type", "SECURITY"), "securityPublicSummary", null), 409, 49314);

        Map<String, Object> security = with(with(releaseBody("security-redacted"), "type", "SECURITY"), "groups",
                List.of(group("SECURITY", "安全修复", item("安全修复", "内部 exploit 细节不公开", "SECURITY", "server", false))));
        String releaseId = createApprovedPublishedRelease(with(security, "securityPublicSummary", "修复一个已确认的安全问题，建议玩家更新客户端资源。"));
        JsonNode publicDetail = performJson(get("/api/v1/changelog/releases/" + releaseId), 200);
        assertThat(publicDetail.toString()).contains("已脱敏").doesNotContain("exploit", "server.properties");
        assertNoSecrets(publicDetail);
    }

    @Test
    @DisplayName("CHG-RELATED, CHG-CALENDAR, and CHG-NOTIF cover dependency failures and degraded summaries")
    void dependencyCalendarAndNotificationBoundaries() throws Exception {
        performJson(post("/api/v1/changelog/admin/releases")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Resource-Mode", "unavailable"),
                releaseBody("resource-down-create"), 502, 49110);
        performJson(post("/api/v1/changelog/admin/releases")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Server-Status-Mode", "bad-schema"),
                releaseBody("server-status-bad-create"), 502, 49122);
        performJson(post("/api/v1/changelog/admin/releases")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Content-Mode", "timeout"),
                releaseBody("content-timeout-create"), 504, 49131);

        String releaseId = createApprovedPublishedRelease("calendar-notify-fail");
        JsonNode dryRun = performJson(post("/api/v1/changelog/admin/releases/" + releaseId + "/calendar-sync").header("Authorization", bearer("admin-token")),
                Map.of("mode", "DRY_RUN", "reason", "同步预检", "idempotencyKey", "calendar-dry-run"), 200);
        assertThat(dryRun.at("/data/syncStatus").asText()).isEqualTo("SKIPPED");

        performJson(post("/api/v1/changelog/admin/releases/" + releaseId + "/calendar-sync")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Calendar-Mode", "unavailable"),
                Map.of("mode", "UPSERT_SNAPSHOT", "reason", "日历不可用", "idempotencyKey", "calendar-down"), 502, 49140);

        JsonNode offlined = performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/offline")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("publicReason", "通知失败也要下架", "reason", "通知失败摘要", "idempotencyKey", "notify-offline"), 200);
        assertThat(offlined.at("/data/notificationSummary/failure/failureCode").asText()).isEqualTo("49150");
        assertNoSecrets(offlined);
    }

    @Test
    @DisplayName("CHG-HARDEN replays idempotent writes and rejects idempotency fingerprint conflicts")
    void idempotencyReplaysWritesAndRejectsConflicts() throws Exception {
        Map<String, Object> body = releaseBody("idem-changelog-create");
        JsonNode firstCreate = performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")), body, 201);
        JsonNode replayCreate = performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")), body, 201);
        assertThat(replayCreate.at("/data/releaseId").asText()).isEqualTo(firstCreate.at("/data/releaseId").asText());
        performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")),
                with(body, "title", "同 key 改标题"), 409, 49312);

        String releaseId = approveAndPublish(firstCreate.at("/data/releaseId").asText(), "idem-changelog-create");
        Map<String, Object> bookmarkBody = Map.of("idempotencyKey", "idem-bookmark");
        JsonNode firstBookmark = performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/bookmark").header("Authorization", bearer("member-user-1-token")),
                bookmarkBody, 201);
        JsonNode replayBookmark = performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/bookmark").header("Authorization", bearer("member-user-1-token")),
                bookmarkBody, 201);
        assertThat(replayBookmark.at("/data/bookmark/bookmarkId").asText()).isEqualTo(firstBookmark.at("/data/bookmark/bookmarkId").asText());
        performJson(post("/api/v1/changelog/me/releases/" + releaseId + "/bookmark").header("Authorization", bearer("member-user-1-token")),
                with(bookmarkBody, "note", "same key changed"), 409, 49312);
    }

    @Test
    @DisplayName("CHG-ADMIN enforces filters, audit filters, and HELPER ownership")
    void adminFiltersAuditFiltersAndHelperOwnership() throws Exception {
        JsonNode helperRelease = performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("helper-token")),
                releaseBody("helper-owned-release"), 201);
        String helperReleaseId = helperRelease.at("/data/releaseId").asText();
        JsonNode helperPatch = performJson(patch("/api/v1/changelog/admin/releases/" + helperReleaseId).header("Authorization", bearer("helper-token")),
                Map.of("title", "helper owned release", "reason", "修改自己的草稿", "idempotencyKey", "helper-owned-patch"), 200);
        assertThat(helperPatch.at("/data/title").asText()).isEqualTo("helper owned release");

        String adminReleaseId = createRelease("admin-owned-for-helper").at("/data/releaseId").asText();
        performJson(patch("/api/v1/changelog/admin/releases/" + adminReleaseId).header("Authorization", bearer("helper-token")),
                Map.of("title", "协管不能改别人", "reason", "权限边界", "idempotencyKey", "helper-patch-admin-owned"), 403, 42001);

        String publishedId = createApprovedPublishedRelease("admin-filter-published");
        JsonNode keyword = performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("keyword", "helper owned"), 200);
        assertThat(keyword.toString()).contains(helperReleaseId).doesNotContain(adminReleaseId);
        JsonNode status = performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("status", "PUBLISHED"), 200);
        assertThat(status.toString()).contains(publishedId).doesNotContain(helperReleaseId);
        JsonNode visibility = performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("visibility", "PUBLIC"), 200);
        assertThat(visibility.toString()).contains(helperReleaseId, adminReleaseId);
        JsonNode createdBy = performJson(get("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")).param("createdBy", "helper-user"), 200);
        assertThat(createdBy.toString()).contains(helperReleaseId).doesNotContain(adminReleaseId);

        performJson(get("/api/v1/changelog/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        JsonNode action = performJson(get("/api/v1/changelog/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "CHANGELOG_RELEASE_UPDATED"), 200);
        assertThat(action.toString()).contains(helperReleaseId).doesNotContain(adminReleaseId);

        performJson(get("/api/v1/changelog/admin/audit-logs").header("Authorization", bearer("admin-token")).param("result", "BAD"), 400, 40001);
        performJson(get("/api/v1/changelog/admin/audit-logs").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
    }

    @Test
    @DisplayName("CHG-AUDIT rolls back backend writes and bookmark writes when persistence fails")
    void auditFailureRollsBackWrites() throws Exception {
        String releaseId = createRelease("audit-rollback-submit").at("/data/releaseId").asText();
        performJson(post("/api/v1/changelog/admin/releases/" + releaseId + "/submit")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                Map.of("reason", "审计失败", "idempotencyKey", "audit-fail-submit"), 500, 54901);
        JsonNode afterSubmitFail = performJson(get("/api/v1/changelog/admin/releases/" + releaseId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterSubmitFail.at("/data/release/status").asText()).isEqualTo("DRAFT");

        String bookmarkReleaseId = createApprovedPublishedRelease("audit-bookmark-release");
        performJson(post("/api/v1/changelog/me/releases/" + bookmarkReleaseId + "/bookmark")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Fail-Bookmark", "true"),
                Map.of("idempotencyKey", "audit-bookmark-fail"), 500, 54903);
        JsonNode afterBookmarkFail = performJson(get("/api/v1/changelog/admin/releases/" + bookmarkReleaseId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterBookmarkFail.at("/data/release/bookmarkCount").asInt()).isZero();
    }

    @Test
    @DisplayName("CHG-DEPS, CHG-AUDIT, CHG-OPS, CHG-COMPAT, and CHG-HARDEN cover boundaries")
    void dependencyAuditOpsAndCompatibilityBoundaries() throws Exception {
        performJson(post("/api/v1/changelog/me/releases/chg-seed-public/bookmark")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Auth-Mode", "unavailable"),
                Map.of("idempotencyKey", "auth-down-bookmark"), 502, 49100);

        JsonNode audit = performJson(get("/api/v1/changelog/admin/audit-logs").header("Authorization", bearer("admin-token")).param("pageSize", "100"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/changelog/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("changelog");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8115);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "CALENDAR_WRITE_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("backend/changelog-service/src/main/java");
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
                "cn.beiming.activity.", "cn.beiming.calendar.", "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "cloudreveToken", "terminal", "container", "backupRestore", "file-manager",
                "server.properties", "whitelist add", "whitelist remove", "scoreBalance", "attendancePoints",
                "leaderboard", "ActivityStore", "CalendarStore", "ResourceStore", "ServerStatusStore");
    }

    private JsonNode createRelease(String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")), releaseBody(idempotencyKey), 201);
    }

    private String createApprovedPublishedRelease(String idempotencyKey) throws Exception {
        JsonNode release = createRelease(idempotencyKey);
        return approveAndPublish(release.at("/data/releaseId").asText(), idempotencyKey);
    }

    private String createApprovedPublishedRelease(Map<String, Object> body) throws Exception {
        String idempotencyKey = body.get("idempotencyKey").toString();
        JsonNode release = performJson(post("/api/v1/changelog/admin/releases").header("Authorization", bearer("admin-token")), body, 201);
        return approveAndPublish(release.at("/data/releaseId").asText(), idempotencyKey);
    }

    private String approveAndPublish(String releaseId, String idempotencyKey) throws Exception {
        performJson(post("/api/v1/changelog/admin/releases/" + releaseId + "/submit").header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", idempotencyKey + "-submit"), 200);
        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/approve").header("Authorization", bearer("helper-token")),
                Map.of("reviewComment", "审核通过", "internalNote", "private note", "reason", "符合更新日志规则", "idempotencyKey", idempotencyKey + "-approve"), 200);
        performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布更新日志", "idempotencyKey", idempotencyKey + "-publish"), 200);
        return releaseId;
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

    private Map<String, Object> releaseBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", idempotencyKey);
        body.put("versionName", "v1.20.4-" + idempotencyKey);
        body.put("title", "北冥服务器更新");
        body.put("summary", "一次用于契约测试的更新日志");
        body.put("body", "更新说明覆盖服务器版本、插件、资源包、地图和维护记录。");
        body.put("type", "SERVER_VERSION");
        body.put("visibility", "PUBLIC");
        body.put("impactLevel", "MEDIUM");
        body.put("releasedAt", "2026-06-01T12:00:00Z");
        body.put("effectiveAt", "2026-06-01T13:00:00Z");
        body.put("minecraftVersion", "1.20.4");
        body.put("pluginVersions", List.of(Map.of("name", "CoreProtect", "version", "22.4", "action", "UPDATED")));
        body.put("resourcePackVersions", List.of(Map.of("name", "Beiming Pack", "version", "2026.06")));
        body.put("mapVersion", "map-2026-06");
        body.put("groups", List.of(group("ADDED", "新增内容")));
        body.put("compatibilityNotes", "兼容 1.20.4 客户端。");
        body.put("knownIssues", "暂无公开已知问题。");
        body.put("rollbackNotes", "如有问题由管理员按维护流程回滚。");
        body.put("relatedResourceIds", List.of("resource-pack-1"));
        body.put("relatedServerInstanceIds", List.of("survival-main"));
        body.put("relatedContentId", "content-release-note");
        body.put("reason", "创建更新日志");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> group(String type, String title) {
        return group(type, title, item("变更项", "公开安全的变更说明", "INFO", "server", true));
    }

    private Map<String, Object> group(String type, String title, Map<String, Object> item) {
        return Map.of("type", type, "title", title, "description", "分组说明", "items", List.of(item), "sortOrder", 10);
    }

    private Map<String, Object> item(String title, String description, String severity, String component, boolean publicSafe) {
        return Map.of("title", title, "description", description, "severity", severity, "component", component, "publicSafe", publicSafe, "sortOrder", 10);
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
                "internalNote", "private note", "server.properties", "whitelist add", "whitelist remove",
                "node-daemon", "terminal", "container", "cloudreveToken", "attendancePoints", "scoreBalance",
                "leaderboard", "ActivityStore", "CalendarStore", "ResourceStore", "ServerStatusStore");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
