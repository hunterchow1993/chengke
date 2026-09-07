package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence;

import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;

/**
 * 把 PostgreSQL 部门函数 SQLSTATE 和唯一约束错误转换为稳定、安全的业务异常。
 */
@Component
public class DepartmentDatabaseExceptionTranslator {
    private static final Map<String, ErrorMapping> STATE_MAPPING = Map.of(
            "CK001", new ErrorMapping("DEPARTMENT_IMMUTABLE_FIELD", 422, "部门不可变字段不能修改"),
            "CK002", new ErrorMapping("DEPARTMENT_IMMUTABLE_FIELD", 422, "部门不可变字段不能修改"),
            "CK003", new ErrorMapping("DEPARTMENT_VERSION_CONFLICT", 409, "部门已被其他管理员修改"),
            "CK004", new ErrorMapping("DEPARTMENT_MOVE_CYCLE", 422, "部门不能移动到自身或后代"),
            "CK005", new ErrorMapping("DEPARTMENT_MOVE_REQUIRED", 422, "修改上级请使用移动操作"),
            "CK006", new ErrorMapping("DEPARTMENT_PARENT_INVALID", 422, "目标上级或部门状态不合法"),
            "CK007", new ErrorMapping("DEPARTMENT_ACTIVE_DESCENDANTS", 422, "存在正常后代，请选择级联停用"),
            "CK008", new ErrorMapping("DEPARTMENT_PARENT_DISABLED", 422, "上级部门已停用"),
            "CK009", new ErrorMapping("DEPARTMENT_ROOT_PROTECTED", 422, "集团根节点受保护"),
            "CK010", new ErrorMapping("DEPARTMENT_DELETE_BLOCKED", 422, "部门存在下级或受保护引用")
    );

    /**
     * 转换数据库异常；不会把 SQL、约束名或数据库消息返回客户端。
     *
     * @param exception MyBatis/Spring 包装后的数据库异常
     * @return 稳定业务异常
     */
    public BusinessException translate(RuntimeException exception) {
        SQLException sql = findSqlException(exception);
        if (sql != null) {
            ErrorMapping mapping = STATE_MAPPING.get(sql.getSQLState());
            if (mapping != null) {
                return new BusinessException(mapping.code(), mapping.status(), mapping.message());
            }
            if ("23505".equals(sql.getSQLState())) {
                String message = sql.getMessage() == null ? "" : sql.getMessage();
                if (message.contains("uq_department_active_sibling_name")) {
                    return new BusinessException("DEPARTMENT_SIBLING_NAME_DUPLICATE", 409, "同级部门名称已存在");
                }
                return new BusinessException("DEPARTMENT_CODE_DUPLICATE", 409, "部门编码已存在");
            }
        }
        return new BusinessException("DEPARTMENT_DATABASE_OPERATION_FAILED", 500, "部门数据库操作失败");
    }

    /** 从任意数据库包装异常链中查找原始 SQLException。 */
    private SQLException findSqlException(Throwable exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof SQLException sqlException) return sqlException;
            cursor = cursor.getCause();
        }
        return null;
    }

    /** 稳定业务异常映射配置。 */
    private record ErrorMapping(String code, int status, String message) {
    }
}
