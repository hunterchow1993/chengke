package com.chengke.chengkecrmbackend.shared.security;

/**
 * 从已经验证的 Spring Security 认证对象解析当前租户、操作者、权限和数据范围。
 */
public interface CurrentActorProvider {
    /**
     * 返回当前请求操作者；不会读取客户端自定义租户或操作者 Header。
     *
     * @return 已验证的服务端安全上下文
     */
    CurrentActor currentActor();
}
