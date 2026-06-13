<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CaretTop,
  CaretBottom,
  ChatDotRound,
  Edit,
  Delete,
  Warning,
  Top,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useAuthStore } from '../stores/auth'
import { markdownRenderer } from '../utils/markdown'
import {
  voteComment,
  updateDiscussion,
  deleteDiscussion,
  reportComment,
  togglePinComment,
  getMoreReplies,
} from '../api/discussions'
import type { CommentTreeDTO } from '../types'
import DiscussionEditor from './DiscussionEditor.vue'

const props = defineProps<{
  comment: CommentTreeDTO
  postAuthorId: number
  maxDepth?: number
}>()

const emit = defineEmits<{
  reply: [comment: CommentTreeDTO]
  deleted: [commentId: number]
  updated: [comment: CommentTreeDTO]
  loadReplies: [commentId: number, replies: CommentTreeDTO[]]
}>()

const route = useRoute()
const authStore = useAuthStore()
const showActions = ref(false)
const showReplyEditor = ref(false)
const showEditEditor = ref(false)
const showReportDialog = ref(false)
const isLoadingReplies = ref(false)
const isHighlighted = ref(false)

const replyContent = ref('')
const editContent = ref('')
const reportReason = ref('')
const reportDescription = ref('')
const isSubmitting = ref(false)

const renderedContent = computed(() => {
  if (props.comment.deleted) return ''
  return markdownRenderer.renderToHtml(props.comment.content)
})

const formattedTime = computed(() => {
  return dayjs(props.comment.createdAt).format('YYYY-MM-DD HH:mm')
})

const canEdit = computed(() => props.comment.canEdit)
const canDelete = computed(() => props.comment.canDelete)
const isPostAuthor = computed(() => authStore.user?.id === props.postAuthorId)
const canPin = computed(() => isPostAuthor.value && !props.comment.deleted)
const canReport = computed(() => {
  return (
    authStore.isLoggedIn &&
    !props.comment.deleted &&
    authStore.user?.id !== props.comment.userId
  )
})

const isUpvoted = computed(() => props.comment.currentUserVote === 1)
const isDownvoted = computed(() => props.comment.currentUserVote === -1)

const voteScore = computed(() => {
  return props.comment.upvotes - props.comment.downvotes
})

const hasMoreReplies = computed(() => {
  return props.comment.depth < (props.maxDepth || 3)
})

const commentAnchorId = computed(() => `comment-${props.comment.id}`)

function handleMouseEnter() {
  showActions.value = true
}

function handleMouseLeave() {
  showActions.value = false
}

async function handleVote(voteType: number) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const currentVote = props.comment.currentUserVote
  const newVoteType = currentVote === voteType ? 0 : voteType

  const oldUpvotes = props.comment.upvotes
  const oldDownvotes = props.comment.downvotes
  const oldCurrentVote = props.comment.currentUserVote

  if (newVoteType === 1) {
    props.comment.upvotes++
    if (oldCurrentVote === -1) props.comment.downvotes--
  } else if (newVoteType === -1) {
    props.comment.downvotes++
    if (oldCurrentVote === 1) props.comment.upvotes--
  } else {
    if (oldCurrentVote === 1) props.comment.upvotes--
    if (oldCurrentVote === -1) props.comment.downvotes--
  }
  props.comment.currentUserVote = newVoteType

  try {
    await voteComment(props.comment.id, newVoteType)
  } catch {
    props.comment.upvotes = oldUpvotes
    props.comment.downvotes = oldDownvotes
    props.comment.currentUserVote = oldCurrentVote
    ElMessage.error('投票失败，请稍后重试')
  }
}

function openReplyEditor() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  showReplyEditor.value = true
  replyContent.value = ''
}

function cancelReply() {
  showReplyEditor.value = false
  replyContent.value = ''
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  emit('reply', {
    ...props.comment,
    content: replyContent.value,
  })
  showReplyEditor.value = false
  replyContent.value = ''
}

function openEditEditor() {
  showEditEditor.value = true
  editContent.value = props.comment.content
}

function cancelEdit() {
  showEditEditor.value = false
  editContent.value = ''
}

