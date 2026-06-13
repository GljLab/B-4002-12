package com.label4002.blog.service;

import com.label4002.blog.dto.AuthorReplyRequest;
import com.label4002.blog.dto.CreateReviewCommentRequest;
import com.label4002.blog.dto.CreateReviewTemplateRequest;
import com.label4002.blog.dto.ReviewCommentDTO;
import com.label4002.blog.dto.ReviewRoundDTO;
import com.label4002.blog.dto.ReviewTemplateDTO;
import com.label4002.blog.dto.ReviewTimelineDTO;
import com.label4002.blog.dto.ResubmitWithNoteRequest;
import com.label4002.blog.dto.UpdateReviewTemplateRequest;
import com.label4002.blog.entity.CommentPriority;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.ReviewCommentEntity;
import com.label4002.blog.entity.ReviewResult;
import com.label4002.blog.entity.ReviewRoundEntity;
import com.label4002.blog.entity.ReviewTemplateEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.ReviewCommentRepository;
import com.label4002.blog.repository.ReviewRoundRepository;
import com.label4002.blog.repository.ReviewTemplateRepository;
import com.label4002.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRoundRepository reviewRoundRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewTemplateRepository reviewTemplateRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRoundRepository reviewRoundRepository,
                         ReviewCommentRepository reviewCommentRepository,
                         ReviewTemplateRepository reviewTemplateRepository,
                         PostRepository postRepository,
                         UserRepository userRepository) {
        this.reviewRoundRepository = reviewRoundRepository;
        this.reviewCommentRepository = reviewCommentRepository;
        this.reviewTemplateRepository = reviewTemplateRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewRoundDTO approvePost(Long postId, Long reviewerId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以审核通过");
        }

        UserEntity reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        ReviewRoundEntity round = new ReviewRoundEntity();
        round.setPost(post);
        round.setReviewer(reviewer);
        round.setResult(ReviewResult.APPROVED);
        reviewRoundRepository.save(round);

        if (post.isRevision()) {
            PostEntity parentPost = post.getParentPost();
            if (parentPost == null) {
                throw new BadRequestException("修订版本未关联原文章");
            }
            if (post.getScheduledAt() != null && post.getScheduledAt().isAfter(LocalDateTime.now())) {
                post.setStatus(PostStatus.SCHEDULED);
                post.setRejectionReason(null);
                postRepository.save(post);
            } else {
                parentPost.setTitle(post.getTitle());
                parentPost.setContent(post.getContent());
                parentPost.setCategory(post.getCategory());
                parentPost.setStatus(PostStatus.PUBLISHED);
                parentPost.setScheduledAt(null);
                postRepository.save(parentPost);
                postRepository.delete(post);
            }
        } else {
            if (post.getScheduledAt() != null && post.getScheduledAt().isAfter(LocalDateTime.now())) {
                post.setStatus(PostStatus.SCHEDULED);
            } else {
                post.setStatus(PostStatus.PUBLISHED);
                post.setScheduledAt(null);
            }
            post.setRejectionReason(null);
            postRepository.save(post);
        }

        return toRoundDTO(round);
    }

    @Transactional
    public ReviewRoundDTO rejectPost(Long postId, Long reviewerId, List<CreateReviewCommentRequest> commentRequests) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以驳回");
        }

        UserEntity reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        boolean hasMustFix = commentRequests != null && commentRequests.stream()
                .anyMatch(c -> "MUST_FIX".equalsIgnoreCase(c.priority()));

        if (!hasMustFix) {
            throw new BadRequestException("驳回时必须至少有一条「必须修改」意见");
        }

        ReviewRoundEntity round = new ReviewRoundEntity();
        round.setPost(post);
        round.setReviewer(reviewer);
        round.setResult(ReviewResult.REJECTED);
        reviewRoundRepository.save(round);

        if (commentRequests != null) {
            for (CreateReviewCommentRequest req : commentRequests) {
                ReviewCommentEntity comment = new ReviewCommentEntity();
                comment.setRound(round);
                comment.setContent(req.content());
                comment.setPriority("MUST_FIX".equalsIgnoreCase(req.priority()) ? CommentPriority.MUST_FIX : CommentPriority.SUGGESTION);
                comment.setParagraphIndex(req.paragraphIndex());
                comment.setPositionStart(req.positionStart());
                comment.setPositionEnd(req.positionEnd());
                reviewCommentRepository.save(comment);
            }
        }

        StringBuilder reasonBuilder = new StringBuilder();
        if (commentRequests != null) {
            for (int i = 0; i < commentRequests.size(); i++) {
                CreateReviewCommentRequest c = commentRequests.get(i);
                if (i > 0) reasonBuilder.append("; ");
                reasonBuilder.append("[").append(c.priority()).append("] ").append(c.content());
            }
        }

        post.setStatus(PostStatus.REJECTED);
        post.setRejectionReason(reasonBuilder.length() > 500 ? reasonBuilder.substring(0, 500) : reasonBuilder.toString());
        postRepository.save(post);

        return toRoundDTO(round);
    }

    @Transactional
    public ReviewCommentDTO addComment(Long postId, Long reviewerId, CreateReviewCommentRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以添加批注");
        }

        UserEntity reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        List<ReviewRoundEntity> pendingRounds = reviewRoundRepository.findByPostIdOrderByCreatedAtAsc(postId);
        ReviewRoundEntity round;
        if (pendingRounds.isEmpty()) {
            round = new ReviewRoundEntity();
            round.setPost(post);
            round.setReviewer(reviewer);
            round.setResult(null);
            reviewRoundRepository.save(round);
        } else {
            round = pendingRounds.get(pendingRounds.size() - 1);
            if (round.getResult() != null) {
                round = new ReviewRoundEntity();
                round.setPost(post);
                round.setReviewer(reviewer);
                round.setResult(null);
                reviewRoundRepository.save(round);
            }
        }

        ReviewCommentEntity comment = new ReviewCommentEntity();
        comment.setRound(round);
        comment.setContent(request.content());
        comment.setPriority("MUST_FIX".equalsIgnoreCase(request.priority()) ? CommentPriority.MUST_FIX : CommentPriority.SUGGESTION);
        comment.setParagraphIndex(request.paragraphIndex());
        comment.setPositionStart(request.positionStart());
        comment.setPositionEnd(request.positionEnd());
        ReviewCommentEntity saved = reviewCommentRepository.save(comment);

        return toCommentDTO(saved);
    }

    @Transactional
    public ReviewCommentDTO authorReply(Long commentId, Long authorId, AuthorReplyRequest request) {
        ReviewCommentEntity comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("批注不存在"));

        PostEntity post = comment.getRound().getPost();
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new BadRequestException("无权限回复此批注");
        }

        comment.setAuthorReply(request.reply());
        comment.setAuthorResolved(true);
        comment.setAuthorRepliedAt(LocalDateTime.now());
        ReviewCommentEntity saved = reviewCommentRepository.save(comment);

        return toCommentDTO(saved);
    }

    @Transactional
    public ReviewCommentDTO toggleResolved(Long commentId, Long authorId) {
        ReviewCommentEntity comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("批注不存在"));

        PostEntity post = comment.getRound().getPost();
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new BadRequestException("无权限操作此批注");
        }

        comment.setAuthorResolved(!comment.isAuthorResolved());
        if (comment.isAuthorResolved()) {
            comment.setAuthorRepliedAt(LocalDateTime.now());
        } else {
            comment.setAuthorRepliedAt(null);
        }
        ReviewCommentEntity saved = reviewCommentRepository.save(comment);

        return toCommentDTO(saved);
    }

    @Transactional
    public void resubmitWithNote(Long postId, Long authorId, ResubmitWithNoteRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new BadRequestException("无权限提交他人的文章");
        }

        if (post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅已驳回的文章可以重新提交审核");
        }

        List<ReviewRoundEntity> rounds = reviewRoundRepository.findByPostIdAndResult(postId, ReviewResult.REJECTED);
        if (!rounds.isEmpty()) {
            ReviewRoundEntity lastRound = rounds.get(rounds.size() - 1);
            lastRound.setModificationNote(request.modificationNote());
            reviewRoundRepository.save(lastRound);
        }

        post.setStatus(PostStatus.PENDING);
        post.setRejectionReason(null);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<ReviewRoundDTO> getReviewHistory(Long postId) {
        List<ReviewRoundEntity> rounds = reviewRoundRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return rounds.stream().map(this::toRoundDTO).toList();
    }

    @Transactional(readOnly = true)
    public ReviewTimelineDTO getTimeline(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        List<ReviewRoundEntity> rounds = reviewRoundRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<ReviewTimelineDTO.TimelineEventDTO> events = new ArrayList<>();

        events.add(new ReviewTimelineDTO.TimelineEventDTO(
                "CREATE", null, post.getAuthor().getId(),
                post.getAuthor().getDisplayName(), "创建文章", null, post.getCreatedAt()));

        for (ReviewRoundEntity round : rounds) {
            if (round.getResult() == ReviewResult.APPROVED) {
                events.add(new ReviewTimelineDTO.TimelineEventDTO(
                        "APPROVE", round.getId(), round.getReviewer().getId(),
                        round.getReviewer().getDisplayName(), "审核通过", null, round.getCreatedAt()));
            } else if (round.getResult() == ReviewResult.REJECTED) {
                events.add(new ReviewTimelineDTO.TimelineEventDTO(
                        "REJECT", round.getId(), round.getReviewer().getId(),
                        round.getReviewer().getDisplayName(), "审核驳回", null, round.getCreatedAt()));

                List<ReviewCommentEntity> comments = reviewCommentRepository.findByRoundIdOrderByCreatedAtAsc(round.getId());
                for (ReviewCommentEntity c : comments) {
                    events.add(new ReviewTimelineDTO.TimelineEventDTO(
                            "COMMENT", round.getId(), round.getReviewer().getId(),
                            round.getReviewer().getDisplayName(),
                            "添加批注 [" + c.getPriority().name() + "]",
                            c.getContent(), c.getCreatedAt()));

                    if (c.getAuthorReply() != null) {
                        events.add(new ReviewTimelineDTO.TimelineEventDTO(
                                "REPLY", round.getId(), post.getAuthor().getId(),
                                post.getAuthor().getDisplayName(), "回复批注",
                                c.getAuthorReply(), c.getAuthorRepliedAt()));
                    }
                }

                if (round.getModificationNote() != null) {
                    events.add(new ReviewTimelineDTO.TimelineEventDTO(
                            "RESUBMIT", round.getId(), post.getAuthor().getId(),
                            post.getAuthor().getDisplayName(), "重新提交（修改说明）",
                            round.getModificationNote(), round.getCreatedAt()));
                }
            } else {
                List<ReviewCommentEntity> comments = reviewCommentRepository.findByRoundIdOrderByCreatedAtAsc(round.getId());
                for (ReviewCommentEntity c : comments) {
                    events.add(new ReviewTimelineDTO.TimelineEventDTO(
                            "COMMENT", round.getId(), round.getReviewer().getId(),
                            round.getReviewer().getDisplayName(),
                            "添加批注 [" + c.getPriority().name() + "]",
                            c.getContent(), c.getCreatedAt()));
                }
            }
        }

        return new ReviewTimelineDTO(postId, post.getTitle(), post.getStatus().name(), events);
    }

    @Transactional(readOnly = true)
    public List<ReviewTemplateDTO> listTemplates() {
        return reviewTemplateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTemplateDTO)
                .toList();
    }

    @Transactional
    public ReviewTemplateDTO createTemplate(CreateReviewTemplateRequest request) {
        ReviewTemplateEntity template = new ReviewTemplateEntity();
        template.setName(request.name());
        template.setContent(request.content());
        template.setPriority("MUST_FIX".equalsIgnoreCase(request.priority()) ? CommentPriority.MUST_FIX : CommentPriority.SUGGESTION);
        ReviewTemplateEntity saved = reviewTemplateRepository.save(template);
        return toTemplateDTO(saved);
    }

    @Transactional
    public ReviewTemplateDTO updateTemplate(Long id, UpdateReviewTemplateRequest request) {
        ReviewTemplateEntity template = reviewTemplateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("审查模板不存在"));
        template.setName(request.name());
        template.setContent(request.content());
        template.setPriority("MUST_FIX".equalsIgnoreCase(request.priority()) ? CommentPriority.MUST_FIX : CommentPriority.SUGGESTION);
        ReviewTemplateEntity saved = reviewTemplateRepository.save(template);
        return toTemplateDTO(saved);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!reviewTemplateRepository.existsById(id)) {
            throw new NotFoundException("审查模板不存在");
        }
        reviewTemplateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Long> findOverduePostIds() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        List<PostEntity> pendingPosts = postRepository.findByStatus(PostStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        return pendingPosts.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isBefore(threshold))
                .map(PostEntity::getId)
                .toList();
    }

    private ReviewRoundDTO toRoundDTO(ReviewRoundEntity round) {
        List<ReviewCommentEntity> comments = reviewCommentRepository.findByRoundIdOrderByCreatedAtAsc(round.getId());
        List<ReviewCommentDTO> commentDTOs = comments.stream().map(this::toCommentDTO).toList();

        return new ReviewRoundDTO(
                round.getId(),
                round.getPost().getId(),
                round.getReviewer().getId(),
                round.getReviewer().getDisplayName(),
                round.getResult() != null ? round.getResult().name() : null,
                round.getModificationNote(),
                round.getCreatedAt(),
                commentDTOs
        );
    }

    private ReviewCommentDTO toCommentDTO(ReviewCommentEntity comment) {
        return new ReviewCommentDTO(
                comment.getId(),
                comment.getRound().getId(),
                comment.getContent(),
                comment.getPriority().name(),
                comment.getParagraphIndex(),
                comment.getPositionStart(),
                comment.getPositionEnd(),
                comment.getAuthorReply(),
                comment.isAuthorResolved(),
                comment.getAuthorRepliedAt(),
                comment.getCreatedAt()
        );
    }

    private ReviewTemplateDTO toTemplateDTO(ReviewTemplateEntity template) {
        return new ReviewTemplateDTO(
                template.getId(),
                template.getName(),
                template.getContent(),
                template.getPriority().name(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
