package com.chengke.chengkecrmbackend.modules.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体。
 */
public record LoginRequest(
        @NotBlank @Size(max = 32) String account,
        @NotBlank @Size(min = 8, max = 32) String password,
        Boolean rememberMe,
        @Size(min = 4, max = 6) String captchaCode,
        String captchaToken
) {
    public boolean rememberMeOrFalse() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
