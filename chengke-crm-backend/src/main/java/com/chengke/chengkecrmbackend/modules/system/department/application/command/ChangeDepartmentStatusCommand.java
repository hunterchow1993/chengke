package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/** 部门启用或停用执行命令。 */
public record ChangeDepartmentStatusCommand(
        CurrentActor actor, UUID departmentId, DepartmentStatus targetStatus, boolean cascade,
        int version, String previewToken, String reason
) {
}
