<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKeywordCloud } from '../api/keywords'
import type { KeywordCloud } from '../types'

const router = useRouter()
const keywords = ref<KeywordCloud[]>([])
const loading = ref(false)
const searchQuery = ref('')

const filteredKeywords = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  let list = keywords.value
  if (query) {
    list = list.filter(k => k.name.toLowerCase().includes(query))
  }
  return list
    .sort((a, b) => b.heatScore - a.heatScore)
    .slice(0, 100)
})

const displayCount = computed(() => filteredKeywords.value.length)

const totalCount = computed(() => keywords.value.length)

const minHeat = computed(() => {
  if (keywords.value.length === 0) return 0
  return Math.min(...keywords.value.map(k => k.heatScore))
})

const maxHeat = computed(() => {
  if (keywords.value.length === 0) return 1
  return Math.max(...keywords.value.map(k => k.heatScore))
})

function heatToSize(heat: number): string {
  const range = maxHeat.value - minHeat.value || 1
  const ratio = (heat - minHeat.value) / range
  const size = 14 + ratio * 22
  return `${size}px`
}

function heatToColor(heat: number): string {
  const range = maxHeat.value - minHeat.value || 1
  const ratio = (heat - minHeat.value) / range
  const r = Math.round(64 + ratio * (64 - 64))
  const g = Math.round(158 + ratio * (128 - 158))
  const b = Math.round(255 + ratio * (255 - 255))
  const dr = Math.round(100 + ratio * (40 - 100))
  const dg = Math.round(180 + ratio * (100 - 180))
  const db = Math.round(255 + ratio * (200 - 255))
  return `rgb(${dr}, ${dg}, ${db})`
}

function handleClick(keyword: KeywordCloud) {
  router.push({ path: '/', query: { keywordId: String(keyword.id) } })
}

async function loadKeywords() {
  loading.value = true
  try {
    keywords.value = await getKeywordCloud()
  } catch {
    ElMessage.error('关键词云加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadKeywords)
</script>

<template>
  <div class="view-shell cloud-shell">
    <section class="cloud-hero">
      <p class="hero-kicker">KEYWORDS</p>
      <h1>关键词云</h1>
      <p class="hero-desc">可视化关键词热度分布，点击关键词查看相关文章。</p>
    </section>

    <div class="cloud-toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索关键词..."
        clearable
        class="cloud-search"
      />
      <span class="cloud-count">显示 {{ displayCount }} / {{ totalCount }} 个关键词</span>
    </div>

    <el-skeleton v-if="loading" class="skeleton-panel" :rows="6" animated />

    <template v-else>
      <el-empty v-if="keywords.length === 0" class="cloud-empty" description="暂无关键词数据" />
      <el-empty v-else-if="filteredKeywords.length === 0" class="cloud-empty" description="未找到匹配的关键词" />
      <div v-else class="cloud-container card-list">
        <span
          v-for="keyword in filteredKeywords"
          :key="keyword.id"
          class="cloud-tag"
          :style="{
            fontSize: heatToSize(keyword.heatScore),
            color: heatToColor(keyword.heatScore),
          }"
          @click="handleClick(keyword)"
        >
          {{ keyword.name }}
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.cloud-hero {
  text-align: center;
  margin-bottom: 2rem;
}

.cloud-hero .hero-kicker {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: var(--el-color-primary);
  margin-bottom: 0.5rem;
}

.cloud-hero h1 {
  font-size: 2rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
}

.cloud-hero .hero-desc {
  color: var(--el-text-color-secondary);
  font-size: 0.95rem;
  margin: 0;
}

.cloud-toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.cloud-search {
  max-width: 320px;
}

.cloud-count {
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.cloud-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 0.75rem 1rem;
  padding: 1.5rem;
}

.cloud-tag {
  display: inline-block;
  cursor: pointer;
  font-weight: 600;
  line-height: 1.4;
  padding: 0.2em 0.5em;
  border-radius: 4px;
  transition: transform 0.2s, background-color 0.2s;
  user-select: none;
}

.cloud-tag:hover {
  transform: scale(1.12);
  background-color: var(--el-fill-color-light);
}
</style>
