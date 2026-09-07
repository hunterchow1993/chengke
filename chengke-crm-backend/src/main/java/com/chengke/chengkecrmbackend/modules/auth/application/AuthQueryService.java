package com.chengke.chengkecrmbackend.modules.auth.application;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthRoleRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.port.model.AuthUserRecord;
import com.chengke.chengkecrmbackend.modules.auth.application.result.AuthContextResult;
import com.chengke.chengkecrmbackend.modules.auth.application.result.MenuNodeResult;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 按登录 Spec 计算授权上下文。
 */
public class AuthQueryService {
    private final AuthPersistencePort persistence;
    private final PermissionVersionReader permissionVersionReader;

    public AuthQueryService(AuthPersistencePort persistence, PermissionVersionReader permissionVersionReader) {
        this.persistence = persistence;
        this.permissionVersionReader = permissionVersionReader;
    }

    /**
     * 加载当前会话用户的菜单、权限码和首页。强制改密未完成时拒绝。
     *
     * @param actor 已认证操作者
     * @param expiresAt 访问令牌过期时间
     * @return 授权上下文
     */
    public AuthContextResult load(CurrentActor actor, Instant expiresAt) {
        AuthUserRecord user = persistence.findUser(actor.tenantId(), actor.actorId())
                .orElseThrow(() -> new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录"));
        if (!"active".equals(user.status())) {
            throw new BusinessException("SESSION_REVOKED", 401, "会话已撤销");
        }
        if (user.forcePasswordChange()) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先完成密码修改");
        }
        AuthRoleRecord role = persistence.findRole(user.tenantId(), user.roleId())
                .orElseThrow(() -> new BusinessException("ROLE_UNAVAILABLE", 403, "账号暂时无法访问系统，请联系管理员"));
        if (!role.usable()) {
            throw new BusinessException("ROLE_UNAVAILABLE", 403, "账号暂时无法访问系统，请联系管理员");
        }
        List<String> permissions = role.superAdmin()
                ? PermissionCatalog.ALL
                : List.copyOf(actor.permissions());
        List<MenuNodeResult> menus = MenuCatalog.forPermissions(new HashSet<>(permissions));
        String homeRoute = menus.stream()
                .filter(node -> "page".equals(node.type()))
                .map(MenuNodeResult::path)
                .findFirst()
                .or(() -> menus.stream()
                        .flatMap(node -> node.children().stream())
                        .filter(child -> "page".equals(child.type()))
                        .map(MenuNodeResult::path)
                        .findFirst())
                .orElse("/app/dashboard");
        Map<String, String> dataScope = role.superAdmin() || actor.manageAllDepartments()
                ? Map.of("summary", "all")
                : Map.of("summary", "limited");
        if (menusHaveNoPages(menus)) {
            persistence.insertAudit(user.tenantId(), user.id(), user.username(), "failure", "NO_AVAILABLE_MENU",
                    null, null, false, false);
        }
        return new AuthContextResult(
                new AuthContextResult.User(user.id().toString(), user.name(), user.avatarUrl()),
                new AuthContextResult.Tenant(user.tenantId().toString(), "澄客集团"),
                new AuthContextResult.Role(role.id().toString(), role.name()),
                menus,
                permissions,
                dataScope,
                homeRoute,
                permissionVersionReader.version(user.tenantId()),
                DateTimeFormatter.ISO_INSTANT.format(expiresAt)
        );
    }

    private boolean menusHaveNoPages(List<MenuNodeResult> menus) {
        return menus.stream().noneMatch(node -> "page".equals(node.type()))
                && menus.stream().flatMap(node -> node.children().stream())
                .noneMatch(child -> "page".equals(child.type()));
    }
}
