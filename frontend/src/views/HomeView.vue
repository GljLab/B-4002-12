<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Star, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getPublicPosts } from '../api/posts'
import { getPublicCategories } from '../api/categories'
import { getKeywordCloud } from '../api/keywords'
import { getPublicAuthors } from '../api/authors'
import { getFavoriteRanking } from '../api/collections'
import type { PostSummary, PostFavoriteCount, Category, KeywordCloud, AuthorPublicDTO, PageResponse } from '../types'
import { markdownRenderer } from '../utils/markdown'

const route = useRoute()
const router = useRouter()

const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const postPreviews = computed(() => {
  return posts.value.map((post) => {
    const content = post.previewContent || post.excerpt || ''
    const { html } = markdownRenderer.render(content)
    return {
      ...post,
      previewHtml: html,
    }
  })
})

const categories = ref<Category[]>([])
const keywords = ref<KeywordCloud[]>([])
const authors = ref<AuthorPublicDTO[]>([])
const favoriteRanking = ref<PostFavoriteCount[]>([])
const selectedCategoryId = ref<number | null>(null)
const selectedKeywordId = ref<number | null>(null)
const selectedAuthorId = ref<number | null>(null)

function readQueryParams() {
  const qCat = route.query.categoryId
  const qKw = route.query.keywordId
  const qAuthor = route.query.authorId
  selectedCategoryId.value = qCat ? Number(qCat) : null
  selectedKeywordId.value = qKw ? Number(qKw) : null
  selectedAuthorId.value = qAuthor ? Number(qAuthor) : null
}

function pushQueryParams() {
  const query: Record<string, string> = {}
  if (selectedCategoryId.value != null) query.categoryId = String(selectedCategoryId.value)
  if (selectedKeywordId.value != null) query.keywordId = String(selectedKeywordId.value)
  if (selectedAuthorId.value != null) query.authorId = String(selectedAuthorId.value)
  router.replace({ path: '/', query })
}

