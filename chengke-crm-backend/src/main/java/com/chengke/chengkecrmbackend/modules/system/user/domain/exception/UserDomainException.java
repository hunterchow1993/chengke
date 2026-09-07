package com.chengke.chengkecrmbackend.modules.system.user.domain.exception;

/**
 * 表示不依赖技术框架的用户领域规则冲突，错误码可稳定映射为 HTTP 业务错误。
 */
public final class UserDomainException extends RuntimeException {

    private final String code;

    /**
     * 创建一个领域规则异常。
     *
     * @param code 稳定用户业务错误码
     */
    public UserDomainException(String code) {
        super(code);
        this.code = code;
    }

    /**
     * 返回稳定用户业务错误码。
     *
     * @return 供应用层和异常处理器映射的错误码
     */
    public String code() {
        return code;
    }
}
