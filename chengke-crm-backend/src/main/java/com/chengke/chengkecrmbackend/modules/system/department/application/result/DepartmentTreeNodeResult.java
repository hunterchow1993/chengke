package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import java.util.UUID;

/** 部门树节点应用结果。 */
public record DepartmentTreeNodeResult(
        UUID id, UUID parentId, String name, String code, DepartmentNodeType nodeType,
        DepartmentStatus status, int depth, boolean hasChildren, Integer directMemberCount,
        boolean readOnlyAncestor, DepartmentCapabilitiesResult capabilities
) {
}
