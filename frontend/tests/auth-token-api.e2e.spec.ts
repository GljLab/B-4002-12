import { expect, resetSeed, test } from './fixtures'

test.describe('鉴权接口全链路（Session + JWT）', () => {
  test('Session 登录后可访问 me，登出后返回 401', async ({ request }) => {
    await resetSeed(request)

    const loginResponse = await request.post('http://127.0.0.1:8081/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin123456' },
    })
    expect(loginResponse.ok()).toBeTruthy()

    const meResponse = await request.get('http://127.0.0.1:8081/api/v1/auth/me')
    expect(meResponse.ok()).toBeTruthy()
    const meData = await meResponse.json()
    expect(meData.username).toBe('admin')

    const logoutResponse = await request.post('http://127.0.0.1:8081/api/v1/auth/logout')
    expect(logoutResponse.ok()).toBeTruthy()

    const forbiddenResponse = await request.get('http://127.0.0.1:8081/api/v1/admin/posts/mine')
    expect(forbiddenResponse.status()).toBe(401)
    const forbiddenBody = await forbiddenResponse.json()
    expect(forbiddenBody.code).toBe('UNAUTHORIZED')
  })

  test('JWT 登录/刷新/受保护接口成功分支', async ({ request }) => {
    await resetSeed(request)

    const loginResponse = await request.post('http://127.0.0.1:8081/api/v1/token/login', {
      data: { username: 'admin', password: 'admin123456' },
    })
    expect(loginResponse.ok()).toBeTruthy()
    const loginData = await loginResponse.json()

    const secureResponse = await request.get('http://127.0.0.1:8081/api/v1/token/secure/me', {
      headers: {
        Authorization: `Bearer ${loginData.accessToken}`,
      },
    })
    expect(secureResponse.ok()).toBeTruthy()
    const secureData = await secureResponse.json()
    expect(secureData.username).toBe('admin')

    const refreshResponse = await request.post('http://127.0.0.1:8081/api/v1/token/refresh', {
      data: { refreshToken: loginData.refreshToken },
    })
    expect(refreshResponse.ok()).toBeTruthy()
    const refreshData = await refreshResponse.json()
    expect(typeof refreshData.accessToken).toBe('string')
    expect(typeof refreshData.refreshToken).toBe('string')
    expect(refreshData.refreshToken).not.toBe(loginData.refreshToken)
  })

  test('JWT 非法 accessToken / refreshToken 分支', async ({ request }) => {
    await resetSeed(request)

    const invalidAccessResponse = await request.get('http://127.0.0.1:8081/api/v1/token/secure/me', {
      headers: {
        Authorization: 'Bearer invalid-token',
      },
    })
    expect(invalidAccessResponse.status()).toBe(401)
    const invalidAccessBody = await invalidAccessResponse.json()
    expect(invalidAccessBody.code).toBe('UNAUTHORIZED')

    const invalidRefreshResponse = await request.post('http://127.0.0.1:8081/api/v1/token/refresh', {
      data: { refreshToken: 'invalid-refresh-token' },
    })
    expect(invalidRefreshResponse.status()).toBe(401)
    const invalidRefreshBody = await invalidRefreshResponse.json()
    expect(invalidRefreshBody.code).toBe('UNAUTHORIZED')
  })
})
