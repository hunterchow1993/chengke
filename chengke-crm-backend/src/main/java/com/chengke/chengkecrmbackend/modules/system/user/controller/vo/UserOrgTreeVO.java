package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.util.List;
import java.util.UUID;

/** 用户管理组织树默认加载结果。 */
public record UserOrgTreeVO(List<UserOrgNodeVO> nodes, UUID defaultSelectedId) {
}
