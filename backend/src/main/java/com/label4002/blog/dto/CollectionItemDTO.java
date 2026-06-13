package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record CollectionItemDTO(
        Long id,
        Long albumId,
        Long postId,
        String postTitle,
        String postExcerpt,
        String postAuthorName,
        String note,
        int sortOrder,
        boolean postDeleted,
        LocalDateTime createdAt
) {
}
