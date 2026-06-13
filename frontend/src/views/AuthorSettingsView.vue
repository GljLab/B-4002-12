<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuthorProfile, updateAuthorProfile, changePassword } from '../api/posts'
import type { AuthorDTO } from '../types'

const profileLoading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profile = reactive({
  nickname: '',
  avatarUrl: '',
  bio: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function loadProfile() {
  profileLoading.value = true
  try {
    const data: AuthorDTO = await getAuthorProfile()
    profile.nickname = data.nickname ?? ''
    profile.avatarUrl = data.avatarUrl ?? ''
    profile.bio = data.bio ?? ''
  } catch {
    ElMessage.error('获取个人信息失败')
  } finally {
    profileLoading.value = false
  }
}

async function saveProfile() {
  profileSaving.value = true
  try {
    await updateAuthorProfile({
      nickname: profile.nickname || undefined,
      avatarUrl: profile.avatarUrl || undefined,
      bio: profile.bio || undefined,
    })
    ElMessage.success('个人信息已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写当前密码和新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6个字符')
    return
  }
  passwordSaving.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <h1>个人设置</h1>
      <p class="author-subtitle">管理你的个人信息和账号安全。</p>
    </header>

    <el-card class="panel-card" v-loading="profileLoading">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">个人资料</strong>
        </div>
      </template>
      <el-form class="settings-form" label-position="top" @submit.prevent>
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>
        <el-form-item label="头像链接">
          <el-input v-model="profile.avatarUrl" placeholder="请输入头像URL" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="profile.bio"
            type="textarea"
            :rows="4"
            placeholder="请输入个人简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">修改密码</strong>
        </div>
      </template>
      <el-form class="settings-form" label-position="top" @submit.prevent>
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（至少6位）"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.author-header h1 {
  margin: 0;
  font-size: clamp(28px, 3.4vw, 34px);
  line-height: 1.2;
}

.author-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-3);
  font-size: 15px;
  line-height: 1.65;
}

.panel-card {
  width: 100%;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--color-text-1);
}

.settings-form .el-form-item {
  margin-bottom: var(--space-4);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-2);
}
</style>
