<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminStats, getAuthorRanking } from '../api/stats'
import type { AdminStatsDTO, AuthorRankItem } from '../types'

const router = useRouter()
const stats = ref<AdminStatsDTO | null>(null)
const ranking = ref<AuthorRankItem[]>([])
const loading = ref(false)
const rankLoading = ref(false)
const sortBy = ref<'posts' | 'views'>('posts')

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getAdminStats()
  } catch {
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

async function loadRanking() {
  rankLoading.value = true
  try {
    ranking.value = await getAuthorRanking(sortBy.value)
  } catch {
    ElMessage.error('获取排行榜失败')
  } finally {
    rankLoading.value = false
  }
}

function handleSortChange(mode: 'posts' | 'views') {
  sortBy.value = mode
  loadRanking()
}

function viewAuthorPosts(authorId: number) {
  router.push({ name: 'author-profile', params: { id: authorId } })
}

onMounted(() => {
  loadStats()
  loadRanking()
})
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>数据统计</h1>
      <p class="admin-subtitle">全局数据概览与作者排行榜。</p>
    </header>

    <div v-loading="loading" class="stat-cards">
      <el-card class="stat-card stat-card-posts" shadow="hover">
        <el-statistic title="总文章数" :value="stats?.totalPosts ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-pending" shadow="hover">
        <el-statistic title="待审核数" :value="stats?.pendingPosts ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-authors" shadow="hover">
        <el-statistic title="总作者数" :value="stats?.totalAuthors ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-monthly" shadow="hover">
        <el-statistic title="本月新增" :value="stats?.monthlyNewPosts ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-views" shadow="hover">
        <el-statistic title="总阅读量" :value="stats?.totalViews ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-scheduled" shadow="hover">
        <el-statistic title="预约上线" :value="stats?.scheduledPosts ?? 0" />
      </el-card>
      <el-card class="stat-card stat-card-revisions" shadow="hover">
        <el-statistic title="修订待审核" :value="stats?.pendingRevisions ?? 0" />
      </el-card>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">作者排行榜</strong>
          <el-radio-group :model-value="sortBy" size="small" @change="handleSortChange">
            <el-radio-button value="posts">按文章数</el-radio-button>
            <el-radio-button value="views">按阅读量</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table class="admin-table" v-loading="rankLoading" :data="ranking" stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="作者" min-width="200">
          <template #default="{ row }">
            <div class="author-cell">
              <el-avatar v-if="row.avatarUrl" :src="row.avatarUrl" :size="32" />
              <el-avatar v-else :size="32">{{ row.nickname.charAt(0) }}</el-avatar>
              <span class="author-name">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="postCount" label="文章数" width="120" align="center" />
        <el-table-column prop="totalViews" label="总阅读量" width="140" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button class="action-btn" type="primary" link @click="viewAuthorPosts(row.id)">查看文章</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  border-radius: var(--radius-md);
  border: 1px solid rgba(180, 201, 228, 0.72);
}

.stat-card-posts { border-left: 4px solid #2d6df6; }
.stat-card-pending { border-left: 4px solid #eab308; }
.stat-card-authors { border-left: 4px solid #16a34a; }
.stat-card-monthly { border-left: 4px solid #f97316; }
.stat-card-views { border-left: 4px solid #8b5cf6; }
.stat-card-scheduled { border-left: 4px solid #06b6d4; }
.stat-card-revisions { border-left: 4px solid #ec4899; }

.stat-card :deep(.el-statistic__head) {
  font-size: 13px;
  color: var(--color-text-3);
}

.stat-card :deep(.el-statistic__number) {
  font-size: 28px;
  font-weight: 700;
}

.stat-card-posts :deep(.el-statistic__number) { color: #2d6df6; }
.stat-card-pending :deep(.el-statistic__number) { color: #eab308; }
.stat-card-authors :deep(.el-statistic__number) { color: #16a34a; }
.stat-card-monthly :deep(.el-statistic__number) { color: #f97316; }
.stat-card-views :deep(.el-statistic__number) { color: #8b5cf6; }
.stat-card-scheduled :deep(.el-statistic__number) { color: #06b6d4; }
.stat-card-revisions :deep(.el-statistic__number) { color: #ec4899; }

.author-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.author-name {
  font-weight: 550;
  color: var(--color-text-1);
}
</style>
