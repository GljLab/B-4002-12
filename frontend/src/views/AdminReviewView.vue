<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPendingPosts,
  getReviewDetail,
  reviewAction,
  batchReview,
  addReviewComment,
  getReviewHistory,
  getReviewTimeline,
  getReviewTemplates,
  createReviewTemplate,
  updateReviewTemplate,
  deleteReviewTemplate,
} from '../api/reviews'
import { getRevisionDiffAdmin } from '../api/posts'
import type {
  PostSummary,
  PostDetail,
  RevisionDiffDTO,
  ReviewCommentDTO,
  ReviewRoundDTO,
  ReviewTemplateDTO,
  ReviewTimelineDTO,
  CreateReviewCommentPayload,
} from '../types'

const pendingPosts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const selectedIds = ref<number[]>([])
const selectedDetail = ref<PostDetail | null>(null)
const detailLoading = ref(false)
const batchRejectDialog = ref(false)
const batchRejectLoading = ref(false)
const batchRejectReason = ref('')
const revisionDiff = ref<RevisionDiffDTO | null>(null)
const diffLoading = ref(false)

const activeTab = ref('content')
const reviewHistory = ref<ReviewRoundDTO[]>([])
const timeline = ref<ReviewTimelineDTO | null>(null)
const templates = ref<ReviewTemplateDTO[]>([])

const rejectDialogVisible = ref(false)
const rejectComments = ref<CreateReviewCommentPayload[]>([])
const rejectCommentContent = ref('')
const rejectCommentPriority = ref<string>('MUST_FIX')
const rejectCommentParagraph = ref<number | null>(null)

const commentDialogVisible = ref(false)
const newCommentContent = ref('')
const newCommentPriority = ref<string>('MUST_FIX')
const newCommentParagraph = ref<number | null>(null)

const templateDialogVisible = ref(false)
const templateForm = ref({ id: null as number | null, name: '', content: '', priority: 'SUGGESTION' as string })
const templateLoading = ref(false)

const contentParagraphs = computed(() => {
  if (!selectedDetail.value) return []
  return selectedDetail.value.content.split(/\n\n+/)
})

const hasSelection = computed(() => selectedIds.value.length > 0)

