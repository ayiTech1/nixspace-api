package com.nixspace.api.repository;

import com.nixspace.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserId(String userId, Pageable pageable);

    List<Notification> findByUserIdAndReadFalse(String userId);

    Notification findByIdAndUserId(String id, String userId);

    long countByUserIdAndReadFalse(String userId);
}