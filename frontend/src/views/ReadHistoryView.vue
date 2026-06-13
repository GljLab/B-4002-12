<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getReadHistory, clearReadHistory } from '../api/collections'
import { getPublicCategoriesFlat } from '../api/categories'
import type { ReadHistoryDetail, Category } from '../types'

const historyItems = ref<ReadHistoryDetail[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const pageTotal = ref(0)
const selectedCategoryId = ref<number | undefined>(undefined)

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m === 0) return `${s}秒`
  return `${m}分钟${s}秒`
}

function formatReadPosition(position: number): string {
  if (position > 0) return `阅读至第${position}px`
  return '刚开始阅读'
}

function formatDate(dateStr: string): string {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

async function loadCategories() {
  try {
    categories.value = await getPublicCategoriesFlat()
  } catch {
    // silently ignore
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const res = await getReadHistory(currentPage.value - 1, pageSize, selectedCategoryId.value)
    historyItems.value = res.items
    pageTotal.value = res.total
  } catch {
    ElMessage.error('加载阅读历史失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadHistory()
}

function handleCategoryChange() {
  currentPage.value = 1
  loadHistory()
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm('确定要清空所有阅读历史吗？此操作不可撤销。', '清空阅读历史', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearReadHistory()
    ElMessage.success('阅读历史已清空')
    currentPage.value = 1
    loadHistory()
  } catch {
    // user cancelled or error
  }
}

onMounted(() => {
  loadCategories()
  loadHistory()
})
</script>

<template>
  <div class="view-shell reader-shell" v-loading="loading">
    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <span class="panel-title">阅读历史</span>
          <div class="title-actions">
            <el-select
              v-model="selectedCategoryId"
              placeholder="全部分类"
              clearable
              style="width: 180px"
              @change="handleCategoryChange"
            >
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
            <el-button type="danger" plain @click="handleClearAll">清空历史</el-button>
          </div>
        </div>
      </template>

      <div v-if="historyItems.length === 0" class="empty-tip">暂无阅读历史</div>

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in historyItems"
          :key="item.id"
          :timestamp="formatDate(item.readAt)"
          placement="top"
          color="var(--color-primary)"
        >
          <div class="history-entry">
            <div class="entry-title-row">
              <router-link
                v-if="!item.postDeleted"
                :to="`/posts/${item.postId}`"
                class="entry-title"
              >{{ item.postTitle }}</router-link>
              <span v-else class="entry-title deleted">已删除的作品</span>
            </div>
            <p class="entry-excerpt">{{ item.postExcerpt }}</p>
            <div class="entry-meta">
              <span class="meta-author">{{ item.postAuthorName }}</span>
              <span class="meta-divider">·</span>
              <span class="meta-duration">{{ formatDuration(item.durationSeconds) }}</span>
              <span class="meta-divider">·</span>
              <span class="meta-position">{{ formatReadPosition(item.scrollPosition) }}</span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div v-if="pageTotal > pageSize" class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="pageTotal"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.panel-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.title-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.empty-tip {
  text-align: center;
  padding: var(--space-5);
  color: var(--color-text-3);
  font-size: 14px;
}

.history-entry {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.entry-title-row {
  display: flex;
  align-items: center;
}

.entry-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
  transition: opacity 0.15s;
}

.entry-title:hover {
  opacity: 0.8;
}

.entry-title.deleted {
  color: var(--color-text-3);
  font-style: italic;
  font-weight: 400;
}

.entry-excerpt {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-2);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.entry-meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: 12px;
  color: var(--color-text-3);
}

.meta-author {
  color: var(--color-primary);
  font-weight: 500;
}

.meta-divider {
  color: var(--color-text-4);
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: var(--space-4);
}
</style>
