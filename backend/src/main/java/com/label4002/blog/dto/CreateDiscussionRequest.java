package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiscussionRequest(
        @NotBlank(message = "讨论内容不能为空")
        @Size(min = 1, max = 5000, message = "讨论长度需在1到5000之间")
        String content,
        Long parentId,
        Long replyToCommentId,
        Long replyToUserId
) {
}
