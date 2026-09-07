package com.chengke.chengkecrmbackend.modules.system.user.application.command;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 编辑用户命令；不含用户名与密码。
 */
public record UpdateUserCommand(
        CurrentActor actor,
        UUID userId,
        String avatarUrl,
        String name,
        String mobile,
        String email,
        UUID departmentId,
        UUID roleId,
        UserStatus status,
        String remark,
        int version
) {
}
