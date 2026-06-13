package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateReviewCommentRequest(
        @NotBlank(message = "批注内容不能为空")
        String content,

        String priority,

        Integer paragraphIndex,

        Integer positionStart,

        Integer positionEnd
) {
}
