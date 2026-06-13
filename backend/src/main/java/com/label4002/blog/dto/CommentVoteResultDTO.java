package com.label4002.blog.dto;

public record CommentVoteResultDTO(
        Long commentId,
        int upvotes,
        int downvotes,
        Integer currentUserVote
) {
}
