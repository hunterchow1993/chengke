package com.chengke.chengkecrmbackend.shared.security;

import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 从 Spring Security 已验证 JWT 的 tenant_id、sub 和 manageable_department_ids Claim 构建安全上下文。
 */
@Component
public class JwtCurrentActorProvider implements CurrentActorProvider {

    /**
     * {@inheritDoc}
     *
     * @throws BusinessException 未认证、Claim 缺失或 UUID 格式错误时返回 401，并按 fail closed 拒绝请求
     */
    @Override
    public CurrentActor currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录");
        }
        try {
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
            UUID actorId = UUID.fromString(jwt.getSubject());
            Set<String> permissions = new HashSet<>();
            authentication.getAuthorities().forEach(authority -> {
                String value = authority.getAuthority();
                permissions.add(value.startsWith("SCOPE_") ? value.substring(6) : value);
            });
            Set<UUID> manageableIds = parseUuidSet(jwt.getClaim("manageable_department_ids"));
            boolean manageAll = permissions.contains("system:department:manage:all");
            String requestId = MDC.get("requestId");
            return new CurrentActor(tenantId, actorId, Set.copyOf(permissions), manageableIds,
                    manageAll, requestId == null ? UUID.randomUUID().toString() : requestId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException("AUTHENTICATION_INVALID", 401, "认证信息不完整");
        }
    }

    /**
     * 把 JWT 数组 Claim 转换成不可变 UUID 集合。
     *
     * @param values Claim 原始值；缺失时返回空集合并保持 fail closed
     * @return 合法 UUID 组成的不可变集合
     */
    private Set<UUID> parseUuidSet(Object values) {
        if (!(values instanceof Collection<?> collection)) {
            return Set.of();
        }
        var result = new HashSet<UUID>();
        for (var value : collection) {
            result.add(UUID.fromString(String.valueOf(value)));
        }
        return Set.copyOf(result);
    }
}
