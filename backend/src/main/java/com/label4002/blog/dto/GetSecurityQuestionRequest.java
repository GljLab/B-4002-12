package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record GetSecurityQuestionRequest(
        @NotBlank(message = "用户名不能为空")
        String username
) {
}
