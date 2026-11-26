package cn.beiming.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minecraft 昵称 → UUID 解析。
 *
 * 离线 UUID 用的是 Minecraft 服务端自己的算法（`OfflinePlayer:<name>` 取 MD5，
 * 再设 version=3 / variant=RFC4122），和离线服实际发放的 UUID 一致，
 * 所以这部分完全可以离线断言，不依赖网络。
 *
 * 联网部分（LittleSkin / Mojang）不在这里断言——第三方抖动不该让测试变红。
 */
class MinecraftIdentityResolverTest {

    private final MinecraftIdentityResolver resolver = new MinecraftIdentityResolver();

    @Test
    @DisplayName("MCID-001 离线 UUID 与 Minecraft 服务端算法一致")
    void offlineUuidMatchesVanillaAlgorithm() {
        // 这两个值可以用 Minecraft 服务端或任何标准实现对照验证
        assertThat(MinecraftIdentityResolver.offlineUuid("Notch"))
                .isEqualTo("b50ad385829d3141a2167e7d7539ba7f");
        assertThat(MinecraftIdentityResolver.offlineUuid("huachan"))
                .isEqualTo("f748a66f1b9031d59a87cfa1000c6bf4");
    }

    @Test
    @DisplayName("MCID-002 离线 UUID 是 32 位小写十六进制，且带 v3 位")
    void offlineUuidShape() {
        String uuid = MinecraftIdentityResolver.offlineUuid("Steve");
        assertThat(uuid).matches("[a-f0-9]{32}");
        // 第 13 个十六进制位（version）恒为 3
        assertThat(uuid.charAt(12)).isEqualTo('3');
    }

    @Test
    @DisplayName("MCID-003 不同昵称得到不同 UUID，大小写敏感")
    void offlineUuidIsNameSensitive() {
        assertThat(MinecraftIdentityResolver.offlineUuid("Steve"))
                .isNotEqualTo(MinecraftIdentityResolver.offlineUuid("Alex"));
        // Minecraft 的离线 UUID 按原始大小写计算，Steve / steve 是两个身份
        assertThat(MinecraftIdentityResolver.offlineUuid("Steve"))
                .isNotEqualTo(MinecraftIdentityResolver.offlineUuid("steve"));
    }

    @Test
    @DisplayName("MCID-004 离线平台不联网即可解析成功")
    void offlinePlatformResolvesWithoutNetwork() {
        var identity = resolver.resolve("huachan", "OFFLINE");
        assertThat(identity).isNotNull();
        assertThat(identity.name()).isEqualTo("huachan");
        assertThat(identity.uuid()).isEqualTo("f748a66f1b9031d59a87cfa1000c6bf4");
        assertThat(identity.platform()).isEqualTo(MinecraftIdentityResolver.Platform.OFFLINE);
    }

    @Test
    @DisplayName("MCID-005 未传平台时默认按离线处理")
    void defaultsToOffline() {
        var identity = resolver.resolve("Steve", null);
        assertThat(identity).isNotNull();
        assertThat(identity.platform()).isEqualTo(MinecraftIdentityResolver.Platform.OFFLINE);
    }

    @Test
    @DisplayName("MCID-006 昵称不符合 Minecraft 规则时拒绝")
    void rejectsIllegalName() {
        // 太短 / 太长 / 含中文 / 含空格 / 含连字符，都不是合法 MC 昵称
        assertThat(resolver.resolve("ab", "OFFLINE")).isNull();
        assertThat(resolver.resolve("abcdefghijklmnopq", "OFFLINE")).isNull();
        assertThat(resolver.resolve("花蝉", "OFFLINE")).isNull();
        assertThat(resolver.resolve("hua chan", "OFFLINE")).isNull();
        assertThat(resolver.resolve("hua-chan", "OFFLINE")).isNull();
        assertThat(resolver.resolve("", "OFFLINE")).isNull();
        assertThat(resolver.resolve(null, "OFFLINE")).isNull();
    }

    @Test
    @DisplayName("MCID-007 未知平台名直接拒绝，不会静默当离线")
    void rejectsUnknownPlatform() {
        assertThat(resolver.resolve("Steve", "SOMETHING_ELSE")).isNull();
    }

    @Test
    @DisplayName("MCID-008 平台名大小写不敏感")
    void platformNameIsCaseInsensitive() {
        assertThat(resolver.resolve("Steve", "offline")).isNotNull();
        assertThat(resolver.resolve("Steve", "Offline")).isNotNull();
    }

    @Test
    @DisplayName("MCID-009 昵称两端空格会被裁掉")
    void trimsName() {
        var identity = resolver.resolve("  Steve  ", "OFFLINE");
        assertThat(identity).isNotNull();
        assertThat(identity.name()).isEqualTo("Steve");
    }
}