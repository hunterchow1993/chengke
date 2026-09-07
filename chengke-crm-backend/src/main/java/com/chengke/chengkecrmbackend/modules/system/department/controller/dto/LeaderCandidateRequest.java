package com.chengke.chengkecrmbackend.modules.system.department.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 负责人候选 HTTP 查询参数。 */
@Schema(description = "负责人候选分页参数")
public record LeaderCandidateRequest(
        @Size(max = 50) String keyword,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer pageSize
) {
    /** 统一分页缺省值。 */
    public LeaderCandidateRequest {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
    }
}
