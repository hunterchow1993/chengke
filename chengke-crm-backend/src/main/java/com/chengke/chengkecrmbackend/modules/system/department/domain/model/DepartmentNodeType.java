package com.chengke.chengkecrmbackend.modules.system.department.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * 部门树节点类型；集团是受保护根节点，普通部门必须具有父节点。
 */
public enum DepartmentNodeType {
    GROUP,
    DEPARTMENT;

    /**
     * 返回 PostgreSQL 使用的小写节点类型。
     *
     * @return group 或 department
     */
    @JsonValue
    public String databaseValue() {
        return name().toLowerCase();
    }

    /**
     * 解析不区分大小写的 API 或数据库节点类型。
     *
     * @param value group 或 department
     * @return 对应节点类型
     */
    @JsonCreator
    public static DepartmentNodeType fromValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
