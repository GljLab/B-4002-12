<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { createAuthorPost, submitForReview } from '../api/posts'
import { getPublicCategoriesFlat } from '../api/categories'
import type { Category } from '../types'
import PostEditor from '../components/PostEditor.vue'

const router = useRouter()
const saving = ref(false)
const submitting = ref(false)
const lastSavedAt = ref<string | null>(null)
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  title: '',
  content: '',
  categoryId: null as number | null,
  keywords: [] as string[],
  scheduledAt: null as string | null,
})

const categories = ref<Category[]>([])
const categoriesLoading = ref(false)

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await getPublicCategoriesFlat()
  } catch {
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

function isCategoryDisabled(category: Category): boolean {
  return !category.enabled
}

function buildCreatePayload() {
  return {
    title: form.title.trim(),
    content: form.content.trim(),
    categoryId: form.categoryId,
    keywords: form.keywords,
    scheduledAt: form.scheduledAt ? dayjs(form.scheduledAt).format('YYYY-MM-DDTHH:mm:ss') : null,
  }
}

async function saveDraft() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    const detail = await createAuthorPost(buildCreatePayload())
    lastSavedAt.value = dayjs().format('HH:mm:ss')
    ElMessage.success('草稿已保存')
    router.push(`/author/posts/${detail.id}/edit`)
  } catch {
    ElMessage.error('保存草稿失败')
  } finally {
    saving.value = false
  }
}

async function submitReview() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  submitting.value = true
  try {
    const detail = await createAuthorPost(buildCreatePayload())
    await submitForReview(detail.id)
    ElMessage.success('已提交审核')
    router.push('/author/posts')
  } catch {
    ElMessage.error('提交审核失败')
  } finally {
    submitting.value = false
  }
}

function handleSubmit() {
  submitReview()
}

onMounted(() => {
  loadCategories()
  autoSaveTimer = setInterval(() => {
    if (form.title.trim() || form.content.trim()) {
      lastSavedAt.value = dayjs().format('HH:mm:ss')
    }
  }, 30000)
})

onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <h1>新建文章</h1>
      <p class="author-subtitle">撰写并发布你的文章。</p>
      <p v-if="lastSavedAt" class="auto-save-hint">
        上次自动保存: {{ lastSavedAt }}
      </p>
    </header>

    <el-card class="panel-card panel-editor">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">编辑器</strong>
          <div class="panel-meta">
            支持 Markdown 语法，实时预览
          </div>
        </div>
      </template>

      <PostEditor
        v-model:title="form.title"
        v-model:content="form.content"
        v-model:category-id="form.categoryId"
        v-model:keywords="form.keywords"
        :loading="submitting || saving"
        :textarea-rows="16"
        submit-text="提交审核"
        @submit="handleSubmit"
      />

      <div class="editor-extra-actions">
        <el-date-picker
          v-model="form.scheduledAt"
          type="datetime"
          placeholder="选择预约上线时间（可选）"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DDTHH:mm"
          :disabled-date="(date: Date) => date.getTime() < Date.now() - 86400000"
          style="width: 240px"
          clearable
        />
        <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
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

.auto-save-hint {
  margin-top: var(--space-2);
  font-size: 12px;
  color: var(--color-text-3);
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

.panel-meta {
  font-size: 13px;
  color: var(--color-text-3);
}

.editor-extra-actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  align-items: center;
  padding-top: var(--space-4);
  margin-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

@media (max-width: 768px) {
  .editor-extra-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .editor-extra-actions .el-date-picker {
    width: 100% !important;
  }
}
</style>
