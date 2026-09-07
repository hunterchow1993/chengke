package com.chengke.chengkecrmbackend.modules.system.user.application.port;

/**
 * 发布事务内用户变化事实的端口；监听器只在事务提交后处理缓存和授权版本副作用。
 */
public interface UserChangeEventPublisher {

    /**
     * 发布用户变化事件。
     *
     * @param event 不包含敏感字段的应用事件
     */
    void publish(Object event);
}
