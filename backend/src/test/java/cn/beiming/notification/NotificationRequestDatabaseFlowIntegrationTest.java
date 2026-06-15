package cn.beiming.notification;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "beiming.business-core.test-control-headers.enabled=true"
)
class NotificationRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "notification-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:notification_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    NotificationStore store;

    @Autowired
    NotificationAuthContextProvider auth;

    @BeforeEach
    void setUp() throws Exception {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "notification_flow_request_log",
                    "notification_flow_recipients",
                    "notification_flow_messages",
                    "notification_flow_templates",
                    "notification_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void messageCreateRunsThroughBackendAndDatabaseThenReturnsCreatedMessage() throws Exception {
        String requestId = "req-message-" + FLOW_ID;
        String title = "SQL Message " + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/notifications/admin/messages",
                bearerHeaders("admin-token", requestId),
                Map.of(
                        "recipientUserIds", List.of("user"),
                        "title", title,
                        "body", "Notification SQL evidence body",
                        "type", "SYSTEM",
                        "channels", List.of("IN_APP"),
                        "sourceModule", "notification",
                        "sourceId", "flow-message",
                        "reason", "message sql evidence"
                )
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/title").asText()).isEqualTo(title);
        assertThat(json.at("/data/recipients/0/status").asText()).isEqualTo("UNREAD");
        String notificationId = json.at("/data/notificationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT title FROM notification_flow_messages WHERE flow_id = ? AND notification_id = ? AND action = 'NOTIFICATION_MESSAGE_CREATED'",
                    FLOW_ID, notificationId, title);
            assertSingleValue(connection,
                    "SELECT status FROM notification_flow_recipients WHERE flow_id = ? AND notification_id = ? AND recipient_user_id = ? AND action = 'NOTIFICATION_MESSAGE_CREATED'",
                    FLOW_ID, notificationId, "user", "UNREAD");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM notification_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'NOTIFICATION_MESSAGE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM notification_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/notifications/admin/messages'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: notification message create reached backend, wrote message/recipient/audit/request rows, and returned 201.");
    }

    @Test
    void markReadRunsThroughBackendAndDatabaseThenReturnsReadRecipient() throws Exception {
        String requestId = "req-read-" + FLOW_ID;
        String notificationId = store.notificationId("unread-user");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/notifications/me/" + notificationId + "/read",
                bearerHeaders("user-token", requestId),
                Map.of("reason", "read sql evidence")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/notificationId").asText()).isEqualTo(notificationId);
        assertThat(json.at("/data/status").asText()).isEqualTo("READ");
        assertThat(json.at("/data/readAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM notification_flow_recipients WHERE flow_id = ? AND notification_id = ? AND recipient_user_id = ? AND action = 'NOTIFICATION_RECIPIENT_READ'",
                    FLOW_ID, notificationId, "user", "READ");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM notification_flow_recipients WHERE flow_id = ? AND notification_id = ? AND action = 'NOTIFICATION_RECIPIENT_READ' AND read_at IS NOT NULL",
                    FLOW_ID, notificationId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM notification_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/notifications/me/" + notificationId + "/read", 200);
        }
        System.out.println("SQL evidence: notification mark read reached backend, wrote recipient/request rows, and returned 200.");
    }

    @Test
    void archiveRunsThroughBackendAndDatabaseThenReturnsArchivedRecipient() throws Exception {
        String requestId = "req-archive-" + FLOW_ID;
        String notificationId = store.notificationId("concurrent-archive-user");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/notifications/me/" + notificationId + "/archive",
                bearerHeaders("user-token", requestId),
                Map.of("reason", "archive sql evidence")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/notificationId").asText()).isEqualTo(notificationId);
        assertThat(json.at("/data/status").asText()).isEqualTo("ARCHIVED");
        assertThat(json.at("/data/archivedAt").asText()).isNotBlank();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM notification_flow_recipients WHERE flow_id = ? AND notification_id = ? AND recipient_user_id = ? AND action = 'NOTIFICATION_RECIPIENT_ARCHIVED'",
                    FLOW_ID, notificationId, "user", "ARCHIVED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM notification_flow_recipients WHERE flow_id = ? AND notification_id = ? AND action = 'NOTIFICATION_RECIPIENT_ARCHIVED' AND archived_at IS NOT NULL",
                    FLOW_ID, notificationId, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM notification_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'NOTIFICATION_RECIPIENT_ARCHIVED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM notification_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/notifications/me/" + notificationId + "/archive", 200);
        }
        System.out.println("SQL evidence: notification archive reached backend, wrote recipient/audit/request rows, and returned 200.");
    }

    @Test
    void templateCreateRunsThroughBackendAndDatabaseThenReturnsCreatedTemplate() throws Exception {
        String requestId = "req-template-" + FLOW_ID;
        String code = "FLOW_TEMPLATE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/notifications/admin/templates",
                bearerHeaders("admin-token", requestId),
                Map.of(
                        "code", code,
                        "name", "Flow Template",
                        "titleTemplate", "Hello ${playerName}",
                        "bodyTemplate", "Result ${result}",
                        "variableDefinitions", List.of(
                                Map.of("name", "playerName", "required", true, "description", "player", "example", "Steve"),
                                Map.of("name", "result", "required", true, "description", "result", "example", "PASS")
                        ),
                        "type", "EXAM",
                        "channels", List.of("IN_APP"),
                        "reason", "template sql evidence"
                )
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/code").asText()).isEqualTo(code);
        assertThat(json.at("/data/status").asText()).isEqualTo("ENABLED");
        String templateId = json.at("/data/templateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT code FROM notification_flow_templates WHERE flow_id = ? AND template_id = ? AND action = 'NOTIFICATION_TEMPLATE_CREATED'",
                    FLOW_ID, templateId, code);
            assertSingleValue(connection,
                    "SELECT status FROM notification_flow_templates WHERE flow_id = ? AND template_id = ? AND action = 'NOTIFICATION_TEMPLATE_CREATED'",
                    FLOW_ID, templateId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM notification_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'NOTIFICATION_TEMPLATE_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM notification_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/notifications/admin/templates'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: notification template create reached backend, wrote template/audit/request rows, and returned 201.");
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
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
                CREATE TABLE IF NOT EXISTS notification_flow_messages (
                    flow_id VARCHAR(128) NOT NULL,
                    notification_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    recipient_total INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS notification_flow_recipients (
                    flow_id VARCHAR(128) NOT NULL,
                    notification_id VARCHAR(128) NOT NULL,
                    recipient_user_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    read_at VARCHAR(64),
                    archived_at VARCHAR(64),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS notification_flow_templates (
                    flow_id VARCHAR(128) NOT NULL,
                    template_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    code VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    version INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS notification_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS notification_flow_request_log (
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

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        NotificationFlowEvidenceRecorder notificationFlowEvidenceRecorder() {
            return new JdbcNotificationFlowEvidenceRecorder();
        }
    }

    static class JdbcNotificationFlowEvidenceRecorder implements NotificationFlowEvidenceRecorder {
        @Override
        public void recordMessageWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String notificationId = String.valueOf(payload.get("notificationId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO notification_flow_messages(flow_id, notification_id, action, title, type, recipient_total, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, notificationId, action, payload.get("title"), payload.get("type"), payload.get("recipientTotal"), Timestamp.from(Instant.now()));
                for (Map<String, Object> recipient : recipients(payload)) {
                    insert(connection,
                            "INSERT INTO notification_flow_recipients(flow_id, notification_id, recipient_user_id, action, status, read_at, archived_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            flowId, notificationId, recipient.get("recipientUserId"), action, recipient.get("status"), recipient.get("readAt"), recipient.get("archivedAt"), Timestamp.from(Instant.now()));
                }
                insertAuditAndRequest(connection, flowId, requestId, action, notificationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write notification message database evidence", exception);
            }
        }

        @Override
        public void recordRecipientWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String notificationId = String.valueOf(payload.get("notificationId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO notification_flow_recipients(flow_id, notification_id, recipient_user_id, action, status, read_at, archived_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, notificationId, payload.get("recipientUserId"), action, payload.get("status"), payload.get("readAt"), payload.get("archivedAt"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, notificationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write notification recipient database evidence", exception);
            }
        }

        @Override
        public void recordTemplateWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String templateId = String.valueOf(payload.get("templateId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO notification_flow_templates(flow_id, template_id, action, code, status, version, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, templateId, action, payload.get("code"), payload.get("status"), payload.get("version"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, templateId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write notification template database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO notification_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO notification_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, requestId, path, responseCode, Timestamp.from(Instant.now()));
        }

        private static List<Map<String, Object>> recipients(Map<String, Object> payload) {
            Object raw = payload.get("recipients");
            if (!(raw instanceof List<?> rows)) {
                return List.of();
            }
            List<Map<String, Object>> recipients = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    Map<String, Object> recipient = new java.util.LinkedHashMap<>();
                    map.forEach((key, value) -> recipient.put(String.valueOf(key), value));
                    recipients.add(recipient);
                }
            }
            return recipients;
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
