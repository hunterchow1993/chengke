package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** 手机号可用性查询参数。 */
@Schema(description = "手机号可用性查询")
public record MobileAvailableRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String mobile,
        UUID excludeUserId
) {
}
