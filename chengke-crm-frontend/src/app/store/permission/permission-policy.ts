/**
 * 判断当前授权集合是否包含路由或按钮要求的全部权限码。
 * @param required 功能声明的权限码；为空表示仅要求登录。
 * @param granted 后端授权上下文返回的权限码集合。
 * @returns 是否满足全部权限要求。
 */
export function hasRequiredPermissions(
  required: readonly string[] | undefined,
  granted: ReadonlySet<string>,
): boolean {
  return !required?.length || required.every((permission) => granted.has(permission));
}
