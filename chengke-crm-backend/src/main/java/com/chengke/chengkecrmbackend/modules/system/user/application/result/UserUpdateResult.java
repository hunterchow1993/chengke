package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 编辑用户结果。 */
public record UserUpdateResult(UUID userId, int version, UserStatus status, OffsetDateTime updatedAt) {
}
