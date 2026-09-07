package com.chengke.chengkecrmbackend.modules.auth.infrastructure.jwt;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 使用 HS256 签发带 tenant_id、scope 与 jti 的访问令牌。
 */
@Component
public class NimbusAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;

    public NimbusAccessTokenIssuer(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String issue(UUID tenantId, UUID userId, UUID sessionId, Collection<String> permissions,
                        Collection<UUID> manageableDepartmentIds, boolean forcePasswordChange, Instant expiresAt,
                        String permissionVersion) {
        Instant now = Instant.now();
        List<String> departmentIds = manageableDepartmentIds.stream().map(id -> id.toString()).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(sessionId.toString())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("tenant_id", tenantId.toString())
                .claim("scope", String.join(" ", permissions))
                .claim("manageable_department_ids", departmentIds)
                .claim("force_password_change", forcePasswordChange)
                .claim("permission_version", permissionVersion)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
