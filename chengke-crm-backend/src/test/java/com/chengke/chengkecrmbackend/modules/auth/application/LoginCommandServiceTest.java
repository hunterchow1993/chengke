package com.chengke.chengkecrmbackend.modules.auth.application;

import com.chengke.chengkecrmbackend.modules.auth.application.command.LoginCommand;
import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.CaptchaGenerator;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthSessionRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.FailureState;
import com.chengke.chengkecrmbackend.modules.auth.domain.policy.LoginPolicy;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.PasswordHasher;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证登录成功、防枚举、停用/角色失败和验证码风控。
 */
class LoginCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T04:00:00Z");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID ROLE = UUID.fromString("10000000-0000-0000-0000-000000000201");

    private FakePersistence persistence;
    private LoginCommandService service;

    @BeforeEach
    void setUp() {
        persistence = new FakePersistence();
        persistence.users.add(new AuthUserRecord(USER, TENANT, "管理员", "admin", "13800000001", null,
                "hashed:Crm@2026!", "active", false, ROLE, UUID.randomUUID()));
        persistence.roles.put(ROLE, new AuthRoleRecord(ROLE, "超级管理员", "super_admin", "active", true));
        service = new LoginCommandService(
                new LoginPolicy(5, 10), new UserPolicy(), persistence, new FakeHasher(),
                (tenantId, userId, sessionId, permissions, deptIds, forcePasswordChange, expiresAt, permissionVersion) ->
                        "token-" + sessionId + ":" + permissionVersion,
                () -> new CaptchaGenerator.GeneratedCaptcha("cap-token", "Ab12", "img"),
                tenantId -> "3",
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofHours(8), Duration.ofDays(7)
        );
    }

    @Test
    void shouldLoginByUsernameAndIssueToken() {
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, null, null, "127.0.0.1", "test"));
        assertThat(result.success()).isTrue();
        assertThat(result.authContextRequired()).isTrue();
        assertThat(result.accessToken()).startsWith("token-");
        assertThat(result.accessToken()).endsWith(":3");
        assertThat(persistence.cleared).contains("admin");
    }

    @Test
    void shouldLoginByMobile() {
        var result = service.login(new LoginCommand("13800000001", "Crm@2026!", true, null, null, "127.0.0.1", "test"));
        assertThat(result.success()).isTrue();
    }

    @Test
    void shouldUseSameErrorForUnknownAccountAndWrongPassword() {
        var unknown = service.login(new LoginCommand("nobody", "Crm@2026!", false, null, null, "1.1.1.1", "ua"));
        var wrong = service.login(new LoginCommand("admin", "WrongPass1", false, null, null, "1.1.1.1", "ua"));
        assertThat(unknown.errorCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(wrong.errorCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(unknown.success()).isFalse();
    }

    @Test
    void shouldRejectDisabledAccountAfterPasswordMatches() {
        persistence.users.set(0, new AuthUserRecord(USER, TENANT, "管理员", "admin", "13800000001", null,
                "hashed:Crm@2026!", "disabled", false, ROLE, UUID.randomUUID()));
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, null, null, "1.1.1.1", "ua"));
        assertThat(result.errorCode()).isEqualTo("ACCOUNT_DISABLED");
        assertThat(persistence.sessions).isEmpty();
    }

    @Test
    void shouldRejectWhenRoleDisabled() {
        persistence.roles.put(ROLE, new AuthRoleRecord(ROLE, "超级管理员", "super_admin", "disabled", true));
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, null, null, "1.1.1.1", "ua"));
        assertThat(result.errorCode()).isEqualTo("ROLE_UNAVAILABLE");
    }

    @Test
    void shouldRequireCaptchaAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            service.login(new LoginCommand("admin", "WrongPass1", false, null, null, "1.1.1.1", "ua"));
        }
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, null, null, "1.1.1.1", "ua"));
        assertThat(result.errorCode()).isEqualTo("CAPTCHA_REQUIRED");
        assertThat(result.requiresCaptcha()).isTrue();
    }

    @Test
    void shouldRejectInvalidCaptcha() {
        persistence.failures.put("admin", new FailureState(5, NOW.minusSeconds(60), null));
        persistence.captchas.put("cap-token", "not-matching");
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, "zzzz", "cap-token", "1.1.1.1", "ua"));
        assertThat(result.errorCode()).isEqualTo("CAPTCHA_INVALID");
    }

    @Test
    void shouldRejectLockedAccount() {
        persistence.failures.put("admin", new FailureState(10, NOW.minusSeconds(60), NOW.plusSeconds(60)));
        var result = service.login(new LoginCommand("admin", "Crm@2026!", false, "Ab12", "cap-token", "1.1.1.1", "ua"));
        assertThat(result.errorCode()).isEqualTo("ACCOUNT_TEMP_LOCKED");
    }

    @Test
    void shouldRejectBlankAccountAsInvalidRequest() {
        assertThatThrownBy(() -> service.login(new LoginCommand("  ", "Crm@2026!", false, null, null, "1.1.1.1", "ua")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldRefreshAccessTokenWithCurrentPermissionVersion() {
        var login = service.login(new LoginCommand("admin", "Crm@2026!", false, null, null, "127.0.0.1", "test"));
        UUID sessionId = persistence.sessions.getFirst().id();
        var refreshed = service.refresh(sessionId);
        assertThat(refreshed.success()).isTrue();
        assertThat(refreshed.accessToken()).isEqualTo("token-" + sessionId + ":3");
        assertThat(login.accessToken()).isNotBlank();
        assertThat(persistence.sessions).hasSize(1);
    }

    @Test
    void shouldRejectRefreshWhenSessionMissing() {
        assertThatThrownBy(() -> service.refresh(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("AUTHENTICATION_REQUIRED");
    }

    private static final class FakeHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return ("hashed:" + rawPassword).equals(passwordHash);
        }
    }

    private static final class FakePersistence implements AuthPersistencePort {
        private final List<AuthUserRecord> users = new ArrayList<>();
        private final Map<UUID, AuthRoleRecord> roles = new HashMap<>();
        private final Map<String, FailureState> failures = new HashMap<>();
        private final Map<String, String> captchas = new HashMap<>();
        private final List<AuthSessionRecord> sessions = new ArrayList<>();
        private final List<String> cleared = new ArrayList<>();

        @Override
        public List<AuthUserRecord> findByUsername(String username) {
            return users.stream().filter(user -> user.username().equalsIgnoreCase(username)).toList();
        }

        @Override
        public List<AuthUserRecord> findByMobile(String mobile) {
            return users.stream().filter(user -> user.mobile().equals(mobile)).toList();
        }

        @Override
        public Optional<AuthUserRecord> findUser(UUID tenantId, UUID userId) {
            return users.stream().filter(user -> user.tenantId().equals(tenantId) && user.id().equals(userId)).findFirst();
        }

        @Override
        public Optional<AuthRoleRecord> findRole(UUID tenantId, UUID roleId) {
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public int updatePassword(UUID tenantId, UUID userId, String passwordHash, boolean forcePasswordChange, UUID actorId) {
            return 1;
        }

        @Override
        public FailureState failureState(String accountKey) {
            return failures.getOrDefault(accountKey, FailureState.empty());
        }

        @Override
        public void recordFailure(String accountKey, int failCount, Instant windowStartedAt, Instant lockedUntil) {
            failures.put(accountKey, new FailureState(failCount, windowStartedAt, lockedUntil));
        }

        @Override
        public void clearFailure(String accountKey) {
            failures.remove(accountKey);
            cleared.add(accountKey);
        }

        @Override
        public void insertCaptcha(String token, String codeHash, Instant expiresAt) {
            captchas.put(token, codeHash);
        }

        @Override
        public Optional<String> consumeCaptcha(String token, Instant now) {
            return Optional.ofNullable(captchas.remove(token));
        }

        @Override
        public UUID insertSession(UUID tenantId, UUID userId, boolean rememberMe, Instant expiresAt) {
            UUID id = UUID.randomUUID();
            sessions.add(new AuthSessionRecord(id, tenantId, userId, expiresAt));
            return id;
        }

        @Override
        public Optional<AuthSessionRecord> findActiveSession(UUID sessionId, Instant now) {
            return sessions.stream()
                    .filter(session -> session.id().equals(sessionId) && session.expiresAt().isAfter(now))
                    .findFirst();
        }

        @Override
        public void revokeSession(UUID sessionId, Instant now) {
        }

        @Override
        public void revokeUserSessions(UUID tenantId, UUID userId, Instant now, UUID keepSessionId) {
        }

        @Override
        public boolean sessionActive(UUID sessionId, Instant now) {
            return findActiveSession(sessionId, now).isPresent();
        }

        @Override
        public void insertAudit(UUID tenantId, UUID userId, String accountKey, String result, String errorCode,
                                String ip, String userAgent, boolean sessionCreated, boolean sessionRevoked) {
        }
    }
}
