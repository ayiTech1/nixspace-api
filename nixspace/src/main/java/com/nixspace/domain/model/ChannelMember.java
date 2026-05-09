package com.nixspace.domain.model;

import com.nixspace.domain.enums.MemberRole;
import com.nixspace.domain.enums.NotificationPreference;
import com.nixspace.domain.model.ids.ChannelMemberId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "channel_members")
@IdClass(ChannelMemberId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChannelMember {
    @Id
    private String channelId;
    @Id
    private String userId;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private String lastReadMessageId;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference;

    private LocalDateTime joinedAt;
}