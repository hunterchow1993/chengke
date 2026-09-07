package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 重置密码结果，不含新密码。 */
public record ResetPasswordResult(UUID userId, boolean forcePasswordChange, OffsetDateTime updatedAt) {
}
