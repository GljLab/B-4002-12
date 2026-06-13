package com.label4002.blog.dto;

public record UpdateReadingProgressRequest(
        int durationSeconds,
        int scrollPosition
) {
}
