package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 用户列表项 HTTP 视图。 */
public record UserListItemVO(
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
