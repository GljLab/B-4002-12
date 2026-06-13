package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ReviewActionRequest(
        @NotBlank(message = "操作不能为空")
        String action,

        String reason,

        List<CreateReviewCommentRequest> comments
) {
}
