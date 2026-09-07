package com.chengke.chengkecrmbackend.modules.auth.infrastructure;

import com.chengke.chengkecrmbackend.modules.auth.application.port.PermissionVersionReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 读取用户/部门写操作递增的租户授权版本；Redis 不可用时返回 1。
 */
@Component
public class RedisPermissionVersionReader implements PermissionVersionReader {
    private final ObjectProvider<StringRedisTemplate> redis;

    public RedisPermissionVersionReader(ObjectProvider<StringRedisTemplate> redis) {
        this.redis = redis;
    }

    @Override
    public String version(UUID tenantId) {
        StringRedisTemplate template = redis.getIfAvailable();
        if (template == null) {
            return "1";
        }
        String value = template.opsForValue().get("chengke:authorization:version:" + tenantId);
        return value == null || value.isBlank() ? "1" : value;
    }
}
