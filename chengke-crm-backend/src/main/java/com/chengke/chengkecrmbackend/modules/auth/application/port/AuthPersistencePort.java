package com.chengke.chengkecrmbackend.modules.auth.application.port;

import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthSessionRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.FailureState;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 登录模块对用户、会话、验证码、失败计数和审计的持久化端口。
 */
public interface AuthPersistencePort {

    List<AuthUserRecord> findByUsername(String username);

    List<AuthUserRecord> findByMobile(String mobile);

    Optional<AuthUserRecord> findUser(UUID tenantId, UUID userId);

    Optional<AuthRoleRecord> findRole(UUID tenantId, UUID roleId);

    int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange, UUID actorId);

    FailureState failureState(String accountKey);

    void recordFailure(String accountKey, int failCount, Instant windowStartedAt, Instant lockedUntil);

    void clearFailure(String accountKey);

    void insertCaptcha(String token, String codeHash, Instant expiresAt);

    Optional<String> consumeCaptcha(String token, Instant now);

    UUID insertSession(UUID tenantId, UUID userId, boolean rememberMe, Instant expiresAt);

    void revokeSession(UUID sessionId, Instant now);

    void revokeUserSessions(UUID tenantId, UUID userId, Instant now, UUID keepSessionId);

    boolean sessionActive(UUID sessionId, Instant now);

    Optional<AuthSessionRecord> findActiveSession(UUID sessionId, Instant now);

    void insertAudit(UUID tenantId, UUID userId, String accountKey, String result, String errorCode,
                     String ip, String userAgent, boolean sessionCreated, boolean sessionRevoked);
}
