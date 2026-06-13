<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminPostDetail, updatePost } from '../api/posts'
import PostEditor from '../components/PostEditor.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  title: '',
  content: '',
  categoryId: null as number | null,
  keywords: [] as string[],
})

const postId = computed(() => Number(route.params.id))

async function loadPost() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) {
    ElMessage.error('文章参数错误')
    await router.replace('/admin')
    return
  }

  loading.value = true
  try {
    const detail = await getAdminPostDetail(postId.value)
    form.title = detail.title
    form.content = detail.content
    form.categoryId = detail.categoryId
    form.keywords = detail.keywords.map(k => k.name)
  } catch {
    ElMessage.error('文章加载失败')
    await router.replace('/admin')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  saving.value = true
  try {
    await updatePost(postId.value, {
      title: form.title.trim(),
      content: form.content.trim(),
      categoryId: form.categoryId,
      keywords: form.keywords,
    })
    ElMessage.success('文章编辑成功')
    await router.push('/admin')
  } catch {
    ElMessage.error('文章编辑失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadPost)
</script>

<template>
  <div class="view-shell editor-shell">
    <header class="editor-header">
      <h1>编辑文章</h1>
      <p class="editor-subtitle">更新标题和正文后保存修改。</p>
    </header>

    <el-card class="panel-card editor-card" v-loading="loading">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">编辑文章</strong>
        </div>
      </template>
      <PostEditor
        v-model:title="form.title"
        v-model:content="form.content"
        v-model:category-id="form.categoryId"
        v-model:keywords="form.keywords"
        :loading="saving"
        submit-text="保存修改"
        @submit="submit"
      />
    </el-card>
  </div>
</template>
