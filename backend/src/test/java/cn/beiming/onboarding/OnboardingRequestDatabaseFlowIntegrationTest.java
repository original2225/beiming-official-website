package cn.beiming.onboarding;

import cn.beiming.admission.AdmissionCoreServiceApplication;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "onboarding.test-controls.enabled=true"
)
class OnboardingRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "onboarding-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:onboarding_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
            for (String table : java.util.List.of(
                    "onboarding_flow_request_log",
                    "onboarding_flow_applications",
                    "onboarding_flow_confirmations",
                    "onboarding_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void startRunsThroughBackendAndDatabaseThenReturnsCreatedApplication() throws Exception {
        String requestId = "req-start-" + FLOW_ID;
        String userId = "flow-start-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/start",
                trustedUserHeaders(requestId, userId, "FlowStart"),
                Map.of("idempotencyKey", "start-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/userId").asText()).isEqualTo(userId);
        assertThat(json.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        String applicationId = json.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM onboarding_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'ONBOARDING_STARTED'",
                    FLOW_ID, applicationId, "IN_PROGRESS");
            assertSingleValue(connection,
                    "SELECT user_id FROM onboarding_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'ONBOARDING_STARTED'",
                    FLOW_ID, applicationId, userId);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM onboarding_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ONBOARDING_STARTED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM onboarding_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/onboarding/me/start'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: onboarding start reached backend, wrote application/audit/request rows, and returned 201.");
    }

    @Test
    void profileConfirmationRunsThroughBackendAndDatabaseThenReturnsConfirmedProfile() throws Exception {
        String userId = "flow-profile-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String requestId = "req-profile-" + FLOW_ID;
        JsonNode started = startApplication(userId, "FlowProfile");
        String applicationId = started.at("/data/applicationId").asText();

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/me/profile-confirmation",
                trustedUserHeaders(requestId, userId, "FlowProfile"),
                Map.of("confirmed", true, "idempotencyKey", "profile-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo(applicationId);
        assertThat(json.at("/data/profileConfirmation/confirmed").asBoolean()).isTrue();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT confirmation_type FROM onboarding_flow_confirmations WHERE flow_id = ? AND application_id = ? AND action = 'ONBOARDING_PROFILE_CONFIRMED'",
                    FLOW_ID, applicationId, "PROFILE");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM onboarding_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ONBOARDING_PROFILE_CONFIRMED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM onboarding_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/onboarding/me/profile-confirmation'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: onboarding profile confirmation reached backend, wrote confirmation/audit/request rows, and returned 200.");
    }

    @Test
    void advanceRunsThroughBackendAndDatabaseThenReturnsReadyForExam() throws Exception {
        String userId = "flow-advance-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String requestId = "req-advance-" + FLOW_ID;
        JsonNode started = startApplication(userId, "FlowAdvance");
        String applicationId = started.at("/data/applicationId").asText();
        confirmProfile(userId, "FlowAdvance");
        confirmRules(userId, "FlowAdvance");
        selectDirection(userId, "FlowAdvance");

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/advance",
                trustedUserHeaders(requestId, userId, "FlowAdvance"),
                Map.of("idempotencyKey", "advance-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo(applicationId);
        assertThat(json.at("/data/status").asText()).isEqualTo("READY_FOR_EXAM");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM onboarding_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'ONBOARDING_READY_FOR_EXAM'",
                    FLOW_ID, applicationId, "READY_FOR_EXAM");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM onboarding_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ONBOARDING_READY_FOR_EXAM'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM onboarding_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/onboarding/me/advance'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: onboarding advance reached backend, wrote ready-for-exam/audit/request rows, and returned 200.");
    }

    @Test
    void adminBlockRunsThroughBackendAndDatabaseThenReturnsBlockedApplication() throws Exception {
        String requestId = "req-block-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/admin/applications/app-in-progress/block",
                bearerHeaders("owner-token", requestId),
                Map.of(
                        "blockReason", "SQL flow review",
                        "notifyUser", false,
                        "reason", "block sql evidence",
                        "idempotencyKey", "block-" + UUID.randomUUID().toString().replace("-", "")
                )
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo("app-in-progress");
        assertThat(json.at("/data/status").asText()).isEqualTo("BLOCKED");
        assertThat(json.at("/data/blockedReason").asText()).isEqualTo("SQL flow review");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM onboarding_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'ONBOARDING_BLOCKED'",
                    FLOW_ID, "app-in-progress", "BLOCKED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM onboarding_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ONBOARDING_BLOCKED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM onboarding_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/onboarding/admin/applications/app-in-progress/block'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: onboarding admin block reached backend, wrote application/audit/request rows, and returned 200.");
    }

    private JsonNode startApplication(String userId, String displayName) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/start",
                trustedUserHeaders("req-setup-start-" + userId, userId, displayName),
                Map.of("idempotencyKey", "setup-start-" + UUID.randomUUID().toString().replace("-", ""))
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body());
    }

    private void confirmProfile(String userId, String displayName) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/me/profile-confirmation",
                trustedUserHeaders("req-setup-profile-" + userId, userId, displayName),
                Map.of("confirmed", true, "idempotencyKey", "setup-profile-" + UUID.randomUUID().toString().replace("-", ""))
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void confirmRules(String userId, String displayName) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/me/rules-confirmation",
                trustedUserHeaders("req-setup-rules-" + userId, userId, displayName),
                Map.of(
                        "confirmed", true,
                        "ruleContentId", "rule-current",
                        "ruleVersion", "2026-05-22",
                        "idempotencyKey", "setup-rules-" + UUID.randomUUID().toString().replace("-", "")
                )
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void selectDirection(String userId, String displayName) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/me/direction",
                trustedUserHeaders("req-setup-direction-" + userId, userId, displayName),
                Map.of("reviewDirection", "REDSTONE", "idempotencyKey", "setup-direction-" + UUID.randomUUID().toString().replace("-", ""))
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = baseHeaders(requestId);
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders trustedUserHeaders(String requestId, String userId, String displayName) {
        HttpHeaders headers = baseHeaders(requestId);
        headers.set("X-Gateway-Internal-Request-Id", "gw-" + requestId);
        headers.set("X-Beiming-Actor-User-Id", userId);
        headers.set("X-Beiming-Actor-Roles", "USER");
        headers.set("X-Beiming-Actor-Minecraft-Id", displayName + "Mc");
        headers.set("X-Beiming-Actor-Minecraft-Uuid", "uuid-" + userId);
        return headers;
    }

    private HttpHeaders baseHeaders(String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
                CREATE TABLE IF NOT EXISTS onboarding_flow_applications (
                    flow_id VARCHAR(128) NOT NULL,
                    application_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    current_step VARCHAR(64) NOT NULL,
                    review_direction VARCHAR(32),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS onboarding_flow_confirmations (
                    flow_id VARCHAR(128) NOT NULL,
                    application_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    confirmation_type VARCHAR(32) NOT NULL,
                    confirmed BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS onboarding_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS onboarding_flow_request_log (
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
        OnboardingFlowEvidenceRecorder onboardingFlowEvidenceRecorder() {
            return new JdbcOnboardingFlowEvidenceRecorder();
        }
    }

    static class JdbcOnboardingFlowEvidenceRecorder implements OnboardingFlowEvidenceRecorder {
        @Override
        public void recordApplicationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String applicationId = String.valueOf(payload.get("applicationId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO onboarding_flow_applications(flow_id, application_id, action, user_id, status, current_step, review_direction, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, applicationId, action, payload.get("userId"), payload.get("status"), payload.get("currentStep"), payload.get("reviewDirection"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, applicationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write onboarding application database evidence", exception);
            }
        }

        @Override
        public void recordConfirmationWrite(HttpServletRequest request, String action, String confirmationType, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String applicationId = String.valueOf(payload.get("applicationId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO onboarding_flow_confirmations(flow_id, application_id, action, confirmation_type, confirmed, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, applicationId, action, confirmationType, true, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, requestId, action, applicationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write onboarding confirmation database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO onboarding_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO onboarding_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
