package com.chengke.chengkecrmbackend.modules.system.department.application.port.model;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeSnapshot;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 部门持久化端口返回的框架无关记录。
 *
 * @param id 部门标识
 * @param tenantId 租户标识
 * @param parentId 父节点标识
 * @param name 部门名称
 * @param code 部门编码
 * @param nodeType 节点类型
 * @param leaderUserId 负责人用户标识
 * @param sortOrder 同级排序
 * @param status 部门状态
 * @param remark 备注
 * @param depth 节点深度
 * @param version 乐观锁版本
 * @param updatedBy 最近更新操作者标识
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record DepartmentRecord(
        UUID id,
        UUID tenantId,
        UUID parentId,
        String name,
        String code,
        DepartmentNodeType nodeType,
        UUID leaderUserId,
        int sortOrder,
        DepartmentStatus status,
        String remark,
        int depth,
        int version,
        UUID updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 转换为纯领域规则所需的最小快照。
     *
     * @return 不包含展示字段的节点快照
     */
    public DepartmentNodeSnapshot toSnapshot() {
        return new DepartmentNodeSnapshot(id, tenantId, parentId, nodeType, status, depth, version);
    }
}
