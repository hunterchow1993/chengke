package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import java.util.UUID;

/**
 * 用户模块读取的部门摘要，用于范围、组织树和可分配选项。
 */
public record UserDepartmentRecord(
        UUID id,
        UUID parentId,
        String name,
        String nodeType,
        String status,
        int depth
) {
}
