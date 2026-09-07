package com.chengke.chengkecrmbackend.modules.system.department.application.command;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 移动部门用例输入。
 *
 * @param actor 服务端认证与数据范围上下文
 * @param departmentId 待移动部门
 * @param newParentId 新父部门
 * @param version 预期版本
 * @param previewToken 一次性预览令牌
 * @param reason 操作原因
 */
public record MoveDepartmentCommand(
        CurrentActor actor,
        UUID departmentId,
        UUID newParentId,
        int version,
        String previewToken,
        String reason
) {
}
