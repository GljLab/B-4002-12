package com.label4002.blog.dto;

public record UnreadNotificationCountDTO(
        long total,
        long replyCount,
        long mentionCount,
        long upvoteCount,
        long reportResultCount
) {
}
