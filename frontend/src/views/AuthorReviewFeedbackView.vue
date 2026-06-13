<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAuthorReviewHistory,
  getAuthorReviewTimeline,
  replyToComment,
  toggleCommentResolved,
  resubmitWithNote,
} from '../api/reviews'
import { getAuthorPostDetail } from '../api/posts'
import type { PostDetail, ReviewRoundDTO, ReviewTimelineDTO, ReviewCommentDTO } from '../types'

const route = useRoute()
const router = useRouter()
const postId = computed(() => Number(route.params.id))

const post = ref<PostDetail | null>(null)
const reviewHistory = ref<ReviewRoundDTO[]>([])
const timeline = ref<ReviewTimelineDTO | null>(null)
const loading = ref(false)

const activeTab = ref('comments')
const replyText = ref<Record<number, string>>({})
const resubmitDialogVisible = ref(false)
const modificationNote = ref('')
const resubmitLoading = ref(false)

const contentParagraphs = computed(() => {
  if (!post.value) return []
  return post.value.content.split(/\n\n+/)
})

const latestRejectedRound = computed(() => {
  const rejected = reviewHistory.value.filter((r) => r.result === 'REJECTED')
  return rejected.length > 0 ? rejected[rejected.length - 1] : null
})

const totalComments = computed(() => {
  return reviewHistory.value.reduce((sum, round) => sum + round.comments.length, 0)
})

const resolvedComments = computed(() => {
  return reviewHistory.value.reduce(
    (sum, round) => sum + round.comments.filter((c) => c.authorResolved).length,
    0,
  )
})

async function loadPost() {
  loading.value = true
  try {
    post.value = await getAuthorPostDetail(postId.value)
  } catch {
    ElMessage.error('获取文章详情失败')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  try {
    reviewHistory.value = await getAuthorReviewHistory(postId.value)
  } catch {
    reviewHistory.value = []
  }
}

async function loadTimeline() {
  try {
    timeline.value = await getAuthorReviewTimeline(postId.value)
  } catch {
    timeline.value = null
  }
}

async function handleReply(commentId: number) {
  const reply = replyText.value[commentId]?.trim()
  if (!reply) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await replyToComment(commentId, { reply })
    ElMessage.success('回复成功')
    replyText.value[commentId] = ''
    await loadHistory()
  } catch {
    ElMessage.error('回复失败')
  }
}

async function handleToggleResolved(comment: ReviewCommentDTO) {
  try {
    await toggleCommentResolved(comment.id)
    ElMessage.success(comment.authorResolved ? '已标记为未处理' : '已标记为已处理')
    await loadHistory()
  } catch {
    ElMessage.error('操作失败')
  }
}

function openResubmit() {
  modificationNote.value = ''
  resubmitDialogVisible.value = true
}

async function handleResubmit() {
  if (!modificationNote.value.trim()) {
    ElMessage.warning('请填写修改说明')
    return
  }
  resubmitLoading.value = true
  try {
    await resubmitWithNote(postId.value, { modificationNote: modificationNote.value.trim() })
    ElMessage.success('已重新提交审核')
    resubmitDialogVisible.value = false
    await loadPost()
    await loadHistory()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '重新提交失败')
  } finally {
    resubmitLoading.value = false
  }
}

