package com.nixspace.api.service;

import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.ChannelMemberRepository;
import com.nixspace.api.repository.ChannelRepository;
import com.nixspace.domain.enums.ChannelType;
import com.nixspace.domain.enums.MemberRole;
import com.nixspace.domain.enums.NotificationPreference;
import com.nixspace.domain.model.Channel;
import com.nixspace.domain.model.ChannelMember;
import com.nixspace.domain.request.ChannelMemberRequest;
import com.nixspace.domain.request.ChannelRequest;
import com.nixspace.domain.request.NotificationPreferenceRequest;
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
public class ChannelService {

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    ChannelMemberRepository channelMemberRepository;


    public OperationResponse<Channel> createChannel(ChannelRequest channelRequest) {
        log.info("Incoming createChannel {}", channelRequest);
        try {
            Channel channel = Channel.builder()
                    .channelId(UUID.randomUUID().toString())
                    .workspaceId(channelRequest.getWorkspaceId())
                    .name(channelRequest.getName())
                    .type(channelRequest.getType())
                    .topic(channelRequest.getTopic())
                    .description(channelRequest.getDescription())
                    .createdBy(channelRequest.getCreatedBy())
                    .archived(false)
                    .defaultChannel(channelRequest.isDefaultChannel())
                    .build();

            Channel savedChannel = channelRepository.save(channel);

            ChannelMember creatorMember = ChannelMember.builder()
                    .channelId(savedChannel.getChannelId())
                    .userId(channelRequest.getCreatedBy())
                    .role(MemberRole.ADMIN)
                    .notificationPreference(NotificationPreference.ALL)
                    .joinedAt(LocalDateTime.now())
                    .build();

            channelMemberRepository.save(creatorMember);

            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedChannel);
        } catch (Exception e) {
            log.error("Error creating channel: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse<List<Channel>> listPublicChannels(String workspaceId) {
        log.info("Incoming listPublicChannels workspaceId {}", workspaceId);
        try {
            List<Channel> channels = channelRepository.findByWorkspaceIdAndTypeAndArchivedFalse(workspaceId, ChannelType.PUBLIC);

            if (Objects.nonNull(channels) && !channels.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, channels);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "public channels"), null);
            }
        } catch (Exception e) {
            log.error("Error listing public channels: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "public channels", e.getMessage()), null);
        }
    }


    public OperationResponse<List<Channel>> listJoinedChannels(String workspaceId, String userId) {
        log.info("Incoming listJoinedChannels workspaceId {} userId {}", workspaceId, userId);
        try {
            List<ChannelMember> memberships = channelMemberRepository.findByUserId(userId);

            if (Objects.isNull(memberships) || memberships.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "joined channels"), null);
            }

            List<String> channelIds = memberships.stream()
                    .map(ChannelMember::getChannelId)
                    .toList();

            List<Channel> channels = channelRepository.findByChannelIdInAndWorkspaceIdAndArchivedFalse(channelIds, workspaceId);

            if (Objects.nonNull(channels) && !channels.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, channels);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "joined channels"), null);
            }
        } catch (Exception e) {
            log.error("Error listing joined channels: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "joined channels", e.getMessage()), null);
        }
    }



    public OperationResponse<Channel> getChannelById(String channelId) {
        log.info("Incoming getChannelById channelId {}", channelId);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.nonNull(channel)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, channel);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse<Channel> updateChannel(String channelId, ChannelRequest channelRequest) {
        log.info("Incoming updateChannel channelId {} request {}", channelId, channelRequest);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.nonNull(channel)) {
                channel.setName(channelRequest.getName());
                channel.setTopic(channelRequest.getTopic());
                channel.setDescription(channelRequest.getDescription());

                Channel updatedChannel = channelRepository.save(channel);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedChannel);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }
        } catch (Exception e) {
            log.error("Error updating channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse<Channel> archiveChannel(String channelId) {
        log.info("Incoming archiveChannel channelId {}", channelId);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.nonNull(channel)) {
                channel.setArchived(true);
                Channel archivedChannel = channelRepository.save(channel);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, archivedChannel);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }
        } catch (Exception e) {
            log.error("Error archiving channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse deleteChannel(String channelId) {
        log.info("Incoming deleteChannel channelId {}", channelId);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.nonNull(channel)) {
                channelMemberRepository.deleteByChannelId(channelId);
                channelRepository.delete(channel);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "channel", channelId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }
        } catch (Exception e) {
            log.error("Error deleting channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse<ChannelMember> joinChannel(String channelId, String userId) {
        log.info("Incoming joinChannel channelId {} userId {}", channelId, userId);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.isNull(channel)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }

            if (!ChannelType.PUBLIC.equals(channel.getType())) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ACCESS_DENIED, null);
            }

            boolean alreadyMember = channelMemberRepository.existsByChannelIdAndUserId(channelId, userId);
            if (alreadyMember) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ALREADY_EXISTS, null);
            }

            ChannelMember channelMember = ChannelMember.builder()
                    .channelId(channelId)
                    .userId(userId)
                    .role(MemberRole.MEMBER)
                    .notificationPreference(NotificationPreference.ALL)
                    .joinedAt(LocalDateTime.now())
                    .build();

            ChannelMember savedMember = channelMemberRepository.save(channelMember);

            if (Objects.nonNull(savedMember)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedMember);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
            }
        } catch (Exception e) {
            log.error("Error joining channel {} for user {}: {}", channelId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel", e.getMessage()), null);
        }
    }


    public OperationResponse leaveChannel(String channelId, String userId) {
        log.info("Incoming leaveChannel channelId {} userId {}", channelId, userId);
        try {
            ChannelMember channelMember = channelMemberRepository.findByChannelIdAndUserId(channelId, userId);

            if (Objects.nonNull(channelMember)) {
                channelMemberRepository.delete(channelMember);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "channel member", userId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel member"), null);
            }
        } catch (Exception e) {
            log.error("Error leaving channel {} for user {}: {}", channelId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel member", e.getMessage()), null);
        }
    }


    public OperationResponse<List<ChannelMember>> listChannelMembers(String channelId) {
        log.info("Incoming listChannelMembers channelId {}", channelId);
        try {
            List<ChannelMember> members = channelMemberRepository.findByChannelId(channelId);

            if (Objects.nonNull(members) && !members.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, members);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel members"), null);
            }
        } catch (Exception e) {
            log.error("Error listing members for channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel members", e.getMessage()), null);
        }
    }


    public OperationResponse<ChannelMember> addMemberToChannel(String channelId, ChannelMemberRequest channelMemberRequest) {
        log.info("Incoming addMemberToChannel channelId {} request {}", channelId, channelMemberRequest);
        try {
            Channel channel = channelRepository.findByChannelId(channelId);

            if (Objects.isNull(channel)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel"), null);
            }

            boolean alreadyMember = channelMemberRepository.existsByChannelIdAndUserId(channelId, channelMemberRequest.getUserId());
            if (alreadyMember) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ALREADY_EXISTS, null);
            }

            ChannelMember channelMember = ChannelMember.builder()
                    .channelId(channelId)
                    .userId(channelMemberRequest.getUserId())
                    .role(Objects.nonNull(channelMemberRequest.getRole()) ? channelMemberRequest.getRole() : MemberRole.MEMBER)
                    .notificationPreference(NotificationPreference.ALL)
                    .joinedAt(LocalDateTime.now())
                    .build();

            ChannelMember savedMember = channelMemberRepository.save(channelMember);

            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedMember);
        } catch (Exception e) {
            log.error("Error adding member to channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel member", e.getMessage()), null);
        }
    }


    public OperationResponse removeMemberFromChannel(String channelId, String userId) {
        log.info("Incoming removeMemberFromChannel channelId {} userId {}", channelId, userId);
        try {
            ChannelMember channelMember = channelMemberRepository.findByChannelIdAndUserId(channelId, userId);

            if (Objects.nonNull(channelMember)) {
                channelMemberRepository.delete(channelMember);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "channel member", userId), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel member"), null);
            }
        } catch (Exception e) {
            log.error("Error removing member {} from channel {}: {}", userId, channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel member", e.getMessage()), null);
        }
    }


    public OperationResponse<ChannelMember> updateNotificationPreference(String channelId, String userId, NotificationPreferenceRequest preferenceRequest) {
        log.info("Incoming updateNotificationPreference channelId {} userId {} preference {}", channelId, userId, preferenceRequest);
        try {
            ChannelMember channelMember = channelMemberRepository.findByChannelIdAndUserId(channelId, userId);

            if (Objects.nonNull(channelMember)) {
                channelMember.setNotificationPreference(preferenceRequest.getNotificationPreference());
                ChannelMember updatedMember = channelMemberRepository.save(channelMember);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedMember);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel member"), null);
            }
        } catch (Exception e) {
            log.error("Error updating notification preference for user {} in channel {}: {}", userId, channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "notification preference", e.getMessage()), null);
        }
    }


    public OperationResponse<Channel> createDirectMessageChannel(ChannelRequest channelRequest) {
        log.info("Incoming createDirectMessageChannel request {}", channelRequest);
        try {
            if (Objects.isNull(channelRequest.getMemberIds()) || channelRequest.getMemberIds().isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.FAILED_TO_ADD_REASON, "memberIds are required for DM channels"), null);
            }

            ChannelType dmType = channelRequest.getMemberIds().size() > 2 ? ChannelType.GROUP_DM : ChannelType.DIRECT;

            Channel channel = Channel.builder()
                    .channelId(UUID.randomUUID().toString())
                    .workspaceId(channelRequest.getWorkspaceId())
                    .name(channelRequest.getName())
                    .type(dmType)
                    .createdBy(channelRequest.getCreatedBy())
                    .archived(false)
                    .defaultChannel(false)
                    .build();

            Channel savedChannel = channelRepository.save(channel);

            for (String memberId : channelRequest.getMemberIds()) {
                ChannelMember member = ChannelMember.builder()
                        .channelId(savedChannel.getChannelId())
                        .userId(memberId)
                        .role(memberId.equals(channelRequest.getCreatedBy()) ? MemberRole.ADMIN : MemberRole.MEMBER)
                        .notificationPreference(NotificationPreference.ALL)
                        .joinedAt(LocalDateTime.now())
                        .build();
                channelMemberRepository.save(member);
            }
            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedChannel);
        } catch (Exception e) {
            log.error("Error creating DM channel: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "DM channel", e.getMessage()), null);
        }
    }
}