<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElPopover } from 'element-plus'
import {
  Bell,
  ChatDotRound,
  CaretTop,
  User,
  Warning,
  Check,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useAuthStore } from '../stores/auth'
import {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
} from '../api/notifications'
import type { NotificationDTO, UnreadNotificationCountDTO } from '../types'

const router = useRouter()
const authStore = useAuthStore()

const unreadCount = ref<UnreadNotificationCountDTO | null>(null)
const notifications = ref<NotificationDTO[]>([])
const popoverVisible = ref(false)
const loading = ref(false)
const markingAllRead = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

const showBadge = computed(() => unreadCount.value && unreadCount.value.total > 0)
const badgeCount = computed(() => {
  if (!unreadCount.value) return 0
  return unreadCount.value.total > 99 ? '99+' : unreadCount.value.total
})

function getNotificationIcon(type: string) {
  switch (type) {
    case 'REPLY':
      return ChatDotRound
    case 'UPVOTE':
      return CaretTop
    case 'MENTION':
      return User
    case 'REPORT_RESULT':
      return Warning
    default:
      return Bell
  }
}

function getNotificationTypeText(type: string) {
  switch (type) {
    case 'REPLY':
      return '回复'
    case 'UPVOTE':
      return '点赞'
    case 'MENTION':
      return '提及'
    case 'REPORT_RESULT':
      return '举报结果'
    default:
      return '通知'
  }
}

function formatTime(time: string) {
  const now = dayjs()
  const target = dayjs(time)
  const diffMinutes = now.diff(target, 'minute')

  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`

  const diffHours = now.diff(target, 'hour')
  if (diffHours < 24) return `${diffHours}小时前`

  const diffDays = now.diff(target, 'day')
  if (diffDays < 7) return `${diffDays}天前`

  return target.format('MM-DD HH:mm')
}

async function fetchUnreadCount() {
  if (!authStore.isLoggedIn) return
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    // ignore
  }
}

async function fetchNotifications() {
  if (!authStore.isLoggedIn) return

  loading.value = true
  try {
    const result = await getNotifications(true, 0, 10)
    notifications.value = result.items || []
  } catch {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

async function handleNotificationClick(notification: NotificationDTO) {
  if (!notification.read) {
    try {
      await markAsRead(notification.id)
      notification.read = true
      if (unreadCount.value) {
        unreadCount.value.total--
        if (notification.type === 'REPLY') unreadCount.value.replyCount--
        if (notification.type === 'UPVOTE') unreadCount.value.upvoteCount--
        if (notification.type === 'MENTION') unreadCount.value.mentionCount--
        if (notification.type === 'REPORT_RESULT') unreadCount.value.reportResultCount--
      }
    } catch {
      // ignore
    }
  }

  popoverVisible.value = false

  if (notification.relatedPostId) {
    const route = router.resolve({
      path: `/posts/${notification.relatedPostId}`,
      hash: notification.relatedCommentId ? `#comment-${notification.relatedCommentId}` : '',
    })
    await router.push(route)
  }
}

async function handleMarkAllRead() {
  if (markingAllRead.value) return

  markingAllRead.value = true
  try {
    const result = await markAllAsRead()
    notifications.value.forEach((n) => (n.read = true))
    if (unreadCount.value) {
      unreadCount.value.total = 0
      unreadCount.value.replyCount = 0
      unreadCount.value.mentionCount = 0
      unreadCount.value.upvoteCount = 0
      unreadCount.value.reportResultCount = 0
    }
    ElMessage.success(result.message)
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAllRead.value = false
  }
}

