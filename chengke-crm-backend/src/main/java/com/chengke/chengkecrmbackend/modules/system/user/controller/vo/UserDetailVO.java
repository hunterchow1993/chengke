package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 用户详情 HTTP 视图。 */
public record UserDetailVO(
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
