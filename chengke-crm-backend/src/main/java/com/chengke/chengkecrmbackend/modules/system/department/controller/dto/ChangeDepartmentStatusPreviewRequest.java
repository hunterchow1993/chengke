package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 启停影响预览 HTTP 请求体。 */
@Schema(description = "启停影响预览请求")
public record ChangeDepartmentStatusPreviewRequest(
        @NotNull DepartmentStatus targetStatus,
        boolean cascade,
        @NotNull @Positive Integer version
) {}