function handlePopoverShow() {
  fetchNotifications()
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    fetchUnreadCount()
  }, 30000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(
  () => authStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn) {
      fetchUnreadCount()
      startPolling()
    } else {
      stopPolling()
      unreadCount.value = null
      notifications.value = []
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (authStore.isLoggedIn) {
    fetchUnreadCount()
    startPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    :width="380"
    trigger="click"
    popper-class="notification-popover"
    @show="handlePopoverShow"
  >
    <template #reference>
      <button class="notification-bell-btn" @click.stop>
        <el-icon class="bell-icon"><Bell /></el-icon>
        <span v-if="showBadge" class="notification-badge">
          {{ badgeCount }}
        </span>
      </button>
    </template>

    <div class="notification-panel">
      <div class="panel-header">
        <span class="panel-title">通知消息</span>
        <el-button
          v-if="unreadCount && unreadCount.total > 0"
          type="primary"
          text
          size="small"
          :loading="markingAllRead"
          @click="handleMarkAllRead"
        >
          <el-icon><Check /></el-icon>
          全部已读
        </el-button>
      </div>

      <div class="panel-body" v-loading="loading">
        <template v-if="notifications.length > 0">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ 'is-unread': !notification.read }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon-wrapper">
              <el-icon
                class="notification-icon"
                :class="`type-${notification.type.toLowerCase()}`"
              >
                <component :is="getNotificationIcon(notification.type)" />
              </el-icon>
              <span v-if="!notification.read" class="unread-dot"></span>
            </div>

            <div class="notification-content">
              <div class="notification-title">
                <span class="type-tag">{{ getNotificationTypeText(notification.type) }}</span>
                <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
              </div>
              <div class="notification-preview">
                <span v-if="notification.relatedNickname" class="related-user">
                  {{ notification.relatedNickname }}
                </span>
                <span class="notification-text">{{ notification.content }}</span>
              </div>
              <div v-if="notification.relatedPostTitle" class="notification-post">
                《{{ notification.relatedPostTitle }}》
              </div>
            </div>
          </div>
        </template>
        <el-empty
          v-else-if="!loading"
          description="暂无新通知"
          :image-size="80"
          class="empty-notifications"
        />
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.notification-bell-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--color-text-2);
  cursor: pointer;
  border-radius: 50%;
  transition: all var(--motion-fast) var(--ease-standard);
}

.notification-bell-btn:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.bell-icon {
  font-size: 20px;
}

.notification-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  box-shadow: 0 0 0 2px var(--color-surface);
}

.notification-panel {
  max-height: 480px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  margin: 0 -16px;
  margin-top: -12px;
  margin-bottom: 8px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-1);
}

.panel-body {
  max-height: 400px;
  overflow-y: auto;
  margin: 0 -16px;
  padding: 0 4px;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--motion-fast) var(--ease-standard);
  margin-bottom: 4px;
}

.notification-item:hover {
  background: var(--color-primary-soft);
}

.notification-item.is-unread {
  background: rgba(45, 109, 246, 0.04);
}

.notification-icon-wrapper {
  position: relative;
  flex-shrink: 0;
}

.notification-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 18px;
}

.notification-icon.type-reply {
  background: rgba(67, 97, 238, 0.1);
  color: #4361ee;
}

.notification-icon.type-upvote {
  background: rgba(76, 217, 100, 0.1);
  color: #4cd964;
}

.notification-icon.type-mention {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.notification-icon.type-report_result {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

.unread-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background: var(--el-color-danger);
  border-radius: 50%;
  border: 2px solid var(--color-surface);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  border-radius: 4px;
}

.notification-time {
  font-size: 12px;
  color: var(--color-text-3);
  margin-left: auto;
}

.notification-preview {
  font-size: 13px;
  color: var(--color-text-2);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.related-user {
  color: var(--color-primary);
  font-weight: 600;
  margin-right: 4px;
}

.notification-post {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-notifications {
  padding: 24px 0;
}

.panel-body::-webkit-scrollbar {
  width: 6px;
}

.panel-body::-webkit-scrollbar-track {
  background: transparent;
}

.panel-body::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

@media (max-width: 768px) {
  .notification-bell-btn {
    width: 32px;
    height: 32px;
  }

  .bell-icon {
    font-size: 18px;
  }

  :deep(.notification-popover) {
    width: calc(100vw - 32px) !important;
    max-width: 360px;
  }
}
</style>
