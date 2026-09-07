package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import java.util.UUID;

/**
 * 用户模块读取的角色摘要，用于可授予校验与超级管理员识别。
 */
public record RoleRecord(
        UUID id,
        String name,
        String code,
        String status,
        boolean builtIn
) {
    /**
     * 判断是否为内置超级管理员角色。
     *
     * @return 编码为 super_admin 且内置时为 true
     */
    public boolean superAdmin() {
        return builtIn && "super_admin".equals(code);
    }
}
