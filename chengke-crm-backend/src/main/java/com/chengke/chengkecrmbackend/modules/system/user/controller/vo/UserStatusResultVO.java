package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 单个启停结果。 */
public record UserStatusResultVO(UUID userId, UserStatus status, int version, OffsetDateTime updatedAt) {
}
