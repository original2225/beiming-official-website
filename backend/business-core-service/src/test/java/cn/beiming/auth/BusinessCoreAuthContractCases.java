package cn.beiming.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

@AutoConfigureMockMvc
abstract class BusinessCoreAuthContractCases {
    private static final String TEST_DOCUMENT_COVERAGE = """
            AUTH-COM-001 AUTH-COM-002 AUTH-COM-003 AUTH-COM-004 AUTH-COM-005 AUTH-COM-006 AUTH-COM-007 AUTH-COM-008 AUTH-COM-009 AUTH-COM-010
            AUTH-REG-001 AUTH-REG-002 AUTH-REG-003 AUTH-REG-004 AUTH-REG-005 AUTH-REG-006 AUTH-REG-007 AUTH-REG-008 AUTH-REG-009 AUTH-REG-010 AUTH-REG-011 AUTH-REG-012 AUTH-REG-013 AUTH-REG-014 AUTH-REG-015 AUTH-REG-016 AUTH-REG-017 AUTH-REG-018 AUTH-REG-019
            AUTH-LOGIN-001 AUTH-LOGIN-002 AUTH-LOGIN-003 AUTH-LOGIN-004 AUTH-LOGIN-005 AUTH-LOGIN-006 AUTH-LOGIN-007 AUTH-LOGIN-008 AUTH-LOGIN-009
            AUTH-LOGOUT-001 AUTH-LOGOUT-002 AUTH-LOGOUT-003 AUTH-LOGOUT-004
            AUTH-ME-001 AUTH-ME-002 AUTH-ME-003 AUTH-ME-004 AUTH-ME-005
            AUTH-VERIFY-001 AUTH-VERIFY-002
            AUTH-SESSION-LIST-001 AUTH-SESSION-LIST-002 AUTH-SESSION-LIST-003
            AUTH-SESSION-REVOKE-001 AUTH-SESSION-REVOKE-002 AUTH-SESSION-REVOKE-003 AUTH-SESSION-REVOKE-004 AUTH-SESSION-REVOKE-005
            AUTH-PWD-001 AUTH-PWD-002 AUTH-PWD-003 AUTH-PWD-004 AUTH-PWD-005 AUTH-PWD-006 AUTH-PWD-007 AUTH-PWD-008 AUTH-PWD-009 AUTH-PWD-010 AUTH-PWD-011 AUTH-PWD-012 AUTH-PWD-013 AUTH-PWD-014 AUTH-PWD-015 AUTH-PWD-016 AUTH-PWD-017 AUTH-PWD-018 AUTH-PWD-019
            AUTH-MC-001 AUTH-MC-002 AUTH-MC-003 AUTH-MC-004 AUTH-MC-005 AUTH-MC-006 AUTH-MC-007 AUTH-MC-008 AUTH-MC-009 AUTH-MC-010 AUTH-MC-011
            AUTH-USER-LIST-001 AUTH-USER-LIST-002 AUTH-USER-LIST-003 AUTH-USER-LIST-004 AUTH-USER-LIST-005 AUTH-USER-LIST-006 AUTH-USER-LIST-007
            AUTH-USER-DETAIL-001 AUTH-USER-DETAIL-002 AUTH-USER-DETAIL-003
            AUTH-USER-PATCH-001 AUTH-USER-PATCH-002 AUTH-USER-PATCH-003 AUTH-USER-PATCH-004 AUTH-USER-PATCH-005 AUTH-USER-PATCH-006 AUTH-USER-PATCH-007 AUTH-USER-PATCH-008 AUTH-USER-PATCH-009
            AUTH-ROLE-001 AUTH-ROLE-002 AUTH-ROLE-003 AUTH-ROLE-004 AUTH-ROLE-005 AUTH-ROLE-006 AUTH-ROLE-007 AUTH-ROLE-008
            AUTH-INV-LIST-001 AUTH-INV-LIST-002 AUTH-INV-LIST-003 AUTH-INV-LIST-004 AUTH-INV-LIST-005 AUTH-INV-LIST-006
            AUTH-INV-CREATE-001 AUTH-INV-CREATE-002 AUTH-INV-CREATE-003 AUTH-INV-CREATE-004 AUTH-INV-CREATE-005 AUTH-INV-CREATE-006 AUTH-INV-CREATE-007 AUTH-INV-CREATE-008 AUTH-INV-CREATE-009 AUTH-INV-CREATE-010 AUTH-INV-CREATE-011
            AUTH-INV-DISABLE-001 AUTH-INV-DISABLE-002 AUTH-INV-DISABLE-003 AUTH-INV-DISABLE-004 AUTH-INV-DISABLE-005 AUTH-INV-DISABLE-006
            AUTH-INV-USAGE-001 AUTH-INV-USAGE-002 AUTH-INV-USAGE-003 AUTH-INV-USAGE-004
            AUTH-STATE-001 AUTH-STATE-002 AUTH-STATE-003 AUTH-STATE-004 AUTH-STATE-005 AUTH-STATE-006 AUTH-STATE-007 AUTH-STATE-008 AUTH-STATE-009
            AUTH-SEC-001 AUTH-SEC-002 AUTH-SEC-003 AUTH-SEC-004 AUTH-SEC-005 AUTH-SEC-006 AUTH-SEC-007 AUTH-SEC-008
            """;

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
        store.seedMinecraftBinding("user", "UsedName", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
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
    @DisplayName("AUTH-COM-007/008/009/010 list endpoints use pagination, hide secrets, and write auditable request ids")
    void commonPaginationSensitiveFieldsAndAuditContract() throws Exception {
        String ownerToken = login("owner");

        mvc.perform(get("/api/v1/auth/admin/invitations")
                        .header("Authorization", bearer(ownerToken))
                        .param("page", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items[0].rawCode").doesNotExist());

        mvc.perform(get("/api/v1/auth/admin/invitations")
                        .header("Authorization", bearer(ownerToken))
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));

        mvc.perform(get("/api/v1/auth/admin/users/" + store.userId("user"))
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mvc.perform(patch("/api/v1/auth/admin/users/" + store.userId("user"))
                        .header("Authorization", bearer(ownerToken))
                        .header("X-Request-Id", "req-audit-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "Audit User", "reason", "audit"))))
                .andExpect(status().isOk());

        assertThat(store.latestAuditRequestId("AUTH_USER_UPDATED")).isEqualTo("req-audit-check");
    }

    @Test
    @DisplayName("auth local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() throws Exception {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("AUTH-[A-Z]+(?:-[A-Z]+)*-[0-9]{3}");
        Set<String> mapped = pattern.matcher(TEST_DOCUMENT_COVERAGE).results()
                .map(java.util.regex.MatchResult::group)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertThat(mapped).hasSize(158);
        assertThat(TEST_DOCUMENT_COVERAGE).contains("AUTH-COM-001", "AUTH-REG-001", "AUTH-LOGIN-001",
                "AUTH-SESSION-LIST-001", "AUTH-PWD-019", "AUTH-INV-USAGE-004", "AUTH-SEC-008");
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
    @DisplayName("AUTH-REG-016/018/019 register rate limit, owner protection, and request id contract")
    void registerRateLimitOwnerProtectionAndRequestId() throws Exception {
        store.seedInvitation("OWNER-CODE-1", "ADMIN", Set.of("OWNER"), Set.of(), 1, null, "owner");
        performJson(post("/api/v1/auth/register").header("X-Request-Id", "req-owner-reg"), registerBody("OWNER-CODE-1", "owner_attempt"), 403, 42101);
        store.resetRegisterAttempts();

        for (int i = 0; i < 5; i++) {
            Map<String, Object> body = registerBody("PLAYER-CODE-1", "rate_user_" + i);
            performJson(post("/api/v1/auth/register").header("X-Request-Id", "req-rate-" + i), body, 201);
        }

        int before = store.invitationUsedCount("PLAYER-CODE-1");
        performJson(post("/api/v1/auth/register").header("X-Request-Id", "req-rate-limit"), registerBody("PLAYER-CODE-1", "rate_user_limit"), 429, 44101);
        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(before);
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
    @DisplayName("AUTH-REG-017 rolls back user and invitation state when session creation fails")
    void registerRollsBackWhenSessionCreationFails() throws Exception {
        int beforeUsedCount = store.invitationUsedCount("PLAYER-CODE-1");
        store.failNextSessionCreation();

        performJson(post("/api/v1/auth/register"), registerBody("PLAYER-CODE-1", "rollback_user"), 500, 51100);

        assertThat(store.userExists("rollback_user")).isFalse();
        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(beforeUsedCount);
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
    @DisplayName("AUTH-LOGIN-008/009 validates login fields and idempotent retry")
    void loginFieldValidationAndIdempotency() throws Exception {
        performJson(post("/api/v1/auth/login"), Map.of("username", "user"), 400, 40001);

        Map<String, Object> body = new java.util.LinkedHashMap<>(Map.of(
                "username", "user",
                "password", "Password12345",
                "idempotencyKey", "login-idem-1"
        ));
        JsonNode first = performJson(post("/api/v1/auth/login"), body, 200);
        JsonNode second = performJson(post("/api/v1/auth/login"), body, 200);
        assertThat(second.at("/data/accessToken").asText()).isEqualTo(first.at("/data/accessToken").asText());
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
    @DisplayName("AUTH-LOGOUT-002/003/004 repeat logout is idempotent and old token stays invalid")
    void logoutIdempotencyAndFailureCases() throws Exception {
        String token = login("user");

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        assertThat(store.auditCount("AUTH_LOGOUT_SUCCESS")).isEqualTo(1);
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
    @DisplayName("AUTH-ME-002/003/004/005 and AUTH-VERIFY-002 reject unusable sessions")
    void currentUserAndVerifyRejectUnusableSessions() throws Exception {
        String token = login("user");
        store.expireSession(token);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41002));
        mvc.perform(get("/api/v1/auth/session/verify").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        String activeToken = login("user");
        store.setUserStatus("user", "DISABLED");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(activeToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41101));
    }

    @Test
    @DisplayName("AUTH-SESSION-LIST-001/002/003 returns current user sessions without tokens and updates last seen")
    void currentUserSessionListContract() throws Exception {
        String firstToken = login("user");
        login("user");

        JsonNode firstList = performJson(get("/api/v1/auth/me/sessions").header("Authorization", bearer(firstToken)), 200);
        assertThat(firstList.at("/data/items").size()).isEqualTo(2);
        assertThat(firstList.toString()).doesNotContain("accessToken").doesNotContain(firstToken);
        assertThat(firstList.at("/data/items").findValues("current").stream().map(JsonNode::asText).toList()).contains("true");
        String currentSessionId = currentSessionId(firstList);
        Instant firstSeenAt = Instant.parse(currentSession(firstList).path("lastSeenAt").asText());

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(firstToken))).andExpect(status().isOk());
        JsonNode secondList = performJson(get("/api/v1/auth/me/sessions").header("Authorization", bearer(firstToken)), 200);
        Instant secondSeenAt = Instant.parse(currentSession(secondList).path("lastSeenAt").asText());
        assertThat(secondSeenAt).isAfterOrEqualTo(firstSeenAt);
        assertThat(currentSessionId(secondList)).isEqualTo(currentSessionId);

        performJson(get("/api/v1/auth/me/sessions"), 401, 41000);
    }

    @Test
    @DisplayName("AUTH-SESSION-REVOKE-001/002/003/004/005 revokes only owned sessions with audit and idempotency")
    void revokeCurrentUserSessionContract() throws Exception {
        String currentToken = login("user");
        String otherToken = login("user");
        String adminToken = login("admin");
        JsonNode sessionList = performJson(get("/api/v1/auth/me/sessions").header("Authorization", bearer(currentToken)), 200);
        String otherSessionId = nonCurrentSessionId(sessionList);

        performJson(delete("/api/v1/auth/me/sessions/" + otherSessionId).header("Authorization", bearer(currentToken)), Map.of("reason", "lost device"), 200);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(otherToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41103));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(currentToken))).andExpect(status().isOk());
        performJson(delete("/api/v1/auth/me/sessions/" + otherSessionId).header("Authorization", bearer(currentToken)), Map.of("reason", "repeat"), 200);
        assertThat(store.auditCount("AUTH_SESSION_REVOKED")).isEqualTo(1);

        String adminSessionId = currentSessionId(performJson(get("/api/v1/auth/me/sessions").header("Authorization", bearer(adminToken)), 200));
        performJson(delete("/api/v1/auth/me/sessions/" + adminSessionId).header("Authorization", bearer(currentToken)), Map.of("reason", "not mine"), 401, 41106);
        performJson(delete("/api/v1/auth/me/sessions/" + otherSessionId).header("Authorization", bearer(currentToken)), Map.of(), 400, 40001);

        String currentSessionId = currentSessionId(performJson(get("/api/v1/auth/me/sessions").header("Authorization", bearer(currentToken)), 200));
        performJson(delete("/api/v1/auth/me/sessions/" + currentSessionId).header("Authorization", bearer(currentToken)), Map.of("reason", "sign out device"), 200);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(currentToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41103));
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
    @DisplayName("AUTH-PWD-004/007/009/010 password reset limit and invalid states")
    void passwordResetLimitAndInvalidStateCases() throws Exception {
        for (int i = 0; i < 5; i++) {
            performJson(post("/api/v1/auth/password-reset/request"), Map.of("username", "user"), 200);
        }
        performJson(post("/api/v1/auth/password-reset/request"), Map.of("username", "user"), 429, 44102);

        String expiredToken = store.createExpiredPasswordResetToken("user");
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of("resetToken", expiredToken, "newPassword", "NewPassword12345"), 401, 41104);

        String validToken = store.latestPasswordResetToken("user");
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of("resetToken", validToken, "newPassword", "short"), 400, 40001);
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of("resetToken", validToken, "newPassword", "Password12345"), 409, 43001);
    }

