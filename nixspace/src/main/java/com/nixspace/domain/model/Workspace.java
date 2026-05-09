package com.nixspace.domain.model;

import com.nixspace.domain.enums.WorkspaceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "workspaces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workspace extends BaseEntity {
    @Id
    private String workspaceId;

    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private String userId;

    @Enumerated(EnumType.STRING)
    private WorkspaceStatus status;

    @ElementCollection
    @CollectionTable(name = "workspace_members", joinColumns = @JoinColumn(name = "workspaceId"))
    @Column(name = "userId")
    private List<String> workspaceMemberIds;
}