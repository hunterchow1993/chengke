package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.cache;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPreviewTokenStore;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.PreviewTokenBinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 使用 Redis GETDEL 保证预览令牌跨实例、一次性原子消费。
 */
@Component
public class RedisDepartmentPreviewTokenStore implements DepartmentPreviewTokenStore {
    private static final String KEY_PREFIX = "chengke:department:preview:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** @param redis Redis 字符串客户端 @param objectMapper JSON 序列化器 */
    public RedisDepartmentPreviewTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>副作用：在 Redis 写入带剩余有效期的随机令牌绑定。</p>
     */
    @Override
    public String save(PreviewTokenBinding binding) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Duration ttl = Duration.between(Instant.now(), binding.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("preview binding already expired");
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + token, objectMapper.writeValueAsString(binding), ttl);
            return token;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot serialize department preview", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>副作用：通过 Redis GETDEL 原子删除令牌，确保重放请求无法再次成功。</p>
     */
    @Override
    public Optional<PreviewTokenBinding> consume(String token) {
        String value = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PreviewTokenBinding.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot deserialize department preview", exception);
        }
    }
}
