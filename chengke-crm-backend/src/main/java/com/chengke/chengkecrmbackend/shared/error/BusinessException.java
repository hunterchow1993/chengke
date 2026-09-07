package com.chengke.chengkecrmbackend.shared.error;

/**
 * 携带稳定业务码与 HTTP 状态语义的应用异常，避免把数据库实现细节暴露给 Controller。
 */
public final class BusinessException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    /**
     * 创建业务异常。
     *
     * @param code 稳定业务错误码
     * @param httpStatus 对应 HTTP 状态码
     * @param message 可供日志和客户端展示的安全消息
     */
    public BusinessException(String code, int httpStatus, String message) {
        super(code + ": " + message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /** @return 稳定业务错误码 */
    public String code() {
        return code;
    }

    /** @return 对应 HTTP 状态码 */
    public int httpStatus() {
        return httpStatus;
    }
}
