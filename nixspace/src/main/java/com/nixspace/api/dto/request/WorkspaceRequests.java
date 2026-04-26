package com.nixspace.api.dto.request;

import com.nixspace.common.enums.ChannelType;
import com.nixspace.common.enums.NotificationPreference;
import com.nixspace.common.enums.WorkspaceRole;
import jakarta.validation.constraints.*;

import java.util.List;

public class WorkspaceRequests {

    public record CreateWorkspaceRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Size(min = 2, max = 100)
            @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug can only contain lowercase letters, numbers and hyphens")
            String slug,
            String description
    ) {}

    public record UpdateWorkspaceRequest(
            @Size(min = 2, max = 100) String name,
            String description
    ) {}

    public record InviteMemberRequest(
            @NotBlank @Email String email,
            WorkspaceRole role
    ) {}

    public record UpdateMemberRoleRequest(
            @NotNull WorkspaceRole role
    ) {}
}


