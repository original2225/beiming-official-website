package cn.beiming.profile;

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
class ProfilePostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "profile-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_profile_flow")
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
    ProfileStore store;

    @Autowired
    ProfileAuthContextProvider auth;

    @BeforeEach
    void setUp() {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
    }

    @Test
    void selfProfilePatchPersistsMemberAuditAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("self");
        JsonNode response = exchange(HttpMethod.PATCH, "/api/v1/profile/me", "active-member-token", requestId, Map.of(
                "bio", "PostgreSQL self bio",
                "visibility", "PRIVATE",
                "reason", "self postgres evidence"
        ), 200);

        String memberId = response.at("/data/memberId").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT bio FROM profile_members WHERE member_id = ?", memberId, "PostgreSQL self bio");
            assertSingleValue(connection, "SELECT visibility FROM profile_members WHERE member_id = ?", memberId, "PRIVATE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_SELF_UPDATED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/profile/me'", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile self update wrote profile_members/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void groupCreatePersistsGroupAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("group-create");
        String groupName = uniqueName("PgGroup");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId, Map.of(
                "name", groupName,
                "description", "PostgreSQL group",
                "color", "#123ABC",
                "sortOrder", 12,
                "reason", "group postgres evidence",
                "idempotencyKey", "idem-" + groupName
        ), 201);

        String groupId = response.at("/data/id").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT name FROM profile_member_groups WHERE group_id = ?", groupId, groupName);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_GROUP_CREATED' AND target_id = ?", requestId, groupId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'profile.group.create' AND idempotency_key = ?", "idem-" + groupName, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/profile/admin/groups'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL profile group create wrote profile_member_groups/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void groupCreateIdempotencyReplaysPersistedResultInPostgreSql() throws Exception {
        String groupName = uniqueName("PgReplay");
        Map<String, Object> body = Map.of(
                "name", groupName,
                "description", "Replay group",
                "color", "#654321",
                "sortOrder", 13,
                "reason", "group replay evidence",
                "idempotencyKey", "idem-" + groupName
        );
        JsonNode first = exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId("group-replay-first"), body, 201);
        JsonNode second = exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId("group-replay-second"), body, 201);

        assertThat(second.at("/data/id").asText()).isEqualTo(first.at("/data/id").asText());
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM profile_member_groups WHERE name = ?", groupName, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'profile.group.create' AND idempotency_key = ?", "idem-" + groupName, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL profile group create idempotency replay kept one profile_member_groups row and one app_idempotency_records row.");
    }

    @Test
    void groupCreateIdempotencyRejectsSameKeyWithDifferentFingerprint() throws Exception {
        String groupName = uniqueName("PgConflict");
        String idempotencyKey = "idem-" + groupName;
        exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId("group-conflict-first"), Map.of(
                "name", groupName,
                "reason", "first",
                "idempotencyKey", idempotencyKey
        ), 201);

        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId("group-conflict-second"), Map.of(
                "name", groupName + "x",
                "reason", "second",
                "idempotencyKey", idempotencyKey
        ), 409);

        assertThat(conflict.at("/code").asInt()).isEqualTo(43002);
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'profile.group.create' AND idempotency_key = ?", idempotencyKey, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL profile group create idempotency conflict preserved the original app_idempotency_records row and returned 409.");
    }

    @Test
    void memberActivatePersistsMemberAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("activate");
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/profile/admin/members/activate", "admin-token", requestId, Map.of(
                "userId", "target_user",
                "visibility", "PUBLIC",
                "bio", "Activated in PostgreSQL",
                "reason", "activate postgres evidence",
                "idempotencyKey", "idem-activate-target"
        ), 201);

        String memberId = response.at("/data/memberId").asText();
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT user_id FROM profile_members WHERE member_id = ?", memberId, "target_user");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_MEMBER_ACTIVATED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'profile.member.activate' AND idempotency_key = 'idem-activate-target'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/profile/admin/members/activate'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL profile member activate wrote profile_members/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void memberPatchPersistsMemberAuditAndRequestLogInPostgreSql() throws Exception {
        String memberId = store.memberIdByUserId("active_member");
        String requestId = requestId("member-patch");
        exchange(HttpMethod.PATCH, "/api/v1/profile/admin/members/" + memberId, "admin-token", requestId, Map.of(
                "bio", "PostgreSQL admin bio",
                "adminNote", "PostgreSQL note",
                "reason", "member patch postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT bio FROM profile_members WHERE member_id = ?", memberId, "PostgreSQL admin bio");
            assertSingleValue(connection, "SELECT admin_note FROM profile_members WHERE member_id = ?", memberId, "PostgreSQL note");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_MEMBER_UPDATED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile member patch wrote profile_members/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void memberStatusPatchPersistsStatusAuditAndRequestLogInPostgreSql() throws Exception {
        String memberId = store.memberIdByUserId("active_member");
        String requestId = requestId("status");
        exchange(HttpMethod.PATCH, "/api/v1/profile/admin/members/" + memberId + "/status", "admin-token", requestId, Map.of(
                "status", "INACTIVE",
                "reason", "status postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM profile_members WHERE member_id = ?", memberId, "INACTIVE");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_MEMBER_STATUS_CHANGED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile status patch wrote profile_members/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void groupPatchPersistsGroupAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createTemporaryGroup("PgPatch");
        String groupId = created.at("/data/id").asText();
        String requestId = requestId("group-patch");
        exchange(HttpMethod.PATCH, "/api/v1/profile/admin/groups/" + groupId, "admin-token", requestId, Map.of(
                "name", "PgPatched" + suffix(),
                "description", "patched",
                "reason", "group patch postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT description FROM profile_member_groups WHERE group_id = ?", groupId, "patched");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_GROUP_UPDATED' AND target_id = ?", requestId, groupId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile group patch wrote profile_member_groups/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void groupArchivePersistsGroupAuditAndRequestLogInPostgreSql() throws Exception {
        JsonNode created = createTemporaryGroup("PgArchive");
        String groupId = created.at("/data/id").asText();
        String requestId = requestId("group-archive");
        exchange(HttpMethod.PATCH, "/api/v1/profile/admin/groups/" + groupId + "/archive", "admin-token", requestId, Map.of(
                "reason", "group archive postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT archived FROM profile_member_groups WHERE group_id = ?", groupId, true);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_GROUP_ARCHIVED' AND target_id = ?", requestId, groupId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile group archive wrote profile_member_groups/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void milestoneReplacePersistsMilestonesAuditAndRequestLogInPostgreSql() throws Exception {
        String memberId = store.memberIdByUserId("member_with_milestones");
        String requestId = requestId("milestones");
        exchange(HttpMethod.PUT, "/api/v1/profile/admin/members/" + memberId + "/milestones", "admin-token", requestId, Map.of(
                "items", List.of(Map.of(
                        "type", "PROJECT",
                        "title", "PostgreSQL Build",
                        "description", "verified",
                        "happenedAt", "2026-05-21T00:00:00Z",
                        "publicVisible", true,
                        "sortOrder", 1
                )),
                "reason", "milestones postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT title FROM profile_member_milestones WHERE member_id = ?", memberId, "PostgreSQL Build");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_MEMBER_MILESTONES_REPLACED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile milestone replace wrote profile_member_milestones/app_audit_logs/app_request_logs and returned 200.");
    }

    @Test
    void workSnapshotReplacePersistsWorksAuditAndRequestLogInPostgreSql() throws Exception {
        String memberId = store.memberIdByUserId("member_with_work_snapshots");
        String requestId = requestId("works");
        exchange(HttpMethod.PUT, "/api/v1/profile/admin/members/" + memberId + "/work-snapshots", "admin-token", requestId, Map.of(
                "items", List.of(Map.of(
                        "type", "BUILD",
                        "title", "PostgreSQL Castle",
                        "summary", "verified",
                        "coverUrl", "https://example.com/castle.png",
                        "sourceModule", "content",
                        "sourceId", "content-pg",
                        "publicVisible", true,
                        "sortOrder", 1
                )),
                "reason", "works postgres evidence"
        ), 200);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT title FROM profile_member_work_snapshots WHERE member_id = ?", memberId, "PostgreSQL Castle");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'PROFILE_MEMBER_WORKS_REPLACED' AND target_id = ?", requestId, memberId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ?", requestId, 200);
        }
        System.out.println("SQL evidence: PostgreSQL profile work snapshot replace wrote profile_member_work_snapshots/app_audit_logs/app_request_logs and returned 200.");
    }

    private JsonNode createTemporaryGroup(String prefix) throws Exception {
        return exchange(HttpMethod.POST, "/api/v1/profile/admin/groups", "admin-token", requestId(prefix.toLowerCase()), Map.of(
                "name", uniqueName(prefix),
                "reason", prefix + " create"
        ), 201);
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

    private String requestId(String name) {
        return "req-" + FLOW_ID + "-" + name;
    }

    private String uniqueName(String prefix) {
        return prefix + suffix();
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
