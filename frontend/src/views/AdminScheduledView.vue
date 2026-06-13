<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getScheduledTasks, forcePublishScheduled, adminCancelSchedule } from '../api/posts'
import type { ScheduledTaskDTO, PageResponse } from '../types'

const tasks = ref<ScheduledTaskDTO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

async function loadTasks() {
  loading.value = true
  try {
    const res: PageResponse<ScheduledTaskDTO> = await getScheduledTasks(page.value, size.value)
    tasks.value = res.items
    total.value = res.total
  } catch {
    ElMessage.error('获取预约上线任务失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadTasks()
}

async function handleForcePublish(task: ScheduledTaskDTO) {
  try {
    await ElMessageBox.confirm('确定立即发布该文章吗？', '强制发布', {
      type: 'warning',
      confirmButtonText: '确认发布',
      cancelButtonText: '取消',
    })
    await forcePublishScheduled(task.postId)
    ElMessage.success('文章已强制发布')
    await loadTasks()
  } catch {
    // cancelled or failed
  }
}

async function handleCancelSchedule(task: ScheduledTaskDTO) {
  try {
    await ElMessageBox.confirm('确定取消该文章的预约上线吗？', '取消预约', {
      type: 'warning',
      confirmButtonText: '取消预约',
      cancelButtonText: '返回',
    })
    await adminCancelSchedule(task.postId)
    ElMessage.success('预约上线已取消')
    await loadTasks()
  } catch {
    // cancelled or failed
  }
}

onMounted(loadTasks)
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>预约上线管理</h1>
      <p class="admin-subtitle">查看和管理所有待执行的预约上线任务。</p>
    </header>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">预约上线任务列表</strong>
        </div>
      </template>

      <el-table class="admin-table" v-loading="loading" :data="tasks" stripe>
        <el-table-column prop="title" label="文章标题" min-width="200" />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column label="预约上线时间" width="180">
          <template #default="{ row }">
            {{ dayjs(row.scheduledAt).format('YYYY-MM-DD HH:mm') }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isRevision" size="small" type="warning">修订版</el-tag>
            <el-tag v-else size="small" type="info">新文章</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" link @click="handleForcePublish(row)">立即发布</el-button>
            <el-button type="danger" size="small" link @click="handleCancelSchedule(row)">取消预约</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > size" class="pagination-row" style="margin-top: 16px">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.admin-header h1 {
  margin: 0;
  font-size: clamp(28px, 3.4vw, 34px);
  line-height: 1.2;
}

.admin-subtitle {
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

.pagination-row {
  display: flex;
  justify-content: center;
  padding: 6px;
  margin-top: var(--space-3);
}
</style>
