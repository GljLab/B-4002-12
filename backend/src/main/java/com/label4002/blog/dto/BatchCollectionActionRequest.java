package com.label4002.blog.dto;

import java.util.List;

public record BatchCollectionActionRequest(
        List<Long> itemIds,
        Long targetAlbumId,
        String action
) {
}
