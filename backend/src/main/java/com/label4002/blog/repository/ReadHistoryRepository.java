package com.label4002.blog.repository;

import com.label4002.blog.entity.ReadHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReadHistoryRepository extends JpaRepository<ReadHistoryEntity, Long> {

    Page<ReadHistoryEntity> findByUserIdOrderByReadAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    Optional<ReadHistoryEntity> findByUserIdAndPostId(Long userId, Long postId);

    @Modifying
    @Query("DELETE FROM ReadHistoryEntity r WHERE r.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM ReadHistoryEntity r JOIN PostEntity p ON r.postId = p.id " +
           "JOIN CategoryEntity c ON p.category.id = c.id " +
           "WHERE r.userId = :userId AND c.id = :categoryId ORDER BY r.readAt DESC")
    Page<ReadHistoryEntity> findByUserIdAndCategoryIdOrderByReadAtDesc(@Param("userId") Long userId,
                                                                        @Param("categoryId") Long categoryId,
                                                                        Pageable pageable);

    @Query("SELECT rh.postId, COUNT(rh.id) as cnt FROM ReadHistoryEntity rh GROUP BY rh.postId ORDER BY cnt DESC")
    List<Object[]> findPostReadCounts(Pageable pageable);
}
