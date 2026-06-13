package com.label4002.blog.dto;

import jakarta.validation.constraints.Size;

public record UpdateAlbumRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description,
        String coverUrl,
        Boolean isPublic
) {
}
