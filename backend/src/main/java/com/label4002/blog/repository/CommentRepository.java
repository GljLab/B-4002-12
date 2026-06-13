package com.label4002.blog.repository;

import com.label4002.blog.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    Page<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<CommentEntity> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    long countByUserId(Long userId);

    long countByPostId(Long postId);
}
