package com.nixspace.domain.service;

import com.nixspace.api.dto.request.ChannelRequests.*;
import com.nixspace.api.dto.response.Responses.*;

import java.util.List;

public interface ChannelService {
    ChannelResponse createChannel(Long userId, Long workspaceId, CreateChannelRequest request);
    ChannelResponse getChannel(Long userId, Long workspaceId, Long channelId);
    List<ChannelResponse> getWorkspaceChannels(Long userId, Long workspaceId);
    List<ChannelResponse> getMyChannels(Long userId, Long workspaceId);
    ChannelResponse updateChannel(Long userId, Long workspaceId, Long channelId, UpdateChannelRequest request);
    void archiveChannel(Long userId, Long workspaceId, Long channelId);
    void deleteChannel(Long userId, Long workspaceId, Long channelId);

    ChannelResponse createDirectMessage(Long userId, Long workspaceId, CreateDmRequest request);

    void joinChannel(Long userId, Long workspaceId, Long channelId);
    void leaveChannel(Long userId, Long workspaceId, Long channelId);
    void addMember(Long userId, Long workspaceId, Long channelId, Long targetUserId);
    void removeMember(Long userId, Long workspaceId, Long channelId, Long targetUserId);
    List<ChannelMemberResponse> getChannelMembers(Long userId, Long workspaceId, Long channelId);

    void updateNotificationPreference(Long userId, Long channelId, UpdateNotificationPrefRequest request);
}
