package com.label4002.blog.repository;

import com.label4002.blog.entity.SubscriptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Page<SubscriptionEntity> findByReaderIdOrderByCreatedAtDesc(Long readerId, Pageable pageable);

    long countByReaderId(Long readerId);

    boolean existsByReaderIdAndAuthorId(Long readerId, Long authorId);

    void deleteByReaderIdAndAuthorId(Long readerId, Long authorId);
}
