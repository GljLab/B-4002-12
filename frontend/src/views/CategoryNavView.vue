<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicCategories } from '../api/categories'
import type { Category } from '../types'

const router = useRouter()
const loading = ref(false)
const categories = ref<Category[]>([])

function countWithChildren(cat: Category): number {
  let total = cat.postCount
  for (const child of cat.children ?? []) {
    if (child.enabled) {
      total += countWithChildren(child)
    }
  }
  return total
}

function filterEnabled(list: Category[]): Category[] {
  return list
    .filter(c => c.enabled)
    .map(c => ({ ...c, children: filterEnabled(c.children ?? []) }))
}

const enabledTree = computed(() => filterEnabled(categories.value))

const topLevelCards = computed(() =>
  enabledTree.value.map(c => ({
    ...c,
    totalCount: countWithChildren(c),
  }))
)

function handleNodeClick(data: Category) {
  router.push({ path: '/', query: { categoryId: String(data.id) } })
}

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await getPublicCategories()
  } catch {
    ElMessage.error('分类加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="view-shell category-shell">
    <section class="home-hero">
      <p class="hero-kicker">CATEGORIES</p>
      <h1>分类导航</h1>
      <p class="hero-desc">按分类浏览文章，快速定位感兴趣的内容。</p>
    </section>

    <el-skeleton v-if="loading" class="skeleton-panel" :rows="6" animated />

    <template v-else>
      <el-empty v-if="enabledTree.length === 0" class="home-empty" description="暂无分类" />

      <template v-else>
        <section class="category-tree-section">
          <el-card class="panel-card tree-card" shadow="never">
            <template #header>
              <span class="panel-title">分类树</span>
            </template>
            <el-tree
              :data="enabledTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              default-expand-all
              :expand-on-click-node="false"
              @node-click="handleNodeClick"
            >
              <template #default="{ data }">
                <span class="tree-node">
                  <span class="tree-node-label">{{ data.name }}</span>
                  <el-badge
                    v-if="countWithChildren(data) > 0"
                    :value="countWithChildren(data)"
                    class="tree-node-badge"
                  />
                </span>
              </template>
            </el-tree>
          </el-card>
        </section>

        <section class="category-cards-section">
          <h2 class="section-heading">分类卡片</h2>
          <div class="card-list category-card-list">
            <el-card
              v-for="(cat, index) in topLevelCards"
              :key="cat.id"
              class="post-card category-card"
              shadow="hover"
              :style="{ '--stagger-index': index }"
            >
              <template #header>
                <router-link
                  :to="{ path: '/', query: { categoryId: String(cat.id) } }"
                  class="title-link"
                >
                  {{ cat.name }}
                </router-link>
              </template>
              <p class="meta">文章数：{{ cat.totalCount }}</p>
              <ul v-if="cat.children.length > 0" class="child-list">
                <li v-for="child in cat.children" :key="child.id" class="child-item">
                  <router-link
                    :to="{ path: '/', query: { categoryId: String(child.id) } }"
                    class="child-link"
                  >
                    {{ child.name }}
                    <span v-if="countWithChildren(child) > 0" class="child-count">
                      ({{ countWithChildren(child) }})
                    </span>
                  </router-link>
                </li>
              </ul>
            </el-card>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<style scoped>
.category-shell {
  gap: var(--space-5);
}

.tree-card {
  max-width: 720px;
}

.tree-card :deep(.el-card__body) {
  padding: var(--space-3) var(--space-4);
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
}

.tree-node-label {
  font-size: 15px;
  font-weight: 550;
  color: var(--color-text-1);
}

.tree-node-badge :deep(.el-badge__content) {
  font-size: 11px;
}

.category-cards-section {
  display: grid;
  gap: var(--space-4);
}

.section-heading {
  margin: 0;
  font-size: clamp(22px, 3vw, 28px);
  color: var(--color-text-1);
  line-height: 1.2;
}

.category-card-list {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.child-list {
  margin: var(--space-2) 0 0;
  padding-left: var(--space-3);
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.child-item {
  font-size: 14px;
}

.child-link {
  color: var(--color-text-2);
  transition: color var(--motion-fast) var(--ease-standard);
}

.child-link:hover {
  color: var(--color-primary);
}

.child-count {
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
