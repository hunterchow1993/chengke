package com.chengke.chengkecrmbackend.modules.system.user.application;

import com.chengke.chengkecrmbackend.modules.system.user.application.command.BatchDisableUsersCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.ChangeUserStatusCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.CreateUserCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.ResetUserPasswordCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.UpdateUserCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.event.UserChangedEvent;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.PasswordHasher;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserChangeEventPublisher;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.RoleRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserAuditSnapshot;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserDepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserInsert;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.BatchDisableResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.ResetPasswordResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserCreateResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserStatusResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserUpdateResult;
import com.chengke.chengkecrmbackend.modules.system.user.domain.exception.UserDomainException;
import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 编排用户写用例，负责权限、范围、保护账号、唯一性、部门引用、审计和提交后失效。
 */
@Service
public class UserCommandService {
    private static final String CREATE = "system:user:create";
    private static final String UPDATE = "system:user:update";
    private static final String DISABLE = "system:user:disable";
    private static final String RESET_PASSWORD = "system:user:reset-password";
    private static final String PASSWORD_RULE_MESSAGE = "密码须为 8–32 字符，至少包含字母、数字、特殊字符中的两类，且不允许连续空格";

    private final UserPersistencePort persistence;
    private final PasswordHasher passwordHasher;
    private final UserChangeEventPublisher eventPublisher;
    private final UserPolicy policy;
    private final Clock clock;
    private final TransactionTemplate itemTransactions;

    /**
     * 创建用户写应用服务。
     *
     * @param persistence 用户持久化端口
     * @param passwordHasher 密码不可逆化
     * @param eventPublisher 事务内变化事件
     * @param policy 保护与密码规则
     * @param clock 可测试时钟
     * @param itemTransactions 批量停用逐项独立事务；无事务管理器时回退为同步执行
     */
    @Autowired
    public UserCommandService(
            UserPersistencePort persistence,
            PasswordHasher passwordHasher,
            UserChangeEventPublisher eventPublisher,
            UserPolicy policy,
            Clock clock,
            @Qualifier("userRequiresNewTransactionTemplate") ObjectProvider<TransactionTemplate> itemTransactions
    ) {
        this(persistence, passwordHasher, eventPublisher, policy, clock,
                itemTransactions.getIfAvailable(UserCommandService::passthroughTemplate));
    }

    /**
     * 供单测直接注入事务模板。
     */
    public UserCommandService(
            UserPersistencePort persistence,
            PasswordHasher passwordHasher,
            UserChangeEventPublisher eventPublisher,
            UserPolicy policy,
            Clock clock,
            TransactionTemplate itemTransactions
    ) {
        this.persistence = persistence;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.policy = policy;
        this.clock = clock;
        this.itemTransactions = itemTransactions;
    }