function scrollToParagraph(index: number) {
  const el = document.getElementById(`author-paragraph-${index}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('highlight-flash')
    setTimeout(() => el.classList.remove('highlight-flash'), 2000)
  }
}

function handleCommentClick(comment: ReviewCommentDTO) {
  if (comment.paragraphIndex != null) {
    activeTab.value = 'content'
    nextTick(() => scrollToParagraph(comment.paragraphIndex!))
  }
}

function getEventIcon(type: string): string {
  switch (type) {
    case 'CREATE': return '📝'
    case 'APPROVE': return '✅'
    case 'REJECT': return '❌'
    case 'COMMENT': return '💬'
    case 'REPLY': return '↩️'
    case 'RESUBMIT': return '🔄'
    default: return '📌'
  }
}

function getEventColor(type: string): string {
  switch (type) {
    case 'CREATE': return '#409eff'
    case 'APPROVE': return '#67c23a'
    case 'REJECT': return '#f56c6c'
    case 'COMMENT': return '#e6a23c'
    case 'REPLY': return '#909399'
    case 'RESUBMIT': return '#409eff'
    default: return '#909399'
  }
}

onMounted(async () => {
  await loadPost()
  await loadHistory()
  await loadTimeline()
})
</script>

<template>
  <div class="view-shell author-shell">
    <header class="author-header">
      <div class="header-row">
        <el-button @click="router.push('/author/posts')" link>&larr; 返回文章列表</el-button>
      </div>
      <h1 v-if="post">审查反馈：{{ post.title }}</h1>
      <p class="author-subtitle">查看审查意见、回复批注、标记处理状态并重新提交。</p>
    </header>

    <div v-if="post" v-loading="loading">
      <div class="stats-bar">
        <div class="stat-item">
          <span class="stat-value">{{ totalComments }}</span>
          <span class="stat-label">总批注</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ resolvedComments }}</span>
          <span class="stat-label">已处理</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ totalComments - resolvedComments }}</span>
          <span class="stat-label">待处理</span>
        </div>
        <el-button
          v-if="post.status === 'REJECTED'"
          type="primary"
          @click="openResubmit"
        >
          重新提交审核
        </el-button>
      </div>

      <el-card class="panel-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="文章内容" name="content">
            <div class="content-readonly">
              <div
                v-for="(para, idx) in contentParagraphs"
                :key="idx"
                :id="`author-paragraph-${idx}`"
                class="paragraph-block"
              >
                <span class="paragraph-index">{{ idx + 1 }}</span>
                <span class="paragraph-text">{{ para }}</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="审查意见" name="comments">
            <div v-if="reviewHistory.length === 0" class="empty-hint">暂无审查意见</div>
            <div v-for="round in reviewHistory" :key="round.id" class="round-block">
              <div class="round-header">
                <el-tag :type="round.result === 'APPROVED' ? 'success' : 'danger'" size="small">
                  {{ round.result === 'APPROVED' ? '通过' : '驳回' }}
                </el-tag>
                <span class="round-meta">
                  审查人：{{ round.reviewerName }} · {{ dayjs(round.createdAt).format('YYYY-MM-DD HH:mm') }}
                </span>
              </div>

              <div v-if="round.modificationNote" class="mod-note">
                <strong>修改说明：</strong>{{ round.modificationNote }}
              </div>

              <div v-for="comment in round.comments" :key="comment.id" class="comment-item" @click="handleCommentClick(comment)">
                <div class="comment-header">
                  <el-tag
                    :type="comment.priority === 'MUST_FIX' ? 'danger' : 'warning'"
                    size="small"
                  >
                    {{ comment.priority === 'MUST_FIX' ? '必须修改' : '建议优化' }}
                  </el-tag>
                  <span v-if="comment.paragraphIndex != null" class="comment-para">
                    段落 {{ comment.paragraphIndex + 1 }}
                  </span>
                  <el-tag
                    :type="comment.authorResolved ? 'success' : 'info'"
                    size="small"
                    style="cursor: pointer"
                    @click.stop="handleToggleResolved(comment)"
                  >
                    {{ comment.authorResolved ? '已处理 ✓' : '标记已处理' }}
                  </el-tag>
                </div>
                <div class="comment-content">{{ comment.content }}</div>

                <div v-if="comment.authorReply" class="comment-reply">
                  <strong>我的回复：</strong>{{ comment.authorReply }}
                </div>

                <div v-if="!comment.authorReply" class="reply-row">
                  <el-input
                    v-model="replyText[comment.id]"
                    placeholder="输入回复说明修改情况..."
                    size="small"
                    style="flex: 1"
                    @keyup.enter="handleReply(comment.id)"
                  />
                  <el-button type="primary" size="small" @click="handleReply(comment.id)">回复</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="审查时间线" name="timeline">
            <div v-if="!timeline || timeline.events.length === 0" class="empty-hint">暂无时间线记录</div>
            <div v-else class="timeline-container">
              <div v-for="(evt, idx) in timeline.events" :key="idx" class="timeline-item">
                <div class="timeline-dot" :style="{ backgroundColor: getEventColor(evt.type) }">
                  {{ getEventIcon(evt.type) }}
                </div>
                <div class="timeline-body">
                  <div class="timeline-title">
                    <strong>{{ evt.userName }}</strong> {{ evt.action }}
                  </div>
                  <div v-if="evt.content" class="timeline-content">{{ evt.content }}</div>
                  <div class="timeline-time">{{ dayjs(evt.timestamp).format('YYYY-MM-DD HH:mm:ss') }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <el-dialog v-model="resubmitDialogVisible" title="重新提交审核" width="500px">
      <el-alert
        title="请列出针对上次审查意见所做的修改"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <div v-if="latestRejectedRound" class="previous-comments">
        <p class="prev-label">上次审查意见：</p>
        <div v-for="comment in latestRejectedRound.comments" :key="comment.id" class="prev-comment">
          <el-tag :type="comment.priority === 'MUST_FIX' ? 'danger' : 'warning'" size="small">
            {{ comment.priority === 'MUST_FIX' ? '必须修改' : '建议优化' }}
          </el-tag>
          <span>{{ comment.content }}</span>
          <el-tag v-if="comment.authorResolved" type="success" size="small">已处理</el-tag>
        </div>
      </div>
      <el-input
        v-model="modificationNote"
        type="textarea"
        :rows="5"
        placeholder="请描述你针对审查意见做了哪些修改和调整..."
      />
      <template #footer>
        <el-button @click="resubmitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resubmitLoading" @click="handleResubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.author-header h1 {
  margin: var(--space-2) 0 0;
  font-size: clamp(24px, 3vw, 30px);
  line-height: 1.2;
}

.author-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-3);
  font-size: 15px;
}

.header-row {
  margin-bottom: var(--space-1);
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-1);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-3);
}

.panel-card {
  width: 100%;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.content-readonly {
  max-height: 60vh;
  overflow-y: auto;
}

.paragraph-block {
  position: relative;
  padding: var(--space-2) var(--space-2) var(--space-2) 36px;
  margin-bottom: var(--space-1);
  border-radius: var(--radius-sm);
  line-height: 1.8;
  font-size: 15px;
  color: var(--color-text-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.paragraph-index {
  position: absolute;
  left: 4px;
  top: 8px;
  font-size: 11px;
  color: var(--color-text-4);
  font-weight: 600;
}

.paragraph-text {
  white-space: pre-wrap;
}

.highlight-flash {
  animation: flash-highlight 2s ease-out;
}

@keyframes flash-highlight {
  0% { background-color: #ffeaa7; }
  100% { background-color: transparent; }
}

.round-block {
  margin-bottom: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
}

.round-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.round-meta {
  color: var(--color-text-3);
  font-size: 13px;
}

.mod-note {
  background: #f0f9eb;
  padding: var(--space-2);
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: var(--space-2);
}

.comment-item {
  border-left: 3px solid var(--color-border);
  padding: var(--space-2) var(--space-3);
  margin-bottom: var(--space-2);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  cursor: pointer;
  transition: border-color 0.2s;
}

.comment-item:hover {
  border-left-color: #409eff;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 4px;
}

.comment-para {
  font-size: 12px;
  color: var(--color-text-3);
}

.comment-content {
  font-size: 14px;
  color: var(--color-text-2);
  line-height: 1.6;
}

.comment-reply {
  margin-top: var(--space-1);
  padding: var(--space-1) var(--space-2);
  background: #f5f7fa;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-3);
}

.reply-row {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.timeline-container {
  position: relative;
  padding-left: 32px;
}

.timeline-item {
  position: relative;
  padding-bottom: var(--space-4);
  display: flex;
  gap: var(--space-3);
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: -26px;
  top: 24px;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-dot {
  position: absolute;
  left: -32px;
  top: 2px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.timeline-title {
  font-size: 14px;
}

.timeline-content {
  font-size: 13px;
  color: var(--color-text-3);
  margin-top: 4px;
  padding: var(--space-1) var(--space-2);
  background: #f5f7fa;
  border-radius: var(--radius-sm);
}

.timeline-time {
  font-size: 12px;
  color: var(--color-text-4);
  margin-top: 4px;
}

.previous-comments {
  margin-bottom: var(--space-3);
  padding: var(--space-2);
  background: #fafbfc;
  border-radius: var(--radius-sm);
}

.prev-label {
  font-size: 13px;
  color: var(--color-text-3);
  margin: 0 0 var(--space-1);
}

.prev-comment {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  font-size: 13px;
}

.empty-hint {
  color: var(--color-text-3);
  font-size: 14px;
  text-align: center;
  padding: 48px 0;
}
</style>
