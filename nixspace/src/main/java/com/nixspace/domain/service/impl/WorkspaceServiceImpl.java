package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.WorkspaceRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.WorkspaceMapper;
import com.nixspace.common.enums.ChannelType;
import com.nixspace.common.enums.WorkspaceRole;
import com.nixspace.domain.model.*;
import com.nixspace.domain.repository.*;
import com.nixspace.domain.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final WorkspaceMapper workspaceMapper;

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request) {
        if (workspaceRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Workspace slug already taken: " + request.slug());
        }

        User owner = findActiveUser(userId);

        Workspace workspace = Workspace.builder()
                .name(request.name())
                .slug(request.slug().toLowerCase())
                .description(request.description())
                .owner(owner)
                .build();

        workspaceRepository.save(workspace);

        // Auto-join creator as OWNER
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .build();
        workspaceMemberRepository.save(ownerMember);

        // Create default #general channel
        Channel general = Channel.builder()
                .workspace(workspace)
                .name("general")
                .type(ChannelType.PUBLIC)
                .description("Company-wide announcements and general discussion")
                .createdBy(owner)
                .defaultChannel(true)
                .build();
        channelRepository.save(general);

        log.info("Workspace created: {} by userId={}", workspace.getSlug(), userId);
        return workspaceMapper.toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceAndVerifyMembership(userId, workspaceId);
        return workspaceMapper.toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceBySlug(Long userId, String slug) {
        Workspace workspace = workspaceRepository.findBySlug(slug)
                .filter(Workspace::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + slug));
        verifyMembership(userId, workspace.getId());
        return workspaceMapper.toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(Long userId) {
        return workspaceRepository.findAllByUserId(userId)
                .stream()
                .map(workspaceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = findWorkspaceAndVerifyMembership(userId, workspaceId);
        requireRole(userId, workspaceId, WorkspaceRole.ADMIN, WorkspaceRole.OWNER);

        if (request.name() != null) workspace.setName(request.name());
        if (request.description() != null) workspace.setDescription(request.description());

        return workspaceMapper.toResponse(workspaceRepository.save(workspace));
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long userId, Long workspaceId) {
        findWorkspaceAndVerifyMembership(userId, workspaceId);
        requireRole(userId, workspaceId, WorkspaceRole.OWNER);
        workspaceRepository.deleteById(workspaceId);
        log.info("Workspace {} deleted by userId={}", workspaceId, userId);
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse inviteMember(Long userId, Long workspaceId, InviteMemberRequest request) {
        findWorkspaceAndVerifyMembership(userId, workspaceId);
        requireRole(userId, workspaceId, WorkspaceRole.ADMIN, WorkspaceRole.OWNER);

        User invitee = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + request.email()));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitee.getId())) {
            throw new DuplicateResourceException("User is already a member of this workspace");
        }

        Workspace workspace = workspaceRepository.getReferenceById(workspaceId);
        WorkspaceRole role = request.role() != null ? request.role() : WorkspaceRole.MEMBER;

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(invitee)
                .role(role)
                .build();

        workspaceMemberRepository.save(member);

        // Auto-join default channels
        channelRepository.findAllByWorkspaceIdAndDefaultChannelTrue(workspaceId)
                .forEach(channel -> {
                    // channel joining handled by ChannelService in production
                });

        return workspaceMapper.toMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getMembers(Long userId, Long workspaceId) {
        verifyMembership(userId, workspaceId);
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(workspaceMapper::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse updateMemberRole(Long userId, Long workspaceId,
                                                     Long targetUserId, UpdateMemberRoleRequest request) {
        requireRole(userId, workspaceId, WorkspaceRole.OWNER);

        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessRuleException("Cannot change the role of the workspace owner");
        }

        member.setRole(request.role());
        return workspaceMapper.toMemberResponse(workspaceMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long workspaceId, Long targetUserId) {
        requireRole(userId, workspaceId, WorkspaceRole.ADMIN, WorkspaceRole.OWNER);

        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessRuleException("Cannot remove the workspace owner");
        }

        workspaceMemberRepository.delete(target);
    }

    @Override
    @Transactional
    public void leaveWorkspace(Long userId, Long workspaceId) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this workspace"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessRuleException("Workspace owner cannot leave. Transfer ownership first.");
        }

        workspaceMemberRepository.delete(member);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private Workspace findWorkspaceAndVerifyMembership(Long userId, Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(Workspace::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        verifyMembership(userId, workspaceId);
        return workspace;
    }

    private void verifyMembership(Long userId, Long workspaceId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("You are not a member of this workspace");
        }
    }

    private void requireRole(Long userId, Long workspaceId, WorkspaceRole... allowedRoles) {
        WorkspaceRole role = workspaceMemberRepository
                .findRoleByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this workspace"));

        for (WorkspaceRole allowed : allowedRoles) {
            if (role == allowed) return;
        }
        throw new AccessDeniedException("Insufficient permissions for this action");
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
