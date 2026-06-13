package com.label4002.blog.dto;

public record AuthUserDTO(
        Long id,
        String username,
        String role,
        String nickname,
        String avatarUrl,
        Integer readerLevel
) {
    public AuthUserDTO(Long id, String username, String role, String nickname, String avatarUrl) {
        this(id, username, role, nickname, avatarUrl, null);
    }
}
