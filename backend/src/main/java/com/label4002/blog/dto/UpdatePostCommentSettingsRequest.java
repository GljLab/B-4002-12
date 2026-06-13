package com.label4002.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdatePostCommentSettingsRequest(
        String accessRule,
        Boolean requireApproval,
        @Min(value = 1, message = "最大嵌套深度最小为1")
        @Max(value = 5, message = "最大嵌套深度最大为5")
        Integer maxDepth,
        @Min(value = 100, message = "最大长度最小为100")
        @Max(value = 10000, message = "最大长度最大为10000")
        Integer maxLength,
        Boolean allowImages,
        Boolean allowCode,
        Boolean allowEmojis,
        Boolean allowMentions
) {
}
