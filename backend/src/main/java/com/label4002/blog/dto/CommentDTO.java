package com.label4002.blog.dto;

public record CommentDTO(
        Long id,
        Long userId,
        String username,
        String nickname,
        String avatarUrl,
        Long postId,
        String postTitle,
        String content,
        String createdAt
) {
}
