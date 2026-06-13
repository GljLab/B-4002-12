<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getPublicAuthorProfile, getAuthorPublicPosts } from '../api/authors'
import { getPublicCategories } from '../api/categories'
import type { AuthorPublicDTO, Category, PostSummary, PageResponse } from '../types'

const route = useRoute()
const router = useRouter()

const authorId = computed(() => Number(route.params.id))

const author = ref<AuthorPublicDTO | null>(null)
const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const authorLoading = ref(false)

const categories = ref<Category[]>([])
const selectedCategoryId = ref<number | null>(null)

async function loadAuthor() {
  authorLoading.value = true
  try {
    author.value = await getPublicAuthorProfile(authorId.value)
  } catch {
    ElMessage.error('作者信息加载失败')
  } finally {
    authorLoading.value = false
  }
}

async function loadPosts() {
  loading.value = true
  try {
    const data: PageResponse<PostSummary> = await getAuthorPublicPosts(authorId.value, {
      page: page.value,
      size: size.value,
      categoryId: selectedCategoryId.value ?? undefined,
    })
    posts.value = data.items
    total.value = data.total
  } catch {
    ElMessage.error('文章列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getPublicCategories()
  } catch {
    ElMessage.error('分类加载失败')
  }
}

function onCategoryChange() {
  page.value = 1
  loadPosts()
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage
  await loadPosts()
}

async function init() {
  page.value = 1
  selectedCategoryId.value = null
  await Promise.all([loadAuthor(), loadPosts()])
}

onMounted(() => {
  loadCategories()
  init()
})

watch(() => route.params.id, () => {
  if (Number(route.params.id) !== authorId.value) return
  init()
})

watch(authorId, () => {
  init()
})
</script>

<template>
  <div class="view-shell author-profile-shell">
    <el-skeleton v-if="authorLoading" :rows="4" animated />
    <template v-else-if="author">
      <el-card class="author-card" shadow="never">
        <div class="author-info">
          <el-avatar :size="80" :src="author.avatarUrl || undefined" class="author-avatar">
            {{ author.nickname?.charAt(0) }}
          </el-avatar>
          <div class="author-detail">
            <h1 class="author-nickname">{{ author.nickname }}</h1>
            <p class="author-username">@{{ author.username }}</p>
            <p v-if="author.bio" class="author-bio">{{ author.bio }}</p>
            <p class="author-stats">已发布 {{ author.postCount }} 篇文章</p>
          </div>
        </div>
      </el-card>

      <div class="filter-bar">
        <el-tree-select
          v-model="selectedCategoryId"
          :data="categories"
          :props="{ label: 'name', children: 'children', value: 'id' }"
          value-key="id"
          placeholder="选择分类"
          clearable
          check-strictly
          style="min-width: 200px"
          @change="onCategoryChange"
        />
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />

      <template v-else>
        <el-empty v-if="posts.length === 0" description="该作者暂无文章" />
        <div v-else class="card-list">
          <el-card
            v-for="(post, index) in posts"
            :key="post.id"
            class="post-card"
            shadow="hover"
            :style="{ '--stagger-index': index }"
          >
            <template #header>
              <router-link :to="`/posts/${post.id}`" class="title-link">
                {{ post.title }}
              </router-link>
            </template>
            <p class="meta">{{ dayjs(post.createdAt).format('YYYY-MM-DD HH:mm:ss') }}</p>
            <div class="post-tags">
              <el-tag
                v-if="post.categoryPath"
                class="category-tag"
                size="small"
                @click="selectedCategoryId = post.categoryId; onCategoryChange()"
              >
                {{ post.categoryPath }}
              </el-tag>
              <el-tag
                v-for="kw in post.keywords"
                :key="kw.id"
                size="small"
                type="info"
                class="keyword-tag"
              >
                {{ kw.name }}
              </el-tag>
            </div>
            <p class="excerpt">{{ post.excerpt }}</p>
          </el-card>
        </div>
        <div class="pagination-row">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </template>
    <el-empty v-else description="未找到该作者" />
  </div>
</template>

<style scoped>
.author-card {
  margin-bottom: var(--space-4);
}

.author-info {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.author-avatar {
  flex-shrink: 0;
}

.author-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.author-nickname {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.author-username {
  margin: 0;
  color: var(--color-text-secondary, #909399);
  font-size: 0.875rem;
}

.author-bio {
  margin: 0;
  color: var(--color-text-regular, #606266);
}

.author-stats {
  margin: 0;
  color: var(--color-text-secondary, #909399);
  font-size: 0.875rem;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.post-tags {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
  margin-top: var(--space-2);
}

.category-tag {
  cursor: pointer;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.category-tag:hover {
  opacity: 0.75;
}

.keyword-tag {
  transition: opacity var(--motion-fast) var(--ease-standard);
}
</style>
