package com.chengke.chengkecrmbackend.modules.auth.controller.mapper;

import com.chengke.chengkecrmbackend.modules.auth.application.result.AuthContextResult;
import com.chengke.chengkecrmbackend.modules.auth.application.result.CaptchaChallengeResult;
import com.chengke.chengkecrmbackend.modules.auth.application.result.LoginAttemptResult;
import com.chengke.chengkecrmbackend.modules.auth.application.result.MenuNodeResult;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.AuthContextVO;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.CaptchaVO;
import com.chengke.chengkecrmbackend.modules.auth.controller.vo.LoginResultVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 登录应用结果到 HTTP 视图的转换。
 */
@Component
public class AuthApiMapper {

    /** @return 登录业务结果，含前端当前契约所需的 accessToken */
    public LoginResultVO toVO(LoginAttemptResult result) {
        return new LoginResultVO(result.success(), result.errorCode(), result.requiresCaptcha(),
                result.forcePasswordChange(), result.authContextRequired(), result.accessToken());
    }

    /** @return 验证码挑战 */
    public CaptchaVO toVO(CaptchaChallengeResult result) {
        return new CaptchaVO(result.captchaToken(), result.imageBase64());
    }

    /** @return 授权上下文，同时写出 Spec 字段与前端 Zod 别名 */
    public AuthContextVO toVO(AuthContextResult result) {
        String avatarUrl = blankToNull(result.user().avatarUrl());
        return new AuthContextVO(
                new AuthContextVO.UserVO(result.user().id(), result.user().name(), result.user().displayName(), avatarUrl),
                new AuthContextVO.TenantVO(result.tenant().id(), result.tenant().name()),
                new AuthContextVO.RoleVO(result.role().id(), result.role().name()),
                result.menus().stream().map(this::toMenu).toList(),
                result.permissionCodes(),
                result.dataScope(),
                result.homeRoute(),
                result.permissionVersion(),
                result.expiresAt(),
                result.authorizedHome(),
                result.dataScopes(),
                result.sensitiveFieldPermissions()
        );
    }

    private AuthContextVO.MenuNodeVO toMenu(MenuNodeResult node) {
        List<AuthContextVO.MenuNodeVO> children = node.children().stream().map(this::toMenu).toList();
        return new AuthContextVO.MenuNodeVO(
                node.id(), node.parentId(), node.type(), node.name(), node.label(),
                node.routeName(), node.routeKey(), node.path(), node.iconKey(), node.icon(),
                node.permissionCode(), node.order(), children
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
