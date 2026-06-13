package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageDTO(
        Long id,
        String filename,
        String originalName,
        String url,
        Long authorId,
        String contentType,
        long fileSize,
        Integer width,
        Integer height,
        LocalDateTime createdAt,
        List<PostSummaryDTO> referencedPosts
) {
}
