<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSecurityQuestion, resetPassword } from '../api/reader'

const router = useRouter()
const step = ref(1)
const loading = ref(false)

const form = reactive({
  username: '',
  securityAnswer: '',
  newPassword: '',
  confirmPassword: '',
  securityQuestion: '',
})

const passwordError = computed(() => {
  if (!form.newPassword) return ''
  if (form.newPassword.length < 8) return '密码不能少于8位'
  if (!/[a-z]/.test(form.newPassword)) return '必须包含小写字母'
  if (!/[A-Z]/.test(form.newPassword)) return '必须包含大写字母'
  if (!/\d/.test(form.newPassword)) return '必须包含数字'
  return ''
})

const confirmError = computed(() => {
  if (!form.confirmPassword) return ''
  if (form.confirmPassword !== form.newPassword) return '两次密码不一致'
  return ''
})

async function fetchQuestion() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  loading.value = true
  try {
    const result = await getSecurityQuestion(form.username.trim())
    form.securityQuestion = result.securityQuestion
    step.value = 2
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '无法获取安全问题')
  } finally {
    loading.value = false
  }
}

async function submitReset() {
  if (passwordError.value || confirmError.value) {
    ElMessage.warning('请检查密码格式')
    return
  }
  loading.value = true
  try {
    await resetPassword({
      username: form.username.trim(),
      securityAnswer: form.securityAnswer,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功，请登录')
    await router.push('/login')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="view-shell login-shell">
    <el-card class="login-card">
      <div class="login-card-head">
        <p class="login-kicker">BLOG</p>
        <h1>找回密码</h1>
        <p class="login-desc">通过安全问题验证身份后重置密码。</p>
      </div>

      <div v-if="step === 1" class="auth-form">
        <el-form label-position="top" @submit.prevent="fetchQuestion">
          <el-form-item label="用户名">
            <el-input v-model="form.username" maxlength="50" placeholder="请输入注册时的用户名" />
          </el-form-item>
          <el-button class="auth-submit-btn" type="primary" :loading="loading" @click="fetchQuestion">下一步</el-button>
        </el-form>
      </div>

      <div v-else-if="step === 2" class="auth-form">
        <el-form label-position="top" @submit.prevent="submitReset">
          <el-form-item label="安全问题">
            <el-input :model-value="form.securityQuestion" disabled />
          </el-form-item>
          <el-form-item label="安全答案">
            <el-input v-model="form.securityAnswer" maxlength="255" placeholder="请输入你设置的答案" />
          </el-form-item>
          <el-form-item label="新密码" :error="passwordError">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="至少8位，须含大小写字母和数字" />
          </el-form-item>
          <el-form-item label="确认新密码" :error="confirmError">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-button class="auth-submit-btn" type="primary" :loading="loading" @click="submitReset">重置密码</el-button>
        </el-form>
      </div>

      <div class="auth-footer">
        <router-link to="/login">返回登录</router-link>
        <span style="margin-left: 12px">没有账号？<router-link to="/register">去注册</router-link></span>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-footer {
  text-align: center;
  margin-top: var(--space-3);
  color: var(--color-text-3);
  font-size: 14px;
}
.auth-footer a {
  color: var(--color-primary);
  font-weight: 500;
}
</style>
