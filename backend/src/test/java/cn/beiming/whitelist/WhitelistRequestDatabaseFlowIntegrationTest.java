package cn.beiming.whitelist;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "whitelist.test-controls.enabled=true"
)
class WhitelistRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "whitelist-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:whitelist_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "whitelist_flow_request_log",
                    "whitelist_flow_applications",
                    "whitelist_flow_materials",
                    "whitelist_flow_handoffs",
                    "whitelist_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createApplicationRunsThroughBackendAndDatabaseThenReturnsPendingReview() throws Exception {
        String requestId = "req-wl-create-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/whitelist/me/applications",
                bearerHeaders("user-token", requestId),
                createBody("session-passed", "create-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/userId").asText()).isEqualTo("user");
        assertThat(json.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(json.at("/data/result").asText()).isEqualTo("PENDING");
        String applicationId = json.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM whitelist_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_APPLICATION_CREATED'",
                    FLOW_ID, applicationId, "PENDING_REVIEW");
            assertSingleValue(connection,
                    "SELECT user_id FROM whitelist_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_APPLICATION_CREATED'",
                    FLOW_ID, applicationId, "user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM whitelist_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'WHITELIST_APPLICATION_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM whitelist_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/whitelist/me/applications'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: whitelist create application reached backend, wrote application/audit/request rows, and returned 201.");
    }

    @Test
    void updateMaterialsRunsThroughBackendAndDatabaseThenReturnsUpdatedMaterials() throws Exception {
        String requestId = "req-wl-materials-" + FLOW_ID;
        String applicationId = createApplication("reject-user-token", "session-reject");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/whitelist/me/applications/" + applicationId,
                bearerHeaders("reject-user-token", requestId),
                Map.of("idempotencyKey", "materials-" + randomKey(), "materials", materials("updated material evidence"))
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo(applicationId);
        assertThat(json.at("/data/materials/0/content").asText()).isEqualTo("updated material evidence");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT material_count FROM whitelist_flow_materials WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_MATERIALS_UPDATED'",
                    FLOW_ID, applicationId, 1);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM whitelist_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'WHITELIST_MATERIALS_UPDATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM whitelist_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/whitelist/me/applications/" + applicationId, 200);
        }
        System.out.println("SQL evidence: whitelist material update reached backend, wrote material/audit/request rows, and returned 200.");
    }

    @Test
    void submitRunsThroughBackendAndDatabaseThenReturnsPendingReview() throws Exception {
        String requestId = "req-wl-submit-" + FLOW_ID;
        String applicationId = createApplication("profile-fail-token", "session-profile-fail");

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/whitelist/me/applications/" + applicationId + "/submit",
                bearerHeaders("profile-fail-token", requestId),
                Map.of("idempotencyKey", "submit-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo(applicationId);
        assertThat(json.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM whitelist_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_APPLICATION_SUBMITTED'",
                    FLOW_ID, applicationId, "PENDING_REVIEW");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM whitelist_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'WHITELIST_APPLICATION_SUBMITTED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM whitelist_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/whitelist/me/applications/" + applicationId + "/submit", 200);
        }
        System.out.println("SQL evidence: whitelist submit reached backend, wrote application/audit/request rows, and returned 200.");
    }

    @Test
    void approveRunsThroughBackendAndDatabaseThenReturnsAttendanceHandoff() throws Exception {
        String requestId = "req-wl-approve-" + FLOW_ID;
        String applicationId = createApplication("approve-user-token", "session-approve");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/whitelist/admin/applications/" + applicationId + "/approve",
                bearerHeaders("admin-token", requestId),
                Map.of("idempotencyKey", "approve-" + randomKey(), "reviewComment", "approved by database flow", "reason", "meets entry requirements")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/applicationId").asText()).isEqualTo(applicationId);
        assertThat(json.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(json.at("/data/profileActivation/status").asText()).isEqualTo("ACTIVATED");
        assertThat(json.at("/data/attendanceHandoff/initializationStatus").asText()).isEqualTo("WAITING_MODULE");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT result FROM whitelist_flow_applications WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_APPROVED'",
                    FLOW_ID, applicationId, "APPROVED");
            assertSingleValue(connection,
                    "SELECT initialization_status FROM whitelist_flow_handoffs WHERE flow_id = ? AND application_id = ? AND action = 'WHITELIST_ATTENDANCE_HANDOFF_GENERATED'",
                    FLOW_ID, applicationId, "WAITING_MODULE");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM whitelist_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'WHITELIST_APPROVED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM whitelist_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/whitelist/admin/applications/" + applicationId + "/approve", 200);
        }
        System.out.println("SQL evidence: whitelist approve reached backend, wrote approved application/handoff/audit/request rows, and returned 200.");
    }

    private String createApplication(String token, String sessionId) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/whitelist/me/applications",
                bearerHeaders(token, "req-setup-" + sessionId + "-" + FLOW_ID),
                createBody(sessionId, "setup-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/applicationId").asText();
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

    private Map<String, Object> createBody(String sessionId, String idempotencyKey) {
        return Map.of("examSessionId", sessionId, "idempotencyKey", idempotencyKey, "materials", materials("initial material evidence"));
    }

    private List<Map<String, Object>> materials(String content) {
        return List.of(Map.of("type", "TEXT", "title", "Evidence", "content", content));
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
                CREATE TABLE IF NOT EXISTS whitelist_flow_applications (
                    flow_id VARCHAR(128) NOT NULL,
                    application_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    review_direction VARCHAR(32) NOT NULL,
                    profile_status VARCHAR(32),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS whitelist_flow_materials (
                    flow_id VARCHAR(128) NOT NULL,
                    application_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    material_count INT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS whitelist_flow_handoffs (
                    flow_id VARCHAR(128) NOT NULL,
                    application_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    handoff_id VARCHAR(128) NOT NULL,
                    member_id VARCHAR(128) NOT NULL,
                    initialization_status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS whitelist_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS whitelist_flow_request_log (
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
        WhitelistFlowEvidenceRecorder whitelistFlowEvidenceRecorder() {
            return new JdbcWhitelistFlowEvidenceRecorder();
        }
    }

    static class JdbcWhitelistFlowEvidenceRecorder implements WhitelistFlowEvidenceRecorder {
        @Override
        public void recordApplicationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String applicationId = String.valueOf(payload.get("applicationId"));
            try (Connection connection = openConnection()) {
                Object profileActivation = payload.get("profileActivation");
                String profileStatus = profileActivation instanceof Map<?, ?> activation ? String.valueOf(activation.get("status")) : null;
                insert(connection,
                        "INSERT INTO whitelist_flow_applications(flow_id, application_id, action, user_id, status, result, review_direction, profile_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, applicationId, action, payload.get("userId"), payload.get("status"), payload.get("result"), payload.get("reviewDirection"), profileStatus, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, applicationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write whitelist application database evidence", exception);
            }
        }

        @Override
        public void recordMaterialsWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String applicationId = String.valueOf(payload.get("applicationId"));
            int materialCount = payload.get("materials") instanceof List<?> materials ? materials.size() : 0;
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO whitelist_flow_materials(flow_id, application_id, action, material_count, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, applicationId, action, materialCount, payload.get("status"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, applicationId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write whitelist material database evidence", exception);
            }
        }

        @Override
        public void recordAttendanceHandoffWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Object handoff = payload.get("attendanceHandoff");
            if (!(handoff instanceof Map<?, ?> handoffMap)) {
                return;
            }
            String applicationId = String.valueOf(payload.get("applicationId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO whitelist_flow_handoffs(flow_id, application_id, action, handoff_id, member_id, initialization_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, applicationId, action, handoffMap.get("handoffId"), handoffMap.get("memberId"), handoffMap.get("initializationStatus"), Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write whitelist handoff database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO whitelist_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO whitelist_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
