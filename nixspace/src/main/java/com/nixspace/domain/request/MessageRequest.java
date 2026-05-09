package com.nixspace.domain.request;

import com.nixspace.domain.enums.ContentType;
import com.nixspace.domain.enums.MessageType;
import lombok.Data;

@Data
public class MessageRequest {
    private String channelId;
    private String senderId;
    private String parentMessageId;
    private MessageType messageType;
    private String content;
    private ContentType contentType;
}
