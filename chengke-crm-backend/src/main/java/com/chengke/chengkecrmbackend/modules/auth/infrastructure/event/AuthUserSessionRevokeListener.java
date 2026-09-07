package com.chengke.chengkecrmbackend.modules.auth.infrastructure.event;

import com.chengke.chengkecrmbackend.modules.auth.application.LoginCommandService;
import com.chengke.chengkecrmbackend.modules.system.user.application.event.UserChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户停用或重置密码提交后撤销其全部登录会话。
 */
@Component
public class AuthUserSessionRevokeListener {
    private final LoginCommandService loginCommandService;

    public AuthUserSessionRevokeListener(LoginCommandService loginCommandService) {
        this.loginCommandService = loginCommandService;
    }

    /**
     * 处理已提交的用户变化。
     *
     * <p>副作用：停用或重置密码时撤销该用户全部会话。</p>
     *
     * @param event 已提交的用户变化事实
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(UserChangedEvent event) {
        if (event.userId() == null) {
            return;
        }
        if ("disable".equals(event.action()) || "reset_password".equals(event.action())) {
            loginCommandService.revokeAllSessions(event.tenantId(), event.userId());
        }
    }
}
