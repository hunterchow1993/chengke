package com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.dataobject;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 映射 chengke_crm.sys_user 及关联展示列。
 */
public class UserDO {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String username;
    private String avatarUrl;
    private String mobile;
    private String email;
    private UUID departmentId;
    private String departmentName;
    private UUID roleId;
    private String roleName;
    private String roleCode;
    private boolean roleBuiltIn;
    private String status;
    private boolean forcePasswordChange;
    private String remark;
    private int version;
    private UUID updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public boolean isRoleBuiltIn() { return roleBuiltIn; }
    public void setRoleBuiltIn(boolean roleBuiltIn) { this.roleBuiltIn = roleBuiltIn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
