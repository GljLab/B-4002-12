package com.label4002.blog.dto;

public record CommentReportDTO(
        Long id,
        Long commentId,
        String commentContent,
        Long commentAuthorId,
        String commentAuthorUsername,
        Long reporterId,
        String reporterUsername,
        String reporterNickname,
        String reason,
        String description,
        String status,
        Long reviewedBy,
        String reviewerUsername,
        String reviewedAt,
        String resolution,
        String createdAt,
        String updatedAt
) {
}
