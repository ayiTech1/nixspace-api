package com.nixspace.api.controller;

import com.nixspace.infrastructure.redis.PresenceService;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST endpoints for presence status + STOMP handlers for typing events.
 *
 * WebSocket topics:
 *   /topic/channels/{channelId}/typing   — who is currently typing
 *   /topic/workspaces/{workspaceId}/presence — online/offline events
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Presence", description = "Online status and typing indicators")
public class PresenceController {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── REST: heartbeat & presence query ────────────────────────────────

    @PostMapping("/api/v1/workspaces/{workspaceId}/presence/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send a heartbeat to mark the user as online in a workspace")
    public void heartbeat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        presenceService.heartbeat(principal.getId(), workspaceId);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/presence")
    @Operation(summary = "Get online/offline status for a list of user IDs")
    public Map<Long, Boolean> getBulkPresence(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @RequestParam List<Long> userIds
    ) {
        return presenceService.getBulkPresence(userIds, workspaceId);
    }

    @GetMapping("/api/v1/channels/{channelId}/typing")
    @Operation(summary = "Get the set of users currently typing in a channel")
    public Set<String> getTypingUsers(@PathVariable Long channelId) {
        return presenceService.getTypingUsers(channelId);
    }

    // ─── WebSocket: typing start / stop ──────────────────────────────────

    /**
     * Client sends: SEND /app/channels/{channelId}/typing/start
     * Broadcasts:   /topic/channels/{channelId}/typing  { userId, typing: true }
     */
    @MessageMapping("/channels/{channelId}/typing/start")
    public void typingStart(@DestinationVariable Long channelId, Principal principal) {
        Long userId = extractUserId(principal);
        presenceService.setTyping(userId, channelId);
        broadcastTyping(channelId, userId, true);
    }

    /**
     * Client sends: SEND /app/channels/{channelId}/typing/stop
     * Broadcasts:   /topic/channels/{channelId}/typing  { userId, typing: false }
     */
    @MessageMapping("/channels/{channelId}/typing/stop")
    public void typingStop(@DestinationVariable Long channelId, Principal principal) {
        Long userId = extractUserId(principal);
        presenceService.clearTyping(userId, channelId);
        broadcastTyping(channelId, userId, false);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private void broadcastTyping(Long channelId, Long userId, boolean isTyping) {
        messagingTemplate.convertAndSend(
                "/topic/channels/" + channelId + "/typing",
                Map.of("userId", userId, "typing", isTyping)
        );
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal up) {
                return up.getId();
            }
        }
        throw new IllegalStateException("Cannot extract userId from WebSocket principal");
    }
}
