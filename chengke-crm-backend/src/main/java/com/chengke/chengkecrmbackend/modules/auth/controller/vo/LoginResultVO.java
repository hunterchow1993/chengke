package com.chengke.chengkecrmbackend.modules.auth.controller.vo;

/**
 * 登录业务结果。成功时含 accessToken 供前端当前契约使用。
 */
public record LoginResultVO(
        boolean success,
        String errorCode,
        boolean requiresCaptcha,
        Boolean forcePasswordChange,
        Boolean authContextRequired,
        String accessToken
) {
}
