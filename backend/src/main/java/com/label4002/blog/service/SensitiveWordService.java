package com.label4002.blog.service;

import com.label4002.blog.dto.CreateSensitiveWordRequest;
import com.label4002.blog.dto.SensitiveWordDTO;
import com.label4002.blog.dto.UpdateSensitiveWordRequest;
import com.label4002.blog.entity.SensitiveWordEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.SensitiveWordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SensitiveWordService {

    private final SensitiveWordRepository sensitiveWordRepository;

    private volatile List<CompiledWord> compiledWords = new ArrayList<>();

    public SensitiveWordService(SensitiveWordRepository sensitiveWordRepository) {
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        List<SensitiveWordEntity> active = sensitiveWordRepository.findByActiveTrue();
        List<CompiledWord> compiled = active.stream()
                .sorted(Comparator.comparingInt(w -> -w.getWord().length()))
                .map(w -> new CompiledWord(
                        w.getWord(),
                        Pattern.compile(Pattern.quote(w.getWord()), Pattern.CASE_INSENSITIVE),
                        w.getSeverity(),
                        w.getReplacement() != null ? w.getReplacement() : "***",
                        w.getCategory() != null ? w.getCategory().name() : "GENERAL"
                ))
                .collect(Collectors.toList());
        this.compiledWords = compiled;
    }

    public FilterResult filter(String content) {
        if (content == null || content.isBlank()) {
            return new FilterResult(content, false, 1, List.of());
        }

        String result = content;
        int maxSeverity = 0;
        List<String> matchedWords = new ArrayList<>();
        boolean modified = false;

        for (CompiledWord cw : compiledWords) {
            Matcher m = cw.pattern().matcher(result);
            StringBuffer sb = new StringBuffer();
            boolean found = false;
            while (m.find()) {
                found = true;
                m.appendReplacement(sb, Matcher.quoteReplacement(cw.replacement()));
                if (!matchedWords.contains(cw.word())) {
                    matchedWords.add(cw.word());
                }
            }
            m.appendTail(sb);
            if (found) {
                result = sb.toString();
                modified = true;
                if (cw.severity() > maxSeverity) {
                    maxSeverity = cw.severity();
                }
            }
        }

        return new FilterResult(result, modified, Math.max(maxSeverity, 1), matchedWords);
    }

    @Transactional(readOnly = true)
    public Page<SensitiveWordDTO> getAllWords(int page, int size) {
        return sensitiveWordRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SensitiveWordDTO> getByCategory(String category, int page, int size) {
        try {
            SensitiveWordEntity.WordCategory cat = SensitiveWordEntity.WordCategory.valueOf(category.toUpperCase());
            return sensitiveWordRepository.findByCategory(cat, PageRequest.of(page, size))
                    .map(this::toDTO);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的分类: " + category);
        }
    }

    @Transactional(readOnly = true)
    public SensitiveWordDTO getById(Long id) {
        SensitiveWordEntity entity = sensitiveWordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("敏感词不存在"));
        return toDTO(entity);
    }

    @Transactional
    public SensitiveWordDTO create(CreateSensitiveWordRequest request) {
        if (sensitiveWordRepository.existsByWord(request.word())) {
            throw new BadRequestException("该敏感词已存在");
        }
        SensitiveWordEntity entity = new SensitiveWordEntity();
        entity.setWord(request.word());
        try {
            entity.setCategory(SensitiveWordEntity.WordCategory.valueOf(request.category().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的分类: " + request.category());
        }
        entity.setSeverity(request.severity());
        entity.setReplacement(request.replacement());
        entity.setActive(true);
        entity = sensitiveWordRepository.save(entity);
        refreshCache();
        return toDTO(entity);
    }

    @Transactional
    public SensitiveWordDTO update(Long id, UpdateSensitiveWordRequest request) {
        SensitiveWordEntity entity = sensitiveWordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("敏感词不存在"));

        if (request.word() != null) {
            if (!request.word().equals(entity.getWord()) && sensitiveWordRepository.existsByWord(request.word())) {
                throw new BadRequestException("该敏感词已存在");
            }
            entity.setWord(request.word());
        }
        if (request.category() != null) {
            try {
                entity.setCategory(SensitiveWordEntity.WordCategory.valueOf(request.category().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("无效的分类: " + request.category());
            }
        }
        if (request.severity() != null) {
            entity.setSeverity(request.severity());
        }
        if (request.replacement() != null) {
            entity.setReplacement(request.replacement());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        entity = sensitiveWordRepository.save(entity);
        refreshCache();
        return toDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!sensitiveWordRepository.existsById(id)) {
            throw new NotFoundException("敏感词不存在");
        }
        sensitiveWordRepository.deleteById(id);
        refreshCache();
    }

    private SensitiveWordDTO toDTO(SensitiveWordEntity entity) {
        return new SensitiveWordDTO(
                entity.getId(),
                entity.getWord(),
                entity.getCategory() != null ? entity.getCategory().name() : "GENERAL",
                entity.getSeverity(),
                entity.getReplacement(),
                entity.isActive(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }

    public record FilterResult(
            String filteredContent,
            boolean modified,
            int maxSeverity,
            List<String> matchedWords
    ) {}

    private record CompiledWord(
            String word,
            Pattern pattern,
            int severity,
            String replacement,
            String category
    ) {}
}
