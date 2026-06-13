package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateReviewTemplateRequest(
        @NotBlank(message = "模板名称不能为空")
        String name,

        @NotBlank(message = "模板内容不能为空")
        String content,

        String priority
) {
}
