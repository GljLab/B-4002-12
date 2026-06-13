package com.label4002.blog.repository;

import com.label4002.blog.entity.FavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    Page<FavoriteEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    Optional<FavoriteEntity> findByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("SELECT f.postId, COUNT(f.id) as cnt FROM FavoriteEntity f GROUP BY f.postId ORDER BY cnt DESC")
    List<Object[]> findPostFavoriteCounts(Pageable pageable);

    @Query("SELECT f.postId, COUNT(f.id) FROM FavoriteEntity f WHERE f.postId IN :postIds GROUP BY f.postId")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}
