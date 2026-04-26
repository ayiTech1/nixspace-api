package com.nixspace.domain.service;

import com.nixspace.api.dto.request.WorkspaceRequests.*;
import com.nixspace.api.dto.response.Responses.*;

import java.util.List;

public interface WorkspaceService {
    WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request);
    WorkspaceResponse getWorkspace(Long userId, Long workspaceId);
    WorkspaceResponse getWorkspaceBySlug(Long userId, String slug);
    List<WorkspaceResponse> getMyWorkspaces(Long userId);
    WorkspaceResponse updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request);
    void deleteWorkspace(Long userId, Long workspaceId);
    WorkspaceMemberResponse inviteMember(Long userId, Long workspaceId, InviteMemberRequest request);
    List<WorkspaceMemberResponse> getMembers(Long userId, Long workspaceId);
    WorkspaceMemberResponse updateMemberRole(Long userId, Long workspaceId, Long targetUserId, UpdateMemberRoleRequest request);
    void removeMember(Long userId, Long workspaceId, Long targetUserId);
    void leaveWorkspace(Long userId, Long workspaceId);
}
