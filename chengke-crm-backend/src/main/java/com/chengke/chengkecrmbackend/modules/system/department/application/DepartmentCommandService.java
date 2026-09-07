package com.chengke.chengkecrmbackend.modules.system.department.application;

import com.chengke.chengkecrmbackend.modules.system.department.application.command.ChangeDepartmentStatusCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.ChangeDepartmentStatusPreviewCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.CreateDepartmentCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.DeleteDepartmentCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.MoveDepartmentCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.MoveDepartmentPreviewCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.command.UpdateDepartmentCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.event.DepartmentChangedEvent;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentChangeEventPublisher;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPreviewTokenStore;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.PreviewTokenBinding;
import com.chengke.chengkecrmbackend.modules.system.department.application.result.DepartmentImpactPreviewResult;
import com.chengke.chengkecrmbackend.modules.system.department.application.result.DepartmentOperationResult;
import com.chengke.chengkecrmbackend.modules.system.department.domain.policy.DepartmentPolicy;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 编排部门写用例，负责 fail-closed 权限、最新树规则、预览消费、数据库函数和事务事件。
 */
@Service
public class DepartmentCommandService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(5);
    private final DepartmentPersistencePort persistence;
    private final DepartmentPreviewTokenStore previewTokenStore;
    private final DepartmentChangeEventPublisher eventPublisher;
    private final DepartmentPolicy policy;
    private final Clock clock;

    /**
     * 创建部门写应用服务。
     *
     * @param persistence 部门持久化端口
     * @param previewTokenStore 一次性预览令牌存储
     * @param eventPublisher 事务内变化事件发布端口
     * @param policy 部门领域策略
     * @param clock 可测试时钟
     */
    public DepartmentCommandService(
            DepartmentPersistencePort persistence,
            DepartmentPreviewTokenStore previewTokenStore,
            DepartmentChangeEventPublisher eventPublisher,
            DepartmentPolicy policy,
            Clock clock
    ) {
        this.persistence = persistence;
        this.previewTokenStore = previewTokenStore;
        this.eventPublisher = eventPublisher;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * 新增集团根节点或普通部门。
     *
     * <p>副作用：调用 PostgreSQL 创建函数写节点、闭包和审计，并发布提交后缓存失效事件。</p>
     *
     * @param command 新增命令
     * @return 新部门标识、初始版本和状态
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentOperationResult create(CreateDepartmentCommand command) {
        requirePermission(command.actor().hasPermission("system:department:create"));
        DepartmentNodeType nodeType;
        if (command.parentId() == null) {
            if (!command.actor().manageAllDepartments()) {
                throw new BusinessException("DEPARTMENT_OPERATION_FORBIDDEN", 403, "仅租户级管理员可创建集团根节点");
            }
            nodeType = DepartmentNodeType.GROUP;
        } else {
            requireManageable(command.actor().canManage(command.parentId()));
            var parent = persistence.findById(command.actor().tenantId(), command.parentId())
                    .orElseThrow(this::notFound);
            policy.validateParent(parent.status());
            if (parent.depth() + 1 > 10) {
                throw new BusinessException("DEPARTMENT_PARENT_INVALID", 422, "部门层级超过上限");
            }
            nodeType = DepartmentNodeType.DEPARTMENT;
        }
        var id = persistence.create(command.actor().tenantId(), command.parentId(), command.name(), command.code(),
                nodeType, command.leaderUserId(), command.sortOrder(), command.status(), command.remark(),
                command.actor().actorId());
        eventPublisher.publish(new DepartmentChangedEvent(command.actor().tenantId(), id, "create", false));
        return new DepartmentOperationResult(id, 1, command.status(), OffsetDateTime.now(clock), 1, "部门创建成功");
    }

    /**
     * 编辑部门名称、负责人、排序和备注。
     *
     * <p>副作用：调用 PostgreSQL 更新函数写节点和审计，成功事务中发布缓存失效事件。</p>
     *
     * @param command 编辑命令
     * @return 操作后的部门版本
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentOperationResult update(UpdateDepartmentCommand command) {
        requirePermission(command.actor().hasPermission("system:department:update"));
        var source = requireManageableNode(command.actor(), command.departmentId());
        requireVersion(source.version(), command.version());
        int newVersion = persistence.update(command.actor().tenantId(), source.id(), command.name(),
                command.leaderUserId(), command.sortOrder(), command.remark(), command.version(),
                command.actor().actorId(), command.reason());
        eventPublisher.publish(new DepartmentChangedEvent(command.actor().tenantId(), source.id(), "update", false));
        return new DepartmentOperationResult(source.id(), newVersion, source.status(),
                OffsetDateTime.now(clock), 1, "部门更新成功");
    }

    /**
     * 计算移动影响并签发五分钟一次性预览令牌。
     *
     * <p>副作用：仅写预览令牌存储，不修改部门、闭包或审计。</p>
     *
     * @param command 移动预览命令
     * @return 当前/目标路径、影响计数、风险和令牌
     */
    @Transactional(readOnly = true)
    public DepartmentImpactPreviewResult previewMove(MoveDepartmentPreviewCommand command) {
        requirePermission(command.actor().hasPermission("system:department:move"));
        var source = requireManageableNode(command.actor(), command.departmentId());
        var target = requireManageableNode(command.actor(), command.newParentId());
        requireVersion(source.version(), command.version());
        policy.validateParent(target.status());
        policy.validateMove(source.toSnapshot(), target.id(), target.depth(),
                persistence.subtreeHeight(command.actor().tenantId(), source.id()),
                persistence.isDescendant(command.actor().tenantId(), source.id(), target.id()));
        var counts = persistence.countImpact(command.actor().tenantId(), source.id());
        var expiresAt = clock.instant().plus(PREVIEW_TTL);
        var binding = new PreviewTokenBinding(command.actor().tenantId(), command.actor().actorId(), source.id(),
                "move", target.id().toString(), source.version(), false, expiresAt);
        var token = previewTokenStore.save(binding);
        return previewResult(token, expiresAt,
                persistence.findPath(command.actor().tenantId(), source.id()),
                persistence.findPath(command.actor().tenantId(), target.id()),
                counts, "移动将影响部门路径和数据权限");
    }

    /**
     * 校验并移动部门子树。
     *
     * <p>副作用：消费一次性预览令牌，调用 PostgreSQL 原子移动函数写节点、闭包和审计，
     * 并在当前事务中发布变化事件；缓存失效和授权版本提升由提交后监听器执行。</p>
     *
     * @param command 移动命令及服务端认证上下文
     * @return 操作后的版本、状态和影响数量
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentOperationResult move(MoveDepartmentCommand command) {
        requirePermission(command.actor().hasPermission("system:department:move"));
        requireManageable(command.actor().canManage(command.departmentId()));
        requireManageable(command.actor().canManage(command.newParentId()));

        var source = persistence.findById(command.actor().tenantId(), command.departmentId())
                .orElseThrow(this::notFound);
        var target = persistence.findById(command.actor().tenantId(), command.newParentId())
                .orElseThrow(this::notFound);
        if (source.version() != command.version()) {
            throw new BusinessException("DEPARTMENT_VERSION_CONFLICT", 409, "部门已被其他管理员修改");
        }
        policy.validateParent(target.status());
        policy.validateMove(source.toSnapshot(), target.id(), target.depth(),
                persistence.subtreeHeight(command.actor().tenantId(), source.id()),
                persistence.isDescendant(command.actor().tenantId(), source.id(), target.id()));
        var counts = persistence.countImpact(command.actor().tenantId(), source.id());

        var expected = new PreviewTokenBinding(command.actor().tenantId(), command.actor().actorId(),
                source.id(), "move", target.id().toString(), command.version(), false, clock.instant());
        validateAndConsumePreview(command.previewToken(), expected);

        int newVersion = persistence.move(command.actor().tenantId(), source.id(), target.id(),
                command.version(), command.actor().actorId(), command.reason());
        eventPublisher.publish(new DepartmentChangedEvent(command.actor().tenantId(), source.id(), "move", true));
        return new DepartmentOperationResult(source.id(), newVersion, source.status(),
                OffsetDateTime.now(clock), counts.descendantCount() + 1, "部门移动成功");
    }

    /**
     * 计算启停影响并签发五分钟一次性预览令牌。
     *
     * <p>副作用：仅写预览令牌存储，不修改业务事实。</p>
     *
     * @param command 启停预览命令
     * @return 影响计数、风险和令牌
     */
    @Transactional(readOnly = true)
    public DepartmentImpactPreviewResult previewStatus(ChangeDepartmentStatusPreviewCommand command) {
        requirePermission(command.actor().hasPermission("system:department:status"));
        var source = requireManageableNode(command.actor(), command.departmentId());
        requireVersion(source.version(), command.version());
        var counts = persistence.countImpact(command.actor().tenantId(), source.id());
        policy.validateStatusChange(source.toSnapshot(), command.targetStatus(), command.cascade(),
                counts.descendantCount() > 0, persistence.hasDisabledAncestor(command.actor().tenantId(), source.id()));
        var expiresAt = clock.instant().plus(PREVIEW_TTL);
        var binding = new PreviewTokenBinding(command.actor().tenantId(), command.actor().actorId(), source.id(),
                "status", command.targetStatus().databaseValue(), source.version(), command.cascade(), expiresAt);
        var token = previewTokenStore.save(binding);
        return previewResult(token, expiresAt, persistence.findPath(command.actor().tenantId(), source.id()),
                List.of(), counts, "状态变化可能影响登录、数据范围和业务操作");
    }

    /**
     * 执行部门启用或停用。
     *
     * <p>副作用：消费预览令牌，调用 PostgreSQL 状态函数写节点和审计，发布需要授权版本提升的事件。</p>
     *
     * @param command 启停执行命令
     * @return 操作后的状态与影响数量
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentOperationResult changeStatus(ChangeDepartmentStatusCommand command) {
        requirePermission(command.actor().hasPermission("system:department:status"));
        var source = requireManageableNode(command.actor(), command.departmentId());
        requireVersion(source.version(), command.version());
        var counts = persistence.countImpact(command.actor().tenantId(), source.id());
        policy.validateStatusChange(source.toSnapshot(), command.targetStatus(), command.cascade(),
                counts.descendantCount() > 0, persistence.hasDisabledAncestor(command.actor().tenantId(), source.id()));
        var expected = new PreviewTokenBinding(command.actor().tenantId(), command.actor().actorId(), source.id(),
                "status", command.targetStatus().databaseValue(), source.version(), command.cascade(), clock.instant());
        validateAndConsumePreview(command.previewToken(), expected);
        int affected = persistence.changeStatus(command.actor().tenantId(), source.id(), command.targetStatus(),
                command.cascade(), command.version(), command.actor().actorId(), command.reason());
        eventPublisher.publish(new DepartmentChangedEvent(command.actor().tenantId(), source.id(), "status", true));
        return new DepartmentOperationResult(source.id(), source.version() + 1, command.targetStatus(),
                OffsetDateTime.now(clock), affected, "部门状态更新成功");
    }

    /**
     * 软删除无下级且无受保护引用的普通部门。
     *
     * <p>副作用：调用 PostgreSQL 软删除函数写删除标记和审计，发布缓存失效事件。</p>
     *
     * @param command 删除命令
     * @return 删除后的版本与状态
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentOperationResult delete(DeleteDepartmentCommand command) {
        requirePermission(command.actor().hasPermission("system:department:delete"));
        var source = requireManageableNode(command.actor(), command.departmentId());
        requireVersion(source.version(), command.version());
        var counts = persistence.countImpact(command.actor().tenantId(), source.id());
        policy.validateDelete(source.toSnapshot(), counts.descendantCount(), counts.directMemberCount(),
                counts.roleReferenceCount(), counts.businessReferenceCount());
        int newVersion = persistence.softDelete(command.actor().tenantId(), source.id(), command.version(),
                command.actor().actorId(), command.reason());
        eventPublisher.publish(new DepartmentChangedEvent(command.actor().tenantId(), source.id(), "delete", true));
        return new DepartmentOperationResult(source.id(), newVersion, DepartmentStatus.DISABLED,
                OffsetDateTime.now(clock), 1, "部门删除成功");
    }

    /** 校验功能权限；失败时不继续读取资源状态。 */
    private void requirePermission(boolean allowed) {
        if (!allowed) {
            throw new BusinessException("DEPARTMENT_OPERATION_FORBIDDEN", 403, "无部门操作权限");
        }
    }

    /** 校验节点数据范围；失败时以 404 隐藏资源存在性。 */
    private void requireManageable(boolean manageable) {
        if (!manageable) {
            throw notFound();
        }
    }

    /** @return 不泄露跨租户或越权资源存在性的 404 异常 */
    private BusinessException notFound() {
        return new BusinessException("DEPARTMENT_NOT_FOUND", 404, "部门不存在");
    }

    /** 读取位于当前数据范围的最新部门，范围外与跨租户统一返回 404。 */
    private DepartmentRecord requireManageableNode(CurrentActor actor, UUID departmentId) {
        requireManageable(actor.canManage(departmentId));
        return persistence.findById(actor.tenantId(), departmentId).orElseThrow(this::notFound);
    }

    /** 校验客户端版本与最新数据库快照一致。 */
    private void requireVersion(int actual, int expected) {
        if (actual != expected) {
            throw new BusinessException("DEPARTMENT_VERSION_CONFLICT", 409, "部门已被其他管理员修改");
        }
    }

    /** 组装统一影响预览并按影响规模评估风险。 */
    private DepartmentImpactPreviewResult previewResult(
            String token,
            Instant expiresAt,
            List<DepartmentPathItem> currentPath,
            List<DepartmentPathItem> targetPath,
            DepartmentImpactCounts counts,
            String message
    ) {
        int affected = counts.descendantCount() + 1;
        String risk = affected > 100 || counts.businessReferenceCount() > 0 ? "high"
                : affected > 10 || counts.roleReferenceCount() > 0 ? "medium" : "low";
        return new DepartmentImpactPreviewResult(token, OffsetDateTime.ofInstant(expiresAt, clock.getZone()),
                currentPath, targetPath, affected, counts.directMemberCount(), counts.roleReferenceCount(),
                counts.businessReferenceCount(), counts.hasHiddenReferences(), risk, true, List.of(message));
    }

    /** 消费并校验预览绑定、有效期和重放状态。 */
    private void validateAndConsumePreview(String token, PreviewTokenBinding expected) {
        var actual = previewTokenStore.consume(token)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_PREVIEW_EXPIRED", 409, "预览已过期或已使用"));
        if (actual.expiresAt().isBefore(clock.instant())) {
            throw new BusinessException("DEPARTMENT_PREVIEW_EXPIRED", 409, "预览已过期或已使用");
        }
        if (!actual.matches(expected)) {
            throw new BusinessException("DEPARTMENT_PREVIEW_MISMATCH", 409, "预览内容与当前操作不一致");
        }
    }
}
