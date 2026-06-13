package com.label4002.blog.dto;

public record ReaderProfileDTO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        int readerLevel,
        int readerExp,
        boolean showFootprint,
        int streakDays,
        long readCount,
        long favoriteCount,
        long commentCount,
        long subscriptionCount,
        int maxCommentLength,
        int commentIntervalSeconds
) {
}
