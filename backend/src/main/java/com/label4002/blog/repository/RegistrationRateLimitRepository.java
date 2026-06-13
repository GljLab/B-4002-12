package com.label4002.blog.repository;

import com.label4002.blog.entity.RegistrationRateLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RegistrationRateLimitRepository extends JpaRepository<RegistrationRateLimitEntity, Long> {

    long countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime after);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
