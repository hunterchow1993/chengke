package com.chengke.chengkecrmbackend.modules.system.user.controller;

import com.chengke.chengkecrmbackend.modules.system.user.application.UserCommandService;
import com.chengke.chengkecrmbackend.modules.system.user.application.UserQueryService;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.BatchDisableUsersRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.ChangeUserStatusRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.CreateUserRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.MobileAvailableRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.ResetPasswordRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UpdateUserRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UserListRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UserOrgSearchRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UsernameAvailableRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.mapper.UserApiMapper;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.AssignableDepartmentVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.AssignableRoleVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.AvailabilityCheckVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.BatchDisableResultVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.PageVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.ResetPasswordResultVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserCreateResultVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserDetailVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserListItemVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserOrgNodeVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserOrgTreeVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserStatusResultVO;
import com.chengke.chengkecrmbackend.modules.system.user.controller.vo.UserUpdateResultVO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 提供用户查询、新增、编辑、启停、重置密码和组织树 HTTP API。
 *
 * <p>所有方法从 JWT 安全上下文解析租户和操作者；Controller 仅做校验、映射和响应封装。</p>
 */
@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/v1/system/users")
public class UserController {
    private final UserQueryService queryService;
    private final UserCommandService commandService;
    private final UserApiMapper mapper;
    private final CurrentActorProvider actorProvider;

