import { http } from './http'
import type {
  CollectionAlbum,
  CollectionItem,
  CreateAlbumPayload,
  UpdateAlbumPayload,
  AddToAlbumPayload,
  BatchCollectionActionPayload,
  ReorderItemsPayload,
  PostFavoriteCount,
  ReadHistoryDetail,
  FavoriteDetail,
  ReadingProgress,
  PageResponse,
} from '../types'

export async function listAlbums(): Promise<CollectionAlbum[]> {
  const { data } = await http.get<CollectionAlbum[]>('/collections/albums')
  return data
}

export async function createAlbum(payload: CreateAlbumPayload): Promise<CollectionAlbum> {
  const { data } = await http.post<CollectionAlbum>('/collections/albums', payload)
  return data
}

export async function updateAlbum(albumId: number, payload: UpdateAlbumPayload): Promise<CollectionAlbum> {
  const { data } = await http.put<CollectionAlbum>(`/collections/albums/${albumId}`, payload)
  return data
}

export async function deleteAlbum(albumId: number): Promise<void> {
  await http.delete(`/collections/albums/${albumId}`)
}

export async function listAlbumItems(albumId: number): Promise<CollectionItem[]> {
  const { data } = await http.get<CollectionItem[]>(`/collections/albums/${albumId}/items`)
  return data
}

export async function addItemToAlbum(albumId: number, payload: AddToAlbumPayload): Promise<CollectionItem> {
  const { data } = await http.post<CollectionItem>(`/collections/albums/${albumId}/items`, payload)
  return data
}

export async function removeItemFromAlbum(albumId: number, postId: number): Promise<void> {
  await http.delete(`/collections/albums/${albumId}/items/${postId}`)
}

export async function reorderAlbumItems(albumId: number, payload: ReorderItemsPayload): Promise<void> {
  await http.put(`/collections/albums/${albumId}/reorder`, payload)
}

export async function batchCollectionAction(payload: BatchCollectionActionPayload): Promise<void> {
  await http.post('/collections/batch', payload)
}

export async function exportAlbum(albumId: number, format: string = 'markdown'): Promise<Blob> {
  const { data } = await http.get(`/collections/albums/${albumId}/export`, {
    params: { format },
    responseType: 'blob',
  })
  return data
}

export async function getPublicAlbum(shareToken: string): Promise<CollectionAlbum> {
  const { data } = await http.get<CollectionAlbum>(`/collections/public/${shareToken}`)
  return data
}

export async function getPublicAlbumItems(shareToken: string): Promise<CollectionItem[]> {
  const { data } = await http.get<CollectionItem[]>(`/collections/public/${shareToken}/items`)
  return data
}

export async function getFavoriteRanking(top: number = 10): Promise<PostFavoriteCount[]> {
  const { data } = await http.get<PostFavoriteCount[]>('/collections/ranking', { params: { top } })
  return data
}

export async function getReadHistory(page = 0, size = 20, categoryId?: number): Promise<PageResponse<ReadHistoryDetail>> {
  const params: any = { page, size }
  if (categoryId) params.categoryId = categoryId
  const { data } = await http.get<PageResponse<ReadHistoryDetail>>('/reader/me/history', { params })
  return data
}

export async function getFavoritesList(page = 0, size = 20): Promise<PageResponse<FavoriteDetail>> {
  const { data } = await http.get<PageResponse<FavoriteDetail>>('/reader/me/favorites', { params: { page, size } })
  return data
}

export async function getReadingProgress(postId: number): Promise<ReadingProgress | null> {
  try {
    const { data } = await http.get<ReadingProgress>(`/reader/me/reading-progress/${postId}`)
    return data.postId ? data : null
  } catch {
    return null
  }
}

export async function updateReadingProgress(postId: number, durationSeconds: number, scrollPosition: number): Promise<void> {
  await http.put(`/reader/me/reading-progress/${postId}`, { durationSeconds, scrollPosition })
}

export async function clearReadHistory(): Promise<void> {
  await http.delete('/reader/me/history')
}
