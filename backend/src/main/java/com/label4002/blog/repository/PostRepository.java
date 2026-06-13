package com.label4002.blog.repository;

import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PostEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Optional<PostEntity> findByIdAndAuthorId(Long id, Long authorId);

    @Query("SELECT p FROM PostEntity p JOIN p.category c WHERE c.id IN :categoryIds AND c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findByCategoryIdsAndEnabled(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    @Query("SELECT DISTINCT p FROM PostEntity p JOIN p.postKeywords pk JOIN pk.keyword k JOIN p.category c WHERE k.id = :keywordId AND c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findByKeywordIdAndCategoryEnabled(@Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM PostEntity p JOIN p.category c JOIN p.postKeywords pk JOIN pk.keyword k WHERE c.id IN :categoryIds AND c.enabled = true AND k.id = :keywordId ORDER BY p.createdAt DESC")
    Page<PostEntity> findByCategoryIdsAndKeywordId(@Param("categoryIds") List<Long> categoryIds, @Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p JOIN p.category c WHERE c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findAllWithEnabledCategory(Pageable pageable);

    long countByCategoryId(Long categoryId);

    @Query("SELECT p.category.id, COUNT(p) FROM PostEntity p WHERE p.category.id IS NOT NULL GROUP BY p.category.id")
    List<Object[]> countByCategoryIdGrouped();

    @Modifying
    @Query("UPDATE PostEntity p SET p.category.id = :newCategoryId WHERE p.category.id = :oldCategoryId")
    void reassignCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategoryId") Long newCategoryId);

    Page<PostEntity> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);

    Page<PostEntity> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status ORDER BY p.createdAt ASC")
    Page<PostEntity> findByStatus(@Param("status") PostStatus status, Pageable pageable);

    List<PostEntity> findTop10ByAuthorIdAndStatusOrderByViewCountDesc(Long authorId, PostStatus status);

    long countByAuthorIdAndStatus(Long authorId, PostStatus status);

    long countByAuthorId(Long authorId);

    long countByStatus(PostStatus status);

    long countByStatusAndCreatedAtBetween(PostStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM PostEntity p WHERE p.author.id = :authorId AND p.status = :status")
    long sumViewCountByAuthorIdAndStatus(@Param("authorId") Long authorId, @Param("status") PostStatus status);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM PostEntity p WHERE p.status = 'PUBLISHED'")
    long sumPublishedViewCount();

    @Modifying
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Query("SELECT p.author.id, p.author.nickname, p.author.avatarUrl, COUNT(p) as postCount, COALESCE(SUM(p.viewCount),0) as totalViews FROM PostEntity p WHERE p.status = 'PUBLISHED' GROUP BY p.author.id, p.author.nickname, p.author.avatarUrl ORDER BY postCount DESC")
    List<Object[]> findAuthorRankingByPostCount();

    @Query("SELECT p.author.id, p.author.nickname, p.author.avatarUrl, COUNT(p) as postCount, COALESCE(SUM(p.viewCount),0) as totalViews FROM PostEntity p WHERE p.status = 'PUBLISHED' GROUP BY p.author.id, p.author.nickname, p.author.avatarUrl ORDER BY totalViews DESC")
    List<Object[]> findAuthorRankingByViews();

    @Modifying
    @Query("UPDATE PostEntity p SET p.author.id = :newAuthorId WHERE p.author.id = :oldAuthorId")
    void transferAuthor(@Param("oldAuthorId") Long oldAuthorId, @Param("newAuthorId") Long newAuthorId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.status = :status WHERE p.id IN :ids")
    void updateStatusByIds(@Param("status") PostStatus status, @Param("ids") List<Long> ids);

    Page<PostEntity> findByStatusAndCategoryId(PostStatus status, Long categoryId, Pageable pageable);

    Page<PostEntity> findByAuthorIdAndStatusAndCategoryId(Long authorId, PostStatus status, Long categoryId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.scheduledAt IS NOT NULL AND p.scheduledAt <= :now")
    List<PostEntity> findScheduledPostsDue(@Param("status") PostStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.scheduledAt IS NOT NULL ORDER BY p.scheduledAt ASC")
    Page<PostEntity> findScheduledPosts(@Param("status") PostStatus status, Pageable pageable);

    Optional<PostEntity> findByParentPostIdAndRevisionTrue(Long parentPostId);

    List<PostEntity> findByParentPostId(Long parentPostId);

    long countByStatusAndRevisionTrue(PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.revision = true AND p.parentPost.id = :parentPostId")
    Optional<PostEntity> findRevisionByParentPostId(@Param("parentPostId") Long parentPostId);
}
