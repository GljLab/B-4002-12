import { http } from './http'
import type { AuthorDTO, AuthorPublicDTO, CreateAuthorPayload, UpdateAuthorPayload, PageResponse, PostSummary } from '../types'

export async function getAdminAuthors(): Promise<AuthorDTO[]> {
  const { data } = await http.get<AuthorDTO[]>('/admin/authors')
  return data
}

export async function createAuthor(payload: CreateAuthorPayload): Promise<AuthorDTO> {
  const { data } = await http.post<AuthorDTO>('/admin/authors', payload)
  return data
}

export async function getAuthor(id: number): Promise<AuthorDTO> {
  const { data } = await http.get<AuthorDTO>(`/admin/authors/${id}`)
  return data
}

export async function updateAuthor(id: number, payload: UpdateAuthorPayload): Promise<AuthorDTO> {
  const { data } = await http.put<AuthorDTO>(`/admin/authors/${id}`, payload)
  return data
}

export async function resetAuthorPassword(id: number, newPassword: string): Promise<void> {
  await http.put(`/admin/authors/${id}/reset-password`, { newPassword })
}

export async function disableAuthor(id: number): Promise<void> {
  await http.put(`/admin/authors/${id}/disable`)
}

export async function enableAuthor(id: number): Promise<void> {
  await http.put(`/admin/authors/${id}/enable`)
}

export async function deleteAuthor(id: number, transferTo?: number): Promise<void> {
  const params: Record<string, unknown> = {}
  if (transferTo != null) params.transferTo = transferTo
  await http.delete(`/admin/authors/${id}`, { params })
}

export async function getPublicAuthors(): Promise<AuthorPublicDTO[]> {
  const { data } = await http.get<AuthorPublicDTO[]>('/authors')
  return data
}

export async function getPublicAuthorProfile(id: number): Promise<AuthorPublicDTO> {
  const { data } = await http.get<AuthorPublicDTO>(`/authors/${id}`)
  return data
}

export async function getAuthorPublicPosts(id: number, params: { page?: number; size?: number; categoryId?: number }): Promise<PageResponse<PostSummary>> {
  const { data } = await http.get<PageResponse<PostSummary>>(`/authors/${id}/posts`, { params })
  return data
}
