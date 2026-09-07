package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import com.chengke.chengkecrmbackend.modules.system.user.domain.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 新增用户结果。 */
public record UserCreateResultVO(UUID userId, UserStatus status, OffsetDateTime updatedAt) {
}
