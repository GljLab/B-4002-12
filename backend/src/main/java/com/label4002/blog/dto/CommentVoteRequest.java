package com.label4002.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CommentVoteRequest(
        @NotNull(message = "投票类型不能为空")
        @Min(value = -1, message = "投票类型只能是-1、0或1")
        @Max(value = 1, message = "投票类型只能是-1、0或1")
        Integer voteType
) {
}
