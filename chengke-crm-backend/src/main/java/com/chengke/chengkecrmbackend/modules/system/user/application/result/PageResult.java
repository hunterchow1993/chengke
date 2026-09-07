package com.chengke.chengkecrmbackend.modules.system.user.application.result;

import java.util.List;

/** 用户应用层分页结果。 */
public record PageResult<T>(List<T> items, int page, int pageSize, long total) {
}
