import { http } from './http'
import type {
  NotificationDTO,
  UnreadNotificationCountDTO,
  NotificationListResponse,
  MessageResponse,
} from '../types'

export async function getNotifications(
  onlyUnread?: boolean,
  page = 0,
  size = 10,
): Promise<NotificationListResponse> {
  const params: Record<string, unknown> = { page, size }
  if (onlyUnread != null) params.onlyUnread = onlyUnread
  const { data } = await http.get<NotificationListResponse>('/notifications', { params })
  return data
}

export async function getUnreadCount(): Promise<UnreadNotificationCountDTO> {
  const { data } = await http.get<UnreadNotificationCountDTO>('/notifications/unread-count')
  return data
}

export async function markAsRead(id: number): Promise<MessageResponse> {
  const { data } = await http.post<MessageResponse>(`/notifications/${id}/read`)
  return data
}

export async function markAllAsRead(): Promise<{ message: string; markedCount: number }> {
  const { data } = await http.post<{ message: string; markedCount: number }>(
    '/notifications/read-all',
  )
  return data
}
