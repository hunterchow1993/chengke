package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户持久化端口返回的框架无关记录，不含密码凭据。
 */
public record UserRecord(
        UUID id,
        UUID tenantId,
        String name,
        String username,
        String avatarUrl,
        String mobile,
        String email,
        UUID departmentId,
        String departmentName,
        UUID roleId,
        String roleName,
        String roleCode,
        boolean roleBuiltIn,
        UserStatus status,
        boolean forcePasswordChange,
        String remark,
        int version,
        UUID updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
