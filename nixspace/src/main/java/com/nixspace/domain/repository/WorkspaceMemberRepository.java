package com.nixspace.domain.repository;

import com.nixspace.common.enums.WorkspaceRole;
import com.nixspace.domain.model.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

    @Query("""
            SELECT wm.role FROM WorkspaceMember wm
            WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId
            """)
    Optional<WorkspaceRole> findRoleByWorkspaceIdAndUserId(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );

    long countByWorkspaceId(Long workspaceId);
}
