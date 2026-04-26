package com.nixspace.api.controller;

import com.nixspace.api.dto.request.MessageRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.service.MessageService;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Messages", description = "Send, edit, delete messages and manage reactions")
public class MessageController {

    private final MessageService messageService;

    // ─── Messages ─────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message to a channel (or as a thread reply)")
    public MessageResponse sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.sendMessage(principal.getId(), channelId, request);
    }

    @GetMapping
    @Operation(summary = "Paginate top-level messages in a channel (newest first)")
    public PagedResponse<MessageResponse> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        size = Math.min(size, 100);  // cap page size
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return messageService.getChannelMessages(principal.getId(), channelId, pageable);
    }

    @PatchMapping("/{messageId}")
    @Operation(summary = "Edit the content of a message (sender only)")
    public MessageResponse editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return messageService.editMessage(principal.getId(), channelId, messageId, request);
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a message (sender or channel admin)")
    public void deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @PathVariable Long messageId
    ) {
        messageService.deleteMessage(principal.getId(), channelId, messageId);
    }

    // ─── Threads ──────────────────────────────────────────────────────────

    @GetMapping("/{parentMessageId}/replies")
    @Operation(summary = "Get all thread replies for a parent message")
    public List<MessageResponse> getReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @PathVariable Long parentMessageId
    ) {
        return messageService.getThreadReplies(principal.getId(), channelId, parentMessageId);
    }

    // ─── Reactions ────────────────────────────────────────────────────────

    @PostMapping("/{messageId}/reactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add an emoji reaction to a message")
    public void addReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @Valid @RequestBody AddReactionRequest request
    ) {
        messageService.addReaction(principal.getId(), channelId, messageId, request);
    }

    @DeleteMapping("/{messageId}/reactions/{reaction}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a reaction from a message")
    public void removeReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @PathVariable @Size(min = 1, max = 50) String reaction
    ) {
        messageService.removeReaction(principal.getId(), channelId, messageId, reaction);
    }

    // ─── Read tracking ────────────────────────────────────────────────────

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a channel as read up to a given message ID")
    public void markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long channelId,
            @Valid @RequestBody MarkReadRequest request
    ) {
        messageService.markRead(principal.getId(), channelId, request);
    }
}
