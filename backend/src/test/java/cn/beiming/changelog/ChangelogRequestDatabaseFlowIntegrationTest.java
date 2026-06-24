package cn.beiming.changelog;

import cn.beiming.engagement.EngagementCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
        classes = EngagementCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "changelog.test-controls.enabled=true"
)
@Import(ChangelogRequestDatabaseFlowIntegrationTest.EvidenceConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ChangelogRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "changelog-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:changelog_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "changelog_flow_request_log",
                    "changelog_flow_releases",
                    "changelog_flow_bookmarks",
                    "changelog_flow_calendar_syncs",
                    "changelog_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createReleaseRunsThroughBackendAndDatabaseThenReturnsDraftRelease() throws Exception {
        String requestId = "req-chg-release-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/changelog/admin/releases",
                bearerHeaders("admin-token", requestId),
                releaseBody("release-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("admin-user");
        String releaseId = json.at("/data/releaseId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM changelog_flow_releases WHERE flow_id = ? AND release_id = ? AND action = 'CHANGELOG_RELEASE_CREATED'",
                    FLOW_ID, releaseId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM changelog_flow_releases WHERE flow_id = ? AND release_id = ? AND action = 'CHANGELOG_RELEASE_CREATED'",
                    FLOW_ID, releaseId, "admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM changelog_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CHANGELOG_RELEASE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM changelog_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/changelog/admin/releases'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: changelog release create reached backend, wrote release/audit/request rows, and returned 201.");
    }

    @Test
    void publishReleaseRunsThroughBackendAndDatabaseThenReturnsPublishedRelease() throws Exception {
        String releaseId = createRelease("setup-publish");
        exchangeOk(HttpMethod.POST, "/api/v1/changelog/admin/releases/" + releaseId + "/submit", "helper-token", "req-setup-submit-publish", Map.of("reason", "提交审核", "idempotencyKey", "submit-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/approve", "helper-token", "req-setup-approve-publish", Map.of("reviewComment", "审核通过", "reason", "符合更新日志规则", "idempotencyKey", "approve-" + randomKey()));
        String requestId = "req-chg-publish-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/changelog/admin/releases/" + releaseId + "/publish",
                bearerHeaders("admin-token", requestId),
                Map.of("reason", "发布更新日志", "idempotencyKey", "publish-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("PUBLISHED");
        assertThat(json.at("/data/publishedAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM changelog_flow_releases WHERE flow_id = ? AND release_id = ? AND action = 'CHANGELOG_RELEASE_PUBLISHED'",
                    FLOW_ID, releaseId, "PUBLISHED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM changelog_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CHANGELOG_RELEASE_PUBLISHED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM changelog_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/changelog/admin/releases/" + releaseId + "/publish", 200);
        }
        System.out.println("SQL evidence: changelog publish reached backend, wrote published release/audit/request rows, and returned 200.");
    }

    @Test
    void bookmarkAndUnbookmarkRunThroughBackendAndDatabaseThenReturnCanceledBookmark() throws Exception {
        String releaseId = createApprovedPublishedRelease("setup-bookmark");
        String bookmarkRequestId = "req-chg-bookmark-" + FLOW_ID;

        TestHttpResponse bookmarked = exchange(
                HttpMethod.POST,
                "/api/v1/changelog/me/releases/" + releaseId + "/bookmark",
                bearerHeaders("member-user-1-token", bookmarkRequestId),
                Map.of("idempotencyKey", "bookmark-" + randomKey())
        );

        assertThat(bookmarked.statusCode()).isEqualTo(201);
        JsonNode bookmarkJson = objectMapper.readTree(bookmarked.body());
        assertThat(bookmarkJson.at("/data/bookmark/status").asText()).isEqualTo("ACTIVE");
        assertThat(bookmarkJson.at("/data/bookmark/userId").asText()).isEqualTo("member-user-1");
        String bookmarkId = bookmarkJson.at("/data/bookmark/bookmarkId").asText();

        String unbookmarkRequestId = "req-chg-unbookmark-" + FLOW_ID;
        TestHttpResponse unbookmarked = exchange(
                HttpMethod.POST,
                "/api/v1/changelog/me/releases/" + releaseId + "/unbookmark",
                bearerHeaders("member-user-1-token", unbookmarkRequestId),
                Map.of("reason", "取消收藏", "idempotencyKey", "unbookmark-" + randomKey())
        );

        assertThat(unbookmarked.statusCode()).isEqualTo(200);
        JsonNode unbookmarkJson = objectMapper.readTree(unbookmarked.body());
        assertThat(unbookmarkJson.at("/data/bookmark/bookmarkId").asText()).isEqualTo(bookmarkId);
        assertThat(unbookmarkJson.at("/data/bookmark/status").asText()).isEqualTo("CANCELED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM changelog_flow_bookmarks WHERE flow_id = ? AND bookmark_id = ? AND action = 'CHANGELOG_RELEASE_BOOKMARKED'",
                    FLOW_ID, bookmarkId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT status FROM changelog_flow_bookmarks WHERE flow_id = ? AND bookmark_id = ? AND action = 'CHANGELOG_RELEASE_UNBOOKMARKED'",
                    FLOW_ID, bookmarkId, "CANCELED");
            assertSingleValue(connection,
                    "SELECT response_code FROM changelog_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, unbookmarkRequestId, "/api/v1/changelog/me/releases/" + releaseId + "/unbookmark", 200);
        }
        System.out.println("SQL evidence: changelog bookmark and unbookmark reached backend, wrote bookmark/request rows, and returned 200.");
    }

    @Test
    void calendarSyncRunsThroughBackendAndDatabaseThenReturnsSyncedSnapshot() throws Exception {
        String releaseId = createApprovedPublishedRelease("setup-calendar-sync");
        String requestId = "req-chg-calendar-sync-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/changelog/admin/releases/" + releaseId + "/calendar-sync",
                bearerHeaders("admin-token", requestId),
                Map.of("mode", "UPSERT_SNAPSHOT", "reason", "同步日历", "idempotencyKey", "calendar-sync-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/syncStatus").asText()).isEqualTo("SYNCED");
        assertThat(json.at("/data/calendarEvent/syncStatus").asText()).isEqualTo("SYNCED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT sync_status FROM changelog_flow_calendar_syncs WHERE flow_id = ? AND request_id = ? AND action = 'CHANGELOG_CALENDAR_SYNCED'",
                    FLOW_ID, requestId, "SYNCED");
            assertSingleValue(connection,
                    "SELECT calendar_event_id FROM changelog_flow_calendar_syncs WHERE flow_id = ? AND request_id = ? AND action = 'CHANGELOG_CALENDAR_SYNCED'",
                    FLOW_ID, requestId, "cal-from-" + releaseId);
            assertSingleValue(connection,
                    "SELECT response_code FROM changelog_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/changelog/admin/releases/" + releaseId + "/calendar-sync", 200);
        }
        System.out.println("SQL evidence: changelog calendar sync reached backend, wrote sync/request rows, and returned 200.");
    }

    private String createApprovedPublishedRelease(String purpose) throws Exception {
        String releaseId = createRelease(purpose);
        exchangeOk(HttpMethod.POST, "/api/v1/changelog/admin/releases/" + releaseId + "/submit", "helper-token", "req-setup-submit-" + purpose, Map.of("reason", "提交审核", "idempotencyKey", purpose + "-submit-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/approve", "helper-token", "req-setup-approve-" + purpose, Map.of("reviewComment", "审核通过", "reason", "符合更新日志规则", "idempotencyKey", purpose + "-approve-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/publish", "admin-token", "req-setup-publish-" + purpose, Map.of("reason", "发布更新日志", "idempotencyKey", purpose + "-publish-" + randomKey()));
        return releaseId;
    }

    private String createRelease(String purpose) throws Exception {
        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/changelog/admin/releases",
                bearerHeaders("admin-token", "req-setup-release-" + purpose + "-" + FLOW_ID),
                releaseBody(purpose + "-" + randomKey())
        );
        assertThat(created.statusCode()).isEqualTo(201);
        return objectMapper.readTree(created.body()).at("/data/releaseId").asText();
    }

    private void exchangeOk(HttpMethod method, String path, String token, String requestId, Map<String, Object> body) throws Exception {
        TestHttpResponse response = exchange(method, path, bearerHeaders(token, requestId + "-" + FLOW_ID), body);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private Map<String, Object> releaseBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "changelog-db-flow-" + idempotencyKey.toLowerCase());
        body.put("versionName", "v1.20.4-db-flow-" + idempotencyKey);
        body.put("title", "数据库流更新日志");
        body.put("summary", "数据库流更新日志摘要");
        body.put("body", "更新说明覆盖真实 HTTP、后端处理和 SQL 证据。");
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
        return Map.of(
                "type", type,
                "title", title,
                "description", "分组说明",
                "items", List.of(Map.of(
                        "title", "变更项",
                        "description", "公开安全的变更说明",
                        "severity", "INFO",
                        "component", "server",
                        "publicSafe", true,
                        "sortOrder", 10
                )),
                "sortOrder", 10
        );
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

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS changelog_flow_releases (
                    flow_id VARCHAR(128) NOT NULL,
                    release_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS changelog_flow_bookmarks (
                    flow_id VARCHAR(128) NOT NULL,
                    bookmark_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    release_id VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS changelog_flow_calendar_syncs (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    release_id VARCHAR(128) NOT NULL,
                    sync_status VARCHAR(32) NOT NULL,
                    calendar_event_id VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS changelog_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS changelog_flow_request_log (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    path VARCHAR(256) NOT NULL,
                    response_code INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    private static void deleteFlowRows(Statement statement, String table) {
        try {
            statement.executeUpdate("DELETE FROM " + table + " WHERE flow_id = '" + FLOW_ID + "'");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    static class EvidenceConfiguration {
        @Bean
        ChangelogFlowEvidenceRecorder changelogFlowEvidenceRecorder() {
            return new JdbcChangelogFlowEvidenceRecorder();
        }
    }

    static class JdbcChangelogFlowEvidenceRecorder implements ChangelogFlowEvidenceRecorder {
        @Override
        public void recordReleaseWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO changelog_flow_releases(flow_id, release_id, action, slug, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("releaseId"), action, payload.get("slug"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("releaseId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write changelog release database evidence", exception);
            }
        }

        @Override
        public void recordBookmarkWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> bookmark = map(payload.get("bookmark"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO changelog_flow_bookmarks(flow_id, bookmark_id, action, release_id, user_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, bookmark.get("bookmarkId"), action, bookmark.get("releaseId"), bookmark.get("userId"), bookmark.get("status"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(bookmark.get("bookmarkId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write changelog bookmark database evidence", exception);
            }
        }

        @Override
        public void recordCalendarSyncWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> calendarEvent = map(payload.get("calendarEvent"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO changelog_flow_calendar_syncs(flow_id, request_id, action, release_id, sync_status, calendar_event_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, request.getHeader("X-Request-Id"), action, payload.get("releaseId"), payload.get("syncStatus"), calendarEvent.get("eventId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("releaseId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write changelog calendar sync database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO changelog_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO changelog_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, requestId, path, responseCode, Timestamp.from(Instant.now()));
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }

        private static Map<?, ?> map(Object value) {
            if (value instanceof Map<?, ?> map) {
                return map;
            }
            throw new IllegalArgumentException("expected map payload");
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
