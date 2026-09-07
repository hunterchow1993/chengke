package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
/** 部门详情响应，包含路径、统计、阻塞项、版本和最新节点能力。 */
public record DepartmentDetailVO(
 UUID id, UUID parentId, String name, String code, DepartmentNodeType nodeType, DepartmentStatus status,
 int depth, List<DepartmentPathItemVO> path, DepartmentLeaderVO leader, int sortOrder, String remark, int version,
 Integer directChildCount, Integer descendantCount, Integer roleReferenceCount, Integer businessReferenceCount,
 boolean hasHiddenReferences, List<DepartmentDeleteBlockerVO> deleteBlockers,
 OffsetDateTime createdAt, OffsetDateTime updatedAt, OperatorSummaryVO updatedBy,
 DepartmentCapabilitiesVO capabilities
) {}
