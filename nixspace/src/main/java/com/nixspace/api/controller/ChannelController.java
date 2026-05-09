package com.nixspace.api.controller;

import com.nixspace.api.service.ChannelService;
import com.nixspace.domain.request.ChannelMemberRequest;
import com.nixspace.domain.request.ChannelRequest;
import com.nixspace.domain.request.NotificationPreferenceRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("channel")
@Slf4j
public class ChannelController {

    @Autowired
    ChannelService channelService;


    @PostMapping("create")
    public OperationResponse createChannel(@RequestBody @Validated ChannelRequest channelRequest) {
        OperationResponse createChannelResponse = channelService.createChannel(channelRequest);
        log.info("Response for createChannel {} {}", channelRequest, createChannelResponse);
        return createChannelResponse;
    }

    @GetMapping("list-public/{workspaceId}")
    public OperationResponse listPublicChannels(@PathVariable String workspaceId) {
        OperationResponse listPublicChannelsResponse = channelService.listPublicChannels(workspaceId);
        log.info("Response for listPublicChannels {} {}", workspaceId, listPublicChannelsResponse);
        return listPublicChannelsResponse;
    }

    @GetMapping("list-joined/{workspaceId}/{userId}")
    public OperationResponse listJoinedChannels(@PathVariable String workspaceId, @PathVariable String userId) {
        OperationResponse listJoinedChannelsResponse = channelService.listJoinedChannels(workspaceId, userId);
        log.info("Response for listJoinedChannels {} {} {}", workspaceId, userId, listJoinedChannelsResponse);
        return listJoinedChannelsResponse;
    }

    @GetMapping("retrieve/{channelId}")
    public OperationResponse getChannelById(@PathVariable String channelId) {
        OperationResponse getChannelByIdResponse = channelService.getChannelById(channelId);
        log.info("Response for getChannelById {} {}", channelId, getChannelByIdResponse);
        return getChannelByIdResponse;
    }

    @PostMapping("update/{channelId}")
    public OperationResponse updateChannel(@PathVariable String channelId, @RequestBody @Validated ChannelRequest channelRequest) {
        OperationResponse updateChannelResponse = channelService.updateChannel(channelId, channelRequest);
        log.info("Response for updateChannel {} {} {}", channelId, channelRequest, updateChannelResponse);
        return updateChannelResponse;
    }

    @PostMapping("archive/{channelId}")
    public OperationResponse archiveChannel(@PathVariable String channelId) {
        OperationResponse archiveChannelResponse = channelService.archiveChannel(channelId);
        log.info("Response for archiveChannel {} {}", channelId, archiveChannelResponse);
        return archiveChannelResponse;
    }

    @PostMapping("delete/{channelId}")
    public OperationResponse deleteChannel(@PathVariable String channelId) {
        OperationResponse deleteChannelResponse = channelService.deleteChannel(channelId);
        log.info("Response for deleteChannel {} {}", channelId, deleteChannelResponse);
        return deleteChannelResponse;
    }

    @PostMapping("join/{channelId}/{userId}")
    public OperationResponse joinChannel(@PathVariable String channelId, @PathVariable String userId) {
        OperationResponse joinChannelResponse = channelService.joinChannel(channelId, userId);
        log.info("Response for joinChannel {} {} {}", channelId, userId, joinChannelResponse);
        return joinChannelResponse;
    }

    @PostMapping("leave/{channelId}/{userId}")
    public OperationResponse leaveChannel(@PathVariable String channelId, @PathVariable String userId) {
        OperationResponse leaveChannelResponse = channelService.leaveChannel(channelId, userId);
        log.info("Response for leaveChannel {} {} {}", channelId, userId, leaveChannelResponse);
        return leaveChannelResponse;
    }

    @GetMapping("members/{channelId}")
    public OperationResponse listChannelMembers(@PathVariable String channelId) {
        OperationResponse listChannelMembersResponse = channelService.listChannelMembers(channelId);
        log.info("Response for listChannelMembers {} {}", channelId, listChannelMembersResponse);
        return listChannelMembersResponse;
    }

    @PostMapping("add-member/{channelId}")
    public OperationResponse addMemberToChannel(@PathVariable String channelId, @RequestBody @Validated ChannelMemberRequest channelMemberRequest) {
        OperationResponse addMemberResponse = channelService.addMemberToChannel(channelId, channelMemberRequest);
        log.info("Response for addMemberToChannel {} {} {}", channelId, channelMemberRequest, addMemberResponse);
        return addMemberResponse;
    }

    @PostMapping("remove-member/{channelId}/{userId}")
    public OperationResponse removeMemberFromChannel(@PathVariable String channelId, @PathVariable String userId) {
        OperationResponse removeMemberResponse = channelService.removeMemberFromChannel(channelId, userId);
        log.info("Response for removeMemberFromChannel {} {} {}", channelId, userId, removeMemberResponse);
        return removeMemberResponse;
    }

    @PostMapping("members-notification-preference/{channelId}/{userId}")
    public OperationResponse updateNotificationPreference(@PathVariable String channelId, @PathVariable String userId,
                                                          @RequestBody @Validated NotificationPreferenceRequest preferenceRequest) {
        OperationResponse updateNotificationPreferenceResponse = channelService.updateNotificationPreference(channelId, userId, preferenceRequest);
        log.info("Response for updateNotificationPreference {} {} {} {}", channelId, userId, preferenceRequest, updateNotificationPreferenceResponse);
        return updateNotificationPreferenceResponse;
    }

    @PostMapping("create-direct-message-channel")
    public OperationResponse createDirectMessageChannel(@RequestBody @Validated ChannelRequest channelRequest) {
        OperationResponse createDmResponse = channelService.createDirectMessageChannel(channelRequest);
        log.info("Response for createDirectMessageChannel {} {}", channelRequest, createDmResponse);
        return createDmResponse;
    }
}