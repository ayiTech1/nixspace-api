package com.nixspace.domain.model;

import com.nixspace.domain.enums.WorkspaceRole;
import com.nixspace.domain.model.ids.WorkspaceMemberId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_members")
@IdClass(WorkspaceMemberId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMember {
    @Id
    private String workspaceId;
    @Id
    private String userId;

    @Enumerated(EnumType.STRING)
    private WorkspaceRole role;

    private LocalDateTime joinedAt;
}
