<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAdminAuthors,
  createAuthor,
  updateAuthor,
  resetAuthorPassword,
  disableAuthor,
  enableAuthor,
  deleteAuthor,
} from '../api/authors'
import { useAuthStore } from '../stores/auth'
import type { AuthorDTO, CreateAuthorPayload, UpdateAuthorPayload } from '../types'

const authStore = useAuthStore()
const authors = ref<AuthorDTO[]>([])
const loading = ref(false)

const createDialog = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  username: '',
  password: '',
  nickname: '',
  avatarUrl: '',
  bio: '',
})
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const editDialog = ref(false)
const editLoading = ref(false)
const editingId = ref<number | null>(null)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  nickname: '',
  avatarUrl: '',
  bio: '',
})
const editRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

const resetPwdDialog = ref(false)
const resetPwdLoading = ref(false)
const resetPwdId = ref<number | null>(null)
const resetPwdForm = reactive({ newPassword: '' })

const deleteDialog = ref(false)
const deleteLoading = ref(false)
const deletingAuthor = ref<AuthorDTO | null>(null)
const deleteTransferTo = ref<number | undefined>(undefined)
const deleteUnpublishAll = ref(false)

const otherAuthors = computed(() =>
  authors.value.filter((a) => a.id !== deletingAuthor.value?.id),
)
const deletingHasPosts = computed(() =>
  Boolean(deletingAuthor.value && deletingAuthor.value.postCount > 0),
)

function resetCreateForm() {
  createForm.username = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.avatarUrl = ''
  createForm.bio = ''
  createFormRef.value?.resetFields()
}

function resetEditForm() {
  editForm.nickname = ''
  editForm.avatarUrl = ''
  editForm.bio = ''
  editingId.value = null
  editFormRef.value?.resetFields()
}

function openCreate() {
  resetCreateForm()
  createDialog.value = true
}

function openEdit(row: AuthorDTO) {
  resetEditForm()
  editingId.value = row.id
  editForm.nickname = row.nickname
  editForm.avatarUrl = row.avatarUrl ?? ''
  editForm.bio = row.bio ?? ''
  editDialog.value = true
}

function openResetPwd(row: AuthorDTO) {
  resetPwdId.value = row.id
  resetPwdForm.newPassword = ''
  resetPwdDialog.value = true
}

function openDelete(row: AuthorDTO) {
  deletingAuthor.value = row
  deleteTransferTo.value = undefined
  deleteUnpublishAll.value = false
  deleteDialog.value = true
}

async function loadAuthors() {
  loading.value = true
  try {
    authors.value = await getAdminAuthors()
  } catch {
    ElMessage.error('获取作者列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const payload: CreateAuthorPayload = {
      username: createForm.username.trim(),
      password: createForm.password,
      nickname: createForm.nickname.trim() || undefined,
      avatarUrl: createForm.avatarUrl.trim() || undefined,
      bio: createForm.bio.trim() || undefined,
    }
    await createAuthor(payload)
    ElMessage.success('创建作者成功')
    createDialog.value = false
    await loadAuthors()
  } catch {
    ElMessage.error('创建作者失败')
  } finally {
    createLoading.value = false
  }
}

async function handleEdit() {
  if (editingId.value === null) return
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    const payload: UpdateAuthorPayload = {
      nickname: editForm.nickname.trim() || undefined,
      avatarUrl: editForm.avatarUrl.trim() || undefined,
      bio: editForm.bio.trim() || undefined,
    }
    await updateAuthor(editingId.value, payload)
    ElMessage.success('更新作者成功')
    editDialog.value = false
    await loadAuthors()
  } catch {
    ElMessage.error('更新作者失败')
  } finally {
    editLoading.value = false
  }
}

async function handleResetPwd() {
  if (resetPwdId.value === null) return
  if (!resetPwdForm.newPassword.trim()) {
    ElMessage.warning('请输入新密码')
    return
  }
  resetPwdLoading.value = true
  try {
    await resetAuthorPassword(resetPwdId.value, resetPwdForm.newPassword.trim())
    ElMessage.success('重置密码成功')
    resetPwdDialog.value = false
  } catch {
    ElMessage.error('重置密码失败')
  } finally {
    resetPwdLoading.value = false
  }
}

