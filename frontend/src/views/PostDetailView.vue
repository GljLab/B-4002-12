<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CollectionTag, Star, StarFilled, FolderAdd } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getPostDetail } from '../api/posts'
import { toggleFavorite as toggleFavoriteApi, recordRead as recordReadApi } from '../api/reader'
import { listAlbums, addItemToAlbum, getReadingProgress, updateReadingProgress as updateReadingProgressApi } from '../api/collections'
import { localFavorites, localHistory } from '../utils/local-storage'
import { useAuthStore } from '../stores/auth'
import type { BreadcrumbItem, KeywordItem, PostDetail, CollectionAlbum } from '../types'
import { markdownRenderer, countWords, type TocItem } from '../utils/markdown'
import ImagePreview from '../components/ImagePreview.vue'
import TableOfContents from '../components/TableOfContents.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const post = ref<PostDetail | null>(null)

const postId = computed(() => Number(route.params.id))

const renderedContent = ref('')
const tocItems = ref<TocItem[]>([])
const wordCount = ref(0)

const previewVisible = ref(false)
const previewImageUrl = ref('')
const previewImageAlt = ref('')

const isFavorited = ref(false)

const collectionDialogVisible = ref(false)
const albums = ref<CollectionAlbum[]>([])
const selectedAlbumId = ref<number | null>(null)
const collectionNote = ref('')
const albumsLoading = ref(false)

const readingDuration = ref(0)
const scrollPosition = ref(0)
let readingTimer: ReturnType<typeof setInterval> | null = null
let scrollDebounceTimer: ReturnType<typeof setTimeout> | null = null

async function loadPost() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) {
    ElMessage.error('文章参数错误')
    return
  }

  loading.value = true
  try {
    post.value = await getPostDetail(postId.value)
    if (post.value) {
      const { html, toc } = markdownRenderer.render(post.value.content)
      renderedContent.value = html
      tocItems.value = toc
      wordCount.value = countWords(post.value.content)
      nextTick(() => {
        bindImageClickEvents()
      })

      recordRead()
      checkFavoriteStatus()
      loadReadingProgress()
    }
  } catch {
    ElMessage.error('文章不存在或已删除')
  } finally {
    loading.value = false
  }
}

async function recordRead() {
  if (authStore.isLoggedIn) {
    try { await recordReadApi(postId.value) } catch { /* ignore */ }
  } else {
    localHistory.record(postId.value)
  }
}

function checkFavoriteStatus() {
  if (authStore.isLoggedIn) {
    isFavorited.value = false
  } else {
    isFavorited.value = localFavorites.has(postId.value)
  }
}

