package com.chengke.chengkecrmbackend.modules.system.department.infrastructure.persistence.dataobject;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 映射 chengke_crm.department 的未删除部门行。
 */
public class DepartmentDO {
    private UUID id;
    private UUID tenantId;
    private UUID parentId;
    private String name;
    private String code;
    private String nodeType;
    private UUID leaderUserId;
    private int sortOrder;
    private String status;
    private String remark;
    private int depth;
    private int version;
    private UUID updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** @return 部门标识 */
    public UUID getId() { return id; }
    /** @param id 部门标识 */
    public void setId(UUID id) { this.id = id; }
    /** @return 租户标识 */
    public UUID getTenantId() { return tenantId; }
    /** @param tenantId 租户标识 */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    /** @return 父部门标识 */
    public UUID getParentId() { return parentId; }
    /** @param parentId 父部门标识 */
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    /** @return 部门名称 */
    public String getName() { return name; }
    /** @param name 部门名称 */
    public void setName(String name) { this.name = name; }
    /** @return 部门编码 */
    public String getCode() { return code; }
    /** @param code 部门编码 */
    public void setCode(String code) { this.code = code; }
    /** @return 数据库节点类型 */
    public String getNodeType() { return nodeType; }
    /** @param nodeType 数据库节点类型 */
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    /** @return 负责人用户标识 */
    public UUID getLeaderUserId() { return leaderUserId; }
    /** @param leaderUserId 负责人用户标识 */
    public void setLeaderUserId(UUID leaderUserId) { this.leaderUserId = leaderUserId; }
    /** @return 排序值 */
    public int getSortOrder() { return sortOrder; }
    /** @param sortOrder 排序值 */
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    /** @return 数据库状态 */
    public String getStatus() { return status; }
    /** @param status 数据库状态 */
    public void setStatus(String status) { this.status = status; }
    /** @return 备注 */
    public String getRemark() { return remark; }
    /** @param remark 备注 */
    public void setRemark(String remark) { this.remark = remark; }
    /** @return 深度 */
    public int getDepth() { return depth; }
    /** @param depth 深度 */
    public void setDepth(int depth) { this.depth = depth; }
    /** @return 乐观锁版本 */
    public int getVersion() { return version; }
    /** @param version 乐观锁版本 */
    public void setVersion(int version) { this.version = version; }
    /** @return 最近更新操作者 */
    public UUID getUpdatedBy() { return updatedBy; }
    /** @param updatedBy 最近更新操作者 */
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    /** @return 创建时间 */
    public OffsetDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    /** @return 更新时间 */
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt 更新时间 */
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
