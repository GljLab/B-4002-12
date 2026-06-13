package com.label4002.blog.repository;

import com.label4002.blog.entity.ReviewRoundEntity;
import com.label4002.blog.entity.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRoundRepository extends JpaRepository<ReviewRoundEntity, Long> {

    List<ReviewRoundEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<ReviewRoundEntity> findByPostIdAndResult(Long postId, ReviewResult result);

    long countByPostId(Long postId);
}
