package com.chengke.chengkecrmbackend.shared.security;

import java.util.Set;
import java.util.UUID;

/**
 * 已认证操作者的服务端安全上下文。
 *
 * @param tenantId JWT tenant_id Claim 中的租户标识
 * @param actorId JWT sub Claim 中的操作者标识
 * @param permissions 已授予的功能权限
 * @param manageableDepartmentIds 可管理的部门范围
 * @param manageAllDepartments 是否具有租户内全量部门范围
 * @param requestId 当前请求追踪标识
 */
public record CurrentActor(
        UUID tenantId,
        UUID actorId,
        Set<String> permissions,
        Set<UUID> manageableDepartmentIds,
        boolean manageAllDepartments,
        String requestId
) {
    /**
     * 判断操作者是否拥有指定功能权限。
     *
     * @param permission 稳定权限标识
     * @return 拥有权限时为 true
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * 判断部门是否位于操作者最新的数据管理范围内。
     *
     * @param departmentId 部门标识
     * @return 具有全量范围或部门在可管理集合中时为 true
     */
    public boolean canManage(UUID departmentId) {
        return manageAllDepartments || manageableDepartmentIds.contains(departmentId);
    }
}
