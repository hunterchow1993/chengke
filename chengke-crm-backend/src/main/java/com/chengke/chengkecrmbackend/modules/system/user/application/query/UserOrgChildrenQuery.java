package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

import java.util.UUID;

/**
 * 懒加载组织树直接子节点。
 */
public record UserOrgChildrenQuery(CurrentActor actor, UUID nodeId) {
}
