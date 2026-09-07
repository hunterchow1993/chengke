package com.chengke.chengkecrmbackend.modules.auth.application.port;

/**
 * 生成一次性图形验证码挑战。
 */
public interface CaptchaGenerator {

    /**
     * @return token、明文码（仅内存使用一次）和 PNG Base64
     */
    GeneratedCaptcha generate();

    record GeneratedCaptcha(String token, String code, String imageBase64) {
    }
}
