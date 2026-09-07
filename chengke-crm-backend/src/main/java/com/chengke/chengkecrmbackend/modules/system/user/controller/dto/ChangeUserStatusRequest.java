package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 单个用户启停请求体。 */
@Schema(description = "启用或停用用户请求")
public record ChangeUserStatusRequest(
        @NotNull UserStatus targetStatus
) {
}
