package cn.beiming.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.beiming.core.BusinessCoreServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册时带 Minecraft 绑定是否真的落库。
 *
 * 为什么单独写这个测试：2026-11-27 实测发现注册时设置的 minecraftBinding
 * 只进了内存 Map，`auth_minecraft_bindings` 表里一行都没有 —— 后端一重启
 * 绑定就没了。接口返回 200、userSummary 里也带着 binding，看起来完全正常，
 * 光靠接口断言抓不到。所以这里直接查库。
 */
@Testcontainers
@SpringBootTest(
        classes = BusinessCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "beiming.business-core.test-control-headers.enabled=true",
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true"
        }
)
class RegisterMinecraftBindingPersistenceTest {

    static {
        // Docker Desktop 的 npipe 需要显式 API 版本，否则 Testcontainers 连不上
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_mcbind")
            .withUsername("beiming")
            .withPassword("beiming");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    TestRestTemplate http;
    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AuthStore store;
    @Autowired
    ObjectMapper mapper;

    private ResponseEntity<String> postRegister(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(body, headers), String.class);
    }
    private ResponseEntity<String> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }


    @Test
    @DisplayName("MCBIND-001 离线平台注册后绑定必须落进 auth_minecraft_bindings")
    void offlineBindingIsPersisted() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        // 邀请码是内存态，reset() 后必须重新播种，否则注册直接 43101
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> response = postRegister(Map.of(
                "username", "bindoff1",
                "displayName", "绑定离线一",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1",
                "minecraftName", "bindoff1",
                "minecraftPlatform", "OFFLINE"));
        assertThat(response.getStatusCode().value()).as("响应体: %s", response.getBody()).isEqualTo(201);

        JsonNode root = mapper.readTree(response.getBody());
        String userId = root.path("data").path("user").path("id").asText();

        // 接口返回里带着绑定
        assertThat(root.path("data").path("user").path("minecraftBinding").path("minecraftId").asText())
                .isEqualTo("bindoff1");

        // 关键：库里也要有，否则重启即丢
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT minecraft_id, minecraft_uuid, source FROM auth_minecraft_bindings WHERE user_id = ?",
                userId);
        assertThat(row.get("minecraft_id")).isEqualTo("bindoff1");
        assertThat(row.get("source")).isEqualTo("OFFLINE");
        assertThat(row.get("minecraft_uuid"))
                .isEqualTo(MinecraftIdentityResolver.offlineUuid("bindoff1"));
    }

    @Test
    @DisplayName("MCBIND-002 不传 minecraftName 时照常注册、不产生绑定记录")
    void registerWithoutMinecraftStillWorks() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        // 邀请码是内存态，reset() 后必须重新播种，否则注册直接 43101
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> response = postRegister(Map.of(
                "username", "nomc1",
                "displayName", "无游戏账号",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1"));
        assertThat(response.getStatusCode().value()).as("响应体: %s", response.getBody()).isEqualTo(201);

        String userId = mapper.readTree(response.getBody()).path("data").path("user").path("id").asText();

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM auth_minecraft_bindings WHERE user_id = ?", Integer.class, userId);
        assertThat(count).isZero();
    }
    @Test
    @DisplayName("MCBIND-003 重启后（重新 hydrate）绑定仍在，不会丢")
    void bindingSurvivesReload() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> response = postRegister(Map.of(
                "username", "reload1",
                "displayName", "重载一号",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1",
                "minecraftName", "reload1",
                "minecraftPlatform", "OFFLINE"));
        assertThat(response.getStatusCode().value()).as("响应体: %s", response.getBody()).isEqualTo(201);

        // 这一条是关键：模拟「进程重启」——不动数据库，只清掉内存里的用户索引，
        // 再走一遍启动加载路径（seedLocalDevDataIfEmpty 会从库读回用户）。
        // 原来 loadUsers() 不读 auth_minecraft_bindings，于是库里明明有绑定，
        // 重启后 /me 返回 null。注意不能调 store.reset()：它会连库一起清。
        store.clearUserIndexesForReloadTest();
        store.seedLocalDevDataIfEmpty();

        String userId = mapper.readTree(response.getBody()).path("data").path("user").path("id").asText();
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT minecraft_id, minecraft_uuid FROM auth_minecraft_bindings WHERE user_id = ?", userId);
        assertThat(row.get("minecraft_id")).isEqualTo("reload1");

        // 内存里也要有：登录后的 /me 才带得出绑定
        ResponseEntity<String> login = post("/api/v1/auth/login", Map.of(
                "username", "reload1", "password", "ab12CD"));
        String token = mapper.readTree(login.getBody()).path("data").path("accessToken").asText();
        ResponseEntity<String> me = getWithToken("/api/v1/auth/me", token);
        String boundName = mapper.readTree(me.getBody()).path("data")
                .path("minecraftBinding").path("minecraftId").asText();
        assertThat(boundName).as("重启后 /me 应仍带出绑定，实际响应: %s", me.getBody())
                .isEqualTo("reload1");
    }
}
