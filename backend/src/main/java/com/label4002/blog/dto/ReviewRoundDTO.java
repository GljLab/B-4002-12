package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewRoundDTO(
        Long id,
        Long postId,
        Long reviewerId,
        String reviewerName,
        String result,
        String modificationNote,
        LocalDateTime createdAt,
        List<ReviewCommentDTO> comments
) {
}
