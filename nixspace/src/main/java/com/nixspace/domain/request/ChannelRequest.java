package com.nixspace.domain.request;


import com.nixspace.domain.enums.ChannelType;
import lombok.Data;

import java.util.List;

@Data
public class ChannelRequest {
    private String workspaceId;
    private String name;
    private ChannelType type;
    private String topic;
    private String description;
    private String createdBy;
    private boolean defaultChannel;
    private List<String> memberIds;
}