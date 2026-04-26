package com.nixspace.api.dto.request;

import com.nixspace.common.enums.ChannelType;
import com.nixspace.common.enums.NotificationPreference;
import jakarta.validation.constraints.*;

import java.util.List;

public class ChannelRequests {

    public record CreateChannelRequest(
            @NotBlank @Size(min = 1, max = 100)
            @Pattern(regexp = "^[a-z0-9-_]+$", message = "Channel name can only contain lowercase letters, numbers, hyphens and underscores")
            String name,

            ChannelType type,

            String topic,
            String description,

            List<Long> memberIds
    ) {}

    public record UpdateChannelRequest(
            @Size(min = 1, max = 100) String name,
            @Size(max = 250) String topic,
            String description
    ) {}

    public record CreateDmRequest(
            @NotNull @Size(min = 1, max = 8, message = "DM can have 1–8 participants")
            List<Long> userIds
    ) {}

    public record UpdateNotificationPrefRequest(
            @NotNull NotificationPreference preference
    ) {}
}
