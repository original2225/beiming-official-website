package cn.beiming.profile;

import cn.beiming.core.BusinessCoreServiceApplication;
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
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "beiming.business-core.test-control-headers.enabled=true"
)
class ProfileRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "profile-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:profile_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProfileStore store;

    @Autowired
    ProfileAuthContextProvider auth;

    @BeforeEach
    void setUp() throws Exception {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            List.of(
                    "profile_flow_request_log",
                    "profile_flow_members",
                    "profile_flow_groups",
                    "profile_flow_audits"
            ).forEach(table -> deleteFlowRows(statement, table));
        }
    }

    @Test
    void selfProfilePatchRunsThroughBackendAndDatabaseThenReturnsUpdatedProfile() throws Exception {
        String requestId = "req-self-" + FLOW_ID;
        String bio = "SQL verified self bio";

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/profile/me",
                bearerHeaders("active-member-token", requestId),
                Map.of("bio", bio, "visibility", "PRIVATE", "reason", "self sql evidence")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/userId").asText()).isEqualTo("active_member");
        assertThat(json.at("/data/bio").asText()).isEqualTo(bio);
        assertThat(json.at("/data/visibility").asText()).isEqualTo("PRIVATE");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT bio FROM profile_flow_members WHERE flow_id = ? AND member_id = ? AND action = 'PROFILE_SELF_UPDATED'",
                    FLOW_ID, store.memberIdByUserId("active_member"), bio);
            assertSingleValue(connection,
                    "SELECT visibility FROM profile_flow_members WHERE flow_id = ? AND member_id = ? AND action = 'PROFILE_SELF_UPDATED'",
                    FLOW_ID, store.memberIdByUserId("active_member"), "PRIVATE");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM profile_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'PROFILE_SELF_UPDATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM profile_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/profile/me'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: profile self patch reached backend, wrote member/audit/request rows, and returned 200.");
    }

    @Test
    void groupCreateRunsThroughBackendAndDatabaseThenReturnsCreatedGroup() throws Exception {
        String requestId = "req-group-" + FLOW_ID;
        String groupName = "FlowGroup" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/profile/admin/groups",
                bearerHeaders("admin-token", requestId),
                Map.of(
                        "name", groupName,
                        "description", "SQL flow group",
                        "color", "#123ABC",
                        "sortOrder", 12,
                        "reason", "group sql evidence"
                )
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/name").asText()).isEqualTo(groupName);
        String groupId = json.at("/data/id").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT name FROM profile_flow_groups WHERE flow_id = ? AND group_id = ? AND action = 'PROFILE_GROUP_CREATED'",
                    FLOW_ID, groupId, groupName);
            assertSingleValue(connection,
                    "SELECT archived FROM profile_flow_groups WHERE flow_id = ? AND group_id = ? AND action = 'PROFILE_GROUP_CREATED'",
                    FLOW_ID, groupId, false);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM profile_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'PROFILE_GROUP_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM profile_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/profile/admin/groups'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: profile group create reached backend, wrote group/audit/request rows, and returned 201.");
    }

    @Test
    void memberStatusPatchRunsThroughBackendAndDatabaseThenReturnsUpdatedStatus() throws Exception {
        String requestId = "req-status-" + FLOW_ID;
        String memberId = store.memberIdByUserId("active_member");

        TestHttpResponse response = exchange(
                HttpMethod.PATCH,
                "/api/v1/profile/admin/members/" + memberId + "/status",
                bearerHeaders("admin-token", requestId),
                Map.of("status", "INACTIVE", "reason", "status sql evidence")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/memberId").asText()).isEqualTo(memberId);
        assertThat(json.at("/data/status").asText()).isEqualTo("INACTIVE");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM profile_flow_members WHERE flow_id = ? AND member_id = ? AND action = 'PROFILE_MEMBER_STATUS_CHANGED'",
                    FLOW_ID, memberId, "INACTIVE");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM profile_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'PROFILE_MEMBER_STATUS_CHANGED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM profile_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/profile/admin/members/" + memberId + "/status", 200);
        }
        System.out.println("SQL evidence: profile status patch reached backend, wrote member/audit/request rows, and returned 200.");
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
                CREATE TABLE IF NOT EXISTS profile_flow_members (
                    flow_id VARCHAR(128) NOT NULL,
                    member_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32),
                    visibility VARCHAR(32),
                    bio VARCHAR(1000),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS profile_flow_groups (
                    flow_id VARCHAR(128) NOT NULL,
                    group_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    name VARCHAR(64) NOT NULL,
                    archived BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS profile_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS profile_flow_request_log (
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
        ProfileFlowEvidenceRecorder profileFlowEvidenceRecorder() {
            return new JdbcProfileFlowEvidenceRecorder();
        }
    }

    static class JdbcProfileFlowEvidenceRecorder implements ProfileFlowEvidenceRecorder {
        @Override
        public void recordMemberWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String memberId = String.valueOf(payload.get("memberId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO profile_flow_members(flow_id, member_id, action, status, visibility, bio, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, memberId, action, payload.get("status"), payload.get("visibility"), payload.get("bio"), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO profile_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, requestId, action, memberId, "SUCCESS", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO profile_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId, requestId, request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write profile member database evidence", exception);
            }
        }

        @Override
        public void recordGroupWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String requestId = request.getHeader("X-Request-Id");
            String groupId = String.valueOf(payload.get("id"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO profile_flow_groups(flow_id, group_id, action, name, archived, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, groupId, action, payload.get("name"), payload.get("archived"), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO profile_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, requestId, action, groupId, "SUCCESS", Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO profile_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                        flowId, requestId, request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write profile group database evidence", exception);
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

    record TestHttpResponse(int statusCode, String body) {
    }
}
