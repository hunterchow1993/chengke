package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import java.util.List;

/** 部门详情应用结果，包含最新统计、路径和节点能力。 */
public record DepartmentDetailResult(
        DepartmentRecord department, List<DepartmentPathItem> path, DepartmentImpactCounts counts,
        DepartmentCapabilitiesResult capabilities
) {
}
