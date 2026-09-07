package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.UUID;
/** 部门祖先路径项。 */
public record DepartmentPathItemVO(UUID id, String name, boolean readOnlyAncestor) {}
