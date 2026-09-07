package com.chengke.chengkecrmbackend.modules.auth.application.port.model;

import java.util.UUID;

/**
 * 登录校验使用的角色状态摘要。
 */
public record AuthRoleRecord(
        UUID id,
        String name,
        String code,
        String status,
        boolean builtIn
) {
    /** @return 内置超级管理员 */
    public boolean superAdmin() {
        return builtIn && "super_admin".equals(code);
    }

    /** @return 角色可用于创建会话 */
    public boolean usable() {
        return "active".equals(status);
    }
}
