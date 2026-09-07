package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 组织树搜索参数。 */
@Schema(description = "组织树搜索参数")
public record UserOrgSearchRequest(
        @NotBlank @Size(max = 50) String keyword
) {
    /** 去除首尾空格；全空白视为空值由 @NotBlank 拒绝。 */
    public UserOrgSearchRequest {
        keyword = keyword == null ? null : keyword.trim();
    }
}
