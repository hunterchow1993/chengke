package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.List;
/** 部门搜索结果项。 */
public record DepartmentSearchItemVO(
 DepartmentTreeNodeVO department, List<DepartmentPathItemVO> path, String matchedField
) {}
