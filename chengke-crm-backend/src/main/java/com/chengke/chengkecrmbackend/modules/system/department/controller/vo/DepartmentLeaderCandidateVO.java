package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.UUID;
/** 负责人候选用户响应。 */
public record DepartmentLeaderCandidateVO(
 UUID userId, String displayName, String username, String avatarUrl, String departmentPath
) {}
