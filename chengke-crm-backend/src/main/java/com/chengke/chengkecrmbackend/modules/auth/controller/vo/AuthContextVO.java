package com.chengke.chengkecrmbackend.modules.auth.controller.vo;

import java.util.List;
import java.util.Map;

/**
 * 授权上下文 HTTP 视图，同时包含 Spec 字段与前端当前 Zod 所需别名。
 */
public record AuthContextVO(
        UserVO user,
        TenantVO tenant,
        RoleVO role,
        List<MenuNodeVO> menus,
        List<String> permissionCodes,
        Map<String, String> dataScope,
        String homeRoute,
        String permissionVersion,
        String expiresAt,
        String authorizedHome,
        List<String> dataScopes,
        List<String> sensitiveFieldPermissions
) {
    public record UserVO(String id, String name, String displayName, String avatarUrl) {
    }

    public record TenantVO(String id, String name) {
    }

    public record RoleVO(String id, String name) {
    }

    public record MenuNodeVO(
            String id,
            String parentId,
            String type,
            String name,
            String label,
            String routeName,
            String routeKey,
            String path,
            String iconKey,
            String icon,
            String permissionCode,
            int order,
            List<MenuNodeVO> children
    ) {
    }
}
