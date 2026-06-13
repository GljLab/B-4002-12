<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { List } from '@element-plus/icons-vue'
import type { TocItem } from '../utils/markdown'

const props = defineProps<{
  items: TocItem[]
  contentSelector: string
}>()

const emit = defineEmits<{
  navigate: [id: string]
}>()

const activeId = ref<string | null>(null)
const isVisible = ref(true)
let lastScrollY = 0
let scrollObserver: IntersectionObserver | null = null

function flattenItems(items: TocItem[]): TocItem[] {
  const result: TocItem[] = []
  function traverse(list: TocItem[]) {
    list.forEach((item) => {
      result.push(item)
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(items)
  return result
}

function scrollToHeading(id: string) {
  const contentEl = document.querySelector(props.contentSelector)
  if (!contentEl) return

  const target = contentEl.querySelector(`#${id}`) as HTMLElement
  if (target) {
    const offset = 80
    const targetPosition = target.offsetTop - offset

    window.scrollTo({
      top: targetPosition,
      behavior: 'smooth',
    })

    emit('navigate', id)
  }
}

function setupScrollSpy() {
  const contentEl = document.querySelector(props.contentSelector)
  if (!contentEl) return

  const headings = contentEl.querySelectorAll('.md-heading')
  if (headings.length === 0) return

  const allItems = flattenItems(props.items)

  if (scrollObserver) {
    scrollObserver.disconnect()
  }

  scrollObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          activeId.value = entry.target.id
        }
      })
    },
    {
      rootMargin: '-80px 0px -70% 0px',
      threshold: 0,
    },
  )

  headings.forEach((heading) => {
    scrollObserver?.observe(heading)
  })

  if (allItems.length > 0 && !activeId.value) {
    activeId.value = allItems[0].id
  }
}

function handleScroll() {
  const currentScrollY = window.scrollY
  isVisible.value = currentScrollY <= lastScrollY || currentScrollY < 100
  lastScrollY = currentScrollY
}

onMounted(() => {
  nextTick(() => {
    setupScrollSpy()
  })
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  if (scrollObserver) {
    scrollObserver.disconnect()
  }
  window.removeEventListener('scroll', handleScroll)
})

watch(
  () => props.items,
  () => {
    nextTick(() => {
      setupScrollSpy()
    })
  },
  { deep: true },
)
</script>

<template>
  <div
    v-if="items.length > 0"
    class="toc-container"
    :class="{ 'toc-hidden': !isVisible }"
  >
    <div class="toc-header">
      <el-icon><List /></el-icon>
      <span>目录</span>
    </div>
    <div class="toc-body">
      <template v-for="item in items" :key="item.id">
        <div
          class="toc-item"
          :class="[
            `toc-level-${item.level}`,
            { 'toc-active': activeId === item.id },
          ]"
          @click="scrollToHeading(item.id)"
        >
          <span class="toc-dot"></span>
          <span class="toc-text">{{ item.text }}</span>
        </div>
        <template v-for="child in item.children" :key="child.id">
          <div
            class="toc-item"
            :class="[
              `toc-level-${child.level}`,
              { 'toc-active': activeId === child.id },
            ]"
            @click="scrollToHeading(child.id)"
          >
            <span class="toc-dot"></span>
            <span class="toc-text">{{ child.text }}</span>
          </div>
          <template v-for="grandChild in child.children" :key="grandChild.id">
            <div
              class="toc-item"
              :class="[
                `toc-level-${grandChild.level}`,
                { 'toc-active': activeId === grandChild.id },
              ]"
              @click="scrollToHeading(grandChild.id)"
            >
              <span class="toc-dot"></span>
              <span class="toc-text">{{ grandChild.text }}</span>
            </div>
          </template>
        </template>
      </template>
    </div>
  </div>
</template>

<style scoped>
.toc-container {
  position: fixed;
  right: 24px;
  top: 120px;
  width: 240px;
  max-height: calc(100vh - 160px);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: all var(--motion-base) var(--ease-standard);
  z-index: 100;
}

.toc-container.toc-hidden {
  transform: translateX(calc(100% + 32px));
  opacity: 0;
}

.toc-header {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  font-weight: 600;
  color: var(--color-text-1);
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, transparent 100%);
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
}

.toc-body {
  padding: var(--space-2);
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all var(--motion-fast) var(--ease-standard);
  margin-bottom: 2px;
}

.toc-item:hover {
  background: var(--color-primary-soft);
}

.toc-item.toc-active {
  background: var(--color-primary-soft);
}

.toc-item.toc-active .toc-text {
  color: var(--color-primary);
  font-weight: 500;
}

.toc-item.toc-active .toc-dot {
  background: var(--color-primary);
  transform: scale(1.2);
}

.toc-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border-strong);
  flex-shrink: 0;
  transition: all var(--motion-fast) var(--ease-standard);
}

.toc-text {
  font-size: 13px;
  color: var(--color-text-2);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.toc-level-1 {
  padding-left: 12px;
}

.toc-level-2 {
  padding-left: 24px;
}

.toc-level-3 {
  padding-left: 36px;
  font-size: 12px;
}

.toc-level-4 {
  padding-left: 48px;
  font-size: 12px;
}

.toc-level-5,
.toc-level-6 {
  padding-left: 60px;
  font-size: 12px;
}

.toc-body::-webkit-scrollbar {
  width: 4px;
}

.toc-body::-webkit-scrollbar-track {
  background: transparent;
}

.toc-body::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 2px;
}

.toc-body::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-strong);
}

@media (max-width: 1200px) {
  .toc-container {
    display: none;
  }
}
</style>
