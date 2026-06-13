import { http } from './http'
import type {
  CaptchaDTO,
  RegisterReaderPayload,
  ReaderProfileDTO,
  ReaderPublicProfileDTO,
  UpdateReaderProfilePayload,
  ReadHistoryItem,
  FavoriteItem,
  SubscriptionItem,
  SecurityQuestionDTO,
  ResetPasswordPayload,
  PageResponse,
} from '../types'

export async function getCaptcha(): Promise<CaptchaDTO> {
  const { data } = await http.get<CaptchaDTO>('/reader/captcha')
  return data
}

export async function registerReader(payload: RegisterReaderPayload): Promise<ReaderProfileDTO> {
  const { data } = await http.post<ReaderProfileDTO>('/reader/register', payload)
  return data
}

export async function getReaderProfile(): Promise<ReaderProfileDTO> {
  const { data } = await http.get<ReaderProfileDTO>('/reader/me')
  return data
}

export async function updateReaderProfile(payload: UpdateReaderProfilePayload): Promise<ReaderProfileDTO> {
  const { data } = await http.put<ReaderProfileDTO>('/reader/me', payload)
  return data
}

export async function getReaderPublicProfile(id: number): Promise<ReaderPublicProfileDTO> {
  const { data } = await http.get<ReaderPublicProfileDTO>(`/reader/${id}`)
  return data
}

export async function getSecurityQuestion(username: string): Promise<SecurityQuestionDTO> {
  const { data } = await http.post<SecurityQuestionDTO>('/reader/security-question', { username })
  return data
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  await http.post('/reader/reset-password', payload)
}

export async function recordRead(postId: number): Promise<void> {
  await http.post(`/reader/${postId}/read`)
}

export async function toggleFavorite(postId: number): Promise<void> {
  await http.post(`/reader/${postId}/favorite`)
}

export async function createComment(postId: number, content: string): Promise<void> {
  await http.post(`/reader/${postId}/comment`, null, { params: { content } })
}

export async function toggleSubscription(authorId: number): Promise<void> {
  await http.post(`/reader/subscribe/${authorId}`)
}

export async function getReadHistory(page = 0, size = 20): Promise<PageResponse<ReadHistoryItem>> {
  const { data } = await http.get<PageResponse<ReadHistoryItem>>('/reader/me/history', { params: { page, size } })
  return data
}

export async function getFavorites(page = 0, size = 20): Promise<PageResponse<FavoriteItem>> {
  const { data } = await http.get<PageResponse<FavoriteItem>>('/reader/me/favorites', { params: { page, size } })
  return data
}

export async function getMyComments(page = 0, size = 20) {
  const { data } = await http.get('/reader/me/comments', { params: { page, size } })
  return data
}

export async function getMySubscriptions(page = 0, size = 20): Promise<PageResponse<SubscriptionItem>> {
  const { data } = await http.get<PageResponse<SubscriptionItem>>('/reader/me/subscriptions', { params: { page, size } })
  return data
}
