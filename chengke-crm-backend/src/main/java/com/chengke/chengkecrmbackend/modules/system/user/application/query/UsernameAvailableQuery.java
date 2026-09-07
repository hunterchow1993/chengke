package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 用户名可用性查询。
 */
public record UsernameAvailableQuery(CurrentActor actor, String username, UUID excludeUserId) {
}
