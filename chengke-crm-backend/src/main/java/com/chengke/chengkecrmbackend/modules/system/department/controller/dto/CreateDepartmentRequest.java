package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.department.domain.model.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** 新增部门 HTTP 请求体，不接受租户和操作者字段。 */
@Schema(description = "新增部门请求")
public record CreateDepartmentRequest(
        @Schema(description = "上级部门；仅租户级初始化根节点时可为空") UUID parentId,
        @NotBlank @Size(min = 2, max = 50) String name,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$") String code,
        UUID leaderUserId,
        @Min(0) @Max(9999) Integer sortOrder,
        DepartmentStatus status,
        @Size(max = 200) String remark
) {
    /** 统一排序和状态缺省值。 */
    public CreateDepartmentRequest {
        sortOrder = sortOrder == null ? 100 : sortOrder;
        status = status == null ? DepartmentStatus.ACTIVE : status;
    }
}
