package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCommentReportRequest(
        @NotBlank(message = "处置结果不能为空")
        String action,
        @Size(max = 2000, message = "处置说明长度不能超过2000")
        String resolution
) {
}
