export interface AuthUser {
  id: number
  username: string
  role: string
  nickname: string
  avatarUrl: string | null
  readerLevel?: number | null
}

export interface LoginPayload {
  username: string
  password: string
}

export interface CreatePostPayload {
  title: string
  content: string
  categoryId?: number | null
  keywords?: string[]
  scheduledAt?: string | null
}

export interface UpdatePostPayload {
  title: string
  content: string
  categoryId?: number | null
  keywords?: string[]
  scheduledAt?: string | null
}

export interface PostSummary {
  id: number
  title: string
  excerpt: string
  previewContent: string
  authorId: number
  authorName: string
  authorAvatar: string | null
  status: string
  categoryId: number | null
  categoryPath: string
  keywords: KeywordItem[]
  viewCount: number
  favoriteCount: number
  scheduledAt: string | null
  revision: boolean
  parentPostId: number | null
  hasRevision: boolean
  createdAt: string
}

export interface PostDetail {
  id: number
  title: string
  content: string
  authorId: number
  authorName: string
  authorAvatar: string | null
  status: string
  rejectionReason: string | null
  categoryId: number | null
  categoryPath: string
  categoryBreadcrumb: BreadcrumbItem[]
  keywords: KeywordItem[]
  viewCount: number
  scheduledAt: string | null
  revision: boolean
  parentPostId: number | null
  hasRevision: boolean
  createdAt: string
  updatedAt: string | null
}

export interface BreadcrumbItem {
  id: number
  name: string
  slug: string
}

export interface KeywordItem {
  id: number
  name: string
  usageCount: number
  lastUsedAt: string | null
  archived: boolean
  createdAt: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface ApiError {
  code: string
  message: string
  traceId: string
}

export interface TokenResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
}

export interface Category {
  id: number
  name: string
  slug: string
  parentId: number | null
  parentName: string | null
  enabled: boolean
  sortOrder: number
  postCount: number
  createdAt: string
  updatedAt: string
  children: Category[]
}

export interface Keyword {
  id: number
  name: string
  usageCount: number
  lastUsedAt: string | null
  archived: boolean
  createdAt: string
}

export interface KeywordCloud {
  id: number
  name: string
  usageCount: number
  heatScore: number
  lastUsedAt: string | null
}

export interface CreateCategoryPayload {
  name: string
  slug?: string
  parentId?: number | null
  sortOrder?: number
}

export interface UpdateCategoryPayload {
  name: string
  slug?: string
  parentId?: number | null
  enabled?: boolean
  sortOrder?: number
}

export interface BatchUpdateCategoryPayload {
  postIds: number[]
  categoryId: number
}

export interface BatchAddKeywordsPayload {
  postIds: number[]
  keywords: string[]
}

export interface AuthorDTO {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  enabled: boolean
  role: string
  createdAt: string
  postCount: number
  lastLoginAt: string | null
}

export interface AuthorPublicDTO {
  id: number
  nickname: string
  username: string
  avatarUrl: string | null
  bio: string | null
  postCount: number
}

export interface AuthorStatsDTO {
  totalPublished: number
  totalDraft: number
  totalViews: number
  avgViews: number
  topPosts: PostSummary[]
}

export interface CreateAuthorPayload {
  username: string
  password: string
  nickname?: string
  avatarUrl?: string
  bio?: string
}

export interface UpdateAuthorPayload {
  nickname?: string
  avatarUrl?: string
  bio?: string
}

