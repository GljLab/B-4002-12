<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthorPosts, submitForReview, deleteAuthorPost, cancelSchedule, discardRevision } from '../api/posts'
import type { PostSummary, PageResponse } from '../types'

const router = useRouter()
const posts = ref<PostSummary[]>([])
const loading = ref(false)
const activeStatus = ref('')
const searchTitle = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap: Record<string, string> = {
  DRAFT: 'info',
  PENDING: 'warning',
  PUBLISHED: 'success',
  REJECTED: 'danger',
  SCHEDULED: '',
}

const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已拒绝',
  SCHEDULED: '待上线',
}

async function loadPosts() {
  loading.value = true
  try {
    const params: { status?: string; page: number; size: number } = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (activeStatus.value) params.status = activeStatus.value
    const res: PageResponse<PostSummary> = await getAuthorPosts(params)
    posts.value = res.items
    total.value = res.total
  } catch {
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  currentPage.value = 1
  loadPosts()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadPosts()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadPosts()
}

function handleEdit(id: number) {
  router.push(`/author/posts/${id}/edit`)
}

async function handleSubmitReview(row: PostSummary) {
  try {
    await ElMessageBox.confirm('确定提交审核吗？提交后将无法修改。', '提交审核', {
      type: 'warning',
      confirmButtonText: '提交',
      cancelButtonText: '取消',
    })
    await submitForReview(row.id)
    ElMessage.success('已提交审核')
    await loadPosts()
  } catch {
    // cancelled or failed
  }
}

async function handleDelete(row: PostSummary) {
  try {
    await ElMessageBox.confirm('确定删除这篇文章吗？此操作不可恢复。', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAuthorPost(row.id)
    ElMessage.success('删除成功')
    await loadPosts()
  } catch {
    // cancelled or failed
  }
}

async function handleCancelSchedule(row: PostSummary) {
  try {
    await ElMessageBox.confirm('确定取消预约上线吗？', '取消预约', {
      type: 'warning',
      confirmButtonText: '取消预约',
      cancelButtonText: '返回',
    })
    await cancelSchedule(row.id)
    ElMessage.success('预约上线已取消')
    await loadPosts()
  } catch {
    // cancelled or failed
  }
}

async function handleDiscardRevision(row: PostSummary) {
  try {
    await ElMessageBox.confirm('确定丢弃修订版本吗？此操作不可恢复。', '丢弃修订', {
      type: 'warning',
      confirmButtonText: '丢弃',
      cancelButtonText: '取消',
    })
    await discardRevision(row.id)
    ElMessage.success('修订版本已丢弃')
    await loadPosts()
  } catch {
    // cancelled or failed
  }
}

function canEdit(status: string, isRevision: boolean) {
  if (isRevision) return status === 'DRAFT' || status === 'REJECTED'
  return status === 'DRAFT' || status === 'REJECTED' || status === 'PUBLISHED'
}

function canSubmit(status: string) {
  return status === 'DRAFT' || status === 'REJECTED'
}

function canDelete(status: string) {
  return status === 'DRAFT' || status === 'REJECTED'
}

watch(searchTitle, () => {
  currentPage.value = 1
  loadPosts()
})

onMounted(loadPosts)
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <h1>我的文章</h1>
      <p class="author-subtitle">管理你发布的所有文章。</p>
    </header>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">文章列表</strong>
          <el-button type="primary" @click="router.push('/author/posts/create')">新建文章</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-radio-group v-model="activeStatus" @change="handleStatusChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DRAFT">草稿</el-radio-button>
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="PUBLISHED">已发布</el-radio-button>
          <el-radio-button value="REJECTED">已拒绝</el-radio-button>
          <el-radio-button value="SCHEDULED">待上线</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="searchTitle"
          placeholder="搜索标题"
          clearable
          style="max-width: 240px"
        />
      </div>

      <el-table class="admin-table" v-loading="loading" :data="posts" stripe>
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="scope">
            {{ scope.row.title }}
            <el-tag v-if="scope.row.revision" size="small" type="warning" style="margin-left: 4px">修订</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="(statusMap[scope.row.status] as any)" size="small">
              {{ statusLabel[scope.row.status] || scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.categoryPath" size="small">{{ scope.row.categoryPath }}</el-tag>
            <span v-else class="meta">未分类</span>
          </template>
        </el-table-column>
        <el-table-column label="预约上线" width="160">
          <template #default="scope">
            <span v-if="scope.row.scheduledAt">{{ dayjs(scope.row.scheduledAt).format('YYYY-MM-DD HH:mm') }}</span>
            <span v-else class="meta">--</span>
          </template>
        </el-table-column>
        <el-table-column label="修订" width="80" align="center">
          <template #default="scope">
            <el-icon v-if="scope.row.hasRevision" color="#eab308"><i class="el-icon-edit" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="scope">
            {{ dayjs(scope.row.createdAt).format('YYYY-MM-DD HH:mm') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <el-button
              v-if="canEdit(scope.row.status, scope.row.revision)"
              class="action-btn"
              type="primary"
              link
              @click="handleEdit(scope.row.id)"
            >
              {{ scope.row.status === 'PUBLISHED' ? '修订' : '编辑' }}
            </el-button>
            <el-button
              v-if="canSubmit(scope.row.status)"
              class="action-btn"
              type="warning"
              link
              @click="handleSubmitReview(scope.row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="scope.row.status === 'REJECTED' || scope.row.status === 'PENDING'"
              class="action-btn"
              type="info"
              link
              @click="router.push(`/author/posts/${scope.row.id}/review-feedback`)"
            >
              审查反馈
            </el-button>
            <el-button
              v-if="scope.row.status === 'SCHEDULED'"
              class="action-btn"
              type="info"
              link
              @click="handleCancelSchedule(scope.row)"
            >
              取消预约
            </el-button>
            <el-button
              v-if="scope.row.hasRevision && scope.row.status === 'PUBLISHED'"
              class="action-btn"
              type="danger"
              link
              @click="handleDiscardRevision(scope.row)"
            >
              丢弃修订
            </el-button>
            <el-button
              v-if="canDelete(scope.row.status)"
              class="action-btn"
              type="danger"
              link
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
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

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.action-btn {
  font-weight: 580;
}

.meta {
  color: var(--color-text-3);
  font-size: 13px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding: 6px;
  margin-top: var(--space-3);
}
</style>
