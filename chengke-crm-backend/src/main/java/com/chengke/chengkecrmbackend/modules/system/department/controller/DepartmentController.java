package com.chengke.chengkecrmbackend.modules.system.department.controller;

import com.chengke.chengkecrmbackend.modules.system.department.application.DepartmentCommandService;
import com.chengke.chengkecrmbackend.modules.system.department.application.DepartmentQueryService;
import com.chengke.chengkecrmbackend.modules.system.department.application.query.GetDepartmentDetailQuery;
import com.chengke.chengkecrmbackend.modules.system.department.controller.dto.*;
import com.chengke.chengkecrmbackend.modules.system.department.controller.mapper.DepartmentApiMapper;
import com.chengke.chengkecrmbackend.modules.system.department.controller.vo.*;
import com.chengke.chengkecrmbackend.shared.api.ApiResponse;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import com.chengke.chengkecrmbackend.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 提供部门树查询与新增、编辑、移动、启停、软删除 HTTP API。
 *
 * <p>所有方法从 JWT 安全上下文解析租户和操作者；Controller 仅做校验、映射和响应封装。
 * 写接口的数据库、审计、缓存和授权版本副作用由 Application Service 负责。</p>
 */
@Tag(name = "部门管理")
@Validated
@RestController
@RequestMapping("/api/v1/system/departments")
public class DepartmentController {
    private final DepartmentQueryService queryService;
    private final DepartmentCommandService commandService;
    private final DepartmentApiMapper mapper;
    private final CurrentActorProvider actorProvider;

