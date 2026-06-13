package com.label4002.blog.dto;

public record PostFavoriteCountDTO(
        Long postId,
        String postTitle,
        long favoriteCount
) {
}
