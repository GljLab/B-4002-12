package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchReviewRequest(
        @NotEmpty(message = "文章ID列表不能为空")
        List<Long> postIds,

        @NotBlank(message = "操作不能为空")
        String action,

        String reason
) {
}
