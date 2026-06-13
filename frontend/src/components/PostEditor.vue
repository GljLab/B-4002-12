<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, FullScreen, Document } from '@element-plus/icons-vue'
import { getAdminCategoriesFlat } from '../api/categories'
import { searchKeywords } from '../api/keywords'
import { uploadImage } from '../api/upload'
import type { Category, Keyword } from '../types'
import {
  countWords,
  countCharacters,
  validateImageFile,
  getMarkdownImageSyntax,
  extractImageFilesFromClipboard,
  extractImageFilesFromDataTransfer,
  type TocItem,
  markdownRenderer,
} from '../utils/markdown'

const props = withDefaults(
  defineProps<{
    title: string
    content: string
    categoryId: number | null
    keywords: string[]
    textareaRows?: number
    loading?: boolean
    submitText?: string
  }>(),
  {
    textareaRows: 10,
    loading: false,
    submitText: '提交',
  },
)

const emit = defineEmits<{
  'update:title': [value: string]
  'update:content': [value: string]
  'update:categoryId': [value: number | null]
  'update:keywords': [value: string[]]
  submit: []
}>()

const categories = ref<Category[]>([])
const categoriesLoading = ref(false)

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await getAdminCategoriesFlat()
  } catch {
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

loadCategories()

function isCategoryDisabled(category: Category): boolean {
  return !category.enabled
}

function buildCategoryLabel(category: Category): string {
  const indent = category.parentName ? `${category.parentName} / ` : ''
  const suffix = category.postCount > 0 ? ` (${category.postCount})` : ''
  return `${indent}${category.name}${suffix}`
}

const keywordOptions = ref<Keyword[]>([])
const keywordSearchLoading = ref(false)
let keywordSearchTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.keywords,
  () => {
    if (props.keywords.length > 0 && keywordOptions.value.length === 0) {
      searchKeywords('')
    }
  },
  { immediate: true },
)

async function handleKeywordSearch(query: string) {
  if (keywordSearchTimer) {
    clearTimeout(keywordSearchTimer)
  }
  keywordSearchTimer = setTimeout(async () => {
    keywordSearchLoading.value = true
    try {
      keywordOptions.value = await searchKeywords(query)
    } catch {
      keywordOptions.value = []
    } finally {
      keywordSearchLoading.value = false
    }
  }, 300)
}

const editorRef = ref<InstanceType<any> | null>(null)
const isFullscreen = ref(false)
const uploadingImage = ref(false)
const editorMode = ref<'edit' | 'preview' | 'live'>('live')

const wordCount = computed(() => countWords(props.content))
const charCount = computed(() => countCharacters(props.content))

const previewToc = computed<TocItem[]>(() => {
  return markdownRenderer.getToc(props.content)
})

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  nextTick(() => {
    document.body.style.overflow = isFullscreen.value ? 'hidden' : ''
  })
}

function handleEditorChange(value: string) {
  emit('update:content', value)
}

function insertAtCursor(text: string, cursorOffset: number = 0) {
  emit('update:content', text)
}

async function handleImageUpload(file: File): Promise<string> {
  const validation = validateImageFile(file)
  if (!validation.valid) {
    ElMessage.error(validation.message!)
    return ''
  }

  uploadingImage.value = true
  try {
    const result = await uploadImage(file)
    ElMessage.success('图片上传成功')
    return result.url
  } catch (err: any) {
    const message = err?.response?.data?.message || err?.message || '图片上传失败，请稍后重试'
    ElMessage.error(`上传失败: ${message}`)
    return ''
  } finally {
    uploadingImage.value = false
  }
}

async function handleUploadImage() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: Event) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]
    if (!file) return

    const url = await handleImageUpload(file)
    if (url) {
      const mdSyntax = getMarkdownImageSyntax(url, file.name.replace(/\.[^/.]+$/, ''))
      insertAtCursor(props.content + (props.content ? '\n' : '') + mdSyntax + '\n')
    }
  }
  input.click()
}

async function handlePaste(event: ClipboardEvent) {
  const files = extractImageFilesFromClipboard(event)
  if (files.length === 0) return

  event.preventDefault()

  for (const file of files) {
    const url = await handleImageUpload(file)
    if (url) {
      const mdSyntax = getMarkdownImageSyntax(url, file.name || 'pasted-image')
      insertAtCursor(props.content + (props.content ? '\n' : '') + mdSyntax + '\n')
    }
  }
}

