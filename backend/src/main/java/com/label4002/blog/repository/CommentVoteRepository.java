package com.label4002.blog.repository;

import com.label4002.blog.entity.CommentVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentVoteRepository extends JpaRepository<CommentVoteEntity, Long> {

    Optional<CommentVoteEntity> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentVoteEntity> findByCommentId(Long commentId);

    List<CommentVoteEntity> findByUserId(Long userId);

    @Query("SELECT v.voteType FROM CommentVoteEntity v WHERE v.commentId = :commentId AND v.userId = :userId")
    Integer findVoteTypeByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CommentVoteEntity v WHERE v.commentId = :commentId AND v.userId = :userId")
    void deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM CommentVoteEntity v WHERE v.commentId = :commentId AND v.voteType = 1")
    long countUpvotesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(v) FROM CommentVoteEntity v WHERE v.commentId = :commentId AND v.voteType = -1")
    long countDownvotesByCommentId(@Param("commentId") Long commentId);
}