async function handleToggleFavorite() {
  if (authStore.isLoggedIn) {
    try {
      await toggleFavoriteApi(postId.value)
      isFavorited.value = !isFavorited.value
      ElMessage.success(isFavorited.value ? '已收藏' : '已取消收藏')
    } catch {
      ElMessage.error('操作失败')
    }
  } else {
    if (isFavorited.value) {
      localFavorites.remove(postId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      localFavorites.add(postId.value)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  }
}

async function openCollectionDialog() {
  collectionDialogVisible.value = true
  selectedAlbumId.value = null
  collectionNote.value = ''
  albumsLoading.value = true
  try {
    albums.value = await listAlbums()
  } catch {
    ElMessage.error('获取专辑列表失败')
  } finally {
    albumsLoading.value = false
  }
}

async function handleAddToAlbum() {
  if (!selectedAlbumId.value) {
    ElMessage.warning('请选择一个专辑')
    return
  }
  try {
    await addItemToAlbum(selectedAlbumId.value, {
      postId: postId.value,
      note: collectionNote.value || undefined,
    })
    ElMessage.success('已珍藏到专辑')
    collectionDialogVisible.value = false
  } catch {
    ElMessage.error('珍藏失败')
  }
}

async function loadReadingProgress() {
  if (authStore.isLoggedIn) {
    try {
      const progress = await getReadingProgress(postId.value)
      if (progress && progress.scrollPosition > 0) {
        readingDuration.value = progress.durationSeconds
        nextTick(() => {
          window.scrollTo(0, progress.scrollPosition)
        })
      }
    } catch { /* ignore */ }
  } else {
    const progress = localHistory.getProgress(postId.value)
    if (progress && progress.scrollPosition > 0) {
      readingDuration.value = progress.durationSeconds
      nextTick(() => {
        window.scrollTo(0, progress.scrollPosition)
      })
    }
  }
}

function saveReadingProgress() {
  if (!post.value) return
  if (authStore.isLoggedIn) {
    updateReadingProgressApi(postId.value, readingDuration.value, scrollPosition.value).catch(() => {})
  } else {
    localHistory.updateProgress(postId.value, readingDuration.value, scrollPosition.value)
  }
}

function startReadingTimer() {
  readingTimer = setInterval(() => {
    readingDuration.value += 10
    saveReadingProgress()
  }, 10000)
}

function handleScroll() {
  if (scrollDebounceTimer) clearTimeout(scrollDebounceTimer)
  scrollDebounceTimer = setTimeout(() => {
    scrollPosition.value = window.scrollY
  }, 300)
}

function goToCategory(item: BreadcrumbItem) {
  router.push({ path: '/', query: { categoryId: String(item.id) } })
}

function goToKeyword(keyword: KeywordItem) {
  router.push({ path: '/', query: { keywordId: String(keyword.id) } })
}

function bindImageClickEvents() {
  nextTick(() => {
    const contentEl = document.querySelector('.detail-content')
    if (!contentEl) return

    const images = contentEl.querySelectorAll('img')
    images.forEach((img) => {
      img.style.cursor = 'zoom-in'
      img.addEventListener('click', (e) => {
        const target = e.target as HTMLImageElement
        previewImageUrl.value = target.src
        previewImageAlt.value = target.alt || ''
        previewVisible.value = true
      })
    })
  })
}

function handleTocNavigate(id: string) {
  const hash = `#${id}`
  if (history.replaceState) {
    history.replaceState(null, '', hash)
  }
}

watch(
  () => renderedContent.value,
  () => {
    nextTick(() => {
      bindImageClickEvents()
    })
  },
)

onMounted(() => {
  loadPost()
  startReadingTimer()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (readingTimer) {
    clearInterval(readingTimer)
    readingTimer = null
  }
  if (scrollDebounceTimer) {
    clearTimeout(scrollDebounceTimer)
    scrollDebounceTimer = null
  }
  scrollPosition.value = window.scrollY
  saveReadingProgress()
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="view-shell detail-shell">
    <TableOfContents
      v-if="tocItems.length > 0"
      :items="tocItems"
      content-selector=".detail-content"
      @navigate="handleTocNavigate"
    />

    <el-card v-loading="loading" class="detail-card">
      <template v-if="post">
        <el-breadcrumb v-if="post.categoryBreadcrumb.length" separator="/">
          <el-breadcrumb-item
            v-for="item in post.categoryBreadcrumb"
            :key="item.id"
          >
            <a @click.prevent="goToCategory(item)">{{ item.name }}</a>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-divider v-if="post.categoryBreadcrumb.length" />

        <header class="detail-header">
          <h1 class="detail-title">{{ post.title }}</h1>
          <p class="meta">
            <el-avatar :size="36" :src="post.authorAvatar || undefined" class="author-avatar">
              {{ post.authorName?.charAt(0) }}
            </el-avatar>
            <router-link :to="`/authors/${post.authorId}`" class="author-link">作者：{{ post.authorName }}</router-link>
            <span>·</span>
            {{ post.viewCount }} 次阅读
            <span>·</span>
            {{ dayjs(post.createdAt).format('YYYY-MM-DD HH:mm:ss') }}
            <span v-if="wordCount > 0" class="word-count">
              · {{ wordCount }} 字
            </span>
            <span class="meta-actions">
              <el-button
                :type="isFavorited ? 'danger' : 'default'"
                :icon="isFavorited ? StarFilled : Star"
                circle
                size="small"
                @click="handleToggleFavorite"
              />
              <el-button
                v-if="authStore.isLoggedIn"
                :icon="FolderAdd"
                circle
                size="small"
                @click="openCollectionDialog"
              />
            </span>
          </p>
        </header>

        <el-divider />

        <div
          class="detail-content markdown-body"
          v-html="renderedContent"
        />

        <el-divider v-if="post.keywords.length" />

        <div v-if="post.keywords.length" class="detail-keywords">
          <span class="keywords-label">
            <el-icon><CollectionTag /></el-icon>
            关键词：
          </span>
          <el-tag
            v-for="keyword in post.keywords"
            :key="keyword.id"
            class="keyword-tag"
            @click="goToKeyword(keyword)"
          >
            {{ keyword.name }}
          </el-tag>
        </div>
      </template>

      <template v-else>
        <el-empty class="detail-empty" description="未找到该文章" />
      </template>
    </el-card>

    <ImagePreview
      v-model:visible="previewVisible"
      :image-url="previewImageUrl"
      :image-alt="previewImageAlt"
    />

    <el-dialog v-model="collectionDialogVisible" title="珍藏到专辑" width="480px">
      <div v-loading="albumsLoading">
        <el-select v-model="selectedAlbumId" placeholder="选择专辑" style="width: 100%">
          <el-option
            v-for="album in albums"
            :key="album.id"
            :label="album.name"
            :value="album.id"
          />
        </el-select>
        <el-input
          v-model="collectionNote"
          type="textarea"
          placeholder="添加备注（可选）"
          :rows="3"
          style="margin-top: 16px"
        />
      </div>
      <template #footer>
        <el-button @click="collectionDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAlbumId" @click="handleAddToAlbum">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.meta-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
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

.word-count {
  color: var(--color-text-3);
}

.keyword-tag {
  cursor: pointer;
  margin: 0 8px 8px 0;
}

.detail-keywords {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.keywords-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-3);
  font-size: 14px;
  padding-top: 4px;
}

.detail-content {
  color: var(--color-text-2);
  font-size: 16px;
  line-height: 1.9;
  word-break: break-word;
}

.detail-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 16px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform var(--motion-base) var(--ease-standard);
}

.detail-content :deep(img:hover) {
  transform: scale(1.01);
}

.detail-content :deep(.md-heading) {
  scroll-margin-top: 80px;
  position: relative;
  padding-left: 16px;
  margin-top: 32px;
  margin-bottom: 16px;
}

.detail-content :deep(.md-heading::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0.3em;
  bottom: 0.3em;
  width: 4px;
  background: var(--color-primary);
  border-radius: 2px;
}

.detail-content :deep(h1) {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-1);
  padding-bottom: 12px;
  border-bottom: 2px solid var(--color-border);
}

.detail-content :deep(h2) {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-1);
  margin-top: 40px;
}

