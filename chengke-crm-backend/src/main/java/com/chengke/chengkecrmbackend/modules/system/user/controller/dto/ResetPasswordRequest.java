package com.chengke.chengkecrmbackend.modules.system.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 重置密码请求体。 */
@Schema(description = "重置用户密码请求")
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 32) String newPassword,
        Boolean forcePasswordChange
) {
    /** 改密开关默认 true。 */
    public ResetPasswordRequest {
        forcePasswordChange = forcePasswordChange == null || forcePasswordChange;
    }
}
