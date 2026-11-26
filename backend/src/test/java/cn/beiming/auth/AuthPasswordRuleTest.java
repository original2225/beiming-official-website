package cn.beiming.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 密码规则单元测试。
 *
 * 规则（花蝉 2026-11-26 定的，已同步进 docs/api-reference.md）：
 * - 长度 6 到 128
 * - 至少含两类字符：大写字母 / 小写字母 / 数字 / 其他符号
 * - 不得命中常见弱口令表
 *
 * 原先是 10 位 + 必须「字母和数字」，会把 abcdef、123456 这类挡在外面，
 * 但也会把 abcd!@ 这种有符号的挡在外面。改成「任意两类」后：
 * 纯数字和纯字母仍然不行，但字母配符号、大小写混合都能过。
 *
 * 这些用例直接打 validatePassword，不起 Spring 上下文，跑起来是毫秒级。
 */
class AuthPasswordRuleTest {

    @Test
    @DisplayName("PWD-RULE-001 六位起步，六位合法")
    void acceptsSixCharacterPassword() {
        // 6 位是下限本身，必须过
        assertThatCode(() -> AuthController.validatePassword("abc123"))
                .as("abc123 是 6 位、小写字母+数字两类，应当合法")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PWD-RULE-002 五位被拒")
    void rejectsFiveCharacterPassword() {
        assertThatThrownBy(() -> AuthController.validatePassword("ab12c"))
                .as("5 位低于下限，即便含两类字符也要拒")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PWD-RULE-003 长度上限 128")
    void rejectsOverlongPassword() {
        String tooLong = "a1".repeat(65); // 130 位
        assertThatThrownBy(() -> AuthController.validatePassword(tooLong))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PWD-RULE-004 纯数字被拒")
    void rejectsDigitsOnly() {
        assertThatThrownBy(() -> AuthController.validatePassword("123456789"))
                .as("只有数字一类，必须拒")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PWD-RULE-005 纯小写字母被拒")
    void rejectsLowerCaseOnly() {
        assertThatThrownBy(() -> AuthController.validatePassword("abcdefgh"))
                .as("只有小写一类，必须拒")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PWD-RULE-006 字母加符号合法（不算数字也算两类）")
    void acceptsLettersPlusSymbols() {
        assertThatCode(() -> AuthController.validatePassword("abcd!@"))
                .as("小写字母 + 符号 = 两类，应当合法")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PWD-RULE-007 大小写混合合法（没有数字也算两类）")
    void acceptsMixedCaseWithoutDigits() {
        assertThatCode(() -> AuthController.validatePassword("Abcdef"))
                .as("大写 + 小写 = 两类，应当合法")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PWD-RULE-008 数字加符号合法")
    void acceptsDigitsPlusSymbols() {
        assertThatCode(() -> AuthController.validatePassword("123456!"))
                .as("数字 + 符号 = 两类，应当合法")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PWD-RULE-009 常见弱口令仍被拒")
    void rejectsCommonWeakPasswords() {
        // 这些即使满足长度和字符类型，也要被弱口令表挡住（大小写不敏感）
        assertThatThrownBy(() -> AuthController.validatePassword("password123"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> AuthController.validatePassword("PASSWORD123"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PWD-RULE-010 中文等多字节字符计入符号类")
    void acceptsMultibyteCharacters() {
        assertThatCode(() -> AuthController.validatePassword("密码123456"))
                .as("中文按非字母数字处理，算符号类，配上数字就是两类")
                .doesNotThrowAnyException();
    }
}