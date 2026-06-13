package com.label4002.blog.controller;

import com.label4002.blog.dto.BatchAddKeywordsRequest;
import com.label4002.blog.dto.BatchUpdateCategoryRequest;
import com.label4002.blog.dto.CreatePostRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.ScheduledTaskDTO;
import com.label4002.blog.dto.UpdatePostRequest;
import com.label4002.blog.security.AppUserPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/mine")
    public List<PostSummaryDTO> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return postService.listMine(principal.getId());
    }

    @GetMapping
    public List<PostSummaryDTO> list(@RequestParam(required = false) String status) {
        return postService.listAllPosts(status);
    }

    @GetMapping("/{id}")
    public PostDetailDTO getById(@PathVariable Long id) {
        return postService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailDTO create(@AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(principal.getId(), request);
    }

    @PutMapping("/{id}")
    public PostDetailDTO update(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id,
                                @Valid @RequestBody UpdatePostRequest request) {
        return postService.updatePost(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long id) {
        postService.deletePost(principal.getId(), id);
    }

    @PutMapping("/{id}/unpublish")
    public PostDetailDTO unpublish(@PathVariable Long id) {
        postService.unpublishPost(id);
        return postService.getDetail(id);
    }

    @PutMapping("/batch/category")
    public void batchUpdateCategory(@Valid @RequestBody BatchUpdateCategoryRequest request) {
        postService.batchUpdateCategory(request.postIds(), request.categoryId());
    }

    @PostMapping("/batch/keywords")
    public void batchAddKeywords(@Valid @RequestBody BatchAddKeywordsRequest request) {
        postService.batchAddKeywords(request.postIds(), request.keywords());
    }

    @GetMapping("/scheduled")
    public PageResponse<ScheduledTaskDTO> listScheduled(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.listScheduledTasks(page, size);
    }

    @PutMapping("/{id}/force-publish")
    public MessageResponse forcePublish(@PathVariable Long id) {
        postService.forcePublishScheduled(id);
        return new MessageResponse("文章已强制发布");
    }

    @PutMapping("/{id}/cancel-schedule")
    public MessageResponse cancelSchedule(@PathVariable Long id) {
        PostDetailDTO post = postService.getDetail(id);
        postService.cancelSchedule(post.authorId(), id);
        return new MessageResponse("预约上线已取消");
    }
}
