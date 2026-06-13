<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchUpdateCategory, batchAddKeywords, createPost, deletePost, getMyPosts } from '../api/posts'
import { getAdminCategoriesFlat } from '../api/categories'
import type { PostSummary, Category } from '../types'
import { useAuthStore } from '../stores/auth'
import PostEditor from '../components/PostEditor.vue'

const authStore = useAuthStore()

const router = useRouter()
const posts = ref<PostSummary[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const submitting = ref(false)
const selectedPosts = ref<number[]>([])
const batchCategoryDialog = ref(false)
const batchKeywordDialog = ref(false)
const batchCategoryId = ref<number | null>(null)
const batchKeywords = ref<string[]>([])

const form = reactive({
  title: '',
  content: '',
  categoryId: null as number | null,
  keywords: [] as string[],
})

async function loadMine() {
  loading.value = true
  try {
    posts.value = await getMyPosts()
  } catch {
    ElMessage.error('获取我的文章失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getAdminCategoriesFlat()
  } catch {
    categories.value = []
  }
}

async function submitPost() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  submitting.value = true
  try {
    await createPost({
      title: form.title.trim(),
      content: form.content.trim(),
      categoryId: form.categoryId,
      keywords: form.keywords,
    })
    form.title = ''
    form.content = ''
    form.categoryId = null
    form.keywords = []
    ElMessage.success('文章发布成功')
    await loadMine()
  } catch {
    ElMessage.error('文章发布失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这篇文章吗？', '确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deletePost(id)
    ElMessage.success('删除成功')
    await loadMine()
  } catch {
    // cancelled or failed
  }
}

function handleEdit(id: number) {
  if (authStore.isAdmin) {
    router.push(`/admin/posts/${id}/edit`)
  } else if (authStore.isAuthor) {
    router.push(`/author/posts/${id}/edit`)
  } else {
    ElMessage.error('无权限编辑此文章')
  }
}

function handleSelectionChange(rows: PostSummary[]) {
  selectedPosts.value = rows.map(r => r.id)
}

async function handleBatchCategory() {
  if (selectedPosts.value.length === 0 || batchCategoryId.value == null) {
    ElMessage.warning('请选择文章和分类')
    return
  }
  try {
    await batchUpdateCategory(selectedPosts.value, batchCategoryId.value)
    ElMessage.success('批量修改分类成功')
    batchCategoryDialog.value = false
    batchCategoryId.value = null
    selectedPosts.value = []
    await loadMine()
  } catch {
    ElMessage.error('批量修改分类失败')
  }
}

async function handleBatchKeywords() {
  if (selectedPosts.value.length === 0 || batchKeywords.value.length === 0) {
    ElMessage.warning('请选择文章和关键词')
    return
  }
  try {
    await batchAddKeywords(selectedPosts.value, batchKeywords.value)
    ElMessage.success('批量添加关键词成功')
    batchKeywordDialog.value = false
    batchKeywords.value = []
    selectedPosts.value = []
    await loadMine()
  } catch {
    ElMessage.error('批量添加关键词失败')
  }
}

onMounted(() => {
  loadMine()
  loadCategories()
})
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>后台管理</h1>
      <p class="admin-subtitle">发布新文章并管理你的内容。</p>
    </header>

    <section class="admin-compose-section">
      <el-card class="panel-card panel-editor panel-editor-full">
        <template #header>
          <div class="panel-title-wrap">
            <strong class="panel-title">发布新文章</strong>
            <span class="panel-meta">全宽编辑区，专注写作</span>
          </div>
        </template>
        <PostEditor
          v-model:title="form.title"
          v-model:content="form.content"
          v-model:category-id="form.categoryId"
          v-model:keywords="form.keywords"
          :loading="submitting"
          :textarea-rows="16"
          submit-text="发布文章"
          @submit="submitPost"
        />
      </el-card>
    </section>

    <section class="admin-list-section">
      <el-card class="panel-card panel-table">
        <template #header>
          <div class="panel-title-wrap">
            <strong class="panel-title">我的文章</strong>
            <div v-if="selectedPosts.length > 0" class="batch-actions">
              <el-button size="small" @click="batchCategoryDialog = true">批量修改分类</el-button>
              <el-button size="small" @click="batchKeywordDialog = true">批量添加关键词</el-button>
            </div>
          </div>
        </template>

        <el-table
          class="admin-table"
          v-loading="loading"
          :data="posts"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column label="分类" min-width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.categoryPath" size="small">{{ scope.row.categoryPath }}</el-tag>
              <span v-else class="meta">未分类</span>
            </template>
          </el-table-column>
          <el-table-column label="关键词" min-width="160">
            <template #default="scope">
              <el-tag v-for="kw in scope.row.keywords" :key="kw.id" size="small" type="info" style="margin: 2px">
                {{ kw.name }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" min-width="160">
            <template #default="scope">
              {{ dayjs(scope.row.createdAt).format('YYYY-MM-DD HH:mm') }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button class="action-btn" type="primary" link @click="handleEdit(scope.row.id)">编辑</el-button>
              <el-button class="action-btn" type="danger" link @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <el-dialog v-model="batchCategoryDialog" title="批量修改分类" width="420px">
      <el-tree-select
        v-model="batchCategoryId"
        :data="categories"
        :props="{ label: 'name', children: 'children', disabled: (c: Category) => !c.enabled }"
        value-key="id"
        node-key="id"
        check-strictly
        filterable
        clearable
        placeholder="请选择分类"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="batchCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBatchCategory">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchKeywordDialog" title="批量添加关键词" width="420px">
      <el-select
        v-model="batchKeywords"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入或选择关键词"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="batchKeywordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBatchKeywords">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.batch-actions {
  display: flex;
  gap: var(--space-2);
}
</style>
