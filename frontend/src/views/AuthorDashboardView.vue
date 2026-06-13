<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { getAuthorStats } from '../api/posts'
import { useAuthStore } from '../stores/auth'
import type { AuthorStatsDTO } from '../types'

const auth = useAuthStore()
const stats = ref<AuthorStatsDTO | null>(null)
const loading = ref(false)

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getAuthorStats()
  } catch {
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <h1>作者中心</h1>
      <p class="author-subtitle">欢迎回来，{{ auth.displayName }}</p>
    </header>

    <div v-loading="loading" class="stat-cards">
      <el-card class="stat-card stat-card-published" shadow="hover">
        <el-statistic title="总发布文章数" :value="stats?.totalPublished ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-draft" shadow="hover">
        <el-statistic title="草稿数" :value="stats?.totalDraft ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-views" shadow="hover">
        <el-statistic title="总阅读量" :value="stats?.totalViews ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-avg" shadow="hover">
        <el-statistic title="平均阅读量" :value="stats?.avgViews ?? 0" :precision="1" />
      </el-card>
    </div>

    <el-card class="panel-card" v-if="stats">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">最近文章</strong>
        </div>
      </template>
      <el-table class="admin-table" :data="stats.topPosts.slice(0, 5)" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="分类" min-width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.categoryPath" size="small">{{ scope.row.categoryPath }}</el-tag>
            <span v-else class="meta">未分类</span>
          </template>
        </el-table-column>
        <el-table-column label="阅读量" width="100">
          <template #default="scope">
            {{ scope.row.viewCount }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="scope">
            {{ dayjs(scope.row.createdAt).format('YYYY-MM-DD HH:mm') }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.author-header h1 {
  margin: 0;
  font-size: clamp(28px, 3.4vw, 34px);
  line-height: 1.2;
}

.author-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-3);
  font-size: 15px;
  line-height: 1.65;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  border-radius: var(--radius-md);
  border: 1px solid rgba(180, 201, 228, 0.72);
}

.stat-card-published {
  border-left: 4px solid #16a34a;
}

.stat-card-draft {
  border-left: 4px solid #eab308;
}

.stat-card-views {
  border-left: 4px solid #2d6df6;
}

.stat-card-avg {
  border-left: 4px solid #8b5cf6;
}

.stat-card :deep(.el-statistic__head) {
  font-size: 13px;
  color: var(--color-text-3);
}

.stat-card :deep(.el-statistic__number) {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-1);
}

.stat-card-published :deep(.el-statistic__number) {
  color: #16a34a;
}

.stat-card-draft :deep(.el-statistic__number) {
  color: #eab308;
}

.stat-card-views :deep(.el-statistic__number) {
  color: #2d6df6;
}

.stat-card-avg :deep(.el-statistic__number) {
  color: #8b5cf6;
}

.panel-card {
  width: 100%;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--color-text-1);
}

.meta {
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
