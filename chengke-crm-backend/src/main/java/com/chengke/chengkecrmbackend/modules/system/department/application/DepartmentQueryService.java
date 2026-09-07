package com.chengke.chengkecrmbackend.modules.system.department.application;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentRecord;
import com.chengke.chengkecrmbackend.modules.system.department.application.query.*;
import com.chengke.chengkecrmbackend.modules.system.department.application.result.*;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentNodeType;
import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;

/**
 * 执行部门树、下级、搜索、详情、可移动父节点和负责人候选查询。
 *
 * <p>所有数据库读取均携带 tenantId；结果在应用层按最新可管理范围及其只读祖先裁剪。</p>
 */
@Service
@Transactional(readOnly = true)
public class DepartmentQueryService {
    private final DepartmentPersistencePort persistence;
    private final LeaderDirectoryPort leaderDirectory;

    /**
     * 创建部门查询服务。
     *
     * @param persistence 部门持久化端口
     * @param leaderDirectory 同租户用户目录端口
     */
    public DepartmentQueryService(DepartmentPersistencePort persistence, LeaderDirectoryPort leaderDirectory) {
        this.persistence = persistence;
        this.leaderDirectory = leaderDirectory;
    }

    /**
     * 查询部门树。
     *
     * @param query 服务端操作者、局部根和选中节点
     * @return 可见节点及定位所需只读祖先
     */
    public List<DepartmentTreeNodeResult> getTree(DepartmentTreeQuery query) {
        requireView(query.actor());
        var all = persistence.findAll(query.actor().tenantId());
        var visible = visibleIds(query.actor(), all);
        return all.stream().filter(node -> visible.contains(node.id()))
                .filter(node -> query.rootId() == null || Objects.equals(node.id(), query.rootId())
                        || Objects.equals(node.parentId(), query.rootId()))
                .map(node -> toTreeNode(query.actor(), node, all, !query.actor().canManage(node.id())))
                .toList();
    }

    /**
     * 查询直接下级。
     *
     * @param query 服务端操作者、父节点和停用节点选项
     * @return 指定可见父节点的直接下级
     */
    public List<DepartmentTreeNodeResult> getChildren(DepartmentChildrenQuery query) {
        requireView(query.actor());
        var all = persistence.findAll(query.actor().tenantId());
        if (!visibleIds(query.actor(), all).contains(query.parentId())) {
            throw notFound();
        }
        return all.stream().filter(node -> Objects.equals(node.parentId(), query.parentId()))
                .filter(node -> query.includeDisabled() || node.status() == DepartmentStatus.ACTIVE)
                .filter(node -> visibleIds(query.actor(), all).contains(node.id()))
                .map(node -> toTreeNode(query.actor(), node, all, !query.actor().canManage(node.id())))
                .toList();
    }

    /**
     * 搜索可见部门。
     *
     * @param query 关键词和分页条件
     * @return 按名称或编码匹配并裁剪后的分页结果
     */
    public PageResult<DepartmentSearchItemResult> search(DepartmentSearchQuery query) {
        requireView(query.actor());
        String keyword = query.keyword().toLowerCase(Locale.ROOT);
        var all = persistence.findAll(query.actor().tenantId());
        var visible = visibleIds(query.actor(), all);
        var matched = all.stream().filter(node -> visible.contains(node.id()))
                .filter(node -> node.name().toLowerCase(Locale.ROOT).contains(keyword)
                        || node.code().toLowerCase(Locale.ROOT).contains(keyword))
                .map(node -> new DepartmentSearchItemResult(toTreeNode(query.actor(), node, all,
                        !query.actor().canManage(node.id())), persistence.findPath(query.actor().tenantId(), node.id()),
                        node.name().toLowerCase(Locale.ROOT).contains(keyword) ? "name" : "code"))
                .toList();
        return page(matched, query.page(), query.pageSize());
    }

    /**
     * 查询部门详情。
     *
     * @param query 服务端操作者和部门标识
     * @return 可见部门的最新详情、路径、引用统计和能力
     */
    public DepartmentDetailResult getDetail(GetDepartmentDetailQuery query) {
        requireView(query.actor());
        var all = persistence.findAll(query.actor().tenantId());
        if (!visibleIds(query.actor(), all).contains(query.departmentId())) {
            throw notFound();
        }
        var department = persistence.findById(query.actor().tenantId(), query.departmentId()).orElseThrow(this::notFound);
        return new DepartmentDetailResult(department, persistence.findPath(query.actor().tenantId(), department.id()),
                persistence.countImpact(query.actor().tenantId(), department.id()),
                capabilities(query.actor(), department));
    }

