package com.label4002.blog.repository;

import com.label4002.blog.entity.CollectionAlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionAlbumRepository extends JpaRepository<CollectionAlbumEntity, Long> {

    List<CollectionAlbumEntity> findByUserIdOrderBySortOrderAscCreatedAtDesc(Long userId);

    Optional<CollectionAlbumEntity> findByShareToken(String shareToken);

    long countByUserId(Long userId);
}
