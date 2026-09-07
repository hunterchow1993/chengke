package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 分页查询用户。
 */
public record UserListQuery(
        CurrentActor actor,
        UUID departmentId,
        boolean includeDescendants,
        String keyword,
        UserStatus status,
        int page,
        int pageSize
) {
}
