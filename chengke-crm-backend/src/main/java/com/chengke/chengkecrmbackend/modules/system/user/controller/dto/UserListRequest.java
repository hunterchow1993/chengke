package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** 用户分页查询参数。 */
@Schema(description = "用户分页查询参数")
public record UserListRequest(
        @NotNull UUID departmentId,
        Boolean includeDescendants,
        @Size(max = 50) String keyword,
        UserStatus status,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer pageSize
) {
    /** 统一分页、包含下级和关键词去空格缺省值。 */
    public UserListRequest {
        includeDescendants = Boolean.TRUE.equals(includeDescendants);
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 10 : pageSize;
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
    }
}
