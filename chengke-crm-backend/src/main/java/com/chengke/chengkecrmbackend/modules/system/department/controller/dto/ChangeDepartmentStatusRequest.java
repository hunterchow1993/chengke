package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 启停执行 HTTP 请求体。 */
@Schema(description = "启停部门请求")
public record ChangeDepartmentStatusRequest(
        @NotNull DepartmentStatus targetStatus,
        boolean cascade,
        @NotNull @Positive Integer version,
        @NotBlank String previewToken,
        @Size(max = 500) String reason
) {}
