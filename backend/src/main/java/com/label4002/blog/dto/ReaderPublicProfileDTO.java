package com.label4002.blog.dto;

public record ReaderPublicProfileDTO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        int readerLevel,
        int streakDays,
        long readCount,
        long favoriteCount,
        long commentCount,
        long subscriptionCount
) {
}
