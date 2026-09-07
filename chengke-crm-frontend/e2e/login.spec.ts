import { expect, test } from '@playwright/test';

test('登录页可访问并执行客户端字段校验', async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });

  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible();
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page.getByText('请输入账号')).toBeVisible();
  await expect(page.getByText('密码至少需要 8 个字符')).toBeVisible();
  expect(consoleErrors).toEqual([]);
});
