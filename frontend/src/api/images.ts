import { http } from './http'
import type { ImageDTO, PostSummary } from '../types'

export async function getMyImages(): Promise<ImageDTO[]> {
  const { data } = await http.get<ImageDTO[]>('/author/images')
  return data
}

export async function getImage(id: number): Promise<ImageDTO> {
  const { data } = await http.get<ImageDTO>(`/author/images/${id}`)
  return data
}

export async function renameImage(id: number, originalName: string): Promise<ImageDTO> {
  const { data } = await http.put<ImageDTO>(`/author/images/${id}/rename`, { originalName })
  return data
}

export async function deleteImage(id: number): Promise<void> {
  await http.delete(`/author/images/${id}`)
}

export async function getImageReferences(id: number): Promise<PostSummary[]> {
  const { data } = await http.get<PostSummary[]>(`/author/images/${id}/references`)
  return data
}
