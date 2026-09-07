package com.chengke.chengkecrmbackend.modules.system.user.application.command;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 单个用户启停命令。
 */
public record ChangeUserStatusCommand(
        CurrentActor actor,
        UUID userId,
        UserStatus targetStatus
) {
}
