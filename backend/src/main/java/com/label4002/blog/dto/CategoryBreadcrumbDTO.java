package com.label4002.blog.dto;

import java.util.List;

public record CategoryBreadcrumbDTO(
        Long id,
        String name,
        String slug
) {
    public static String buildPath(List<CategoryBreadcrumbDTO> breadcrumbs) {
        return breadcrumbs.stream()
                .map(CategoryBreadcrumbDTO::name)
                .reduce((a, b) -> a + "/" + b)
                .orElse("");
    }
}
