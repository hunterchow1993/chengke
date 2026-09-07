package com.chengke.chengkecrmbackend.modules.auth.domain.exception;

/**
 * 登录领域规则冲突，错误码映射为登录业务失败或参数拒绝。
 */
public final class LoginDomainException extends RuntimeException {

    private final String code;

    /**
     * @param code 稳定登录错误码
     */
    public LoginDomainException(String code) {
        super(code);
        this.code = code;
    }

    /** @return 稳定错误码 */
    public String code() {
        return code;
    }
}
