package com.chengke.chengkecrmbackend.modules.auth.application.command;

/**
 * 登录提交。
 */
public record LoginCommand(
        String account,
        String password,
        boolean rememberMe,
        String captchaCode,
        String captchaToken,
        String ip,
        String userAgent
) {
}
