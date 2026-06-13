package com.label4002.blog.repository;

import com.label4002.blog.entity.SensitiveWordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWordEntity, Long> {

    List<SensitiveWordEntity> findByActiveTrue();

    @Query("SELECT s FROM SensitiveWordEntity s WHERE s.active = true AND s.severity >= :minSeverity")
    List<SensitiveWordEntity> findActiveByMinSeverity(int minSeverity);

    Page<SensitiveWordEntity> findByCategory(SensitiveWordEntity.WordCategory category, Pageable pageable);

    Page<SensitiveWordEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByWord(String word);

    SensitiveWordEntity findByWord(String word);
}
