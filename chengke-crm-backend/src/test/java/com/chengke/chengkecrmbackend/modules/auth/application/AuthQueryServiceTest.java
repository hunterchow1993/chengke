package com.chengke.chengkecrmbackend.modules.auth.application;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证超级管理员授权上下文含工作台、用户和部门菜单。
 */
class AuthQueryServiceTest {

    @Test
    void shouldBuildSuperAdminContextWithDashboardHome() {
        UUID tenantId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000101");
        UUID roleId = UUID.fromString("10000000-0000-0000-0000-000000000201");
        AuthPersistencePort persistence = mock(AuthPersistencePort.class);
        when(persistence.findUser(tenantId, userId)).thenReturn(Optional.of(
                new AuthUserRecord(userId, tenantId, "管理员", "admin", "13800000001", null,
                        "hash", "active", false, roleId, UUID.randomUUID())));
        when(persistence.findRole(tenantId, roleId)).thenReturn(Optional.of(
                new AuthRoleRecord(roleId, "超级管理员", "super_admin", "active", true)));
        var service = new AuthQueryService(persistence, ignored -> "3");
        var actor = new CurrentActor(tenantId, userId, Set.of(), Set.of(), true, "req-1");

        var result = service.load(actor, Instant.parse("2026-09-05T12:00:00Z"));

        assertThat(result.homeRoute()).isEqualTo("/app/dashboard");
        assertThat(result.authorizedHome()).isEqualTo("/app/dashboard");
        assertThat(result.user().displayName()).isEqualTo("管理员");
        assertThat(result.permissionCodes()).contains("system:user:view", "system:department:view");
        assertThat(result.menus()).extracting(menu -> menu.name()).contains("工作台", "系统管理");
        assertThat(result.expiresAt()).isEqualTo("2026-09-05T12:00:00Z");
        assertThat(result.permissionVersion()).isEqualTo("3");
    }
}
