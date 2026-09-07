package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 手机号可用性查询。
 */
public record MobileAvailableQuery(CurrentActor actor, String mobile, UUID excludeUserId) {
}
