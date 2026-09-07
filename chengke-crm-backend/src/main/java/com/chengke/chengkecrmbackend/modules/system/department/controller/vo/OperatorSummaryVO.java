package com.chengke.chengkecrmbackend.modules.system.department.controller.vo;

import java.util.UUID;
/** 最近更新操作者摘要；身份目录未加载时仅返回稳定标识。 */
public record OperatorSummaryVO(UUID userId, String displayName) {}
