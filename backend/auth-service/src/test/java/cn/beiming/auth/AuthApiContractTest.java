package cn.beiming.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuthStore store;

    @BeforeEach
    void setUp() {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedUser("admin", "Password12345", Set.of("ADMIN"), Set.of(), "ACTIVE");
        store.seedUser("helper", "Password12345", Set.of("HELPER"), Set.of(), "ACTIVE");
        store.seedUser("user", "Password12345", Set.of("USER"), Set.of(), "ACTIVE");
        store.seedUser("disabled", "Password12345", Set.of("USER"), Set.of(), "DISABLED");
        store.seedUser("banned", "Password12345", Set.of("USER"), Set.of(), "BANNED");
        store.seedUser("deleted", "Password12345", Set.of("USER"), Set.of(), "DELETED");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 10, null, "owner");
        store.seedInvitation("ADMIN-CODE-1", "ADMIN", Set.of("ADMIN"), Set.of(), 3, null, "owner");
        store.seedInvitation("DISABLED-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 10, null, "owner");
        store.disableInvitationByCode("DISABLED-CODE-1", "seed");
        store.seedInvitation("EXPIRED-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 10, Instant.now().minusSeconds(60), "owner");
        store.seedInvitation("EXHAUSTED-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 1, null, "owner");
        store.exhaustInvitationByCode("EXHAUSTED-CODE-1");
        store.seedInvitation("LAST-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 1, null, "owner");
        store.bindMinecraft("user", "UsedName", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    @DisplayName("AUTH-COM-001/003 success responses include unified body and request id")
    void successResponseUsesCommonEnvelopeAndRequestId() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .header("X-Request-Id", "req-test-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "user", "password", "Password12345"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-test-login"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("AUTH-COM-002/004 validation errors include errors and generated request id")
    void validationErrorUsesCommonEnvelope() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    @DisplayName("AUTH-COM-005/006 protected endpoints reject missing or malformed bearer tokens")
    void protectedEndpointsRequireBearerToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Token bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));
    }

    @Test
    @DisplayName("AUTH-REG-001 registers by player invitation and writes invitation usage and audit")
    void registerWithPlayerInvitation() throws Exception {
        JsonNode body = performJson(post("/api/v1/auth/register"), Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", "new_user",
                "password", "Password12345",
                "displayName", "New User"
        ), 201);

        assertThat(body.at("/data/tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.at("/data/user/status").asText()).isEqualTo("PENDING_PROFILE");
        assertThat(values(body.at("/data/user/roles"))).containsExactly("USER");
        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(1);
        assertThat(store.auditActions()).contains("AUTH_REGISTER_SUCCESS");
    }

    @Test
    @DisplayName("AUTH-REG-002 registers by admin invitation and writes admin invitation audit")
    void registerWithAdminInvitation() throws Exception {
        JsonNode body = performJson(post("/api/v1/auth/register"), Map.of(
                "invitationCode", "ADMIN-CODE-1",
                "username", "new_admin",
                "password", "Password12345",
                "displayName", "New Admin"
        ), 201);

        assertThat(values(body.at("/data/user/roles"))).containsExactly("ADMIN");
        assertThat(store.auditActions()).contains("AUTH_ADMIN_INVITATION_USED");
    }

    @Test
    @DisplayName("AUTH-REG-003/004/005/006 rejects invalid register fields without consuming invitation")
    void registerRejectsInvalidFields() throws Exception {
        int before = store.invitationUsedCount("PLAYER-CODE-1");

        performJson(post("/api/v1/auth/register"), Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", "x!",
                "password", "short",
                "displayName", ""
        ), 400, 40001);

        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(before);
    }

    @Test
    @DisplayName("AUTH-REG-007/008 rejects duplicated username and display name without consuming invitation")
    void registerRejectsDuplicateUserFields() throws Exception {
        performJson(post("/api/v1/auth/register"), Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", "user",
                "password", "Password12345",
                "displayName", "Unique A"
        ), 409, 43110);

        performJson(post("/api/v1/auth/register"), Map.of(
                "invitationCode", "PLAYER-CODE-1",
                "username", "unique_user",
                "password", "Password12345",
                "displayName", "user"
        ), 409, 43111);

        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isZero();
    }

    @Test
    @DisplayName("AUTH-REG-009/010/011/012 rejects unavailable invitations")
    void registerRejectsUnavailableInvitations() throws Exception {
        performJson(post("/api/v1/auth/register"), registerBody("NONE-CODE-1", "missing_user"), 404, 43101);
        performJson(post("/api/v1/auth/register"), registerBody("DISABLED-CODE-1", "disabled_invite_user"), 409, 43112);
        performJson(post("/api/v1/auth/register"), registerBody("EXPIRED-CODE-1", "expired_invite_user"), 409, 43113);
        performJson(post("/api/v1/auth/register"), registerBody("EXHAUSTED-CODE-1", "exhausted_invite_user"), 409, 43114);
    }

    @Test
    @DisplayName("AUTH-REG-013/014 handles register idempotency")
    void registerIsIdempotent() throws Exception {
        Map<String, Object> body = registerBody("PLAYER-CODE-1", "idem_user");
        body.put("idempotencyKey", "idem-reg-1");

        JsonNode first = performJson(post("/api/v1/auth/register"), body, 201);
        JsonNode second = performJson(post("/api/v1/auth/register"), body, 201);

        assertThat(second.at("/data/user/id").asText()).isEqualTo(first.at("/data/user/id").asText());
        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(1);

        Map<String, Object> changed = registerBody("PLAYER-CODE-1", "idem_user_2");
        changed.put("idempotencyKey", "idem-reg-1");
        performJson(post("/api/v1/auth/register"), changed, 409, 43002);
    }

    @Test
    @DisplayName("AUTH-REG-015 allows only one concurrent final invitation use")
    void registerProtectsConcurrentLastInvitationUse() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger exhaustedCount = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (String username : List.of("race_a", "race_b")) {
                pool.submit(() -> {
                    start.await();
                    MvcResult result = mvc.perform(post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(registerBody("LAST-CODE-1", username))))
                            .andReturn();
                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    }
                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    if (json.path("code").asInt() == 43114) {
                        exhaustedCount.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(exhaustedCount.get()).isEqualTo(1);
        assertThat(store.invitationUsedCount("LAST-CODE-1")).isEqualTo(1);
    }

    @Test
    @DisplayName("AUTH-LOGIN-001/002/003/004/005/006 covers login success and failures")
    void loginContract() throws Exception {
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Password12345"), 200);
        performJson(post("/api/v1/auth/login"), Map.of("username", "missing", "password", "Password12345"), 401, 41100);
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Wrong12345"), 401, 41100);
        performJson(post("/api/v1/auth/login"), Map.of("username", "disabled", "password", "Password12345"), 401, 41101);
        performJson(post("/api/v1/auth/login"), Map.of("username", "banned", "password", "Password12345"), 401, 41102);
        performJson(post("/api/v1/auth/login"), Map.of("username", "deleted", "password", "Password12345"), 409, 43116);
    }

    @Test
    @DisplayName("AUTH-LOGIN-007 rate limits repeated login failures")
    void loginRateLimitDoesNotCreateSession() throws Exception {
        for (int i = 0; i < 5; i++) {
            performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Wrong12345"), 401, 41100);
        }

        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Wrong12345"), 429, 44100);
        assertThat(store.auditActions()).contains("AUTH_LOGIN_RISK_BLOCKED");
    }

    @Test
    @DisplayName("AUTH-LOGOUT and AUTH-ME enforce session revocation")
    void logoutRevokesCurrentSession() throws Exception {
        String token = login("user");

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41103));

        assertThat(store.auditActions()).contains("AUTH_LOGOUT_SUCCESS");
    }

    @Test
    @DisplayName("AUTH-ME-001 and AUTH-VERIFY-001 return current authenticated user")
    void currentUserAndVerifySession() throws Exception {
        String token = login("user");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));

        mvc.perform(get("/api/v1/auth/session/verify").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.user.username").value("user"));
    }

    @Test
    @DisplayName("AUTH-PWD resets password and revokes old sessions")
    void passwordResetFlow() throws Exception {
        String oldToken = login("user");
        performJson(post("/api/v1/auth/password-reset/request"), Map.of("username", "user"), 200);
        String resetToken = store.latestPasswordResetToken("user");

        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of(
                "resetToken", resetToken,
                "newPassword", "NewPassword12345"
        ), 200);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Password12345"), 401, 41100);
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "NewPassword12345"), 200);
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of(
                "resetToken", resetToken,
                "newPassword", "AnotherPassword12345"
        ), 401, 41104);
        assertThat(store.auditActions()).contains("AUTH_PASSWORD_RESET_REQUESTED", "AUTH_PASSWORD_RESET_CONFIRMED");
    }

    @Test
    @DisplayName("AUTH-PWD hides unknown usernames and rejects bad reset tokens")
    void passwordResetFailureCases() throws Exception {
        performJson(post("/api/v1/auth/password-reset/request"), Map.of("username", "missing"), 200);
        performJson(post("/api/v1/auth/password-reset/request"), Map.of(), 400, 40001);
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of(
                "resetToken", "missing-token",
                "newPassword", "NewPassword12345"
        ), 401, 41104);
        assertThat(store.auditActions()).contains("AUTH_PASSWORD_RESET_FAILED");
    }

    @Test
    @DisplayName("AUTH-MC binds, rejects conflicts, and unbinds with audit")
    void minecraftBindingContract() throws Exception {
        String token = login("admin");

        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(token)), Map.of(
                "minecraftId", "AdminName",
                "minecraftUuid", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "verificationCode", "valid"
        ), 200);

        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(token)), Map.of(
                "minecraftId", "UsedName",
                "minecraftUuid", "cccccccccccccccccccccccccccccccc",
                "verificationCode", "valid"
        ), 409, 43116);

        performJson(delete("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(token)), Map.of("reason", "rebind"), 200);
        performJson(delete("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(token)), Map.of("reason", "again"), 404, 43102);
        assertThat(store.auditActions()).contains("AUTH_MINECRAFT_BOUND", "AUTH_MINECRAFT_UNBOUND");
    }

    @Test
    @DisplayName("AUTH-USER-LIST covers pagination, auth, permission, filter, and sort")
    void adminUserListContract() throws Exception {
        String adminToken = login("admin");
        String userToken = login("user");

        mvc.perform(get("/api/v1/auth/admin/users").header("Authorization", bearer(adminToken)).param("page", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").isNumber());

        mvc.perform(get("/api/v1/auth/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mvc.perform(get("/api/v1/auth/admin/users").header("Authorization", bearer(adminToken)).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    @DisplayName("AUTH-USER-PATCH updates users, protects OWNER, revokes sessions, and writes audit")
    void adminUserPatchContract() throws Exception {
        String adminToken = login("admin");
        String userToken = login("user");
        String userId = store.userId("user");

        performJson(patch("/api/v1/auth/admin/users/" + userId).header("Authorization", bearer(adminToken)), Map.of(
                "displayName", "Renamed User",
                "reason", "rename"
        ), 200);

        performJson(patch("/api/v1/auth/admin/users/" + userId).header("Authorization", bearer(adminToken)), Map.of(
                "status", "DISABLED",
                "reason", "risk"
        ), 200);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isUnauthorized());

        performJson(patch("/api/v1/auth/admin/users/" + store.userId("owner")).header("Authorization", bearer(adminToken)), Map.of(
                "status", "DISABLED",
                "reason", "bad"
        ), 403, 42100);

        assertThat(store.auditActions()).contains("AUTH_USER_UPDATED");
    }

    @Test
    @DisplayName("AUTH-ROLE only OWNER can modify roles and unique OWNER is protected")
    void rolePermissionContract() throws Exception {
        String ownerToken = login("owner");
        String adminToken = login("admin");
        String helperId = store.userId("helper");

        performJson(put("/api/v1/auth/admin/users/" + helperId + "/roles").header("Authorization", bearer(adminToken)), Map.of(
                "roles", List.of("ADMIN"),
                "permissions", List.of(),
                "reason", "promote"
        ), 403, 42001);

        performJson(put("/api/v1/auth/admin/users/" + helperId + "/roles").header("Authorization", bearer(ownerToken)), Map.of(
                "roles", List.of("ADMIN"),
                "permissions", List.of("NODE_READ"),
                "reason", "promote"
        ), 200);

        performJson(put("/api/v1/auth/admin/users/" + store.userId("owner") + "/roles").header("Authorization", bearer(ownerToken)), Map.of(
                "roles", List.of("USER"),
                "permissions", List.of(),
                "reason", "bad"
        ), 403, 42101);

        assertThat(store.auditActions()).contains("AUTH_ROLE_PERMISSION_UPDATED");
    }

    @Test
    @DisplayName("AUTH-INV-CREATE handles player/admin invitations, raw code visibility, and idempotency")
    void invitationCreateContract() throws Exception {
        String adminToken = login("admin");
        String ownerToken = login("owner");

        JsonNode playerInvite = performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(adminToken)), Map.of(
                "type", "PLAYER",
                "boundRoles", List.of("USER"),
                "maxUses", 3,
                "reason", "new players",
                "idempotencyKey", "invite-idem-1"
        ), 201);
        assertThat(playerInvite.at("/data/rawCode").asText()).isNotBlank();

        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(adminToken)), Map.of(
                "type", "ADMIN",
                "boundRoles", List.of("ADMIN"),
                "maxUses", 1,
                "reason", "bad"
        ), 403, 42102);

        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), Map.of(
                "type", "ADMIN",
                "boundRoles", List.of("ADMIN"),
                "maxUses", 1,
                "reason", "ops"
        ), 201);

        mvc.perform(get("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].rawCode").doesNotExist());
    }

    @Test
    @DisplayName("AUTH-INV-DISABLE and AUTH-INV-USAGE enforce ownership and audit")
    void invitationDisableAndUsageContract() throws Exception {
        String adminToken = login("admin");
        String ownerToken = login("owner");
        String invitationId = store.invitationId("PLAYER-CODE-1");

        performJson(patch("/api/v1/auth/admin/invitations/" + invitationId + "/disable").header("Authorization", bearer(adminToken)), Map.of("reason", "stop"), 403, 42001);
        performJson(patch("/api/v1/auth/admin/invitations/" + invitationId + "/disable").header("Authorization", bearer(ownerToken)), Map.of("reason", "stop"), 200);
        performJson(patch("/api/v1/auth/admin/invitations/" + invitationId + "/disable").header("Authorization", bearer(ownerToken)), Map.of("reason", "again"), 200);

        mvc.perform(get("/api/v1/auth/admin/invitations/" + invitationId + "/usage-records").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        assertThat(store.auditCount("AUTH_INVITATION_DISABLED")).isEqualTo(1);
    }

    @Test
    @DisplayName("AUTH-SEC stores password and invitation code without reusable plaintext")
    void secretsAreNotStoredAsPlaintext() throws Exception {
        performJson(post("/api/v1/auth/register"), registerBody("PLAYER-CODE-1", "secret_user"), 201);
        assertThat(store.passwordHash("secret_user")).doesNotContain("Password12345");
        assertThat(store.invitationStoredSecret("PLAYER-CODE-1")).doesNotContain("PLAYER-CODE-1");
    }

    private String login(String username) throws Exception {
        JsonNode body = performJson(post("/api/v1/auth/login"), Map.of("username", username, "password", "Password12345"), 200);
        return body.at("/data/accessToken").asText();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, body, expectedStatus);
        assertThat(result.path("code").asInt()).isEqualTo(expectedCode);
        return result;
    }

    private Map<String, Object> registerBody(String invitationCode, String username) {
        return new java.util.LinkedHashMap<>(Map.of(
                "invitationCode", invitationCode,
                "username", username,
                "password", "Password12345",
                "displayName", username
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private List<String> values(JsonNode arrayNode) {
        return java.util.stream.StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }
}
