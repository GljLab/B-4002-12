package com.label4002.blog.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken
) {
}
