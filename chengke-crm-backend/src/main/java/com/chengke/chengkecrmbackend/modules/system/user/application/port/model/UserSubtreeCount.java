package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import java.util.UUID;

/** 组织节点子树在权限范围内的用户计数。 */
public record UserSubtreeCount(UUID nodeId, int userCount) {
}
