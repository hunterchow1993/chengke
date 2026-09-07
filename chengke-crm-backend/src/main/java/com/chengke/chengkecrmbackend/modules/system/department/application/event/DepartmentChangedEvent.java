package com.chengke.chengkecrmbackend.modules.system.department.application.event;

import java.util.UUID;

/**
 * 事务内发布的部门变化事实。
 *
 * @param tenantId 租户标识
 * @param departmentId 部门标识
 * @param action 动作类型
 * @param affectsAuthorization 是否需要提升相关授权版本
 */
public record DepartmentChangedEvent(
        UUID tenantId,
        UUID departmentId,
        String action,
        boolean affectsAuthorization
) {
}
