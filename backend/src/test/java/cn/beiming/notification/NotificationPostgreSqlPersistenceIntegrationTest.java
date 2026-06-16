package cn.beiming.notification;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "beiming.business-core.test-control-headers.enabled=true",
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true"
        }
)
@Testcontainers
class NotificationPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "notification-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_notification_flow")
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
    NotificationStore store;

    @Autowired
    NotificationAuthContextProvider auth;

    @BeforeEach
    void setUp() {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
    }

    @Test
    void messageCreatePersistsMessageRecipientsAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("message-create");
        String title = unique("Pg Message");
        String idempotencyKey = "idem-" + title;
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId, Map.of(
                "recipientUserIds", List.of("user", "another_user"),
                "title", title,
                "body", "Notification PostgreSQL body",
                "type", "SYSTEM",
                "channels", List.of("IN_APP"),
                "sourceModule", "notification",
                "sourceId", "pg-message",
                "riskLevel", "HIGH",
                "reason", "message postgres evidence",
                "idempotencyKey", idempotencyKey
        ), 201);

        String notificationId = response.at("/data/notificationId").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT title FROM notification_messages WHERE notification_id = ?", notificationId, title);
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients WHERE notification_id = ? AND status = 'UNREAD' AND delivery_status = 'DELIVERED'", notificationId, 2L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_MESSAGE_CREATED' AND target_id = ?", requestId, notificationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.message.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/notifications/admin/messages'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL notification message create wrote notification_messages/notification_recipients/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void messageCreateIdempotencyReplaysPersistedResultInPostgreSql() throws Exception {
        String title = unique("Pg Replay");
        String idempotencyKey = "idem-" + title;
        Map<String, Object> body = messageBody(List.of("user"), title, idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId("message-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId("message-replay-second"), body, 201);

        assertThat(second.at("/data/notificationId").asText()).isEqualTo(first.at("/data/notificationId").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_messages WHERE title = ?", title, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients WHERE notification_id = ?", first.at("/data/notificationId").asText(), 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.message.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL notification message create idempotency replay kept one business row and one idempotency row.");
    }

    @Test
    void messageCreateIdempotencyRejectsSameKeyWithDifferentFingerprint() throws Exception {
        String title = unique("Pg Conflict");
        String idempotencyKey = "idem-" + title;
        exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId("message-conflict-first"), messageBody(List.of("user"), title, idempotencyKey), 201);

        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId("message-conflict-second"), messageBody(List.of("another_user"), title + "x", idempotencyKey), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.message.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_messages WHERE title = ?", title + "x", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL notification message create idempotency conflict preserved original rows and returned 409.");
    }

    @Test
    void messageFromTemplatePersistsRenderedMessageAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("from-template");
        String idempotencyKey = "idem-from-template-" + suffix();
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages/from-template", "admin-token", requestId, Map.of(
                "templateCode", "ENABLED_TEMPLATE",
                "recipientUserIds", List.of("user"),
                "variables", Map.of("playerName", "Steve", "result", "PASS"),
                "channels", List.of("IN_APP"),
                "sourceModule", "exam",
                "sourceId", "exam-pg",
                "reason", "from template postgres evidence",
                "idempotencyKey", idempotencyKey
        ), 201);

        String notificationId = response.at("/data/notificationId").asText();
        String templateId = response.at("/data/templateId").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT template_code FROM notification_messages WHERE notification_id = ?", notificationId, "ENABLED_TEMPLATE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients WHERE notification_id = ? AND recipient_user_id = 'user'", notificationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_templates WHERE template_id = ?", templateId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_MESSAGE_FROM_TEMPLATE_CREATED' AND target_id = ?", requestId, notificationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.message.from-template' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/notifications/admin/messages/from-template'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL notification from-template create wrote message/recipient/template/audit/idempotency/request rows and returned 201.");
    }

    @Test
    void markReadPersistsRecipientAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createMessage("read-target");
        String notificationId = created.at("/data/notificationId").asText();
        String requestId = requestId("read");

        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/notifications/me/" + notificationId + "/read", "user-token", requestId, Map.of("reason", "read postgres evidence"), 200);

        assertThat(response.at("/data/status").asText()).isEqualTo("READ");
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM notification_recipients WHERE notification_id = ? AND recipient_user_id = 'user'", notificationId, "READ");
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients WHERE notification_id = ? AND read_at IS NOT NULL", notificationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_RECIPIENT_READ' AND target_id = ?", requestId, notificationId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification mark read updated notification_recipients and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void markAllReadPersistsRecipientsAuditAndRequestLogInPostgreSql() throws Exception {
        createMessage("read-all-a");
        createMessage("read-all-b");
        String requestId = requestId("read-all");

        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/notifications/me/read-all", "user-token", requestId, Map.of(
                "type", "SYSTEM",
                "sourceModule", "notification"
        ), 200);

        assertThat(response.at("/data/updatedCount").asInt()).isGreaterThanOrEqualTo(2);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients r JOIN notification_messages m ON m.notification_id = r.notification_id WHERE r.recipient_user_id = 'user' AND m.source_module = 'notification' AND m.title LIKE 'read-all-%' AND r.status = 'UNREAD'", 0L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_RECIPIENTS_READ_ALL'", requestId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/notifications/me/read-all'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification read-all updated notification_recipients and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void archivePersistsRecipientAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createMessage("archive-target");
        String notificationId = created.at("/data/notificationId").asText();
        String requestId = requestId("archive");

        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/notifications/me/" + notificationId + "/archive", "user-token", requestId, Map.of("reason", "archive postgres evidence"), 200);

        assertThat(response.at("/data/status").asText()).isEqualTo("ARCHIVED");
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM notification_recipients WHERE notification_id = ? AND recipient_user_id = 'user'", notificationId, "ARCHIVED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_recipients WHERE notification_id = ? AND archived_at IS NOT NULL", notificationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_RECIPIENT_ARCHIVED' AND target_id = ?", requestId, notificationId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification archive updated notification_recipients and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void templateCreatePersistsTemplateAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String code = uniqueCode("PG_TEMPLATE");
        String idempotencyKey = "idem-" + code;
        String requestId = requestId("template-create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId, templateBody(code, idempotencyKey), 201);

        String templateId = response.at("/data/templateId").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT code FROM notification_templates WHERE template_id = ?", templateId, code);
            assertSingleValue(connection, "SELECT status FROM notification_templates WHERE template_id = ?", templateId, "ENABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_TEMPLATE_CREATED' AND target_id = ?", requestId, templateId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.template.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/notifications/admin/templates'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL notification template create wrote notification_templates/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void templateCreateIdempotencyReplaysPersistedResultInPostgreSql() throws Exception {
        String code = uniqueCode("PG_REPLAY");
        String idempotencyKey = "idem-" + code;
        Map<String, Object> body = templateBody(code, idempotencyKey);
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId("template-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId("template-replay-second"), body, 201);

        assertThat(second.at("/data/templateId").asText()).isEqualTo(first.at("/data/templateId").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_templates WHERE code = ?", code, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.template.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL notification template create idempotency replay kept one template row and one idempotency row.");
    }

    @Test
    void templateCreateIdempotencyRejectsSameKeyWithDifferentFingerprint() throws Exception {
        String code = uniqueCode("PG_TPL_CONFLICT");
        String idempotencyKey = "idem-" + code;
        exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId("template-conflict-first"), templateBody(code, idempotencyKey), 201);

        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId("template-conflict-second"), templateBody(code + "X", idempotencyKey), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'notification.template.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_templates WHERE code = ?", code + "X", 0L);
        }
        System.out.println("SQL evidence: PostgreSQL notification template create idempotency conflict preserved original rows and returned 409.");
    }

    @Test
    void templatePatchPersistsTemplateAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createTemplate("PG_PATCH");
        String templateId = created.at("/data/templateId").asText();
        String requestId = requestId("template-patch");

        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/notifications/admin/templates/" + templateId, "admin-token", requestId, Map.of(
                "name", "Patched Template",
                "titleTemplate", "Updated ${playerName}",
                "reason", "template patch postgres evidence"
        ), 200);

        assertThat(response.at("/data/version").asInt()).isEqualTo(2);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT name FROM notification_templates WHERE template_id = ?", templateId, "Patched Template");
            assertSingleValue(connection, "SELECT version FROM notification_templates WHERE template_id = ?", templateId, 2);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_TEMPLATE_UPDATED' AND target_id = ?", requestId, templateId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification template patch updated notification_templates and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void templateDisablePersistsTemplateAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createTemplate("PG_DISABLE");
        String templateId = created.at("/data/templateId").asText();
        String requestId = requestId("template-disable");

        exchange(HttpMethod.PATCH, "/api/v1/notifications/admin/templates/" + templateId + "/disable", "admin-token", requestId, Map.of("reason", "disable postgres evidence"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM notification_templates WHERE template_id = ?", templateId, "DISABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_templates WHERE template_id = ? AND disabled_at IS NOT NULL", templateId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_TEMPLATE_DISABLED' AND target_id = ?", requestId, templateId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification template disable updated notification_templates and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void templateEnablePersistsTemplateAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createTemplate("PG_ENABLE");
        String templateId = created.at("/data/templateId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/notifications/admin/templates/" + templateId + "/disable", "admin-token", requestId("template-enable-disable"), Map.of("reason", "disable before enable"), 200);
        String requestId = requestId("template-enable");

        exchange(HttpMethod.PATCH, "/api/v1/notifications/admin/templates/" + templateId + "/enable", "admin-token", requestId, Map.of("reason", "enable postgres evidence"), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM notification_templates WHERE template_id = ?", templateId, "ENABLED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM notification_templates WHERE template_id = ? AND disabled_at IS NULL", templateId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'NOTIFICATION_TEMPLATE_ENABLED' AND target_id = ?", requestId, templateId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL notification template enable updated notification_templates and wrote app_audit_logs/app_request_logs.");
    }

    private JsonNode createMessage(String label) throws Exception {
        return exchange(HttpMethod.POST, "/api/v1/notifications/admin/messages", "admin-token", requestId("create-" + label), messageBody(List.of("user"), unique(label), null), 201);
    }

    private JsonNode createTemplate(String prefix) throws Exception {
        return exchange(HttpMethod.POST, "/api/v1/notifications/admin/templates", "admin-token", requestId("create-" + prefix.toLowerCase()), templateBody(uniqueCode(prefix), null), 201);
    }

    private JsonNode exchange(HttpMethod method, String path, String token, String requestId, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> messageBody(List<String> recipients, String title, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("recipientUserIds", recipients);
        body.put("title", title);
        body.put("body", "Notification PostgreSQL body");
        body.put("type", "SYSTEM");
        body.put("channels", List.of("IN_APP"));
        body.put("sourceModule", "notification");
        body.put("sourceId", "pg-message");
        body.put("riskLevel", "MEDIUM");
        body.put("reason", "message postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> templateBody(String code, String idempotencyKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", code);
        body.put("name", "PostgreSQL Template");
        body.put("titleTemplate", "Hello ${playerName}");
        body.put("bodyTemplate", "Result ${result}");
        body.put("variableDefinitions", List.of(
                Map.of("name", "playerName", "required", true, "description", "player", "example", "Steve"),
                Map.of("name", "result", "required", true, "description", "result", "example", "PASS")
        ));
        body.put("type", "EXAM");
        body.put("channels", List.of("IN_APP"));
        body.put("reason", "template postgres evidence");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private String requestId(String name) {
        return "req-" + FLOW_ID + "-" + name;
    }

    private String unique(String prefix) {
        return prefix + "-" + suffix();
    }

    private String uniqueCode(String prefix) {
        return prefix + "_" + suffix().toUpperCase();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
        }
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