async function submitEdit() {
  if (!editContent.value.trim() || editContent.value === props.comment.content) {
    cancelEdit()
    return
  }

  isSubmitting.value = true
  try {
    const updated = await updateDiscussion(props.comment.id, { content: editContent.value })
    props.comment.content = updated.content
    props.comment.edited = true
    props.comment.editedAt = updated.editedAt
    emit('updated', updated)
    ElMessage.success('修改成功')
    showEditEditor.value = false
    editContent.value = ''
  } catch {
    ElMessage.error('修改失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？此操作不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const oldDeleted = props.comment.deleted
    const oldDeletedBy = props.comment.deletedBy
    const oldContent = props.comment.content

    props.comment.deleted = true
    props.comment.deletedBy = 'self'
    props.comment.content = ''

    try {
      await deleteDiscussion(props.comment.id)
      emit('deleted', props.comment.id)
      ElMessage.success('删除成功')
    } catch {
      props.comment.deleted = oldDeleted
      props.comment.deletedBy = oldDeletedBy
      props.comment.content = oldContent
      ElMessage.error('删除失败，请稍后重试')
    }
  } catch {
    // 用户取消
  }
}

async function handleReport() {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请选择举报原因')
    return
  }

  isSubmitting.value = true
  try {
    await reportComment(props.comment.id, {
      reason: reportReason.value,
      description: reportDescription.value || undefined,
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    showReportDialog.value = false
    reportReason.value = ''
    reportDescription.value = ''
  } catch {
    ElMessage.error('举报失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

async function handleTogglePin() {
  try {
    const result = await togglePinComment(props.comment.id)
    props.comment.pinned = result.pinned
    ElMessage.success(result.pinned ? '已置顶' : '已取消置顶')
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  }
}

async function loadMoreReplies() {
  if (isLoadingReplies.value) return

  isLoadingReplies.value = true
  try {
    const result = await getMoreReplies(props.comment.id, 1)
    if (result.items && result.items.length > 0) {
      emit('loadReplies', props.comment.id, result.items)
    }
  } catch {
    ElMessage.error('加载回复失败')
  } finally {
    isLoadingReplies.value = false
  }
}

function handleChildReply(comment: CommentTreeDTO) {
  emit('reply', comment)
}

function handleChildDeleted(commentId: number) {
  emit('deleted', commentId)
}

function handleChildUpdated(comment: CommentTreeDTO) {
  emit('updated', comment)
}

function handleChildLoadReplies(commentId: number, replies: CommentTreeDTO[]) {
  emit('loadReplies', commentId, replies)
}

function checkHashAndScroll() {
  const hash = route.hash.replace('#', '')
  if (hash === commentAnchorId.value) {
    nextTick(() => {
      const el = document.getElementById(commentAnchorId.value)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        isHighlighted.value = true
        setTimeout(() => {
          isHighlighted.value = false
        }, 3000)
      }
    })
  }
}

watch(
  () => route.hash,
  () => {
    checkHashAndScroll()
  },
)

onMounted(() => {
  checkHashAndScroll()
})
</script>

<template>
  <div
    :id="commentAnchorId"
    class="discussion-item"
    :class="{
      'is-pinned': comment.pinned,
      'is-deleted': comment.deleted,
      'is-highlighted': isHighlighted,
    }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div v-if="comment.pinned" class="pinned-badge">
      <el-icon><Top /></el-icon>
      <span>置顶</span>
    </div>

    <div class="comment-header">
      <el-avatar
        :size="36"
        :src="comment.avatarUrl || undefined"
        class="comment-avatar"
      >
        {{ comment.nickname?.charAt(0) }}
      </el-avatar>
      <div class="comment-meta">
        <div class="comment-author">
          <span class="author-name">{{ comment.nickname }}</span>
          <span v-if="comment.edited" class="edited-badge">已编辑</span>
        </div>
        <div class="comment-time">
          {{ formattedTime }}
        </div>
      </div>
    </div>

    <div class="comment-body">
      <div
        v-if="comment.deleted"
        class="deleted-placeholder"
      >
        该评论已被{{ comment.deletedBy === 'self' ? '作者' : '管理员' }}删除
      </div>
      <template v-else>
        <div
          v-if="comment.replyToNickname"
          class="reply-to-hint"
        >
          回复 @{{ comment.replyToNickname }}
        </div>
        <div
          class="comment-content markdown-body"
          v-html="renderedContent"
        />
      </template>
    </div>

    <div class="comment-actions" :class="{ 'visible': showActions }">
      <button
        class="action-btn"
        :class="{ 'voted': isUpvoted }"
        @click="handleVote(1)"
      >
        <el-icon><CaretTop /></el-icon>
        <span>{{ comment.upvotes }}</span>
      </button>
      <button
        class="action-btn"
        :class="{ 'voted': isDownvoted }"
        @click="handleVote(-1)"
      >
        <el-icon><CaretBottom /></el-icon>
        <span>{{ comment.downvotes }}</span>
      </button>
      <button
        v-if="!comment.deleted"
        class="action-btn"
        @click="openReplyEditor"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span>回复</span>
      </button>
      <button
        v-if="canEdit && !comment.deleted"
        class="action-btn"
        @click="openEditEditor"
      >
        <el-icon><Edit /></el-icon>
        <span>编辑</span>
      </button>
      <button
        v-if="canDelete && !comment.deleted"
        class="action-btn danger"
        @click="handleDelete"
      >
        <el-icon><Delete /></el-icon>
        <span>删除</span>
      </button>
      <button
        v-if="canReport"
        class="action-btn"
        @click="showReportDialog = true"
      >
        <el-icon><Warning /></el-icon>
        <span>举报</span>
      </button>
      <button
        v-if="canPin"
        class="action-btn"
        :class="{ 'pinned': comment.pinned }"
        @click="handleTogglePin"
      >
        <el-icon><Top /></el-icon>
        <span>{{ comment.pinned ? '取消置顶' : '置顶' }}</span>
      </button>
    </div>

    <div v-if="showEditEditor" class="edit-editor-wrapper">
      <DiscussionEditor
        v-model="editContent"
        placeholder="编辑你的评论..."
        :loading="isSubmitting"
        :show-cancel="true"
        @submit="submitEdit"
        @cancel="cancelEdit"
      />
    </div>

    <div v-if="showReplyEditor" class="reply-editor-wrapper">
      <DiscussionEditor
        v-model="replyContent"
        placeholder="写下你的回复..."
        :reply-to-username="comment.nickname"
        :loading="isSubmitting"
        :show-cancel="true"
        @submit="submitReply"
        @cancel="cancelReply"
      />
    </div>

    <div v-if="comment.replies && comment.replies.length > 0" class="replies-wrapper">
      <DiscussionItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :post-author-id="postAuthorId"
        :max-depth="maxDepth"
        @reply="handleChildReply"
        @deleted="handleChildDeleted"
        @updated="handleChildUpdated"
        @load-replies="handleChildLoadReplies"
      />
    </div>

    <div
      v-if="hasMoreReplies && !isLoadingReplies"
      class="load-more-replies"
      @click="loadMoreReplies"
    >
      <span>加载更多回复</span>
    </div>
    <div v-if="isLoadingReplies" class="loading-replies">
      <el-icon class="is-loading"><CaretTop /></el-icon>
      <span>加载中...</span>
    </div>

    <el-dialog
      v-model="showReportDialog"
      title="举报评论"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="举报原因" required>
          <el-select v-model="reportReason" placeholder="请选择举报原因" style="width: 100%">
            <el-option label="垃圾广告" value="垃圾广告" />
            <el-option label="色情低俗" value="色情低俗" />
            <el-option label="人身攻击" value="人身攻击" />
            <el-option label="违法违规" value="违法违规" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明（可选）">
          <el-input
            v-model="reportDescription"
            type="textarea"
            :rows="3"
            placeholder="请描述具体问题（最多2000字）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="handleReport">
          提交举报
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.discussion-item {
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--color-border);
  position: relative;
  transition: all var(--motion-fast) var(--ease-standard);
  scroll-margin-top: 100px;
}

.discussion-item:last-child {
  border-bottom: none;
}

.discussion-item.is-pinned {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.05) 0%, transparent 100%);
  padding-left: var(--space-3);
  padding-right: var(--space-3);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--el-color-danger);
}

