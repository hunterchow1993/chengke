package com.chengke.chengkecrmbackend.modules.system.user.domain.policy;

import com.chengke.chengkecrmbackend.modules.system.user.domain.exception.UserDomainException;

/**
 * 集中执行用户脱敏、密码强度和保护账号规则。
 *
 * <p>该策略无数据库与框架副作用；调用方仍需在事务中读取最新快照并由唯一约束做最终校验。</p>
 */
public final class UserPolicy {

    /**
     * 按「前 3 位 + **** + 后 4 位」脱敏 11 位手机号。
     *
     * @param mobile 完整手机号
     * @return 脱敏后的手机号；空值原样返回
     */
    public String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    /**
     * 按「本地首字符 + *** + @域名」脱敏邮箱。
     *
     * @param email 完整小写邮箱
     * @return 脱敏后的邮箱；空值原样返回
     */
    public String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 判断密码是否满足：8–32 字符，字母 / 数字 / 特殊字符至少两类，且不含连续空格。
     *
     * @param password 明文密码
     * @return 满足强度规则时为 true
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 32 || password.contains("  ")) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                letter = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                special = true;
            }
        }
        int classes = (letter ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
        return classes >= 2;
    }

    /**
     * 校验停用是否违反本人或唯一超级管理员保护。
     *
     * @param targetIsSelf 目标是否为当前操作者
     * @param targetIsLastSuperAdmin 目标是否为租户内唯一正常超级管理员
     * @throws UserDomainException 保护规则拒绝时抛出
     */
    public void validateDisable(boolean targetIsSelf, boolean targetIsLastSuperAdmin) {
        if (targetIsSelf) {
            throw new UserDomainException("USER_SELF_DISABLE_FORBIDDEN");
        }
        if (targetIsLastSuperAdmin) {
            throw new UserDomainException("USER_LAST_SUPER_ADMIN_PROTECTED");
        }
    }

    /**
     * 校验是否允许修改目标用户角色。
     *
     * @param targetIsSelf 目标是否为当前操作者
     * @param roleChanged 提交角色是否与当前角色不同
     * @throws UserDomainException 本人修改角色时抛出
     */
    public void validateRoleChange(boolean targetIsSelf, boolean roleChanged) {
        if (targetIsSelf && roleChanged) {
            throw new UserDomainException("USER_SELF_ROLE_CHANGE_FORBIDDEN");
        }
    }
}
