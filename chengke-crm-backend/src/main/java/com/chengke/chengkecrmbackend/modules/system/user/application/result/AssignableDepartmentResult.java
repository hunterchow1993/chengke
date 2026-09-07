package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import java.util.UUID;

/** 可分配部门选项。 */
public record AssignableDepartmentResult(UUID id, UUID parentId, String name, String nodeType) {
}
