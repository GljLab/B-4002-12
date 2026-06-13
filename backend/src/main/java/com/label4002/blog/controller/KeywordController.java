package com.label4002.blog.controller;

import com.label4002.blog.dto.KeywordCloudDTO;
import com.label4002.blog.dto.KeywordDTO;
import com.label4002.blog.service.KeywordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping("/keywords/cloud")
    public List<KeywordCloudDTO> cloud() {
        return keywordService.getCloud();
    }

    @GetMapping("/keywords/search")
    public List<KeywordDTO> search(@RequestParam String name) {
        return keywordService.search(name);
    }

    @GetMapping("/admin/keywords")
    public List<KeywordDTO> adminList(@RequestParam(defaultValue = "heat") String sortBy) {
        return keywordService.listAll(sortBy);
    }

    @PostMapping("/admin/keywords/{id}/toggle-archive")
    public KeywordDTO toggleArchive(@PathVariable Long id) {
        keywordService.toggleArchive(id);
        return keywordService.listAll("heat").stream()
                .filter(k -> k.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/admin/keywords/archive-stale")
    public int archiveStale() {
        return keywordService.archiveStaleKeywords();
    }
}
