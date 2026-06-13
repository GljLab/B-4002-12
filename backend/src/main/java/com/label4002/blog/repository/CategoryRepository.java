package com.label4002.blog.repository;

import com.label4002.blog.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<CategoryEntity> findByParentIdIsNullOrderBySortOrderAscNameAsc();

    List<CategoryEntity> findByParentIdOrderBySortOrderAscNameAsc(Long parentId);

    List<CategoryEntity> findByEnabledTrueOrderBySortOrderAscNameAsc();

    List<CategoryEntity> findByParentIdInAndEnabledTrue(List<Long> parentIds);

    List<CategoryEntity> findByParentIdAndEnabledTrueOrderBySortOrderAscNameAsc(Long parentId);

    @Query("SELECT c FROM CategoryEntity c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<CategoryEntity> findRootsWithChildren();

    @Query("SELECT c.id FROM CategoryEntity c WHERE c.parent.id = :parentId")
    List<Long> findIdsByParentId(@Param("parentId") Long parentId);

    long countByParentId(Long parentId);

    boolean existsByParentId(Long parentId);

    @Query("SELECT c FROM CategoryEntity c ORDER BY c.sortOrder ASC, c.name ASC")
    List<CategoryEntity> findAllOrderBySortOrder();
}
