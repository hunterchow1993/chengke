package com.chengke.chengkecrmbackend.modules.system.user.infrastructure;

import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.UserDatabaseExceptionTranslator;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证用户唯一约束不会泄露，并稳定映射为 API 业务码。
 */
class UserDatabaseExceptionTranslatorTest {

    @Test
    void shouldTranslateUsernameAndMobileDuplicates() {
        var translator = new UserDatabaseExceptionTranslator();

        BusinessException username = translator.translate(
                new RuntimeException(new SQLException("uq_sys_user_tenant_username", "23505")));
        BusinessException mobile = translator.translate(
                new RuntimeException(new SQLException("uq_sys_user_tenant_mobile", "23505")));

        assertThat(username.code()).isEqualTo("USER_USERNAME_DUPLICATE");
        assertThat(username.httpStatus()).isEqualTo(409);
        assertThat(mobile.code()).isEqualTo("USER_MOBILE_DUPLICATE");
        assertThat(mobile.httpStatus()).isEqualTo(409);
    }
}
