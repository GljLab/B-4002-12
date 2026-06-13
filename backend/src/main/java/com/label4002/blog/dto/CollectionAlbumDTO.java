package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record CollectionAlbumDTO(
        Long id,
        String name,
        String description,
        String coverUrl,
        boolean isPublic,
        int sortOrder,
        String shareToken,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
