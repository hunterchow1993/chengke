package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 部门搜索 HTTP 查询参数。 */
@Schema(description = "部门分页搜索参数")
public record DepartmentSearchRequest(
        @NotBlank @Size(max = 50) @Schema(description = "名称或编码关键词", requiredMode = Schema.RequiredMode.REQUIRED) String keyword,
        @Min(1) @Schema(defaultValue = "1") Integer page,
        @Min(1) @Max(100) @Schema(defaultValue = "20") Integer pageSize
) {
    /** 统一分页缺省值。 */
    public DepartmentSearchRequest {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
    }
}
