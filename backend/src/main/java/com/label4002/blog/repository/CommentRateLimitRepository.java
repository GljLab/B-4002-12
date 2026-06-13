package com.label4002.blog.repository;

import com.label4002.blog.entity.CommentRateLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CommentRateLimitRepository extends JpaRepository<CommentRateLimitEntity, Long> {

    Optional<CommentRateLimitEntity> findByUserIdAndWindowStart(Long userId, LocalDateTime windowStart);

    @Modifying
    @Query("UPDATE CommentRateLimitEntity r SET r.count = r.count + 1 WHERE r.userId = :userId AND r.windowStart = :windowStart")
    int incrementCount(@Param("userId") Long userId, @Param("windowStart") LocalDateTime windowStart);

    @Query("SELECT COALESCE(SUM(r.count), 0) FROM CommentRateLimitEntity r WHERE r.userId = :userId AND r.windowStart >= :windowStart")
    int countCommentsInWindow(@Param("userId") Long userId, @Param("windowStart") LocalDateTime windowStart);

    @Modifying
    @Query("DELETE FROM CommentRateLimitEntity r WHERE r.windowStart < :cutoffDate")
    int deleteOldWindows(@Param("cutoffDate") LocalDateTime cutoffDate);
}
