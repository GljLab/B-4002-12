package com.label4002.blog.dto;

public record PinnedCommentResultDTO(
        Long commentId,
        boolean pinned
) {
}
