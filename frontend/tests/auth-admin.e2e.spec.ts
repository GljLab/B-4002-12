import { expect, test } from './fixtures'
import { assertToast, loginAsAdmin } from './helpers'

test.describe('登录与后台全链路', () => {
  test('未登录访问后台会跳转登录', async ({ page }) => {
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/login/)
  })

  test('登录表单空值分支', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('请输入用户名').fill('')
    await page.getByPlaceholder('请输入密码').fill('')
    await page.getByRole('button', { name: '登录' }).click()
    await assertToast(page, '用户名和密码不能为空')
  })

  test('登录失败分支', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('请输入用户名').fill('admin')
    await page.getByPlaceholder('请输入密码').fill('wrong-password')
    await page.getByRole('button', { name: '登录' }).click()
    await assertToast(page, '用户名或密码错误')
  })

  test('后台发布成功并在首页可见（含标题长度边界=200）', async ({ page, seed }) => {
    expect(seed.adminUserId).toBeGreaterThan(0)
    await loginAsAdmin(page)

    const title200 = 'T'.repeat(200)
    const content = '这是一篇由E2E发布的文章内容。'

    await page.getByPlaceholder('请输入文章标题').fill(title200)
    await page.getByPlaceholder('请输入正文内容').fill(content)
    await page.getByRole('button', { name: '发布文章' }).click()

    await assertToast(page, '文章发布成功')
    await expect(page.locator('.el-table')).toContainText(title200)

    await page.goto('/')
    await expect(page.getByText(title200)).toBeVisible()
  })

  test('后台发布空值分支', async ({ page, seed }) => {
    expect(seed.adminUserId).toBeGreaterThan(0)
    await loginAsAdmin(page)
    await page.getByPlaceholder('请输入文章标题').fill('')
    await page.getByPlaceholder('请输入正文内容').fill('')
    await page.getByRole('button', { name: '发布文章' }).click()
    await assertToast(page, '标题和内容不能为空')
  })

  test('编辑本人文章成功分支', async ({ page, seed }) => {
    await loginAsAdmin(page)
    await page.goto(`/admin/posts/${seed.adminPostId}/edit`)

    const titleInput = page.getByPlaceholder('请输入文章标题').first()
    await titleInput.click()
    await titleInput.press('Meta+A')
    await titleInput.fill('管理员示例文章-已编辑')

    const contentInput = page.getByPlaceholder('请输入正文内容').first()
    await contentInput.click()
    await contentInput.press('Meta+A')
    await contentInput.fill('管理员正文已更新')

    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.request().method() === 'PUT'
          && response.url().includes(`/api/v1/admin/posts/${seed.adminPostId}`)
          && response.status() === 200,
      ),
      page.getByRole('button', { name: '保存修改' }).click(),
    ])

    await assertToast(page, '文章编辑成功')
    await expect(page).toHaveURL(/\/admin$/)
    await expect(page.locator('.el-table')).toContainText('管理员示例文章-已编辑')
  })

  test('编辑他人文章越权分支（403）', async ({ page, seed }) => {
    await loginAsAdmin(page)
    await page.goto(`/admin/posts/${seed.otherPostId}/edit`)

    await page.getByRole('button', { name: '保存修改' }).click()
    await assertToast(page, '文章编辑失败')
  })

  test('删除本人文章成功分支', async ({ page, seed }) => {
    await loginAsAdmin(page)
    await expect(page.locator('.el-table')).toContainText('管理员示例文章')

    const row = page.locator('.el-table__row').filter({ hasText: '管理员示例文章' }).first()
    await row.getByRole('button', { name: '删除' }).click()

    const dialog = page.locator('.el-message-box')
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', { name: '删除' }).click()

    await assertToast(page, '删除成功')
    await page.goto(`/posts/${seed.adminPostId}`)
    await expect(page.getByText('未找到该文章')).toBeVisible()
  })

  test('退出登录后再次访问后台分支', async ({ page }) => {
    await loginAsAdmin(page)
    await page.getByRole('link', { name: /退出（admin）/ }).click()
    await expect(page).toHaveURL(/\/login/)

    await page.goto('/admin')
    await expect(page).toHaveURL(/\/login/)
  })
})
