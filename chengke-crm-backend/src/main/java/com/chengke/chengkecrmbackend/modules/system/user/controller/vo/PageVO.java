package com.chengke.chengkecrmbackend.modules.system.user.controller.vo;

import java.util.List;

/** HTTP 分页响应。 */
public record PageVO<T>(List<T> items, int page, int pageSize, long total) {
}
