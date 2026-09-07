package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import java.util.List;
import java.util.UUID;

/** 用户管理组织树默认加载结果。 */
public record UserOrgTreeResult(List<UserOrgNodeResult> nodes, UUID defaultSelectedId) {
}
