package com.nixspace.api.repository;

import com.nixspace.domain.model.WorkspaceMember;
import com.nixspace.domain.model.ids.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

    WorkspaceMember findByWorkspaceIdAndUserId(String workspaceId, String userId);

    List<WorkspaceMember> findByWorkspaceId(String workspaceId);

    List<WorkspaceMember> findByUserId(String userId);

    boolean existsByWorkspaceIdAndUserId(String workspaceId, String userId);

    void deleteByWorkspaceId(String workspaceId);
}