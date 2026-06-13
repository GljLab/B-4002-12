package com.label4002.blog.dto;

import jakarta.validation.constraints.Size;

public record UpdateAuthorRequest(
        @Size(max = 100, message = "昵称长度不能超过100")
        String nickname,

        @Size(max = 500, message = "头像URL长度不能超过500")
        String avatarUrl,

        @Size(max = 500, message = "简介长度不能超过500")
        String bio
) {
}
