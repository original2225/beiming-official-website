package com.beiming.profile;

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
        classes = ProfileServiceContractTest.TestApplication.class)
class ProfileServiceContractTest {

    @LocalServerPort
    int port;

    TestRestTemplate rest = new TestRestTemplate();

    @Test
    void supportsPublicMemberListDetailAndSelfProfileUpdates() {
        String groupId = createGroup("建筑组", 10);
        Map profile = createMember(adminAuth(), Map.of(
                "authUserId", "user_player_001",
                "usernameSnapshot", "player_one",
                "displayName", "北冥玩家一号",
                "minecraftId", "MinecraftOne",
                "minecraftUuid", "00000000-0000-0000-0000-000000000001",
                "avatarUrl", "https://example.com/avatar-one.png",
                "memberGroupId", groupId,
                "status", "ACTIVE",
                "publicVisible", true,
                "joinedAt", Instant.now().minus(1, ChronoUnit.DAYS).toString()));

        ResponseEntity<Map> listResponse = get("/api/v1/profile/members?keyword=北冥", null);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).extracting("code").isEqualTo(0);
        Map list = data(listResponse);
        assertThat(list.get("page")).isEqualTo(1);
        assertThat((List<?>) list.get("items")).hasSize(1);

        ResponseEntity<Map> detailResponse = get("/api/v1/profile/members/" + profile.get("id"), null);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(detailResponse).get("minecraftId")).isEqualTo("MinecraftOne");

        ResponseEntity<Map> meResponse = get("/api/v1/profile/me", userAuth("user_player_001"));
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(meResponse).get("id")).isEqualTo(profile.get("id"));

        ResponseEntity<Map> updateResponse = patch("/api/v1/profile/me", Map.of(
                "displayName", "北冥玩家一号改",
                "bio", "喜欢大型建筑工程",
                "publicVisible", true,
                "memberGroupId", "group_should_be_ignored",
                "status", "BANNED",
                "achievements", List.of(Map.of(
                        "title", "主城一期建设",
                        "description", "参与主城地标建设",
                        "occurredAt", Instant.now().toString())),
                "works", List.of(Map.of(
                        "title", "北冥主城钟楼",
                        "description", "主城公共建筑",
                        "coverUrl", "https://example.com/work.png",
                        "linkUrl", "https://example.com/detail",
                        "sortOrder", 1))),
                userAuth("user_player_001"));

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map updated = data(updateResponse);
        assertThat(updated.get("displayName")).isEqualTo("北冥玩家一号改");
        assertThat(updated.get("bio")).isEqualTo("喜欢大型建筑工程");
        assertThat(updated.get("memberGroupId")).isEqualTo(groupId);
        assertThat(updated.get("status")).isEqualTo("ACTIVE");
        assertThat((List<?>) updated.get("achievements")).hasSize(1);
        assertThat((List<?>) updated.get("works")).hasSize(1);
    }

    @Test
    void protectsPrivateProfilesAuthenticationAdminRulesAndUniqueIdentity() {
        String groupId = createGroup("红石组", 20);
        Map privateProfile = createMember(adminAuth(), Map.of(
                "authUserId", "user_private_001",
                "usernameSnapshot", "private_one",
                "displayName", "隐藏成员",
                "minecraftId", "HiddenMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000002",
                "memberGroupId", groupId,
                "status", "ACTIVE",
                "publicVisible", false,
                "joinedAt", Instant.now().toString()));

        ResponseEntity<Map> hiddenDetailResponse = get("/api/v1/profile/members/" + privateProfile.get("id"), null);
        assertThat(hiddenDetailResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(hiddenDetailResponse.getBody()).extracting("code").isEqualTo(43000);

        ResponseEntity<Map> unauthenticatedMeResponse = get("/api/v1/profile/me", null);
        assertThat(unauthenticatedMeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticatedMeResponse.getBody()).extracting("code").isEqualTo(41000);

        ResponseEntity<Map> forbiddenAdminResponse = get("/api/v1/profile/admin/members", userAuth("user_private_001"));
        assertThat(forbiddenAdminResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbiddenAdminResponse.getBody()).extracting("code").isEqualTo(42001);

        ResponseEntity<Map> duplicateUserResponse = post("/api/v1/profile/admin/members", Map.of(
                "authUserId", "user_private_001",
                "usernameSnapshot", "private_duplicate",
                "displayName", "重复用户",
                "minecraftId", "OtherMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000003",
                "memberGroupId", groupId,
                "status", "ACTIVE",
                "publicVisible", true,
                "joinedAt", Instant.now().toString()), adminAuth());
        assertThat(duplicateUserResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateUserResponse.getBody()).extracting("code").isEqualTo(43100);

        ResponseEntity<Map> duplicateMinecraftResponse = post("/api/v1/profile/admin/members", Map.of(
                "authUserId", "user_private_002",
                "usernameSnapshot", "private_two",
                "displayName", "重复身份",
                "minecraftId", "HiddenMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000002",
                "memberGroupId", groupId,
                "status", "ACTIVE",
                "publicVisible", true,
                "joinedAt", Instant.now().toString()), adminAuth());
        assertThat(duplicateMinecraftResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateMinecraftResponse.getBody()).extracting("code").isEqualTo(43101);

        ResponseEntity<Map> missingResponse = get("/api/v1/profile/admin/members/profile_missing", adminAuth());
        assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missingResponse.getBody()).extracting("code").isEqualTo(43000);
    }

    @Test
    void supportsAdminMaintenanceStatusFlowGroupsAndInternalActivation() {
        String groupId = createGroup("后期组", 30);
        ResponseEntity<Map> duplicateGroupResponse = post("/api/v1/profile/admin/groups", Map.of(
                "name", "后期组",
                "description", "重复组",
                "sortOrder", 31,
                "enabled", true), adminAuth());
        assertThat(duplicateGroupResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateGroupResponse.getBody()).extracting("code").isEqualTo(43104);

        Map profile = createMember(adminAuth(), Map.of(
                "authUserId", "user_admin_001",
                "usernameSnapshot", "admin_created",
                "displayName", "后台成员",
                "minecraftId", "AdminCreatedMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000004",
                "memberGroupId", groupId,
                "status", "ACTIVE",
                "publicVisible", true,
                "joinedAt", Instant.now().toString()));

        ResponseEntity<Map> adminListResponse = get("/api/v1/profile/admin/members?status=ACTIVE", adminAuth());
        assertThat(adminListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) data(adminListResponse).get("items")).isNotEmpty();

        ResponseEntity<Map> adminUpdateResponse = patch("/api/v1/profile/admin/members/" + profile.get("id"), Map.of(
                "activitySummary", "最近参与主城建设",
                "contributionSummary", "累计贡献 20 分",
                "publicVisible", false), adminAuth());
        assertThat(adminUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(adminUpdateResponse).get("activitySummary")).isEqualTo("最近参与主城建设");
        assertThat(data(adminUpdateResponse).get("publicVisible")).isEqualTo(false);

        ResponseEntity<Map> statusResponse = patch("/api/v1/profile/admin/members/" + profile.get("id") + "/status",
                Map.of("status", "INACTIVE", "reason", "连续一个月未参与服务器活动"), adminAuth());
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(statusResponse).get("status")).isEqualTo("INACTIVE");

        ResponseEntity<Map> archiveResponse = patch("/api/v1/profile/admin/members/" + profile.get("id") + "/status",
                Map.of("status", "ARCHIVED", "reason", "历史档案归档"), adminAuth());
        assertThat(archiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> invalidStatusResponse = patch("/api/v1/profile/admin/members/" + profile.get("id") + "/status",
                Map.of("status", "ACTIVE", "reason", "归档后不允许恢复"), adminAuth());
        assertThat(invalidStatusResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(invalidStatusResponse.getBody()).extracting("code").isEqualTo(43103);

        ResponseEntity<Map> disableGroupResponse = patch("/api/v1/profile/admin/groups/" + groupId,
                Map.of("enabled", false), adminAuth());
        assertThat(disableGroupResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(disableGroupResponse.getBody()).extracting("code").isEqualTo(43105);

        ResponseEntity<Map> internalResponse = post("/api/v1/profile/internal/members/activate", Map.of(
                "authUserId", "user_internal_001",
                "usernameSnapshot", "internal_one",
                "displayName", "白名单成员",
                "minecraftId", "InternalMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000005",
                "avatarUrl", "https://example.com/internal.png",
                "memberGroupId", groupId,
                "publicVisible", true,
                "joinedAt", Instant.now().toString()), internalAuth());
        assertThat(internalResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> idempotentInternalResponse = post("/api/v1/profile/internal/members/activate", Map.of(
                "authUserId", "user_internal_001",
                "usernameSnapshot", "internal_one_changed",
                "displayName", "白名单成员改",
                "minecraftId", "InternalMc",
                "minecraftUuid", "00000000-0000-0000-0000-000000000005",
                "memberGroupId", groupId,
                "publicVisible", true,
                "joinedAt", Instant.now().toString()), internalAuth());
        assertThat(idempotentInternalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(idempotentInternalResponse).get("displayName")).isEqualTo("白名单成员改");
    }

    private String createGroup(String name, int sortOrder) {
        ResponseEntity<Map> response = post("/api/v1/profile/admin/groups", Map.of(
                "name", name,
                "description", name + "成员",
                "sortOrder", sortOrder,
                "enabled", true), adminAuth());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) data(response).get("id");
    }

    private Map createMember(AuthContext auth, Map<String, Object> body) {
        ResponseEntity<Map> response = post("/api/v1/profile/admin/members", body, auth);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return data(response);
    }

    private AuthContext adminAuth() {
        return new AuthContext("owner-token", "user_owner", "OWNER", "NODE_READ");
    }

    private AuthContext userAuth(String userId) {
        return new AuthContext("user-token-" + userId, userId, "USER", "");
    }

    private AuthContext internalAuth() {
        return new AuthContext("internal-token", "service_whitelist", "SERVICE", "PROFILE_WRITE");
    }

    private ResponseEntity<Map> get(String path, AuthContext auth) {
        return exchange(path, HttpMethod.GET, null, auth);
    }

    private ResponseEntity<Map> post(String path, Object body, AuthContext auth) {
        return exchange(path, HttpMethod.POST, body, auth);
    }

    private ResponseEntity<Map> patch(String path, Object body, AuthContext auth) {
        return exchange(path, HttpMethod.PATCH, body, auth);
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body, AuthContext auth) {
        HttpHeaders headers = new HttpHeaders();
        if (auth != null) {
            headers.setBearerAuth(auth.token());
            headers.add("X-Auth-User-Id", auth.userId());
            headers.add("X-Auth-Role", auth.role());
            headers.add("X-Auth-Permissions", auth.permissions());
        }
        return rest.exchange("http://localhost:" + port + path, method, new HttpEntity<>(body, headers), Map.class);
    }

    private static Map data(ResponseEntity<Map> response) {
        return (Map) response.getBody().get("data");
    }

    record AuthContext(String token, String userId, String role, String permissions) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.beiming.profile")
    static class TestApplication {
    }
}
