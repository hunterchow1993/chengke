package com.chengke.chengkecrmbackend.modules.system.department.application.port.model;

import java.util.UUID;

/**
 * 负责人目录端口返回的同租户正常用户摘要。
 */
public record DepartmentLeaderCandidate(
        UUID userId, String displayName, String username, String avatarUrl, String departmentPath
) {
}
