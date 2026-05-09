package com.nixspace.api.repository;

import com.nixspace.domain.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {

    Workspace findByWorkspaceId(String workspaceId);

    Workspace findBySlug(String slug);

    List<Workspace> findByWorkspaceIdIn(List<String> workspaceIds);
}
