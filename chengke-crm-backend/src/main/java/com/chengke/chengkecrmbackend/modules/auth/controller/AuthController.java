package com.chengke.chengkecrmbackend.modules.auth.controller;

import com.chengke.chengkecrmbackend.modules.auth.application.AuthQueryService;
import com.chengke.chengkecrmbackend.modules.auth.application.LoginCommandService;
import com.chengke.chengkecrmbackend.modules.auth.application.command.ChangeFirstPasswordCommand;
import com.chengke.chengkecrmbackend.modules.auth.application.command.LoginCommand;
import com.chengke.chengkecrmbackend.modules.auth.controller.dto.FirstPasswordChangeRequest;
import com.chengke.chengkecrmbackend.modules.auth.controller.dto.LoginRequest;
import com.chengke.chengkecrmbackend.modules.auth.controller.mapper.AuthApiMapper;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.AuthContextVO;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.CaptchaVO;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.LoginResultVO;
import com.chengke.chengkecrmbackend.shared.api.ApiResponse;
import com.chengke.chengkecrmbackend.shared.error.BusinessException;
import com.chengke.chengkecrmbackend.shared.security.CurrentActor;
import com.chengke.chengkecrmbackend.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * 登录、验证码、首次改密、授权上下文与退出 HTTP API。
 *
 * <p>登录与验证码公开；其余接口依赖 {@code Authorization: Bearer} 访问令牌。登录业务失败仍返回 HTTP 200。</p>
 */
@Tag(name = "登录认证")
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LoginCommandService commandService;
    private final AuthQueryService queryService;
    private final AuthApiMapper mapper;
    private final CurrentActorProvider actorProvider;

    /**
     * 创建登录 HTTP 控制器。
     *
     * @param commandService 登录写用例
     * @param queryService 授权上下文用例
     * @param mapper HTTP 边界转换器
     * @param actorProvider JWT 操作者上下文提供者
     */
    public AuthController(LoginCommandService commandService, AuthQueryService queryService,
                          AuthApiMapper mapper, CurrentActorProvider actorProvider) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.mapper = mapper;
        this.actorProvider = actorProvider;
    }

    /**
     * 提交账号密码创建会话。公开接口；业务失败 HTTP 200 且 data.success=false。
     *
     * @param request 账号、密码、可选验证码与保持登录
     * @param httpRequest 用于审计的客户端信息
     * @return 登录业务结果；成功时含 accessToken
     */
    @Operation(summary = "登录", description = "公开接口；失败时 HTTP 200 并返回 errorCode")
    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        var result = commandService.login(new LoginCommand(
                request.account(), request.password(), request.rememberMeOrFalse(),
                request.captchaCode(), request.captchaToken(), clientIp(httpRequest), userAgent(httpRequest)
        ));
        return ApiResponse.success(mapper.toVO(result), requestId());
    }

    /**
     * 签发一次性图形验证码。公开接口。
     *
     * @return captchaToken 与 PNG Base64
     */
    @Operation(summary = "获取图形验证码", description = "公开接口；挑战 60 秒一次使用")
    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha() {
        return ApiResponse.success(mapper.toVO(commandService.issueCaptcha()), requestId());
    }

    /**
     * 首次登录修改密码并签发新会话。需要强制改密会话。
     *
     * @param request 新密码与可选确认密码
     * @param httpRequest 用于审计的客户端信息
     * @return 与登录成功相同的业务结果
     */
    @Operation(summary = "首次登录修改密码", description = "需要有效且处于强制改密的会话")
    @PostMapping("/first-password-change")
    public ApiResponse<LoginResultVO> changeFirstPassword(
            @Valid @RequestBody FirstPasswordChangeRequest request, HttpServletRequest httpRequest
    ) {
        CurrentActor actor = actorProvider.currentActor();
        var result = commandService.changeFirstPassword(new ChangeFirstPasswordCommand(
                actor, request.newPassword(), request.confirmPassword(), currentSessionId(),
                clientIp(httpRequest), userAgent(httpRequest)
        ));
        return ApiResponse.success(mapper.toVO(result), actor.requestId());
    }

    /**
     * 返回当前会话的菜单、权限码与首页。强制改密未完成时拒绝。
     *
     * @return 授权上下文
     */
    @Operation(summary = "获取授权上下文", description = "需要有效会话，且已完成首次改密")
    @GetMapping("/context")
    public ApiResponse<AuthContextVO> context() {
        CurrentActor actor = actorProvider.currentActor();
        return ApiResponse.success(mapper.toVO(queryService.load(actor, currentExpiresAt())), actor.requestId());
    }

    /**
     * 为当前会话重签 Access Token。需要请求头中的有效访问令牌。
     *
     * @return 与登录成功相同的业务结果，含新 accessToken
     */
    @Operation(summary = "刷新访问令牌", description = "需要 Authorization: Bearer；写入当前租户授权版本")
    @PostMapping("/refresh")
    public ApiResponse<LoginResultVO> refresh() {
        CurrentActor actor = actorProvider.currentActor();
        var result = commandService.refresh(currentSessionId());
        return ApiResponse.success(mapper.toVO(result), actor.requestId());
    }

    /**
     * 撤销当前会话。会话已失效时仍返回成功。
     *
     * @param httpRequest 用于审计的客户端信息
     * @return 空成功体
     */
    @Operation(summary = "退出登录", description = "撤销当前会话；重复退出视为成功")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        CurrentActor actor = actorProvider.currentActor();
        commandService.logout(actor.tenantId(), actor.actorId(), currentSessionId(),
                clientIp(httpRequest), userAgent(httpRequest));
        return ApiResponse.success(null, actor.requestId());
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", 401, "请先登录");
        }
        return jwt;
    }

    private UUID currentSessionId() {
        String jti = currentJwt().getId();
        if (jti == null || jti.isBlank()) {
            throw new BusinessException("AUTHENTICATION_INVALID", 401, "认证信息不完整");
        }
        try {
            return UUID.fromString(jti);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("AUTHENTICATION_INVALID", 401, "认证信息不完整");
        }
    }

    private Instant currentExpiresAt() {
        Instant expiresAt = currentJwt().getExpiresAt();
        return expiresAt == null ? Instant.now() : expiresAt;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        if (value == null) {
            return null;
        }
        return value.length() <= 300 ? value : value.substring(0, 300);
    }

    private String requestId() {
        String value = MDC.get("requestId");
        return value == null ? UUID.randomUUID().toString() : value;
    }
}
