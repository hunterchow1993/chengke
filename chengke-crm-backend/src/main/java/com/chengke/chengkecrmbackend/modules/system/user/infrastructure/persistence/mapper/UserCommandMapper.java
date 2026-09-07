package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * 执行用户写入、审计和部门引用登记。
 */
@Mapper
public interface UserCommandMapper {
    int insertUser(@Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("name") String name,
                   @Param("username") String username, @Param("passwordHash") String passwordHash,
                   @Param("avatarUrl") String avatarUrl, @Param("mobile") String mobile, @Param("email") String email,
                   @Param("departmentId") UUID departmentId, @Param("roleId") UUID roleId, @Param("status") String status,
                   @Param("forcePasswordChange") boolean forcePasswordChange, @Param("remark") String remark,
                   @Param("actorId") UUID actorId);

    int updateUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("name") String name,
                   @Param("avatarUrl") String avatarUrl, @Param("mobile") String mobile, @Param("email") String email,
                   @Param("departmentId") UUID departmentId, @Param("roleId") UUID roleId, @Param("status") String status,
                   @Param("remark") String remark, @Param("version") int version, @Param("actorId") UUID actorId);

    int updateStatus(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("status") String status,
                     @Param("actorId") UUID actorId);

    int updatePassword(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("forcePasswordChange") boolean forcePasswordChange, @Param("actorId") UUID actorId);

    int insertAudit(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("action") String action,
                    @Param("actorId") UUID actorId, @Param("result") String result,
                    @Param("beforeData") String beforeData, @Param("afterData") String afterData);

    void registerDepartmentReference(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                                     @Param("userId") UUID userId);

    boolean unregisterDepartmentReference(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                                          @Param("userId") UUID userId);
}
