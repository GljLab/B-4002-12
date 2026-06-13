package com.label4002.blog.repository;

import com.label4002.blog.entity.CollectionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionItemRepository extends JpaRepository<CollectionItemEntity, Long> {

    List<CollectionItemEntity> findByAlbumIdOrderBySortOrderAscCreatedAtDesc(Long albumId);

    boolean existsByAlbumIdAndPostId(Long albumId, Long postId);

    void deleteByAlbumIdAndPostId(Long albumId, Long postId);

    List<CollectionItemEntity> findByAlbumIdInOrderBySortOrderAscCreatedAtDesc(List<Long> albumIds);

    long countByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM CollectionItemEntity ci WHERE ci.albumId = :albumId AND ci.postId IN :postIds")
    void deleteByAlbumIdAndPostIdIn(@Param("albumId") Long albumId, @Param("postIds") List<Long> postIds);

    @Query("SELECT ci.postId, COUNT(ci.id) FROM CollectionItemEntity ci WHERE ci.postId IN :postIds GROUP BY ci.postId")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}
