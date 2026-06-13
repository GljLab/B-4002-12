import { http } from './http'
import type {
  CommentTreeDTO,
  CreateDiscussionRequest,
  UpdateDiscussionRequest,
  CommentVoteRequest,
  CommentVoteResultDTO,
  CommentReportDTO,
  CreateCommentReportRequest,
  PinnedCommentResultDTO,
  PostCommentSettingsDTO,
  UpdatePostCommentSettingsRequest,
  DiscussionListResponse,
  MessageResponse,
} from '../types'

export async function getDiscussionsForPost(
  postId: number,
  sortBy: 'newest' | 'hottest' | 'oldest' = 'newest',
  page = 1,
): Promise<DiscussionListResponse> {
  const { data } = await http.get<DiscussionListResponse>(`/discussions/posts/${postId}`, {
    params: { sortBy, page },
  })
  return data
}

export async function getMoreReplies(
  commentId: number,
  page = 1,
): Promise<DiscussionListResponse> {
  const { data } = await http.get<DiscussionListResponse>(`/discussions/${commentId}/replies`, {
    params: { page },
  })
  return data
}

export async function createDiscussion(
  postId: number,
  payload: CreateDiscussionRequest,
): Promise<CommentTreeDTO> {
  const { data } = await http.post<CommentTreeDTO>(`/discussions/posts/${postId}`, payload)
  return data
}

export async function updateDiscussion(
  commentId: number,
  payload: UpdateDiscussionRequest,
): Promise<CommentTreeDTO> {
  const { data } = await http.put<CommentTreeDTO>(`/discussions/${commentId}`, payload)
  return data
}

export async function deleteDiscussion(commentId: number): Promise<MessageResponse> {
  const { data } = await http.delete<MessageResponse>(`/discussions/${commentId}`)
  return data
}

export async function voteComment(
  commentId: number,
  voteType: number,
): Promise<CommentVoteResultDTO> {
  const payload: CommentVoteRequest = { voteType }
  const { data } = await http.post<CommentVoteResultDTO>(`/discussions/${commentId}/vote`, payload)
  return data
}

export async function reportComment(
  commentId: number,
  payload: CreateCommentReportRequest,
): Promise<CommentReportDTO> {
  const { data } = await http.post<CommentReportDTO>(`/discussions/${commentId}/report`, payload)
  return data
}

export async function togglePinComment(commentId: number): Promise<PinnedCommentResultDTO> {
  const { data } = await http.post<PinnedCommentResultDTO>(`/discussions/${commentId}/pin`)
  return data
}

export async function getCommentSettings(postId: number): Promise<PostCommentSettingsDTO> {
  const { data } = await http.get<PostCommentSettingsDTO>(`/discussions/posts/${postId}/settings`)
  return data
}

export async function updateCommentSettings(
  postId: number,
  payload: UpdatePostCommentSettingsRequest,
): Promise<PostCommentSettingsDTO> {
  const { data } = await http.patch<PostCommentSettingsDTO>(
    `/discussions/posts/${postId}/settings`,
    payload,
  )
  return data
}