    /**
     * 创建部门 HTTP 控制器。
     *
     * @param queryService 部门查询用例
     * @param commandService 部门写用例
     * @param mapper HTTP 边界转换器
     * @param actorProvider JWT 操作者上下文提供者
     */
    public DepartmentController(DepartmentQueryService queryService, DepartmentCommandService commandService,
                                DepartmentApiMapper mapper, CurrentActorProvider actorProvider) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.mapper = mapper;
        this.actorProvider = actorProvider;
    }

    /**
     * 查询可见部门树。权限：system:department:view；成功 HTTP 200。
     *
     * @param request 树定位参数
     * @return 可见节点及只读祖先
     */
    @Operation(summary = "查询部门树", description = "需要 system:department:view")
    @PreAuthorize("hasAuthority('system:department:view')")
    @GetMapping("/tree")
    public ApiResponse<List<DepartmentTreeNodeVO>> getTree(@Valid @ParameterObject DepartmentTreeRequest request) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toTreeVOs(queryService.getTree(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 查询直接下级。权限：system:department:view；成功 HTTP 200。
     *
     * @param id 父部门标识
     * @param request 是否包含停用节点
     * @return 可见直接下级
     */
    @Operation(summary = "查询部门直接下级", description = "需要 system:department:view")
    @PreAuthorize("hasAuthority('system:department:view')")
    @GetMapping("/{id}/children")
    public ApiResponse<List<DepartmentTreeNodeVO>> getChildren(
            @PathVariable UUID id, @Valid @ParameterObject DepartmentChildrenRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toTreeVOs(queryService.getChildren(mapper.toQuery(actor, id, request))), actor);
    }

    /**
     * 搜索部门。权限：system:department:view；成功 HTTP 200。
     *
     * @param request 关键词与分页条件
     * @return 部门搜索分页结果
     */
    @Operation(summary = "搜索部门", description = "需要 system:department:view")
    @PreAuthorize("hasAuthority('system:department:view')")
    @GetMapping("/search")
    public ApiResponse<PageVO<DepartmentSearchItemVO>> search(
            @Valid @ParameterObject DepartmentSearchRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toSearchPageVO(queryService.search(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 查询部门详情。权限：system:department:view；成功 HTTP 200。
     *
     * @param id 部门标识
     * @return 部门详情、统计与能力
     */
    @Operation(summary = "查询部门详情", description = "需要 system:department:view")
    @PreAuthorize("hasAuthority('system:department:view')")
    @GetMapping("/{id}")
    public ApiResponse<DepartmentDetailVO> getDetail(@PathVariable UUID id) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(queryService.getDetail(new GetDepartmentDetailQuery(actor, id))), actor);
    }

    /**
     * 查询可移动父节点。权限：system:department:move；成功 HTTP 200。
     *
     * @param id 待移动部门标识
     * @param request 关键词与候选数量
     * @return 合法父节点候选
     */
    @Operation(summary = "查询可移动父节点", description = "需要 system:department:move")
    @PreAuthorize("hasAuthority('system:department:move')")
    @GetMapping("/{id}/movable-parents")
    public ApiResponse<List<DepartmentTreeNodeVO>> getMovableParents(
            @PathVariable UUID id, @Valid @ParameterObject DepartmentMovableParentRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toTreeVOs(queryService.getMovableParents(mapper.toQuery(actor, id, request))), actor);
    }

    /**
     * 查询负责人候选。权限：system:department:create 或 update；成功 HTTP 200。
     *
     * @param request 关键词与分页条件
     * @return 同租户正常用户候选
     */
    @Operation(summary = "查询负责人候选", description = "需要 system:department:create 或 system:department:update")
    @PreAuthorize("hasAnyAuthority('system:department:create','system:department:update')")
    @GetMapping("/leader-candidates")
    public ApiResponse<PageVO<DepartmentLeaderCandidateVO>> getLeaderCandidates(
            @Valid @ParameterObject LeaderCandidateRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toLeaderPageVO(queryService.getLeaderCandidates(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 新增部门。权限：system:department:create；成功 HTTP 201。
     * 副作用由 Application Service 写节点、闭包、审计并在提交后失效缓存。
     *
     * @param request 新部门字段
     * @return HTTP 201 和操作结果
     */
    @Operation(summary = "新增部门", description = "需要 system:department:create")
    @PreAuthorize("hasAuthority('system:department:create')")
    @PostMapping({"", "/"})
    public ResponseEntity<ApiResponse<DepartmentOperationVO>> create(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        var result = mapper.toVO(commandService.create(mapper.toCommand(actor, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(success(result, actor));
    }

    /**
     * 编辑部门。权限：system:department:update；成功 HTTP 200。
     *
     * @param id 部门标识
     * @param request 可编辑字段与预期版本
     * @return 更新后版本和状态
     */
    @Operation(summary = "编辑部门", description = "需要 system:department:update")
    @PreAuthorize("hasAuthority('system:department:update')")
    @PutMapping("/{id}")
    public ApiResponse<DepartmentOperationVO> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.update(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 预览移动影响。权限：system:department:move；成功 HTTP 200。
     * 副作用仅为签发五分钟一次性预览令牌。
     *
     * @param id 待移动部门标识
     * @param request 目标父节点与预期版本
     * @return 路径、影响计数、风险和令牌
     */
    @Operation(summary = "预览移动影响", description = "需要 system:department:move")
    @PreAuthorize("hasAuthority('system:department:move')")
    @PostMapping("/{id}/move-preview")
    public ApiResponse<DepartmentImpactPreviewVO> previewMove(
            @PathVariable UUID id, @Valid @RequestBody MoveDepartmentPreviewRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.previewMove(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 移动部门。权限：system:department:move；成功 HTTP 200。
     * 副作用为消费预览、移动整棵子树、审计、缓存失效和授权版本提升。
     *
     * @param id 待移动部门标识
     * @param request 目标、版本、令牌和原因
     * @return 移动后版本和影响数量
     */
    @Operation(summary = "移动部门", description = "需要 system:department:move")
    @PreAuthorize("hasAuthority('system:department:move')")
    @PostMapping("/{id}/move")
    public ApiResponse<DepartmentOperationVO> move(
            @PathVariable UUID id, @Valid @RequestBody MoveDepartmentRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.move(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 预览启停影响。权限：system:department:status；成功 HTTP 200。
     * 副作用仅为签发五分钟一次性预览令牌。
     *
     * @param id 部门标识
     * @param request 目标状态、级联选项和版本
     * @return 影响计数、风险和令牌
     */
    @Operation(summary = "预览启停影响", description = "需要 system:department:status")
    @PreAuthorize("hasAuthority('system:department:status')")
    @PostMapping("/{id}/status-preview")
    public ApiResponse<DepartmentImpactPreviewVO> previewStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeDepartmentStatusPreviewRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.previewStatus(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 启用或停用部门。权限：system:department:status；成功 HTTP 200。
     * 副作用为消费预览、写状态和审计、缓存失效及授权版本提升。
     *
     * @param id 部门标识
     * @param request 目标状态、级联、版本和预览令牌
     * @return 操作后状态和影响数量
     */
    @Operation(summary = "启用或停用部门", description = "需要 system:department:status")
    @PreAuthorize("hasAuthority('system:department:status')")
    @PatchMapping("/{id}/status")
    public ApiResponse<DepartmentOperationVO> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeDepartmentStatusRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.changeStatus(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 软删除部门。权限：system:department:delete；成功 HTTP 200。
     * 副作用为写删除标记和审计，并在提交后清理组织与权限缓存。
     *
     * @param id 部门标识
     * @param request 版本、可选令牌和原因
     * @return 软删除后的版本和状态
     */
    @Operation(summary = "软删除部门", description = "需要 system:department:delete")
    @PreAuthorize("hasAuthority('system:department:delete')")
    @DeleteMapping("/{id}")
    public ApiResponse<DepartmentOperationVO> delete(
            @PathVariable UUID id, @Valid @ParameterObject DeleteDepartmentRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.delete(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 使用当前请求追踪标识封装成功响应。
     *
     * @param data 业务响应数据
     * @param actor 当前操作者上下文
     * @return code 为 OK 的统一响应
     */
    private <T> ApiResponse<T> success(T data, CurrentActor actor) {
        return ApiResponse.success(data, actor.requestId());
    }
}
