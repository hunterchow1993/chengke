package com.chengke.chengkecrmbackend.modules.auth.controller.vo;

/**
 * 验证码挑战。
 */
public record CaptchaVO(String captchaToken, String imageBase64) {
}
