package com.nixspace.domain.model;

import com.nixspace.domain.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "channels")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class Channel extends BaseEntity {

    @Id
    private String channelId;

    private String workspaceId;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type;

    private String topic;

    private String description;

    private String createdBy;

    private boolean archived;

    private boolean defaultChannel;

    @ElementCollection
    @CollectionTable(name = "channel_members", joinColumns = @JoinColumn(name = "channelId"))
    @Column(name = "userId")
    private List<String> workspaceMemberId;

    @ElementCollection
    @CollectionTable(name = "messages", joinColumns = @JoinColumn(name = "channelId"))
    @Column(name = "messageId")
    private List<String> messageId;
}