package com.label4002.blog.dto;

import java.util.List;

public record ReorderItemsRequest(
        List<Long> itemIds
) {
}
