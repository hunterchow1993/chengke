package com.chengke.chengkecrmbackend.modules.auth.infrastructure.security;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 校验会话未撤销后，JWT 权限版本与 Redis 租户版本不一致时返回 409。
 */
class AuthSessionFilterTest {
    private static final Instant NOW = Instant.parse("2026-09-05T08:00:00Z");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SESSION = UUID.fromString("10000000-0000-0000-0000-000000000301");

    private AuthPersistencePort persistence;
    private PermissionVersionReader versions;
    private AuthSessionFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        persistence = mock(AuthPersistencePort.class);
        versions = mock(PermissionVersionReader.class);
        filter = new AuthSessionFilter(persistence, versions, Clock.fixed(NOW, ZoneOffset.UTC));
        chain = mock(FilterChain.class);
        when(persistence.sessionActive(eq(SESSION), any())).thenReturn(true);
        when(versions.version(TENANT)).thenReturn("3");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectStalePermissionVersionWithConflict() throws Exception {
        authenticate("1");
        MockHttpServletResponse response = filterRequest("/api/v1/system/users");

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("PERMISSION_VERSION_CHANGED");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectMissingPermissionVersionClaim() throws Exception {
        authenticate(null);
        MockHttpServletResponse response = filterRequest("/api/v1/system/users");

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("PERMISSION_VERSION_CHANGED");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldContinueWhenPermissionVersionMatches() throws Exception {
        authenticate("3");
        MockHttpServletResponse response = filterRequest("/api/v1/system/users");

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void shouldAllowContextLogoutAndRefreshWhenVersionChanged() throws Exception {
        authenticate("1");

        assertThat(filterRequest("/api/v1/auth/context").getStatus()).isEqualTo(200);
        assertThat(filterRequest("/api/v1/auth/logout").getStatus()).isEqualTo(200);
        assertThat(filterRequest("/api/v1/auth/refresh").getStatus()).isEqualTo(200);
        verify(chain, org.mockito.Mockito.times(3)).doFilter(any(), any());
    }

    private void authenticate(String permissionVersion) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("10000000-0000-0000-0000-000000000101")
                .jti(SESSION.toString())
                .claim("tenant_id", TENANT.toString());
        if (permissionVersion != null) {
            builder.claim("permission_version", permissionVersion);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build(), List.of()));
    }

    private MockHttpServletResponse filterRequest(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
