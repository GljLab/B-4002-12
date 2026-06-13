import { http } from './http'
import type { ImageDTO } from '../types'

export async function uploadImage(file: File): Promise<ImageDTO> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImageDTO>('/upload/image', formData)
  return data
}
