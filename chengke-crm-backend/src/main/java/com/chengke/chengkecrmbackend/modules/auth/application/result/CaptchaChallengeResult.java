package com.chengke.chengkecrmbackend.modules.auth.application.result;

/**
 * 图形验证码挑战。
 */
public record CaptchaChallengeResult(String captchaToken, String imageBase64) {
}
