package com.chengke.chengkecrmbackend.modules.auth.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 首次登录修改密码。
 */
public record ChangeFirstPasswordCommand(
        CurrentActor actor,
        String newPassword,
        String confirmPassword,
        UUID currentSessionId,
        String ip,
        String userAgent
) {
}
