package com.chengke.chengkecrmbackend.modules.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 首次登录改密请求。
 */
public record FirstPasswordChangeRequest(
        @NotBlank @Size(min = 8, max = 32) String newPassword,
        String confirmPassword
) {
}
