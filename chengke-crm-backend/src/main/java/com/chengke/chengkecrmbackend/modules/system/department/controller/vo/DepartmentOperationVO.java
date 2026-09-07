package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
/** 部门写操作统一响应。 */
public record DepartmentOperationVO(
 UUID departmentId, int version, DepartmentStatus status, OffsetDateTime updatedAt,
 int affectedDepartmentCount, String message
) {}
