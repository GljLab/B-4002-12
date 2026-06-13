package com.label4002.blog.dto;

public record RevisionDiffDTO(
        Long revisionId,
        Long parentPostId,
        String parentTitle,
        String revisionTitle,
        String parentContent,
        String revisionContent,
        Long parentCategoryId,
        Long revisionCategoryId,
        String parentCategoryPath,
        String revisionCategoryPath,
        String parentKeywords,
        String revisionKeywords,
        String status,
        String authorName
) {
}
