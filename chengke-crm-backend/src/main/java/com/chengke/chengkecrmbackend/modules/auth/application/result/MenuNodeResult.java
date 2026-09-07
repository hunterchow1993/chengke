package com.chengke.chengkecrmbackend.modules.auth.application.result;

import java.util.List;

/**
 * 菜单树节点，字段与登录 Spec MenuNode 一致，并带前端当前契约别名。
 */
public record MenuNodeResult(
        String id,
        String parentId,
        String type,
        String name,
        String routeName,
        String path,
        String iconKey,
        String permissionCode,
        int order,
        List<MenuNodeResult> children
) {
    /** 前端当前菜单 label。 */
    public String label() {
        return name;
    }

    /** 前端当前 routeKey。 */
    public String routeKey() {
        return routeName;
    }

    /** 前端当前 icon。 */
    public String icon() {
        return iconKey;
    }
}
