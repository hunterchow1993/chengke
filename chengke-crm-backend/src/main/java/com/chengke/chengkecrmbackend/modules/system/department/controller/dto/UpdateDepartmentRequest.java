package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** 编辑部门 HTTP 请求体，只包含允许修改的字段。 */
@Schema(description = "编辑部门请求")
public record UpdateDepartmentRequest(
        @NotBlank @Size(min = 2, max = 50) String name,
        UUID leaderUserId,
        @NotNull @Min(0) @Max(9999) Integer sortOrder,
        @Size(max = 200) String remark,
        @NotNull @Positive Integer version,
        @Size(max = 500) String reason
) {}
