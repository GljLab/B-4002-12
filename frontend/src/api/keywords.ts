import { http } from './http'
import type { Keyword, KeywordCloud } from '../types'

export async function getKeywordCloud(): Promise<KeywordCloud[]> {
  const { data } = await http.get<KeywordCloud[]>('/keywords/cloud')
  return data
}

export async function searchKeywords(name: string): Promise<Keyword[]> {
  const { data } = await http.get<Keyword[]>('/keywords/search', { params: { name } })
  return data
}

export async function getAdminKeywords(sortBy = 'heat'): Promise<Keyword[]> {
  const { data } = await http.get<Keyword[]>('/admin/keywords', { params: { sortBy } })
  return data
}

export async function toggleKeywordArchive(id: number): Promise<void> {
  await http.post(`/admin/keywords/${id}/toggle-archive`)
}

export async function archiveStaleKeywords(): Promise<number> {
  const { data } = await http.post<number>('/admin/keywords/archive-stale')
  return data
}
