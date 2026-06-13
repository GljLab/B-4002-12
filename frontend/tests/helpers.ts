import { expect, type Page } from '@playwright/test'

export async function assertToast(page: Page, message: string) {
  const locator = page.locator('.el-message__content').filter({ hasText: message }).first()
  await expect(locator).toBeVisible()
}

export async function loginAsAdmin(page: Page) {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('admin123456')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/admin/, { timeout: 15_000 })
}
