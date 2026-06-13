package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewTimelineDTO(
        Long postId,
        String postTitle,
        String postStatus,
        List<TimelineEventDTO> events
) {
    public record TimelineEventDTO(
            String type,
            Long roundId,
            Long userId,
            String userName,
            String action,
            String content,
            LocalDateTime timestamp
    ) {
    }
}
