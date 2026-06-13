package com.label4002.blog.service;

import com.label4002.blog.dto.NotificationDTO;
import com.label4002.blog.dto.UnreadNotificationCountDTO;
import com.label4002.blog.entity.NotificationEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.NotificationRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final int DELETE_READ_NOTIFICATIONS_AFTER_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               PostRepository postRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public void createReplyNotification(Long toUserId, Long fromUserId, Long commentId, Long postId, String replyContent) {
        if (toUserId.equals(fromUserId)) {
            return;
        }
        UserEntity fromUser = userRepository.findById(fromUserId).orElse(null);
        PostEntity post = postRepository.findById(postId).orElse(null);
        String fromName = fromUser != null ? fromUser.getDisplayName() : "用户";
        String postTitle = post != null ? post.getTitle() : "文章";

        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(toUserId);
        notif.setType(NotificationEntity.NotificationType.REPLY);
        notif.setRelatedCommentId(commentId);
        notif.setRelatedPostId(postId);
        notif.setRelatedUserId(fromUserId);
        notif.setTitle(fromName + " 回复了你的讨论");
        notif.setContent(truncate(replyContent, 200));
        notificationRepository.save(notif);
    }

    @Transactional
    public void createMentionNotifications(List<Long> mentionedUserIds, Long fromUserId, Long commentId, Long postId, String content) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return;
        }
        UserEntity fromUser = userRepository.findById(fromUserId).orElse(null);
        PostEntity post = postRepository.findById(postId).orElse(null);
        String fromName = fromUser != null ? fromUser.getDisplayName() : "用户";

        Set<Long> uniqueIds = mentionedUserIds.stream()
                .filter(id -> !id.equals(fromUserId))
                .collect(Collectors.toSet());

        for (Long userId : uniqueIds) {
            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(userId);
            notif.setType(NotificationEntity.NotificationType.MENTION);
            notif.setRelatedCommentId(commentId);
            notif.setRelatedPostId(postId);
            notif.setRelatedUserId(fromUserId);
            notif.setTitle(fromName + " 在讨论中提到了你");
            notif.setContent(truncate(content, 200));
            notificationRepository.save(notif);
        }
    }

    @Transactional
    public void createUpvoteNotification(Long toUserId, Long fromUserId, Long commentId, Long postId) {
        if (toUserId.equals(fromUserId)) {
            return;
        }
        UserEntity fromUser = userRepository.findById(fromUserId).orElse(null);
        String fromName = fromUser != null ? fromUser.getDisplayName() : "用户";

        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(toUserId);
        notif.setType(NotificationEntity.NotificationType.UPVOTE);
        notif.setRelatedCommentId(commentId);
        notif.setRelatedPostId(postId);
        notif.setRelatedUserId(fromUserId);
        notif.setTitle(fromName + " 赞了你的讨论");
        notif.setContent("你的讨论获得了支持");
        notificationRepository.save(notif);
    }

    @Transactional
    public void createReportResultNotification(Long toUserId, Long commentId, String result, String resolution) {
        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(toUserId);
        notif.setType(NotificationEntity.NotificationType.REPORT_RESULT);
        notif.setRelatedCommentId(commentId);
        notif.setTitle("你的举报处理结果");
        notif.setContent("处理结果: " + result + (resolution != null ? "\n说明: " + resolution : ""));
        notificationRepository.save(notif);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Long userId, Boolean onlyUnread, int page, int size) {
        Page<NotificationEntity> entities;
        if (onlyUnread != null && onlyUnread) {
            entities = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, PageRequest.of(page, size));
        } else {
            entities = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        }

        Map<Long, UserEntity> userCache = new HashMap<>();
        Map<Long, PostEntity> postCache = new HashMap<>();

        return entities.map(e -> toDTO(e, userCache, postCache));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountDTO getUnreadCount(Long userId) {
        List<NotificationEntity> unread = notificationRepository.findByUserIdAndIsRead(userId, false);
        long replyCount = 0, mentionCount = 0, upvoteCount = 0, reportResultCount = 0;

        for (NotificationEntity n : unread) {
            switch (n.getType()) {
                case REPLY -> replyCount++;
                case MENTION -> mentionCount++;
                case UPVOTE -> upvoteCount++;
                case REPORT_RESULT -> reportResultCount++;
            }
        }

        return new UnreadNotificationCountDTO(
                unread.size(),
                replyCount,
                mentionCount,
                upvoteCount,
                reportResultCount
        );
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(userId, notificationId, LocalDateTime.now());
        if (updated == 0) {
            throw new NotFoundException("通知不存在");
        }
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DELETE_READ_NOTIFICATIONS_AFTER_DAYS);
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            notificationRepository.deleteOldReadNotifications(user.getId(), cutoff);
        }
    }

    private NotificationDTO toDTO(NotificationEntity entity, Map<Long, UserEntity> userCache, Map<Long, PostEntity> postCache) {
        UserEntity relatedUser = null;
        if (entity.getRelatedUserId() != null) {
            relatedUser = userCache.computeIfAbsent(entity.getRelatedUserId(),
                    id -> userRepository.findById(id).orElse(null));
        }
        PostEntity relatedPost = null;
        if (entity.getRelatedPostId() != null) {
            relatedPost = postCache.computeIfAbsent(entity.getRelatedPostId(),
                    id -> postRepository.findById(id).orElse(null));
        }

        return new NotificationDTO(
                entity.getId(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getRelatedCommentId(),
                entity.getRelatedPostId(),
                relatedPost != null ? relatedPost.getTitle() : null,
                entity.getRelatedUserId(),
                relatedUser != null ? relatedUser.getUsername() : null,
                relatedUser != null ? relatedUser.getDisplayName() : null,
                relatedUser != null ? relatedUser.getAvatarUrl() : null,
                entity.getTitle(),
                entity.getContent(),
                entity.isRead(),
                entity.getReadAt() != null ? entity.getReadAt().toString() : null,
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    private String truncate(String content, int maxLen) {
        if (content == null) return "";
        return content.length() > maxLen ? content.substring(0, maxLen) + "..." : content;
    }
}
