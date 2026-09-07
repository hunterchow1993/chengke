package com.chengke.chengkecrmbackend.modules.system.department.application.port;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentLeaderCandidate;

import java.util.List;
import java.util.UUID;

/**
 * 查询同租户正常用户的负责人目录端口，隔离部门模块与用户表实现。
 */
public interface LeaderDirectoryPort {
    /**
     * 搜索负责人候选。
     *
     * @param tenantId 服务端租户标识
     * @param keyword 可选姓名或用户名关键词
     * @return 同租户、状态正常的用户摘要
     */
    List<DepartmentLeaderCandidate> search(UUID tenantId, String keyword);
}