    private static TransactionTemplate passthroughTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }

    /**
     * 新增用户并登记部门引用与审计。
     *
     * @param command 新增命令
     * @return 新用户标识与状态；响应不含密码
     */
    @Transactional(rollbackFor = Exception.class)
    public UserCreateResult create(CreateUserCommand command) {
        requirePermission(command.actor(), CREATE);
        requirePassword(command.initialPassword());
        requireAssignableDepartment(command.actor(), command.departmentId());
        requireAssignableRole(command.actor(), command.roleId());
        if (persistence.existsUsername(command.actor().tenantId(), command.username(), null)) {
            throw new BusinessException("USER_USERNAME_DUPLICATE", 409, "用户名已存在");
        }
        if (persistence.existsMobile(command.actor().tenantId(), command.mobile(), null)) {
            throw new BusinessException("USER_MOBILE_DUPLICATE", 409, "手机号已存在");
        }
        UUID userId = UUID.randomUUID();
        persistence.insert(new UserInsert(
                userId, command.actor().tenantId(), command.name(), command.username(),
                passwordHasher.hash(command.initialPassword()), command.avatarUrl(), command.mobile(),
                command.email(), command.departmentId(), command.roleId(), command.status(),
                command.forcePasswordChange(), command.remark(), command.actor().actorId()
        ));
        persistence.registerDepartmentReference(command.actor().tenantId(), command.departmentId(), userId);
        UserRecord created = persistence.findById(command.actor().tenantId(), userId).orElseThrow(this::databaseFailed);
        persistence.insertAudit(command.actor().tenantId(), userId, "create", command.actor().actorId(), "success",
                null, UserAuditSnapshot.from(created));
        eventPublisher.publish(new UserChangedEvent(command.actor().tenantId(), userId, "create", true, true));
        return new UserCreateResult(userId, created.status(), OffsetDateTime.now(clock));
    }

    /**
     * 编辑用户资料、部门、角色和状态。
     *
     * @param command 编辑命令
     * @return 更新后版本与状态
     */
    @Transactional(rollbackFor = Exception.class)
    public UserUpdateResult update(UpdateUserCommand command) {
        requirePermission(command.actor(), UPDATE);
        UserRecord current = requireManageableUserForWrite(command.actor(), command.userId(), true);
        if (current.version() != command.version()) {
            throw new BusinessException("USER_VERSION_CONFLICT", 409, "用户信息已被其他管理员修改");
        }
        requireAssignableDepartment(command.actor(), command.departmentId());
        requireAssignableRole(command.actor(), command.roleId());
        if (persistence.existsMobile(command.actor().tenantId(), command.mobile(), command.userId())) {
            throw new BusinessException("USER_MOBILE_DUPLICATE", 409, "手机号已存在");
        }
        boolean self = command.userId().equals(command.actor().actorId());
        policy.validateRoleChange(self, !current.roleId().equals(command.roleId()));
        if (command.status() == UserStatus.DISABLED && current.status() != UserStatus.DISABLED) {
            policy.validateDisable(self, isLastSuperAdmin(command.actor().tenantId(), current));
        }
        int newVersion = persistence.update(
                command.actor().tenantId(), command.userId(), command.name(), command.avatarUrl(),
                command.mobile(), command.email(), command.departmentId(), command.roleId(),
                command.status(), command.remark(), command.version(), command.actor().actorId()
        );
        if (newVersion == 0) {
            throw new BusinessException("USER_VERSION_CONFLICT", 409, "用户信息已被其他管理员修改");
        }
        if (!current.departmentId().equals(command.departmentId())) {
            persistence.unregisterDepartmentReference(
                    command.actor().tenantId(), current.departmentId(), command.userId());
            persistence.registerDepartmentReference(
                    command.actor().tenantId(), command.departmentId(), command.userId());
        }
        UserRecord updated = persistence.findById(command.actor().tenantId(), command.userId())
                .orElseThrow(this::databaseFailed);
        persistence.insertAudit(command.actor().tenantId(), command.userId(), "update", command.actor().actorId(),
                "success", UserAuditSnapshot.from(current), UserAuditSnapshot.from(updated));
        boolean statusChanged = current.status() != command.status();
        boolean deptChanged = !current.departmentId().equals(command.departmentId());
        boolean roleChanged = !current.roleId().equals(command.roleId());
        eventPublisher.publish(new UserChangedEvent(
                command.actor().tenantId(), command.userId(), "update",
                statusChanged || deptChanged || roleChanged,
                statusChanged || deptChanged
        ));
        return new UserUpdateResult(command.userId(), newVersion, updated.status(), OffsetDateTime.now(clock));
    }

    /**
     * 启用或停用单个用户。
     *
     * @param command 目标状态
     * @return 变更后状态与版本
     */
    @Transactional(rollbackFor = Exception.class)
    public UserStatusResult changeStatus(ChangeUserStatusCommand command) {
        requirePermission(command.actor(), DISABLE);
        return changeStatusInternal(command.actor(), command.userId(), command.targetStatus(), false, true);
    }

    /**
     * 批量停用；用户之间相互独立，允许部分成功。
     *
     * @param command 待停用用户集合
     * @return 逐项结果与统计
     */
    public BatchDisableResult batchDisable(BatchDisableUsersCommand command) {
        requirePermission(command.actor(), DISABLE);
        if (command.userIds() == null || command.userIds().isEmpty() || command.userIds().size() > 100) {
            throw new BusinessException("USER_REQUEST_INVALID", 400, "请求参数不合法");
        }
        List<BatchDisableResult.BatchDisableItemResult> results = new ArrayList<>();
        boolean anySuccess = false;
        for (UUID userId : command.userIds()) {
            try {
                itemTransactions.executeWithoutResult(status -> {
                    changeStatusInternal(command.actor(), userId, UserStatus.DISABLED, true, false);
                    eventPublisher.publish(new UserChangedEvent(
                            command.actor().tenantId(), userId, "disable", false, false));
                });
                results.add(new BatchDisableResult.BatchDisableItemResult(userId, true, "OK", "停用成功"));
                anySuccess = true;
            } catch (RuntimeException exception) {
                results.add(toItemFailure(userId, exception));
            }
        }
        if (anySuccess) {
            itemTransactions.executeWithoutResult(status -> eventPublisher.publish(new UserChangedEvent(
                    command.actor().tenantId(), null, "batch_disable", true, true)));
        }
        int successCount = (int) results.stream().filter(BatchDisableResult.BatchDisableItemResult::success).count();
        return new BatchDisableResult(command.userIds().size(), successCount,
                command.userIds().size() - successCount, results);
    }

    /**
     * 重置用户密码并设置下次登录改密开关。
     *
     * @param command 新密码与改密开关
     * @return 不含密码的操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResult resetPassword(ResetUserPasswordCommand command) {
        requirePermission(command.actor(), RESET_PASSWORD);
        requirePassword(command.newPassword());
        UserRecord current = requireManageableUserForWrite(command.actor(), command.userId(), false);
        persistence.updatePassword(
                command.actor().tenantId(), command.userId(),
                passwordHasher.hash(command.newPassword()), command.forcePasswordChange(),
                command.actor().actorId()
        );
        UserRecord updated = persistence.findById(command.actor().tenantId(), command.userId())
                .orElseThrow(this::databaseFailed);
        persistence.insertAudit(command.actor().tenantId(), command.userId(), "reset_password",
                command.actor().actorId(), "success",
                UserAuditSnapshot.from(current), UserAuditSnapshot.from(updated));
        eventPublisher.publish(new UserChangedEvent(
                command.actor().tenantId(), command.userId(), "reset_password", true, false));
        return new ResetPasswordResult(command.userId(), command.forcePasswordChange(), OffsetDateTime.now(clock));
    }

    /**
     * 在当前事务中执行单个状态变更；批量停用通过独立事务调用。
     */
    private UserStatusResult changeStatusInternal(
            CurrentActor actor, UUID userId, UserStatus targetStatus,
            boolean rejectAlreadyDisabled, boolean publishEvent
    ) {
        UserRecord current = persistence.lockById(actor.tenantId(), userId)
                .orElseThrow(this::notFound);
        if (!actor.canManage(current.departmentId())) {
            throw notFound();
        }
        if (current.status() == targetStatus) {
            if (rejectAlreadyDisabled && targetStatus == UserStatus.DISABLED) {
                throw new BusinessException("USER_REQUEST_INVALID", 422, "已停用");
            }
            return new UserStatusResult(userId, current.status(), current.version(), OffsetDateTime.now(clock));
        }
        if (targetStatus == UserStatus.DISABLED) {
            policy.validateDisable(userId.equals(actor.actorId()), isLastSuperAdmin(actor.tenantId(), current));
        }
        int newVersion = persistence.updateStatus(actor.tenantId(), userId, targetStatus, actor.actorId());
        UserRecord updated = persistence.findById(actor.tenantId(), userId).orElseThrow(this::databaseFailed);
        String action = targetStatus == UserStatus.DISABLED ? "disable" : "enable";
        persistence.insertAudit(actor.tenantId(), userId, action, actor.actorId(), "success",
                UserAuditSnapshot.from(current), UserAuditSnapshot.from(updated));
        if (publishEvent) {
            eventPublisher.publish(new UserChangedEvent(actor.tenantId(), userId, action, true, true));
        }
        return new UserStatusResult(userId, targetStatus, newVersion, OffsetDateTime.now(clock));
    }

    /** 目标为超级管理员角色且锁定后仅剩其一人处于正常状态。 */
    private boolean isLastSuperAdmin(UUID tenantId, UserRecord user) {
        if (!user.roleBuiltIn() || !"super_admin".equals(user.roleCode())) {
            return false;
        }
        List<UUID> active = persistence.lockActiveSuperAdminIds(tenantId);
        return active.size() == 1 && active.contains(user.id());
    }

    /** 写操作加载用户；编辑保存时范围外使用 PRD 指定消息。 */
    private UserRecord requireManageableUserForWrite(CurrentActor actor, UUID userId, boolean updateMessage) {
        UserRecord user = persistence.lockById(actor.tenantId(), userId).orElseThrow(this::notFound);
        if (!actor.canManage(user.departmentId())) {
            if (updateMessage) {
                throw new BusinessException("USER_NOT_FOUND", 404, "该用户已不在你的管理范围内");
            }
            throw notFound();
        }
        return user;
    }

    private void requireAssignableDepartment(CurrentActor actor, UUID departmentId) {
        if (!actor.canManage(departmentId)) {
            throw new BusinessException("USER_DEPARTMENT_UNAVAILABLE", 404, "部门不存在");
        }
        UserDepartmentRecord department = persistence.findDepartment(actor.tenantId(), departmentId)
                .orElseThrow(() -> new BusinessException("USER_DEPARTMENT_UNAVAILABLE", 404, "部门不存在"));
        if (!"active".equals(department.status())) {
            throw new BusinessException("USER_DEPARTMENT_UNAVAILABLE", 422, "部门已停用");
        }
    }

    private void requireAssignableRole(CurrentActor actor, UUID roleId) {
        RoleRecord role = persistence.findRole(actor.tenantId(), roleId)
                .orElseThrow(() -> new BusinessException("USER_ROLE_UNAVAILABLE", 404, "角色不存在"));
        if (!"active".equals(role.status())) {
            throw new BusinessException("USER_ROLE_UNAVAILABLE", 422, "角色已停用");
        }
    }

    private void requirePassword(String password) {
        if (!policy.isValidPassword(password)) {
            throw new BusinessException("USER_REQUEST_INVALID", 400, PASSWORD_RULE_MESSAGE);
        }
    }

    private void requirePermission(CurrentActor actor, String permission) {
        if (!actor.hasPermission(permission)) {
            throw new BusinessException("USER_OPERATION_FORBIDDEN", 403, "无操作权限");
        }
    }

    private BatchDisableResult.BatchDisableItemResult toItemFailure(UUID userId, RuntimeException exception) {
        if (exception instanceof BusinessException business) {
            return new BatchDisableResult.BatchDisableItemResult(
                    userId, false, business.code(), messageOf(business));
        }
        if (exception instanceof UserDomainException domain) {
            return new BatchDisableResult.BatchDisableItemResult(
                    userId, false, domain.code(), domainMessage(domain.code()));
        }
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessException business) {
            return new BatchDisableResult.BatchDisableItemResult(
                    userId, false, business.code(), messageOf(business));
        }
        if (cause instanceof UserDomainException domain) {
            return new BatchDisableResult.BatchDisableItemResult(
                    userId, false, domain.code(), domainMessage(domain.code()));
        }
        return new BatchDisableResult.BatchDisableItemResult(
                userId, false, "USER_DATABASE_OPERATION_FAILED", "系统异常");
    }

    private String messageOf(BusinessException exception) {
        int separator = exception.getMessage().indexOf(": ");
        return separator < 0 ? "业务操作失败" : exception.getMessage().substring(separator + 2);
    }

    private String domainMessage(String code) {
        return switch (code) {
            case "USER_SELF_DISABLE_FORBIDDEN" -> "不能停用自己";
            case "USER_LAST_SUPER_ADMIN_PROTECTED" -> "唯一超级管理员受保护";
            case "USER_SELF_ROLE_CHANGE_FORBIDDEN" -> "不能修改自己的角色";
            default -> "用户操作不符合业务规则";
        };
    }

    private BusinessException notFound() {
        return new BusinessException("USER_NOT_FOUND", 404, "用户不存在");
    }

    private BusinessException databaseFailed() {
        return new BusinessException("USER_DATABASE_OPERATION_FAILED", 500, "系统异常");
    }
}
