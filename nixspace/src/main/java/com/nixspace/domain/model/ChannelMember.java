package com.nixspace.domain.model;

import com.nixspace.common.enums.ChannelRole;
import com.nixspace.common.enums.NotificationPreference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "channel_members")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChannelMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChannelRole role = ChannelRole.MEMBER;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_pref", nullable = false, length = 20)
    @Builder.Default
    private NotificationPreference notificationPreference = NotificationPreference.ALL;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private Instant joinedAt = Instant.now();
}
