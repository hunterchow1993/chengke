package com.chengke.chengkecrmbackend.modules.auth.application.port.model;

import java.time.Instant;

/**
 * 按账号键累计的连续失败与锁定状态。
 */
public record FailureState(int failCount, Instant windowStartedAt, Instant lockedUntil) {
    public static FailureState empty() { 
        return new FailureState(0, null, null);
    }

    public boolean locked(Instant now) {
        // 锁定时间未到
        return lockedUntil != null && now.isBefore(lockedUntil);
    }
}
