package com.chengke.chengkecrmbackend.modules.auth.infrastructure.security;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.UUID;

/**
 * 校验 JWT jti 对应会话未撤销，比对权限版本，并阻止强制改密会话访问业务接口。
 *
 * <p>不注册为 Servlet Filter，仅由 SecurityFilterChain 挂载，避免执行两次。</p>
 */
public class AuthSessionFilter extends OncePerRequestFilter {
    private final AuthPersistencePort persistence;
    private final PermissionVersionReader versions;
    private final Clock clock;

    public AuthSessionFilter(AuthPersistencePort persistence, PermissionVersionReader versions, Clock clock) {
        this.persistence = persistence;
        this.versions = versions;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        boolean versionExempt = versionExempt(path);
        boolean logout = path.contains("/auth/logout");
        String jti = jwt.getId();
        if (jti != null) {
            try {
                if (!persistence.sessionActive(UUID.fromString(jti), clock.instant()) && !logout) {
                    writeJson(response, 401, "SESSION_REVOKED", "会话已撤销");
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                writeJson(response, 401, "AUTHENTICATION_INVALID", "认证信息不完整");
                return;
            }
        }
        boolean forceChange = Boolean.TRUE.equals(jwt.getClaim("force_password_change"));
        if (forceChange && !path.contains("/auth/first-password-change") && !logout) {
            writeJson(response, 401, "AUTHENTICATION_REQUIRED", "请先完成密码修改");
            return;
        }
        if (!versionExempt && permissionVersionChanged(jwt)) {
            writeJson(response, 409, "PERMISSION_VERSION_CHANGED", "权限已更新");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean permissionVersionChanged(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        String tokenVersion = jwt.getClaimAsString("permission_version");
        if (tenantClaim == null || tenantClaim.isBlank() || tokenVersion == null || tokenVersion.isBlank()) {
            return true;
        }
        try {
            return !versions.version(UUID.fromString(tenantClaim)).equals(tokenVersion);
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private static boolean versionExempt(String path) {
        return path.contains("/auth/context")
                || path.contains("/auth/logout")
                || path.contains("/auth/refresh")
                || path.contains("/auth/first-password-change");
    }

    private void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"data\":null}");
    }
}
