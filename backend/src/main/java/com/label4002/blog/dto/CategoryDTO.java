package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryDTO(
        Long id,
        String name,
        String slug,
        Long parentId,
        String parentName,
        Boolean enabled,
        Integer sortOrder,
        Long postCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CategoryDTO> children
) {
}
