package com.chengke.chengkecrmbackend.modules.auth.application;

import java.util.List;

/**
 * 角色管理未落地前，超级管理员使用的功能权限全集。
 */
public final class PermissionCatalog {
    public static final List<String> ALL = List.of(
            "system:department:view",
            "system:department:create",
            "system:department:update",
            "system:department:move",
            "system:department:status",
            "system:department:delete",
            "system:department:member:view",
            "system:department:manage:all",
            "system:user:view",
            "system:user:create",
            "system:user:update",
            "system:user:disable",
            "system:user:reset-password",
            "system:user:mobile:full",
            "system:user:email:full"
    );

    private PermissionCatalog() {
    }
}
