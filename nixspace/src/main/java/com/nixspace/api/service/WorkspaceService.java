package com.nixspace.api.service;


import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.WorkspaceMemberRepository;
import com.nixspace.api.repository.WorkspaceRepository;
import com.nixspace.domain.enums.WorkspaceRole;
import com.nixspace.domain.enums.WorkspaceStatus;
import com.nixspace.domain.model.Workspace;
import com.nixspace.domain.model.WorkspaceMember;
import com.nixspace.domain.request.WorkspaceMemberRequest;
import com.nixspace.domain.request.WorkspaceRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class WorkspaceService {

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;


    public OperationResponse<Workspace> createWorkspace(WorkspaceRequest workspaceRequest) {
        log.info("Incoming createWorkspace {}", workspaceRequest);
        try {
            Workspace workspace = Workspace.builder()
                    .workspaceId(UUID.randomUUID().toString())
                    .name(workspaceRequest.getName())
                    .slug(workspaceRequest.getSlug())
                    .description(workspaceRequest.getDescription())
                    .iconUrl(workspaceRequest.getIconUrl())
                    .userId(workspaceRequest.getUserId())
                    .status(WorkspaceStatus.ACTIVE)
                    .build();

            Workspace savedWorkspace = workspaceRepository.save(workspace);

            WorkspaceMember ownerMember = WorkspaceMember.builder()
                    .workspaceId(savedWorkspace.getWorkspaceId())
                    .userId(workspaceRequest.getUserId())
                    .role(WorkspaceRole.OWNER)
                    .joinedAt(LocalDateTime.now())
                    .build();

            workspaceMemberRepository.save(ownerMember);

            if (Objects.nonNull(savedWorkspace)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedWorkspace);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
            }
        } catch (Exception e) {
            log.error("Error creating workspace: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace", e.getMessage()), null);
        }
    }


    public OperationResponse<List<Workspace>> listUserWorkspaces(String userId) {
        log.info("Incoming listUserWorkspaces userId {}", userId);
        try {
            List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(userId);

            if (Objects.isNull(memberships) || memberships.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspaces"), null);
            }

            List<String> workspaceIds = memberships.stream()
                    .map(WorkspaceMember::getWorkspaceId)
                    .toList();

            List<Workspace> workspaces = workspaceRepository.findByWorkspaceIdIn(workspaceIds);

            if (Objects.nonNull(workspaces) && !workspaces.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, workspaces);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspaces"), null);
            }
        } catch (Exception e) {
            log.error("Error listing workspaces for user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspaces", e.getMessage()), null);
        }
    }


    public OperationResponse<Workspace> getWorkspaceById(String workspaceId) {
        log.info("Incoming getWorkspaceById workspaceId {}", workspaceId);
        try {
            Workspace workspace = workspaceRepository.findByWorkspaceId(workspaceId);

            if (Objects.nonNull(workspace)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, workspace);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving workspace {}: {}", workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace", e.getMessage()), null);
        }
    }


    public OperationResponse<Workspace> getWorkspaceBySlug(String slug) {
        log.info("Incoming getWorkspaceBySlug slug {}", slug);
        try {
            Workspace workspace = workspaceRepository.findBySlug(slug);

            if (Objects.nonNull(workspace)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, workspace);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving workspace by slug {}: {}", slug, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace", e.getMessage()), null);
        }
    }


    public OperationResponse<Workspace> updateWorkspace(String workspaceId, WorkspaceRequest workspaceRequest) {
        log.info("Incoming updateWorkspace workspaceId {} request {}", workspaceId, workspaceRequest);
        try {
            Workspace workspace = workspaceRepository.findByWorkspaceId(workspaceId);

            if (Objects.nonNull(workspace)) {
                workspace.setName(workspaceRequest.getName());
                workspace.setSlug(workspaceRequest.getSlug());
                workspace.setDescription(workspaceRequest.getDescription());
                workspace.setIconUrl(workspaceRequest.getIconUrl());

                Workspace updatedWorkspace = workspaceRepository.save(workspace);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedWorkspace);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace"), null);
            }
        } catch (Exception e) {
            log.error("Error updating workspace {}: {}", workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace", e.getMessage()), null);
        }
    }


    public OperationResponse deleteWorkspace(String workspaceId) {
        log.info("Incoming deleteWorkspace workspaceId {}", workspaceId);
        try {
            Workspace workspace = workspaceRepository.findByWorkspaceId(workspaceId);

            if (Objects.nonNull(workspace)) {
                workspaceMemberRepository.deleteByWorkspaceId(workspaceId);
                workspaceRepository.delete(workspace);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "workspace", workspaceId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace"), null);
            }
        } catch (Exception e) {
            log.error("Error deleting workspace {}: {}", workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace", e.getMessage()), null);
        }
    }


    public OperationResponse<List<WorkspaceMember>> listWorkspaceMembers(String workspaceId) {
        log.info("Incoming listWorkspaceMembers workspaceId {}", workspaceId);
        try {
            List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);

            if (Objects.nonNull(members) && !members.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, members);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace members"), null);
            }
        } catch (Exception e) {
            log.error("Error listing members for workspace {}: {}", workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace members", e.getMessage()), null);
        }
    }


    public OperationResponse<WorkspaceMember> inviteMember(String workspaceId, WorkspaceMemberRequest workspaceMemberRequest) {
        log.info("Incoming inviteMember workspaceId {} request {}", workspaceId, workspaceMemberRequest);
        try {
            Workspace workspace = workspaceRepository.findByWorkspaceId(workspaceId);

            if (Objects.isNull(workspace)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace"), null);
            }

            boolean alreadyMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, workspaceMemberRequest.getUserId());
            if (alreadyMember) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ALREADY_EXISTS, null);
            }

            WorkspaceMember workspaceMember = WorkspaceMember.builder()
                    .workspaceId(workspaceId)
                    .userId(workspaceMemberRequest.getUserId())
                    .role(WorkspaceRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .build();

            WorkspaceMember savedMember = workspaceMemberRepository.save(workspaceMember);

            if (Objects.nonNull(savedMember)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedMember);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
            }
        } catch (Exception e) {
            log.error("Error inviting member to workspace {}: {}", workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace member", e.getMessage()), null);
        }
    }


    public OperationResponse<WorkspaceMember> updateMemberRole(String workspaceId, String userId, WorkspaceMemberRequest workspaceMemberRequest) {
        log.info("Incoming updateMemberRole workspaceId {} userId {} request {}", workspaceId, userId, workspaceMemberRequest);
        try {
            WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);

            if (Objects.nonNull(workspaceMember)) {
                workspaceMember.setRole(workspaceMemberRequest.getRole());
                WorkspaceMember updatedMember = workspaceMemberRepository.save(workspaceMember);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedMember);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace member"), null);
            }
        } catch (Exception e) {
            log.error("Error updating role for user {} in workspace {}: {}", userId, workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace member", e.getMessage()), null);
        }
    }


    public OperationResponse removeMember(String workspaceId, String userId) {
        log.info("Incoming removeMember workspaceId {} userId {}", workspaceId, userId);
        try {
            WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);

            if (Objects.nonNull(workspaceMember)) {
                workspaceMemberRepository.delete(workspaceMember);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "workspace member", userId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace member"), null);
            }
        } catch (Exception e) {
            log.error("Error removing member {} from workspace {}: {}", userId, workspaceId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace member", e.getMessage()), null);
        }
    }


    public OperationResponse leaveWorkspace(String workspaceId, String userId) {
        log.info("Incoming leaveWorkspace workspaceId {} userId {}", workspaceId, userId);
        try {
            WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);

            if (Objects.nonNull(workspaceMember)) {
                workspaceMemberRepository.delete(workspaceMember);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "workspace member", userId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "workspace member"), null);
            }
        } catch (Exception e) {
            log.error("Error leaving workspace {} for user {}: {}", workspaceId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "workspace member", e.getMessage()), null);
        }
    }
}