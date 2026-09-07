package com.chengke.chengkecrmbackend.modules.system.department.application.port.model;

/**
 * 部门变更影响和删除阻塞计数。
 *
 * @param descendantCount 后代部门数量
 * @param directMemberCount 直属成员数量
 * @param roleReferenceCount 角色引用数量
 * @param businessReferenceCount 受保护业务引用数量
 * @param hasHiddenReferences 是否包含当前操作者不可查看的引用
 */
public record DepartmentImpactCounts(
        int descendantCount,
        int directMemberCount,
        int roleReferenceCount,
        int businessReferenceCount,
        boolean hasHiddenReferences
) {
}
