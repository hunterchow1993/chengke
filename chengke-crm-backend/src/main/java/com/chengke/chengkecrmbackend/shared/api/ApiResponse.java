package com.chengke.chengkecrmbackend.shared.api;

import java.time.OffsetDateTime;

/**
 * 所有 HTTP 成功和业务错误响应的统一外层结构。
 *
 * @param code 稳定业务码
 * @param message 安全展示消息
 * @param data 响应数据或错误详情
 * @param requestId 请求追踪标识
 * @param timestamp 响应时间
 */
public record ApiResponse<T>(
        String code, String message, T data, String requestId, OffsetDateTime timestamp
) {
    /**
     * 创建成功响应。
     *
     * @param data 业务数据
     * @param requestId 请求追踪标识
     * @return code 为 OK 的统一响应
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>("OK", "success", data, requestId, OffsetDateTime.now());
    }

    /**
     * 创建安全错误响应。
     *
     * @param code 稳定业务错误码
     * @param message 安全错误消息
     * @param data 字段错误等可选详情
     * @param requestId 请求追踪标识
     * @return 统一错误响应
     */
    public static <T> ApiResponse<T> error(String code, String message, T data, String requestId) {
        return new ApiResponse<>(code, message, data, requestId, OffsetDateTime.now());
    }
}
