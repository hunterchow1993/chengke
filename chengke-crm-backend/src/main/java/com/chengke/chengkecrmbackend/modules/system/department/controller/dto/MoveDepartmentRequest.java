package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** 移动部门执行 HTTP 请求体。 */
@Schema(description = "移动部门请求")
public record MoveDepartmentRequest(
        @NotNull UUID newParentId,
        @NotNull @Positive Integer version,
        @NotBlank String previewToken,
        @Size(max = 500) String reason
) {}
