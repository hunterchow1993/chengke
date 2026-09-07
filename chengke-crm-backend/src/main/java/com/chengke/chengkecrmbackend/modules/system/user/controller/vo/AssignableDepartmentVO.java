package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.util.UUID;

/** 可分配部门选项。 */
public record AssignableDepartmentVO(UUID id, UUID parentId, String name, String nodeType) {
}
