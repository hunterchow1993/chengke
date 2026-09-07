package com.chengke.chengkecrmbackend.modules.system.user.application.command;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 新增用户命令。
 */
public record CreateUserCommand(
        CurrentActor actor,
        String avatarUrl,
        String name,
        String username,
        String mobile,
        String email,
        UUID departmentId,
        UUID roleId,
        UserStatus status,
        String initialPassword,
        boolean forcePasswordChange,
        String remark
) {
}
