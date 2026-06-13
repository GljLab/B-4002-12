<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Sort, Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import {
  getDiscussionsForPost,
  createDiscussion,
} from '../api/discussions'
import type { CommentTreeDTO, DiscussionListResponse } from '../types'
import DiscussionItem from './DiscussionItem.vue'
import DiscussionEditor from './DiscussionEditor.vue'

const props = defineProps<{
  postId: number
  postAuthorId: number
  maxDepth?: number
}>()

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const sortBy = ref<'newest' | 'hottest' | 'oldest'>('newest')
const page = ref(1)
const total = ref(0)
const totalPages = ref(0)
const hasMore = ref(false)
const discussions = ref<CommentTreeDTO[]>([])
const newCommentContent = ref('')

const sortOptions = [
  { label: '最新', value: 'newest' },
  { label: '最热', value: 'hottest' },
  { label: '最早', value: 'oldest' },
]

const pinnedDiscussions = computed(() => {
  return discussions.value.filter((d) => d.pinned)
})

const normalDiscussions = computed(() => {
  return discussions.value.filter((d) => !d.pinned)
})

async function loadDiscussions(reset = false) {
  if (loading.value) return

  if (reset) {
    page.value = 1
    discussions.value = []
  }

  loading.value = true
  try {
    const result: DiscussionListResponse = await getDiscussionsForPost(
      props.postId,
      sortBy.value,
      page.value,
    )
    total.value = result.total
    totalPages.value = result.totalPages
    hasMore.value = result.hasMore
    if (reset) {
      discussions.value = result.items || []
    } else {
      discussions.value = [...discussions.value, ...(result.items || [])]
    }
  } catch {
    ElMessage.error('加载讨论失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleSortChange() {
  loadDiscussions(true)
}

async function handleRefresh() {
  loadDiscussions(true)
}

async function handleLoadMore() {
  if (loading.value || !hasMore.value) return
  page.value++
  await loadDiscussions(false)
}

async function handleSubmitNewComment() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!newCommentContent.value.trim()) return

  submitting.value = true
  try {
    const newComment = await createDiscussion(props.postId, {
      content: newCommentContent.value,
    })
    discussions.value.unshift(newComment)
    total.value++
    newCommentContent.value = ''
    ElMessage.success('评论发表成功')
  } catch {
    ElMessage.error('发表失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function handleReply(parentComment: CommentTreeDTO) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  submitting.value = true
  try {
    const newComment = await createDiscussion(props.postId, {
      content: parentComment.content,
      parentId: parentComment.parentId || parentComment.id,
      replyToCommentId: parentComment.id,
      replyToUserId: parentComment.userId,
    })

    function addReplyToComment(comments: CommentTreeDTO[]): boolean {
      for (const comment of comments) {
        if (comment.id === (parentComment.parentId || parentComment.id)) {
          comment.replies = comment.replies || []
          comment.replies.push(newComment)
          return true
        }
        if (comment.replies && comment.replies.length > 0) {
          if (addReplyToComment(comment.replies)) {
            return true
          }
        }
      }
      return false
    }

    if (!addReplyToComment(discussions.value)) {
      discussions.value.unshift(newComment)
    }

    total.value++
    ElMessage.success('回复发表成功')
  } catch {
    ElMessage.error('发表失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleCommentDeleted(commentId: number) {
  function removeComment(comments: CommentTreeDTO[]): boolean {
    const index = comments.findIndex((c) => c.id === commentId)
    if (index !== -1) {
      comments.splice(index, 1)
      total.value--
      return true
    }
    for (const comment of comments) {
      if (comment.replies && removeComment(comment.replies)) {
        return true
      }
    }
    return false
  }
  removeComment(discussions.value)
}

function handleCommentUpdated(updatedComment: CommentTreeDTO) {
  function updateComment(comments: CommentTreeDTO[]): boolean {
    for (const comment of comments) {
      if (comment.id === updatedComment.id) {
        comment.content = updatedComment.content
        comment.edited = updatedComment.edited
        comment.editedAt = updatedComment.editedAt
        return true
      }
      if (comment.replies && updateComment(comment.replies)) {
        return true
      }
    }
    return false
  }
  updateComment(discussions.value)
}

function handleLoadReplies(commentId: number, newReplies: CommentTreeDTO[]) {
  function addReplies(comments: CommentTreeDTO[]): boolean {
    for (const comment of comments) {
      if (comment.id === commentId) {
        comment.replies = comment.replies || []
        comment.replies.push(...newReplies)
        return true
      }
      if (comment.replies && addReplies(comment.replies)) {
        return true
      }
    }
    return false
  }
  addReplies(discussions.value)
}

watch(
  () => props.postId,
  () => {
    if (props.postId) {
      loadDiscussions(true)
    }
  },
)

onMounted(() => {
  if (props.postId) {
    loadDiscussions(true)
  }
})
</script>

<template>
  <div class="discussion-section">
    <div class="section-header">
      <div class="header-left">
        <el-icon class="section-icon"><ChatDotRound /></el-icon>
        <h3 class="section-title">讨论区</h3>
        <span class="comment-count">({{ total }})</span>
      </div>
      <div class="header-right">
        <el-select
          v-model="sortBy"
          size="small"
          class="sort-select"
          @change="handleSortChange"
        >
          <el-option
            v-for="option in sortOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button
          size="small"
          :icon="Refresh"
          @click="handleRefresh"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>
    </div>

    <div v-if="authStore.isLoggedIn" class="new-comment-wrapper">
      <DiscussionEditor
        v-model="newCommentContent"
        placeholder="发表你的看法..."
        :loading="submitting"
        @submit="handleSubmitNewComment"
      />
    </div>
    <div v-else class="login-prompt">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          请先<router-link to="/login" class="login-link">登录</router-link>后发表评论
        </template>
      </el-alert>
    </div>

    <div v-loading="loading" class="discussion-list-wrapper">
      <template v-if="discussions.length > 0">
        <div v-if="pinnedDiscussions.length > 0" class="pinned-section">
          <DiscussionItem
            v-for="comment in pinnedDiscussions"
            :key="comment.id"
            :comment="comment"
            :post-author-id="postAuthorId"
            :max-depth="maxDepth || 3"
            @reply="handleReply"
            @deleted="handleCommentDeleted"
            @updated="handleCommentUpdated"
            @load-replies="handleLoadReplies"
          />
        </div>

        <div class="normal-section">
          <DiscussionItem
            v-for="comment in normalDiscussions"
            :key="comment.id"
            :comment="comment"
            :post-author-id="postAuthorId"
            :max-depth="maxDepth || 3"
            @reply="handleReply"
            @deleted="handleCommentDeleted"
            @updated="handleCommentUpdated"
            @load-replies="handleLoadReplies"
          />
        </div>

        <div v-if="hasMore" class="load-more-wrapper">
          <el-button
            v-if="!loading"
            size="small"
            @click="handleLoadMore"
          >
            加载更多
          </el-button>
          <div v-else class="loading-text">
            加载中...
          </div>
        </div>
      </template>

      <el-empty
        v-else-if="!loading"
        description="暂无评论，快来发表第一条评论吧"
        class="empty-state"
      />
    </div>
  </div>
</template>

<style scoped>
.discussion-section {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.section-icon {
  font-size: 24px;
  color: var(--color-primary);
}

.section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-1);
}

.comment-count {
  font-size: 14px;
  color: var(--color-text-3);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.sort-select {
  width: 120px;
}

.new-comment-wrapper {
  margin-bottom: var(--space-5);
}

.login-prompt {
  margin-bottom: var(--space-4);
}

.login-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.login-link:hover {
  text-decoration: underline;
}

.discussion-list-wrapper {
  min-height: 200px;
}

.pinned-section {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 2px dashed var(--color-border);
}

.load-more-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--space-4) 0;
}

.loading-text {
  color: var(--color-text-3);
  font-size: 14px;
  padding: var(--space-2) 0;
}

.empty-state {
  padding: var(--space-6) 0;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }

  .sort-select {
    flex: 1;
    width: auto;
  }

  .section-title {
    font-size: 18px;
  }
}
</style>
