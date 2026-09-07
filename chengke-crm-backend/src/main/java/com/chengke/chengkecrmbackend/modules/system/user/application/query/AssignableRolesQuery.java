package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/**
 * 查询可分配角色选项。
 */
public record AssignableRolesQuery(CurrentActor actor) {
}
