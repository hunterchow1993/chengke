package com.chengke.chengkecrmbackend.modules.system.user.application.port;

/**
 * 把明文密码转换为不可逆凭据；实现不得记录明文。
 */
public interface PasswordHasher {

    /**
     * 对明文密码做不可逆散列。
     *
     * @param rawPassword 已通过强度校验的明文
     * @return 可安全落库的凭据
     */
    String hash(String rawPassword);

    /**
     * 比对明文与已存储的不可逆凭据。
     *
     * @param rawPassword 用户输入
     * @param passwordHash 库存散列
     * @return 匹配时为 true
     */
    boolean matches(String rawPassword, String passwordHash);
}
