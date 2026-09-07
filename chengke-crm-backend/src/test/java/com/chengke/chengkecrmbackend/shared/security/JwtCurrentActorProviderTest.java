package com.chengke.chengkecrmbackend.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证租户、操作者、权限和部门范围只从已验证 JWT 认证对象解析。
 */
class JwtCurrentActorProviderTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBuildFailClosedActorFromJwtClaims() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("tenant_id", tenantId.toString(), "sub", actorId.toString(),
                        "manageable_department_ids", List.of(departmentId.toString())));
        var authentication = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("system:department:view")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CurrentActor actor = new JwtCurrentActorProvider().currentActor();

        assertThat(actor.tenantId()).isEqualTo(tenantId);
        assertThat(actor.actorId()).isEqualTo(actorId);
        assertThat(actor.permissions()).containsExactly("system:department:view");
        assertThat(actor.manageableDepartmentIds()).containsExactly(departmentId);
        assertThat(actor.manageAllDepartments()).isFalse();
    }
}