.detail-content :deep(h3) {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}

.detail-content :deep(h4) {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);
}

.detail-content :deep(p) {
  margin: 16px 0;
  line-height: 1.9;
}

.detail-content :deep(strong) {
  font-weight: 600;
  color: var(--color-text-1);
}

.detail-content :deep(em) {
  font-style: italic;
}

.detail-content :deep(del) {
  text-decoration: line-through;
  color: var(--color-text-3);
}

.detail-content :deep(blockquote) {
  margin: 20px 0;
  padding: 16px 24px;
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, rgba(255, 255, 255, 0.5) 100%);
  border-left: 4px solid var(--color-primary);
  border-radius: 0 8px 8px 0;
  color: var(--color-text-2);
  font-style: italic;
}

.detail-content :deep(blockquote p) {
  margin: 0;
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
  margin: 16px 0;
  padding-left: 24px;
}

.detail-content :deep(li) {
  margin: 8px 0;
  line-height: 1.8;
}

.detail-content :deep(ul li) {
  list-style-type: disc;
}

.detail-content :deep(ol li) {
  list-style-type: decimal;
}

.detail-content :deep(code) {
  background: #f4f7fb;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.detail-content :deep(pre) {
  margin: 20px 0;
  padding: 20px;
  background: #1e293b;
  border-radius: 8px;
  overflow-x: auto;
  position: relative;
}

.detail-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.7;
}

.detail-content :deep(table) {
  width: 100%;
  margin: 20px 0;
  border-collapse: collapse;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.detail-content :deep(thead) {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-strong) 100%);
}

.detail-content :deep(th) {
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.detail-content :deep(td) {
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  background: #fff;
}

.detail-content :deep(tbody tr:hover td) {
  background: var(--color-primary-soft);
}

.detail-content :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all var(--motion-fast) var(--ease-standard);
}

.detail-content :deep(a:hover) {
  border-bottom-color: var(--color-primary);
}

.detail-content :deep(hr) {
  margin: 32px 0;
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-border), transparent);
}

.detail-content :deep(.contains-task-list) {
  list-style: none;
  padding-left: 0;
}

.detail-content :deep(.task-list-item) {
  list-style: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 0;
}

.detail-content :deep(.task-list-item-checkbox) {
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary);
}

.detail-content :deep(.hljs) {
  background: transparent;
  padding: 0;
}

.detail-content :deep(.hljs-keyword),
.detail-content :deep(.hljs-selector-tag),
.detail-content :deep(.hljs-literal),
.detail-content :deep(.hljs-section),
.detail-content :deep(.hljs-link) {
  color: #c792ea;
}

.detail-content :deep(.hljs-string),
.detail-content :deep(.hljs-title),
.detail-content :deep(.hljs-name),
.detail-content :deep(.hljs-type),
.detail-content :deep(.hljs-attribute),
.detail-content :deep(.hljs-symbol),
.detail-content :deep(.hljs-bullet),
.detail-content :deep(.hljs-addition),
.detail-content :deep(.hljs-variable),
.detail-content :deep(.hljs-template-tag),
.detail-content :deep(.hljs-template-variable) {
  color: #c3e88d;
}

.detail-content :deep(.hljs-number),
.detail-content :deep(.hljs-meta),
.detail-content :deep(.hljs-built_in),
.detail-content :deep(.hljs-builtin-name),
.detail-content :deep(.hljs-literal),
.detail-content :deep(.hljs-type),
.detail-content :deep(.hljs-selector-attr),
.detail-content :deep(.hljs-selector-pseudo) {
  color: #f78c6c;
}

.detail-content :deep(.hljs-comment),
.detail-content :deep(.hljs-quote),
.detail-content :deep(.hljs-deletion),
.detail-content :deep(.hljs-meta) {
  color: #676e95;
  font-style: italic;
}

.detail-content :deep(.hljs-title.function_) {
  color: #82aaff;
}

@media (max-width: 768px) {
  .detail-content {
    font-size: 15px;
  }

  .detail-content :deep(h1) {
    font-size: 24px;
  }

  .detail-content :deep(h2) {
    font-size: 20px;
  }

  .detail-content :deep(h3) {
    font-size: 18px;
  }

  .detail-content :deep(pre) {
    padding: 16px;
  }

  .detail-content :deep(pre code) {
    font-size: 13px;
  }

  .detail-content :deep(th),
  .detail-content :deep(td) {
    padding: 10px 12px;
    font-size: 14px;
  }
}
</style>
