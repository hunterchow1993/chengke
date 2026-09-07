package com.chengke.chengkecrmbackend.modules.system.department.application.port;

import com.chengke.chengkecrmbackend.modules.system.department.application.port.model.PreviewTokenBinding;

import java.util.Optional;

/**
 * 保存并原子消费部门影响预览令牌的端口；生产适配器必须提供跨实例一致性。
 */
public interface DepartmentPreviewTokenStore {

    /**
     * 保存预览绑定。
     *
     * @param binding 租户、操作者、目标、版本和有效期绑定
     * @return 新签发的不可预测预览令牌
     */
    String save(PreviewTokenBinding binding);

    /**
     * 原子消费令牌。
     *
     * @param token 不可预测预览令牌
     * @return 原子消费成功时返回绑定；不存在、过期或已消费时为空
     */
    Optional<PreviewTokenBinding> consume(String token);
}
