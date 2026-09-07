package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** 批量停用请求体。 */
@Schema(description = "批量停用用户请求")
public record BatchDisableUsersRequest(
        @NotEmpty @Size(max = 100) List<UUID> userIds
) {
}
