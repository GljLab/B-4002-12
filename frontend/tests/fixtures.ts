import { expect, test as base, type APIRequestContext } from '@playwright/test'

export interface ResetPayload {
  includePosts?: boolean
  includeOtherUserPost?: boolean
}

export interface SeedData {
  adminUserId: number
  otherUserId: number
  adminPostId: number | null
  otherPostId: number | null
}

const backendBaseUrl = process.env.E2E_BACKEND_BASE_URL ?? 'http://127.0.0.1:8081'

export async function resetSeed(request: APIRequestContext, payload?: ResetPayload): Promise<SeedData> {
  const response = await request.post(`${backendBaseUrl}/api/testing/reset`, {
    data: payload ?? {},
  })

  expect(response.ok()).toBeTruthy()
  return (await response.json()) as SeedData
}

export const test = base.extend<{ seed: SeedData }>({
  seed: async ({ request }, use) => {
    const seed = await resetSeed(request)
    await use(seed)
  },
})

export { expect }
