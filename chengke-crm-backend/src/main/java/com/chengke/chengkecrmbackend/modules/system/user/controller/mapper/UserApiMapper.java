package com.chengke.chengkecrmbackend.modules.system.user.controller.mapper;

import com.chengke.chengkecrmbackend.modules.system.user.application.command.BatchDisableUsersCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.ChangeUserStatusCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.CreateUserCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.ResetUserPasswordCommand;
import com.chengke.chengkecrmbackend.modules.system.user.application.command.UpdateUserCommand;
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
import com.chengke.chengkecrmbackend.modules.system.user.application.result.BatchDisableResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.PageResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.ResetPasswordResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserCreateResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserDetailResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserListItemResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserOrgNodeResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserOrgTreeResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserStatusResult;
import com.chengke.chengkecrmbackend.modules.system.user.application.result.UserUpdateResult;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.BatchDisableUsersRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.ChangeUserStatusRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.CreateUserRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.MobileAvailableRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.ResetPasswordRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UpdateUserRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UserListRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UserOrgSearchRequest;
import com.chengke.chengkecrmbackend.modules.system.user.controller.dto.UsernameAvailableRequest;
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
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

/**
 * 在 HTTP DTO/VO 与应用 Command/Query/Result 之间执行显式边界转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class UserApiMapper {

    /** @return 新增命令 */
    public CreateUserCommand toCommand(CurrentActor actor, CreateUserRequest request) {
        return new CreateUserCommand(actor, request.avatarUrl(), request.name(), request.username(), request.mobile(),
                request.email(), request.departmentId(), request.roleId(), request.status(),
                request.initialPassword(), request.forcePasswordChange(), request.remark());
    }

    /** @return 编辑命令 */
    public UpdateUserCommand toCommand(CurrentActor actor, UUID id, UpdateUserRequest request) {
        return new UpdateUserCommand(actor, id, request.avatarUrl(), request.name(), request.mobile(), request.email(),
                request.departmentId(), request.roleId(), request.status(), request.remark(), request.version());
    }

    /** @return 启停命令 */
    public ChangeUserStatusCommand toCommand(CurrentActor actor, UUID id, ChangeUserStatusRequest request) {
        return new ChangeUserStatusCommand(actor, id, request.targetStatus());
    }

    /** @return 批量停用命令 */
    public BatchDisableUsersCommand toCommand(CurrentActor actor, BatchDisableUsersRequest request) {
        return new BatchDisableUsersCommand(actor, request.userIds());
    }

    /** @return 重置密码命令 */
    public ResetUserPasswordCommand toCommand(CurrentActor actor, UUID id, ResetPasswordRequest request) {
        return new ResetUserPasswordCommand(actor, id, request.newPassword(), request.forcePasswordChange());
    }

    /** @return 列表查询 */
    public UserListQuery toQuery(CurrentActor actor, UserListRequest request) {
        return new UserListQuery(actor, request.departmentId(), request.includeDescendants(), request.keyword(),
                request.status(), request.page(), request.pageSize());
    }

    /** @return 详情查询 */
    public GetUserDetailQuery toDetailQuery(CurrentActor actor, UUID id) {
        return new GetUserDetailQuery(actor, id);
    }

    /** @return 用户名可用性查询 */
    public UsernameAvailableQuery toQuery(CurrentActor actor, UsernameAvailableRequest request) {
        return new UsernameAvailableQuery(actor, request.username(), request.excludeUserId());
    }

    /** @return 手机号可用性查询 */
    public MobileAvailableQuery toQuery(CurrentActor actor, MobileAvailableRequest request) {
        return new MobileAvailableQuery(actor, request.mobile(), request.excludeUserId());
    }

    /** @return 组织树查询 */
    public UserOrgTreeQuery toOrgTreeQuery(CurrentActor actor) {
        return new UserOrgTreeQuery(actor);
    }

    /** @return 组织树子节点查询 */
    public UserOrgChildrenQuery toOrgChildrenQuery(CurrentActor actor, UUID nodeId) {
        return new UserOrgChildrenQuery(actor, nodeId);
    }

    /** @return 组织树搜索查询 */
    public UserOrgSearchQuery toQuery(CurrentActor actor, UserOrgSearchRequest request) {
        return new UserOrgSearchQuery(actor, request.keyword());
    }

    /** @return 可分配部门查询 */
    public AssignableDepartmentsQuery toAssignableDepartmentsQuery(CurrentActor actor) {
        return new AssignableDepartmentsQuery(actor);
    }

    /** @return 可分配角色查询 */
    public AssignableRolesQuery toAssignableRolesQuery(CurrentActor actor) {
        return new AssignableRolesQuery(actor);
    }

    public abstract UserListItemVO toVO(UserListItemResult result);

    public abstract UserDetailVO toVO(UserDetailResult result);

    public abstract UserCreateResultVO toVO(UserCreateResult result);

    public abstract UserUpdateResultVO toVO(UserUpdateResult result);

    public abstract UserStatusResultVO toVO(UserStatusResult result);

    public abstract ResetPasswordResultVO toVO(ResetPasswordResult result);

    public abstract AvailabilityCheckVO toVO(AvailabilityResult result);

    public abstract UserOrgNodeVO toVO(UserOrgNodeResult result);

    public abstract AssignableDepartmentVO toVO(AssignableDepartmentResult result);

    public abstract AssignableRoleVO toVO(AssignableRoleResult result);

    public abstract List<UserOrgNodeVO> toOrgNodeVOs(List<UserOrgNodeResult> results);

    public abstract List<AssignableDepartmentVO> toDepartmentVOs(List<AssignableDepartmentResult> results);

    public abstract List<AssignableRoleVO> toRoleVOs(List<AssignableRoleResult> results);

    /** @return 列表分页视图 */
    public PageVO<UserListItemVO> toPageVO(PageResult<UserListItemResult> result) {
        return new PageVO<>(result.items().stream().map(this::toVO).toList(), result.page(), result.pageSize(),
                result.total());
    }

    /** @return 组织树视图 */
    public UserOrgTreeVO toVO(UserOrgTreeResult result) {
        return new UserOrgTreeVO(toOrgNodeVOs(result.nodes()), result.defaultSelectedId());
    }

    /** @return 批量停用视图 */
    public BatchDisableResultVO toVO(BatchDisableResult result) {
        return new BatchDisableResultVO(
                result.total(), result.successCount(), result.failedCount(),
                result.results().stream()
                        .map(item -> new BatchDisableResultVO.BatchDisableItemVO(
                                item.userId(), item.success(), item.errorCode(), item.message()))
                        .toList()
        );
    }
}
