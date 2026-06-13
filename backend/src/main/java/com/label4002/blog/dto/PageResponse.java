package com.label4002.blog.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int size
) {
}
