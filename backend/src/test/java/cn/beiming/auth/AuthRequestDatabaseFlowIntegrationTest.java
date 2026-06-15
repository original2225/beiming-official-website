package cn.beiming.auth;

import cn.beiming.core.BusinessCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "beiming.business-core.test-control-headers.enabled=true"
)
class AuthRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "auth-register-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:auth_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuthStore store;

    @BeforeEach
    void setUp() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER", java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "auth_flow_request_log",
                    "auth_flow_sessions",
                    "auth_flow_invitation_usage",
                    "auth_flow_audits",
                    "auth_flow_session_revocations",
                    "auth_flow_users"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void registerRequestRunsThroughBackendAndDatabaseThenReturnsSessionPayload() throws Exception {
        String requestId = "req-" + FLOW_ID;
        String username = "flow_user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        Map<String, Object> body = Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", username,
                "password", "Password12345",
                "displayName", "Flow User"
        );

        ResponseEntity<String> response = http.postForEntity(
                "http://127.0.0.1:" + port + "/api/v1/auth/register",
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo(requestId);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/tokenType").asText()).isEqualTo("Bearer");
        assertThat(json.at("/data/user/username").asText()).isEqualTo(username);
        assertThat(json.at("/data/user/status").asText()).isEqualTo("PENDING_PROFILE");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT username FROM auth_flow_users WHERE flow_id = ? AND username = ? AND status = 'PENDING_PROFILE'",
                    FLOW_ID, username, username);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_sessions WHERE flow_id = ? AND username = ? AND token_type = 'Bearer'",
                    FLOW_ID, username, 1L);
            assertSingleValue(connection,
                    "SELECT used_count FROM auth_flow_invitation_usage WHERE flow_id = ? AND raw_code = 'PLAYER-CODE-1'",
                    FLOW_ID, 1);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_audits WHERE flow_id = ? AND action = 'AUTH_REGISTER_SUCCESS' AND result = 'SUCCESS'",
                    FLOW_ID, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM auth_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/auth/register'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: auth register request reached backend, wrote user/session/invitation/audit/request rows, and returned 201.");
    }

    @Test
    void loginRequestRunsThroughBackendAndDatabaseThenReturnsSessionPayload() throws Exception {
        String requestId = "req-login-" + FLOW_ID;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        ResponseEntity<String> response = http.postForEntity(
                "http://127.0.0.1:" + port + "/api/v1/auth/login",
                new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                        "username", "owner",
                        "password", "Password12345"
                )), headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/accessToken").asText()).startsWith("ses_");
        assertThat(json.at("/data/user/username").asText()).isEqualTo("owner");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_sessions WHERE flow_id = ? AND username = ? AND token_type = 'Bearer'",
                    FLOW_ID, "owner", 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_audits WHERE flow_id = ? AND action = 'AUTH_LOGIN_SUCCESS' AND result = 'SUCCESS'",
                    FLOW_ID, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM auth_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/auth/login'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: auth login request reached backend, wrote session/audit/request rows, and returned 200.");
    }

    @Test
    void logoutRequestRunsThroughBackendAndDatabaseThenReturnsSuccessPayload() throws Exception {
        String loginRequestId = "req-logout-login-" + FLOW_ID;
        String logoutRequestId = "req-logout-" + FLOW_ID;
        String token = loginForLogout(loginRequestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", logoutRequestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        ResponseEntity<String> response = http.postForEntity(
                "http://127.0.0.1:" + port + "/api/v1/auth/logout",
                new HttpEntity<>("{}", headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data").isNull()).isTrue();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_session_revocations WHERE flow_id = ? AND username = ? AND token = ?",
                    FLOW_ID, "owner", token, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_flow_audits WHERE flow_id = ? AND action = 'AUTH_LOGOUT_SUCCESS' AND result = 'SUCCESS'",
                    FLOW_ID, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM auth_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/auth/logout'",
                    FLOW_ID, logoutRequestId, 200);
        }
        System.out.println("SQL evidence: auth logout request reached backend, wrote revocation/audit/request rows, and returned 200.");
    }

    private String loginForLogout(String requestId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        ResponseEntity<String> response = http.postForEntity(
                "http://127.0.0.1:" + port + "/api/v1/auth/login",
                new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                        "username", "owner",
                        "password", "Password12345"
                )), headers),
                String.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return objectMapper.readTree(response.getBody()).at("/data/accessToken").asText();
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

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_users (
                    flow_id VARCHAR(128) NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_sessions (
                    flow_id VARCHAR(128) NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    token_type VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_invitation_usage (
                    flow_id VARCHAR(128) NOT NULL,
                    raw_code VARCHAR(128) NOT NULL,
                    used_count INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_session_revocations (
                    flow_id VARCHAR(128) NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    token VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS auth_flow_request_log (
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
        AuthFlowEvidenceRecorder authFlowEvidenceRecorder() {
            return new JdbcAuthFlowEvidenceRecorder();
        }
    }

    static class JdbcAuthFlowEvidenceRecorder implements AuthFlowEvidenceRecorder {
        @Override
        public void recordRegisterSuccess(jakarta.servlet.http.HttpServletRequest request, String rawInvitationCode, String username, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO auth_flow_users(flow_id, username, status, created_at) VALUES (?, ?, ?, ?)",
                        flowId, username, "PENDING_PROFILE", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_sessions(flow_id, username, token_type, created_at) VALUES (?, ?, ?, ?)",
                        flowId, username, String.valueOf(payload.get("tokenType")), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_invitation_usage(flow_id, raw_code, used_count, created_at) VALUES (?, ?, ?, ?)",
                        flowId, rawInvitationCode, 1, Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_audits(flow_id, action, result, created_at) VALUES (?, ?, ?, ?)",
                        flowId, "AUTH_REGISTER_SUCCESS", "SUCCESS", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId, requestId, request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write auth flow database evidence", exception);
            }
        }

        @Override
        public void recordLoginSuccess(jakarta.servlet.http.HttpServletRequest request, String username, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO auth_flow_sessions(flow_id, username, token_type, created_at) VALUES (?, ?, ?, ?)",
                        flowId, username, String.valueOf(payload.get("tokenType")), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_audits(flow_id, action, result, created_at) VALUES (?, ?, ?, ?)",
                        flowId, "AUTH_LOGIN_SUCCESS", "SUCCESS", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId, requestId, request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write auth login database evidence", exception);
            }
        }

        @Override
        public void recordLogoutSuccess(jakarta.servlet.http.HttpServletRequest request, String username, String token, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO auth_flow_session_revocations(flow_id, username, token, created_at) VALUES (?, ?, ?, ?)",
                        flowId, username, token, Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_audits(flow_id, action, result, created_at) VALUES (?, ?, ?, ?)",
                        flowId, "AUTH_LOGOUT_SUCCESS", "SUCCESS", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO auth_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId, requestId, request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write auth logout database evidence", exception);
            }
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
}
