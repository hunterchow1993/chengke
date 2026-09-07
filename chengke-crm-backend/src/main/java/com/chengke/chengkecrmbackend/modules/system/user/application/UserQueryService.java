package com.chengke.chengkecrmbackend.modules.system.user.application;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserDepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.model.UserRecord;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.AssignableDepartmentsQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.AssignableRolesQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.GetUserDetailQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.MobileAvailableQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UserListQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UserOrgChildrenQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UserOrgSearchQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UserOrgTreeQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.query.UsernameAvailableQuery;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.AssignableDepartmentResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.AssignableRoleResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.AvailabilityResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.PageResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserDetailResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserListItemResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserOrgNodeResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserOrgTreeResult;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 执行用户列表、详情、可用性、组织树和可分配选项查询。
 *
 * <p>列表范围进入查询条件；敏感字段在返回前按权限脱敏。</p>
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService {
    private static final String VIEW = "system:user:view";
    private static final String CREATE = "system:user:create";
    private static final String UPDATE = "system:user:update";
    private static final String MOBILE_FULL = "system:user:mobile:full";
    private static final String EMAIL_FULL = "system:user:email:full";

    private final UserPersistencePort persistence;
    private final UserPolicy policy;

    /**
     * 创建用户查询服务。
     *
     * @param persistence 用户持久化端口
     * @param policy 脱敏规则
     */
    public UserQueryService(UserPersistencePort persistence, UserPolicy policy) {
        this.persistence = persistence;
        this.policy = policy;
    }

    /**
     * 在组织与数据范围内分页检索用户。
     *
     * @param query 选中节点、是否含下级、关键词、状态和分页
     * @return 范围内分页结果；手机号按权限脱敏
     */
    public PageResult<UserListItemResult> list(UserListQuery query) {
        requirePermission(query.actor(), VIEW);
        requireVisibleDepartment(query.actor(), query.departmentId());
        List<UUID> effective = effectiveDepartmentIds(query.actor(), query.departmentId(), query.includeDescendants());
        if (effective.isEmpty()) {
            return new PageResult<>(List.of(), query.page(), query.pageSize(), 0);
        }
        String status = query.status() == null ? null : query.status().databaseValue();
        int offset = (query.page() - 1) * query.pageSize();
        long total = persistence.countPage(query.actor().tenantId(), effective, query.keyword(), status);
        List<UserListItemResult> items = persistence.findPage(
                        query.actor().tenantId(), effective, query.keyword(), status, offset, query.pageSize())
                .stream()
                .map(user -> toListItem(query.actor(), user))
                .toList();
        return new PageResult<>(items, query.page(), query.pageSize(), total);
    }

    /**
     * 查询编辑回填所需的用户详情。
     *
     * @param query 目标用户
     * @return 脱敏后的详情；不含密码
     */
    public UserDetailResult getDetail(GetUserDetailQuery query) {
        requirePermission(query.actor(), VIEW);
        UserRecord user = requireManageableUser(query.actor(), query.userId());
        return new UserDetailResult(
                user.id(), user.name(), user.username(), user.avatarUrl(),
                maskMobile(query.actor(), user.mobile()),
                maskEmail(query.actor(), user.email()),
                user.departmentId(), user.departmentName(), user.roleId(), user.roleName(),
                user.status(), user.remark(), user.version(), user.updatedAt()
        );
    }

    /**
     * 校验租户内用户名是否可用。
     *
     * @param query 用户名与可选排除用户
     * @return available=true 表示无冲突
     */
    public AvailabilityResult usernameAvailable(UsernameAvailableQuery query) {
        requireCreateOrUpdate(query.actor());
        boolean taken = persistence.existsUsername(query.actor().tenantId(), query.username(), query.excludeUserId());
        return new AvailabilityResult(!taken);
    }

    /**
     * 校验租户内手机号是否可用。
     *
     * @param query 手机号与可选排除用户
     * @return available=true 表示无冲突
     */
    public AvailabilityResult mobileAvailable(MobileAvailableQuery query) {
        requireCreateOrUpdate(query.actor());
        boolean taken = persistence.existsMobile(query.actor().tenantId(), query.mobile(), query.excludeUserId());
        return new AvailabilityResult(!taken);
    }

    /**
     * 查询默认加载的可见组织树节点。
     *
     * @param query 当前操作者
     * @return 扁平节点列表与默认选中节点
     */
    public UserOrgTreeResult getOrganizationTree(UserOrgTreeQuery query) {
        requirePermission(query.actor(), VIEW);
        List<UserDepartmentRecord> all = persistence.findAllDepartments(query.actor().tenantId());
        Set<UUID> visible = visibleIds(query.actor(), all);
        if (visible.isEmpty()) {
            return new UserOrgTreeResult(List.of(), null);
        }
        UUID actorDepartmentId = persistence.findDepartmentIdByUserId(query.actor().tenantId(), query.actor().actorId())
                .orElse(null);
        Set<UUID> defaultIds = defaultLoadedIds(all, visible, actorDepartmentId);
        List<UserOrgNodeResult> nodes = toOrgNodes(query.actor(), all, visible, defaultIds);
        UUID selected = actorDepartmentId != null && visible.contains(actorDepartmentId) ? actorDepartmentId : null;
        return new UserOrgTreeResult(nodes, selected);
    }

    /**
     * 懒加载指定可见节点的直接子节点。
     *
     * @param query 父节点
     * @return 可见直接下级
     */
    public List<UserOrgNodeResult> getOrganizationChildren(UserOrgChildrenQuery query) {
        requirePermission(query.actor(), VIEW);
        List<UserDepartmentRecord> all = persistence.findAllDepartments(query.actor().tenantId());
        Set<UUID> visible = visibleIds(query.actor(), all);
        if (!visible.contains(query.nodeId())) {
            throw notFound();
        }
        Set<UUID> childIds = all.stream()
                .filter(node -> Objects.equals(node.parentId(), query.nodeId()) && visible.contains(node.id()))
                .map(node -> node.id())
                .collect(Collectors.toSet());
        return toOrgNodes(query.actor(), all, visible, childIds);
    }

    /**
     * 在可见节点内按名称搜索，并附带完整祖先路径。
     *
     * @param query 关键词
     * @return 匹配节点及其祖先的去重扁平列表
     */
    public List<UserOrgNodeResult> searchOrganization(UserOrgSearchQuery query) {
        requirePermission(query.actor(), VIEW);
        List<UserDepartmentRecord> all = persistence.findAllDepartments(query.actor().tenantId());
        Set<UUID> visible = visibleIds(query.actor(), all);
        Map<UUID, UserDepartmentRecord> byId = indexById(all);
        String keyword = query.keyword().toLowerCase();
        Set<UUID> returned = new HashSet<>();
        for (UserDepartmentRecord node : all) {
            if (!visible.contains(node.id()) || !node.name().toLowerCase().contains(keyword)) {
                continue;
            }
            UserDepartmentRecord cursor = node;
            while (cursor != null && returned.add(cursor.id())) {
                cursor = cursor.parentId() == null ? null : byId.get(cursor.parentId());
            }
        }
        return toOrgNodes(query.actor(), all, visible, returned);
    }

    /**
     * 查询操作者可管理且状态正常的部门。
     *
     * @param query 当前操作者
     * @return 扁平部门选项
     */
    public List<AssignableDepartmentResult> assignableDepartments(AssignableDepartmentsQuery query) {
        requireCreateOrUpdate(query.actor());
        return persistence.findAssignableDepartments(
                        query.actor().tenantId(), query.actor().manageableDepartmentIds(),
                        query.actor().manageAllDepartments())
                .stream()
                .map(node -> new AssignableDepartmentResult(node.id(), node.parentId(), node.name(), node.nodeType()))
                .toList();
    }

    /**
     * 查询状态正常的可分配角色。
     *
     * @param query 当前操作者
     * @return 角色选项；角色权限边界落地前返回全部 active 角色
     */
    public List<AssignableRoleResult> assignableRoles(AssignableRolesQuery query) {
        requireCreateOrUpdate(query.actor());
        return persistence.findActiveRoles(query.actor().tenantId()).stream()
                .map(role -> new AssignableRoleResult(role.id(), role.name()))
                .toList();
    }

    /**
     * 计算列表查询的有效部门集合：请求范围与可管理集合的交集。
     */
    private List<UUID> effectiveDepartmentIds(CurrentActor actor, UUID departmentId, boolean includeDescendants) {
        if (!actor.canManage(departmentId) && !actor.manageAllDepartments()) {
            if (!includeDescendants) {
                return List.of();
            }
            List<UUID> descendants = persistence.findDescendantIds(actor.tenantId(), departmentId, true);
            Set<UUID> manageable = actor.manageableDepartmentIds();
            return descendants.stream().filter(manageable::contains).toList();
        }
        return persistence.findDescendantIds(actor.tenantId(), departmentId, includeDescendants).stream()
                .filter(actor::canManage)
                .toList();
    }

    /** 校验部门存在且位于可见集合（可管理或只读祖先）。 */
    private void requireVisibleDepartment(CurrentActor actor, UUID departmentId) {
        persistence.findDepartment(actor.tenantId(), departmentId).orElseThrow(this::notFound);
        if (actor.canManage(departmentId)) {
            return;
        }
        if (!persistence.isVisibleAncestor(actor.tenantId(), departmentId, actor.manageableDepartmentIds())) {
            throw notFound();
        }
    }

    /** 读取范围内用户，范围外按不存在处理。 */
    private UserRecord requireManageableUser(CurrentActor actor, UUID userId) {
        UserRecord user = persistence.findById(actor.tenantId(), userId).orElseThrow(this::notFound);
        if (!actor.canManage(user.departmentId())) {
            throw notFound();
        }
        return user;
    }

    /** 计算可管理部门及其祖先组成的可见集合。 */
    private Set<UUID> visibleIds(CurrentActor actor, List<UserDepartmentRecord> all) {
        if (actor.manageAllDepartments()) {
            return all.stream().map(node -> node.id()).collect(Collectors.toSet());
        }
        Map<UUID, UserDepartmentRecord> byId = indexById(all);
        Set<UUID> visible = new HashSet<>();
        for (UUID id : actor.manageableDepartmentIds()) {
            UserDepartmentRecord cursor = byId.get(id);
            while (cursor != null && visible.add(cursor.id())) {
                cursor = cursor.parentId() == null ? null : byId.get(cursor.parentId());
            }
        }
        return visible;
    }

    /** 默认加载：根、首层，以及操作者所属部门的祖先路径。 */
    private Set<UUID> defaultLoadedIds(List<UserDepartmentRecord> all, Set<UUID> visible, UUID actorDepartmentId) {
        Map<UUID, UserDepartmentRecord> byId = indexById(all);
        Set<UUID> ids = new HashSet<>();
        for (UserDepartmentRecord node : all) {
            if (!visible.contains(node.id())) {
                continue;
            }
            if (node.parentId() == null || node.depth() == 1) {
                ids.add(node.id());
            }
        }
        UserDepartmentRecord cursor = actorDepartmentId == null ? null : byId.get(actorDepartmentId);
        while (cursor != null) {
            if (visible.contains(cursor.id())) {
                ids.add(cursor.id());
            }
            cursor = cursor.parentId() == null ? null : byId.get(cursor.parentId());
        }
        return ids;
    }

    /** 为指定节点集合附加权限范围内人数与是否可管理标记。 */
    private List<UserOrgNodeResult> toOrgNodes(
            CurrentActor actor, List<UserDepartmentRecord> all, Set<UUID> visible, Set<UUID> includeIds
    ) {
        if (includeIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> counts = persistence.countUsersByAncestor(
                actor.tenantId(), includeIds, actor.manageableDepartmentIds(), actor.manageAllDepartments());
        List<UserOrgNodeResult> nodes = new ArrayList<>();
        for (UserDepartmentRecord node : all) {
            if (!includeIds.contains(node.id())) {
                continue;
            }
            boolean hasChildren = all.stream()
                    .anyMatch(child -> Objects.equals(child.parentId(), node.id()) && visible.contains(child.id()));
            nodes.add(new UserOrgNodeResult(
                    node.id(), node.parentId(), node.name(), node.nodeType(), node.status(), node.depth(),
                    hasChildren, counts.getOrDefault(node.id(), 0), actor.canManage(node.id())
            ));
        }
        return nodes;
    }

    private UserListItemResult toListItem(CurrentActor actor, UserRecord user) {
        return new UserListItemResult(
                user.id(), user.name(), user.username(), user.avatarUrl(),
                maskMobile(actor, user.mobile()),
                user.departmentId(), user.departmentName(), user.roleId(), user.roleName(),
                user.status(), user.updatedAt()
        );
    }

    private String maskMobile(CurrentActor actor, String mobile) {
        return actor.hasPermission(MOBILE_FULL) ? mobile : policy.maskMobile(mobile);
    }

    private String maskEmail(CurrentActor actor, String email) {
        if (email == null || actor.hasPermission(EMAIL_FULL)) {
            return email;
        }
        return policy.maskEmail(email);
    }

    private Map<UUID, UserDepartmentRecord> indexById(List<UserDepartmentRecord> all) {
        Map<UUID, UserDepartmentRecord> byId = new HashMap<>();
        all.forEach(node -> byId.put(node.id(), node));
        return byId;
    }

    private void requireCreateOrUpdate(CurrentActor actor) {
        if (!actor.hasPermission(CREATE) && !actor.hasPermission(UPDATE)) {
            throw forbidden();
        }
    }

    private void requirePermission(CurrentActor actor, String permission) {
        if (!actor.hasPermission(permission)) {
            throw forbidden();
        }
    }

    private BusinessException forbidden() {
        return new BusinessException("USER_OPERATION_FORBIDDEN", 403, "无操作权限");
    }

    private BusinessException notFound() {
        return new BusinessException("USER_NOT_FOUND", 404, "用户不存在");
    }
}
