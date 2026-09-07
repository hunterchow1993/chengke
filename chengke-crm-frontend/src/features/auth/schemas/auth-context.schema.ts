import { z } from 'zod';

const applicationPathSchema = z
  .string()
  .startsWith('/app', '授权首页必须是应用内路径')
  .refine((path) => !path.startsWith('//'), '授权首页不能是外部地址');

export const menuItemSchema: z.ZodType<MenuItem> = z.lazy(() =>
  z.object({
    id: z.string().min(1),
    label: z.string().min(1),
    routeKey: z.string().nullable(),
    icon: z.string().nullable(),
    children: z.array(menuItemSchema),
  }),
);

export interface MenuItem {
  id: string;
  label: string;
  routeKey: string | null;
  icon: string | null;
  children: MenuItem[];
}

export const authContextSchema = z.object({
  user: z.object({
    id: z.string().min(1),
    displayName: z.string().min(1),
    avatarUrl: z.url().nullable(),
  }),
  tenant: z.object({
    id: z.string().min(1),
    name: z.string().min(1),
  }),
  menus: z.array(menuItemSchema),
  permissionCodes: z.array(z.string()).default([]),
  dataScopes: z.array(z.string()).default([]),
  sensitiveFieldPermissions: z.array(z.string()).default([]),
  expiresAt: z.iso.datetime({ offset: true }),
  authorizedHome: applicationPathSchema,
  permissionVersion: z.string().min(1).optional(),
});

export type AuthContext = z.infer<typeof authContextSchema>;
