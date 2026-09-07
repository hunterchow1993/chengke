package com.chengke.chengkecrmbackend.modules.system.department.infrastructure;

import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.DepartmentDatabaseExceptionTranslator;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 PostgreSQL 自定义 SQLSTATE 不会泄露，并稳定映射为 API 业务码。
 */
class DepartmentDatabaseExceptionTranslatorTest {

    @Test
    void shouldTranslateOptimisticLockAndRootProtectionStates() {
        var translator = new DepartmentDatabaseExceptionTranslator();

        BusinessException conflict = translator.translate(new RuntimeException(new SQLException("version", "CK003")));
        BusinessException root = translator.translate(new RuntimeException(new SQLException("root", "CK009")));

        assertThat(conflict.code()).isEqualTo("DEPARTMENT_VERSION_CONFLICT");
        assertThat(conflict.httpStatus()).isEqualTo(409);
        assertThat(root.code()).isEqualTo("DEPARTMENT_ROOT_PROTECTED");
        assertThat(root.httpStatus()).isEqualTo(422);
    }
}
