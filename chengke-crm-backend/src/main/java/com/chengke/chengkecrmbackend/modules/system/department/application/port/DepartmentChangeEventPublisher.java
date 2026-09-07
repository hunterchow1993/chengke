package com.chengke.chengkecrmbackend.modules.system.department.application.port;

import org.springframework.lang.NonNull;

/**
 * 发布事务内部门变化事实的端口；监听器只在事务提交后处理缓存和授权版本副作用。
 */
public interface DepartmentChangeEventPublisher {

    /**
     * 发布部门变化事件。
     *
     * @param event 不包含敏感字段的应用事件
     */
    void publish(@NonNull Object event);
}
