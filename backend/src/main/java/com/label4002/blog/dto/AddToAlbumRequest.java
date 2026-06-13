package com.label4002.blog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddToAlbumRequest(
        @NotNull Long postId,
        @Size(max = 500) String note
) {
}
