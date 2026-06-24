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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class AuthRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "auth-register-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_auth_flow")
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
                    "SELECT username FROM auth_users WHERE username = ? AND status = 'PENDING_PROFILE'",
                    username, username);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_sessions s JOIN auth_users u ON u.user_id = s.user_id WHERE u.username = ? AND s.revoked = false",
                    username, 1L);
            assertSingleValue(connection,
                    "SELECT used_count FROM auth_invitations WHERE code_prefix = 'PLAYER-C'",
                    1);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'AUTH_REGISTER_SUCCESS' AND result = 'SUCCESS'",
                    requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/auth/register'",
                    requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL auth register wrote auth_users/auth_sessions/auth_invitations/app_audit_logs/app_request_logs and returned 201.");
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
                    "SELECT COUNT(*) FROM auth_sessions s JOIN auth_users u ON u.user_id = s.user_id WHERE u.username = ? AND s.revoked = false",
                    "owner", 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'AUTH_LOGIN_SUCCESS' AND result = 'SUCCESS'",
                    requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/auth/login'",
                    requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL auth login wrote auth_sessions/app_audit_logs/app_request_logs and returned 200.");
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
                    "SELECT COUNT(*) FROM auth_sessions s JOIN auth_users u ON u.user_id = s.user_id WHERE u.username = ? AND s.revoked = true",
                    "owner", 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_sessions WHERE token_hash = ?",
                    token, 0L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'AUTH_LOGOUT_SUCCESS' AND result = 'SUCCESS'",
                    logoutRequestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/auth/logout'",
                    logoutRequestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL auth logout updated auth_sessions and wrote app_audit_logs/app_request_logs.");
    }

    @Test
    void registerIdempotencyPersistsReplayEvidenceInPostgreSQL() throws Exception {
        String firstRequestId = "req-idem-first-" + FLOW_ID;
        String secondRequestId = "req-idem-second-" + FLOW_ID;
        String username = "idem_pg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, Object> body = Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", username,
                "password", "Password12345",
                "displayName", "Idem User",
                "idempotencyKey", "idem-" + username
        );

        JsonNode first = postJson("/api/v1/auth/register", firstRequestId, body, 201);
        JsonNode second = postJson("/api/v1/auth/register", secondRequestId, body, 201);

        assertThat(second.at("/data/user/id").asText()).isEqualTo(first.at("/data/user/id").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM auth_users WHERE username = ?",
                    username, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'anonymous' AND scope = 'auth.register' AND idempotency_key = ?",
                    "idem-" + username, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM app_request_logs WHERE request_id IN (?, ?) AND path = '/api/v1/auth/register'",
                    firstRequestId, secondRequestId, 2L);
        }
        System.out.println("SQL evidence: PostgreSQL auth register idempotency replay kept one auth_users row and wrote app_idempotency_records/app_request_logs.");
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

    private JsonNode postJson(String path, String requestId, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        ResponseEntity<String> response = http.postForEntity(
                "http://127.0.0.1:" + port + path,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.getBody());
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
