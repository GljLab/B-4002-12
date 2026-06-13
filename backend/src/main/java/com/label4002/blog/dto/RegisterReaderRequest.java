package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterReaderRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度需在2到50之间")
        @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线和中文")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度不能少于8位")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
        String password,

        @NotBlank(message = "验证码key不能为空")
        String captchaKey,

        @NotBlank(message = "验证码不能为空")
        String captchaCode,

        @Size(max = 100, message = "昵称长度不能超过100")
        String nickname,

        @Size(max = 200, message = "安全问题长度不能超过200")
        String securityQuestion,

        @Size(max = 255, message = "安全答案长度不能超过255")
        String securityAnswer
) {
}
