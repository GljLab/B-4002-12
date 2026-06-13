package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthorRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度需在6到100之间")
        String password,

        @Size(max = 100, message = "昵称长度不能超过100")
        String nickname,

        @Size(max = 500, message = "头像URL长度不能超过500")
        String avatarUrl,

        @Size(max = 500, message = "简介长度不能超过500")
        String bio
) {
}
