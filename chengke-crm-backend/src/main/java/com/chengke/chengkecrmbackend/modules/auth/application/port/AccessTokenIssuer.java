package com.chengke.chengkecrmbackend.modules.auth.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * 签发可供资源服务器校验的访问令牌。
 */
public interface AccessTokenIssuer {

    /**
     * @param tenantId 租户
     * @param userId 用户
     * @param sessionId 会话 jti
     * @param permissions 功能权限码
     * @param manageableDepartmentIds 可管理部门
     * @param expiresAt 过期时间
     * @param permissionVersion 签发时的租户授权版本
     * @return 访问令牌字符串
     */
    String issue(UUID tenantId, UUID userId, UUID sessionId, Collection<String> permissions,
                 Collection<UUID> manageableDepartmentIds, boolean forcePasswordChange, Instant expiresAt,
                 String permissionVersion);
}
