package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 查询用户详情。
 */
public record GetUserDetailQuery(CurrentActor actor, UUID userId) {
}