async function handleDrop(event: DragEvent) {
  event.preventDefault()
  const dataTransfer = event.dataTransfer
  if (!dataTransfer) return

  const files = extractImageFilesFromDataTransfer(dataTransfer)
  if (files.length === 0) return

  for (const file of files) {
    const url = await handleImageUpload(file)
    if (url) {
      const mdSyntax = getMarkdownImageSyntax(url, file.name || 'dropped-image')
      insertAtCursor(props.content + (props.content ? '\n' : '') + mdSyntax + '\n')
    }
  }
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
}

function scrollToHeading(id: string) {
  const previewEl = document.querySelector('.v-md-preview')
  if (!previewEl) return
  const target = previewEl.querySelector(`#${id}`) as HTMLElement
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function handleSaveDraft() {
  ElMessage.success('草稿已保存')
}

onMounted(() => {
  document.addEventListener('paste', handlePaste)
  document.addEventListener('drop', handleDrop)
  document.addEventListener('dragover', handleDragOver)
})

onBeforeUnmount(() => {
  document.removeEventListener('paste', handlePaste)
  document.removeEventListener('drop', handleDrop)
  document.removeEventListener('dragover', handleDragOver)
  if (isFullscreen.value) {
    document.body.style.overflow = ''
  }
})

const toolbarConfig = [
  'bold',
  'italic',
  'strikethrough',
  '|',
  'title',
  'subtitle',
  '|',
  'quote',
  'unordered-list',
  'ordered-list',
  'task-list',
  '|',
  'link',
  {
    name: 'image',
    title: '插入图片',
    icon: 'v-md-icon-image',
    action() {
      handleUploadImage()
    },
  },
  'table',
  'code',
  'code-block',
  '|',
  'undo',
  'redo',
  '|',
  'fullscreen',
  '|',
  {
    name: 'preview-only',
    title: '仅预览',
    text: '预览',
    action() {
      editorMode.value = editorMode.value === 'preview' ? 'live' : 'preview'
    },
  },
  {
    name: 'edit-only',
    title: '仅编辑',
    text: '编辑',
    action() {
      editorMode.value = editorMode.value === 'edit' ? 'live' : 'edit'
    },
  },
]
</script>

<template>
  <div :class="['post-editor-wrapper', { 'is-fullscreen': isFullscreen }]">
    <div v-if="isFullscreen" class="fullscreen-header">
      <h2>文章编辑</h2>
      <div class="fullscreen-stats">
        <span>{{ wordCount }} 字</span>
        <span>{{ charCount }} 字符</span>
      </div>
      <el-button @click="toggleFullscreen">退出全屏</el-button>
    </div>

    <el-form class="editor-form" label-position="top" @submit.prevent="$emit('submit')">
      <el-row :gutter="20">
        <el-col :span="previewToc.length > 0 ? 20 : 24">
          <el-form-item class="editor-form-item" label="标题">
            <el-input
              class="editor-input"
              :model-value="title"
              maxlength="200"
              show-word-limit
              placeholder="请输入文章标题"
              @update:model-value="$emit('update:title', $event)"
            />
          </el-form-item>

          <el-form-item class="editor-form-item editor-content-item" label="正文">
            <div class="editor-toolbar">
              <el-button size="small" @click="handleUploadImage" :loading="uploadingImage">
                <el-icon><Picture /></el-icon>
                上传图片
              </el-button>
              <el-button size="small" @click="toggleFullscreen">
                <el-icon><FullScreen /></el-icon>
                {{ isFullscreen ? '退出全屏' : '全屏编辑' }}
              </el-button>
              <div class="editor-stats">
                <span class="stat-item">字数: {{ wordCount }}</span>
                <span class="stat-item">字符: {{ charCount }}</span>
              </div>
            </div>

            <v-md-editor
              ref="editorRef"
              :model-value="content"
              :mode="editorMode"
              :toolbar="toolbarConfig"
              :height="isFullscreen ? 'calc(100vh - 280px)' : '500px'"
              placeholder="开始撰写你的精彩内容..."
              :disabled="loading"
              @update:model-value="handleEditorChange"
            />
          </el-form-item>
        </el-col>

        <el-col v-if="previewToc.length > 0" :span="4" class="toc-column">
          <div class="toc-wrapper">
            <div class="toc-title">
              <el-icon><Document /></el-icon>
              文章目录
            </div>
            <div class="toc-list">
              <div
                v-for="item in previewToc"
                :key="item.id"
                class="toc-item"
                :class="`toc-level-${item.level}`"
                @click="scrollToHeading(item.id)"
              >
                {{ item.text }}
              </div>
              <template v-for="item in previewToc" :key="'children-' + item.id">
                <div
                  v-for="child in item.children"
                  :key="child.id"
                  class="toc-item"
                  :class="`toc-level-${child.level}`"
                  @click="scrollToHeading(child.id)"
                >
                  {{ child.text }}
                </div>
              </template>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item class="editor-form-item" label="分类">
            <el-tree-select
              :model-value="categoryId"
              :data="categories"
              :loading="categoriesLoading"
              :props="{ label: 'name', children: 'children', disabled: isCategoryDisabled }"
              value-key="id"
              node-key="id"
              check-strictly
              filterable
              clearable
              placeholder="请选择分类"
              style="width: 100%"
              @update:model-value="$emit('update:categoryId', $event ?? null)"
            >
              <template #default="{ data: node }">
                <span :class="{ 'category-disabled': !node.enabled }">
                  {{ buildCategoryLabel(node) }}
                </span>
              </template>
            </el-tree-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item class="editor-form-item" label="关键词">
            <el-select
              :model-value="keywords"
              multiple
              filterable
              allow-create
              default-first-option
              :loading="keywordSearchLoading"
              placeholder="输入或选择关键词"
              style="width: 100%"
              @update:model-value="$emit('update:keywords', $event)"
              @filter-change="handleKeywordSearch"
            >
              <el-option
                v-for="kw in keywordOptions"
                :key="kw.id"
                :label="kw.name"
                :value="kw.name"
              >
                <span>{{ kw.name }}</span>
                <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px">
                  {{ kw.usageCount }} 次引用
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item class="editor-form-item">
        <el-button class="editor-submit-btn" type="primary" :loading="loading" @click="$emit('submit')">
          {{ submitText }}
        </el-button>
        <el-button class="editor-draft-btn" @click="handleSaveDraft" :disabled="loading">
          保存草稿
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.post-editor-wrapper {
  width: 100%;
}

.post-editor-wrapper.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  background: var(--color-surface);
  padding: var(--space-4);
  overflow-y: auto;
}

