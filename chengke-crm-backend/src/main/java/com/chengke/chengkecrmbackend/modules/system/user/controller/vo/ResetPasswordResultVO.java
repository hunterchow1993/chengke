package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 重置密码结果，不含新密码。 */
public record ResetPasswordResultVO(UUID userId, boolean forcePasswordChange, OffsetDateTime updatedAt) {
}
