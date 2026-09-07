package com.chengke.chengkecrmbackend.modules.system.department.application;

import com.chengke.chengkecrmbackend.modules.system.department.application.command.MoveDepartmentCommand;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentChangeEventPublisher;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPreviewTokenStore;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentImpactCounts;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentPathItem;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.PreviewTokenBinding;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.modules.system.department.domain.policy.DepartmentPolicy;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证部门写用例的权限、预览绑定、乐观锁编排和变更事件发布。
 */
class DepartmentCommandServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID DEPARTMENT_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-10T09:00:00Z");

    @Test
    void shouldConsumeMatchingPreviewAndMoveDepartment() {
        var persistence = new FakePersistence();
        persistence.nodes.put(DEPARTMENT_ID, node(DEPARTMENT_ID, UUID.randomUUID(), 2, DepartmentNodeType.DEPARTMENT));
        persistence.nodes.put(PARENT_ID, node(PARENT_ID, null, 1, DepartmentNodeType.DEPARTMENT));
        var previews = new FakePreviewTokenStore();
        previews.binding = new PreviewTokenBinding(
                TENANT_ID, ACTOR_ID, DEPARTMENT_ID, "move", PARENT_ID.toString(), 1, false,
                NOW.plusSeconds(300)
        );
        var events = new CapturingEventPublisher();
        var service = new DepartmentCommandService(
                persistence, previews, events, new DepartmentPolicy(10), Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var result = service.move(new MoveDepartmentCommand(actor("system:department:move"), DEPARTMENT_ID,
                PARENT_ID, 1, "preview-token", "组织调整"));

        assertThat(previews.consumedToken).isEqualTo("preview-token");
        assertThat(persistence.movedDepartmentId).isEqualTo(DEPARTMENT_ID);
        assertThat(result.version()).isEqualTo(2);
        assertThat(events.events).hasSize(1);
    }

    @Test
    void shouldFailClosedWhenActorCannotManageNode() {
        var persistence = new FakePersistence();
        persistence.nodes.put(DEPARTMENT_ID, node(DEPARTMENT_ID, UUID.randomUUID(), 2, DepartmentNodeType.DEPARTMENT));
        var service = new DepartmentCommandService(
                persistence, new FakePreviewTokenStore(), new CapturingEventPublisher(),
                new DepartmentPolicy(10), Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var actor = new CurrentActor(TENANT_ID, ACTOR_ID, Set.of("system:department:move"), Set.of(), false, "req-1");

        assertThatThrownBy(() -> service.move(new MoveDepartmentCommand(actor, DEPARTMENT_ID,
                PARENT_ID, 1, "preview-token", null)))
                .hasMessageContaining("DEPARTMENT_NOT_FOUND");
    }

    @Test
    void shouldRejectPreviewBoundToAnotherActor() {
        var persistence = new FakePersistence();
        persistence.nodes.put(DEPARTMENT_ID, node(DEPARTMENT_ID, UUID.randomUUID(), 2, DepartmentNodeType.DEPARTMENT));
        persistence.nodes.put(PARENT_ID, node(PARENT_ID, null, 1, DepartmentNodeType.DEPARTMENT));
        var previews = new FakePreviewTokenStore();
        previews.binding = new PreviewTokenBinding(
                TENANT_ID, UUID.randomUUID(), DEPARTMENT_ID, "move", PARENT_ID.toString(), 1, false,
                NOW.plusSeconds(300)
        );
        var service = new DepartmentCommandService(persistence, previews, new CapturingEventPublisher(),
                new DepartmentPolicy(10), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.move(new MoveDepartmentCommand(actor("system:department:move"),
                DEPARTMENT_ID, PARENT_ID, 1, "preview-token", null)))
                .hasMessageContaining("DEPARTMENT_PREVIEW_MISMATCH");
        assertThat(persistence.movedDepartmentId).isNull();
    }

    /** 创建具有测试所需字段的部门持久化记录。 */
    private static DepartmentRecord node(UUID id, UUID parentId, int depth, DepartmentNodeType type) {
        return new DepartmentRecord(id, TENANT_ID, parentId, "测试部门", "test_" + id.toString().substring(0, 8),
                type, null, 100, DepartmentStatus.ACTIVE, null, depth, 1, ACTOR_ID,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    /** 创建拥有指定功能权限且可管理测试节点的操作者。 */
    private static CurrentActor actor(String permission) {
        return new CurrentActor(TENANT_ID, ACTOR_ID, Set.of(permission), Set.of(DEPARTMENT_ID, PARENT_ID), false, "req-1");
    }

    /** 仅实现测试用例所需行为的内存持久化端口。 */
    private static final class FakePersistence implements DepartmentPersistencePort {
        private final Map<UUID, DepartmentRecord> nodes = new HashMap<>();
        private UUID movedDepartmentId;

        @Override
        public Optional<DepartmentRecord> findById(UUID tenantId, UUID departmentId) {
            return Optional.ofNullable(nodes.get(departmentId));
        }

        @Override
        public List<DepartmentRecord> findAll(UUID tenantId) {
            return List.copyOf(nodes.values());
        }

        @Override
        public List<DepartmentPathItem> findPath(UUID tenantId, UUID departmentId) {
            return List.of();
        }

        @Override
        public DepartmentImpactCounts countImpact(UUID tenantId, UUID departmentId) {
            return new DepartmentImpactCounts(0, 0, 0, 0, false);
        }

        @Override
        public boolean isDescendant(UUID tenantId, UUID ancestorId, UUID possibleDescendantId) {
            return false;
        }

        @Override
        public int subtreeHeight(UUID tenantId, UUID departmentId) {
            return 0;
        }

        @Override
        public boolean hasDisabledAncestor(UUID tenantId, UUID departmentId) {
            return false;
        }

        @Override
        public int move(UUID tenantId, UUID departmentId, UUID newParentId, int version, UUID actorId, String reason) {
            movedDepartmentId = departmentId;
            return version + 1;
        }

        @Override
        public UUID create(UUID tenantId, UUID parentId, String name, String code, DepartmentNodeType nodeType,
                           UUID leaderUserId, int sortOrder, DepartmentStatus status, String remark, UUID actorId) {
            return UUID.randomUUID();
        }

        @Override
        public int update(UUID tenantId, UUID departmentId, String name, UUID leaderUserId, int sortOrder,
                          String remark, int version, UUID actorId, String reason) {
            return version + 1;
        }

        @Override
        public int changeStatus(UUID tenantId, UUID departmentId, DepartmentStatus status, boolean cascade,
                                int version, UUID actorId, String reason) {
            return 1;
        }

        @Override
        public int softDelete(UUID tenantId, UUID departmentId, int version, UUID actorId, String reason) {
            return version + 1;
        }
    }

    /** 提供一次固定预览绑定并记录消费令牌的测试存储。 */
    private static final class FakePreviewTokenStore implements DepartmentPreviewTokenStore {
        private PreviewTokenBinding binding;
        private String consumedToken;

        @Override
        public String save(PreviewTokenBinding binding) {
            this.binding = binding;
            return "preview-token";
        }

        @Override
        public Optional<PreviewTokenBinding> consume(String token) {
            consumedToken = token;
            return Optional.ofNullable(binding);
        }
    }

    /** 收集应用服务发布的部门变化事件。 */
    private static final class CapturingEventPublisher implements DepartmentChangeEventPublisher {
        private final List<Object> events = new ArrayList<>();

        @Override
        public void publish(Object event) {
            events.add(event);
        }
    }
}
