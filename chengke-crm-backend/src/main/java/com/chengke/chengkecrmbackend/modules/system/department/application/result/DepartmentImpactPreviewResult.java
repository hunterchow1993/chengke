package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 移动或启停操作的确认预览。
 */
public record DepartmentImpactPreviewResult(
        String previewToken,
        OffsetDateTime expiresAt,
        List<DepartmentPathItem> currentPath,
        List<DepartmentPathItem> targetPath,
        int affectedDepartmentCount,
        Integer affectedDirectUserCount,
        Integer affectedRoleCount,
        Integer affectedBusinessReferenceCount,
        boolean hasHiddenReferences,
        String riskLevel,
        boolean requiresConfirmation,
        List<String> messages
) {
}
