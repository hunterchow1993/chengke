package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import java.util.UUID;

/** 可移动父节点查询条件。 */
public record MovableParentQuery(CurrentActor actor, UUID departmentId, String keyword, int limit) {
}
