<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReaderPublicProfile, getReadHistory, getFavorites, getMyComments, getMySubscriptions } from '../api/reader'
import { getReadHistory as getReadHistoryDetail, getFavoritesList } from '../api/collections'
import type { ReaderPublicProfileDTO, ReadHistoryDetail, FavoriteDetail } from '../types'

const route = useRoute()
const profile = ref<ReaderPublicProfileDTO | null>(null)
const loading = ref(false)
const activeTab = ref('footprint')

const historyItems = ref<ReadHistoryDetail[]>([])
const favoriteItems = ref<FavoriteDetail[]>([])
const commentItems = ref<any[]>([])
const subscriptionItems = ref<any[]>([])
const pageTotal = ref(0)
const currentPage = ref(0)
const pageSize = 20

const userId = computed(() => Number(route.params.id))

const levelName = computed(() => {
  if (!profile.value) return ''
  const lv = profile.value.readerLevel
  if (lv >= 10) return '至尊读者'
  if (lv >= 8) return '资深读者'
  if (lv >= 5) return '活跃读者'
  if (lv >= 3) return '进阶读者'
  return '新手读者'
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getReaderPublicProfile(userId.value)
    await loadTabData()
  } catch {
    ElMessage.error('加载读者信息失败')
  } finally {
    loading.value = false
  }
}

async function loadTabData() {
  if (!profile.value) return
  try {
    if (activeTab.value === 'footprint' && profile.value.readCount > 0) {
      const res = await getReadHistoryDetail(currentPage.value, pageSize)
      historyItems.value = res.items
      pageTotal.value = res.total
    } else if (activeTab.value === 'favorites') {
      const res = await getFavoritesList(currentPage.value, pageSize)
      favoriteItems.value = res.items
      pageTotal.value = res.total
    } else if (activeTab.value === 'comments') {
      const res = await getMyComments(currentPage.value, pageSize)
      commentItems.value = res.items
      pageTotal.value = res.total
    } else if (activeTab.value === 'subscriptions') {
      const res = await getMySubscriptions(currentPage.value, pageSize)
      subscriptionItems.value = res.items
      pageTotal.value = res.total
    }
  } catch {
    // silently ignore tab load errors
  }
}

function handleTabChange() {
  currentPage.value = 0
  loadTabData()
}

