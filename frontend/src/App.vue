<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'
import NotificationBell from './components/NotificationBell.vue'

const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(() => authStore.displayName)
const isAdmin = computed(() => authStore.isAdmin)
const isAuthor = computed(() => authStore.isAuthor)
const isReader = computed(() => authStore.isReader)

async function logout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell" :class="{ 'author-theme': isAuthor, 'reader-theme': isReader }">
    <header class="top-nav">
      <div class="top-nav-inner">
        <div class="brand" @click="$router.push('/')">简易博客</div>
        <nav class="nav-links" aria-label="主导航">
          <router-link class="nav-link" to="/">首页</router-link>
          <router-link class="nav-link" to="/categories">分类</router-link>
          <router-link class="nav-link" to="/keywords">关键词</router-link>
          <template v-if="isAdmin">
            <router-link class="nav-link" to="/admin">后台管理</router-link>
            <router-link class="nav-link" to="/admin/categories">分类管理</router-link>
            <router-link class="nav-link" to="/admin/keywords">关键词统计</router-link>
            <router-link class="nav-link" to="/admin/authors">作者管理</router-link>
            <router-link class="nav-link" to="/admin/reviews">审核队列</router-link>
            <router-link class="nav-link" to="/admin/scheduled">预约上线</router-link>
            <router-link class="nav-link" to="/admin/stats">全局统计</router-link>
          </template>
          <template v-if="isAuthor">
            <router-link class="nav-link" to="/author">工作台</router-link>
            <router-link class="nav-link" to="/author/posts">我的文章</router-link>
            <router-link class="nav-link" to="/author/posts/create">创作中心</router-link>
            <router-link class="nav-link" to="/author/images">图片管理</router-link>
            <router-link class="nav-link" to="/author/stats">个人统计</router-link>
            <router-link class="nav-link" to="/author/settings">个人设置</router-link>
          </template>
          <template v-if="isReader">
            <router-link class="nav-link" to="/reader">个人空间</router-link>
            <router-link class="nav-link" to="/reader/collections">我的珍藏</router-link>
            <router-link class="nav-link" to="/reader/history">阅读足迹</router-link>
          </template>
          <router-link v-if="!authStore.isLoggedIn" class="nav-link" to="/login">登录</router-link>
          <template v-if="authStore.isLoggedIn">
            <NotificationBell />
            <a class="nav-link" href="#" @click.prevent="logout">退出（{{ displayName }}）</a>
          </template>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>
