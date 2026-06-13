package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthorReplyRequest;
import com.label4002.blog.dto.ReviewCommentDTO;
import com.label4002.blog.dto.ReviewRoundDTO;
import com.label4002.blog.dto.ReviewTimelineDTO;
import com.label4002.blog.dto.ResubmitWithNoteRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/author/reviews")
public class AuthorReviewController {

    private final ReviewService reviewService;

    public AuthorReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{postId}/history")
    public List<ReviewRoundDTO> getReviewHistory(@PathVariable Long postId) {
        return reviewService.getReviewHistory(postId);
    }

    @GetMapping("/{postId}/timeline")
    public ReviewTimelineDTO getTimeline(@PathVariable Long postId) {
        return reviewService.getTimeline(postId);
    }

    @PutMapping("/comments/{commentId}/reply")
    public ReviewCommentDTO replyToComment(
            @PathVariable Long commentId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody AuthorReplyRequest request) {
        return reviewService.authorReply(commentId, principal.getId(), request);
    }

    @PutMapping("/comments/{commentId}/toggle-resolved")
    public ReviewCommentDTO toggleResolved(
            @PathVariable Long commentId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return reviewService.toggleResolved(commentId, principal.getId());
    }

    @PutMapping("/{postId}/resubmit")
    public MessageResponse resubmitWithNote(
            @PathVariable Long postId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ResubmitWithNoteRequest request) {
        reviewService.resubmitWithNote(postId, principal.getId(), request);
        return new MessageResponse("已重新提交审核");
    }
}
