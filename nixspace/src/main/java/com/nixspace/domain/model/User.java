package com.nixspace.domain.model;

import com.nixspace.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {
    @Id
    private String userId;

    private String email;
    private String displayName;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @ElementCollection
    @CollectionTable(name = "workspace_members", joinColumns = @JoinColumn(name = "userId"))
    @Column(name = "workspaceId")
    private List<String> workspaceMemberships;
}