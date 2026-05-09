package com.nixspace.api.service;


import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.ChannelMemberRepository;
import com.nixspace.api.repository.MessageReactionRepository;
import com.nixspace.api.repository.MessageRepository;
import com.nixspace.domain.model.ChannelMember;
import com.nixspace.domain.model.Message;
import com.nixspace.domain.model.MessageReaction;
import com.nixspace.domain.model.ids.MessageReactionId;
import com.nixspace.domain.request.MessageRequest;
import com.nixspace.domain.request.ReactionRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class MessageService {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    MessageReactionRepository messageReactionRepository;

    @Autowired
    ChannelMemberRepository channelMemberRepository;


    public OperationResponse<Message> sendMessage(MessageRequest messageRequest) {
        log.info("Incoming sendMessage {}", messageRequest);
        try {
            Message message = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .channelId(messageRequest.getChannelId())
                    .senderId(messageRequest.getSenderId())
                    .parentMessageId(messageRequest.getParentMessageId())
                    .messageType(messageRequest.getMessageType())
                    .content(messageRequest.getContent())
                    .contentType(messageRequest.getContentType())
                    .edited(false)
                    .deleted(false)
                    .replyCount(0)
                    .build();

            Message savedMessage = messageRepository.save(message);

            if (Objects.nonNull(savedMessage)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedMessage);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
            }
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "message", e.getMessage()), null);
        }
    }


    public OperationResponse<Page<Message>> getChannelMessages(String channelId, int page, int size) {
        log.info("Incoming getChannelMessages channelId {} page {} size {}", channelId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messages = messageRepository.findByChannelIdAndParentMessageIdIsNullAndDeletedFalse(channelId, pageable);

            if (Objects.nonNull(messages) && !messages.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, messages);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "messages"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving messages for channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "messages", e.getMessage()), null);
        }
    }


    public OperationResponse<Message> editMessage(String messageId, String senderId, MessageRequest messageRequest) {
        log.info("Incoming editMessage messageId {} senderId {}", messageId, senderId);
        try {
            Message message = messageRepository.findByMessageId(messageId);

            if (Objects.isNull(message)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "message"), null);
            }

            if (!message.getSenderId().equals(senderId)) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ACCESS_DENIED, null);
            }

            if (message.isDeleted()) {
                return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.FAILED_TO_UPDATE_REASON, "message has been deleted"), null);
            }

            message.setContent(messageRequest.getContent());
            message.setEdited(true);

            Message updatedMessage = messageRepository.save(message);
            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedMessage);
        } catch (Exception e) {
            log.error("Error editing message {}: {}", messageId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "message", e.getMessage()), null);
        }
    }


    public OperationResponse<Message> deleteMessage(String messageId, String senderId) {
        log.info("Incoming deleteMessage messageId {} senderId {}", messageId, senderId);
        try {
            Message message = messageRepository.findByMessageId(messageId);

            if (Objects.isNull(message)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "message"), null);
            }

            if (!message.getSenderId().equals(senderId)) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ACCESS_DENIED, null);
            }

            message.setDeleted(true);
            message.setContent(null);

            Message deletedMessage = messageRepository.save(message);
            return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "message", messageId), deletedMessage);
        } catch (Exception e) {
            log.error("Error deleting message {}: {}", messageId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "message", e.getMessage()), null);
        }
    }


    public OperationResponse<List<Message>> getThreadReplies(String parentMessageId) {
        log.info("Incoming getThreadReplies parentMessageId {}", parentMessageId);
        try {
            Message parentMessage = messageRepository.findByMessageId(parentMessageId);

            if (Objects.isNull(parentMessage)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "message"), null);
            }

            List<Message> replies = messageRepository.findByParentMessageIdAndDeletedFalseOrderByCreatedAtAsc(parentMessageId);

            if (Objects.nonNull(replies) && !replies.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, replies);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "replies"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving thread replies for message {}: {}", parentMessageId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "replies", e.getMessage()), null);
        }
    }


    public OperationResponse<MessageReaction> addReaction(String messageId, ReactionRequest reactionRequest) {
        log.info("Incoming addReaction messageId {} request {}", messageId, reactionRequest);
        try {
            Message message = messageRepository.findByMessageId(messageId);

            if (Objects.isNull(message)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "message"), null);
            }

            boolean alreadyReacted = messageReactionRepository.existsByMessageIdAndUserIdAndReaction(
                    messageId, reactionRequest.getUserId(), reactionRequest.getReaction());

            if (alreadyReacted) {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ALREADY_EXISTS, null);
            }

            MessageReaction messageReaction = MessageReaction.builder()
                    .messageId(messageId)
                    .userId(reactionRequest.getUserId())
                    .reaction(reactionRequest.getReaction())
                    .createdAt(LocalDateTime.now())
                    .build();

            MessageReaction savedReaction = messageReactionRepository.save(messageReaction);

            if (Objects.nonNull(savedReaction)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedReaction);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
            }
        } catch (Exception e) {
            log.error("Error adding reaction to message {}: {}", messageId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "reaction", e.getMessage()), null);
        }
    }


    public OperationResponse removeReaction(String messageId, String userId, String reaction) {
        log.info("Incoming removeReaction messageId {} userId {} reaction {}", messageId, userId, reaction);
        try {
            MessageReactionId messageReactionId = new MessageReactionId(messageId, userId, reaction);
            MessageReaction messageReaction = messageReactionRepository.findById(messageReactionId).orElse(null);

            if (Objects.nonNull(messageReaction)) {
                messageReactionRepository.delete(messageReaction);
                return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "reaction", reaction), null);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "reaction"), null);
            }
        } catch (Exception e) {
            log.error("Error removing reaction {} from message {} for user {}: {}", reaction, messageId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "reaction", e.getMessage()), null);
        }
    }


    public OperationResponse<ChannelMember> markChannelAsRead(String channelId, String userId, String lastReadMessageId) {
        log.info("Incoming markChannelAsRead channelId {} userId {} lastReadMessageId {}", channelId, userId, lastReadMessageId);
        try {
            ChannelMember channelMember = channelMemberRepository.findByChannelIdAndUserId(channelId, userId);

            if (Objects.isNull(channelMember)) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "channel member"), null);
            }

            channelMember.setLastReadMessageId(lastReadMessageId);
            ChannelMember updatedMember = channelMemberRepository.save(channelMember);
            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedMember);
        } catch (Exception e) {
            log.error("Error marking channel {} as read for user {}: {}", channelId, userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "channel member", e.getMessage()), null);
        }
    }
}
