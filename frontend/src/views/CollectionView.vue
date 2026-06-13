<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAlbums,
  createAlbum,
  updateAlbum,
  deleteAlbum,
  listAlbumItems,
  addItemToAlbum,
  removeItemFromAlbum,
  reorderAlbumItems,
  batchCollectionAction,
  exportAlbum,
  getFavoriteRanking,
} from '../api/collections'
import type {
  CollectionAlbum,
  CollectionItem,
  CreateAlbumPayload,
  UpdateAlbumPayload,
  AddToAlbumPayload,
  BatchCollectionActionPayload,
  PostFavoriteCount,
} from '../types'

const activeTab = ref('albums')
const albums = ref<CollectionAlbum[]>([])
const albumsLoading = ref(false)

const selectedAlbum = ref<CollectionAlbum | null>(null)
const items = ref<CollectionItem[]>([])
const itemsLoading = ref(false)

const rankingList = ref<PostFavoriteCount[]>([])
const rankingLoading = ref(false)

const albumDialogVisible = ref(false)
const albumDialogMode = ref<'create' | 'edit'>('create')
const albumForm = ref<{ name: string; description: string; coverUrl: string; isPublic: boolean }>({
  name: '',
  description: '',
  coverUrl: '',
  isPublic: false,
})

const addItemDialogVisible = ref(false)
const addItemForm = ref<{ postId: number | null; note: string }>({ postId: null, note: '' })

const batchDialogVisible = ref(false)
const batchAction = ref<'transfer' | 'remove'>('transfer')
const batchTargetAlbumId = ref<number | null>(null)
const selectedRowIds = ref<number[]>([])

const dragIndex = ref<number | null>(null)

const editingAlbum = computed(() =>
  albumDialogMode.value === 'edit' ? selectedAlbum.value : null,
)

async function loadAlbums() {
  albumsLoading.value = true
  try {
    albums.value = await listAlbums()
  } catch {
    ElMessage.error('加载收藏集失败')
  } finally {
    albumsLoading.value = false
  }
}

async function loadItems() {
  if (!selectedAlbum.value) return
  itemsLoading.value = true
  try {
    items.value = await listAlbumItems(selectedAlbum.value.id)
  } catch {
    ElMessage.error('加载收藏项失败')
  } finally {
    itemsLoading.value = false
  }
}

async function loadRanking() {
  rankingLoading.value = true
  try {
    rankingList.value = await getFavoriteRanking(20)
  } catch {
    ElMessage.error('加载排行失败')
  } finally {
    rankingLoading.value = false
  }
}

function openCreateDialog() {
  albumDialogMode.value = 'create'
  albumForm.value = { name: '', description: '', coverUrl: '', isPublic: false }
  albumDialogVisible.value = true
}

function openEditDialog(album: CollectionAlbum) {
  albumDialogMode.value = 'edit'
  albumForm.value = {
    name: album.name,
    description: album.description || '',
    coverUrl: album.coverUrl || '',
    isPublic: album.isPublic,
  }
  albumDialogVisible.value = true
}

