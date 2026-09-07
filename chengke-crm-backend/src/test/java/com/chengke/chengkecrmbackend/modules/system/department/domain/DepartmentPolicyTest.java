package com.chengke.chengkecrmbackend.modules.system.department.domain;

import com.chengke.chengkecrmbackend.modules.system.department.domain.exception.DepartmentDomainException;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeSnapshot;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.modules.system.department.domain.policy.DepartmentPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证部门树在应用服务和数据库函数之外仍具有一致的领域保护规则。
 */
class DepartmentPolicyTest {

    private final DepartmentPolicy policy = new DepartmentPolicy(10);

    @Test
    void shouldRejectMovingGroupRoot() {
        var root = node(DepartmentNodeType.GROUP, DepartmentStatus.ACTIVE, 0);

        assertThatThrownBy(() -> policy.validateMove(root, UUID.randomUUID(), 1, 0, false))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_ROOT_PROTECTED");
    }

    @Test
    void shouldRejectMovingToSelfDescendantOrTooDeepParent() {
        var department = node(DepartmentNodeType.DEPARTMENT, DepartmentStatus.ACTIVE, 2);

        assertThatThrownBy(() -> policy.validateMove(department, department.id(), 3, 0, false))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_MOVE_CYCLE");
        assertThatThrownBy(() -> policy.validateMove(department, UUID.randomUUID(), 8, 2, false))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_PARENT_INVALID");
        assertThatThrownBy(() -> policy.validateMove(department, UUID.randomUUID(), 3, 0, true))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_MOVE_CYCLE");
    }

    @Test
    void shouldRejectDisabledParentForCreateMoveAndEnable() {
        var department = node(DepartmentNodeType.DEPARTMENT, DepartmentStatus.DISABLED, 2);

        assertThatThrownBy(() -> policy.validateParent(DepartmentStatus.DISABLED))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_PARENT_DISABLED");
        assertThatThrownBy(() -> policy.validateStatusChange(department, DepartmentStatus.ACTIVE, false, false, true))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_PARENT_DISABLED");
    }

    @Test
    void shouldRequireCascadeWhenDisablingNodeWithActiveDescendants() {
        var department = node(DepartmentNodeType.DEPARTMENT, DepartmentStatus.ACTIVE, 2);

        assertThatThrownBy(() -> policy.validateStatusChange(department, DepartmentStatus.DISABLED, false, true, false))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_ACTIVE_DESCENDANTS");
        assertThatCode(() -> policy.validateStatusChange(department, DepartmentStatus.DISABLED, true, true, false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDeleteWhenAnyProtectedReferenceExists() {
        var department = node(DepartmentNodeType.DEPARTMENT, DepartmentStatus.DISABLED, 2);

        assertThatThrownBy(() -> policy.validateDelete(department, 0, 1, 0, 0))
                .isInstanceOf(DepartmentDomainException.class)
                .hasMessageContaining("DEPARTMENT_DELETE_BLOCKED");
        assertThatCode(() -> policy.validateDelete(department, 0, 0, 0, 0))
                .doesNotThrowAnyException();
    }

    /**
     * 创建供策略测试使用的最小节点快照。
     *
     * @param nodeType 节点类型
     * @param status 节点状态
     * @param depth 节点深度
     * @return 包含随机稳定标识的节点快照
     */
    private DepartmentNodeSnapshot node(DepartmentNodeType nodeType, DepartmentStatus status, int depth) {
        return new DepartmentNodeSnapshot(UUID.randomUUID(), UUID.randomUUID(), null, nodeType, status, depth, 1);
    }
}
