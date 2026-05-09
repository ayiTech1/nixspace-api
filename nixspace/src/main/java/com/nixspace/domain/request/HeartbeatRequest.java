package com.nixspace.domain.request;

import lombok.Data;

@Data
public class HeartbeatRequest {
    private String userId;
    private String workspaceId;
}