const mustFixCount = computed(() =>
  rejectComments.value.filter((c) => c.priority === 'MUST_FIX').length,
)

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingPosts(page.value, size.value)
    pendingPosts.value = res.items
    total.value = res.total
  } catch {
    ElMessage.error('获取待审核列表失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: number) {
  detailLoading.value = true
  revisionDiff.value = null
  reviewHistory.value = []
  timeline.value = null
  try {
    selectedDetail.value = await getReviewDetail(id)
    if (selectedDetail.value.revision) {
      await loadRevisionDiff(selectedDetail.value.parentPostId!)
    }
    loadReviewHistory(id)
    loadTimeline(id)
  } catch {
    ElMessage.error('获取文章详情失败')
    selectedDetail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function loadRevisionDiff(parentPostId: number) {
  diffLoading.value = true
  try {
    revisionDiff.value = await getRevisionDiffAdmin(parentPostId)
  } catch {
    revisionDiff.value = null
  } finally {
    diffLoading.value = false
  }
}

async function loadReviewHistory(postId: number) {
  try {
    reviewHistory.value = await getReviewHistory(postId)
  } catch {
    reviewHistory.value = []
  }
}

async function loadTimeline(postId: number) {
  try {
    timeline.value = await getReviewTimeline(postId)
  } catch {
    timeline.value = null
  }
}

async function loadTemplates() {
  try {
    templates.value = await getReviewTemplates()
  } catch {
    templates.value = []
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadPending()
}

function handleSelectionChange(rows: PostSummary[]) {
  selectedIds.value = rows.map((r) => r.id)
}

function handleRowClick(row: PostSummary) {
  loadDetail(row.id)
}

async function handleApprove(id: number) {
  try {
    await reviewAction(id, { action: 'APPROVE' })
    ElMessage.success('已通过')
    selectedDetail.value = null
    revisionDiff.value = null
    await loadPending()
  } catch {
    ElMessage.error('操作失败')
  }
}

function openRejectDialog(id: number) {
  rejectComments.value = []
  rejectCommentContent.value = ''
  rejectCommentPriority.value = 'MUST_FIX'
  rejectCommentParagraph.value = null
  rejectDialogVisible.value = true
}

function addRejectComment() {
  if (!rejectCommentContent.value.trim()) {
    ElMessage.warning('请输入批注内容')
    return
  }
  rejectComments.value.push({
    content: rejectCommentContent.value.trim(),
    priority: rejectCommentPriority.value,
    paragraphIndex: rejectCommentParagraph.value,
  })
  rejectCommentContent.value = ''
  rejectCommentParagraph.value = null
}

function removeRejectComment(index: number) {
  rejectComments.value.splice(index, 1)
}

function applyTemplateToReject(tpl: ReviewTemplateDTO) {
  rejectCommentContent.value = tpl.content
  rejectCommentPriority.value = tpl.priority
}

async function submitReject(id: number) {
  if (mustFixCount.value === 0) {
    ElMessage.warning('驳回时必须至少有一条「必须修改」意见')
    return
  }
  try {
    await reviewAction(id, {
      action: 'REJECT',
      comments: rejectComments.value,
    })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    selectedDetail.value = null
    revisionDiff.value = null
    await loadPending()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function openCommentDialog(paragraphIndex?: number) {
  newCommentContent.value = ''
  newCommentPriority.value = 'MUST_FIX'
  newCommentParagraph.value = paragraphIndex ?? null
  commentDialogVisible.value = true
}

function applyTemplate(tpl: ReviewTemplateDTO) {
  newCommentContent.value = tpl.content
  newCommentPriority.value = tpl.priority
}

async function submitComment() {
  if (!selectedDetail.value) return
  if (!newCommentContent.value.trim()) {
    ElMessage.warning('请输入批注内容')
    return
  }
  try {
    await addReviewComment(selectedDetail.value.id, {
      content: newCommentContent.value.trim(),
      priority: newCommentPriority.value,
      paragraphIndex: newCommentParagraph.value,
    })
    ElMessage.success('批注已添加')
    commentDialogVisible.value = false
    await loadReviewHistory(selectedDetail.value.id)
  } catch {
    ElMessage.error('添加批注失败')
  }
}

function scrollToParagraph(index: number) {
  const el = document.getElementById(`paragraph-${index}`)
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

async function handleBatchApprove() {
  if (!hasSelection.value) {
    ElMessage.warning('请先选择文章')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定批量通过选中的 ${selectedIds.value.length} 篇文章吗？`,
      '批量通过',
      { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
    await batchReview({ postIds: selectedIds.value, action: 'APPROVE' })
    ElMessage.success('批量通过成功')
    selectedIds.value = []
    selectedDetail.value = null
    await loadPending()
  } catch {
    // cancelled or failed
  }
}

function openBatchReject() {
  if (!hasSelection.value) {
    ElMessage.warning('请先选择文章')
    return
  }
  batchRejectReason.value = ''
  batchRejectDialog.value = true
}

async function handleBatchReject() {
  if (!batchRejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  batchRejectLoading.value = true
  try {
    await batchReview({
      postIds: selectedIds.value,
      action: 'REJECT',
      reason: batchRejectReason.value.trim(),
    })
    ElMessage.success('批量拒绝成功')
    batchRejectDialog.value = false
    selectedIds.value = []
    selectedDetail.value = null
    await loadPending()
  } catch {
    ElMessage.error('批量拒绝失败')
  } finally {
    batchRejectLoading.value = false
  }
}

function openTemplateDialog(tpl?: ReviewTemplateDTO) {
  if (tpl) {
    templateForm.value = { id: tpl.id, name: tpl.name, content: tpl.content, priority: tpl.priority }
  } else {
    templateForm.value = { id: null, name: '', content: '', priority: 'SUGGESTION' }
  }
  templateDialogVisible.value = true
}

async function saveTemplate() {
  if (!templateForm.value.name.trim() || !templateForm.value.content.trim()) {
    ElMessage.warning('模板名称和内容不能为空')
    return
  }
  templateLoading.value = true
  try {
    if (templateForm.value.id) {
      await updateReviewTemplate(templateForm.value.id, {
        name: templateForm.value.name,
        content: templateForm.value.content,
        priority: templateForm.value.priority,
      })
    } else {
      await createReviewTemplate({
        name: templateForm.value.name,
        content: templateForm.value.content,
        priority: templateForm.value.priority,
      })
    }
    ElMessage.success('模板已保存')
    templateDialogVisible.value = false
    await loadTemplates()
  } catch {
    ElMessage.error('保存模板失败')
  } finally {
    templateLoading.value = false
  }
}

async function handleDeleteTemplate(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此模板吗？', '确认删除', { type: 'warning' })
    await deleteReviewTemplate(id)
    ElMessage.success('模板已删除')
    await loadTemplates()
  } catch {
    // cancelled or failed
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

onMounted(() => {
  loadPending()
  loadTemplates()
})
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>审核队列</h1>
      <p class="admin-subtitle">审核待发布的文章，支持批注、模板与时间线追踪。</p>
    </header>

    <section class="review-layout">
      <div class="review-left">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-title-wrap">
              <strong class="panel-title">待审核文章</strong>
              <div v-if="hasSelection" class="batch-actions">
                <el-button type="success" size="small" @click="handleBatchApprove">
                  批量通过 ({{ selectedIds.length }})
                </el-button>
                <el-button type="danger" size="small" @click="openBatchReject">
                  批量拒绝 ({{ selectedIds.length }})
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            class="admin-table"
            v-loading="loading"
            :data="pendingPosts"
            stripe
            highlight-current-row
            @selection-change="handleSelectionChange"
            @row-click="handleRowClick"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                {{ row.title }}
                <el-tag v-if="row.revision" size="small" type="warning" style="margin-left: 4px">修订</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="authorName" label="作者" width="120" />
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                {{ dayjs(row.createdAt).format('YYYY-MM-DD HH:mm') }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button class="action-btn" type="success" link size="small" @click.stop="handleApprove(row.id)">通过</el-button>
                <el-button class="action-btn" type="danger" link size="small" @click.stop="openRejectDialog(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="total > size" class="pagination-row" style="margin-top: 16px">
            <el-pagination
              v-model:current-page="page"
              :page-size="size"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </div>

      <div class="review-right">
        <el-card v-if="!selectedDetail" class="panel-card review-empty-card">
          <div class="review-empty">
            <p>点击左侧文章查看详情</p>
          </div>
        </el-card>

        <el-card v-else class="panel-card" v-loading="detailLoading">
          <template #header>
            <div class="panel-title-wrap">
              <strong class="panel-title">
                {{ selectedDetail.title }}
                <el-tag v-if="selectedDetail.revision" size="small" type="warning" style="margin-left: 8px">修订版本</el-tag>
                <el-tag v-else size="small" type="info" style="margin-left: 8px">新文章</el-tag>
              </strong>
              <div class="detail-actions">
                <el-button type="primary" size="small" @click="openCommentDialog()">添加批注</el-button>
                <el-button type="success" size="small" @click="handleApprove(selectedDetail!.id)">通过</el-button>
                <el-button type="danger" size="small" @click="openRejectDialog(selectedDetail!.id)">驳回</el-button>
              </div>
            </div>
          </template>

          <div class="detail-meta">
            <span>作者：{{ selectedDetail.authorName }}</span>
            <span>分类：{{ selectedDetail.categoryPath || '未分类' }}</span>
            <span>提交时间：{{ dayjs(selectedDetail.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
            <span v-if="selectedDetail.scheduledAt">预约上线：{{ dayjs(selectedDetail.scheduledAt).format('YYYY-MM-DD HH:mm') }}</span>
          </div>

          <div v-if="selectedDetail.keywords && selectedDetail.keywords.length" class="detail-keywords">
            <el-tag v-for="kw in selectedDetail.keywords" :key="kw.id" size="small" style="margin-right: 4px">{{ kw.name }}</el-tag>
          </div>

          <el-tabs v-model="activeTab" class="review-tabs">
            <el-tab-pane label="文章内容" name="content">
              <template v-if="revisionDiff">
                <h4 class="diff-title">修订内容对比</h4>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="标题（原）">{{ revisionDiff.parentTitle }}</el-descriptions-item>
                  <el-descriptions-item label="标题（修订）">{{ revisionDiff.revisionTitle }}</el-descriptions-item>
                  <el-descriptions-item label="分类（原）">{{ revisionDiff.parentCategoryPath || '未分类' }}</el-descriptions-item>
                  <el-descriptions-item label="分类（修订）">{{ revisionDiff.revisionCategoryPath || '未分类' }}</el-descriptions-item>
                  <el-descriptions-item label="关键词（原）">{{ revisionDiff.parentKeywords || '无' }}</el-descriptions-item>
                  <el-descriptions-item label="关键词（修订）">{{ revisionDiff.revisionKeywords || '无' }}</el-descriptions-item>
                </el-descriptions>
                <div class="diff-content-wrap">
                  <div class="diff-col">
                    <h5>原文内容</h5>
                    <div class="diff-content">{{ revisionDiff.parentContent }}</div>
                  </div>
                  <div class="diff-col">
                    <h5>修订内容</h5>
                    <div class="diff-content">{{ revisionDiff.revisionContent }}</div>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="content-with-annotations">
                  <div
                    v-for="(para, idx) in contentParagraphs"
                    :key="idx"
                    :id="`paragraph-${idx}`"
                    class="paragraph-block"
                    @click="openCommentDialog(idx)"
                  >
                    <span class="paragraph-index">{{ idx + 1 }}</span>
                    <span class="paragraph-text">{{ para }}</span>
                  </div>
                </div>
              </template>
            </el-tab-pane>

            <el-tab-pane label="审查意见" name="comments">
              <div v-if="reviewHistory.length === 0" class="empty-hint">暂无审查意见</div>
              <div v-for="round in reviewHistory" :key="round.id" class="round-block">
                <div class="round-header">
                  <el-tag :type="round.result === 'APPROVED' ? 'success' : 'danger'" size="small">
                    {{ round.result === 'APPROVED' ? '通过' : '驳回' }}
                  </el-tag>
                  <span class="round-meta">
                    {{ round.reviewerName }} · {{ dayjs(round.createdAt).format('YYYY-MM-DD HH:mm') }}
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
                    <el-tag v-if="comment.authorResolved" type="success" size="small">已处理</el-tag>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                  <div v-if="comment.authorReply" class="comment-reply">
                    <strong>作者回复：</strong>{{ comment.authorReply }}
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

            <el-tab-pane label="审查模板" name="templates">
              <div class="template-header">
                <strong>审查意见模板</strong>
                <el-button type="primary" size="small" @click="openTemplateDialog()">新建模板</el-button>
              </div>
              <div v-if="templates.length === 0" class="empty-hint">暂无模板，点击上方按钮创建</div>
              <div v-for="tpl in templates" :key="tpl.id" class="template-item">
                <div class="template-info">
                  <strong>{{ tpl.name }}</strong>
                  <el-tag :type="tpl.priority === 'MUST_FIX' ? 'danger' : 'warning'" size="small" style="margin-left: 8px">
                    {{ tpl.priority === 'MUST_FIX' ? '必须修改' : '建议优化' }}
                  </el-tag>
                </div>
                <div class="template-content">{{ tpl.content }}</div>
                <div class="template-actions">
                  <el-button link size="small" type="primary" @click="openTemplateDialog(tpl)">编辑</el-button>
                  <el-button link size="small" type="danger" @click="handleDeleteTemplate(tpl.id)">删除</el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="rejectDialogVisible" title="驳回文章" width="640px" top="5vh">
      <div class="reject-form">
        <el-alert
          v-if="mustFixCount === 0"
          title="驳回时必须至少有一条「必须修改」意见"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />

        <div class="comment-input-row">
          <el-select v-model="rejectCommentPriority" style="width: 120px">
            <el-option label="必须修改" value="MUST_FIX" />
            <el-option label="建议优化" value="SUGGESTION" />
          </el-select>
          <el-input-number
            v-model="rejectCommentParagraph"
            :min="0"
            :max="contentParagraphs.length - 1"
            placeholder="段落"
            style="width: 100px"
          />
          <el-input
            v-model="rejectCommentContent"
            placeholder="输入批注内容..."
            style="flex: 1"
            @keyup.enter="addRejectComment"
          />
          <el-button type="primary" @click="addRejectComment">添加</el-button>
        </div>

        <div v-if="templates.length > 0" class="template-quick-bar">
          <span class="quick-label">快速引用：</span>
          <el-tag
            v-for="tpl in templates"
            :key="tpl.id"
            class="quick-tag"
            size="small"
            @click="applyTemplateToReject(tpl)"
          >
            {{ tpl.name }}
          </el-tag>
        </div>

        <div v-if="rejectComments.length > 0" class="reject-comments-list">
          <div v-for="(c, idx) in rejectComments" :key="idx" class="reject-comment-item">
            <el-tag :type="c.priority === 'MUST_FIX' ? 'danger' : 'warning'" size="small">
              {{ c.priority === 'MUST_FIX' ? '必须修改' : '建议优化' }}
            </el-tag>
            <span v-if="c.paragraphIndex != null" class="comment-para">段落 {{ c.paragraphIndex + 1 }}</span>
            <span class="comment-text">{{ c.content }}</span>
            <el-button link type="danger" size="small" @click="removeRejectComment(idx)">删除</el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :disabled="mustFixCount === 0" @click="submitReject(selectedDetail!.id)">
          确认驳回（{{ mustFixCount }} 条必须修改）
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="commentDialogVisible" title="添加批注" width="500px">
      <el-form label-position="top">
        <el-form-item label="优先级">
          <el-radio-group v-model="newCommentPriority">
            <el-radio value="MUST_FIX">必须修改</el-radio>
            <el-radio value="SUGGESTION">建议优化</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="newCommentParagraph != null" label="段落位置">
          <el-tag>段落 {{ newCommentParagraph + 1 }}</el-tag>
        </el-form-item>
        <el-form-item label="批注内容">
          <el-input v-model="newCommentContent" type="textarea" :rows="3" placeholder="输入批注内容..." />
        </el-form-item>
        <div v-if="templates.length > 0" class="template-quick-bar">
          <span class="quick-label">快速引用：</span>
          <el-tag
            v-for="tpl in templates"
            :key="tpl.id"
            class="quick-tag"
            size="small"
            @click="applyTemplate(tpl)"
          >
            {{ tpl.name }}
          </el-tag>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment">添加批注</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" :title="templateForm.id ? '编辑模板' : '新建模板'" width="480px">
      <el-form label-position="top">
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.name" placeholder="例如：格式不规范" />
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="templateForm.content" type="textarea" :rows="3" placeholder="例如：文章格式需要调整，请检查标题层级" />
        </el-form-item>
        <el-form-item label="默认优先级">
          <el-radio-group v-model="templateForm.priority">
            <el-radio value="MUST_FIX">必须修改</el-radio>
            <el-radio value="SUGGESTION">建议优化</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateLoading" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchRejectDialog" title="批量拒绝" width="480px">
      <p>将拒绝选中的 {{ selectedIds.length }} 篇文章，请输入拒绝原因：</p>
      <el-input
        v-model="batchRejectReason"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝原因"
        style="margin-top: 12px"
      />
      <template #footer>
        <el-button @click="batchRejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="batchRejectLoading" @click="handleBatchReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  width: 100%;
}

.review-empty-card {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  color: var(--color-text-3);
  font-size: 14px;
}

.batch-actions {
  display: flex;
  gap: var(--space-2);
}

.detail-actions {
  display: flex;
  gap: var(--space-1);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  color: var(--color-text-3);
  font-size: 13px;
}

.detail-keywords {
  margin-top: var(--space-2);
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.review-tabs {
  margin-top: var(--space-3);
}

.content-with-annotations {
  max-height: 60vh;
  overflow-y: auto;
}

.paragraph-block {
  position: relative;
  padding: var(--space-2) var(--space-2) var(--space-2) 36px;
  margin-bottom: var(--space-1);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.2s;
  line-height: 1.8;
  font-size: 15px;
  color: var(--color-text-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.paragraph-block:hover {
  background-color: #f0f7ff;
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

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.template-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-2) var(--space-3);
  margin-bottom: var(--space-2);
}

.template-info {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.template-content {
  font-size: 13px;
  color: var(--color-text-3);
  margin-bottom: 4px;
}

.template-actions {
  display: flex;
  gap: var(--space-1);
}

.reject-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.comment-input-row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.template-quick-bar {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.quick-label {
  font-size: 13px;
  color: var(--color-text-3);
  margin-right: 4px;
}

.quick-tag {
  cursor: pointer;
  transition: opacity 0.2s;
}

.quick-tag:hover {
  opacity: 0.8;
}

.reject-comments-list {
  max-height: 200px;
  overflow-y: auto;
}

.reject-comment-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  border-bottom: 1px solid #f0f0f0;
}

.reject-comment-item:last-child {
  border-bottom: none;
}

.comment-text {
  flex: 1;
  font-size: 13px;
}

.empty-hint {
  color: var(--color-text-3);
  font-size: 14px;
  text-align: center;
  padding: var(--space-6) 0;
}

.diff-title {
  margin: 0 0 var(--space-3);
  font-size: 15px;
  font-weight: 600;
}

.diff-content-wrap {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.diff-col h5 {
  margin: 0 0 var(--space-2);
  font-size: 13px;
  color: var(--color-text-3);
}

.diff-content {
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 40vh;
  overflow-y: auto;
  padding: var(--space-2);
  background: #fafbfc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

@media (max-width: 1023px) {
  .review-layout {
    grid-template-columns: 1fr;
  }
  .diff-content-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
