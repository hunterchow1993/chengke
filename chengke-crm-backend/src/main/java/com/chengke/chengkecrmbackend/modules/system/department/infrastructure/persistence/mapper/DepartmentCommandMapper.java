package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * 仅调用已评审 PostgreSQL 原子函数执行部门写入，禁止手工更新路径和闭包。
 */
@Mapper
public interface DepartmentCommandMapper {
    /** @return 创建函数生成的 UUID */
    UUID createDepartment(@Param("tenantId") UUID tenantId, @Param("parentId") UUID parentId,
                          @Param("name") String name, @Param("code") String code,
                          @Param("nodeType") String nodeType, @Param("leaderUserId") UUID leaderUserId,
                          @Param("sortOrder") int sortOrder, @Param("status") String status,
                          @Param("remark") String remark, @Param("actorId") UUID actorId);
    /** @return 更新后的新版本 */
    int updateDepartment(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                         @Param("name") String name, @Param("leaderUserId") UUID leaderUserId,
                         @Param("sortOrder") int sortOrder, @Param("remark") String remark,
                         @Param("version") int version, @Param("actorId") UUID actorId,
                         @Param("reason") String reason);
    /** @return 移动后的新版本 */
    int moveDepartment(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                       @Param("newParentId") UUID newParentId, @Param("version") int version,
                       @Param("actorId") UUID actorId, @Param("reason") String reason);
    /** @return 状态函数影响节点数 */
    int changeDepartmentStatus(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                               @Param("status") String status, @Param("cascade") boolean cascade,
                               @Param("version") int version, @Param("actorId") UUID actorId,
                               @Param("reason") String reason);
    /** @return 软删除后的新版本 */
    int softDeleteDepartment(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId,
                             @Param("version") int version, @Param("actorId") UUID actorId,
                             @Param("reason") String reason);
}