    /**
     * 创建用户 HTTP 控制器。
     *
     * @param queryService 用户查询用例
     * @param commandService 用户写用例
     * @param mapper HTTP 边界转换器
     * @param actorProvider JWT 操作者上下文提供者
     */
    public UserController(UserQueryService queryService, UserCommandService commandService,
                          UserApiMapper mapper, CurrentActorProvider actorProvider) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.mapper = mapper;
        this.actorProvider = actorProvider;
    }

    /**
     * 分页查询用户。权限：system:user:view；成功 HTTP 200。
     *
     * @param request 组织节点、筛选和分页
     * @return 范围内分页用户
     */
    @Operation(summary = "分页查询用户", description = "需要 system:user:view")
    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping
    public ApiResponse<PageVO<UserListItemVO>> list(@Valid @ParameterObject UserListRequest request) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toPageVO(queryService.list(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 校验用户名可用性。权限：system:user:create 或 update；成功 HTTP 200。
     *
     * @param request 用户名与可选排除用户
     * @return 是否可用
     */
    @Operation(summary = "校验用户名可用性", description = "需要 system:user:create 或 system:user:update")
    @PreAuthorize("hasAnyAuthority('system:user:create','system:user:update')")
    @GetMapping("/username-available")
    public ApiResponse<AvailabilityCheckVO> usernameAvailable(
            @Valid @ParameterObject UsernameAvailableRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(queryService.usernameAvailable(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 校验手机号可用性。权限：system:user:create 或 update；成功 HTTP 200。
     *
     * @param request 手机号与可选排除用户
     * @return 是否可用
     */
    @Operation(summary = "校验手机号可用性", description = "需要 system:user:create 或 system:user:update")
    @PreAuthorize("hasAnyAuthority('system:user:create','system:user:update')")
    @GetMapping("/mobile-available")
    public ApiResponse<AvailabilityCheckVO> mobileAvailable(
            @Valid @ParameterObject MobileAvailableRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(queryService.mobileAvailable(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 查询用户管理组织树。权限：system:user:view；成功 HTTP 200。
     *
     * @return 默认可见节点与默认选中节点
     */
    @Operation(summary = "查询用户管理组织树", description = "需要 system:user:view")
    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/organization-tree")
    public ApiResponse<UserOrgTreeVO> organizationTree() {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(queryService.getOrganizationTree(mapper.toOrgTreeQuery(actor))), actor);
    }

    /**
     * 搜索组织节点。权限：system:user:view；成功 HTTP 200。
     *
     * @param request 名称关键词
     * @return 匹配节点及其祖先
     */
    @Operation(summary = "搜索组织节点", description = "需要 system:user:view")
    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/organization-tree/search")
    public ApiResponse<List<UserOrgNodeVO>> searchOrganization(
            @Valid @ParameterObject UserOrgSearchRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toOrgNodeVOs(queryService.searchOrganization(mapper.toQuery(actor, request))), actor);
    }

    /**
     * 懒加载组织树子节点。权限：system:user:view；成功 HTTP 200。
     *
     * @param nodeId 父节点
     * @return 可见直接下级
     */
    @Operation(summary = "懒加载组织树子节点", description = "需要 system:user:view")
    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/organization-tree/{nodeId}/children")
    public ApiResponse<List<UserOrgNodeVO>> organizationChildren(@PathVariable UUID nodeId) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toOrgNodeVOs(
                queryService.getOrganizationChildren(mapper.toOrgChildrenQuery(actor, nodeId))), actor);
    }

    /**
     * 查询可分配部门。权限：system:user:create 或 update；成功 HTTP 200。
     *
     * @return 可管理且状态正常的部门
     */
    @Operation(summary = "查询可分配部门选项", description = "需要 system:user:create 或 system:user:update")
    @PreAuthorize("hasAnyAuthority('system:user:create','system:user:update')")
    @GetMapping("/assignable-departments")
    public ApiResponse<List<AssignableDepartmentVO>> assignableDepartments() {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toDepartmentVOs(
                queryService.assignableDepartments(mapper.toAssignableDepartmentsQuery(actor))), actor);
    }

    /**
     * 查询可分配角色。权限：system:user:create 或 update；成功 HTTP 200。
     *
     * @return 状态正常的角色
     */
    @Operation(summary = "查询可分配角色选项", description = "需要 system:user:create 或 system:user:update")
    @PreAuthorize("hasAnyAuthority('system:user:create','system:user:update')")
    @GetMapping("/assignable-roles")
    public ApiResponse<List<AssignableRoleVO>> assignableRoles() {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toRoleVOs(queryService.assignableRoles(mapper.toAssignableRolesQuery(actor))), actor);
    }

    /**
     * 查询用户详情。权限：system:user:view；成功 HTTP 200。
     *
     * @param id 用户标识
     * @return 编辑回填资料
     */
    @Operation(summary = "查询用户详情", description = "需要 system:user:view")
    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/{id}")
    public ApiResponse<UserDetailVO> getDetail(@PathVariable UUID id) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(queryService.getDetail(mapper.toDetailQuery(actor, id))), actor);
    }

    /**
     * 新增用户。权限：system:user:create；成功 HTTP 201。
     *
     * @param request 新用户字段
     * @return HTTP 201 和创建结果
     */
    @Operation(summary = "新增用户", description = "需要 system:user:create")
    @PreAuthorize("hasAuthority('system:user:create')")
    @PostMapping({"", "/"})
    public ResponseEntity<ApiResponse<UserCreateResultVO>> create(@Valid @RequestBody CreateUserRequest request) {
        CurrentActor actor = actorProvider.currentActor();
        var result = mapper.toVO(commandService.create(mapper.toCommand(actor, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(success(result, actor));
    }

    /**
     * 批量停用用户。权限：system:user:disable；成功 HTTP 200。
     *
     * @param request 待停用用户集合
     * @return 逐项结果
     */
    @Operation(summary = "批量停用用户", description = "需要 system:user:disable")
    @PreAuthorize("hasAuthority('system:user:disable')")
    @PostMapping("/batch-disable")
    public ApiResponse<BatchDisableResultVO> batchDisable(@Valid @RequestBody BatchDisableUsersRequest request) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.batchDisable(mapper.toCommand(actor, request))), actor);
    }

    /**
     * 重置用户密码。权限：system:user:reset-password；成功 HTTP 200。
     *
     * @param id 用户标识
     * @param request 新密码与改密开关
     * @return 不含密码的操作结果
     */
    @Operation(summary = "重置用户密码", description = "需要 system:user:reset-password")
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResultVO> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.resetPassword(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 编辑用户。权限：system:user:update；成功 HTTP 200。
     *
     * @param id 用户标识
     * @param request 可编辑字段与预期版本
     * @return 更新后版本和状态
     */
    @Operation(summary = "编辑用户", description = "需要 system:user:update")
    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/{id}")
    public ApiResponse<UserUpdateResultVO> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.update(mapper.toCommand(actor, id, request))), actor);
    }

    /**
     * 启用或停用单个用户。权限：system:user:disable；成功 HTTP 200。
     *
     * @param id 用户标识
     * @param request 目标状态
     * @return 变更后状态
     */
    @Operation(summary = "启用或停用单个用户", description = "需要 system:user:disable")
    @PreAuthorize("hasAuthority('system:user:disable')")
    @PatchMapping("/{id}/status")
    public ApiResponse<UserStatusResultVO> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        CurrentActor actor = actorProvider.currentActor();
        return success(mapper.toVO(commandService.changeStatus(mapper.toCommand(actor, id, request))), actor);
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
