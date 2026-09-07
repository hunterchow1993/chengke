package com.chengke.chengkecrmbackend.modules.auth.infrastructure.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 为本地登录签发注册对称密钥 JwtEncoder / JwtDecoder。
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthJwtConfiguration {

    /**
     * @param properties 登录配置
     * @return HS256 密钥，长度不足时右侧补齐到 32 字节
     */
    @Bean
    public SecretKey authHmacKey(AuthProperties properties) {
        byte[] secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(secret, 0, padded, 0, secret.length);
            secret = padded;
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    /** @return 访问令牌编码器 */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey authHmacKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(authHmacKey));
    }

    /** @return 资源服务器解码器 */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey authHmacKey) {
        return NimbusJwtDecoder.withSecretKey(authHmacKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
