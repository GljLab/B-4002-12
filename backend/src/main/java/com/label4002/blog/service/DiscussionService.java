package com.label4002.blog.service;

import com.label4002.blog.dto.CommentReportDTO;
import com.label4002.blog.dto.CommentTreeDTO;
import com.label4002.blog.dto.CommentVoteResultDTO;
import com.label4002.blog.dto.CreateCommentReportRequest;
import com.label4002.blog.dto.CreateDiscussionRequest;
import com.label4002.blog.dto.PinnedCommentResultDTO;
import com.label4002.blog.dto.PostCommentSettingsDTO;
import com.label4002.blog.dto.ReviewCommentReportRequest;
import com.label4002.blog.dto.UpdateDiscussionRequest;
import com.label4002.blog.dto.UpdatePostCommentSettingsRequest;
import com.label4002.blog.entity.CommentEntity;
import com.label4002.blog.entity.CommentRateLimitEntity;
import com.label4002.blog.entity.CommentReportEntity;
import com.label4002.blog.entity.CommentVoteEntity;
import com.label4002.blog.entity.PostCommentSettingsEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.entity.UserRole;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CommentRateLimitRepository;
import com.label4002.blog.repository.CommentReportRepository;
import com.label4002.blog.repository.CommentRepository;
import com.label4002.blog.repository.CommentVoteRepository;
import com.label4002.blog.repository.PostCommentSettingsRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.SubscriptionRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final int MAX_COMMENTS_PER_MINUTE = 3;
    private static final int EDIT_WINDOW_MINUTES = 5;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]{2,50})");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\(.*?\\)|<img[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_PATTERN = Pattern.compile("```[\\s\\S]*?```|<code[^>]*>[\\s\\S]*?</code>", Pattern.CASE_INSENSITIVE);
    private static final String DELETED_PLACEHOLDER = "此讨论已移除";
    private static final int ROOT_PAGE_SIZE = 20;
    private static final int REPLIES_PAGE_SIZE = 10;

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentRateLimitRepository commentRateLimitRepository;
    private final PostCommentSettingsRepository postCommentSettingsRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SensitiveWordService sensitiveWordService;
    private final NotificationService notificationService;
    private final ReaderService readerService;

    public DiscussionService(CommentRepository commentRepository,
                             CommentVoteRepository commentVoteRepository,
                             CommentReportRepository commentReportRepository,
                             CommentRateLimitRepository commentRateLimitRepository,
                             PostCommentSettingsRepository postCommentSettingsRepository,
                             PostRepository postRepository,
                             UserRepository userRepository,
                             SubscriptionRepository subscriptionRepository,
                             SensitiveWordService sensitiveWordService,
                             NotificationService notificationService,
                             ReaderService readerService) {
        this.commentRepository = commentRepository;
        this.commentVoteRepository = commentVoteRepository;
        this.commentReportRepository = commentReportRepository;
        this.commentRateLimitRepository = commentRateLimitRepository;
        this.postCommentSettingsRepository = postCommentSettingsRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.sensitiveWordService = sensitiveWordService;
        this.notificationService = notificationService;
        this.readerService = readerService;
    }

    @Transactional
    public CommentTreeDTO createDiscussion(Long userId, Long postId, CreateDiscussionRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ForbiddenException("只能对已公开的文章发起讨论");
        }

        PostCommentSettingsEntity settings = getOrCreateSettings(postId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        checkAccessPermission(userId, post, settings);

        checkRateLimit(userId);

        String content = request.content();
        int maxLen = settings.getMaxLength();
        if (content.length() > maxLen) {
            throw new BadRequestException("讨论内容长度不能超过" + maxLen + "字");
        }

        int depth = 0;
        Long parentId = request.parentId();
        String path = "";
        Integer sortOrder;

        if (parentId != null) {
            CommentEntity parent = commentRepository.findByIdAndDeletedFalse(parentId)
                    .orElseThrow(() -> new NotFoundException("父讨论不存在"));
            if (!parent.getPostId().equals(postId)) {
                throw new BadRequestException("父讨论不属于该文章");
            }
            depth = parent.getDepth() + 1;
            if (depth >= settings.getMaxDepth()) {
                throw new BadRequestException("已达到最大嵌套层数(" + settings.getMaxDepth() + "层)");
            }
            path = parent.getPath() + parent.getId() + "/";
            Integer maxSort = commentRepository.findMaxSortOrderByParentId(parentId);
            sortOrder = (maxSort == null ? 0 : maxSort) + 1;
        } else {
            Integer maxSort = commentRepository.findMaxSortOrderByPostIdRoot(postId);
            sortOrder = (maxSort == null ? 0 : maxSort) + 1;
        }

        Long replyToCommentId = request.replyToCommentId();
        Long replyToUserId = request.replyToUserId();
        if (replyToCommentId != null && parentId == null) {
            throw new BadRequestException("针对性回复需要指定父讨论");
        }
        if (replyToCommentId != null) {
            CommentEntity replyTo = commentRepository.findByIdAndDeletedFalse(replyToCommentId)
                    .orElseThrow(() -> new NotFoundException("被回复的讨论不存在"));
            replyToUserId = replyTo.getUserId();
        }

        boolean hasImages = settings.isAllowImages() && IMAGE_PATTERN.matcher(content).find();
        boolean hasCode = settings.isAllowCode() && CODE_PATTERN.matcher(content).find();

        if (!settings.isAllowImages() && IMAGE_PATTERN.matcher(content).find()) {
            throw new BadRequestException("当前文章不允许插入图片");
        }
        if (!settings.isAllowCode() && CODE_PATTERN.matcher(content).find()) {
            throw new BadRequestException("当前文章不允许插入代码片段");
        }

        SensitiveWordService.FilterResult filterResult = sensitiveWordService.filter(content);
        if (filterResult.maxSeverity() >= 2) {
            throw new BadRequestException("内容包含违规词汇，请修改后重试");
        }
        content = filterResult.filteredContent();

        List<Long> mentionedUserIds = new ArrayList<>();
        if (settings.isAllowMentions()) {
            mentionedUserIds = extractMentions(content, user);
        }

        CommentEntity entity = new CommentEntity();
        entity.setUserId(userId);
        entity.setPostId(postId);
        entity.setParentId(parentId);
        entity.setReplyToCommentId(replyToCommentId);
        entity.setReplyToUserId(replyToUserId);
        entity.setDepth(depth);
        entity.setPath(path);
        entity.setSortOrder(sortOrder);
        entity.setContent(content);
        entity.setUpvotes(0);
        entity.setDownvotes(0);
        entity.setPinned(false);
        entity.setEdited(false);
        entity.setDeleted(false);
        entity.setHasImages(hasImages);
        entity.setHasCode(hasCode);
        entity.setMentions(mentionedUserIds.isEmpty() ? null : mentionedUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        entity.setVersion(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity = commentRepository.save(entity);

        recordRateLimit(userId);
        readerService.addExperience(userId, 5);

        if (parentId != null && replyToUserId != null) {
            notificationService.createReplyNotification(replyToUserId, userId, entity.getId(), postId, content);
        } else if (parentId != null) {
            CommentEntity parent = commentRepository.findById(parentId).orElse(null);
            if (parent != null) {
                notificationService.createReplyNotification(parent.getUserId(), userId, entity.getId(), postId, content);
            }
        }
        if (!mentionedUserIds.isEmpty()) {
            notificationService.createMentionNotifications(mentionedUserIds, userId, entity.getId(), postId, content);
        }

        if (filterResult.maxSeverity() >= 3 && !filterResult.matchedWords().isEmpty()) {
            autoReportForSeverity(entity, filterResult.matchedWords());
        }

        Map<Long, UserEntity> userCache = new HashMap<>();
        userCache.put(userId, user);
        return toTreeDTO(entity, userCache, Collections.emptyMap(), userId, settings.getMaxDepth() - 1);
    }

    @Transactional
    public CommentTreeDTO updateDiscussion(Long userId, Long commentId, UpdateDiscussionRequest request) {
        CommentEntity entity = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        if (!entity.getUserId().equals(userId)) {
            throw new ForbiddenException("只能编辑自己发布的讨论");
        }

        LocalDateTime editDeadline = entity.getCreatedAt().plusMinutes(EDIT_WINDOW_MINUTES);
        if (LocalDateTime.now().isAfter(editDeadline)) {
            throw new BadRequestException("编辑窗口已过（发布后" + EDIT_WINDOW_MINUTES + "分钟内可编辑）");
        }

        PostCommentSettingsEntity settings = getOrCreateSettings(entity.getPostId());

        String content = request.content();
        if (content.length() > settings.getMaxLength()) {
            throw new BadRequestException("讨论内容长度不能超过" + settings.getMaxLength() + "字");
        }

        boolean hasImages = settings.isAllowImages() && IMAGE_PATTERN.matcher(content).find();
        boolean hasCode = settings.isAllowCode() && CODE_PATTERN.matcher(content).find();

        SensitiveWordService.FilterResult filterResult = sensitiveWordService.filter(content);
        if (filterResult.maxSeverity() >= 2) {
            throw new BadRequestException("内容包含违规词汇，请修改后重试");
        }
        content = filterResult.filteredContent();

        UserEntity user = userRepository.findById(userId).orElse(null);
        List<Long> mentionedUserIds = settings.isAllowMentions() ? extractMentions(content, user) : new ArrayList<>();

        entity.setContent(content);
        entity.setEdited(true);
        entity.setEditedAt(LocalDateTime.now());
        entity.setHasImages(hasImages);
        entity.setHasCode(hasCode);
        entity.setMentions(mentionedUserIds.isEmpty() ? null : mentionedUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        entity.setVersion(entity.getVersion() + 1);
        entity = commentRepository.save(entity);

        if (!mentionedUserIds.isEmpty()) {
            notificationService.createMentionNotifications(mentionedUserIds, userId, entity.getId(), entity.getPostId(), content);
        }

        Map<Long, UserEntity> userCache = new HashMap<>();
        if (user != null) userCache.put(userId, user);
        return toTreeDTO(entity, userCache, Collections.emptyMap(), userId, settings.getMaxDepth() - entity.getDepth() - 1);
    }

    @Transactional
    public void deleteDiscussion(Long userId, Long commentId, boolean isAdmin) {
        CommentEntity entity = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        if (!isAdmin && !entity.getUserId().equals(userId)) {
            PostEntity post = postRepository.findById(entity.getPostId()).orElse(null);
            if (post == null || !post.getAuthor().getId().equals(userId)) {
                throw new ForbiddenException("无权限删除此讨论");
            }
        }

        String deletedBy = isAdmin ? "ADMIN" : "USER";
        commentRepository.softDelete(commentId, LocalDateTime.now(), deletedBy);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDiscussionTree(Long postId, Long currentUserId, String sortBy, int rootPage) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        PostCommentSettingsEntity settings = getOrCreateSettings(postId);

        List<CommentEntity> rootComments;
        switch (sortBy == null ? "newest" : sortBy.toLowerCase()) {
            case "popular", "popularity" -> rootComments = commentRepository.findRootCommentsByPostIdOrderByPopularity(postId);
            case "author", "author-only" -> {
                Long authorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
                rootComments = authorId != null
                        ? commentRepository.findRootCommentsByPostIdAndAuthor(postId, authorId)
                        : commentRepository.findRootCommentsByPostIdOrderByNewest(postId);
            }
            default -> rootComments = commentRepository.findRootCommentsByPostIdOrderByNewest(postId);
        }

        List<CommentEntity> allNonDeleted = commentRepository.findAllNonDeletedByPostId(postId);
        Map<Long, List<CommentEntity>> repliesByParent = allNonDeleted.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentEntity::getParentId));

        Set<Long> allUserIds = new HashSet<>();
        allUserIds.add(post.getAuthor() != null ? post.getAuthor().getId() : -1L);
        for (CommentEntity c : allNonDeleted) {
            allUserIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) allUserIds.add(c.getReplyToUserId());
        }
        allUserIds.remove(-1L);
        Map<Long, UserEntity> userCache = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        Map<Long, Integer> userVotes = new HashMap<>();
        if (currentUserId != null) {
            List<CommentVoteEntity> votes = commentVoteRepository.findByUserId(currentUserId);
            for (CommentVoteEntity v : votes) {
                userVotes.put(v.getCommentId(), v.getVoteType());
            }
        }

        int start = (Math.max(0, rootPage - 1)) * ROOT_PAGE_SIZE;
        int end = Math.min(start + ROOT_PAGE_SIZE, rootComments.size());
        List<CommentEntity> pagedRoots = start >= rootComments.size() ? Collections.emptyList() : rootComments.subList(start, end);

        List<CommentTreeDTO> trees = new ArrayList<>();
        int remainingDepth = settings.getMaxDepth() - 1;
        for (CommentEntity root : pagedRoots) {
            trees.add(buildTree(root, repliesByParent, userCache, userVotes, currentUserId, remainingDepth));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", trees);
        result.put("totalCount", rootComments.size());
        result.put("page", Math.max(1, rootPage));
        result.put("pageSize", ROOT_PAGE_SIZE);
        result.put("totalPages", (int) Math.ceil((double) rootComments.size() / ROOT_PAGE_SIZE));
        result.put("settings", toSettingsDTO(settings));
        result.put("sortBy", sortBy == null ? "newest" : sortBy);
        result.put("totalCommentsCount", commentRepository.countByPostIdAndDeletedFalse(postId));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMoreReplies(Long commentId, Long currentUserId, int page) {
        CommentEntity parent = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        PostCommentSettingsEntity settings = getOrCreateSettings(parent.getPostId());
        int maxRepliesDepth = settings.getMaxDepth() - parent.getDepth() - 1;

        Page<CommentEntity> pageResult = commentRepository.findRepliesByParentIdPaged(
                commentId, PageRequest.of(Math.max(0, page - 1), REPLIES_PAGE_SIZE));

        List<CommentEntity> allNonDeleted = commentRepository.findAllNonDeletedByPostId(parent.getPostId());
        Map<Long, List<CommentEntity>> repliesByParent = allNonDeleted.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentEntity::getParentId));

        Set<Long> allUserIds = new HashSet<>();
        for (CommentEntity c : pageResult.getContent()) {
            allUserIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) allUserIds.add(c.getReplyToUserId());
        }
        Map<Long, UserEntity> userCache = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        Map<Long, Integer> userVotes = new HashMap<>();
        if (currentUserId != null) {
            List<CommentVoteEntity> votes = commentVoteRepository.findByUserId(currentUserId);
            for (CommentVoteEntity v : votes) {
                userVotes.put(v.getCommentId(), v.getVoteType());
            }
        }

        List<CommentTreeDTO> trees = new ArrayList<>();
        for (CommentEntity reply : pageResult.getContent()) {
            trees.add(buildTree(reply, repliesByParent, userCache, userVotes, currentUserId, maxRepliesDepth));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", trees);
        result.put("totalCount", pageResult.getTotalElements());
        result.put("page", Math.max(1, page));
        result.put("pageSize", REPLIES_PAGE_SIZE);
        result.put("totalPages", pageResult.getTotalPages());
        return result;
    }

    @Transactional
    public CommentVoteResultDTO vote(Long userId, Long commentId, Integer voteType) {
        CommentEntity comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        if (voteType == null || (voteType != -1 && voteType != 0 && voteType != 1)) {
            throw new BadRequestException("投票类型只能是-1(反对)、0(取消)或1(支持)");
        }

        CommentVoteEntity existing = commentVoteRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);
        int oldVote = existing != null ? existing.getVoteType() : 0;

        if (oldVote == voteType) {
            return buildVoteResult(commentId, userId);
        }

        if (oldVote == 1) {
            commentRepository.adjustUpvotes(commentId, -1);
            comment.setUpvotes(Math.max(0, comment.getUpvotes() - 1));
        } else if (oldVote == -1) {
            commentRepository.adjustDownvotes(commentId, -1);
            comment.setDownvotes(Math.max(0, comment.getDownvotes() - 1));
        }

        if (voteType == 1) {
            commentRepository.adjustUpvotes(commentId, 1);
            comment.setUpvotes(comment.getUpvotes() + 1);
            if (oldVote != 1) {
                notificationService.createUpvoteNotification(comment.getUserId(), userId, commentId, comment.getPostId());
            }
        } else if (voteType == -1) {
            commentRepository.adjustDownvotes(commentId, 1);
            comment.setDownvotes(comment.getDownvotes() + 1);
        }

        if (voteType == 0) {
            if (existing != null) {
                commentVoteRepository.deleteByCommentIdAndUserId(commentId, userId);
            }
        } else {
            if (existing != null) {
                existing.setVoteType(voteType);
                commentVoteRepository.save(existing);
            } else {
                CommentVoteEntity v = new CommentVoteEntity();
                v.setCommentId(commentId);
                v.setUserId(userId);
                v.setVoteType(voteType);
                v.setCreatedAt(LocalDateTime.now());
                commentVoteRepository.save(v);
            }
        }

        return buildVoteResult(commentId, userId);
    }

    @Transactional
    public CommentReportDTO reportComment(Long userId, Long commentId, CreateCommentReportRequest request) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        CommentReportEntity entity = new CommentReportEntity();
        entity.setCommentId(commentId);
        entity.setReporterId(userId);
        entity.setReason(request.reason());
        entity.setDescription(request.description());
        entity.setStatus(CommentReportEntity.ReportStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity = commentReportRepository.save(entity);

        return toReportDTO(entity);
    }

    @Transactional
    public PinnedCommentResultDTO togglePin(Long userId, Long commentId) {
        CommentEntity comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("讨论不存在"));

        PostEntity post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("只有文章作者可以置顶讨论");
        }

        if (comment.getParentId() != null) {
            throw new BadRequestException("只能置顶主讨论");
        }

        comment.setPinned(!comment.isPinned());
        commentRepository.save(comment);

        return new PinnedCommentResultDTO(commentId, comment.isPinned());
    }

    @Transactional(readOnly = true)
    public PostCommentSettingsDTO getCommentSettings(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("文章不存在");
        }
        PostCommentSettingsEntity settings = getOrCreateSettings(postId);
        return toSettingsDTO(settings);
    }

    @Transactional
    public PostCommentSettingsDTO updateCommentSettings(Long userId, Long postId, UpdatePostCommentSettingsRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("只有文章作者可以修改讨论设置");
        }

        PostCommentSettingsEntity settings = getOrCreateSettings(postId);

        if (request.accessRule() != null) {
            try {
                settings.setAccessRule(PostCommentSettingsEntity.AccessRule.valueOf(request.accessRule().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("无效的访问规则: " + request.accessRule());
            }
        }
        if (request.requireApproval() != null) {
            settings.setRequireApproval(request.requireApproval());
        }
        if (request.maxDepth() != null) {
            settings.setMaxDepth(request.maxDepth());
        }
        if (request.maxLength() != null) {
            settings.setMaxLength(request.maxLength());
        }
        if (request.allowImages() != null) {
            settings.setAllowImages(request.allowImages());
        }
        if (request.allowCode() != null) {
            settings.setAllowCode(request.allowCode());
        }
        if (request.allowEmojis() != null) {
            settings.setAllowEmojis(request.allowEmojis());
        }
        if (request.allowMentions() != null) {
            settings.setAllowMentions(request.allowMentions());
        }

        settings = postCommentSettingsRepository.save(settings);
        return toSettingsDTO(settings);
    }

    @Transactional(readOnly = true)
    public Page<CommentReportDTO> getReports(String status, int page, int size) {
        Page<CommentReportEntity> entities;
        if (status != null && !status.isBlank()) {
            try {
                CommentReportEntity.ReportStatus s = CommentReportEntity.ReportStatus.valueOf(status.toUpperCase());
                entities = commentReportRepository.findByStatusOrderByCreatedAtDesc(s, PageRequest.of(page, size));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("无效的状态: " + status);
            }
        } else {
            entities = commentReportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        return entities.map(this::toReportDTO);
    }

    @Transactional
    public CommentReportDTO reviewReport(Long reviewerId, Long reportId, ReviewCommentReportRequest request) {
        CommentReportEntity report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("举报不存在"));

        if (report.getStatus() != CommentReportEntity.ReportStatus.PENDING) {
            throw new BadRequestException("该举报已处理");
        }

        String action = request.action().toUpperCase();
        CommentEntity comment = commentRepository.findById(report.getCommentId()).orElse(null);
        String resultText;

        switch (action) {
            case "DELETE", "REMOVE" -> {
                if (comment != null) {
                    commentRepository.softDelete(comment.getId(), LocalDateTime.now(), "ADMIN");
                }
                report.setStatus(CommentReportEntity.ReportStatus.RESOLVED);
                resultText = "已删除违规讨论";
            }
            case "WARNING" -> {
                report.setStatus(CommentReportEntity.ReportStatus.REVIEWED);
                resultText = "已发出警告";
            }
            case "REJECT", "DISMISS" -> {
                report.setStatus(CommentReportEntity.ReportStatus.REJECTED);
                resultText = "举报不成立";
            }
            default -> throw new BadRequestException("无效的处置操作: " + action);
        }

        report.setReviewedBy(reviewerId);
        report.setReviewedAt(LocalDateTime.now());
        report.setResolution(request.resolution() != null ? request.resolution() : resultText);
        report = commentReportRepository.save(report);

        notificationService.createReportResultNotification(report.getReporterId(), report.getCommentId(),
                resultText, report.getResolution());

        return toReportDTO(report);
    }

    @Transactional
    public void initPostSettings(Long postId) {
        if (!postCommentSettingsRepository.existsByPostId(postId)) {
            PostCommentSettingsEntity settings = new PostCommentSettingsEntity();
            settings.setPostId(postId);
            settings.setAccessRule(PostCommentSettingsEntity.AccessRule.OPEN);
            settings.setMaxDepth(3);
            settings.setMaxLength(2000);
            postCommentSettingsRepository.save(settings);
        }
    }

    @Scheduled(cron = "0 5 * * * ?")
    @Transactional
    public void cleanupOldRateLimits() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        commentRateLimitRepository.deleteOldWindows(cutoff);
    }

    private CommentTreeDTO buildTree(CommentEntity entity,
                                     Map<Long, List<CommentEntity>> repliesByParent,
                                     Map<Long, UserEntity> userCache,
                                     Map<Long, Integer> userVotes,
                                     Long currentUserId,
                                     int remainingDepth) {
        List<CommentTreeDTO> children = new ArrayList<>();
        if (remainingDepth > 0) {
            List<CommentEntity> directReplies = repliesByParent.getOrDefault(entity.getId(), Collections.emptyList());
            for (CommentEntity reply : directReplies) {
                children.add(buildTree(reply, repliesByParent, userCache, userVotes, currentUserId, remainingDepth - 1));
            }
        }
        return toTreeDTOInternal(entity, userCache, userVotes, currentUserId, children);
    }

    private CommentTreeDTO toTreeDTO(CommentEntity entity,
                                     Map<Long, UserEntity> userCache,
                                     Map<Long, Integer> userVotes,
                                     Long currentUserId,
                                     int remainingDepth) {
        return toTreeDTOInternal(entity, userCache, userVotes, currentUserId, Collections.emptyList());
    }

    private CommentTreeDTO toTreeDTOInternal(CommentEntity entity,
                                             Map<Long, UserEntity> userCache,
                                             Map<Long, Integer> userVotes,
                                             Long currentUserId,
                                             List<CommentTreeDTO> replies) {
        UserEntity user = userCache.computeIfAbsent(entity.getUserId(),
                id -> userRepository.findById(id).orElse(null));
        UserEntity replyToUser = entity.getReplyToUserId() != null
                ? userCache.computeIfAbsent(entity.getReplyToUserId(),
                        id -> userRepository.findById(id).orElse(null))
                : null;

        Integer userVote = currentUserId != null ? userVotes.get(entity.getId()) : null;

        boolean canEdit = currentUserId != null
                && entity.getUserId().equals(currentUserId)
                && !entity.isDeleted()
                && entity.getCreatedAt() != null
                && LocalDateTime.now().isBefore(entity.getCreatedAt().plusMinutes(EDIT_WINDOW_MINUTES));

        boolean canDelete = currentUserId != null && !entity.isDeleted();
        if (canDelete && !entity.getUserId().equals(currentUserId)) {
            PostEntity post = postRepository.findById(entity.getPostId()).orElse(null);
            if (post == null || post.getAuthor() == null || !post.getAuthor().getId().equals(currentUserId)) {
                UserEntity u = userRepository.findById(currentUserId).orElse(null);
                canDelete = u != null && u.getRole() == UserRole.ADMIN;
            }
        }

        String content = entity.isDeleted() ? DELETED_PLACEHOLDER : entity.getContent();

        return new CommentTreeDTO(
                entity.getId(),
                entity.getUserId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getDisplayName() : (entity.isDeleted() ? "已移除" : "匿名用户"),
                user != null ? user.getAvatarUrl() : null,
                entity.getPostId(),
                entity.getParentId(),
                entity.getReplyToCommentId(),
                entity.getReplyToUserId(),
                replyToUser != null ? replyToUser.getUsername() : null,
                replyToUser != null ? replyToUser.getDisplayName() : null,
                entity.getDepth(),
                content,
                entity.isDeleted() ? 0 : entity.getUpvotes(),
                entity.isDeleted() ? 0 : entity.getDownvotes(),
                entity.isDeleted() ? null : userVote,
                !entity.isDeleted() && entity.isPinned(),
                !entity.isDeleted() && entity.isEdited(),
                entity.getEditedAt() != null ? entity.getEditedAt().toString() : null,
                entity.isDeleted(),
                entity.isDeleted() ? entity.getDeletedBy() : null,
                !entity.isDeleted() && entity.isHasImages(),
                !entity.isDeleted() && entity.isHasCode(),
                canEdit,
                canDelete,
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                replies
        );
    }

    private CommentVoteResultDTO buildVoteResult(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId).orElse(null);
        Integer vote = commentVoteRepository.findVoteTypeByCommentIdAndUserId(commentId, userId);
        return new CommentVoteResultDTO(
                commentId,
                comment != null ? comment.getUpvotes() : 0,
                comment != null ? comment.getDownvotes() : 0,
                vote
        );
    }

    private CommentReportDTO toReportDTO(CommentReportEntity entity) {
        CommentEntity comment = commentRepository.findById(entity.getCommentId()).orElse(null);
        UserEntity commentAuthor = comment != null ? userRepository.findById(comment.getUserId()).orElse(null) : null;
        UserEntity reporter = userRepository.findById(entity.getReporterId()).orElse(null);
        UserEntity reviewer = entity.getReviewedBy() != null ? userRepository.findById(entity.getReviewedBy()).orElse(null) : null;

        return new CommentReportDTO(
                entity.getId(),
                entity.getCommentId(),
                comment != null ? comment.getContent() : null,
                commentAuthor != null ? commentAuthor.getId() : null,
                commentAuthor != null ? commentAuthor.getUsername() : null,
                entity.getReporterId(),
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getDisplayName() : null,
                entity.getReason(),
                entity.getDescription(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getReviewedBy(),
                reviewer != null ? reviewer.getUsername() : null,
                entity.getReviewedAt() != null ? entity.getReviewedAt().toString() : null,
                entity.getResolution(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }

    private PostCommentSettingsDTO toSettingsDTO(PostCommentSettingsEntity entity) {
        return new PostCommentSettingsDTO(
                entity.getId(),
                entity.getPostId(),
                entity.getAccessRule() != null ? entity.getAccessRule().name() : null,
                entity.isRequireApproval(),
                entity.getMaxDepth(),
                entity.getMaxLength(),
                entity.isAllowImages(),
                entity.isAllowCode(),
                entity.isAllowEmojis(),
                entity.isAllowMentions(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }

    private PostCommentSettingsEntity getOrCreateSettings(Long postId) {
        return postCommentSettingsRepository.findByPostId(postId).orElseGet(() -> {
            PostCommentSettingsEntity s = new PostCommentSettingsEntity();
            s.setPostId(postId);
            s.setAccessRule(PostCommentSettingsEntity.AccessRule.OPEN);
            s.setMaxDepth(3);
            s.setMaxLength(2000);
            s.setCreatedAt(LocalDateTime.now());
            return postCommentSettingsRepository.save(s);
        });
    }

    private void checkAccessPermission(Long userId, PostEntity post, PostCommentSettingsEntity settings) {
        PostCommentSettingsEntity.AccessRule rule = settings.getAccessRule();
        if (rule == null) rule = PostCommentSettingsEntity.AccessRule.OPEN;

        switch (rule) {
            case CLOSED -> throw new ForbiddenException("该文章讨论区已关闭");
            case SUBSCRIBERS_ONLY -> {
                Long authorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
                if (authorId == null) {
                    throw new ForbiddenException("该文章仅订阅者可讨论");
                }
                if (!authorId.equals(userId) && !subscriptionRepository.existsByReaderIdAndAuthorId(userId, authorId)) {
                    UserEntity u = userRepository.findById(userId).orElse(null);
                    if (u == null || u.getRole() != UserRole.ADMIN) {
                        throw new ForbiddenException("该文章仅订阅者可讨论，请先订阅作者");
                    }
                }
            }
            default -> {
            }
        }
    }

    private void checkRateLimit(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        int count = commentRateLimitRepository.countCommentsInWindow(userId, windowStart);
        if (count >= MAX_COMMENTS_PER_MINUTE) {
            throw new BadRequestException("发表讨论过于频繁，请稍后再试（每分钟最多" + MAX_COMMENTS_PER_MINUTE + "条）");
        }
    }

    private void recordRateLimit(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        int updated = commentRateLimitRepository.incrementCount(userId, windowStart);
        if (updated == 0) {
            CommentRateLimitEntity entity = new CommentRateLimitEntity();
            entity.setUserId(userId);
            entity.setWindowStart(windowStart);
            entity.setCount(1);
            entity.setCreatedAt(LocalDateTime.now());
            commentRateLimitRepository.save(entity);
        }
    }

    private List<Long> extractMentions(String content, UserEntity currentUser) {
        if (content == null || content.isBlank()) return Collections.emptyList();

        Set<String> mentionedNames = new HashSet<>();
        Matcher m = MENTION_PATTERN.matcher(content);
        while (m.find()) {
            mentionedNames.add(m.group(1));
        }
        if (mentionedNames.isEmpty()) return Collections.emptyList();

        List<Long> ids = new ArrayList<>();
        for (String name : mentionedNames) {
            UserEntity u = userRepository.findByUsername(name).orElse(null);
            if (u != null) {
                ids.add(u.getId());
            }
        }
        return ids;
    }

    private void autoReportForSeverity(CommentEntity comment, List<String> matchedWords) {
        CommentReportEntity auto = new CommentReportEntity();
        auto.setCommentId(comment.getId());
        auto.setReporterId(0L);
        auto.setReason("系统自动检测-严重敏感词");
        auto.setDescription("系统自动检测到严重违规词汇: " + String.join(", ", matchedWords));
        auto.setStatus(CommentReportEntity.ReportStatus.PENDING);
        auto.setCreatedAt(LocalDateTime.now());
        commentReportRepository.save(auto);
    }
}
