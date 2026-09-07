package com.chengke.chengkecrmbackend.modules.system.user.application.port.model;

import java.util.UUID;

/**
 * 写入审计快照的用户字段，禁止包含任何密码凭据。
 */
public record UserAuditSnapshot(
        UUID id,
        String name,
        String username,
        String avatarUrl,
        String mobile,
        String email,
        UUID departmentId,
        UUID roleId,
        String status,
        Boolean forcePasswordChange,
        String remark,
        Integer version
) {
    /**
     * 从用户记录生成不含密码的审计快照。
     *
     * @param user 最新用户记录
     * @return 可序列化为 JSON 的快照
     */
    public static UserAuditSnapshot from(UserRecord user) {
        return new UserAuditSnapshot(
                user.id(), user.name(), user.username(), user.avatarUrl(), user.mobile(), user.email(),
                user.departmentId(), user.roleId(), user.status().databaseValue(), user.forcePasswordChange(),
                user.remark(), user.version()
        );
    }
}
