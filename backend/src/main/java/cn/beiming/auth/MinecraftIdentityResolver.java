package cn.beiming.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Minecraft 身份解析：昵称 → UUID。
 *
 * 背景（花蝉 2026-11-27 提的）：原 `PUT /me/minecraft-binding` 要求用户自己填
 * 32 位 UUID，普通玩家根本不知道自己 UUID 是多少，等于这个功能不可用。
 * 改成「只填游戏昵称，服务端按平台自动查 UUID」。
 *
 * 三个平台：
 * - LITTLESKIN：外置登录站，POST Yggdrasil 标准的 profiles/minecraft 批量查名。
 * - MOJANG：正版（微软账号迁移后仍走这个老端点查 UUID）。
 * - OFFLINE：离线服。UUID 由 `OfflinePlayer:<昵称>` 的 MD5 加版本/variant 位算出，
 *   不联网也能得到，与离线服务器实际发放的 UUID 一致。
 *
 * 查询失败一律返回 null，由调用方决定是拒绝注册还是允许仅填昵称不绑定——
 * 这里不抛异常，避免第三方服务抖动直接把注册流程打挂。
 */
@Component
public class MinecraftIdentityResolver {

    /** 支持的外置登录平台 */
    public enum Platform {
        LITTLESKIN, MOJANG, OFFLINE;

        static Platform parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return OFFLINE;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** 解析结果：昵称 + 32 位无连字符小写 UUID + 来源平台 */
    public record Identity(String name, String uuid, Platform platform) {
    }

    static final String LITTLESKIN_ENDPOINT = "https://littleskin.cn/api/yggdrasil/api/profiles/minecraft";
    static final String MOJANG_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";

    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 按昵称解析身份。昵称不合法或查不到都返回 null。
     * 昵称规则与 Minecraft 一致：3-16 位字母数字下划线。
     */
    public Identity resolve(String rawName, String rawPlatform) {
        String name = rawName == null ? "" : rawName.trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            return null;
        }
        Platform platform = Platform.parse(rawPlatform);
        if (platform == null) {
            return null;
        }
        if (platform == Platform.OFFLINE) {
            return new Identity(name, offlineUuid(name), Platform.OFFLINE);
        }
        String uuid = platform == Platform.LITTLESKIN ? fromLittleSkin(name) : fromMojang(name);
        return uuid == null ? null : new Identity(name, uuid, platform);
    }

    /** 离线服 UUID：MD5("OfflinePlayer:<name>") 再设 version=3、variant=RFC4122 */
    static String offlineUuid(String name) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            bytes[6] = (byte) ((bytes[6] & 0x0F) | 0x30);
            bytes[8] = (byte) ((bytes[8] & 0x3F) | 0x80);
            return toHex(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * LittleSkin：POST JSON 数组到 Yggdrasil 标准查名端点。
     * 返回 [{"id":"...","name":"..."}]，查不到返回 []。
     * 注意路径必须带 `/api/yggdrasil` 前缀，少一层会 404（实测
     * `/api/yggdrasil/profiles/minecraft` 与 `/api/yggdrasil/api/users/profiles/minecraft` 都是 404）。
     */
    private String fromLittleSkin(String name) {
        try {
            String body = mapper.writeValueAsString(List.of(name));
            HttpRequest request = HttpRequest.newBuilder(URI.create(LITTLESKIN_ENDPOINT))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }
            JsonNode first = root.get(0);
            String id = first.path("id").asText("");
            String found = first.path("name").asText("");
            // 昵称必须对得上，避免大小写不同被换成别人
            if (!id.matches("[a-f0-9]{32}") || !found.equalsIgnoreCase(name)) {
                return null;
            }
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    /** 正版：GET /users/profiles/minecraft/{name}，返回 {"id":"...","name":"..."}，查不到 204/404 */
    private String fromMojang(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(MOJANG_ENDPOINT + name))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode root = mapper.readTree(response.body());
            String id = root.path("id").asText("");
            String found = root.path("name").asText("");
            if (!id.matches("[a-f0-9]{32}") || !found.equalsIgnoreCase(name)) {
                return null;
            }
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}