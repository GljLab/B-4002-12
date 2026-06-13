package com.label4002.blog.controller;

import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.NotificationDTO;
import com.label4002.blog.dto.UnreadNotificationCountDTO;
import com.label4002.blog.security.AppUserPrincipal;
import com.label4002.blog.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Map<String, Object> getNotifications(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) Boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDTO> pageResult = notificationService.getNotifications(
                principal.getId(), onlyUnread, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", pageResult.getTotalPages());
        return result;
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountDTO getUnreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return notificationService.getUnreadCount(principal.getId());
    }

    @PostMapping("/{id}/read")
    public MessageResponse markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        notificationService.markAsRead(principal.getId(), id);
        return new MessageResponse("已标记为已读");
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllAsRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        int count = notificationService.markAllAsRead(principal.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("message", "已全部标记为已读");
        result.put("markedCount", count);
        return result;
    }
}
