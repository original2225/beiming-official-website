package cn.beiming.calendar;

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
                "calendar.test-controls.enabled=true"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CalendarPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "calendar-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_calendar_pg")
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
            statement.executeUpdate("DELETE FROM calendar_activity_sync_runs");
            statement.executeUpdate("DELETE FROM calendar_watches");
            statement.executeUpdate("DELETE FROM calendar_events");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'calendar.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type LIKE 'CALENDAR%'");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/calendar%'");
        }
    }

    @Test
    void eventLifecyclePersistsEventAuditIdempotencyAndRequestLogs() throws Exception {
        String createRequestId = requestId("event-create");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("admin-token", createRequestId), eventBody("event-" + randomKey()));
        assertThat(created.at("/code").asInt()).isZero();
        String eventId = created.at("/data/eventId").asText();

        exchange(HttpMethod.POST, "/api/v1/calendar/admin/events/" + eventId + "/submit", bearerHeaders("helper-token", requestId("event-submit")), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/approve", bearerHeaders("helper-token", requestId("event-approve")), Map.of("reviewComment", "审核通过", "reason", "符合日程规则", "idempotencyKey", "approve-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/publish", bearerHeaders("admin-token", requestId("event-publish")), Map.of("reason", "发布日程", "idempotencyKey", "publish-" + randomKey()));

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM calendar_events WHERE event_id = ?", eventId, "PUBLISHED");
            assertSingleValue(connection, "SELECT created_by FROM calendar_events WHERE event_id = ?", eventId, "admin-user");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'CALENDAR_EVENT' AND target_id = ? AND action = 'CALENDAR_EVENT_PUBLISHED'", eventId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'calendar.event.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/calendar/admin/events'", createRequestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL calendar event lifecycle wrote event/audit/idempotency/request rows.");
    }

    @Test
    void watchUnwatchAndActivitySyncPersistBusinessRows() throws Exception {
        String eventId = createPublishedEvent("watch-" + randomKey());
        String watchRequestId = requestId("watch");
        JsonNode watched = exchange(HttpMethod.POST, "/api/v1/calendar/me/events/" + eventId + "/watch",
                bearerHeaders("member-user-1-token", watchRequestId), watchBody("watch-" + randomKey()));
        String watchId = watched.at("/data/watch/watchId").asText();
        exchange(HttpMethod.POST, "/api/v1/calendar/me/events/" + eventId + "/unwatch",
                bearerHeaders("member-user-1-token", requestId("unwatch")), Map.of("reason", "取消关注", "idempotencyKey", "unwatch-" + randomKey()));

        String syncRequestId = requestId("sync");
        JsonNode sync = exchange(HttpMethod.POST, "/api/v1/calendar/admin/sync/activity", bearerHeaders("admin-token", syncRequestId), syncBody("sync-" + randomKey(), "UPSERT_SNAPSHOT"));
        assertThat(sync.at("/data/syncStatus").asText()).isEqualTo("SYNCED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM calendar_watches WHERE watch_id = ?", watchId, "CANCELED");
            assertSingleValue(connection, "SELECT user_id FROM calendar_watches WHERE watch_id = ?", watchId, "member-user-1");
            assertSingleValue(connection, "SELECT COUNT(*) FROM calendar_events WHERE source_type = 'ACTIVITY' AND source_id = 'act-calendar-summary-1'", 1L);
            assertSingleValue(connection, "SELECT sync_status FROM calendar_activity_sync_runs WHERE request_id = ?", syncRequestId, "SYNCED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'CALENDAR_EVENT_WATCHED' AND target_id = ?", watchRequestId, watchId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'member-user-1' AND scope = 'calendar.watch.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", watchRequestId, "/api/v1/calendar/me/events/" + eventId + "/watch", 201);
        }
        System.out.println("SQL evidence: PostgreSQL calendar watch/unwatch/activity-sync wrote business/audit/idempotency/request rows.");
    }

    @Test
    void httpBoundariesAndIdempotencyConflictRemainConsistent() throws Exception {
        assertThat(exchange(HttpMethod.GET, "/api/v1/calendar/me/watchlist", jsonHeaders(), null).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("user-token", requestId("forbidden")), eventBody("forbidden-" + randomKey())).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("admin-token", requestId("validation")), with(eventBody("validation-" + randomKey()), "idempotencyKey", "short")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchange(HttpMethod.GET, "/api/v1/calendar/admin/events/missing", bearerHeaders("admin-token", requestId("missing")), null).at("/code").asInt()).isEqualTo(49900);
        assertThat(exchange(HttpMethod.POST, "/api/v1/calendar/admin/sync/activity", headersWith("admin-token", requestId("dependency"), "X-Test-Activity-Mode", "unavailable"), syncBody("dep-" + randomKey(), "UPSERT_SNAPSHOT")).at("/code").asInt()).isEqualTo(49810);

        String idempotencyKey = "idem-" + randomKey();
        exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("admin-token", requestId("idem-seed")), eventBody(idempotencyKey));
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("admin-token", requestId("idem-conflict")), with(eventBody(idempotencyKey), "title", "不同标题"));
        assertThat(conflict.at("/code").asInt()).isEqualTo(49914);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'calendar.event.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("idem-conflict"), 0L);
        }
        System.out.println("SQL evidence: PostgreSQL calendar boundary test covered auth, permission, validation, missing resource, dependency failure, and idempotency conflict rollback.");
    }

    private String createPublishedEvent(String purpose) throws Exception {
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/calendar/admin/events", bearerHeaders("admin-token", requestId("setup-create-" + purpose)), eventBody("event-" + purpose));
        String eventId = created.at("/data/eventId").asText();
        exchange(HttpMethod.POST, "/api/v1/calendar/admin/events/" + eventId + "/submit", bearerHeaders("helper-token", requestId("setup-submit-" + purpose)), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/approve", bearerHeaders("helper-token", requestId("setup-approve-" + purpose)), Map.of("reviewComment", "审核通过", "reason", "符合日程规则", "idempotencyKey", "approve-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/publish", bearerHeaders("admin-token", requestId("setup-publish-" + purpose)), Map.of("reason", "发布日程", "idempotencyKey", "publish-" + purpose));
        return eventId;
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        return bearerHeaders(token, requestId, Map.of());
    }

    private HttpHeaders bearerHeaders(String token, String requestId, Map<String, String> extraHeaders) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        extraHeaders.forEach(headers::set);
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

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "PostgreSQL 日程");
        body.put("summary", "PostgreSQL 日程摘要");
        body.put("description", "日程描述覆盖真实 HTTP、PostgreSQL 业务表和公共日志。");
        body.put("type", "MAINTENANCE");
        body.put("visibility", "PUBLIC");
        body.put("startAt", "2026-06-01T12:00:00Z");
        body.put("endAt", "2026-06-01T14:00:00Z");
        body.put("timezone", "Asia/Shanghai");
        body.put("allDay", false);
        body.put("location", "北冥服务器");
        body.put("relatedUrl", "/calendar/postgresql");
        body.put("labels", List.of("calendar", "postgresql"));
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
        return Map.of("from", "2026-05-25T00:00:00Z", "to", "2026-06-30T00:00:00Z", "mode", mode, "reason", "同步 activity 日历摘要", "idempotencyKey", idempotencyKey);
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
