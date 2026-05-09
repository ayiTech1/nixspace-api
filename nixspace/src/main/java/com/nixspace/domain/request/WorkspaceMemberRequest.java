package com.nixspace.domain.request;

import com.nixspace.domain.enums.WorkspaceRole;
import lombok.Data;

@Data
public class WorkspaceMemberRequest {
    private String userId;
    private String email;
    private WorkspaceRole role;
}
