package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record KeywordDTO(
        Long id,
        String name,
        Integer usageCount,
        LocalDateTime lastUsedAt,
        Boolean archived,
        LocalDateTime createdAt
) {
}
