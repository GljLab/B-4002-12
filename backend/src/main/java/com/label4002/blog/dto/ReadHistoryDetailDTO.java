package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record ReadHistoryDetailDTO(
        Long id,
        Long postId,
        String postTitle,
        String postExcerpt,
        String postAuthorName,
        boolean postDeleted,
        int durationSeconds,
        int scrollPosition,
        LocalDateTime readAt
) {
}
