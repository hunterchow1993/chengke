package com.chengke.chengkecrmbackend.shared.error;

/**
 * Bean Validation 返回给客户端的字段级安全错误。
 */
public record FieldValidationError(String field, String message) {
}
