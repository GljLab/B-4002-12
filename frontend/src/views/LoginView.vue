<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)

async function submit() {
  if (!form.username.trim() || !form.password.trim()) {
    ElMessage.warning('用户名和密码不能为空')
    return
  }

  loading.value = true
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    if (redirect) {
      await router.push(redirect)
    } else if (authStore.isAdmin) {
      await router.push('/admin')
    } else if (authStore.isReader) {
      await router.push('/reader')
    } else {
      await router.push('/author')
    }
  } catch {
    ElMessage.error('用户名或密码错误')
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
        <h1>登录</h1>
        <p class="login-desc">管理员和作者使用同一入口登录。</p>
      </div>
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" maxlength="50" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button class="auth-submit-btn" type="primary" :loading="loading" @click="submit">登录</el-button>

        <div class="auth-footer">
          <router-link to="/register">读者注册</router-link>
          <span style="margin-left: 12px"><router-link to="/forgot-password">忘记密码</router-link></span>
        </div>
      </el-form>
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
