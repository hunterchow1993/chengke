package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.repository;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.dataobject.DepartmentDO;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.DepartmentDatabaseExceptionTranslator;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.mapper.DepartmentCommandMapper;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.mapper.DepartmentQueryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 使用 MyBatis XML 和 PostgreSQL 原子函数实现部门持久化端口。
 */
@Repository
public class MyBatisDepartmentRepository implements DepartmentPersistencePort {
    private final DepartmentQueryMapper queryMapper;
    private final DepartmentCommandMapper commandMapper;
    private final DepartmentDatabaseExceptionTranslator exceptionTranslator;

    /** @param mapper 部门 MyBatis Mapper @param exceptionTranslator PostgreSQL 业务错误转换器 */
    public MyBatisDepartmentRepository(DepartmentQueryMapper queryMapper,
                                       DepartmentCommandMapper commandMapper,
                                       DepartmentDatabaseExceptionTranslator exceptionTranslator) {
        this.queryMapper = queryMapper;
        this.commandMapper = commandMapper;
        this.exceptionTranslator = exceptionTranslator;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DepartmentRecord> findById(UUID tenantId, UUID departmentId) {
        return Optional.ofNullable(queryMapper.selectById(tenantId, departmentId)).map(this::toRecord);
    }

    /** {@inheritDoc} */
    @Override
    public List<DepartmentRecord> findAll(UUID tenantId) {
        return queryMapper.selectAll(tenantId).stream().map(this::toRecord).toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<DepartmentPathItem> findPath(UUID tenantId, UUID departmentId) {
        return queryMapper.selectPath(tenantId, departmentId);
    }

    /** {@inheritDoc} */
    @Override
    public DepartmentImpactCounts countImpact(UUID tenantId, UUID departmentId) {
        return queryMapper.countImpact(tenantId, departmentId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDescendant(UUID tenantId, UUID ancestorId, UUID possibleDescendantId) {
        return queryMapper.isDescendant(tenantId, ancestorId, possibleDescendantId);
    }

    /** {@inheritDoc} */
    @Override
    public int subtreeHeight(UUID tenantId, UUID departmentId) {
        return queryMapper.subtreeHeight(tenantId, departmentId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDisabledAncestor(UUID tenantId, UUID departmentId) {
        return queryMapper.hasDisabledAncestor(tenantId, departmentId);
    }

    /** {@inheritDoc} */
    @Override
    public int move(UUID tenantId, UUID departmentId, UUID newParentId, int version, UUID actorId, String reason) {
        return execute(() -> commandMapper.moveDepartment(tenantId, departmentId, newParentId, version, actorId, reason));
    }

    /** {@inheritDoc} */
    @Override
    public UUID create(UUID tenantId, UUID parentId, String name, String code, DepartmentNodeType nodeType,
                       UUID leaderUserId, int sortOrder, DepartmentStatus status, String remark, UUID actorId) {
        return execute(() -> commandMapper.createDepartment(tenantId, parentId, name, code, nodeType.databaseValue(),
                leaderUserId, sortOrder, status.databaseValue(), remark, actorId));
    }

    /** {@inheritDoc} */
    @Override
    public int update(UUID tenantId, UUID departmentId, String name, UUID leaderUserId, int sortOrder,
                      String remark, int version, UUID actorId, String reason) {
        return execute(() -> commandMapper.updateDepartment(tenantId, departmentId, name, leaderUserId, sortOrder,
                remark, version, actorId, reason));
    }

    /** {@inheritDoc} */
    @Override
    public int changeStatus(UUID tenantId, UUID departmentId, DepartmentStatus status, boolean cascade,
                            int version, UUID actorId, String reason) {
        return execute(() -> commandMapper.changeDepartmentStatus(tenantId, departmentId, status.databaseValue(),
                cascade, version, actorId, reason));
    }

    /** {@inheritDoc} */
    @Override
    public int softDelete(UUID tenantId, UUID departmentId, int version, UUID actorId, String reason) {
        return execute(() -> commandMapper.softDeleteDepartment(tenantId, departmentId, version, actorId, reason));
    }

    /** 调用数据库函数并把技术异常转换为稳定业务码。 */
    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }

    /**
     * 把 MyBatis 数据对象转换为应用端口记录。
     *
     * @param value 数据库部门行
     * @return 框架无关部门记录
     */
    private DepartmentRecord toRecord(DepartmentDO value) {
        return new DepartmentRecord(value.getId(), value.getTenantId(), value.getParentId(), value.getName(),
                value.getCode(), DepartmentNodeType.valueOf(value.getNodeType().toUpperCase()),
                value.getLeaderUserId(), value.getSortOrder(),
                DepartmentStatus.valueOf(value.getStatus().toUpperCase()), value.getRemark(), value.getDepth(),
                value.getVersion(), value.getUpdatedBy(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
