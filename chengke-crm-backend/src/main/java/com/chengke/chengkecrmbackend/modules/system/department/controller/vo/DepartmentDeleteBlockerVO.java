package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

/** 部门删除阻塞项。 */
public record DepartmentDeleteBlockerVO(String type, Integer count, String message, String nextAction) {}
