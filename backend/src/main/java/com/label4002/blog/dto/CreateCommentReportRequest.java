package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentReportRequest(
        @NotBlank(message = "举报原因不能为空")
        @Size(max = 50, message = "举报原因长度不能超过50")
        String reason,
        @Size(max = 2000, message = "描述长度不能超过2000")
        String description
) {
}
