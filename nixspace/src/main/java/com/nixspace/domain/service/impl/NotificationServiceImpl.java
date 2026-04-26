package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.NotificationMapper;
import com.nixspace.domain.repository.NotificationRepository;
import com.nixspace.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        Page<?> page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationResponse> content = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .getContent()
                .stream()
                .map(notificationMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.markReadByIdAndUserId(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
