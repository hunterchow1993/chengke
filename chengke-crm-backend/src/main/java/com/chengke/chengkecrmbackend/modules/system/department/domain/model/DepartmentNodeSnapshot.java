package com.chengke.chengkecrmbackend.modules.system.department.domain.model;

import java.util.UUID;

/**
 * 领域策略执行时使用的不可变部门快照，不携带 Controller 或持久化框架语义。
 *
 * @param id 部门标识
 * @param tenantId 所属租户标识
 * @param parentId 上级部门标识，集团根节点为空
 * @param nodeType 节点类型
 * @param status 当前状态
 * @param depth 当前深度，集团根节点为 0
 * @param version 当前乐观锁版本
 */
public record DepartmentNodeSnapshot(
        UUID id,
        UUID tenantId,
        UUID parentId,
        DepartmentNodeType nodeType,
        DepartmentStatus status,
        int depth,
        int version
) {
}
