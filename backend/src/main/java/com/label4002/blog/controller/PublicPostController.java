package com.label4002.blog.controller;

import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@Validated
public class PublicPostController {

    private final PostService postService;
    private final PostRepository postRepository;

    public PublicPostController(PostService postService, PostRepository postRepository) {
        this.postService = postService;
        this.postRepository = postRepository;
    }

    @GetMapping
    public PageResponse<PostSummaryDTO> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false) Long authorId
    ) {
        return postService.listPublic(page, size, categoryId, keywordId, authorId);
    }

    @GetMapping("/{id}")
    public PostDetailDTO detail(@PathVariable Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("文章不存在"));
        if (post.getStatus() != PostStatus.PUBLISHED || post.isRevision()) {
            throw new NotFoundException("文章不存在");
        }
        postService.incrementViewCount(id);
        return postService.getDetail(id);
    }
}
