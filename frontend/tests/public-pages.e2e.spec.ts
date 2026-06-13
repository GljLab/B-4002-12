import { expect, resetSeed, test } from './fixtures'

test.describe('公开页面全链路', () => {
  test('首页展示文章列表并可跳转详情', async ({ page, seed }) => {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: '最新文章' })).toBeVisible()
    await expect(page.getByText('管理员示例文章')).toBeVisible()

    await page.getByRole('link', { name: '管理员示例文章' }).click()
    await expect(page).toHaveURL(new RegExp(`/posts/${seed.adminPostId}$`))
    await expect(page.getByText('管理员示例正文，用于E2E回归。')).toBeVisible()
  })

  test('首页空列表分支', async ({ page, request }) => {
    await resetSeed(request, { includePosts: false, includeOtherUserPost: false })
    await page.goto('/')

    await expect(page.getByText('暂无文章')).toBeVisible()
  })

  test('详情页不存在分支', async ({ page }) => {
    await page.goto('/posts/999999')
    await expect(page.getByText('未找到该文章')).toBeVisible()
  })

  test('详情页非法参数分支', async ({ page }) => {
    await page.goto('/posts/abc')
    await expect(page.getByText('未找到该文章')).toBeVisible()
  })
})
