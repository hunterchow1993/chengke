package com.chengke.chengkecrmbackend.modules.system.user.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 重置用户密码命令。
 */
public record ResetUserPasswordCommand(
        CurrentActor actor,
        UUID userId,
        String newPassword,
        boolean forcePasswordChange
) {
}
