package com.chengke.chengkecrmbackend.modules.auth.application;

import com.chengke.chengkecrmbackend.modules.auth.application.command.ChangeFirstPasswordCommand;
import com.chengke.chengkecrmbackend.modules.auth.application.command.LoginCommand;
import com.chengke.chengkecrmbackend.modules.auth.application.port.AccessTokenIssuer;
import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.CaptchaGenerator;
import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.FailureState;
import com.chengke.chengkecrmbackend.modules.auth.application.result.CaptchaChallengeResult;
import com.chengke.chengkecrmbackend.modules.auth.application.result.LoginAttemptResult;
import com.chengke.chengkecrmbackend.modules.auth.domain.exception.LoginDomainException;
import com.chengke.chengkecrmbackend.modules.auth.domain.policy.LoginPolicy;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.PasswordHasher;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 登录、验证码、首次改密与退出。
 */
public class LoginCommandService {
    private static final Duration CAPTCHA_TTL = Duration.ofSeconds(60);

    private final LoginPolicy policy;
    private final UserPolicy userPolicy;
    private final AuthPersistencePort persistence;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer tokenIssuer;
    private final CaptchaGenerator captchaGenerator;
    private final PermissionVersionReader versions;
    private final Clock clock;
    private final Duration failureWindow;
    private final Duration lockDuration;
    private final Duration sessionTtl;
    private final Duration rememberMeTtl;

    public LoginCommandService(LoginPolicy policy, UserPolicy userPolicy, AuthPersistencePort persistence,
                               PasswordHasher passwordHasher, AccessTokenIssuer tokenIssuer,
                               CaptchaGenerator captchaGenerator, PermissionVersionReader versions, Clock clock,
                               Duration failureWindow, Duration lockDuration, Duration sessionTtl,
                               Duration rememberMeTtl) {
        this.policy = policy;
        this.userPolicy = userPolicy;
        this.persistence = persistence;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.captchaGenerator = captchaGenerator;
        this.versions = versions;
        this.clock = clock;
        this.failureWindow = failureWindow;
        this.lockDuration = lockDuration;
        this.sessionTtl = sessionTtl;
        this.rememberMeTtl = rememberMeTtl;
    }

    /**
     * 校验凭据并在成功时创建会话。业务失败不抛异常，由调用方以 HTTP 200 返回 errorCode。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginAttemptResult login(LoginCommand command) {
        try {
            // 验证帐号和密码是否正确
            policy.validateLoginInput(command.account(), command.password());
        } catch (LoginDomainException exception) {
            throw new BusinessException("INVALID_REQUEST", 400, "请求参数不合法");
        }

        // 去除首尾空格，不改变字母大小写
        String accountKey = policy.normalizeAccount(command.account());
        Instant now = clock.instant();
        // 查询该帐号是否已连续失败达到锁定阈值
        FailureState failures = persistence.failureState(accountKey);
        // 连续失败次数达到锁定阈值，拒绝登录
        if (failures.locked(now)) {
            // 记录审计日志
            persistence.insertAudit(null, null, accountKey, "failure", "ACCOUNT_TEMP_LOCKED",
                    command.ip(), command.userAgent(), false, false);
            // 返回锁定提示，前端可提示用户稍后再试        
            return LoginAttemptResult.failure("ACCOUNT_TEMP_LOCKED", true);
        }
        
        if (policy.captchaRequired(failures.failCount())) {
            LoginAttemptResult captchaFailure = verifyCaptcha(command, accountKey);
            if (captchaFailure != null) {
                return captchaFailure;
            }
        }
        AuthUserRecord user = uniqueUser(accountKey);
        boolean passwordOk = user != null && passwordHasher.matches(command.password(), user.passwordHash());

        // 当密码错误时，增加失败计数并返回失败结果。若连续失败次数达到锁定阈值，则返回锁定提示。
        if (!passwordOk) {
            // 记录失败计数并返回失败结果
            FailureState next = incrementFailure(accountKey, now);
            persistence.insertAudit(user == null ? null : user.tenantId(), user == null ? null : user.id(),
                    accountKey, "failure", "INVALID_CREDENTIALS", command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("INVALID_CREDENTIALS", policy.captchaRequired(next.failCount()) || next.locked(now));
        }
        if (!"active".equals(user.status())) {
            // 账号被禁用
            persistence.insertAudit(user.tenantId(), user.id(), accountKey, "failure", "ACCOUNT_DISABLED",
                    command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("ACCOUNT_DISABLED", policy.captchaRequired(failures.failCount()));
        }
        // 查询角色权限
        AuthRoleRecord role = persistence.findRole(user.tenantId(), user.roleId()).orElse(null);
        if (role == null || !role.usable()) {
            persistence.insertAudit(user.tenantId(), user.id(), accountKey, "failure", "ROLE_UNAVAILABLE",
                    command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("ROLE_UNAVAILABLE", policy.captchaRequired(failures.failCount()));
        }
        persistence.clearFailure(accountKey);
        // 生成 token 并返回成功结果
        return issueSession(user, role, command.rememberMe(), command.ip(), command.userAgent());
    }

    /**
     * 签发一次性验证码挑战。
     */
    @Transactional(rollbackFor = Exception.class)
    public CaptchaChallengeResult issueCaptcha() {
        var generated = captchaGenerator.generate();
        persistence.insertCaptcha(generated.token(), hashCaptcha(generated.code()), clock.instant().plus(CAPTCHA_TTL));
        return new CaptchaChallengeResult(generated.token(), generated.imageBase64());
    }

