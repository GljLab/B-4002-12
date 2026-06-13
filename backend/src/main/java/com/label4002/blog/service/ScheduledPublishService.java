package com.label4002.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
public class ScheduledPublishService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPublishService.class);

    private final PostService postService;
    private final ReviewService reviewService;

    public ScheduledPublishService(PostService postService, ReviewService reviewService) {
        this.postService = postService;
        this.reviewService = reviewService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkAndPublishScheduledPosts() {
        try {
            postService.publishScheduledPosts();
        } catch (Exception e) {
            log.error("预约上线任务执行失败", e);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void checkOverdueReviews() {
        try {
            List<Long> overduePostIds = reviewService.findOverduePostIds();
            if (!overduePostIds.isEmpty()) {
                log.warn("以下文章提交审核已超过48小时未处理，请尽快审核: {}", overduePostIds);
            }
        } catch (Exception e) {
            log.error("审查超时检查任务执行失败", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        try {
            postService.publishScheduledPosts();
            log.info("启动时恢复预约上线任务完成");
        } catch (Exception e) {
            log.error("启动时恢复预约上线任务失败", e);
        }
    }
}
