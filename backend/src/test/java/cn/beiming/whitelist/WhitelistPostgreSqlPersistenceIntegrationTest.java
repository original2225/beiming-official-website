package cn.beiming.whitelist;

import cn.beiming.admission.AdmissionCoreServiceApplication;
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
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "whitelist.test-controls.enabled=false"
        }
)
@Testcontainers
class WhitelistPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "whitelist-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_whitelist_pg")
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
    void setUp() {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
    }

    @Test
    void createApplicationPersistsWhitelistApplicationAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("create");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/whitelist/me/applications", bearerHeaders("user-token", requestId), createBody("session-passed", "create-" + randomKey()));

        assertThat(response.at("/code").asInt()).isZero();
        String applicationId = response.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM whitelist_applications WHERE application_id = ?", applicationId, "PENDING_REVIEW");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'WHITELIST_APPLICATION_CREATED' AND target_id = ?", requestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'user' AND scope = 'whitelist.create-application'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/whitelist/me/applications'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL whitelist create application wrote whitelist_applications/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void materialsAndSubmitPersistStateEventsAuditAndRequestLogInPostgreSql() throws Exception {
        String applicationId = createApplication("reject-user-token", "session-reject");
        String materialsRequestId = requestId("materials");
        JsonNode materials = exchange(HttpMethod.PATCH, "/api/v1/whitelist/me/applications/" + applicationId, bearerHeaders("reject-user-token", materialsRequestId),
                Map.of("idempotencyKey", "materials-" + randomKey(), "materials", materials("updated material")));
        assertThat(materials.at("/code").asInt()).isZero();

        String submitRequestId = requestId("submit");
        JsonNode submitted = exchange(HttpMethod.POST, "/api/v1/whitelist/me/applications/" + applicationId + "/submit", bearerHeaders("reject-user-token", submitRequestId),
                Map.of("idempotencyKey", "submit-" + randomKey()));
        assertThat(submitted.at("/code").asInt()).isZero();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT jsonb_array_length(materials_payload) FROM whitelist_applications WHERE application_id = ?", applicationId, 1);
            assertSingleValue(connection, "SELECT COUNT(*) FROM whitelist_state_events WHERE request_id = ? AND action = 'WHITELIST_MATERIALS_UPDATED'", materialsRequestId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'WHITELIST_APPLICATION_SUBMITTED' AND target_id = ?", submitRequestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", submitRequestId, "/api/v1/whitelist/me/applications/" + applicationId + "/submit", 200);
        }
        System.out.println("SQL evidence: PostgreSQL whitelist materials and submit wrote application/state-event/audit/request rows.");
    }

    @Test
    void approvePersistsProfileActivationAttendanceHandoffAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String applicationId = createApplication("approve-user-token", "session-approve");
        String requestId = requestId("approve");
        JsonNode approved = exchange(HttpMethod.PATCH, "/api/v1/whitelist/admin/applications/" + applicationId + "/approve", bearerHeaders("admin-token", requestId),
                Map.of("idempotencyKey", "approve-" + randomKey(), "reviewComment", "approved", "reason", "meets entry requirements"));

        assertThat(approved.at("/code").asInt()).isZero();
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM whitelist_applications WHERE application_id = ?", applicationId, "APPROVED");
            assertSingleValue(connection, "SELECT status FROM whitelist_profile_activations WHERE application_id = ?", applicationId, "ACTIVATED");
            assertSingleValue(connection, "SELECT initialization_status FROM whitelist_attendance_handoffs WHERE application_id = ?", applicationId, "WAITING_MODULE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'WHITELIST_APPROVED' AND target_id = ?", requestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'whitelist.approve'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", requestId, "/api/v1/whitelist/admin/applications/" + applicationId + "/approve", 200);
        }
        System.out.println("SQL evidence: PostgreSQL whitelist approve wrote application/profile activation/attendance handoff/audit/idempotency/request rows.");
    }

    private String createApplication(String token, String sessionId) throws Exception {
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/whitelist/me/applications", bearerHeaders(token, requestId("setup-" + sessionId)), createBody(sessionId, "setup-" + randomKey()));
        assertThat(response.at("/code").asInt()).isZero();
        return response.at("/data/applicationId").asText();
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        return headers;
    }

    private Map<String, Object> createBody(String sessionId, String idempotencyKey) {
        return Map.of("examSessionId", sessionId, "idempotencyKey", idempotencyKey, "materials", materials("initial material"));
    }

    private List<Map<String, Object>> materials(String content) {
        return List.of(Map.of("type", "TEXT", "title", "Evidence", "content", content));
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
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
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
