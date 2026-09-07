package com.chengke.chengkecrmbackend.shared.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 开启 Controller 的功能权限注解校验；应用服务仍执行节点范围和最新状态复核。
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfiguration {
}
