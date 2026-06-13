package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record FavoriteDetailDTO(
        Long id,
        Long postId,
        String postTitle,
        String postExcerpt,
        String postAuthorName,
        boolean postDeleted,
        String note,
        LocalDateTime createdAt
) {
}
