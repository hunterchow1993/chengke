package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.time.OffsetDateTime;
import java.util.List;
/** 移动或启停影响预览响应。 */
public record DepartmentImpactPreviewVO(
 String previewToken, OffsetDateTime expiresAt, List<DepartmentPathItemVO> currentPath,
 List<DepartmentPathItemVO> targetPath, int affectedDepartmentCount, Integer affectedDirectUserCount,
 Integer affectedRoleCount, Integer affectedBusinessReferenceCount, boolean hasHiddenReferences,
 String riskLevel, boolean requiresConfirmation, List<String> messages
) {}
