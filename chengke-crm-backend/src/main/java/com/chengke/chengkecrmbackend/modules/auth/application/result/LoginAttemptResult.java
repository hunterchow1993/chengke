package com.chengke.chengkecrmbackend.modules.auth.application.result;

/**
 * 登录、刷新或首次改密后的业务结果。成功时带访问令牌；失败时 HTTP 仍为 200。
 */
public record LoginAttemptResult(
        boolean success,
        String errorCode,
        boolean requiresCaptcha,
        Boolean forcePasswordChange,
        Boolean authContextRequired,
        String accessToken
) {
    public static LoginAttemptResult failure(String errorCode, boolean requiresCaptcha) {
        return new LoginAttemptResult(false, errorCode, requiresCaptcha, null, null, null);
    }

    public static LoginAttemptResult ok(boolean forcePasswordChange, String accessToken) {
        return new LoginAttemptResult(true, null, false, forcePasswordChange, true, accessToken);
    }
}
