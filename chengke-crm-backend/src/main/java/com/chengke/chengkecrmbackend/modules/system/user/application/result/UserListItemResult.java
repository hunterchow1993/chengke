package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 用户列表项。 */
public record UserListItemResult(
        UUID id,
        String name,
        String username,
        String avatarUrl,
        String mobile,
        UUID departmentId,
        String departmentName,
        UUID roleId,
        String roleName,
        UserStatus status,
        OffsetDateTime updatedAt
) {
}
