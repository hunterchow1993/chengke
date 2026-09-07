package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence;

import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * 把 PostgreSQL 用户唯一约束和未映射错误转换为稳定、安全的业务异常。
 */
@Component
public class UserDatabaseExceptionTranslator {

    /**
     * 转换数据库异常；不会把 SQL、约束名或数据库消息返回客户端。
     *
     * @param exception MyBatis/Spring 包装后的数据库异常
     * @return 稳定业务异常
     */
    public BusinessException translate(RuntimeException exception) {
        SQLException sql = findSqlException(exception);
        if (sql != null && "23505".equals(sql.getSQLState())) {
            String message = sql.getMessage() == null ? "" : sql.getMessage();
            if (message.contains("uq_sys_user_tenant_username")) {
                return new BusinessException("USER_USERNAME_DUPLICATE", 409, "用户名已存在");
            }
            if (message.contains("uq_sys_user_tenant_mobile")) {
                return new BusinessException("USER_MOBILE_DUPLICATE", 409, "手机号已存在");
            }
        }
        return new BusinessException("USER_DATABASE_OPERATION_FAILED", 500, "系统异常");
    }

    private SQLException findSqlException(Throwable exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof SQLException sqlException) {
                return sqlException;
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
