package com.label4002.blog.service;

import com.label4002.blog.dto.CategoryBreadcrumbDTO;
import com.label4002.blog.dto.CategoryDTO;
import com.label4002.blog.dto.CreateCategoryRequest;
import com.label4002.blog.dto.UpdateCategoryRequest;
import com.label4002.blog.entity.CategoryEntity;
import com.label4002.blog.exception.BusinessException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CategoryRepository;
import com.label4002.blog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listTree(boolean onlyEnabled) {
        List<CategoryEntity> all = onlyEnabled
                ? categoryRepository.findByEnabledTrueOrderBySortOrderAscNameAsc()
                : categoryRepository.findAllOrderBySortOrder();

        Map<Long, Long> postCountMap = buildPostCountMap();

        Map<Long, CategoryDTO> dtoMap = new LinkedHashMap<>();
        for (CategoryEntity entity : all) {
            dtoMap.put(entity.getId(), toDTO(entity, postCountMap.getOrDefault(entity.getId(), 0L)));
        }

        List<CategoryDTO> roots = new ArrayList<>();
        for (CategoryEntity entity : all) {
            CategoryDTO dto = dtoMap.get(entity.getId());
            if (entity.getParent() == null) {
                roots.add(dto);
            } else {
                CategoryDTO parentDto = dtoMap.get(entity.getParent().getId());
                if (parentDto != null) {
                    parentDto.children().add(dto);
                }
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listAllFlat(boolean onlyEnabled) {
        List<CategoryEntity> all = onlyEnabled
                ? categoryRepository.findByEnabledTrueOrderBySortOrderAscNameAsc()
                : categoryRepository.findAllOrderBySortOrder();
        Map<Long, Long> postCountMap = buildPostCountMap();
        return all.stream()
                .map(e -> toDTO(e, postCountMap.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getById(Long id) {
        CategoryEntity entity = findOrThrow(id);
        Map<Long, Long> postCountMap = buildPostCountMap();
        return toDTO(entity, postCountMap.getOrDefault(entity.getId(), 0L));
    }

    @Transactional(readOnly = true)
    public List<CategoryBreadcrumbDTO> getBreadcrumb(Long categoryId) {
        CategoryEntity entity = findOrThrow(categoryId);
        List<CategoryBreadcrumbDTO> path = new ArrayList<>();
        CategoryEntity current = entity;
        while (current != null) {
            path.addFirst(new CategoryBreadcrumbDTO(current.getId(), current.getName(), current.getSlug()));
            current = current.getParent();
        }
        return path;
    }

    @Transactional(readOnly = true)
    public Set<Long> getEnabledCategoryIdsWithDescendants(Long categoryId) {
        CategoryEntity entity = findOrThrow(categoryId);
        if (!entity.isEnabled()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        ids.add(entity.getId());
        collectEnabledDescendantIds(entity, ids);
        return ids;
    }

    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name().trim());
        entity.setSlug(resolveSlug(request.slug(), request.name()));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

        if (request.parentId() != null) {
            CategoryEntity parent = findOrThrow(request.parentId());
            if (!parent.isEnabled()) {
                throw new BusinessException("PARENT_DISABLED", "不能在禁用的分类下创建子分类", HttpStatus.BAD_REQUEST);
            }
            entity.setParent(parent);
        }

        return toDTO(categoryRepository.save(entity), 0L);
    }

    @Transactional
    public CategoryDTO update(Long id, UpdateCategoryRequest request) {
        CategoryEntity entity = findOrThrow(id);

        entity.setName(request.name().trim());
        entity.setSlug(resolveSlugForUpdate(request.slug(), request.name(), id));

        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }

        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BusinessException("CIRCULAR_REFERENCE", "分类不能成为自身的子分类", HttpStatus.BAD_REQUEST);
            }
            CategoryEntity newParent = findOrThrow(request.parentId());
            checkCircularReference(id, newParent);
            entity.setParent(newParent);
        } else if (request.parentId() == null && entity.getParent() != null) {
            entity.setParent(null);
        }

        Map<Long, Long> postCountMap = buildPostCountMap();
        return toDTO(categoryRepository.save(entity), postCountMap.getOrDefault(entity.getId(), 0L));
    }

    @Transactional
    public void delete(Long id) {
        CategoryEntity entity = findOrThrow(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException("HAS_CHILDREN", "该分类下存在子分类，不允许直接删除。请先移动或删除子分类。", HttpStatus.BAD_REQUEST);
        }

        long postCount = postRepository.countByCategoryId(id);
        if (postCount > 0) {
            CategoryEntity fallback = entity.getParent();
            if (fallback == null) {
                fallback = categoryRepository.findBySlug("default")
                        .orElseThrow(() -> new NotFoundException("默认分类不存在"));
            }
            if (fallback.getId().equals(id)) {
                throw new BusinessException("CANNOT_DELETE_DEFAULT", "默认分类下存在文章，无法删除", HttpStatus.BAD_REQUEST);
            }
            postRepository.reassignCategory(id, fallback.getId());
        }

        categoryRepository.delete(entity);
    }

    @Transactional
    public void toggleEnabled(Long id) {
        CategoryEntity entity = findOrThrow(id);
        entity.setEnabled(!entity.isEnabled());
        categoryRepository.save(entity);
    }

    private void checkCircularReference(Long categoryId, CategoryEntity newParent) {
        Set<Long> visited = new HashSet<>();
        CategoryEntity current = newParent;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                throw new BusinessException("CIRCULAR_REFERENCE", "调整父级关系会形成循环引用，操作已阻止", HttpStatus.BAD_REQUEST);
            }
            if (!visited.add(current.getId())) {
                break;
            }
            current = current.getParent();
        }
    }

    private String resolveSlug(String slug, String name) {
        if (slug != null && !slug.isBlank()) {
            String cleaned = slug.trim().toLowerCase().replaceAll("[^a-z0-9\\-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
            if (!cleaned.isEmpty()) {
                return ensureSlugUnique(cleaned, null);
            }
        }
        String generated = generatePinyinSlug(name);
        return ensureSlugUnique(generated, null);
    }

    private String resolveSlugForUpdate(String slug, String name, Long id) {
        if (slug != null && !slug.isBlank()) {
            String cleaned = slug.trim().toLowerCase().replaceAll("[^a-z0-9\\-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
            if (!cleaned.isEmpty()) {
                return ensureSlugUnique(cleaned, id);
            }
        }
        String generated = generatePinyinSlug(name);
        return ensureSlugUnique(generated, id);
    }

    private String ensureSlugUnique(String base, Long excludeId) {
        if (excludeId != null) {
            if (!categoryRepository.existsBySlugAndIdNot(base, excludeId)) {
                return base;
            }
        } else {
            if (!categoryRepository.existsBySlug(base)) {
                return base;
            }
        }
        int suffix = 1;
        String candidate;
        do {
            candidate = base + "-" + suffix;
            suffix++;
        } while (excludeId != null
                ? categoryRepository.existsBySlugAndIdNot(candidate, excludeId)
                : categoryRepository.existsBySlug(candidate));
        return candidate;
    }

    private String generatePinyinSlug(String name) {
        String simplified = name.trim()
                .toLowerCase()
                .replaceAll("[\\s]+", "-")
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\-]", "");
        if (simplified.isEmpty()) {
            return "category";
        }
        if (simplified.matches(".*[\\u4e00-\\u9fa5].*")) {
            StringBuilder sb = new StringBuilder();
            for (char c : simplified.toCharArray()) {
                if (c >= 0x4e00 && c <= 0x9fa5) {
                    sb.append("c");
                } else {
                    sb.append(c);
                }
            }
            simplified = sb.toString();
        }
        simplified = simplified.replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (simplified.isEmpty()) {
            return "category";
        }
        return simplified;
    }

    private void collectEnabledDescendantIds(CategoryEntity parent, Set<Long> ids) {
        List<CategoryEntity> children = categoryRepository.findByParentIdAndEnabledTrueOrderBySortOrderAscNameAsc(parent.getId());
        for (CategoryEntity child : children) {
            ids.add(child.getId());
            collectEnabledDescendantIds(child, ids);
        }
    }

    private Map<Long, Long> buildPostCountMap() {
        List<Object[]> counts = postRepository.countByCategoryIdGrouped();
        Map<Long, Long> directCount = new HashMap<>();
        for (Object[] row : counts) {
            Long catId = (Long) row[0];
            Long cnt = (Long) row[1];
            directCount.put(catId, cnt);
        }

        Map<Long, Long> result = new HashMap<>();
        List<CategoryEntity> allCats = categoryRepository.findAllOrderBySortOrder();
        for (CategoryEntity cat : allCats) {
            long total = computeRecursivePostCount(cat.getId(), directCount);
            result.put(cat.getId(), total);
        }
        return result;
    }

    private long computeRecursivePostCount(Long catId, Map<Long, Long> directCount) {
        long total = directCount.getOrDefault(catId, 0L);
        List<Long> childIds = categoryRepository.findIdsByParentId(catId);
        for (Long childId : childIds) {
            total += computeRecursivePostCount(childId, directCount);
        }
        return total;
    }

    private CategoryEntity findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("分类不存在"));
    }

    private CategoryDTO toDTO(CategoryEntity entity, long postCount) {
        return new CategoryDTO(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getParent() != null ? entity.getParent().getName() : null,
                entity.isEnabled(),
                entity.getSortOrder(),
                postCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                new ArrayList<>()
        );
    }
}
