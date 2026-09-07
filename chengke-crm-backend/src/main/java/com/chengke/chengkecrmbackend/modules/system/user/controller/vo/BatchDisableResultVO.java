package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.util.List;
import java.util.UUID;

/** 批量停用结果。 */
public record BatchDisableResultVO(
        int total, int successCount, int failedCount, List<BatchDisableItemVO> results
) {
    /** 逐项结果。 */
    public record BatchDisableItemVO(UUID userId, boolean success, String errorCode, String message) {
    }
}
