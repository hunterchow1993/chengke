package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.mapper;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentLeaderCandidate;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.dataobject.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * 通过 XML SQL 执行所有带 tenant_id 条件的部门读取。
 */
@Mapper
public interface DepartmentQueryMapper {
    /** @return 当前租户未删除节点，跨租户或不存在时为空 */
    DepartmentDO selectById(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);
    /** @return 当前租户全部未删除节点 */
    List<DepartmentDO> selectAll(@Param("tenantId") UUID tenantId);
    /** @return 用户引用元数据提供的负责人候选 */
    List<DepartmentLeaderCandidate> selectLeaderCandidates(@Param("tenantId") UUID tenantId,
                                                            @Param("keyword") String keyword);
    /** @return 根到节点的路径 */
    List<DepartmentPathItem> selectPath(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);
    /** @return 最新影响计数 */
    DepartmentImpactCounts countImpact(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);
    /** @return 是否为后代 */
    boolean isDescendant(@Param("tenantId") UUID tenantId, @Param("ancestorId") UUID ancestorId,
                         @Param("descendantId") UUID descendantId);
    /** @return 子树高度 */
    int subtreeHeight(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);
    /** @return 是否存在停用祖先 */
    boolean hasDisabledAncestor(@Param("tenantId") UUID tenantId, @Param("departmentId") UUID departmentId);
}
