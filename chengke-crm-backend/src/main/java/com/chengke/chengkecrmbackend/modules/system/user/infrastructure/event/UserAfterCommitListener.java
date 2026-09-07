package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.event;

import com.chengke.chengkecrmbackend.modules.system.user.application.event.UserChangedEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 仅在用户数据库事务提交后失效组织缓存，并在影响权限范围时提升租户授权版本。
 */
@Component
public class UserAfterCommitListener {
    private static final List<String> DEPARTMENT_CACHE_NAMES = List.of(
            "department-tree", "department-detail", "department-leader-candidates", "role-department-options"
    );
    private final ObjectProvider<CacheManager> cacheManagerProvider;
    private final StringRedisTemplate redis;

    /** @param cacheManagerProvider Spring 缓存管理器 @param redis 授权版本 Redis 存储 */
    public UserAfterCommitListener(ObjectProvider<CacheManager> cacheManagerProvider, StringRedisTemplate redis) {
        this.cacheManagerProvider = cacheManagerProvider;
        this.redis = redis;
    }

    /**
     * 处理已提交用户变化。
     *
     * <p>副作用：按事件标记清理部门相关缓存并原子提升租户授权版本。
     * 缓存基础设施缺失时抛出异常并进入监控，不静默使用 no-op。</p>
     *
     * @param event 已提交的用户变化事实
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(UserChangedEvent event) {
        if (event.affectsDepartmentTree()) {
            CacheManager cacheManager = cacheManagerProvider.getIfAvailable(
                    () -> { throw new IllegalStateException("CacheManager is required for user changes"); });
            for (String name : DEPARTMENT_CACHE_NAMES) {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
        if (event.affectsAuthorization()) {
            redis.opsForValue().increment("chengke:authorization:version:" + event.tenantId());
        }
    }
}
