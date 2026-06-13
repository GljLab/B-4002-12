package com.label4002.blog.controller;

import com.label4002.blog.dto.CreateReviewCommentRequest;
import com.label4002.blog.dto.CreateReviewTemplateRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.ReviewCommentDTO;
import com.label4002.blog.dto.ReviewRoundDTO;
import com.label4002.blog.dto.ReviewTemplateDTO;
import com.label4002.blog.dto.ReviewTimelineDTO;
import com.label4002.blog.dto.RevisionDiffDTO;
import com.label4002.blog.dto.ReviewActionRequest;
import com.label4002.blog.dto.BatchReviewRequest;
import com.label4002.blog.dto.UpdateReviewTemplateRequest;
import com.label4002.blog.entity.CommentPriority;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.PostService;
import com.label4002.blog.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class ReviewController {

    private final PostService postService;
    private final ReviewService reviewService;

    public ReviewController(PostService postService, ReviewService reviewService) {
        this.postService = postService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public PageResponse<PostSummaryDTO> listPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.listPendingPosts(page, size);
    }

    @GetMapping("/{id}")
    public PostDetailDTO getPostForReview(@PathVariable Long id) {
        return postService.getDetail(id);
    }

    @GetMapping("/{id}/revision-diff")
    public RevisionDiffDTO getRevisionDiff(@PathVariable Long id) {
        return postService.getRevisionDiff(id);
    }

    @PutMapping("/{id}")
    public MessageResponse reviewAction(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ReviewActionRequest request) {
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            reviewService.approvePost(id, principal.getId());
            return new MessageResponse("文章已审核通过");
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            List<CreateReviewCommentRequest> comments = request.comments() != null ? request.comments() : List.of();
            reviewService.rejectPost(id, principal.getId(), comments);
            return new MessageResponse("文章已驳回");
        } else {
            throw new com.label4002.blog.exception.BadRequestException("无效的审核操作: " + request.action());
        }
    }

    @PutMapping("/batch")
    public MessageResponse batchReview(@Valid @RequestBody BatchReviewRequest request) {
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            postService.batchApprove(request.postIds());
            return new MessageResponse("批量审核通过完成");
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            postService.batchReject(request.postIds(), request.reason());
            return new MessageResponse("批量驳回完成");
        } else {
            throw new com.label4002.blog.exception.BadRequestException("无效的审核操作: " + request.action());
        }
    }

    @PostMapping("/{id}/comments")
    public ReviewCommentDTO addComment(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateReviewCommentRequest request) {
        return reviewService.addComment(id, principal.getId(), request);
    }

    @GetMapping("/{id}/history")
    public List<ReviewRoundDTO> getReviewHistory(@PathVariable Long id) {
        return reviewService.getReviewHistory(id);
    }

    @GetMapping("/{id}/timeline")
    public ReviewTimelineDTO getTimeline(@PathVariable Long id) {
        return reviewService.getTimeline(id);
    }

    @GetMapping("/templates")
    public List<ReviewTemplateDTO> listTemplates() {
        return reviewService.listTemplates();
    }

    @PostMapping("/templates")
    public ReviewTemplateDTO createTemplate(@Valid @RequestBody CreateReviewTemplateRequest request) {
        return reviewService.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    public ReviewTemplateDTO updateTemplate(@PathVariable Long id, @Valid @RequestBody UpdateReviewTemplateRequest request) {
        return reviewService.updateTemplate(id, request);
    }

    @DeleteMapping("/templates/{id}")
    public MessageResponse deleteTemplate(@PathVariable Long id) {
        reviewService.deleteTemplate(id);
        return new MessageResponse("模板已删除");
    }
}
