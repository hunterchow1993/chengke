package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.repository;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentLeaderCandidate;
import com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.mapper.DepartmentQueryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 从 sys_user 提供负责人候选；仅返回同租户正常状态用户。
 */
@Repository
public class DepartmentReferenceLeaderDirectoryAdapter implements LeaderDirectoryPort {
    private final DepartmentQueryMapper mapper;

    /** @param mapper 只读取同租户 user 引用的部门 Mapper */
    public DepartmentReferenceLeaderDirectoryAdapter(DepartmentQueryMapper mapper) {
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<DepartmentLeaderCandidate> search(UUID tenantId, String keyword) {
        return mapper.selectLeaderCandidates(tenantId, keyword);
    }
}
