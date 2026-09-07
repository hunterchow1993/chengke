package com.chengke.chengkecrmbackend.modules.auth.application.port;

import java.util.UUID;

/**
 * 读取租户权限版本，供授权上下文判断缓存是否过期。
 */
public interface PermissionVersionReader {

    /**
     * @param tenantId 租户
     * @return 版本字符串，缺失时为 1
     */
    String version(UUID tenantId);
}
