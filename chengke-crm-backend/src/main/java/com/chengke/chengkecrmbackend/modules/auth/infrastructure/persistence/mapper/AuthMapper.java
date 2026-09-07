package com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence.mapper;

import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthSessionRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.FailureState;
import com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence.dataobject.AuthUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 登录模块对用户、角色、会话、验证码、失败计数和审计的 SQL。
 */
@Mapper
public interface AuthMapper {
    List<AuthUserDO> findByUsername(@Param("username") String username);

    List<AuthUserDO> findByMobile(@Param("mobile") String mobile);

    AuthUserDO findUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    AuthRoleRecord findRole(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

    int updatePassword(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("forcePasswordChange") boolean forcePasswordChange,
                       @Param("actorId") UUID actorId);

    FailureState failureState(@Param("accountKey") String accountKey);

    void upsertFailure(@Param("accountKey") String accountKey, @Param("failCount") int failCount,
                       @Param("windowStartedAt") Instant windowStartedAt, @Param("lockedUntil") Instant lockedUntil);

    void clearFailure(@Param("accountKey") String accountKey);

    void insertCaptcha(@Param("token") String token, @Param("codeHash") String codeHash,
                       @Param("expiresAt") Instant expiresAt);

    String consumeCaptcha(@Param("token") String token, @Param("now") Instant now);

    void insertSession(@Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                       @Param("rememberMe") boolean rememberMe, @Param("expiresAt") Instant expiresAt);

    void revokeSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    void revokeUserSessions(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                            @Param("now") Instant now, @Param("keepSessionId") UUID keepSessionId);

    int countActiveSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    AuthSessionRecord findActiveSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    void insertAudit(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                     @Param("accountKey") String accountKey, @Param("result") String result,
                     @Param("errorCode") String errorCode, @Param("ip") String ip,
                     @Param("userAgent") String userAgent, @Param("sessionCreated") boolean sessionCreated,
                     @Param("sessionRevoked") boolean sessionRevoked);
}
