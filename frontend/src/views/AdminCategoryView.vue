<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAdminCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  toggleCategoryEnabled,
} from '../api/categories'
import type { Category, CreateCategoryPayload, UpdateCategoryPayload } from '../types'

const categories = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  slug: '',
  parentId: null as number | null,
  sortOrder: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

const dialogTitle = computed(() => (editingId.value !== null ? '编辑分类' : '新建分类'))

const flatCategories = computed(() => {
  const result: Category[] = []
  function walk(list: Category[]) {
    for (const c of list) {
      result.push(c)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(categories.value)
  return result
})

const treeSelectData = computed(() => {
  if (editingId.value === null) return categories.value
  function filterSelf(list: Category[]): Category[] {
    return list
      .filter((c) => c.id !== editingId.value)
      .map((c) => ({
        ...c,
        children: c.children?.length ? filterSelf(c.children) : [],
      }))
  }
  return filterSelf(categories.value)
})

function resetForm() {
  form.name = ''
  form.slug = ''
  form.parentId = null
  form.sortOrder = 0
  editingId.value = null
  formRef.value?.resetFields()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Category) {
  resetForm()
  editingId.value = row.id
  form.name = row.name
  form.slug = row.slug
  form.parentId = row.parentId
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await getAdminCategories()
  } catch {
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    if (editingId.value !== null) {
      const payload: UpdateCategoryPayload = {
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        parentId: form.parentId,
        sortOrder: form.sortOrder,
      }
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类更新成功')
    } else {
      const payload: CreateCategoryPayload = {
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        parentId: form.parentId,
        sortOrder: form.sortOrder,
      }
      await createCategory(payload)
      ElMessage.success('分类创建成功')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.response?.data?.code || ''
    if (msg.includes('cycle') || msg.includes('CYCLE') || msg.includes('循环')) {
      ElMessage.error('操作失败：父分类设置会产生循环引用')
    } else {
      ElMessage.error(editingId.value !== null ? '更新分类失败' : '创建分类失败')
    }
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    await loadCategories()
  } catch (err: any) {
    const code = err?.response?.data?.code || ''
    if (code === 'HAS_CHILDREN') {
      ElMessage.error('该分类下存在子分类，无法删除')
    } else {
      ElMessage.error('删除分类失败')
    }
  }
}

async function handleToggle(row: Category) {
  try {
    await toggleCategoryEnabled(row.id)
    row.enabled = !row.enabled
  } catch {
    ElMessage.error('切换状态失败')
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>分类管理</h1>
      <p class="admin-subtitle">管理文章分类的层级结构、排序与启用状态。</p>
    </header>

    <section class="admin-list-section">
      <el-card class="panel-card panel-table">
        <template #header>
          <div class="panel-title-wrap">
            <strong class="panel-title">分类列表</strong>
            <el-button type="primary" @click="openCreate">新建分类</el-button>
          </div>
        </template>

        <el-table
          class="admin-table"
          v-loading="loading"
          :data="categories"
          row-key="id"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          default-expand-all
          stripe
        >
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column prop="slug" label="Slug" min-width="140" />
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="handleToggle(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="postCount" label="文章数" width="90" align="center" />
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button class="action-btn" type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button class="action-btn" type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="Slug" prop="slug">
          <el-input v-model="form.slug" placeholder="可选，留空则自动生成" />
        </el-form-item>
        <el-form-item label="父分类" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="无（顶级分类）"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
