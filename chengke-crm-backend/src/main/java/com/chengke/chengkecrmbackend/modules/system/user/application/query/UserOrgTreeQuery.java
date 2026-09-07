package com.chengke.chengkecrmbackend.modules.system.user.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/**
 * 查询用户管理组织树默认节点。
 */
public record UserOrgTreeQuery(CurrentActor actor) {
}
