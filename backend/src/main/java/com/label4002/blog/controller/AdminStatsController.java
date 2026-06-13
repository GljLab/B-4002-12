package com.label4002.blog.controller;

import com.label4002.blog.dto.AdminStatsDTO;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.UserRole;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import com.label4002.blog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final PostService postService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public AdminStatsController(PostService postService,
                                PostRepository postRepository,
                                UserRepository userRepository) {
        this.postService = postService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public AdminStatsDTO getGlobalStats() {
        long totalPosts = postRepository.count();
        long pendingPosts = postService.countPending();
        long totalAuthors = userRepository.countByRole(UserRole.AUTHOR);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthlyNewPosts = postRepository.countByStatusAndCreatedAtBetween(PostStatus.PUBLISHED, monthStart, LocalDateTime.now());

        long totalViews = postRepository.sumPublishedViewCount();

        long scheduledPosts = postService.countScheduled();
        long pendingRevisions = postService.countPendingRevisions();

        List<Object[]> rankingData = postRepository.findAuthorRankingByPostCount();
        List<AdminStatsDTO.AuthorRankItem> authorRanking = rankingData.stream()
                .limit(10)
                .map(row -> new AdminStatsDTO.AuthorRankItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()
                ))
                .toList();

        return new AdminStatsDTO(totalPosts, pendingPosts, totalAuthors, monthlyNewPosts, totalViews, scheduledPosts, pendingRevisions, authorRanking);
    }

    @GetMapping("/ranking")
    public List<AdminStatsDTO.AuthorRankItem> getAuthorRanking(
            @RequestParam(defaultValue = "posts") String sortBy) {
        List<Object[]> rankingData;
        if ("views".equalsIgnoreCase(sortBy)) {
            rankingData = postRepository.findAuthorRankingByViews();
        } else {
            rankingData = postRepository.findAuthorRankingByPostCount();
        }

        return rankingData.stream()
                .limit(20)
                .map(row -> new AdminStatsDTO.AuthorRankItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()
                ))
                .toList();
    }
}
