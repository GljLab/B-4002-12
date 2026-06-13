package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "refreshToken不能为空")
        String refreshToken
) {
}
