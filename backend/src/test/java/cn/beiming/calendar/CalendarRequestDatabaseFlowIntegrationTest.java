package cn.beiming.calendar;

import cn.beiming.engagement.EngagementCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
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
        properties = "calendar.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CalendarRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "calendar-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:calendar_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "calendar_flow_request_log",
                    "calendar_flow_events",
                    "calendar_flow_watches",
                    "calendar_flow_syncs",
                    "calendar_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createEventRunsThroughBackendAndDatabaseThenReturnsDraftEvent() throws Exception {
        String requestId = "req-cal-event-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/admin/events",
                bearerHeaders("admin-token", requestId),
                eventBody("event-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("admin-user");
        String eventId = json.at("/data/eventId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM calendar_flow_events WHERE flow_id = ? AND event_id = ? AND action = 'CALENDAR_EVENT_CREATED'",
                    FLOW_ID, eventId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT source_type FROM calendar_flow_events WHERE flow_id = ? AND event_id = ? AND action = 'CALENDAR_EVENT_CREATED'",
                    FLOW_ID, eventId, "MANUAL");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM calendar_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CALENDAR_EVENT_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM calendar_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/calendar/admin/events'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: calendar event create reached backend, wrote event/audit/request rows, and returned 201.");
    }

    @Test
    void publishAndWatchRunThroughBackendAndDatabaseThenReturnActiveWatch() throws Exception {
        String eventId = createApprovedPublishedEvent("setup-watch");
        String watchRequestId = "req-cal-watch-" + FLOW_ID;

        TestHttpResponse watched = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/me/events/" + eventId + "/watch",
                bearerHeaders("member-user-1-token", watchRequestId),
                watchBody("watch-" + randomKey())
        );

        assertThat(watched.statusCode()).isEqualTo(201);
        JsonNode watchJson = objectMapper.readTree(watched.body());
        assertThat(watchJson.at("/data/watch/status").asText()).isEqualTo("ACTIVE");
        assertThat(watchJson.at("/data/watch/userId").asText()).isEqualTo("member-user-1");
        String watchId = watchJson.at("/data/watch/watchId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM calendar_flow_events WHERE flow_id = ? AND event_id = ? AND action = 'CALENDAR_EVENT_PUBLISHED'",
                    FLOW_ID, eventId, "PUBLISHED");
            assertSingleValue(connection,
                    "SELECT status FROM calendar_flow_watches WHERE flow_id = ? AND watch_id = ? AND action = 'CALENDAR_EVENT_WATCHED'",
                    FLOW_ID, watchId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT user_id FROM calendar_flow_watches WHERE flow_id = ? AND watch_id = ? AND action = 'CALENDAR_EVENT_WATCHED'",
                    FLOW_ID, watchId, "member-user-1");
            assertSingleValue(connection,
                    "SELECT response_code FROM calendar_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, watchRequestId, "/api/v1/calendar/me/events/" + eventId + "/watch", 201);
        }
        System.out.println("SQL evidence: calendar publish and watch reached backend, wrote event/watch/request rows, and returned 201.");
    }

    @Test
    void unwatchRunsThroughBackendAndDatabaseThenReturnsCanceledWatch() throws Exception {
        String eventId = createApprovedPublishedEvent("setup-unwatch");
        String watchId = watch(eventId, "setup-unwatch");
        String requestId = "req-cal-unwatch-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/me/events/" + eventId + "/unwatch",
                bearerHeaders("member-user-1-token", requestId),
                Map.of("reason", "取消提醒", "idempotencyKey", "unwatch-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/watch/watchId").asText()).isEqualTo(watchId);
        assertThat(json.at("/data/watch/status").asText()).isEqualTo("CANCELED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM calendar_flow_watches WHERE flow_id = ? AND watch_id = ? AND action = 'CALENDAR_EVENT_UNWATCHED'",
                    FLOW_ID, watchId, "CANCELED");
            assertSingleValue(connection,
                    "SELECT response_code FROM calendar_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/calendar/me/events/" + eventId + "/unwatch", 200);
        }
        System.out.println("SQL evidence: calendar unwatch reached backend, wrote canceled watch/request rows, and returned 200.");
    }

    @Test
    void activitySyncRunsThroughBackendAndDatabaseThenReturnsSyncedSnapshot() throws Exception {
        String requestId = "req-cal-sync-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/admin/sync/activity",
                bearerHeaders("admin-token", requestId),
                syncBody("sync-" + randomKey(), "UPSERT_SNAPSHOT")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/syncStatus").asText()).isEqualTo("SYNCED");
        assertThat(json.at("/data/createdTotal").asInt()).isEqualTo(1);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT sync_status FROM calendar_flow_syncs WHERE flow_id = ? AND request_id = ? AND action = 'CALENDAR_ACTIVITY_SYNCED'",
                    FLOW_ID, requestId, "SYNCED");
            assertSingleValue(connection,
                    "SELECT source_type FROM calendar_flow_events WHERE flow_id = ? AND source_id = ? AND action = 'CALENDAR_ACTIVITY_SYNCED'",
                    FLOW_ID, "act-calendar-summary-1", "ACTIVITY");
            assertSingleValue(connection,
                    "SELECT response_code FROM calendar_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/calendar/admin/sync/activity'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: calendar activity sync reached backend, wrote sync/event/request rows, and returned 200.");
    }

    private String createApprovedPublishedEvent(String purpose) throws Exception {
        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/admin/events",
                bearerHeaders("admin-token", "req-setup-event-" + purpose + "-" + FLOW_ID),
                eventBody(purpose + "-" + randomKey())
        );
        assertThat(created.statusCode()).isEqualTo(201);
        String eventId = objectMapper.readTree(created.body()).at("/data/eventId").asText();
        exchangeOk(HttpMethod.POST, "/api/v1/calendar/admin/events/" + eventId + "/submit", "helper-token", "req-setup-submit-" + purpose, Map.of("reason", "提交审核", "idempotencyKey", purpose + "-submit-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/approve", "helper-token", "req-setup-approve-" + purpose, Map.of("reviewComment", "审核通过", "reason", "符合日程规则", "idempotencyKey", purpose + "-approve-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/calendar/admin/events/" + eventId + "/publish", "admin-token", "req-setup-publish-" + purpose, Map.of("reason", "发布日程", "idempotencyKey", purpose + "-publish-" + randomKey()));
        return eventId;
    }

    private String watch(String eventId, String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/calendar/me/events/" + eventId + "/watch",
                bearerHeaders("member-user-1-token", "req-setup-watch-" + purpose + "-" + FLOW_ID),
                watchBody(purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/watch/watchId").asText();
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

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "数据库流日程");
        body.put("summary", "数据库流日程摘要");
        body.put("description", "日程描述覆盖真实 HTTP、后端处理和 SQL 证据。");
        body.put("type", "MAINTENANCE");
        body.put("visibility", "PUBLIC");
        body.put("startAt", "2026-06-01T12:00:00Z");
        body.put("endAt", "2026-06-01T14:00:00Z");
        body.put("timezone", "Asia/Shanghai");
        body.put("allDay", false);
        body.put("location", "北冥服务器");
        body.put("relatedUrl", "/calendar/database-flow");
        body.put("labels", List.of("calendar", "database-flow"));
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
                CREATE TABLE IF NOT EXISTS calendar_flow_events (
                    flow_id VARCHAR(128) NOT NULL,
                    event_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    source_id VARCHAR(128),
                    status VARCHAR(32) NOT NULL,
                    type VARCHAR(64) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS calendar_flow_watches (
                    flow_id VARCHAR(128) NOT NULL,
                    watch_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    event_id VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS calendar_flow_syncs (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    sync_status VARCHAR(32) NOT NULL,
                    created_total INT NOT NULL,
                    updated_total INT NOT NULL,
                    skipped_total INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS calendar_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS calendar_flow_request_log (
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

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        CalendarFlowEvidenceRecorder calendarFlowEvidenceRecorder() {
            return new JdbcCalendarFlowEvidenceRecorder();
        }
    }

    static class JdbcCalendarFlowEvidenceRecorder implements CalendarFlowEvidenceRecorder {
        @Override
        public void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> source = map(payload.get("source"));
            try (Connection connection = openConnection()) {
                insertEvent(connection, flowId, action, payload, source);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("eventId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write calendar event database evidence", exception);
            }
        }

        @Override
        public void recordWatchWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> watch = map(payload.get("watch"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO calendar_flow_watches(flow_id, watch_id, action, event_id, user_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, watch.get("watchId"), action, watch.get("eventId"), watch.get("userId"), watch.get("status"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(watch.get("watchId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write calendar watch database evidence", exception);
            }
        }

        @Override
        public void recordActivitySyncWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO calendar_flow_syncs(flow_id, request_id, action, sync_status, created_total, updated_total, skipped_total, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, request.getHeader("X-Request-Id"), action, payload.get("syncStatus"), payload.get("createdTotal"), payload.get("updatedTotal"), payload.get("skippedTotal"), Timestamp.from(Instant.now()));
                Object eventsValue = payload.get("syncedEvents");
                if (eventsValue instanceof List<?> events) {
                    for (Object eventValue : events) {
                        if (eventValue instanceof Map<?, ?> event) {
                            insertEvent(connection, flowId, action, event, map(event.get("source")));
                        }
                    }
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, "activity", request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write calendar sync database evidence", exception);
            }
        }

        private static void insertEvent(Connection connection, String flowId, String action, Map<?, ?> payload, Map<?, ?> source) throws Exception {
            insert(connection,
                    "INSERT INTO calendar_flow_events(flow_id, event_id, action, source_type, source_id, status, type, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("eventId"), action, source.get("sourceType"), source.get("sourceId"), payload.get("status"), payload.get("type"), payload.get("createdBy"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO calendar_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO calendar_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
