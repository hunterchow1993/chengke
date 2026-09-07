package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 编辑用户结果。 */
public record UserUpdateResultVO(UUID userId, int version, UserStatus status, OffsetDateTime updatedAt) {
}
