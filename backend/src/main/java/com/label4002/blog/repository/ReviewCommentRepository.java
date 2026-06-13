package com.label4002.blog.repository;

import com.label4002.blog.entity.ReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewCommentEntity, Long> {

    List<ReviewCommentEntity> findByRoundIdOrderByCreatedAtAsc(Long roundId);

    List<ReviewCommentEntity> findByRoundId(Long roundId);

    long countByRoundId(Long roundId);
}
