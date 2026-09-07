package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import java.util.Map;

/**
 * 当前操作者对一个部门节点的展示能力；写接口仍会重新鉴权。
 */
public record DepartmentCapabilitiesResult(
        boolean canView, boolean canCreateChild, boolean canEdit, boolean canMove,
        boolean canChangeStatus, boolean canDelete, boolean canViewMembers,
        Map<String, String> disabledReasons
) {
}
