package com.chengke.chengkecrmbackend.modules.system.department.application.result;

import java.util.List;

/** 通用应用层分页结果。 */
public record PageResult<T>(List<T> items, int page, int pageSize, long total) {
}
