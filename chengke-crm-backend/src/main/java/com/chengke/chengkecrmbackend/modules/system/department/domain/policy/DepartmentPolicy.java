package com.chengke.chengkecrmbackend.modules.system.department.domain.policy;

import com.chengke.chengkecrmbackend.modules.system.department.domain.exception.DepartmentDomainException;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeSnapshot;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * 集中执行部门根保护、父状态、无环、深度、启停和删除阻塞规则。
 *
 * <p>该策略无数据库与框架副作用；调用方仍需在事务中读取最新快照并由数据库函数做最终校验。</p>
 */
public final class DepartmentPolicy {

    private final int maximumDepth;

    /**
     * 创建部门规则策略。
     *
     * @param maximumDepth 允许的最大节点深度，根节点深度为 0
     */
    public DepartmentPolicy(int maximumDepth) {
        if (maximumDepth < 1) {
            throw new IllegalArgumentException("maximumDepth must be positive");
        }
        this.maximumDepth = maximumDepth;
    }

    /**
     * 校验新增或移入目标父节点的状态。
     *
     * @param parentStatus 目标父节点当前状态
     * @throws DepartmentDomainException 父节点停用时抛出
     */
    public void validateParent(DepartmentStatus parentStatus) {
        if (parentStatus != DepartmentStatus.ACTIVE) {
            throw new DepartmentDomainException("DEPARTMENT_PARENT_DISABLED");
        }
    }

    /**
     * 校验移动是否满足根保护、无环和整棵子树最大深度限制。
     *
     * @param source 被移动节点的最新快照
     * @param newParentId 新父节点标识
     * @param newParentDepth 新父节点深度
     * @param subtreeHeight 被移动节点到其最深后代的距离
     * @param targetIsDescendant 新父节点是否为被移动节点的后代
     * @throws DepartmentDomainException 移动违反部门树规则时抛出
     */
    public void validateMove(
            DepartmentNodeSnapshot source,
            UUID newParentId,
            int newParentDepth,
            int subtreeHeight,
            boolean targetIsDescendant
    ) {
        requireMutableNode(source);
        if (Objects.equals(source.id(), newParentId) || targetIsDescendant) {
            throw new DepartmentDomainException("DEPARTMENT_MOVE_CYCLE");
        }
        if (newParentDepth + 1 + subtreeHeight > maximumDepth) {
            throw new DepartmentDomainException("DEPARTMENT_PARENT_INVALID");
        }
    }

    /**
     * 校验部门启用或停用动作。
     *
     * @param source 目标节点最新快照
     * @param targetStatus 目标状态
     * @param cascade 是否级联处理后代
     * @param hasActiveDescendants 是否存在正常后代
     * @param hasDisabledAncestor 是否存在停用祖先
     * @throws DepartmentDomainException 状态变化违反根保护或祖先/后代规则时抛出
     */
    public void validateStatusChange(
            DepartmentNodeSnapshot source,
            DepartmentStatus targetStatus,
            boolean cascade,
            boolean hasActiveDescendants,
            boolean hasDisabledAncestor
    ) {
        requireMutableNode(source);
        if (targetStatus == DepartmentStatus.DISABLED && hasActiveDescendants && !cascade) {
            throw new DepartmentDomainException("DEPARTMENT_ACTIVE_DESCENDANTS");
        }
        if (targetStatus == DepartmentStatus.ACTIVE && hasDisabledAncestor) {
            throw new DepartmentDomainException("DEPARTMENT_PARENT_DISABLED");
        }
    }

    /**
     * 校验软删除是否受到下级、成员、角色或业务引用阻塞。
     *
     * @param source 目标节点最新快照
     * @param childCount 直接下级数量
     * @param memberCount 直属成员数量
     * @param roleReferenceCount 角色引用数量
     * @param businessReferenceCount 受保护业务引用数量
     * @throws DepartmentDomainException 根节点或任一引用阻塞删除时抛出
     */
    public void validateDelete(
            DepartmentNodeSnapshot source,
            int childCount,
            int memberCount,
            int roleReferenceCount,
            int businessReferenceCount
    ) {
        requireMutableNode(source);
        if (childCount > 0 || memberCount > 0 || roleReferenceCount > 0 || businessReferenceCount > 0) {
            throw new DepartmentDomainException("DEPARTMENT_DELETE_BLOCKED");
        }
    }

    /**
     * 拒绝对集团根节点执行结构或生命周期写操作。
     *
     * @param source 待变更节点
     * @throws DepartmentDomainException 节点为集团根节点时抛出
     */
    private void requireMutableNode(DepartmentNodeSnapshot source) {
        if (source.nodeType() == DepartmentNodeType.GROUP) {
            throw new DepartmentDomainException("DEPARTMENT_ROOT_PROTECTED");
        }
    }
}
