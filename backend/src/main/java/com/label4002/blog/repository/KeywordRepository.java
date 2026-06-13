package com.label4002.blog.repository;

import com.label4002.blog.entity.KeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<KeywordEntity, Long> {

    Optional<KeywordEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<KeywordEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<KeywordEntity> findByArchivedFalseOrderByUsageCountDescNameAsc();

    List<KeywordEntity> findAllByOrderByUsageCountDescNameAsc();

    List<KeywordEntity> findAllByOrderByLastUsedAtDescNameAsc();

    @Query("SELECT k FROM KeywordEntity k WHERE k.archived = false ORDER BY k.usageCount DESC, k.name ASC")
    List<KeywordEntity> findActiveOrderByUsageCountDesc();

    @Query("SELECT k FROM KeywordEntity k WHERE k.archived = false AND k.name LIKE %:name% ORDER BY k.usageCount DESC, k.name ASC")
    List<KeywordEntity> searchActiveByName(@Param("name") String name);

    @Query("SELECT k FROM KeywordEntity k WHERE k.archived = true AND k.usageCount = 0 AND k.lastUsedAt < :threshold")
    List<KeywordEntity> findStaleArchivedBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE KeywordEntity k SET k.lastUsedAt = :now WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    long countByArchivedFalse();
}
