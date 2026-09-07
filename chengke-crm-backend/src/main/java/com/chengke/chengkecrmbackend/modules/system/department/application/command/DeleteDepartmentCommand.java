package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/** 部门软删除命令。 */
public record DeleteDepartmentCommand(
        CurrentActor actor, UUID departmentId, int version, String previewToken, String reason
) {
}
