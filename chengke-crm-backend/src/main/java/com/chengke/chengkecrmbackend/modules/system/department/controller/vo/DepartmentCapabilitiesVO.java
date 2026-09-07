package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.Map;
/** 当前操作者节点能力，供前端展示按钮与禁用原因。 */
public record DepartmentCapabilitiesVO(
 boolean canView, boolean canCreateChild, boolean canEdit, boolean canMove,
 boolean canChangeStatus, boolean canDelete, boolean canViewMembers, Map<String,String> disabledReasons
) {}
