package com.label4002.blog.repository;

import com.label4002.blog.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    List<ImageEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Optional<ImageEntity> findByFilename(String filename);

    long countByAuthorId(Long authorId);

    void deleteByFilename(String filename);
}
