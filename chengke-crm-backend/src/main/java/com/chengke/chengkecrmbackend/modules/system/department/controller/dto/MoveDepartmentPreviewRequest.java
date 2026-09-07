package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** 移动影响预览 HTTP 请求体。 */
@Schema(description = "移动影响预览请求")
public record MoveDepartmentPreviewRequest(
        @NotNull UUID newParentId,
        @NotNull @Positive Integer version
) {}
