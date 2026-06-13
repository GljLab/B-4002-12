package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 100, message = "分类名称长度不能超过100")
        String name,

        @Size(max = 100, message = "英文标识符长度不能超过100")
        String slug,

        Long parentId,

        Boolean enabled,

        Integer sortOrder
) {
}
