package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "新密码长度需在6到100之间")
        String newPassword
) {
}
