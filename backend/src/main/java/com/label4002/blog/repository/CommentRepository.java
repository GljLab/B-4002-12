package com.label4002.blog.repository;

import com.label4002.blog.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    Page<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<CommentEntity> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    long countByUserId(Long userId);

    long countByPostId(Long postId);

    long countByPostIdAndDeletedFalse(Long postId);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.deleted = false ORDER BY c.pinned DESC, c.createdAt DESC")
    List<CommentEntity> findRootCommentsByPostIdOrderByNewest(@Param("postId") Long postId);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.deleted = false ORDER BY c.pinned DESC, (c.upvotes - c.downvotes) DESC, c.createdAt DESC")
    List<CommentEntity> findRootCommentsByPostIdOrderByPopularity(@Param("postId") Long postId);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.deleted = false ORDER BY c.pinned DESC, c.createdAt ASC")
    List<CommentEntity> findRootCommentsByPostIdOrderByOldest(@Param("postId") Long postId);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.deleted = false AND c.userId = :authorUserId ORDER BY c.pinned DESC, c.createdAt DESC")
    List<CommentEntity> findRootCommentsByPostIdAndAuthor(@Param("postId") Long postId, @Param("authorUserId") Long authorUserId);

    @Query("SELECT c FROM CommentEntity c WHERE c.parentId = :parentId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<CommentEntity> findRepliesByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM CommentEntity c WHERE c.parentId = :parentId AND c.deleted = false ORDER BY c.createdAt ASC")
    Page<CommentEntity> findRepliesByParentIdPaged(@Param("parentId") Long parentId, Pageable pageable);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.deleted = false AND c.path LIKE :pathPrefix% ORDER BY c.path ASC, c.createdAt ASC")
    List<CommentEntity> findByPostIdAndPathPrefix(@Param("postId") Long postId, @Param("pathPrefix") String pathPrefix);

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.deleted = false")
    List<CommentEntity> findAllNonDeletedByPostId(@Param("postId") Long postId);

    @Query("SELECT c.userId FROM CommentEntity c WHERE c.postId = :postId AND c.deleted = false GROUP BY c.userId")
    List<Long> findDistinctUserIdsByPostId(@Param("postId") Long postId);

    Optional<CommentEntity> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COUNT(c) > 0 FROM CommentEntity c WHERE c.id = :id AND c.userId = :userId AND c.deleted = false")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.upvotes = c.upvotes + :delta WHERE c.id = :commentId")
    int adjustUpvotes(@Param("commentId") Long commentId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.downvotes = c.downvotes + :delta WHERE c.id = :commentId")
    int adjustDownvotes(@Param("commentId") Long commentId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.deleted = true, c.deletedAt = :deletedAt, c.deletedBy = :deletedBy WHERE c.id = :commentId")
    int softDelete(@Param("commentId") Long commentId, @Param("deletedAt") java.time.LocalDateTime deletedAt, @Param("deletedBy") String deletedBy);

    @Query("SELECT MAX(c.sortOrder) FROM CommentEntity c WHERE c.parentId = :parentId")
    Integer findMaxSortOrderByParentId(@Param("parentId") Long parentId);

    @Query("SELECT MAX(c.sortOrder) FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL")
    Integer findMaxSortOrderByPostIdRoot(@Param("postId") Long postId);
}
