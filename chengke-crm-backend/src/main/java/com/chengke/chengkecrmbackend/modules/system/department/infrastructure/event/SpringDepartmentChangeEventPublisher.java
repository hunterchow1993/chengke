package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.event;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentChangeEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring 事务事件基础设施发布部门变化事实。
 */
@Component
public class SpringDepartmentChangeEventPublisher implements DepartmentChangeEventPublisher {
    private final ApplicationEventPublisher publisher;

    /** @param publisher Spring 事件发布器 */
    public SpringDepartmentChangeEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** {@inheritDoc} */
    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