function handlePageChange(page: number) {
  currentPage.value = page - 1
  loadTabData()
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="view-shell reader-shell" v-loading="loading">
    <div v-if="profile" class="reader-space-layout">
      <el-card class="panel-card space-header-card">
        <div class="space-header">
          <el-avatar :size="64" :src="profile.avatarUrl || undefined">
            {{ (profile.nickname || profile.username)[0] }}
          </el-avatar>
          <div class="space-header-info">
            <h1 class="space-name">{{ profile.nickname || profile.username }}</h1>
            <p class="space-username">@{{ profile.username }}</p>
            <div class="space-meta">
              <span class="level-badge-sm">Lv.{{ profile.readerLevel }} {{ levelName }}</span>
              <span v-if="profile.streakDays > 0" class="streak-badge">🔥 {{ profile.streakDays }}天</span>
            </div>
            <p v-if="profile.bio" class="space-bio">{{ profile.bio }}</p>
          </div>
          <div class="space-stats-row">
            <div class="stat-item">
              <span class="stat-num">{{ profile.readCount }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.favoriteCount }}</span>
              <span class="stat-label">珍藏</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.commentCount }}</span>
              <span class="stat-label">评论</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.subscriptionCount }}</span>
              <span class="stat-label">订阅</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="浏览足迹" name="footprint">
            <div v-if="historyItems.length === 0" class="empty-tip">暂无浏览记录</div>
            <div v-else class="item-list">
              <component
                :is="item.postDeleted ? 'span' : 'router-link'"
                v-for="item in historyItems"
                :key="item.id"
                :to="item.postDeleted ? undefined : `/posts/${item.postId}`"
                class="item-row"
                :class="{ 'item-deleted': item.postDeleted }"
              >
                <span class="item-title">{{ item.postDeleted ? '已删除的作品' : item.postTitle }}</span>
                <span class="item-meta">
                  <span v-if="item.durationSeconds > 0" class="item-duration">{{ Math.floor(item.durationSeconds / 60) }}分钟</span>
                  <span class="item-date">{{ item.readAt }}</span>
                </span>
              </component>
            </div>
          </el-tab-pane>

          <el-tab-pane label="珍藏作品" name="favorites">
            <div v-if="favoriteItems.length === 0" class="empty-tip">暂无珍藏</div>
            <div v-else class="item-list">
              <component
                :is="item.postDeleted ? 'span' : 'router-link'"
                v-for="item in favoriteItems"
                :key="item.id"
                :to="item.postDeleted ? undefined : `/posts/${item.postId}`"
                class="item-row"
                :class="{ 'item-deleted': item.postDeleted }"
              >
                <span class="item-title">{{ item.postDeleted ? '已删除的作品' : item.postTitle }}</span>
                <span class="item-date">{{ item.createdAt }}</span>
              </component>
            </div>
          </el-tab-pane>

          <el-tab-pane label="发表的见解" name="comments">
            <div v-if="commentItems.length === 0" class="empty-tip">暂无评论</div>
            <div v-else class="item-list">
              <div v-for="item in commentItems" :key="item.id" class="item-row comment-row">
                <p class="comment-content">{{ item.content }}</p>
                <div class="comment-meta">
                  <router-link :to="`/posts/${item.postId}`">文章 #{{ item.postId }}</router-link>
                  <span class="item-date">{{ item.createdAt }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="订阅的创作者" name="subscriptions">
            <div v-if="subscriptionItems.length === 0" class="empty-tip">暂无订阅</div>
            <div v-else class="item-list">
              <router-link
                v-for="item in subscriptionItems"
                :key="item.id"
                :to="`/authors/${item.authorId}`"
                class="item-row"
              >
                <span class="item-title">作者 #{{ item.authorId }}</span>
                <span class="item-date">{{ item.createdAt }}</span>
              </router-link>
            </div>
          </el-tab-pane>
        </el-tabs>

        <div v-if="pageTotal > pageSize" class="pagination-row">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="pageTotal"
            :page-size="pageSize"
            :current-page="currentPage + 1"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.reader-space-layout {
  display: grid;
  gap: var(--space-4);
}

.space-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.space-header-info {
  flex: 1;
  min-width: 200px;
}

.space-name {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-1);
}

.space-username {
  margin: 4px 0 0;
  color: var(--color-text-3);
  font-size: 13px;
}

.space-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.level-badge-sm {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
}

.streak-badge {
  font-size: 12px;
  font-weight: 600;
  color: #f97316;
}

.space-bio {
  margin: var(--space-2) 0 0;
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 1.6;
}

.space-stats-row {
  display: flex;
  gap: var(--space-5);
  align-items: center;
  padding-top: var(--space-2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-1);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-3);
}

.empty-tip {
  text-align: center;
  padding: var(--space-5);
  color: var(--color-text-3);
  font-size: 14px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--color-page-1);
  transition: background 0.15s;
}

.item-row:hover {
  background: var(--color-primary-soft);
}

.item-title {
  font-weight: 500;
  color: var(--color-text-1);
}

.item-date {
  font-size: 12px;
  color: var(--color-text-3);
}

.item-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.item-duration {
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  padding: 1px 6px;
  border-radius: 999px;
}

.item-deleted {
  opacity: 0.55;
  pointer-events: none;
}

.item-deleted .item-title {
  text-decoration: line-through;
  font-style: italic;
}

.comment-row {
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
}

.comment-content {
  margin: 0;
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 1.5;
}

.comment-meta {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  font-size: 12px;
  color: var(--color-text-3);
}

.comment-meta a {
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .space-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .space-stats-row {
    justify-content: center;
  }
}
</style>
