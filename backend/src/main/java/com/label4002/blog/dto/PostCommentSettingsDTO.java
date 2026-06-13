package com.label4002.blog.dto;

public record PostCommentSettingsDTO(
        Long id,
        Long postId,
        String accessRule,
        boolean requireApproval,
        int maxDepth,
        int maxLength,
        boolean allowImages,
        boolean allowCode,
        boolean allowEmojis,
        boolean allowMentions,
        String createdAt,
        String updatedAt
) {
}
