import { http } from './http'
import type {
  PageResponse,
  PostSummary,
  PostDetail,
  CreatePostPayload,
  UpdatePostPayload,
  AuthorStatsDTO,
  AuthorDTO,
  UpdateProfilePayload,
  ChangePasswordPayload,
  RevisionDiffDTO,
  ScheduledTaskDTO,
} from '../types'

export async function getPublicPosts(
  page = 1,
  size = 10,
  categoryId?: number | null,
  keywordId?: number | null,
  authorId?: number | null,
): Promise<PageResponse<PostSummary>> {
  const params: Record<string, unknown> = { page, size }
  if (categoryId != null) params.categoryId = categoryId
  if (keywordId != null) params.keywordId = keywordId
  if (authorId != null) params.authorId = authorId
  const { data } = await http.get<PageResponse<PostSummary>>('/posts', { params })
  return data
}

export async function getPostDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/posts/${id}`)
  return data
}

export async function getAdminPostDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/admin/posts/${id}`)
  return data
}

export async function getAuthorPostDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/author/posts/${id}`)
  return data
}

export async function getMyPosts(): Promise<PostSummary[]> {
  const { data } = await http.get<PostSummary[]>('/admin/posts/mine')
  return data
}

export async function createPost(payload: CreatePostPayload): Promise<PostDetail> {
  const { data } = await http.post<PostDetail>('/admin/posts', payload)
  return data
}

export async function updatePost(id: number, payload: UpdatePostPayload): Promise<PostDetail> {
  const { data } = await http.put<PostDetail>(`/admin/posts/${id}`, payload)
  return data
}

export async function deletePost(id: number): Promise<void> {
  await http.delete(`/admin/posts/${id}`)
}

export async function batchUpdateCategory(postIds: number[], categoryId: number): Promise<void> {
  await http.put('/admin/posts/batch/category', { postIds, categoryId })
}

export async function batchAddKeywords(postIds: number[], keywords: string[]): Promise<void> {
  await http.post('/admin/posts/batch/keywords', { postIds, keywords })
}

export async function getAuthorPosts(params: { status?: string; page?: number; size?: number }): Promise<PageResponse<PostSummary>> {
  const { data } = await http.get<PageResponse<PostSummary>>('/author/posts', { params })
  return data
}

export async function createAuthorPost(payload: CreatePostPayload): Promise<PostDetail> {
  const { data } = await http.post<PostDetail>('/author/posts', payload)
  return data
}

export async function updateAuthorPost(id: number, payload: UpdatePostPayload): Promise<PostDetail> {
  const { data } = await http.put<PostDetail>(`/author/posts/${id}`, payload)
  return data
}

export async function submitForReview(id: number): Promise<void> {
  await http.put(`/author/posts/${id}/submit`)
}

export async function deleteAuthorPost(id: number): Promise<void> {
  await http.delete(`/author/posts/${id}`)
}

export async function getAuthorStats(): Promise<AuthorStatsDTO> {
  const { data } = await http.get<AuthorStatsDTO>('/author/stats')
  return data
}

export async function getAuthorProfile(): Promise<AuthorDTO> {
  const { data } = await http.get<AuthorDTO>('/author/profile')
  return data
}

export async function updateAuthorProfile(payload: UpdateProfilePayload): Promise<AuthorDTO> {
  const { data } = await http.put<AuthorDTO>('/author/profile', payload)
  return data
}

export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  await http.put('/author/password', payload)
}

export async function getAdminPosts(status?: string): Promise<PostSummary[]> {
  const params: Record<string, unknown> = {}
  if (status) params.status = status
  const { data } = await http.get<PostSummary[]>('/admin/posts', { params })
  return data
}

export async function unpublishPost(id: number): Promise<void> {
  await http.put(`/admin/posts/${id}/unpublish`)
}

export async function cancelSchedule(id: number): Promise<void> {
  await http.put(`/author/posts/${id}/cancel-schedule`)
}

export async function updateSchedule(id: number, scheduledAt: string): Promise<void> {
  await http.put(`/author/posts/${id}/schedule`, null, { params: { scheduledAt } })
}

export async function getRevisionDiff(id: number): Promise<RevisionDiffDTO> {
  const { data } = await http.get<RevisionDiffDTO>(`/author/posts/${id}/revision-diff`)
  return data
}

export async function getRevisionDetail(parentPostId: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/author/posts/${parentPostId}/revision`)
  return data
}

export async function discardRevision(id: number): Promise<void> {
  await http.delete(`/author/posts/${id}/revision`)
}

export async function getScheduledTasks(page = 1, size = 20): Promise<PageResponse<ScheduledTaskDTO>> {
  const { data } = await http.get<PageResponse<ScheduledTaskDTO>>('/admin/posts/scheduled', { params: { page, size } })
  return data
}

export async function forcePublishScheduled(id: number): Promise<void> {
  await http.put(`/admin/posts/${id}/force-publish`)
}

export async function adminCancelSchedule(id: number): Promise<void> {
  await http.put(`/admin/posts/${id}/cancel-schedule`)
}

export async function getRevisionDiffAdmin(id: number): Promise<RevisionDiffDTO> {
  const { data } = await http.get<RevisionDiffDTO>(`/admin/reviews/${id}/revision-diff`)
  return data
}
