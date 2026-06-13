package com.label4002.blog.service;

import com.label4002.blog.dto.AuthorStatsDTO;
import com.label4002.blog.dto.CategoryBreadcrumbDTO;
import com.label4002.blog.dto.CreatePostRequest;
import com.label4002.blog.dto.KeywordDTO;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.RevisionDiffDTO;
import com.label4002.blog.dto.ScheduledTaskDTO;
import com.label4002.blog.dto.UpdatePostRequest;
import com.label4002.blog.entity.CategoryEntity;
import com.label4002.blog.entity.KeywordEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CategoryRepository;
import com.label4002.blog.repository.FavoriteRepository;
import com.label4002.blog.repository.KeywordRepository;
import com.label4002.blog.repository.PostKeywordRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordService keywordService;
    private final PostKeywordRepository postKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final CategoryService categoryService;
    private final FavoriteRepository favoriteRepository;
    private final DiscussionService discussionService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       KeywordService keywordService,
                       PostKeywordRepository postKeywordRepository,
                       KeywordRepository keywordRepository,
                       CategoryService categoryService,
                       FavoriteRepository favoriteRepository,
                       DiscussionService discussionService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keywordService = keywordService;
        this.postKeywordRepository = postKeywordRepository;
        this.keywordRepository = keywordRepository;
        this.categoryService = categoryService;
        this.favoriteRepository = favoriteRepository;
        this.discussionService = discussionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPublic(int page, int size, Long categoryId, Long keywordId, Long authorId) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;

        if (authorId != null && categoryId != null) {
            postPage = postRepository.findByAuthorIdAndStatusAndCategoryId(authorId, PostStatus.PUBLISHED, categoryId, pageable);
        } else if (authorId != null) {
            postPage = postRepository.findByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED, pageable);
        } else if (categoryId != null && keywordId != null) {
            Set<Long> catIds = categoryService.getEnabledCategoryIdsWithDescendants(categoryId);
            if (catIds.isEmpty()) {
                return new PageResponse<>(Collections.emptyList(), 0, normalizedPage, normalizedSize);
            }
            postPage = postRepository.findByCategoryIdsAndKeywordId(catIds.stream().toList(), keywordId, pageable);
        } else if (categoryId != null) {
            Set<Long> catIds = categoryService.getEnabledCategoryIdsWithDescendants(categoryId);
            if (catIds.isEmpty()) {
                return new PageResponse<>(Collections.emptyList(), 0, normalizedPage, normalizedSize);
            }
            postPage = postRepository.findByStatusAndCategoryId(PostStatus.PUBLISHED, categoryId, pageable);
        } else if (keywordId != null) {
            postPage = postRepository.findByKeywordIdAndCategoryEnabled(keywordId, pageable);
        } else {
            postPage = postRepository.findByStatus(PostStatus.PUBLISHED, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .filter(p -> !p.isRevision())
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public PostDetailDTO getDetail(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));
        return toDetail(post);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDTO> listMine(Long userId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public PostDetailDTO createPost(Long userId, CreatePostRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        PostEntity post = new PostEntity();
        post.setTitle(normalizeTitle(request.title()));
        post.setContent(request.content().trim());
        post.setAuthor(user);
        post.setStatus(PostStatus.DRAFT);
        post.setViewCount(0);
        post.setRevision(false);

        if (request.scheduledAt() != null) {
            if (request.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("预约上线时间不能早于当前时间");
            }
            post.setScheduledAt(request.scheduledAt());
        }

        if (request.categoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("分类不存在"));
            if (!category.isEnabled()) {
                throw new NotFoundException("分类不存在或已禁用");
            }
            post.setCategory(category);
        } else {
            CategoryEntity defaultCat = categoryRepository.findBySlug("default")
                    .orElseThrow(() -> new NotFoundException("默认分类不存在"));
            post.setCategory(defaultCat);
        }

        PostEntity saved = postRepository.save(post);

        if (request.keywords() != null && !request.keywords().isEmpty()) {
            List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
            List<Long> keywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
            for (Long kwId : keywordIds) {
                postKeywordRepository.insertPostKeyword(saved.getId(), kwId);
            }
            keywordService.addKeywordCounts(keywordIds);
        }

        return toDetail(saved);
    }

    @Transactional
    public PostDetailDTO updatePost(Long userId, Long postId, UpdatePostRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限编辑他人的文章");
        }

        if (post.getStatus() == PostStatus.PUBLISHED) {
            return createRevision(userId, postId, request);
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以编辑");
        }

        post.setTitle(normalizeTitle(request.title()));
        post.setContent(request.content().trim());

        if (request.scheduledAt() != null) {
            if (request.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("预约上线时间不能早于当前时间");
            }
            post.setScheduledAt(request.scheduledAt());
        } else {
            post.setScheduledAt(null);
        }

        if (request.categoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("分类不存在"));
            post.setCategory(category);
        }

        PostEntity saved = postRepository.save(post);

        if (request.keywords() != null) {
            List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
            List<Long> newKeywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
            keywordService.syncKeywordCounts(saved.getId(), newKeywordIds);
        }

        return toDetail(saved);
    }

    @Transactional
    public PostDetailDTO createRevision(Long userId, Long postId, UpdatePostRequest request) {
        PostEntity originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!originalPost.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限编辑他人的文章");
        }

        if (originalPost.getStatus() != PostStatus.PUBLISHED) {
            throw new BadRequestException("仅已发布的文章可以创建修订版本");
        }

        Optional<PostEntity> existingRevision = postRepository.findRevisionByParentPostId(postId);
        if (existingRevision.isPresent()) {
            PostEntity revision = existingRevision.get();
            if (revision.getStatus() == PostStatus.PENDING) {
                throw new BadRequestException("该文章已有待审核的修订版本，请等待审核完成");
            }
            if (revision.getStatus() == PostStatus.SCHEDULED) {
                throw new BadRequestException("该文章已有已审核等待上线的修订版本");
            }
            if (revision.getStatus() == PostStatus.DRAFT || revision.getStatus() == PostStatus.REJECTED) {
                revision.setTitle(normalizeTitle(request.title()));
                revision.setContent(request.content().trim());
                if (request.categoryId() != null) {
                    CategoryEntity category = categoryRepository.findById(request.categoryId())
                            .orElseThrow(() -> new NotFoundException("分类不存在"));
                    revision.setCategory(category);
                }
                if (request.scheduledAt() != null) {
                    revision.setScheduledAt(request.scheduledAt());
                } else {
                    revision.setScheduledAt(null);
                }
                if (request.keywords() != null) {
                    List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
                    List<Long> newKeywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
                    keywordService.syncKeywordCounts(revision.getId(), newKeywordIds);
                }
                revision.setRejectionReason(null);
                return toDetail(postRepository.save(revision));
            }
        }

        PostEntity revision = new PostEntity();
        revision.setTitle(normalizeTitle(request.title()));
        revision.setContent(request.content().trim());
        revision.setAuthor(originalPost.getAuthor());
        revision.setStatus(PostStatus.DRAFT);
        revision.setViewCount(0);
        revision.setRevision(true);
        revision.setParentPost(originalPost);
        revision.setCategory(originalPost.getCategory());

        if (request.scheduledAt() != null) {
            revision.setScheduledAt(request.scheduledAt());
        }

        PostEntity saved = postRepository.save(revision);

        if (request.keywords() != null && !request.keywords().isEmpty()) {
            List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
            List<Long> keywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
            for (Long kwId : keywordIds) {
                postKeywordRepository.insertPostKeyword(saved.getId(), kwId);
            }
            keywordService.addKeywordCounts(keywordIds);
        }

        return toDetail(saved);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限删除他人的文章");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以删除");
        }

        keywordService.decrementKeywordCountsForPost(postId);
        postRepository.delete(post);
    }

    @Transactional
    public void submitForReview(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限提交他人的文章");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以提交审核");
        }

        post.setStatus(PostStatus.PENDING);
        post.setRejectionReason(null);
        postRepository.save(post);
    }

    @Transactional
    public void approvePost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以审核通过");
        }

        if (post.isRevision()) {
            approveRevision(post);
        } else {
            if (post.getScheduledAt() != null && post.getScheduledAt().isAfter(LocalDateTime.now())) {
                post.setStatus(PostStatus.SCHEDULED);
            } else {
                post.setStatus(PostStatus.PUBLISHED);
                post.setScheduledAt(null);
                discussionService.initPostSettings(post.getId());
            }
            post.setRejectionReason(null);
            postRepository.save(post);
        }
    }

    private void approveRevision(PostEntity revision) {
        PostEntity parentPost = revision.getParentPost();
        if (parentPost == null) {
            throw new BadRequestException("修订版本未关联原文章");
        }

        if (revision.getScheduledAt() != null && revision.getScheduledAt().isAfter(LocalDateTime.now())) {
            revision.setStatus(PostStatus.SCHEDULED);
            revision.setRejectionReason(null);
            postRepository.save(revision);
        } else {
            applyRevisionToParent(revision, parentPost);
        }
    }

    @Transactional
    public void applyRevisionToParent(PostEntity revision, PostEntity parentPost) {
        parentPost.setTitle(revision.getTitle());
        parentPost.setContent(revision.getContent());
        parentPost.setCategory(revision.getCategory());
        parentPost.setStatus(PostStatus.PUBLISHED);
        parentPost.setScheduledAt(null);

        keywordService.decrementKeywordCountsForPost(parentPost.getId());

        List<KeywordEntity> revisionKeywords = postKeywordRepository.findKeywordsByPostId(revision.getId());
        List<Long> keywordIds = revisionKeywords.stream().map(KeywordEntity::getId).toList();
        keywordService.syncKeywordCounts(parentPost.getId(), keywordIds);

        postRepository.save(parentPost);

        keywordService.decrementKeywordCountsForPost(revision.getId());
        postRepository.delete(revision);
    }

    @Transactional
    public void rejectPost(Long postId, String reason) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以驳回");
        }

        post.setStatus(PostStatus.REJECTED);
        post.setRejectionReason(reason);
        postRepository.save(post);
    }

    @Transactional
    public void unpublishPost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BadRequestException("仅已发布的文章可以取消发布");
        }

        post.setStatus(PostStatus.DRAFT);
        post.setRejectionReason(null);
        post.setScheduledAt(null);
        postRepository.save(post);
    }

    @Transactional
    public void batchApprove(List<Long> postIds) {
        for (Long postId : postIds) {
            try {
                approvePost(postId);
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public void batchReject(List<Long> postIds, String reason) {
        List<PostEntity> posts = postRepository.findAllById(postIds);
        for (PostEntity post : posts) {
            if (post.getStatus() == PostStatus.PENDING) {
                post.setStatus(PostStatus.REJECTED);
                post.setRejectionReason(reason);
            }
        }
        postRepository.saveAll(posts);
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listAuthorPosts(Long authorId, String status, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;
        if (status != null && !status.isBlank()) {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            postPage = postRepository.findByAuthorIdAndStatus(authorId, postStatus, pageable);
        } else {
            postPage = postRepository.findByAuthorId(authorId, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public AuthorStatsDTO getAuthorStats(Long authorId) {
        long totalPublished = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);
        long totalDraft = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.DRAFT);
        long totalViews = postRepository.sumViewCountByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);
        long avgViews = totalPublished > 0 ? totalViews / totalPublished : 0;

        List<PostEntity> topPosts = postRepository.findTop10ByAuthorIdAndStatusOrderByViewCountDesc(authorId, PostStatus.PUBLISHED);
        List<PostSummaryDTO> topPostDTOs = topPosts.stream()
                .map(this::toSummary)
                .toList();

        return new AuthorStatsDTO(totalPublished, totalDraft, totalViews, avgViews, topPostDTOs);
    }

    @Transactional
    public void transferAuthor(Long oldAuthorId, Long newAuthorId) {
        postRepository.transferAuthor(oldAuthorId, newAuthorId);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return postRepository.countByStatus(PostStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countScheduled() {
        return postRepository.countByStatus(PostStatus.SCHEDULED);
    }

    @Transactional(readOnly = true)
    public long countPendingRevisions() {
        return postRepository.countByStatusAndRevisionTrue(PostStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDTO> listAllPosts(String status) {
        if (status != null && !status.isBlank()) {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            return postRepository.findByStatus(postStatus, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent().stream()
                    .map(this::toSummary)
                    .toList();
        }
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPendingPosts(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<PostEntity> postPage = postRepository.findByStatus(PostStatus.PENDING, pageable);

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPublicByAuthor(Long authorId, Long categoryId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;
        if (categoryId != null) {
            postPage = postRepository.findByAuthorIdAndStatusAndCategoryId(authorId, PostStatus.PUBLISHED, categoryId, pageable);
        } else {
            postPage = postRepository.findByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional
    public void batchUpdateCategory(List<Long> postIds, Long categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("分类不存在"));
        for (Long postId : postIds) {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("文章不存在 id=" + postId));
            post.setCategory(category);
            postRepository.save(post);
        }
    }

    @Transactional
    public void batchAddKeywords(List<Long> postIds, List<String> keywordNames) {
        List<KeywordEntity> keywords = keywordService.resolveOrCreate(keywordNames);
        List<Long> keywordIds = keywords.stream().map(KeywordEntity::getId).toList();
        for (Long postId : postIds) {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("文章不存在 id=" + postId));
            keywordService.syncKeywordCounts(postId, keywordIds);
        }
    }

    @Transactional
    public void cancelSchedule(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限操作他人的文章");
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            throw new BadRequestException("仅预约上线的文章可以取消预约");
        }

        post.setStatus(PostStatus.DRAFT);
        post.setScheduledAt(null);
        postRepository.save(post);
    }

    @Transactional
    public void updateSchedule(Long userId, Long postId, LocalDateTime scheduledAt) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限操作他人的文章");
        }

        if (post.getStatus() != PostStatus.SCHEDULED && post.getStatus() != PostStatus.DRAFT) {
            throw new BadRequestException("仅预约上线或草稿状态的文章可以设置预约时间");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("预约上线时间不能早于当前时间");
        }

        post.setScheduledAt(scheduledAt);
        if (post.getStatus() == PostStatus.DRAFT) {
            post.setStatus(PostStatus.PENDING);
        } else {
            post.setStatus(PostStatus.SCHEDULED);
        }
        postRepository.save(post);
    }

    @Transactional
    public void forcePublishScheduled(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.SCHEDULED) {
            throw new BadRequestException("仅预约上线的文章可以强制发布");
        }

        if (post.isRevision()) {
            PostEntity parentPost = post.getParentPost();
            if (parentPost == null) {
                throw new BadRequestException("修订版本未关联原文章");
            }
            applyRevisionToParent(post, parentPost);
        } else {
            post.setStatus(PostStatus.PUBLISHED);
            post.setScheduledAt(null);
            if (!post.isRevision()) {
                discussionService.initPostSettings(post.getId());
            }
            postRepository.save(post);
        }
    }

    @Transactional
    public void publishScheduledPosts() {
        List<PostEntity> duePosts = postRepository.findScheduledPostsDue(PostStatus.SCHEDULED, LocalDateTime.now());
        for (PostEntity post : duePosts) {
            try {
                if (post.isRevision()) {
                    PostEntity parentPost = post.getParentPost();
                    if (parentPost != null && parentPost.getStatus() == PostStatus.PUBLISHED) {
                        applyRevisionToParent(post, parentPost);
                    } else {
                        post.setStatus(PostStatus.PUBLISHED);
                        post.setScheduledAt(null);
                        postRepository.save(post);
                    }
                } else {
                    if (post.getAuthor() != null && post.getAuthor().isEnabled()) {
                        post.setStatus(PostStatus.PUBLISHED);
                        post.setScheduledAt(null);
                        discussionService.initPostSettings(post.getId());
                        postRepository.save(post);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduledTaskDTO> listScheduledTasks(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.ASC, "scheduledAt"));

        Page<PostEntity> postPage = postRepository.findScheduledPosts(PostStatus.SCHEDULED, pageable);

        List<ScheduledTaskDTO> items = postPage.getContent().stream()
                .map(this::toScheduledTask)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public RevisionDiffDTO getRevisionDiff(Long postId) {
        PostEntity parentPost = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        PostEntity revision = postRepository.findRevisionByParentPostId(postId)
                .orElseThrow(() -> new NotFoundException("该文章没有修订版本"));

        String parentKw = postKeywordRepository.findKeywordsByPostId(parentPost.getId()).stream()
                .map(KeywordEntity::getName).reduce((a, b) -> a + ", " + b).orElse("");
        String revisionKw = postKeywordRepository.findKeywordsByPostId(revision.getId()).stream()
                .map(KeywordEntity::getName).reduce((a, b) -> a + ", " + b).orElse("");

        String parentCatPath = "";
        if (parentPost.getCategory() != null) {
            List<CategoryBreadcrumbDTO> breadcrumbs = categoryService.getBreadcrumb(parentPost.getCategory().getId());
            parentCatPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        String revisionCatPath = "";
        if (revision.getCategory() != null) {
            List<CategoryBreadcrumbDTO> breadcrumbs = categoryService.getBreadcrumb(revision.getCategory().getId());
            revisionCatPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        return new RevisionDiffDTO(
                revision.getId(),
                parentPost.getId(),
                parentPost.getTitle(),
                revision.getTitle(),
                parentPost.getContent(),
                revision.getContent(),
                parentPost.getCategory() != null ? parentPost.getCategory().getId() : null,
                revision.getCategory() != null ? revision.getCategory().getId() : null,
                parentCatPath,
                revisionCatPath,
                parentKw,
                revisionKw,
                revision.getStatus().name(),
                revision.getAuthor().getNickname()
        );
    }

    @Transactional(readOnly = true)
    public PostDetailDTO getRevisionDetail(Long parentPostId) {
        PostEntity revision = postRepository.findRevisionByParentPostId(parentPostId)
                .orElseThrow(() -> new NotFoundException("该文章没有修订版本"));
        return toDetail(revision);
    }

    @Transactional
    public void discardRevision(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限操作他人的文章");
        }

        PostEntity revision = postRepository.findRevisionByParentPostId(postId)
                .orElseThrow(() -> new NotFoundException("该文章没有修订版本"));

        if (revision.getStatus() == PostStatus.PENDING) {
            throw new BadRequestException("审核中的修订版本不能丢弃，请先联系管理员处理");
        }

        keywordService.decrementKeywordCountsForPost(revision.getId());
        postRepository.delete(revision);
    }

    private String normalizeTitle(String title) {
        return title.trim().replaceAll("\\s+", " ");
    }

    private ScheduledTaskDTO toScheduledTask(PostEntity post) {
        return new ScheduledTaskDTO(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getScheduledAt(),
                post.isRevision(),
                post.getParentPost() != null ? post.getParentPost().getId() : null,
                post.getStatus().name()
        );
    }

    private PostSummaryDTO toSummary(PostEntity post) {
        List<KeywordDTO> keywordDTOs = postKeywordRepository.findKeywordsByPostId(post.getId())
                .stream()
                .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getUsageCount(), k.getLastUsedAt(), k.isArchived(), k.getCreatedAt()))
                .toList();

        String categoryPath = "";
        if (post.getCategory() != null) {
            List<CategoryBreadcrumbDTO> breadcrumbs = categoryService.getBreadcrumb(post.getCategory().getId());
            categoryPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        boolean hasRevision = false;
        if (!post.isRevision()) {
            hasRevision = postRepository.findRevisionByParentPostId(post.getId()).isPresent();
        }

        long favCount = favoriteRepository.countByPostId(post.getId());

        return new PostSummaryDTO(
                post.getId(),
                post.getTitle(),
                excerpt(post.getContent(), 100),
                previewContent(post.getContent(), 800),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getStatus().name(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                keywordDTOs,
                post.getViewCount(),
                favCount,
                post.getScheduledAt(),
                post.isRevision(),
                post.getParentPost() != null ? post.getParentPost().getId() : null,
                hasRevision,
                post.getCreatedAt()
        );
    }

    private PostDetailDTO toDetail(PostEntity post) {
        List<KeywordDTO> keywordDTOs = postKeywordRepository.findKeywordsByPostId(post.getId())
                .stream()
                .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getUsageCount(), k.getLastUsedAt(), k.isArchived(), k.getCreatedAt()))
                .toList();

        List<CategoryBreadcrumbDTO> breadcrumbs = Collections.emptyList();
        String categoryPath = "";
        if (post.getCategory() != null) {
            breadcrumbs = categoryService.getBreadcrumb(post.getCategory().getId());
            categoryPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        boolean hasRevision = false;
        if (!post.isRevision()) {
            hasRevision = postRepository.findRevisionByParentPostId(post.getId()).isPresent();
        }

        return new PostDetailDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getStatus().name(),
                post.getRejectionReason(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                breadcrumbs,
                keywordDTOs,
                post.getViewCount(),
                post.getScheduledAt(),
                post.isRevision(),
                post.getParentPost() != null ? post.getParentPost().getId() : null,
                hasRevision,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String excerpt(String content, int length) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, length) + "...";
    }

    private String previewContent(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        int lastNewline = trimmed.lastIndexOf("\n", maxLength);
        int endIndex = lastNewline > maxLength * 0.6 ? lastNewline : maxLength;
        String preview = trimmed.substring(0, endIndex).trim();
        if (preview.length() < trimmed.length()) {
            preview = preview + "\n\n...";
        }
        return preview;
    }
}
