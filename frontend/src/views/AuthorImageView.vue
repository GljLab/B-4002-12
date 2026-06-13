<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getMyImages, renameImage, deleteImage, getImageReferences } from '../api/images'
import type { ImageDTO, PostSummary } from '../types'

const images = ref<ImageDTO[]>([])
const loading = ref(false)
const renameDialogVisible = ref(false)
const renameLoading = ref(false)
const renameId = ref<number | null>(null)
const renameValue = ref('')
const referencesDialogVisible = ref(false)
const referencesLoading = ref(false)
const referencesPosts = ref<PostSummary[]>([])

async function loadImages() {
  loading.value = true
  try {
    images.value = await getMyImages()
  } catch {
    ElMessage.error('获取图片列表失败')
  } finally {
    loading.value = false
  }
}

function openRenameDialog(image: ImageDTO) {
  renameId.value = image.id
  renameValue.value = image.originalName || ''
  renameDialogVisible.value = true
}

async function handleRename() {
  if (!renameValue.value.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  renameLoading.value = true
  try {
    await renameImage(renameId.value!, renameValue.value.trim())
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    await loadImages()
  } catch {
    ElMessage.error('重命名失败')
  } finally {
    renameLoading.value = false
  }
}

async function handleDelete(image: ImageDTO) {
  try {
    await ElMessageBox.confirm('确定删除该图片吗？如果图片被文章引用，将无法删除。', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteImage(image.id)
    ElMessage.success('删除成功')
    await loadImages()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

async function showReferences(image: ImageDTO) {
  referencesDialogVisible.value = true
  referencesLoading.value = true
  try {
    referencesPosts.value = await getImageReferences(image.id)
  } catch {
    referencesPosts.value = []
  } finally {
    referencesLoading.value = false
  }
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(loadImages)
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <h1>图片管理</h1>
      <p class="author-subtitle">管理你上传的所有图片资源。</p>
    </header>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">图片列表 ({{ images.length }})</strong>
        </div>
      </template>

      <div v-loading="loading" class="image-grid" v-if="images.length > 0">
        <div v-for="image in images" :key="image.id" class="image-card">
          <div class="image-thumb">
            <img :src="image.url" :alt="image.originalName || image.filename" loading="lazy" />
          </div>
          <div class="image-info">
            <p class="image-name">{{ image.originalName || image.filename }}</p>
            <p class="image-meta">{{ formatFileSize(image.fileSize) }} · {{ dayjs(image.createdAt).format('YYYY-MM-DD') }}</p>
          </div>
          <div class="image-actions">
            <el-button size="small" link type="primary" @click="showReferences(image)">引用</el-button>
            <el-button size="small" link type="info" @click="openRenameDialog(image)">重命名</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(image)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-else-if="!loading" description="暂无上传图片" />
    </el-card>

    <el-dialog v-model="renameDialogVisible" title="重命名图片" width="400px">
      <el-input v-model="renameValue" placeholder="请输入新名称" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameLoading" @click="handleRename">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="referencesDialogVisible" title="图片引用" width="500px">
      <div v-loading="referencesLoading">
        <p v-if="referencesPosts.length === 0">该图片未被任何文章引用</p>
        <el-table v-else :data="referencesPosts" stripe size="small">
          <el-table-column prop="title" label="文章标题" min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
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

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-4);
}

.image-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.image-card:hover {
  box-shadow: var(--shadow-md);
}

.image-thumb {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  overflow: hidden;
}

.image-thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-info {
  padding: var(--space-2) var(--space-3);
}

.image-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--color-text-3);
}

.image-actions {
  display: flex;
  justify-content: flex-end;
  padding: 0 var(--space-2) var(--space-2);
  gap: var(--space-1);
}
</style>
