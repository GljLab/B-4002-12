package com.label4002.blog.dto;

import java.time.LocalDateTime;

public record ReviewCommentDTO(
        Long id,
        Long roundId,
        String content,
        String priority,
        Integer paragraphIndex,
        Integer positionStart,
        Integer positionEnd,
        String authorReply,
        boolean authorResolved,
        LocalDateTime authorRepliedAt,
        LocalDateTime createdAt
) {
}
