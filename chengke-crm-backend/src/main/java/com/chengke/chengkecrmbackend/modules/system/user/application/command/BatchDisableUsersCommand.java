package com.chengke.chengkecrmbackend.modules.system.user.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.List;
import java.util.UUID;

/**
 * 批量停用用户命令。
 */
public record BatchDisableUsersCommand(
        CurrentActor actor,
        List<UUID> userIds
) {
}
