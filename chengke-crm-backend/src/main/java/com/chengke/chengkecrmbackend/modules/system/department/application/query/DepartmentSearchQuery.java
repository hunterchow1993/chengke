package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/** 部门名称或编码分页搜索条件。 */
public record DepartmentSearchQuery(CurrentActor actor, String keyword, int page, int pageSize) {
}
