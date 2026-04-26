package com.nixspace.domain.service;

import com.nixspace.api.dto.request.MessageRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(Long userId, Long channelId, SendMessageRequest request);
    Responses.PagedResponse<MessageResponse> getChannelMessages(Long userId, Long channelId, Pageable pageable);
    List<MessageResponse> getThreadReplies(Long userId, Long channelId, Long parentMessageId);
    MessageResponse editMessage(Long userId, Long channelId, Long messageId, EditMessageRequest request);
    void deleteMessage(Long userId, Long channelId, Long messageId);
    void addReaction(Long userId, Long channelId, Long messageId, AddReactionRequest request);
    void removeReaction(Long userId, Long channelId, Long messageId, String reaction);
    void markRead(Long userId, Long channelId, MarkReadRequest request);
}
