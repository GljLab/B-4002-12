package com.label4002.blog.dto;

public record AuthorPublicDTO(
        Long id,
        String nickname,
        String username,
        String avatarUrl,
        String bio,
        long postCount
) {
}
