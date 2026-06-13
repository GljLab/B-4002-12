package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthorDTO;
import com.label4002.blog.dto.AuthorStatsDTO;
import com.label4002.blog.dto.ChangePasswordRequest;
import com.label4002.blog.dto.CreatePostRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.RevisionDiffDTO;
import com.label4002.blog.dto.UpdateAuthorProfileRequest;
import com.label4002.blog.dto.UpdatePostRequest;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.AuthorService;
import com.label4002.blog.service.PostService;
import jakarta.validation.Valid;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/author")
public class AuthorController {

    private final AuthorService authorService;
    private final PostService postService;

    public AuthorController(AuthorService authorService, PostService postService) {
        this.authorService = authorService;
        this.postService = postService;
    }

    @GetMapping("/posts")
    public PageResponse<PostSummaryDTO> listMyPosts(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.listAuthorPosts(principal.getId(), status, page, size);
    }

    @GetMapping("/posts/{id}")
    public PostDetailDTO getMyPost(@AuthenticationPrincipal AppUserPrincipal principal,
                                   @PathVariable Long id) {
        PostDetailDTO post = postService.getDetail(id);
        if (!post.authorId().equals(principal.getId())) {
            throw new com.label4002.blog.exception.ForbiddenException("无权限查看他人的文章");
        }
        return post;
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailDTO createPost(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(principal.getId(), request);
    }

    @PutMapping("/posts/{id}")
    public PostDetailDTO updatePost(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdatePostRequest request) {
        return postService.updatePost(principal.getId(), id, request);
    }

    @PutMapping("/posts/{id}/submit")
    public MessageResponse submitForReview(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable Long id) {
        postService.submitForReview(principal.getId(), id);
        return new MessageResponse("已提交审核");
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@AuthenticationPrincipal AppUserPrincipal principal,
                           @PathVariable Long id) {
        postService.deletePost(principal.getId(), id);
    }

    @GetMapping("/stats")
    public AuthorStatsDTO getMyStats(@AuthenticationPrincipal AppUserPrincipal principal) {
        return postService.getAuthorStats(principal.getId());
    }

    @GetMapping("/profile")
    public AuthorDTO getMyProfile(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authorService.getAuthor(principal.getId());
    }

    @PutMapping("/profile")
    public AuthorDTO updateMyProfile(@AuthenticationPrincipal AppUserPrincipal principal,
                                     @Valid @RequestBody UpdateAuthorProfileRequest request) {
        return authorService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        authorService.changePassword(principal.getId(), request);
        return new MessageResponse("密码修改成功");
    }

    @PutMapping("/posts/{id}/cancel-schedule")
    public MessageResponse cancelSchedule(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long id) {
        postService.cancelSchedule(principal.getId(), id);
        return new MessageResponse("预约上线已取消");
    }

    @PutMapping("/posts/{id}/schedule")
    public MessageResponse updateSchedule(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long id,
                                          @RequestParam LocalDateTime scheduledAt) {
        postService.updateSchedule(principal.getId(), id, scheduledAt);
        return new MessageResponse("预约上线时间已更新");
    }

    @GetMapping("/posts/{id}/revision-diff")
    public RevisionDiffDTO getRevisionDiff(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable Long id) {
        PostDetailDTO post = postService.getDetail(id);
        if (!post.authorId().equals(principal.getId())) {
            throw new com.label4002.blog.exception.ForbiddenException("无权限查看他人的文章修订");
        }
        return postService.getRevisionDiff(id);
    }

    @GetMapping("/posts/{id}/revision")
    public PostDetailDTO getRevisionDetail(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable Long id) {
        PostDetailDTO revision = postService.getRevisionDetail(id);
        if (!revision.authorId().equals(principal.getId())) {
            throw new com.label4002.blog.exception.ForbiddenException("无权限查看他人的文章修订");
        }
        return revision;
    }

    @DeleteMapping("/posts/{id}/revision")
    public MessageResponse discardRevision(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable Long id) {
        postService.discardRevision(principal.getId(), id);
        return new MessageResponse("修订版本已丢弃");
    }
}
