package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 部门直接下级 HTTP 查询参数。 */
@Schema(description = "部门下级查询参数")
public record DepartmentChildrenRequest(
        @Schema(description = "是否包含停用节点", defaultValue = "false") Boolean includeDisabled
) {
    /** 统一缺省值。 */
    public DepartmentChildrenRequest {
        includeDisabled = includeDisabled != null && includeDisabled;
    }
}
