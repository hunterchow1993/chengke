package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 新增部门命令。
 *
 * @param actor 服务端认证上下文
 * @param parentId 上级部门；创建集团根节点时为空
 * @param name 名称
 * @param code 唯一编码
 * @param leaderUserId 负责人用户
 * @param sortOrder 排序值
 * @param status 初始状态
 * @param remark 备注
 */
public record CreateDepartmentCommand(
        CurrentActor actor, UUID parentId, String name, String code, UUID leaderUserId,
        int sortOrder, DepartmentStatus status, String remark
) {
}
