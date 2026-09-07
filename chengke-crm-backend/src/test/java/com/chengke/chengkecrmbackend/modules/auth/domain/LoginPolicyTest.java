package com.chengke.chengkecrmbackend.modules.auth.domain;

import com.chengke.chengkecrmbackend.modules.auth.domain.exception.LoginDomainException;
import com.chengke.chengkecrmbackend.modules.auth.domain.policy.LoginPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证登录账号识别、凭据格式和风控阈值，不依赖持久化与框架。
 */
class LoginPolicyTest {

    private final LoginPolicy policy = new LoginPolicy(5, 10);

    @Test
    void shouldTrimAccountWithoutChangingCase() {
        assertThat(policy.normalizeAccount("  AdminUser  ")).isEqualTo("AdminUser");
    }

    @Test
    void shouldTreatElevenDigitMainlandMobileAsMobile() {
        assertThat(policy.mobileAccount("13800000001")).isTrue();
        assertThat(policy.mobileAccount("admin")).isFalse();
        assertThat(policy.mobileAccount("1380000000")).isFalse();
    }

    @Test
    void shouldRejectBlankOrOversizedAccountAndShortPassword() {
        assertThatThrownBy(() -> policy.validateLoginInput("  ", "Abcdef12"))
                .isInstanceOf(LoginDomainException.class)
                .extracting(exception -> ((LoginDomainException) exception).code())
                .isEqualTo("INVALID_REQUEST");
        assertThatThrownBy(() -> policy.validateLoginInput("a".repeat(33), "Abcdef12"))
                .isInstanceOf(LoginDomainException.class)
                .extracting(exception -> ((LoginDomainException) exception).code())
                .isEqualTo("INVALID_REQUEST");
        assertThatThrownBy(() -> policy.validateLoginInput("admin", "short"))
                .isInstanceOf(LoginDomainException.class)
                .extracting(exception -> ((LoginDomainException) exception).code())
                .isEqualTo("INVALID_REQUEST");
        policy.validateLoginInput("admin", "Crm@2026!");
    }

    @Test
    void shouldRequireCaptchaAfterFiveFailuresAndLockAfterTen() {
        assertThat(policy.captchaRequired(4)).isFalse();
        assertThat(policy.captchaRequired(5)).isTrue();
        assertThat(policy.locked(9)).isFalse();
        assertThat(policy.locked(10)).isTrue();
    }
}
