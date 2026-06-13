import { http } from './http'
import type { Category, CreateCategoryPayload, UpdateCategoryPayload } from '../types'

export async function getPublicCategories(): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/categories')
  return data
}

export async function getPublicCategoriesFlat(): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/categories/flat')
  return data
}

export async function getPublicCategory(id: number): Promise<Category> {
  const { data } = await http.get<Category>(`/categories/${id}`)
  return data
}

export async function getAdminCategories(): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/admin/categories')
  return data
}

export async function getAdminCategoriesFlat(): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/admin/categories/flat')
  return data
}

export async function createCategory(payload: CreateCategoryPayload): Promise<Category> {
  const { data } = await http.post<Category>('/admin/categories', payload)
  return data
}

export async function updateCategory(id: number, payload: UpdateCategoryPayload): Promise<Category> {
  const { data } = await http.put<Category>(`/admin/categories/${id}`, payload)
  return data
}

export async function deleteCategory(id: number): Promise<void> {
  await http.delete(`/admin/categories/${id}`)
}

export async function toggleCategoryEnabled(id: number): Promise<Category> {
  const { data } = await http.put<Category>(`/admin/categories/${id}/toggle-enabled`)
  return data
}
