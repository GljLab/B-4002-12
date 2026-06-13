package com.label4002.blog.controller;

import com.label4002.blog.dto.AuthorPublicDTO;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.service.AuthorService;
import com.label4002.blog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authors")
public class AuthorProfileController {

    private final AuthorService authorService;
    private final PostService postService;

    public AuthorProfileController(AuthorService authorService, PostService postService) {
        this.authorService = authorService;
        this.postService = postService;
    }

    @GetMapping
    public List<AuthorPublicDTO> listPublicAuthors() {
        return authorService.listPublicAuthors();
    }

    @GetMapping("/{id}")
    public AuthorPublicDTO getPublicProfile(@PathVariable Long id) {
        return authorService.getPublicProfile(id);
    }

    @GetMapping("/{id}/posts")
    public PageResponse<PostSummaryDTO> listAuthorPosts(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.listPublicByAuthor(id, categoryId, page, size);
    }
}
