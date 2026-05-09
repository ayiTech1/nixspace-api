package com.nixspace.domain.model;

import com.nixspace.domain.enums.ContentType;
import com.nixspace.domain.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class Message extends BaseEntity {

    @Id
    private String messageId;

    private String channelId;

    private String senderId;

    private String parentMessageId;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String content;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private boolean edited;

    private boolean deleted;

    private int replyCount;
}