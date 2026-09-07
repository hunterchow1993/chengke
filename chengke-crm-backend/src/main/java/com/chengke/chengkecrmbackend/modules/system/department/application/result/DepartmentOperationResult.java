package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 部门写操作统一结果。
 *
 * @param departmentId 部门标识
 * @param version 操作后版本
 * @param status 操作后状态
 * @param updatedAt 操作完成时间
 * @param affectedDepartmentCount 影响部门数量
 * @param message 安全的结果说明
 */
public record DepartmentOperationResult(
        UUID departmentId,
        int version,
        DepartmentStatus status,
        OffsetDateTime updatedAt,
        int affectedDepartmentCount,
        String message
) {
}
