package cn.beiming.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册限流窗口的单元测试。
 *
 * 规则（花蝉 2026-11-26 定的）：
 * - 按客户端 IP 分桶，互不连坐
 * - 每个桶 10 分钟窗口内 5 次
 * - 窗口过期自动清零，不需要重启后端
 *
 * 原先是 Map 里一个写死 "global" 的计数器，没有过期时间：任何一个人
 * 试错 5 次，整站就没人能注册，只能重启后端才恢复。测试当天被卡 3 次。
 */
class AuthRegisterLimitWindowTest {

    @Test
    @DisplayName("LIMIT-001 同一 IP 前 5 次递增，第 6 次超限")
    void countsPerIpUpToLimit() {
        Map<String, AuthStore.AttemptWindow> buckets = new HashMap<>();

        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(1);
        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(2);
        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(3);
        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(4);
        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(5);
        // 第 6 次仍然返回 6，由调用方判 > REGISTER_LIMIT 后拒绝
        assertThat(AuthStore.nextAttempt(buckets, "1.1.1.1")).isEqualTo(6);
        assertThat(6).isGreaterThan(AuthStore.REGISTER_LIMIT);
    }

    @Test
    @DisplayName("LIMIT-002 不同 IP 各自独立计数，互不连坐")
    void differentIpsAreIndependent() {
        Map<String, AuthStore.AttemptWindow> buckets = new HashMap<>();

        // A 用满 5 次
        for (int i = 1; i <= 5; i++) {
            assertThat(AuthStore.nextAttempt(buckets, "10.0.0.1")).isEqualTo(i);
        }
        // B 完全不受影响，仍然从 1 开始
        assertThat(AuthStore.nextAttempt(buckets, "10.0.0.2")).isEqualTo(1);
        assertThat(AuthStore.nextAttempt(buckets, "10.0.0.2")).isEqualTo(2);
    }

    @Test
    @DisplayName("LIMIT-003 窗口过期后计数从 1 重新开始")
    void windowExpiresAndResets() {
        Map<String, AuthStore.AttemptWindow> buckets = new HashMap<>();

        // 造一个 11 分钟前的旧桶（超过 10 分钟窗口）
        Instant stale = Instant.now().minus(AuthStore.REGISTER_WINDOW).minusSeconds(60);
        buckets.put("10.0.0.3", new AuthStore.AttemptWindow(stale, 5));

        // 过期了，应该重置为 1 而不是 6
        assertThat(AuthStore.nextAttempt(buckets, "10.0.0.3")).isEqualTo(1);
    }

    @Test
    @DisplayName("LIMIT-004 窗口内未过期则继续累加，不会误重置")
    void windowStillValidKeepsCounting() {
        Map<String, AuthStore.AttemptWindow> buckets = new HashMap<>();

        // 9 分钟前开始，窗口还没过
        Instant recent = Instant.now().minusSeconds(9 * 60);
        buckets.put("10.0.0.4", new AuthStore.AttemptWindow(recent, 3));

        assertThat(AuthStore.nextAttempt(buckets, "10.0.0.4")).isEqualTo(4);
    }

    @Test
    @DisplayName("LIMIT-005 窗口常量是 10 分钟、阈值 5")
    void constantsMatchAgreedValues() {
        assertThat(AuthStore.REGISTER_LIMIT).isEqualTo(5);
        assertThat(AuthStore.REGISTER_WINDOW.getSeconds()).isEqualTo(600);
    }
}