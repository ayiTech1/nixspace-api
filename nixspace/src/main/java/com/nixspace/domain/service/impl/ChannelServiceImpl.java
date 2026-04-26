package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.ChannelRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.ChannelMapper;
import com.nixspace.common.enums.ChannelRole;
import com.nixspace.common.enums.ChannelType;
import com.nixspace.common.enums.WorkspaceRole;
import com.nixspace.domain.model.*;
import com.nixspace.domain.repository.*;
import com.nixspace.domain.service.ChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ChannelMapper channelMapper;

    @Override
    @Transactional
    public ChannelResponse createChannel(Long userId, Long workspaceId, CreateChannelRequest request) {
        requireWorkspaceMembership(userId, workspaceId);

        ChannelType type = request.type() != null ? request.type() : ChannelType.PUBLIC;

        if (type == ChannelType.DM || type == ChannelType.GROUP_DM) {
            throw new BadRequestException("Use the DM endpoint to create direct messages");
        }

        if (channelRepository.existsByWorkspaceIdAndName(workspaceId, request.name())) {
            throw new DuplicateResourceException("Channel name already exists in this workspace: #" + request.name());
        }

        User creator = userRepository.getReferenceById(userId);
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);

        Channel channel = Channel.builder()
                .workspace(workspace)
                .name(request.name().toLowerCase())
                .type(type)
                .topic(request.topic())
                .description(request.description())
                .createdBy(creator)
                .build();

        channelRepository.save(channel);

        // Auto-add creator as channel admin
        addMemberInternal(channel, creator, ChannelRole.ADMIN);

        // Add any extra members specified
        if (request.memberIds() != null) {
            List<User> members = userRepository.findActiveByIds(request.memberIds());
            members.stream()
                    .filter(u -> !u.getId().equals(userId))
                    .forEach(u -> addMemberInternal(channel, u, ChannelRole.MEMBER));
        }

        return channelMapper.toResponse(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getChannel(Long userId, Long workspaceId, Long channelId) {
        Channel channel = findChannelInWorkspace(channelId, workspaceId);
        if (channel.getType() == ChannelType.PRIVATE) {
            requireChannelMembership(userId, channelId);
        } else {
            requireWorkspaceMembership(userId, workspaceId);
        }
        return channelMapper.toResponse(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getWorkspaceChannels(Long userId, Long workspaceId) {
        requireWorkspaceMembership(userId, workspaceId);
        // Returns only PUBLIC channels visible to all workspace members
        return channelRepository.findByWorkspaceIdAndType(workspaceId, ChannelType.PUBLIC)
                .stream()
                .map(channelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getMyChannels(Long userId, Long workspaceId) {
        requireWorkspaceMembership(userId, workspaceId);
        return channelRepository.findAllByWorkspaceIdAndUserId(workspaceId, userId)
                .stream()
                .map(channelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChannelResponse updateChannel(Long userId, Long workspaceId, Long channelId, UpdateChannelRequest request) {
        Channel channel = findChannelInWorkspace(channelId, workspaceId);
        requireChannelAdminOrWorkspaceAdmin(userId, workspaceId, channelId);

        if (request.name() != null) {
            if (channelRepository.existsByWorkspaceIdAndName(workspaceId, request.name())) {
                throw new DuplicateResourceException("Channel name already exists: #" + request.name());
            }
            channel.setName(request.name().toLowerCase());
        }
        if (request.topic() != null)       channel.setTopic(request.topic());
        if (request.description() != null) channel.setDescription(request.description());

        return channelMapper.toResponse(channelRepository.save(channel));
    }

    @Override
    @Transactional
    public void archiveChannel(Long userId, Long workspaceId, Long channelId) {
        Channel channel = findChannelInWorkspace(channelId, workspaceId);
        requireChannelAdminOrWorkspaceAdmin(userId, workspaceId, channelId);

        if (channel.isDefaultChannel()) {
            throw new BusinessRuleException("Cannot archive the default channel");
        }
        channel.setArchived(true);
        channelRepository.save(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long userId, Long workspaceId, Long channelId) {
        findChannelInWorkspace(channelId, workspaceId);
        requireWorkspaceRole(userId, workspaceId, WorkspaceRole.ADMIN, WorkspaceRole.OWNER);
        channelRepository.deleteById(channelId);
    }

    @Override
    @Transactional
    public ChannelResponse createDirectMessage(Long userId, Long workspaceId, CreateDmRequest request) {
        requireWorkspaceMembership(userId, workspaceId);

        List<Long> allParticipantIds = new java.util.ArrayList<>(request.userIds());
        if (!allParticipantIds.contains(userId)) {
            allParticipantIds.add(userId);
        }

        if (allParticipantIds.size() > 9) {
            throw new BadRequestException("Group DMs support a maximum of 8 participants + you");
        }

        ChannelType dmType = allParticipantIds.size() == 2 ? ChannelType.DM : ChannelType.GROUP_DM;

        User creator = userRepository.getReferenceById(userId);
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);

        Channel channel = Channel.builder()
                .workspace(workspace)
                .name("dm-" + System.currentTimeMillis())  // internal name
                .type(dmType)
                .createdBy(creator)
                .build();

        channelRepository.save(channel);

        List<User> participants = userRepository.findActiveByIds(allParticipantIds);
        participants.forEach(u -> addMemberInternal(channel, u, ChannelRole.MEMBER));

        return channelMapper.toResponse(channel);
    }

    @Override
    @Transactional
    public void joinChannel(Long userId, Long workspaceId, Long channelId) {
        Channel channel = findChannelInWorkspace(channelId, workspaceId);
        requireWorkspaceMembership(userId, workspaceId);

        if (channel.getType() == ChannelType.PRIVATE) {
            throw new AccessDeniedException("Cannot join private channel without an invitation");
        }
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new DuplicateResourceException("You are already a member of this channel");
        }

        User user = userRepository.getReferenceById(userId);
        addMemberInternal(channel, user, ChannelRole.MEMBER);
    }

    @Override
    @Transactional
    public void leaveChannel(Long userId, Long workspaceId, Long channelId) {
        Channel channel = findChannelInWorkspace(channelId, workspaceId);

        if (channel.isDefaultChannel()) {
            throw new BusinessRuleException("Cannot leave the default channel");
        }

        channelMemberRepository.deleteByChannelIdAndUserId(channelId, userId);
    }

    @Override
    @Transactional
    public void addMember(Long userId, Long workspaceId, Long channelId, Long targetUserId) {
        requireChannelAdminOrWorkspaceAdmin(userId, workspaceId, channelId);
        requireWorkspaceMembership(targetUserId, workspaceId);

        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, targetUserId)) {
            throw new DuplicateResourceException("User is already a member of this channel");
        }

        Channel channel = channelRepository.getReferenceById(channelId);
        User target = userRepository.getReferenceById(targetUserId);
        addMemberInternal(channel, target, ChannelRole.MEMBER);
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long workspaceId, Long channelId, Long targetUserId) {
        requireChannelAdminOrWorkspaceAdmin(userId, workspaceId, channelId);
        channelMemberRepository.deleteByChannelIdAndUserId(channelId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getChannelMembers(Long userId, Long workspaceId, Long channelId) {
        requireChannelMembership(userId, channelId);
        return channelMemberRepository.findAllByChannelId(channelId)
                .stream()
                .map(channelMapper::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateNotificationPreference(Long userId, Long channelId, UpdateNotificationPrefRequest request) {
        ChannelMember member = channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel membership not found"));
        member.setNotificationPreference(request.preference());
        channelMemberRepository.save(member);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private Channel findChannelInWorkspace(Long channelId, Long workspaceId) {
        return channelRepository.findByIdAndWorkspaceId(channelId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found in workspace"));
    }

    private void requireWorkspaceMembership(Long userId, Long workspaceId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("You are not a member of this workspace");
        }
    }

    private void requireChannelMembership(Long userId, Long channelId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AccessDeniedException("You are not a member of this channel");
        }
    }

    private void requireWorkspaceRole(Long userId, Long workspaceId, WorkspaceRole... roles) {
        WorkspaceRole actual = workspaceMemberRepository
                .findRoleByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a workspace member"));
        for (WorkspaceRole r : roles) if (actual == r) return;
        throw new AccessDeniedException("Insufficient workspace permissions");
    }

    private void requireChannelAdminOrWorkspaceAdmin(Long userId, Long workspaceId, Long channelId) {
        WorkspaceRole workspaceRole = workspaceMemberRepository
                .findRoleByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a workspace member"));

        if (workspaceRole == WorkspaceRole.OWNER || workspaceRole == WorkspaceRole.ADMIN) {
            return;
        }

        ChannelMember cm = channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a channel member"));

        if (cm.getRole() != ChannelRole.ADMIN) {
            throw new AccessDeniedException("Insufficient channel permissions");
        }
    }

    private void addMemberInternal(Channel channel, User user, ChannelRole role) {
        ChannelMember member = ChannelMember.builder()
                .channel(channel)
                .user(user)
                .role(role)
                .build();
        channelMemberRepository.save(member);
    }
}
