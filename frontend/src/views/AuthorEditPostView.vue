<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getAuthorPostDetail, updateAuthorPost, submitForReview, getRevisionDetail } from '../api/posts'
import { getPublicCategoriesFlat } from '../api/categories'
import type { Category, PostDetail } from '../types'
import PostEditor from '../components/PostEditor.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const lastSavedAt = ref<string | null>(null)
const postData = ref<PostDetail | null>(null)
const effectivePostId = ref<number>(0)
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const postId = computed(() => Number(route.params.id))

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

async function loadPost() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) {
    ElMessage.error('文章参数错误')
    router.replace('/author/posts')
    return
  }

  loading.value = true
  try {
    const detail = await getAuthorPostDetail(postId.value)
    if (detail.hasRevision && !detail.revision) {
      try {
        const revisionDetail = await getRevisionDetail(postId.value)
        postData.value = revisionDetail
        effectivePostId.value = revisionDetail.id
        form.title = revisionDetail.title
        form.content = revisionDetail.content
        form.categoryId = revisionDetail.categoryId
        form.keywords = revisionDetail.keywords.map(k => k.name)
        if (revisionDetail.scheduledAt) {
          form.scheduledAt = dayjs(revisionDetail.scheduledAt).format('YYYY-MM-DDTHH:mm')
        }
      } catch {
        postData.value = detail
        effectivePostId.value = detail.id
        form.title = detail.title
        form.content = detail.content
        form.categoryId = detail.categoryId
        form.keywords = detail.keywords.map(k => k.name)
        if (detail.scheduledAt) {
          form.scheduledAt = dayjs(detail.scheduledAt).format('YYYY-MM-DDTHH:mm')
        }
      }
    } else {
      postData.value = detail
      effectivePostId.value = detail.id
      form.title = detail.title
      form.content = detail.content
      form.categoryId = detail.categoryId
      form.keywords = detail.keywords.map(k => k.name)
      if (detail.scheduledAt) {
        form.scheduledAt = dayjs(detail.scheduledAt).format('YYYY-MM-DDTHH:mm')
      }
    }
  } catch {
    ElMessage.error('文章加载失败')
    router.replace('/author/posts')
  } finally {
    loading.value = false
  }
}

function buildUpdatePayload() {
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
    const updated = await updateAuthorPost(effectivePostId.value, buildUpdatePayload())
    postData.value = updated
    effectivePostId.value = updated.id
    lastSavedAt.value = dayjs().format('HH:mm:ss')
    ElMessage.success('草稿已保存')
  } catch {
    ElMessage.error('保存失败')
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
    const updated = await updateAuthorPost(effectivePostId.value, buildUpdatePayload())
    await submitForReview(updated.id)
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
  loadPost()
  loadCategories()
  autoSaveTimer = setInterval(() => {
    if (form.title.trim() || form.content.trim()) {
      saveDraft()
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
      <h1>编辑文章</h1>
      <p class="author-subtitle">修改文章内容后保存或提交审核。</p>
      <p v-if="lastSavedAt" class="auto-save-hint">
        上次自动保存: {{ lastSavedAt }}
      </p>
    </header>

    <el-alert
      v-if="postData?.status === 'REJECTED' && postData?.rejectionReason"
      type="error"
      :title="'拒绝原因: ' + postData.rejectionReason"
      show-icon
      :closable="false"
      style="margin-bottom: var(--space-3)"
    />

    <el-alert
      v-if="postData?.revision"
      type="warning"
      title="当前正在编辑的是修订版本"
      description="修改将创建该文章的修订版本，审核通过后将替换线上内容。"
      show-icon
      :closable="false"
      style="margin-bottom: var(--space-3)"
    />

    <el-alert
      v-if="postData?.status === 'PUBLISHED' && !postData?.revision"
      type="info"
      title="编辑已发布文章将创建修订版本"
      description="保存后将创建修订版本，原文章内容不受影响。修订版本需审核通过后才会替换线上内容。"
      show-icon
      :closable="false"
      style="margin-bottom: var(--space-3)"
    />

    <el-card class="panel-card panel-editor" v-loading="loading">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">
            编辑器
            <el-tag v-if="postData?.revision" size="small" type="warning" style="margin-left: 8px">修订版</el-tag>
          </strong>
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
        :loading="submitting || saving || loading"
        :textarea-rows="16"
        submit-text="提交审核"
        @submit="handleSubmit"
      />

      <div class="editor-extra-actions">
        <el-button @click="router.push('/author/posts')">返回列表</el-button>
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
