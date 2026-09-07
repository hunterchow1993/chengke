package com.chengke.chengkecrmbackend.modules.system.department.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 一次性影响预览令牌绑定内容。
 *
 * @param tenantId 租户标识
 * @param actorId 操作者标识
 * @param departmentId 目标部门标识
 * @param action 动作类型
 * @param target 目标父部门或目标状态
 * @param version 预览时部门版本
 * @param cascade 是否级联
 * @param expiresAt 失效时刻
 */
public record PreviewTokenBinding(
        UUID tenantId,
        UUID actorId,
        UUID departmentId,
        String action,
        String target,
        int version,
        boolean cascade,
        Instant expiresAt
) {
    /**
     * 比较执行请求与预览内容是否完全一致。
     *
     * @param expected 执行请求构造的预期绑定
     * @return 除失效时间外所有安全字段一致时为 true
     */
    public boolean matches(PreviewTokenBinding expected) {
        return tenantId.equals(expected.tenantId)
                && actorId.equals(expected.actorId)
                && departmentId.equals(expected.departmentId)
                && action.equals(expected.action)
                && target.equals(expected.target)
                && version == expected.version
                && cascade == expected.cascade;
    }
}