async function handleAlbumSubmit() {
  if (!albumForm.value.name.trim()) {
    ElMessage.warning('请输入收藏集名称')
    return
  }
  try {
    if (albumDialogMode.value === 'create') {
      const payload: CreateAlbumPayload = {
        name: albumForm.value.name.trim(),
        description: albumForm.value.description.trim() || undefined,
        coverUrl: albumForm.value.coverUrl.trim() || undefined,
        isPublic: albumForm.value.isPublic,
      }
      await createAlbum(payload)
      ElMessage.success('创建成功')
    } else if (selectedAlbum.value) {
      const payload: UpdateAlbumPayload = {
        name: albumForm.value.name.trim(),
        description: albumForm.value.description.trim() || undefined,
        coverUrl: albumForm.value.coverUrl.trim() || undefined,
        isPublic: albumForm.value.isPublic,
      }
      await updateAlbum(selectedAlbum.value.id, payload)
      ElMessage.success('更新成功')
    }
    albumDialogVisible.value = false
    await loadAlbums()
    if (selectedAlbum.value && albumDialogMode.value === 'edit') {
      const updated = albums.value.find((a) => a.id === selectedAlbum.value!.id)
      if (updated) selectedAlbum.value = updated
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteAlbum(album: CollectionAlbum) {
  try {
    await ElMessageBox.confirm(`确定删除收藏集「${album.name}」？此操作不可撤销。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAlbum(album.id)
    ElMessage.success('已删除')
    if (selectedAlbum.value?.id === album.id) {
      selectedAlbum.value = null
      items.value = []
    }
    await loadAlbums()
  } catch {
    // cancelled or error
  }
}

function selectAlbum(album: CollectionAlbum) {
  selectedAlbum.value = album
  activeTab.value = 'detail'
  selectedRowIds.value = []
  loadItems()
}

function openAddItemDialog() {
  addItemForm.value = { postId: null, note: '' }
  addItemDialogVisible.value = true
}

async function handleAddItem() {
  if (!selectedAlbum.value || !addItemForm.value.postId) {
    ElMessage.warning('请输入文章ID')
    return
  }
  try {
    const payload: AddToAlbumPayload = {
      postId: addItemForm.value.postId,
      note: addItemForm.value.note.trim() || undefined,
    }
    await addItemToAlbum(selectedAlbum.value.id, payload)
    ElMessage.success('已添加')
    addItemDialogVisible.value = false
    await loadItems()
    await loadAlbums()
  } catch {
    ElMessage.error('添加失败')
  }
}

async function handleRemoveItem(item: CollectionItem) {
  if (!selectedAlbum.value) return
  try {
    await ElMessageBox.confirm('确定移除此项？', '确认', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await removeItemFromAlbum(selectedAlbum.value.id, item.postId)
    ElMessage.success('已移除')
    await loadItems()
    await loadAlbums()
  } catch {
    // cancelled
  }
}

function onDragStart(index: number) {
  dragIndex.value = index
}

function onDragOver(e: DragEvent) {
  e.preventDefault()
}

function onDrop(targetIndex: number) {
  if (dragIndex.value === null || dragIndex.value === targetIndex) {
    dragIndex.value = null
    return
  }
  const reordered = [...items.value]
  const [moved] = reordered.splice(dragIndex.value, 1)
  reordered.splice(targetIndex, 0, moved)
  items.value = reordered
  dragIndex.value = null
  persistReorder(reordered)
}

async function persistReorder(reordered: CollectionItem[]) {
  if (!selectedAlbum.value) return
  try {
    const itemIds = reordered.map((i) => i.id)
    await reorderAlbumItems(selectedAlbum.value.id, { itemIds } as any)
  } catch {
    ElMessage.error('排序保存失败')
    await loadItems()
  }
}

function toggleRowSelection(itemId: number) {
  const idx = selectedRowIds.value.indexOf(itemId)
  if (idx >= 0) {
    selectedRowIds.value.splice(idx, 1)
  } else {
    selectedRowIds.value.push(itemId)
  }
}

function toggleSelectAll() {
  if (selectedRowIds.value.length === items.value.length) {
    selectedRowIds.value = []
  } else {
    selectedRowIds.value = items.value.map((i) => i.id)
  }
}

function openBatchDialog(action: 'transfer' | 'remove') {
  if (selectedRowIds.value.length === 0) {
    ElMessage.warning('请先选择项目')
    return
  }
  batchAction.value = action
  batchTargetAlbumId.value = null
  batchDialogVisible.value = true
}

async function handleBatchAction() {
  if (!selectedAlbum.value) return
  if (batchAction.value === 'transfer' && !batchTargetAlbumId.value) {
    ElMessage.warning('请选择目标收藏集')
    return
  }
  try {
    const payload: BatchCollectionActionPayload = {
      itemIds: selectedRowIds.value,
      targetAlbumId: batchAction.value === 'transfer' ? batchTargetAlbumId.value! : undefined,
      action: batchAction.value,
    }
    await batchCollectionAction(payload)
    ElMessage.success('操作成功')
    batchDialogVisible.value = false
    selectedRowIds.value = []
    await loadItems()
    await loadAlbums()
  } catch {
    ElMessage.error('批量操作失败')
  }
}

async function handleExport(format: string) {
  if (!selectedAlbum.value) return
  try {
    const blob = await exportAlbum(selectedAlbum.value.id, format)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const ext = format === 'pdf' ? 'pdf' : 'md'
    a.download = `${selectedAlbum.value.name}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

function copyShareLink() {
  if (!selectedAlbum.value?.shareToken) return
  const link = `${window.location.origin}/collections/public/${selectedAlbum.value.shareToken}`
  navigator.clipboard.writeText(link).then(() => {
    ElMessage.success('分享链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const transferableAlbums = computed(() =>
  albums.value.filter((a) => a.id !== selectedAlbum.value?.id),
)

function handleTabChange(tab: string) {
  if (tab === 'ranking') {
    loadRanking()
  }
}

onMounted(() => {
  loadAlbums()
})
</script>

<template>
  <div class="view-shell reader-shell collection-shell">
    <section class="collection-header">
      <div>
        <h1 class="panel-title">我的收藏集</h1>
        <p class="panel-meta">管理你的珍藏专辑，归类喜爱的文章</p>
      </div>
    </section>

    <el-card class="panel-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="收藏专辑" name="albums">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openCreateDialog">新建专辑</el-button>
          </div>
          <div v-loading="albumsLoading" class="album-grid">
            <el-empty v-if="!albumsLoading && albums.length === 0" description="暂无收藏专辑" />
            <div
              v-for="album in albums"
              :key="album.id"
              class="album-card"
              :class="{ 'album-card-active': selectedAlbum?.id === album.id }"
              @click="selectAlbum(album)"
            >
              <div class="album-cover">
                <img v-if="album.coverUrl" :src="album.coverUrl" :alt="album.name" />
                <div v-else class="album-cover-placeholder">
                  <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z"/><path d="M8 10v4"/><path d="M12 10v2"/><path d="M16 10v6"/></svg>
                </div>
                <el-tag
                  v-if="album.isPublic"
                  class="album-public-badge"
                  type="success"
                  size="small"
                >公开</el-tag>
                <el-tag v-else class="album-public-badge" type="info" size="small">私密</el-tag>
              </div>
              <div class="album-info">
                <h3 class="album-name">{{ album.name }}</h3>
                <p v-if="album.description" class="album-desc">{{ album.description }}</p>
                <span class="album-count">{{ album.itemCount }} 篇文章</span>
              </div>
              <div class="album-actions" @click.stop>
                <el-button link size="small" @click="openEditDialog(album)">编辑</el-button>
                <el-button link size="small" type="danger" @click="handleDeleteAlbum(album)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="专辑详情" name="detail" :disabled="!selectedAlbum">
          <template v-if="selectedAlbum">
            <div class="detail-toolbar">
              <div class="detail-toolbar-left">
                <el-button size="small" @click="activeTab = 'albums'">← 返回列表</el-button>
                <span class="detail-album-name">{{ selectedAlbum.name }}</span>
                <el-tag v-if="selectedAlbum.isPublic" type="success" size="small">公开</el-tag>
              </div>
              <div class="detail-toolbar-right">
                <el-button size="small" @click="openAddItemDialog">添加文章</el-button>
                <el-button size="small" :disabled="selectedRowIds.length === 0" @click="openBatchDialog('transfer')">批量转移</el-button>
                <el-button size="small" type="danger" :disabled="selectedRowIds.length === 0" @click="openBatchDialog('remove')">批量移除</el-button>
                <el-button size="small" @click="handleExport('markdown')">导出 Markdown</el-button>
                <el-button size="small" @click="handleExport('pdf')">导出 PDF</el-button>
                <el-button v-if="selectedAlbum.isPublic && selectedAlbum.shareToken" size="small" @click="copyShareLink">复制分享链接</el-button>
              </div>
            </div>

            <div v-loading="itemsLoading" class="items-section">
              <el-empty v-if="!itemsLoading && items.length === 0" description="此专辑暂无文章" />
              <div v-else class="items-table-wrapper">
                <div class="items-table-header">
                  <el-checkbox
                    :model-value="selectedRowIds.length === items.length && items.length > 0"
                    :indeterminate="selectedRowIds.length > 0 && selectedRowIds.length < items.length"
                    @change="toggleSelectAll"
                  />
                  <span class="th-title">文章</span>
                  <span class="th-author">作者</span>
                  <span class="th-note">备注</span>
                  <span class="th-actions">操作</span>
                </div>
                <div
                  v-for="(item, index) in items"
                  :key="item.id"
                  class="item-row"
                  :class="{ 'item-row-dragover': dragIndex !== null && dragIndex !== index }"
                  draggable="true"
                  @dragstart="onDragStart(index)"
                  @dragover="onDragOver"
                  @drop="onDrop(index)"
                >
                  <el-checkbox
                    :model-value="selectedRowIds.includes(item.id)"
                    @change="toggleRowSelection(item.id)"
                  />
                  <div class="item-title-cell">
                    <span class="item-title" :class="{ 'item-deleted': item.postDeleted }">
                      {{ item.postTitle }}
                    </span>
                    <el-tag v-if="item.postDeleted" type="danger" size="small" class="deleted-tag">已删除</el-tag>
                    <p class="item-excerpt">{{ item.postExcerpt }}</p>
                  </div>
                  <span class="item-author">{{ item.postAuthorName }}</span>
                  <span class="item-note">{{ item.note || '—' }}</span>
                  <div class="item-row-actions">
                    <el-button link size="small" type="danger" @click="handleRemoveItem(item)">移除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="请从专辑列表中选择一个专辑" />
        </el-tab-pane>

        <el-tab-pane label="最受青睐" name="ranking">
          <div v-loading="rankingLoading" class="ranking-section">
            <el-empty v-if="!rankingLoading && rankingList.length === 0" description="暂无排行数据" />
            <div v-else class="ranking-list">
              <div
                v-for="(post, idx) in rankingList"
                :key="post.postId"
                class="ranking-item"
              >
                <span class="ranking-index" :class="{ 'ranking-top': idx < 3 }">{{ idx + 1 }}</span>
                <span class="ranking-title">{{ post.postTitle }}</span>
                <span class="ranking-count">{{ post.favoriteCount }} 次收藏</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="albumDialogVisible"
      :title="albumDialogMode === 'create' ? '新建专辑' : '编辑专辑'"
      width="480px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="albumForm.name" placeholder="输入专辑名称" maxlength="60" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="albumForm.description" type="textarea" :rows="3" placeholder="简短描述（可选）" />
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="albumForm.coverUrl" placeholder="https://example.com/cover.jpg" />
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="albumForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="albumDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlbumSubmit">
          {{ albumDialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addItemDialogVisible" title="添加文章到专辑" width="420px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="文章 ID" required>
          <el-input v-model.number="addItemForm.postId" type="number" placeholder="输入文章ID" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addItemForm.note" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addItemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddItem">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" :title="batchAction === 'transfer' ? '批量转移' : '批量移除'" width="420px" destroy-on-close>
      <template v-if="batchAction === 'transfer'">
        <p class="batch-info">已选择 {{ selectedRowIds.length }} 项，转移到：</p>
        <el-select v-model="batchTargetAlbumId" placeholder="选择目标专辑" style="width: 100%">
          <el-option
            v-for="album in transferableAlbums"
            :key="album.id"
            :label="album.name"
            :value="album.id"
          />
        </el-select>
      </template>
      <template v-else>
        <p class="batch-info">确定移除已选的 {{ selectedRowIds.length }} 项？此操作不可撤销。</p>
      </template>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchAction">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.collection-shell {
  gap: var(--space-4);
}

.collection-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--space-3);
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-4);
}

.album-card {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--motion-base) var(--ease-standard),
    box-shadow var(--motion-base) var(--ease-standard),
    border-color var(--motion-base) var(--ease-standard);
}

.album-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(139, 92, 246, 0.5);
}

.album-card-active {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md), 0 0 0 2px var(--color-primary-soft);
}

.album-cover {
  position: relative;
  height: 140px;
  background: var(--color-page-2);
  overflow: hidden;
}

.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
}

.album-public-badge {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
}

.album-info {
  padding: var(--space-3);
  flex: 1;
}

.album-name {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  color: var(--color-text-1);
  line-height: 1.3;
}

.album-desc {
  margin: var(--space-1) 0 0;
  font-size: 13px;
  color: var(--color-text-3);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.album-count {
  margin-top: var(--space-1);
  display: inline-block;
  font-size: 12px;
  color: var(--color-text-3);
  background: var(--color-page-2);
  padding: 2px 8px;
  border-radius: 999px;
}

.album-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-1);
  padding: 0 var(--space-3) var(--space-3);
}

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.detail-toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.detail-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.detail-album-name {
  font-weight: 650;
  font-size: 16px;
  color: var(--color-text-1);
}

.items-section {
  min-height: 120px;
}

.items-table-wrapper {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.items-table-header {
  display: grid;
  grid-template-columns: 40px 1fr 100px 120px 80px;
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-2) var(--space-3);
  background: #f4f8ff;
  font-size: 13px;
  font-weight: 620;
  color: #1f3555;
}

.item-row {
  display: grid;
  grid-template-columns: 40px 1fr 100px 120px 80px;
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-2) var(--space-3);
  border-top: 1px solid rgba(180, 201, 228, 0.45);
  background: #fff;
  transition: background var(--motion-fast) var(--ease-standard);
  cursor: grab;
}

.item-row:hover {
  background: #f6f9ff;
}

.item-row:active {
  cursor: grabbing;
}

.item-row-dragover {
  border-top: 2px solid var(--color-primary);
}

.item-title-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-title {
  font-weight: 550;
  color: var(--color-text-1);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-title.item-deleted {
  text-decoration: line-through;
  color: var(--color-text-3);
}

.deleted-tag {
  width: fit-content;
}

.item-excerpt {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-3);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-author {
  font-size: 13px;
  color: var(--color-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-note {
  font-size: 13px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-row-actions {
  display: flex;
  gap: var(--space-1);
}

.th-title,
.th-author,
.th-note,
.th-actions {
  font-size: 13px;
  font-weight: 620;
  color: #1f3555;
}

.ranking-section {
  min-height: 120px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  transition: background var(--motion-fast) var(--ease-standard);
}

.ranking-item:hover {
  background: var(--color-primary-soft);
}

.ranking-index {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-3);
  background: var(--color-page-2);
  flex-shrink: 0;
}

.ranking-index.ranking-top {
  background: var(--color-primary);
  color: #fff;
}

.ranking-title {
  flex: 1;
  font-weight: 550;
  color: var(--color-text-1);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-count {
  font-size: 13px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.batch-info {
  margin: 0 0 var(--space-3);
  color: var(--color-text-2);
  font-size: 14px;
}

@media (max-width: 768px) {
  .album-grid {
    grid-template-columns: 1fr;
  }

  .detail-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .items-table-header {
    display: none;
  }

  .item-row {
    grid-template-columns: 40px 1fr;
    grid-template-rows: auto auto;
  }

  .item-author,
  .item-note {
    grid-column: 2;
  }

  .item-row-actions {
    grid-column: 2;
    justify-content: flex-end;
  }
}
</style>
