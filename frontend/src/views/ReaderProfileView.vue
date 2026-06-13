<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderAdd, Clock } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { getReaderProfile, updateReaderProfile } from '../api/reader'
import type { ReaderProfileDTO } from '../types'

const authStore = useAuthStore()
const profile = ref<ReaderProfileDTO | null>(null)
const loading = ref(false)
const saving = ref(false)

const editForm = ref({
  nickname: '',
  avatarUrl: '',
  bio: '',
  showFootprint: true,
})

const levelName = computed(() => {
  if (!profile.value) return ''
  const lv = profile.value.readerLevel
  if (lv >= 10) return '至尊读者'
  if (lv >= 8) return '资深读者'
  if (lv >= 5) return '活跃读者'
  if (lv >= 3) return '进阶读者'
  return '新手读者'
})

const levelProgress = computed(() => {
  if (!profile.value) return 0
  const thresholds = [0, 50, 150, 350, 700, 1200, 2000, 3200, 5000, 7500, 10000]
  const lv = profile.value.readerLevel
  const current = profile.value.readerExp
  const base = thresholds[Math.min(lv - 1, thresholds.length - 1)]
  const next = thresholds[Math.min(lv, thresholds.length - 1)]
  if (next === base) return 100
  return Math.min(Math.round(((current - base) / (next - base)) * 100), 100)
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getReaderProfile()
    editForm.value = {
      nickname: profile.value.nickname || '',
      avatarUrl: profile.value.avatarUrl || '',
      bio: profile.value.bio || '',
      showFootprint: profile.value.showFootprint,
    }
  } catch {
    ElMessage.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    profile.value = await updateReaderProfile({
      nickname: editForm.value.nickname || undefined,
      avatarUrl: editForm.value.avatarUrl || undefined,
      bio: editForm.value.bio || undefined,
      showFootprint: editForm.value.showFootprint,
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="view-shell reader-shell" v-loading="loading">
    <div v-if="profile" class="reader-layout">
      <div class="reader-sidebar">
        <el-card class="panel-card reader-profile-card">
          <div class="profile-avatar-area">
            <el-avatar :size="80" :src="profile.avatarUrl || undefined">
              {{ (profile.nickname || profile.username)[0] }}
            </el-avatar>
          </div>
          <h2 class="profile-name">{{ profile.nickname || profile.username }}</h2>
          <p class="profile-username">@{{ profile.username }}</p>
          <div class="level-badge">
            <span class="level-label">Lv.{{ profile.readerLevel }}</span>
            <span class="level-name">{{ levelName }}</span>
          </div>
          <div class="level-bar">
            <div class="level-bar-fill" :style="{ width: levelProgress + '%' }"></div>
          </div>
          <p class="level-exp">经验值 {{ profile.readerExp }}</p>
          <div class="streak-info" v-if="profile.streakDays > 0">
            🔥 连续活跃 {{ profile.streakDays }} 天
          </div>

          <div class="profile-stats-grid">
            <div class="stat-item">
              <span class="stat-num">{{ profile.readCount }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.favoriteCount }}</span>
              <span class="stat-label">珍藏</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.commentCount }}</span>
              <span class="stat-label">评论</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.subscriptionCount }}</span>
              <span class="stat-label">订阅</span>
            </div>
          </div>
        </el-card>
      </div>

      <div class="reader-main">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-title-wrap">
              <span class="panel-title">个人资料编辑</span>
            </div>
          </template>
          <el-form label-position="top" @submit.prevent="saveProfile">
            <el-form-item label="头像URL">
              <el-input v-model="editForm.avatarUrl" placeholder="输入头像图片链接" maxlength="500" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" placeholder="展示名称" maxlength="100" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="editForm.bio" type="textarea" :rows="3" placeholder="介绍一下自己" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="隐私设置">
              <el-switch v-model="editForm.showFootprint" active-text="公开浏览足迹" inactive-text="隐藏浏览足迹" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="panel-card" style="margin-top: var(--space-4)">
          <template #header>
            <span class="panel-title">快捷入口</span>
          </template>
          <div class="quick-links">
            <router-link to="/reader/collections" class="quick-link-item">
              <el-icon :size="24"><FolderAdd /></el-icon>
              <span>我的珍藏</span>
            </router-link>
            <router-link to="/reader/history" class="quick-link-item">
              <el-icon :size="24"><Clock /></el-icon>
              <span>阅读足迹</span>
            </router-link>
          </div>
        </el-card>

        <el-card class="panel-card" style="margin-top: var(--space-4)">
          <template #header>
            <span class="panel-title">等级权益</span>
          </template>
          <div class="privilege-info">
            <p>当前等级：<strong>Lv.{{ profile.readerLevel }} {{ levelName }}</strong></p>
            <p>评论字数上限：<strong>{{ profile.maxCommentLength }}字</strong></p>
            <p>发言间隔：<strong>{{ profile.commentIntervalSeconds }}秒</strong></p>
            <el-divider />
            <div class="level-table">
              <div class="level-row header">
                <span>等级</span><span>称号</span><span>评论上限</span><span>发言间隔</span>
              </div>
              <div class="level-row" v-for="item in [
                { lv: 1, name: '新手读者', max: 200, interval: 60 },
                { lv: 3, name: '进阶读者', max: 500, interval: 30 },
                { lv: 5, name: '活跃读者', max: 1000, interval: 30 },
                { lv: 8, name: '资深读者', max: 2000, interval: 10 },
                { lv: 10, name: '至尊读者', max: 2000, interval: 10 },
              ]" :key="item.lv" :class="{ active: profile.readerLevel >= item.lv && profile.readerLevel < (item.lv === 10 ? 11 : item.lv + 2) }">
                <span>Lv.{{ item.lv }}</span><span>{{ item.name }}</span><span>{{ item.max }}字</span><span>{{ item.interval }}秒</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reader-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: var(--space-4);
}

.reader-profile-card {
  text-align: center;
  padding: var(--space-4);
}

.profile-avatar-area {
  margin-bottom: var(--space-3);
}

.profile-name {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
  color: var(--color-text-1);
}

.profile-username {
  margin: var(--space-1) 0 0;
  color: var(--color-text-3);
  font-size: 13px;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--space-3);
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
}

.level-label {
  font-weight: 700;
}

.level-name {
  font-weight: 500;
}

.level-bar {
  margin-top: var(--space-2);
  height: 6px;
  border-radius: 3px;
  background: var(--color-page-2);
  overflow: hidden;
}

.level-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--color-primary), #4684fb);
  transition: width 0.3s ease;
}

.level-exp {
  margin: var(--space-1) 0 0;
  color: var(--color-text-3);
  font-size: 12px;
}

.streak-info {
  margin-top: var(--space-2);
  font-size: 14px;
  font-weight: 600;
  color: #f97316;
}

.profile-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-1);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-3);
}

.privilege-info p {
  margin: var(--space-1) 0;
  color: var(--color-text-2);
  font-size: 14px;
}

.level-table {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.level-row {
  display: grid;
  grid-template-columns: 60px 1fr 80px 80px;
  gap: var(--space-2);
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--color-text-2);
}

.level-row.header {
  font-weight: 600;
  color: var(--color-text-1);
  background: var(--color-page-2);
}

.level-row.active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

.quick-links {
  display: flex;
  gap: var(--space-4);
}

.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-sm);
  background: var(--color-page-1);
  color: var(--color-text-2);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--motion-fast) var(--ease-standard);
}

.quick-link-item:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .reader-layout {
    grid-template-columns: 1fr;
  }
}
</style>
