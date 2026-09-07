package com.chengke.chengkecrmbackend.modules.auth.application;

import com.chengke.chengkecrmbackend.modules.auth.application.result.MenuNodeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 按功能权限筛选内置菜单树，并删除空目录。
 */
public final class MenuCatalog {

    private MenuCatalog() {
    }

    /**
     * @param permissions 当前角色权限码
     * @return 已排序且去掉空目录的菜单
     */
    public static List<MenuNodeResult> forPermissions(Set<String> permissions) {
        List<MenuNodeResult> roots = new ArrayList<>();
        roots.add(page("menu-dashboard", null, "工作台", "dashboard", "/app/dashboard", "layout-dashboard", null, 1));
        List<MenuNodeResult> systemChildren = new ArrayList<>();
        addIfPermitted(systemChildren, permissions,
                page("menu-users", "menu-system", "用户管理", "system.users",
                        "/app/system/users", "users", "system:user:view", 1));
        addIfPermitted(systemChildren, permissions,
                page("menu-departments", "menu-system", "部门管理", "system.departments",
                        "/app/system/departments", "network", "system:department:view", 2));
        if (!systemChildren.isEmpty()) {
            roots.add(new MenuNodeResult("menu-system", null, "directory", "系统管理",
                    null, null, "settings", null, 2, List.copyOf(systemChildren)));
        }
        return List.copyOf(roots);
    }

    private static void addIfPermitted(List<MenuNodeResult> target, Set<String> permissions, MenuNodeResult node) {
        if (node.permissionCode() == null || permissions.contains(node.permissionCode())) {
            target.add(node);
        }
    }

    private static MenuNodeResult page(String id, String parentId, String name, String routeName,
                                       String path, String iconKey, String permissionCode, int order) {
        return new MenuNodeResult(id, parentId, "page", name, routeName, path, iconKey, permissionCode, order, List.of());
    }
}
