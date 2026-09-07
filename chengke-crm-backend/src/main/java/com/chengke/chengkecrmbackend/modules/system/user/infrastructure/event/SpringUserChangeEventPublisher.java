package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.event;

import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserChangeEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring 事务事件基础设施发布用户变化事实。
 */
@Component
public class SpringUserChangeEventPublisher implements UserChangeEventPublisher {
    private final ApplicationEventPublisher publisher;

    /** @param publisher Spring 事件发布器 */
    public SpringUserChangeEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** {@inheritDoc} */
    @Override
    public void publish(@NonNull Object event) {
        publisher.publishEvent(event);
    }
}
