package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

/** 新增用户 HTTP 请求体，不接受租户、操作者和确认密码字段。 */
@Schema(description = "新增用户请求")
public record CreateUserRequest(
        @Size(max = 500) String avatarUrl,
        @NotBlank @Size(min = 2, max = 30)
        @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z· ]+$") String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9._-]{3,31}$") String username,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String mobile,
        @Email @Size(max = 100) String email,
        @NotNull UUID departmentId,
        @NotNull UUID roleId,
        UserStatus status,
        @NotBlank @Size(min = 8, max = 32) String initialPassword,
        Boolean forcePasswordChange,
        @Size(max = 200) @Pattern(regexp = "^[^\\p{Cntrl}]*$") String remark
) {
    /** 统一状态、改密开关、邮箱小写和姓名去空格缺省值。 */
    public CreateUserRequest {
        name = name == null ? null : name.trim();
        status = status == null ? UserStatus.ACTIVE : status;
        forcePasswordChange = forcePasswordChange == null || forcePasswordChange;
        email = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
        remark = remark == null || remark.isBlank() ? null : remark;
    }
}
