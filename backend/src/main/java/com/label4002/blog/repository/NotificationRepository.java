package com.label4002.blog.repository;

import com.label4002.blog.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead, Pageable pageable);

    List<NotificationEntity> findByUserIdAndIsRead(Long userId, boolean isRead);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.id = :id")
    int markAsRead(@Param("userId") Long userId, @Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("userId") Long userId, @Param("cutoffDate") LocalDateTime cutoffDate);
}
