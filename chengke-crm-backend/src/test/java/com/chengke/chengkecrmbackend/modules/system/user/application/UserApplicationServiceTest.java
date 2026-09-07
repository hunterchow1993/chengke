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
import com.chengke.chengkecrmbackend.modules.system.user.application.query.GetUserDetailQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UserListQuery;
import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证用户写查询用例的权限、范围、保护账号、唯一性和脱敏编排。
 */
class UserApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID DEPT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID SUPER_ROLE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-04T06:00:00Z");

    @Test
    void shouldCreateUserRegisterReferenceAndAuditWithoutStoringPlainPassword() {
        var persistence = new FakePersistence();
        persistence.departments.put(DEPT_ID, department(DEPT_ID, "active"));
        persistence.roles.put(ROLE_ID, new RoleRecord(ROLE_ID, "销售", "sales", "active", false));
        var events = new CapturingEvents();
        var hasher = new RecordingHasher();
        var service = commandService(persistence, hasher, events);

        var result = service.create(new CreateUserCommand(
                actor(Set.of("system:user:create"), Set.of(DEPT_ID), false),
                null, "周八", "zhouba", "13800008006", "ZhouBa@example.com",
                DEPT_ID, ROLE_ID, UserStatus.ACTIVE, "Abcdef12", true, "备注"
        ));

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(hasher.lastRaw).isEqualTo("Abcdef12");
        assertThat(persistence.inserted.passwordHash()).isEqualTo("hashed:Abcdef12");
        assertThat(persistence.registered).containsExactly(DEPT_ID);
        assertThat(persistence.audits).containsExactly("create");
        assertThat(events.events).hasSize(1);
        assertThat(events.events.getFirst().affectsAuthorization()).isTrue();
    }

    @Test
    void shouldRejectCreateWhenDepartmentOutOfScopeOrDisabled() {
        var persistence = new FakePersistence();
        persistence.departments.put(DEPT_ID, department(DEPT_ID, "disabled"));
        var service = commandService(persistence, new RecordingHasher(), new CapturingEvents());
        var outsider = actor(Set.of("system:user:create"), Set.of(), false);

        assertThatThrownBy(() -> service.create(createCommand(outsider)))
                .hasMessageContaining("USER_DEPARTMENT_UNAVAILABLE");

        var manager = actor(Set.of("system:user:create"), Set.of(DEPT_ID), false);
        assertThatThrownBy(() -> service.create(createCommand(manager)))
                .hasMessageContaining("USER_DEPARTMENT_UNAVAILABLE");
    }

    @Test
    void shouldRejectSelfDisableAndLastSuperAdmin() {
        var persistence = new FakePersistence();
        persistence.users.put(ACTOR_ID, user(ACTOR_ID, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        UUID superAdminId = UUID.randomUUID();
        persistence.users.put(superAdminId, user(superAdminId, DEPT_ID, SUPER_ROLE_ID, "super_admin", true,
                UserStatus.ACTIVE));
        persistence.activeSuperAdmins.add(superAdminId);
        var service = commandService(persistence, new RecordingHasher(), new CapturingEvents());
        var actor = actor(Set.of("system:user:disable"), Set.of(DEPT_ID), false);

        assertThatThrownBy(() -> service.changeStatus(new ChangeUserStatusCommand(actor, ACTOR_ID, UserStatus.DISABLED)))
                .hasMessageContaining("USER_SELF_DISABLE_FORBIDDEN");
        assertThatThrownBy(() -> service.changeStatus(
                new ChangeUserStatusCommand(actor, superAdminId, UserStatus.DISABLED)))
                .hasMessageContaining("USER_LAST_SUPER_ADMIN_PROTECTED");
    }

    @Test
    void shouldAllowPartialSuccessInBatchDisable() {
        var persistence = new FakePersistence();
        UUID other = UUID.randomUUID();
        persistence.users.put(ACTOR_ID, user(ACTOR_ID, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        persistence.users.put(other, user(other, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        var events = new CapturingEvents();
        var service = commandService(persistence, new RecordingHasher(), events);
        var actor = actor(Set.of("system:user:disable"), Set.of(DEPT_ID), false);

        var result = service.batchDisable(new BatchDisableUsersCommand(actor, List.of(ACTOR_ID, other)));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.results().getFirst().errorCode()).isEqualTo("USER_SELF_DISABLE_FORBIDDEN");
        assertThat(persistence.users.get(other).status()).isEqualTo(UserStatus.DISABLED);
        assertThat(persistence.users.get(ACTOR_ID).status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(events.events).extracting(event -> event.action()).containsExactly("disable", "batch_disable");
    }

    @Test
    void shouldResetPasswordWithoutEchoingSecretAndBumpAuthorization() {
        var persistence = new FakePersistence();
        UUID userId = UUID.randomUUID();
        persistence.users.put(userId, user(userId, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        var hasher = new RecordingHasher();
        var events = new CapturingEvents();
        var service = commandService(persistence, hasher, events);

        var result = service.resetPassword(new ResetUserPasswordCommand(
                actor(Set.of("system:user:reset-password"), Set.of(DEPT_ID), false),
                userId, "Newpass1!", true
        ));

        assertThat(result.forcePasswordChange()).isTrue();
        assertThat(hasher.lastRaw).isEqualTo("Newpass1!");
        assertThat(persistence.passwordHashes.get(userId)).isEqualTo("hashed:Newpass1!");
        assertThat(persistence.audits).containsExactly("reset_password");
        assertThat(events.events.getFirst().affectsAuthorization()).isTrue();
        assertThat(events.events.getFirst().affectsDepartmentTree()).isFalse();
    }

    @Test
    void shouldRejectSelfRoleChangeOnUpdate() {
        var persistence = new FakePersistence();
        persistence.users.put(ACTOR_ID, user(ACTOR_ID, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        persistence.departments.put(DEPT_ID, department(DEPT_ID, "active"));
        UUID otherRole = UUID.randomUUID();
        persistence.roles.put(otherRole, new RoleRecord(otherRole, "经理", "manager", "active", false));
        var service = commandService(persistence, new RecordingHasher(), new CapturingEvents());

        assertThatThrownBy(() -> service.update(new UpdateUserCommand(
                actor(Set.of("system:user:update"), Set.of(DEPT_ID), false),
                ACTOR_ID, null, "周八", "13800008006", null, DEPT_ID, otherRole,
                UserStatus.ACTIVE, null, 1
        ))).hasMessageContaining("USER_SELF_ROLE_CHANGE_FORBIDDEN");
    }

    @Test
    void shouldMaskMobileOnListWhenLackingFullPermission() {
        var persistence = new FakePersistence();
        persistence.departments.put(DEPT_ID, department(DEPT_ID, "active"));
        UUID userId = UUID.randomUUID();
        persistence.users.put(userId, user(userId, DEPT_ID, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        persistence.descendantIds.put(DEPT_ID, List.of(DEPT_ID));
        var queryService = new UserQueryService(persistence, new UserPolicy());

        var page = queryService.list(new UserListQuery(
                actor(Set.of("system:user:view"), Set.of(DEPT_ID), false),
                DEPT_ID, false, null, null, 1, 10
        ));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().mobile()).isEqualTo("138****8006");
    }

    @Test
    void shouldReturnNotFoundForUserOutsideManageableDepartments() {
        var persistence = new FakePersistence();
        UUID otherDept = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        persistence.users.put(userId, user(userId, otherDept, ROLE_ID, "sales", false, UserStatus.ACTIVE));
        var queryService = new UserQueryService(persistence, new UserPolicy());

        assertThatThrownBy(() -> queryService.getDetail(new GetUserDetailQuery(
                actor(Set.of("system:user:view"), Set.of(DEPT_ID), false), userId
        ))).hasMessageContaining("USER_NOT_FOUND");
    }

    private UserCommandService commandService(
            FakePersistence persistence, PasswordHasher hasher, CapturingEvents events
    ) {
        return new UserCommandService(
                persistence, hasher, events, new UserPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC), passthroughTemplate()
        );
    }

    private CreateUserCommand createCommand(CurrentActor actor) {
        return new CreateUserCommand(actor, null, "周八", "zhouba", "13800008006", null,
                DEPT_ID, ROLE_ID, UserStatus.ACTIVE, "Abcdef12", true, null);
    }

    private static CurrentActor actor(Set<String> permissions, Set<UUID> departments, boolean manageAll) {
        return new CurrentActor(TENANT_ID, ACTOR_ID, permissions, departments, manageAll, "req-1");
    }

    private static UserDepartmentRecord department(UUID id, String status) {
        return new UserDepartmentRecord(id, null, "销售部", "department", status, 1);
    }

    private static UserRecord user(
            UUID id, UUID departmentId, UUID roleId, String roleCode, boolean builtIn, UserStatus status
    ) {
        return new UserRecord(
                id, TENANT_ID, "周八", "zhouba", null, "13800008006", "zhouba@example.com",
                departmentId, "销售部", roleId, "角色", roleCode, builtIn, status, true, null, 1,
                ACTOR_ID, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }

    private static TransactionTemplate passthroughTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            @NonNull
            public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(@NonNull TransactionStatus status) {
            }

            @Override
            public void rollback(@NonNull TransactionStatus status) {
            }
        });
    }

    private static final class RecordingHasher implements PasswordHasher {
        private String lastRaw;

        @Override
        public String hash(String rawPassword) {
            lastRaw = rawPassword;
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return ("hashed:" + rawPassword).equals(passwordHash);
        }
    }

    private static final class CapturingEvents implements UserChangeEventPublisher {
        private final List<UserChangedEvent> events = new ArrayList<>();

        @Override
        public void publish(@NonNull Object event) {
            events.add((UserChangedEvent) event);
        }
    }

    private static final class FakePersistence implements UserPersistencePort {
        private final Map<UUID, UserRecord> users = new HashMap<>();
        private final Map<UUID, UserDepartmentRecord> departments = new HashMap<>();
        private final Map<UUID, RoleRecord> roles = new HashMap<>();
        private final Map<UUID, List<UUID>> descendantIds = new HashMap<>();
        private final List<UUID> activeSuperAdmins = new ArrayList<>();
        private final List<UUID> registered = new ArrayList<>();
        private final List<String> audits = new ArrayList<>();
        private final Map<UUID, String> passwordHashes = new HashMap<>();
        private UserInsert inserted;

        @Override
        public Optional<UserRecord> findById(UUID tenantId, UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<UserRecord> lockById(UUID tenantId, UUID userId) {
            return findById(tenantId, userId);
        }

        @Override
        public List<UserRecord> findPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status,
                                         int offset, int limit) {
            return users.values().stream()
                    .filter(user -> departmentIds.contains(user.departmentId()))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status) {
            return users.values().stream().filter(user -> departmentIds.contains(user.departmentId())).count();
        }

        @Override
        public boolean existsUsername(UUID tenantId, String username, UUID excludeUserId) {
            return users.values().stream().anyMatch(user ->
                    user.username().equalsIgnoreCase(username) && !user.id().equals(excludeUserId));
        }

        @Override
        public boolean existsMobile(UUID tenantId, String mobile, UUID excludeUserId) {
            return users.values().stream().anyMatch(user ->
                    user.mobile().equals(mobile) && !user.id().equals(excludeUserId));
        }

        @Override
        public void insert(UserInsert insert) {
            this.inserted = insert;
            users.put(insert.id(), user(insert.id(), insert.departmentId(), insert.roleId(), "sales", false,
                    insert.status()));
        }

        @Override
        public int update(UUID tenantId, UUID userId, String name, String avatarUrl, String mobile, String email,
                          UUID departmentId, UUID roleId, UserStatus status, String remark, int version, UUID actorId) {
            UserRecord current = users.get(userId);
            if (current == null || current.version() != version) {
                return 0;
            }
            users.put(userId, new UserRecord(userId, tenantId, name, current.username(), avatarUrl, mobile, email,
                    departmentId, current.departmentName(), roleId, current.roleName(), current.roleCode(),
                    current.roleBuiltIn(), status, current.forcePasswordChange(), remark, version + 1, actorId,
                    current.createdAt(), OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)));
            return version + 1;
        }

        @Override
        public int updateStatus(UUID tenantId, UUID userId, UserStatus status, UUID actorId) {
            UserRecord current = users.get(userId);
            users.put(userId, new UserRecord(current.id(), current.tenantId(), current.name(), current.username(),
                    current.avatarUrl(), current.mobile(), current.email(), current.departmentId(),
                    current.departmentName(), current.roleId(), current.roleName(), current.roleCode(),
                    current.roleBuiltIn(), status, current.forcePasswordChange(), current.remark(),
                    current.version() + 1, actorId, current.createdAt(), OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)));
            return current.version() + 1;
        }

        @Override
        public int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange,
                                  UUID actorId) {
            passwordHashes.put(userId, passwordHash);
            UserRecord current = users.get(userId);
            users.put(userId, new UserRecord(current.id(), current.tenantId(), current.name(), current.username(),
                    current.avatarUrl(), current.mobile(), current.email(), current.departmentId(),
                    current.departmentName(), current.roleId(), current.roleName(), current.roleCode(),
                    current.roleBuiltIn(), current.status(), forcePasswordChange, current.remark(),
                    current.version() + 1, actorId, current.createdAt(), OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)));
            return current.version() + 1;
        }

        @Override
        public void insertAudit(UUID tenantId, UUID userId, String action, UUID actorId, String result,
                                UserAuditSnapshot before, UserAuditSnapshot after) {
            audits.add(action);
        }

        @Override
        public void registerDepartmentReference(UUID tenantId, UUID departmentId, UUID userId) {
            registered.add(departmentId);
        }

        @Override
        public void unregisterDepartmentReference(UUID tenantId, UUID departmentId, UUID userId) {
        }

        @Override
        public Optional<UserDepartmentRecord> findDepartment(UUID tenantId, UUID departmentId) {
            return Optional.ofNullable(departments.get(departmentId));
        }

        @Override
        public List<UUID> findDescendantIds(UUID tenantId, UUID ancestorId, boolean includeDescendants) {
            return descendantIds.getOrDefault(ancestorId, List.of(ancestorId));
        }

        @Override
        public List<UserDepartmentRecord> findAllDepartments(UUID tenantId) {
            return List.copyOf(departments.values());
        }

        @Override
        public List<UserDepartmentRecord> findDirectChildren(UUID tenantId, UUID parentId) {
            return List.of();
        }

        @Override
        public List<UserDepartmentRecord> searchDepartments(UUID tenantId, String keyword) {
            return List.of();
        }

        @Override
        public Map<UUID, Integer> countUsersByAncestor(UUID tenantId, Collection<UUID> ancestorIds,
                                                       Collection<UUID> manageableDepartmentIds, boolean manageAll) {
            return Map.of();
        }

        @Override
        public List<UserDepartmentRecord> findAssignableDepartments(UUID tenantId, Collection<UUID> manageableIds,
                                                                    boolean manageAll) {
            return departments.values().stream()
                    .filter(dept -> "active".equals(dept.status()))
                    .filter(dept -> manageAll || manageableIds.contains(dept.id()))
                    .toList();
        }

        @Override
        public Optional<RoleRecord> findRole(UUID tenantId, UUID roleId) {
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public List<RoleRecord> findActiveRoles(UUID tenantId) {
            return roles.values().stream().filter(role -> "active".equals(role.status())).toList();
        }

        @Override
        public List<UUID> lockActiveSuperAdminIds(UUID tenantId) {
            return List.copyOf(activeSuperAdmins);
        }

        @Override
        public Optional<UUID> findDepartmentIdByUserId(UUID tenantId, UUID userId) {
            return Optional.ofNullable(users.get(userId)).map(user -> user.departmentId());
        }

        @Override
        public boolean isVisibleAncestor(UUID tenantId, UUID departmentId, Collection<UUID> manageableIds) {
            return false;
        }
    }
}
