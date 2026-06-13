package com.label4002.blog.controller;

import com.label4002.blog.dto.CommentReportDTO;
import com.label4002.blog.dto.CreateSensitiveWordRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.ReviewCommentReportRequest;
import com.label4002.blog.dto.SensitiveWordDTO;
import com.label4002.blog.dto.UpdateSensitiveWordRequest;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.DiscussionService;
import com.label4002.blog.service.SensitiveWordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/discussions")
public class AdminCommentController {

    private final DiscussionService discussionService;
    private final SensitiveWordService sensitiveWordService;

    public AdminCommentController(DiscussionService discussionService,
                                  SensitiveWordService sensitiveWordService) {
        this.discussionService = discussionService;
        this.sensitiveWordService = sensitiveWordService;
    }

    @GetMapping("/reports")
    public PageResponse<CommentReportDTO> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Page<CommentReportDTO> result = discussionService.getReports(status, normalizedPage - 1, normalizedSize);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), normalizedPage, normalizedSize);
    }

    @PostMapping("/reports/{reportId}/review")
    public CommentReportDTO reviewReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ReviewCommentReportRequest request) {
        return discussionService.reviewReport(principal.getId(), reportId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    public MessageResponse deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        discussionService.deleteDiscussion(principal.getId(), commentId, true);
        return new MessageResponse("讨论已删除");
    }

    @GetMapping("/sensitive-words")
    public PageResponse<SensitiveWordDTO> getSensitiveWords(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Page<SensitiveWordDTO> result;
        if (category != null && !category.isBlank()) {
            result = sensitiveWordService.getByCategory(category, normalizedPage - 1, normalizedSize);
        } else {
            result = sensitiveWordService.getAllWords(normalizedPage - 1, normalizedSize);
        }
        return new PageResponse<>(result.getContent(), result.getTotalElements(), normalizedPage, normalizedSize);
    }

    @GetMapping("/sensitive-words/{id}")
    public SensitiveWordDTO getSensitiveWord(@PathVariable Long id) {
        return sensitiveWordService.getById(id);
    }

    @PostMapping("/sensitive-words")
    @ResponseStatus(HttpStatus.CREATED)
    public SensitiveWordDTO createSensitiveWord(@Valid @RequestBody CreateSensitiveWordRequest request) {
        return sensitiveWordService.create(request);
    }

    @PutMapping("/sensitive-words/{id}")
    public SensitiveWordDTO updateSensitiveWord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSensitiveWordRequest request) {
        return sensitiveWordService.update(id, request);
    }

    @DeleteMapping("/sensitive-words/{id}")
    public MessageResponse deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return new MessageResponse("敏感词已删除");
    }

    @PostMapping("/sensitive-words/refresh-cache")
    public MessageResponse refreshSensitiveWordCache() {
        sensitiveWordService.refreshCache();
        return new MessageResponse("敏感词缓存已刷新");
    }

}
