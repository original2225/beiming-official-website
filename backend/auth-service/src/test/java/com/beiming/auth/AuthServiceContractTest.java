package com.beiming.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AuthServiceContractTest.TestApplication.class)
class AuthServiceContractTest {

    @LocalServerPort
    int port;

    TestRestTemplate rest = new TestRestTemplate();

    @Test
    void supportsInviteRegistrationLoginSessionUserManagementAndMinecraftBinding() {
        AuthResponse ownerLogin = login("owner", "OwnerPass123!");

        ResponseEntity<Map> inviteResponse = post("/api/v1/auth/invites",
                Map.of(
                        "type", "PLAYER",
                        "role", "USER",
                        "permissions", List.of(),
                        "maxUses", 2,
                        "expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString(),
                        "note", "P0 test player invite"),
                ownerLogin.accessToken);

        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(inviteResponse.getBody()).extracting("code").isEqualTo(0);
        Map invite = data(inviteResponse);
        String inviteCode = (String) invite.get("code");
        assertThat(inviteCode).startsWith("BM-PLAYER-");

        ResponseEntity<Map> registerResponse = post("/api/v1/auth/register",
                Map.of(
                        "inviteCode", inviteCode,
                        "username", "player_one",
                        "password", "PlayerPass123!",
                        "displayName", "北冥玩家",
                        "email", "player@example.com"),
                null);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse registered = AuthResponse.from(registerResponse);
        assertThat(registered.user.get("role")).isEqualTo("USER");
        assertThat(registered.user.get("status")).isEqualTo("PENDING_PROFILE");

        AuthResponse loggedIn = login("player_one", "PlayerPass123!");
        assertThat(loggedIn.user.get("username")).isEqualTo("player_one");

        ResponseEntity<Map> meResponse = get("/api/v1/auth/me", loggedIn.accessToken);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(meResponse).get("username")).isEqualTo("player_one");

