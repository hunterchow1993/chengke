package com.chengke.chengkecrmbackend.modules.system.department.application.port.model;

import java.util.UUID;

/**
 * 持久化端口返回的部门祖先路径项。
 *
 * @param id 节点标识
 * @param name 节点名称
 * @param depth 节点深度
 */
public record DepartmentPathItem(UUID id, String name, int depth) {
}
