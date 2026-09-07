package com.chengke.chengkecrmbackend.shared.security;

import com.chengke.chengkecrmbackend.modules.auth.infrastructure.security.AuthSessionFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * 始终注册无状态安全过滤链：放行 OpenAPI、Swagger UI 与健康检查，其余请求必须认证。
 *
 * <p>存在 {@link JwtDecoder} 时启用 OAuth2 Resource Server；不存在时关闭默认表单登录与
 * HTTP Basic，避免本地调试被重定向到 {@code /login}。</p>
 */
@Configuration
public class JwtResourceServerConfiguration {

    /**
     * 配置全局 HTTP 安全过滤链。
     *
     * <p>副作用：关闭 CSRF、会话、表单登录与 HTTP Basic；未认证返回 401；
     * 存在 JWT 解码器时启用资源服务器。</p>
     *
     * @param http Spring Security HTTP 配置器
     * @param jwtDecoder 可选 JWT 解码器；本地未配置 issuer 或 JWK 时为空
     * @param authSessionFilter 可选会话校验过滤器；无持久化时为空
     * @return 安全过滤链
     * @throws Exception Security 配置失败时抛出
     */
    @Bean
    public SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http, ObjectProvider<JwtDecoder> jwtDecoder,
            ObjectProvider<AuthSessionFilter> authSessionFilter
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/actuator/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/captcha")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        if (jwtDecoder.getIfAvailable() != null) {
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }
        AuthSessionFilter sessionFilter = authSessionFilter.getIfAvailable();
        if (sessionFilter != null) {
            http.addFilterAfter(sessionFilter, BearerTokenAuthenticationFilter.class);
        }
        return http.build();
    }

    /**
     * 把 scope Claim 原样转换为权限标识，避免默认 SCOPE_ 前缀破坏文档约定。
     *
     * @return 支持 system:department:* 权限字符串的 JWT 转换器
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
