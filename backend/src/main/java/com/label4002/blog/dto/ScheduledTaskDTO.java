package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record ScheduledTaskDTO(
        Long postId,
        String title,
        Long authorId,
        String authorName,
        LocalDateTime scheduledAt,
        boolean isRevision,
        Long parentPostId,
        String status
) {
}
