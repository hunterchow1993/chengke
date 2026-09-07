package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import java.util.List;

/** 部门搜索项及完整祖先路径。 */
public record DepartmentSearchItemResult(
        DepartmentTreeNodeResult department, List<DepartmentPathItem> path, String matchedField
) {
}
