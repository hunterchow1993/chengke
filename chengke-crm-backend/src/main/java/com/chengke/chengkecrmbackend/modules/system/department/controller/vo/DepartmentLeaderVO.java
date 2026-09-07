package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.UUID;
/** 部门负责人展示摘要。 */
public record DepartmentLeaderVO(UUID userId, String displayName, String username, String avatarUrl) {}
