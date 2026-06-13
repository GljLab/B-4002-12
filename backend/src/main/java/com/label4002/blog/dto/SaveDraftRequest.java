package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveDraftRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题长度不能超过200")
        String title,

        @NotBlank(message = "内容不能为空")
        String content,

        Long categoryId,

        List<String> keywords
) {
}
