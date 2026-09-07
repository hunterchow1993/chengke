package com.chengke.chengkecrmbackend.modules.system.department.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * 部门可用状态；用于领域规则、应用命令和数据库值之间的稳定映射。
 */
public enum DepartmentStatus {
    ACTIVE,
    DISABLED;

    /**
     * 返回 PostgreSQL 使用的小写状态值。
     *
     * @return active 或 disabled
     */
    @JsonValue
    public String databaseValue() {
        return name().toLowerCase();
    }

    /**
     * 解析不区分大小写的 API 或数据库状态值。
     *
     * @param value active 或 disabled
     * @return 对应部门状态
     */
    @JsonCreator
    public static DepartmentStatus fromValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
