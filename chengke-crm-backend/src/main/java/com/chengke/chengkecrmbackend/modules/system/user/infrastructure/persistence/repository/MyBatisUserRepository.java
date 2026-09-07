package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.repository;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.RoleRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserAuditSnapshot;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserDepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserInsert;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserSubtreeCount;
import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.UserDatabaseExceptionTranslator;
import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.dataobject.UserDO;
import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper.UserCommandMapper;
import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper.UserQueryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 使用 MyBatis XML 实现用户持久化端口，并把数据库异常转换为稳定业务码。
 */
@Repository
public class MyBatisUserRepository implements UserPersistencePort {
    private final UserQueryMapper queryMapper;
    private final UserCommandMapper commandMapper;
    private final UserDatabaseExceptionTranslator exceptionTranslator;
    private final ObjectMapper objectMapper;

    /**
     * @param queryMapper 用户查询 Mapper
     * @param commandMapper 用户写入 Mapper
     * @param exceptionTranslator PostgreSQL 业务错误转换器
     * @param objectMapper 审计快照 JSON 序列化
     */
    public MyBatisUserRepository(UserQueryMapper queryMapper, UserCommandMapper commandMapper,
                                 UserDatabaseExceptionTranslator exceptionTranslator, ObjectMapper objectMapper) {
        this.queryMapper = queryMapper;
        this.commandMapper = commandMapper;
        this.exceptionTranslator = exceptionTranslator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<UserRecord> findById(UUID tenantId, UUID userId) {
        return Optional.ofNullable(queryMapper.selectById(tenantId, userId)).map(this::toRecord);
    }

    @Override
    public Optional<UserRecord> lockById(UUID tenantId, UUID userId) {
        return Optional.ofNullable(queryMapper.lockById(tenantId, userId)).map(this::toRecord);
    }

    @Override
    public List<UserRecord> findPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status,
                                     int offset, int limit) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return List.of();
        }
        return queryMapper.selectPage(tenantId, departmentIds, keyword, status, offset, limit)
                .stream().map(this::toRecord).toList();
    }

    @Override
    public long countPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return 0;
        }
        return queryMapper.countPage(tenantId, departmentIds, keyword, status);
    }

    @Override
    public boolean existsUsername(UUID tenantId, String username, UUID excludeUserId) {
        return queryMapper.existsUsername(tenantId, username, excludeUserId);
    }

    @Override
    public boolean existsMobile(UUID tenantId, String mobile, UUID excludeUserId) {
        return queryMapper.existsMobile(tenantId, mobile, excludeUserId);
    }

    @Override
    public void insert(UserInsert insert) {
        execute(() -> commandMapper.insertUser(
                insert.id(), insert.tenantId(), insert.name(), insert.username(), insert.passwordHash(),
                insert.avatarUrl(), insert.mobile(), insert.email(), insert.departmentId(), insert.roleId(),
                insert.status().databaseValue(), insert.forcePasswordChange(), insert.remark(), insert.actorId()
        ));
    }

    @Override
    public int update(UUID tenantId, UUID userId, String name, String avatarUrl, String mobile, String email,
                      UUID departmentId, UUID roleId, UserStatus status, String remark, int version, UUID actorId) {
        int rows = execute(() -> commandMapper.updateUser(
                tenantId, userId, name, avatarUrl, mobile, email, departmentId, roleId,
                status.databaseValue(), remark, version, actorId
        ));
        return rows == 0 ? 0 : version + 1;
    }

    @Override
    public int updateStatus(UUID tenantId, UUID userId, UserStatus status, UUID actorId) {
        execute(() -> commandMapper.updateStatus(tenantId, userId, status.databaseValue(), actorId));
        return queryMapper.selectById(tenantId, userId).getVersion();
    }

    @Override
    public int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange,
                              UUID actorId) {
        execute(() -> commandMapper.updatePassword(tenantId, userId, passwordHash, forcePasswordChange, actorId));
        return queryMapper.selectById(tenantId, userId).getVersion();
    }

    @Override
    public void insertAudit(UUID tenantId, UUID userId, String action, UUID actorId, String result,
                            UserAuditSnapshot before, UserAuditSnapshot after) {
        execute(() -> commandMapper.insertAudit(
                tenantId, userId, action, actorId, result, toJson(before), toJson(after)
        ));
    }

    @Override
    public void registerDepartmentReference(UUID tenantId, UUID departmentId, UUID userId) {
        execute(() -> {
            commandMapper.registerDepartmentReference(tenantId, departmentId, userId);
            return null;
        });
    }

    @Override
    public void unregisterDepartmentReference(UUID tenantId, UUID departmentId, UUID userId) {
        execute(() -> commandMapper.unregisterDepartmentReference(tenantId, departmentId, userId));
    }

    @Override
    public Optional<UserDepartmentRecord> findDepartment(UUID tenantId, UUID departmentId) {
        return Optional.ofNullable(queryMapper.selectDepartment(tenantId, departmentId));
    }

    @Override
    public List<UUID> findDescendantIds(UUID tenantId, UUID ancestorId, boolean includeDescendants) {
        return queryMapper.selectDescendantIds(tenantId, ancestorId, includeDescendants);
    }

    @Override
    public List<UserDepartmentRecord> findAllDepartments(UUID tenantId) {
        return queryMapper.selectAllDepartments(tenantId);
    }

    @Override
    public List<UserDepartmentRecord> findDirectChildren(UUID tenantId, UUID parentId) {
        return queryMapper.selectDirectChildren(tenantId, parentId);
    }

    @Override
    public List<UserDepartmentRecord> searchDepartments(UUID tenantId, String keyword) {
        return queryMapper.searchDepartments(tenantId, keyword);
    }

    @Override
    public Map<UUID, Integer> countUsersByAncestor(UUID tenantId, Collection<UUID> ancestorIds,
                                                   Collection<UUID> manageableDepartmentIds, boolean manageAll) {
        if (ancestorIds == null || ancestorIds.isEmpty()) {
            return Map.of();
        }
        if (!manageAll && (manageableDepartmentIds == null || manageableDepartmentIds.isEmpty())) {
            return ancestorIds.stream().collect(Collectors.toMap(id -> id, id -> 0, (a, b) -> a));
        }
        return queryMapper.countUsersByAncestor(tenantId, ancestorIds, manageableDepartmentIds, manageAll)
                .stream()
                .collect(Collectors.toMap(UserSubtreeCount::nodeId, UserSubtreeCount::userCount, (a, b) -> a));
    }

    @Override
    public List<UserDepartmentRecord> findAssignableDepartments(UUID tenantId, Collection<UUID> manageableIds,
                                                                boolean manageAll) {
        if (!manageAll && (manageableIds == null || manageableIds.isEmpty())) {
            return List.of();
        }
        return queryMapper.selectAssignableDepartments(tenantId, manageableIds, manageAll);
    }

    @Override
    public Optional<RoleRecord> findRole(UUID tenantId, UUID roleId) {
        return Optional.ofNullable(queryMapper.selectRole(tenantId, roleId));
    }

    @Override
    public List<RoleRecord> findActiveRoles(UUID tenantId) {
        return queryMapper.selectActiveRoles(tenantId);
    }

    @Override
    public List<UUID> lockActiveSuperAdminIds(UUID tenantId) {
        return queryMapper.lockActiveSuperAdminIds(tenantId);
    }

    @Override
    public Optional<UUID> findDepartmentIdByUserId(UUID tenantId, UUID userId) {
        return Optional.ofNullable(queryMapper.selectDepartmentIdByUserId(tenantId, userId));
    }

    @Override
    public boolean isVisibleAncestor(UUID tenantId, UUID departmentId, Collection<UUID> manageableIds) {
        if (manageableIds == null || manageableIds.isEmpty()) {
            return false;
        }
        return queryMapper.isVisibleAncestor(tenantId, departmentId, manageableIds);
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }

    private String toJson(UserAuditSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize user audit snapshot", exception);
        }
    }

    private UserRecord toRecord(UserDO value) {
        return new UserRecord(
                value.getId(), value.getTenantId(), value.getName(), value.getUsername(), value.getAvatarUrl(),
                value.getMobile(), value.getEmail(), value.getDepartmentId(), value.getDepartmentName(),
                value.getRoleId(), value.getRoleName(), value.getRoleCode(), value.isRoleBuiltIn(),
                UserStatus.fromValue(value.getStatus()), value.isForcePasswordChange(), value.getRemark(),
                value.getVersion(), value.getUpdatedBy(), value.getCreatedAt(), value.getUpdatedAt()
        );
    }
}
