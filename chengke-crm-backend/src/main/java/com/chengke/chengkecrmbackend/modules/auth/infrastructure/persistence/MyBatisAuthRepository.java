package com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthSessionRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.FailureState;
import com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence.dataobject.AuthUserDO;
import com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence.mapper.AuthMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 登录模块 PostgreSQL 持久化。
 */
@Repository
public class MyBatisAuthRepository implements AuthPersistencePort {
    private final AuthMapper mapper;

    public MyBatisAuthRepository(AuthMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AuthUserRecord> findByUsername(String username) {
        return mapper.findByUsername(username).stream().map(this::toUser).toList();
    }

    @Override
    public List<AuthUserRecord> findByMobile(String mobile) {
        return mapper.findByMobile(mobile).stream().map(this::toUser).toList();
    }

    @Override
    public Optional<AuthUserRecord> findUser(UUID tenantId, UUID userId) {
        return Optional.ofNullable(mapper.findUser(tenantId, userId)).map(this::toUser);
    }

    @Override
    public Optional<AuthRoleRecord> findRole(UUID tenantId, UUID roleId) {
        return Optional.ofNullable(mapper.findRole(tenantId, roleId));
    }

    @Override
    public int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange, UUID actorId) {
        return mapper.updatePassword(tenantId, userId, passwordHash, forcePasswordChange, actorId);
    }

    @Override
    public FailureState failureState(String accountKey) {
        FailureState state = mapper.failureState(accountKey);
        return state == null ? FailureState.empty() : state;
    }

    @Override
    public void recordFailure(String accountKey, int failCount, Instant windowStartedAt, Instant lockedUntil) {
        mapper.upsertFailure(accountKey, failCount, windowStartedAt, lockedUntil);
    }

    @Override
    public void clearFailure(String accountKey) {
        mapper.clearFailure(accountKey);
    }

    @Override
    public void insertCaptcha(String token, String codeHash, Instant expiresAt) {
        mapper.insertCaptcha(token, codeHash, expiresAt);
    }

    @Override
    public Optional<String> consumeCaptcha(String token, Instant now) {
        return Optional.ofNullable(mapper.consumeCaptcha(token, now));
    }

    @Override
    public UUID insertSession(UUID tenantId, UUID userId, boolean rememberMe, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        mapper.insertSession(id, tenantId, userId, rememberMe, expiresAt);
        return id;
    }

    @Override
    public void revokeSession(UUID sessionId, Instant now) {
        mapper.revokeSession(sessionId, now);
    }

    @Override
    public void revokeUserSessions(UUID tenantId, UUID userId, Instant now, UUID keepSessionId) {
        mapper.revokeUserSessions(tenantId, userId, now, keepSessionId);
    }

    @Override
    public boolean sessionActive(UUID sessionId, Instant now) {
        return mapper.countActiveSession(sessionId, now) > 0;
    }

    @Override
    public Optional<AuthSessionRecord> findActiveSession(UUID sessionId, Instant now) {
        return Optional.ofNullable(mapper.findActiveSession(sessionId, now));
    }

    @Override
    public void insertAudit(UUID tenantId, UUID userId, String accountKey, String result, String errorCode,
                            String ip, String userAgent, boolean sessionCreated, boolean sessionRevoked) {
        mapper.insertAudit(tenantId, userId, accountKey, result, errorCode, ip, userAgent, sessionCreated, sessionRevoked);
    }

    private AuthUserRecord toUser(AuthUserDO row) {
        return new AuthUserRecord(row.getId(), row.getTenantId(), row.getName(), row.getUsername(), row.getMobile(),
                row.getAvatarUrl(), row.getPasswordHash(), row.getStatus(), row.isForcePasswordChange(),
                row.getRoleId(), row.getDepartmentId());
    }
}
