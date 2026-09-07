package com.chengke.chengkecrmbackend.modules.system.user.infrastructure;

import com.chengke.chengkecrmbackend.modules.system.user.domain.policy.UserPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 提供用户领域策略和批量停用逐项独立事务模板。
 */
@Configuration
public class UserModuleConfiguration {

    /** @return 用户脱敏与保护规则 */
    @Bean
    public UserPolicy userPolicy() {
        return new UserPolicy();
    }

    /**
     * 批量停用中每个用户使用独立事务，避免单项失败回滚其他成功项。
     *
     * @param transactionManager 平台事务管理器
     * @return REQUIRES_NEW 事务模板
     */
    @Bean(name = "userRequiresNewTransactionTemplate")
    @ConditionalOnBean(PlatformTransactionManager.class)
    public TransactionTemplate userRequiresNewTransactionTemplate(@NonNull PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