async function loadPosts() {
  loading.value = true
  try {
    const data: PageResponse<PostSummary> = await getPublicPosts(
      page.value,
      size.value,
      selectedCategoryId.value,
      selectedKeywordId.value,
      selectedAuthorId.value,
    )
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

async function loadKeywords() {
  try {
    keywords.value = await getKeywordCloud()
  } catch {
    ElMessage.error('关键词加载失败')
  }
}

async function loadAuthors() {
  try {
    authors.value = await getPublicAuthors()
  } catch {
    ElMessage.error('作者列表加载失败')
  }
}

async function loadFavoriteRanking() {
  try {
    favoriteRanking.value = await getFavoriteRanking(5)
  } catch {
    favoriteRanking.value = []
  }
}

async function applyFilter() {
  page.value = 1
  pushQueryParams()
  await loadPosts()
}

function onCategoryChange() {
  applyFilter()
}

function onKeywordChange() {
  applyFilter()
}

function onAuthorChange() {
  applyFilter()
}

function clearFilters() {
  selectedCategoryId.value = null
  selectedKeywordId.value = null
  selectedAuthorId.value = null
  applyFilter()
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage
  await loadPosts()
}

onMounted(() => {
  readQueryParams()
  loadCategories()
  loadKeywords()
  loadAuthors()
  loadFavoriteRanking()
  loadPosts()
})

watch(() => route.query, () => {
  readQueryParams()
  loadPosts()
})
</script>

<template>
  <div class="view-shell home-shell">
    <section class="home-hero">
      <p class="hero-kicker">BLOG</p>
      <h1>最新文章</h1>
      <p class="hero-desc">浏览最新发布内容，快速进入文章详情。</p>
    </section>

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
      <el-select
        v-model="selectedKeywordId"
        placeholder="选择关键词"
        clearable
        style="min-width: 180px"
        @change="onKeywordChange"
      >
        <el-option
          v-for="kw in keywords"
          :key="kw.id"
          :label="kw.name"
          :value="kw.id"
        />
      </el-select>
      <el-select
        v-model="selectedAuthorId"
        placeholder="选择作者"
        clearable
        style="min-width: 180px"
        @change="onAuthorChange"
      >
        <el-option
          v-for="author in authors"
          :key="author.id"
          :label="author.nickname"
          :value="author.id"
        />
      </el-select>
      <el-button
        v-if="selectedCategoryId != null || selectedKeywordId != null || selectedAuthorId != null"
        @click="clearFilters"
      >
        清除筛选
      </el-button>
    </div>

    <el-skeleton v-if="loading" class="skeleton-panel" :rows="6" animated />

    <template v-else>
      <el-empty v-if="posts.length === 0" class="home-empty" description="暂无文章" />
      <div v-else class="card-list">
        <el-card
          v-for="(post, index) in postPreviews"
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
          <p class="meta">
            <el-avatar :size="24" :src="post.authorAvatar || undefined" class="author-avatar">
              {{ post.authorName?.charAt(0) }}
            </el-avatar>
            <router-link :to="`/authors/${post.authorId}`" class="author-link">{{ post.authorName }}</router-link>
            <span>·</span>
            {{ dayjs(post.createdAt).format('YYYY-MM-DD HH:mm:ss') }}
            <span class="meta-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ post.viewCount }}
              </span>
              <span class="stat-item">
                <el-icon><Star /></el-icon>
                {{ post.favoriteCount }}
              </span>
            </span>
          </p>
          <div class="post-tags">
            <el-tag
              v-if="post.categoryPath"
              class="category-tag"
              size="small"
              @click="selectedCategoryId = post.categoryId; applyFilter()"
            >
              {{ post.categoryPath }}
            </el-tag>
            <el-tag
              v-for="kw in post.keywords"
              :key="kw.id"
              size="small"
              type="info"
              class="keyword-tag"
              @click="selectedKeywordId = kw.id; applyFilter()"
            >
              {{ kw.name }}
            </el-tag>
          </div>
          <div class="post-preview markdown-body" v-html="post.previewHtml"></div>
          <div class="read-more-wrap">
            <router-link :to="`/posts/${post.id}`" class="read-more-link">
              阅读全文
              <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
        </el-card>
      </div>
      <el-card v-if="favoriteRanking.length > 0" class="favorite-ranking-card" shadow="hover">
        <template #header>
          <span class="ranking-title">
            <el-icon><Star /></el-icon>
            最受青睐
          </span>
        </template>
        <ol class="ranking-list">
          <li v-for="(item, idx) in favoriteRanking" :key="item.postId" class="ranking-item">
            <span class="ranking-rank">{{ idx + 1 }}</span>
            <router-link :to="`/posts/${item.postId}`" class="ranking-link">{{ item.postTitle }}</router-link>
            <span class="ranking-count">
              <el-icon><Star /></el-icon>
              {{ item.favoriteCount }}
            </span>
          </li>
        </ol>
      </el-card>
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
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: var(--space-3) var(--space-4);
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.meta-stats {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--color-text-3);
  font-size: 13px;
}

.stat-item .el-icon {
  font-size: 14px;
}

.author-avatar {
  flex-shrink: 0;
}

.author-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.author-link:hover {
  opacity: 0.75;
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
  cursor: pointer;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.keyword-tag:hover {
  opacity: 0.75;
}

.post-preview {
  margin-top: var(--space-3);
  max-height: 320px;
  overflow: hidden;
  position: relative;
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.post-preview::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(to bottom, transparent, var(--el-bg-color));
  pointer-events: none;
}

.post-preview :deep(img) {
  max-width: 100%;
  max-height: 240px;
  border-radius: 8px;
  margin: 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  object-fit: cover;
}

.post-preview :deep(p) {
  margin: 10px 0;
}

.post-preview :deep(h1),
.post-preview :deep(h2),
.post-preview :deep(h3),
.post-preview :deep(h4) {
  font-weight: 600;
  color: var(--color-text-1);
  margin: 14px 0 8px;
}

.post-preview :deep(h1) { font-size: 20px; }
.post-preview :deep(h2) { font-size: 18px; }
.post-preview :deep(h3) { font-size: 16px; }
.post-preview :deep(h4) { font-size: 15px; }

.post-preview :deep(strong) {
  font-weight: 600;
  color: var(--color-text-1);
}

.post-preview :deep(em) {
  font-style: italic;
}

.post-preview :deep(del) {
  text-decoration: line-through;
  color: var(--color-text-3);
}

.post-preview :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 16px;
  background: var(--color-primary-soft);
  border-left: 3px solid var(--color-primary);
  border-radius: 0 6px 6px 0;
  color: var(--color-text-2);
  font-style: italic;
}

.post-preview :deep(blockquote p) {
  margin: 0;
}

.post-preview :deep(ul),
.post-preview :deep(ol) {
  margin: 10px 0;
  padding-left: 24px;
}

.post-preview :deep(li) {
  margin: 4px 0;
}

.post-preview :deep(code) {
  background: #f4f7fb;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 0.85em;
  color: #e83e8c;
}

.post-preview :deep(pre) {
  margin: 12px 0;
  padding: 12px 16px;
  background: #1e293b;
  border-radius: 6px;
  overflow-x: auto;
}

.post-preview :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #e2e8f0;
  font-size: 13px;
  line-height: 1.6;
}

.post-preview :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
  border-radius: 6px;
  overflow: hidden;
}

.post-preview :deep(th) {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  background: var(--color-primary);
  color: #fff;
}

.post-preview :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  background: #fff;
}

.post-preview :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all var(--motion-fast) var(--ease-standard);
}

.post-preview :deep(a:hover) {
  border-bottom-color: var(--color-primary);
}

.read-more-wrap {
  margin-top: var(--space-3);
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border-light);
}

.read-more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--motion-fast) var(--ease-standard);
}

.read-more-link:hover {
  gap: 8px;
  opacity: 0.85;
}

.favorite-ranking-card {
  margin-top: var(--space-4);
}

.ranking-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: 600;
  font-size: 15px;
}

.ranking-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.ranking-link {
  flex: 1;
  color: var(--color-text-1);
  text-decoration: none;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--motion-fast) var(--ease-standard);
}

.ranking-link:hover {
  color: var(--color-primary);
}

.ranking-count {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--color-text-3);
  font-size: 13px;
  flex-shrink: 0;
}

.ranking-count .el-icon {
  font-size: 14px;
  color: #e6a23c;
}

@media (max-width: 768px) {
  .post-preview {
    max-height: 240px;
    font-size: 13px;
  }

  .post-preview :deep(h1) { font-size: 18px; }
  .post-preview :deep(h2) { font-size: 16px; }
  .post-preview :deep(h3) { font-size: 15px; }

  .post-preview :deep(img) {
    max-height: 180px;
  }
}
</style>