async function handleToggleEnabled(row: AuthorDTO) {
  try {
    if (row.enabled) {
      await disableAuthor(row.id)
      ElMessage.success('已禁用该作者')
    } else {
      await enableAuthor(row.id)
      ElMessage.success('已启用该作者')
    }
    row.enabled = !row.enabled
  } catch {
    ElMessage.error('切换状态失败')
  }
}

async function handleDelete() {
  if (!deletingAuthor.value) return
  deleteLoading.value = true
  try {
    await deleteAuthor(
      deletingAuthor.value.id,
      deleteUnpublishAll.value ? undefined : deleteTransferTo.value,
    )
    ElMessage.success('删除作者成功')
    deleteDialog.value = false
    await loadAuthors()
  } catch {
    ElMessage.error('删除作者失败')
  } finally {
    deleteLoading.value = false
  }
}

async function handleQuickDelete(row: AuthorDTO) {
  if (row.postCount > 0) {
    openDelete(row)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除作者「${row.nickname || row.username}」吗？`, '确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAuthor(row.id)
    ElMessage.success('删除成功')
    await loadAuthors()
  } catch {
    // cancelled or failed
  }
}

onMounted(loadAuthors)
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>作者管理</h1>
      <p class="admin-subtitle">管理作者账户、权限与内容归属。</p>
    </header>

    <section class="admin-list-section">
      <el-card class="panel-card panel-table">
        <template #header>
          <div class="panel-title-wrap">
            <strong class="panel-title">作者列表</strong>
            <el-button type="primary" @click="openCreate">创建作者</el-button>
          </div>
        </template>

        <el-table class="admin-table" v-loading="loading" :data="authors" stripe>
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column label="头像" width="80" align="center">
            <template #default="{ row }">
              <el-avatar v-if="row.avatarUrl" :src="row.avatarUrl" :size="32" />
              <el-avatar v-else :size="32">{{ (row.nickname || row.username).charAt(0) }}</el-avatar>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="handleToggleEnabled(row)"
                :disabled="row.id === authStore.user?.id"
              />
            </template>
          </el-table-column>
          <el-table-column prop="postCount" label="文章数" width="90" align="center" />
          <el-table-column label="最后登录" min-width="160">
            <template #default="{ row }">
              {{ row.lastLoginAt ? dayjs(row.lastLoginAt).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <el-button class="action-btn" type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button class="action-btn" type="warning" link @click="openResetPwd(row)">重置密码</el-button>
              <el-button
                class="action-btn"
                type="danger"
                link
                @click="handleQuickDelete(row)"
                :disabled="row.id === authStore.user?.id"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <el-dialog v-model="createDialog" title="创建作者" width="520px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="头像URL" prop="avatarUrl">
          <el-input v-model="createForm.avatarUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="简介" prop="bio">
          <el-input v-model="createForm.bio" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑作者" width="480px" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="头像URL" prop="avatarUrl">
          <el-input v-model="editForm.avatarUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="简介" prop="bio">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdDialog" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deleteDialog" title="删除作者" width="520px">
      <p v-if="deletingAuthor">
        确定删除作者「{{ deletingAuthor.nickname || deletingAuthor.username }}」吗？
      </p>
      <template v-if="deletingHasPosts">
        <el-divider />
        <p class="meta">该作者有 {{ deletingAuthor!.postCount }} 篇已发布文章，请选择处理方式：</p>
        <el-form label-width="100px" style="margin-top: 12px">
          <el-form-item label="文章处理">
            <el-radio-group v-model="deleteUnpublishAll">
              <el-radio :value="false">转移给其他作者</el-radio>
              <el-radio :value="true">取消发布所有文章</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="!deleteUnpublishAll" label="目标作者">
            <el-select v-model="deleteTransferTo" placeholder="请选择目标作者" style="width: 100%">
              <el-option
                v-for="a in otherAuthors"
                :key="a.id"
                :label="a.nickname || a.username"
                :value="a.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="deleteDialog = false">取消</el-button>
        <el-button
          type="danger"
          :loading="deleteLoading"
          :disabled="deletingHasPosts && !deleteUnpublishAll && deleteTransferTo == null"
          @click="handleDelete"
        >确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.meta {
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
