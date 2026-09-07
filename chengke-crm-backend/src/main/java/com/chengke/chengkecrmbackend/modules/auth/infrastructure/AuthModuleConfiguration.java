package com.chengke.chengkecrmbackend.modules.auth.infrastructure;

import com.chengke.chengkecrmbackend.modules.auth.application.AuthQueryService;
import com.chengke.chengkecrmbackend.modules.auth.application.LoginCommandService;
import com.chengke.chengkecrmbackend.modules.auth.application.port.AccessTokenIssuer;
import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.CaptchaGenerator;
import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import com.chengke.chengkecrmbackend.modules.auth.domain.policy.LoginPolicy;
import com.chengke.chengkecrmbackend.modules.auth.infrastructure.jwt.AuthProperties;
import com.chengke.chengkecrmbackend.modules.auth.infrastructure.security.AuthSessionFilter;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.PasswordHasher;
import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 注册登录用例与风控策略。
 */
@Configuration
public class AuthModuleConfiguration {

    /** @return 登录领域策略 */
    @Bean
    public LoginPolicy loginPolicy(AuthProperties properties) {
        return new LoginPolicy(properties.captchaThreshold(), properties.lockThreshold());
    }

    /** @return 登录写用例 */
    @Bean
    public LoginCommandService loginCommandService(
            LoginPolicy loginPolicy, UserPolicy userPolicy, AuthPersistencePort persistence,
            PasswordHasher passwordHasher, AccessTokenIssuer tokenIssuer, CaptchaGenerator captchaGenerator,
            PermissionVersionReader versions, Clock clock, AuthProperties properties
    ) {
        return new LoginCommandService(loginPolicy, userPolicy, persistence, passwordHasher, tokenIssuer,
                captchaGenerator, versions, clock, properties.failureWindow(), properties.lockDuration(),
                properties.accessTtl(), properties.rememberMeTtl());
    }

    /** @return 授权上下文用例 */
    @Bean
    public AuthQueryService authQueryService(AuthPersistencePort persistence, PermissionVersionReader versions) {
        return new AuthQueryService(persistence, versions);
    }

    /** @return 挂到安全链上的会话过滤器 */
    @Bean
    public AuthSessionFilter authSessionFilter(
            AuthPersistencePort persistence, PermissionVersionReader versions, Clock clock
    ) {
        return new AuthSessionFilter(persistence, versions, clock);
    }

    /**
     * 关闭 Servlet 容器对该 Filter 的自动注册，仅由 SecurityFilterChain 执行一次。
     *
     * @param filter 会话过滤器
     * @return 已禁用的注册
     */
    @Bean
    public FilterRegistrationBean<AuthSessionFilter> authSessionFilterRegistration(AuthSessionFilter filter) {
        FilterRegistrationBean<AuthSessionFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
