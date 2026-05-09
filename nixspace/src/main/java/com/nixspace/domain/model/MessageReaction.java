package com.nixspace.domain.model;

import com.nixspace.domain.model.ids.MessageReactionId;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
@Entity
@Table(name = "message_reactions")
@IdClass(MessageReactionId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageReaction {
    @Id
    private String messageId;
    @Id
    private String userId;
    @Id
    private String reaction;

    private LocalDateTime createdAt;
}