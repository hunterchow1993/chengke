package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import java.util.UUID;

/** 部门树查询条件。 */
public record DepartmentTreeQuery(CurrentActor actor, UUID rootId, UUID selectedId) {
}
