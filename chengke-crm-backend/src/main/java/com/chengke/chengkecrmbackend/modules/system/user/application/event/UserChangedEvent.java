package com.chengke.chengkecrmbackend.modules.system.user.application.event;

import java.util.UUID;

/**
 * 事务内发布的用户变化事实。
 *
 * @param tenantId 租户标识
 * @param userId 目标用户；批量场景可为空
 * @param action 动作类型
 * @param affectsAuthorization 是否提升租户授权版本
 * @param affectsDepartmentTree 是否失效部门树相关缓存
 */
public record UserChangedEvent(
        UUID tenantId,
        UUID userId,
        String action,
        boolean affectsAuthorization,
        boolean affectsDepartmentTree
) {
}
