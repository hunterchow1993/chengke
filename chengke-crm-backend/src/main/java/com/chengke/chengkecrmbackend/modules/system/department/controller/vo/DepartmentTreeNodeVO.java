package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.*;
import java.util.UUID;
/** 部门树节点响应。 */
public record DepartmentTreeNodeVO(
 UUID id, UUID parentId, String name, String code, DepartmentNodeType nodeType, DepartmentStatus status,
 int depth, boolean hasChildren, Integer directMemberCount, boolean readOnlyAncestor,
 DepartmentCapabilitiesVO capabilities
) {}
