package com.label4002.blog.dto;

import java.util.List;

public record BatchUpdateCategoryRequest(
        List<Long> postIds,
        Long categoryId
) {
}