        ResponseEntity<Map> verifyResponse = post("/api/v1/auth/session/verify", Map.of(), loggedIn.accessToken);
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(verifyResponse).get("valid")).isEqualTo(true);

        ResponseEntity<Map> userListResponse = get("/api/v1/auth/users", ownerLogin.accessToken);
        assertThat(userListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map userList = data(userListResponse);
        assertThat(userList.get("page")).isEqualTo(1);
        List<String> usernames = ((List<Map<?, ?>>) userList.get("items")).stream()
                .map(item -> (String) item.get("username"))
                .toList();
        assertThat(usernames).contains("owner", "player_one");

        String playerId = (String) registered.user.get("id");
        ResponseEntity<Map> updateResponse = patch("/api/v1/auth/users/" + playerId,
                Map.of("displayName", "北冥玩家一号", "email", "player1@example.com"),
                loggedIn.accessToken);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updateResponse).get("displayName")).isEqualTo("北冥玩家一号");

        ResponseEntity<Map> roleResponse = patch("/api/v1/auth/users/" + playerId + "/roles",
                Map.of("role", "HELPER", "permissions", List.of("NODE_READ"), "reason", "P0 auth contract test"),
                ownerLogin.accessToken);
        assertThat(roleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(roleResponse).get("role")).isEqualTo("HELPER");
        assertThat((List<String>) data(roleResponse).get("permissions")).contains("NODE_READ");

        ResponseEntity<Map> bindResponse = put("/api/v1/auth/minecraft-binding",
                Map.of("minecraftId", "MinecraftName", "minecraftUuid", "00000000-0000-0000-0000-000000000000"),
                loggedIn.accessToken);
        assertThat(bindResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(bindResponse).get("minecraftId")).isEqualTo("MinecraftName");

        ResponseEntity<Map> usesResponse = get("/api/v1/auth/invites/" + invite.get("id") + "/uses", ownerLogin.accessToken);
        assertThat(usesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) data(usesResponse).get("items")).hasSize(1);

        ResponseEntity<Map> disableInviteResponse = patch("/api/v1/auth/invites/" + invite.get("id"),
                Map.of("status", "DISABLED"),
                ownerLogin.accessToken);
        assertThat(disableInviteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(disableInviteResponse).get("status")).isEqualTo("DISABLED");

        ResponseEntity<Map> logoutResponse = post("/api/v1/auth/logout", Map.of(), loggedIn.accessToken);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> afterLogoutResponse = get("/api/v1/auth/me", loggedIn.accessToken);
        assertThat(afterLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(afterLogoutResponse.getBody()).extracting("code").isEqualTo(41001);
    }

    @Test
    void protectsOwnerAndAdminInviteRules() {
        AuthResponse ownerLogin = login("owner", "OwnerPass123!");

        ResponseEntity<Map> adminInviteResponse = post("/api/v1/auth/invites",
                Map.of(
                        "type", "ADMIN",
                        "role", "ADMIN",
                        "permissions", List.of("NODE_READ"),
                        "maxUses", 1,
                        "expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString(),
                        "note", "P0 admin invite"),
                ownerLogin.accessToken);
        assertThat(adminInviteResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String adminInviteCode = (String) data(adminInviteResponse).get("code");

        AuthResponse adminLogin = register("admin_one", "AdminPass123!", "admin@example.com", adminInviteCode);
        assertThat(adminLogin.user.get("role")).isEqualTo("ADMIN");

        ResponseEntity<Map> forbiddenInviteResponse = post("/api/v1/auth/invites",
                Map.of(
                        "type", "ADMIN",
                        "role", "ADMIN",
                        "permissions", List.of(),
                        "maxUses", 1,
                        "expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString(),
                        "note", "forbidden"),
                adminLogin.accessToken);
        assertThat(forbiddenInviteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbiddenInviteResponse.getBody()).extracting("code").isEqualTo(41205);

        ResponseEntity<Map> ownerListResponse = get("/api/v1/auth/users?keyword=owner", ownerLogin.accessToken);
        String ownerId = (String) ((Map<?, ?>) ((List<?>) data(ownerListResponse).get("items")).getFirst()).get("id");

        ResponseEntity<Map> demoteOwnerResponse = patch("/api/v1/auth/users/" + ownerId + "/roles",
                Map.of("role", "ADMIN", "permissions", List.of(), "reason", "should fail"),
                ownerLogin.accessToken);
        assertThat(demoteOwnerResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(demoteOwnerResponse.getBody()).extracting("code").isEqualTo(42101);
    }

    @Test
    void supportsPasswordResetWithoutExposingAccountExistence() {
        AuthResponse ownerLogin = login("owner", "OwnerPass123!");
        ResponseEntity<Map> inviteResponse = post("/api/v1/auth/invites",
                Map.of(
                        "type", "PLAYER",
                        "role", "USER",
                        "permissions", List.of(),
                        "maxUses", 1,
                        "expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString(),
                        "note", "password reset invite"),
                ownerLogin.accessToken);
        AuthResponse playerLogin = register("reset_user", "OldPass123!", "reset@example.com", (String) data(inviteResponse).get("code"));

        ResponseEntity<Map> requestResponse = post("/api/v1/auth/password-reset/request",
                Map.of("usernameOrEmail", "reset_user"),
                null);
        assertThat(requestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String resetToken = (String) data(requestResponse).get("resetToken");
        assertThat(resetToken).startsWith("rst_");

        ResponseEntity<Map> confirmResponse = post("/api/v1/auth/password-reset/confirm",
                Map.of("token", resetToken, "newPassword", "NewPass123!"),
                null);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> oldSessionResponse = get("/api/v1/auth/me", playerLogin.accessToken);
        assertThat(oldSessionResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        AuthResponse newLogin = login("reset_user", "NewPass123!");
        assertThat(newLogin.user.get("username")).isEqualTo("reset_user");
    }

    private AuthResponse login(String username, String password) {
        ResponseEntity<Map> response = post("/api/v1/auth/login", Map.of("username", username, "password", password), null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return AuthResponse.from(response);
    }

    private AuthResponse register(String username, String password, String email, String inviteCode) {
        ResponseEntity<Map> response = post("/api/v1/auth/register",
                Map.of(
                        "inviteCode", inviteCode,
                        "username", username,
                        "password", password,
                        "displayName", username,
                        "email", email),
                null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return AuthResponse.from(response);
    }

    private ResponseEntity<Map> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private ResponseEntity<Map> post(String path, Object body, String token) {
        return exchange(path, HttpMethod.POST, body, token);
    }

    private ResponseEntity<Map> patch(String path, Object body, String token) {
        return exchange(path, HttpMethod.PATCH, body, token);
    }

    private ResponseEntity<Map> put(String path, Object body, String token) {
        return exchange(path, HttpMethod.PUT, body, token);
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange("http://localhost:" + port + path, method, new HttpEntity<>(body, headers), Map.class);
    }

    private static Map data(ResponseEntity<Map> response) {
        return (Map) response.getBody().get("data");
    }

    record AuthResponse(String accessToken, Map user) {
        static AuthResponse from(ResponseEntity<Map> response) {
            Map body = data(response);
            return new AuthResponse((String) body.get("accessToken"), (Map) body.get("user"));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.beiming.auth")
    static class TestApplication {
    }
}
