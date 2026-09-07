package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/** 移动影响预览命令。 */
public record MoveDepartmentPreviewCommand(
        CurrentActor actor, UUID departmentId, UUID newParentId, int version
) {
}
