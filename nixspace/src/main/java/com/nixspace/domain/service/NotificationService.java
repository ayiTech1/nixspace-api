package com.nixspace.domain.service;

import com.nixspace.api.dto.response.Responses.*;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PagedResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable);
    long getUnreadCount(Long userId);
    void markRead(Long userId, Long notificationId);
    void markAllRead(Long userId);
}
