package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 软删除部门 HTTP 查询参数。 */
@Schema(description = "软删除部门参数")
public record DeleteDepartmentRequest(
        @NotNull @Positive Integer version,
        String previewToken,
        @Size(max = 500) String reason
) {}
