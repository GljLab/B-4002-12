package com.label4002.blog.dto;

import java.util.List;

public record BatchAddKeywordsRequest(
        List<Long> postIds,
        List<String> keywords
) {
}
