package com.label4002.blog.service;

import com.label4002.blog.dto.AddToAlbumRequest;
import com.label4002.blog.dto.BatchCollectionActionRequest;
import com.label4002.blog.dto.CollectionAlbumDTO;
import com.label4002.blog.dto.CollectionItemDTO;
import com.label4002.blog.dto.CreateAlbumRequest;
import com.label4002.blog.dto.PostFavoriteCountDTO;
import com.label4002.blog.dto.ReorderItemsRequest;
import com.label4002.blog.dto.UpdateAlbumRequest;
import com.label4002.blog.entity.CollectionAlbumEntity;
import com.label4002.blog.entity.CollectionItemEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CollectionAlbumRepository;
import com.label4002.blog.repository.CollectionItemRepository;
import com.label4002.blog.repository.FavoriteRepository;
import com.label4002.blog.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class CollectionService {

    private final CollectionAlbumRepository albumRepository;
    private final CollectionItemRepository itemRepository;
    private final PostRepository postRepository;
    private final FavoriteRepository favoriteRepository;

    public CollectionService(CollectionAlbumRepository albumRepository,
                             CollectionItemRepository itemRepository,
                             PostRepository postRepository,
                             FavoriteRepository favoriteRepository) {
        this.albumRepository = albumRepository;
        this.itemRepository = itemRepository;
        this.postRepository = postRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectionAlbumDTO> listAlbums(Long userId) {
        return albumRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(userId)
                .stream()
                .map(this::toAlbumDTO)
                .toList();
    }

    @Transactional
    public CollectionAlbumDTO createAlbum(Long userId, CreateAlbumRequest request) {
        CollectionAlbumEntity entity = new CollectionAlbumEntity();
        entity.setUserId(userId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCoverUrl(request.coverUrl());
        entity.setPublic(request.isPublic());
        entity.setCreatedAt(LocalDateTime.now());
        if (request.isPublic()) {
            entity.setShareToken(generateShareToken());
        }
        return toAlbumDTO(albumRepository.save(entity));
    }

    @Transactional
    public CollectionAlbumDTO updateAlbum(Long userId, Long albumId, UpdateAlbumRequest request) {
        CollectionAlbumEntity entity = findAlbumOrThrow(albumId);
        if (!entity.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.coverUrl() != null) entity.setCoverUrl(request.coverUrl());
        if (request.isPublic() != null) {
            entity.setPublic(request.isPublic());
            if (request.isPublic() && entity.getShareToken() == null) {
                entity.setShareToken(generateShareToken());
            } else if (!request.isPublic()) {
                entity.setShareToken(null);
            }
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return toAlbumDTO(albumRepository.save(entity));
    }

    @Transactional
    public void deleteAlbum(Long userId, Long albumId) {
        CollectionAlbumEntity entity = findAlbumOrThrow(albumId);
        if (!entity.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        albumRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<CollectionItemDTO> listAlbumItems(Long userId, Long albumId) {
        CollectionAlbumEntity album = findAlbumOrThrow(albumId);
        if (!album.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限访问此专辑");
        }
        return itemRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(albumId)
                .stream()
                .map(this::toItemDTO)
                .toList();
    }

    @Transactional
    public CollectionItemDTO addItem(Long userId, Long albumId, AddToAlbumRequest request) {
        CollectionAlbumEntity album = findAlbumOrThrow(albumId);
        if (!album.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        if (itemRepository.existsByAlbumIdAndPostId(albumId, request.postId())) {
            throw new BadRequestException("该作品已在专辑中");
        }
        postRepository.findById(request.postId())
                .orElseThrow(() -> new NotFoundException("作品不存在"));

        CollectionItemEntity item = new CollectionItemEntity();
        item.setAlbumId(albumId);
        item.setPostId(request.postId());
        item.setNote(request.note());
        item.setCreatedAt(LocalDateTime.now());
        return toItemDTO(itemRepository.save(item));
    }

    @Transactional
    public void removeItem(Long userId, Long albumId, Long postId) {
        CollectionAlbumEntity album = findAlbumOrThrow(albumId);
        if (!album.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        itemRepository.deleteByAlbumIdAndPostId(albumId, postId);
    }

    @Transactional
    public void reorderItems(Long userId, Long albumId, ReorderItemsRequest request) {
        CollectionAlbumEntity album = findAlbumOrThrow(albumId);
        if (!album.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        List<CollectionItemEntity> items = itemRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(albumId);
        for (int i = 0; i < request.itemIds().size(); i++) {
            final int sortOrder = i;
            Long targetId = request.itemIds().get(i);
            items.stream().filter(it -> it.getId().equals(targetId)).findFirst().ifPresent(it -> {
                it.setSortOrder(sortOrder);
                itemRepository.save(it);
            });
        }
    }

    @Transactional
    public void batchAction(Long userId, BatchCollectionActionRequest request) {
        String action = request.action();
        List<Long> itemIds = request.itemIds();
        if (itemIds == null || itemIds.isEmpty()) return;

        if ("remove".equals(action)) {
            for (Long itemId : itemIds) {
                itemRepository.findById(itemId).ifPresent(item -> {
                    CollectionAlbumEntity album = albumRepository.findById(item.getAlbumId())
                            .orElse(null);
                    if (album != null && album.getUserId().equals(userId)) {
                        itemRepository.delete(item);
                    }
                });
            }
        } else if ("transfer".equals(action)) {
            if (request.targetAlbumId() == null) {
                throw new BadRequestException("目标专辑不能为空");
            }
            CollectionAlbumEntity target = findAlbumOrThrow(request.targetAlbumId());
            if (!target.getUserId().equals(userId)) {
                throw new ForbiddenException("无权限操作目标专辑");
            }
            for (Long itemId : itemIds) {
                itemRepository.findById(itemId).ifPresent(item -> {
                    CollectionAlbumEntity src = albumRepository.findById(item.getAlbumId())
                            .orElse(null);
                    if (src != null && src.getUserId().equals(userId)) {
                        if (!itemRepository.existsByAlbumIdAndPostId(target.getId(), item.getPostId())) {
                            item.setAlbumId(target.getId());
                            itemRepository.save(item);
                        } else {
                            itemRepository.delete(item);
                        }
                    }
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public CollectionAlbumDTO getPublicAlbum(String shareToken) {
        CollectionAlbumEntity album = albumRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new NotFoundException("专辑不存在或未公开"));
        if (!album.isPublic()) {
            throw new NotFoundException("专辑不存在或未公开");
        }
        return toAlbumDTO(album);
    }

    @Transactional(readOnly = true)
    public List<CollectionItemDTO> getPublicAlbumItems(String shareToken) {
        CollectionAlbumEntity album = albumRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new NotFoundException("专辑不存在或未公开"));
        if (!album.isPublic()) {
            throw new NotFoundException("专辑不存在或未公开");
        }
        return itemRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(album.getId())
                .stream()
                .map(this::toItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportAlbumMarkdown(Long userId, Long albumId) {
        CollectionAlbumEntity album = findAlbumOrThrow(albumId);
        if (!album.getUserId().equals(userId)) {
            throw new ForbiddenException("无权限操作此专辑");
        }
        List<CollectionItemEntity> items = itemRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(albumId);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(album.getName()).append("\n\n");
        if (album.getDescription() != null && !album.getDescription().isBlank()) {
            sb.append(album.getDescription()).append("\n\n");
        }
        for (CollectionItemEntity item : items) {
            PostEntity post = postRepository.findById(item.getPostId()).orElse(null);
            sb.append("## ");
            if (post != null) {
                sb.append(post.getTitle());
            } else {
                sb.append("已删除的作品");
            }
            sb.append("\n\n");
            if (item.getNote() != null && !item.getNote().isBlank()) {
                sb.append("> ").append(item.getNote()).append("\n\n");
            }
            if (post != null) {
                sb.append(post.getContent()).append("\n\n");
            }
            sb.append("---\n\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<PostFavoriteCountDTO> getFavoriteRanking(int top) {
        return favoriteRepository.findPostFavoriteCounts(PageRequest.of(0, top))
                .stream()
                .map(row -> {
                    Long postId = (Long) row[0];
                    Long count = (Long) row[1];
                    String title = postRepository.findById(postId)
                            .map(PostEntity::getTitle).orElse("已删除的作品");
                    return new PostFavoriteCountDTO(postId, title, count);
                })
                .toList();
    }

    private CollectionAlbumDTO toAlbumDTO(CollectionAlbumEntity entity) {
        int itemCount = (int) itemRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(entity.getId()).size();
        return new CollectionAlbumDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCoverUrl(),
                entity.isPublic(),
                entity.getSortOrder(),
                entity.getShareToken(),
                itemCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CollectionItemDTO toItemDTO(CollectionItemEntity item) {
        PostEntity post = postRepository.findById(item.getPostId()).orElse(null);
        boolean postDeleted = post == null;
        String title = post != null ? post.getTitle() : "已删除的作品";
        String excerpt = post != null ? excerpt(post.getContent(), 100) : "该作品已被删除";
        String authorName = post != null && post.getAuthor() != null ? post.getAuthor().getDisplayName() : "";
        return new CollectionItemDTO(
                item.getId(),
                item.getAlbumId(),
                item.getPostId(),
                title,
                excerpt,
                authorName,
                item.getNote(),
                item.getSortOrder(),
                postDeleted,
                item.getCreatedAt()
        );
    }

    private String excerpt(String content, int length) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= length) return normalized;
        return normalized.substring(0, length) + "...";
    }

    private CollectionAlbumEntity findAlbumOrThrow(Long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new NotFoundException("专辑不存在"));
    }

    private String generateShareToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