.discussion-item.is-deleted .comment-content {
  opacity: 0.6;
}

.discussion-item.is-highlighted {
  animation: highlightPulse 3s ease-out;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, transparent 100%);
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 2px var(--color-primary-soft);
}

@keyframes highlightPulse {
  0% {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.2) 0%, transparent 100%);
  }
  50% {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, transparent 100%);
  }
  100% {
    background: transparent;
  }
}

.pinned-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  margin-bottom: var(--space-2);
}

.comment-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-meta {
  flex: 1;
  min-width: 0;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 2px;
}

.author-name {
  font-weight: 600;
  color: var(--color-text-1);
  font-size: 14px;
}

.edited-badge {
  font-size: 11px;
  color: var(--color-text-3);
  background: var(--color-border);
  padding: 2px 6px;
  border-radius: 4px;
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-3);
}

.comment-body {
  margin-left: 48px;
  margin-bottom: var(--space-2);
}

.reply-to-hint {
  font-size: 13px;
  color: var(--color-text-3);
  margin-bottom: var(--space-1);
}

.reply-to-hint::before {
  content: '↳ ';
  color: var(--color-primary);
  font-weight: 600;
}

.comment-content {
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.deleted-placeholder {
  color: var(--color-text-3);
  font-style: italic;
  font-size: 13px;
  padding: var(--space-2) 0;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: 48px;
  opacity: 0;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.comment-actions.visible {
  opacity: 1;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--color-text-3);
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  transition: all var(--motion-fast) var(--ease-standard);
}

.action-btn:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.action-btn.voted {
  color: var(--color-primary);
  font-weight: 600;
}

.action-btn.danger:hover {
  background: var(--el-color-danger) + '15';
  color: var(--el-color-danger);
}

.action-btn.pinned {
  color: var(--el-color-danger);
}

.edit-editor-wrapper,
.reply-editor-wrapper {
  margin-left: 48px;
  margin-top: var(--space-3);
}

.replies-wrapper {
  margin-left: 48px;
  margin-top: var(--space-2);
  padding-left: var(--space-4);
  border-left: 2px solid var(--color-border);
}

.replies-wrapper .discussion-item {
  padding: var(--space-3) 0;
}

.replies-wrapper .comment-header .comment-avatar {
  width: 30px;
  height: 30px;
  font-size: 13px;
}

.replies-wrapper .comment-body,
.replies-wrapper .comment-actions,
.replies-wrapper .edit-editor-wrapper,
.replies-wrapper .reply-editor-wrapper {
  margin-left: 40px;
}

.load-more-replies {
  margin-left: 48px;
  padding: var(--space-2);
  text-align: center;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  border-radius: 8px;
  transition: all var(--motion-fast) var(--ease-standard);
}

.load-more-replies:hover {
  background: var(--color-primary-soft);
}

.loading-replies {
  margin-left: 48px;
  padding: var(--space-2);
  text-align: center;
  color: var(--color-text-3);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.comment-content :deep(p) {
  margin: 8px 0;
}

.comment-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

.comment-content :deep(code) {
  background: #f4f7fb;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.comment-content :deep(pre) {
  background: #1e293b;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.comment-content :deep(pre code) {
  background: transparent;
  color: #e2e8f0;
}

@media (max-width: 768px) {
  .discussion-item {
    padding: var(--space-3) 0;
  }

  .discussion-item.is-pinned {
    padding-left: var(--space-2);
    padding-right: var(--space-2);
  }

  .comment-body,
  .comment-actions,
  .edit-editor-wrapper,
  .reply-editor-wrapper,
  .load-more-replies,
  .loading-replies {
    margin-left: 0;
  }

  .replies-wrapper {
    margin-left: 0;
    padding-left: var(--space-3);
  }

  .replies-wrapper .comment-body,
  .replies-wrapper .comment-actions,
  .replies-wrapper .edit-editor-wrapper,
  .replies-wrapper .reply-editor-wrapper {
    margin-left: 40px;
  }

  .comment-actions {
    opacity: 1;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .action-btn {
    font-size: 12px;
    padding: 4px 6px;
  }
}
</style>
