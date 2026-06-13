package com.label4002.blog.dto;

import java.util.List;

public record AuthorStatsDTO(
        long totalPublished,
        long totalDraft,
        long totalViews,
        long avgViews,
        List<PostSummaryDTO> topPosts
) {
}
