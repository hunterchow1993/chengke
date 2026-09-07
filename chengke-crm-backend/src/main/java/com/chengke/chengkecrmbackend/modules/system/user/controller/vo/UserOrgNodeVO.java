package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.util.UUID;

/** 用户管理组织树节点。 */
public record UserOrgNodeVO(
        UUID id,
        UUID parentId,
        String name,
        String nodeType,
        String status,
        int depth,
        boolean hasChildren,
        int userCount,
        boolean manageable
) {
}
