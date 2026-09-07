package com.chengke.chengkecrmbackend.shared.error;

import com.chengke.chengkecrmbackend.modules.auth.domain.exception.LoginDomainException;
import com.chengke.chengkecrmbackend.modules.system.department.domain.exception.DepartmentDomainException;
import com.chengke.chengkecrmbackend.modules.system.user.domain.exception.UserDomainException;
import com.chengke.chengkecrmbackend.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * 把应用、领域和 Bean Validation 异常转换为稳定响应，避免泄露 SQL、约束名和堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @return 按异常携带状态输出的稳定业务错误 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.httpStatus())
                .body(ApiResponse.error(exception.code(), safeMessage(exception), null, requestId()));
    }

    /** @return 映射为 422 的部门领域规则错误 */
    @ExceptionHandler(DepartmentDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDepartmentDomain(DepartmentDomainException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.error(exception.code(), "部门操作不符合业务规则", null, requestId()));
    }

    /** @return 映射为 400 的登录参数规则错误 */
    @ExceptionHandler(LoginDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginDomain(LoginDomainException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "请求参数不合法", null, requestId()));
    }

    /** @return 映射为 422 的用户领域规则错误；保护账号使用专用消息 */
    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserDomain(UserDomainException exception) {
        String message = switch (exception.code()) {
            case "USER_SELF_DISABLE_FORBIDDEN" -> "不能停用自己";
            case "USER_LAST_SUPER_ADMIN_PROTECTED" -> "唯一超级管理员受保护";
            case "USER_SELF_ROLE_CHANGE_FORBIDDEN" -> "不能修改自己的角色";
            default -> "用户操作不符合业务规则";
        };
        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.error(exception.code(), message, null, requestId()));
    }

    /** @return 功能权限注解拒绝 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        boolean userApi = isUserApi();
        String code = userApi ? "USER_OPERATION_FORBIDDEN" : "DEPARTMENT_OPERATION_FORBIDDEN";
        String message = userApi ? "无操作权限" : "无部门操作权限";
        return ResponseEntity.status(403).body(ApiResponse.error(code, message, null, requestId()));
    }

    /** @return 请求体字段校验错误列表 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleBodyValidation(
            MethodArgumentNotValidException exception
    ) {
        return invalid(exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage())).toList());
    }

    /** @return 查询参数字段校验错误列表 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleBindValidation(BindException exception) {
        return invalid(exception.getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage())).toList());
    }

    /** @return 路径或查询参数类型错误 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(requestInvalidCode(), "请求参数不合法", null, requestId()));
    }

    /** 组装统一 400 字段校验响应。 */
    private ResponseEntity<ApiResponse<List<FieldValidationError>>> invalid(List<FieldValidationError> errors) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error(requestInvalidCode(), "请求参数不正确", errors, requestId()));
    }

    /** 仅返回不含内部前缀和技术信息的业务消息。 */
    private String safeMessage(BusinessException exception) {
        int separator = exception.getMessage().indexOf(": ");
        return separator < 0 ? "业务操作失败" : exception.getMessage().substring(separator + 2);
    }

    /** @return 当前请求是否为登录认证 API */
    private boolean isAuthApi() {
        HttpServletRequest request = currentRequest();
        return request != null && request.getRequestURI() != null
                && request.getRequestURI().contains("/auth/");
    }

    /** @return 当前请求是否为用户管理 API */
    private boolean isUserApi() {
        HttpServletRequest request = currentRequest();
        return request != null && request.getRequestURI() != null
                && request.getRequestURI().contains("/system/users");
    }

    /** @return 按模块前缀选择参数错误码 */
    private String requestInvalidCode() {
        if (isAuthApi()) {
            return "INVALID_REQUEST";
        }
        return isUserApi() ? "USER_REQUEST_INVALID" : "DEPARTMENT_REQUEST_INVALID";
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }

    /** @return MDC 中的请求标识，缺失时生成临时标识 */
    private String requestId() {
        String value = MDC.get("requestId");
        return value == null ? UUID.randomUUID().toString() : value;
    }
}
