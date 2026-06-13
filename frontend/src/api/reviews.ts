import { http } from './http'
import type {
  PageResponse,
  PostSummary,
  PostDetail,
  ReviewActionPayload,
  BatchReviewPayload,
  CreateReviewCommentPayload,
  ReviewCommentDTO,
  ReviewRoundDTO,
  ReviewTemplateDTO,
  CreateReviewTemplatePayload,
  UpdateReviewTemplatePayload,
  ReviewTimelineDTO,
  ResubmitWithNotePayload,
  AuthorReplyPayload,
} from '../types'

export async function getPendingPosts(page = 1, size = 20): Promise<PageResponse<PostSummary>> {
  const { data } = await http.get<PageResponse<PostSummary>>('/admin/reviews', { params: { page, size } })
  return data
}

export async function getReviewDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/admin/reviews/${id}`)
  return data
}

export async function reviewAction(id: number, payload: ReviewActionPayload): Promise<void> {
  await http.put(`/admin/reviews/${id}`, payload)
}

export async function batchReview(payload: BatchReviewPayload): Promise<void> {
  await http.put('/admin/reviews/batch', payload)
}

export async function addReviewComment(postId: number, payload: CreateReviewCommentPayload): Promise<ReviewCommentDTO> {
  const { data } = await http.post<ReviewCommentDTO>(`/admin/reviews/${postId}/comments`, payload)
  return data
}

export async function getReviewHistory(postId: number): Promise<ReviewRoundDTO[]> {
  const { data } = await http.get<ReviewRoundDTO[]>(`/admin/reviews/${postId}/history`)
  return data
}

export async function getReviewTimeline(postId: number): Promise<ReviewTimelineDTO> {
  const { data } = await http.get<ReviewTimelineDTO>(`/admin/reviews/${postId}/timeline`)
  return data
}

export async function getReviewTemplates(): Promise<ReviewTemplateDTO[]> {
  const { data } = await http.get<ReviewTemplateDTO[]>('/admin/reviews/templates')
  return data
}

export async function createReviewTemplate(payload: CreateReviewTemplatePayload): Promise<ReviewTemplateDTO> {
  const { data } = await http.post<ReviewTemplateDTO>('/admin/reviews/templates', payload)
  return data
}

export async function updateReviewTemplate(id: number, payload: UpdateReviewTemplatePayload): Promise<ReviewTemplateDTO> {
  const { data } = await http.put<ReviewTemplateDTO>(`/admin/reviews/templates/${id}`, payload)
  return data
}

export async function deleteReviewTemplate(id: number): Promise<void> {
  await http.delete(`/admin/reviews/templates/${id}`)
}

export async function getAuthorReviewHistory(postId: number): Promise<ReviewRoundDTO[]> {
  const { data } = await http.get<ReviewRoundDTO[]>(`/author/reviews/${postId}/history`)
  return data
}

export async function getAuthorReviewTimeline(postId: number): Promise<ReviewTimelineDTO> {
  const { data } = await http.get<ReviewTimelineDTO>(`/author/reviews/${postId}/timeline`)
  return data
}

export async function replyToComment(commentId: number, payload: AuthorReplyPayload): Promise<ReviewCommentDTO> {
  const { data } = await http.put<ReviewCommentDTO>(`/author/reviews/comments/${commentId}/reply`, payload)
  return data
}

export async function toggleCommentResolved(commentId: number): Promise<ReviewCommentDTO> {
  const { data } = await http.put<ReviewCommentDTO>(`/author/reviews/comments/${commentId}/toggle-resolved`)
  return data
}

export async function resubmitWithNote(postId: number, payload: ResubmitWithNotePayload): Promise<void> {
  await http.put(`/author/reviews/${postId}/resubmit`, payload)
}
