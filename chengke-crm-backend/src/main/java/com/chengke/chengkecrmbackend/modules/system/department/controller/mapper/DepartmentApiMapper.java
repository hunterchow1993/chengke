package com.chengke.chengkecrmbackend.modules.system.department.controller.mapper;

import com.chengke.chengkecrmbackend.modules.system.department.application.command.*;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.*;
import com.chengke.chengkecrmbackend.modules.system.department.application.query.*;
import com.chengke.chengkecrmbackend.modules.system.department.application.result.*;
import com.chengke.chengkecrmbackend.modules.system.department.controller.dto.*;
import com.chengke.chengkecrmbackend.modules.system.department.controller.vo.*;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 在 HTTP DTO/VO 与应用 Command/Query/Result 之间执行显式边界转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class DepartmentApiMapper {

    /** @return 新增应用命令 */
    public CreateDepartmentCommand toCommand(CurrentActor actor, CreateDepartmentRequest request) {
        return new CreateDepartmentCommand(actor, request.parentId(), request.name(), request.code(),
                request.leaderUserId(), request.sortOrder(), request.status(), request.remark());
    }

    /** @return 编辑应用命令 */
    public UpdateDepartmentCommand toCommand(CurrentActor actor, UUID id, UpdateDepartmentRequest request) {
        return new UpdateDepartmentCommand(actor, id, request.name(), request.leaderUserId(), request.sortOrder(),
                request.remark(), request.version(), request.reason());
    }

    /** @return 移动预览命令 */
    public MoveDepartmentPreviewCommand toCommand(CurrentActor actor, UUID id, MoveDepartmentPreviewRequest request) {
        return new MoveDepartmentPreviewCommand(actor, id, request.newParentId(), request.version());
    }

    /** @return 移动执行命令 */
    public MoveDepartmentCommand toCommand(CurrentActor actor, UUID id, MoveDepartmentRequest request) {
        return new MoveDepartmentCommand(actor, id, request.newParentId(), request.version(),
                request.previewToken(), request.reason());
    }

    /** @return 启停预览命令 */
    public ChangeDepartmentStatusPreviewCommand toCommand(
            CurrentActor actor, UUID id, ChangeDepartmentStatusPreviewRequest request
    ) {
        return new ChangeDepartmentStatusPreviewCommand(actor, id, request.targetStatus(),
                request.cascade(), request.version());
    }

    /** @return 启停执行命令 */
    public ChangeDepartmentStatusCommand toCommand(
            CurrentActor actor, UUID id, ChangeDepartmentStatusRequest request
    ) {
        return new ChangeDepartmentStatusCommand(actor, id, request.targetStatus(), request.cascade(),
                request.version(), request.previewToken(), request.reason());
    }

    /** @return 删除命令 */
    public DeleteDepartmentCommand toCommand(CurrentActor actor, UUID id, DeleteDepartmentRequest request) {
        return new DeleteDepartmentCommand(actor, id, request.version(), request.previewToken(), request.reason());
    }

    /** @return 部门树应用查询 */
    public DepartmentTreeQuery toQuery(CurrentActor actor, DepartmentTreeRequest request) {
        return new DepartmentTreeQuery(actor, request.rootId(), request.selectedId());
    }

    /** @return 直接下级应用查询 */
    public DepartmentChildrenQuery toQuery(CurrentActor actor, UUID id, DepartmentChildrenRequest request) {
        return new DepartmentChildrenQuery(actor, id, request.includeDisabled());
    }

    /** @return 部门搜索应用查询 */
    public DepartmentSearchQuery toQuery(CurrentActor actor, DepartmentSearchRequest request) {
        return new DepartmentSearchQuery(actor, request.keyword(), request.page(), request.pageSize());
    }

    /** @return 可移动父节点应用查询 */
    public MovableParentQuery toQuery(CurrentActor actor, UUID id, DepartmentMovableParentRequest request) {
        return new MovableParentQuery(actor, id, request.keyword(), request.limit());
    }

    /** @return 负责人候选应用查询 */
    public LeaderCandidateQuery toQuery(CurrentActor actor, LeaderCandidateRequest request) {
        return new LeaderCandidateQuery(actor, request.keyword(), request.page(), request.pageSize());
    }

    /** @return 树节点 HTTP 响应 */
    public abstract DepartmentTreeNodeVO toVO(DepartmentTreeNodeResult result);
    /** @return 节点能力 HTTP 响应 */
    public abstract DepartmentCapabilitiesVO toVO(DepartmentCapabilitiesResult result);
    /** @return 写操作 HTTP 响应 */
    public abstract DepartmentOperationVO toVO(DepartmentOperationResult result);
    /** @return 负责人候选 HTTP 响应 */
    public abstract DepartmentLeaderCandidateVO toVO(DepartmentLeaderCandidate result);

    /** @return 路径 HTTP 响应，默认不是只读祖先 */
    @Mapping(target = "readOnlyAncestor", constant = "false")
    public abstract DepartmentPathItemVO toVO(DepartmentPathItem result);

    /** @return 搜索项 HTTP 响应 */
    public abstract DepartmentSearchItemVO toVO(DepartmentSearchItemResult result);

    /** @return 影响预览 HTTP 响应 */
    public abstract DepartmentImpactPreviewVO toVO(DepartmentImpactPreviewResult result);

    /** @return 树节点响应列表 */
    public List<DepartmentTreeNodeVO> toTreeVOs(List<DepartmentTreeNodeResult> values) {
        return values.stream().map(this::toVO).toList();
    }

    /** @return 搜索分页 HTTP 响应 */
    public PageVO<DepartmentSearchItemVO> toSearchPageVO(PageResult<DepartmentSearchItemResult> result) {
        return new PageVO<>(result.items().stream().map(this::toVO).toList(),
                result.page(), result.pageSize(), result.total());
    }

    /** @return 负责人候选分页 HTTP 响应 */
    public PageVO<DepartmentLeaderCandidateVO> toLeaderPageVO(PageResult<DepartmentLeaderCandidate> result) {
        return new PageVO<>(result.items().stream().map(this::toVO).toList(),
                result.page(), result.pageSize(), result.total());
    }

    /**
     * 组装部门详情 HTTP 响应，包括明确删除阻塞项。
     *
     * @param result 应用详情结果
     * @return 不暴露持久化对象的详情 VO
     */
    public DepartmentDetailVO toVO(DepartmentDetailResult result) {
        var node = result.department();
        var blockers = new ArrayList<DepartmentDeleteBlockerVO>();
        addBlocker(blockers, "children", result.counts().descendantCount(), "存在下级部门", "请先移动或删除下级");
        addBlocker(blockers, "members", result.counts().directMemberCount(), "存在直属成员", "请先调整成员部门");
        addBlocker(blockers, "roles", result.counts().roleReferenceCount(), "存在角色引用", "请先解除角色引用");
        addBlocker(blockers, "business", result.counts().businessReferenceCount(), "存在业务引用", "请先解除业务引用");
        return new DepartmentDetailVO(node.id(), node.parentId(), node.name(), node.code(), node.nodeType(),
                node.status(), node.depth(), result.path().stream().map(this::toVO).toList(),
                node.leaderUserId() == null ? null : new DepartmentLeaderVO(node.leaderUserId(), null, null, null),
                node.sortOrder(), node.remark(), node.version(), null, result.counts().descendantCount(),
                result.counts().roleReferenceCount(), result.counts().businessReferenceCount(),
                result.counts().hasHiddenReferences(), blockers, node.createdAt(), node.updatedAt(),
                new OperatorSummaryVO(node.updatedBy(), null),
                toVO(result.capabilities()));
    }

    /** 只在计数大于零时添加一条删除阻塞项。 */
    private void addBlocker(List<DepartmentDeleteBlockerVO> blockers, String type, int count,
                            String message, String nextAction) {
        if (count > 0) blockers.add(new DepartmentDeleteBlockerVO(type, count, message, nextAction));
    }
}
