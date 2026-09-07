package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/** 部门启停影响预览命令。 */
public record ChangeDepartmentStatusPreviewCommand(
        CurrentActor actor, UUID departmentId, DepartmentStatus targetStatus, boolean cascade, int version
) {
}
