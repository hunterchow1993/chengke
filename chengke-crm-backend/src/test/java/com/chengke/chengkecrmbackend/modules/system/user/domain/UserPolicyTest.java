package com.chengke.chengkecrmbackend.modules.system.user.domain;

import com.chengke.chengkecrmbackend.modules.system.user.domain.exception.UserDomainException;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证用户脱敏、密码强度和保护账号规则独立于持久化与框架。
 */
class UserPolicyTest {

    private final UserPolicy policy = new UserPolicy();

    @Test
    void shouldMaskMobileKeepingFirstThreeAndLastFour() {
        assertThat(policy.maskMobile("13800008006")).isEqualTo("138****8006");
    }

    @Test
    void shouldMaskEmailKeepingFirstLocalCharacter() {
        assertThat(policy.maskEmail("zhouba@example.com")).isEqualTo("z***@example.com");
        assertThat(policy.maskEmail(null)).isNull();
    }

    @Test
    void shouldAcceptPasswordWithTwoCharacterClassesAndRejectWeakOnes() {
        assertThat(policy.isValidPassword("Abcdef12")).isTrue();
        assertThat(policy.isValidPassword("abcdefg!")).isTrue();
        assertThat(policy.isValidPassword("abcdefg1")).isTrue();
        assertThat(policy.isValidPassword("abcdefgh")).isFalse();
        assertThat(policy.isValidPassword("short1!")).isFalse();
        assertThat(policy.isValidPassword("Abcdef  12")).isFalse();
    }

    @Test
    void shouldRejectSelfDisableAndLastSuperAdmin() {
        assertThatThrownBy(() -> policy.validateDisable(true, false))
                .isInstanceOf(UserDomainException.class)
                .extracting(exception -> ((UserDomainException) exception).code())
                .isEqualTo("USER_SELF_DISABLE_FORBIDDEN");
        assertThatThrownBy(() -> policy.validateDisable(false, true))
                .isInstanceOf(UserDomainException.class)
                .extracting(exception -> ((UserDomainException) exception).code())
                .isEqualTo("USER_LAST_SUPER_ADMIN_PROTECTED");
        policy.validateDisable(false, false);
    }

    @Test
    void shouldRejectSelfRoleChange() {
        assertThatThrownBy(() -> policy.validateRoleChange(true, true))
                .isInstanceOf(UserDomainException.class)
                .extracting(exception -> ((UserDomainException) exception).code())
                .isEqualTo("USER_SELF_ROLE_CHANGE_FORBIDDEN");
        policy.validateRoleChange(true, false);
        policy.validateRoleChange(false, true);
    }
}
