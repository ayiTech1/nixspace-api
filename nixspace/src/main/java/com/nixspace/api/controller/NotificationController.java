package com.nixspace.api.controller;

import com.nixspace.api.service.NotificationService;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("notification")
@Slf4j
public class NotificationController {

    @Autowired
    NotificationService notificationService;


    @GetMapping("list/{userId}")
    public OperationResponse listNotifications(@PathVariable String userId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        OperationResponse listNotificationsResponse = notificationService.listNotifications(userId, page, size);
        log.info("Response for listNotifications {} {} {} {}", userId, page, size, listNotificationsResponse);
        return listNotificationsResponse;
    }

    @GetMapping("unread-count/{userId}")
    public OperationResponse getUnreadNotificationCount(@PathVariable String userId) {
        OperationResponse getUnreadNotificationCountResponse = notificationService.getUnreadNotificationCount(userId);
        log.info("Response for getUnreadNotificationCount {} {}", userId, getUnreadNotificationCountResponse);
        return getUnreadNotificationCountResponse;
    }

    @PostMapping("mark-read/{notificationId}/{userId}")
    public OperationResponse markNotificationAsRead(@PathVariable String notificationId, @PathVariable String userId) {
        OperationResponse markNotificationAsReadResponse = notificationService.markNotificationAsRead(notificationId, userId);
        log.info("Response for markNotificationAsRead {} {} {}", notificationId, userId, markNotificationAsReadResponse);
        return markNotificationAsReadResponse;
    }

    @PostMapping("mark-all-read/{userId}")
    public OperationResponse markAllNotificationsAsRead(@PathVariable String userId) {
        OperationResponse markAllNotificationsAsReadResponse = notificationService.markAllNotificationsAsRead(userId);
        log.info("Response for markAllNotificationsAsRead {} {}", userId, markAllNotificationsAsReadResponse);
        return markAllNotificationsAsReadResponse;
    }



}