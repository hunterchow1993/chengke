package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 可移动父节点 HTTP 查询参数。 */
@Schema(description = "可移动父节点查询参数")
public record DepartmentMovableParentRequest(
        @Size(max = 50) String keyword,
        @Min(1) @Max(100) @Schema(defaultValue = "50") Integer limit
) {
    /** 统一候选数量缺省值。 */
    public DepartmentMovableParentRequest {
        limit = limit == null ? 50 : limit;
    }
}
