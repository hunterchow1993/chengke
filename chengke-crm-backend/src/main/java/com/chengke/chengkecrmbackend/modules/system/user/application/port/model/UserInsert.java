package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 新增用户时写入持久化端口的字段集合。
 */
public record UserInsert(
        UUID id,
        UUID tenantId,
        String name,
        String username,
        String passwordHash,
        String avatarUrl,
        String mobile,
        String email,
        UUID departmentId,
        UUID roleId,
        UserStatus status,
        boolean forcePasswordChange,
        String remark,
        UUID actorId
) {
}
