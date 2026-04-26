package com.nixspace.api.controller;

import com.nixspace.api.dto.request.ChannelRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.service.ChannelService;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Channels", description = "Channel management within a workspace")
public class ChannelController {

    private final ChannelService channelService;

    // ─── Channel CRUD ─────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new channel in the workspace")
    public ChannelResponse createChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        return channelService.createChannel(principal.getId(), workspaceId, request);
    }

    @GetMapping
    @Operation(summary = "List all public channels in the workspace")
    public List<ChannelResponse> getChannels(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        return channelService.getWorkspaceChannels(principal.getId(), workspaceId);
    }

    @GetMapping("/mine")
    @Operation(summary = "List channels the authenticated user has joined")
    public List<ChannelResponse> getMyChannels(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        return channelService.getMyChannels(principal.getId(), workspaceId);
    }

    @GetMapping("/{channelId}")
    @Operation(summary = "Get a channel by ID")
    public ChannelResponse getChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        return channelService.getChannel(principal.getId(), workspaceId, channelId);
    }

    @PatchMapping("/{channelId}")
    @Operation(summary = "Update channel name, topic or description")
    public ChannelResponse updateChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId,
            @Valid @RequestBody UpdateChannelRequest request
    ) {
        return channelService.updateChannel(principal.getId(), workspaceId, channelId, request);
    }

    @PostMapping("/{channelId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive a channel (Admin only)")
    public void archiveChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        channelService.archiveChannel(principal.getId(), workspaceId, channelId);
    }

    @DeleteMapping("/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a channel (Workspace Admin/Owner only)")
    public void deleteChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        channelService.deleteChannel(principal.getId(), workspaceId, channelId);
    }

    // ─── Membership ───────────────────────────────────────────────────────

    @PostMapping("/{channelId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Join a public channel")
    public void joinChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        channelService.joinChannel(principal.getId(), workspaceId, channelId);
    }

    @DeleteMapping("/{channelId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leave a channel")
    public void leaveChannel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        channelService.leaveChannel(principal.getId(), workspaceId, channelId);
    }

    @GetMapping("/{channelId}/members")
    @Operation(summary = "List members of a channel")
    public List<ChannelMemberResponse> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId
    ) {
        return channelService.getChannelMembers(principal.getId(), workspaceId, channelId);
    }

    @PostMapping("/{channelId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add a member to a channel (Channel Admin only)")
    public void addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId,
            @PathVariable Long userId
    ) {
        channelService.addMember(principal.getId(), workspaceId, channelId, userId);
    }

    @DeleteMapping("/{channelId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from a channel (Channel Admin only)")
    public void removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId,
            @PathVariable Long userId
    ) {
        channelService.removeMember(principal.getId(), workspaceId, channelId, userId);
    }

    @PatchMapping("/{channelId}/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update notification preference for this channel")
    public void updateNotificationPreference(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long channelId,
            @Valid @RequestBody UpdateNotificationPrefRequest request
    ) {
        channelService.updateNotificationPreference(principal.getId(), channelId, request);
    }

    // ─── Direct Messages ──────────────────────────────────────────────────

    @PostMapping("/dm")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a direct message or group DM channel")
    public ChannelResponse createDm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateDmRequest request
    ) {
        return channelService.createDirectMessage(principal.getId(), workspaceId, request);
    }
}
