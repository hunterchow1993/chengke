package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** 部门树 HTTP 查询参数。 */
@Schema(description = "部门树查询参数")
public record DepartmentTreeRequest(
        @Schema(description = "可选局部根节点") UUID rootId,
        @Schema(description = "需要定位的选中节点") UUID selectedId
) {}
