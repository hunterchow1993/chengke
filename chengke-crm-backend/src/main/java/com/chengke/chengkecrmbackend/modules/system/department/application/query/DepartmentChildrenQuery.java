package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import java.util.UUID;

/** 直接下级查询条件。 */
public record DepartmentChildrenQuery(CurrentActor actor, UUID parentId, boolean includeDisabled) {
}
