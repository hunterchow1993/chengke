package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.security;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 使用 BCrypt 生成不可逆密码凭据，禁止记录明文。
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** {@inheritDoc} */
    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && passwordHash != null && encoder.matches(rawPassword, passwordHash);
    }
}
