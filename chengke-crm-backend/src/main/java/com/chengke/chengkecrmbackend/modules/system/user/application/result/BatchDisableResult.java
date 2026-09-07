package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import java.util.List;
import java.util.UUID;

/** 批量停用汇总。 */
public record BatchDisableResult(int total, int successCount, int failedCount, List<BatchDisableItemResult> results) {
    /** 逐项停用结果。 */
    public record BatchDisableItemResult(UUID userId, boolean success, String errorCode, String message) {
    }
}
