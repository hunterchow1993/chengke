package com.chengke.chengkecrmbackend.modules.auth.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 登录签发与风控可配置项。
 */
@ConfigurationProperties(prefix = "chengke.auth")
public class AuthProperties {
    private String jwtSecret = "chengke-local-hs256-secret-key-32bytes!";
    private String jwtIssuer = "chengke-crm-auth";
    private String jwtAudience = "chengke-crm-api";
    private Duration accessTtl = Duration.ofDays(1);
    private Duration rememberMeTtl = Duration.ofDays(1);
    private int captchaThreshold = 5;
    private int lockThreshold = 10;
    private Duration failureWindow = Duration.ofMinutes(15);
    private Duration lockDuration = Duration.ofMinutes(30);

    public String jwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String jwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public String jwtAudience() {
        return jwtAudience;
    }

    public void setJwtAudience(String jwtAudience) {
        this.jwtAudience = jwtAudience;
    }

    public Duration accessTtl() {
        return accessTtl;
    }

    public void setAccessTtl(Duration accessTtl) {
        this.accessTtl = accessTtl;
    }

    public Duration rememberMeTtl() {
        return rememberMeTtl;
    }

    public void setRememberMeTtl(Duration rememberMeTtl) {
        this.rememberMeTtl = rememberMeTtl;
    }

    public int captchaThreshold() {
        return captchaThreshold;
    }

    public void setCaptchaThreshold(int captchaThreshold) {
        this.captchaThreshold = captchaThreshold;
    }

    public int lockThreshold() {
        return lockThreshold;
    }

    public void setLockThreshold(int lockThreshold) {
        this.lockThreshold = lockThreshold;
    }

    public Duration failureWindow() {
        return failureWindow;
    }

    public void setFailureWindow(Duration failureWindow) {
        this.failureWindow = failureWindow;
    }

    public Duration lockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }
}
