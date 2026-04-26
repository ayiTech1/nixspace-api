package com.nixspace.infrastructure.kafka.events;

import com.nixspace.api.dto.response.Responses.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {

    public enum Type { CREATED, UPDATED, DELETED }

    private Type type;
    private Long channelId;
    private Long workspaceId;
    private MessageResponse payload;
}
