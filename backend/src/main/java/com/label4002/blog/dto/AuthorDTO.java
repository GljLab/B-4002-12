package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record AuthorDTO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        boolean enabled,
        String role,
        LocalDateTime createdAt,
        long postCount,
        LocalDateTime lastLoginAt
) {
}
