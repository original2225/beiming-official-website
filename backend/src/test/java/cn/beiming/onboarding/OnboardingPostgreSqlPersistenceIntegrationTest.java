package cn.beiming.onboarding;

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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "onboarding.test-controls.enabled=false"
        }
)
@Testcontainers
class OnboardingPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "onboarding-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_onboarding_pg")
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
    void startPersistsOnboardingApplicationAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("start");
        String userId = uniqueUser("start");
        JsonNode response = exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/start",
                trustedUserHeaders(userId, "Start User", requestId),
                Map.of("idempotencyKey", "start-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.at("/code").asInt()).isZero();
        String applicationId = response.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM onboarding_applications WHERE application_id = ?", applicationId, "IN_PROGRESS");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ONBOARDING_STARTED' AND target_id = ?", requestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = ? AND scope = 'onboarding.start'", userId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/onboarding/me/start'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL onboarding start wrote onboarding_applications/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void profileConfirmationPersistsOnboardingConfirmationAuditAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("profile");
        String userId = uniqueUser("profile");
        exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/start",
                trustedUserHeaders(userId, "Profile User", requestId("profile-setup")),
                Map.of("idempotencyKey", "profile-setup-" + UUID.randomUUID().toString().replace("-", ""))
        );
        JsonNode response = exchange(
                HttpMethod.PATCH,
                "/api/v1/onboarding/me/profile-confirmation",
                trustedUserHeaders(userId, "Profile User", requestId),
                Map.of("confirmed", true, "idempotencyKey", "profile-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.at("/code").asInt()).isZero();
        String applicationId = response.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT confirmation_type FROM onboarding_confirmations WHERE application_id = ? AND confirmation_type = 'PROFILE'", applicationId, "PROFILE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ONBOARDING_PROFILE_CONFIRMED' AND target_id = ?", requestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/onboarding/me/profile-confirmation'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL onboarding profile confirmation wrote onboarding_confirmations/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void advancePersistsReadyForExamAuditAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("advance");
        String userId = uniqueUser("advance");
        exchange(HttpMethod.POST, "/api/v1/onboarding/me/start", trustedUserHeaders(userId, "Advance User", requestId("setup-start")),
                Map.of("idempotencyKey", "setup-start-" + UUID.randomUUID().toString().replace("-", "")));
        exchange(HttpMethod.PATCH, "/api/v1/onboarding/me/profile-confirmation", trustedUserHeaders(userId, "Advance User", requestId("setup-profile")),
                Map.of("confirmed", true, "idempotencyKey", "setup-profile-" + UUID.randomUUID().toString().replace("-", "")));
        exchange(HttpMethod.PATCH, "/api/v1/onboarding/me/rules-confirmation", trustedUserHeaders(userId, "Advance User", requestId("setup-rules")),
                Map.of("confirmed", true, "ruleContentId", "rule-current", "ruleVersion", "2026-05-22", "idempotencyKey", "setup-rules-" + UUID.randomUUID().toString().replace("-", "")));
        exchange(HttpMethod.PATCH, "/api/v1/onboarding/me/direction", trustedUserHeaders(userId, "Advance User", requestId("setup-direction")),
                Map.of("reviewDirection", "REDSTONE", "idempotencyKey", "setup-direction-" + UUID.randomUUID().toString().replace("-", "")));

        JsonNode response = exchange(
                HttpMethod.POST,
                "/api/v1/onboarding/me/advance",
                trustedUserHeaders(userId, "Advance User", requestId),
                Map.of("idempotencyKey", "advance-" + UUID.randomUUID().toString().replace("-", ""))
        );

        assertThat(response.at("/code").asInt()).isZero();
        String applicationId = response.at("/data/applicationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM onboarding_applications WHERE application_id = ?", applicationId, "READY_FOR_EXAM");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ONBOARDING_READY_FOR_EXAM' AND target_id = ?", requestId, applicationId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/onboarding/me/advance'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL onboarding advance wrote onboarding_applications/app_audit_logs/app_request_logs and returned 200.");
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

    private HttpHeaders trustedUserHeaders(String userId, String displayName, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Gateway-Internal-Request-Id", "gw-" + requestId);
        headers.set("X-Beiming-Actor-User-Id", userId);
        headers.set("X-Beiming-Actor-Display-Name", displayName);
        headers.set("X-Beiming-Actor-Roles", "USER");
        headers.set("X-Beiming-Actor-Minecraft-Id", userId + "Mc");
        headers.set("X-Beiming-Actor-Minecraft-Uuid", "uuid-" + userId);
        return headers;
    }

    private String requestId(String prefix) {
        return "req-" + prefix + "-" + FLOW_ID;
    }

    private String uniqueUser(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
