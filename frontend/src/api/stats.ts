import { http } from './http'
import type { AdminStatsDTO, AuthorRankItem } from '../types'

export async function getAdminStats(): Promise<AdminStatsDTO> {
  const { data } = await http.get<AdminStatsDTO>('/admin/stats')
  return data
}

export async function getAuthorRanking(sortBy: string = 'posts'): Promise<AuthorRankItem[]> {
  const { data } = await http.get<AuthorRankItem[]>('/admin/stats/ranking', { params: { sortBy } })
  return data
}
