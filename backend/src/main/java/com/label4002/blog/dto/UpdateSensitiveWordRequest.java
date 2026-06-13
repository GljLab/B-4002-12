package com.label4002.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSensitiveWordRequest(
        @Size(min = 1, max = 100, message = "敏感词长度需在1到100之间")
        String word,
        String category,
        @Min(value = 1, message = "严重程度最小为1")
        @Max(value = 3, message = "严重程度最大为3")
        Integer severity,
        @Size(max = 100, message = "替换文本长度不能超过100")
        String replacement,
        Boolean active
) {
}