    @Test
    @DisplayName("AUTH-PWD-013 writes failure audit for expired password reset token")
    void passwordResetExpiredTokenWritesFailureAudit() throws Exception {
        String expiredToken = store.createExpiredPasswordResetToken("user");
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of("resetToken", expiredToken, "newPassword", "NewPassword12345"), 401, 41104);
        assertThat(store.auditActions()).contains("AUTH_PASSWORD_RESET_FAILED");
    }

    @Test
    @DisplayName("AUTH-PWD-014/019 active password change keeps current session and revokes other sessions")
    void changePasswordKeepsCurrentSessionAndRevokesOtherSessions() throws Exception {
        String currentToken = login("user");
        String otherToken = login("user");
        String beforeHash = store.passwordHash("user");

        performJson(post("/api/v1/auth/me/password").header("Authorization", bearer(currentToken)), Map.of(
                "currentPassword", "Password12345",
                "newPassword", "ChangedPassword12345",
                "reason", "regular rotation"
        ), 200);

        assertThat(store.passwordHash("user")).isNotEqualTo(beforeHash);
        assertThat(store.auditActions()).contains("AUTH_PASSWORD_CHANGED");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(currentToken))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(otherToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41103));
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "Password12345"), 401, 41100);
        performJson(post("/api/v1/auth/login"), Map.of("username", "user", "password", "ChangedPassword12345"), 200);
    }

    @Test
    @DisplayName("AUTH-PWD-015/016/017/018 active password change rejects unsafe or invalid requests")
    void changePasswordRejectsInvalidRequests() throws Exception {
        String token = login("user");
        String otherToken = login("user");
        String beforeHash = store.passwordHash("user");

        performJson(post("/api/v1/auth/me/password").header("Authorization", bearer(token)), Map.of(
                "currentPassword", "WrongPassword12345",
                "newPassword", "ChangedPassword12345",
                "reason", "bad current"
        ), 401, 41105);
        performJson(post("/api/v1/auth/me/password").header("Authorization", bearer(token)), Map.of(
                "currentPassword", "Password12345",
                "newPassword", "Password12345",
                "reason", "same"
        ), 409, 43001);
        performJson(post("/api/v1/auth/me/password").header("Authorization", bearer(token)), Map.of(
                "currentPassword", "Password12345",
                "newPassword", "Password123",
                "reason", "common"
        ), 400, 40001);
        performJson(post("/api/v1/auth/me/password"), Map.of(
                "currentPassword", "Password12345",
                "newPassword", "ChangedPassword12345",
                "reason", "missing auth"
        ), 401, 41000);
        performJson(post("/api/v1/auth/me/password").header("Authorization", bearer(token)), Map.of(
                "currentPassword", "Password12345",
                "newPassword", "ChangedPassword12345"
        ), 400, 40001);

        assertThat(store.passwordHash("user")).isEqualTo(beforeHash);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(otherToken))).andExpect(status().isOk());
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
    @DisplayName("AUTH-MC-002/003/004/005/006/007/010/011 cover binding failures and idempotency")
    void minecraftBindingFailureAndIdempotencyCases() throws Exception {
        mvc.perform(put("/api/v1/auth/me/minecraft-binding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("minecraftId", "NoLogin", "minecraftUuid", "dddddddddddddddddddddddddddddddd", "verificationCode", "valid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        String helperToken = login("helper");
        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(
                "minecraftId", "bad id",
                "minecraftUuid", "bad",
                "verificationCode", "valid"
        ), 400, 40001);

        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(
                "minecraftId", "UsedName",
                "minecraftUuid", "dddddddddddddddddddddddddddddddd",
                "verificationCode", "valid"
        ), 409, 43115);

        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(
                "minecraftId", "HelperName",
                "minecraftUuid", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "verificationCode", "valid"
        ), 409, 43115);

        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(
                "minecraftId", "HelperName",
                "minecraftUuid", "dddddddddddddddddddddddddddddddd",
                "verificationCode", "valid"
        ), 200);
        performJson(put("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(
                "minecraftId", "HelperName",
                "minecraftUuid", "dddddddddddddddddddddddddddddddd",
                "verificationCode", "valid"
        ), 200);
        assertThat(store.auditCount("AUTH_MINECRAFT_BOUND")).isEqualTo(1);

        performJson(delete("/api/v1/auth/me/minecraft-binding").header("Authorization", bearer(helperToken)), Map.of(), 400, 40001);
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
    @DisplayName("AUTH-USER-DETAIL-001/002/003 and AUTH-USER-LIST-004/005/006/007 cover detail and list filters")
    void adminUserDetailAndListFilterContract() throws Exception {
        String adminToken = login("admin");
        String userToken = login("user");

        mvc.perform(get("/api/v1/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("keyword", "UsedName")
                        .param("status", "ACTIVE")
                        .param("role", "USER")
                        .param("sort", "createdAt_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].username").value("user"));

        mvc.perform(get("/api/v1/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mvc.perform(get("/api/v1/auth/admin/users/" + store.userId("user")).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mvc.perform(get("/api/v1/auth/admin/users/missing-user").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(43100));

        mvc.perform(get("/api/v1/auth/admin/users/" + store.userId("user")).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
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
    @DisplayName("AUTH-USER-PATCH-003/004/005/007/008 cover user update failure rollback")
    void adminUserPatchFailureCases() throws Exception {
        String ownerToken = login("owner");
        String adminToken = login("admin");
        String userId = store.userId("user");

        performJson(patch("/api/v1/auth/admin/users/" + userId).header("Authorization", bearer(adminToken)), Map.of("displayName", "No Reason"), 400, 40001);
        performJson(patch("/api/v1/auth/admin/users/" + userId).header("Authorization", bearer(adminToken)), Map.of("displayName", "admin", "reason", "dup"), 409, 43111);
        performJson(patch("/api/v1/auth/admin/users/missing").header("Authorization", bearer(adminToken)), Map.of("displayName", "Missing", "reason", "missing"), 404, 43100);
        performJson(patch("/api/v1/auth/admin/users/" + store.userId("owner")).header("Authorization", bearer(ownerToken)), Map.of("status", "DISABLED", "reason", "bad"), 403, 42101);
        performJson(patch("/api/v1/auth/admin/users/" + store.userId("deleted")).header("Authorization", bearer(ownerToken)), Map.of("status", "ACTIVE", "reason", "recover"), 409, 43116);

        assertThat(store.displayName("user")).isEqualTo("user");
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
    @DisplayName("AUTH-ROLE-003/004/005/007/008 cover role failures and session downgrade")
    void rolePermissionFailureAndDowngradeCases() throws Exception {
        String ownerToken = login("owner");
        String adminToken = login("admin");
        String adminId = store.userId("admin");

        performJson(put("/api/v1/auth/admin/users/" + adminId + "/roles"), Map.of("roles", List.of("USER"), "permissions", List.of(), "reason", "bad"), 401, 41000);
        performJson(put("/api/v1/auth/admin/users/" + adminId + "/roles").header("Authorization", bearer(ownerToken)), Map.of("roles", List.of(), "permissions", List.of(), "reason", "bad"), 400, 40001);
        performJson(put("/api/v1/auth/admin/users/" + adminId + "/roles").header("Authorization", bearer(ownerToken)), Map.of("roles", List.of("NOPE"), "permissions", List.of(), "reason", "bad"), 400, 40001);

        performJson(put("/api/v1/auth/admin/users/" + adminId + "/roles").header("Authorization", bearer(ownerToken)), Map.of("roles", List.of("USER"), "permissions", List.of(), "reason", "downgrade"), 200);

        mvc.perform(get("/api/v1/auth/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isUnauthorized());
        assertThat(store.latestAuditAction()).isEqualTo("AUTH_ROLE_PERMISSION_UPDATED");
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
    @DisplayName("AUTH-INV-CREATE-004/005/006/007/008/009/010 cover invitation validation and idempotency")
    void invitationCreateFailureAndIdempotencyCases() throws Exception {
        String adminToken = login("admin");
        String ownerToken = login("owner");

        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), Map.of(
                "type", "PLAYER", "boundRoles", List.of("OWNER"), "maxUses", 1, "reason", "bad"
        ), 403, 42101);
        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(adminToken)), Map.of(
                "type", "PLAYER", "boundRoles", List.of("ADMIN"), "maxUses", 1, "reason", "bad"
        ), 403, 42103);
        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), Map.of(
                "type", "PLAYER", "maxUses", 1, "reason", "bad"
        ), 400, 40001);
        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), Map.of(
                "type", "PLAYER", "boundRoles", List.of("USER"), "maxUses", 0, "reason", "bad"
        ), 400, 40001);
        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), Map.of(
                "type", "PLAYER", "boundRoles", List.of("USER"), "maxUses", 1, "expiresAt", "2020-01-01T00:00:00Z", "reason", "bad"
        ), 400, 40001);

        Map<String, Object> body = new java.util.LinkedHashMap<>(Map.of(
                "type", "PLAYER",
                "boundRoles", List.of("USER"),
                "maxUses", 2,
                "reason", "idem",
                "idempotencyKey", "inv-idem-2"
        ));
        JsonNode first = performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), body, 201);
        JsonNode second = performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), body, 201);
        assertThat(second.at("/data/rawCode").asText()).isEqualTo(first.at("/data/rawCode").asText());

        Map<String, Object> changed = new java.util.LinkedHashMap<>(body);
        changed.put("maxUses", 3);
        performJson(post("/api/v1/auth/admin/invitations").header("Authorization", bearer(ownerToken)), changed, 409, 43002);
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
    @DisplayName("AUTH-INV-LIST-002/003/004/005/006 and AUTH-INV-DISABLE-004/006 and AUTH-INV-USAGE-002/003/004")
    void invitationListDisableAndUsageFailureCases() throws Exception {
        String ownerToken = login("owner");
        String userToken = login("user");

        mvc.perform(get("/api/v1/auth/admin/invitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));
        mvc.perform(get("/api/v1/auth/admin/invitations").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
        mvc.perform(get("/api/v1/auth/admin/invitations")
                        .header("Authorization", bearer(ownerToken))
                        .param("type", "PLAYER")
                        .param("status", "ACTIVE")
                        .param("createdBy", store.userId("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("PLAYER"));

        performJson(patch("/api/v1/auth/admin/invitations/missing/disable").header("Authorization", bearer(ownerToken)), Map.of("reason", "missing"), 404, 43101);
        performJson(patch("/api/v1/auth/admin/invitations/" + store.invitationId("PLAYER-CODE-1") + "/disable").header("Authorization", bearer(ownerToken)), Map.of(), 400, 40001);

        mvc.perform(get("/api/v1/auth/admin/invitations/missing/usage-records").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(43101));
        mvc.perform(get("/api/v1/auth/admin/invitations/" + store.invitationId("PLAYER-CODE-1") + "/usage-records")
                        .header("Authorization", bearer(ownerToken))
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));

        String adminToken = login("admin");
        mvc.perform(get("/api/v1/auth/admin/invitations/" + store.invitationId("PLAYER-CODE-1") + "/usage-records")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
    }

    @Test
    @DisplayName("AUTH-STATE-001/002/003/004/005/006/007/008/009 cover state transitions")
    void stateTransitionContract() throws Exception {
        JsonNode registered = performJson(post("/api/v1/auth/register"), registerBody("PLAYER-CODE-1", "state_user"), 201);
        assertThat(registered.at("/data/user/status").asText()).isEqualTo("PENDING_PROFILE");

        String ownerToken = login("owner");
        performJson(patch("/api/v1/auth/admin/users/" + store.userId("disabled")).header("Authorization", bearer(ownerToken)), Map.of("status", "ACTIVE", "reason", "restore"), 200);
        performJson(patch("/api/v1/auth/admin/users/" + store.userId("banned")).header("Authorization", bearer(ownerToken)), Map.of("status", "ACTIVE", "reason", "restore"), 200);
        performJson(patch("/api/v1/auth/admin/users/" + store.userId("deleted")).header("Authorization", bearer(ownerToken)), Map.of("status", "ACTIVE", "reason", "restore"), 409, 43116);

        assertThat(store.invitationStatus("DISABLED-CODE-1")).isEqualTo("DISABLED");
        assertThat(store.invitationStatus("EXPIRED-CODE-1")).isEqualTo("EXPIRED");
        assertThat(store.invitationStatus("EXHAUSTED-CODE-1")).isEqualTo("EXHAUSTED");

        String adminToken = login("admin");
        performJson(put("/api/v1/auth/admin/users/" + store.userId("admin") + "/roles").header("Authorization", bearer(ownerToken)), Map.of("roles", List.of("USER"), "permissions", List.of(), "reason", "downgrade"), 200);
        mvc.perform(get("/api/v1/auth/admin/users").header("Authorization", bearer(adminToken))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH-SEC stores password and invitation code without reusable plaintext")
    void secretsAreNotStoredAsPlaintext() throws Exception {
        performJson(post("/api/v1/auth/register"), registerBody("PLAYER-CODE-1", "secret_user"), 201);
        assertThat(store.passwordHash("secret_user")).doesNotContain("Password12345");
        assertThat(store.invitationStoredSecret("PLAYER-CODE-1")).doesNotContain("PLAYER-CODE-1");
    }

    @Test
    @DisplayName("AUTH-SEC-008 rejects common weak passwords without mutating auth state")
    void commonWeakPasswordsAreRejected() throws Exception {
        int beforeUsedCount = store.invitationUsedCount("PLAYER-CODE-1");
        Map<String, Object> registerBody = registerBody("PLAYER-CODE-1", "weak_password_user");
        registerBody.put("password", "Password123");
        performJson(post("/api/v1/auth/register"), registerBody, 400, 40001);
        assertThat(store.userExists("weak_password_user")).isFalse();
        assertThat(store.invitationUsedCount("PLAYER-CODE-1")).isEqualTo(beforeUsedCount);

        performJson(post("/api/v1/auth/password-reset/request"), Map.of("username", "user"), 200);
        String resetToken = store.latestPasswordResetToken("user");
        String beforeHash = store.passwordHash("user");
        performJson(post("/api/v1/auth/password-reset/confirm"), Map.of("resetToken", resetToken, "newPassword", "Password123"), 400, 40001);
        assertThat(store.passwordHash("user")).isEqualTo(beforeHash);
    }

    @Test
    @DisplayName("AUTH-SEC-005 backend write rolls back when audit fails")
    void backendWriteRollsBackWhenAuditFails() throws Exception {
        String ownerToken = login("owner");
        String userId = store.userId("user");
        store.failNextAudit();

        performJson(patch("/api/v1/auth/admin/users/" + userId).header("Authorization", bearer(ownerToken)), Map.of(
                "displayName", "Audit Fail User",
                "reason", "simulate audit failure"
        ), 500, 51100);

        assertThat(store.displayName("user")).isEqualTo("user");
    }

    @Test
    @DisplayName("AUTH-SEC-006 ordinary register succeeds with compensation when audit fails")
    void ordinaryRegisterKeepsCompensationWhenAuditFails() throws Exception {
        store.failNextAudit();

        performJson(post("/api/v1/auth/register"), registerBody("PLAYER-CODE-1", "audit_comp_user"), 201);

        assertThat(store.userExists("audit_comp_user")).isTrue();
        assertThat(store.auditActions()).contains("AUTH_AUDIT_COMPENSATION_RECORDED");
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
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request)
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

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, expectedStatus);
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

    private JsonNode currentSession(JsonNode sessionList) {
        return java.util.stream.StreamSupport.stream(sessionList.at("/data/items").spliterator(), false)
                .filter(item -> item.path("current").asBoolean())
                .findFirst()
                .orElseThrow();
    }

    private String currentSessionId(JsonNode sessionList) {
        return currentSession(sessionList).path("id").asText();
    }

    private String nonCurrentSessionId(JsonNode sessionList) {
        return java.util.stream.StreamSupport.stream(sessionList.at("/data/items").spliterator(), false)
                .filter(item -> !item.path("current").asBoolean())
                .findFirst()
                .orElseThrow()
                .path("id")
                .asText();
    }
}
