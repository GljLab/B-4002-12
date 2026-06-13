package com.label4002.blog.service;

import com.label4002.blog.dto.KeywordCloudDTO;
import com.label4002.blog.dto.KeywordDTO;
import com.label4002.blog.entity.KeywordEntity;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.KeywordRepository;
import com.label4002.blog.repository.PostKeywordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordService {

    private static final int ARCHIVE_THRESHOLD_DAYS = 90;

    private final KeywordRepository keywordRepository;
    private final PostKeywordRepository postKeywordRepository;

    public KeywordService(KeywordRepository keywordRepository, PostKeywordRepository postKeywordRepository) {
        this.keywordRepository = keywordRepository;
        this.postKeywordRepository = postKeywordRepository;
    }

    @Transactional(readOnly = true)
    public List<KeywordDTO> listAll(String sortBy) {
        List<KeywordEntity> keywords;
        if ("time".equalsIgnoreCase(sortBy)) {
            keywords = keywordRepository.findAllByOrderByLastUsedAtDescNameAsc();
        } else {
            keywords = keywordRepository.findAllByOrderByUsageCountDescNameAsc();
        }
        return keywords.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<KeywordDTO> listActive() {
        return keywordRepository.findByArchivedFalseOrderByUsageCountDescNameAsc()
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<KeywordDTO> search(String name) {
        if (name == null || name.isBlank()) {
            return listActive();
        }
        return keywordRepository.searchActiveByName(name.trim())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<KeywordCloudDTO> getCloud() {
        List<KeywordEntity> active = keywordRepository.findActiveOrderByUsageCountDesc();
        List<KeywordCloudDTO> items = active.stream()
                .map(k -> new KeywordCloudDTO(k.getId(), k.getName(), k.getUsageCount(), 0.0, k.getLastUsedAt()))
                .toList();
        return KeywordCloudDTO.computeHeat(items);
    }

    @Transactional
    public List<KeywordEntity> resolveOrCreate(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<KeywordEntity> result = new ArrayList<>();
        for (String name : names) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            KeywordEntity keyword = keywordRepository.findByName(trimmed).orElse(null);
            if (keyword == null) {
                keyword = new KeywordEntity();
                keyword.setName(trimmed);
                keyword.setUsageCount(0);
                keyword.setLastUsedAt(null);
                keyword.setArchived(false);
                keyword = keywordRepository.save(keyword);
            } else {
                if (keyword.isArchived()) {
                    keyword.setArchived(false);
                }
            }
            result.add(keyword);
        }
        return result;
    }

    @Transactional
    public void addKeywordCounts(List<Long> keywordIds) {
        LocalDateTime now = LocalDateTime.now();
        for (Long kwId : keywordIds) {
            keywordRepository.findById(kwId).ifPresent(k -> {
                k.setUsageCount(k.getUsageCount() + 1);
                k.setLastUsedAt(now);
                if (k.isArchived()) {
                    k.setArchived(false);
                }
                keywordRepository.save(k);
            });
        }
    }

    @Transactional
    public void syncKeywordCounts(Long postId, List<Long> newKeywordIds) {
        List<Long> existingIds = postKeywordRepository.findKeywordIdsByPostId(postId);

        List<Long> toRemove = existingIds.stream()
                .filter(id -> !newKeywordIds.contains(id))
                .toList();
        List<Long> toAdd = newKeywordIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        for (Long removeId : toRemove) {
            postKeywordRepository.deleteByPostIdAndKeywordId(postId, removeId);
            keywordRepository.findById(removeId).ifPresent(k -> {
                k.setUsageCount(Math.max(0, k.getUsageCount() - 1));
                if (k.getUsageCount() == 0) {
                    k.setLastUsedAt(LocalDateTime.now());
                }
                keywordRepository.save(k);
            });
        }

        for (Long addId : toAdd) {
            postKeywordRepository.insertPostKeyword(postId, addId);
            keywordRepository.findById(addId).ifPresent(k -> {
                k.setUsageCount(k.getUsageCount() + 1);
                k.setLastUsedAt(LocalDateTime.now());
                if (k.isArchived()) {
                    k.setArchived(false);
                }
                keywordRepository.save(k);
            });
        }
    }

    @Transactional
    public void decrementKeywordCountsForPost(Long postId) {
        List<Long> keywordIds = postKeywordRepository.findKeywordIdsByPostId(postId);
        for (Long keywordId : keywordIds) {
            keywordRepository.findById(keywordId).ifPresent(k -> {
                k.setUsageCount(Math.max(0, k.getUsageCount() - 1));
                if (k.getUsageCount() == 0) {
                    k.setLastUsedAt(LocalDateTime.now());
                }
                keywordRepository.save(k);
            });
        }
        postKeywordRepository.deleteByPostId(postId);
    }

    @Transactional
    public int archiveStaleKeywords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(ARCHIVE_THRESHOLD_DAYS);
        List<KeywordEntity> stale = keywordRepository.findStaleArchivedBefore(threshold);
        for (KeywordEntity k : stale) {
            k.setArchived(true);
            keywordRepository.save(k);
        }
        return stale.size();
    }

    @Transactional
    public void toggleArchive(Long id) {
        KeywordEntity keyword = keywordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("关键词不存在"));
        keyword.setArchived(!keyword.isArchived());
        keywordRepository.save(keyword);
    }

    private KeywordDTO toDTO(KeywordEntity entity) {
        return new KeywordDTO(
                entity.getId(),
                entity.getName(),
                entity.getUsageCount(),
                entity.getLastUsedAt(),
                entity.isArchived(),
                entity.getCreatedAt()
        );
    }
}
