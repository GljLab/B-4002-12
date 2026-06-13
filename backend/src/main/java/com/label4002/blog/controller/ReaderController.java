package com.label4002.blog.controller;

import com.label4002.blog.dto.CaptchaDTO;
import com.label4002.blog.dto.GetSecurityQuestionRequest;
import com.label4002.blog.dto.ReaderProfileDTO;
import com.label4002.blog.dto.ReaderPublicProfileDTO;
import com.label4002.blog.dto.RegisterReaderRequest;
import com.label4002.blog.dto.ResetPasswordRequest;
import com.label4002.blog.dto.SecurityQuestionDTO;
import com.label4002.blog.dto.UpdateReaderProfileRequest;
import com.label4002.blog.dto.UpdateReadingProgressRequest;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.ReaderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @GetMapping("/captcha")
    public CaptchaDTO getCaptcha() {
        return readerService.generateCaptcha();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ReaderProfileDTO register(@Valid @RequestBody RegisterReaderRequest request,
                                     HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return readerService.register(request, clientIp);
    }

    @GetMapping("/me")
    public ReaderProfileDTO getMyProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return readerService.getMyProfile(principal);
    }

    @PutMapping("/me")
    public ReaderProfileDTO updateProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                          @Valid @RequestBody UpdateReaderProfileRequest request) {
        return readerService.updateProfile(principal, request);
    }

    @GetMapping("/{id}")
    public ReaderPublicProfileDTO getPublicProfile(@PathVariable Long id) {
        return readerService.getPublicProfile(id);
    }

    @PostMapping("/security-question")
    public SecurityQuestionDTO getSecurityQuestion(@Valid @RequestBody GetSecurityQuestionRequest request) {
        return readerService.getSecurityQuestion(request.username());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        readerService.resetPassword(request);
        Map<String, String> result = new HashMap<>();
        result.put("message", "密码重置成功");
        return result;
    }

    @PostMapping("/{postId}/read")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> recordRead(@PathVariable Long postId,
                                          @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        readerService.recordReadHistory(principal.getId(), postId);
        readerService.updateStreakAndExp(principal.getId());
        Map<String, String> result = new HashMap<>();
        result.put("message", "已记录");
        return result;
    }

    @PostMapping("/{postId}/favorite")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> toggleFavorite(@PathVariable Long postId,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        readerService.toggleFavorite(principal.getId(), postId);
        Map<String, String> result = new HashMap<>();
        result.put("message", "操作成功");
        return result;
    }

    @PostMapping("/{postId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createComment(@PathVariable Long postId,
                                             @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                             @RequestParam String content) {
        int maxLen = readerService.getMaxCommentLength(
                readerService.getMyProfile(principal).readerLevel());
        if (content.length() > maxLen) {
            Map<String, String> err = new HashMap<>();
            err.put("message", "当前等级评论最长" + maxLen + "字");
            return err;
        }
        readerService.createComment(principal.getId(), postId, content);
        Map<String, String> result = new HashMap<>();
        result.put("message", "评论成功");
        return result;
    }

    @PostMapping("/subscribe/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> toggleSubscription(@PathVariable Long authorId,
                                                  @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        readerService.toggleSubscription(principal.getId(), authorId);
        Map<String, String> result = new HashMap<>();
        result.put("message", "操作成功");
        return result;
    }

    @GetMapping("/me/history")
    public Map<String, Object> getReadHistory(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) Long categoryId) {
        var pageResult = categoryId != null
                ? readerService.getReadHistoryByCategory(principal.getId(), categoryId, page, size)
                : readerService.getReadHistory(principal.getId(), page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/me/favorites")
    public Map<String, Object> getFavorites(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        var pageResult = readerService.getFavorites(principal.getId(), page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/me/reading-progress/{postId}")
    public Map<String, Object> getReadingProgress(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long postId) {
        var progress = readerService.getReadingProgress(principal.getId(), postId);
        Map<String, Object> result = new HashMap<>();
        if (progress != null) {
            result.put("postId", progress.postId());
            result.put("durationSeconds", progress.durationSeconds());
            result.put("scrollPosition", progress.scrollPosition());
            result.put("readAt", progress.readAt());
        }
        return result;
    }

    @PutMapping("/me/reading-progress/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> updateReadingProgress(@PathVariable Long postId,
                                                      @org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                                      @Valid @RequestBody UpdateReadingProgressRequest request) {
        readerService.updateReadingProgress(principal.getId(), postId, request.durationSeconds(), request.scrollPosition());
        Map<String, String> result = new HashMap<>();
        result.put("message", "已更新");
        return result;
    }

    @DeleteMapping("/me/history")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> clearReadHistory(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        readerService.clearReadHistory(principal.getId());
        Map<String, String> result = new HashMap<>();
        result.put("message", "阅读记录已清空");
        return result;
    }

    @GetMapping("/me/comments")
    public Map<String, Object> getMyComments(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        var pageResult = readerService.getComments(principal.getId(), page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/me/subscriptions")
    public Map<String, Object> getMySubscriptions(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        var pageResult = readerService.getSubscriptions(principal.getId(), page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
