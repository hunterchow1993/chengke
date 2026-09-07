package com.chengke.chengkecrmbackend.modules.system.department.application.query;

import com.chengke.chengkecrmbackend.shared.security.CurrentActor;

/** 负责人候选分页查询条件。 */
public record LeaderCandidateQuery(CurrentActor actor, String keyword, int page, int pageSize) {
}
