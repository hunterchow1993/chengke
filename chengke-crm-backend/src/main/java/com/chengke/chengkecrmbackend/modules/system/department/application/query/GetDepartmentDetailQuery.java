package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import java.util.UUID;

/** 部门详情查询条件。 */
public record GetDepartmentDetailQuery(CurrentActor actor, UUID departmentId) {
}
