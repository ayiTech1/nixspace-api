package com.nixspace.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nixspace.common.enums.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Responses {

    // ─────────────────── Auth ───────────────────

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserResponse user
    ) {}

    // ─────────────────── User ───────────────────

    public record UserResponse(
            Long id,
            String email,
            String displayName,
            String avatarUrl,
            String statusText,
            String timezone,
            boolean active,
            Instant lastSeenAt,
            Instant createdAt
    ) {}

    public record UserSummary(
            Long id,
            String displayName,
            String avatarUrl,
            String statusText
    ) {}

    // ─────────────────── Workspace ───────────────────

    public record WorkspaceResponse(
            Long id,
            String name,
            String slug,
            String description,
            String iconUrl,
            UserSummary owner,
            long memberCount,
            Instant createdAt
    ) {}

    public record WorkspaceMemberResponse(
            Long id,
            UserSummary user,
            WorkspaceRole role,
            Instant joinedAt
    ) {}

    // ─────────────────── Channel ───────────────────

    public record ChannelResponse(
            Long id,
            Long workspaceId,
            String name,
            ChannelType type,
            String topic,
            String description,
            UserSummary createdBy,
            boolean archived,
            boolean defaultChannel,
            long memberCount,
            Instant createdAt
    ) {}

    public record ChannelMemberResponse(
            Long id,
            UserSummary user,
            ChannelRole role,
            NotificationPreference notificationPreference,
            Instant joinedAt
    ) {}

    // ─────────────────── Message ───────────────────

    public record MessageResponse(
            Long id,
            Long channelId,
            UserSummary sender,
            Long parentMessageId,
            MessageType messageType,
            String content,
            ContentType contentType,
            boolean edited,
            boolean deleted,
            int replyCount,
            List<ReactionSummary> reactions,
            List<FileResponse> files,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ReactionSummary(
            String reaction,
            long count,
            List<Long> userIds   // who reacted (for tooltip display)
    ) {}

    // ─────────────────── File ───────────────────

    public record FileUploadUrlResponse(
            Long fileId,
            String uploadUrl,
            String storageKey
    ) {}

    public record FileResponse(
            Long id,
            String fileName,
            String mimeType,
            Long fileSize,
            String downloadUrl,
            VirusScanStatus virusScanStatus,
            Instant createdAt
    ) {}

    // ─────────────────── Notification ───────────────────

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String entityType,
            Long entityId,
            UserSummary actor,
            boolean read,
            Instant createdAt
    ) {}

    // ─────────────────── Unread Counts ───────────────────

    public record UnreadCountsResponse(
            Map<Long, Long> channelUnreadCounts,   // channelId -> count
            long totalNotificationsUnread
    ) {}

    // ─────────────────── Pagination ───────────────────

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {}

    // ─────────────────── Generic ───────────────────

    public record ApiResponse(boolean success, String message) {
        public static ApiResponse ok(String message) {
            return new ApiResponse(true, message);
        }
    }
}
