package com.label4002.blog.repository;

import com.label4002.blog.entity.CaptchaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CaptchaRepository extends JpaRepository<CaptchaEntity, String> {

    void deleteByCreatedAtBefore(LocalDateTime before);

    long countByCreatedAtAfter(LocalDateTime after);
}
