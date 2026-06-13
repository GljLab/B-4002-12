<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, registerReader } from '../api/reader'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  captchaCode: '',
  securityQuestion: '',
  securityAnswer: '',
})
const captchaKey = ref('')
const captchaImg = ref('')
const loading = ref(false)
const captchaLoading = ref(false)

const usernameError = computed(() => {
  if (!form.username) return ''
  if (form.username.length < 2) return '用户名至少2个字符'
  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(form.username)) return '只能包含字母、数字、下划线和中文'
  return ''
})

const passwordError = computed(() => {
  if (!form.password) return ''
  if (form.password.length < 8) return '密码不能少于8位'
  if (!/[a-z]/.test(form.password)) return '必须包含小写字母'
  if (!/[A-Z]/.test(form.password)) return '必须包含大写字母'
  if (!/\d/.test(form.password)) return '必须包含数字'
  return ''
})

const confirmError = computed(() => {
  if (!form.confirmPassword) return ''
  if (form.confirmPassword !== form.password) return '两次密码不一致'
  return ''
})

const canSubmit = computed(() => {
  return form.username && !usernameError.value
    && form.password && !passwordError.value
    && form.confirmPassword && !confirmError.value
    && form.captchaCode
})

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const result = await getCaptcha()
    captchaKey.value = result.key
    captchaImg.value = result.svgDataUrl
  } catch {
    ElMessage.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  if (!canSubmit.value) {
    ElMessage.warning('请完善表单信息')
    return
  }
  loading.value = true
  try {
    await registerReader({
      username: form.username.trim(),
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode.trim(),
      nickname: form.nickname.trim() || undefined,
      securityQuestion: form.securityQuestion.trim() || undefined,
      securityAnswer: form.securityAnswer.trim() || undefined,
    })
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } catch (e: any) {
    const msg = e?.response?.data?.message || '注册失败'
    ElMessage.error(msg)
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<template>
  <div class="view-shell login-shell">
    <el-card class="login-card">
      <div class="login-card-head">
        <p class="login-kicker">BLOG</p>
        <h1>读者注册</h1>
        <p class="login-desc">创建你的读者账号，开启个性化阅读之旅。</p>
      </div>
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" :error="usernameError">
          <el-input v-model="form.username" maxlength="50" placeholder="2-50位，字母/数字/下划线/中文" />
        </el-form-item>

        <el-form-item label="密码" :error="passwordError">
          <el-input v-model="form.password" type="password" show-password placeholder="至少8位，须含大小写字母和数字" />
          <div v-if="form.password" class="pwd-rules">
            <span :class="{ met: form.password.length >= 8 }">8位以上</span>
            <span :class="{ met: /[a-z]/.test(form.password) }">小写字母</span>
            <span :class="{ met: /[A-Z]/.test(form.password) }">大写字母</span>
            <span :class="{ met: /\d/.test(form.password) }">数字</span>
          </div>
        </el-form-item>

        <el-form-item label="确认密码" :error="confirmError">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>

        <el-form-item label="昵称（选填）">
          <el-input v-model="form.nickname" maxlength="100" placeholder="显示名称，不填则用用户名" />
        </el-form-item>

        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" maxlength="4" placeholder="输入右侧验证码" class="captcha-input" />
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" class="captcha-img" @click="refreshCaptcha" />
            <el-button :loading="captchaLoading" @click="refreshCaptcha">刷新</el-button>
          </div>
        </el-form-item>

        <el-divider>安全设置（选填，用于找回密码）</el-divider>

        <el-form-item label="安全问题">
          <el-input v-model="form.securityQuestion" maxlength="200" placeholder="例如：你的小学名称？" />
        </el-form-item>

        <el-form-item label="安全答案">
          <el-input v-model="form.securityAnswer" maxlength="255" placeholder="请牢记此答案" />
        </el-form-item>

        <el-button class="auth-submit-btn" type="primary" :loading="loading" :disabled="!canSubmit" @click="submit">注册</el-button>

        <div class="auth-footer">
          已有账号？<router-link to="/login">去登录</router-link>
          <span style="margin-left: 12px">忘记密码？<router-link to="/forgot-password">找回密码</router-link></span>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid var(--color-border);
}
.pwd-rules {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.pwd-rules span {
  font-size: 12px;
  color: var(--color-text-3);
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--color-page-2);
}
.pwd-rules span.met {
  color: #16a34a;
  background: #dcfce7;
}
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
