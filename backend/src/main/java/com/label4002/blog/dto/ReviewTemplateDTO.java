package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record ReviewTemplateDTO(
        Long id,
        String name,
        String content,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