export interface UpdateProfilePayload {
  nickname?: string
  avatarUrl?: string
  bio?: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface ReviewActionPayload {
  action: string
  reason?: string
  comments?: CreateReviewCommentPayload[]
}

export interface CreateReviewCommentPayload {
  content: string
  priority?: string
  paragraphIndex?: number | null
  positionStart?: number | null
  positionEnd?: number | null
}

export interface ReviewCommentDTO {
  id: number
  roundId: number
  content: string
  priority: string
  paragraphIndex: number | null
  positionStart: number | null
  positionEnd: number | null
  authorReply: string | null
  authorResolved: boolean
  authorRepliedAt: string | null
  createdAt: string
}

export interface ReviewRoundDTO {
  id: number
  postId: number
  reviewerId: number
  reviewerName: string
  result: string | null
  modificationNote: string | null
  createdAt: string
  comments: ReviewCommentDTO[]
}

export interface ReviewTemplateDTO {
  id: number
  name: string
  content: string
  priority: string
  createdAt: string
  updatedAt: string | null
}

export interface CreateReviewTemplatePayload {
  name: string
  content: string
  priority?: string
}

export interface UpdateReviewTemplatePayload {
  name: string
  content: string
  priority?: string
}

export interface ReviewTimelineDTO {
  postId: number
  postTitle: string
  postStatus: string
  events: TimelineEventDTO[]
}

export interface TimelineEventDTO {
  type: string
  roundId: number | null
  userId: number
  userName: string
  action: string
  content: string | null
  timestamp: string
}

export interface ResubmitWithNotePayload {
  modificationNote: string
}

export interface AuthorReplyPayload {
  reply: string
}

export interface BatchReviewPayload {
  postIds: number[]
  action: string
  reason?: string
}

export interface AdminStatsDTO {
  totalPosts: number
  pendingPosts: number
  totalAuthors: number
  monthlyNewPosts: number
  totalViews: number
  scheduledPosts: number
  pendingRevisions: number
  authorRanking: AuthorRankItem[]
}

export interface AuthorRankItem {
  id: number
  nickname: string
  avatarUrl: string | null
  postCount: number
  totalViews: number
}

export interface RevisionDiffDTO {
  revisionId: number
  parentPostId: number
  parentTitle: string
  revisionTitle: string
  parentContent: string
  revisionContent: string
  parentCategoryId: number | null
  revisionCategoryId: number | null
  parentCategoryPath: string
  revisionCategoryPath: string
  parentKeywords: string
  revisionKeywords: string
  status: string
  authorName: string
}

export interface ScheduledTaskDTO {
  postId: number
  title: string
  authorId: number
  authorName: string
  scheduledAt: string
  isRevision: boolean
  parentPostId: number | null
  status: string
}

export interface ImageDTO {
  id: number
  filename: string
  originalName: string | null
  url: string
  authorId: number
  contentType: string
  fileSize: number
  width: number | null
  height: number | null
  createdAt: string
  referencedPosts: PostSummary[] | null
}

export interface CaptchaDTO {
  key: string
  svgDataUrl: string
}

export interface RegisterReaderPayload {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
  nickname?: string
  securityQuestion?: string
  securityAnswer?: string
}

export interface ReaderProfileDTO {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  role: string
  readerLevel: number
  readerExp: number
  showFootprint: boolean
  streakDays: number
  readCount: number
  favoriteCount: number
  commentCount: number
  subscriptionCount: number
  maxCommentLength: number
  commentIntervalSeconds: number
}

export interface ReaderPublicProfileDTO {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  readerLevel: number
  streakDays: number
  readCount: number
  favoriteCount: number
  commentCount: number
  subscriptionCount: number
}

export interface UpdateReaderProfilePayload {
  nickname?: string
  avatarUrl?: string
  bio?: string
  showFootprint?: boolean
}

export interface ReadHistoryItem {
  id: number
  userId: number
  postId: number
  readAt: string
}

export interface FavoriteItem {
  id: number
  userId: number
  postId: number
  createdAt: string
}

export interface SubscriptionItem {
  id: number
  readerId: number
  authorId: number
  createdAt: string
}

export interface SecurityQuestionDTO {
  username: string
  securityQuestion: string
}

export interface ResetPasswordPayload {
  username: string
  securityAnswer: string
  newPassword: string
}

export interface CollectionAlbum {
  id: number
  name: string
  description: string | null
  coverUrl: string | null
  isPublic: boolean
  sortOrder: number
  shareToken: string | null
  itemCount: number
  createdAt: string
  updatedAt: string | null
}

export interface CollectionItem {
  id: number
  albumId: number
  postId: number
  postTitle: string
  postExcerpt: string
  postAuthorName: string
  note: string | null
  sortOrder: number
  postDeleted: boolean
  createdAt: string
}

export interface CreateAlbumPayload {
  name: string
  description?: string
  coverUrl?: string
  isPublic?: boolean
}

export interface UpdateAlbumPayload {
  name?: string
  description?: string
  coverUrl?: string
  isPublic?: boolean
}

export interface AddToAlbumPayload {
  postId: number
  note?: string
}

export interface BatchCollectionActionPayload {
  itemIds: number[]
  targetAlbumId?: number
  action: string
}

export interface ReorderItemsPayload {
  itemIds: number[]
}

export interface ReadHistoryDetail {
  id: number
  postId: number
  postTitle: string
  postExcerpt: string
  postAuthorName: string
  postDeleted: boolean
  durationSeconds: number
  scrollPosition: number
  readAt: string
}

export interface FavoriteDetail {
  id: number
  postId: number
  postTitle: string
  postExcerpt: string
  postAuthorName: string
  postDeleted: boolean
  note: string | null
  createdAt: string
}

export interface PostFavoriteCount {
  postId: number
  postTitle: string
  favoriteCount: number
}

export interface ReadingProgress {
  postId: number
  durationSeconds: number
  scrollPosition: number
  readAt: string
}

export interface CommentTreeDTO {
  id: number
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
  postId: number
  parentId: number | null
  replyToCommentId: number | null
  replyToUserId: number | null
  replyToUsername: string | null
  replyToNickname: string | null
  depth: number
  content: string
  upvotes: number
  downvotes: number
  currentUserVote: number | null
  pinned: boolean
  edited: boolean
  editedAt: string | null
  deleted: boolean
  deletedBy: string | null
  hasImages: boolean
  hasCode: boolean
  canEdit: boolean
  canDelete: boolean
  createdAt: string
  replies: CommentTreeDTO[]
}

export interface CreateDiscussionRequest {
  content: string
  parentId?: number | null
  replyToCommentId?: number | null
  replyToUserId?: number | null
}

export interface UpdateDiscussionRequest {
  content: string
}

export interface CommentVoteRequest {
  voteType: number
}

export interface CommentVoteResultDTO {
  commentId: number
  upvotes: number
  downvotes: number
  currentUserVote: number | null
}

export interface CommentReportDTO {
  id: number
  commentId: number
  commentContent: string
  commentAuthorId: number
  commentAuthorUsername: string
  reporterId: number
  reporterUsername: string
  reporterNickname: string
  reason: string
  description: string | null
  status: string
  reviewedBy: number | null
  reviewerUsername: string | null
  reviewedAt: string | null
  resolution: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateCommentReportRequest {
  reason: string
  description?: string
}

export interface PinnedCommentResultDTO {
  commentId: number
  pinned: boolean
}

export interface PostCommentSettingsDTO {
  id: number
  postId: number
  accessRule: string
  requireApproval: boolean
  maxDepth: number
  maxLength: number
  allowImages: boolean
  allowCode: boolean
  allowEmojis: boolean
  allowMentions: boolean
  createdAt: string
  updatedAt: string
}

export interface UpdatePostCommentSettingsRequest {
  accessRule?: string
  requireApproval?: boolean
  maxDepth?: number
  maxLength?: number
  allowImages?: boolean
  allowCode?: boolean
  allowEmojis?: boolean
  allowMentions?: boolean
}

export interface NotificationDTO {
  id: number
  type: string
  relatedCommentId: number | null
  relatedPostId: number
  relatedPostTitle: string
  relatedUserId: number | null
  relatedUsername: string | null
  relatedNickname: string | null
  relatedAvatarUrl: string | null
  title: string
  content: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface UnreadNotificationCountDTO {
  total: number
  replyCount: number
  mentionCount: number
  upvoteCount: number
  reportResultCount: number
}

export interface DiscussionListResponse {
  items: CommentTreeDTO[]
  total: number
  page: number
  size: number
  totalPages: number
  hasMore: boolean
}

export interface NotificationListResponse {
  items: NotificationDTO[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface MessageResponse {
  message: string
}
