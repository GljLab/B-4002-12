package com.label4002.blog.dto;

public record SensitiveWordDTO(
        Long id,
        String word,
        String category,
        int severity,
        String replacement,
        boolean active,
        String createdAt,
        String updatedAt
) {
}
