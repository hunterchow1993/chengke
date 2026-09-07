package com.chengke.chengkecrmbackend.modules.auth.infrastructure.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthJwtConfigurationTest {

    @Test
    void shouldIssueAndValidateConfiguredJwtClaims() {
        AuthProperties properties = new AuthProperties();
        AuthJwtConfiguration configuration = new AuthJwtConfiguration();
        var secretKey = configuration.authHmacKey(properties);
        var issuer = new NimbusAccessTokenIssuer(configuration.jwtEncoder(secretKey), properties);
        Instant expiresAt = Instant.now().plus(Duration.ofDays(1));

        String token = issuer.issue(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                List.of("system:user:view"), List.of(), false, expiresAt, "1");
        var jwt = configuration.jwtDecoder(secretKey, properties).decode(token);

        assertThat(properties.accessTtl()).isEqualTo(Duration.ofDays(1));
        assertThat(properties.rememberMeTtl()).isEqualTo(Duration.ofDays(1));
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("chengke-crm-auth");
        assertThat(jwt.getAudience()).containsExactly("chengke-crm-api");
        assertThat(jwt.getExpiresAt()).isEqualTo(expiresAt.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void shouldRejectTokenForDifferentAudience() {
        AuthProperties properties = new AuthProperties();
        AuthJwtConfiguration configuration = new AuthJwtConfiguration();
        var secretKey = configuration.authHmacKey(properties);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(properties.jwtIssuer())
                .audience(List.of("other-api"))
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(1)))
                .build();
        String token = configuration.jwtEncoder(secretKey)
                .encode(JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();

        assertThatThrownBy(() -> configuration.jwtDecoder(secretKey, properties).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }
}
