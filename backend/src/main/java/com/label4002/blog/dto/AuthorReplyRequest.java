package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        String reply
) {
}
