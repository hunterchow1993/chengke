package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/**
 * 查询可分配部门选项。
 */
public record AssignableDepartmentsQuery(CurrentActor actor) {
}