.fullscreen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.fullscreen-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--color-text-1);
}

.fullscreen-stats {
  display: flex;
  gap: var(--space-4);
  color: var(--color-text-3);
  font-size: 14px;
}

.editor-content-item {
  margin-bottom: var(--space-3) !important;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
  flex-wrap: wrap;
}

.editor-stats {
  margin-left: auto;
  display: flex;
  gap: var(--space-3);
  color: var(--color-text-3);
  font-size: 13px;
}

.stat-item {
  padding: 4px 10px;
  background: var(--color-primary-soft);
  border-radius: 999px;
  color: var(--color-primary);
  font-weight: 500;
}

.toc-column {
  position: sticky;
  top: 20px;
  align-self: flex-start;
}

.toc-wrapper {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.toc-title {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: var(--space-2);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
}

.toc-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.toc-item {
  padding: 6px 10px;
  font-size: 13px;
  color: var(--color-text-2);
  cursor: pointer;
  border-radius: 6px;
  transition: all var(--motion-fast) var(--ease-standard);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-item:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.toc-level-2 {
  padding-left: 20px;
}

.toc-level-3 {
  padding-left: 32px;
  font-size: 12px;
}

.toc-level-4 {
  padding-left: 44px;
  font-size: 12px;
}

.editor-draft-btn {
  margin-left: var(--space-2);
}

.category-disabled {
  color: var(--el-text-color-disabled);
}

:deep(.v-md-editor) {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

:deep(.v-md-editor__toolbar) {
  border-bottom: 1px solid var(--color-border);
  background: #fafbfc;
}

:deep(.v-md-editor__editor-wrapper) {
  min-height: 400px;
}

:deep(.v-md-editor__textarea) {
  font-family: 'SF Mono', 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.8;
  padding: 20px;
}

:deep(.v-md-preview) {
  padding: 20px 24px;
}

@media (max-width: 768px) {
  .toc-column {
    display: none;
  }

  .fullscreen-stats {
    display: none;
  }

  .editor-stats {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>
