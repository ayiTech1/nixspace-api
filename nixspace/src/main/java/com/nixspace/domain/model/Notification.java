package com.nixspace.domain.model;

import com.nixspace.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class Notification {

    @Id
    private String id;

    private String userId;

    private String entityId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String entityType;

    private String actorId;

    @Column(name = "is_read")
    private boolean read;

    private LocalDateTime createdAt;
}