<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ZoomIn, ZoomOut, RefreshRight, RefreshLeft, Close, Loading } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  imageUrl: string
  imageAlt?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
}>()

const isVisible = ref(props.visible)
const loading = ref(false)
const scale = ref(1)
const rotation = ref(0)

watch(
  () => props.visible,
  (val) => {
    isVisible.value = val
    if (val) {
      loading.value = true
      scale.value = 1
      rotation.value = 0
    }
  },
)

function handleClose() {
  isVisible.value = false
  emit('update:visible', false)
  emit('close')
}

function handleImageLoad() {
  loading.value = false
}

function handleZoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
}

function handleZoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5)
}

function handleRotate() {
  rotation.value = (rotation.value + 90) % 360
}

function handleReset() {
  scale.value = 1
  rotation.value = 0
}

function handleKeydown(e: KeyboardEvent) {
  if (!isVisible.value) return

  if (e.key === 'Escape') {
    handleClose()
  } else if (e.key === '+' || e.key === '=') {
    handleZoomIn()
  } else if (e.key === '-') {
    handleZoomOut()
  } else if (e.key === 'r' || e.key === 'R') {
    handleRotate()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="preview-fade">
      <div v-if="isVisible" class="image-preview-overlay" @click.self="handleClose">
        <div class="preview-toolbar">
          <div class="preview-title">{{ imageAlt || '图片预览' }}</div>
          <div class="preview-actions">
            <el-button size="small" circle @click.stop="handleZoomOut">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <span class="zoom-value">{{ Math.round(scale * 100) }}%</span>
            <el-button size="small" circle @click.stop="handleZoomIn">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
            <el-button size="small" circle @click.stop="handleRotate">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button size="small" circle @click.stop="handleReset">
              <el-icon><RefreshLeft /></el-icon>
            </el-button>
            <el-button size="small" circle @click.stop="handleClose">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="preview-content">
          <div v-if="loading" class="preview-loading">
            <el-icon class="is-loading" :size="48"><Loading /></el-icon>
            <p>图片加载中...</p>
          </div>
          <img
            :src="imageUrl"
            :alt="imageAlt"
            class="preview-image"
            :style="{
              transform: `scale(${scale}) rotate(${rotation}deg)`,
            }"
            @load="handleImageLoad"
            @click.stop
          />
        </div>

        <div class="preview-tips">
          <span>ESC 关闭</span>
          <span>+ / - 缩放</span>
          <span>R 旋转</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3000;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.preview-toolbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
}

.preview-title {
  font-size: 16px;
  font-weight: 500;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zoom-value {
  font-size: 14px;
  min-width: 60px;
  text-align: center;
}

.preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 80px 40px;
  overflow: hidden;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  cursor: grab;
  transition: transform 0.2s ease;
  user-select: none;
}

.preview-image:active {
  cursor: grabbing;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #aaa;
}

.preview-tips {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 24px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.3s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}
</style>
