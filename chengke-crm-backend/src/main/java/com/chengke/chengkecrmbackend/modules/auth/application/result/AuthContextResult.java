package com.chengke.chengkecrmbackend.modules.auth.application.result;

import java.util.List;
import java.util.Map;

/**
 * 授权上下文。含 Spec 字段与前端当前 Zod 契约所需别名。
 */
public record AuthContextResult(
        User user,
        Tenant tenant,
        Role role,
        List<MenuNodeResult> menus,
        List<String> permissionCodes,
        Map<String, String> dataScope,
        String homeRoute,
        String permissionVersion,
        String expiresAt
) {
    public record User(String id, String name, String avatarUrl) {
        public String displayName() {
            return name;
        }
    }

    public record Tenant(String id, String name) {
    }

    public record Role(String id, String name) {
    }

    public String authorizedHome() {
        return homeRoute;
    }

    public List<String> dataScopes() {
        return dataScope == null ? List.of() : List.copyOf(dataScope.values());
    }

    public List<String> sensitiveFieldPermissions() {
        return permissionCodes.stream().filter(code -> code.endsWith(":full")).toList();
    }
}
