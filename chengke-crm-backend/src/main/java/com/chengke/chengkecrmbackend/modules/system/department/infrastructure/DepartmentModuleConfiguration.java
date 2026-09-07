package com.chengke.chengkecrmbackend.modules.system.department.infrastructure;

import com.chengke.chengkecrmbackend.modules.system.department.domain.policy.DepartmentPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 提供部门领域策略和可测试系统时钟。
 */
@Configuration
public class DepartmentModuleConfiguration {
    /** @return 最大深度固定为 10 的部门策略 */
    @Bean
    public DepartmentPolicy departmentPolicy() {
        return new DepartmentPolicy(10);
    }

    /** @return 使用系统默认时区的生产时钟 */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
