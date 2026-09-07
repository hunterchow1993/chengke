package com.chengke.chengkecrmbackend.modules.auth.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 仍有效的登录会话，供刷新凭证在没有 Access Token 时定位用户。
 */
public record AuthSessionRecord(UUID id, UUID tenantId, UUID userId, Instant expiresAt) {
}
