package com.nixspace.api.service;

import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.NotificationRepository;
import com.nixspace.domain.model.Notification;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    NotificationRepository notificationRepository;


    public OperationResponse<Page<Notification>> listNotifications(String userId, int page, int size) {
        log.info("Incoming listNotifications userId {} page {} size {}", userId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);

            if (Objects.nonNull(notifications) && !notifications.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, notifications);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "notifications"), null);
            }
        } catch (Exception e) {
            log.error("Error listing notifications for user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "notifications", e.getMessage()), null);
        }
    }


    public OperationResponse<Long> getUnreadNotificationCount(String userId) {
        log.info("Incoming getUnreadNotificationCount userId {}", userId);
        try {
            long count = notificationRepository.countByUserIdAndReadFalse(userId);
            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, count);
        } catch (Exception e) {
            log.error("Error retrieving unread notification count for user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "notifications", e.getMessage()), null);
        }
    }


    public OperationResponse<Notification> markNotificationAsRead(String notificationId, String userId) {
        log.info("Incoming markNotificationAsRead notificationId {} userId {}", notificationId, userId);
        try {
            Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);

            if (Objects.nonNull(notification)) {
                notification.setRead(true);
                Notification updatedNotification = notificationRepository.save(notification);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedNotification);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "notification"), null);
            }
        } catch (Exception e) {
            log.error("Error marking notification {} as read for user {}: {}", notificationId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "notification", e.getMessage()), null);
        }
    }


    public OperationResponse markAllNotificationsAsRead(String userId) {
        log.info("Incoming markAllNotificationsAsRead userId {}", userId);
        try {
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);

            if (Objects.isNull(unreadNotifications) || unreadNotifications.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "unread notifications"), null);
            }

            unreadNotifications.forEach(notification -> notification.setRead(true));
            notificationRepository.saveAll(unreadNotifications);

            return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "unread notifications", userId), null);
        } catch (Exception e) {
            log.error("Error marking all notifications as read for user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "notifications", e.getMessage()), null);
        }
    }
}
