package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

/** 编辑用户 HTTP 请求体；不含用户名与密码。 */
@Schema(description = "编辑用户请求")
public record UpdateUserRequest(
        @Size(max = 500) String avatarUrl,
        @NotBlank @Size(min = 2, max = 30)
        @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z· ]+$") String name,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String mobile,
        @Email @Size(max = 100) String email,
        @NotNull UUID departmentId,
        @NotNull UUID roleId,
        @NotNull UserStatus status,
        @Size(max = 200) @Pattern(regexp = "^[^\\p{Cntrl}]*$") String remark,
        @NotNull @Positive Integer version
) {
    /** 统一邮箱小写和姓名去空格。 */
    public UpdateUserRequest {
        name = name == null ? null : name.trim();
        email = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
        remark = remark == null || remark.isBlank() ? null : remark;
    }
}
