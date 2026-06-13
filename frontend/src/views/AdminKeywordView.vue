<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { getAdminKeywords, toggleKeywordArchive, archiveStaleKeywords } from '../api/keywords'
import type { Keyword } from '../types'

const keywords = ref<Keyword[]>([])
const loading = ref(false)
const togglingId = ref<number | null>(null)
const archiving = ref(false)
const sortBy = ref<'heat' | 'time'>('heat')

async function loadKeywords() {
  loading.value = true
  try {
    keywords.value = await getAdminKeywords(sortBy.value)
  } catch {
    ElMessage.error('获取关键词失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleArchive(keyword: Keyword) {
  togglingId.value = keyword.id
  try {
    await toggleKeywordArchive(keyword.id)
    ElMessage.success(keyword.archived ? '已恢复关键词' : '已归档关键词')
    await loadKeywords()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    togglingId.value = null
  }
}

async function handleArchiveStale() {
  archiving.value = true
  try {
    const count = await archiveStaleKeywords()
    ElMessage.success(`已归档 ${count} 个长期未使用关键词`)
    await loadKeywords()
  } catch {
    ElMessage.error('归档失败')
  } finally {
    archiving.value = false
  }
}

function handleSortChange(mode: 'heat' | 'time') {
  sortBy.value = mode
  loadKeywords()
}

onMounted(loadKeywords)
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>关键词统计</h1>
      <p class="admin-subtitle">查看和管理所有关键词的使用情况与归档状态。</p>
    </header>

    <section class="admin-list-section">
      <el-card class="panel-card panel-table">
        <template #header>
          <div class="panel-title-wrap">
            <strong class="panel-title">关键词列表</strong>
            <div class="panel-actions">
              <el-radio-group :model-value="sortBy" size="small" @change="handleSortChange">
                <el-radio-button value="heat">按热度</el-radio-button>
                <el-radio-button value="time">按时间</el-radio-button>
              </el-radio-group>
              <el-button
                type="warning"
                size="small"
                :loading="archiving"
                @click="handleArchiveStale"
              >
                归档长期未使用关键词
              </el-button>
            </div>
          </div>
        </template>

        <el-table class="admin-table" v-loading="loading" :data="keywords" stripe>
          <el-table-column prop="name" label="关键词" min-width="140" />
          <el-table-column prop="usageCount" label="使用次数" width="120" />
          <el-table-column label="最后使用" min-width="180">
            <template #default="scope">
              {{ scope.row.lastUsedAt ? dayjs(scope.row.lastUsedAt).format('YYYY-MM-DD HH:mm:ss') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="归档" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.archived ? 'info' : 'success'" size="small">
                {{ scope.row.archived ? '已归档' : '活跃' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                class="action-btn"
                :type="scope.row.archived ? 'success' : 'warning'"
                link
                :loading="togglingId === scope.row.id"
                @click="handleToggleArchive(scope.row)"
              >
                {{ scope.row.archived ? '恢复' : '归档' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
