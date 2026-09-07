package com.chengke.chengkecrmbackend.modules.auth.domain.policy;

import com.chengke.chengkecrmbackend.modules.auth.domain.exception.LoginDomainException;

import java.util.regex.Pattern;

/**
 * 登录账号识别、凭据格式和连续失败风控阈值。
 */
public final class LoginPolicy {
    private static final Pattern MOBILE = Pattern.compile("^1[3-9][0-9]{9}$");
    private static final Pattern CAPTCHA = Pattern.compile("^[A-Za-z0-9]{4,6}$");

    private final int captchaThreshold;
    private final int lockThreshold;

    /**
     * @param captchaThreshold 窗口内失败达到后必须验证码
     * @param lockThreshold 窗口内失败达到后临时锁定
     */
    public LoginPolicy(int captchaThreshold, int lockThreshold) {
        this.captchaThreshold = captchaThreshold;
        this.lockThreshold = lockThreshold;
    }

    /**
     * 去除账号首尾空格，不改变字母大小写。
     *
     * @param account 原始账号
     * @return 规范化账号；null 仍为 null
     */
    public String normalizeAccount(String account) {
        return account == null ? null : account.trim();
    }

    /**
     * 判断规范化账号是否按中国大陆 11 位手机号匹配。
     *
     * @param account 已规范化账号
     * @return 应按手机号查找时为 true
     */
    public boolean mobileAccount(String account) {
        return account != null && MOBILE.matcher(account).matches();
    }

    /**
     * 校验登录请求账号、密码长度。
     *
     * @param account 原始账号
     * @param password 原始密码
     * @throws LoginDomainException 格式不合法
     */
    public void validateLoginInput(String account, String password) {
        // 去除首尾空格
        String normalized = normalizeAccount(account);
        // 账号长度必须在 1~32 之间
        if (normalized == null || normalized.isEmpty() || normalized.length() > 32) {
            throw new LoginDomainException("INVALID_REQUEST");
        }
        //密码长度必须在 8~32 之间
        if (password == null || password.length() < 8 || password.length() > 32) {
            throw new LoginDomainException("INVALID_REQUEST");
        }
    }

    /**
     * @param failureCount 当前窗口失败次数
     * @return 下次登录必须带验证码
     */
    public boolean captchaRequired(int failureCount) {
        return failureCount >= captchaThreshold;
    }

    /**
     * @param failureCount 当前窗口失败次数
     * @return 账号处于临时锁定
     */
    public boolean locked(int failureCount) {
        return failureCount >= lockThreshold;
    }

    /**
     * 校验条件必填的验证码格式。
     *
     * @param captchaCode 用户输入
     * @param captchaToken 服务端凭证
     * @throws LoginDomainException 缺失或格式错误
     */
    public void validateCaptchaInput(String captchaCode, String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()
                || captchaCode == null || !CAPTCHA.matcher(captchaCode).matches()) {
            throw new LoginDomainException("CAPTCHA_INVALID");
        }
    }
}
