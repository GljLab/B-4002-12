package com.label4002.blog.dto;

import java.util.List;

public record CommentTreeDTO(
        Long id,
        Long userId,
        String username,
        String nickname,
        String avatarUrl,
        Long postId,
        Long parentId,
        Long replyToCommentId,
        Long replyToUserId,
        String replyToUsername,
        String replyToNickname,
        int depth,
        String content,
        int upvotes,
        int downvotes,
        Integer currentUserVote,
        boolean pinned,
        boolean edited,
        String editedAt,
        boolean deleted,
        String deletedBy,
        boolean hasImages,
        boolean hasCode,
        boolean canEdit,
        boolean canDelete,
        String createdAt,
        List<CommentTreeDTO> replies
) {
}
