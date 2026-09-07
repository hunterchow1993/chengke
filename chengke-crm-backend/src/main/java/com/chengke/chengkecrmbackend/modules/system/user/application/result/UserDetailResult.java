package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 用户详情，不含密码与强制改密开关。 */
public record UserDetailResult(
        UUID id,
        String name,
        String username,
        String avatarUrl,
        String mobile,
        String email,
        UUID departmentId,
        String departmentName,
        UUID roleId,
        String roleName,
        UserStatus status,
        String remark,
        int version,
        OffsetDateTime updatedAt
) {
}
