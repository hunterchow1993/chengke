package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.RoleRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserDepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserSubtreeCount;
import com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 通过 XML SQL 执行所有带 tenant_id 条件的用户与组织读取。
 */
@Mapper
public interface UserQueryMapper {
    UserDO selectById(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    UserDO lockById(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    List<UserDO> selectPage(@Param("tenantId") UUID tenantId,
                            @Param("departmentIds") Collection<UUID> departmentIds,
                            @Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    long countPage(@Param("tenantId") UUID tenantId,
                   @Param("departmentIds") Collection<UUID> departmentIds,
                   @Param("keyword") String keyword,
                   @Param("status") String status);

    boolean existsUsername(@Param("tenantId") UUID tenantId, @Param("username") String username,
                           @Param("excludeUserId") UUID excludeUserId);

    boolean existsMobile(@Param("tenantId") UUID tenantId, @Param("mobile") String mobile,
                         @Param("excludeUserId") UUID excludeUserId);

    UserDepartmentRecord selectDepartment(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);

    List<UUID> selectDescendantIds(@Param("tenantId") UUID tenantId, @Param("ancestorId") UUID ancestorId,
                                   @Param("includeDescendants") boolean includeDescendants);

    List<UserDepartmentRecord> selectAllDepartments(@Param("tenantId") UUID tenantId);

    List<UserDepartmentRecord> selectDirectChildren(@Param("tenantId") UUID tenantId, @Param("parentId") UUID parentId);

    List<UserDepartmentRecord> searchDepartments(@Param("tenantId") UUID tenantId, @Param("keyword") String keyword);

    List<UserSubtreeCount> countUsersByAncestor(@Param("tenantId") UUID tenantId,
                                                @Param("ancestorIds") Collection<UUID> ancestorIds,
                                                @Param("manageableDepartmentIds") Collection<UUID> manageableDepartmentIds,
                                                @Param("manageAll") boolean manageAll);

    List<UserDepartmentRecord> selectAssignableDepartments(@Param("tenantId") UUID tenantId,
                                                           @Param("manageableIds") Collection<UUID> manageableIds,
                                                           @Param("manageAll") boolean manageAll);

    RoleRecord selectRole(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

    List<RoleRecord> selectActiveRoles(@Param("tenantId") UUID tenantId);

    List<UUID> lockActiveSuperAdminIds(@Param("tenantId") UUID tenantId);

    UUID selectDepartmentIdByUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    boolean isVisibleAncestor(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                              @Param("manageableIds") Collection<UUID> manageableIds);
}
