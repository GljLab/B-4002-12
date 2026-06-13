package com.label4002.blog.dto;

public record NotificationDTO(
        Long id,
        String type,
        Long relatedCommentId,
        Long relatedPostId,
        String relatedPostTitle,
        Long relatedUserId,
        String relatedUsername,
        String relatedNickname,
        String relatedAvatarUrl,
        String title,
        String content,
        boolean read,
        String readAt,
        String createdAt
) {
}