    /**
     * 查询可移动父节点。
     *
     * @param query 待移动部门、关键词和数量上限
     * @return 排除自身、后代、停用、越权与超深目标后的父节点候选
     */
    public List<DepartmentTreeNodeResult> getMovableParents(MovableParentQuery query) {
        requirePermission(query.actor(), "system:department:move");
        var source = requireManageable(query.actor(), query.departmentId());
        int height = persistence.subtreeHeight(query.actor().tenantId(), source.id());
        var all = persistence.findAll(query.actor().tenantId());
        Predicate<DepartmentRecord> keyword = node -> query.keyword() == null || query.keyword().isBlank()
                || node.name().toLowerCase(Locale.ROOT).contains(query.keyword().toLowerCase(Locale.ROOT))
                || node.code().toLowerCase(Locale.ROOT).contains(query.keyword().toLowerCase(Locale.ROOT));
        return all.stream().filter(node -> query.actor().canManage(node.id()))
                .filter(node -> !node.id().equals(source.id()) && node.status() == DepartmentStatus.ACTIVE)
                .filter(node -> node.depth() + 1 + height <= 10)
                .filter(node -> !persistence.isDescendant(query.actor().tenantId(), source.id(), node.id()))
                .filter(keyword).limit(query.limit())
                .map(node -> toTreeNode(query.actor(), node, all, false)).toList();
    }

    /**
     * 查询负责人候选。
     *
     * @param query 服务端操作者、关键词和分页条件
     * @return 同租户正常用户引用提供的负责人候选分页结果
     */
    public PageResult<com.chengke.chengkecrmbackend.modules.system.department.application.port.model.DepartmentLeaderCandidate>
    getLeaderCandidates(LeaderCandidateQuery query) {
        if (!query.actor().hasPermission("system:department:create")
                && !query.actor().hasPermission("system:department:update")) {
            throw new BusinessException("DEPARTMENT_OPERATION_FORBIDDEN", 403, "无负责人候选查看权限");
        }
        return page(leaderDirectory.search(query.actor().tenantId(), query.keyword()),
                query.page(), query.pageSize());
    }

    /** 计算可管理节点与其祖先组成的可见集合。 */
    private Set<UUID> visibleIds(CurrentActor actor, List<DepartmentRecord> all) {
        if (actor.manageAllDepartments()) {
            return all.stream().map(node -> node.id()).collect(java.util.stream.Collectors.toSet());
        }
        var byId = new HashMap<UUID, DepartmentRecord>();
        all.forEach(node -> byId.put(node.id(), node));
        var visible = new HashSet<UUID>();
        for (var id : actor.manageableDepartmentIds()) {
            var cursor = byId.get(id);
            while (cursor != null && visible.add(cursor.id())) {
                cursor = cursor.parentId() == null ? null : byId.get(cursor.parentId());
            }
        }
        return visible;
    }

    /** 把部门记录转换为当前操作者的树节点展示结果。 */
    private DepartmentTreeNodeResult toTreeNode(CurrentActor actor, DepartmentRecord node,
                                                List<DepartmentRecord> all, boolean readOnlyAncestor) {
        boolean hasChildren = all.stream().anyMatch(child -> Objects.equals(child.parentId(), node.id()));
        Integer members = actor.hasPermission("system:department:member:view")
                ? persistence.countImpact(actor.tenantId(), node.id()).directMemberCount() : null;
        return new DepartmentTreeNodeResult(node.id(), node.parentId(), node.name(), node.code(), node.nodeType(),
                node.status(), node.depth(), hasChildren, members, readOnlyAncestor, capabilities(actor, node));
    }

    /** 计算节点展示能力及主要禁用原因。 */
    private DepartmentCapabilitiesResult capabilities(CurrentActor actor, DepartmentRecord node) {
        boolean manageable = actor.canManage(node.id());
        boolean mutable = node.nodeType() != DepartmentNodeType.GROUP;
        var reasons = new LinkedHashMap<String, String>();
        if (!manageable) reasons.put("scope", "节点仅用于路径定位");
        if (!mutable) reasons.put("root", "集团根节点受保护");
        return new DepartmentCapabilitiesResult(true,
                manageable && node.status() == DepartmentStatus.ACTIVE && actor.hasPermission("system:department:create"),
                manageable && actor.hasPermission("system:department:update"),
                manageable && mutable && actor.hasPermission("system:department:move"),
                manageable && mutable && actor.hasPermission("system:department:status"),
                manageable && mutable && actor.hasPermission("system:department:delete"),
                actor.hasPermission("system:department:member:view"), reasons);
    }

    /** 读取当前管理范围内的节点，范围外返回不泄露存在性的 404。 */
    private DepartmentRecord requireManageable(CurrentActor actor, UUID id) {
        if (!actor.canManage(id)) throw notFound();
        return persistence.findById(actor.tenantId(), id).orElseThrow(this::notFound);
    }

    /** 校验查看权限。 */
    private void requireView(CurrentActor actor) {
        requirePermission(actor, "system:department:view");
    }

    /** 校验功能权限。 */
    private void requirePermission(CurrentActor actor, String permission) {
        if (!actor.hasPermission(permission)) {
            throw new BusinessException("DEPARTMENT_OPERATION_FORBIDDEN", 403, "无部门操作权限");
        }
    }

    /** @return 不泄露资源存在性的部门未找到异常 */
    private BusinessException notFound() {
        return new BusinessException("DEPARTMENT_NOT_FOUND", 404, "部门不存在");
    }

    /** 对已经完成权限和可见性裁剪的列表进行一页切片。 */
    private <T> PageResult<T> page(List<T> values, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, values.size());
        int to = Math.min(from + pageSize, values.size());
        return new PageResult<>(values.subList(from, to), page, pageSize, values.size());
    }
}
