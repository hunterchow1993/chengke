package com.chengke.chengkecrmbackend.modules.system.department.application.port;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 部门应用层访问最新树状态和执行数据库原子移动函数的持久化端口。
 */
public interface DepartmentPersistencePort {

    /**
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @return 当前租户内未删除部门；不存在或跨租户时为空
     */
    Optional<DepartmentRecord> findById(UUID tenantId, UUID departmentId);

    /**
     * @param tenantId 租户标识
     * @return 当前租户全部未删除部门；应用层负责数据范围裁剪
     */
    List<DepartmentRecord> findAll(UUID tenantId);

    /**
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @return 从集团根节点到目标部门的有序路径
     */
    List<DepartmentPathItem> findPath(UUID tenantId, UUID departmentId);

    /**
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @return 后代、成员、角色和业务引用的最新影响计数
     */
    DepartmentImpactCounts countImpact(UUID tenantId, UUID departmentId);

    /**
     * @param tenantId 租户标识
     * @param ancestorId 祖先标识
     * @param possibleDescendantId 候选后代标识
     * @return 是否为后代
     */
    boolean isDescendant(UUID tenantId, UUID ancestorId, UUID possibleDescendantId);

    /**
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @return 部门到其最深后代的距离
     */
    int subtreeHeight(UUID tenantId, UUID departmentId);

    /**
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @return 节点是否存在任一停用祖先
     */
    boolean hasDisabledAncestor(UUID tenantId, UUID departmentId);

    /**
     * 原子移动部门子树并写审计。
     *
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @param newParentId 新父节点
     * @param version 预期版本
     * @param actorId 操作者
     * @param reason 原因
     * @return 移动节点的新版本
     */
    int move(UUID tenantId, UUID departmentId, UUID newParentId, int version, UUID actorId, String reason);

    /**
     * 调用 PostgreSQL 创建函数。
     *
     * @param tenantId 租户标识
     * @param parentId 父节点
     * @param name 名称
     * @param code 编码
     * @param nodeType 节点类型
     * @param leaderUserId 负责人
     * @param sortOrder 排序
     * @param status 状态
     * @param remark 备注
     * @param actorId 操作者
     * @return PostgreSQL 创建函数生成的部门标识
     */
    UUID create(UUID tenantId, UUID parentId, String name, String code, DepartmentNodeType nodeType,
                UUID leaderUserId, int sortOrder, DepartmentStatus status, String remark, UUID actorId);

    /**
     * 调用 PostgreSQL 更新函数。
     *
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @param name 名称
     * @param leaderUserId 负责人
     * @param sortOrder 排序
     * @param remark 备注
     * @param version 预期版本
     * @param actorId 操作者
     * @param reason 原因
     * @return PostgreSQL 更新函数返回的新版本
     */
    int update(UUID tenantId, UUID departmentId, String name, UUID leaderUserId, int sortOrder,
               String remark, int version, UUID actorId, String reason);

    /**
     * 调用 PostgreSQL 状态函数。
     *
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @param status 目标状态
     * @param cascade 是否级联
     * @param version 预期版本
     * @param actorId 操作者
     * @param reason 原因
     * @return PostgreSQL 状态函数返回的受影响部门数
     */
    int changeStatus(UUID tenantId, UUID departmentId, DepartmentStatus status, boolean cascade,
                     int version, UUID actorId, String reason);

    /**
     * 调用 PostgreSQL 软删除函数。
     *
     * @param tenantId 租户标识
     * @param departmentId 部门标识
     * @param version 预期版本
     * @param actorId 操作者
     * @param reason 原因
     * @return PostgreSQL 软删除函数返回的新版本
     */
    int softDelete(UUID tenantId, UUID departmentId, int version, UUID actorId, String reason);
}
