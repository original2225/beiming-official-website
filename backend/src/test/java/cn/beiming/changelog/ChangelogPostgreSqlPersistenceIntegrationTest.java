package cn.beiming.changelog;

import cn.beiming.engagement.EngagementCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EngagementCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "changelog.test-controls.enabled=true"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ChangelogPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "changelog-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_changelog_pg")
            .withUsername("beiming")
            .withPassword("beiming");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM changelog_calendar_syncs");
            statement.executeUpdate("DELETE FROM changelog_bookmarks");
            statement.executeUpdate("DELETE FROM changelog_releases");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'changelog.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type LIKE 'CHANGELOG%'");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/changelog%'");
        }
    }

    @Test
    void releaseLifecyclePersistsReleaseAuditIdempotencyAndRequestLogs() throws Exception {
        String createRequestId = requestId("release-create");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("admin-token", createRequestId), releaseBody("release-" + randomKey()));
        assertThat(created.at("/code").asInt()).isZero();
        String releaseId = created.at("/data/releaseId").asText();

        exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases/" + releaseId + "/submit", bearerHeaders("helper-token", requestId("release-submit")), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/approve", bearerHeaders("helper-token", requestId("release-approve")), Map.of("reviewComment", "审核通过", "reason", "符合更新日志规则", "idempotencyKey", "approve-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/publish", bearerHeaders("admin-token", requestId("release-publish")), Map.of("reason", "发布更新日志", "idempotencyKey", "publish-" + randomKey()));

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM changelog_releases WHERE release_id = ?", releaseId, "PUBLISHED");
            assertSingleValue(connection, "SELECT created_by FROM changelog_releases WHERE release_id = ?", releaseId, "admin-user");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'CHANGELOG_RELEASE' AND target_id = ? AND action = 'CHANGELOG_RELEASE_PUBLISHED'", releaseId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'changelog.release.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/changelog/admin/releases'", createRequestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL changelog release lifecycle wrote release/audit/idempotency/request rows.");
    }

    @Test
    void bookmarkUnbookmarkAndCalendarSyncPersistBusinessRows() throws Exception {
        String releaseId = createPublishedRelease("bookmark-" + randomKey());
        String bookmarkRequestId = requestId("bookmark");
        JsonNode bookmarked = exchange(HttpMethod.POST, "/api/v1/changelog/me/releases/" + releaseId + "/bookmark",
                bearerHeaders("member-user-1-token", bookmarkRequestId), Map.of("idempotencyKey", "bookmark-" + randomKey()));
        String bookmarkId = bookmarked.at("/data/bookmark/bookmarkId").asText();
        exchange(HttpMethod.POST, "/api/v1/changelog/me/releases/" + releaseId + "/unbookmark",
                bearerHeaders("member-user-1-token", requestId("unbookmark")), Map.of("reason", "取消收藏", "idempotencyKey", "unbookmark-" + randomKey()));

        String syncRequestId = requestId("calendar-sync");
        JsonNode sync = exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases/" + releaseId + "/calendar-sync",
                bearerHeaders("admin-token", syncRequestId), Map.of("mode", "UPSERT_SNAPSHOT", "reason", "同步日历", "idempotencyKey", "sync-" + randomKey()));
        assertThat(sync.at("/data/syncStatus").asText()).isEqualTo("SYNCED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM changelog_bookmarks WHERE bookmark_id = ?", bookmarkId, "CANCELED");
            assertSingleValue(connection, "SELECT user_id FROM changelog_bookmarks WHERE bookmark_id = ?", bookmarkId, "member-user-1");
            assertSingleValue(connection, "SELECT calendar_event_id FROM changelog_releases WHERE release_id = ?", releaseId, "cal-from-" + releaseId);
            assertSingleValue(connection, "SELECT sync_status FROM changelog_calendar_syncs WHERE request_id = ?", syncRequestId, "SYNCED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'CHANGELOG_RELEASE_BOOKMARKED' AND target_id = ?", bookmarkRequestId, bookmarkId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'member-user-1' AND scope = 'changelog.bookmark.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", bookmarkRequestId, "/api/v1/changelog/me/releases/" + releaseId + "/bookmark", 201);
        }
        System.out.println("SQL evidence: PostgreSQL changelog bookmark/unbookmark/calendar-sync wrote business/audit/idempotency/request rows.");
    }

    @Test
    void httpBoundariesAndIdempotencyConflictRemainConsistent() throws Exception {
        assertThat(exchange(HttpMethod.GET, "/api/v1/changelog/me/bookmarks", jsonHeaders(), null).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("user-token", requestId("forbidden")), releaseBody("forbidden-" + randomKey())).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("admin-token", requestId("validation")), with(releaseBody("validation-" + randomKey()), "idempotencyKey", "short")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchange(HttpMethod.GET, "/api/v1/changelog/admin/releases/missing", bearerHeaders("admin-token", requestId("missing")), null).at("/code").asInt()).isEqualTo(49300);
        assertThat(exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", headersWith("admin-token", requestId("dependency"), "X-Test-Resource-Mode", "unavailable"), releaseBody("dep-" + randomKey())).at("/code").asInt()).isEqualTo(49110);

        String idempotencyKey = "idem-" + randomKey();
        exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("admin-token", requestId("idem-seed")), releaseBody(idempotencyKey));
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("admin-token", requestId("idem-conflict")), with(releaseBody(idempotencyKey), "title", "不同标题"));
        assertThat(conflict.at("/code").asInt()).isEqualTo(49312);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'changelog.release.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("idem-conflict"), 0L);
        }
        System.out.println("SQL evidence: PostgreSQL changelog boundary test covered auth, permission, validation, missing resource, dependency failure, and idempotency conflict rollback.");
    }

    private String createPublishedRelease(String purpose) throws Exception {
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases", bearerHeaders("admin-token", requestId("setup-create-" + purpose)), releaseBody("release-" + purpose));
        String releaseId = created.at("/data/releaseId").asText();
        exchange(HttpMethod.POST, "/api/v1/changelog/admin/releases/" + releaseId + "/submit", bearerHeaders("helper-token", requestId("setup-submit-" + purpose)), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/approve", bearerHeaders("helper-token", requestId("setup-approve-" + purpose)), Map.of("reviewComment", "审核通过", "reason", "符合更新日志规则", "idempotencyKey", "approve-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/changelog/admin/releases/" + releaseId + "/publish", bearerHeaders("admin-token", requestId("setup-publish-" + purpose)), Map.of("reason", "发布更新日志", "idempotencyKey", "publish-" + purpose));
        return releaseId;
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        return headers;
    }

    private HttpHeaders headersWith(String token, String requestId, String name, String value) {
        HttpHeaders headers = bearerHeaders(token, requestId);
        headers.set(name, value);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> releaseBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "changelog-pg-" + idempotencyKey.toLowerCase());
        body.put("versionName", "v1.20.4-pg-" + idempotencyKey);
        body.put("title", "PostgreSQL 更新日志");
        body.put("summary", "PostgreSQL 更新日志摘要");
        body.put("body", "更新说明覆盖真实 HTTP、PostgreSQL 业务表和公共日志。");
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
        return Map.of("type", type, "title", title, "description", "分组说明", "items", List.of(Map.of(
                "title", "变更项",
                "description", "公开安全的变更说明",
                "severity", "INFO",
                "component", "server",
                "publicSafe", true,
                "sortOrder", 10
        )), "sortOrder", 10);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String requestId(String prefix) {
        return "req-" + prefix + "-" + FLOW_ID;
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
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
}