    /**
     * 首次改密：更新密码、撤销旧会话、签发新会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginAttemptResult changeFirstPassword(ChangeFirstPasswordCommand command) {
        if (command.confirmPassword() != null && !command.confirmPassword().equals(command.newPassword())) {
            throw new BusinessException("INVALID_REQUEST", 400, "请求参数不合法");
        }
        if (!userPolicy.isValidPassword(command.newPassword())) {
            throw new BusinessException("INVALID_REQUEST", 400, "密码至少 8 位，且须包含字母、数字、特殊字符中的两类");
        }
        AuthUserRecord user = persistence.findUser(command.actor().tenantId(), command.actor().actorId())
                .orElseThrow(() -> new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录"));
        if (!user.forcePasswordChange()) {
            throw new BusinessException("INVALID_REQUEST", 400, "当前账号无需首次改密");
        }
        AuthRoleRecord role = persistence.findRole(user.tenantId(), user.roleId())
                .orElseThrow(() -> new BusinessException("ROLE_UNAVAILABLE", 403, "账号暂时无法访问系统，请联系管理员"));
        persistence.updatePassword(user.tenantId(), user.id(), passwordHasher.hash(command.newPassword()), false,
                user.id());
        Instant now = clock.instant();
        persistence.revokeUserSessions(user.tenantId(), user.id(), now, null);
        AuthUserRecord updated = persistence.findUser(user.tenantId(), user.id()).orElse(user);
        LoginAttemptResult result = issueSession(updated, role, false, command.ip(), command.userAgent());
        persistence.insertAudit(user.tenantId(), user.id(), user.username(), "success", null,
                command.ip(), command.userAgent(), true, true);
        return result;
    }

    /**
     * 撤销当前会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(UUID tenantId, UUID userId, UUID sessionId, String ip, String userAgent) {
        persistence.revokeSession(sessionId, clock.instant());
        persistence.insertAudit(tenantId, userId, null, "success", null, ip, userAgent, false, true);
    }

    /**
     * 停用或重置密码后撤销该用户全部会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeAllSessions(UUID tenantId, UUID userId) {
        persistence.revokeUserSessions(tenantId, userId, clock.instant(), null);
        persistence.insertAudit(tenantId, userId, null, "success", null, null, null, false, true);
    }

    /**
     * 为当前有效会话重签 Access Token，写入最新权限与租户授权版本。
     *
     * @param sessionId 刷新 Cookie 还原出的会话主键
     */
    @Transactional(readOnly = true)
    public LoginAttemptResult refresh(UUID sessionId) {
        var session = persistence.findActiveSession(sessionId, clock.instant())
                .orElseThrow(() -> new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录"));
        return refresh(session.tenantId(), session.userId(), session.id(), session.expiresAt());
    }

    private LoginAttemptResult refresh(UUID tenantId, UUID userId, UUID sessionId, Instant expiresAt) {
        AuthUserRecord user = persistence.findUser(tenantId, userId)
                .orElseThrow(() -> new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录"));
        if (!"active".equals(user.status())) {
            throw new BusinessException("SESSION_REVOKED", 401, "会话已撤销");
        }
        AuthRoleRecord role = persistence.findRole(user.tenantId(), user.roleId())
                .orElseThrow(() -> new BusinessException("ROLE_UNAVAILABLE", 403, "账号暂时无法访问系统，请联系管理员"));
        if (!role.usable()) {
            throw new BusinessException("ROLE_UNAVAILABLE", 403, "账号暂时无法访问系统，请联系管理员");
        }
        Instant tokenExpiry = expiresAt == null ? clock.instant().plus(sessionTtl) : expiresAt;
        String token = issueToken(user, role, sessionId, tokenExpiry);
        return LoginAttemptResult.ok(user.forcePasswordChange(), token);
    }

    private LoginAttemptResult verifyCaptcha(LoginCommand command, String accountKey) {
        if (command.captchaCode() == null || command.captchaCode().isBlank()
                || command.captchaToken() == null || command.captchaToken().isBlank()) {
            persistence.insertAudit(null, null, accountKey, "failure", "CAPTCHA_REQUIRED",
                    command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("CAPTCHA_REQUIRED", true);
        }
        try {
            policy.validateCaptchaInput(command.captchaCode(), command.captchaToken());
        } catch (LoginDomainException exception) {
            persistence.insertAudit(null, null, accountKey, "failure", "CAPTCHA_INVALID",
                    command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("CAPTCHA_INVALID", true);
        }
        var stored = persistence.consumeCaptcha(command.captchaToken(), clock.instant());
        if (stored.isEmpty() || !stored.get().equals(hashCaptcha(command.captchaCode()))) {
            persistence.insertAudit(null, null, accountKey, "failure", "CAPTCHA_INVALID",
                    command.ip(), command.userAgent(), false, false);
            return LoginAttemptResult.failure("CAPTCHA_INVALID", true);
        }
        return null;
    }

    private AuthUserRecord uniqueUser(String accountKey) {
        // 判断用户传过是否是手机号，如果是手机号则按手机号查找，否则按用户名查找
        List<AuthUserRecord> matches = policy.mobileAccount(accountKey)
                ? persistence.findByMobile(accountKey)
                : persistence.findByUsername(accountKey);
        return matches.size() == 1 ? matches.getFirst() : null;
    }
    
    /**
     * 增加失败次数并更新失败状态。
     *
     * @param accountKey 账号键
     * @param now 当前时间
     * @return 更新后的失败状态
     */
    private FailureState incrementFailure(String accountKey, Instant now) {
        FailureState current = persistence.failureState(accountKey);
        Instant windowStart = current.windowStartedAt();
        // 获取失败次数
        int count = current.failCount();
        // 
        if (windowStart == null || now.isAfter(windowStart.plus(failureWindow))) {
            windowStart = now;
            count = 1;
        } else {
            count++;
        }
        Instant lockedUntil = policy.locked(count) ? now.plus(lockDuration) : current.lockedUntil();
        persistence.recordFailure(accountKey, count, windowStart, lockedUntil);
        return new FailureState(count, windowStart, lockedUntil);
    }

    private LoginAttemptResult issueSession(AuthUserRecord user, AuthRoleRecord role, boolean rememberMe,
                                            String ip, String userAgent) {
        Instant now = clock.instant();
        Duration ttl = rememberMe ? rememberMeTtl : sessionTtl;
        Instant expiresAt = now.plus(ttl);
        UUID sessionId = persistence.insertSession(user.tenantId(), user.id(), rememberMe, expiresAt);
        String token = issueToken(user, role, sessionId, expiresAt);
        persistence.insertAudit(user.tenantId(), user.id(), user.username(), "success", null, ip, userAgent, true, false);
        return LoginAttemptResult.ok(user.forcePasswordChange(), token);
    }

    private String issueToken(AuthUserRecord user, AuthRoleRecord role, UUID sessionId, Instant expiresAt) {
        List<String> permissions = role.superAdmin() ? PermissionCatalog.ALL : List.of();
        return tokenIssuer.issue(user.tenantId(), user.id(), sessionId, permissions, List.of(),
                user.forcePasswordChange(), expiresAt, versions.version(user.tenantId()));
    }

    private String hashCaptcha(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(code.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
