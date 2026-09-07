package com.chengke.chengkecrmbackend.modules.auth.application.port.model;

import java.util.UUID;

/**
 * 登录读取的账号凭据与状态，含密码散列。
 */
public record AuthUserRecord(
        UUID id,
        UUID tenantId,
        String name,
        String username,
        String mobile,
        String avatarUrl,
        String passwordHash,
        String status,
        boolean forcePasswordChange,
        UUID roleId,
        UUID departmentId
) {
}
