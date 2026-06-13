package com.label4002.blog.controller;

import com.label4002.blog.dto.CommentReportDTO;
import com.label4002.blog.dto.CommentTreeDTO;
import com.label4002.blog.dto.CommentVoteRequest;
import com.label4002.blog.dto.CommentVoteResultDTO;
import com.label4002.blog.dto.CreateCommentReportRequest;
import com.label4002.blog.dto.CreateDiscussionRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PinnedCommentResultDTO;
import com.label4002.blog.dto.PostCommentSettingsDTO;
import com.label4002.blog.dto.UpdateDiscussionRequest;
import com.label4002.blog.dto.UpdatePostCommentSettingsRequest;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/posts/{postId}")
    public Map<String, Object> getDiscussionsForPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "1") int page) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return discussionService.getDiscussionTree(postId, currentUserId, sortBy, page);
    }

    @GetMapping("/{commentId}/replies")
    public Map<String, Object> getMoreReplies(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "1") int page) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return discussionService.getMoreReplies(commentId, currentUserId, page);
    }

    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentTreeDTO createDiscussion(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateDiscussionRequest request) {
        return discussionService.createDiscussion(principal.getId(), postId, request);
    }

    @PutMapping("/{commentId}")
    public CommentTreeDTO updateDiscussion(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateDiscussionRequest request) {
        return discussionService.updateDiscussion(principal.getId(), commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public MessageResponse deleteDiscussion(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        discussionService.deleteDiscussion(principal.getId(), commentId, false);
        return new MessageResponse("讨论已删除");
    }

    @PostMapping("/{commentId}/vote")
    public CommentVoteResultDTO vote(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CommentVoteRequest request) {
        return discussionService.vote(principal.getId(), commentId, request.voteType());
    }

    @PostMapping("/{commentId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentReportDTO reportComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateCommentReportRequest request) {
        return discussionService.reportComment(principal.getId(), commentId, request);
    }

    @PostMapping("/{commentId}/pin")
    public PinnedCommentResultDTO togglePin(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return discussionService.togglePin(principal.getId(), commentId);
    }

    @GetMapping("/posts/{postId}/settings")
    public PostCommentSettingsDTO getCommentSettings(@PathVariable Long postId) {
        return discussionService.getCommentSettings(postId);
    }

    @PatchMapping("/posts/{postId}/settings")
    public PostCommentSettingsDTO updateCommentSettings(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdatePostCommentSettingsRequest request) {
        return discussionService.updateCommentSettings(principal.getId(), postId, request);
    }
}
