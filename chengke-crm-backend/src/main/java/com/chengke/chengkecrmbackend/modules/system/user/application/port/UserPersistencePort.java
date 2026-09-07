package com.chengke.chengkecrmbackend.modules.system.user.application.port;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.RoleRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserAuditSnapshot;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserDepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserInsert;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserRecord;
import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户应用层访问最新用户、部门范围、角色目录和审计写入的持久化端口。
 */
public interface UserPersistencePort {

    Optional<UserRecord> findById(UUID tenantId, UUID userId);

    Optional<UserRecord> lockById(UUID tenantId, UUID userId);

    List<UserRecord> findPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status,
                              int offset, int limit);

    long countPage(UUID tenantId, Collection<UUID> departmentIds, String keyword, String status);

    boolean existsUsername(UUID tenantId, String username, UUID excludeUserId);

    boolean existsMobile(UUID tenantId, String mobile, UUID excludeUserId);

    void insert(UserInsert insert);

    /**
     * @return 更新后版本；影响行数为 0 时返回 0
     */
    int update(UUID tenantId, UUID userId, String name, String avatarUrl, String mobile, String email,
               UUID departmentId, UUID roleId, UserStatus status, String remark, int version, UUID actorId);

    int updateStatus(UUID tenantId, UUID userId, UserStatus status, UUID actorId);

    int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange, UUID actorId);

    void insertAudit(UUID tenantId, UUID userId, String action, UUID actorId, String result,
                     UserAuditSnapshot before, UserAuditSnapshot after);

    void registerDepartmentReference(UUID tenantId, UUID departmentId, UUID userId);

    void unregisterDepartmentReference(UUID tenantId, UUID departmentId, UUID userId);

    Optional<UserDepartmentRecord> findDepartment(UUID tenantId, UUID departmentId);

    List<UUID> findDescendantIds(UUID tenantId, UUID ancestorId, boolean includeDescendants);

    List<UserDepartmentRecord> findAllDepartments(UUID tenantId);

    List<UserDepartmentRecord> findDirectChildren(UUID tenantId, UUID parentId);

    List<UserDepartmentRecord> searchDepartments(UUID tenantId, String keyword);

    Map<UUID, Integer> countUsersByAncestor(UUID tenantId, Collection<UUID> ancestorIds,
                                            Collection<UUID> manageableDepartmentIds, boolean manageAll);

    List<UserDepartmentRecord> findAssignableDepartments(UUID tenantId, Collection<UUID> manageableIds,
                                                         boolean manageAll);

    Optional<RoleRecord> findRole(UUID tenantId, UUID roleId);

    List<RoleRecord> findActiveRoles(UUID tenantId);

    /**
     * 锁定租户内全部正常状态超级管理员行，供唯一超管判定串行化。
     *
     * @return 当前仍为 active 的超级管理员用户标识
     */
    List<UUID> lockActiveSuperAdminIds(UUID tenantId);

    Optional<UUID> findDepartmentIdByUserId(UUID tenantId, UUID userId);

    boolean isVisibleAncestor(UUID tenantId, UUID departmentId, Collection<UUID> manageableIds);
}
