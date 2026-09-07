package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** 用户名可用性查询参数。 */
@Schema(description = "用户名可用性查询")
public record UsernameAvailableRequest(
        @NotBlank @Size(max = 32) String username,
        UUID excludeUserId
) {
}
