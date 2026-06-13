package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record ResubmitWithNoteRequest(
        @NotBlank(message = "修改说明不能为空")
        String modificationNote
) {
}
