package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/**
 * 搜索可见组织节点。
 */
public record UserOrgSearchQuery(CurrentActor actor, String keyword) {
}
