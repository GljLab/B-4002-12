package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "安全答案不能为空")
        String securityAnswer,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度不能少于8位")
        @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
        String newPassword
) {
}
