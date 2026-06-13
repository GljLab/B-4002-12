package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record ImageRenameRequest(
        @NotBlank(message = "新名称不能为空")
        String originalName
) {
}
