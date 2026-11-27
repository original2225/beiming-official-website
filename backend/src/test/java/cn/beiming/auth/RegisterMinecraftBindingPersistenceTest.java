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
 * 游戏账号绑定：注册不绑、个人中心绑、绑定要落库、重启能读回。
 *
 * <p>2026-11-27 两次修正都体现在这里：
 * <ol>
 *   <li>第一版把绑定塞进注册 —— 查到库里没写、重启就丢，于是补了持久化。</li>
 *   <li>随后确认「注册时绑」本身就是错的：注册只填名字只是「声称」，
 *       服务端查得到名字存在，不等于这个人拥有它（任何人填 Notch 都能认领 Notch）。
 *       所以注册阶段一律不绑，绑定挪到 PUT /me/minecraft-binding。</li>
 * </ol>
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

    private ResponseEntity<String> putWithToken(String path, Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return http.exchange(path, org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }


    @Test
    @DisplayName("MCBIND-001 注册阶段即便传了 minecraftName 也绝不产生绑定")
    void registerNeverBindsEvenIfMinecraftNameGiven() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> response = postRegister(Map.of(
                "username", "bindoff1",
                "displayName", "绑定离线一",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1",
                // 老客户端可能还在传这两个字段，服务端必须无视
                "minecraftName", "bindoff1",
                "minecraftPlatform", "OFFLINE"));
        assertThat(response.getStatusCode().value())
                .as("响应体: %s", response.getBody()).isEqualTo(201);

        JsonNode root = mapper.readTree(response.getBody());
        String userId = root.path("data").path("user").path("id").asText();

        // 接口返回里不能带绑定
        JsonNode binding = root.path("data").path("user").path("minecraftBinding");
        assertThat(binding.isNull() || binding.isMissingNode())
                .as("注册返回不应带 minecraftBinding，实际: %s", root.path("data").path("user"))
                .isTrue();

        // 库里也不能有
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM auth_minecraft_bindings WHERE user_id = ?", Integer.class, userId);
        assertThat(count).as("注册阶段不应写入任何绑定").isZero();
    }

    @Test
    @DisplayName("MCBIND-002 个人中心绑定后必须落进 auth_minecraft_bindings")
    void bindingViaProfileIsPersisted() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> reg = postRegister(Map.of(
                "username", "centerbind1",
                "displayName", "个人中心一号",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1"));
        assertThat(reg.getStatusCode().value()).as("响应体: %s", reg.getBody()).isEqualTo(201);
        String token = mapper.readTree(reg.getBody()).path("data").path("accessToken").asText();
        String userId = mapper.readTree(reg.getBody()).path("data").path("user").path("id").asText();

        // 离线平台不联网，适合当确定性用例
        ResponseEntity<String> bind = putWithToken("/api/v1/auth/me/minecraft-binding",
                Map.of("minecraftName", "centerbind1", "minecraftPlatform", "OFFLINE"), token);
        assertThat(bind.getStatusCode().value()).as("绑定响应: %s", bind.getBody()).isEqualTo(200);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT minecraft_id, minecraft_uuid FROM auth_minecraft_bindings WHERE user_id = ?", userId);
        assertThat(row.get("minecraft_id")).isEqualTo("centerbind1");
        assertThat(row.get("minecraft_uuid"))
                .isEqualTo(MinecraftIdentityResolver.offlineUuid("centerbind1"));
    }

    @Test
    @DisplayName("MCBIND-003 个人中心绑定后重启（重新 hydrate）仍能读回")
    void bindingSurvivesReload() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> reg = postRegister(Map.of(
                "username", "reload1",
                "displayName", "重载一号",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1"));
        assertThat(reg.getStatusCode().value()).as("响应体: %s", reg.getBody()).isEqualTo(201);
        String token = mapper.readTree(reg.getBody()).path("data").path("accessToken").asText();

        putWithToken("/api/v1/auth/me/minecraft-binding",
                Map.of("minecraftName", "reload1", "minecraftPlatform", "OFFLINE"), token);

        // 模拟「进程重启」：不动数据库，只清内存索引，再走一遍启动加载。
        // 不能调 store.reset()——它会连库一起清。
        store.clearUserIndexesForReloadTest();
        store.seedLocalDevDataIfEmpty();

        ResponseEntity<String> login = post("/api/v1/auth/login",
                Map.of("username", "reload1", "password", "ab12CD"));
        String freshToken = mapper.readTree(login.getBody()).path("data").path("accessToken").asText();
        ResponseEntity<String> me = getWithToken("/api/v1/auth/me", freshToken);
        String boundName = mapper.readTree(me.getBody()).path("data")
                .path("minecraftBinding").path("minecraftId").asText();
        assertThat(boundName).as("重启后 /me 应仍带出绑定，实际响应: %s", me.getBody())
                .isEqualTo("reload1");
    }

    @Test
    @DisplayName("MCBIND-004 平台上查不到该昵称时返回 43102，不静默绑定")
    void unknownIdentityIsRejected() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> reg = postRegister(Map.of(
                "username", "notfound1",
                "displayName", "查无此人一",
                "password", "ab12CD",
                "invitationCode", "PLAYER-CODE-1"));
        assertThat(reg.getStatusCode().value()).as("响应体: %s", reg.getBody()).isEqualTo(201);
        String token = mapper.readTree(reg.getBody()).path("data").path("accessToken").asText();

        // BEIMING 站实测 523 不可达，Mojang 查不存在的人也是 404，都该判「查不到身份」
        ResponseEntity<String> bind = putWithToken("/api/v1/auth/me/minecraft-binding",
                Map.of("minecraftName", "zz_no_such_player_9999", "minecraftPlatform", "BEIMING"), token);
        assertThat(bind.getStatusCode().value())
                .as("查不到应返回 400/43102，实际: %s", bind.getBody()).isEqualTo(400);
        assertThat(bind.getBody()).contains("43102");
    }

    @Test
    @DisplayName("MCBIND-005 同一游戏身份不能被两个站内账号绑定")
    void duplicateBindingIsRejected() throws Exception {
        store.reset();
        store.seedOwner("owner", "Password12345");
        store.seedInvitation("PLAYER-CODE-1", "PLAYER",
                java.util.Set.of("USER"), java.util.Set.of(), 10, null, "owner");

        ResponseEntity<String> regA = postRegister(Map.of(
                "username", "dupfirst1", "displayName", "先绑一号",
                "password", "ab12CD", "invitationCode", "PLAYER-CODE-1"));
        assertThat(regA.getStatusCode().value()).as("响应体: %s", regA.getBody()).isEqualTo(201);
        String tokenA = mapper.readTree(regA.getBody()).path("data").path("accessToken").asText();

        ResponseEntity<String> regB = postRegister(Map.of(
                "username", "dupsecond1", "displayName", "后绑一号",
                "password", "ab12CD", "invitationCode", "PLAYER-CODE-1"));
        assertThat(regB.getStatusCode().value()).as("响应体: %s", regB.getBody()).isEqualTo(201);
        String tokenB = mapper.readTree(regB.getBody()).path("data").path("accessToken").asText();

        putWithToken("/api/v1/auth/me/minecraft-binding",
                Map.of("minecraftName", "sameplayer1", "minecraftPlatform", "OFFLINE"), tokenA);

        ResponseEntity<String> second = putWithToken("/api/v1/auth/me/minecraft-binding",
                Map.of("minecraftName", "sameplayer1", "minecraftPlatform", "OFFLINE"), tokenB);
        assertThat(second.getStatusCode().value())
                .as("同一个游戏身份重复绑定应被拒，实际: %s", second.getBody())
                .isEqualTo(409);
        assertThat(second.getBody()).contains("43115");
    }
}
