package com.label4002.blog.dto;

public record ApiError(
        String code,
        String message,
        String traceId
) {
}
