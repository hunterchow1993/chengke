package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 编辑部门可变基本信息的命令；不包含编码、类型、父节点和状态。
 */
public record UpdateDepartmentCommand(
        CurrentActor actor, UUID departmentId, String name, UUID leaderUserId,
        int sortOrder, String remark, int version, String reason
) {
}
