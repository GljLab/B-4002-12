package com.label4002.blog.repository;

import com.label4002.blog.entity.ReviewTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewTemplateRepository extends JpaRepository<ReviewTemplateEntity, Long> {

    List<ReviewTemplateEntity> findAllByOrderByCreatedAtDesc();
}
