package com.chengke.chengkecrmbackend.modules.system.department.application;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.application.query.DepartmentTreeQuery;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证部门查询只返回管理范围节点及其只读祖先，不泄露兄弟部门。
 */
class DepartmentQueryServiceTest {

    @Test
    void shouldReturnManageableNodeAndAncestorsOnly() {
        var rootId = UUID.randomUUID();
        var allowedId = UUID.randomUUID();
        var hiddenId = UUID.randomUUID();
        var tenantId = UUID.randomUUID();
        var persistence = mock(DepartmentPersistencePort.class);
        when(persistence.findAll(tenantId)).thenReturn(List.of(
                node(rootId, tenantId, null, "集团", 0),
                node(allowedId, tenantId, rootId, "可管理部门", 1),
                node(hiddenId, tenantId, rootId, "隐藏部门", 1)
        ));
        var actor = new CurrentActor(tenantId, UUID.randomUUID(), Set.of("system:department:view"),
                Set.of(allowedId), false, "req-1");

        var result = new DepartmentQueryService(persistence, mock(LeaderDirectoryPort.class))
                .getTree(new DepartmentTreeQuery(actor, null, null));

        assertThat(result).extracting("id").containsExactly(rootId, allowedId);
        assertThat(result.getFirst().readOnlyAncestor()).isTrue();
        assertThat(result.getLast().readOnlyAncestor()).isFalse();
    }

    /** 创建查询可见性测试所需的部门记录。 */
    private DepartmentRecord node(UUID id, UUID tenantId, UUID parentId, String name, int depth) {
        return new DepartmentRecord(id, tenantId, parentId, name, "code_" + id.toString().substring(0, 8),
                depth == 0 ? DepartmentNodeType.GROUP : DepartmentNodeType.DEPARTMENT,
                null, 100, DepartmentStatus.ACTIVE, null, depth, 1, UUID.randomUUID(),
                OffsetDateTime.parse("2026-08-10T09:00:00Z"), OffsetDateTime.parse("2026-08-10T09:00:00Z"));
    }
}
